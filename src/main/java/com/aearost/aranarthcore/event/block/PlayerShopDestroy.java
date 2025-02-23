package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class PlayerShopDestroy implements Listener {

	public PlayerShopDestroy(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the deletion of a player shop.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerShopDestroy(final BlockBreakEvent e) {
		Player player = e.getPlayer();
		int deletionResult = -1;
		Location destroyedSignLocation = null;
		Location destroyedChestLocation = null;
		// Must add support to check if sign is broken by block it is connected to
		if (isSign(e.getBlock().getType())) {
			destroyedSignLocation = e.getBlock().getLocation();
			deletionResult = deleteShopIfPossible(player, destroyedSignLocation);
		}
		else if (isChestBlock(e.getBlock().getType())) {
			destroyedChestLocation = e.getBlock().getLocation();
			destroyedSignLocation = new Location(destroyedChestLocation.getWorld(),
					destroyedChestLocation.getBlockX(),
					destroyedChestLocation.getBlockY() + 1,
					destroyedChestLocation.getBlockZ());
			if (isSign(destroyedSignLocation.getBlock().getType())) {
				deletionResult = deleteShopIfPossible(player, destroyedSignLocation);
			}
		} else {
			return;
		}

		if (deletionResult == 1) {
			player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.1F);
			// If the chest was destroyed
			if (destroyedChestLocation != null) {
				Sign sign = (Sign) destroyedSignLocation.getBlock().getState();
				sign.getSide(Side.FRONT).setLine(0, ChatUtils.translateToColor("&4&l[Shop]"));
				sign.getSide(Side.FRONT).setLine(1, "");
				sign.getSide(Side.FRONT).setLine(2, "");
				sign.getSide(Side.FRONT).setLine(3, "");
				sign.update();
			}
		} else if (deletionResult == 0){
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
			e.setCancelled(true);
		}
	}

	private boolean isSign(Material type) {
		return type.name().toLowerCase().endsWith("sign");
	}

	private boolean isChestBlock(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

	private int deleteShopIfPossible(Player player, Location destroyedSignLocation) {
		if (AranarthUtils.isShop(destroyedSignLocation)) {
			PlayerShop shop = AranarthUtils.getShop(player.getUniqueId(), destroyedSignLocation);
			// If it is the player's shop
			if (shop != null) {
				AranarthUtils.removeShop(player.getUniqueId(), destroyedSignLocation);
				return 1;
			}
			// If it is another player's shop
			else {
				return 0;
			}
		}
		return -1;
	}

}
