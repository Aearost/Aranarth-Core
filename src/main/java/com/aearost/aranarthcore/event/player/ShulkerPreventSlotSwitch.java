package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents the player from putting a shulker box into a shulker box.
 */
public class ShulkerPreventSlotSwitch {
	public void execute(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player player) {
			if (e.getClick() == ClickType.NUMBER_KEY) {
				// If it is out of the hotbar
				if (e.getSlot() > 8) {
					ItemStack itemInHotbarSlot = player.getInventory().getContents()[e.getHotbarButton()];
					if (itemInHotbarSlot != null) {
						if (itemInHotbarSlot.getType().name().endsWith("SHULKER_BOX")) {
							if (e.getClickedInventory() != player.getInventory()) {
								e.setCancelled(true);
							}
						}
					}
				}
			}
		}
	}
}
