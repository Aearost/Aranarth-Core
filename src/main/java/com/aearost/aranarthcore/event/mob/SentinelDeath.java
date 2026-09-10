package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Sentinel;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
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
		AranarthPlayer aranarthPlayerOfSentinel = null;
		Sentinel sentinelToRemove = null;
		EntityType sentinelTypeToRemove = null;
		// Search all tracked sentinel types by UUID rather than the event entity type.
		// SentinelMark stores any AbstractHorse (Donkey, Mule, etc.) as HORSE, so
		// matching only on e.getEntityType() would miss those variants when they die.
		EntityType[] trackedTypes = {EntityType.HORSE, EntityType.IRON_GOLEM, EntityType.WOLF};
		for (AranarthPlayer aranarthPlayer : AranarthUtils.getAranarthPlayers().values()) {
			if (aranarthPlayer.getSentinels() == null) {
				continue;
			}

			for (EntityType trackedType : trackedTypes) {
				List<Sentinel> sentinelsOfType = aranarthPlayer.getSentinels().get(trackedType);
				if (sentinelsOfType == null) {
					continue;
				}
				for (Sentinel sentinel : sentinelsOfType) {
					if (sentinel.getUuid().equals(uuidOfSentinel)) {
						aranarthPlayerOfSentinel = aranarthPlayer;
						sentinelToRemove = sentinel;
						sentinelTypeToRemove = trackedType;
						break;
					}
				}
				if (sentinelToRemove != null) {
					break;
				}
			}
			if (sentinelToRemove != null) {
				break;
			}
		}

		if (aranarthPlayerOfSentinel != null) {
			HashMap<EntityType, List<Sentinel>> sentinels = aranarthPlayerOfSentinel.getSentinels();
			List<Sentinel> sentinelsOfType = sentinels.get(sentinelTypeToRemove);
			sentinelsOfType.remove(sentinelToRemove);
			sentinels.put(sentinelTypeToRemove, sentinelsOfType);
			aranarthPlayerOfSentinel.setSentinels(sentinels);
			AranarthUtils.setPlayer(AranarthUtils.getUuidOfAranarthPlayer(aranarthPlayerOfSentinel), aranarthPlayerOfSentinel);
			PersistenceUtils.syncPlayerSentinelsToDatabase(AranarthUtils.getUuidOfAranarthPlayer(aranarthPlayerOfSentinel));
		} else if (NetworkManager.isActive()) {
			// Owner is on a different server - notify them so they can clean up their sentinel list
			NetworkManager.getInstance().publishSentinelDeath(uuidOfSentinel, e.getEntityType());
		}
	}
}
