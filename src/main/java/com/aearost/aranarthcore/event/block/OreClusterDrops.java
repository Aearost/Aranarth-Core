package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Adds functionality of ore clusters when breaking ore blocks.
 */
public class OreClusterDrops {
	public void execute(BlockBreakEvent e) {
		Material material = e.getBlock().getType();
		Random random = new Random();
		World world = e.getBlock().getWorld();
		Location loc = e.getBlock().getLocation();

		if (e.getPlayer().getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		// Only apply in the survival worlds
		if (loc.getWorld().getName().startsWith("world") || loc.getWorld().getName().startsWith("smp") || loc.getWorld().getName().startsWith("resource")) {
			ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
			if (!heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
				// Increased drop rate with fortune
				double fortuneBonus = 1;
				if (heldItem.containsEnchantment(Enchantment.FORTUNE)) {
					int enchantmentLevel = heldItem.getEnchantmentLevel(Enchantment.FORTUNE);
					if (enchantmentLevel == 1) {
						fortuneBonus = 0.98;
					} else if (enchantmentLevel == 2) {
						fortuneBonus = 0.95;
					} else if (enchantmentLevel == 3) {
						fortuneBonus = 0.9;
					}
				}

				double dwarvenReduction = 1;
				if (AranarthUtils.isWearingArmorType(e.getPlayer(), "dwarven")) {
					dwarvenReduction = 0.9;
				}

				double miningBoostReduction = 1;
				// 1.25x increase drop chance
				if (AranarthUtils.getServerBoosts().containsKey(Boost.MINER)) {
					miningBoostReduction = 0.8;
				}

				double evaluatedReduction = fortuneBonus * dwarvenReduction * miningBoostReduction;
				// Prevent going lower than a 2.5x increase
				if (evaluatedReduction < 0.4) {
					evaluatedReduction = 0.4;
				}

				if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
					double calculation = 55 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new DiamondCluster().getItem());
					}
				} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
					double calculation = 7 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new EmeraldCluster().getItem());
					}
				} else if (material.name().endsWith("GOLD_ORE")) {
					if (material == Material.NETHER_GOLD_ORE) {
						double calculation = 250 * evaluatedReduction;
						if (calculation <= 0) {
							return;
						}
						else if (random.nextInt((int) (calculation)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					} else {
						double calculation = 50 * evaluatedReduction;
						if (calculation <= 0) {
							return;
						}
						else if (random.nextInt((int) (calculation)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					}
				} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
					double calculation = 50 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new IronCluster().getItem());
					}
				} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
					double calculation = 180 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new CopperCluster().getItem());
					}
				} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
					double calculation = 75 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new RedstoneCluster().getItem());
					}
				} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
					double calculation = 40 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new LapisCluster().getItem());
					}
				} else if (material == Material.NETHER_QUARTZ_ORE) {
					double calculation = 160 * evaluatedReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new QuartzCluster().getItem());
					}
				}
			}
		}
	}
}
