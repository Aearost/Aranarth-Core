package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiVillager;

public class VillagerInventoryViewClick implements Listener {

	public VillagerInventoryViewClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Opens a GUI to view a villager's inventory when right clicked while sneaking.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onVillagerClick(final PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof Villager) {
			Villager villager = (Villager) e.getRightClicked();
			GuiVillager gui = new GuiVillager(e.getPlayer(), villager);
			gui.openGui();
		}
	}
}
