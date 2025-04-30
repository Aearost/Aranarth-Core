package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

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
			if (AranarthUtils.getMonth() == Month.FAUNIVOR) {
				dropCount = dropCount + 1;
			}

			if (e.getEntity().isVisualFire()) {
				e.getEntity().getLocation().getWorld().dropItemNaturally(
						e.getEntity().getLocation(), new ItemStack(Material.COOKED_MUTTON, dropCount));
			} else {
				e.getEntity().getLocation().getWorld().dropItemNaturally(
						e.getEntity().getLocation(), new ItemStack(Material.MUTTON, dropCount));
			}
		}
	}

}
