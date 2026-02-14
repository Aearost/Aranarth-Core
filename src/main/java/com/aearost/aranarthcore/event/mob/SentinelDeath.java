package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Sentinel;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Deals with removing the sentinel from the player's list of sentinels when it dies.
 */
public class SentinelDeath {
	public void execute(final EntityDeathEvent e) {
		UUID uuidOfSentinel = e.getEntity().getUniqueId();
		UUID uuidOfPlayer = null;
		AranarthPlayer aranarthPlayerOfSentinel = null;
		Sentinel sentinelToRemove = null;
		for (AranarthPlayer aranarthPlayer : AranarthUtils.getAranarthPlayers().values()) {
			if (aranarthPlayer.getSentinels() == null || aranarthPlayer.getSentinels().get(e.getEntityType()) == null) {
				continue;
			}

			for (Sentinel sentinel : aranarthPlayer.getSentinels().get(e.getEntityType())) {
				if (sentinel.getUuid().equals(uuidOfSentinel)) {
					aranarthPlayerOfSentinel = aranarthPlayer;
					sentinelToRemove = sentinel;
					break;
				}
			}
			if (sentinelToRemove != null) {
				break;
			}
		}

		HashMap<EntityType, List<Sentinel>> sentinels = aranarthPlayerOfSentinel.getSentinels();
		List<Sentinel> sentinelsOfType = sentinels.get(e.getEntityType());
		sentinelsOfType.remove(sentinelToRemove);
		sentinels.put(e.getEntityType(), sentinelsOfType);
		aranarthPlayerOfSentinel.setSentinels(sentinels);
		AranarthUtils.setPlayer(AranarthUtils.getUuidOfAranarthPlayer(aranarthPlayerOfSentinel), aranarthPlayerOfSentinel);
	}
}
