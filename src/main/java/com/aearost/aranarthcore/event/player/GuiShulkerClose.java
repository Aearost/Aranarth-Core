package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class GuiShulkerClose implements Listener {

	public GuiShulkerClose(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles updating the shulker inventory when the GUI is closed.
	 * @param e The event.
	 */
	@EventHandler
	public void onShulkerInventoryClose(final InventoryCloseEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Shulker") && e.getView().getType() == InventoryType.CHEST) {
			Inventory inventory = e.getInventory();

		}
		
	}
}
