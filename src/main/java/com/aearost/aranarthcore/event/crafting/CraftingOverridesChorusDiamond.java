package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomKeys.CHORUS_DIAMOND;
import static com.aearost.aranarthcore.objects.CustomKeys.HOMEPAD;

/**
 * Handles the overrides when crafting involving a Chorus Diamond.
 */
public class CraftingOverridesChorusDiamond {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        ItemMeta meta = is.getItemMeta();
        if (e.getRecipe().getResult().hasItemMeta()) {
            ItemMeta resultMeta = e.getRecipe().getResult().getItemMeta();
            if (is.getType() == Material.DIAMOND) {
                if (resultMeta.getPersistentDataContainer().has(HOMEPAD)) {
                    // A normal diamond is used as an ingredient instead of a chorus diamond
                    if (!meta.getPersistentDataContainer().has(CHORUS_DIAMOND)) {
                        player.sendMessage(ChatUtils.chatMessage("&cYou must use a Chorus Diamond to craft this!"));
                        e.setCancelled(true);
                    }
                }
                // A chorus diamond is used as an ingredient incorrectly
                else {
                    if (meta.getPersistentDataContainer().has(CHORUS_DIAMOND)) {
                        player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Chorus Diamond to craft this!"));
                        e.setCancelled(true);
                    }
                }
            }
        }
        // Result has no meta, therefore not requiring Chorus Diamonds
        else {
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Chorus Diamond to craft this!"));
            e.setCancelled(true);
        }
    }

}
