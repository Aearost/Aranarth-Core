package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;

public class MobDestroyDoorPrevent implements Listener {

	public MobDestroyDoorPrevent(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Deals with preventing non-player entities from destroying doors.
	 * @param e The event.
	 */
	@EventHandler
	public void onDoorDestroy(final EntityBreakDoorEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			e.setCancelled(true);
		}
	}
}
