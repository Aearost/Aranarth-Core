package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the teleport logic for homes.
 */
public class GuiHomesClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Your Homes")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			if (e.getWhoClicked() instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

				// Ensures the player is actually clicking a home
				if (e.getSlot() >= aranarthPlayer.getHomes().size()) {
					return;
				}

				if (e.getClickedInventory().getType() == InventoryType.CHEST) {
					for (int i = 0; i < aranarthPlayer.getHomes().size(); i++) {
						if (e.getSlot() == i) {
							Home home = aranarthPlayer.getHomes().get(i);

							Material heldItem = e.getCursor().getType();
							// If the user is trying to update the icon of a home
							if (heldItem != Material.AIR) {
								e.setCancelled(true);
								if (heldItem == home.getIcon()) {
									player.sendMessage(ChatUtils.chatMessage("&cThis home already uses that icon!"));
								} else {
									AranarthUtils.updateHome(player, home.getName(), home.getLocation(), heldItem);
									player.sendMessage(ChatUtils.chatMessage("&e" + home.getName() + "&7's icon is now &e" + ChatUtils.getFormattedItemName(heldItem.name())));
								}
								player.closeInventory();
								return;
							} else {
								AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation());
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getName()));
								player.closeInventory();
								return;
							}
						}
					}
					player.closeInventory();
					player.sendMessage(ChatUtils.chatMessage("&cSomething with wrong with teleporting to that home..."));
				}
			}
		}
	}

}
