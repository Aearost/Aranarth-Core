package com.aearost.aranarthcore.abilities.firebending.combustion;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CombustionAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.firebending.FireJet;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class JetFumes extends CombustionAbility implements AddonAbility, ComboAbility {

    public enum Phase {FLYING, DISPERSING}

    private static final long TRAIL_PUFF_LIFESPAN_MS = 5500L;
    private static final long TRAIL_PUFF_INTERVAL_MS = 100L;
    private static final long SOUND_INTERVAL_MS = 50L;
    private static final long EFFECT_INTERVAL_MS = 500L;
    private static final int BASE_EFFECT_DURATION_TICKS = 20;

    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute("FlightSpeed")
    private double flightSpeed;
    @Attribute("PuffRadius")
    private double puffRadius;

    private Phase phase;
    private long flightStartTime;
    private long lastTrailPuffTime;
    private long lastSoundTime;

    private final List<TrailPuff> puffs = new ArrayList<>();
    private final Map<UUID, Long> entityEntryTime = new HashMap<>();
    private final Map<UUID, Long> entityLastEffectTime = new HashMap<>();
    private final Map<UUID, Integer> entityHitCounts = new HashMap<>();

    private static final Map<UUID, JetFumes> ACTIVE_INSTANCES = new HashMap<>();
    private static final long COOLDOWN_MS = 12000L;
    private final Random random = new Random();

    private boolean started = false;
    private long flightEndTime = 0L;

    private static class TrailPuff {
        final Location center;
        final long createdAt = System.currentTimeMillis();
        final long lifespan;

        TrailPuff(Location center, long lifespan) {
            this.center = center.clone();
            this.lifespan = lifespan;
        }

        long getAge() {
            return System.currentTimeMillis() - createdAt;
        }

        boolean isExpired() {
            return getAge() >= lifespan;
        }

        /**
         * Expands from 50% to 100% of full radius over the first second.
         */
        double getExpansionFactor() {
            return 0.5 + 0.5 * Math.min(1.0, getAge() / 1000.0);
        }
    }

    public JetFumes(Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }
        if (!NoxiousFumes.wasRecentlyCharged(player.getUniqueId())) {
            return;
        }

        duration = 5000L;
        flightSpeed = 1.6;
        puffRadius = 3.0;

        phase = Phase.FLYING;
        flightStartTime = System.currentTimeMillis();
        lastTrailPuffTime = 0L;
        lastSoundTime = 0L;

        // Must be near a fire or lava source to launch
        if (!isNearFireOrLava(player, 6.0)) {
            return;
        }

        NoxiousFumes.clearChargeTimestamp(player.getUniqueId());

        // Register as active BEFORE removing FireJet so that the PlayerCooldownChangeEvent
        // listener can intercept and cancel the cooldown PK applies inside remove().
        ACTIVE_INSTANCES.put(player.getUniqueId(), this);

        // Remove any active FireJet so it doesn't conflict with JetFumes' own flight.
        FireJet activeFireJet = CoreAbility.getAbility(player, FireJet.class);
        if (activeFireJet != null) {
            activeFireJet.remove();
        }
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 0.8f);

        start();
        started = true;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            puffs.clear();
            remove();
            return;
        }
        switch (phase) {
            case FLYING -> progressFlying();
            case DISPERSING -> progressDispersing();
        }
    }

    private void progressFlying() {
        // Suppress any FireJet that activates during flight.
        // The PlayerCooldownChangeEvent listener blocks any cooldown PK applies via remove().
        FireJet activeFireJet = CoreAbility.getAbility(player, FireJet.class);
        if (activeFireJet != null) {
            activeFireJet.remove();
        }

        long now = System.currentTimeMillis();
        if (now - flightStartTime >= duration) {
            endFlight();
            return;
        }
        tickFlight();
        spawnFlightParticles();
        tickTrail(now);
        tickPuffs();
        tickEffects();
        tickSounds(now);
    }

    private void progressDispersing() {
        puffs.removeIf(TrailPuff::isExpired);
        if (puffs.isEmpty()) {
            remove();
            return;
        }
        for (TrailPuff puff : puffs) {
            spawnPuffParticles(puff);
        }
        tickEffects();
    }

    public void onLeftClick() {
        if (phase == Phase.FLYING && System.currentTimeMillis() - getStartTime() > 250) {
            endFlight();
        }
    }

    private void endFlight() {
        phase = Phase.DISPERSING;
        flightEndTime = System.currentTimeMillis();
        bPlayer.addCooldown("JetFumes", COOLDOWN_MS);
        bPlayer.addCooldown("FireJet", 5000L);
    }

    private void tickFlight() {
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(flightSpeed);
        player.setVelocity(velocity);
        player.setFallDistance(0f);
        player.setFireTicks(0);
    }

    private void spawnFlightParticles() {
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.4, 0.25, 0.02);
    }

    private void tickTrail(long now) {
        if (now - lastTrailPuffTime >= TRAIL_PUFF_INTERVAL_MS) {
            puffs.add(new TrailPuff(player.getLocation().add(0, 1, 0), TRAIL_PUFF_LIFESPAN_MS));
            lastTrailPuffTime = now;
        }
    }

    private void tickPuffs() {
        puffs.removeIf(TrailPuff::isExpired);
        for (TrailPuff puff : puffs) {
            spawnPuffParticles(puff);
        }
    }

    private void spawnPuffParticles(TrailPuff puff) {
        double radius = puffRadius * puff.getExpansionFactor();
        World world = puff.center.getWorld();

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
                Particle particle = random.nextInt(5) == 0 ? Particle.LARGE_SMOKE : Particle.SMOKE;
                world.spawnParticle(particle, loc, 1, 0, 0, 0, 0.004);
            }
        }
    }

    private void tickEffects() {
        if (puffs.isEmpty()) {
            entityEntryTime.clear();
            entityLastEffectTime.clear();
            entityHitCounts.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<UUID> inFumes = new HashSet<>();

        for (TrailPuff puff : puffs) {
            double radius = puffRadius * puff.getExpansionFactor();
            double radiusSq = radius * radius;

            for (Entity entity : puff.center.getWorld().getNearbyEntities(puff.center, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                if (entity.getLocation().distanceSquared(puff.center) > radiusSq) {
                    continue;
                }
                // Caster is immune to a cloud only for its first second
                if (entity.getUniqueId().equals(player.getUniqueId()) && puff.getAge() < 1000L) {
                    continue;
                }

                UUID id = entity.getUniqueId();
                boolean firstContact = !entityEntryTime.containsKey(id);
                inFumes.add(id);
                entityEntryTime.putIfAbsent(id, now);
                entityLastEffectTime.putIfAbsent(id, now);

                if (!firstContact && now - entityLastEffectTime.get(id) >= EFFECT_INTERVAL_MS) {
                    int hits = entityHitCounts.getOrDefault(id, 0) + 1;
                    entityHitCounts.put(id, hits);
                    entityLastEffectTime.put(id, now);

                    int durationTicks = BASE_EFFECT_DURATION_TICKS + hits * 20;
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

        entityEntryTime.keySet().retainAll(inFumes);
        entityLastEffectTime.keySet().retainAll(inFumes);
        entityHitCounts.keySet().retainAll(inFumes);
    }

    private void tickSounds(long now) {
        if (now - lastSoundTime >= SOUND_INTERVAL_MS) {
            lastSoundTime = now;
            Location loc = player.getLocation();
            loc.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 0.5f, 0.5f);
        }
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static JetFumes getActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    private static boolean isNearFireOrLava(Player player, double radius) {
        int r = (int) Math.ceil(radius);
        double radiusSq = radius * radius;
        Location loc = player.getLocation();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > radiusSq) {
                        continue;
                    }
                    Material mat = loc.getBlock().getRelative(x, y, z).getType();
                    if (mat == Material.FIRE || mat == Material.SOUL_FIRE || mat == Material.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void remove() {
        super.remove(); // PK applies getCooldown() here, resetting JetFumes to 12s from now
        CoreAbility fireJetAbility = CoreAbility.getAbility("FireJet");
        long fireJetCooldown = fireJetAbility != null ? fireJetAbility.getCooldown() : 7000L;
        // Remove from active instances BEFORE applying cooldowns so the PlayerCooldownChangeEvent
        // listener allows them through (it only blocks while the player is still in ACTIVE_INSTANCES).
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        if (started) {
            if (flightEndTime == 0) {
                // endFlight() was never called (e.g. player died or disconnected mid-flight)
                bPlayer.addCooldown("FireJet", fireJetCooldown);
                bPlayer.addCooldown("JetFumes", COOLDOWN_MS);
            } else {
                // endFlight() already started both cooldowns, but super.remove() just reset
                // JetFumes to a fresh 12s. Restore the correct remaining time.
                long elapsed = System.currentTimeMillis() - flightEndTime;
                long remaining = COOLDOWN_MS - elapsed;
                if (remaining > 0) {
                    bPlayer.addCooldown("JetFumes", remaining);
                } else {
                    bPlayer.getCooldowns().remove("JetFumes");
                }
            }
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
        return COOLDOWN_MS;
    }

    @Override
    public String getName() {
        return "JetFumes";
    }

    @Override
    public Location getLocation() {
        if (!puffs.isEmpty()) {
            return puffs.get(0).center;
        }
        return player.getLocation();
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new JetFumes(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        return ComboUtil.generateCombinationFromList(this, new ArrayList<>(Arrays.asList(
                "FireJet:SHIFT_DOWN",
                "FireJet:SHIFT_UP",
                "FireJet:SHIFT_DOWN",
                "FireJet:SHIFT_UP",
                "NoxiousFumes:SHIFT_DOWN",
                "FireJet:LEFT_CLICK"
        )));
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        ACTIVE_INSTANCES.clear();
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
        return "Harness the toxic fumes from a fire or lava source to launch yourself through the air, " +
                "leaving poisonous clouds of smoke in your trail.\n" +
                ChatUtils.translateToColor(
                        "&fUsage: FireJet (Tap Shift) > FireJet (Tap Shift) > " +
                                "NoxiousFumes (Hold Shift) > FireJet (Left-Click if NoxiousFumes is charged)");
    }
}
