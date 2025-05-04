package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LeafDecayDrops implements Listener {

	public LeafDecayDrops(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Adds increased apple drop rates during the month of Solarvor.
	 * Additionally, adds increased sapling drop rate during the month of Follivor.
	 * Also adds god apple fragments into the game.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeavesDecay(final LeavesDecayEvent e) {
		Block block = e.getBlock();

		// During the month of Solarvor
		if (AranarthUtils.getMonth() == Month.SOLARVOR) {
			if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
				// 5% chance of dropping an apple instead of 0.5%
				if (new Random().nextInt(20) == 0) {
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
				}
				// 1% chance of dropping a god apple fragment
				else if (new Random().nextInt(100) == 0) {
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), GodAppleFragment.getGodAppleFragment());
					for (Player player : Bukkit.getOnlinePlayers()) {
						// If the player is within 32 blocks of the spawn location
						if (block.getLocation().distance(player.getLocation()) <= 32) {
							player.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has been dropped!"));
						}
					}
				}
			}
			return;
		}
		// During the month of Follivor
		else if (AranarthUtils.getMonth() == Month.FOLLIVOR) {
			// 25% chance of sapling drop chance
			if (new Random().nextInt(4) == 0) {
				if (e.getBlock().getBlockData() instanceof Leaves leaves) {
					ItemStack sapling = null;
					if (block.getType() == Material.OAK_LEAVES) {
						sapling = new ItemStack(Material.OAK_SAPLING);
					} else if (block.getType() == Material.BIRCH_LEAVES) {
						sapling = new ItemStack(Material.BIRCH_SAPLING);
					} else if (block.getType() == Material.SPRUCE_LEAVES) {
						sapling = new ItemStack(Material.SPRUCE_SAPLING);
					} else if (block.getType() == Material.JUNGLE_LEAVES) {
						sapling = new ItemStack(Material.JUNGLE_SAPLING);
					} else if (block.getType() == Material.ACACIA_LEAVES) {
						sapling = new ItemStack(Material.ACACIA_SAPLING);
					} else if (block.getType() == Material.DARK_OAK_LEAVES) {
						sapling = new ItemStack(Material.DARK_OAK_SAPLING);
					} else if (block.getType() == Material.MANGROVE_LEAVES) {
						sapling = new ItemStack(Material.MANGROVE_PROPAGULE);
					} else if (block.getType() == Material.CHERRY_LEAVES) {
						sapling = new ItemStack(Material.CHERRY_SAPLING);
					} else if (block.getType() == Material.PALE_OAK_LEAVES) {
						sapling = new ItemStack(Material.PALE_OAK_SAPLING);
					} else {
						return;
					}
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), sapling);
				}
			}
		}

		// 0.005% chance of dropping a god apple fragment
		// Applies to all months other than Solarvor
		if (new Random().nextInt(2000) == 0) {
			if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
				block.getLocation().getWorld().dropItemNaturally(block.getLocation(), GodAppleFragment.getGodAppleFragment());
				for (Player player : Bukkit.getOnlinePlayers()) {
					// If the player is within 32 blocks of the spawn location
					if (block.getLocation().distance(player.getLocation()) <= 32) {
						player.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has been dropped!"));
					}
				}
			}
		}
	}
}
