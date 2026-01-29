package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.taming.TamingManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;


public class AnimalBreedingListener implements Listener {

	public AnimalBreedingListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Provides XP for the Taming mcMMO skill when breeding.
	 * @param e The event.
	 */
	@EventHandler
	public void onAnimalTame(final EntityBreedEvent e) {
		McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(e.getBreeder());
		if (mcMMOPlayer != null) {
			TamingManager tamingManager = mcMMOPlayer.getTamingManager();
			if (e.getEntityType() == EntityType.HORSE || e.getEntityType() == EntityType.DONKEY) {
				tamingManager.applyXpGain(150, XPGainReason.PVE);
			} else {
				tamingManager.applyXpGain(75, XPGainReason.PVE);
			}
		}
	}
}
