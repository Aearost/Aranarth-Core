package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Deals with preventing Guardians and Elder Guardians from targeting players.
 */
public class GuardianTargetPrevent {
	public void execute(EntityTargetEvent e) {
		if (e.getEntity() instanceof Guardian guardian) {
			if (e.getTarget() instanceof Player player) {
				if (AranarthUtils.isArmorType(player, "aquatic")) {
					e.setCancelled(true);
				}
			}
		}
	}
}
