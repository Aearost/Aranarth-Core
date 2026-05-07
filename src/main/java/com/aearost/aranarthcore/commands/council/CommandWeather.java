package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Updates the weather across all survival worlds.
 */
public class CommandWeather {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() != 3) {
				player.sendMessage(ChatUtils.chatMessage("&cYou cannot use this command!"));
				return true;
			}
		}

		if (args.length >= 2) {
			Month month = AranarthUtils.getMonth();
			World world = Bukkit.getWorld("world");
			World smp = Bukkit.getWorld("smp");
			World resource = Bukkit.getWorld("resource");
			Random random = new Random();

			if (args[1].equalsIgnoreCase("CLEAR")) {
				int delay = computeClearDelay(month, random);

				// Set duration to 0 before modifying world state to prevent the
				// WeatherChangeListener's storm-ending check from overriding this change
				AranarthUtils.setStormDuration(0);
				AranarthUtils.setWeather(Weather.CLEAR);

				world.setThunderDuration(0);
				world.setWeatherDuration(0);
				world.setThundering(false);
				world.setStorm(false);
				world.setClearWeatherDuration(delay);

				smp.setThunderDuration(0);
				smp.setWeatherDuration(0);
				smp.setThundering(false);
				smp.setStorm(false);
				smp.setClearWeatherDuration(delay);

				resource.setThunderDuration(0);
				resource.setWeatherDuration(0);
				resource.setThundering(false);
				resource.setStorm(false);
				resource.setClearWeatherDuration(delay);

				// Must match the -100 offset used throughout DateUtils to stay in sync
				AranarthUtils.setStormDelay(delay - 100);

				for (Player p : Bukkit.getOnlinePlayers()) {
					String pWorld = p.getWorld().getName();
					if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
					if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
					p.sendMessage(ChatUtils.chatMessage("&7&oThe storm has subsided..."));
					DateUtils.playClearSound(p);
				}

			} else if (args[1].equalsIgnoreCase("RAIN") || args[1].equalsIgnoreCase("THUNDER")) {
				boolean isThunder = args[1].equalsIgnoreCase("THUNDER");

				// Pure winter months only support snow; convert RAIN/THUNDER accordingly.
				// Ignivor and Umbravor can naturally rain and snow, so they are excluded.
				boolean isPureWinterMonth = DateUtils.isWinterMonth(month) && month != Month.UMBRAVOR;

				if (isPureWinterMonth) {
					int duration = computeSnowDuration(month, random);
					setSnow(world, smp, resource, duration);
				} else {
					// Ignivor, Umbravor, and all non-winter months: honour the requested type
					Weather type = isThunder ? Weather.THUNDER : Weather.RAIN;
					// At least 0.5 days, no more than 1.25 days
					int duration = random.nextInt(18000) + 12000;

					// Set AranarthUtils state before modifying world state so the
					// WeatherChangeListener does not cancel the incoming storm event
					AranarthUtils.setWeather(type);
					AranarthUtils.setStormDelay(0);

					world.setClearWeatherDuration(0);
					world.setStorm(true);
					world.setThundering(isThunder);
					world.setWeatherDuration(duration);
					if (isThunder) {
						world.setThunderDuration(duration);
					}

					smp.setClearWeatherDuration(0);
					smp.setStorm(true);
					smp.setThundering(isThunder);
					smp.setWeatherDuration(duration);
					if (isThunder) {
						smp.setThunderDuration(duration);
					}

					resource.setClearWeatherDuration(0);
					resource.setStorm(true);
					resource.setThundering(isThunder);
					resource.setWeatherDuration(duration);
					if (isThunder) {
						resource.setThunderDuration(duration);
					}

					// Must be set after world state changes to avoid interfering with the
					// WeatherChangeListener (matches DateUtils convention)
					AranarthUtils.setStormDuration(duration - 100);

					String broadcastMsg = isThunder ? "&7&oA thunderstorm has started..." : "&7&oIt has started to rain...";
					for (Player p : Bukkit.getOnlinePlayers()) {
						String pWorld = p.getWorld().getName();
						if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
						if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
						p.sendMessage(ChatUtils.chatMessage(broadcastMsg));
						if (isThunder) {
							DateUtils.playThunderStartSound(p);
						} else {
							DateUtils.playRainStartSound(p);
						}
					}
				}
			} else if (args[1].equalsIgnoreCase("DURATION") || args[1].equalsIgnoreCase("DELAY")) {
				if (args.length >= 3) {
					try {
						int value = Integer.parseInt(args[2]);
						if (args[1].equalsIgnoreCase("DURATION")) {
							// Only update the AranarthUtils tracking counter; no world state changes
							// needed here, as DateUtils manages the storm lifecycle via this counter
							AranarthUtils.setStormDuration(value);
							sender.sendMessage(ChatUtils.chatMessage("&7The duration of the storm will be &e" + value + " &7ticks"));
						} else {
							// Only update the AranarthUtils tracking counter; no world state changes
							// needed here, as DateUtils manages the storm delay via this counter
							AranarthUtils.setStormDelay(value);
							sender.sendMessage(ChatUtils.chatMessage("&7The delay until the next storm will be &e" + value + " &7ticks"));
						}
					} catch (NumberFormatException ex) {
						sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac weather " + args[1].toUpperCase() + " <ticks>"));
					}
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac weather " + args[1].toUpperCase() + " <ticks>"));
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac weather <CLEAR|RAIN|THUNDER|DURATION|DELAY>"));
			}
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/ac weather <CLEAR|RAIN|THUNDER|DURATION|DELAY>"));
		}
		return true;
	}

	/**
	 * Forces snow across all survival worlds, keeping AranarthUtils state in sync with DateUtils.
	 * Sets stormDuration to 0 before touching world state to prevent the WeatherChangeListener's
	 * storm-ending check from interfering, then restores the correct tracking value afterwards.
	 * @param world The main survival world.
	 * @param smp The SMP world.
	 * @param resource The resource world.
	 * @param duration The duration in ticks.
	 */
	private static void setSnow(World world, World smp, World resource, int duration) {
		// Set weather type before world changes; zero duration first to block the
		// WeatherChangeListener's storm-ending override when setStorm(false) fires
		AranarthUtils.setWeather(Weather.SNOW);
		AranarthUtils.setStormDuration(0);
		AranarthUtils.setStormDelay(0);

		world.setThunderDuration(0);
		world.setWeatherDuration(0);
		world.setThundering(false);
		world.setStorm(false);
		world.setClearWeatherDuration(duration);

		smp.setThunderDuration(0);
		smp.setWeatherDuration(0);
		smp.setThundering(false);
		smp.setStorm(false);
		smp.setClearWeatherDuration(duration);

		resource.setThunderDuration(0);
		resource.setWeatherDuration(0);
		resource.setThundering(false);
		resource.setStorm(false);
		resource.setClearWeatherDuration(duration);

		// Must be set after world state changes to avoid interfering with the
		// WeatherChangeListener (matches DateUtils convention)
		AranarthUtils.setStormDuration(duration - 100);

		for (Player p : Bukkit.getOnlinePlayers()) {
			String pWorld = p.getWorld().getName();
			if (pWorld.equals("arena") || pWorld.equals("creative")) continue;
			if (AranarthUtils.getPlayer(p.getUniqueId()).isWeatherMessageDisabled()) continue;
			p.sendMessage(ChatUtils.chatMessage("&7&oIt has started to snow..."));
			DateUtils.playSnowStartSound(p);
		}
	}

	/**
	 * Computes the delay before the next storm after clearing the weather. Uses the same
	 * random ranges DateUtils applies naturally when a storm ends for the given month.
	 * @param month The current month.
	 * @param random A Random instance.
	 * @return The delay in ticks.
	 */
	private static int computeClearDelay(Month month, Random random) {
		if (month == Month.IGNIVOR) {
			// At least 2 days, no more than 10 days
			return random.nextInt(240000) + 48000;
		} else if (month == Month.AQUINVOR) {
			// At least 0.25 days, no more than 2.25 days
			return random.nextInt(48000) + 6000;
		} else if (DateUtils.isWinterMonth(month)) {
			return switch (month) {
				case UMBRAVOR -> random.nextInt(102000) + 18000;  // At least 0.75 days, no more than 5 days
				case GLACIVOR -> random.nextInt(48000) + 12000;   // At least 0.5 days, no more than 2 days
				case FRIGORVOR -> random.nextInt(18000) + 6000;   // At least 0.25 days, no more than 1 day
				case OBSCURVOR -> random.nextInt(36000) + 12000;  // At least 0.5 days, no more than 1.5 days
				default -> random.nextInt(108000) + 12000;
			};
		} else {
			// At least 0.5 days, no more than 5 days
			return random.nextInt(108000) + 12000;
		}
	}

	/**
	 * Computes the snow duration for a snow-only month. Uses the same random ranges
	 * DateUtils applies naturally for snowstorms.
	 * @param month The current month.
	 * @param random A Random instance.
	 * @return The duration in ticks.
	 */
	private static int computeSnowDuration(Month month, Random random) {
		return switch (month) {
			case GLACIVOR -> random.nextInt(24000) + 12000;   // At least 0.5 days, no more than 1.5 days
			case FRIGORVOR -> random.nextInt(30000) + 18000;  // At least 0.75 days, no more than 2 days
			case OBSCURVOR -> random.nextInt(18000) + 6000;   // At least 0.25 days, no more than 1 day
			default -> random.nextInt(18000) + 12000;
		};
	}
}
