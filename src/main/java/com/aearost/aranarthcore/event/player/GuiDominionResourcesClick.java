package com.aearost.aranarthcore.event.player;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles preventing the items from being added to and from the Dominion Resources inventory.
 */
public class GuiDominionResourcesClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);
	}
}
