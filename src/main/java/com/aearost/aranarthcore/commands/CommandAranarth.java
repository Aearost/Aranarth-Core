package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Teleports the player to the arena world, sharing the survival inventory.
 */
public class CommandAranarth {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta meta = (BookMeta) book.getItemMeta();
			meta.setItemName(ChatUtils.translateToColor("&8&l--=&6&lThe Realm of Aranarth&8&l=--"));
			meta.setAuthor(ChatUtils.translateToColor("&8Unknown"));

			meta.addPage(bookIntroduction());
			meta.addPage(bookCalendar1());
			meta.addPage(bookCalendar2());
			meta.addPage(bookWeek());
			meta.addPage(bookMonth1());
			meta.addPage(bookMonth2());
			meta.addPage(bookMonth3());
			meta.addPage(bookMonth4());
			meta.addPage(bookMonth5());
			meta.addPage(bookMonth6());
			meta.addPage(bookMonth7());
			meta.addPage(bookMonth8());
			meta.addPage(bookMonth9());
			meta.addPage(bookMonth10());
			meta.addPage(bookMonth11());
			meta.addPage(bookMonth12());
			meta.addPage(bookMonth13());
			meta.addPage(bookMonth14());
			meta.addPage(bookMonth15());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
	}

	private static String bookIntroduction() {
		return ChatUtils.translateToColor(
				"Welcome to the &lRealm of Aranarth&r, where time, seasons, and the world is different than what we typically know." +
				"\n\nIn this guide, you will find some key information on how to navigate the beautiful world, as well as all of the wonders that it has to offer."
		);
	}

	private static String bookCalendar1() {
		return ChatUtils.translateToColor(
				"&lYear" +
						"\n&rThere is a new Calendar system that exists in Aranarth, involving custom days of the week, and custom months. Throughout the various months, the world will have different effects depending on the month."
		);
	}

	private static String bookCalendar2() {
		return ChatUtils.translateToColor(
				"&lYear" +
						"\n&rOne year equates to roughly 30 days of in real life server uptime, being made up of 15 months and 2192 in-game days." +
						"\n\n&lMonth" +
						"\n&rEach month is 145 to 147 in-game days per month, averaging out to roughly 2 days per in-game month."
		);
	}

	private static String bookWeek() {
		return ChatUtils.translateToColor(
				"&lWeek" +
						"\n&rWeeks have 8 different days representing one of the elements." +
						"\n\nHydris - Water" +
						"\nTerris - Earth" +
						"\nPyris - Fire" +
						"\nAeris - Air" +
						"\nFerris - Metal" +
						"\nSylvis - Nature" +
						"\nUmbris - Darkness" +
						"\nAethis - Aether"
		);
	}

	private static String bookMonth1() {
		return ChatUtils.translateToColor(
				"&lIgnivor (1)" +
						"\n&r&oThe New Fire" +
						"\n\n&rThe month of Ignivor represents the beginning of the new year. Players are granted passive Regeneration I, as well as passive Luck. While snowstorms are still a possibility, they are much rarer, and snow begins to melt."
		);
	}

	private static String bookMonth2() {
		return ChatUtils.translateToColor(
				"&lAquinvor (2)" +
						"\n&r&oThe Dampened" +
						"\n\n&rThe month of Aquinvor is plentiful of rain. Players are granted the passive effects of Dolphin's Grace, and Water Breathing. Rain falls more frequently and lasts longer than other months, melting the remaining snow."
		);
	}

	private static String bookMonth3() {
		return ChatUtils.translateToColor(
				"&lVentivor (3)" +
						"\n&r&oThe Zephyr" +
						"\n\n&rThe month of Ventivor brings a wind behind your tail. The sound of wind is heard flying through the air, granting players with the passive effect of Speed I."
		);
	}

	private static String bookMonth4() {
		return ChatUtils.translateToColor(
				"&lFlorivor (4)" +
						"\n&r&oThe Blossoming" +
						"\n\n&rThe month of Florivor smells of flower petals drifting in the wind. Aside from the cherry leaves across the world, there is also a doubled crop growth rate."
		);
	}

	private static String bookMonth5() {
		return ChatUtils.translateToColor(
				"&lAestivor (5)" +
						"\n&r&oThe Electrified" +
						"\n\n&rThe month of Aestivor is ridden with violent thunderstorms. The skies will thunder far more frequently than other months. There is also a 5% chance that creepers will spawn as the charged variant."
		);
	}

	private static String bookMonth6() {
		return ChatUtils.translateToColor(
				"&lCalorvor (6)" +
						"\n&r&oThe Warming" +
						"\n\n&rThe month of Calorvor represents a period of warmth. Baby animals mature at a quicker rate, and have a 50% chance of being born as a twin."
		);
	}

	private static String bookMonth7() {
		return ChatUtils.translateToColor(
				"&lArdorvor (7)" +
						"\n&r&oThe Enflamed" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth8() {
		return ChatUtils.translateToColor(
				"&lSolarvor (8)" +
						"\n&r&oThe Fruitful" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth9() {
		return ChatUtils.translateToColor(
				"&lFructivor (9)" +
						"\n&r&oThe Harvest" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth10() {
		return ChatUtils.translateToColor(
				"&lFollivor (10)" +
						"\n&r&oThe Lumbered" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth11() {
		return ChatUtils.translateToColor(
				"&lFaunivor (11)" +
						"\n&r&oThe Hunting" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth12() {
		return ChatUtils.translateToColor(
				"&lUmbravor (12)" +
						"\n&r&oThe Shadowed" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth13() {
		return ChatUtils.translateToColor(
				"&lGlacivor (13)" +
						"\n&r&oThe Frost" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth14() {
		return ChatUtils.translateToColor(
				"&lFrigorvor (14)" +
						"\n&r&oThe Frozen" +
						"\n\n&rThe month of _____"
		);
	}

	private static String bookMonth15() {
		return ChatUtils.translateToColor(
				"&lObscurvor (15)" +
						"\n&r&oThe Darkness" +
						"\n\n&rThe month of _____"
		);
	}

}
