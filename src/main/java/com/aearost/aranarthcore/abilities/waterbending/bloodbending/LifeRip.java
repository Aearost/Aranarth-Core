package com.aearost.aranarthcore.abilities.waterbending.bloodbending;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import static org.bukkit.attribute.Attribute.MAX_HEALTH;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class LifeRip extends BloodAbility implements AddonAbility {


    /**
     * PDC key tracking the total maximum health drained from a player by LifeRip.
     */
    private static final NamespacedKey TARGET_DRAIN_KEY =
            new NamespacedKey("aranarthcore", "liferip_target_drain");

    /**
     * PDC key tracking the total maximum health gained by a caster from LifeRip.
     */
    private static final NamespacedKey CASTER_GAIN_KEY =
            new NamespacedKey("aranarthcore", "liferip_caster_gain");

    private static final Map<UUID, LifeRip> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute("CastDuration")
    private long castDuration;

    private long castStartTime;
    private Player target;
    private final Set<Element> suppressedElements = new HashSet<>();

    public LifeRip(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        double currentGain = player.getPersistentDataContainer()
                .getOrDefault(CASTER_GAIN_KEY, PersistentDataType.DOUBLE, 0.0);
        if (currentGain >= AbilityConfig.getDouble("LifeRip.CasterMaxGainCap")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "bending.life_rip_max")));
            return;
        }

        this.cooldown = AbilityConfig.getLong("LifeRip.Cooldown");
        this.range = AbilityConfig.getDouble("LifeRip.Range");
        this.castDuration = AbilityConfig.getLong("LifeRip.Duration");

        Player found = findTarget();
        if (found == null) {
            return;
        }

        this.target = found;
        this.castStartTime = System.currentTimeMillis();

        BendingPlayer targetBP = BendingPlayer.getBendingPlayer(found);
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

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.6f);

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

        if (!player.isSneaking()) {
            remove();
            return;
        }

        if (player.getLocation().distance(target.getLocation()) > range) {
            remove();
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true), true);
        spawnCastingParticles();

        if (System.currentTimeMillis() - castStartTime >= castDuration) {
            applyLifeRip();
            finishWithCooldown();
        }
    }

    private void applyLifeRip() {
        // The target may have moved somewhere protected while the drain was charging
        if (!DominionUtils.canAttackPlayer(player, target)
                || AranarthBendingUtils.isBendingBlockedAtLocation(player, target.getLocation())) {
            return;
        }
        double missingHP = 0.0;
        AttributeInstance targetAttr = target.getAttribute(MAX_HEALTH);
        if (targetAttr != null) {
            missingHP = targetAttr.getValue() - target.getHealth();
            // Real damage so protection, combat tagging and kill attribution all apply. The max health
            // drain only happens if the hit actually landed
            if (!AranarthBendingUtils.damageIgnoringArmor(target, AbilityConfig.getDouble("LifeRip.DrainDamage"), this)) {
                return;
            }
            if (target.isDead()) {
                return;
            }

            // Reduce max health by the standard 1 heart plus whatever the target was already missing
            double totalMaxReduction = AbilityConfig.getDouble("LifeRip.TargetMaxHealthLoss") + missingHP;
            double newBaseValue = Math.max(2.0, targetAttr.getBaseValue() - totalMaxReduction);
            double actualReduction = targetAttr.getBaseValue() - newBaseValue;
            targetAttr.setBaseValue(newBaseValue);
            double existingDrain = target.getPersistentDataContainer()
                    .getOrDefault(TARGET_DRAIN_KEY, PersistentDataType.DOUBLE, 0.0);
            recordAmount(target, TARGET_DRAIN_KEY, existingDrain + actualReduction);
            // Clamp current health to the new computed maximum
            if (target.getHealth() > targetAttr.getValue()) {
                target.setHealth(targetAttr.getValue());
            }
        }

        AttributeInstance casterAttr = player.getAttribute(MAX_HEALTH);
        if (casterAttr != null) {
            casterAttr.setBaseValue(casterAttr.getBaseValue() + AbilityConfig.getDouble("LifeRip.CasterMaxHealthGain"));
            double existingGain = player.getPersistentDataContainer()
                    .getOrDefault(CASTER_GAIN_KEY, PersistentDataType.DOUBLE, 0.0);
            recordAmount(player, CASTER_GAIN_KEY, existingGain + AbilityConfig.getDouble("LifeRip.CasterMaxHealthGain"));
            // Restore half of whatever health the target was missing as current health to the caster
            if (missingHP > 0.0) {
                player.setHealth(Math.min(player.getHealth() + missingHP / 2.0, casterAttr.getValue()));
            }
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true, true));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
        spawnSuccessBurst();
    }

    public static void resetTargetDrain(final Player target) {
        Double drain = target.getPersistentDataContainer().get(TARGET_DRAIN_KEY, PersistentDataType.DOUBLE);
        recordAmount(target, TARGET_DRAIN_KEY, 0.0);
        if (drain == null) {
            return;
        }
        AttributeInstance attr = target.getAttribute(MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(attr.getBaseValue() + drain);
        }
    }

    public static void resetCasterGain(final Player caster) {
        Double gain = caster.getPersistentDataContainer().get(CASTER_GAIN_KEY, PersistentDataType.DOUBLE);
        recordAmount(caster, CASTER_GAIN_KEY, 0.0);
        if (gain == null) {
            return;
        }
        AttributeInstance attr = caster.getAttribute(MAX_HEALTH);
        if (attr != null) {
            // Ensure base health never drops below 1 heart from floating-point imprecision
            attr.setBaseValue(Math.max(2.0, attr.getBaseValue() - gain));
        }
    }

    /**
     * Saves a LifeRip amount both on this server's player data, which tracks what is applied to the
     * player's maximum health here, and on the AranarthPlayer, which is shared by every server.
     */
    private static void recordAmount(final Player player, final NamespacedKey key, final double amount) {
        if (amount > 0) {
            player.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, amount);
        } else {
            player.getPersistentDataContainer().remove(key);
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer != null) {
            if (key.equals(TARGET_DRAIN_KEY)) {
                aranarthPlayer.setLifeRipDrain(amount);
            } else {
                aranarthPlayer.setLifeRipGain(amount);
            }
        }
    }

    /**
     * Updates the player's maximum health on this server to match the LifeRip amounts saved on their
     * AranarthPlayer, as each server has its own player data and the player may have gained or lost
     * health from LifeRip on the other server since they were last here.
     *
     * @param player The player.
     */
    public static void syncMaxHealth(final Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        AttributeInstance attr = player.getAttribute(MAX_HEALTH);
        if (aranarthPlayer == null || attr == null) {
            return;
        }
        double appliedDrain = player.getPersistentDataContainer().getOrDefault(TARGET_DRAIN_KEY, PersistentDataType.DOUBLE, 0.0);
        double appliedGain = player.getPersistentDataContainer().getOrDefault(CASTER_GAIN_KEY, PersistentDataType.DOUBLE, 0.0);
        // Saved before the amounts were shared between servers, so this server's amounts are used
        if (aranarthPlayer.getLifeRipDrain() < 0 || aranarthPlayer.getLifeRipGain() < 0) {
            aranarthPlayer.setLifeRipDrain(appliedDrain);
            aranarthPlayer.setLifeRipGain(appliedGain);
            return;
        }

        double drain = aranarthPlayer.getLifeRipDrain();
        double gain = aranarthPlayer.getLifeRipGain();
        double adjustment = (appliedDrain - drain) + (gain - appliedGain);
        if (Math.abs(adjustment) < 0.0001) {
            return;
        }
        attr.setBaseValue(Math.max(2.0, attr.getBaseValue() + adjustment));
        recordAmount(player, TARGET_DRAIN_KEY, drain);
        recordAmount(player, CASTER_GAIN_KEY, gain);
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }

    private void spawnCastingParticles() {
        Location base = target.getLocation();
        double elapsed = (System.currentTimeMillis() - castStartTime) / 1000.0;
        double height = target.getHeight();
        int columns = 4;
        int pointsPerColumn = 3;

        // Four rising columns of particles that slowly rotate around the target
        for (int c = 0; c < columns; c++) {
            double columnAngle = (2.0 * Math.PI / columns) * c + elapsed * 1.5;
            for (int p = 0; p < pointsPerColumn; p++) {
                // Each point rises continuously and loops
                double rise = ((elapsed * 0.6 + (double) p / pointsPerColumn + (double) c / columns) % 1.0);
                double y = rise * height;
                double radius = 0.45 + Math.sin(rise * Math.PI) * 0.15;
                double x = Math.cos(columnAngle) * radius;
                double z = Math.sin(columnAngle) * radius;
                base.getWorld().spawnParticle(Particle.DUST, base.clone().add(x, y, z),
                        1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
            }
        }
    }

    private void spawnSuccessBurst() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        int spherePoints = 20;

        for (int i = 0; i < spherePoints; i++) {
            double yaw = (2.0 * Math.PI / spherePoints) * i;
            double pitch = Math.PI * (i % 4) / 4.0 - Math.PI / 2.0;
            double x = Math.cos(pitch) * Math.cos(yaw) * 0.7;
            double y = Math.sin(pitch) * 0.7;
            double z = Math.cos(pitch) * Math.sin(yaw) * 0.7;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z),
                    1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
        }
        // Vertical column shooting upward to represent life force leaving the body
        int columnPoints = 10;
        for (int i = 0; i < columnPoints; i++) {
            double y = (double) i / columnPoints * target.getHeight() * 1.5;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, y, 0),
                    1, 0.05, 0, 0.05, 0, AranarthBendingUtils.BLOOD_DUST);
        }
    }

    private Player findTarget() {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Dominion casterDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (!(entity instanceof Player targetPlayer)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (!entity.isValid()) {
                continue;
            }

            // Only players that could be attacked normally, outside of areas where bending is blocked
            if (!DominionUtils.canAttackPlayer(player, targetPlayer)
                    || AranarthBendingUtils.isBendingBlockedAtLocation(player, targetPlayer.getLocation())) {
                continue;
            }

            // Exclude same-dominion members and allied/truced dominion players
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

            double dist = entity.getLocation().distance(eye);
            if (dist > range) {
                continue;
            }

            if (entity.getBoundingBox().expand(AbilityConfig.getDouble("LifeRip.HitRadius"))
                    .rayTrace(eye.toVector(), direction, range) == null) {
                continue;
            }

            if (dist < closestDist) {
                closestDist = dist;
                closest = targetPlayer;
            }
        }
        return closest;
    }

    public void cancelFromDamage() {
        remove();
    }

    public void endWithCooldown() {
        finishWithCooldown();
    }

    private void finishWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    private void releaseTarget() {
        if (suppressedElements == null || suppressedElements.isEmpty() || target == null) {
            return;
        }
        BendingPlayer targetBP = BendingPlayer.getBendingPlayer(target);
        if (targetBP != null) {
            for (Element element : suppressedElements) {
                if (!targetBP.isElementToggled(element)) {
                    targetBP.toggleElement(element);
                }
            }
        }
        suppressedElements.clear();
    }

    @Override
    public void remove() {
        releaseTarget();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public void stop() {
        releaseTarget();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static LifeRip getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
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
        return "LifeRip";
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
        return "Siphon the very life force from your target with the darkest bloodbending technique, " +
                "permanently claiming a portion of your target's health as your own.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak");
    }

}
