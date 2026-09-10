package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Regrowth extends PlantAbility implements AddonAbility {

    public enum Phase { GROWING, STREAMING, PULSING }

    // ---- Shared constants ----
    private static final double SOURCE_RANGE = 6.0;

    // ---- Growing phase constants ----
    @Attribute(Attribute.RANGE)
    private double growingRadius;
    private static final double GROWING_RING_RATE = 0.08; // blocks/tick - sweeps 5 blocks in ~63 ticks

    private static final List<Material> PLANT_POOL = List.of(
            Material.SHORT_GRASS,
            Material.SHORT_GRASS,
            Material.SHORT_GRASS,
            Material.DANDELION,
            Material.POPPY
    );

    private static final Set<Material> VALID_SOIL = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.ROOTED_DIRT
    );

    // ---- Water wave constants ----
    private static final double STREAM_SPEED = 0.4;
    private static final double WAVE_RING_RATE = 0.12; // blocks/tick - sweeps 4 blocks in ~33 ticks
    private static final double WAVE_RADIUS = 4.0;

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH,
            Material.COCOA,
            Material.PUMPKIN_STEM,
            Material.MELON_STEM
    );

    private static final Particle.DustOptions GROW_DUST =
            new Particle.DustOptions(Color.fromRGB(60, 180, 40), 0.7f);

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    private Phase phase;
    private final Random random = new Random();

    private double growingRingRadius;
    private Location sourceLocation;
    private Location streamHead;
    private double waveRingRadius;
    private final Set<Block> grownCrops = new HashSet<>();

    private static final Map<UUID, Regrowth> activeInstances = new HashMap<>();
    private static final Map<UUID, Block> pendingSources = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> pendingSourceTasks = new HashMap<>();

    public Regrowth(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 10_000L;
        growingRadius = 5.0;

        if (pendingSources.containsKey(player.getUniqueId())) {
            // Water wave path
            Block source = pendingSources.get(player.getUniqueId());
            clearPendingSource(player.getUniqueId());

            phase = Phase.STREAMING;
            sourceLocation = source.getLocation().clone();
            streamHead = source.getLocation().clone().add(0.5, 0.5, 0.5);
            source.setType(Material.AIR);
        } else {
            // Old growing path - no cooldown check needed
            phase = Phase.GROWING;
            growingRingRadius = 0.0;
        }

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            restoreWater();
            remove();
            return;
        }

        switch (phase) {
            case GROWING -> progressGrowing();
            case STREAMING -> progressStreaming();
            case PULSING -> progressPulsing();
        }
    }

    private void progressGrowing() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        double prev = growingRingRadius;
        growingRingRadius += GROWING_RING_RATE;

        spawnRingParticles(growingRingRadius, GROW_DUST, null);
        tryGrowPlantsInBand(prev, growingRingRadius);

        if (growingRingRadius >= growingRadius) {
            growingRingRadius = 0.0;
        }
    }

    private void tryGrowPlantsInBand(double inner, double outer) {
        Location center = player.getLocation();
        int r = (int) Math.ceil(outer) + 1;
        double innerSq = inner * inner;
        double outerSq = outer * outer;

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double distSq = x * x + z * z;
                if (distSq < innerSq || distSq > outerSq) {
                    continue;
                }
                // Low per-block chance so only a few plants grow per sweep
                if (random.nextDouble() > 0.04) {
                    continue;
                }
                for (int dy = -3; dy <= 3; dy++) {
                    Block soil = center.getBlock().getRelative(x, dy, z);
                    if (!VALID_SOIL.contains(soil.getType())) {
                        continue;
                    }
                    Block above = soil.getRelative(BlockFace.UP);
                    if (above.getType() != Material.AIR) {
                        continue;
                    }
                    Material plant = PLANT_POOL.get(random.nextInt(PLANT_POOL.size()));
                    above.setType(plant);

                    Location loc = above.getLocation().add(0.5, 0.3, 0.5);
                    player.getWorld().spawnParticle(Particle.DUST, loc, 6, 0.2, 0.2, 0.2, 0, GROW_DUST);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.2, 0.2, 0.2, 0);
                    player.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.3f, 1.2f);
                    break; // One plant per x/z per tick
                }
            }
        }
    }

    private void progressStreaming() {
        if (!player.isSneaking()) {
            restoreWater();
            remove();
            return;
        }

        Location target = player.getLocation().add(0, 1, 0);
        Vector dir = target.toVector().subtract(streamHead.toVector());
        double dist = dir.length();

        if (dist <= STREAM_SPEED + 0.2) {
            transitionToPulsing();
            return;
        }

        streamHead.add(dir.normalize().multiply(STREAM_SPEED));
        spawnStreamParticles(streamHead);
    }

    private void transitionToPulsing() {
        phase = Phase.PULSING;
        waveRingRadius = 0.0;
        restoreWater();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.7f);
    }

    private void progressPulsing() {
        double prev = waveRingRadius;
        waveRingRadius += WAVE_RING_RATE;

        spawnRingParticles(waveRingRadius, AranarthBendingUtils.WATER_DUST, Particle.SPLASH);
        growCropsInBand(prev, waveRingRadius);

        if (((int) (waveRingRadius / WAVE_RING_RATE)) % 6 == 0) {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WATER_AMBIENT, 0.25f, 1.1f);
        }

        if (waveRingRadius >= WAVE_RADIUS) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    private void growCropsInBand(double inner, double outer) {
        Location center = player.getLocation();
        int r = (int) Math.ceil(outer) + 1;
        double innerSq = inner * inner;
        double outerSq = outer * outer;

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double distSq = x * x + z * z;
                if (distSq < innerSq || distSq > outerSq) {
                    continue;
                }
                for (int dy = -2; dy <= 2; dy++) {
                    Block block = center.getBlock().getRelative(x, dy, z);
                    if (!CROPS.contains(block.getType())) {
                        continue;
                    }
                    if (grownCrops.contains(block)) {
                        continue;
                    }
                    if (block.getBlockData() instanceof Ageable ageable
                            && ageable.getAge() < ageable.getMaximumAge()) {
                        ageable.setAge(ageable.getAge() + 1);
                        block.setBlockData(ageable);
                        grownCrops.add(block);

                        Location cropLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, cropLoc, 4, 0.2, 0.2, 0.2, 0);
                        player.getWorld().spawnParticle(Particle.DUST, cropLoc, 3, 0.2, 0.2, 0.2, 0, GROW_DUST);
                        player.getWorld().playSound(cropLoc, Sound.BLOCK_GRASS_PLACE, 0.3f, 1.4f);
                    }
                }
            }
        }
    }

    /**
     * Draws a flat ring at the player's feet level.
     */
    private void spawnRingParticles(double r, Particle.DustOptions dust, Particle extra) {
        Location center = player.getLocation().add(0, 0.05, 0);
        World world = player.getWorld();
        int points = Math.max(14, (int) (r * 18));
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            Location p = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
            if (extra != null) {
                world.spawnParticle(extra, p, 1, 0, 0.02, 0, 0.01);
            }
        }
    }

    private void spawnStreamParticles(Location head) {
        World world = player.getWorld();
        world.spawnParticle(Particle.SPLASH, head, 4, 0.05, 0.05, 0.05, 0.04);
        world.spawnParticle(Particle.DUST, head, 2, 0.04, 0.04, 0.04, 0, AranarthBendingUtils.WATER_DUST);
        if (Math.random() < 0.5) {
            world.spawnParticle(Particle.DRIPPING_WATER, head, 1, 0.06, 0.06, 0.06, 0);
        }
    }

    private void restoreWater() {
        if (sourceLocation == null) {
            return;
        }
        Block block = sourceLocation.getBlock();
        if (block.getType() == Material.AIR) {
            block.setType(Material.WATER);
        }
        sourceLocation = null;
    }

    public static void trySelectSource(Player player, Block block) {
        if (block.getType() != Material.WATER) {
            return;
        }
        if (!(block.getBlockData() instanceof Levelled levelled) || levelled.getLevel() != 0) {
            return;
        }
        if (!block.getWorld().equals(player.getWorld())) {
            return;
        }
        if (block.getLocation().distance(player.getLocation()) > SOURCE_RANGE) {
            return;
        }

        BendingPlayer bp = BendingPlayer.getBendingPlayer(player);
        if (bp == null || bp.isOnCooldown("Regrowth")) {
            return;
        }

        clearPendingSource(player.getUniqueId());
        pendingSources.put(player.getUniqueId(), block);

        Location loc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!pendingSources.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                block.getWorld().spawnParticle(Particle.SPLASH, loc, 2, 0.1, 0.1, 0.1, 0.02, null, true);
                block.getWorld().spawnParticle(
                        Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0, AranarthBendingUtils.WATER_DUST, true);
            }
        };
        task.runTaskTimer(AranarthCore.getInstance(), 0L, 2L);
        pendingSourceTasks.put(player.getUniqueId(), task);
    }

    public static boolean hasPendingSource(UUID uuid) {
        return pendingSources.containsKey(uuid);
    }

    public static void clearPendingSource(UUID uuid) {
        pendingSources.remove(uuid);
        BukkitRunnable task = pendingSourceTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Regrowth getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public void remove() {
        restoreWater();
        activeInstances.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "Regrowth";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
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
        return "Channel your plantbending to speed up the growth of grass and flowers into bloom around you. " +
                "Alternatively, you may select a water source, and use it to increase the growth rate of nearby crops.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak | Left-click (water source) > Hold Sneak");
    }
}
