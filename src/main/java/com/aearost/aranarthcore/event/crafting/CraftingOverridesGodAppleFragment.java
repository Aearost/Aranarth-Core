package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomKeys.GOD_APPLE_FRAGMENT;

/**
 * Handles the overrides when crafting involving God Apple Fragments.
 */
public class CraftingOverridesGodAppleFragment {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        ItemMeta meta = is.getItemMeta();
        ItemStack result = e.getRecipe().getResult();
        if (is.getType() == Material.GOLD_NUGGET) {
            if (result.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
                // A normal gold nugget is used as an ingredient instead of a god apple fragment
                if (meta == null || !meta.getPersistentDataContainer().has(GOD_APPLE_FRAGMENT)) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must use a God Apple Fragment to craft this!"));
                    e.setCancelled(true);
                }
            }
            // A god apple fragment is used as an ingredient incorrectly
            else {
                if (meta.getPersistentDataContainer().has(GOD_APPLE_FRAGMENT)) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a God Apple Fragment to craft this!"));
                    e.setCancelled(true);
                }
            }
        }
    }
}
