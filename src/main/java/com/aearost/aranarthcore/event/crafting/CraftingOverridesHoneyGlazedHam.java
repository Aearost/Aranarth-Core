package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.items.CustomItemKeys.HONEY_GLAZED_HAM;

/**
 * Handles the overrides when crafting involving Honey Glazed Ham.
 */
public class CraftingOverridesHoneyGlazedHam {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        ItemMeta meta = is.getItemMeta();
        if (e.getRecipe().getResult().hasItemMeta()) {
            ItemMeta resultMeta = e.getRecipe().getResult().getItemMeta();
            if (is.getType() == Material.COOKED_PORKCHOP) {
                if (meta.getPersistentDataContainer().has(HONEY_GLAZED_HAM)) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Honey Glazed Ham to craft this!"));
                    e.setCancelled(true);
                }
            }
        }
        // Result has no meta, therefore not requiring Honey Glazed Ham
        else {
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Honey Glazed Ham to craft this!"));
            e.setCancelled(true);
        }
    }
}
