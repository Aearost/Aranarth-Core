package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving a Chorus Diamond.
 */
public class CraftingOverridesChorusDiamond {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();

        // If a Chorus Diamond is used in place of a regular Diamond
        if (isHasLore) {
            if (e.getRecipe().getResult().getType() != Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Chorus Diamond to craft this!"));
                return;
            }
        }
        // If a regular Diamond is used in place of a Chorus Diamond
        else {
            if (e.getRecipe().getResult().getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou must use a Chorus Diamond to craft this!"));
                return;
            }
        }
    }

}
