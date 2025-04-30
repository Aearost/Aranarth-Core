package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Random;

public class BoneMealSapling implements Listener {

	public BoneMealSapling(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Increases the chances of bone meal instantly growing the sapling during the month of Follivor.
	 * @param e The event.
	 */
	@EventHandler
	public void onBoneMealUse(final PlayerInteractEvent e) {
		if (AranarthUtils.getMonth() == Month.FOLLIVOR) {
			Player player = e.getPlayer();
			Material item = player.getInventory().getItemInMainHand().getType();
			if (item == Material.BONE_MEAL) {
				if (e.getClickedBlock() != null) {
					Block clicked = e.getClickedBlock();
					Material clickedType = clicked.getType();
					if (clicked.getBlockData() instanceof Sapling sapling) {
						// 50% chance of instantly growing the tree
						if (new Random().nextInt(2) == 0) {
							Block[] saplings = getSaplingsIfLargeTree(clicked);
							// If it is a 2x2 tree
							if (saplings != null) {
								// Clear the saplings
								saplings[0].setType(Material.AIR);
								saplings[1].setType(Material.AIR);
								saplings[2].setType(Material.AIR);
								saplings[3].setType(Material.AIR);

								// Determines the type of tree to attempt to grow
								TreeType treeType = switch (sapling.getMaterial()) {
									case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
									case JUNGLE_SAPLING -> TreeType.JUNGLE;
									case SPRUCE_SAPLING -> TreeType.MEGA_PINE;
									case PALE_OAK_SAPLING -> TreeType.PALE_OAK;
									default -> null;
								};

								if (treeType != null) {
									// Try to generate the big tree at top-left block
									boolean isSuccessfulGrowth = clicked.getLocation().getWorld().generateTree(saplings[0].getLocation(), treeType);

									if (isSuccessfulGrowth) {
										// Decrease the amount of bone meal by one
										player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
									} else {
										// Set the sapling back
										saplings[0].setType(sapling.getMaterial());
										saplings[1].setType(sapling.getMaterial());
										saplings[2].setType(sapling.getMaterial());
										saplings[3].setType(sapling.getMaterial());
									}
								}
							}
							// If it is a normal sized tree
							else {
								TreeType treeType = getSaplingType(sapling.getMaterial());
								// Cannot generate the tree if the sapling exists
								clicked.setType(Material.AIR);
								if (treeType != null) {
									boolean isSuccessfulGrowth = clicked.getLocation().getWorld().generateTree(clicked.getLocation(), treeType);
									if (isSuccessfulGrowth) {
										// Decrease the amount of bone meal by one
										player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
									} else {
										// Set the sapling back
										clicked.setType(clickedType);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Provides the associated TreeType of the sapling.
	 * @param sapling The sapling.
	 * @return Confirmation of what the TreeType of the sapling is.
	 */
	private TreeType getSaplingType(Material sapling) {
		return switch (sapling) {
			case OAK_SAPLING -> TreeType.TREE;
			case BIRCH_SAPLING -> TreeType.BIRCH;
			case SPRUCE_SAPLING -> TreeType.REDWOOD;
			case JUNGLE_SAPLING -> TreeType.SMALL_JUNGLE;
			case ACACIA_SAPLING -> TreeType.ACACIA;
			case DARK_OAK_SAPLING -> TreeType.DARK_OAK;
			case CHERRY_SAPLING -> TreeType.CHERRY;
			case MANGROVE_PROPAGULE -> TreeType.MANGROVE;
			case BROWN_MUSHROOM -> TreeType.BROWN_MUSHROOM;
			case RED_MUSHROOM -> TreeType.RED_MUSHROOM;
			case PALE_OAK_SAPLING -> TreeType.PALE_OAK;
			case WARPED_FUNGUS -> TreeType.WARPED_FUNGUS;
			case CRIMSON_FUNGUS -> TreeType.CRIMSON_FUNGUS;
			case AZALEA, FLOWERING_AZALEA -> TreeType.AZALEA;
            default -> null;
		};
	}

	/**
	 * Provides an array of the Blocks that are saplings if this is a 2x2 tree.
	 * @param clickedSapling The sapling that was initially clicked.
	 * @return The array of Blocks.
	 */
	private Block[] getSaplingsIfLargeTree(Block clickedSapling) {
		Material saplingType = clickedSapling.getType();
		Location loc = clickedSapling.getLocation();
		Block[] saplings = new Block[4];

		// Try all 4 possible top-left corners of a 2x2
		for (int dx = -1; dx <= 0; dx++) {
			for (int dz = -1; dz <= 0; dz++) {
				Block b1 = loc.clone().add(dx, 0, dz).getBlock();
				Block b2 = loc.clone().add(dx + 1, 0, dz).getBlock();
				Block b3 = loc.clone().add(dx, 0, dz + 1).getBlock();
				Block b4 = loc.clone().add(dx + 1, 0, dz + 1).getBlock();

				if (b1.getType() == saplingType && b2.getType() == saplingType
						&& b3.getType() == saplingType && b4.getType() == saplingType) {
					saplings[0] = b1;
					saplings[1] = b2;
					saplings[2] = b3;
					saplings[3] = b4;
					return saplings;
				}
			}
		}
		return null;
	}
}
