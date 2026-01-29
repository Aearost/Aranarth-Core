package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Prevents players from removing potions in the potions list GUI.
 */
public class GuiPotionListPreventRemoval {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith("Your Potions")) {
			e.setCancelled(true);
		}
	}
	
}
