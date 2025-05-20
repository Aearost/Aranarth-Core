package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving Honey Glazed Ham.
 */
public class CraftingOverridesHoneyGlazedHam {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();

        // If a Honey Glazed Ham is used in place of a regular Cooked Porkchop
        if (isHasLore) {
            if (e.getRecipe().getResult().getType() == Material.COOKED_PORKCHOP) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use Honey Glazed Ham to craft this!"));
                return;
            }
        }
        // If a Cooked Porkchop is used in place of a Honey Glazed Ham
        else {
            if (e.getRecipe().getResult().getType() != Material.COOKED_PORKCHOP) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou must use a regular Cooked Porkchop to craft this!"));
                return;
            }
        }
    }

}
