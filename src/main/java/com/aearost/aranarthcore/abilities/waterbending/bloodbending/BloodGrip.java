package com.aearost.aranarthcore.abilities.waterbending.bloodbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BloodAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BloodGrip extends BloodAbility implements AddonAbility {

    public enum Phase {CHARGING, CONTROLLING}

    private static final long CHARGE_DURATION_MS = 1000L;
    private static final long CONTROL_DURATION_MS = 5000L;
    private static final double FLING_STRENGTH = 2.8;
    private static final double MAX_FLING_H = 0.9;
    private static final double MAX_FLING_V = 1.0;
    private static final double FLING_ARMOR_DRAG = 0.35;
    private static final double DRAG_DEAD_ZONE = 1.2;
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

    private static final Map<UUID, BloodGrip> ACTIVE_INSTANCES = new HashMap<>();
    private static final Set<UUID> CONTROLLED_PLAYERS = new HashSet<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute("ControlDuration")
    private long controlDuration;

    private Phase phase;
    private long chargeStartTime;
    private long controlStartTime;
    private LivingEntity target;
    private boolean targetAiDisabled;
    private final Set<Element> suppressedElements = new HashSet<>();

    public BloodGrip(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (!isNight(player.getWorld())) {
            return;
        }

        this.cooldown = 8000L;
        this.range = 6.0;
        this.chargeDuration = CHARGE_DURATION_MS;
        this.controlDuration = CONTROL_DURATION_MS;

        LivingEntity found = findTarget();
        if (found == null) {
            return;
        }

        this.target = found;
        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.targetAiDisabled = false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.4f, 1.3f);

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
            case CONTROLLING -> progressControlling();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            finishWithCooldown();
            return;
        }

        spawnChargingParticles();

        if (System.currentTimeMillis() - chargeStartTime >= chargeDuration) {
            acquireGrip();
        }
    }

    private void acquireGrip() {
        phase = Phase.CONTROLLING;
        controlStartTime = System.currentTimeMillis();

        if (target instanceof Mob mob) {
            mob.setAI(false);
            targetAiDisabled = true;
        }

        if (target instanceof Player targetPlayer) {
            CONTROLLED_PLAYERS.add(targetPlayer.getUniqueId());
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

        spawnGripBurst();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 0.9f, 0.5f);
    }

    private void progressControlling() {
        if (!player.isSneaking()) {
            finishWithCooldown();
            return;
        }

        if (System.currentTimeMillis() - controlStartTime >= controlDuration) {
            finishWithCooldown();
            return;
        }

        if (!isNight(player.getWorld())) {
            finishWithCooldown();
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true, true), true);
        spawnControlParticles();
        dragTarget();
    }

    private void dragTarget() {
        Location victimLocation = target.getLocation();
        Location aimTarget = GeneralMethods.getTargetedLocation(player, 10);
        double gap = aimTarget.distance(victimLocation);
        if (gap > DRAG_DEAD_ZONE) {
            double speed = computeDragSpeed(target);
            if (target instanceof Mob) {
                // Mobs with AI disabled ignore setVelocity, so teleport them smoothly instead
                Vector toTarget = aimTarget.toVector().subtract(victimLocation.toVector()).normalize();
                Location newLoc = victimLocation.clone().add(toTarget.multiply(Math.min(speed, gap)));
                newLoc.setYaw(victimLocation.getYaw());
                newLoc.setPitch(victimLocation.getPitch());
                target.teleport(newLoc);
            } else {
                Vector v = GeneralMethods.getDirection(victimLocation, aimTarget);
                target.setVelocity(v.normalize().multiply(speed));
            }
        } else {
            if (!(target instanceof Mob)) {
                target.setVelocity(new Vector(0, 0, 0));
            }
        }
        target.setFallDistance(0.0F);
    }

    /**
     * Returns the drag speed for the target based on the heaviest armor tier worn.
     */
    private double computeDragSpeed(LivingEntity entity) {
        ItemStack[] pieces;
        if (entity instanceof Player p) {
            pieces = p.getInventory().getArmorContents();
        } else {
            EntityEquipment eq = entity.getEquipment();
            if (eq == null) return 0.4;
            pieces = new ItemStack[]{eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots()};
        }
        int maxTier = 0;
        for (ItemStack piece : pieces) {
            if (piece == null || piece.getType() == Material.AIR) continue;
            String name = piece.getType().name();
            int tier;
            if (name.startsWith("NETHERITE_")) {
                tier = 4;
            } else if (name.startsWith("DIAMOND_")) {
                tier = 3;
            } else if (name.startsWith("IRON_") || name.startsWith("GOLDEN_")) {
                tier = 2;
            } else if (name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_")) {
                tier = 1;
            } else {
                tier = 0;
            }
            if (tier > maxTier) maxTier = tier;
        }
        return switch (maxTier) {
            case 4 -> 0.1;
            case 3 -> 0.2;
            case 2 -> 0.3;
            case 1 -> 0.4;
            default -> 0.5;
        };
    }

    private void spawnChargingParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        double elapsed = (System.currentTimeMillis() - chargeStartTime) / 1000.0;
        int points = 8;

        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI / points) * i + elapsed * 3.0;
            double radius = 0.55;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.sin(elapsed * 4.0 + i) * 0.35;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
        }
    }

    private void spawnGripBurst() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        int burstPoints = 24;

        for (int i = 0; i < burstPoints; i++) {
            double yaw = (2.0 * Math.PI / burstPoints) * i;
            double pitch = Math.PI * (i % 3) / 3.0 - Math.PI / 2.0;
            double x = Math.cos(pitch) * Math.cos(yaw);
            double y = Math.sin(pitch);
            double z = Math.cos(pitch) * Math.sin(yaw);
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST_BRIGHT);
        }
    }

    private void spawnControlParticles() {
        Location center = target.getLocation().add(0, target.getHeight() / 2.0, 0);
        long tick = System.currentTimeMillis() / 50L;
        double baseAngle = (tick % 72) * (2.0 * Math.PI / 72.0);
        int points = 10;

        for (int i = 0; i < points; i++) {
            double angle = baseAngle + (2.0 * Math.PI / points) * i;
            double x = Math.cos(angle) * 0.6;
            double z = Math.sin(angle) * 0.6;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0, AranarthBendingUtils.BLOOD_DUST);
        }
    }

    /**
     * Returns the armor weight contribution of a single armor piece used to scale drag.
     */
    private double getArmorPieceWeight(ItemStack piece) {
        if (piece == null || piece.getType() == Material.AIR) {
            return 0;
        }
        String name = piece.getType().name();
        if (name.startsWith("NETHERITE_")) {
            return 2.0;
        }
        if (name.startsWith("DIAMOND_") || piece.getType() == Material.ELYTRA) {
            return 1.5;
        }
        if (name.startsWith("IRON_") || name.startsWith("GOLDEN_")) {
            return 1.0;
        }
        if (name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_")) {
            return 0.5;
        }
        return 0;
    }

    /** Returns the total armor weight for an entity across all four equipment slots. */
    private double getArmorWeight(LivingEntity entity) {
        double weight = 0;
        if (entity instanceof Player p) {
            for (ItemStack piece : p.getInventory().getArmorContents()) {
                weight += getArmorPieceWeight(piece);
            }
        } else {
            EntityEquipment eq = entity.getEquipment();
            if (eq != null) {
                weight += getArmorPieceWeight(eq.getHelmet());
                weight += getArmorPieceWeight(eq.getChestplate());
                weight += getArmorPieceWeight(eq.getLeggings());
                weight += getArmorPieceWeight(eq.getBoots());
            }
        }
        return weight;
    }

    /**
     * Performs a ray trace from the caster's eye outward to find the nearest valid target within range.
     */
    private LivingEntity findTarget() {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
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
            if (entity instanceof Player targetPlayer && CONTROLLED_PLAYERS.contains(targetPlayer.getUniqueId())) {
                continue;
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
     * Flings the target in the caster's current look direction with strong knockback.
     */
    public void onLeftClick() {
        if (phase != Phase.CONTROLLING) {
            return;
        }

        Vector flingDir = player.getEyeLocation().getDirection().normalize();
        Vector velocity = flingDir.multiply(FLING_STRENGTH);

        // Reduce throw caps by armor weight (heavier armor = shorter throw)
        double weight = getArmorWeight(target);
        double flingFactor = 1.0 / (1.0 + weight * FLING_ARMOR_DRAG);
        double effectiveMaxH = MAX_FLING_H * flingFactor;
        double effectiveMaxV = MAX_FLING_V * flingFactor;

        double hLen = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        if (hLen > effectiveMaxH) {
            double scale = effectiveMaxH / hLen;
            velocity.setX(velocity.getX() * scale);
            velocity.setZ(velocity.getZ() * scale);
        }
        if (velocity.getY() > effectiveMaxV) {
            velocity.setY(effectiveMaxV);
        }

        target.setVelocity(velocity);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);

        finishWithCooldown();
    }

    /**
     * Ends the ability immediately with the cooldown applied.
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

    private void restoreTarget() {
        if (target == null) {
            return;
        }
        if (targetAiDisabled && target instanceof Mob mob) {
            mob.setAI(true);
            targetAiDisabled = false;
        }
        if (target instanceof Player targetPlayer) {
            CONTROLLED_PLAYERS.remove(targetPlayer.getUniqueId());
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

    public static BloodGrip getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public static boolean isControlled(final UUID targetUuid) {
        return CONTROLLED_PLAYERS.contains(targetUuid);
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
        return "BloodGrip";
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
        return "Seize control of a target's body, and control their movements as you wish. " +
                "Once gripped, the target is frozen in place and dragged wherever you look - " +
                "though heavier armor slows how quickly they can be moved.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (on target) > Drag Mouse (controlling) > Left-Click (throw)");
    }

}
