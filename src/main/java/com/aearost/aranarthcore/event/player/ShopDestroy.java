package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Handles the deletion of a player shop.
 */
public class ShopDestroy {
	public void execute(BlockBreakEvent e) {
		Player player = e.getPlayer();

		int deletionResult = -1;
		Material type = e.getBlock().getType();

		if (type.name().endsWith("_SIGN")) {
			Location signLocation = e.getBlock().getLocation();
			Shop playerShop = ShopUtils.getShopFromLocation(signLocation);
			if (playerShop != null) {
				deletionResult = deleteShopIfPossible(player, signLocation);
			}
			// If the sign is not a shop
			else {
				return;
			}
		} else if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
			Location signLocation = e.getBlock().getRelative(BlockFace.UP).getLocation();
			Shop playerShop = ShopUtils.getShopFromLocation(signLocation);
			if (playerShop != null) {
				deletionResult = deleteShopIfPossible(player, signLocation);

				Sign sign = (Sign) signLocation.getBlock().getState();
				SignSide back = sign.getSide(Side.BACK);
				back.setLine(0, ChatUtils.stripColorFormatting(back.getLine(0)));
				back.setLine(1, ChatUtils.stripColorFormatting(back.getLine(1)));
				back.setLine(2, ChatUtils.stripColorFormatting(back.getLine(2)));
				back.setLine(3, ChatUtils.stripColorFormatting(back.getLine(3)));

				SignSide front = sign.getSide(Side.FRONT);
				front.setLine(0, ChatUtils.stripColorFormatting(front.getLine(0)));
				front.setLine(1, ChatUtils.stripColorFormatting(front.getLine(1)));
				front.setLine(2, ChatUtils.stripColorFormatting(front.getLine(2)));
				front.setLine(3, ChatUtils.stripColorFormatting(front.getLine(3)));

				sign.update(true, false);
			}
			// If the sign is not a shop
			else {
				return;
			}
		} else {
			// Prevents breaking a block that has a shop on it
			Block signBlock = isRelativeBlockShop(e.getBlock());
			if (signBlock != null) {
				Shop shop = ShopUtils.getShopFromLocation(signBlock.getLocation());

				if (shop.getUuid() == null || shop.getUuid().equals(player.getUniqueId())) {
					player.sendMessage(ChatUtils.chatMessage("&cYou must destroy the shop first!"));
					e.setCancelled(true);
					return;
				} else {
					deletionResult = 0;
				}
			} else {
				return;
			}
		}

		if (deletionResult == 1) {
			player.sendMessage(ChatUtils.chatMessage("&7You have destroyed this shop"));
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.1F);
		} else if (deletionResult == 0) {
			player.sendMessage(ChatUtils.chatMessage("&cYou cannot destroy someone else's shop!"));
			e.setCancelled(true);
		} else {
			player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with deleting this shop..."));
			e.setCancelled(true);
		}
	}

	/**
	 * Determines if the surrounding blocks are shops.
	 * @return The relative sign block.
	 */
	private Block isRelativeBlockShop(Block block) {
		for (BlockFace face : BlockFace.values()) {
			if (!face.isCartesian()) {
				continue;
			}

			Block relative = block.getRelative(face);

			if (!(relative.getState() instanceof Sign signState)) {
				continue;
			} else {
				if (signState.getBlockData() instanceof WallSign wallSign) {
					BlockFace attachedFace = wallSign.getFacing().getOppositeFace();

					if (relative.getRelative(attachedFace).equals(block)) {
						if (ShopUtils.getShopFromLocation(relative.getLocation()) != null) {
							return relative;
						}
					}
				}
			}
		}

		return null;
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
		Shop shop = ShopUtils.getShopFromLocation(destroyedSignLocation);
		if (shop == null) {
			return -1;
		}

		// If it is the player's shop
		if (shop.getUuid().equals(player.getUniqueId())) {
			ShopUtils.removeShop(shop);
			return 1;
		}
		// If it is a server shop
		else if (shop.getUuid() == null) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 3) {
				ShopUtils.removeShop(shop);
				return 1;
			}
		}
		return 0;
	}

}
