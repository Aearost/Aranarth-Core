package com.aearost.aranarthcore.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Puts items in the player's inventory if they were in the Fletching Table GUI.
 */
public class GuiFletchingTableClose {
	public void execute(InventoryCloseEvent e) {
		ItemStack[] contents = e.getInventory().getContents();
		Player player = (Player) e.getPlayer();
		if (contents[2] != null) {
			player.getInventory().addItem(contents[2]);
		}
		if (contents[5] != null) {
			player.getInventory().addItem(contents[5]);
		}
		if (contents[8] != null) {
			player.getInventory().addItem(contents[8]);
		}
	}
}
