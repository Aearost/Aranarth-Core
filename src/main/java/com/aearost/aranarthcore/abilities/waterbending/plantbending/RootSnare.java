package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class RootSnare extends PlantAbility implements AddonAbility {

    public enum Phase { CHARGING, CHARGED, SPREADING, SNARING, RECEDING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute("SnareDuration")
    private long snareDuration;
    @Attribute("SpreadDuration")
    private long spreadDuration;

    private Phase phase;
    private long chargeStartTime;
    private long chargedStartTime;
    private long spreadStartTime;
    private long snareStartTime;
    private long recedeStartTime;

    private final List<RootPath> rootPaths = new ArrayList<>();
    private final Set<UUID> snaredEntities = new HashSet<>();
    private final Map<UUID, LivingEntity> snaredEntityRefs = new HashMap<>();

    private static final long CHARGE_DURATION_MS = 1000;
    private static final long CHARGED_TIMEOUT_MS = 5000;
    private static final int MIN_WOOD_PILLARS = 2;
    private static final int MAX_TARGET_PATHS = 5;
    private static final int DECORATIVE_PATHS = 3;
    private static final double PATH_STEP = 0.4;
    private static final double MAX_JITTER = 0.6;

    private static final Particle.DustOptions ROOT_DUST =
            new Particle.DustOptions(Color.fromRGB(89, 52, 9), 0.75f);
    private static final Particle.DustOptions ROOT_DUST_DARK =
            new Particle.DustOptions(Color.fromRGB(52, 28, 5), 0.65f);

    private static final Map<UUID, RootSnare> activeInstances = new HashMap<>();
    private static final Set<UUID> snaredPlayers = new HashSet<>();

    private final Random random = new Random();

    /**
     * A single root path from a tree trunk origin to a destination (target or open ground).
     */
    private static class RootPath {
        final List<double[]> points;
        final LivingEntity target;
        final int targetReachIndex;
        boolean targetSnared;

        RootPath(List<double[]> points, LivingEntity target, int targetReachIndex) {
            this.points = points;
            this.target = target;
            this.targetReachIndex = targetReachIndex;
            this.targetSnared = false;
        }
    }

    public RootSnare(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 8000;
        range = 12.0;
        damage = 2.0;
        snareDuration = 2000;
        spreadDuration = 750;

        phase = Phase.CHARGING;
        chargeStartTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        switch (phase) {
            case CHARGING  -> progressCharging();
            case CHARGED   -> progressCharged();
            case SPREADING -> progressSpreading();
            case SNARING   -> progressSnaring();
            case RECEDING  -> progressReceding();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }
        long elapsed = System.currentTimeMillis() - chargeStartTime;
        spawnChargeParticles(Math.min(1.0, (double) elapsed / CHARGE_DURATION_MS));
        if (elapsed < CHARGE_DURATION_MS) {
            return;
        }
        if (!hasForestNearby()) {
            remove();
            return;
        }
        phase = Phase.CHARGED;
        chargedStartTime = System.currentTimeMillis();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.45f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 1.0f, 0.5f);
    }

    private void progressCharged() {
        if (System.currentTimeMillis() - chargedStartTime >= CHARGED_TIMEOUT_MS) {
            remove();
            return;
        }
        spawnChargeParticles(1.0);
    }

    public void onSneakRelease() {
        if (phase == Phase.CHARGING) {
            remove();
            return;
        }
        if (phase != Phase.CHARGED) {
            return;
        }
        buildRootPaths();
        phase = Phase.SPREADING;
        spreadStartTime = System.currentTimeMillis();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 1.2f, 0.35f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 1.0f, 0.3f);
    }

    public void cancelFromSlotChange() {
        if (phase == Phase.CHARGING || phase == Phase.CHARGED) {
            remove();
        }
    }

    private void progressSpreading() {
        long elapsed = System.currentTimeMillis() - spreadStartTime;
        double travelFraction = Math.min(1.0, (double) elapsed / spreadDuration);

        for (RootPath path : rootPaths) {
            int frontIndex = Math.min(path.points.size() - 1,
                    (int) (travelFraction * path.points.size()));
            renderPath(path, 0, frontIndex);
            checkPathHit(path, frontIndex);
        }

        if (elapsed >= spreadDuration) {
            // Force-snare any target path whose front didn't quite trigger during tick sampling
            for (RootPath path : rootPaths) {
                if (path.target != null && !path.targetSnared) {
                    snareEntity(path.target);
                    path.targetSnared = true;
                }
            }
            phase = Phase.SNARING;
            snareStartTime = System.currentTimeMillis();
        }
    }

    private void progressSnaring() {
        for (RootPath path : rootPaths) {
            renderPath(path, 0, path.points.size() - 1);
        }
        tickSnaredMobVelocity();
        if (System.currentTimeMillis() - snareStartTime >= snareDuration) {
            phase = Phase.RECEDING;
            recedeStartTime = System.currentTimeMillis();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.6f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.6f, 0.65f);
        }
    }

    private void progressReceding() {
        long elapsed = System.currentTimeMillis() - recedeStartTime;
        double recedeFraction = Math.min(1.0, (double) elapsed / spreadDuration);

        // Tips retract back toward the tree origins
        for (RootPath path : rootPaths) {
            int frontIndex = Math.max(0,
                    (int) ((1.0 - recedeFraction) * path.points.size()) - 1);
            if (frontIndex > 0) {
                renderPath(path, 0, frontIndex);
            }
        }

        if (elapsed >= spreadDuration) {
            bPlayer.addCooldown(this);
            remove();
        }
    }

    private void renderPath(RootPath path, int startIdx, int endIdx) {
        World world = player.getWorld();
        List<double[]> pts = path.points;
        for (int i = startIdx; i <= endIdx && i < pts.size(); i++) {
            double[] pt = pts.get(i);
            Particle.DustOptions dust = (i % 3 == 0) ? ROOT_DUST_DARK : ROOT_DUST;
            world.spawnParticle(Particle.DUST, pt[0], pt[1], pt[2], 1, 0, 0, 0, 0, dust);
            if (i >= endIdx - 1) {
                world.spawnParticle(Particle.DUST,
                        pt[0] + (random.nextDouble() - 0.5) * 0.12,
                        pt[1] + 0.05 + random.nextDouble() * 0.14,
                        pt[2] + (random.nextDouble() - 0.5) * 0.12,
                        1, 0, 0, 0, 0, ROOT_DUST);
            }
        }
    }

    private void checkPathHit(RootPath path, int frontIndex) {
        if (path.target == null || path.targetSnared) {
            return;
        }
        if (!path.target.isValid() || path.target.isDead()) {
            path.targetSnared = true;
            return;
        }
        if (frontIndex >= path.targetReachIndex) {
            snareEntity(path.target);
            path.targetSnared = true;
        }
    }

    private void snareEntity(LivingEntity entity) {
        if (snaredEntities.contains(entity.getUniqueId())) {
            return;
        }
        UUID id = entity.getUniqueId();
        snaredEntities.add(id);
        snaredEntityRefs.put(id, entity);
        entity.damage(damage, player);
        if (entity instanceof Player snaredPlayer) {
            snaredPlayers.add(snaredPlayer.getUniqueId());
        }
        entity.getWorld().spawnParticle(Particle.DUST,
                entity.getLocation().add(0, 0.1, 0), 20, 0.5, 0.15, 0.5, 0, ROOT_DUST);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.45f);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_GRASS_STEP, 0.6f, 0.4f);
    }

    private void tickSnaredMobVelocity() {
        for (LivingEntity entity : snaredEntityRefs.values()) {
            if (entity.isValid() && !(entity instanceof Player)) {
                double vy = entity.getVelocity().getY();
                entity.setVelocity(new Vector(0, Math.min(0, vy), 0));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 254, false, false));
            }
        }
    }

    private void buildRootPaths() {
        List<Location> treeOrigins = findTreeOrigins();
        List<LivingEntity> targets = findGroundTargets();

        int targetCount = Math.min(targets.size(), MAX_TARGET_PATHS);
        for (int i = 0; i < targetCount; i++) {
            LivingEntity target = targets.get(i);
            Location origin = nearestOrigin(treeOrigins, target.getLocation());
            rootPaths.add(buildPath(origin, target.getLocation(), target));
        }

        for (int i = 0; i < DECORATIVE_PATHS; i++) {
            Location origin = treeOrigins.get(i % treeOrigins.size());
            double angle = (2.0 * Math.PI * i / DECORATIVE_PATHS) + (random.nextDouble() - 0.5) * 0.7;
            double dist = range * (0.65 + random.nextDouble() * 0.35);
            Location dest = origin.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            rootPaths.add(buildPath(origin, dest, null));
        }
    }

    private RootPath buildPath(Location origin, Location destination, LivingEntity target) {
        double dx = destination.getX() - origin.getX();
        double dz = destination.getZ() - origin.getZ();
        double targetDistXZ = Math.sqrt(dx * dx + dz * dz);
        double totalDist = Math.min(targetDistXZ, range);
        if (totalDist < 0.5) {
            totalDist = 0.5;
        }

        Vector dir = totalDist > 0.01
                ? new Vector(dx, 0, dz).normalize()
                : new Vector(1, 0, 0);
        Vector perp = new Vector(-dir.getZ(), 0, dir.getX());

        World world = origin.getWorld();
        int segments = (int) (totalDist / PATH_STEP) + 2;
        List<double[]> points = new ArrayList<>(segments);
        double lateralOffset = 0;
        double lateralVel = 0;
        int targetReachIndex = -1;

        for (int i = 0; i < segments; i++) {
            double d = i * PATH_STEP;
            if (d > totalDist) {
                break;
            }

            lateralVel += (random.nextDouble() - 0.5) * 0.65;
            lateralVel = Math.max(-0.38, Math.min(0.38, lateralVel));
            lateralVel *= 0.72;
            lateralOffset += lateralVel;
            lateralOffset = Math.max(-MAX_JITTER, Math.min(MAX_JITTER, lateralOffset));

            double px = origin.getX() + dir.getX() * d + perp.getX() * lateralOffset;
            double pz = origin.getZ() + dir.getZ() * d + perp.getZ() * lateralOffset;

            Location ground = findGroundLevel(world, px, origin.getY() + 3, pz);
            double groundY = ground != null ? ground.getY() : origin.getY();
            points.add(new double[]{px, groundY + 0.1, pz});

            if (target != null && targetReachIndex == -1 && d >= targetDistXZ - PATH_STEP) {
                targetReachIndex = i;
            }
        }

        if (targetReachIndex == -1) {
            targetReachIndex = Math.max(0, points.size() - 1);
        }
        return new RootPath(points, target, targetReachIndex);
    }

    private List<Location> findTreeOrigins() {
        Location playerLoc = player.getLocation();
        int r = (int) Math.ceil(range);
        double rangeSq = range * range;
        Map<Long, Location> lowestLogInPillar = new HashMap<>();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > rangeSq) {
                    continue;
                }
                // Scan the full vertical span for logs in this XZ column
                for (int y = -r; y <= r; y++) {
                    Block block = playerLoc.getBlock().getRelative(x, y, z);
                    if (!isUnstrippedLog(block.getType())) {
                        continue;
                    }
                    long key = ((long) (playerLoc.getBlockX() + x)) << 32
                             | ((playerLoc.getBlockZ() + z) & 0xFFFFFFFFL);
                    Location logLoc = block.getLocation().clone().add(0.5, 0, 0.5);
                    if (!lowestLogInPillar.containsKey(key)
                            || logLoc.getY() < lowestLogInPillar.get(key).getY()) {
                        lowestLogInPillar.put(key, logLoc);
                    }
                }
            }
        }

        if (lowestLogInPillar.isEmpty()) {
            return List.of(player.getLocation());
        }
        return new ArrayList<>(lowestLogInPillar.values());
    }

    private List<LivingEntity> findGroundTargets() {
        List<LivingEntity> result = new ArrayList<>();
        Location center = player.getLocation();
        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            if (isNearGround(living)) {
                result.add(living);
            }
        }
        return result;
    }

    private static boolean isNearGround(LivingEntity entity) {
        Location loc = entity.getLocation();
        for (int dy = 0; dy >= -2; dy--) {
            if (entity.getWorld()
                    .getBlockAt(loc.getBlockX(), loc.getBlockY() + dy, loc.getBlockZ())
                    .getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static Location nearestOrigin(List<Location> origins, Location target) {
        Location nearest = origins.get(0);
        double nearestDist = nearest.distanceSquared(target);
        for (Location origin : origins) {
            double dist = origin.distanceSquared(target);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = origin;
            }
        }
        return nearest;
    }

    private static Location findGroundLevel(World world, double px, double startY, double pz) {
        int bx = (int) Math.floor(px);
        int bz = (int) Math.floor(pz);
        for (int y = (int) startY; y >= (int) startY - 15; y--) {
            Material mat = world.getBlockAt(bx, y, bz).getType();
            if (mat.isSolid() && !isUnstrippedLog(mat) && !mat.name().endsWith("_LEAVES")) {
                return new Location(world, px, y + 1.0, pz);
            }
        }
        return null;
    }

    private void spawnChargeParticles(double chargeFraction) {
        Location base = player.getLocation().add(0, 0.1, 0);
        World world = player.getWorld();
        int pointCount = (int) (8 * chargeFraction) + 4;
        double radius = 1.5 * (1.0 - chargeFraction * 0.8);
        for (int i = 0; i < pointCount; i++) {
            double angle = 2.0 * Math.PI * i / pointCount;
            world.spawnParticle(Particle.DUST,
                    base.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius),
                    1, 0, 0, 0, 0, ROOT_DUST);
        }
    }

    private boolean hasForestNearby() {
        Location playerLoc = player.getLocation();
        int r = (int) Math.ceil(range);
        double rangeSq = range * range;
        Set<Long> pillarKeys = new HashSet<>();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > rangeSq) {
                        continue;
                    }
                    if (!isUnstrippedLog(playerLoc.getBlock().getRelative(x, y, z).getType())) {
                        continue;
                    }
                    long key = ((long) (playerLoc.getBlockX() + x)) << 32
                             | ((playerLoc.getBlockZ() + z) & 0xFFFFFFFFL);
                    pillarKeys.add(key);
                    if (pillarKeys.size() >= MIN_WOOD_PILLARS) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isUnstrippedLog(Material mat) {
        return switch (mat) {
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG,
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG,
                 OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD,
                 ACACIA_WOOD, DARK_OAK_WOOD, MANGROVE_WOOD, CHERRY_WOOD -> true;
            default -> false;
        };
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static RootSnare getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public static boolean isSnaredPlayer(UUID uuid) {
        return snaredPlayers.contains(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public void remove() {
        snaredPlayers.removeAll(snaredEntities);
        for (LivingEntity entity : snaredEntityRefs.values()) {
            if (entity.isValid() && !(entity instanceof Player)) {
                entity.removePotionEffect(PotionEffectType.SLOWNESS);
            }
        }
        snaredEntities.clear();
        snaredEntityRefs.clear();
        activeInstances.remove(player.getUniqueId());
        super.remove();
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
    public String getName() {
        return "RootSnare";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
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
        return "Draw power from the roots of nearby trees, and snare any nearby enemies.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (charge when nearby trees) > Sneak (release)");
    }
}
