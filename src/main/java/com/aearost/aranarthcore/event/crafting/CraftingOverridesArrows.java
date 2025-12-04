package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW;
import static com.aearost.aranarthcore.objects.CustomItemKeys.ARROW_HEAD;

/**
 * Handles the overrides when crafting involving God Apple Fragments.
 */
public class CraftingOverridesArrows {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        ItemMeta meta = is.getItemMeta();
        ItemStack result = e.getRecipe().getResult();

        // Custom arrows and arrowheads are only to be created and used in a Fletching Table
        if (meta == null || (!meta.getPersistentDataContainer().has(ARROW) || !meta.getPersistentDataContainer().has(ARROW_HEAD))) {
            if (is.getType() == Material.FEATHER || is.getType() == Material.STICK) {
                return;
            }

            if (is.getType() == Material.FLINT) {
                player.sendMessage(ChatUtils.chatMessage("&cYou must craft an arrowhead in a Fletching Table!"));
                e.setCancelled(true);
                return;
            }

            player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this item as an ingredient!"));
            e.setCancelled(true);
        }
    }
}
