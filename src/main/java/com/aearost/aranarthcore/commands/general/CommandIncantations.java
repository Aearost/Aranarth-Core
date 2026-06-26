package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.items.incantation.IncantationLifesteal;
import com.aearost.aranarthcore.items.incantation.IncantationMagnetism;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Provides the player with a written book outlining the way incantations work in Aranarth.
 */
public class CommandIncantations implements CommandExecutor {

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
			meta.setItemName(ChatUtils.translateToColor("&8&l--=&6&lIncantations&8&l=--"));
			meta.setAuthor(ChatUtils.translateToColor("&8Unknown"));

			meta.addPage(introduction1());
			meta.addPage(introduction2());
			meta.addPage(applying());
			meta.addPage(beheading());
			meta.addPage(lifesteal());
			meta.addPage(plentiful());
			meta.addPage(magnetism());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			player.sendMessage(ChatUtils.chatMessage("&7You have received the &eIncantations&7 book!"));
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return false;
		}
	}

	private static String introduction1() {
		return ChatUtils.translateToColor(
				"&lIncantations&r are Aranarth's own enchanting system, granting powerful " +
						"bonuses to weapons and tools beyond what the standard enchantments offer.\n\n" +
						"They are obtained exclusively from the crates found at &6/warp crates&r."
		);
	}

	private static String introduction2() {
		return ChatUtils.translateToColor(
				"There are three different Incantations that are currently found on Aranarth:\n"
					+ "- " + new IncantationBeheading().getColor() + "&lIncantation of Beheading&r\n"
					+ "- " + new IncantationLifesteal().getColor() + "&lIncantation of Lifesteal&r\n"
					+ "- " + new IncantationPlentiful().getColor() + "&lIncantation of Plentiful&r\n\n"
					+ "Note that only &oone incantation&r may be applied per item."
		);
	}

	private static String applying() {
		return ChatUtils.translateToColor(
				"&lApplying Incantations&r\n" +
				"Drop the incantation item onto the weapon or tool you " +
				"wish to apply it to. After a short delay, the incantation will be consumed and applied.\n\n" +
				"Incantations can have several levels, increased by applying the same Incantation."
		);
	}

	private static String beheading() {
		return ChatUtils.translateToColor(
				new IncantationBeheading().getColor() + "&lThe Incantation\nof Beheading&r\n\n" +
						"&oIncreases the drop rates of player heads\n\n" +
						"&rApplies to: swords, axes, maces, tridents, spears\n\n" +
						"- 25% chance at I\n" +
						"- 50% chance at II\n" +
						"- 75% chance at III\n"
		);
	}

	private static String lifesteal() {
		return ChatUtils.translateToColor(
				new IncantationLifesteal().getColor() + "&lThe Incantation\nof Lifesteal&r\n\n" +
						"&oHeal off of your dealt damage\n\n" +
						"&rApplies to: swords, axes, maces, tridents, spears\n\n" +
						"- 15% heal at I\n" +
						"- 30% heal at II\n" +
						"- 50% heal at III\n"
		);
	}

	private static String plentiful() {
		return ChatUtils.translateToColor(
				new IncantationPlentiful().getColor() + "&lThe Incantation\nof Plentiful&r\n" +
						"&oIncreased block harvest size\n\n" +
						"&rApplies to: pickaxes, axes, shovels, hoes\n\n" +
						"You must drop an " + new AranarthiumIngot().getName() +
						" &ringot onto the tool, followed by the incantation. There is only one level of Plentiful."
		);
	}

	private static String magnetism() {
		return ChatUtils.translateToColor(
				new IncantationMagnetism().getColor() + "&lThe Incantation\nof Magnetism&r\n" +
						"&oPulls harvested items to you\n\n" +
						"&rApplies to: pickaxes, axes, shovels, hoes\n\n" +
						"You must drop an " + new AranarthiumIngot().getName() +
						" &ringot onto the tool, followed by the incantation. There is only one level of Magnetism."
		);
	}

}
