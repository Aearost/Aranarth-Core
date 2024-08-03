package com.aearost.aranarthcore.event;

import java.util.Objects;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.AranarthCore;

public class GoatDeath implements Listener {

	public GoatDeath(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with dropping mutton if a Goat is killed.
	 * @param e The event.
	 */
	@EventHandler
	public void onGoatDeath(final EntityDeathEvent e) {
		if (e.getEntityType() == EntityType.GOAT) {
			Random r = new Random();
			int dropCount = r.nextInt(3) + 1;
			if (e.getEntity().isVisualFire()) {
				Objects.requireNonNull(e.getEntity().getLocation().getWorld()).dropItemNaturally(
						e.getEntity().getLocation(), new ItemStack(Material.COOKED_MUTTON, dropCount));
			} else {
				Objects.requireNonNull(e.getEntity().getLocation().getWorld()).dropItemNaturally(
						e.getEntity().getLocation(), new ItemStack(Material.MUTTON, dropCount));
			}
		}
	}

}
