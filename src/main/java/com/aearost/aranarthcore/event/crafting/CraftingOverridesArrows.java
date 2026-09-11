package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;
import static com.aearost.aranarthcore.objects.CustomKeys.ARROW_HEAD;

/**
 * Handles the overrides when crafting involving God Apple Fragments.
 */
public class CraftingOverridesArrows {

    public void onCrafterCraft(CrafterCraftEvent e, ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        if (meta == null || (!meta.getPersistentDataContainer().has(ARROW) || !meta.getPersistentDataContainer().has(ARROW_HEAD))) {
            if (is.getType() == Material.FEATHER || is.getType() == Material.STICK) {
                return;
            }
            e.setCancelled(true);
        }
    }

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        ItemMeta meta = is.getItemMeta();
        ItemStack result = e.getRecipe().getResult();

        // Custom arrows and arrowheads are only to be created and used in a Fletching Table
        if (meta == null || (!meta.getPersistentDataContainer().has(ARROW) || !meta.getPersistentDataContainer().has(ARROW_HEAD))) {
            if (is.getType() == Material.FEATHER || is.getType() == Material.STICK) {
                return;
            }

            if (is.getType() == Material.FLINT) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("crafting.arrowhead_fletching")));
                e.setCancelled(true);
                return;
            }

            player.sendMessage(ChatUtils.chatMessage(Lang.get("crafting.cannot_use_ingredient")));
            e.setCancelled(true);
        }
    }
}
