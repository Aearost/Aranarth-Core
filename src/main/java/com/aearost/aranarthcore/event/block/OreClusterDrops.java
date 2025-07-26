package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
		// Only apply in the survival worlds
		if (loc.getWorld().getName().startsWith("world")) {
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

				double dwarvenReduction = 0;
				if (AranarthUtils.isArmorType(e.getPlayer(), "dwarven")) {
					dwarvenReduction = 0.9;
				}

				if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
					double calculation = 55 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new DiamondCluster().getItem());
					}
				} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
					double calculation = 7 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new EmeraldCluster().getItem());
					}
				} else if (material.name().endsWith("GOLD_ORE")) {
					if (material == Material.NETHER_GOLD_ORE) {
						double calculation = 250 * fortuneBonus * dwarvenReduction;
						if (calculation <= 0) {
							return;
						}
						else if (random.nextInt((int) (calculation)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					} else {
						double calculation = 35 * fortuneBonus * dwarvenReduction;
						if (calculation <= 0) {
							return;
						}
						else if (random.nextInt((int) (calculation)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					}
				} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
					double calculation = 50 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new IronCluster().getItem());
					}
				} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
					double calculation = 460 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new CopperCluster().getItem());
					}
				} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
					double calculation = 100 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new RedstoneCluster().getItem());
					}
				} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
					double calculation = 55 * fortuneBonus * dwarvenReduction;
					if (calculation <= 0) {
						return;
					}
					else if (random.nextInt((int) (calculation)) == 0) {
						world.dropItemNaturally(loc, new LapisCluster().getItem());
					}
				} else if (material == Material.NETHER_QUARTZ_ORE) {
					double calculation = 240 * fortuneBonus * dwarvenReduction;
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
