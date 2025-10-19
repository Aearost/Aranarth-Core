package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Random;

public class SnowballHitListener implements Listener {

	public SnowballHitListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Allows snowballs to deal a small amount of damage during the winter months.
	 * @param e The event.
	 */
	@EventHandler
	public void onSnowballHit(final ProjectileHitEvent e) {
		Month month = AranarthUtils.getMonth();
		// If it is a winter month
		if (month == Month.UMBRAVOR || month == Month.GLACIVOR
				|| month == Month.FRIGORVOR || month == Month.OBSCURVOR) {
			if (e.getEntity() instanceof Snowball) {
				if (e.getHitEntity() != null) {
					if (e.getHitEntity() instanceof LivingEntity entity) {
						if (e.getEntity().getShooter() instanceof Entity shooter) {
							// 50% chance of adding 0.5 hearts of damage
							entity.damage(new Random().nextInt(2), shooter);
						}
					}
				}
			}
		}
	}
}
