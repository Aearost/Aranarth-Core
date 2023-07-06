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
				// If a new world needs to be created, uncomment below
				/*
				WorldCreator wc = new WorldCreator("arena");
				wc.environment(World.Environment.NORMAL);
				wc.type(WorldType.FLAT);
				wc.createWorld();
				*/
				for (World world : Bukkit.getWorlds()) {
					System.out.println(world.getName());
				}
				player.teleport(new Location(Bukkit.getWorld("arena"), 0, 105, 0, 180, 0));
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
