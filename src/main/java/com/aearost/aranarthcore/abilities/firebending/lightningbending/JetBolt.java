package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.firebending.FireJet;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class JetBolt extends LightningAbility implements AddonAbility, ComboAbility {

    private static final long COOLDOWN_MS = 12000L;
    private static final long STRIKE_INTERVAL_MS = 500L;
    private static final long ELECTROCUTION_DURATION_MS = 1000L;
    private static final long SOUND_INTERVAL_MS = 50L;
    private static final double FLIGHT_SPEED = 2.5;
    private static final int BOLTS_PER_TICK = 15;
    private static final long TRAIL_NODE_INTERVAL_MS = 100L;
    private static final long TRAIL_NODE_LIFESPAN_MS = 1200L;

    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DURATION)
    private long duration;

    private long flightStartTime;
    private long lastSoundTime;
    private long lastTrailNodeTime;
    private long flightEndTime;
    private boolean started;
    private boolean flightEnded;

    private final Map<UUID, Long> entityLastStrikeTime = new HashMap<>();
    private final List<TrailNode> trailNodes = new ArrayList<>();
    private static final Map<UUID, JetBolt> ACTIVE_INSTANCES = new HashMap<>();
    private final Random random = new Random();

    private static class TrailNode {
        final Location center;
        final long createdAt = System.currentTimeMillis();

        TrailNode(Location center) {
            this.center = center.clone();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt >= TRAIL_NODE_LIFESPAN_MS;
        }
    }

    public JetBolt(Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        damage = 4.0;
        cooldown = COOLDOWN_MS;
        range = 10.0;
        duration = 4000L;

        flightStartTime = System.currentTimeMillis();
        lastSoundTime = 0L;
        lastTrailNodeTime = 0L;
        flightEndTime = 0L;
        started = false;
        flightEnded = false;

        // Register before removing FireJet to cancel any FireJet cooldown PK applies inside remove()
        ACTIVE_INSTANCES.put(player.getUniqueId(), this);

        FireJet activeFireJet = CoreAbility.getAbility(player, FireJet.class);
        if (activeFireJet != null) {
            activeFireJet.remove();
        }

        // Remove any Lightning instance that activated during the combo sequence.
        AranarthBendingUtils.suppressComboTrigger(bPlayer, player, "Lightning");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, 0.8f, 0.2f);

        start();
        started = true;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            trailNodes.clear();
            remove();
            return;
        }

        if (flightEnded) {
            trailNodes.removeIf(TrailNode::isExpired);
            if (trailNodes.isEmpty()) {
                remove();
                return;
            }
            for (TrailNode node : trailNodes) {
                spawnTrailStrikes(node);
            }
            return;
        }

        // Suppress any FireJet that activates during flight
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
        spawnBoltTrail();
        tickTrailNodes(now);
        tickStrikeEffects(now);
        tickSounds(now);
    }

    private void endFlight() {
        flightEnded = true;
        flightEndTime = System.currentTimeMillis();
        bPlayer.addCooldown("FireJet", 7000L);
        bPlayer.addCooldown("JetBolt", COOLDOWN_MS);
    }

    private void tickFlight() {
        Vector velocity = player.getEyeLocation().getDirection().normalize().multiply(FLIGHT_SPEED);
        player.setVelocity(velocity);
        player.setFallDistance(0f);
        player.setFireTicks(0);
    }

    /**
     * Spawns jagged electrical bolt particles behind the player each tick, creating the
     * appearance of hundreds of bolts chasing and striking through their wake.
     */
    private void spawnBoltTrail() {
        World world = player.getWorld();
        Location origin = player.getLocation().add(0, 1, 0);
        Vector backward = player.getEyeLocation().getDirection().normalize().multiply(-1);

        Vector right = backward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 0.001) {
            right = new Vector(1, 0, 0);
        } else {
            right.normalize();
        }
        Vector up = right.clone().crossProduct(backward).normalize();

        for (int i = 0; i < BOLTS_PER_TICK; i++) {
            double depth = range * (0.15 + 0.85 * random.nextDouble());
            double lateralRadius = 4.0 * random.nextDouble();
            double lateralAngle = 2.0 * Math.PI * random.nextDouble();

            Location boltOrigin = origin.clone()
                    .add(backward.clone().multiply(depth))
                    .add(right.clone().multiply(lateralRadius * Math.cos(lateralAngle)))
                    .add(up.clone().multiply(lateralRadius * Math.sin(lateralAngle)));

            spawnZigzagBoltTowardPlayer(world, boltOrigin, i);
        }
    }

    /**
     * Records a trail node at the player's current position every trail node interval.
     */
    private void tickTrailNodes(long now) {
        if (now - lastTrailNodeTime >= TRAIL_NODE_INTERVAL_MS) {
            trailNodes.add(new TrailNode(player.getLocation().add(0, 1, 0)));
            lastTrailNodeTime = now;
        }
        trailNodes.removeIf(TrailNode::isExpired);
        for (TrailNode node : trailNodes) {
            spawnTrailStrikes(node);
        }
    }

    /**
     * Spawns 1–2 random short lightning bolt strikes around a trail node location,
     * creating the appearance of static electricity continuing to discharge in the
     * player's wake after they have moved on.
     */
    private void spawnTrailStrikes(TrailNode node) {
        World world = node.center.getWorld();
        int strikes = 1 + random.nextInt(2);
        for (int i = 0; i < strikes; i++) {
            double ox = (random.nextDouble() - 0.5) * 3.0;
            double oy = 0.5 + random.nextDouble() * 1.5;
            double oz = (random.nextDouble() - 0.5) * 3.0;
            Location start = node.center.clone().add(ox, oy, oz);

            Vector direction = new Vector(
                    (random.nextDouble() - 0.5) * 0.4,
                    -1.0,
                    (random.nextDouble() - 0.5) * 0.4
            ).normalize();

            spawnZigzagBoltInDirection(world, start, direction, i);
        }
    }

    /**
     * Draws a short jagged lightning bolt path toward the player from the given origin,
     * using the three lightning dust colors to match the standard lightning visual style.
     *
     * @param world  The world to spawn particles in.
     * @param start  The starting location of the bolt segment.
     * @param index  The bolt index, used to cycle dust colors across concurrent bolts.
     */
    private void spawnZigzagBoltTowardPlayer(World world, Location start, int index) {
        Vector towardPlayer = player.getLocation().add(0, 1, 0).toVector().subtract(start.toVector());
        double dist = towardPlayer.length();
        if (dist < 0.1) {
            return;
        }
        towardPlayer.normalize();

        int segments = 4 + random.nextInt(3);
        double totalLength = Math.min(dist, 2.0 + random.nextDouble() * 3.0);
        double segmentLength = totalLength / segments;
        Location current = start.clone();

        for (int s = 0; s < segments; s++) {
            Vector jitter = new Vector(
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.7
            );
            Location next = current.clone()
                    .add(towardPlayer.clone().multiply(segmentLength))
                    .add(jitter);

            Particle.DustOptions dust = switch ((s + index) % 3) {
                case 0 -> AranarthBendingUtils.LIGHTNING_DUST_BRIGHT;
                case 1 -> AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                default -> AranarthBendingUtils.LIGHTNING_DUST;
            };

            world.spawnParticle(Particle.DUST, next, 1, 0, 0, 0, 0, dust);
            if (random.nextDouble() < 0.25) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, next, 1, 0.05, 0.05, 0.05, 0.0);
            }

            current = next;
        }
    }

    /**
     * Draws a short jagged lightning bolt segment from the given start in the given direction.
     *
     * @param world     The world to spawn particles in.
     * @param start     The starting location of the bolt.
     * @param direction The primary direction of the bolt (should be normalized).
     * @param index     The bolt index, used to cycle dust colors.
     */
    private void spawnZigzagBoltInDirection(World world, Location start, Vector direction, int index) {
        int segments = 3 + random.nextInt(3);
        double totalLength = 0.8 + random.nextDouble() * 1.2;
        double segmentLength = totalLength / segments;
        Location current = start.clone();

        for (int s = 0; s < segments; s++) {
            Vector jitter = new Vector(
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5
            );
            Location next = current.clone()
                    .add(direction.clone().multiply(segmentLength))
                    .add(jitter);

            Particle.DustOptions dust = switch ((s + index) % 3) {
                case 0 -> AranarthBendingUtils.LIGHTNING_DUST_BRIGHT;
                case 1 -> AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                default -> AranarthBendingUtils.LIGHTNING_DUST;
            };

            world.spawnParticle(Particle.DUST, next, 1, 0, 0, 0, 0, dust);
            if (random.nextDouble() < 0.3) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, next, 1, 0.05, 0.05, 0.05, 0.0);
            }

            current = next;
        }
    }

    private void tickStrikeEffects(long now) {
        Location loc = player.getLocation();
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            UUID id = entity.getUniqueId();
            Long lastStrike = entityLastStrikeTime.get(id);
            if (lastStrike != null && now - lastStrike < STRIKE_INTERVAL_MS) {
                continue;
            }

            entityLastStrikeTime.put(id, now);
            DamageHandler.damageEntity(living, damage, this);
            AranarthBendingUtils.applyElectrocution(living, ELECTROCUTION_DURATION_MS);

            for (int k = 0; k < 8; k++) {
                Particle.DustOptions dust = switch (k % 3) {
                    case 0 -> AranarthBendingUtils.LIGHTNING_DUST_BRIGHT;
                    case 1 -> AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                    default -> AranarthBendingUtils.LIGHTNING_DUST;
                };
                entity.getLocation().getWorld().spawnParticle(
                        Particle.DUST, entity.getLocation(), 1,
                        random.nextDouble() * 0.4,
                        random.nextDouble() * living.getHeight() * 0.5,
                        random.nextDouble() * 0.4, 0, dust);
            }
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BEE_HURT, 0.8f, 0.2f);
        }
    }

    private void tickSounds(long now) {
        if (now - lastSoundTime >= SOUND_INTERVAL_MS) {
            lastSoundTime = now;
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BEE_HURT, 0.5f, 0.2f);
        }
    }

    public boolean isFlightEnded() {
        return flightEnded;
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static JetBolt getActiveInstance(UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    @Override
    public void remove() {
        super.remove();

        ACTIVE_INSTANCES.remove(player.getUniqueId());
        if (started) {
            if (flightEndTime == 0) {
                // endFlight() was never called (i.e player died or disconnected mid-flight)
                bPlayer.addCooldown("FireJet", 7000L);
                bPlayer.addCooldown("JetBolt", COOLDOWN_MS);
            } else {
                // endFlight() already started both cooldowns, but super.remove() just reset
                long now = System.currentTimeMillis();
                long jbRemaining = COOLDOWN_MS - (now - flightEndTime);
                if (jbRemaining > 0) {
                    bPlayer.addCooldown("JetBolt", jbRemaining);
                } else {
                    bPlayer.getCooldowns().remove("JetBolt");
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
        return cooldown;
    }

    @Override
    public String getName() {
        return "JetBolt";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new JetBolt(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        return ComboUtil.generateCombinationFromList(this, new ArrayList<>(Arrays.asList(
                "FireJet:SHIFT_DOWN",
                "FireJet:SHIFT_UP",
                "FireJet:SHIFT_DOWN",
                "FireJet:SHIFT_UP",
                "Lightning:SHIFT_DOWN",
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
        return "Channel a storm of electrical energy, and surge yourself through the air at " +
                "the highest of speeds, leaving a trail of lightning strikes that electrocute everything " +
                "in your trail.\n" +
                ChatUtils.translateToColor(
                        "&fUsage: FireJet (Tap Shift) > FireJet (Tap Shift) > " +
                                "Lightning (Hold Shift) > FireJet (Left-Click)");
    }
}
