package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FireTickDamageHigher implements Listener {

	public FireTickDamageHigher(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Increases fire tick damage during the month of Ardorvor.
	 * @param e The event.
	 */
	@EventHandler
	public void onFireDamage(final EntityDamageEvent e) {
		if (AranarthUtils.getMonth() == 6) {
			// Tripled when in fire
			if (e.getDamageSource().getDamageType() == DamageType.IN_FIRE) {
				e.setDamage(e.getDamage() * 3);
			}
			// Doubled when on fire
			else if (e.getDamageSource().getDamageType() == DamageType.ON_FIRE) {
				e.setDamage(e.getDamage() * 2);
			}
		}
	}
}
