package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Adds extra ore drop rates if the player is wearing full Dwarven Aranarthium armor.
 */
public class DwarvenAranarthiumOreDrops {
	public void execute(BlockBreakEvent e) {
		Player player = e.getPlayer();
		if (AranarthUtils.isArmorType(player, "dwarven")) {
			Material material = e.getBlock().getType();
			Random random = new Random();
			World world = e.getBlock().getWorld();
			Location loc = e.getBlock().getLocation();
			// Only apply in the survival worlds
			if (loc.getWorld().getName().startsWith("world") || loc.getWorld().getName().startsWith("smp")) {
				ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
				if (!heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
					int chance = random.nextInt(100);
					// 65% chance of dropping 1
					// 30% chance of dropping 2
					// 5% chance of dropping 3
					if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 1));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 2));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 3));
						}
					} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.EMERALD, 1));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.EMERALD, 2));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.EMERALD, 3));
						}
					} else if (material.name().endsWith("_GOLD_ORE")) {
						if (material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE) {
							if (chance <= 65) {
								world.dropItemNaturally(loc, new ItemStack(Material.RAW_GOLD, 1));
							} else if (chance <= 95)  {
								world.dropItemNaturally(loc, new ItemStack(Material.RAW_GOLD, 2));
							} else {
								world.dropItemNaturally(loc, new ItemStack(Material.RAW_GOLD, 3));
							}
						} else {
							if (chance <= 65) {
								world.dropItemNaturally(loc, new ItemStack(Material.GOLD_NUGGET, 1));
							} else if (chance <= 95)  {
								world.dropItemNaturally(loc, new ItemStack(Material.GOLD_NUGGET, 2));
							} else {
								world.dropItemNaturally(loc, new ItemStack(Material.GOLD_NUGGET, 3));
							}
						}
					} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_IRON, 1));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_IRON, 2));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_IRON, 3));
						}
					} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_COPPER, 1));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_COPPER, 2));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.RAW_COPPER, 3));
						}
					} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.REDSTONE, 2));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.REDSTONE, 4));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.REDSTONE, 6));
						}
					} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.LAPIS_LAZULI, 2));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.LAPIS_LAZULI, 5));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.LAPIS_LAZULI, 10));
						}
					} else if (material == Material.NETHER_QUARTZ_ORE) {
						if (chance <= 65) {
							world.dropItemNaturally(loc, new ItemStack(Material.QUARTZ, 2));
						} else if (chance <= 95)  {
							world.dropItemNaturally(loc, new ItemStack(Material.QUARTZ, 4));
						} else {
							world.dropItemNaturally(loc, new ItemStack(Material.QUARTZ, 6));
						}
					} else if (material == Material.ANCIENT_DEBRIS) {
						// 40% chance of dropping extra Ancient Debris
						if (random.nextInt(10) <= 4) {
							world.dropItemNaturally(loc, new ItemStack(Material.ANCIENT_DEBRIS, 1));
						}
					}
				} else {
					// 40% chance of dropping extra Ancient Debris
					if (material == Material.ANCIENT_DEBRIS) {
						if (random.nextInt(10) <= 4) {
							world.dropItemNaturally(loc, new ItemStack(Material.ANCIENT_DEBRIS, 1));
						}
					}
				}
			}
		}
	}
}
