package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Provides the guide book for the /dominion guide subcommand.
 */
public class CommandDominions {

	public static void giveBook(Player player) {
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setItemName(ChatUtils.translateToColor("&8&l--=&6&lDominions&8&l=--"));
		meta.setAuthor(ChatUtils.translateToColor("&8Unknown"));

		meta.addPage(introduction());
		meta.addPage(creation());
		meta.addPage(landClaiming());
		meta.addPage(members());
		meta.addPage(foodUpkeep1());
		meta.addPage(foodUpkeep2());
		meta.addPage(resources());
		meta.addPage(diplomacyPeace());
		meta.addPage(diplomacyEnmity());
		meta.addPage(conquest());
		meta.addPage(conquestEffects());
		meta.addPage(rebellion());
		meta.addPage(mapAndInfo());

		book.setItemMeta(meta);
		player.getInventory().addItem(book);
	}

	private static String introduction() {
		return ChatUtils.translateToColor(
				"&lDominions&r are the land-claiming system on Aranarth. " +
						"In your dominion, you can recruit members, forge &5alliances&r, wage &cwar&r, and claim your land's resources.\n\n" +
						"A Dominion's survival depends on keeping its &6food reserves &rstocked and its bank funded. "
		);
	}

	private static String creation() {
		return ChatUtils.translateToColor(
				"&lCreating a Dominion&r\n\n" +
						"Use &o/dominion create <name>&r to create your Dominion for &6$5,000&r.\n\n" +
						"Stand at your chosen home point and use &o/dominion sethome&r.\n\n" +
						"To return to your Dominion's home, use &o/dominion home&r."
		);
	}

	private static String landClaiming() {
		return ChatUtils.translateToColor(
				"&lLand Claiming&r\n" +
						"Claim the chunk you are currently in for &6$250 &rwith &o/dominion claim&r.\n\n" +
						"Every new claim must be connected to an existing one - you cannot have any " +
						"gaps in your territory. Your Dominion may hold up to &o25 chunks &rper member."
		);
	}

	private static String members() {
		return ChatUtils.translateToColor(
				"&lMembers&r\n\n" +
						"Invite players with &o/dominion invite <player>&r. " +
						"The invited player accepts with &o/dominion accept&r.\n\n" +
						"Members can be manually removed with &o/dominion remove <player>&r, " +
						"or they may leave the Dominion with &o/dominion leave&r."
		);
	}

	private static String foodUpkeep1() {
		return ChatUtils.translateToColor(
				"&lFood Upkeep&r\n\n" +
						"Every Dominion consumes food daily. " +
						"Stock the food stores using &o/dominion food&r.\n\n" +
						"The daily consumption is by the number of claimed chunks:\n" +
						"- \u226425 is &o100 power&r\n" +
						"- 26-100 is &o250 power&r\n" +
						"- >100 is &o500 power&r"
		);
	}

	private static String foodUpkeep2() {
		return ChatUtils.translateToColor(
				"&lFood Upkeep&r\n\n" +
						"If food runs out, money is spent ($250/day) from the Dominion's balance.\n\n" +
						"If the balance is fully emptied, chunks are sold.\n\n" +
						"When its last chunk is lost, the Dominion will be disbanded."
		);
	}

	private static String resources() {
		return ChatUtils.translateToColor(
				"&lResources&r\n" +
						"Harvested resources from the biomes in your Dominion chunks are claimable with &o/dominion resources&r. " +
						"Dominions begin with 16 stored claims, gaining 8 more for each Dominion conquered. " +
						"Conquered Dominions are capped at 8 claims with only a 50% claim success rate."
		);
	}

	private static String diplomacyPeace() {
		return ChatUtils.translateToColor(
				"&lDiplomacy - Peace&r\n\n" +
						"Both Dominion leaders must use the same command to enter a peaceful relation.\n\n" +
						"&5Alliance: &r&o/dominion ally <dominion>&r\n" +
						"&dTruce: &r&o/dominion truce <dominion>&r\n" +
						"&7Neutrality: &r&o/dominion neutral <dominion>&r\n"
		);
	}

	private static String diplomacyEnmity() {
		return ChatUtils.translateToColor(
				"&lDiplomacy - War&r\n\n" +
						"Dominions can go to war if one of the two leaders declares the other as an enemy. " +
						"Once declared, any existing alliance/truce is automatically broken.\n\n" +
						"&cEnemy: &r&o/dominion enemy <dominion>&r"
		);
	}

	private static String conquest() {
		return ChatUtils.translateToColor(
				"&lConquest&r\n\n" +
						"Only a free (unconquered) Dominion may conquer another. " +
						"The attacking leader uses &o/dominion conquer <dominion>&r.\n\n" +
						"To complete the conquest, the defending leader must use " +
						"&o/dominion surrender <attacker>&r\n\n"
		);
	}

	private static String conquestEffects() {
		return ChatUtils.translateToColor(
				"&lConquest&r\n\n" +
						"A conquered Dominion has half of the available resource claims, and their food power is 25% lower.\n\n" +
						"In addition to this, the conqueror's resource claim limit increases by 8 per conquered Dominion."
		);
	}

	private static String rebellion() {
		return ChatUtils.translateToColor(
				"&lRebellion&r\n\n" +
						"A conquered Dominion may seek freedom by using " +
						"&o/dominion rebel <conqueror>&r.\n\n" +
						"The conquering leader then chooses to accept by using " +
						"&o/dominion retreat <rebel>&r, releasing the Dominion from their conquest. "
		);
	}

	private static String mapAndInfo() {
		return ChatUtils.translateToColor(
				"&lMap & Info&r\n\n" +
						"You can use &o/dominion map&r to view your surrounding chunks, where the top is facing the North.\n\n" +
						"The chunks are color-coded:\n" +
						"- &a[] &r(your Dominion)\n" +
						"- &5[] &r(allied Dominions)\n" +
						"- &d[] &r(truced Dominions)\n" +
						"- &c[] &r(enemy Dominions)"
		);
	}

}
