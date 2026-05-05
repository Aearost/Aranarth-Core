package com.aearost.aranarthcore.abilities.waterbending;

import com.aearost.aranarthcore.utils.AranarthUtils;
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

public class ToxicSpores extends PlantAbility implements AddonAbility {

    public enum Phase {READY, CHANNELING, DISPERSING}

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("SporeSpeed")
    private double sporeSpeed;
    @Attribute("MaxChannelingDuration")
    private long maxChannelingDuration;
    @Attribute("PuffLifespan")
    private long puffLifespan;
    @Attribute("PuffRadius")
    private double puffRadius;
    @Attribute("SourceCheckRadius")
    private double sourceCheckRadius;

    private Phase phase;
    private Block sourceBlock;
    private long readyStartTime;
    private long channelingStartTime;
    private long lastTravelerLaunchTime;

    private static final long CHARGE_DURATION_MS = 500;
    private static final long TRAVELER_LAUNCH_INTERVAL_MS = 100;
    private static final long EFFECT_INTERVAL_MS = 500;
    private static final int EFFECT_DURATION_TICKS = 80; // 4 seconds
    private static final int POISON_AMPLIFIER = 4; // Poison V

    private static final Particle.DustOptions SPORE_DUST =
            new Particle.DustOptions(Color.fromRGB(0, 90, 10), 0.6f);
    private static final Particle.DustOptions SPORE_DUST_PINK =
            new Particle.DustOptions(Color.fromRGB(210, 80, 140), 0.5f);

    private final List<SporeTraveler> travelers = new ArrayList<>();
    private final List<SporePuff> puffs = new ArrayList<>();

    private final Map<UUID, Long> entityEntryTime = new HashMap<>();
    private final Map<UUID, Long> entityLastEffectTime = new HashMap<>();

    private static final Map<UUID, ToxicSpores> activeInstances = new HashMap<>();
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Inner class: SporeTraveler
    // A spore blob in flight between two points.
    // -------------------------------------------------------------------------

    private static class SporeTraveler {
        Location position;
        final Location target;
        final Vector direction;

        SporeTraveler(Location start, Location target) {
            this.position = start.clone();
            this.target = target.clone();
            Vector vec = target.toVector().subtract(start.toVector());
            this.direction = vec.lengthSquared() > 0 ? vec.normalize() : new Vector(0, 1, 0);
        }

