package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Detects when a player right-clicks a brew recipe page to unlock the recipe.
 */
public class BrewRecipeUnlockListener implements Listener {

    public BrewRecipeUnlockListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        switch (e.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FILLED_MAP) {
            return;
        }
        if (!item.hasItemMeta()) {
            return;
        }

        String recipeId = item.getItemMeta()
                .getPersistentDataContainer()
                .get(CustomKeys.BREW_RECIPE, PersistentDataType.STRING);
        if (recipeId == null) {
            return;
        }

        e.setCancelled(true);

        Player player = e.getPlayer();
        BrewRecipe recipe = BrewRecipe.fromId(recipeId);
        if (recipe == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("brew.invalid_page")));
            return;
        }

        if (BrewRecipeUtils.isUnlocked(player.getUniqueId(), recipe)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("brew.already_unlocked", "name", recipe.getDisplayName())));
            return;
        }

        // Consume the item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        BrewRecipeUtils.unlock(player.getUniqueId(), recipeId);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("brew.recipe_unlocked", "recipe", recipe.getDisplayName())));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("brew.use_brewbook")));
    }
}
