package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.ArrowConsume;
import com.aearost.aranarthcore.event.player.SpecialArrowShoot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

/**
 * Centralizes all logic to be called when an arrow is shot from a bow.
 */
public class EntityShootBowEventListener implements Listener {

	public EntityShootBowEventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void execute(EntityShootBowEvent e) {
		if (e.getEntity() instanceof Player) {
			new ArrowConsume().execute(e);
			new SpecialArrowShoot().execute(e);
		}
	}


}
