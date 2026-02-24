package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.alchemy.AlchemyManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.aearost.aranarthcore.objects.CustomKeys.LINGERING_POTION_ID;

/**
 * Handles all logic regarding potions increasing alchemy when hitting a target.
 */
public class PotionAlchemyExpListener implements Listener {
	HashMap<UUID, Long> uuidToCloudIdentifier = new HashMap<>();

	public PotionAlchemyExpListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	/**
	 * Deals with cancelling explosion block damage.
	 * @param e The event.
	 */
	@EventHandler
	public void onSplashPotionHit(final PotionSplashEvent e) {
		if (e.getEntity().getShooter() instanceof Player player) {
			McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
			AlchemyManager alchemyManager = new AlchemyManager(mcMMOPlayer);

			for (LivingEntity entity : e.getAffectedEntities()) {
				int xp = 200;
				if (entity instanceof Player) {
					xp = 750;
				}
				alchemyManager.applyXpGain(xp, XPGainReason.PVE, XPGainSource.CUSTOM);
			}
		}
	}

	/**
	 * Deals with a lingering potion first hitting the ground/a target.
	 * @param e The event.
	 */
	@EventHandler
	public void onLingeringPotionHit(final LingeringPotionSplashEvent e) {
		if (e.getEntity().getShooter() instanceof Player player) {
			if (!uuidToCloudIdentifier.containsKey(player.getUniqueId())) {
				long lingeringPotionId = new Random().nextLong();
				uuidToCloudIdentifier.put(player.getUniqueId(), lingeringPotionId);
			}
		}
	}
	
	
	/**
	 * Deals with a lingering potion damaging an entity.
	 * @param e The event.
	 */
	@EventHandler
	public void onLingeringPotionHitEntity(final AreaEffectCloudApplyEvent e) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(e.getEntity().getOwnerUniqueId());
		if (offlinePlayer.isOnline()) {
			Player player = Bukkit.getPlayer(e.getEntity().getOwnerUniqueId());
			if (uuidToCloudIdentifier.containsKey(player.getUniqueId())) {
				McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
				AlchemyManager alchemyManager = new AlchemyManager(mcMMOPlayer);

				for (LivingEntity entity : e.getAffectedEntities()) {
					if (entity.getPersistentDataContainer().has(LINGERING_POTION_ID)) {
						continue;
					}

					int xp = 100;
					if (entity instanceof Player) {
						xp = 500;
					}
					alchemyManager.applyXpGain(xp, XPGainReason.PVE, XPGainSource.CUSTOM);
					entity.getPersistentDataContainer().set(
							LINGERING_POTION_ID, PersistentDataType.LONG, uuidToCloudIdentifier.get(player.getUniqueId()));

					new BukkitRunnable() {
						@Override
						public void run() {
							entity.getPersistentDataContainer().remove(LINGERING_POTION_ID);
						}
					}.runTaskLater(AranarthCore.getInstance(), 120L); // 6-second delay to avoid stacking the exp gain
				}
			}
		}
	}

	/**
	 * Deals with removing a lingering potion from the world.
	 * @param e The event.
	 */
	@EventHandler
	public void onLingeringPotionEnd(final EntityRemoveFromWorldEvent e) {
		if (e.getEntity() instanceof AreaEffectCloud cloud) {
            uuidToCloudIdentifier.remove(cloud.getOwnerUniqueId());
		}
	}

}
