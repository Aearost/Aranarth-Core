package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Handles the logic behind a player vote being received by the server.
 */
public class VotifierListener implements Listener {

	public VotifierListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerVote(VotifierEvent e) {
		Bukkit.getLogger().info("A vote!!!");
	}
}
