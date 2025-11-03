package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Prevents players from adding non-arrow items to the arrows inventory.
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
						Home home = aranarthPlayer.getHomes().get(i);
						aranarthPlayer.setLastKnownTeleportLocation(player.getLocation());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						player.teleport(home.getLocation());
						player.sendMessage(ChatUtils.chatMessage("&7You have teleported to &e" + home.getHomeName()));
						player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
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
