package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Opens the player's ender chest remotely.
 * Admins can also view and modify any online player's ender chest by specifying a username.
 */
public class CommandEnderchest implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
			return true;
		}

		if (args.length == 0) {
			if (!player.hasPermission("aranarth.enderchest")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.getCombatLogTime().isEmpty()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.combat_tagged")));
				return true;
			}
			player.openInventory(player.getEnderChest());
			return true;
		}

		if (args.length == 1) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (!aranarthPlayer.isInAdminMode()) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("admin.must_be_admin_ender")));
				return true;
			}

			Player target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
				return true;
			}

			player.openInventory(target.getEnderChest());
			return true;
		}

		player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "enderchest [username]")));
		return true;
	}

}
