package com.aearost.aranarthcore.commands;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandArena {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				// Teleports you to the arena world aligning directly with the Enter Arena sign
				try {
					AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "arena");
				} catch (IOException e) {
					player.sendMessage(ChatUtils.chatMessageError("Something went wrong with changing world."));
					e.printStackTrace();
					return false;
				}
				player.teleport(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));
				player.sendMessage(ChatUtils.chatMessage("&7You have been teleported to the &eArena!"));
				player.setGameMode(GameMode.SURVIVAL);
				// To affect non-op players
				if (!(player.getName().equals("Aearost") || player.getName().equals("Aearxst"))) {
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
