package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Handles preventing non-food items from being added to and from the Dominion Food inventory.
 */
public class GuiDominionFoodClick {
	public void execute(InventoryClickEvent e) {
		if (e.getClickedInventory() != null) {
			if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
				if (e.getCurrentItem() != null && !isEligibleFoodItem(e.getCurrentItem().getType())) {
					e.setCancelled(true);
				}
			} else {
				if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
					ItemStack hotbarItem = e.getView().getBottomInventory().getItem(e.getHotbarButton());
					if (hotbarItem != null && !isEligibleFoodItem(hotbarItem.getType())) {
						e.setCancelled(true);
					}
				}
			}
		}
	}

	/**
	 * Determines if the item is an eligible food item.
	 * @param type The Material of the food item.
	 * @return Confirmation if the item is an eligible food item.
	 */
	public boolean isEligibleFoodItem(Material type) {
		return type == Material.ENCHANTED_GOLDEN_APPLE || type == Material.CAKE || type == Material.HAY_BLOCK
				|| type == Material.RABBIT_STEW || type == Material.MUSHROOM_STEW || type == Material.BEETROOT_SOUP
				|| type == Material.GOLDEN_APPLE || type == Material.COOKED_PORKCHOP || type == Material.COOKED_MUTTON
				|| type == Material.COOKED_BEEF || type == Material.COOKED_CHICKEN || type == Material.COOKED_RABBIT
				|| type == Material.COOKED_COD || type == Material.COOKED_SALMON || type == Material.PUMPKIN_PIE
				|| type == Material.DRIED_KELP_BLOCK || type == Material.BREAD || type == Material.APPLE
				|| type == Material.GOLDEN_CARROT || type == Material.PORKCHOP || type == Material.MUTTON
				|| type == Material.BEEF || type == Material.CHICKEN || type == Material.RABBIT
				|| type == Material.COD || type == Material.SALMON || type == Material.POISONOUS_POTATO
				|| type == Material.WHEAT || type == Material.BEETROOT || type == Material.BAKED_POTATO
				|| type == Material.CARROT || type == Material.POTATO || type == Material.DRIED_KELP
				|| type == Material.MELON_SLICE || type == Material.SWEET_BERRIES || type == Material.GLOW_BERRIES
				|| type == Material.CHORUS_FRUIT || type == Material.COOKIE;
	}
}
