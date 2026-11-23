package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.abilities.airbending.soundbending.Amplification;
import com.aearost.aranarthcore.integration.SquaremapIntegration;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.ECHO_PULSED;

/**
 * Handles the effects of Echo Aranarthium armour.
 */
public class EchoArmourUtils {

	private static final double HEARING_RANGE = 8;
	private static final int REVEAL_DURATION_TICKS = 100;
	private static final long REVEAL_SOUND_COOLDOWN_MS = 1000;
	private static final double OCCLUSION_STEP = 0.5;

	private static final Set<UUID> wearers = new HashSet<>();
	private static final Set<UUID> hiddenFromMap = new HashSet<>();
	private static final Map<UUID, Long> lastRevealSound = new HashMap<>();

	/**
	 * Updates who is wearing Echo Aranarthium, hiding new wearers from the live map and showing players that took it off.
	 */
	public static void refreshWearers() {
		Set<UUID> current = new HashSet<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (AranarthUtils.isWearingArmorType(player, "echo")) {
				current.add(player.getUniqueId());
			}
		}

		for (UUID uuid : List.copyOf(wearers)) {
			if (!current.contains(uuid)) {
				wearers.remove(uuid);
				lastRevealSound.remove(uuid);
				showOnMap(uuid);
			}
		}
		for (UUID uuid : current) {
			if (wearers.add(uuid)) {
				hideFromMap(uuid);
			}
		}
	}

	/**
	 * Shows every wearer on the live map again, used when the plugin is disabled.
	 */
	public static void disable() {
		for (UUID uuid : List.copyOf(hiddenFromMap)) {
			showOnMap(uuid);
		}
		wearers.clear();
	}

	/**
	 * Provides whether the player is currently wearing a full set of Echo Aranarthium.
	 *
	 * @param player The player.
	 * @return Whether the player is wearing Echo Aranarthium.
	 */
	public static boolean isWearer(Player player) {
		return wearers.contains(player.getUniqueId());
	}

	private static void hideFromMap(UUID uuid) {
		if (!Bukkit.getPluginManager().isPluginEnabled("squaremap")) {
			return;
		}
		// Players that are already hidden (i.e /squaremap hide) are left alone so they are never shown by mistake
		if (SquaremapIntegration.isPlayerHidden(uuid)) {
			return;
		}
		SquaremapIntegration.setPlayerHidden(uuid, true);
		hiddenFromMap.add(uuid);
	}

	private static void showOnMap(UUID uuid) {
		if (!hiddenFromMap.remove(uuid) || !Bukkit.getPluginManager().isPluginEnabled("squaremap")) {
			return;
		}
		SquaremapIntegration.setPlayerHidden(uuid, false);
	}

	/**
	 * Silences every vibration made by a wearer, and lets wearers hear every vibration made near them.
	 *
	 * @param e The event.
	 */
	public static void onGameEvent(GenericGameEvent e) {
		if (wearers.isEmpty() || e.isAsynchronous()) {
			return;
		}
		GameEvent gameEvent = e.getEvent();
		if (!Tag.GAME_EVENT_VIBRATIONS.isTagged(gameEvent)) {
			return;
		}

		Entity source = e.getEntity();
		// Wearers never emit vibrations, so sculk sensors, shriekers and Wardens never pick them up
		if (isMadeByWearer(source)) {
			e.setCancelled(true);
			return;
		}
		// Matches sculk sensors, which ignore careful movements such as sneaking
		if (source instanceof Player player && player.isSneaking() && Tag.GAME_EVENT_IGNORE_VIBRATIONS_SNEAKING.isTagged(gameEvent)) {
			return;
		}

		Location origin = e.getLocation();
		for (UUID uuid : List.copyOf(wearers)) {
			Player wearer = Bukkit.getPlayer(uuid);
			if (wearer == null || wearer.isDead() || wearer.getGameMode() == GameMode.SPECTATOR
					|| !wearer.getWorld().equals(origin.getWorld())) {
				continue;
			}
			Location ear = wearer.getEyeLocation();
			if (ear.distanceSquared(origin) > HEARING_RANGE * HEARING_RANGE || isOccluded(origin, ear)) {
				continue;
			}
			hear(wearer, source);
		}
	}

	/**
	 * Provides whether a vibration was made by a wearer, their projectiles, or anything they are riding.
	 */
	private static boolean isMadeByWearer(Entity source) {
		if (source == null) {
			return false;
		}
		if (source instanceof Player player) {
			return wearers.contains(player.getUniqueId());
		}
		if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
			return wearers.contains(shooter.getUniqueId());
		}
		for (Entity passenger : source.getPassengers()) {
			if (passenger instanceof Player player && wearers.contains(player.getUniqueId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Provides whether a block that blocks vibrations (i.e wool) is between the two locations, as with sculk sensors.
	 */
	private static boolean isOccluded(Location from, Location to) {
		Vector direction = to.toVector().subtract(from.toVector());
		double distance = direction.length();
		if (distance < OCCLUSION_STEP) {
			return false;
		}
		direction.normalize().multiply(OCCLUSION_STEP);
		Location point = from.clone();
		for (double travelled = OCCLUSION_STEP; travelled < distance; travelled += OCCLUSION_STEP) {
			point.add(direction);
			if (Tag.OCCLUDES_VIBRATION_SIGNALS.isTagged(point.getBlock().getType())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Handles a wearer hearing a vibration, which fully charges Amplification and reveals whatever made it.
	 */
	private static void hear(Player wearer, Entity source) {
		Amplification.echoCharge(wearer);

		if (!SpecialArrowUtils.canReveal(wearer, source)) {
			return;
		}
		if (SpecialArrowUtils.revealTo(wearer, source, REVEAL_DURATION_TICKS)) {
			playRevealSound(wearer);
		}
	}

	/**
	 * Plays a quiet sculk click to the wearer only when something new is revealed, at most once a second.
	 */
	private static void playRevealSound(Player wearer) {
		long now = System.currentTimeMillis();
		Long last = lastRevealSound.get(wearer.getUniqueId());
		if (last != null && now - last < REVEAL_SOUND_COOLDOWN_MS) {
			return;
		}
		lastRevealSound.put(wearer.getUniqueId(), now);
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(wearer.getUniqueId());
		int volume = aranarthPlayer == null ? 100 : aranarthPlayer.getAranarthiumSoundVolume();
		if (volume > 0) {
			wearer.playSound(wearer, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f * (volume / 100f), 1.4f);
		}
	}

	/**
	 * Sends out a sonar pulse where an arrow shot by a wearer lands, only once per arrow.
	 *
	 * @param arrow The arrow that landed.
	 */
	public static void onArrowLand(AbstractArrow arrow) {
		if (!(arrow.getShooter() instanceof Player shooter) || !AranarthUtils.isWearingArmorType(shooter, "echo")) {
			return;
		}
		if (arrow.getPersistentDataContainer().has(ECHO_PULSED)) {
			return;
		}
		arrow.getPersistentDataContainer().set(ECHO_PULSED, PersistentDataType.BOOLEAN, true);
		SpecialArrowUtils.sonarPulse(shooter, arrow.getLocation());
	}
}
