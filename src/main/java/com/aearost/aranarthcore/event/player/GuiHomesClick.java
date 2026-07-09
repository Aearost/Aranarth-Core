package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
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
				Location bedSpawn = player.getBedSpawnLocation();
				boolean hasBedSpawn = bedSpawn != null;
				int totalSlots = aranarthPlayer.getHomes().size() + (hasBedSpawn ? 1 : 0);

				// Ensures the player is actually clicking a home
				if (e.getSlot() >= totalSlots) {
					return;
				}

				if (e.getClickedInventory().getType() == InventoryType.CHEST) {
					// Bed spawn is always at slot 0 when present
					if (hasBedSpawn && e.getSlot() == 0) {
						Material heldItem = e.getCursor().getType();
						if (heldItem != Material.AIR) {
							e.setCancelled(true);
							player.sendMessage(ChatUtils.chatMessage("&cYou cannot change the icon of your bed spawn!"));
							player.closeInventory();
							return;
						}
						AranarthUtils.teleportPlayer(player, player.getLocation(), bedSpawn, aranarthPlayer.isInAdminMode(), success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to your &eBed Spawn"));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to your &eBed Spawn"));
							}
						});
						player.closeInventory();
						return;
					}

					int homeIndex = hasBedSpawn ? e.getSlot() - 1 : e.getSlot();
					Home home = aranarthPlayer.getHomes().get(homeIndex);

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
						AranarthUtils.teleportPlayer(player, player.getLocation(), home.getLocation(), aranarthPlayer.isInAdminMode(), success -> {
							if (success) {
								player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getName()));
							} else {
								player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to &e" + home.getName()));
							}
						});
						player.closeInventory();
						return;
					}
				}
			}
		}
	}

}
