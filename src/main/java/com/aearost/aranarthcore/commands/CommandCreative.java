package com.aearost.aranarthcore.commands;

import java.io.IOException;
import java.util.Objects;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Teleports the player to the creative world, using the creative inventory.
 */
public class CommandCreative {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player player) {
                // Teleports you to the creative world spawn
				try {
					if (Objects.nonNull(player.getLocation().getWorld())) {
						AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "creative");
					}
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
					return false;
				}
				for (PotionEffect effect : player.getActivePotionEffects()) {
					player.removePotionEffect(effect.getType());
				}
				player.teleport(new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative!"));
				player.setGameMode(GameMode.CREATIVE);

				PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
				perms.setPermission("worldedit.*", true);

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
			}
		}
		return false;
	}

}
