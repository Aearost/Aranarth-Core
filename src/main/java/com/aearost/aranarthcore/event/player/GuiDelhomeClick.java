package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the deletion logic for homes.
 */
public class GuiDelhomeClick {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Delete Home")) {
			// If the user did not click a slot
			if (e.getClickedInventory() == null) {
				return;
			}

			e.setCancelled(true);

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
							AranarthUtils.deletePlayerHome(player, ChatUtils.stripColorFormatting(home.getName()));
							player.sendMessage(ChatUtils.chatMessage("&7You have deleted the home &e" + home.getName()));
							player.closeInventory();
							return;
						}
					}
					player.closeInventory();
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with deleting that home..."));
				}
			}
		}
	}
}
