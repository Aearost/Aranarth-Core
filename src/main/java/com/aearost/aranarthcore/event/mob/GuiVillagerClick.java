package com.aearost.aranarthcore.event.mob;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiVillagerClick implements Listener {

	public GuiVillagerClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with all clicks of the villager GUI elements.
	 * @param e The event.
	 */
	@EventHandler
	public void onGuiClick(final InventoryClickEvent e) {
		if (ChatUtils.stripColorFormatting(e.getView().getTitle()).equals("Villager") && e.getView().getType() == InventoryType.CHEST) {
			e.setCancelled(true);
			if (e.getSlot() == 8) {
				Player player = (Player) e.getWhoClicked();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
				e.getWhoClicked().closeInventory();
			}
		}
	}
	
}
