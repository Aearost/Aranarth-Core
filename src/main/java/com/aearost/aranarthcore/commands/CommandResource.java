package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * Teleports the player to the Resource world, sharing the Resource inventory.
 */
public class CommandResource {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				if (System.currentTimeMillis() < aranarthPlayer.getLastWorldCommandUse() + 60000) {
					if (!aranarthPlayer.isInAdminMode()) {
						int wait = (int) ((aranarthPlayer.getLastWorldCommandUse() + 60000) - System.currentTimeMillis()) / 1000;
						player.sendMessage(ChatUtils.chatMessage("&cYou must wait another &e" + wait + " seconds &cto use this command!"));
						return true;
					}
				}

				World resource = Bukkit.getWorld("resource");
				Random random = new Random();
				Location selectedLocation = null;
				boolean isLocationFound = false;
				while (!isLocationFound) {
					int x = random.nextInt(5001) - 2500;
					int z = random.nextInt(5001) - 2500;
					if (resource.getHighestBlockAt(x, z).getType() != Material.WATER) {
						isLocationFound = true;
						selectedLocation = resource.getHighestBlockAt(x, z).getLocation();
						selectedLocation.add(0, 1, 0);
					}
				}

				AranarthUtils.teleportPlayer(player, player.getLocation(), selectedLocation, aranarthPlayer.isInAdminMode(), success -> {
					if (success) {
						aranarthPlayer.setLastWorldCommandUse(System.currentTimeMillis());
						AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
						player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eResource &7world"));
					} else {
						player.sendMessage(ChatUtils.chatMessage("&cYou could not teleport to the &eResource &cworld"));
					}
				});
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
				return true;
			}
		}
		return false;
	}
}
