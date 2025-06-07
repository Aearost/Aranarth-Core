package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.items.SugarcaneBlock;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving Sugarcane Blocks.
 */
public class CraftingOverridesSugarcaneBlock {

    public void preCraft(PrepareItemCraftEvent e) {
        int nullCounter = 0;
        int sugarcaneBlockCounter = 0;
        for (ItemStack is : e.getInventory().getMatrix()) {

            if (is == null) {
                nullCounter++;
                continue;
            }

            if (is.isSimilar(new SugarcaneBlock().getItem())) {
                sugarcaneBlockCounter++;
            }
        }

        if (nullCounter == 8 && sugarcaneBlockCounter == 1) {
            e.getInventory().setResult(new ItemStack(Material.SUGAR_CANE, 9));
        }
    }

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        boolean isHasLore = is.getItemMeta().hasLore();

        // If a Sugarcane Block is used in place of a regular Sugarcane or Bamboo
        if (isHasLore) {
            if (e.getRecipe().getResult().getType() != Material.SUGAR_CANE &&
                    e.getRecipe().getResult().getType() != Material.BAMBOO) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Sugarcane Block to craft this!"));
                return;
            }
        }
        // If a Sugarcane or Bamboo is used in place of a Sugarcane Block
        else {
            if (e.getRecipe().getResult().getType() == Material.SUGAR_CANE) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou must use a Sugarcane Block to craft this!"));
                return;
            }
        }
    }

}
