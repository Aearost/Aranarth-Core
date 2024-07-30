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
                // Teleports you to the arena world aligning directly with the Enter Arena sign
				try {
					if (Objects.nonNull(player.getLocation().getWorld())) {
						AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "arena");
					}
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					return false;
				}
				for (PotionEffect effect : player.getActivePotionEffects()) {
					player.removePotionEffect(effect.getType());
				}
				player.teleport(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eArena!"));
				player.setGameMode(GameMode.SURVIVAL);

				PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
				perms.setPermission("worldedit.*", false);

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
