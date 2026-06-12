package com.aearost.aranarthcore.abilities.chiblocking;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DaggerVolley extends ChiAbility implements AddonAbility {

    private static final double BASE_SPEED = 2.5;
    private static final double SPREAD_RADIUS = 0.24;
    private static final double NOISE = 0.03;
    private static final int[] ARROW_COUNTS = {3, 6, 9};
    private static final Map<UUID, Integer> STAGE_MAP = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    private final List<AbstractArrow> arrows = new ArrayList<>();

    public DaggerVolley(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }
        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        start();
        if (!isRemoved()) {
            if (fire()) {
                bPlayer.addCooldown(this);
            } else {
                remove();
            }
        }
    }

    /**
     * Determines the arrow count for this stage, consumes arrows from the quiver and inventory,
     * and launches them in a diverging circular spread.
     *
     * @return {@code true} if at least one arrow was launched; {@code false} otherwise.
     */
    private boolean fire() {
        final int stage = STAGE_MAP.getOrDefault(player.getUniqueId(), 0);
        final int arrowCount = ARROW_COUNTS[stage];

        final List<ItemStack> typeOrder = buildArrowTypeOrder();
        if (typeOrder.isEmpty()) {
            return false;
        }

        int totalAvailable = 0;
        for (final ItemStack template : typeOrder) {
            totalAvailable += countInventoryArrows(template) + countQuiverArrows(template);
        }

        final int toFire = Math.min(arrowCount, totalAvailable);
        if (toFire == 0) {
            return false;
        }

        this.cooldown = toFire * 1000L;
        STAGE_MAP.put(player.getUniqueId(), (stage + 1) % ARROW_COUNTS.length);

        // Snapshot per-arrow templates before consuming so counts are accurate
        final List<ItemStack> arrowTemplates = assignArrowTemplates(typeOrder, toFire);
        consumeArrowsMultiType(typeOrder, toFire);

        final Location eyeLoc = player.getEyeLocation();
        final Vector forward = eyeLoc.getDirection().normalize();

        // Build two orthogonal axes spanning the plane perpendicular to forward so that
        // arrows can be arranged in a circle around the firing direction
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1e-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        final Vector up = right.clone().crossProduct(forward).normalize();

        final ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < toFire; i++) {
            final ItemStack template = arrowTemplates.get(i);
            final String arrowType = template.getItemMeta() != null
                    ? template.getItemMeta().getPersistentDataContainer()
                    .get(CustomKeys.ARROW, PersistentDataType.STRING)
                    : null;

            final double arrowDamage = rng.nextBoolean() ? 2.0 : 1.0;

            // Random point inside a disk
            final double angle = rng.nextDouble(2.0 * Math.PI);
            final double radius = SPREAD_RADIUS * Math.sqrt(rng.nextDouble());
            final Vector perp = right.clone().multiply(Math.cos(angle))
                    .add(up.clone().multiply(Math.sin(angle)))
                    .multiply(radius);

            final Vector velocity = forward.clone().multiply(BASE_SPEED)
                    .add(perp)
                    .add(new Vector(
                            rng.nextDouble(-NOISE, NOISE),
                            rng.nextDouble(-NOISE, NOISE),
                            rng.nextDouble(-NOISE, NOISE)));

            final AbstractArrow arrow = launchTypedArrow(template);
            arrow.setVelocity(velocity);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED);
            arrow.setMetadata("daggervolley", new FixedMetadataValue(AranarthCore.getInstance(), arrowDamage));

            // Propagate the Aranarth arrow type so ArrowHit can apply any custom effects
            if (arrowType != null) {
                arrow.getPersistentDataContainer().set(CustomKeys.ARROW, PersistentDataType.STRING, arrowType);
            }

            arrows.add(arrow);

            final float pitch = 1.5F + rng.nextFloat() * 0.5F;
            player.getWorld().playSound(eyeLoc, Sound.ENTITY_BREEZE_LAND, 1.0F, pitch);
        }

        return true;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        // Remove arrows that have been picked up, hit an entity, or exceeded their flight time.
        // Ground arrows are intentionally kept tracked so remove() doesn't clean them up early.
        arrows.removeIf(arrow -> !arrow.isValid() || arrow.getTicksLived() > 200);
        if (arrows.isEmpty()) {
            remove();
        }
    }

    /**
     * Launches the correct arrow entity type for the consumed template and, for tipped arrows,
     * copies the potion effects from the item meta onto the entity so they apply on hit.
     */
    private AbstractArrow launchTypedArrow(final ItemStack template) {
        if (template.getType() == Material.SPECTRAL_ARROW) {
            final SpectralArrow arrow = player.launchProjectile(SpectralArrow.class);
            arrow.setItemStack(template.clone());
            return arrow;
        }
        final Arrow arrow = player.launchProjectile(Arrow.class);
        arrow.setItemStack(template.clone());
        if (template.getType() == Material.TIPPED_ARROW
                && template.getItemMeta() instanceof PotionMeta meta) {
            if (meta.getBasePotionType() != null) {
                arrow.setBasePotionType(meta.getBasePotionType());
            }
            for (final PotionEffect effect : meta.getCustomEffects()) {
                arrow.addCustomEffect(effect, true);
            }
        }
        return arrow;
    }

    /**
     * Cancels vanilla arrow damage, resets invincibility frames, and applies the rolled damage
     * value for this arrow through ProjectKorra's damage pipeline.
     *
     * @param entity The living entity struck by the arrow.
     * @param arrow  The DaggerVolley arrow projectile.
     */
    public void damageEntityFromArrow(final LivingEntity entity, final AbstractArrow arrow) {
        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }
        if (RegionProtection.isRegionProtected(shooter, arrow.getLocation(), "DaggerVolley")) {
            return;
        }

        final double arrowDamage = (double) arrow.getMetadata("daggervolley").get(0).value();
        entity.setNoDamageTicks(0);
        DamageHandler.damageEntity(entity, arrowDamage, this);
        arrow.remove();
        arrows.remove(arrow);
    }

    /**
     * Returns arrow type templates in priority order.
     */
    private List<ItemStack> buildArrowTypeOrder() {
        final List<ItemStack> order = new ArrayList<>();

        for (final ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && isArrowMaterial(item.getType())) {
                final ItemStack template = item.clone();
                template.setAmount(1);
                if (order.stream().noneMatch(t -> t.isSimilar(template))) {
                    order.add(template);
                }
            }
        }

        if (AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            final AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            final List<ItemStack> quiverArrows = ap.getArrows();
            if (quiverArrows != null) {
                for (final ItemStack item : quiverArrows) {
                    if (item != null && item.getAmount() > 0 && isArrowMaterial(item.getType())) {
                        final ItemStack template = item.clone();
                        template.setAmount(1);
                        if (order.stream().noneMatch(t -> t.isSimilar(template))) {
                            order.add(template);
                        }
                    }
                }
            }
        }

        return order;
    }

    /**
     * Counts arrows matching the template in the player's inventory.
     */
    private int countInventoryArrows(final ItemStack template) {
        int count = 0;
        for (final ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Counts arrows matching the template in the player's quiver.
     */
    private int countQuiverArrows(final ItemStack template) {
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            return 0;
        }
        final AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        final List<ItemStack> quiverArrows = ap.getArrows();
        if (quiverArrows == null) {
            return 0;
        }
        int count = 0;
        for (final ItemStack item : quiverArrows) {
            if (item != null && item.isSimilar(template)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Builds a per-arrow template list.
     */
    private List<ItemStack> assignArrowTemplates(final List<ItemStack> typeOrder, final int toFire) {
        final List<ItemStack> result = new ArrayList<>(toFire);
        int remaining = toFire;

        for (final ItemStack template : typeOrder) {
            if (remaining <= 0) {
                break;
            }
            final int fromQuiver = Math.min(countQuiverArrows(template), remaining);
            for (int i = 0; i < fromQuiver; i++) {
                result.add(template);
            }
            remaining -= fromQuiver;

            if (remaining <= 0) {
                break;
            }
            final int fromInv = Math.min(countInventoryArrows(template), remaining);
            for (int i = 0; i < fromInv; i++) {
                result.add(template);
            }
            remaining -= fromInv;
        }

        return result;
    }

    /**
     * Consumes the needed amount of arrows across all types in priority order.
     */
    private void consumeArrowsMultiType(final List<ItemStack> typeOrder, int needed) {
        final boolean survivalWorld = AranarthUtils.isSurvivalWorld(player.getWorld().getName());
        final AranarthPlayer ap = survivalWorld ? AranarthUtils.getPlayer(player.getUniqueId()) : null;
        final List<ItemStack> quiverArrows = (ap != null) ? ap.getArrows() : null;
        boolean quiverModified = false;

        final ItemStack[] contents = player.getInventory().getStorageContents();

        for (final ItemStack template : typeOrder) {
            if (needed <= 0) {
                break;
            }

            // Quiver first
            if (quiverArrows != null) {
                for (final ItemStack item : quiverArrows) {
                    if (needed <= 0) {
                        break;
                    }
                    if (item != null && item.isSimilar(template)) {
                        final int take = Math.min(item.getAmount(), needed);
                        item.setAmount(item.getAmount() - take);
                        needed -= take;
                        quiverModified = true;
                    }
                }
            }

            // Then inventory
            for (final ItemStack item : contents) {
                if (needed <= 0) {
                    break;
                }
                if (item != null && item.isSimilar(template)) {
                    final int take = Math.min(item.getAmount(), needed);
                    item.setAmount(item.getAmount() - take);
                    needed -= take;
                }
            }
        }

        player.getInventory().setStorageContents(contents);

        if (quiverModified && ap != null && quiverArrows != null) {
            quiverArrows.removeIf(item -> item == null || item.getAmount() <= 0);
            ap.setArrows(quiverArrows);
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
        }
    }

    private boolean isArrowMaterial(final Material material) {
        return material == Material.ARROW
                || material == Material.TIPPED_ARROW
                || material == Material.SPECTRAL_ARROW;
    }

    public static void resetStage(final UUID uuid) {
        STAGE_MAP.remove(uuid);
    }

    @Override
    public void remove() {
        for (final AbstractArrow arrow : arrows) {
            if (arrow.isValid() && !arrow.isOnGround()) {
                arrow.remove();
            }
        }
        super.remove();
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
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "DaggerVolley";
    }

    @Override
    public String getDescription() {
        return "Throw a volley of daggers from your hand, firing 3 arrows "
                + "projectiles in a tight spread that fans out with distance; each dagger deals "
                + "either a half or a full heart of damage. Every successive use fires three more "
                + "daggers than the last (up to nine), then the cycle resets.\n"
                + ChatUtils.translateToColor("&fUsage: Left-click");
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        STAGE_MAP.clear();
    }

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
