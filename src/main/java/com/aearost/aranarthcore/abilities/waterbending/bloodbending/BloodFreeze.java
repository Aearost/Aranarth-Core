package com.aearost.aranarthcore.abilities.waterbending.bloodbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodFreeze extends BloodAbility implements AddonAbility {

    public enum Phase {CHARGING, CASTING}

    private static final long CHARGE_DURATION_MS = 1000L;
    private static final long CAST_DURATION_MS = 3000L;
    private static final long DAMAGE_INTERVAL_MS = 250L;
    private static final double DAMAGE_PER_TICK = 1.0;
    private static final int MAX_SLOWNESS_LEVEL = 5;
    private static final double TARGET_HIT_RADIUS = 0.5;

    private static final Set<EntityType> UNDEAD_TYPES = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.ZOMBIE_VILLAGER,
            EntityType.ZOMBIE_HORSE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.SKELETON,
            EntityType.SKELETON_HORSE,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.WITHER,
            EntityType.PHANTOM,
            EntityType.ZOGLIN,
            EntityType.ZOMBIFIED_PIGLIN
    );

    private static final Map<UUID, BloodFreeze> ACTIVE_INSTANCES = new HashMap<>();
    private static final Set<UUID> FROZEN_PLAYERS = new HashSet<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute("CastDuration")
    private long castDuration;

    private Phase phase;
    private long chargeStartTime;
    private long castStartTime;
    private long lastDamageTime;
    private LivingEntity target;
    private int slownessLevel;
    private boolean decayStarted;
    private final Set<Element> suppressedElements = new HashSet<>();

    public BloodFreeze(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = 10000L;
        this.range = 6.0;
        this.chargeDuration = CHARGE_DURATION_MS;
        this.castDuration = CAST_DURATION_MS;

        LivingEntity found = findTarget();
        if (found == null) {
            return;
        }

        this.target = found;
        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.slownessLevel = 0;
        this.lastDamageTime = 0L;
        this.decayStarted = false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.8f);

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        if (!target.isValid()) {
            finishWithCooldown();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case CASTING -> progressCasting();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        spawnChargingParticles();

        if (System.currentTimeMillis() - chargeStartTime >= chargeDuration) {
            beginCasting();
        }
    }

    private void beginCasting() {
        phase = Phase.CASTING;
        castStartTime = System.currentTimeMillis();
        lastDamageTime = castStartTime;

        if (target instanceof Player targetPlayer) {
            FROZEN_PLAYERS.add(targetPlayer.getUniqueId());
            BendingPlayer targetBP = BendingPlayer.getBendingPlayer(targetPlayer);
            if (targetBP != null) {
                for (Element element : targetBP.getElements()) {
                    if (element != Element.CHI && targetBP.isElementToggled(element)) {
                        targetBP.toggleElement(element);
                        suppressedElements.add(element);
                    }
                }
                for (Element element : targetBP.getSubElements()) {
                    if (targetBP.isElementToggled(element)) {
                        targetBP.toggleElement(element);
                        suppressedElements.add(element);
                    }
                }
            }
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.7f, 0.3f);
    }

    private void progressCasting() {
        if (!player.isSneaking()) {
            finishWithCooldown();
            return;
        }

        if (System.currentTimeMillis() - castStartTime >= castDuration) {
            finishWithCooldown();
            return;
        }

        target.setFreezeTicks(target.getMaxFreezeTicks());
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true), true);
        spawnCastingParticles();

        if (System.currentTimeMillis() - lastDamageTime >= DAMAGE_INTERVAL_MS) {
            applyDamageTick();
            lastDamageTime = System.currentTimeMillis();
        }
    }

    /**
     * Deals one damage tick directly to the target's health, bypassing armor and rank multipliers,
     * then applies or escalates the Slowness effect up to the maximum level.
     */
    private void applyDamageTick() {
        if (!target.isValid()) {
            finishWithCooldown();
            return;
        }

        target.setHealth(Math.max(0.0, target.getHealth() - DAMAGE_PER_TICK));

        slownessLevel = Math.min(slownessLevel + 1, MAX_SLOWNESS_LEVEL);
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                slownessLevel * 20,
                slownessLevel - 1,
                false, true, true), true);

        spawnDamageBurst();
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 0.5f);
    }

    /**
     * Schedules a per-second staircase decay of the Slowness effect applied to the target.
     */
    private void scheduleSlownessDecay() {
        if (decayStarted || slownessLevel <= 0 || !target.isValid()) {
            return;
        }
        decayStarted = true;
        final int[] level = {slownessLevel};
        new BukkitRunnable() {
            @Override
            public void run() {
                level[0]--;
                if (level[0] <= 0 || !target.isValid()) {
                    target.removePotionEffect(PotionEffectType.SLOWNESS);
                    cancel();
                    return;
                }
                target.removePotionEffect(PotionEffectType.SLOWNESS);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        level[0] * 20,
                        level[0] - 1,
                        false, true, true));
            }
        }.runTaskTimer(AranarthCore.getInstance(), 20L, 20L);
    }

    private void spawnChargingParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        double elapsed = (System.currentTimeMillis() - chargeStartTime) / 1000.0;
        double progress = Math.min(elapsed / (chargeDuration / 1000.0), 1.0);
        // Radius tightens toward the target as the charge builds up
        double radius = 0.8 - progress * 0.3;
        int pointsPerStrand = 6;

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            for (int i = 0; i < pointsPerStrand; i++) {
                double t = (double) i / pointsPerStrand;
                double angle = t * Math.PI * 2.0 + elapsed * 2.5 + strandOffset;
                double y = (t - 0.5) * target.getHeight() * 1.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(x, y, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
            }
        }
    }

    private void spawnCastingParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        long tick = System.currentTimeMillis() / 50L;
        double baseAngle = (tick % 60) * (2.0 * Math.PI / 60.0);
        int arms = 5;

        for (int i = 0; i < arms; i++) {
            double angle = baseAngle + (2.0 * Math.PI / arms) * i;
            double yOffset = Math.sin(tick * 0.2 + i) * 0.25;
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(angle) * 0.25, yOffset, Math.sin(angle) * 0.25),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(angle) * 0.5, -yOffset, Math.sin(angle) * 0.5),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
        }
    }

    private void spawnDamageBurst() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        int points = 14;
        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI / points) * i;
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(angle) * 0.65, 0, Math.sin(angle) * 0.65),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
        }
    }

    /**
     * Performs a ray trace from the caster's eye outward to find the nearest valid target
     * within range.
     */
    private LivingEntity findTarget() {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Dominion casterDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (UNDEAD_TYPES.contains(entity.getType())) {
                continue;
            }
            if (!entity.isValid()) {
                continue;
            }
            if (entity instanceof Player targetPlayer && FROZEN_PLAYERS.contains(targetPlayer.getUniqueId())) {
                continue;
            }
            if (entity instanceof Player targetPlayer) {
                Dominion targetDominion = DominionUtils.getPlayerDominion(targetPlayer.getUniqueId());
                if (casterDominion != null && targetDominion != null) {
                    if (casterDominion.isSameDominion(targetDominion)) {
                        continue;
                    }
                    DominionRank relation = DominionUtils.getRelationKey(casterDominion, targetDominion);
                    if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                        continue;
                    }
                }
            }

            double dist = entity.getLocation().distance(eye);
            if (dist > range) {
                continue;
            }

            if (entity.getBoundingBox().expand(TARGET_HIT_RADIUS)
                    .rayTrace(eye.toVector(), direction, range) == null) {
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
     * Ends the ability immediately when the caster takes damage, applying the cooldown.
     */
    public void cancelFromDamage() {
        finishWithCooldown();
    }

    public void endWithCooldown() {
        finishWithCooldown();
    }

    private void finishWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void releaseFrozenTarget() {
        if (target == null) {
            return;
        }
        if (target instanceof Player targetPlayer) {
            FROZEN_PLAYERS.remove(targetPlayer.getUniqueId());
            if (!suppressedElements.isEmpty()) {
                BendingPlayer targetBP = BendingPlayer.getBendingPlayer(targetPlayer);
                if (targetBP != null) {
                    for (Element element : suppressedElements) {
                        if (!targetBP.isElementToggled(element)) {
                            targetBP.toggleElement(element);
                        }
                    }
                }
                suppressedElements.clear();
            }
        }
        if (target.isValid()) {
            target.setFreezeTicks(0);
        }
    }

    @Override
    public void remove() {
        releaseFrozenTarget();
        scheduleSlownessDecay();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        releaseFrozenTarget();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static BloodFreeze getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
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
        return target != null && target.isValid() ? target.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "BloodFreeze";
    }

    @Override
    public void load() {
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
        return "Take control of a player's bloodstream, and lower the temperature of their blood to freeze them from the inside. " +
                "Each pulse of the ability will slow the target further, gradually thawing once the ability ends.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (hold)");
    }

}
