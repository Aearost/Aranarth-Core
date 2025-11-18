package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Weather;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Provides utility methods to facilitate the formatting of all date related content.
 */
public class DateUtils {

	private final int irlMonth;
	private final int irlDay;
	
	public DateUtils() {
		this.irlMonth = getIrlMonth();
		this.irlDay = getIrlDay();
	}

	/**
	 * Provides the current month as an integer.
	 *
	 * @return The current month as an integer.
	 */
	private int getIrlMonth() {
		return LocalDate.now().getMonthValue();
	}

	/**
	 * Provides the current date of the month as an integer.
	 *
	 * @return The current date of the month as an integer.
	 */
	private int getIrlDay() {
		return LocalDate.now().getDayOfMonth();
	}

	/**
	 * Confirms if the current date is within the general range of Valentine's Day.
	 *
	 * @return Confirmation of whether it is roughly Valentine's Day.
	 */
	public boolean isValentinesDay() {
		if (this.irlMonth == 2) {
            return this.irlDay >= 4 && this.irlDay <= 14;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Easter.
	 *
	 * @return Confirmation of whether it is roughly Easter.
	 */
	public boolean isEaster() {
		if (this.irlMonth == 3) {
            return this.irlDay >= 22 && this.irlDay <= 31;
		} else if (this.irlMonth == 4) {
            return this.irlDay >= 1 && this.irlDay <= 25;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Halloween.
	 *
	 * @return Confirmation of whether it is roughly Halloween.
	 */
	public boolean isHalloween() {
		if (this.irlMonth == 10) {
            return this.irlDay >= 20 && this.irlDay <= 31;
		}
		return false;
	}

	/**
	 * Confirms if the current date is within the general range of Christmas.
	 *
	 * @return Confirmation of whether it is roughly Christmas.
	 */
	public boolean isChristmas() {
		if (this.irlMonth == 12) {
            return this.irlDay >= 15 && this.irlDay <= 25;
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
		Month month = AranarthUtils.getMonth();
		int yearNum = AranarthUtils.getYear();
		boolean isNewMonth = false;

		// If it is a new day
		// First 5 seconds of a new day
		if (time >= 0 && time < 5) {
			// Calculates day number based on length of month
			if (checkIfExceedsMonth(dayNum, month)) {
				dayNum = 1;
				if (month == Month.OBSCURVOR) {
					month = Month.IGNIVOR;
					yearNum++;
				} else {
					// Gets the next month
					month = Month.values()[month.ordinal() + 1];
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
			AranarthUtils.setMonth(month);
			AranarthUtils.setYear(yearNum);

			String monthName = provideMonthName(month);
			if (monthName == null) {
				Bukkit.getLogger().info("Something went wrong with calculating the month name!");
				return;
			}

			String weekdayName = provideWeekdayName(weekdayNum);
			if (weekdayName == null) {
				Bukkit.getLogger().info("Something went wrong with calculating the weekday name!");
				return;
			}

			String[] messages = determineServerDate(dayNum, weekdayName, monthName, yearNum);
			Bukkit.broadcastMessage(messages[0]);
			Bukkit.broadcastMessage(messages[1]);
			Bukkit.broadcastMessage(messages[2]);

			if (isNewMonth) {
				String description = "";
				switch (month) {
					case Month.IGNIVOR -> description = DateUtils.getIgnivorDescription();
					case Month.AQUINVOR -> description = DateUtils.getAquinvorDescription();
					case Month.VENTIVOR -> description = DateUtils.getVentivorDescription();
					case Month.FLORIVOR -> description = DateUtils.getFlorivorDescription();
					case Month.AESTIVOR -> description = DateUtils.getAestivorDescription();
					case Month.CALORVOR -> description = DateUtils.getCalorvorDescription();
					case Month.ARDORVOR -> description = DateUtils.getArdorvorDescription();
					case Month.SOLARVOR -> description = DateUtils.getSolarvorDescription();
					case Month.FRUCTIVOR -> description = DateUtils.getFructivorDescription();
					case Month.FOLLIVOR -> description = DateUtils.getFollivorDescription();
					case Month.FAUNIVOR -> description = DateUtils.getFaunivorDescription();
					case Month.UMBRAVOR -> description = DateUtils.getUmbravorDescription();
					case Month.GLACIVOR -> description = DateUtils.getGlacivorDescription();
					case Month.FRIGORVOR -> description = DateUtils.getFrigorvorDescription();
					case Month.OBSCURVOR -> description = DateUtils.getObscurvorDescription();
				}
			}

			for (Player player : Bukkit.getOnlinePlayers()) {
				if (isNewMonth) {
					player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 3f, 0.5f);
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
		determineMonthEffects();
	}

	/**
	 * Provides the server's month name based on the numeric value.
	 * @param month The numeric month value.
	 * @return The actual name of the month.
	 */
	public static String provideMonthName(Month month) {
		if (month == Month.IGNIVOR) {
			return "Ignivór";
		} else if (month == Month.AQUINVOR) {
			return "Aquinvór";
		} else if (month == Month.VENTIVOR) {
			return "Ventivór";
		} else if (month == Month.FLORIVOR) {
			return "Florivór";
		} else if (month == Month.AESTIVOR) {
			return "Aestivór";
		} else if (month == Month.CALORVOR) {
			return "Calorvór";
		} else if (month == Month.ARDORVOR) {
			return "Ardorvór";
		} else if (month == Month.SOLARVOR) {
			return "Solarvór";
		} else if (month == Month.FRUCTIVOR) {
			return "Fructivór";
		} else if (month == Month.FOLLIVOR) {
			return "Follivór";
		} else if (month == Month.FAUNIVOR) {
			return "Faunivór";
		} else if (month == Month.UMBRAVOR) {
			return "Umbravór";
		} else if (month == Month.GLACIVOR) {
			return "Glacivór";
		} else if (month == Month.FRIGORVOR) {
			return "Frigorvór";
		} else if (month == Month.OBSCURVOR) {
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
	public static String provideWeekdayName(int weekdayNum) {
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
	private boolean checkIfExceedsMonth(int day, Month month) {
		// Ignivór
		if (month == Month.IGNIVOR) {
            return day > 147;
		}
		// Aquinvór
		else if (month == Month.AQUINVOR) {
            return day > 147;
		}
		// Ventirór
		else if (month == Month.VENTIVOR) {
            return day > 146;
		}
		// Florivór
		else if (month == Month.FLORIVOR) {
            return day > 145;
		}
		// Aestivór
		else if (month == Month.AESTIVOR) {
            return day > 146;
		}
		// Calorvór
		else if (month == Month.CALORVOR) {
            return day > 145;
		}
		// Ardorvór
		else if (month == Month.ARDORVOR) {
            return day > 146;
		}
		// Solarvór
		else if (month == Month.SOLARVOR) {
			return day > 146;
		}
		// Fructivór
		else if (month == Month.FRUCTIVOR) {
            return day > 146;
		}
		// Follivór
		else if (month == Month.FOLLIVOR) {
			return day > 146;
		}
		// Faunivór
		else if (month == Month.FAUNIVOR) {
            return day > 146;
		}
		// Umbravór
		else if (month == Month.UMBRAVOR) {
            return day > 146;
		}
		// Glacivór
		else if (month == Month.GLACIVOR) {
            return day > 146;
		}
		// Frigorvór
		else if (month == Month.FRIGORVOR) {
            return day > 147;
		}
		// Obscurvór
		else if (month == Month.OBSCURVOR) {
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
	 */
	public static String[] determineServerDate(int dayNum, String weekdayName, String monthName, int yearNum) {
		String dayNumAsString = getDayNumWithSuffix(dayNum);
		String[] messages = new String[3];
		messages[0] = ChatUtils.translateToColor("&6&l---------------------------------");
		messages[1] = ChatUtils.translateToColor("&e&l  " + weekdayName + " &f&lthe " + dayNumAsString + " of " + monthName + ", &e&l" + yearNum + "  ");
		messages[2] = ChatUtils.translateToColor("&6&l---------------------------------");
		return messages;
	}

	/**
	 * Provides the raw in game date, i.e 010100100 for Aquinvor 1st, 100
	 * @return The raw in game date.
	 */
	public static String getRawInGameDate() {
		int dayNum = AranarthUtils.getDay();
		Month month = AranarthUtils.getMonth();
		int yearNum = AranarthUtils.getYear();
		Bukkit.getLogger().info(dayNum + "");
		Bukkit.getLogger().info(month.ordinal() + 1 + "");
		Bukkit.getLogger().info(yearNum + "");

		String dayString = dayNum + "";
		if (dayString.length() == 1) {
			dayString = "00" + dayString;
		} else if (dayString.length() == 2) {
			dayString = "0" + dayString;
		}

		String monthString = month.ordinal() + 1 + "";
		if (monthString.length() == 1) {
			monthString = "0" + monthString;
		}

		String yearString = yearNum + "";
		if (yearString.length() == 3) {
			yearString = "00" + yearString;
		} else if (yearString.length() == 4) {
			yearString = "0" + yearString;
		}
		return monthString + dayString + yearString;
	}

	/**
	 * Provides the raw in real life date, i.e 01012025 for January 1st, 2025
	 * @return The raw in real life date.
	 */
	public static String getRawInRealLifeDate() {
		LocalDateTime localDateTime = LocalDateTime.now();
		int dayNum = localDateTime.getDayOfMonth();
		int month = localDateTime.getMonthValue();
		int yearNum = localDateTime.getYear();
		Bukkit.getLogger().info(dayNum + "");
		Bukkit.getLogger().info(month + "");
		Bukkit.getLogger().info(yearNum + "");

		String dayString = dayNum + "";
		if (dayString.length() == 1) {
			dayString = "0" + dayString;
		}

		String monthString = month + "";
		if (monthString.length() == 1) {
			monthString = "0" + monthString;
		}

		String yearString = yearNum + "";
		if (yearString.length() == 3) {
			yearString = "00" + yearString;
		} else if (yearString.length() == 4) {
			yearString = "0" + yearString;
		}
		return monthString + dayString + yearString;
	}

	/**
	 * Provides the day number with the suffix.
	 * @param day The day.
	 * @return The day with the suffix.
	 */
	public static String getDayNumWithSuffix(int day) {
		String dayNumAsString = day + "";
		if (dayNumAsString.length() > 1) {
			if (dayNumAsString.endsWith("11")) {
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
		return dayNumAsString;
	}

	/**
	 * Provides the formatted date based on the in game or in real life format.
	 * @param date The unformatted date.
	 * @param isInGameFormat Confirmation whether to use the in game format or the in real life format.
	 * @return The formatted date.
	 */
	public static String getFormattedDate(String date, boolean isInGameFormat) {
		if (isInGameFormat) {
			// 0100100105
			String month = date.substring(0, 1);
			String day = date.substring(2, 4);
			String year = date.substring(5, 9);
			Bukkit.getLogger().info(month);
			Bukkit.getLogger().info(day);
			Bukkit.getLogger().info(year);

			if (day.startsWith("00")) {
				day = day.substring(2);
			} else if (day.startsWith("0")) {
				day = day.substring(1);
			}
			day = getDayNumWithSuffix(Integer.parseInt(day));

			if (year.startsWith("0")) {
				year = year.substring(1);
			}

			String monthName = switch (month) {
                case "01" -> "Ignivór";
                case "02" -> "Aquinvór";
                case "03" -> "Ventivór";
                case "04" -> "Florivór";
                case "05" -> "Aestivór";
                case "06" -> "Calorvór";
                case "07" -> "Ardorvór";
                case "08" -> "Solarvór";
                case "09" -> "Fructivór";
                case "10" -> "Follivór";
                case "11" -> "Faunivór";
                case "12" -> "Umbravór";
                case "13" -> "Glacivór";
                case "14" -> "Frigorvór";
                case "15" -> "Obscurvór";
                default -> "IGNIVOR";
            };

			return monthName + day + ", " + year;
        } else {
			// 01012025
			String month = date.substring(0, 1);
			String day = date.substring(2, 3);
			String year = date.substring(4, 7);
			Bukkit.getLogger().info(month);
			Bukkit.getLogger().info(day);
			Bukkit.getLogger().info(year);

			if (day.startsWith("0")) {
				day = day.substring(1);
			}
			day = getDayNumWithSuffix(Integer.parseInt(day));

			String monthName = switch (month) {
				case "01" -> "January";
				case "02" -> "February";
				case "03" -> "March";
				case "04" -> "April";
				case "05" -> "May";
				case "06" -> "June";
				case "07" -> "July";
				case "08" -> "August";
				case "09" -> "September";
				case "10" -> "October";
				case "11" -> "November";
				case "12" -> "December";
				default -> "JANUARY";
			};

			return monthName + day + ", " + year;
		}
	}

	/**
	 * Applies the effects of the given month on Aranarth.
	 */
	private void determineMonthEffects() {
        switch (AranarthUtils.getMonth()) {
			case Month.IGNIVOR -> applyIgnivorEffects();
			case Month.AQUINVOR -> applyAquinvorEffects();
			case Month.VENTIVOR -> applyVentivorEffects();
			case Month.FLORIVOR -> applyFlorivorEffects();
			case Month.AESTIVOR -> applyAestivorEffects();
			case Month.CALORVOR -> applyCalorvorEffects();
			case Month.ARDORVOR -> applyArdorvorEffects();
			case Month.SOLARVOR -> applySolarvorEffects();
			case Month.FRUCTIVOR -> applyFructivorEffects();
			case Month.FOLLIVOR -> applyFollivorEffects();
			case Month.FAUNIVOR -> applyFaunivorEffects();
			case Month.UMBRAVOR -> applyUmbravorEffects();
			case Month.GLACIVOR -> applyGlacivorEffects();
			case Month.FRIGORVOR -> applyFrigorvorEffects();
			case Month.OBSCURVOR -> applyObscurvorEffects();
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

		// If it is not raining, only snow
		if (AranarthUtils.getWeather() == Weather.SNOW && AranarthUtils.getStormDelay() <= 0 && AranarthUtils.getStormDuration() >= 0) {
			// Adds snow but will only apply in low chance - will melt otherwise
			applySnow(15, 400);
		} else {
			meltSnow(1);
			applyRain();
		}
	}

	/**
	 * Provides the Description of the month of Ignivor.
	 * @return The Description of the month.
	 */
	public static String getIgnivorDescription() {
		return "The month of Ignivór represents the beginning of the new year. Players are granted passive Regeneration I, as well as passive Luck. While snowstorms are still a possibility, they are much rarer, and snow begins to melt.";
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
		meltSnow(2);

		// Increased rain chance
		if (!AranarthUtils.getHasStormedInMonth()) {
			AranarthUtils.setHasStormedInMonth(true);
			// Maximum of 2.5 days
			AranarthUtils.setStormDelay(new Random().nextInt(60000));
		}
		applyRain();
	}

	/**
	 * Provides the Description of the month of Aquinvor.
	 * @return The Description of the month.
	 */
	public static String getAquinvorDescription() {
		return "The month of Aquinvór marks the springtime. Players are granted the passive effects of Dolphin's Grace, and Water Breathing. Rain falls more frequently and lasts longer than other months, melting the remaining snow.";
	}

	/**
	 * Apply the effects during the third month of Ventiror.
	 * Players are given the Speed I effect during this month.
	 * Random gusts of wind can be heard during this month.
	 */
	private void applyVentivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SPEED, 320, 0));
		applyEffectToAllPlayers(effects);
		meltSnow(2);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Ventivor.
	 * @return The Description of the month.
	 */
	public static String getVentivorDescription() {
		return "The month of Ventivór brings a wind behind your tail. The sound of wind is heard flying through the air, granting players with the passive effect of Speed I.";
	}

	/**
	 * Apply the effects during the fourth month of Florivor.
	 * Crops will have a chance to increase by two levels during this month.
	 */
	private void applyFlorivorEffects() {
		meltSnow(3);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Florivor.
	 * @return The Description of the month.
	 */
	public static String getFlorivorDescription() {
		return "The month of Florivór smells of flower petals drifting in the wind. Aside from the cherry leaves across the world, there is also a doubled crop growth rate.";
	}

	/**
	 * Apply the effects during the fifth month of Aestivor.
	 */
	private void applyAestivorEffects() {
		meltSnow(5);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Aestivor.
	 * @return The Description of the month.
	 */
	public static String getAestivorDescription() {
		return "The month of Aestivór is ridden with violent thunderstorms. The skies will thunder far more frequently than other months. There is also a 5% chance that creepers will spawn as the charged variant.";
	}

	/**
	 * Apply the effects during the sixth month of Calorvor.
	 */
	private void applyCalorvorEffects() {
		meltSnow(3);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Calorvor.
	 * @return The Description of the month.
	 */
	public static String getCalorvorDescription() {
		return "The month of Calorvór starts off the summer. Baby animals mature at a quicker rate, and have a 50% chance of being born as a twin.";
	}

	/**
	 * Apply the effects during the seventh month of Ardorvor.
	 */
	private void applyArdorvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 320, 0));
		applyEffectToAllPlayers(effects);meltSnow(4);
		applyForestFire();
		applyRain();
	}

	/**
	 * Provides the Description of the month of Ardorvor.
	 * @return The Description of the month.
	 */
	public static String getArdorvorDescription() {
		return "The month of Ardorvór is the month of flames. Forest fires start at random throughout the month, and all sources of fire deal more damage.";
	}

	/**
	 * Apply the effects during the eighth month of Solarvor.
	 */
	private void applySolarvorEffects() {
		meltSnow(4);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Solarvor.
	 * @return The Description of the month.
	 */
	public static String getSolarvorDescription() {
		return "The month of Solarvór favours the picking of apples. Increased apple drop rates, and frequent &6God Apple Fragments &rdrops, there will be plenty for all.";
	}

	/**
	 * Apply the effects during the ninth month of Fructivor.
	 */
	private void applyFructivorEffects() {
		meltSnow(4);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Fructivor.
	 * @return The Description of the month.
	 */
	public static String getFructivorDescription() {
		return "The month of Fructivór is not for the lazy. Crop yields are doubled, and Farmer villagers favour the vendor by providing improved crop sell rates.";
	}

	/**
	 * Apply the effects during the tenth month of Follivor.
	 */
	private void applyFollivorEffects() {
		meltSnow(2);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Follivor.
	 * @return The Description of the month.
	 */
	public static String getFollivorDescription() {
		return "The month of Follivór introduces the start of autumn. Trees provide more EXP and additional log drops, and saplings grow at quicker speeds";
	}

	/**
	 * Apply the effects during the eleventh month of Faunivor.
	 */
	private void applyFaunivorEffects() {
		meltSnow(3);
		applyRain();
	}

	/**
	 * Provides the Description of the month of Faunivor.
	 * @return The Description of the month.
	 */
	public static String getFaunivorDescription() {
		return "The month of Faunivór encourages a little bloodshed. The sacrificed will yield increased weapon damage, and increased drop rates for both meat and animal products.";
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

		// If it is not raining, only snow
		if (AranarthUtils.getWeather() == Weather.SNOW && AranarthUtils.getStormDelay() <= 0 && AranarthUtils.getStormDuration() >= 0) {
			// Adds snow but will only apply in low chance - will melt otherwise
			applySnow(20, 250);
		} else {
			meltSnow(1);
			applyRain();
		}
	}

	/**
	 * Provides the Description of the month of Umbravor.
	 * @return The Description of the month.
	 */
	public static String getUmbravorDescription() {
		return "The month of Umbravór marks the beginning of winter. There is a low chance of world-wide snowstorms, and crops are no longer harvested as efficiently.";
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
		applySnow(30, 500);
	}

	/**
	 * Provides the Description of the month of Glacivor.
	 * @return The Description of the month.
	 */
	public static String getGlacivorDescription() {
		return "The month of Glacivór sends shivers down your spine. You will be slowed down in your tracks, and water begins to freeze in rivers and the sea.";
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
	 * Provides the Description of the month of Frigorvor.
	 * @return The Description of the month.
	 */
	public static String getFrigorvorDescription() {
		return "The month of Frigorvór brings the heaviest snowfalls. Your movements will be slowed even further, as will your crop growth rates.";
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
		applySnow(35, 700);
	}

	/**
	 * Provides the Description of the month of Obscurvor.
	 * @return The Description of the month.
	 */
	public static String getObscurvorDescription() {
		return "The month of Obscurvór is the month of rest. You will harvest blocks at a slower rate, and be hunted more frequently and aggressively by phantoms.";
	}

	/**
	 * Applies potion effects to all online players.
	 * The effects will always be for the same fixed duration and same amplifier.
	 * @param effects The effects to be applied.
	 */
	private void applyEffectToAllPlayers(List<PotionEffect> effects) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			PotionEffect effectToRemove = null;

			for (PotionEffect effect : effects) {
				if (effect.getType() == PotionEffectType.SLOWNESS) {
					effectToRemove = effect;
				}
			}
			if (effectToRemove != null) {
				Location loc = player.getLocation();
				boolean areAllBlocksAir = true;
				Block highestBlock = loc.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
				if (loc.getBlockY() + 2 < (highestBlock.getLocation().getBlockY())) {
					areAllBlocksAir = false;
				}

				if (!areAllBlocksAir) {
					effects.remove(effectToRemove);
				}
			}
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
		// Only melts snow if it isn't currently snowing during the month of Ignivor or Umbravor only
		if ((AranarthUtils.getMonth() == Month.IGNIVOR || AranarthUtils.getMonth() == Month.UMBRAVOR)
				&& AranarthUtils.getWeather() != Weather.SNOW) {
			meltSnow(1);
			return;
		}

		new BukkitRunnable() {
			int runs = 0;

			@Override
			public void run() {
				// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
				if (runs == 20) {
					// If it is currently storming
					if (AranarthUtils.getWeather() != Weather.CLEAR) {
						// Determines if the storm ended
						if (AranarthUtils.getStormDuration() <= 0) {
							Random random = new Random();
							int delay = 0;
							Month month = AranarthUtils.getMonth();
							// Updates the delay until the next storm
							switch (month) {
								case Month.UMBRAVOR ->
									// At least 0.75 days, no more than 5 days
									delay = random.nextInt(102000) + 18000;
								case Month.GLACIVOR ->
									// At least 0.5 days, no more than 2 days
									delay = random.nextInt(48000) + 12000;
								case Month.FRIGORVOR ->
									// At least 0.25 days, no more than 1 day
									delay = random.nextInt(18000) + 6000;
								case Month.OBSCURVOR ->
									// At least 0.5 days, no more than 1.5 days
									delay = random.nextInt(36000) + 12000;
								case Month.IGNIVOR ->
									// At least 2 days, no more than 10 days
									delay = random.nextInt(240000) + 48000;
							}
							updateStorm(Weather.CLEAR, delay);
							// Must be at the end or it interferes with WeatherChangeEventListener
							// Real weather ticks but Aranarth duration only updates after conditions
							// Because of this, a difference of 100 occurs, so it must be reduced
							AranarthUtils.setStormDelay(delay - 100);
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
							int duration = 0;
							Month month = AranarthUtils.getMonth();
							// Updates the duration of the storm
                            switch (month) {
								case Month.UMBRAVOR ->
                                    // At least 0.125 days, no more than 0.75 days
									duration = random.nextInt(15000) + 3000;
								case Month.GLACIVOR ->
                                    // At least 0.5 days, no more than 1.5 days
									duration = random.nextInt(24000) + 12000;
								case Month.FRIGORVOR ->
                                    // At least 0.75 days, no more than 2 days
									duration = random.nextInt(30000) + 18000;
								case Month.OBSCURVOR ->
									// At least 0.25 days, no more than 1 day
									duration = random.nextInt(18000) + 6000;
								case Month.IGNIVOR ->
									// At least 0.5 days, no more than 1.25 day
									duration = random.nextInt(24000) + 6000;
                            }

							// Ignivor can either snow or rain, higher chance of snow
							if (month == Month.IGNIVOR) {
								int chance = random.nextInt(100);
								if (chance >= 98) {
									updateStorm(Weather.THUNDER, duration);
								} else if (chance >= 55) {
									updateStorm(Weather.RAIN, duration);
								} else {
									updateStorm(Weather.SNOW, duration);
								}
							}
							// Umbravor can either snow or rain, higher chance of rain
							else if (month == Month.UMBRAVOR) {
								int chance = random.nextInt(100);
								if (chance >= 96) {
									updateStorm(Weather.THUNDER, duration);
								} else if (chance >= 40) {
									updateStorm(Weather.RAIN, duration);
								} else {
									updateStorm(Weather.SNOW, duration);
								}
							} else {
								updateStorm(Weather.SNOW, duration);
							}
							// Must be at the end or it interferes with WeatherChangeEventListener
							// Real weather ticks but Aranarth duration only updates after conditions
							// Because of this, a difference of 100 occurs, so it must be reduced
							AranarthUtils.setStormDuration(duration - 100);
						} else {
							// 100 ticks per execution
							AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
						}
					}
					this.cancel();
					return;
				}

				// Handle the snow effects
				if (AranarthUtils.getWeather() == Weather.SNOW || AranarthUtils.getWeather() == Weather.CLEAR) {
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player != null) {
							Location loc = player.getLocation();
							// Handles applying the snow functionality
							if (AranarthUtils.getWeather() == Weather.SNOW) {
								// Only apply logic in the survival world
								if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
									continue;
								}
								// Do not proceed if the chunk is not yet loaded
								if (!loc.getChunk().isLoaded()) {
									continue;
								}
								// Determines if the player is underground or not
								boolean areAllBlocksAir = true;
								Block highestBlock = loc.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ());
								if (loc.getBlockY() + 2 < (highestBlock.getLocation().getBlockY())) {
									areAllBlocksAir = false;
								}
								// If it is a warm biome, do not apply snow logic
								if (highestBlock.getTemperature() < 0.85 && highestBlock.getBiome() != Biome.RIVER) {
									// Only apply particles if the player is exposed to air
									if (areAllBlocksAir) {
										// If it is a temperate or cold biome
										if (loc.getWorld().getTemperature(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) <= 1) {
											int particleBigFlake = AranarthUtils.calculateParticlesForPlayer(bigFlakeDensity, AranarthUtils.getPlayer(player.getUniqueId()).getParticleNum());
											int particleSmallFlake = AranarthUtils.calculateParticlesForPlayer(smallFlakeDensity, AranarthUtils.getPlayer(player.getUniqueId()).getParticleNum());

											loc.getWorld().spawnParticle(Particle.END_ROD, loc, particleBigFlake, 9, 12, 9, 0.05);
											loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc, particleSmallFlake, 9, 12, 9, 0.05);
										}
									}
								}
								// Attempts to generate snow only once per second
								if (runs % 5 == 0) {
									generateSnow(player, loc, bigFlakeDensity);
								}
							}
							// Generate ice regardless of if it is snowing
							if (runs % 5 == 0) {
								generateIce(loc, bigFlakeDensity);
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
	private void generateSnow(Player player, Location loc, final int bigFlakeDensity) {
		// Only apply logic in the survival world
		if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
			return;
		}

		// Do not proceed if the chunk is not yet loaded
		if (!loc.getChunk().isLoaded()) {
			return;
		}

		// Blocks that shouldn't have snow placed on them
		final Set<Material> INVALID_SURFACE_BLOCKS = EnumSet.of(
				Material.WATER, Material.LAVA, Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
				Material.SEA_PICKLE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SUGAR_CANE,
				Material.BAMBOO, Material.BAMBOO_SAPLING, Material.TORCH, Material.WALL_TORCH, Material.REDSTONE_TORCH,
				Material.SOUL_TORCH, Material.RAIL, Material.ACTIVATOR_RAIL, Material.DETECTOR_RAIL, Material.POWERED_RAIL,
				Material.LADDER, Material.VINE, Material.SLIME_BLOCK, Material.HONEY_BLOCK, Material.REDSTONE_WIRE,
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
				Material.WITHER_ROSE, Material.PINK_PETALS, Material.SUNFLOWER, Material.LILAC, Material.PEONY, Material.ROSE_BUSH, Material.SNOW, Material.AIR,
				Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.LARGE_FERN, Material.SWEET_BERRY_BUSH
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
		int snowRadius = 250;

		Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
			List<Block> toSnow = new ArrayList<>();

			// Loop over columns within an input block radius
			for (int x = centerX - snowRadius; x <= centerX + snowRadius; x++) {
				for (int z = centerZ - snowRadius; z <= centerZ + snowRadius; z++) {
					Block surfaceBlock = world.getHighestBlockAt(x, z);
					// Only add locations in loaded chunks
					if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;

					if (AranarthUtils.isSpawnLocation(surfaceBlock.getLocation())) {
						continue;
					}

					// Check that the column is within circle
					if (loc.distance(new Location(world, x, loc.getY(), z)) > snowRadius) {
						continue;
					}

					double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
					int snowAmount = bigFlakeDensity;

					// Hot biomes do not get snow
					if (temperature >= 0.85) {
						continue;
					}
					// Frozen biomes have the highest snow rates
					else if (temperature <= 0) {
						snowAmount = bigFlakeDensity / 2;
					}
					// Cold biomes have high snow rates
					else if (temperature < 0.25) {
						snowAmount = bigFlakeDensity / 5;
					}
					// Temperate biomes have standard snow rates
					else {
						snowAmount = bigFlakeDensity / 10;
					}

					// Determines if snow will generate at this block
					int rand = random.nextInt(2000);
					// Proportionate snow amount to the snow density
					if (rand > snowAmount) {
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
					toSnow.add(above);
				}
			}

			// --- SWITCH BACK TO SYNC ---
			Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
				for (Block above : toSnow) {
					Block surfaceBlock = above.getRelative(BlockFace.DOWN);
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
			});
		});
	}

	/**
	 * Handles the generation of ice nearby online players.
	 * @param loc The current location of the player.
	 * @param bigFlakeDensity The density of the large snowflakes to base the ice generation rate on.
	 */
	private void generateIce(Location loc, final int bigFlakeDensity) {
		// Only apply logic in the survival world
		if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
			return;
		}

		// Do not proceed if the chunk is not yet loaded
		if (!loc.getChunk().isLoaded()) {
			return;
		}

		Random random = new Random();
		// Adds snow to the surrounding blocks from the player
		int centerX = loc.getBlockX();
		int centerZ = loc.getBlockZ();
		World world = loc.getWorld();
		int iceRadius = 250;

		Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
			List<Block> toFreeze = new ArrayList<>();
			// Loop over columns within an input block radius
			for (int x = centerX - iceRadius; x <= centerX + iceRadius; x++) {
				for (int z = centerZ - iceRadius; z <= centerZ + iceRadius; z++) {
					Block surfaceBlock = world.getHighestBlockAt(x, z);
					if (AranarthUtils.isSpawnLocation(surfaceBlock.getLocation())) {
						continue;
					}

					// Check that the column is within circle
					if (loc.distance(new Location(world, x, loc.getY(), z)) > iceRadius) {
						continue;
					}

					double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
					int freezeRate = bigFlakeDensity;
					// Hot biomes do not get snow
					if (temperature > 0.85) {
						continue;
					}
					// Frozen biomes have the highest snow rates
					else if (temperature <= 0) {
						freezeRate = bigFlakeDensity / 2;
					}
					// Cold biomes have high snow rates
					else if (temperature < 0.25) {
						freezeRate = bigFlakeDensity / 5;
					}
					// Temperate biomes have standard snow rates
					else {
						freezeRate = bigFlakeDensity / 10;
					}

					// Determines if ice will generate at this block
					int rand = random.nextInt(7500);
					// Proportionate ice amount to the snow density of the month
					if (rand > freezeRate) {
						continue;
					}

					// If the surface block is invalid, skip this column
					if (surfaceBlock.getType() != Material.WATER) {
						continue;
					}
					toFreeze.add(surfaceBlock);
				}
			}

			// --- SWITCH BACK TO SYNC ---
			Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
				for (Block surfaceBlock : toFreeze) {
					if (!isTouchingFarmland(surfaceBlock) && !isNearbyHotBiome(surfaceBlock)) {
						surfaceBlock.setType(Material.ICE);
					}
				}
			});
		});
	}

	/**
	 * Handles melting the snow in biomes that had snow applied due to seasons.
	 */
	private void meltSnow(int meltMultiplier) {
		Month month = AranarthUtils.getMonth();
		if (!isWinterMonth(month)) {
			new BukkitRunnable() {
				int runs = 0;
				boolean isPlayingWindSound = AranarthUtils.getIsPlayingWindSound();

				@Override
				public void run() {
					// 20 executions * 5 ticks is 100 ticks, which is 5 seconds
					if (runs == 20) {
						this.cancel();
						return;
					}

					// Determine if wind should be played during the month of Ventivor
					if (month == Month.VENTIVOR) {
						if (runs == 0) {
							// The end of the wind sound
							if (AranarthUtils.getWindPlayTimer() >= 80) {
								AranarthUtils.setWindPlayTimer(0);
								AranarthUtils.setIsPlayingWindSound(false);
								isPlayingWindSound = false;
							}
							// During a wind sound
							else if (isPlayingWindSound) {
								AranarthUtils.setWindPlayTimer(AranarthUtils.getWindPlayTimer() + 20);
							}
							// If not already playing wind sound, every 5 seconds there's a 10% chance that it will start
							else if (!AranarthUtils.getIsPlayingWindSound() && new Random().nextInt(10) == 0) {
								AranarthUtils.setIsPlayingWindSound(true);
								isPlayingWindSound = true;
							}
						}
					}

					Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
						// Melts snow nearby all online players
						for (Player player : Bukkit.getOnlinePlayers()) {
							if (player != null) {
								Location loc = player.getLocation();

								// Only apply logic in the survival world
								if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
									continue;
								}

								int centerX = loc.getBlockX();
								int centerZ = loc.getBlockZ();
								int meltRadius = 250;
								World world = loc.getWorld();
								Random random = new Random();

								// Precompute candidate coordinates asynchronously
								List<Location> toMelt = new ArrayList<>();

								for (int x = centerX - meltRadius; x <= centerX + meltRadius; x++) {
									for (int z = centerZ - meltRadius; z <= centerZ + meltRadius; z++) {
										double distance = loc.distance(new Location(world, x, loc.getY(), z));
										if (distance > meltRadius) continue;

										// Only add locations in loaded chunks (safe to check async)
										if (!world.isChunkLoaded(x >> 4, z >> 4)) {
											continue;
										}

										// Collect this location for processing later
										toMelt.add(new Location(world, x, loc.getY(), z));
									}
								}

								// --- SWITCH BACK TO SYNC ---
								Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {

									// Play wind sound during Ventivor
									if (month == Month.VENTIVOR) {
										// If it is currently playing the sound and the first set of runs
										if (isPlayingWindSound && AranarthUtils.getWindPlayTimer() < 20) {
											playWindEffect(runs, player);
										}
									}
									// Add pink petals effect in wind during Florivor
									else if (runs == 0 && month == Month.FLORIVOR) {
										// Only display if above sea level
										if (loc.getBlockY() < 62) {
											return;
										}

										if (isBiomeForCherryParticles(loc.getBlock().getBiome())) {
											// More than 10 seconds since the last cherry leaf particle display
											if (AranarthUtils.getCherryParticleDelay() > 20) {
												// 33% chance every 5 seconds of showing the petals
												if (new Random().nextInt(3) == 0) {
													AranarthUtils.setCherryParticleDelay(0);
													for (int i = 0; i < 100; i++) { // Increased particles for visibility
														int x = (int) ((Math.random() - 0.5) * 64);
														int y = (int) (Math.random() * 20 - 5); // From 15 above to 5 below
														int z = (int) ((Math.random() - 0.5) * 64);
														Location spawnLoc = player.getEyeLocation().clone().add(x, y, z);

														Bukkit.getWorld("world").spawnParticle(Particle.CHERRY_LEAVES, spawnLoc, 1);
														Bukkit.getWorld("smp").spawnParticle(Particle.CHERRY_LEAVES, spawnLoc, 1);
													}
												}
											} else {
												AranarthUtils.setCherryParticleDelay(AranarthUtils.getCherryParticleDelay() + 20);
											}
										}
									}
									// Increase animal growth speed during the month of Calorvor
									else if (runs < 4 && month == Month.CALORVOR) {
										Collection<Entity> entitiesInRange = loc.getWorld().getNearbyEntities(loc, 50, 50, 50);
										for (Entity entity : entitiesInRange) {
											if (entity instanceof Animals animal && !animal.isAdult()) {
												int currentAge = animal.getAge();

												// 50% chance to add boost
												boolean shouldAddBoost = new Random().nextInt(4) > 1;
												int ageIncrement = 1 + (shouldAddBoost ? 1 : 0);
												animal.setAge(currentAge + ageIncrement);
											}
										}
									}

									if (runs % 2 == 0) {
										for (Location meltLoc : toMelt) {
											World w = meltLoc.getWorld();
											int x = meltLoc.getBlockX();
											int z = meltLoc.getBlockZ();

											if (AranarthUtils.isSpawnLocation(loc) && world.getName().equals("world")) {
												continue;
											}

											// Check that the column is within a circle
											if (loc.distance(new Location(world, x, loc.getY(), z)) > meltRadius) {
												continue;
											}

											Block surfaceBlock = world.getHighestBlockAt(x, z);
											Block above = surfaceBlock.getRelative(BlockFace.UP);
											if (above.getType() != Material.SNOW && surfaceBlock.getType() != Material.ICE) {
												continue;
											}

											double temperature = surfaceBlock.getWorld().getTemperature(surfaceBlock.getX(), surfaceBlock.getY(), surfaceBlock.getZ());
											int meltRate = 0;
											int grassReplaceRate = 0;
											String biome = surfaceBlock.getBiome().toString();

											// Hot biomes never have snow
											if (temperature >= 0.85) {
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

											// Determines if snow or ice will melt at this block
											int rand = random.nextInt(8000);

											// Ice should melt slightly slower than snow
											if (surfaceBlock.getType() == Material.ICE) {
												meltRate = (int) (meltRate / 1.5);
											}

											// Reduce snow melting rate if it is Ignivor or Umbravor
											if (month == Month.IGNIVOR || month == Month.UMBRAVOR) {
												meltRate = meltRate / 2;
											}
											// Increased snow melting rate if it is raining
											else if (loc.getWorld().hasStorm()) {
												meltRate = meltRate * 4;
											}
											meltRate = meltRate * meltMultiplier;

											// Proportionate melting rate for the given temperature
											if (rand > meltRate) {
												continue;
											}

											// Melt ice if the water is not close to a hot biome or crops
											if (surfaceBlock.getType() == Material.ICE) {
												if (!isTouchingFarmland(surfaceBlock) && !isNearbyHotBiome(surfaceBlock)) {
													surfaceBlock.setType(Material.WATER);
												}
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
																case "TAIGA", "OLD_GROWTH_PINE_TAIGA",
																	 "OLD_GROWTH_SPRUCE_TAIGA":
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
														case "TAIGA", "OLD_GROWTH_PINE_TAIGA",
															 "OLD_GROWTH_SPRUCE_TAIGA":
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
								});
							}
						}
					});
					runs++;
				}
			}.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
		}
	}

	/**
	 * Helper method to verify if the water is touching farmland blocks.
	 * @param surfaceBlock The surface block in question.
	 * @return Confirmation whether the block is touching farmland.
	 */
	private boolean isTouchingFarmland(Block surfaceBlock) {
		return surfaceBlock.getRelative(BlockFace.NORTH).getType() == Material.FARMLAND
				|| surfaceBlock.getRelative(BlockFace.EAST).getType() == Material.FARMLAND
				|| surfaceBlock.getRelative(BlockFace.SOUTH).getType() == Material.FARMLAND
				|| surfaceBlock.getRelative(BlockFace.WEST).getType() == Material.FARMLAND;
	}

	/**
	 * Helper method to verify if the water is close to a hot biome.
	 * @param surfaceBlock The surface block in question.
	 * @return Confirmation whether the block is close to a hot biome.
	 */
	private boolean isNearbyHotBiome(Block surfaceBlock) {
		if (surfaceBlock.getTemperature() >= 0.85) {
			return true;
		} else {
			if (surfaceBlock.getBiome() == Biome.RIVER) {
				Location loc = surfaceBlock.getLocation();
				// Checks if nearby blocks on X coordinate are in hot biome
				for (int x = loc.getBlockX() - 25; x < loc.getBlockX() + 25; x++) {
					if (loc.getWorld().getBlockAt(x, loc.getBlockY(), loc.getBlockZ()).getTemperature() >= 0.85) {
						return true;
					}
				}
				// Same check but for Z coordinate
				for (int z = loc.getBlockZ() - 25; z < loc.getBlockZ() + 25; z++) {
					if (loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY(), z).getTemperature() >= 0.85) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Confirms if the current month is a winter month.
	 * @param month The current month.
	 * @return Confirmation whether the current month is a winter month.
	 */
	public static boolean isWinterMonth(Month month) {
		return month.ordinal() >= 11;
	}

	/**
	 * Applies rain manually based on the given month.
	 */
	private void applyRain() {
		// If it is currently storming
		if (AranarthUtils.getWeather() != Weather.CLEAR) {
			// Check if the duration has ended
			if (AranarthUtils.getStormDuration() <= 0) {
				Random random = new Random();

				// At least 0.5 days, no more than 5 days
				int delay = random.nextInt(108000) + 12000;
				if (AranarthUtils.getMonth() == Month.AQUINVOR) {
					// At least 0.25 days, no more than 2.25 days
					delay = random.nextInt(48000) + 6000;
				}
                updateStorm(Weather.CLEAR, delay);
				// Must be at the end or it interferes with WeatherChangeEventListener
				// Real weather ticks but Aranarth duration only updates after conditions
				// Because of this, a difference of 100 occurs, so it must be reduced
                AranarthUtils.setStormDelay(delay - 100);
			} else {
				// 100 ticks per execution
				AranarthUtils.setStormDuration(AranarthUtils.getStormDuration() - 100);
			}
		}
		// If it is not storming
		else {
			World world = Bukkit.getWorld("world");
			// Raining from previous server restart
			if (world.hasStorm() && AranarthUtils.getWeather() == Weather.CLEAR && AranarthUtils.getStormDuration() == 0) {
				AranarthUtils.setStormDuration(Bukkit.getWorld("world").getWeatherDuration());
				AranarthUtils.setStormDelay(0);
				if (world.isThundering()) {
					AranarthUtils.setWeather(Weather.THUNDER);
				} else {
					AranarthUtils.setWeather(Weather.RAIN);
				}
			} else {
				// If it is time for the next storm
				if (AranarthUtils.getStormDelay() <= 0) {
					Random random = new Random();

					// At least 0.5 days, no more than 1.25 days
					int duration = random.nextInt(18000) + 12000;
					int chance = random.nextInt(10);

					if (AranarthUtils.getMonth() == Month.IGNIVOR) {
						chance = random.nextInt(100);
						// Ignivor can either snow or rain, higher chance of snow
						if (chance >= 95) {
							updateStorm(Weather.THUNDER, duration);
						} else if (chance >= 55) {
							updateStorm(Weather.RAIN, duration);
						} else {
							updateStorm(Weather.SNOW, duration);
						}
					} else if (AranarthUtils.getMonth() == Month.UMBRAVOR) {
						chance = random.nextInt(100);
						// Ignivor can either snow or rain, higher chance of rain
						if (chance >= 95) {
							updateStorm(Weather.THUNDER, duration);
						} else if (chance >= 40) {
							updateStorm(Weather.RAIN, duration);
						} else {
							updateStorm(Weather.SNOW, duration);
						}
					} else if (AranarthUtils.getMonth() == Month.AESTIVOR) {
						if (chance > 5) {
							updateStorm(Weather.THUNDER, duration);
						} else {
							updateStorm(Weather.RAIN, duration);
						}
					} else {
						if (chance > 1) {
							updateStorm(Weather.RAIN, duration);
						} else {
							updateStorm(Weather.THUNDER, duration);
						}
					}
					// Must be at the end or it interferes with WeatherChangeEventListener
					// Real weather ticks but Aranarth duration only updates after conditions
					// Because of this, a difference of 100 occurs, so it must be reduced
					AranarthUtils.setStormDuration(duration - 100);
				} else {
					// 100 ticks per execution
					AranarthUtils.setStormDelay(AranarthUtils.getStormDelay() - 100);
				}
			}
		}
	}

	/**
	 * Updates chat and storm variables based on the provided inputs.
	 * @param type The type of weather condition being added/removed.
	 * @param duration The amount of ticks the storm will last.
	 */
	private void updateStorm(Weather type, int duration) {
		String message = null;
		World world = Bukkit.getWorld("world");
		World smp = Bukkit.getWorld("smp");

		// Start of a new weather
		if (type != Weather.CLEAR) {
			AranarthUtils.setWeather(type);
			if (type == Weather.RAIN) {
				world.setClearWeatherDuration(0);
				world.setStorm(true);
				world.setThundering(false);
				world.setWeatherDuration(duration);
				message = ChatUtils.chatMessage("&7&oIt has started to rain...");
				smp.setClearWeatherDuration(0);
				smp.setStorm(true);
				smp.setThundering(false);
				smp.setWeatherDuration(duration);
			} else if (type == Weather.THUNDER) {
				world.setClearWeatherDuration(0);
				world.setStorm(true);
				world.setThundering(true);
				world.setWeatherDuration(duration);
				world.setThunderDuration(duration);
				message = ChatUtils.chatMessage("&7&oA thunderstorm has started...");
				smp.setClearWeatherDuration(0);
				smp.setStorm(true);
				smp.setThundering(true);
				smp.setWeatherDuration(duration);
				smp.setThunderDuration(duration);
			} else if (type == Weather.SNOW) {
				world.setThunderDuration(0);
				world.setWeatherDuration(0);
				world.setThundering(false);
				world.setStorm(false);
				world.setClearWeatherDuration(duration);
				message = ChatUtils.chatMessage("&7&oIt has started to snow...");
				smp.setThunderDuration(0);
				smp.setWeatherDuration(0);
				smp.setThundering(false);
				smp.setStorm(false);
				smp.setClearWeatherDuration(duration);
			} else {
				Bukkit.getLogger().info("Something went wrong with starting the storm...");
				AranarthUtils.setWeather(Weather.CLEAR);
				return;
			}
		} else {
			world.setThunderDuration(0);
			world.setWeatherDuration(0);
			world.setThundering(false);
			world.setStorm(false);
			world.setClearWeatherDuration(duration);
			AranarthUtils.setWeather(type);
			message = ChatUtils.chatMessage("&7&oThe storm has subsided...");
			smp.setThunderDuration(0);
			smp.setWeatherDuration(0);
			smp.setThundering(false);
			smp.setStorm(false);
			smp.setClearWeatherDuration(duration);
		}
		Bukkit.broadcastMessage(message);
	}

	/**
	 * Applies chances of forest fires at a very low chance during the month of Ardorvor.
	 */
	private void applyForestFire() {
		boolean shouldApplyForestFire = new Random().nextInt(200) == 0;
		if (shouldApplyForestFire) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				Location loc = player.getLocation();
				// Only apply logic in the survival world
				if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
					continue;
				}

				// Do not proceed if the chunk is not yet loaded
				if (!loc.getChunk().isLoaded()) {
					continue;
				}

				int radius = 50;
				for (int x = loc.getBlockX() - radius; x < loc.getBlockX() + radius; x++) {
					for (int z = loc.getBlockZ() - radius; z < loc.getBlockZ() + radius; z++) {
						if (AranarthUtils.isSpawnLocation(loc) && loc.getWorld().getName().equals("world")) {
							continue;
						}
						Block block = loc.getWorld().getHighestBlockAt(x, z);
						if (isBiomeForForestFire(loc.getBlock().getBiome())) {
							if (block.getBlockData() instanceof Leaves leaves) {
								// Only apply to naturally generated trees
								if (!leaves.isPersistent()) {
									Bukkit.broadcastMessage(ChatUtils.chatMessage("&7There is a &c&oforest fire &7nearby " + AranarthUtils.getNickname(player)));
									loc.getWorld().getHighestBlockAt(x, z).setType(Material.FIRE);
									return;
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Applies a gradually fading in and fading out wind sound effect during the month of Ventivor.
	 * @param runs The current number of runs within the runnable, one every 1/4 second up to 20 runs.
	 * @param player The player to play the effect to.
	 */
	private void playWindEffect(int runs, Player player) {
		Location loc = player.getLocation();
		// Only apply logic in the survival world
		if (!loc.getWorld().getName().equals("world") && !loc.getWorld().getName().equals("smp")) {
			return;
		}

		// Do not proceed if the chunk is not yet loaded
		if (!loc.getChunk().isLoaded()) {
			return;
		}

		if (runs == 0) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.01F, 0.5F);
		} else if (runs == 1) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.02F, 0.5F);
		} else if (runs == 2) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.05F, 0.5F);
		} else if (runs == 3) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.075F, 0.5F);
		} else if (runs == 4) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.1F, 0.5F);
		} else if (runs == 5) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.12F, 0.5F);
		} else if (runs == 6) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.15F, 0.5F);
		} else if (runs == 13) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.12F, 0.5F);
		} else if (runs == 14) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.1F, 0.5F);
		} else if (runs == 15) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.075F, 0.5F);
		} else if (runs == 16) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.05F, 0.5F);
		} else if (runs == 17) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.03F, 0.5F);
		} else if (runs == 18) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.02F, 0.5F);
		} else if (runs == 19) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, 0.01F, 0.5F);
		}
	}

	/**
	 * Confirms if the current biome is suitable for a random forest fire.
	 * @param biome The biome.
	 * @return Confirmation whether the biome is suitable for a random forest fire.
	 */
	private boolean isBiomeForForestFire(Biome biome) {
		return biome == Biome.FOREST || biome == Biome.BIRCH_FOREST || biome == Biome.OLD_GROWTH_BIRCH_FOREST
				|| biome == Biome.DARK_FOREST || biome == Biome.SAVANNA || biome == Biome.SAVANNA_PLATEAU
				|| biome == Biome.WOODED_BADLANDS;
	}

	/**
	 * Confirms if the current biome is suitable for cherry leaf particles.
	 * @param biome The biome.
	 * @return Confirmation whether the biome is suitable for cherry leaf particles.
	 */
	private boolean isBiomeForCherryParticles(Biome biome) {
		return biome == Biome.PLAINS || biome == Biome.SUNFLOWER_PLAINS || biome == Biome.BIRCH_FOREST
				|| biome == Biome.OLD_GROWTH_BIRCH_FOREST || biome == Biome.DARK_FOREST || biome == Biome.FOREST
				|| biome == Biome.FLOWER_FOREST || biome == Biome.MEADOW;
	}



}
