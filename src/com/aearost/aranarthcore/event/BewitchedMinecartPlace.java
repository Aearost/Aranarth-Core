package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class BewitchedMinecartPlace implements Listener {

	public BewitchedMinecartPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the auto-refill functionality when consuming of arrows
	 * 
	 * @author Aearost
	 *
	 */
	@EventHandler
	public void onMinecartPlace(final PlayerInteractEvent e) {
		
//		if (e.getHand() == EquipmentSlot.)
		
		Player player = e.getPlayer();
		ItemStack item = null;
		if (player.getInventory().getItemInMainHand().getType() == Material.MINECART) {
			item = player.getInventory().getItemInMainHand();
		} else if (player.getInventory().getItemInOffHand().getType() == Material.MINECART) {
			item = player.getInventory().getItemInOffHand();
		} else {
			return;
		}
		
		if (Objects.nonNull(e.getClickedBlock())) {
			if (e.getClickedBlock().getType() == Material.RAIL || e.getClickedBlock().getType() == Material.ACTIVATOR_RAIL
					|| e.getClickedBlock().getType() == Material.DETECTOR_RAIL
					|| e.getClickedBlock().getType() == Material.POWERED_RAIL) {
				if (item.getItemMeta().hasLore()) {
					e.setCancelled(true);
					Minecart minecart = (Minecart) Bukkit.getWorld(
							player.getWorld().getName()).spawnEntity(e.getClickedBlock().getLocation(), EntityType.MINECART);
					minecart.setMaxSpeed(10000);
					
//			        minecart.setMetadata("Bewitched", METADATA_VALUE_GOES_HERE);
				}
			}
		}
	}
}
