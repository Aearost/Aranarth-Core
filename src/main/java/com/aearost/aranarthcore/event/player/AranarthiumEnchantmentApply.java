package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Allows a custom Aranarth enchantment to be applied to its respective item.
 */
public class AranarthiumEnchantmentApply {
	public void execute(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player) {
			Inventory inventory = e.getClickedInventory();


		}
	}

}
