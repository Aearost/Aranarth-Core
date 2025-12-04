package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;

/**
 * Handles toggling the bulk transaction variable.
 */
public class ShopBulkTransaction {
	public void execute(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Shop shop = ShopUtils.getShopFromLocation(e.getClickedBlock().getLocation());

		// If they click elsewhere after enabling the bulk transaction
		if (aranarthPlayer.getBulkTransactionNum() == 1 && (shop == null || !player.isSneaking())) {
			aranarthPlayer.setBulkTransactionNum(0);
			player.sendMessage(ChatUtils.chatMessage("&7You have disabled the bulk transaction mode"));
		}
		// If they are enabling the bulk transaction mode
		else if (aranarthPlayer.getBulkTransactionNum() == 0 && shop != null && player.isSneaking()) {
			String itemName = "";
			if (shop.getItem().hasItemMeta()) {
				ItemMeta meta = shop.getItem().getItemMeta();
				if (meta.hasDisplayName()) {
					itemName = meta.getDisplayName();
				} else {
					itemName = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
				}
			} else {
				itemName = ChatUtils.getFormattedItemName(shop.getItem().getType().name());
			}

			DecimalFormat df = new DecimalFormat("0.00");
			String saleOrPurchase = "";
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Shop bulkShop = ShopUtils.getBulkShop(shop, player, true);
				if (bulkShop.getQuantity() == shop.getQuantity()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot make a bulk purchase of this item"));
					return;
				}
				saleOrPurchase = "purchase";
				player.sendMessage(ChatUtils.chatMessage("&7Would you like to purchase &e" + bulkShop.getQuantity() + " " + itemName + " &7for &e$" + df.format(bulkShop.getBuyPrice()) + "?"));
			} else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
				Shop bulkShop = ShopUtils.getBulkShop(shop, player, false);
				if (bulkShop.getQuantity() == shop.getQuantity()) {
					player.sendMessage(ChatUtils.chatMessage("&cYou cannot make a bulk sale of this item"));
					return;
				}
				saleOrPurchase = "sale";
				player.sendMessage(ChatUtils.chatMessage("&7Would you like to sell &e" + bulkShop.getQuantity() + " " + itemName + " &7for &e$" + df.format(bulkShop.getSellPrice()) + "?"));
			}
			player.sendMessage(ChatUtils.chatMessage("&eClick again &7to &econfirm &7your bulk " + saleOrPurchase));
			aranarthPlayer.setBulkTransactionNum(1);
		}
		// If they just made a purchase, do not display the message
		else {
			aranarthPlayer.setBulkTransactionNum(0);
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
	}
}
