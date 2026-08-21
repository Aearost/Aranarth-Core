package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.items.incantation.IncantationLifesteal;
import com.aearost.aranarthcore.items.incantation.IncantationMagnetism;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.items.incantation.IncantationPreservation;
import com.aearost.aranarthcore.items.incantation.IncantationResilience;
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
			meta.addPage(resilience());
			meta.addPage(preservation());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			player.sendMessage(ChatUtils.chatMessage("&7You have received the &eIncantations&7 book"));
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
						"They are obtained exclusively from the crates found at &6/crates&r."
		);
	}

	private static String introduction2() {
		return ChatUtils.translateToColor(
				"There are 6 different Incantations found in the Realm of Aranarth:\n"
					+ "- " + new IncantationBeheading().getColor() + "&lBeheading&r\n"
					+ "- " + new IncantationLifesteal().getColor() + "&lLifesteal&r\n"
					+ "- " + new IncantationPlentiful().getColor() + "&lPlentiful&r\n"
					+ "- " + new IncantationMagnetism().getColor() + "&lMagnetism&r\n"
					+ "- " + new IncantationResilience().getColor() + "&lResilience&r\n"
					+ "- " + new IncantationPreservation().getColor() + "&lPreservation&r\n"
					+ "Note that only &oone incantation&r may be applied per item. " +
						"Some additionally require an " + new AranarthiumIngot().getName() + " &ringot."
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
				new IncantationBeheading().getColor() + "&lBeheading&r\n" +
						"&oIncreases the drop rates of heads\n\n" +
						"&rApplies to: swords, axes, maces, tridents, and spears\n\n" +
						"Players: 25% at I, 50% at II, 75% at III\n\n" +
						"Mobs: 10% at I, 20% at II, 30% at III\n"
		);
	}

	private static String lifesteal() {
		return ChatUtils.translateToColor(
				new IncantationLifesteal().getColor() + "&lLifesteal&r\n\n" +
						"&oHeal off of your dealt damage. Requires an " + new AranarthiumIngot().getName() + "&r&o ingot\n\n" +
						"&rApplies to: swords, axes, maces, tridents, spears\n\n" +
						"- 15% heal at I\n" +
						"- 30% heal at II\n" +
						"- 50% heal at III\n"
		);
	}

	private static String plentiful() {
		return ChatUtils.translateToColor(
				new IncantationPlentiful().getColor() + "&lPlentiful&r\n\n" +
						"&oIncreased block harvest size. Requires an " + new AranarthiumIngot().getName() + "&r&o ingot\n\n" +
						"&rApplies to: pickaxes, axes, shovels, hoes\n\n" +
						"- Only one level\n"
		);
	}

	private static String magnetism() {
		return ChatUtils.translateToColor(
				new IncantationMagnetism().getColor() + "&lMagnetism&r\n\n" +
						"&oPulls harvested items to you. Requires an " + new AranarthiumIngot().getName() + "&r&o ingot\n\n" +
						"&rApplies to: pickaxes, axes, shovels, hoes\n\n" +
						"- Only one level\n"
		);
	}

	private static String resilience() {
		return ChatUtils.translateToColor(
				new IncantationResilience().getColor() + "&lResilience&r\n\n" +
						"&oMakes your item indestructible\n\n" +
						"&rApplies to: any tool, weapon, or armor\n\n" +
						"- Item never loses durability\n" +
						"- Cannot be destroyed by fire or lava\n" +
						"- Only one level\n"
		);
	}

	private static String preservation() {
		return ChatUtils.translateToColor(
				new IncantationPreservation().getColor() + "&lPreservation&r\n\n" +
						"&oCollection of unharvestable blocks. Requires an " + new AranarthiumIngot().getName() + "&r&o ingot\n\n" +
						"&rApplies to: pickaxes (no Fortune)\n\n" +
						"- Only one level\n"
		);
	}

}
