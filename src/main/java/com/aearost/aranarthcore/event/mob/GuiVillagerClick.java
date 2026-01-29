package com.aearost.aranarthcore.event.mob;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Deals with all clicks of the villager GUI elements.
 */
public class GuiVillagerClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);
		if (e.getSlot() == 8) {
			Player player = (Player) e.getWhoClicked();
			player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
			e.getWhoClicked().closeInventory();
		}
	}
	
}
