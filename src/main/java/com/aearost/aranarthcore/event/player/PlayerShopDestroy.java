package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

		// Prevents breaking a block that has a shop on it
		if (isRelativeBlockShop(e.getBlock())) {
			player.sendMessage(ChatUtils.chatMessage("&cPlease destroy the shop on its own"));
			e.setCancelled(true);
			return;
		}

		int deletionResult = -1;
		Location destroyedSignLocation = null;
		Location destroyedChestLocation = null;

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
				sign.getSide(Side.FRONT).setLine(0, "");
				sign.getSide(Side.FRONT).setLine(1, "");
				sign.getSide(Side.FRONT).setLine(2, "");
				sign.getSide(Side.FRONT).setLine(3, "");
				sign.update();
			}
		} else if (deletionResult == 0){
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy this shop!"));
			e.setCancelled(true);
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with deleting this shop..."));
			e.setCancelled(true);
		}
	}

	/**
	 * Determines if the surrounding blocks are shops.
	 * @return Confirmation of whether one of the relative blocks are shops.
	 */
	private boolean isRelativeBlockShop(Block block) {
		Block up = block.getRelative(BlockFace.UP);
		Block down = block.getRelative(BlockFace.DOWN);
		Block north = block.getRelative(BlockFace.NORTH);
		Block east = block.getRelative(BlockFace.EAST);
		Block south = block.getRelative(BlockFace.SOUTH);
		Block west = block.getRelative(BlockFace.WEST);

		Block[] relativeBlocks = new Block[] { up, down, north, east, south, west};
		for (Block relativeBlock : relativeBlocks) {
			if (relativeBlock.getType().name().toLowerCase().endsWith("sign")) {
				if (AranarthUtils.getShop(relativeBlock.getLocation()) != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if the clicked block is a sign.
	 * @param type The type of material.
	 * @return Confirmation of whether the block is a sign.
	 */
	private boolean isSign(Material type) {
		return type.name().toLowerCase().endsWith("sign");
	}

	/**
	 * Determines if the clicked block is a chest, trapped chest, or barrel.
	 * @param type The type of material.
	 * @return Confirmation of whether the block is a chest, trapped chest, or barrel.
	 */
	private boolean isChestBlock(Material type) {
		return type == Material.CHEST || type == Material.TRAPPED_CHEST || type == Material.BARREL;
	}

	/**
	 * Determines if the player shop can be deleted or not.
	 * -1 if there is no shop at the sign's location.
	 * 0 if there is a shop however it does not belong to the player.
	 * 1 if the shop belongs to the player.
	 *
	 * @param player The player trying to delete the shop.
	 * @param destroyedSignLocation The location of the sign being destroyed.
	 * @return The result of whether the shop was able to be deleted.
	 */
	private int deleteShopIfPossible(Player player, Location destroyedSignLocation) {
		PlayerShop shop = AranarthUtils.getShop(destroyedSignLocation);

		if (shop == null) {
			return -1;
		}

		if (player != null) {
			// If it is the player's shop
			if (shop.getUuid() == player.getUniqueId()) {
				AranarthUtils.removeShop(player.getUniqueId(), destroyedSignLocation);
				return 1;
			}
			// If it is another player's shop
			else {
				return 0;
			}
		}
		// If it is a server shop
		else {
			if (player.getName().equals("Aearost")) {
				AranarthUtils.removeShop(null, destroyedSignLocation);
				return 1;
			} else {
				return 0;
			}
		}
	}

}
