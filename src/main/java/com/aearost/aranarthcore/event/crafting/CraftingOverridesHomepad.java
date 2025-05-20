package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the overrides when crafting involving Homepads.
 */
public class CraftingOverridesHomepad {

    public void onCraft(CraftItemEvent e, ItemStack is, HumanEntity player) {
        if (!player.getLocation().getWorld().getName().startsWith("world")) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot craft a homepad in this world!"));
            return;
        }
    }

}
