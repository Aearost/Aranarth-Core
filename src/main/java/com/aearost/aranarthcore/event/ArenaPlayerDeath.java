package com.aearost.aranarthcore.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.inventory.ItemStack;

public class ArenaPlayerDeath implements Listener {

	public ArenaPlayerDeath(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Forces the player to respawn in the arena world when they die in it.
	 * It will also auto-equip iron armor upon death.
	 * @param e The event.
	 */
	@EventHandler
	public void onArenaDeath(final PlayerRespawnEvent e) {
		if (e.getPlayer().getWorld().getName().equalsIgnoreCase("arena")) {
			e.setRespawnLocation(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));

			Player player = e.getPlayer();
			player.getInventory().clear();
			player.getInventory().setArmorContents(new ItemStack[] {
					new ItemStack(Material.IRON_BOOTS, 1),
					new ItemStack(Material.IRON_LEGGINGS, 1),
					new ItemStack(Material.IRON_CHESTPLATE, 1),
					new ItemStack(Material.IRON_HELMET, 1)});
		}
	}
}