        boolean hasArrived() {
            return position.distanceSquared(target) < 0.25;
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: SporePuff
    // A settled spore cloud at a fixed location that lingers for its lifespan.
    // -------------------------------------------------------------------------

    private static class SporePuff {
        final Location center;
        final long createdAt;
        final long lifespan;

        SporePuff(Location center, long lifespan) {
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

    public ToxicSpores(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 12000;
        range = 10.0;
        sporeSpeed = 35.0;
        maxChannelingDuration = 2000;
        puffLifespan = 7000;
        puffRadius = 1.5;
        sourceCheckRadius = 6.0;

        sourceBlock = findNearestFlower();
        if (sourceBlock == null) {
            return;
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
        if (phase != Phase.READY) {
            return;
        }
        if (System.currentTimeMillis() - readyStartTime < CHARGE_DURATION_MS) {
            return;
        }
        phase = Phase.CHANNELING;
        channelingStartTime = System.currentTimeMillis();
        lastTravelerLaunchTime = 0;
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.4f);
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
        }
        // If already DISPERSING, let it finish naturally
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
        // Cancel if the locked-in flower has since been broken
        if (!AranarthUtils.isFlower(sourceBlock.getType())) {
            remove();
            return;
        }
        double chargeFraction = Math.min(1.0,
                (double) (System.currentTimeMillis() - readyStartTime) / CHARGE_DURATION_MS);
        drawSourceLine(chargeFraction);
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

        if (now - lastTravelerLaunchTime >= TRAVELER_LAUNCH_INTERVAL_MS) {
            launchTraveler();
            lastTravelerLaunchTime = now;
        }

        drawSourceLine(1.0);
        tickTravelers();
        tickPuffs();
        tickEffects();
    }

    private void progressDispersing() {
        puffs.removeIf(SporePuff::isExpired);
        if (puffs.isEmpty()) {
            remove();
            return;
        }
        for (SporePuff puff : puffs) {
            spawnPuffParticles(puff);
        }
        tickEffects();
    }

    // -------------------------------------------------------------------------
    // Source line (source block to the left hand, drawn every tick)
    // -------------------------------------------------------------------------

    private void drawSourceLine(double fraction) {
        Location sourceTop = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
        Location leftHand = getLeftHandLocation();
        World world = player.getWorld();
        Vector toHand = leftHand.toVector().subtract(sourceTop.toVector());
        double totalLength = toHand.length();
        if (totalLength < 0.1) {
            return;
        }
        Vector dir = toHand.clone().normalize();
        double drawLength = totalLength * fraction;

        double sx = sourceTop.getX();
        double sy = sourceTop.getY();
        double sz = sourceTop.getZ();
        double step = 0.3;
        for (double d = 0; d < drawLength; d += step) {
            world.spawnParticle(Particle.DUST,
                    sx + dir.getX() * d,
                    sy + dir.getY() * d,
                    sz + dir.getZ() * d,
                    1, 0, 0, 0, 0,
                    SPORE_DUST);
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing spore traveler logic (right hand to target, settles into puffs)
    // -------------------------------------------------------------------------

    private void launchTraveler() {
        Location handLoc = getRightHandLocation();
        Location target = getTargetLocation();
        if (handLoc.distanceSquared(target) < 0.25) {
            return;
        }
        travelers.add(new SporeTraveler(handLoc, target));
    }

    private void tickTravelers() {
        double distPerTick = sporeSpeed / 20.0;

        travelers.removeIf(traveler -> {
            double distToTarget = traveler.position.distance(traveler.target);
            double step = Math.min(distPerTick, distToTarget);
            traveler.position.add(traveler.direction.clone().multiply(step));

            traveler.position.getWorld().spawnParticle(
                    Particle.DUST, traveler.position.clone(),
                    2, 0.04, 0.04, 0.04, 0,
                    SPORE_DUST);

            if (traveler.hasArrived()) {
                puffs.add(new SporePuff(traveler.target, puffLifespan));
                traveler.target.getWorld().spawnParticle(
                        Particle.DUST, traveler.target,
                        5, 0.25, 0.25, 0.25, 0,
                        SPORE_DUST);
                return true;
            }
            return false;
        });
    }

    // -------------------------------------------------------------------------
    // Spore puff rendering
    // -------------------------------------------------------------------------

    private void tickPuffs() {
        puffs.removeIf(SporePuff::isExpired);
        for (SporePuff puff : puffs) {
            spawnPuffParticles(puff);
        }
    }

    private void spawnPuffParticles(SporePuff puff) {
        double radius = puffRadius * puff.getExpansionFactor();
        World world = puff.center.getWorld();

        // Fade particle density over the last 1.5 seconds of the puff's life
        long timeLeft = puff.lifespan - puff.getAge();
        double fadeFactor = Math.min(1.0, timeLeft / 1500.0);
        int count = (int) Math.max(1, Math.round(5 * fadeFactor));

        for (int i = 0; i < count; i++) {
            double theta = Math.acos(1.0 - 2.0 * random.nextDouble());
            double phi = 2.0 * Math.PI * random.nextDouble();
            double r = radius * Math.cbrt(random.nextDouble());
            double dx = r * Math.sin(theta) * Math.cos(phi);
            double dy = r * Math.cos(theta);
            double dz = r * Math.sin(theta) * Math.sin(phi);
            Location loc = puff.center.clone().add(dx, dy, dz);
            if (!loc.getBlock().getType().isSolid()) {
                Particle.DustOptions dust = random.nextInt(12) == 0 ? SPORE_DUST_PINK : SPORE_DUST;
                world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Effect application
    // -------------------------------------------------------------------------

    /**
     * Tracks targets inside any active puff and applies Nausea and Poison V every 0.5s they remain in the spores.
     */
    private void tickEffects() {
        if (puffs.isEmpty()) {
            entityEntryTime.clear();
            entityLastEffectTime.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> inSpores = new HashSet<>();

        for (SporePuff puff : puffs) {
            double radius = puffRadius * puff.getExpansionFactor();
            double radiusSq = radius * radius;

            for (Entity entity : puff.center.getWorld().getNearbyEntities(puff.center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                // Player using the ability is immune
                if (entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (entity.getLocation().distanceSquared(puff.center) > radiusSq) {
                    continue;
                }

                UUID id = entity.getUniqueId();
                inSpores.add(id);

                boolean firstContact = !entityEntryTime.containsKey(id);
                entityEntryTime.putIfAbsent(id, now);
                entityLastEffectTime.putIfAbsent(id, now);

                long lastEffect = entityLastEffectTime.get(id);
                if (!firstContact && now - lastEffect >= EFFECT_INTERVAL_MS) {
                    entityLastEffectTime.put(id, now);

                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.NAUSEA, EFFECT_DURATION_TICKS, 0, false, true), true);
                    living.addPotionEffect(
                            new PotionEffect(PotionEffectType.POISON, EFFECT_DURATION_TICKS, POISON_AMPLIFIER, false, true), true);
                }
            }
        }

        entityEntryTime.keySet().retainAll(inSpores);
        entityLastEffectTime.keySet().retainAll(inSpores);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Location getRightHandLocation() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = Math.abs(forward.dot(up)) > 0.99
                ? forward.crossProduct(new Vector(1, 0, 0)).normalize()
                : forward.crossProduct(up).normalize();
        return eye.clone().add(right.multiply(0.5)).add(0, -0.4, 0);
    }

    private Location getLeftHandLocation() {
        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = Math.abs(forward.dot(up)) > 0.99
                ? forward.crossProduct(new Vector(1, 0, 0)).normalize()
                : forward.crossProduct(up).normalize();
        return eye.clone().add(right.multiply(-0.5)).add(0, -0.4, 0);
    }

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
     * Finds the nearest flower block within sourceCheckRadius and returns it, or null if none exists.
     */
    private Block findNearestFlower() {
        Location playerLoc = player.getLocation();
        int r = (int) Math.ceil(sourceCheckRadius);
        double radiusSq = sourceCheckRadius * sourceCheckRadius;
        Block nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > radiusSq) {
                        continue;
                    }
                    Block candidate = playerLoc.getBlock().getRelative(x, y, z);
                    if (!AranarthUtils.isFlower(candidate.getType())) {
                        continue;
                    }
                    double distSq = playerLoc.distanceSquared(candidate.getLocation().add(0.5, 0.5, 0.5));
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = candidate;
                    }
                }
            }
        }
        return nearest;
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static ToxicSpores getActiveInstance(UUID uuid) {
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
        return "ToxicSpores";
    }

    @Override
    public Location getLocation() {
        if (!puffs.isEmpty()) {
            return puffs.get(0).center;
        }
        return player.getLocation();
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        if (travelers != null) {
            travelers.clear();
        }
        if (puffs != null) {
            puffs.clear();
        }
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
        return "Draw toxic spores from a nearby flower and release them toward your desired location, " +
                "creating a lingering cloud that nauseates and critically poisons any entity caught within.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (flower source) > Left-Click");
    }

    public Phase getPhase() {
        return phase;
    }
}
