package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class RainbowExpBoostListener implements Listener {

	public RainbowExpBoostListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Increases EXP gained in the survival worlds while the Aquinvor rainbow is shining.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerExpChange(PlayerExpChangeEvent e) {
		int amount = e.getAmount();
		if (amount <= 0 || !AranarthUtils.isRainbowActive()) {
			return;
		}
		if (!AranarthUtils.isSurvivalWorld(e.getPlayer().getWorld().getName())) {
			return;
		}
		// EXP that was already owned by a player is never boosted, otherwise it could be multiplied in a loop
		if (e.getSource() instanceof ExperienceOrb orb) {
			ExperienceOrb.SpawnReason reason = orb.getSpawnReason();
			if (reason == ExperienceOrb.SpawnReason.EXP_BOTTLE
					|| reason == ExperienceOrb.SpawnReason.GRINDSTONE
					|| reason == ExperienceOrb.SpawnReason.PLAYER_DEATH) {
				return;
			}
		}
		e.setAmount(EnchantingUtils.randomRound(amount * AranarthUtils.RAINBOW_MULTIPLIER));
	}

}
