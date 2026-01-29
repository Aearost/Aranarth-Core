package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * If harvested with a silk touch pickaxe, the Budding Amethyst block will drop.
 */
public class BuddingAmethystBreak {
	public void execute(BlockBreakEvent e) {
		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
		if (heldItem.containsEnchantment(Enchantment.SILK_TOUCH) && isHoldingPickaxe(heldItem)) {
			Location location = e.getBlock().getLocation();
			location.getWorld().dropItemNaturally(location, new ItemStack(Material.BUDDING_AMETHYST, 1));
			location.getBlock().setType(Material.AIR);
		}
	}

	private boolean isHoldingPickaxe(ItemStack heldItem) {
		Material item = heldItem.getType();
        return item == Material.WOODEN_PICKAXE || item == Material.STONE_PICKAXE || item == Material.IRON_PICKAXE
                || item == Material.GOLDEN_PICKAXE || item == Material.DIAMOND_PICKAXE
                || item == Material.NETHERITE_PICKAXE;
	}
}
