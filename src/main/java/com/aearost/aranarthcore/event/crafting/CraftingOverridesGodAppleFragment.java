package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving God Apple Fragments.
 */
public class CraftingOverridesGodAppleFragment {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();

        // If a God Apple Fragment is used in place of a regular Gold Nugget
        if (isHasLore) {
            if (e.getRecipe().getResult().getType() != Material.ENCHANTED_GOLDEN_APPLE) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use God Apple Fragments to craft this!"));
                return;
            }
        }
        // If a Gold Nugget is used in place of a God Apple Fragment
        else {
            if (e.getRecipe().getResult().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Gold Nugget to craft this!"));
                return;
            }
        }
    }

}
