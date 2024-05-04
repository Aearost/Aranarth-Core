package com.aearost.aranarthcore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.aearost.aranarthcore.gui.GuiPotions;
import com.aearost.aranarthcore.utils.ChatUtils;

public class CommandPotion {

	public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				if (args.length == 1) {
					player.sendMessage(ChatUtils.chatMessageError("You must specify a sub-command! /ac potion <sub-command>"));
					return false;
				}
				
				// MUST FIGURE OUT WHY IT IS NOT ENTERING THIS ELSE
				// MIGHT NEED TO REMOVE THE ELSE BLOCK AND JUST GO INTO THE CODE FROM THERE
				// IS IT JUST BECAUSE THERE ARE NO POTIONS THAT I HAVE?????
				// MIGHT NEED TO JUST MAKE SURE I OPEN INVENTORY EVEN IF THERE ARE NONE
				
				else {
					System.out.println("args[1]: " + args[1]);
					if (args[1].equals("view")) {
						// Output chat messages recapping each potion, their strength/duration, and quantity of them
					} else if (args[1].equals("add")) {
						System.out.println("???");
						GuiPotions gui = new GuiPotions(player);
						gui.openGui();
					} else if (args[1].equals("remove")) {
						// Check next sub-commands to ensure valid syntax
						// Also will need auto-completer to display all potions that they have
						// /ac potion remove HEALTH_BOOST 7 (assuming this is the accurate name)
						// Most likely comes from PotionEffectType (for effect) and PotionEffect (for amplifier)
					} else {
						player.sendMessage(ChatUtils.chatMessageError("Please enter a valid potion sub-command!"));
					}
				}
			} else {
				sender.sendMessage(ChatUtils.chatMessageError("You must be a player to use this command!"));
			}
		}
		return false;
	}

}
