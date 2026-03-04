package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiVoteShopPurchase;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the teleport logic for homes.
 */
public class GuiVoteShopClick {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		if (e.getWhoClicked() instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			// Clicked the shop and not their inventory
			if (e.getClickedInventory().getType() == InventoryType.CHEST) {
				e.setCancelled(true);

				ItemStack clicked = e.getClickedInventory().getItem(e.getSlot());
				// Ensures the player is actually clicking a home
				if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE
					|| clicked.getType() == Material.PLAYER_HEAD) {
					return;
				} else if (clicked.getType() == Material.BARRIER) {
					player.closeInventory();
					return;
				}

				String pointsAsString = clicked.getItemMeta().getLore().get(0).split(" ")[0];
				int requiredPoints = Integer.parseInt(ChatUtils.stripColorFormatting(pointsAsString));
				if (aranarthPlayer.getVotePoints() >= requiredPoints) {
					GuiVoteShopPurchase gui = new GuiVoteShopPurchase(player, clicked);
					gui.openGui();
				} else {
					player.closeInventory();
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough vote points to purchase this!"));
				}
			}
		}
	}

}
