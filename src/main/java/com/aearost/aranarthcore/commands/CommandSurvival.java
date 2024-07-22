package com.aearost.aranarthcore.commands;

import java.io.IOException;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

/**
 * Teleports the player to the survival world, sharing the survival inventory.
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
					AranarthUtils.switchInventory(player, Objects.requireNonNull(player.getLocation().getWorld()).getName(), "world");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					return false;
				}
				for (PotionEffect effect : player.getActivePotionEffects()) {
					player.removePotionEffect(effect.getType());
				}
				player.teleport(new Location(Bukkit.getWorld("world"), 0.5, 120, 3, 180, 0));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to &eSurvival!"));
				player.setGameMode(GameMode.SURVIVAL);
				// To affect non-op players
				if (!(player.getName().equals("Aearost") || player.getName().equals("Aearxst"))) 
				{
					player.setOp(false);
				}
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
