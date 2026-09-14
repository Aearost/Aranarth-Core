package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Prevents players from removing potions in the potions list GUI.
 */
public class GuiPotionListPreventRemoval {
	public void execute(InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(com.aearost.aranarthcore.utils.Lang.get("gui.potions.title_view").split("\\(")[0].trim())) {
			e.setCancelled(true);
		}
	}
	
}
