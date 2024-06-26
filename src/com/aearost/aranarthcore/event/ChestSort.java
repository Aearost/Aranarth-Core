package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class ChestSort implements Listener {

	public ChestSort(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles sorting the content of a container.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onContainerSort(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getClickedBlock().getType() == Material.CHEST
					|| e.getClickedBlock().getType() == Material.TRAPPED_CHEST
					|| e.getClickedBlock().getType() == Material.BARREL) {
				if (e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
					BlockState state = e.getClickedBlock().getState();
					Container container = (Container) state;
					
					for (ItemStack item : container.getInventory().getContents()) {
						if (item != null) {
							System.out.println(item.getType().name());
						}
					}
				}
			}
		}
	}
}
