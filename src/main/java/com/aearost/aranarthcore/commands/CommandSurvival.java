package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Random;

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
				World world = Bukkit.getWorld("world");
				Random random = new Random();
				Location selectedLocation = null;
				boolean isLocationFound = false;
				while (!isLocationFound) {
					int x = random.nextInt(24501) - 12250;
					int z = random.nextInt(24501) - 12250;
					if (world.getHighestBlockAt(x, z).getType() != Material.WATER) {
						isLocationFound = true;
						selectedLocation = world.getHighestBlockAt(x, z).getLocation();
						selectedLocation.add(0, 1, 0);
					}
				}

				AranarthUtils.teleportPlayer(player, player.getLocation(), selectedLocation);
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival"));

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
