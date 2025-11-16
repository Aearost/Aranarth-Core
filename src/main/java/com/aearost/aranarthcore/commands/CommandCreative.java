package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
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
import java.util.Objects;

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
				if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
					player.sendMessage(ChatUtils.chatMessage("&cYou do not have access to this world!"));
					return true;
				}

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
				AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
				aranarthPlayer.setLastKnownTeleportLocation(player.getLocation());
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

				player.teleport(new Location(Bukkit.getWorld("creative"), 0, -60, 0, 0, 2));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eCreative!"));
				player.setGameMode(GameMode.CREATIVE);

				PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());
				perms.setPermission("worldedit.*", true);

				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cYou must be a player to use this command!"));
				return true;
			}
		}
		return false;
	}

}
