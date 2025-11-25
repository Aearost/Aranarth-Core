package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Automatically compacts picked up items into their block form.
 */
public class CompactorItemPickup {

	public void execute(EntityPickupItemEvent e) {

		if (e.getEntity() instanceof Player player) {
			if (!player.hasPermission("aranarth.compact")) {
				return;
			}

			// Check if there's metadata/it isn't a basic item, do nothing
			// Check if there's metadata/it isn't a basic item, do nothing
			// Check if there's metadata/it isn't a basic item, do nothing
			// Check if there's metadata/it isn't a basic item, do nothing
			// Check if there's metadata/it isn't a basic item, do nothing


			Material type = e.getItem().getItemStack().getType();

			if (type == Material.COAL) {
				compact(e, type, Material.COAL_BLOCK, 9);
			} else if (type == Material.RAW_COPPER) {
				compact(e, type, Material.RAW_COPPER_BLOCK, 9);
			} else if (type == Material.COPPER_INGOT) {
				compact(e, type, Material.COPPER_BLOCK, 9);
			} else if (type == Material.RAW_IRON) {
				compact(e, type, Material.RAW_IRON_BLOCK, 9);
			} else if (type == Material.IRON_NUGGET) {
				compact(e, type, Material.IRON_INGOT, 9);
			} else if (type == Material.IRON_INGOT) {
				compact(e, type, Material.IRON_BLOCK, 9);
			} else if (type == Material.RAW_GOLD) {
				compact(e, type, Material.RAW_GOLD_BLOCK, 9);
			} else if (type == Material.GOLD_NUGGET) {
				compact(e, type, Material.GOLD_INGOT, 9);
			} else if (type == Material.GOLD_INGOT) {
				compact(e, type, Material.GOLD_BLOCK, 9);
			} else if (type == Material.REDSTONE) {
				compact(e, type, Material.REDSTONE_BLOCK, 9);
			} else if (type == Material.LAPIS_LAZULI) {
				compact(e, type, Material.LAPIS_BLOCK, 9);
			} else if (type == Material.DIAMOND) {
				compact(e, type, Material.DIAMOND_BLOCK, 9);
			} else if (type == Material.EMERALD) {
				compact(e, type, Material.EMERALD_BLOCK, 9);
			} else if (type == Material.NETHERITE_INGOT) {
				compact(e, type, Material.NETHERITE_BLOCK, 9);
			} else if (type == Material.AMETHYST_SHARD) {
				compact(e, type, Material.AMETHYST_BLOCK, 4);
			} else if (type == Material.RESIN_CLUMP) {
				compact(e, type, Material.RESIN_BLOCK, 9);
			} else if (type == Material.GLOWSTONE_DUST) {
				compact(e, type, Material.GLOWSTONE, 4);
			} else if (type == Material.WHEAT) {
				compact(e, type, Material.HAY_BLOCK, 9);
			} else if (type == Material.MELON_SLICE) {
				compact(e, type, Material.MELON, 9);
			} else if (type == Material.DRIED_KELP) {
				compact(e, type, Material.DRIED_KELP_BLOCK, 9);
			} else if (type == Material.SUGAR_CANE) {
				compact(e, type, Material.BAMBOO_BLOCK, 9);
			} else if (type == Material.HONEYCOMB) {
				compact(e, type, Material.HONEYCOMB_BLOCK, 4);
			} else if (type == Material.SLIME_BALL) {
				compact(e, type, Material.SLIME_BLOCK, 9);
			} else if (type == Material.BONE_MEAL) {
				compact(e, type, Material.BONE_BLOCK, 9);
			} else if (type == Material.SNOWBALL) {
				compact(e, type, Material.SNOW_BLOCK, 4);
			} else if (type == Material.CLAY_BALL) {
				compact(e, type, Material.CLAY, 4);
			}
		}
	}

	/**
	 * Handles the full compacting logic of going through a player's inventory and compacting if the picked up item can be.
	 * @param e The event.
	 * @param item The Material of the item the player just picked up.
	 * @param compressed The compressed equivalent Material of the item the player just picked up.
	 * @param amountRequiredToCompress The quantity of the item that must be in the inventory in order to compress.
	 */
	private void compact(EntityPickupItemEvent e, Material item, Material compressed, int amountRequiredToCompress) {
		Player player = (Player) e.getEntity();
		int currentInventoryAmount = 0;

		for (int i = 0; i < player.getInventory().getContents().length; i++) {
			ItemStack inventoryItem = player.getInventory().getContents()[i];
			if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
				continue;
			}


		}

		// Erase the item that's picked up
		// Cancel the event
		// Then add the compressed item into the inventory
		// Then the shulker item pickup will only register the compressed item and not the original

		// Search the player's inventory for all amounts of the item
		// Skip over items that have metadata
		// If it's sugarcane being picked up, make sure it provides the sugarcane block and not bamboo
		// If they have the shulker permission, also search the contents of shulkers recursively
		// Prioritize first empty slot in inventory, NOT in shulkers whatsoever

	}
}
