package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.DiscordUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Prevents ender chests from being opened in Creative mode, and in the creative and arena worlds.
 */
public class EnderChestOpenPrevent {
	public void execute(PlayerInteractEvent e) {
		if (e.getClickedBlock().getType() == Material.ENDER_CHEST) {
			// Creative mode items must never reach the ender chest, so it cannot be opened in Creative mode by anyone
			if (e.getPlayer().getGameMode() == GameMode.CREATIVE) {
				e.setCancelled(true);
				if (e.getHand() == EquipmentSlot.HAND) {
					e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "access.cannot_ender_creative")));
				}
				return;
			}
			// The ender chest is shared by every world, so it cannot be opened in a world with another inventory
			String worldName = e.getPlayer().getWorld().getName();
			String inventoryGroup = AranarthUtils.getInventoryGroup(worldName);
			if (!inventoryGroup.equals("world")) {
				if (!e.getPlayer().isOp()) {
					// Both hands are cancelled so the off hand cannot be used to open it, but the player is only notified once
					e.setCancelled(true);
					if (e.getHand() == EquipmentSlot.HAND) {
						if (inventoryGroup.equals("creative")) {
							e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "access.cannot_ender_creative")));
						} else {
							e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "dominion.must_be_gameplay_world")));
						}
						DiscordUtils.createNotification(e.getPlayer().getName() + " tried to open an enderchest in " + worldName, e.getPlayer().getUniqueId());
					}
				}
			}
		}
	}
}
