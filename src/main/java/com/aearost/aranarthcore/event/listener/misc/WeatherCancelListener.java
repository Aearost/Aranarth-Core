package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.Random;

public class WeatherCancelListener implements Listener {

	public WeatherCancelListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the weather from being set to raining during a winter month.
	 * Instead, manual particles from DateUtils will be displayed.
	 * @param e The event.
	 */
	@EventHandler
	public void onRain(WeatherChangeEvent e) {
		// If being set to raining during a winter month
		if (e.toWeatherState() && DateUtils.isWinterMonth(AranarthUtils.getMonth())) {
			e.setCancelled(true);
			return;
		}

		// If the rain is stopping during Aquinvor, manually add new randomizer
		if (!e.toWeatherState() && AranarthUtils.getMonth() == Month.AQUINVOR) {
			Random random = new Random();
			Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe rain has stopped..."));
			AranarthUtils.setIsStorming(false);
			// At least 0.25 days, no more than 20 days
			AranarthUtils.setStormDelay(random.nextInt(48000) + 6000);
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

	}

}
