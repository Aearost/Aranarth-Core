package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.gui.GuiVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Opens a GUI to view a villager's inventory when right-clicked while sneaking.
 */
public class VillagerInventoryViewClick {
	public void execute(PlayerInteractEntityEvent e) {
		if (e.getPlayer().isSneaking()) {
			e.setCancelled(true);
			Villager villager = (Villager) e.getRightClicked();
			GuiVillager gui = new GuiVillager(e.getPlayer(), villager);
			gui.openGui();
		}
	}
}
