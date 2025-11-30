package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

/**
 * Teleports the player to the arena world, sharing the survival inventory.
 */
public class CommandArena {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				Location arenaSpawn = new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2);
				AranarthUtils.teleportPlayer(player, player.getLocation(), arenaSpawn);
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eArena &7world"));

				PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
				perms.setPermission("worldedit.*", false);

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
				return true;
			}
		}
		return false;
	}
}
