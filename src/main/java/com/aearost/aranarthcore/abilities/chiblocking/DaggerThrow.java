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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class DaggerThrow extends ChiAbility implements AddonAbility {

    private static final double DAMAGE = 1.0;
    private static final int MAX_SHOTS = 5;
    private static final long SHOT_COOLDOWN_MS = 100L;
    private static final long WINDOW_MS = 500L;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown = 3000L;

    private final List<AbstractArrow> arrows = new ArrayList<>();
    private int shots = 0;
    private long endTime;

    public DaggerThrow(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        // If an active instance exists, route the click to it for a follow-up throw
        if (hasAbility(player, DaggerThrow.class)) {
            final DaggerThrow dt = getAbility(player, DaggerThrow.class);
            if (!dt.bPlayer.isOnCooldown("DaggerThrowShot") && dt.shots < MAX_SHOTS) {
                dt.shootArrow();
            }
            return;
        }

        if (bPlayer.isOnCooldown(this)) {
            return;
        }

        start();
        if (!isRemoved()) {
            shootArrow();
        }
    }

    private void shootArrow() {
        final ItemStack template = consumeArrow();
        if (template == null) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        shots++;
        endTime = System.currentTimeMillis() + WINDOW_MS;
        bPlayer.addCooldown("DaggerThrowShot", SHOT_COOLDOWN_MS);

        final Location eyeLoc = player.getEyeLocation();
        final Vector velocity = eyeLoc.getDirection().normalize().multiply(4.0);

        final AbstractArrow arrow = launchTypedArrow(template);
        arrow.setVelocity(velocity);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.ALLOWED);
        arrow.setMetadata("daggerthrow", new FixedMetadataValue(AranarthCore.getInstance(), DAMAGE));

        final String arrowType = template.getItemMeta() != null
                ? template.getItemMeta().getPersistentDataContainer().get(CustomKeys.ARROW, PersistentDataType.STRING)
                : null;
        if (arrowType != null) {
            arrow.getPersistentDataContainer().set(CustomKeys.ARROW, PersistentDataType.STRING, arrowType);
        }

        arrows.add(arrow);

        final float pitch = 1.5F + ThreadLocalRandom.current().nextFloat() * 0.5F;
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_BREEZE_LAND, 1.0F, pitch);
    }

    /**
     * Consumes one arrow from the player's quiver (survival worlds) or inventory.
     *
     * @return a clone of the consumed arrow (amount=1), or {@code null} if none found.
     */
    private ItemStack consumeArrow() {
        if (AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            final AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            final List<ItemStack> quiverArrows = ap.getArrows();
            if (quiverArrows != null) {
                for (final ItemStack item : quiverArrows) {
                    if (item != null && item.getAmount() > 0 && isArrowMaterial(item.getType())) {
                        final ItemStack template = item.clone();
                        template.setAmount(1);
                        item.setAmount(item.getAmount() - 1);
                        quiverArrows.removeIf(i -> i == null || i.getAmount() <= 0);
                        ap.setArrows(quiverArrows);
                        AranarthUtils.setPlayer(player.getUniqueId(), ap);
                        return template;
                    }
                }
            }
        }

        final ItemStack[] contents = player.getInventory().getStorageContents();
        for (final ItemStack item : contents) {
            if (item != null && isArrowMaterial(item.getType())) {
                final ItemStack template = item.clone();
                template.setAmount(1);
                item.setAmount(item.getAmount() - 1);
                player.getInventory().setStorageContents(contents);
                return template;
            }
        }

        return null;
    }

    private boolean isArrowMaterial(final Material material) {
        return material == Material.ARROW
                || material == Material.TIPPED_ARROW
                || material == Material.SPECTRAL_ARROW;
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
     * Cancels vanilla arrow damage and applies DaggerThrow damage through ProjectKorra's pipeline.
     *
     * @param entity The living entity struck by the arrow.
     * @param arrow  The DaggerThrow arrow projectile.
     */
    public void damageEntityFromArrow(final LivingEntity entity, final AbstractArrow arrow) {
        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }
        if (RegionProtection.isRegionProtected(shooter, arrow.getLocation(), "DaggerThrow")) {
            return;
        }

        entity.setNoDamageTicks(0);
        DamageHandler.damageEntity(entity, DAMAGE, this);
        arrow.remove();
        arrows.remove(arrow);
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (System.currentTimeMillis() > endTime || shots >= MAX_SHOTS) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        arrows.removeIf(arrow -> !arrow.isValid() || arrow.getTicksLived() > 200);
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
    public List<Location> getLocations() {
        return arrows.stream().map(AbstractArrow::getLocation).collect(Collectors.toList());
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
        return "DaggerThrow";
    }

    @Override
    public String getDescription() {
        return "Left-click in rapid succession to shoot daggers (arrows) from your inventory at your target. "
                + "The arrows can be sourced either from your inventory, or from your Quiver.\n"
                + ChatUtils.translateToColor("&fUsage: Left-click (up to 5 times)");
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
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
