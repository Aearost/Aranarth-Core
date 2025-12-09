package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Handles the teleport logic for warps.
 */
public class GuiWarpClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Warps")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			List<Home> warps = AranarthUtils.getWarps();

			if (e.getWhoClicked() instanceof Player player) {
				// Ensures the player is actually clicking a home
				if (e.getSlot() >= warps.size()) {
					return;
				}

				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				for (int i = 0; i < warps.size(); i++) {
					if (e.getSlot() == i) {
						Home warp = warps.get(i);

						if (player.hasPermission("aranarth.warp.modify")) {
							Material heldItem = e.getCursor().getType();
							// If the user is trying to update the icon of a home
							if (heldItem != Material.AIR) {
								e.setCancelled(true);
								if (heldItem == warp.getIcon()) {
									player.sendMessage(ChatUtils.chatMessage("&cThis warp already uses that icon!"));
								} else {
									AranarthUtils.updateWarp(warp.getName(), warp.getLocation(), heldItem);
									player.sendMessage(ChatUtils.chatMessage(warp.getName() + "&7's icon is now &e" + ChatUtils.getFormattedItemName(heldItem.name())));
								}
								player.closeInventory();
								return;
							}
						}

						AranarthUtils.teleportPlayer(player, player.getLocation(), warp.getLocation());
						player.sendMessage(ChatUtils.chatMessage("&7You have warped to &e" + warp.getName()));
						player.closeInventory();
						return;
					}
				}
				player.closeInventory();
				player.sendMessage(ChatUtils.chatMessage("&cSomething with wrong with teleporting to that warp..."));
			}
		}
	}

}
