package com.aearost.aranarthcore.abilities.earthbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SandAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Sandstorm extends SandAbility implements AddonAbility {

    // -------------------------------------------------------------------------
    // Dust palettes — colour varies by source block type
    // -------------------------------------------------------------------------

    private static final Particle.DustOptions[] YELLOW_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0xC2, 0xB2, 0x80), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0xD2, 0xB4, 0x8C), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0xE8, 0xD5, 0xA0), 0.8f),
            new Particle.DustOptions(Color.fromRGB(0xC1, 0x9A, 0x6B), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0xA8, 0x96, 0x60), 0.9f),
    };
    private static final Particle.DustOptions[] RED_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0xC8, 0x5A, 0x32), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0xD4, 0x6A, 0x3E), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0xB8, 0x4A, 0x28), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0xDC, 0x7A, 0x50), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0xA0, 0x3C, 0x1E), 0.8f),
    };
    private static final Particle.DustOptions[] SOUL_SAND_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0x6B, 0x4A, 0x2E), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0x7A, 0x56, 0x38), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0x5C, 0x3E, 0x28), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0x8A, 0x64, 0x46), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0x4A, 0x32, 0x1E), 0.8f),
    };
    private static final Particle.DustOptions[] GRAVEL_PALETTE = {
            new Particle.DustOptions(Color.fromRGB(0x80, 0x80, 0x80), 1.2f),
            new Particle.DustOptions(Color.fromRGB(0x6E, 0x6E, 0x6E), 1.0f),
            new Particle.DustOptions(Color.fromRGB(0x96, 0x90, 0x8A), 1.1f),
            new Particle.DustOptions(Color.fromRGB(0x5A, 0x56, 0x52), 0.9f),
            new Particle.DustOptions(Color.fromRGB(0xAA, 0xA4, 0x9C), 0.8f),
    };

    public enum Phase { SELECTED, CASTING, COLLAPSING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute("MaxRadius")
    private double maxRadius;
    @Attribute(Attribute.DAMAGE)
    private double baseDamage;
    @Attribute("DamageIncrement")
    private double damageIncrement;
    @Attribute("MaxDamagePerInterval")
    private double maxDamagePerInterval;
    @Attribute("ExpansionInterval")
    private long expansionInterval;
    @Attribute("DamageInterval")
    private long damageInterval;
    @Attribute("MaxCastingDuration")
    private long maxCastingDuration;
    @Attribute("SelectSourceRange")
    private double selectSourceRange;
    @Attribute("SlownessUpgradeInterval")
    private long slownessUpgradeInterval;

    private static final long COLLAPSE_DURATION = 2000L; // 2-second fade-out

    private Phase phase;
    private final Block sourceBlock;
    private Location castLocation;
    private double currentRadius;
    private long lastExpansionTime;
    private long lastDamageTime;
    private long lastSoundTime;
    private long collapseStartTime;
    private long castingStartTime;
    private final Particle.DustOptions[] dustPalette;

    private final Map<UUID, Long> entityEntryTimes = new HashMap<>();
    private final Map<UUID, LivingEntity> entityInDomeCache = new HashMap<>();
    private final Map<UUID, Integer> entityHitCounts = new HashMap<>();

    private static final Map<UUID, Sandstorm> activeInstances = new HashMap<>();
    private final Random random = new Random();

    public Sandstorm(Player player, Block sourceBlock) {
        super(player);

        this.sourceBlock = sourceBlock;
        this.dustPalette = pickDustPalette(sourceBlock.getType());

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 12000;
        maxRadius = 10.0;
        baseDamage = 1.0;
        damageIncrement = 1.0;
        maxDamagePerInterval = 4.0;
        expansionInterval = 500;
        damageInterval = 500;
        maxCastingDuration = 10000L;
        selectSourceRange = 5.0;
        slownessUpgradeInterval = 1500;

        phase = Phase.SELECTED;
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    // -------------------------------------------------------------------------
    // Public control methods
    // -------------------------------------------------------------------------

    public void startCasting() {
        if (phase != Phase.SELECTED) {
            return;
        }

        phase = Phase.CASTING;
        castLocation = player.getLocation().clone();
        currentRadius = 1.5;
        castingStartTime = System.currentTimeMillis();
        lastExpansionTime = System.currentTimeMillis();
        lastDamageTime = System.currentTimeMillis();
        lastSoundTime = System.currentTimeMillis();

        spawnFeetParticles();
    }

    public void cancelFromSlotChange() {
        if (phase == Phase.SELECTED) {
            remove(); // No cooldown as the player hadn't started casting
        } else if (phase == Phase.CASTING) {
            startCollapsing();
        }
    }

    public boolean isCasting() {
        return phase == Phase.CASTING;
    }

    @Override
    public void progress() {
        if (!player.isOnline()) {
            remove();
            return;
        }
        switch (phase) {
            case SELECTED -> progressSelected();
            case CASTING -> progressCasting();
            case COLLAPSING -> progressCollapsing();
        }
    }

    private void progressSelected() {
        // Reset if too far from the source block
        Location sourceCenter = sourceBlock.getLocation().clone().add(0.5, 0.5, 0.5);
        if (player.getLocation().distance(sourceCenter) > selectSourceRange) {
            bPlayer.removeCooldown(getName());
            remove();
            return;
        }

        // Show particles at the source block
        sourceBlock.getWorld().spawnParticle(Particle.DUST,
                sourceBlock.getLocation().clone().add(0.5, 1.05, 0.5),
                3, 0.25, 0.05, 0.25, 0,
                dustPalette[0]
        );
    }

    // -------------------------------------------------------------------------
    // Phase: CASTING
    // -------------------------------------------------------------------------

    private void progressCasting() {
        if (!player.isSneaking()) {
            startCollapsing();
            return;
        }

        // Root the player, movement is cancelled in AranarthCoreBendingListener
        player.setVelocity(new Vector(0, 0, 0));

        long now = System.currentTimeMillis();

        // End after max casting duration
        if (now - castingStartTime >= maxCastingDuration) {
            startCollapsing();
            return;
        }

        // Expand dome by 1 block every expansionInterval until max radius; stays at max after that
        if (currentRadius < maxRadius && now - lastExpansionTime >= expansionInterval) {
            currentRadius = Math.min(currentRadius + 1.0, maxRadius);
            lastExpansionTime = now;
        }

        spawnDomeParticles();
        refreshEntityTracking();

        if (now - lastDamageTime >= damageInterval) {
            damageEntitiesInDome();
            lastDamageTime = now;
        }

        if (now - lastSoundTime >= 750) {
            playSandstormSounds();
            lastSoundTime = now;
        }
    }

    // -------------------------------------------------------------------------
    // Phase: COLLAPSING
    // -------------------------------------------------------------------------

    private void startCollapsing() {
        phase = Phase.COLLAPSING;
        collapseStartTime = System.currentTimeMillis();
    }

    private void progressCollapsing() {
        long elapsed = System.currentTimeMillis() - collapseStartTime;
        if (elapsed >= COLLAPSE_DURATION) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }
        // Linear fade: full density at collapse start, zero at COLLAPSE_DURATION
        double fadeFactor = 1.0 - (elapsed / (double) COLLAPSE_DURATION);
        spawnDomeParticles(fadeFactor);
    }

    // -------------------------------------------------------------------------
    // Particles
    // -------------------------------------------------------------------------

    private void spawnFeetParticles() {
        World world = player.getWorld();
        double bx = castLocation.getX();
        double by = castLocation.getY();
        double bz = castLocation.getZ();

        for (int i = 0; i < 45; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double r = random.nextDouble();
            world.spawnParticle(
                    Particle.DUST,
                    bx + r * Math.cos(angle),
                    by + random.nextDouble() * 1.2,
                    bz + r * Math.sin(angle),
                    1, 0, 0, 0, 0,
                    dustPalette[random.nextInt(dustPalette.length)]
            );
        }
    }

    private void spawnDomeParticles() {
        spawnDomeParticles(1.0);
    }

    /**
     * Spawns dome particles that are scaled by the input fadeFactor.
     * @param fadeFactor 1.0 for full, 0.0 for none
     */
    private void spawnDomeParticles(double fadeFactor) {
        World world = player.getWorld();
        Location center = castLocation.clone().add(0, 1.0, 0);
        double r = currentRadius;

        // --- Pass 1: surface shell, particles stop at solid blocks
        int surfaceCount = (int) (Math.max(20, Math.min((int) (r * r * 5), 150)) * fadeFactor);
        for (int i = 0; i < surfaceCount; i++) {
            double theta = Math.acos(1.0 - 2.0 * random.nextDouble());
            double phi   = 2.0 * Math.PI * random.nextDouble();
            double dx = r * Math.sin(theta) * Math.cos(phi);
            double dy = r * Math.cos(theta);
            double dz = r * Math.sin(theta) * Math.sin(phi);
            Location loc = center.clone().add(dx, dy, dz);
            if (!hasSolidBlockBetween(center, loc)) {
                world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                        dustPalette[random.nextInt(dustPalette.length)]);
            }
        }

        // --- Pass 2: interior dome fill
        int volumeCount = (int) (Math.max(30, Math.min((int) (r * r * r * 0.4), 200)) * fadeFactor);
        for (int i = 0; i < volumeCount; i++) {
            double rSample = r * Math.cbrt(random.nextDouble());
            double theta = Math.acos(1.0 - 2.0 * random.nextDouble());
            double phi = 2.0 * Math.PI * random.nextDouble();
            double dx = rSample * Math.sin(theta) * Math.cos(phi);
            double dy = rSample * Math.cos(theta);
            double dz = rSample * Math.sin(theta) * Math.sin(phi);
            Location loc = center.clone().add(dx, dy, dz);
            if (!loc.getBlock().getType().isSolid()) {
                world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                        dustPalette[random.nextInt(dustPalette.length)]);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Entity tracking — slowness & damage
    // -------------------------------------------------------------------------

    /**
     * Called every tick during CASTING and COLLAPSING.
     */
    private void refreshEntityTracking() {
        entityInDomeCache.clear();
        Location center = castLocation.clone().add(0, 1.0, 0);
        double scan = currentRadius + 1.5;
        long now = System.currentTimeMillis();

        for (Entity entity : player.getNearbyEntities(scan, scan, scan)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (entity.getLocation().distance(center) > currentRadius) {
                continue;
            }
            if (hasSolidBlockBetween(center, entity.getLocation())) {
                continue;
            }

            UUID id = entity.getUniqueId();
            entityInDomeCache.put(id, living);
            entityEntryTimes.putIfAbsent(id, now);

            long timeInStorm = now - entityEntryTimes.get(id);
            int slownessLevel = (timeInStorm < slownessUpgradeInterval) ? 0 : (timeInStorm < slownessUpgradeInterval * 2) ? 1 : 2;

            // 2-second duration refreshed every tick so it never expires mid-storm
            living.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, 40, slownessLevel, false, true));
        }

        // Remove entries for entities that have left the dome
        entityEntryTimes.keySet().retainAll(entityInDomeCache.keySet());
        entityHitCounts.keySet().retainAll(entityInDomeCache.keySet());
    }

    private void damageEntitiesInDome() {
        for (Map.Entry<UUID, LivingEntity> entry : entityInDomeCache.entrySet()) {
            UUID id = entry.getKey();
            LivingEntity living = entry.getValue();
            int hitCount = entityHitCounts.getOrDefault(id, 0);
            double damage = Math.min(baseDamage + hitCount * damageIncrement, maxDamagePerInterval);
            entityHitCounts.put(id, hitCount + 1);
            DamageHandler.damageEntity(living, damage, this);
        }
    }

    private void playSandstormSounds() {
        Location loc  = castLocation.clone().add(0, 1, 0);
        World world = player.getWorld();
        // Three overlapping layers at slightly randomised pitches for a natural swirl effect
        world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.70f, 0.58f + random.nextFloat() * 0.28f);
        world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.50f, 0.68f + random.nextFloat() * 0.22f);
        world.playSound(loc, Sound.ENTITY_BREEZE_IDLE_GROUND, 0.35f, 0.52f + random.nextFloat() * 0.30f);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean hasSolidBlockBetween(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length < 0.5) {
            return false;
        }
        dir.normalize();
        int steps = (int) (length / 0.5);
        for (int i = 1; i <= steps; i++) {
            if (from.clone().add(dir.clone().multiply(i * 0.5)).getBlock().getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static Particle.DustOptions[] pickDustPalette(Material material) {
        return switch (material) {
            // Red/orange tones
            case RED_SAND,
                 RED_SANDSTONE, RED_SANDSTONE_SLAB, RED_SANDSTONE_STAIRS, RED_SANDSTONE_WALL,
                 CHISELED_RED_SANDSTONE, CUT_RED_SANDSTONE, CUT_RED_SANDSTONE_SLAB,
                 SMOOTH_RED_SANDSTONE, SMOOTH_RED_SANDSTONE_STAIRS, SMOOTH_RED_SANDSTONE_SLAB -> RED_SAND_PALETTE;
            // Dark brown tones
            case SOUL_SAND, SOUL_SOIL -> SOUL_SAND_PALETTE;
            // Gray tones (all concrete powders included)
            case GRAVEL, SUSPICIOUS_GRAVEL,
                 WHITE_CONCRETE_POWDER, ORANGE_CONCRETE_POWDER, MAGENTA_CONCRETE_POWDER,
                 LIGHT_BLUE_CONCRETE_POWDER, YELLOW_CONCRETE_POWDER, LIME_CONCRETE_POWDER,
                 PINK_CONCRETE_POWDER, GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE_POWDER,
                 CYAN_CONCRETE_POWDER, PURPLE_CONCRETE_POWDER, BLUE_CONCRETE_POWDER,
                 BROWN_CONCRETE_POWDER, GREEN_CONCRETE_POWDER, RED_CONCRETE_POWDER,
                 BLACK_CONCRETE_POWDER -> GRAVEL_PALETTE;
            // Yellow/tan tones (SAND, SANDSTONE variants, SUSPICIOUS_SAND)
            default -> YELLOW_SAND_PALETTE;
        };
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Sandstorm getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    @Override public boolean isSneakAbility() {
        return true;
    }

    @Override public boolean isHarmlessAbility() {
        return false;
    }

    @Override public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return castLocation != null ? castLocation : player.getLocation();
    }

    @Override public String getName() {
        return "Sandstorm";
    }

    @Override public void load() {}

    @Override public void stop() {}

    @Override public String getAuthor() {
        return "Aearost";
    }

    @Override public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Create an expanding dome of swirling sand that deals ticking damage to all targets caught in the storm. " +
                "The longer the target remains in the sandstorm, the stronger the effects of the storm.\n" +
                ChatUtils.translateToColor("&fUsage: Left-Click a sandbendable source > Sneak (Hold)");
    }

}
