package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.QuiverFireworkBoost;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Centralizes all logic to be called when a player boosts their elytra flight with a firework rocket.
 */
public class PlayerElytraBoostEventListener implements Listener {

	public PlayerElytraBoostEventListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void execute(PlayerElytraBoostEvent e) {
		new QuiverFireworkBoost().execute(e);
	}
}
