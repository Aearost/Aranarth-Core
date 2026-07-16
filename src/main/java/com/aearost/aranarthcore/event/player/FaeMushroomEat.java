package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.event.listener.misc.PotionEffectListener;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Allows Fae Aranarthium wearers to eat mushrooms for type-specific buffs.
 */
public class FaeMushroomEat {

    public void execute(PlayerInteractEvent e) {
        // Only right-click actions
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // Only main hand to avoid double-fire
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = e.getPlayer();
        if (!AranarthUtils.isWearingArmorType(player, "fae")) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        Material type = item.getType();

        if (type != Material.BROWN_MUSHROOM && type != Material.RED_MUSHROOM
                && type != Material.CRIMSON_FUNGUS && type != Material.WARPED_FUNGUS) {
            return;
        }

        // Only eat if not full hunger
        if (player.getFoodLevel() >= 20 && player.getSaturation() >= 5.0f) {
            return;
        }

        e.setCancelled(true);

        // Consume one mushroom
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Restore a small amount of hunger
        int newFood = Math.min(player.getFoodLevel() + 3, 20);
        player.setFoodLevel(newFood);
        player.setSaturation(Math.min(player.getSaturation() + 1.5f, 20.0f));

        // Play eating sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.8f, 1.0f + (float)(Math.random() * 0.2 - 0.1));

        // Apply mushroom-specific buffs
        switch (type) {
            case BROWN_MUSHROOM ->
                applyStacking(player, PotionEffectType.HASTE, 1200, 1);
            case RED_MUSHROOM ->
                {
                    applyStacking(player, PotionEffectType.STRENGTH, 800, 0);
                    applyStacking(player, PotionEffectType.REGENERATION, 400, 0);
                }
            case CRIMSON_FUNGUS ->
                {
                    applyStacking(player, PotionEffectType.FIRE_RESISTANCE, 1600, 0);
                    applyStacking(player, PotionEffectType.ABSORPTION, 1200, 0);
                }
            case WARPED_FUNGUS ->
                {
                    applyStacking(player, PotionEffectType.NIGHT_VISION, 2400, 0);
                    applyStacking(player, PotionEffectType.INVISIBILITY, 1200, 0);
                }
            default -> {}
        }
    }

    /**
     * Applies a potion effect that stacks amplifier with each successive application.
     */
    private void applyStacking(Player player, PotionEffectType type, int baseDuration, int baseAmplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        int newAmplifier = (existing != null) ? existing.getAmplifier() + 1 : baseAmplifier;
        newAmplifier = PotionEffectListener.determineEffectAmplifierRestriction(newAmplifier, type, player);
        player.addPotionEffect(new PotionEffect(type, baseDuration, newAmplifier), true);
    }
}
