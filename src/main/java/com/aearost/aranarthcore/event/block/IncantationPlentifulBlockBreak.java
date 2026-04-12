package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles logic when breaking a block with a tool with the incantation of plentiful.
 */
public class IncantationPlentifulBlockBreak {
	public void execute(BlockBreakEvent e) {
		Player player = e.getPlayer();
		UUID uuid = player.getUniqueId();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

		if (aranarthPlayer.getPlentifulBlocksToDestroy() == 0) {
			return;
		}

		ItemStack heldItem = player.getInventory().getItemInMainHand();
		// 3x3x1 based on the direction the player is looking
		// 30 degrees and -30 degrees (do both checks i.e looking up vs down vs straight ahead
		float yaw = player.getLocation().getYaw();
		float pitch = player.getLocation().getPitch();
		String name = heldItem.getType().name();
		List<Block> blocks = new ArrayList<>();

		if (name.endsWith("_AXE") || name.endsWith("_HOE")) {
			blocks = getBlocksToDestroy(e.getBlock().getLocation(), yaw, pitch, true);
		} else {
			blocks = getBlocksToDestroy(e.getBlock().getLocation(), yaw, pitch, false);
		}

		// Set the counter to the total number of blocks being processed so it is never
		// exhausted before the loop finishes (e.g. hoe processes 27 blocks but the
		// counter was only initialised to 9, causing it to go negative and re-trigger
		// execute on subsequent inner BlockBreakEvents).
		aranarthPlayer.setPlentifulBlocksToDestroy(blocks.size());
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

		// Cancels the original event
		e.setCancelled(true);

		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		for (Block block : blocks) {
			Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
			if (chunkDominion != null) {
				if (dominion == null || !dominion.getLeader().equals(chunkDominion.getLeader())) {
					// If it is not harvestable, the counter must be manually reduced regardless
					aranarthPlayer.setPlentifulBlocksToDestroy(aranarthPlayer.getPlentifulBlocksToDestroy() - 1);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					continue;
				}
			}

			if (name.endsWith("_PICKAXE")) {
				if (AranarthUtils.isHarvestableWithPickaxe(block.getType())) {
					callNewBlockBreakEvent(block, player, true);
				}
			} else if (name.endsWith("_AXE") && AranarthUtils.isHarvestableWithAxe(block.getType())) {
				callNewBlockBreakEvent(block, player, true);
			} else if (name.endsWith("_SHOVEL") && AranarthUtils.isHarvestableWithShovel(block.getType())) {
				callNewBlockBreakEvent(block, player, true);
			} else if (name.endsWith("_HOE") && AranarthUtils.isBlockCrop(block.getType())) {
				callNewBlockBreakEvent(block, player, false);
			} else {
				// If it is not harvestable, the counter must be manually reduced regardless
				aranarthPlayer.setPlentifulBlocksToDestroy(aranarthPlayer.getPlentifulBlocksToDestroy() - 1);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			}
		}

		if (heldItem.getItemMeta() instanceof Damageable damageableItemMeta) {
			int maxDurability = heldItem.getType().getMaxDurability();
			int damageToReduceBy = 5;

			// Decreased durability consumption for unbreaking
			if (heldItem.containsEnchantment(Enchantment.UNBREAKING)) {
				int level = heldItem.getEnchantmentLevel(Enchantment.UNBREAKING);
				if (level == 1) {
					damageToReduceBy = 3;
				} else if (level == 2) {
					damageToReduceBy = 2;
				} else if (level == 3) {
					damageToReduceBy = 1;
				}
			}

			int newDamage = 0;
			if (!damageableItemMeta.hasDamage()) {
				newDamage = damageToReduceBy;
			} else {
				newDamage = damageableItemMeta.getDamage() + damageToReduceBy;
			}

			boolean willToolBreak = (maxDurability - newDamage) <= 0;
			if (willToolBreak) {
				new BukkitRunnable() {
					@Override
					public void run() {
						player.getInventory().setItemInMainHand(null);
						player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);
						player.swingMainHand();
					}
				}.runTaskLater(AranarthCore.getInstance(), 1);
			} else {
				damageableItemMeta.setDamage(newDamage);
				heldItem.setItemMeta(damageableItemMeta);
			}
		}

