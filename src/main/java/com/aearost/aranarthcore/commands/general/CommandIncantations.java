package com.aearost.aranarthcore.commands.general;

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

			meta.addPage(title());
			meta.addPage(introduction());
			meta.addPage(applying());
			meta.addPage(applyingPlentiful());
			meta.addPage(lifesteal());
			meta.addPage(beheading());
			meta.addPage(plentiful());

			book.setItemMeta(meta);
			player.getInventory().addItem(book);
			return true;
		} else {
			sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			return false;
		}
	}

	private static String title() {
		return ChatUtils.translateToColor("\n\n\n\n   &l&oIncantations\n   of Aranarth");
	}

	private static String introduction() {
		return ChatUtils.translateToColor(
				"&lIncantations&r are Aranarth's own enchanting system, granting powerful " +
				"bonuses to weapons and tools beyond what standard enchantments offer.\n\n" +
				"They are obtained exclusively from &6Epic Crates&r, found at spawn. " +
				"Each crate has an equal chance to yield a &aLifesteal&r, " +
				"&6Plentiful&r, or &4Beheading&r incantation.\n\n" +
				"Only &none incantation&r may be applied per item."
		);
	}

	private static String applying() {
		return ChatUtils.translateToColor(
				"&lApplying Incantations&r\n\n" +
				"Drop the incantation item directly on top of the weapon or tool you " +
				"wish to enchant. The two items must land within a very close proximity " +
				"of one another.\n\n" +
				"After a short delay the incantation is consumed and applied.\n\n" +
				"&lLifesteal&r and &lBeheading&r apply to melee weapons: swords, axes, " +
				"maces, tridents, and spears. Applying the same incantation again " +
				"increases its level (up to &nLevel III&r)."
		);
	}

	private static String applyingPlentiful() {
		return ChatUtils.translateToColor(
				"&lApplying Plentiful&r\n\n" +
				"The &6Incantation of Plentiful&r requires an extra step. " +
				"You must drop the incantation &ntogether with an Aranarthium Ingot&r " +
				"near the target tool — all three items must be in close proximity at " +
				"the same time.\n\n" +
				"Plentiful applies to mining tools: pickaxes, axes, shovels, and hoes. " +
				"It has only one level and cannot be upgraded further."
		);
	}

	private static String lifesteal() {
		return ChatUtils.translateToColor(
				"&a&lIncantation of Lifesteal&r\n" +
				"&oApplies to: swords, axes,\nmaces, tridents, spears&r\n\n" +
				"On each melee hit, a portion of the damage dealt is converted into " +
				"health for the attacker.\n\n" +
				"  &nLevel I&r — heals &a15%&r of damage dealt\n" +
				"  &nLevel II&r — heals &a30%&r of damage dealt\n" +
				"  &nLevel III&r — heals &a50%&r of damage dealt"
		);
	}

	private static String beheading() {
		return ChatUtils.translateToColor(
				"&4&lIncantation of Beheading&r\n" +
				"&oApplies to: swords, axes,\nmaces, tridents, spears&r\n\n" +
				"On killing a player, grants a chance to drop their severed head.\n\n" +
				"  &nLevel I&r — &c25%&r chance to drop a skull\n" +
				"  &nLevel II&r — &c50%&r chance to drop a skull\n" +
				"  &nLevel III&r — &c75%&r chance to drop a skull"
		);
	}

	private static String plentiful() {
		return ChatUtils.translateToColor(
				"&6&lIncantation of Plentiful&r\n" +
				"&oApplies to: pickaxes, axes,\nshovels, hoes&r\n\n" +
				"Expands each block break into an area:\n\n" +
				"  &nPickaxe / Shovel&r — breaks a 3\u00d73 face aligned to your " +
				"facing direction\n" +
				"  &nAxe / Hoe&r — breaks a full 3\u00d73\u00d73 cube\n\n" +
				"Only appropriate block types are broken. " +
				"Dominion land ownership is respected."
		);
	}

}
