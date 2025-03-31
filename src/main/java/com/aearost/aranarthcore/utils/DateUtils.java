package com.aearost.aranarthcore.utils;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

	public void calculateServerDate() {
		// Unformatted in-game day number
		int dayNum = (int) (Bukkit.getWorld("world").getGameTime() / 24000);
		int time = (int) (Bukkit.getWorld("world").getTime() / 20);


		// Last day in each season
		final int month1End = 147;
		final int month2End = 294;
		final int month3End = 440;
		final int month4End = 585;
		final int month5End = 730;
		final int month6End = 876;
		final int month7End = 1022;
		final int month8End = 1168;
		final int month9End = 1314;
		final int month10End = 1460;
		final int month11End = 1606;
		final int month12End = 1752;
		final int month13End = 1898;
		final int month14End = 2045;
		final int month15End = 2192;

		// Gets current server year
		int yearNum = 0;
		// If the amount is a clean multiple of 2192 (days in one year)
		if (dayNum % month15End == 0) {
			yearNum = dayNum / month1End;
		} else {
			yearNum = (int) (double) (dayNum / month1End) + 1;
		}

		// Gets current server day in the given year
		int dayNumInYear = (dayNum % month15End) + 1;
		int dayNumInMonth = 0;

		String monthName = null;
		// Gets the current server month
		if (dayNumInYear >= 1 && dayNumInYear < month1End) {
			monthName = "Ignivór";
		}
		else if (dayNumInYear > month1End && dayNumInYear <= month2End) {
			monthName = "Aquinvór";
			dayNumInMonth = dayNumInYear - month1End;
		} else if (dayNumInYear >= month2End && dayNumInYear <= month3End) {
			monthName = "Nebulivór";
			dayNumInMonth = dayNumInYear - month2End;
		} else if (dayNumInYear > month3End && dayNumInYear <= month4End) {
			monthName = "Ventirór";
			dayNumInMonth = dayNumInYear - month3End;
		} else if (dayNumInYear > month4End && dayNumInYear <= month5End) {
			monthName = "Florivór";
			dayNumInMonth = dayNumInYear - month4End;
		} else if (dayNumInYear > month5End && dayNumInYear <= month6End) {
			monthName = "Calorvór";
			dayNumInMonth = dayNumInYear - month5End;
		} else if (dayNumInYear > month6End && dayNumInYear <= month7End) {
			monthName = "Solarvór";
			dayNumInMonth = dayNumInYear - month6End;
		} else if (dayNumInYear > month7End && dayNumInYear <= month8End) {
			monthName = "Aestivór";
			dayNumInMonth = dayNumInYear - month7End;
		} else if (dayNumInYear > month8End && dayNumInYear <= month9End) {
			monthName = "Ardorvór";
			dayNumInMonth = dayNumInYear - month8End;
		} else if (dayNumInYear > month9End && dayNumInYear <= month10End) {
			monthName = "Fructivór";
			dayNumInMonth = dayNumInYear - month9End;
		} else if (dayNumInYear > month10End && dayNumInYear <= month11End) {
			monthName = "Follivór";
			dayNumInMonth = dayNumInYear - month10End;
		} else if (dayNumInYear > month11End && dayNumInYear <= month12End) {
			monthName = "Umbravór";
			dayNumInMonth = dayNumInYear - month11End;
		} else if (dayNumInYear > month12End && dayNumInYear <= month13End) {
			monthName = "Glacivór";
			dayNumInMonth = dayNumInYear - month12End;
		} else if (dayNumInYear > month13End && dayNumInYear <= month14End) {
			monthName = "Frigorvór";
			dayNumInMonth = dayNumInYear - month13End;
		} else if (dayNumInYear > month14End) {
			monthName = "Obscurvór";
			dayNumInMonth = dayNumInYear - month14End;
		} else {
			Bukkit.getLogger().info("Something went wrong with calculating the month name!");
			return;
		}

		// Gets current server weekday
		int weekdayNum = (dayNum % 8) + 1;
		String weekdayName = null;
		if (weekdayNum == 1) {
			weekdayName = "Hydris";
		} else if (weekdayNum == 2) {
			weekdayName = "Terris";
		}
		else if (weekdayNum == 3) {
			weekdayName = "Pyris";
		}
		else if (weekdayNum == 4) {
			weekdayName = "Aeris";
		}
		else if (weekdayNum == 5) {
			weekdayName = "Ferris";
		}
		else if (weekdayNum == 6) {
			weekdayName = "Sylvis";
		}
		else if (weekdayNum == 7) {
			weekdayName = "Umbris";
		}
		else if (weekdayNum == 8) {
			weekdayName = "Aethis";
		} else {
			Bukkit.getLogger().info("Something went wrong with calculating the weekday name!");
			return;
		}

		if (AranarthUtils.getMonthName() == null) {
			AranarthUtils.setMonthName(monthName);
		}

		// If it is a new day
		// First 5 seconds of a new day
		if (time >= 0 && time < 5) {
			displayServerDate(dayNumInMonth, yearNum, monthName, weekdayName);
		}
		determineMonthEffects();
	}

	private void displayServerDate(int dayNumInMonth, int yearNum, String monthName, String weekdayName) {
		String dayNumAsString = dayNumInMonth + "";
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
			player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 5f, 0.5f);
		}
	}

	private void determineMonthEffects() {
        switch (AranarthUtils.getMonthName()) {
            case "Ignivór" -> applyIgnivorEffects();
            case "Aquinvór" -> applyAquinvorEffects();
            case "Nebulivór" -> applyNebulivorEffects();
            case "Ventirór" -> applyVentirorEffects();
            case "Florivór" -> applyFlorivorEffects();
            case "Calorvór" -> applyCalorvorEffects();
            case "Solarvór" -> applySolarvorEffects();
            case "Aestivór" -> applyAestivorEffects();
            case "Ardorvór" -> applyArdorvorEffects();
            case "Fructivór" -> applyFructivorEffects();
            case "Follivór" -> applyFollivorEffects();
            case "Umbravór" -> applyUmbravorEffects();
            case "Glacivór" -> applyGlacivorEffects();
            case "Frigorvór" -> applyFrigorvorEffects();
            case "Obscurvór" -> applyObscurvorEffects();
            default -> Bukkit.getLogger().info("Something went wrong with applying " + AranarthUtils.getMonthName() + "'s effects!");
        }
	}

	/**
	 * Apply the effects during the first month of Ignivor.
	 */
	private void applyIgnivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.LUCK, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.REGENERATION, 320, 0));
		applyEffectToAllPlayers(effects);
	}

	/**
	 * Apply the effects during the second month of Aquinvor.
	 */
	private void applyAquinvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.WATER_BREATHING, 320, 0));
		applyEffectToAllPlayers(effects);
	}

	/**
	 * Apply the effects during the third month of Nebulivor.
	 */
	private void applyNebulivorEffects() {

	}

	/**
	 * Apply the effects during the fourth month of Ventiror.
	 */
	private void applyVentirorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SPEED, 320, 0));
		applyEffectToAllPlayers(effects);
	}

	/**
	 * Apply the effects during the fifth month of Florivor.
	 */
	private void applyFlorivorEffects() {

	}

	/**
	 * Apply the effects during the sixth month of Calorvor.
	 */
	private void applyCalorvorEffects() {

	}

	/**
	 * Apply the effects during the seventh month of Solarvor.
	 */
	private void applySolarvorEffects() {

	}

	/**
	 * Apply the effects during the eighth month of Aestivor.
	 */
	private void applyAestivorEffects() {

	}

	/**
	 * Apply the effects during the ninth month of Ardorvor.
	 */
	private void applyArdorvorEffects() {

	}

	/**
	 * Apply the effects during the tenth month of Fructivor.
	 */
	private void applyFructivorEffects() {

	}

	/**
	 * Apply the effects during the eleventh month of Follivor.
	 */
	private void applyFollivorEffects() {

	}

	/**
	 * Apply the effects during the twelfth month of Umbravor.
	 */
	private void applyUmbravorEffects() {

	}

	/**
	 * Apply the effects during the thirteenth month of Glacivor.
	 */
	private void applyGlacivorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);
	}

	/**
	 * Apply the effects during the fourteenth month of Frigorvor.
	 */
	private void applyFrigorvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 1));
		applyEffectToAllPlayers(effects);
	}

	/**
	 * Apply the effects during the fifteenth month of Obscurvor.
	 */
	private void applyObscurvorEffects() {
		List<PotionEffect> effects = new ArrayList<>();
		effects.add(new PotionEffect(PotionEffectType.MINING_FATIGUE, 320, 0));
		effects.add(new PotionEffect(PotionEffectType.SLOWNESS, 320, 0));
		applyEffectToAllPlayers(effects);
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

}
