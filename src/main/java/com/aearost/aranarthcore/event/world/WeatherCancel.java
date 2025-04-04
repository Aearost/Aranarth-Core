package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherCancel implements Listener {

	public WeatherCancel(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Prevents the weather from being set to raining during a winter month.
	 * Instead, manual particles from DateUtils will be displayed.
	 * @param e The event.
	 */
	@EventHandler
	public void onRain(final WeatherChangeEvent e) {
		// If being set to raining during a winter month
		if (e.toWeatherState() && DateUtils.isWinterMonth(AranarthUtils.getMonthName())) {
			e.setCancelled(true);
		}
	}


}
