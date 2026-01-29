package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.gmail.nossr50.api.TreeFellerBlockBreakEvent;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.woodcutting.WoodcuttingManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class LeafDropsListener implements Listener {

	public LeafDropsListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles additional functionality with Leaf Blower being used.
	 * One call for each block that is destroyed.
	 * @param e The event.
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		Block block = e.getBlock();
		handleDrops(block);
	}

	/**
	 * Handles additional functionality with leaves falling, such as God Apple Fragment drops and increased apple drop rates.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeavesDecay(final LeavesDecayEvent e) {
		Block block = e.getBlock();
		handleDrops(block);
	}

	/**
	 * Handles additional functionality with Tree Feller being used.
	 * One call for each block that is destroyed.
	 * @param e The event.
	 */
	@EventHandler
	public void onTreeFeller(final TreeFellerBlockBreakEvent e) {
		Block block = e.getBlock();
		handleDrops(block);
	}

	/**
	 * Handles additional functionality with Leaf Blower being used.
	 * One call for each block that is destroyed.
	 * @param e The event.
	 */
	@EventHandler
	public void onLeafBlower(final PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getHand() == EquipmentSlot.HAND) {
			Block block = e.getClickedBlock();
			if (block != null) {
				McMMOPlayer mcmmoPlayer = EventUtils.getMcMMOPlayer(e.getPlayer());
				if (mcmmoPlayer != null) {
					WoodcuttingManager woodcuttingManager = new WoodcuttingManager(mcmmoPlayer);
					if (woodcuttingManager.canUseLeafBlower(e.getPlayer().getInventory().getItemInMainHand())) {
						handleDrops(block);
					}
				}
			}
		}
	}

	/**
	 * All logic of determining the drops from leaf decay or leaves being destroyed by tree feller.
	 * @param block The block.
	 */
	private void handleDrops(Block block) {
		// During the month of Solarvor
		if (AranarthUtils.getMonth() == Month.SOLARVOR) {
			if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
				// 5% chance of dropping an apple instead of 0.5%
				if (new Random().nextInt(20) == 0) {
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
				}
				// 0.5% chance of dropping a god apple fragment during Solarvor
				else if (new Random().nextInt(200) == 0) {
					block.getLocation().getWorld().dropItemNaturally(block.getLocation(), new GodAppleFragment().getItem());
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (!block.getWorld().getName().equals(player.getWorld().getName())) {
							continue;
						}

						// If the player is within 48 blocks of the spawn location
						if (block.getLocation().distance(player.getLocation()) <= 48) {
							player.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has dropped nearby"));
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
				if (block.getBlockData() instanceof Leaves leaves) {
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

		// 0.05% chance of dropping a god apple fragment
		// Applies to all months other than Solarvor
		if (new Random().nextInt(2000) == 0) {
			if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
				block.getLocation().getWorld().dropItemNaturally(block.getLocation(), new GodAppleFragment().getItem());
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (!block.getWorld().getName().equals(player.getWorld().getName())) {
						continue;
					}

					// If the player is within 48 blocks of the spawn location
					if (block.getLocation().distance(player.getLocation()) <= 48) {
						player.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has dropped nearby"));
					}
				}
			}
		}
	}
}
