package com.aearost.aranarthcore.abilities.earthbending.sandbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Burial extends SandAbility implements AddonAbility {

    public enum Phase { CASTING, OPENING, SEALING, BURIED, RELEASING }
    private static final double TRAVEL_SPEED = 16.0;
    private static final double PATH_STEP = 0.35;
    private static final double MAX_JITTER = 0.65;
    private static final int PIT_DEPTH = 5;
    private static final int ANIM_TICKS_PER_LAYER = 2;
    private static final long OPENING_DELAY = 600L;
    private static final long SEALING_DURATION = 600L;
    private static final long RELEASE_DURATION = 1500L;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("Duration")
    private long duration;
    @Attribute("DamageInterval")
    private long damageInterval;

    private Phase phase;
    private LivingEntity target;
    private Location burialCenter;
    private Particle.DustOptions[] dustPalette;
    private final Random random = new Random();

    private long castStartTime;
    private double distanceTraveled;
    private double totalDistance;
    private Vector direction;
    private Vector perp;
    private Location pathOrigin;
    private List<double[]> pathPoints;

    private final Map<Location, BlockData> surfaceBlockData = new LinkedHashMap<>();
    private final List<Map<Location, BlockData>> pitLayers = new ArrayList<>();

    private long phaseStartTime;
    private long lastDamageTime;
    private int damageCount;
    private long animationGeneration = 0;
    private boolean isRemoved = false;

    private static final Map<UUID, Burial> activeInstances = new HashMap<>();
    private static final Set<UUID> buriedEntities = new HashSet<>();

    public Burial(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 12000;
        damage = 1.0;
        range = 24.0;
        duration = 3000L;
        damageInterval = 500L;

        LivingEntity foundTarget = findTarget();
        if (foundTarget == null) {
            return;
        }

        Block groundBlock = getSandbendableBlockUnder(foundTarget);
        if (groundBlock == null) {
            return;
        }

        target = foundTarget;
        dustPalette = AranarthBendingUtils.pickSandDustPalette(groundBlock.getType());

        pathOrigin = player.getLocation().clone();
        Vector toTarget = target.getLocation().toVector().subtract(pathOrigin.toVector());
        toTarget.setY(0);
        totalDistance = toTarget.length();

        if (totalDistance < 1.0) {
            return;
        }

        direction = toTarget.clone().normalize();
        perp = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        pathPoints = buildCrevicePath();

        castStartTime = System.currentTimeMillis();
        distanceTraveled = 0;
        phase = Phase.CASTING;

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    private LivingEntity findTarget() {
        Vector lookDir = player.getLocation().getDirection().clone().normalize();
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }
            Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            double dist = toEntity.length();
            if (dist > range || dist < 1.0) {
                continue;
            }
            if (toEntity.clone().normalize().dot(lookDir) < 0.87) {
                continue;
            }
            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }
        return closest;
    }

    /**
     * Returns the topmost sandbendable block at or up to two blocks below the entity's feet.
     */
    private Block getSandbendableBlockUnder(LivingEntity entity) {
        Location loc = entity.getLocation();
        for (int dy = 0; dy >= -2; dy--) {
            Block block = loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() + dy, loc.getBlockZ());
            if (!block.getType().isAir() && EarthAbility.isSandbendable(player, block.getType())) {
                return block;
            }
        }
        return null;
    }

    /** Determines the path in which the line path will go along **/
    private List<double[]> buildCrevicePath() {
        int segments = (int) (totalDistance / PATH_STEP) + 2;
        List<double[]> points = new ArrayList<>(segments);
        double lateralOffset = 0;
        double lateralVel = 0;

        for (int i = 0; i < segments; i++) {
            double d = i * PATH_STEP;

            lateralVel += (random.nextDouble() - 0.5) * 0.65;
            lateralVel = Math.max(-0.38, Math.min(0.38, lateralVel));
            lateralVel *= 0.72;
            lateralOffset += lateralVel;
            lateralOffset = Math.max(-MAX_JITTER, Math.min(MAX_JITTER, lateralOffset));

            double px = pathOrigin.getX() + direction.getX() * d + perp.getX() * lateralOffset;
            double pz = pathOrigin.getZ() + direction.getZ() * d + perp.getZ() * lateralOffset;
            int groundY = pathOrigin.getWorld().getHighestBlockYAt((int) px, (int) pz);

            points.add(new double[]{px, groundY + 1.1, pz});
        }
        return points;
    }

    @Override
    public void progress() {
        if (!player.isOnline()) {
            remove();
            return;
        }
        switch (phase) {
            case CASTING   -> progressCasting();
            case OPENING   -> progressOpening();
            case SEALING   -> progressSealing();
            case BURIED    -> progressBuried();
            case RELEASING -> progressReleasing();
        }
    }

    private void progressCasting() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        long now = System.currentTimeMillis();
        distanceTraveled = Math.min(totalDistance, TRAVEL_SPEED * (now - castStartTime) / 1000.0);

        renderCrevice();

        if (distanceTraveled >= totalDistance) {
            startOpeningAnimation();
        }
    }

    /**
     * Creates the line of sand dust particles along the precomputed path.
     */
    private void renderCrevice() {
        World world = pathOrigin.getWorld();
        int leadIdx = Math.min((int) (distanceTraveled / PATH_STEP), pathPoints.size() - 1);
        int trailStart = Math.max(0, leadIdx - 5);

        for (int i = trailStart; i <= leadIdx; i++) {
            double[] pt = pathPoints.get(i);
            boolean isLead = i >= leadIdx - 1;

            world.spawnParticle(Particle.DUST,
                    pt[0] + (random.nextDouble() - 0.5) * 0.1,
                    pt[1] + random.nextDouble() * 0.07,
                    pt[2] + (random.nextDouble() - 0.5) * 0.1,
                    1, 0, 0, 0, 0,
                    dustPalette[random.nextInt(dustPalette.length)]);

            if (isLead) {
                world.spawnParticle(Particle.DUST,
                        pt[0] + (random.nextDouble() - 0.5) * 0.15,
                        pt[1] + 0.06 + random.nextDouble() * 0.13,
                        pt[2] + (random.nextDouble() - 0.5) * 0.15,
                        1, 0, 0, 0, 0,
                        dustPalette[random.nextInt(dustPalette.length)]);
            }
        }
    }

    /**
     * Records block data for all surface and pit blocks, and opens the pit from the top-down.
     */
    private void startOpeningAnimation() {
        Location targetLoc = target.getLocation();
        World world = targetLoc.getWorld();
        burialCenter = targetLoc.clone();

        int bx = targetLoc.getBlockX();
        int by = targetLoc.getBlockY();
        int bz = targetLoc.getBlockZ();

        // Record surface blocks (y-1)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block surfaceBlock = world.getBlockAt(bx + dx, by - 1, bz + dz);
                if (!surfaceBlock.getType().isAir() && EarthAbility.isSandbendable(player, surfaceBlock.getType())) {
                    surfaceBlockData.put(surfaceBlock.getLocation().clone(), surfaceBlock.getBlockData().clone());
                }
            }
        }

        if (surfaceBlockData.isEmpty()) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        // Record pit blocks organized by layer
        for (int depth = 0; depth < PIT_DEPTH; depth++) {
            Map<Location, BlockData> layerMap = new LinkedHashMap<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block pitBlock = world.getBlockAt(bx + dx, by - depth - 2, bz + dz);
                    if (!pitBlock.getType().isAir() && pitBlock.getType() != Material.BEDROCK) {
                        layerMap.put(pitBlock.getLocation().clone(), pitBlock.getBlockData().clone());
                    }
                }
            }
            pitLayers.add(layerMap);
        }

        world.playSound(burialCenter, Sound.BLOCK_SAND_BREAK, 1.2f, 0.7f);
        world.playSound(burialCenter, Sound.BLOCK_GRAVEL_FALL, 0.6f, 0.85f);

        // Clear surface immediately so the mob begins falling
        for (Location loc : surfaceBlockData.keySet()) {
            world.getBlockAt(loc).setType(Material.AIR);
        }

        // Schedule each pit layer to clear top-down
        final long myGen = ++animationGeneration;
        for (int i = 0; i < pitLayers.size(); i++) {
            final int layerIndex = i;
            long delay = (long) (i + 1) * ANIM_TICKS_PER_LAYER;
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                if (isRemoved || animationGeneration != myGen) return;
                for (Location loc : pitLayers.get(layerIndex).keySet()) {
                    loc.getBlock().setType(Material.AIR);
                }
            }, delay);
        }

        phase = Phase.OPENING;
        phaseStartTime = System.currentTimeMillis();
    }

    private void progressOpening() {
        if (!target.isValid() || target.isDead()) {
            startReleasing();
            return;
        }

        // Actively pull the target downward each tick and suppress horizontal drift
        Vector vel = target.getVelocity();
        target.setVelocity(new Vector(vel.getX() * 0.1, -0.55, vel.getZ() * 0.1));

        if (System.currentTimeMillis() - phaseStartTime >= OPENING_DELAY) {
            startSealing();
        }
    }

    /**
     * Teleports the target to the pit floor and animates the hole closing bottom-up.
     */
    private void startSealing() {
        if (target.isValid() && !target.isDead()) {
            double pitFloorY = burialCenter.getBlockY() - (PIT_DEPTH + 1);
            Location pitFloor = new Location(
                    burialCenter.getWorld(),
                    burialCenter.getBlockX() + 0.5,
                    pitFloorY,
                    burialCenter.getBlockZ() + 0.5,
                    target.getLocation().getYaw(),
                    target.getLocation().getPitch());
            target.teleport(pitFloor);
        }

        buriedEntities.add(target.getUniqueId());

        // Animate blocks closing bottom-up
        final long myGen = ++animationGeneration;
        for (int i = 0; i < pitLayers.size(); i++) {
            final int layerIndex = pitLayers.size() - 1 - i; // deepest first
            long delay = (long) i * ANIM_TICKS_PER_LAYER;
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                if (isRemoved || animationGeneration != myGen) return;
                for (Map.Entry<Location, BlockData> entry : pitLayers.get(layerIndex).entrySet()) {
                    if (entry.getKey().getBlock().getType().isAir()) {
                        entry.getKey().getBlock().setBlockData(entry.getValue());
                    }
                }
            }, delay);
        }

        // Restore surface after all pit layers have closed
        long surfaceDelay = (long) pitLayers.size() * ANIM_TICKS_PER_LAYER;
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (isRemoved || animationGeneration != myGen) return;
            for (Map.Entry<Location, BlockData> entry : surfaceBlockData.entrySet()) {
                if (entry.getKey().getBlock().getType().isAir()) {
                    entry.getKey().getBlock().setBlockData(entry.getValue());
                }
            }
            player.getWorld().playSound(burialCenter, Sound.BLOCK_SAND_PLACE, 1.5f, 0.55f);
        }, surfaceDelay);

        phase = Phase.SEALING;
        phaseStartTime = System.currentTimeMillis();
    }

    private void progressSealing() {
        if (!target.isValid() || target.isDead()) {
            startReleasing();
            return;
        }

        // Keep the target still while the sealing animation plays
        target.setVelocity(new Vector(0, 0, 0));

        if (System.currentTimeMillis() - phaseStartTime >= SEALING_DURATION) {
            enterBuried();
        }
    }

    private void enterBuried() {
        phase = Phase.BURIED;
        phaseStartTime = System.currentTimeMillis();
        lastDamageTime = phaseStartTime;
        damageCount = 0;
    }

    private void progressBuried() {
        if (!target.isValid() || target.isDead()) {
            startReleasing();
            return;
        }

        long now = System.currentTimeMillis();

        // Immobilise the target
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 254, false, false));
        target.setVelocity(new Vector(0, 0, 0));

        // Applies the damage
        if (now - lastDamageTime >= damageInterval && damageCount < 6) {
            DamageHandler.damageEntity(target, damage, this);
            damageCount++;
            lastDamageTime = now;
        }

        if (now - phaseStartTime >= duration) {
            startReleasing();
        }
    }

    /**
     * Animates the hole opening top-down, launches the target upward, and closes the hole bottom-up.
     */
    private void startReleasing() {
        buriedEntities.remove(target.getUniqueId());
        if (target.isValid()) {
            target.removePotionEffect(PotionEffectType.SLOWNESS);
        }

        World world = player.getWorld();
        world.playSound(burialCenter, Sound.BLOCK_SAND_BREAK, 1.3f, 1.1f);

        final long myGen = ++animationGeneration;

        // Open top-down
        for (Location loc : surfaceBlockData.keySet()) {
            world.getBlockAt(loc).setType(Material.AIR);
        }
        for (int i = 0; i < pitLayers.size(); i++) {
            final int layerIndex = i;
            long delay = (long) (i + 1) * ANIM_TICKS_PER_LAYER;
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                if (isRemoved || animationGeneration != myGen) return;
                for (Location loc : pitLayers.get(layerIndex).keySet()) {
                    loc.getBlock().setType(Material.AIR);
                }
            }, delay);
        }

        // Launch the target after the opening animation completes
       long launchDelay = (long) (pitLayers.size() + 1) * ANIM_TICKS_PER_LAYER;
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (isRemoved || animationGeneration != myGen) return;
            if (target.isValid() && !target.isDead()) {
                target.setVelocity(new Vector(0, 1.2, 0));
            }
        }, launchDelay);

        // Close bottom-up after launch
        for (int i = 0; i < pitLayers.size(); i++) {
            final int layerIndex = pitLayers.size() - 1 - i;
            long delay = launchDelay + ((long) i * ANIM_TICKS_PER_LAYER);
            Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                if (isRemoved || animationGeneration != myGen) return;
                for (Map.Entry<Location, BlockData> entry : pitLayers.get(layerIndex).entrySet()) {
                    if (entry.getKey().getBlock().getType().isAir()) {
                        entry.getKey().getBlock().setBlockData(entry.getValue());
                    }
                }
            }, delay);
        }

        long surfaceCloseDelay = launchDelay + ((long) pitLayers.size() * ANIM_TICKS_PER_LAYER);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (isRemoved || animationGeneration != myGen) return;
            for (Map.Entry<Location, BlockData> entry : surfaceBlockData.entrySet()) {
                if (entry.getKey().getBlock().getType().isAir()) {
                    entry.getKey().getBlock().setBlockData(entry.getValue());
                }
            }
        }, surfaceCloseDelay);

        bPlayer.addCooldown(this);
        phase = Phase.RELEASING;
        phaseStartTime = System.currentTimeMillis();
    }

    private void progressReleasing() {
        if (System.currentTimeMillis() - phaseStartTime >= RELEASE_DURATION) {
            remove();
        }
    }

    public void cancelInstantly() {
        if (phase == Phase.CASTING) {
            remove();
        }
    }

    public Phase getPhase() {
        return phase;
    }

    public static boolean isBuried(UUID uuid) {
        return buriedEntities.contains(uuid);
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Burial getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public void remove() {
        super.remove();
        isRemoved = true;
        activeInstances.remove(player.getUniqueId());

        World world = player.getWorld();

        if (phase == Phase.OPENING) {
            // Surface was cleared
            for (Map.Entry<Location, BlockData> entry : surfaceBlockData.entrySet()) {
                Block block = entry.getKey().getBlock();
                if (block.getType().isAir()) {
                    block.setBlockData(entry.getValue());
                }
            }
            for (Map<Location, BlockData> layer : pitLayers) {
                for (Map.Entry<Location, BlockData> entry : layer.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType().isAir()) {
                        block.setBlockData(entry.getValue());
                    }
                }
            }
        } else if (phase == Phase.SEALING || phase == Phase.BURIED) {
            // Surface may be partially or fully sealed
            for (Location loc : surfaceBlockData.keySet()) {
                world.getBlockAt(loc).setType(Material.AIR);
            }
            for (Map<Location, BlockData> layer : pitLayers) {
                for (Map.Entry<Location, BlockData> entry : layer.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType().isAir()) {
                        block.setBlockData(entry.getValue());
                    }
                }
            }
            for (Map.Entry<Location, BlockData> entry : surfaceBlockData.entrySet()) {
                Block block = entry.getKey().getBlock();
                if (block.getType().isAir()) {
                    block.setBlockData(entry.getValue());
                }
            }
        } else if (phase == Phase.RELEASING) {
            // Release animations may be mid-flight
            for (Map<Location, BlockData> layer : pitLayers) {
                for (Map.Entry<Location, BlockData> entry : layer.entrySet()) {
                    Block block = entry.getKey().getBlock();
                    if (block.getType().isAir()) {
                        block.setBlockData(entry.getValue());
                    }
                }
            }
            for (Map.Entry<Location, BlockData> entry : surfaceBlockData.entrySet()) {
                Block block = entry.getKey().getBlock();
                if (block.getType().isAir()) {
                    block.setBlockData(entry.getValue());
                }
            }
        }

        if (target != null) {
            buriedEntities.remove(target.getUniqueId());
            target.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    @Override
    public boolean isSneakAbility() {
        return false;
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
        return burialCenter != null ? burialCenter : player.getLocation();
    }

    @Override
    public String getName() {
        return "Burial";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

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
        return "Rapidly shift the sand beneath a target, and drag them underneath the surface, " +
                "sealing the target underground temporarily and dealing suffocation damage.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (Hold) + Left-Click");
    }
}
