package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Provides the player with a written book outlining how Aranarthium functions.
 */
public class CommandAranarthium {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player player) {
			ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
			BookMeta meta = (BookMeta) book.getItemMeta();
			meta.setItemName(ChatUtils.translateToColor("&8&l--=&6&lAranarthium&8&l=--"));
			meta.setAuthor(ChatUtils.translateToColor("&8Unknown"));

			meta.addPage(bookIntro());
			meta.addPage(ingotIntro());
			meta.addPage(aquaticArmour());
			meta.addPage(ardentArmour());
			meta.addPage(dwarvenArmour());
			meta.addPage(elvenArmour());
			meta.addPage(scorchedArmour());
			meta.addPage(soulboundArmour());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return true;
		}
	}

	private static String bookIntro() {
		return ChatUtils.translateToColor(
				"&OThe &lRealm of Aranarth&r&O exudes a sacred energy, infusing its ores with a little bit of magic.\n\n" +
						"&l&nOre Clusters\n&rOre clusters are dropped from copper, iron, gold, nether quartz, lapis, redstone, diamond, and emerald ores, at very low rates."
		);
	}

	private static String ingotIntro() {
		return ChatUtils.translateToColor(
				"&l&nAranarthium\n&rWhen crafting all 8 ore clusters together with a netherite ingot, an &lAranarthium Ingot&r is created.\n\n" +
						"This ingot is used as a base to crafting all &oEnhanced Aranarthium Ingots&r, which are applied to netherite armour and grant special abilities."
		);
	}

	private static String aquaticArmour() {
		return ChatUtils.translateToColor(
				"&lAquatic Aranarthium\n\n&r" +
						"Provides the wearer with the status effects of &oDolphin's Grace&r and the &oConduit Power&r.\n\nAdditionally yields increased damage when using a &lTrident&r, and results in immunity of &oMiner's Fatigue."
		);
	}

	private static String ardentArmour() {
		return ChatUtils.translateToColor(
				"&lArdent Aranarthium\n\n&r" +
						"Provides the wearer with the status effects of &oStrength II, Resistance II,&r and an extra row of hearts.\n\nAdditionally yields increased damage using &lSwords."
		);
	}

	private static String dwarvenArmour() {
		return ChatUtils.translateToColor(
				"&lDwarven Aranarthium\n\n&r" +
						"Provides the wearer with the status effects of &oNight Vision&r, and improved ore and cluster drop rates.\n\nAdditionally yields increased damage using &lAxes &rand &lMaces."
		);
	}

	private static String elvenArmour() {
		return ChatUtils.translateToColor(
				"&lElven Aranarthium\n\n&r" +
						"Provides the wearer with the status effects of &oSpeed III&r and &oJump Boost II&r.\n\nAdditionally yields increased damage using &lBows."
		);
	}

	private static String scorchedArmour() {
		return ChatUtils.translateToColor(
				"&lScorched Aranarthium\n\n&r" +
						"Provides the wearer with the status effects of &oFire Resistance&r and Resistance I&r.\n\nAdditionally causes all melee damage to ignite targets, and the wearer is not targeted by Piglins."
		);
	}

	private static String soulboundArmour() {
		return ChatUtils.translateToColor(
				"&lSoulbound Aranarthium\n\n&r" +
						"Provides the wearer with no special status effects, but instead with the blessing of life-everlasting.\n\nThe wearer will have their full inventory persist across death."
		);
	}

}
