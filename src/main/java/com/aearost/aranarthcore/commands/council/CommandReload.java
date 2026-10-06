package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CouponUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MarketUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Reloads config.yml, coupons, server shop signs, and permissions on both servers.
 */
public class CommandReload {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// /ac reload
		AranarthCore plugin = AranarthCore.getInstance();
		plugin.reloadConfig();
		sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.config")));

		// Server shops only exist on Survival
		if (!AranarthCore.isSmpServer()) {
			List<Shop> serverShops = ShopUtils.getShops().get(null);
			int updated = 0;
			if (serverShops != null) {
				for (Shop shop : serverShops) {
					MarketUtils.refreshServerShopSign(shop);
					updated++;
				}
			}
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.shops", "count", String.valueOf(updated))));
		}

		int count = 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			PermissionUtils.evaluatePlayerPermissions(player);
			count++;
		}
		sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.perms", "count", String.valueOf(count))));
		sender.sendMessage(ChatUtils.chatMessage(Lang.get("admin.offline_perms")));

		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishConfigReload();
			NetworkManager.getInstance().publishMarketUpdate();
			NetworkManager.getInstance().publishPermReload(null);
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.other_server")));
		}

		// The coupon import may hit the database, so it runs off the main thread
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			int[] result = CouponUtils.importCoupons();
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (result == null) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.coupons_failed")));
				} else {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("reload.coupons",
							"added", String.valueOf(result[0]),
							"skipped", String.valueOf(result[1]),
							"invalid", String.valueOf(result[2]))));
				}
			});
		});
		return true;
	}
}
