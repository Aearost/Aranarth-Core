package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.utils.AranarthUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SleepSkipListener implements Listener {

	public SleepSkipListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	private int amountRequiredToSkip = 0;
	private int scheduledSkipTask = -1;
	private final List<UUID> sleepingPlayers = new ArrayList<>();

	/**
	 * Allows for players to skip the day cycle in-game
	 * @param e The event.
	 */
	@EventHandler
	public void onPlayerSleep(final PlayerBedEnterEvent e) {
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
		int onlinePlayersInSurvivalWorlds = 0;
		for (Player player : Bukkit.getOnlinePlayers()) {
			String worldName = player.getLocation().getWorld().getName();
			if (worldName.equals("world") || worldName.equals("smp") || worldName.equals("resource")) {
				onlinePlayersInSurvivalWorlds++;
			}
		}

		double percentRequiredToSkip = 0.333333333;
		// Increased amount needed during Obscurvor
		if (AranarthUtils.getMonth() == Month.OBSCURVOR) {
			percentRequiredToSkip = 0.666666666;
		}

		amountRequiredToSkip = (int) Math.ceil(onlinePlayersInSurvivalWorlds * percentRequiredToSkip);
		int sleepingPlayerNum = sleepingPlayers.size();
		String message = "Players sleeping: " + sleepingPlayerNum + "/" + amountRequiredToSkip;
		Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
			// Displays the bar to all players in the survival worlds
			for (Player player : Bukkit.getOnlinePlayers()) {
				String worldName = player.getLocation().getWorld().getName();
				if (worldName.equals("world") || worldName.equals("smp") || worldName.equals("resource")) {
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
				}
			}
		}, 1L);


		// Enough players are sleeping to skip the night
		if (sleepingPlayerNum >= amountRequiredToSkip) {
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
			if (sleepingPlayers.size() < amountRequiredToSkip) {
				scheduledSkipTask = -1;
				return;
			}

			// Skip the night
			World world = Bukkit.getWorld("world");
			World smp = Bukkit.getWorld("smp");
			World resource = Bukkit.getWorld("resource");
			world.setTime(23980);
			smp.setTime(23980);
			resource.setTime(23980);

			// Immediately end any storm, will be picked up by DateUtils logic within 5 seconds
			if (AranarthUtils.getWeather() != Weather.CLEAR) {
				AranarthUtils.setStormDuration(0);
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
}
