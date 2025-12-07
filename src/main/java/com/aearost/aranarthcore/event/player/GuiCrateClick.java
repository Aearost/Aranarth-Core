package com.aearost.aranarthcore.event.player;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles preventing any logic from being done while in the Crates GUIs.
 */
public class GuiCrateClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);
	}
}
