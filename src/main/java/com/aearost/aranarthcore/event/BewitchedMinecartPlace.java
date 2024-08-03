package com.aearost.aranarthcore.event;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class BewitchedMinecartPlace implements Listener {

	public BewitchedMinecartPlace(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Handles the placing of a Bewitched Minecart.
	 * @param e The event.
	 */
	@EventHandler
	public void onBewitchedMinecartPlace(final PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack item;
		if (player.getInventory().getItemInMainHand().getType() == Material.MINECART) {
			item = player.getInventory().getItemInMainHand();
		} else if (player.getInventory().getItemInOffHand().getType() == Material.MINECART) {
			item = player.getInventory().getItemInOffHand();
		} else {
			return;
		}
		
		if (Objects.nonNull(e.getClickedBlock()) && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (e.getClickedBlock().getType() == Material.RAIL || e.getClickedBlock().getType() == Material.ACTIVATOR_RAIL
					|| e.getClickedBlock().getType() == Material.DETECTOR_RAIL
					|| e.getClickedBlock().getType() == Material.POWERED_RAIL) {
				if (Objects.nonNull(item.getItemMeta()) && item.getItemMeta().hasLore()) {
					e.setCancelled(true);
					Minecart minecart = (Minecart) Objects.requireNonNull(Bukkit.getWorld(
                            player.getWorld().getName())).spawnEntity(e.getClickedBlock().getLocation(), EntityType.MINECART);
					minecart.setMaxSpeed(3.5);
					
					// To add support that the carts will drop as bewitched as well
//			        minecart.setMetadata("Bewitched", METADATA_VALUE_GOES_HERE);
				}
			}
		}
	}
}
