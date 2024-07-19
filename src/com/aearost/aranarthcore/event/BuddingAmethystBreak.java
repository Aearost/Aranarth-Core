package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class BuddingAmethystBreak implements Listener {

	public BuddingAmethystBreak(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * If harvested with a silk touch pickaxe, the Budding Amethyst block will drop
	 * 
	 * @param e
	 */
	@EventHandler
	public void onBuddingAmethystBreak(final BlockBreakEvent e) {

		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();

		if (e.getBlock().getType() == Material.BUDDING_AMETHYST && isHoldingPickaxe(heldItem) && heldItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
			Location location = e.getBlock().getLocation();
			location.getWorld().dropItemNaturally(location, new ItemStack(Material.BUDDING_AMETHYST, 1));
			location.getBlock().setType(Material.AIR);
		}
	}

	private boolean isHoldingPickaxe(ItemStack heldItem) {
		Material item = heldItem.getType();
		if (item == Material.WOODEN_PICKAXE || item == Material.STONE_PICKAXE || item == Material.IRON_PICKAXE
				|| item == Material.GOLDEN_PICKAXE || item == Material.DIAMOND_PICKAXE
				|| item == Material.NETHERITE_PICKAXE) {
			return true;
		} else {
			return false;
		}
	}

}
