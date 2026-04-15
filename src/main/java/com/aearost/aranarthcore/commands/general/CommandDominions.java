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

		meta.addPage(title());
		meta.addPage(introduction());
		meta.addPage(creation());
		meta.addPage(landClaiming());
		meta.addPage(members());
		meta.addPage(diplomacyAlliance());
		meta.addPage(diplomacyEnmity());
		meta.addPage(conquest());
		meta.addPage(rebellion());
		meta.addPage(foodUpkeep());
		meta.addPage(resources());
		meta.addPage(mapAndInfo());

		book.setItemMeta(meta);
		player.getInventory().addItem(book);
	}

	private static String title() {
		return ChatUtils.translateToColor("\n\n\n\n    &l&oDominions\n   of Aranarth");
	}

	private static String introduction() {
		return ChatUtils.translateToColor(
				"&lDominions&r are player-run territories in Aranarth. " +
				"Claim land, recruit members, forge alliances, wage war, and manage resources.\n\n" +
				"A Dominion's survival depends on keeping its food stores stocked and its treasury funded. " +
				"Neglect either long enough, and it will crumble."
		);
	}

	private static String creation() {
		return ChatUtils.translateToColor(
				"&lFounding a Dominion&r\n\n" +
				"Run &o/dominion create <name>&r to found your Dominion for &a$5,000&r. " +
				"Names may be up to 30 characters.\n\n" +
				"Stand at your chosen home point and run &o/dominion sethome&r. " +
				"You may later use &o/dominion rename <name>&r to rename, " +
				"or &o/dominion disband&r to dissolve it entirely."
		);
	}

	private static String landClaiming() {
		return ChatUtils.translateToColor(
				"&lLand Claiming&r\n\n" +
				"Claim your current chunk with &o/dominion claim&r (&a$250&r). " +
				"Unclaiming refunds &a$125&r directly to your wallet.\n\n" +
				"Every new claim must be adjacent to an existing one — you cannot leave " +
				"gaps in your territory. Your Dominion may hold up to &n25 chunks per member&r.\n\n" +
				"Toggle &o/dominion autoclaim&r to claim chunks automatically as you walk."
		);
	}

	private static String members() {
		return ChatUtils.translateToColor(
				"&lMembers&r\n\n" +
				"Invite players with &o/dominion invite <player>&r. " +
				"The invited player accepts with &o/dominion accept&r.\n\n" +
				"Members may build and interact freely on Dominion land. " +
				"Remove a member with &o/dominion remove <player>&r, " +
				"or hand over leadership with &o/dominion setleader <player>&r.\n\n" +
				"Members may step down with &o/dominion leave&r."
		);
	}

	private static String diplomacyAlliance() {
		return ChatUtils.translateToColor(
				"&lDiplomacy \u2014 Peace&r\n\n" +
				"Both Dominion leaders must run the same command to enter a peaceful relation.\n\n" +
				"&a/dominion ally <dominion>&r — " +
				"Sends or accepts an alliance request. Allied members cannot harm one another.\n\n" +
				"&e/dominion truce <dominion>&r — " +
				"Sends or accepts a truce. A lighter peace pact; truced members cannot harm each other."
		);
	}

	private static String diplomacyEnmity() {
		return ChatUtils.translateToColor(
				"&lDiplomacy \u2014 War&r\n\n" +
				"&c/dominion enemy <dominion>&r — " +
				"Declares war on another Dominion. Only one leader needs to act. " +
				"Any existing alliance or truce is automatically broken.\n\n" +
				"&7/dominion neutral <dominion>&r — " +
				"Requests to reset all relations back to neutral. " +
				"Both leaders must run this to resolve an enmity."
		);
	}

	private static String conquest() {
		return ChatUtils.translateToColor(
				"&lConquest&r\n\n" +
				"Only a free (unconquered) Dominion may conquer another. " +
				"The attacking leader runs &o/dominion conquer <dominion>&r.\n\n" +
				"To complete the conquest, the defending leader must run " +
				"&o/dominion surrender <attacker>&r. Until this is done the request remains open.\n\n" +
				"A conquered Dominion suffers weakened food power and heavily limited resource claims."
		);
	}

	private static String rebellion() {
		return ChatUtils.translateToColor(
				"&lRebellion&r\n\n" +
				"A conquered Dominion may seek freedom by running " +
				"&o/dominion rebel <conqueror>&r.\n\n" +
				"The conquering leader then chooses to accept by running " +
				"&o/dominion retreat <rebel>&r, releasing the Dominion from conquest. " +
				"If the conqueror does not retreat, the conquest persists.\n\n" +
				"Upon independence, all conquest penalties are lifted."
		);
	}

	private static String foodUpkeep() {
		return ChatUtils.translateToColor(
				"&lFood Upkeep&r\n\n" +
				"Every Dominion consumes food daily. " +
				"Stock the food stores via &o/dominion food&r.\n\n" +
				"Daily consumption by size:\n" +
				"  \u2264&n25 chunks:&r 100 power\n" +
				"  &n26-100 chunks:&r 250 power\n" +
				"  &n>100 chunks:&r 500 power\n\n" +
				"If food runs out, money is spent ($250/day). " +
				"If money runs out, chunks are sold. " +
				"A Dominion that loses its last chunk disbands."
		);
	}

	private static String resources() {
		return ChatUtils.translateToColor(
				"&lResources&r\n\n" +
				"Leaders open the resource GUI with &o/dominion resources&r. " +
				"Each claim draws materials from a biome present within your territory \u2014 " +
				"different biomes yield different goods.\n\n" +
				"A free Dominion starts with 16 claims, gaining more for each Dominion conquered. " +
				"A conquered Dominion is capped at 8 claims with a 50% success rate per claim."
		);
	}

	private static String mapAndInfo() {
		return ChatUtils.translateToColor(
				"&lMap & Info&r\n\n" +
				"&o/dominion map&r \u2014 shows a 15\u00d715 chunk view of your surroundings, " +
				"colour-coded by relation: green (own), purple (allied), " +
				"yellow (truced), red (enemy).\n\n" +
				"&o/dominion info&r \u2014 detailed status\n" +
				"&o/dominion balance&r \u2014 treasury\n" +
				"&o/dominion who&r \u2014 chunk ownership\n" +
				"&o/dominion list&r \u2014 all Dominions"
		);
	}

}
