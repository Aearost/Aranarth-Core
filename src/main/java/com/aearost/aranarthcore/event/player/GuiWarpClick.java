package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
						aranarthPlayer.setLastKnownTeleportLocation(player.getLocation());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

						player.teleport(warp.getLocation());
						player.sendMessage(ChatUtils.chatMessage("&7You have warped to &e" + warp.getName()));
						AranarthUtils.playTeleportSound(player);
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
