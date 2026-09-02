package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.enums.WorldEvent;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SleepSkipListener implements Listener {

	public SleepSkipListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		if (NetworkManager.isActive()) {
			// Pass isRemoteUpdate=true so the callback re-evaluates the skip condition
			// and updates the local action bar without re-publishing back to the remote server.
			NetworkManager.getInstance().setRemoteSleepCallback(() -> updateSleepMessage(true));
		}
	}

	private int amountRequiredToSkip = 0;
	private int scheduledSkipTask = -1;
	private final List<UUID> sleepingPlayers = new ArrayList<>();

	/**
	 * Allows sleeping when the only nearby hostile mobs are defenders.
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerBedEnterNotSafe(final PlayerBedEnterEvent e) {
		if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.NOT_SAFE) {
			return;
		}
		// Check all nearby monsters within vanilla sleep-check radius (8 blocks)
		for (Entity nearby : e.getPlayer().getNearbyEntities(8, 5, 8)) {
			if (nearby instanceof Monster && !DefenderUtils.isDefender(nearby.getUniqueId())) {
				return; // Real hostile mob present - keep the block
			}
		}
		// Only defenders (if any) are nearby - allow sleeping
		e.setUseBed(Event.Result.ALLOW);
	}

	/**
	 * Allows for players to skip the day cycle in-game
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerSleep(final PlayerBedEnterEvent e) {
		// Lunaris blocks sleeping - we also need to prevent tracking this player as sleeping
		if (AranarthUtils.getActiveWorldEvent() == WorldEvent.LUNARIS) {
			long time = e.getPlayer().getWorld().getTime();
			if (time >= 12300 && time <= 23960) {
				return;
			}
		}

		if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
			sleepingPlayers.add(e.getPlayer().getUniqueId());
			updateSleepMessage();
		}
	}

	/**
	 * Captures when a player leaves a bed after sleeping.
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerLeaveBed(final PlayerBedLeaveEvent e) {
		sleepingPlayers.remove(e.getPlayer().getUniqueId());
		if (!sleepingPlayers.isEmpty()) {
			updateSleepMessage();
		}
	}

	/**
	 * Handles updating the boss bar with the current number of players sleeping in a bed.
	 */
	private void updateSleepMessage() {
		updateSleepMessage(false);
	}

	/**
	 * Handles updating the action bar with the current number of players sleeping in a bed.
	 * @param isRemoteUpdate True when triggered by a remote-server sleep event. Skips re-publishing
	 *                       to the remote server to prevent a count feedback loop.
	 */
	private void updateSleepMessage(boolean isRemoteUpdate) {
		int onlinePlayersInSurvivalWorlds = 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			String worldName = player.getLocation().getWorld().getName();
			if (worldName.equals("world") || AranarthUtils.isSmpWorld(worldName) || worldName.equals("resource")) {
				onlinePlayersInSurvivalWorlds++;
			}
		}
		// Include remote players so the sleep threshold accounts for the whole network
		if (NetworkManager.isActive()) {
			onlinePlayersInSurvivalWorlds += NetworkManager.getInstance().getRemoteSleepEligibleCount();
		}

		double percentRequiredToSkip = 0.333333333;
		// Increased amount needed during Obscurvor
		if (AranarthUtils.getMonth() == Month.OBSCURVOR) {
			percentRequiredToSkip = 0.666666666;
		}

		amountRequiredToSkip = (int) Math.ceil(onlinePlayersInSurvivalWorlds * percentRequiredToSkip);
		// Capture the local-only count before combining - this is what gets published so the
		// receiving server stores the correct per-server count (not a combined total).
		final int localSleepingCount = sleepingPlayers.size();
		int sleepingPlayerNum = localSleepingCount;
		// Include players sleeping on the other server so the combined count is accurate
		if (NetworkManager.isActive()) {
			sleepingPlayerNum += NetworkManager.getInstance().getRemoteSleepingCount();
		}
		final int totalSleepingPlayerNum = sleepingPlayerNum;
		String message = "Players sleeping: " + totalSleepingPlayerNum + "/" + amountRequiredToSkip;
		Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
			// Displays the bar to all players in the survival worlds
			for (Player player : Bukkit.getOnlinePlayers()) {
				String worldName = player.getLocation().getWorld().getName();
				if (worldName.equals("world") || AranarthUtils.isSmpWorld(worldName) || worldName.equals("resource")) {
					long time = player.getLocation().getWorld().getTime();
					if (time > 12500 && time <= 23980) {
						player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
					}
				}
			}
			// Publish local-only sleeping count to the remote server. Do not publish when this
			// update was itself triggered by a remote message - that would echo the count back
			// and cause the remote server to double-count it.
			if (!isRemoteUpdate && NetworkManager.isActive()) {
				NetworkManager.getInstance().publishSleepMessage(message, localSleepingCount, amountRequiredToSkip);
			}
		}, 1L);


		// Enough players are sleeping to skip the night
		if (totalSleepingPlayerNum >= amountRequiredToSkip) {
			skipNight();
		} else {
			doNotSkipNight();
		}
	}

	/**
	 * Handles the logic to skip the night cycle.
	 */
	private void skipNight() {
		// The skip is already scheduled
		if (scheduledSkipTask != -1) {
			return;
		}

		scheduledSkipTask = Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
			// If players have left their bed and there are no longer enough to skip the night
			int totalSleeping = sleepingPlayers.size();
			if (NetworkManager.isActive()) {
				totalSleeping += NetworkManager.getInstance().getRemoteSleepingCount();
			}
			if (totalSleeping < amountRequiredToSkip) {
				scheduledSkipTask = -1;
				return;
			}

			// Skip the night
			for (World w : AranarthUtils.getSyncWorlds()) {
				w.setTime(23980);
			}

			// Immediately end any storm, will be picked up by DateUtils logic within 5 seconds
			if (AranarthUtils.getWeather() != Weather.CLEAR) {
				AranarthUtils.setStormDuration(0);
			}

			if (NetworkManager.isActive()) {
				NetworkManager.getInstance().publishSyncTime(23980);
			}

			scheduledSkipTask = -1;
			}, 60 // 3 seconds of sleeping required
		).getTaskId();
	}

	/**
	 * Handles the logic when there are not enough players to skip the night cycle.
	 */
	private void doNotSkipNight() {
		if (scheduledSkipTask != -1) {
			Bukkit.getScheduler().cancelTask(scheduledSkipTask);
			scheduledSkipTask = -1;
		}
	}

	/**
	 * Bypasses the vanilla skipping of night to allow the plugin to handle the logic of setting the time to day.
	 * @param e The skip event.
	 */
	@EventHandler
	public void onTimeSkip(TimeSkipEvent e) {
		if (e.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
			e.setCancelled(true);
		}
	}
}
