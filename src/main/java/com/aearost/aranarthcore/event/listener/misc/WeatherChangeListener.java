package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
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
	public void onWeatherChange(WeatherChangeEvent e) {
		// Storm is starting
		if (e.toWeatherState()) {
			// Winter months checks
			if (DateUtils.isWinterMonth(AranarthUtils.getMonth()) || AranarthUtils.getMonth() == Month.IGNIVOR) {
				// Always prevent rain in winter months that are not Ignivor
				if (AranarthUtils.getMonth() != Month.IGNIVOR) {
					e.setCancelled(true);
					return;
				}

				// Ignivor can only rain if not snowing
				if (AranarthUtils.getMonth() == Month.IGNIVOR && AranarthUtils.getWeather() == Weather.SNOW) {
					e.setCancelled(true);
					return;
				}
			}

			// If there's a storm that is starting before it is supposed to
			if (AranarthUtils.getStormDelay() >= 100 && AranarthUtils.getWeather() == Weather.CLEAR) {
				e.setCancelled(true);
				return;
			}
		}
		// Storm is stopping
		else {
			// If storm is ending before the duration is over (i.e sleeping in bed)
			if (AranarthUtils.getStormDuration() > 100 && AranarthUtils.getStormDelay() <= 0) {
				Random random = new Random();
				AranarthUtils.setWeather(Weather.CLEAR);
				// At least 0.25 days, no more than 2.25 days
//				int delay = random.nextInt(48000) + 6000; TODO
				int delay = 300;

				if (AranarthUtils.getMonth() == Month.AQUINVOR) {
					// At least 0.25 days, no more than 2.25 days
//					delay = random.nextInt(48000) + 6000; TODO
					delay = 300;
				}

				AranarthUtils.setStormDelay(delay);
				AranarthUtils.setStormDuration(0);
				Bukkit.broadcastMessage(ChatUtils.chatMessage("CCC"));
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
				return;
			}
		}

		// If there is no special weather functionality in the month
//		if (!DateUtils.isWinterMonth(AranarthUtils.getMonth()) && AranarthUtils.getMonth() != Month.IGNIVOR
//				&& AranarthUtils.getMonth() != Month.AQUINVOR && AranarthUtils.getMonth() != Month.AESTIVOR) {
//			Bukkit.broadcastMessage(ChatUtils.chatMessage("BBB"));
//			if (e.toWeatherState()) {
//				if (AranarthUtils.getWeather() == Weather.RAIN) {
//					Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oIt has started to rain..."));
//				} else {
//					Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oA thunderstorm has started..."));
//				}
//			} else {
//				Bukkit.broadcastMessage(ChatUtils.chatMessage("DDD"));
//				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
//			}
//		}

	}

}
