package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomItemKeys.HOMEPAD;

/**
 * Handles the overrides when crafting involving Homepads.
 */
public class CraftingOverridesHomepad {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        if (!player.getLocation().getWorld().getName().startsWith("smp")) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot craft a homepad in this world!"));
            return;
        } else {
            ItemMeta meta = is.getItemMeta();
            if (is.getType() == Material.HEAVY_WEIGHTED_PRESSURE_PLATE) {
                if (meta.getPersistentDataContainer().has(HOMEPAD)) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou cannot use a Homepad to craft this!"));
                    e.setCancelled(true);
                }
            }
        }
    }

}
