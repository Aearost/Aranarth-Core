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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static com.aearost.aranarthcore.objects.CustomKeys.LINGERING_POTION_ID;

/**
 * Handles all logic regarding potions increasing alchemy when hitting a target.
 */
public class PotionAlchemyExpListener implements Listener {
	HashMap<UUID, Long> uuidToCloudIdentifier = new HashMap<>();

	// Tracks potion-applied effects: entityId -> (effectType -> original duration when the potion applied it).
	// Only populated for effects whose cause is POTION or AREA_EFFECT_CLOUD, so non-potion sources
	// (beacons, commands, etc.) are never subject to the 80%-elapsed restriction.
	private final HashMap<UUID, HashMap<PotionEffectType, Integer>> potionEffectDurations = new HashMap<>();

	public PotionAlchemyExpListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Keeps {@code potionEffectDurations} in sync with the actual state of each entity's effects.
	 * Runs at MONITOR priority so we only record effects that were not cancelled by another plugin.
	 * @param e The event.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityPotionEffect(final EntityPotionEffectEvent e) {
		UUID entityId = e.getEntity().getUniqueId();

		if (e.getAction() == EntityPotionEffectEvent.Action.CLEARED) {
			potionEffectDurations.remove(entityId);
			return;
		}

		PotionEffectType effectType = e.getModifiedType();

		if (e.getAction() == EntityPotionEffectEvent.Action.REMOVED) {
			Map<PotionEffectType, Integer> tracked = potionEffectDurations.get(entityId);
			if (tracked != null) {
				tracked.remove(effectType);
				if (tracked.isEmpty()) {
					potionEffectDurations.remove(entityId);
				}
			}
			return;
		}

		// ADDED or CHANGED — only track if the effect was applied by a potion.
		EntityPotionEffectEvent.Cause cause = e.getCause();
		if (cause == EntityPotionEffectEvent.Cause.POTION_DRINK ||
				cause == EntityPotionEffectEvent.Cause.POTION_SPLASH ||
				cause == EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD) {
			if (e.getNewEffect() != null) {
				potionEffectDurations.computeIfAbsent(entityId, k -> new HashMap<>())
						.put(effectType, e.getNewEffect().getDuration());
			}
		} else {
			// Effect overwritten by a non-potion source — remove tracking so the restriction no longer applies.
			Map<PotionEffectType, Integer> tracked = potionEffectDurations.get(entityId);
			if (tracked != null) {
				tracked.remove(effectType);
			}
		}
	}

	/**
	 * Returns whether a potion application should grant Alchemy XP to the given entity.
	 * XP is denied if any effect in the potion is already active on the entity (applied by a
	 * prior potion) and less than 80% of its original duration has elapsed (i.e., more than
	 * 20% remains), preventing instant-reapplication grinding.
	 * @param entity The entity receiving the potion effects.
	 * @param potionEffects The effects contained in the potion being applied.
	 * @return true if XP should be granted, false otherwise.
	 */
	private boolean effectAllowsXp(LivingEntity entity, Collection<PotionEffect> potionEffects) {
		Map<PotionEffectType, Integer> trackedEffects =
				potionEffectDurations.getOrDefault(entity.getUniqueId(), new HashMap<>());

		for (PotionEffect appliedEffect : potionEffects) {
			PotionEffectType effectType = appliedEffect.getType();
			PotionEffect existingEffect = entity.getPotionEffect(effectType);

			// Effect is not currently active — no restriction needed.
			if (existingEffect == null) continue;

			// Only restrict if the active effect was itself applied by a potion.
			Integer originalDuration = trackedEffects.get(effectType);
			if (originalDuration == null || originalDuration <= 0) continue;

			// Deny XP if more than 20% of the original duration remains (< 80% has elapsed).
			if ((double) existingEffect.getDuration() / originalDuration > 0.2) {
				return false;
			}
		}
		return true;
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
			Collection<PotionEffect> potionEffects = e.getPotion().getEffects();

			for (LivingEntity entity : e.getAffectedEntities()) {
				if (!effectAllowsXp(entity, potionEffects)) {
					continue;
				}
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
		UUID uuid = e.getEntity().getOwnerUniqueId();
		if (uuid != null) {
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
			if (offlinePlayer.isOnline()) {
				Player player = Bukkit.getPlayer(uuid);
				if (uuidToCloudIdentifier.containsKey(player.getUniqueId())) {
					McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
					AlchemyManager alchemyManager = new AlchemyManager(mcMMOPlayer);
					List<PotionEffect> cloudEffects = e.getEntity().getCustomEffects();

					for (LivingEntity entity : e.getAffectedEntities()) {
						if (entity.getPersistentDataContainer().has(LINGERING_POTION_ID)) {
							continue;
						}
						if (!effectAllowsXp(entity, cloudEffects)) {
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
