package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.aearost.aranarthcore.AranarthCore;

public class DragonHeadClick implements Listener {

	public DragonHeadClick(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the interaction of clicking on a dragon head.
	 * 
	 * @param e
	 */
	@EventHandler
	public void onDragonHeadClick(final PlayerInteractEvent e) {
		if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			System.out.println("A");
			if (e.getClickedBlock().getType() == Material.DRAGON_HEAD ||
					e.getClickedBlock().getType() == Material.DRAGON_WALL_HEAD) {
				System.out.println("B");
				Block head = e.getClickedBlock();
				if (head.isBlockPowered() || head.isBlockIndirectlyPowered()) {
					System.out.println("Is redstone powered head");
				}
			}
				
		}
	}
}
