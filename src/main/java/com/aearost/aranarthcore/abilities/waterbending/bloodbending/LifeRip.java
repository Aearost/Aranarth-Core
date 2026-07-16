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

    private static final long CAST_DURATION_MS = 6000L;
    private static final double DRAIN_DAMAGE = 2.0;
    private static final double TARGET_MAX_HEALTH_LOSS = 2.0;
    private static final double CASTER_MAX_HEALTH_GAIN = 1.0;
    private static final double CASTER_MAX_GAIN_CAP = 20.0;
    private static final double TARGET_HIT_RADIUS = 0.5;

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
        if (currentGain >= CASTER_MAX_GAIN_CAP) {
            player.sendMessage(ChatUtils.chatMessage("&cYou have already absorbed the maximum amount of life force possible"));
            return;
        }

        this.cooldown = 30000L;
        this.range = 7.0;
        this.castDuration = CAST_DURATION_MS;

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
        double missingHP = 0.0;
        AttributeInstance targetAttr = target.getAttribute(MAX_HEALTH);
        if (targetAttr != null) {
            missingHP = targetAttr.getValue() - target.getHealth();
            target.setHealth(Math.max(0.0, target.getHealth() - DRAIN_DAMAGE));

            // Reduce max health by the standard 1 heart plus whatever the target was already missing
            double totalMaxReduction = TARGET_MAX_HEALTH_LOSS + missingHP;
            double newBaseValue = Math.max(2.0, targetAttr.getBaseValue() - totalMaxReduction);
            double actualReduction = targetAttr.getBaseValue() - newBaseValue;
            targetAttr.setBaseValue(newBaseValue);
            double existingDrain = target.getPersistentDataContainer()
                    .getOrDefault(TARGET_DRAIN_KEY, PersistentDataType.DOUBLE, 0.0);
            target.getPersistentDataContainer()
                    .set(TARGET_DRAIN_KEY, PersistentDataType.DOUBLE, existingDrain + actualReduction);
            // Clamp current health to the new computed maximum
            if (target.getHealth() > targetAttr.getValue()) {
                target.setHealth(targetAttr.getValue());
            }
        }

        AttributeInstance casterAttr = player.getAttribute(MAX_HEALTH);
        if (casterAttr != null) {
            casterAttr.setBaseValue(casterAttr.getBaseValue() + CASTER_MAX_HEALTH_GAIN);
            double existingGain = player.getPersistentDataContainer()
                    .getOrDefault(CASTER_GAIN_KEY, PersistentDataType.DOUBLE, 0.0);
            player.getPersistentDataContainer()
                    .set(CASTER_GAIN_KEY, PersistentDataType.DOUBLE, existingGain + CASTER_MAX_HEALTH_GAIN);
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
        if (drain == null) {
            return;
        }
        target.getPersistentDataContainer().remove(TARGET_DRAIN_KEY);
        AttributeInstance attr = target.getAttribute(MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(attr.getBaseValue() + drain);
        }
    }

    public static void resetCasterGain(final Player caster) {
        Double gain = caster.getPersistentDataContainer().get(CASTER_GAIN_KEY, PersistentDataType.DOUBLE);
        if (gain == null) {
            return;
        }
        caster.getPersistentDataContainer().remove(CASTER_GAIN_KEY);
        AttributeInstance attr = caster.getAttribute(MAX_HEALTH);
        if (attr != null) {
            // Ensure base health never drops below 1 heart from floating-point imprecision
            attr.setBaseValue(Math.max(2.0, attr.getBaseValue() - gain));
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

            if (entity.getBoundingBox().expand(TARGET_HIT_RADIUS)
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
