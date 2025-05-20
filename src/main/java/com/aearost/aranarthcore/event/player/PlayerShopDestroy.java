package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.PlayerShop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles the deletion of a player shop.
 */
public class PlayerShopDestroy {
	public void execute(BlockBreakEvent e) {
		Player player = e.getPlayer();

		// Prevents breaking a block that has a shop on it
		if (isRelativeBlockShop(e.getBlock()) && !e.getBlock().getType().name().toLowerCase().endsWith("sign")) {
			player.sendMessage(ChatUtils.chatMessage("&cPlease destroy the shop on its own"));
			e.setCancelled(true);
			return;
		}

		int deletionResult = -1;
		Location destroyedSignLocation = null;

		if (isSign(e.getBlock().getType())) {
			destroyedSignLocation = e.getBlock().getLocation();
			PlayerShop playerShop = AranarthUtils.getShop(destroyedSignLocation);
			if (playerShop != null) {
				deletionResult = deleteShopIfPossible(player, destroyedSignLocation);
			}
			// If the sign is not a shop
			else {
				return;
			}
		} else {
			return;
		}

		if (deletionResult == 1) {
			player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.1F);
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

		// If it is the player's shop
		if (shop.getUuid() == player.getUniqueId()) {
			AranarthUtils.removeShop(player.getUniqueId(), destroyedSignLocation);
			return 1;
		}
		// If it is a server shop
		else if (shop.getUuid() == null) {
			if (player.getName().equals("Aearost")) {
				AranarthUtils.removeShop(null, destroyedSignLocation);
				return 1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}

}
