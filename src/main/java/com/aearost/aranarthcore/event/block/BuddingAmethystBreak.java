package com.aearost.aranarthcore.event.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

/**
 * If harvested with a pickaxe bearing the Incantation of Preservation, the Budding Amethyst block will drop.
 */
public class BuddingAmethystBreak {
	public void execute(BlockBreakEvent e) {
		ItemStack heldItem = e.getPlayer().getInventory().getItemInMainHand();
		if (hasPreservation(heldItem) && isHoldingPickaxe(heldItem)) {
			Location location = e.getBlock().getLocation();
			e.setDropItems(false);
			location.getWorld().dropItemNaturally(location, new ItemStack(Material.BUDDING_AMETHYST, 1));
		}
	}

	private boolean hasPreservation(ItemStack item) {
		if (!item.hasItemMeta()) return false;
		var pdc = item.getItemMeta().getPersistentDataContainer();
		return pdc.has(INCANTATION_TYPE) &&
				"incantation_preservation".equals(pdc.get(INCANTATION_TYPE, PersistentDataType.STRING));
	}

	private boolean isHoldingPickaxe(ItemStack heldItem) {
		Material item = heldItem.getType();
        return item == Material.WOODEN_PICKAXE || item == Material.STONE_PICKAXE || item == Material.IRON_PICKAXE
                || item == Material.GOLDEN_PICKAXE || item == Material.DIAMOND_PICKAXE
                || item == Material.NETHERITE_PICKAXE;
	}
}
