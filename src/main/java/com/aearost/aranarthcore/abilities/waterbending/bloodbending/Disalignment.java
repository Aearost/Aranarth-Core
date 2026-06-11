package com.aearost.aranarthcore.abilities.waterbending.bloodbending;

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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Disalignment extends BloodAbility implements AddonAbility {

    public enum Phase { CHARGING, CHARGED, DISALIGNING }

    private static final long CHARGE_DURATION_MS = 5000L;
    private static final long DISALIGNMENT_DURATION_MS = 10000L;
    private static final long ACTION_BAR_INTERVAL_MS = 500L;
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

    private static final Map<UUID, Disalignment> ACTIVE_INSTANCES = new HashMap<>();
    private static final Set<UUID> DISALIGNED_PLAYERS = new HashSet<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute("DisalignmentDuration")
    private long disalignmentDuration;

    private Phase phase;
    private long chargeStartTime;
    private long disalignmentStartTime;
    private long lastActionBarTime;
    private LivingEntity target;
    private boolean disalignmentApplied;
    private final Set<Element> suppressedElements = new HashSet<>();

    public Disalignment(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (!isNight(player.getWorld())) {
            return;
        }

        this.cooldown = 15000L;
        this.range = 6.0;
        this.chargeDuration = CHARGE_DURATION_MS;
        this.disalignmentDuration = DISALIGNMENT_DURATION_MS;

        LivingEntity found = findTarget();
        if (found == null) {
            return;
        }

        this.target = found;
        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.disalignmentApplied = false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.7f);

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
            remove();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case CHARGED -> progressCharged();
            case DISALIGNING -> progressDisaligning();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }

        if (player.getLocation().distance(target.getLocation()) > range) {
            remove();
            return;
        }

        spawnChargingParticles();

        if (System.currentTimeMillis() - chargeStartTime >= chargeDuration) {
            transitionToCharged();
        }
    }

    private void transitionToCharged() {
        phase = Phase.CHARGED;
        spawnChargedBurst();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.6f, 0.3f);
    }

    private void progressCharged() {
        if (player.getLocation().distance(target.getLocation()) > range) {
            remove();
            return;
        }

        spawnChargedWaitParticles();
    }

    /**
     * Applies chiblocking to the target when the caster left-clicks while in the charged state.
     */
    public void onLeftClick() {
        if (phase != Phase.CHARGED) {
            return;
        }

        if (!target.isValid() || player.getLocation().distance(target.getLocation()) > range) {
            remove();
            return;
        }

        applyDisalignment();
    }

    private void applyDisalignment() {
        if (target instanceof Player targetPlayer) {
            DISALIGNED_PLAYERS.add(targetPlayer.getUniqueId());
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

        disalignmentApplied = true;
        phase = Phase.DISALIGNING;
        disalignmentStartTime = System.currentTimeMillis();
        lastActionBarTime = 0L;

        spawnDisalignmentImpact();
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 0.4f, 1.5f);
    }

    private void progressDisaligning() {
        if (System.currentTimeMillis() - disalignmentStartTime >= disalignmentDuration) {
            finishWithCooldown();
            return;
        }

        if (target.isValid()) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true), true);
        }

        if (target instanceof Player targetPlayer && target.isValid()) {
            if (System.currentTimeMillis() - lastActionBarTime >= ACTION_BAR_INTERVAL_MS) {
                targetPlayer.sendActionBar(Component.text("* MISALIGNED *").color(NamedTextColor.DARK_RED));
                lastActionBarTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Ends the ability immediately when the caster takes damage during charging or the charged state.
     */
    public void cancelFromDamage() {
        if (phase == Phase.DISALIGNING) {
            return;
        }
        remove();
    }

    public void endWithCooldown() {
        finishWithCooldown();
    }

    private void finishWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void restoreTarget() {
        if (!disalignmentApplied || target == null) {
            return;
        }
        if (target instanceof Player targetPlayer) {
            DISALIGNED_PLAYERS.remove(targetPlayer.getUniqueId());
            if (!suppressedElements.isEmpty() && targetPlayer.isOnline()) {
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
        disalignmentApplied = false;
    }

    private void spawnChargingParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        double elapsed = (System.currentTimeMillis() - chargeStartTime) / 1000.0;
        double progress = Math.min(elapsed / (chargeDuration / 1000.0), 1.0);
        // Three strands spiral inward toward the target as the charge builds
        double radius = 1.2 - progress * 0.8;
        int pointsPerStrand = 5;

        for (int strand = 0; strand < 3; strand++) {
            double strandOffset = strand * (2.0 * Math.PI / 3.0);
            for (int i = 0; i < pointsPerStrand; i++) {
                double t = (double) i / pointsPerStrand;
                double angle = t * Math.PI * 2.0 + elapsed * 3.0 + strandOffset;
                double y = (t - 0.5) * target.getHeight() * 1.2;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                center.getWorld().spawnParticle(Particle.DUST,
                        center.clone().add(x, y, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
            }
        }
    }

    private void spawnChargedBurst() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        int burstPoints = 20;

        for (int i = 0; i < burstPoints; i++) {
            double yaw = (2.0 * Math.PI / burstPoints) * i;
            double pitch = Math.PI * (i % 4) / 4.0 - Math.PI / 2.0;
            double x = Math.cos(pitch) * Math.cos(yaw) * 0.6;
            double y = Math.sin(pitch) * 0.6;
            double z = Math.cos(pitch) * Math.sin(yaw) * 0.6;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
        }
    }

    private void spawnChargedWaitParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        long tick = System.currentTimeMillis() / 50L;
        double angle = (tick % 40) * (2.0 * Math.PI / 40.0);

        center.getWorld().spawnParticle(Particle.DUST,
                center.clone().add(Math.cos(angle) * 0.3, 0, Math.sin(angle) * 0.3),
                1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
        center.getWorld().spawnParticle(Particle.DUST,
                center.clone().add(Math.cos(angle + Math.PI) * 0.3, 0, Math.sin(angle + Math.PI) * 0.3),
                1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
    }

    private void spawnDisalignmentImpact() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        int outerPoints = 20;

        for (int i = 0; i < outerPoints; i++) {
            double angle = (2.0 * Math.PI / outerPoints) * i;
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(Math.cos(angle) * 0.6, 0, Math.sin(angle) * 0.6),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
        }
        // Vertical column of particles through the body to represent disrupted chi flow
        for (int i = 0; i < 8; i++) {
            center.getWorld().spawnParticle(Particle.DUST,
                    center.clone().add(0, (i - 3.5) * (target.getHeight() / 8.0), 0),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
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
            if (entity instanceof Player targetPlayer && DISALIGNED_PLAYERS.contains(targetPlayer.getUniqueId())) {
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

    @Override
    public void remove() {
        restoreTarget();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        restoreTarget();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static Disalignment getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public static boolean isDisaligned(final UUID targetUuid) {
        return DISALIGNED_PLAYERS.contains(targetUuid);
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
        return "Disalignment";
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
        return "Deeply manipulate the flow of a target's blood to disrupt their chi pathways entirely, " +
                "severing their connection to their bending for an extended time period.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (hold to charge) > Left-Click (apply)");
    }

}
