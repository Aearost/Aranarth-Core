package com.aearost.aranarthcore.event.player;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static com.aearost.aranarthcore.objects.CustomItemKeys.ARMOR_TYPE;

/**
 * Puts items in the player's inventory if they were in the Enhanced Aranarthium anvil GUI.
 */
public class GuiEnhancedAranarthiumClose {
	public void execute(InventoryCloseEvent e) {
		Inventory inventory = e.getInventory();

		if (inventory.getItem(0) != null) {
			ItemMeta meta = null;
			if (inventory.getItem(0).hasItemMeta()) {
				meta = inventory.getItem(0).getItemMeta();
				if (!meta.getPersistentDataContainer().has(ARMOR_TYPE)) {
					e.getPlayer().getInventory().addItem(inventory.getItem(0));
				}
			} else if (hasNetheriteArmour(inventory.getItem(0))) {
				e.getPlayer().getInventory().addItem(inventory.getItem(0));
			}
		}

		if (inventory.getItem(1) != null) {
			ItemMeta meta = null;
			if (inventory.getItem(1).hasItemMeta()) {
				meta = inventory.getItem(1).getItemMeta();
				if (!meta.getPersistentDataContainer().has(ARMOR_TYPE)) {
					e.getPlayer().getInventory().addItem(inventory.getItem(1));
				}
			} else if (hasNetheriteArmour(inventory.getItem(1))) {
				e.getPlayer().getInventory().addItem(inventory.getItem(1));
			}
		}
	}

	/**
	 * Determines if the item is a normal netherite armour piece.
	 * @param item The item in the anvil to be verified.
	 * @return Confirmation of whether the input slot is a normal netherite armour piece.
	 */
	private boolean hasNetheriteArmour(ItemStack item) {
		if (item != null) {
			Material type = item.getType();
			if (type == Material.NETHERITE_HELMET || type == Material.NETHERITE_CHESTPLATE
					|| type == Material.NETHERITE_LEGGINGS || type == Material.NETHERITE_BOOTS) {
				return true;
			}
		}
		return false;
	}

}
