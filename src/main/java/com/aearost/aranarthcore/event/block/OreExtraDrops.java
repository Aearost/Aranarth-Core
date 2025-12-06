package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Adds extra ore drop rates if the player is wearing full Dwarven Aranarthium armor.
 */
public class OreExtraDrops {
	public void execute(BlockBreakEvent e) {
		Player player = e.getPlayer();
		Material material = e.getBlock().getType();
		Random random = new Random();
		World world = e.getBlock().getWorld();
		Location loc = e.getBlock().getLocation();

		double maxChance = 100;
		int extraDropAmount = 0;

		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		// Only apply in the survival worlds
		if (loc.getWorld().getName().startsWith("world") || loc.getWorld().getName().startsWith("smp") || loc.getWorld().getName().startsWith("resource")) {
			ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
			Material materialToAdd = Material.AIR;
			if (!heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
				// 1.5x the chance
				if (AranarthUtils.getServerBoosts().containsKey(Boost.MINER)) {
					maxChance = maxChance * 0.666666666;
				}

				// Varying chance of dwarven armor increasing and bettering chance of extra drops
				if (AranarthUtils.isWearingArmorType(player, "dwarven")) {
					int dwarvenChance = new Random().nextInt(100);
					if (dwarvenChance < 5) {
						maxChance = maxChance * 0.666666666;
					} else if (dwarvenChance < 25) {
						maxChance = maxChance * 0.75;
					} else {
						maxChance = maxChance * 0.9;
					}
				}

				int chance = new Random().nextInt((int) maxChance);

				if (chance < 5) {
					extraDropAmount = 3;
				} else if (chance < 25) {
					extraDropAmount = 2;
				} else if (chance < 60) {
					extraDropAmount = 1;
				}
				// No extra drops if above 60

				// Only drop extra if Dwarven armor is worn or if there is a miner boost active
				if (maxChance == 100) {
					return;
				}

				if (extraDropAmount == 0) {
					return;
				}

				if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
					materialToAdd = Material.DIAMOND;
				} else if (material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE) {
					materialToAdd = Material.EMERALD;
				} else if (material.name().endsWith("_GOLD_ORE")) {
					if (material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE) {
						materialToAdd = Material.RAW_GOLD;
					} else {
						materialToAdd = Material.GOLD_NUGGET;
						world.dropItemNaturally(loc, new ItemStack(materialToAdd, extraDropAmount * 3));
						return;
					}
				} else if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE) {
					materialToAdd = Material.RAW_IRON;
				} else if (material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE) {
					materialToAdd = Material.RAW_COPPER;
				} else if (material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE) {
					materialToAdd = Material.REDSTONE;
					world.dropItemNaturally(loc, new ItemStack(materialToAdd, extraDropAmount * 2));
					return;
				} else if (material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE) {
					materialToAdd = Material.LAPIS_LAZULI;
					world.dropItemNaturally(loc, new ItemStack(materialToAdd, extraDropAmount * 3));
					return;
				} else if (material == Material.NETHER_QUARTZ_ORE) {
					materialToAdd = Material.QUARTZ;
				} else if (material == Material.ANCIENT_DEBRIS) {
					materialToAdd = Material.ANCIENT_DEBRIS;
					if (maxChance < 30) {
						world.dropItemNaturally(loc, new ItemStack(materialToAdd, 1));
					}
					return;
				}
			} else {
				if (material == Material.ANCIENT_DEBRIS) {
					materialToAdd = Material.ANCIENT_DEBRIS;
					if (maxChance < 30) {
						world.dropItemNaturally(loc, new ItemStack(materialToAdd, 1));
					}
					return;
				}
			}

			if (extraDropAmount > 0) {
				world.dropItemNaturally(loc, new ItemStack(materialToAdd, extraDropAmount));
			}
		}
	}
}
