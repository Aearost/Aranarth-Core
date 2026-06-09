package com.aearost.aranarthcore.abilities.earthbending.lavabending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MagmaWave extends LavaAbility implements AddonAbility {

    private static final int SOURCE_SCAN_RADIUS = 3;

    private static final double HALF_LENGTH = 4.0;
    private static final double HALF_WIDTH = 2.5;
    private static final double HALF_HEIGHT = 0.75;
    private static final double SAMPLE_STEP = 0.5;
    private static final int MAX_HEIGHT_ABOVE_GROUND = 7;
    private static final int GROW_TICKS = 10;

    private static final double WAVE_SPEED = 0.9;
    private static final double STEER_LERP = 0.35;
    private static final double HIT_RADIUS = 3.5;
    private static final double PULLBACK_STOP_DIST = 2.5;
    private static final double RANGE = 28.0;
    private static final long WAVE_DURATION = 4000L;

    private static final Particle.DustOptions SELECT_DUST =
            new Particle.DustOptions(Color.fromRGB(220, 65, 10), 1.2f);

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private Vector steerDir;
    private Location waveCenter;
    private int tickCount;
    private final long startTime = System.currentTimeMillis();

    private final Map<Block, BlockData> originalData = new HashMap<>();
    private final Set<Block> waveBlocks = new HashSet<>();
    private final Set<UUID> hitEntities = new HashSet<>();
    private final Random random = new Random();

    private static final Map<UUID, Block> pendingSources = new HashMap<>();
    private static final Map<UUID, BukkitTask> pendingSourceTasks = new HashMap<>();
    private static final Map<UUID, MagmaWave> activeInstances = new HashMap<>();

    public MagmaWave(Player player) {
        super(player);
        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 8000;
        damage = 14.0;

        Block source = pendingSources.remove(player.getUniqueId());
        cancelPendingSourceTask(player.getUniqueId());
        if (source == null || source.getType() != Material.LAVA) {
            return;
        }

        steerDir = player.getEyeLocation().getDirection().clone().normalize();

        // Start the wave centre 1 block ahead of the source so it clears the lava block itself
        waveCenter = source.getLocation().add(0.5, 0.5, 0.5).add(steerDir.clone());
        tickCount = 0;

        activeInstances.put(player.getUniqueId(), this);

        Location sl = source.getLocation().add(0.5, 0.5, 0.5);
        World slw = sl.getWorld();
        slw.playSound(sl, Sound.BLOCK_LAVA_AMBIENT, 2.5f, 0.3f);
        slw.playSound(sl, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.45f);
        slw.playSound(sl, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.25f);

        start();
    }

    @Override
    public void progress() {
        if (!player.isOnline() || player.isDead()) {
            cancelInstantly();
            return;
        }
        if (System.currentTimeMillis() - startTime >= WAVE_DURATION) {
            finishAbility();
            return;
        }
        if (waveCenter.distanceSquared(player.getLocation()) >= RANGE * RANGE) {
            finishAbility();
            return;
        }

        // Steering the wave
        Vector targetDir;
        if (player.isSneaking()) {
            Vector toPlayer = player.getEyeLocation().toVector().subtract(waveCenter.toVector());
            if (toPlayer.lengthSquared() <= PULLBACK_STOP_DIST * PULLBACK_STOP_DIST) {
                finishAbility();
                return;
            }
            targetDir = toPlayer.normalize();
        } else {
            targetDir = player.getEyeLocation().getDirection().clone().normalize();
        }

        steerDir = steerDir.clone()
                .add(targetDir.clone().subtract(steerDir).multiply(STEER_LERP))
                .normalize();

        // Advance the wave centre
        waveCenter = waveCenter.clone().add(steerDir.clone().multiply(WAVE_SPEED));

        // Applies the height limit
        int groundY = groundYBelow(waveCenter);
        if (groundY != Integer.MIN_VALUE) {
            double maxCenterY = groundY + MAX_HEIGHT_ABOVE_GROUND;
            if (waveCenter.getY() > maxCenterY) {
                waveCenter.setY(maxCenterY);
                // Flatten steerDir so the wave stops climbing and travels horizontally
                if (steerDir.getY() > 0) {
                    Vector flat = steerDir.clone().setY(0);
                    steerDir = flat.lengthSquared() > 0.001 ? flat.normalize()
                            : new Vector(1, 0, 0);
                }
            }
        }

        tickCount++;

        // Rebuild blocks
        Set<Block> desired = computeWaveBlocks();

        Iterator<Block> it = waveBlocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (!desired.contains(b)) {
                restoreBlock(b);
                it.remove();
            }
        }
        for (Block b : desired) {
            if (!waveBlocks.contains(b)) {
                originalData.putIfAbsent(b, b.getBlockData().clone());
                Levelled lava = (Levelled) Material.LAVA.createBlockData();
                lava.setLevel(0); // full source lava — consistent, opaque
                b.setBlockData(lava, false);
                waveBlocks.add(b);
            }
        }

        checkDamage();
        playTravelSound();
    }

    /**
     * Fills an axis-aligned ellipsoid in the wave's local coordinate frame.
     */
    private Set<Block> computeWaveBlocks() {
        Set<Block> blocks = new HashSet<>();

        // Uniform grow-in
        double grow = Math.min(1.0, (double) tickCount / GROW_TICKS);
        double hl = HALF_LENGTH * grow;
        double hw = HALF_WIDTH * grow;
        double hh = HALF_HEIGHT * grow;
        if (hl < 0.1) {
            return blocks; // too Small to show
        }

        // Always horizontal, perpendicular to the horizontal component of steerDir
        Vector horizFwd = steerDir.clone().setY(0);
        Vector sideAxis;
        if (horizFwd.lengthSquared() < 0.01) {
            // Wave is aimed nearly straight up or down
            sideAxis = new Vector(1, 0, 0);
        } else {
            horizFwd.normalize();
            // Rotate 90 degrees in the horizontal plane
            sideAxis = new Vector(-horizFwd.getZ(), 0, horizFwd.getX());
        }

        // Sample the ellipsoidal volume
        for (double fl = -hl; fl <= hl; fl += SAMPLE_STEP) {
            for (double fs = -hw; fs <= hw; fs += SAMPLE_STEP) {
                for (double fh = -hh; fh <= hh; fh += SAMPLE_STEP) {
                    double e = sq(fl / hl) + sq(fs / hw) + sq(fh / hh);
                    if (e > 1.0) {
                        continue;
                    }

                    Location pos = waveCenter.clone()
                            .add(steerDir.clone().multiply(fl)) // Along travel axis
                            .add(sideAxis.clone().multiply(fs)) // Horizontal width
                            .add(0, fh, 0); // Vertical height (world Y)

                    addIfOpen(pos.getBlock(), blocks);
                }
            }
        }

        return blocks;
    }

    private static double sq(double v) {
        return v * v;
    }

    private void addIfOpen(Block b, Set<Block> out) {
        // Keep blocks we already placed so they don't flicker every tick
        if (waveBlocks.contains(b) || (!b.getType().isSolid() && b.getType() != Material.LAVA)) {
            out.add(b);
        }
    }

    private void checkDamage() {
        double radSq = HIT_RADIUS * HIT_RADIUS;
        Location center = waveCenter.clone().add(0, 1, 0);
        for (LivingEntity entity : waveCenter.getWorld().getLivingEntities()) {
            if (entity.equals(player) || hitEntities.contains(entity.getUniqueId())) {
                continue;
            }
            if (entity.getLocation().add(0, entity.getHeight() / 2.0, 0)
                    .distanceSquared(center) > radSq) {
                continue;
            }
            hitEntities.add(entity.getUniqueId());
            DamageHandler.damageEntity(entity, damage, this);
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.85f);
        }
    }

    public static void trySelectSource(Player player) {
        Block source = scanLavaSource(player);
        if (source == null) {
            return;
        }
        cancelPendingSourceTask(player.getUniqueId());
        pendingSources.put(player.getUniqueId(), source);
        source.getWorld().playSound(source.getLocation().add(0.5, 0.5, 0.5),
                Sound.BLOCK_LAVA_AMBIENT, 0.8f, 1.4f);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Block current = pendingSources.get(player.getUniqueId());
                if (current == null) {
                    cancel();
                    return;
                }
                if (current.getType() != Material.LAVA) {
                    pendingSources.remove(player.getUniqueId());
                    pendingSourceTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                Location sourceCenter = current.getLocation().add(0.5, 0.5, 0.5);
                if (!player.isOnline()
                        || !player.getWorld().equals(sourceCenter.getWorld())
                        || player.getLocation().distanceSquared(sourceCenter) > 64.0) {
                    pendingSources.remove(player.getUniqueId());
                    pendingSourceTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                spawnSelectionParticles(current);
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 2L);
        pendingSourceTasks.put(player.getUniqueId(), task);
    }

    private static void cancelPendingSourceTask(UUID uuid) {
        BukkitTask task = pendingSourceTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private static void spawnSelectionParticles(Block source) {
        Location center = source.getLocation().add(0.5, 1.1, 0.5);
        Random rng = new Random();
        for (int i = 0; i < 12; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double r = 0.15 + rng.nextDouble() * 0.45;
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(angle) * r, rng.nextDouble() * 0.2, Math.sin(angle) * r),
                    1, 0, 0, 0, 0, SELECT_DUST);
        }
        center.getWorld().spawnParticle(Particle.LAVA, center, 4, 0.2, 0.1, 0.2, 0);
    }

    public static boolean hasPendingSource(UUID uuid) {
        return pendingSources.containsKey(uuid);
    }

    public static void clearPendingSource(UUID uuid) {
        pendingSources.remove(uuid);
        cancelPendingSourceTask(uuid);
    }

    private static int groundYBelow(Location loc) {
        World world = loc.getWorld();
        int x = loc.getBlockX(), z = loc.getBlockZ();
        int startY = Math.min(loc.getBlockY(), world.getMaxHeight() - 1);
        for (int y = startY; y >= world.getMinHeight(); y--) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void restoreBlock(Block b) {
        BlockData orig = originalData.remove(b);
        if (orig != null) {
            b.setBlockData(orig, false);
        } else {
            b.setType(Material.AIR, false);
        }
    }

    private void playTravelSound() {
        World world = waveCenter.getWorld();
        world.playSound(waveCenter, Sound.ENTITY_BOAT_PADDLE_WATER,
                1.1f, 0.5f);
        world.playSound(waveCenter, Sound.ENTITY_BOAT_PADDLE_LAND,
                1.1f, 0.5f);
        if (tickCount % 2 == 0) {
            world.playSound(waveCenter, Sound.BLOCK_LAVA_POP,
                    0.85f, 0.5f + (float) (random.nextDouble() * 0.15));
        }
        if (tickCount % 3 == 0) {
            world.playSound(waveCenter, Sound.BLOCK_BASALT_BREAK,
                    0.55f, 0.3f + (float) (random.nextDouble() * 0.1));
        }
    }

    private static Block scanLavaSource(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int px = loc.getBlockX(), py = loc.getBlockY(), pz = loc.getBlockZ();
        Block closest = null;
        double closestDSq = Double.MAX_VALUE;
        for (int dx = -SOURCE_SCAN_RADIUS; dx <= SOURCE_SCAN_RADIUS; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -SOURCE_SCAN_RADIUS; dz <= SOURCE_SCAN_RADIUS; dz++) {
                    Block b = world.getBlockAt(px + dx, py + dy, pz + dz);
                    if (b.getType() != Material.LAVA) {
                        continue;
                    }
                    if (!(b.getBlockData() instanceof Levelled lev) || lev.getLevel() != 0) {
                        continue;
                    }
                    double dSq = b.getLocation().distanceSquared(loc);
                    if (dSq < closestDSq) {
                        closestDSq = dSq;
                        closest = b;
                    }
                }
            }
        }
        return closest;
    }

    private void restoreAllBlocks() {
        for (Block b : new HashSet<>(waveBlocks)) restoreBlock(b);
        waveBlocks.clear();
        for (Map.Entry<Block, BlockData> e : new HashMap<>(originalData).entrySet())
            e.getKey().setBlockData(e.getValue(), false);
        originalData.clear();
    }

    private void finishAbility() {
        restoreAllBlocks();
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        restoreAllBlocks();
        remove();
    }

    public void endWithCooldown() {
        restoreAllBlocks();
        bPlayer.addCooldown(this);
        remove();
    }

    public boolean isLavaProtected(LivingEntity entity) {
        if (entity.equals(player)) {
            return true;
        }
        if (waveCenter == null) {
            return false;
        }
        double protRadSq = (HIT_RADIUS + 1.0) * (HIT_RADIUS + 1.0);
        return waveCenter.distanceSquared(
                entity.getLocation().add(0, entity.getHeight() / 2.0, 0)) <= protRadSq;
    }

    public static boolean isWaveBlock(Block block) {
        for (MagmaWave w : activeInstances.values())
            if (w.originalData.containsKey(block)) {
                return true;
            }
        return false;
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static MagmaWave getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public static Map<UUID, MagmaWave> getActiveInstances() {
        return Collections.unmodifiableMap(activeInstances);
    }

    @Override
    public void remove() {
        restoreAllBlocks();
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return waveCenter != null ? waveCenter : player.getLocation();
    }

    @Override
    public String getName() {
        return "MagmaWave";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        restoreAllBlocks();
    }

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Control a heavy wave of molten lava and unleash it onto your enemies.\n" +
                ChatUtils.translateToColor("&fUsage: Tap Sneak (on lava) > Left-click (controllable) > " +
                        "Hold Sneak (pull back the wave)");
    }
}
