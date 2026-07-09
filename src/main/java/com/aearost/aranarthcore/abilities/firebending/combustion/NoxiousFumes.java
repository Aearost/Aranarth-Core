package com.aearost.aranarthcore.abilities.firebending.combustion;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class NoxiousFumes extends CombustionAbility implements AddonAbility {

    public enum Phase { READY, CHANNELING, DISPERSING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("SmokeSpeed")
    private double smokeSpeed;
    @Attribute("MaxChannelingDuration")
    private long maxChannelingDuration;
    @Attribute("PuffLifespan")
    private long puffLifespan;
    @Attribute("PuffRadius")
    private double puffRadius;
    @Attribute("SourceCheckRadius")
    private double sourceCheckRadius;

    private Phase phase;
    private Location sourceLocation; // Self-fire source point only (used when selfOnFire)
    private final List<Location> sourceLocations = new ArrayList<>();      // Top-center of each block source
    private final List<Location> sourceBlockLocations = new ArrayList<>(); // Actual block positions
    private final List<Material> sourceBlockTypes = new ArrayList<>();     // Material at each source
    private boolean selfOnFire; // True when the player's own fire is being used as the source
    private boolean selfFireExtinguished; // True once we've consumed the player's fire at charge time
    private int activeSourceCount = 1; // Number of sources captured at channeling start (drives launch rate)
    private long readyStartTime;
    private long channelingStartTime;
    private long lastTravelerLaunchTime;

    private static final long CHARGE_DURATION_MS = 1500;

    private final List<SmokeTraveler> travelers = new ArrayList<>();
    private final List<SmokePuff> puffs = new ArrayList<>();

    private final Map<UUID, Long> entityEntryTime = new HashMap<>();
    private final Map<UUID, Long> entityLastEffectTime = new HashMap<>();
    private final Map<UUID, Integer> entityHitCounts = new HashMap<>();

    // Launch a new smoke traveler every ~83ms during channeling (3x the original 250ms rate)
    private static final long TRAVELER_LAUNCH_INTERVAL_MS = 83;
    // Apply escalating effects every 500ms while an entity is in the fumes
    private static final long EFFECT_INTERVAL_MS = 500;
    private static final int BASE_EFFECT_DURATION_TICKS = 60; // +20 ticks per 0.5s interval in fumes

    private static final Map<UUID, NoxiousFumes> activeInstances  = new HashMap<>();
    // Tracks when a player's NoxiousFumes reached full charge — consumed by JetFumes activation.
    private static final Map<UUID, Long>         chargeTimestamps = new HashMap<>();
    private static final long                    CHARGE_VALID_MS  = 15000L;
    private final Random random = new Random();

    public static void recordChargeComplete(UUID uuid) {
        chargeTimestamps.putIfAbsent(uuid, System.currentTimeMillis());
    }

    public static boolean wasRecentlyCharged(UUID uuid) {
        Long ts = chargeTimestamps.get(uuid);
        if (ts == null) return false;
        if (System.currentTimeMillis() - ts <= CHARGE_VALID_MS) return true;
        chargeTimestamps.remove(uuid);
        return false;
    }

    public static void clearChargeTimestamp(UUID uuid) {
        chargeTimestamps.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Inner class: SmokeTraveler
    // A smoke blob in flight between two points.
    // -------------------------------------------------------------------------

    private static class SmokeTraveler {
        Location position;
        final Location target;
        final Vector direction;

        SmokeTraveler(Location start, Location target) {
            this.position = start.clone();
            this.target = target.clone();
            Vector vec = target.toVector().subtract(start.toVector());
            this.direction = vec.lengthSquared() > 0 ? vec.normalize() : new Vector(0, 1, 0);
        }

        boolean hasArrived() {
            return position.distanceSquared(target) < 0.25; // Within 0.5 blocks
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: SmokePuff
    // A settled smoke cloud at a fixed location that lingers for its lifespan.
    // -------------------------------------------------------------------------

    private static class SmokePuff {
        final Location center;
        final long createdAt;
        final long lifespan;

        SmokePuff(Location center, long lifespan) {
            this.center = center.clone();
            this.createdAt = System.currentTimeMillis();
            this.lifespan = lifespan;
        }

        long getAge() {
            return System.currentTimeMillis() - createdAt;
        }

        boolean isExpired() {
            return getAge() >= lifespan;
        }

        // Puff expands from 50% to 100% of full radius over the first second
        double getExpansionFactor() {
            return 0.5 + 0.5 * Math.min(1.0, getAge() / 1000.0);
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public NoxiousFumes(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 12000;
        range = 10.0;
        smokeSpeed = 12.0;
        maxChannelingDuration = 4000;
        puffLifespan = 7000;
        puffRadius = 3.0;
        sourceCheckRadius = 6.0;

        if (player.getFireTicks() > 0) {
            selfOnFire = true;
            sourceLocation = getSelfFireSourceLocation();
        } else {
            findNearestSources(isDay() ? 5 : 1);
            if (sourceLocations.isEmpty()) {
                return;
            }
        }

        phase = Phase.READY;
        readyStartTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    // -------------------------------------------------------------------------
    // Public control methods (called by the listener)
    // -------------------------------------------------------------------------

    /**
     * Transitions from READY to CHANNELING when the player left-clicks.
     */
    public void startChanneling() {
        if (phase != Phase.READY) return;
        // Require the 2-second charge to be complete before channeling can begin
        if (System.currentTimeMillis() - readyStartTime < CHARGE_DURATION_MS) return;
        phase = Phase.CHANNELING;
        channelingStartTime = System.currentTimeMillis();
        lastTravelerLaunchTime = 0;
        activeSourceCount = selfOnFire ? 1 : Math.max(1, sourceLocations.size());
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 0.6f);
    }

    public void endChanneling() {
        if (phase == Phase.READY) {
            remove();
            return;
        }
        if (phase == Phase.CHANNELING) {
            travelers.clear();
            phase = Phase.DISPERSING;
            bPlayer.addCooldown(this);
            clearChargeTimestamp(player.getUniqueId());
            consumeSource();
        }
        // If already DISPERSING, do nothing — let it finish naturally.
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            travelers.clear();
            puffs.clear();
            remove();
            return;
        }

        switch (phase) {
            case READY -> progressReady();
            case CHANNELING -> progressChanneling();
            case DISPERSING -> progressDispersing();
        }
    }

    private void progressReady() {
        if (!player.isSneaking()) {
            remove();
            return;
        }
        // Re-derive source each tick so it stays valid
        if (selfOnFire) {
            if (!selfFireExtinguished && player.getFireTicks() <= 0) {
                // Player's fire was extinguished externally before charge — cancel
                remove();
                return;
            }
            sourceLocation = getSelfFireSourceLocation();
        } else {
            findNearestSources(isDay() ? 5 : 1);
            if (sourceLocations.isEmpty()) {
                remove();
                return;
            }
        }
        // Gradually extend the line from source to hand over the charge duration
        double chargeFraction = Math.min(1.0,
                (double)(System.currentTimeMillis() - readyStartTime) / CHARGE_DURATION_MS);
        drawSourceLine(chargeFraction);
        if (isCharged()) {
            if (selfOnFire && !selfFireExtinguished) {
                selfFireExtinguished = true;
                player.setFireTicks(0);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            }
            recordChargeComplete(player.getUniqueId());
        }
    }

    private void progressChanneling() {
        if (!player.isSneaking()) {
            endChanneling();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - channelingStartTime >= maxChannelingDuration) {
            endChanneling();
            return;
        }

        long effectiveInterval = Math.max(1L, TRAVELER_LAUNCH_INTERVAL_MS / activeSourceCount);
        if (now - lastTravelerLaunchTime >= effectiveInterval) {
            launchTraveler();
            lastTravelerLaunchTime = now;
        }

        drawSourceLine(1.0);
        tickTravelers();
        tickPuffs();
        tickEffects();
    }

    private void progressDispersing() {
        // Remove expired puffs
        puffs.removeIf(SmokePuff::isExpired);
        if (puffs.isEmpty()) {
            remove();
            return;
        }
        for (SmokePuff puff : puffs) {
            spawnPuffParticles(puff);
        }
        tickEffects();
    }

    // -------------------------------------------------------------------------
    // Source line (source block → left hand, drawn every tick)
    // -------------------------------------------------------------------------

    /**
     * Draws a line of smoke particles from each source to the player's left hand.
     * @param fraction 0.0 = nothing drawn, 1.0 = full line drawn (used for gradual charge).
     */
    private void drawSourceLine(double fraction) {
        Location leftHand = getLeftHandLocation();
        World world = player.getWorld();

        List<Location> origins = new ArrayList<>();
        if (selfOnFire && sourceLocation != null) {
            origins.add(sourceLocation);
        } else {
            origins.addAll(sourceLocations);
        }

        double step = 0.3;
        for (Location origin : origins) {
            Vector toHand = leftHand.toVector().subtract(origin.toVector());
            double totalLength = toHand.length();
            if (totalLength < 0.1) continue;
            Vector dir = toHand.clone().normalize();
            double drawLength = totalLength * fraction;

            double sx = origin.getX();
            double sy = origin.getY();
            double sz = origin.getZ();
            for (double d = 0; d < drawLength; d += step) {
                world.spawnParticle(Particle.SMOKE,
                        sx + dir.getX() * d,
                        sy + dir.getY() * d,
                        sz + dir.getZ() * d,
                        1, 0, 0, 0, 0);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing smoke traveler logic (right hand to target, settles into puffs)
    // -------------------------------------------------------------------------

    private void launchTraveler() {
        Location handLoc = getRightHandLocation();
        Location target = getTargetLocation();
        if (handLoc.distanceSquared(target) < 0.25) return;
        travelers.add(new SmokeTraveler(handLoc, target));
    }

    private void tickTravelers() {
        double distPerTick = smokeSpeed / 20.0;

        travelers.removeIf(traveler -> {
            double distToTarget = traveler.position.distance(traveler.target);
            double step = Math.min(distPerTick, distToTarget);
            traveler.position.add(traveler.direction.clone().multiply(step));

            // Sparse trail of smoke along the travel path
            traveler.position.getWorld().spawnParticle(
                    Particle.SMOKE, traveler.position.clone(),
                    1, 0.06, 0.06, 0.06, 0.005);

            if (traveler.hasArrived()) {
                // Settle a new puff at the destination
                puffs.add(new SmokePuff(traveler.target, puffLifespan));
                traveler.target.getWorld().spawnParticle(
                        Particle.SMOKE, traveler.target,
                        3, 0.25, 0.25, 0.25, 0.01);
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Smoke puff rendering
    // -------------------------------------------------------------------------

    private void tickPuffs() {
        puffs.removeIf(SmokePuff::isExpired);
        for (SmokePuff puff : puffs) {
            spawnPuffParticles(puff);
        }
    }

    private void spawnPuffParticles(SmokePuff puff) {
        double radius = puffRadius * puff.getExpansionFactor();
        World world = puff.center.getWorld();

        // Fade particle density over the last 1.5 seconds of the puff's life
        long timeLeft = puff.lifespan - puff.getAge();
        double fadeFactor = Math.min(1.0, timeLeft / 1500.0);
        int count = (int) Math.max(1, Math.round(5 * fadeFactor));

        for (int i = 0; i < count; i++) {
            // Volume-uniform random point inside the sphere
            double theta = Math.acos(1.0 - 2.0 * random.nextDouble());
            double phi = 2.0 * Math.PI * random.nextDouble();
            double r = radius * Math.cbrt(random.nextDouble());
            double dx = r * Math.sin(theta) * Math.cos(phi);
            double dy = r * Math.cos(theta);
            double dz = r * Math.sin(theta) * Math.sin(phi);
            Location loc = puff.center.clone().add(dx, dy, dz);
            if (!loc.getBlock().getType().isSolid()) {
                Particle particle = random.nextInt(5) == 0 ? Particle.LARGE_SMOKE : Particle.SMOKE;
                world.spawnParticle(particle, loc, 1, 0, 0, 0, 0.004);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Effect application
    // -------------------------------------------------------------------------

    /**
     * Tracks every living entity currently inside any active puff and applies
     * Blindness, Nausea, and escalating Poison every 0.5s they remain in the fumes.
     */
    private void tickEffects() {
        if (puffs.isEmpty()) {
            entityEntryTime.clear();
            entityLastEffectTime.clear();
            entityHitCounts.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> inFumes = new HashSet<>();

        for (SmokePuff puff : puffs) {
            double radius = puffRadius * puff.getExpansionFactor();
            double radiusSq = radius * radius;

            for (Entity entity : puff.center.getWorld().getNearbyEntities(puff.center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity.getLocation().distanceSquared(puff.center) > radiusSq) continue;

                if (living instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                    continue;
                }

                UUID id = entity.getUniqueId();
                inFumes.add(id);

                boolean firstContact = !entityEntryTime.containsKey(id);
                entityEntryTime.putIfAbsent(id, now);
                entityLastEffectTime.putIfAbsent(id, now);

                long lastEffect = entityLastEffectTime.get(id);
                // Apply effects on first contact (after 0.5s) and every 0.5s thereafter
                if (!firstContact && now - lastEffect >= EFFECT_INTERVAL_MS) {
                    int hits = entityHitCounts.getOrDefault(id, 0) + 1;
                    entityHitCounts.put(id, hits);
                    entityLastEffectTime.put(id, now);

                    // Duration grows by 1s (20 ticks) for every 0.5s interval in fumes
                    int durationTicks = BASE_EFFECT_DURATION_TICKS + hits * 20;
                    // Poison amplifier: Poison I on first interval (amp 0), II on second, etc.
                    int poisonAmp = hits - 1;

                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 0, false, true), true);
                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 0, false, true), true);
                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.POISON, durationTicks, poisonAmp, false, true), true);
                }
            }
        }

        // Drop tracking for entities that have left all puffs
        entityEntryTime.keySet().retainAll(inFumes);
        entityLastEffectTime.keySet().retainAll(inFumes);
        entityHitCounts.keySet().retainAll(inFumes);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Consumes the source when the ability goes on cooldown.
     * If the player was on fire, extinguishes them.
     * Fire/soul fire blocks are extinguished (set to AIR). Lava is solidified into obsidian.
     */
    private void consumeSource() {
        if (selfOnFire) {
            player.setFireTicks(0);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            if (sourceLocation != null) {
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, sourceLocation,
                        12, 0.3, 0.3, 0.3, 0.02);
            }
            return;
        }
        for (int i = 0; i < sourceBlockLocations.size(); i++) {
            Location blockLoc = sourceBlockLocations.get(i);
            Material mat = sourceBlockTypes.get(i);
            org.bukkit.block.Block block = blockLoc.getBlock();
            if (block.getType() != mat) continue;

            if (mat == Material.FIRE || mat == Material.SOUL_FIRE) {
                block.setType(Material.AIR);
                block.getWorld().playSound(blockLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            } else if (mat == Material.LAVA) {
                block.setType(Material.OBSIDIAN);
                block.getWorld().playSound(blockLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Returns the approximate position of the player's right hand.
     */
    private Location getRightHandLocation() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = Math.abs(forward.dot(up)) > 0.99
                ? forward.crossProduct(new Vector(1, 0, 0)).normalize()
                : forward.crossProduct(up).normalize();
        return eye.clone().add(right.multiply(0.5)).add(0, -0.4, 0);
    }

    /**
     * Returns the approximate position of the player's left hand.
     * Mirror of getRightHandLocation - negated lateral offset.
     */
    private Location getLeftHandLocation() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = Math.abs(forward.dot(up)) > 0.99
                ? forward.crossProduct(new Vector(1, 0, 0)).normalize()
                : forward.crossProduct(up).normalize();
        // Left = negate the right offset
        return eye.clone().add(right.multiply(-0.5)).add(0, -0.4, 0);
    }

    /**
     * Ray-casts from the player's eye to find the smoke's destination (first solid
     * block surface within range, or the maximum range point).
     */
    private Location getTargetLocation() {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        for (double d = 0.5; d <= range; d += 0.25) {
            Location loc = eye.clone().add(dir.clone().multiply(d));
            if (loc.getBlock().getType().isSolid()) {
                return eye.clone().add(dir.clone().multiply(Math.max(0.25, d - 0.25)));
            }
        }
        return eye.clone().add(dir.clone().multiply(range));
    }

    /**
     * Populates the source lists with up to {@code max} nearest fire/lava blocks, sorted by distance.
     * During the day up to 5 sources are allowed; at night only 1.
     */
    private void findNearestSources(int max) {
        sourceLocations.clear();
        sourceBlockLocations.clear();
        sourceBlockTypes.clear();

        Location playerLoc = player.getLocation();
        int r = (int) Math.ceil(sourceCheckRadius);
        double radiusSq = sourceCheckRadius * sourceCheckRadius;

        record Candidate(double distSq, Location topCenter, Location blockLoc, Material mat) {}
        List<Candidate> candidates = new ArrayList<>();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > radiusSq) continue;
                    org.bukkit.block.Block rel = playerLoc.getBlock().getRelative(x, y, z);
                    Material mat = rel.getType();
                    if (mat != Material.FIRE && mat != Material.SOUL_FIRE && mat != Material.LAVA) continue;
                    Location topCenter = rel.getLocation().add(0.5, 1.0, 0.5);
                    candidates.add(new Candidate(playerLoc.distanceSquared(topCenter), topCenter, rel.getLocation(), mat));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::distSq));
        for (int i = 0; i < Math.min(max, candidates.size()); i++) {
            Candidate c = candidates.get(i);
            sourceLocations.add(c.topCenter());
            sourceBlockLocations.add(c.blockLoc());
            sourceBlockTypes.add(c.mat());
        }
    }

    /** Returns true if it is currently daytime in the player's world. */
    private boolean isDay() {
        long time = player.getWorld().getTime();
        return time < 13000;
    }

    /**
     * Returns the position of the self-fire smoke ball: 1.2 blocks in front of the
     * player at roughly chest height. Tracked every tick so it follows the player's
     * facing direction while charging.
     */
    private Location getSelfFireSourceLocation() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        return eye.clone().add(forward.multiply(1.2)).add(0, -0.3, 0);
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static NoxiousFumes getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    // -------------------------------------------------------------------------
    // PK ability interface
    // -------------------------------------------------------------------------

    @Override
    public void remove() {
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
    public String getName() {
        return "NoxiousFumes";
    }

    @Override
    public Location getLocation() {
        if (!puffs.isEmpty()) return puffs.get(0).center;
        return player.getLocation();
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        if (travelers != null) travelers.clear();
        if (puffs != null) puffs.clear();
        chargeTimestamps.clear();
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
        return "Channel toxic fumes from a fire or lava source, into one hand and out of the other, " +
                "painting dense clouds of smoke that blind, nauseate, and poison any entity caught within them.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak > Left-Click");
    }

    public Phase getPhase() {
        return phase;
    }

    /**
     * Returns true once the ability has been in the READY phase for at least the
     * full charge duration — i.e., the smoke path has fully reached the player's hand.
     */
    public boolean isCharged() {
        return phase == Phase.READY && System.currentTimeMillis() - readyStartTime >= CHARGE_DURATION_MS;
    }
}
