package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
		meta.setAuthor(ChatUtils.translateToColor("&8" + Lang.getFor(player, "info.unknown")));

		meta.addPage(Lang.getFor(player, "book.dominions_intro"));
		meta.addPage(Lang.getFor(player, "book.dominions_creation"));
		meta.addPage(Lang.getFor(player, "book.dominions_claiming"));
		meta.addPage(Lang.getFor(player, "book.dominions_members"));
		meta.addPage(Lang.getFor(player, "book.dominions_food_1"));
		meta.addPage(Lang.getFor(player, "book.dominions_food_2"));
		meta.addPage(Lang.getFor(player, "book.dominions_resources"));
		meta.addPage(Lang.getFor(player, "book.dominions_peace"));
		meta.addPage(Lang.getFor(player, "book.dominions_war"));
		meta.addPage(Lang.getFor(player, "book.dominions_conquest_1"));
		meta.addPage(Lang.getFor(player, "book.dominions_conquest_2"));
		meta.addPage(Lang.getFor(player, "book.dominions_rebellion"));
		meta.addPage(Lang.getFor(player, "book.dominions_map"));

		book.setItemMeta(meta);
		player.getInventory().addItem(book);
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "book.received", "name", "Dominions")));
	}

}
