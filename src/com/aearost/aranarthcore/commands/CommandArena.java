package com.aearost.aranarthcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandArena {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				// Loads the world if it isn't yet loaded
				if (Bukkit.getWorld("arena") == null) {
					WorldCreator wc = new WorldCreator("arena");
					wc.environment(World.Environment.NORMAL);
					wc.type(WorldType.FLAT);
					wc.createWorld();
				}
				
				// Teleports you to the arena world aligning directly with the Enter Arena sign
				player.teleport(new Location(Bukkit.getWorld("arena"), 0.5, 105, 0.5, 180, 2));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
