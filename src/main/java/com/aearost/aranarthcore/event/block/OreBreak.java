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
 * Adds functionality of ore fragment and clusters when breaking ore blocks.
 */
public class OreBreak {
	public void execute(BlockBreakEvent e) {
		Material material = e.getBlock().getType();
		Random random = new Random();
		World world = e.getBlock().getWorld();
		Location loc = e.getBlock().getLocation();
		// Only apply in the survival worlds
		if (loc.getWorld().getName().startsWith("world")) {
			ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
			if (!heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
				if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
					if (random.nextInt(125) == 0) {
						world.dropItemNaturally(loc, new DiamondCluster().getItem());
					}
				} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
					if (random.nextInt(100) == 0) {
						world.dropItemNaturally(loc, new EmeraldCluster().getItem());
					}
				} else if (material.name().endsWith("_GOLD_ORE")) {
					if (material == Material.NETHER_GOLD_ORE) {
						if (random.nextInt(750) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					} else {
						if (random.nextInt(250) == 0) {
							world.dropItemNaturally(loc, new GoldCluster().getItem());
						}
					}
				} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
					if (random.nextInt(350) == 0) {
						world.dropItemNaturally(loc, new IronCluster().getItem());
					}
				} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
					if (random.nextInt(550) == 0) {
						world.dropItemNaturally(loc, new CopperCluster().getItem());
					}
				} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
					if (random.nextInt(300) == 0) {
						world.dropItemNaturally(loc, new RedstoneCluster().getItem());
					}
				} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
					if (random.nextInt(200) == 0) {
						world.dropItemNaturally(loc, new LapisCluster().getItem());
					}
				} else if (material == Material.NETHER_QUARTZ_ORE) {
					if (random.nextInt(750) == 0) {
						world.dropItemNaturally(loc, new QuartzCluster().getItem());
					}
				}
			}
		}
	}
}
