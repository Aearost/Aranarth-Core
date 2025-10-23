package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.NumberFormat;

/**
 * Handles logic for clicking in the Rank-Up GUI
 */
public class GuiRankupClick {
	public void execute(InventoryClickEvent e) {
		// If the user did not click a slot
		if (e.getClickedInventory() == null) {
			return;
		}

		e.setCancelled(true);

		int slot = e.getSlot();
		// Rankup
		if (slot == 14) {
			Player player = (Player) e.getWhoClicked();
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

			double balance = aranarthPlayer.getBalance();
			String clickedItem = e.getClickedInventory().getItem(slot).getItemMeta().getDisplayName();
			String[] parts = clickedItem.split(" ");

			String priceWithoutDollarSign = ChatUtils.stripColorFormatting(parts[parts.length - 1]).substring(1);
			String priceWithoutCommas = priceWithoutDollarSign.replaceAll(",", "");
			double price = Double.parseDouble(priceWithoutCommas);

			if (balance >= price) {
				String rankDisplay = clickedItem.split(" ")[2];
				String aOrAn = "a";

				NumberFormat formatter = NumberFormat.getCurrencyInstance();
//					PersistenceUtils.logTransaction(player.getName() + " (" + formatter.format(balance) + ") spent "
//							+ price + " and has ranked up to " + rankDisplay);
				aranarthPlayer.setBalance(balance - price);
				aranarthPlayer.setRank(aranarthPlayer.getRank() + 1);

//					ChatUtils.updatePlayerGroupsAndPrefix(player);
				if (ChatUtils.stripColorFormatting(rankDisplay).equals("Esquire")
						|| ChatUtils.stripColorFormatting(rankDisplay).equals("Emperor")) {
					aOrAn = "an";
				}

				Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + player.getName() + " &7has become " + aOrAn + " " + rankDisplay + "&7!"));
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				PermissionUtils.evaluatePlayerPermissions(player);
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
				player.closeInventory();
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have enough money to rankup!"));
				player.closeInventory();
			}
		}
		// Cancel
		else if (slot == 12) {
			Player player = (Player) e.getWhoClicked();
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.8F, 0.5F);
			e.getWhoClicked().closeInventory();
		}
	}
}
