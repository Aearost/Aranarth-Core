package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomItemKeys.SUGARCANE_BLOCK;

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
            if (is.hasItemMeta()) {
                ItemMeta meta = is.getItemMeta();
                if (meta.getPersistentDataContainer().has(SUGARCANE_BLOCK)) {
                    sugarcaneBlockCounter++;
                }
            }
        }

        if (nullCounter == e.getInventory().getMatrix().length - 1 && sugarcaneBlockCounter == 1) {
            e.getInventory().setResult(new ItemStack(Material.SUGAR_CANE, 9));
        }
    }

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        if (is.getType() == Material.SUGAR_CANE) {
            return;
        }

        ItemStack result = e.getRecipe().getResult();
        if (is.hasItemMeta()) {
            ItemMeta meta = is.getItemMeta();
            if (meta.getPersistentDataContainer().has(SUGARCANE_BLOCK)) {
                // preCraft does not actually update it to sugarcane in the backend
                if (result.getType() != Material.BAMBOO) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Block of Sugarcane to craft this!"));
                    Bukkit.getLogger().info(result.getType().name());
                    e.setCancelled(true);
                }
            }
        }
    }
}