		// Ensure the counter is zeroed out after the loop so that any skipped blocks
		// (e.g. soft blocks with a pickaxe) don't leave it positive and cause the next
		// regular block break to be misidentified as a recursive plentiful call.
		aranarthPlayer.setPlentifulBlocksToDestroy(0);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
	}

	/**
	 * Provides the list of blocks that will be in the 3x3x1 grid harvested when the block is destroyed.
	 * @param location The location of the initial block that was destroyed.
	 * @param yaw The yaw of the player.
	 * @param pitch The pitch of the player.
	 * @param isBreakingLarger If the blocks should be broken in a 3x3x3 shape.
	 * @return The list of blocks that will be in the 3x3x1 grid harvested when the block is destroyed.
	 */
	private List<Block> getBlocksToDestroy(Location location, float yaw, float pitch, boolean isBreakingLarger) {
		List<Block> blocks = new ArrayList<>();
		int centerX = location.getBlockX();
		int centerY = location.getBlockY();
		int centerZ = location.getBlockZ();

		// Breaking 3x3x1
		if (!isBreakingLarger) {
			boolean isFacingStraightAhead = pitch > -30 && pitch <= 30;
			boolean isFacingNorthSouth = ((yaw > -180 && yaw <= -135) || (yaw > 135 && yaw <= 180)) // Facing North
					|| (yaw > -30 && yaw <= 30); // Facing South

			if (isFacingStraightAhead) {
				if (isFacingNorthSouth) {
					// Does not change the zz-axis
					for (int x = centerX - 1; x <= centerX + 1; x++) {
						for (int y = centerY - 1; y <= centerY + 1; y++) {
							blocks.add(location.getWorld().getBlockAt(x, y, centerZ));
						}
					}
				} else {
					// Does not change the X-axis
					for (int z = centerZ - 1; z <= centerZ + 1; z++) {
						for (int y = centerY - 1; y <= centerY + 1; y++) {
							blocks.add(location.getWorld().getBlockAt(centerX, y, z));
						}
					}
				}
			}
			// Looking up or down
			else {
				// Does not change the Y-axis
				for (int x = centerX - 1; x <= centerX + 1; x++) {
					for (int z = centerZ - 1; z <= centerZ + 1; z++) {
						blocks.add(location.getWorld().getBlockAt(x, centerY, z));
					}
				}
			}
		} else {
			for (int x = centerX - 1; x <= centerX + 1; x++) {
				for (int y = centerY - 1; y <= centerY + 1; y++) {
					for (int z = centerZ - 1; z <= centerZ + 1; z++) {
						blocks.add(location.getWorld().getBlockAt(x, y, z));
					}
				}
			}
		}

		return blocks;
	}

	/**
	 * Calls the new block break event.
	 * @param block The block that will call the event.
	 * @param player The player.
	 * @param hasDrops Whether the block will be dropped or not.
	 */
	private void callNewBlockBreakEvent(Block block, Player player, boolean hasDrops) {
		// Prevents unbreakable blocks from being destroyed
		if (block.getType().getHardness() < 0 || block.getType().isAir()) {
			return;
		}

		block.getWorld().playSound(block.getLocation(), block.getBlockData().getSoundGroup().getBreakSound(), 1F, 0.1F);
		Bukkit.getServer().getPluginManager().callEvent(new BlockBreakEvent(block, player));
		if (hasDrops) {
			new BukkitRunnable() {
				@Override
				public void run() {
					block.breakNaturally(player.getInventory().getItemInMainHand());
				}
			}.runTaskLater(AranarthCore.getInstance(), 1);
		}
	}
}
