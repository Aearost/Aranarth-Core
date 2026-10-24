package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Prevents players from removing potions in the potions list GUI.
 */
public class GuiPotionListPreventRemoval {
	public void execute(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).startsWith(Lang.getFor(player, "gui.potions.title_view").split("\\(")[0].trim())) {
			e.setCancelled(true);
		}
	}
	
}
