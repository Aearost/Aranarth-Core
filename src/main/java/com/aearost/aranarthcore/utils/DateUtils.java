package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.util.*;

/**
 * Provides utility methods to facilitate the formatting of all date related content.
 */
public class DateUtils {

	private final int month;
	private final int day;
	
	public DateUtils() {
		this.month = getMonth();
		this.day = getDay();
	}

	/**
	 * Provides the current month as an integer.
	 *
	 * @return The current month as an integer.
	 */
	private int getMonth() {
		return LocalDate.now().getMonthValue();
	}

	/**
	 * Provides the current date of the month as an integer.
	 *
	 * @return The current date of the month as an integer.
	 */
	private int getDay() {
		return LocalDate.now().getDayOfMonth();
	}

	/**
	 * Confirms if the current date is within the general range of Valentine's Day.
	 *
	 * @return Confirmation of whether it is roughly Valentine's Day.
	 */
	public boolean isValentinesDay() {
		if (this.month == 2) {
            return this.day >= 4 && this.day <= 14;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Easter.
	 *
	 * @return Confirmation of whether it is roughly Easter.
	 */
	public boolean isEaster() {
		if (this.month == 3) {
            return this.day >= 22 && this.day <= 31;
		} else if (this.month == 4) {
            return this.day >= 1 && this.day <= 25;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Halloween.
	 *
	 * @return Confirmation of whether it is roughly Halloween.
	 */
	public boolean isHalloween() {
		if (this.month == 10) {
            return this.day >= 20 && this.day <= 31;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Christmas.
	 *
	 * @return Confirmation of whether it is roughly Christmas.
	 */
	public boolean isChristmas() {
		if (this.month == 12) {
            return this.day >= 15 && this.day <= 25;
		}
		return false;
	}

	/**
	 * Identifies and re-evaluates the current server date and applies effects based on the given month.
	 */
	public void calculateServerDate() {
		int time = (int) (Bukkit.getWorld("world").getTime() / 20);

		// Gets current server year
		int dayNum = AranarthUtils.getDay();
		int weekdayNum = AranarthUtils.getWeekday();
		int monthNum = AranarthUtils.getMonth();
		int yearNum = AranarthUtils.getYear();
		boolean isNewMonth = false;

		// If it is a new day
		// First 5 seconds of a new day
		if (time >= 0 && time < 5) {
			// Calculates day number based on length of month
			if (checkIfExceedsMonth(dayNum, monthNum)) {
				dayNum = 1;
				if (monthNum == 14) {
					monthNum = 0;
					yearNum++;
				} else {
					monthNum++;
				}
				isNewMonth = true;
			} else {
				dayNum++;
			}

			if (weekdayNum == 7) {
				weekdayNum = 0;
			} else {
				weekdayNum++;
			}

			AranarthUtils.setDay(dayNum);
			AranarthUtils.setWeekday(weekdayNum);
			AranarthUtils.setMonth(monthNum);
			AranarthUtils.setYear(yearNum);

			String monthName = provideMonthName(monthNum);
			if (monthName == null) {
				Bukkit.getLogger().info("Something went wrong with calculating the month name!");
				return;
			}

			String weekdayName = provideWeekdayName(weekdayNum);
			if (weekdayName == null) {
				Bukkit.getLogger().info("Something went wrong with calculating the weekday name!");
				return;
			}

			displayServerDate(dayNum, weekdayName, monthName, yearNum, isNewMonth);
		}
		determineMonthEffects();
	}

	/**
	 * Provides the server's month name based on the numeric value.
	 * @param monthNum The numeric month value.
	 * @return The actual name of the month.
	 */
	private String provideMonthName(int monthNum) {
		if (monthNum == 0) {
			return "Ignivór";
		}
		else if (monthNum == 1) {
			return "Aquinvór";
		} else if (monthNum == 2) {
			return "Nebulivór";
		} else if (monthNum == 3) {
			return "Ventirór";
		} else if (monthNum == 4) {
			return "Florivór";
		} else if (monthNum == 5) {
			return "Calorvór";
		} else if (monthNum == 6) {
			return "Solarvór";
		} else if (monthNum == 7) {
			return "Aestivór";
		} else if (monthNum == 8) {
			return "Ardorvór";
		} else if (monthNum == 9) {
			return "Fructivór";
		} else if (monthNum == 10) {
			return "Follivór";
		} else if (monthNum == 11) {
			return "Umbravór";
		} else if (monthNum == 12) {
			return "Glacivór";
		} else if (monthNum == 13) {
			return "Frigorvór";
		} else if (monthNum == 14) {
			return "Obscurvór";
		} else {
			return null;
		}
	}

	/**
	 * Provides the server's weekday name based on the numeric value.
	 * @param weekdayNum The numeric weekday value.
	 * @return The actual name of the weekday.
	 */
	private String provideWeekdayName(int weekdayNum) {
		if (weekdayNum == 0) {
			return "Hydris";
		} else if (weekdayNum == 1) {
			return "Terris";
		}
		else if (weekdayNum == 2) {
			return "Pyris";
		}
		else if (weekdayNum == 3) {
			return "Aeris";
		}
		else if (weekdayNum == 4) {
			return "Ferris";
		}
		else if (weekdayNum == 5) {
			return "Sylvis";
		}
		else if (weekdayNum == 6) {
			return "Umbris";
		}
		else if (weekdayNum == 7) {
			return "Aethis";
		} else {
			return null;
		}
	}

	/**
	 * Determines if the day is exceeding the current month's length.
	 * @param day The current server day.
	 * @param month The current server month.
	 * @return Whether the day is exceeding the current month's length.
	 */
	private boolean checkIfExceedsMonth(int day, int month) {
		// Ignivór
		if (month == 0) {
            return day > 147;
		}
		// Aquinvór
		else if (month == 1) {
            return day > 147;
		}
		// Nebulivór
		else if (month == 2) {
            return day > 146;
		}
		// Ventirór
		else if (month == 3) {
            return day > 145;
		}
		// Florivór
		else if (month == 4) {
            return day > 145;
		}
		// Calorvór
		else if (month == 5) {
            return day > 146;
		}
		// Solarvór
		else if (month == 6) {
            return day > 146;
		}
		// Aestivór
		else if (month == 7) {
            return day > 146;
		}
		// Ardorvór
		else if (month == 8) {
            return day > 146;
		}
		// Fructivór
		else if (month == 9) {
            return day > 146;
		}
		// Follivór
		else if (month == 10) {
            return day > 146;
		}
		// Umbravór
		else if (month == 11) {
            return day > 146;
		}
		// Glacivór
		else if (month == 12) {
            return day > 146;
		}
		// Frigorvór
		else if (month == 13) {
            return day > 147;
		}
		// Obscurvór
		else if (month == 14) {
            return day > 147;
		}
		return false;
	}

	/**
	 * Displays the server date on new days.
	 * @param dayNum The current server day.
     * @param weekdayName The current server weekday name.
	 * @param monthName The current server month name.
	 * @param yearNum The current server year.
	 * @param isNewMonth The confirmation whether this is a new month or not.
	 */
	private void displayServerDate(int dayNum, String weekdayName, String monthName, int yearNum, boolean isNewMonth) {
		String dayNumAsString = dayNum + "";
		if (dayNumAsString.length() > 1) {
			if (dayNumAsString.equals("11")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("12")) {
				dayNumAsString += "th";
			} else if (dayNumAsString.endsWith("13")) {
				dayNumAsString += "th";
			} else {
				if (dayNumAsString.endsWith("1")) {
					dayNumAsString += "st";
				} else if (dayNumAsString.endsWith("2")) {
					dayNumAsString += "nd";
				} else if (dayNumAsString.endsWith("3")) {
					dayNumAsString += "rd";
				} else {
					dayNumAsString += "th";
				}
			}
		} else {
			if (dayNumAsString.endsWith("1")) {
				dayNumAsString += "st";
			} else if (dayNumAsString.endsWith("2")) {
				dayNumAsString += "nd";
			} else if (dayNumAsString.endsWith("3")) {
				dayNumAsString += "rd";
			} else {
				dayNumAsString += "th";
			}
		}

		Bukkit.broadcastMessage(ChatUtils.chatMessage("&6&l------------------------------"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("\n"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&e&l" + weekdayName + ", &f&lthe " + dayNumAsString + " of " + monthName + ", &e&l" + yearNum));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("\n"));
		Bukkit.broadcastMessage(ChatUtils.chatMessage("&6&l------------------------------"));

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (isNewMonth) {
				player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 5f, 0.5f);
			} else {
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f);
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1f);

				// 0.2s later
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.667f), 4L);
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.26f), 4L);

				// 0.4s later
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.75f), 8L);
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f), 8L);

