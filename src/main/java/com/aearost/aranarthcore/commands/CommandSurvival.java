package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;

import java.io.IOException;

/**
 * Teleports the player to the Survival world, sharing the Survival inventory.
 */
public class CommandSurvival {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
                // Teleports you to the survival world spawn
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					return false;
				}

				// Only remove potion effects if changing from a non-survival world
				if (!player.getLocation().getWorld().getName().startsWith("world")) {
					for (PotionEffect effect : player.getActivePotionEffects()) {
						player.removePotionEffect(effect.getType());
					}
				}

				Location loc = new Location(Bukkit.getWorld("world"), -29.5, 75, -73.5, 0, 0);
				player.teleport(loc);
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival!"));
				player.setGameMode(GameMode.SURVIVAL);

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
