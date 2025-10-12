package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Deals with preventing Piglins and Brute Piglins from targeting players.
 */
public class PiglinTargetPrevent {
	public void execute(EntityTargetEvent e) {
		if (e.getEntity() instanceof PiglinAbstract) {
			if (e.getTarget() instanceof Player player) {
				if (AranarthUtils.isWearingArmorType(player, "scorched")) {
					e.setCancelled(true);
				}
			}
		}
	}
}
