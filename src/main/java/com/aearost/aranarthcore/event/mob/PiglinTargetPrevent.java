package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.PiglinAbstract;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Deals with preventing Piglins and Hoglins from targeting players.
 */
public class PiglinTargetPrevent {
	public void execute(EntityTargetEvent e) {
		if (e.getEntity() instanceof PiglinAbstract || e.getEntity() instanceof Hoglin) {
			if (e.getTarget() instanceof Player player) {
				if (AranarthUtils.isWearingArmorType(player, "scorched")) {
					e.setCancelled(true);
				}
			}
		}
	}
}
