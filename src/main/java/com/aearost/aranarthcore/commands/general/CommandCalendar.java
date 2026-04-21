package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DateUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Provides the player with a written book outlining the way the calendar works in Aranarth.
 */
public class CommandCalendar implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
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
			meta.addPage(follivor());
			meta.addPage(strigavor());
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
			return false;
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
				"#ffe082&lIgnivór &r&l(1)" +
						"\n&r&oThe New Fire" +
						"\n\n&r" + DateUtils.getIgnivorDescription()
		);
	}

	private static String aquinvor() {
		return ChatUtils.translateToColor(
				"&3&lAquinvór &r&l(2)" +
						"\n&r&oThe Dampened" +
						"\n\n&r" + DateUtils.getAquinvorDescription()
		);
	}

	private static String ventivor() {
		return ChatUtils.translateToColor(
				"#d1e5f4&lVentivór &r&l(3)" +
						"\n&r&oThe Zephyr" +
						"\n\n&r" + DateUtils.getVentivorDescription()
		);
	}

	private static String florivor() {
		return ChatUtils.translateToColor(
				"#FDA4BA&lFlorivór &r&l(4)" +
						"\n&r&oThe Blossoming" +
						"\n\n&r" + DateUtils.getFlorivorDescription()
		);
	}

	private static String aestivor() {
		return ChatUtils.translateToColor(
				"&e&lAestivór &r&l(5)" +
						"\n&r&oThe Electrified" +
						"\n\n&r" + DateUtils.getAestivorDescription()
		);
	}

	private static String calorvor() {
		return ChatUtils.translateToColor(
				"&6&lCalorvór &r&l(6)" +
						"\n&r&oThe Warming" +
						"\n\n&r" + DateUtils.getCalorvorDescription()
		);
	}

	private static String ardorvor() {
		return ChatUtils.translateToColor(
				"#ff4500&lArdorvór &r&l(7)" +
						"\n&r&oThe Enflamed" +
						"\n\n&r" + DateUtils.getArdorvorDescription()
		);
	}

	private static String solarvor() {
		return ChatUtils.translateToColor(
				"#BD5745&lSolarvór &r&l(8)" +
						"\n&r&oThe Fruitful" +
						"\n\n&r" + DateUtils.getSolarvorDescription()
		);
	}

	private static String follivor() {
		return ChatUtils.translateToColor(
				"#a17100&lFollivór &r&l(9)" +
						"\n&r&oThe Lumbered" +
						"\n\n&r" + DateUtils.getFollivorDescription()
		);
	}

	private static String strigavor() {
		return ChatUtils.translateToColor(
				"#8a00c2&lStrigavór &r&l(10)" +
						"\n&r&oThe Wicked" +
						"\n\n&r" + DateUtils.getStrigavorDescription()
		);
	}

	private static String faunivor() {
		return ChatUtils.translateToColor(
				"#5b0001&lFaunivór &r&l(11)" +
						"\n&r&oThe Hunting" +
						"\n\n&r" + DateUtils.getFaunivorDescription()
		);
	}

	private static String umbravor() {
		return ChatUtils.translateToColor(
				"#2B3856&lUmbravór &r&l(12)" +
						"\n&r&oThe Shadowed" +
						"\n\n&r" + DateUtils.getUmbravorDescription()
		);
	}

	private static String glacivor() {
		return ChatUtils.translateToColor(
				"#DBE9FA&lGlacivór &r&l(13)" +
						"\n&r&oThe Frost" +
						"\n\n&r" + DateUtils.getGlacivorDescription()
		);
	}

	private static String frigorvor() {
		return ChatUtils.translateToColor(
				"#79BAEC&lFrigorvór &r&l(14)" +
						"\n&r&oThe Frozen" +
						"\n\n&r" + DateUtils.getFrigorvorDescription()
		);
	}

	private static String obscurvor() {
		return ChatUtils.translateToColor(
				"#2C041C&lObscurvór &r&l(15)" +
						"\n&r&oThe Darkness" +
						"\n\n&r" + DateUtils.getObscurvorDescription()
		);
	}

}
