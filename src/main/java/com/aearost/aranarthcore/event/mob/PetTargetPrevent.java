package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Deals with preventing pets from targeting players if they're in a positive Dominion relation.
 */
public class PetTargetPrevent {
	public void execute(EntityTargetEvent e) {
		if (e.getEntity() instanceof Tameable pet) {
			if (pet.isTamed()) {
				if (e.getTarget() instanceof Player target) {
					Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());
					Dominion petOwnerDominion = DominionUtils.getPlayerDominion(pet.getOwner().getUniqueId());
					if (targetDominion != null && petOwnerDominion != null) {
						if (targetDominion.getLeader().equals(petOwnerDominion.getLeader())
							|| DominionUtils.areAllied(targetDominion, petOwnerDominion) || DominionUtils.areTruced(targetDominion, petOwnerDominion)) {
							e.setCancelled(true);
						}
					}
				}
			}
		}
	}
}
