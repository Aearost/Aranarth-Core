package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.ARROW;

/**
 * Deals with dropping extra drops when an animal is killed with a Bone Arrow.
 */
public class ExtraDropsFromBoneArrow {
	public void execute(EntityDeathEvent e) {
		if (e.getDamageSource().getDirectEntity() instanceof Arrow arrow) {
			String arrowType = arrow.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
			if (arrowType != null && arrowType.equals("bone")) {
				AranarthUtils.increaseMobDrops(e);
			}
		}
	}
}
