package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Provides the player with a written book outlining the way the calendar works in Aranarth.
 */
public class CommandCalendar {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta meta = (BookMeta) book.getItemMeta();
			meta.setItemName(ChatUtils.translateToColor("&8&l--=&6&lThe Calendar&8&l=--"));
			meta.setAuthor(ChatUtils.translateToColor("&8Unknown"));

			meta.addPage(title());
			meta.addPage(introduction());
			meta.addPage(ignivor());
			meta.addPage(aquinvor());
			meta.addPage(ventivor());
			meta.addPage(florivor());
			meta.addPage(aestivor());
			meta.addPage(calorvor());
			meta.addPage(ardorvor());
			meta.addPage(solarvor());
			meta.addPage(fructivor());
			meta.addPage(follivor());
			meta.addPage(faunivor());
			meta.addPage(umbravor());
			meta.addPage(glacivor());
			meta.addPage(frigorvor());
			meta.addPage(obscurvor());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
	}

	private static String title() {
		return ChatUtils.translateToColor("\n\n\n\n    &l&oThe Realm of\n      Aranarth");
	}

	private static String introduction() {
		return ChatUtils.translateToColor(
				"Welcome to the &lRealm of Aranarth&r, where time, seasons, and the world is different than what we typically know." +
				"\n\nThroughout the various months of Aranarth, the world will have different effects and unique behaviors."
		);
	}

	private static String ignivor() {
		return ChatUtils.translateToColor(
				"&lIgnivor (1)" +
						"\n&r&oThe New Fire" +
						"\n\n&rThe month of Ignivor represents the beginning of the new year. Players are granted passive Regeneration I, as well as passive Luck. While snowstorms are still a possibility, they are much rarer, and snow begins to melt."
		);
	}

	private static String aquinvor() {
		return ChatUtils.translateToColor(
				"&lAquinvor (2)" +
						"\n&r&oThe Dampened" +
						"\n\n&rThe month of Aquinvor marks the springtime. Players are granted the passive effects of Dolphin's Grace, and Water Breathing. Rain falls more frequently and lasts longer than other months, melting the remaining snow."
		);
	}

	private static String ventivor() {
		return ChatUtils.translateToColor(
				"&lVentivor (3)" +
						"\n&r&oThe Zephyr" +
						"\n\n&rThe month of Ventivor brings a wind behind your tail. The sound of wind is heard flying through the air, granting players with the passive effect of Speed I."
		);
	}

	private static String florivor() {
		return ChatUtils.translateToColor(
				"&lFlorivor (4)" +
						"\n&r&oThe Blossoming" +
						"\n\n&rThe month of Florivor smells of flower petals drifting in the wind. Aside from the cherry leaves across the world, there is also a doubled crop growth rate."
		);
	}

	private static String aestivor() {
		return ChatUtils.translateToColor(
				"&lAestivor (5)" +
						"\n&r&oThe Electrified" +
						"\n\n&rThe month of Aestivor is ridden with violent thunderstorms. The skies will thunder far more frequently than other months. There is also a 5% chance that creepers will spawn as the charged variant."
		);
	}

	private static String calorvor() {
		return ChatUtils.translateToColor(
				"&lCalorvor (6)" +
						"\n&r&oThe Warming" +
						"\n\n&rThe month of Calorvor starts off the summer. Baby animals mature at a quicker rate, and have a 50% chance of being born as a twin."
		);
	}

	private static String ardorvor() {
		return ChatUtils.translateToColor(
				"&lArdorvor (7)" +
						"\n&r&oThe Enflamed" +
						"\n\n&rThe month of Ardorvor is the month of flames. Forest fires start at random throughout the month, and all sources of fire deal more damage."
		);
	}

	private static String solarvor() {
		return ChatUtils.translateToColor(
				"&lSolarvor (8)" +
						"\n&r&oThe Fruitful" +
						"\n\n&rThe month of Solarvor favours the picking of apples. Increased apple drop rates, and frequent &6God Apple Fragments &rdrops, there will be plenty for all."
		);
	}

	private static String fructivor() {
		return ChatUtils.translateToColor(
				"&lFructivor (9)" +
						"\n&r&oThe Harvest" +
						"\n\n&rThe month of Fructivor is not for the lazy. Crop yields are doubled, and Farmer villagers favour the vendor by providing improved crop sell rates."
		);
	}

	private static String follivor() {
		return ChatUtils.translateToColor(
				"&lFollivor (10)" +
						"\n&r&oThe Lumbered" +
						"\n\n&rThe month of Follivor introduces the start of autumn. Trees provide more EXP and log drops, and saplings grow with more efficiency."
		);
	}

	private static String faunivor() {
		return ChatUtils.translateToColor(
				"&lFaunivor (11)" +
						"\n&r&oThe Hunting" +
						"\n\n&rThe month of Faunivor encourages a little bloodshed. The sacrificed will yield increased weapon damage, and increased drop rates for both meat and animal products."
		);
	}

	private static String umbravor() {
		return ChatUtils.translateToColor(
				"&lUmbravor (12)" +
						"\n&r&oThe Shadowed" +
						"\n\n&rThe month of Umbravor marks the beginning of winter. There is a low chance of world-wide snowstorms, and crops are no longer harvested as efficiently."
		);
	}

	private static String glacivor() {
		return ChatUtils.translateToColor(
				"&lGlacivor (13)" +
						"\n&r&oThe Frost" +
						"\n\n&rThe month of Glacivor sends shivers down your spine. You will be slowed down in your tracks, and water begins to freeze in rivers and the sea."
		);
	}

	private static String frigorvor() {
		return ChatUtils.translateToColor(
				"&lFrigorvor (14)" +
						"\n&r&oThe Frozen" +
						"\n\n&rThe month of Frigorvor brings the heaviest snowfalls. Your movements will be slowed even further, as will your crop growth rates."
		);
	}

	private static String obscurvor() {
		return ChatUtils.translateToColor(
				"&lObscurvor (15)" +
						"\n&r&oThe Darkness" +
						"\n\n&rThe month of Obscurvor is the month of rest. You will harvest blocks at a slower rate, and be hunted more frequently and aggressively by phantoms."
		);
	}

}
