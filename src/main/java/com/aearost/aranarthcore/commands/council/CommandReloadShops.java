package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MarketUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Refreshes all server shop signs to reflect their current dynamic sell prices.
 */
public class CommandReloadShops {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		List<Shop> serverShops = ShopUtils.getShops().get(null);
		if (serverShops == null || serverShops.isEmpty()) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("reloadshops.no_shops")));
			return false;
		}

		int updated = 0;
		for (Shop shop : serverShops) {
			MarketUtils.refreshServerShopSign(shop);
			updated++;
		}

		sender.sendMessage(ChatUtils.chatMessage(Lang.get("reloadshops.refreshed", "count", String.valueOf(updated))));

		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishMarketUpdate();
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("shop.smp_signs_refresh")));
		}

		return true;
	}
}
