package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Random;

public class WeatherChangeListener implements Listener {

	public WeatherChangeListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Overrides various weather behaviour triggered by a change in the weather.
	 * @param e The event.
	 */
	@EventHandler
	public void onRain(WeatherChangeEvent e) {
		// If being set to raining during a winter month
		if (e.toWeatherState() && DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
			e.setCancelled(true);
			return;
		}

		// Prevent rain from stopping during Aquinvor before duration ends (i.e sleeping in bed)
		if (!e.toWeatherState() && AranarthUtils.getMonth() == Month.AQUINVOR && AranarthUtils.getIsStorming()) {
			Random random = new Random();
			AranarthUtils.setIsStorming(false);
			// At least 0.25 days, no more than 2.25 days
			int duration = random.nextInt(48000) + 6000;
			World world = Bukkit.getWorld("world");
			world.setWeatherDuration(0);
			world.setStorm(false);
			world.setClearWeatherDuration(duration);
			AranarthUtils.setStormDelay(duration);
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe rain has subsided..."));
		}

		// If there's a snowstorm during the month of Ignivor, only allow rain while it is not snowing
		if (e.toWeatherState() && AranarthUtils.getMonth() == Month.IGNIVOR) {
			if (AranarthUtils.getStormDuration() >= 100) {
				e.setCancelled(true);
				return;
			}
		}

		// Prevent automatic rainfall during the month of Aquinvor
		if (e.toWeatherState() && AranarthUtils.getMonth() == Month.AQUINVOR && !AranarthUtils.getIsStorming()) {
			e.setCancelled(true);
			return;
		}

		// If there is no special weather functionality in the month
		if (!DateUtils.isWinterMonth(AranarthUtils.getMonth())
				&& AranarthUtils.getMonth() != Month.AQUINVOR && AranarthUtils.getMonth() != Month.AESTIVOR) {
			if (e.toWeatherState()) {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oIt has started to rain..."));
			} else {
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe rain has subsided..."));
			}
		}

	}

}
