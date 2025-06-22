package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.items.aranarthium.clusters.*;
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
						fortuneBonus = 0.9;
					} else if (enchantmentLevel == 2) {
						fortuneBonus = 0.7;
					} else if (enchantmentLevel == 3) {
						fortuneBonus = 0.5;
					}
				}

				if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
					if (random.nextInt((int) (30 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new DiamondCluster().getItem());
					}
				} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
					if (random.nextInt((int) (20 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new EmeraldCluster().getItem());
					}
				} else if (material.name().endsWith("GOLD_ORE")) {
					if (material == Material.NETHER_GOLD_ORE) {
						if (random.nextInt((int) (200 * fortuneBonus)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					} else {
						if (random.nextInt((int) (65 * fortuneBonus)) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					}
				} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
					if (random.nextInt((int) (80 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new IronCluster().getItem());
					}
				} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
					if (random.nextInt((int) (200 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new CopperCluster().getItem());
					}
				} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
					if (random.nextInt((int) (75 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new RedstoneCluster().getItem());
					}
				} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
					if (random.nextInt((int) (45 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new LapisCluster().getItem());
					}
				} else if (material == Material.NETHER_QUARTZ_ORE) {
					if (random.nextInt((int) (175 * fortuneBonus)) == 0) {
						world.dropItemNaturally(loc, new QuartzCluster().getItem());
					}
				}
			}
		}
	}
}
