package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Guardian;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Deals with removing the Guardian from the player's list of Guardians when it dies.
 */
public class GuardianDeath {
	public void execute(final EntityDeathEvent e) {
		UUID uuidOfGuardian = e.getEntity().getUniqueId();
		UUID uuidOfPlayer = null;
		AranarthPlayer aranarthPlayerOfGuardian = null;
		Guardian guardianToRemove = null;
		for (AranarthPlayer aranarthPlayer : AranarthUtils.getAranarthPlayers().values()) {
			if (aranarthPlayer.getGuardians() == null || aranarthPlayer.getGuardians().get(e.getEntityType()) == null) {
				continue;
			}

			for (Guardian guardian : aranarthPlayer.getGuardians().get(e.getEntityType())) {
				if (guardian.getUuid().equals(uuidOfGuardian)) {
					aranarthPlayerOfGuardian = aranarthPlayer;
					guardianToRemove = guardian;
					break;
				}
			}
			if (guardianToRemove != null) {
				break;
			}
		}

		HashMap<EntityType, List<Guardian>> guardians = aranarthPlayerOfGuardian.getGuardians();
		List<Guardian> guardiansOfType = guardians.get(e.getEntityType());
		guardiansOfType.remove(guardianToRemove);
		guardians.put(e.getEntityType(), guardiansOfType);
		aranarthPlayerOfGuardian.setGuardians(guardians);
		AranarthUtils.setPlayer(AranarthUtils.getUuidOfAranarthPlayer(aranarthPlayerOfGuardian), aranarthPlayerOfGuardian);
	}
}
