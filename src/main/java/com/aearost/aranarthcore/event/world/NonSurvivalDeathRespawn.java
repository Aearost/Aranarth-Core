package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class NonSurvivalDeathRespawn implements Listener {

	public NonSurvivalDeathRespawn(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}



}
