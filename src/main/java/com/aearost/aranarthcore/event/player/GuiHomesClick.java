package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.IOException;

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

				for (int i = 0; i < aranarthPlayer.getHomes().size(); i++) {
					if (e.getSlot() == i) {
						// Teleports you to the survival world spawn
						try {
							AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
						} catch (IOException ex) {
							player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
							return;
						}

						Home home = aranarthPlayer.getHomes().get(i);
						aranarthPlayer.setLastKnownTeleportLocation(player.getLocation());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						player.teleport(home.getLocation());
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getName()));
						AranarthUtils.playTeleportSound(player);
						player.closeInventory();
						return;
					}
				}
				player.closeInventory();
				player.sendMessage(ChatUtils.chatMessage("&cSomething with wrong with teleporting to that home..."));
			}
		}
	}

}