				// 0.6s later
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1f), 12L);
				Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2f), 12L);
			}
		}
	}

	/**
	 * Applies the effects of the given month on Aranarth.
	 */
	private void determineMonthEffects() {
        switch (AranarthUtils.getMonth()) {
            case 0 -> applyIgnivorEffects();
            case 1 -> applyAquinvorEffects();
            case 2 -> applyNebulivorEffects();
            case 3 -> applyVentirorEffects();
            case 4 -> applyFlorivorEffects();
            case 5 -> applyCalorvorEffects();
            case 6 -> applySolarvorEffects();
            case 7 -> applyAestivorEffects();
            case 8 -> applyArdorvorEffects();
            case 9 -> applyFructivorEffects();
            case 10 -> applyFollivorEffects();
            case 11 -> applyUmbravorEffects();
            case 12 -> applyGlacivorEffects();
            case 13 -> applyFrigorvorEffects();
            case 14 -> applyObscurvorEffects();
            default -> Bukkit.getLogger().info("Something went wrong with applying the " + AranarthUtils.getMonth() + "'s effects!");
        }
	}

	/**
	 * Apply the effects during the first month of Ignivor.
	 * Players are given the Luck and Regeneration effects during this month.
	 * It can randomly snow during Ignivor, however snow will melt slowly.
	 */
	private void applyIgnivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.LUCK, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.REGENERATION, 320, 0));
		applyEffectToAllPlayers(effects);

		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// Delay up to 10 days
			AranarthUtils.setStormDelay(new Random().nextInt(24000));
		}

		// If it is raining, only melt
		if (Bukkit.getWorld("world").hasStorm()) {
			meltSnow();
		}
		// If it is not raining, snow or melt
		else {
			// Adds snow but will only apply in low chance - will melt otherwise
			applySnow(20, 500);
		}
	}

	/**
	 * Apply the effects during the second month of Aquinvor.
	 * Players are given the Dolphin's Grace and Water Breathing effects during this month.
	 * There is also an increased chance of rain during the month of Aquinvor.
	 */
	private void applyAquinvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.WATER_BREATHING, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow();

		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// Maximum of 2.5 days
			AranarthUtils.setStormDelay(new Random().nextInt(60000));
		}
		applyRain();
	}

	/**
	 * Apply the effects during the third month of Nebulivor.
	 */
	private void applyNebulivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the fourth month of Ventiror.
	 */
	private void applyVentirorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SPEED, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow();
	}

	/**
	 * Apply the effects during the fifth month of Florivor.
	 */
	private void applyFlorivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the sixth month of Calorvor.
	 */
	private void applyCalorvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the seventh month of Solarvor.
	 */
	private void applySolarvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the eighth month of Aestivor.
	 */
	private void applyAestivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the ninth month of Ardorvor.
	 */
	private void applyArdorvorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the tenth month of Fructivor.
	 */
	private void applyFructivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the eleventh month of Follivor.
	 */
	private void applyFollivorEffects() {
		meltSnow();
	}

	/**
	 * Apply the effects during the twelfth month of Umbravor.
	 */
	private void applyUmbravorEffects() {
		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// At least 0.75 days, no more than 5 days
			AranarthUtils.setStormDelay(new Random().nextInt(102000) + 18000);
		}
		applySnow(5, 100);
	}

	/**
	 * Apply the effects during the thirteenth month of Glacivor.
	 */
	private void applyGlacivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);

		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// At least 0.5 days, no more than 2 days
			AranarthUtils.setStormDelay(new Random().nextInt(48000) + 12000);
		}
		applySnow(15, 400);
	}

	/**
	 * Apply the effects during the fourteenth month of Frigorvor.
	 */
	private void applyFrigorvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 1));
		applyEffectToAllPlayers(effects);

		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// At least 0.25 days, no more than 1 day
			AranarthUtils.setStormDelay(new Random().nextInt(18000) + 6000);
		}
		applySnow(50, 1000);
	}

	/**
	 * Apply the effects during the fifteenth month of Obscurvor.
	 */
	private void applyObscurvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.MINING_FATIGUE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);

		// Applies delay to first snow storm
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// At least 0.5 days, no more than 1.5 days
			AranarthUtils.setStormDelay(new Random().nextInt(36000) + 12000);
		}
		applySnow(15, 400);
	}

	/**
	 * Applies potion effects to all online players.
	 * The effects will always be for the same fixed duration and same amplifier.
	 * @param effects The effects to be applied.
	 */
	private void applyEffectToAllPlayers(List<PotionEffect> effects) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.addPotionEffects(effects);
		}
	}

	/**
	 * Calculates the snowstorm duration and delays during the winter months of Aranarth.
	 * Also applies the custom snow particle effects when it is snowing.
	 * @param bigFlakeDensity The density of the larger snowflakes, being end rod particles.
	 * @param smallFlakeDensity The density of the smaller snowflakes, being white ash particles.
	 */
	private void applySnow(int bigFlakeDensity, int smallFlakeDensity) {
		// Only melts snow if it isn't currently snowing - during Ignivor only
		if ((AranarthUtils.getStormDelay() > 100 && AranarthUtils.getStormDuration() <= 0)
				&& AranarthUtils.getMonth() == 0) {
			AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
			meltSnow();
			return;
		}

		new BukkitRunnable() {
			int runs = 0;

			@Override
			public void run() {
				// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
				if (runs == 20) {
					// Determines if it is currently storming
					if (AranarthUtils.getIsStorming()) {
						// Determines if the storm ended
						if (AranarthUtils.getStormDuration() <= 0) {
							Random random = new Random();
							Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe snowstorm has subsided..."));
							AranarthUtils.setIsStorming(false);
							int monthNum = AranarthUtils.getMonth();
							// Updates the delay until the next storm
                            switch (monthNum) {
                                case 11 ->
                                    // At least 0.75 days, no more than 5 days
									AranarthUtils.setStormDelay(random.nextInt(102000) + 18000);
                                case 12 ->
                                    // At least 0.5 days, no more than 2 days
									AranarthUtils.setStormDelay(random.nextInt(48000) + 12000);
                                case 13 ->
                                    // At least 0.25 days, no more than 1 day
									AranarthUtils.setStormDelay(random.nextInt(18000) + 6000);
								case 14 ->
									// At least 0.5 days, no more than 1.5 days
										AranarthUtils.setStormDelay(random.nextInt(36000) + 12000);
								case 0 ->
									// At least 2 days, no more than 10 days
										AranarthUtils.setStormDelay(random.nextInt(240000) + 48000);
                            }
						} else {
							// 100 ticks per execution
							AranarthUtils.setStormDuration(AranarthUtils.getStormDuration() - 100);
						}
					}
					// If it is not storming
					else {
						// If it is time for the next storm
						if (AranarthUtils.getStormDelay() <= 0) {
							Random random = new Random();
							Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oA snowstorm has started..."));
							AranarthUtils.setIsStorming(true);
							int monthNum = AranarthUtils.getMonth();
							// Updates the duration of the storm
                            switch (monthNum) {
                                case 11 ->
                                    // At least 0.125 days, no more than 0.75 days
									AranarthUtils.setStormDuration(random.nextInt(15000) + 3000);
                                case 12 ->
                                    // At least 0.5 days, no more than 1.5 days
									AranarthUtils.setStormDuration(random.nextInt(24000) + 12000);
                                case 13 ->
                                    // At least 0.75 days, no more than 2 days
									AranarthUtils.setStormDuration(random.nextInt(30000) + 18000);
								case 14 ->
									// At least 0.25 days, no more than 1 day
										AranarthUtils.setStormDuration(random.nextInt(18000) + 6000);
								case 0 ->
									// At least 0.5 days, no more than 1.25 day
										AranarthUtils.setStormDuration(random.nextInt(24000) + 6000);
                            }
						} else {
							// 100 ticks per execution
							AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
						}
					}

					this.cancel();
					return;
				}

				// Handles applying the snow functionality
				if (AranarthUtils.getIsStorming()) {
					// Applies snow nearby all online players
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player != null) {
							Location loc = player.getLocation();
							// Only snows in the survival world
							if (!loc.getWorld().getName().equals("world")) {
								continue;
							}

							// Determines if the player is underground or not
							boolean areAllBlocksAir = true;
							Block highestBlock = loc.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
							if (loc.getBlockY() + 2 < (highestBlock.getLocation().getBlockY())) {
								areAllBlocksAir = false;
							}

							// If it is a warm biome, do not apply snow logic
							if (highestBlock.getTemperature() < 0.9 && highestBlock.getBiome() != Biome.RIVER) {
								// Only apply particles if the player is exposed to air
								if (areAllBlocksAir) {
									// If it is a temperate or cold biome
									if (loc.getWorld().getTemperature(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) <= 1) {
										loc.getWorld().spawnParticle(Particle.END_ROD, loc, bigFlakeDensity, 9, 12, 9, 0.05);
										loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, smallFlakeDensity, 9, 12, 9, 0.05);
									}
								}
							}
							// Attempts to generate snow only once per second
							if (runs % 5 == 0) {
								generateSnow(loc, bigFlakeDensity);
							}
						}
					}
				}
				runs++;
			}
		}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
	}

	/**
	 * Handles the generation of snow nearby online players.
	 * @param loc The current location of the player.
	 * @param bigFlakeDensity The density of the large snowflakes to base the snowfall chance on.
	 */
	private void generateSnow(Location loc, int bigFlakeDensity) {
		// Blocks that shouldn't have snow placed on them
		final Set<Material> INVALID_SURFACE_BLOCKS = EnumSet.of(
				Material.WATER, Material.LAVA, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
				Material.SEA_PICKLE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SUGAR_CANE,
				Material.BAMBOO, Material.BAMBOO_SAPLING, Material.TORCH, Material.WALL_TORCH, Material.REDSTONE_TORCH,
				Material.SOUL_TORCH, Material.RAIL, Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL,
				Material.LADDER, Material.VINE, Material.SLIME_BLOCK, Material.HONEY_BLOCK,
				Material.LILY_PAD, Material.ANVIL, Material.BELL, Material.CHAIN, Material.LECTERN, Material.LIGHTNING_ROD,
				Material.RESPAWN_ANCHOR, Material.TRIPWIRE, Material.TRIPWIRE_HOOK, Material.LANTERN, Material.SOUL_LANTERN,
				Material.END_ROD, Material.SCAFFOLDING, Material.FLOWER_POT, Material.CANDLE, Material.CANDLE_CAKE,
				Material.AMETHYST_CLUSTER, Material.SMALL_AMETHYST_BUD, Material.MEDIUM_AMETHYST_BUD, Material.LARGE_AMETHYST_BUD,
				Material.POINTED_DRIPSTONE, Material.TURTLE_EGG, Material.SCULK_SENSOR, Material.SCULK_SHRIEKER, Material.BEACON,
				Material.DIRT_PATH, Material.FARMLAND, Material.WHEAT, Material.BEETROOT, Material.CARROTS, Material.POTATOES,
				Material.NETHER_WART, Material.CHEST, Material.TRAPPED_CHEST, Material.STONECUTTER, Material.MANGROVE_PROPAGULE,
				Material.DEAD_BUSH, Material.AZALEA, Material.FLOWERING_AZALEA, Material.COBWEB, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM,
				Material.DECORATED_POT, Material.LIGHT, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
				Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.CLOSED_EYEBLOSSOM, Material.OPEN_EYEBLOSSOM,
				Material.WITHER_ROSE, Material.PINK_PETALS, Material.SUNFLOWER, Material.LILAC, Material.PEONY, Material.ROSE_BUSH, Material.SNOW, Material.AIR
		);

		// Adding other variants
		for (Material material : Material.values()) {
			String name = material.name();
			if (name.endsWith("_WALL") || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE") || name.endsWith("_BUTTON")
					|| name.endsWith("DOOR") || name.endsWith("_PLATE") || name.endsWith("_CARPET") || name.endsWith("_PANE")
					|| name.endsWith("_BED") || name.endsWith("_CANDLE") || name.endsWith("_BANNER") || name.endsWith("_SIGN")
					|| name.endsWith("_SAPLING") || name.endsWith("_CORAL") || name.endsWith("_FAN") || name.startsWith("POTTED_")) {
				INVALID_SURFACE_BLOCKS.add(material);
			}
		}

		Random random = new Random();
		// Adds snow to the surrounding blocks from the player
		int centerX = loc.getBlockX();
		int centerZ = loc.getBlockZ();
		World world = loc.getWorld();

		int snowRadius = 100;

		// Loop over columns within an input block radius
		for (int x = centerX - snowRadius; x <= centerX + snowRadius; x++) {
			for (int z = centerZ - snowRadius; z <= centerZ + snowRadius; z++) {

				// Check that the column is within circle
				if (loc.distance(new Location(world, x, loc.getY(), z)) > snowRadius) {
					continue;
				}

				Block surfaceBlock = world.getHighestBlockAt(x, z);
				double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
				// Hot biomes do not get snow
				if (temperature > 0.9) {
					continue;
				}
				// Frozen biomes have the highest snow rates
				else if (temperature <= 0) {
					bigFlakeDensity = bigFlakeDensity / 2;
				}
				// Cold biomes have high snow rates
				else if (temperature < 0.25) {
					bigFlakeDensity = bigFlakeDensity / 5;
				}
				// Temperate biomes have standard snow rates
				else {
					bigFlakeDensity = bigFlakeDensity / 10;
				}

				// Determines if snow will generate at this block
				int rand = random.nextInt(5000);
				// Proportionate snow amount to the snow density
				if (rand > bigFlakeDensity) {
					continue;
				}

				// If the surface block is invalid, skip this column
				if (INVALID_SURFACE_BLOCKS.contains(surfaceBlock.getType())) {
					continue;
				}

				// Ensures that snow only goes on flat parts of stairs/slabs
				if (surfaceBlock.getBlockData() instanceof Stairs stairs) {
					if (stairs.getHalf() == Bisected.Half.BOTTOM) {
						continue;
					}
				} else if (surfaceBlock.getBlockData() instanceof Slab slab) {
					if (slab.getType() == Slab.Type.BOTTOM) {
						continue;
					}
				}

				Block above = surfaceBlock.getRelative(BlockFace.UP);
				if (above.getType() != Material.AIR && above.getType() != Material.SNOW && above.getType() != Material.SHORT_GRASS && above.getType() != Material.FERN) {
					continue;
				}

				// Places the snow or adds layer
				if (above.getBlockData() instanceof Snow snow) {
					if (snow.getLayers() < 4) {
						int snowLayers = snow.getLayers();
						snow.setLayers(snowLayers + 1);
						above.setBlockData(snow);
					}
				} else {
					above.setType(Material.SNOW);
				}

				if (surfaceBlock.getType().name().endsWith("LEAVES")) {
					Location location = surfaceBlock.getLocation();
					// Keep going down to apply to the next blocks
					for (int i = location.getBlockY(); i > 61; i--) {
						Block block = location.getWorld().getBlockAt(location.getBlockX(), i, location.getBlockZ());

						if (block.getType().name().endsWith("LEAVES")) {
							continue;
						}
						// If there is a space underneath the leaves
						else if (block.getType() != Material.AIR && block.getType() != Material.SHORT_GRASS && block.getType() != Material.FERN && block.getType() != Material.SNOW) {
							continue;
						}
						// The first solid block under the leaves
						else {
							Block blockUnderneath = location.getWorld().getBlockAt(surfaceBlock.getX(), i - 1, surfaceBlock.getZ());

							if (!INVALID_SURFACE_BLOCKS.contains(blockUnderneath.getType())) {
								// Places the snow or adds layer
								if (block.getBlockData() instanceof Snow snow) {
									if (snow.getLayers() < 4) {
										int snowLayers = snow.getLayers();
										snow.setLayers(snowLayers + 1);
										block.setBlockData(snow);
									}
								} else {
									block.setType(Material.SNOW);
								}
							}

							// Do not add snow if the block is underneath a full solid block
							if (blockUnderneath.getType() == Material.GRASS_BLOCK || blockUnderneath.getType() == Material.DIRT
									|| blockUnderneath.getType() == Material.PODZOL || blockUnderneath.getType() == Material.COARSE_DIRT
									|| blockUnderneath.getType() == Material.STONE) {
								break;
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Handles melting the snow in biomes that had snow applied due to seasons.
	 */
	private void meltSnow() {
		if (!isWinterMonth(AranarthUtils.getMonth())) {
			new BukkitRunnable() {
				int runs = 0;

				@Override
				public void run() {
					// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
					if (runs == 20) {
						this.cancel();
						return;
					}

					// Melts snow nearby all online players
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player != null) {
							Location loc = player.getLocation();
							// Only melt snow in the survival world
							if (!loc.getWorld().getName().equals("world")) {
								continue;
							}

							// Attempts to generate snow only once per second
							if (runs % 4 == 0) {
								Random random = new Random();
								// Adds snow to the surrounding blocks from the player
								int centerX = loc.getBlockX();
								int centerZ = loc.getBlockZ();
								World world = loc.getWorld();
								int snowRadius = 100;

								// Loop over columns within a given block radius
								for (int x = centerX - snowRadius; x <= centerX + snowRadius; x++) {
									for (int z = centerZ - snowRadius; z <= centerZ + snowRadius; z++) {

										// Check that the column is within a circle
										if (loc.distance(new Location(world, x, loc.getY(), z)) > snowRadius) {
											continue;
										}

										Block surfaceBlock = world.getHighestBlockAt(x, z);
										Block above = surfaceBlock.getRelative(BlockFace.UP);
										if (above.getType() != Material.SNOW) {
											continue;
										}

										double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
										int meltRate = 0;
										int grassReplaceRate = 0;
										String biome = surfaceBlock.getBiome().toString();

										// Hot biomes never have snow
										if (temperature >= 0.9) {
											continue;
										}
										// Frozen biomes never melt
										else if (temperature <= 0) {
											continue;
										}
										// Cold biomes have higher snow rates so it should melt slower
										else if (temperature < 0.25) {
											meltRate = 2;
											grassReplaceRate = random.nextInt(100) + 1;
										}
										// Temperate biomes have standard melting rates
										else {
											meltRate = 6;
											grassReplaceRate = random.nextInt(100) + 1;
										}

										// Determines if snow will melt at this block
										int rand = random.nextInt(10000);

										// Reduce snow melting rate if it is Ignivor
										if (AranarthUtils.getMonth() == 0) {
											meltRate = meltRate / 2;
										}
										// Increased snow melting rate if it is raining
										else if (loc.getWorld().hasStorm()) {
											meltRate = meltRate * 4;
										}

										// Proportionate melting rate for the given temperature
										if (rand > meltRate) {
											continue;
										}

										if (above.getBlockData() instanceof Snow snow) {
											int snowLayers = snow.getLayers();
											if (snowLayers > 1) {
												snow.setLayers(snowLayers - 1);
												above.setBlockData(snow);
											}
											// Removes snow when there is only 1 layer left
											else {
												above.setType(Material.AIR);
											}

											if (surfaceBlock.getType().name().endsWith("LEAVES")) {
												Location location = surfaceBlock.getLocation();
												// Keep going down to apply to the next blocks
												for (int i = location.getBlockY(); i > 61; i--) {
													Block block = location.getWorld().getBlockAt(location.getBlockX(), i, location.getBlockZ());
													if (block.getBlockData() instanceof Snow lowerSnow) {
														int snowLayersAboveGround = lowerSnow.getLayers();
														if (snowLayersAboveGround > 1) {
															lowerSnow.setLayers(snowLayersAboveGround - 1);
															block.setBlockData(lowerSnow);
                                                        }
														// Removes snow when there is only 1 layer left
														else {
															block.setType(Material.AIR);
                                                        }
                                                        continue;
                                                    } else {
														// Only replace with grass if the block underneath is soil
														if (block.getType() != Material.GRASS_BLOCK && block.getType() != Material.DIRT
																&& block.getType() != Material.PODZOL && block.getType() != Material.COARSE_DIRT) {
															continue;
														}

														// 5% chance of turning the dirt into grass blocks to spread during warm months
														if (block.getType() == Material.DIRT) {
															if ((random.nextInt(100) + 1) <= 5) {
																block.setType(Material.GRASS_BLOCK);
															}
														}

														Block blockAboveDirt = location.getWorld().getBlockAt(location.getBlockX(), i + 1, location.getBlockZ());
														if (blockAboveDirt.getType() != Material.AIR) {
															continue;
														}

														// Adds short grass depending on biome
														switch (biome) {
															case "MEADOW":
																if (grassReplaceRate > 55) {
																	break;
																}
																blockAboveDirt.setType(Material.SHORT_GRASS);
																break;
															case "PLAINS":
																if (grassReplaceRate > 35) {
																	break;
																}
																blockAboveDirt.setType(Material.SHORT_GRASS);
																break;
															case "SUNFLOWER_PLAINS":
																if (grassReplaceRate > 45) {
																	break;
																}
																blockAboveDirt.setType(Material.SHORT_GRASS);
																break;
															case "TAIGA", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA":
																if (grassReplaceRate > 15) {
																	break;
																} else if (grassReplaceRate > 5) {
																	blockAboveDirt.setType(Material.FERN);
																} else {
																	blockAboveDirt.setType(Material.SHORT_GRASS);
																}
																break;
															case "WINDSWEPT_HILLS":
															case "WINDSWEPT_FOREST":
																if (grassReplaceRate > 10) {
																	break;
																}
																blockAboveDirt.setType(Material.SHORT_GRASS);
																break;
															default:
																// For other biomes that are not excluded, randomly place grass but at a low rate
																if (grassReplaceRate > 10) {
																	break;
																}
																blockAboveDirt.setType(Material.SHORT_GRASS);
																break;
														}
													}
												}
											}
											// Not under a tree
											else {

												// Only replace with grass if the block underneath is soil
												if (surfaceBlock.getType() != Material.GRASS_BLOCK && surfaceBlock.getType() != Material.DIRT
														&& surfaceBlock.getType() != Material.PODZOL && surfaceBlock.getType() != Material.COARSE_DIRT) {
													continue;
												}

												// 5% chance of turning the dirt into grass blocks to spread during warm months
												if (surfaceBlock.getType() == Material.DIRT) {
													if ((random.nextInt(100) + 1) <= 5) {
														surfaceBlock.setType(Material.GRASS_BLOCK);
													}
												}

												if (above.getType() != Material.AIR) {
													continue;
												}

												// Adds short grass depending on biome
												switch (biome) {
													case "MEADOW":
														if (grassReplaceRate > 55) {
															break;
														}
														above.setType(Material.SHORT_GRASS);
														break;
													case "PLAINS":
														if (grassReplaceRate > 35) {
															break;
														}
														above.setType(Material.SHORT_GRASS);
														break;
													case "SUNFLOWER_PLAINS":
														if (grassReplaceRate > 45) {
															break;
														}
														above.setType(Material.SHORT_GRASS);
														break;
													case "TAIGA", "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA":
														if (grassReplaceRate > 15) {
															break;
														} else if (grassReplaceRate > 5) {
															above.setType(Material.FERN);
														} else {
															above.setType(Material.SHORT_GRASS);
														}
														break;
													case "WINDSWEPT_HILLS":
													case "WINDSWEPT_FOREST":
														if (grassReplaceRate > 10) {
															break;
														}
														above.setType(Material.SHORT_GRASS);
														break;
													default:
														// For other biomes that are not excluded, randomly place grass but at a low rate
														if (grassReplaceRate > 10) {
															break;
														}
														above.setType(Material.SHORT_GRASS);
														break;
												}
											}
										}
									}
								}
							}
						}
					}
					runs++;
				}
			}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
		}
	}

	/**
	 * Confirms if the current month is a winter month.
	 * @param monthNum The current month number.
	 * @return Confirmation whether the current month is a winter month.
	 */
	public static boolean isWinterMonth(int monthNum) {
		return monthNum >= 11;
	}

	/**
	 * Applies rain at an increased rate during the month of Aquinvor.
	 */
	private void applyRain() {
		// Determines if it is currently storming
		if (AranarthUtils.getIsStorming()) {
			// Determines if the storm ended
			if (AranarthUtils.getStormDuration() <= 0) {
				Random random = new Random();
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oThe rain has stopped..."));
				Bukkit.getWorld("world").setStorm(false);
				AranarthUtils.setIsStorming(false);
				// At least 0.25 days, no more than 20 days
				AranarthUtils.setStormDelay(random.nextInt(48000) + 6000);
			} else {
				// 100 ticks per execution
				AranarthUtils.setStormDuration(AranarthUtils.getStormDuration() - 100);
			}
		}
		// If it is not storming
		else {
			// If it is time for the next storm
			if (AranarthUtils.getStormDelay() <= 0) {
				Random random = new Random();
				Bukkit.broadcastMessage(ChatUtils.chatMessage("&7&oIt has started to rain..."));
				Bukkit.getWorld("world").setStorm(true);
				AranarthUtils.setIsStorming(true);
				// At least 0.5 days, no more than 1.25 days
				AranarthUtils.setStormDuration(random.nextInt(18000) + 12000);
			} else {
				// 100 ticks per execution
				AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
			}
		}
	}
}
