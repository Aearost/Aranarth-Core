package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Prevents ender chests from being opened in creative.
 */
public class EnderChestOpenPrevent {
	public void execute(PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND) {
			if (e.getClickedBlock().getType() == Material.ENDER_CHEST) {
				if (e.getPlayer().getWorld().getName().equalsIgnoreCase("creative")) {
					if (!e.getPlayer().isOp()) {
						e.setCancelled(true);
						e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot open an enderchest in creative!"));
						for (int i = 0; i < 100; i++) {
							Bukkit.getLogger().info(e.getPlayer().getName() + " tried to open an enderchest in creative!");
						}
					}
				}
			}
		}
	}
}
