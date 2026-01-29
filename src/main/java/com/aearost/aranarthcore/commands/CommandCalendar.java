package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
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
				"&lIgnivór (1)" +
						"\n&r&oThe New Fire" +
						"\n\n&r" + DateUtils.getIgnivorDescription()
		);
	}

	private static String aquinvor() {
		return ChatUtils.translateToColor(
				"&lAquinvór (2)" +
						"\n&r&oThe Dampened" +
						"\n\n&r" + DateUtils.getAquinvorDescription()
		);
	}

	private static String ventivor() {
		return ChatUtils.translateToColor(
				"&lVentivór (3)" +
						"\n&r&oThe Zephyr" +
						"\n\n&r" + DateUtils.getVentivorDescription()
		);
	}

	private static String florivor() {
		return ChatUtils.translateToColor(
				"&lFlorivór (4)" +
						"\n&r&oThe Blossoming" +
						"\n\n&r" + DateUtils.getFlorivorDescription()
		);
	}

	private static String aestivor() {
		return ChatUtils.translateToColor(
				"&lAestivór (5)" +
						"\n&r&oThe Electrified" +
						"\n\n&r" + DateUtils.getAestivorDescription()
		);
	}

	private static String calorvor() {
		return ChatUtils.translateToColor(
				"&lCalorvór (6)" +
						"\n&r&oThe Warming" +
						"\n\n&r" + DateUtils.getCalorvorDescription()
		);
	}

	private static String ardorvor() {
		return ChatUtils.translateToColor(
				"&lArdorvór (7)" +
						"\n&r&oThe Enflamed" +
						"\n\n&r" + DateUtils.getArdorvorDescription()
		);
	}

	private static String solarvor() {
		return ChatUtils.translateToColor(
				"&lSolarvór (8)" +
						"\n&r&oThe Fruitful" +
						"\n\n&r" + DateUtils.getSolarvorDescription()
		);
	}

	private static String fructivor() {
		return ChatUtils.translateToColor(
				"&lFructivór (9)" +
						"\n&r&oThe Harvest" +
						"\n\n&r" + DateUtils.getFructivorDescription()
		);
	}

	private static String follivor() {
		return ChatUtils.translateToColor(
				"&lFollivór (10)" +
						"\n&r&oThe Lumbered" +
						"\n\n&r" + DateUtils.getFollivorDescription()
		);
	}

	private static String faunivor() {
		return ChatUtils.translateToColor(
				"&lFaunivór (11)" +
						"\n&r&oThe Hunting" +
						"\n\n&r" + DateUtils.getFaunivorDescription()
		);
	}

	private static String umbravor() {
		return ChatUtils.translateToColor(
				"&lUmbravór (12)" +
						"\n&r&oThe Shadowed" +
						"\n\n&r" + DateUtils.getUmbravorDescription()
		);
	}

	private static String glacivor() {
		return ChatUtils.translateToColor(
				"&lGlacivór (13)" +
						"\n&r&oThe Frost" +
						"\n\n&r" + DateUtils.getGlacivorDescription()
		);
	}

	private static String frigorvor() {
		return ChatUtils.translateToColor(
				"&lFrigorvór (14)" +
						"\n&r&oThe Frozen" +
						"\n\n&r" + DateUtils.getFrigorvorDescription()
		);
	}

	private static String obscurvor() {
		return ChatUtils.translateToColor(
				"&lObscurvór (15)" +
						"\n&r&oThe Darkness" +
						"\n\n&r" + DateUtils.getObscurvorDescription()
		);
	}

}
