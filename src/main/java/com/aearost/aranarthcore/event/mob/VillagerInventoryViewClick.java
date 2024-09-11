package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiVillager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerInventoryViewClick implements Listener {

	public VillagerInventoryViewClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Opens a GUI to view a villager's inventory when right-clicked while sneaking.
	 * @param e The event.
	 */
	@EventHandler
	public void onVillagerClick(final PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof Villager) {
			if (e.getPlayer().isSneaking()) {
				e.setCancelled(true);
				Villager villager = (Villager) e.getRightClicked();
				GuiVillager gui = new GuiVillager(e.getPlayer(), villager);
				gui.openGui();
			}
		}
	}
}
