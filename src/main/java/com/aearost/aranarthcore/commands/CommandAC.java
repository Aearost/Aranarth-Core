package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This is the master command for all commands related to AranarthCore.
 * All sub-commands have their own classes that are called from here.
 */
public class CommandAC implements CommandExecutor {

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: &7/ac <sub-command>"));
			return false;
		} else {
			boolean commandResult = false;
			// Applies only to Aearost (ops) or Console=
			if (sender instanceof Player player) {
				if (player.getName().equalsIgnoreCase("Aearost")) {
					commandResult = isSenderOp(sender, args);
				} else {
					commandResult = isValidCommand(sender, args);
				}
			} else {
				commandResult = isValidCommand(sender, args);
			}

			if (!commandResult) {
				sender.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid sub-command!"));
			}
			return commandResult;
		}
	}

	private boolean isSenderOp(CommandSender sender, String[] args) {
		boolean commandResult = false;
		if (args[0].equalsIgnoreCase("whereis")) {
			CommandWhereIs.onCommand(sender, args);
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("itemname")) {
			CommandItemName.onCommand(sender, args);
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("give")) {
			CommandGive.onCommand(sender, args);
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("dateset")) {
			CommandDate.onCommand(sender, args);
			commandResult = true;
		} else {
			commandResult = isValidCommand(sender, args);
		}
		return commandResult;
	}

	private boolean isValidCommand(CommandSender sender, String[] args) {
		boolean commandResult = false;
		if (args[0].equalsIgnoreCase("homepad")) {
			commandResult = CommandHomePad.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("nick")) {
			commandResult = CommandNickname.onCommand(sender,args);
		} else if (args[0].equalsIgnoreCase("ping")) {
			commandResult = CommandPing.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("arena")) {
			commandResult = CommandArena.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("survival")) {
			commandResult = CommandSurvival.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("smp")) {
			commandResult = CommandSMP.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("creative")) {
			commandResult = CommandCreative.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("blacklist")) {
			commandResult = CommandBlacklist.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("potions")) {
			commandResult = CommandPotions.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("shulker")) {
			commandResult = CommandShulker.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("randomizer")) {
			commandResult = CommandRandomizer.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("balance")) {
			commandResult = CommandBalance.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("date")) {
			commandResult = CommandDate.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("calendar")) {
			commandResult = CommandCalendar.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("aranarthium")) {
			commandResult = CommandAranarthium.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("trust")) {
			commandResult = CommandTrust.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("untrust")) {
			commandResult = CommandUntrust.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unlock")) {
			commandResult = CommandUnlock.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("lock")) {
			commandResult = CommandLock.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("ranks")) {
			commandResult = CommandRanks.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("rankup")) {
			commandResult = CommandRankup.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("rankset")) {
			commandResult = CommandRankSet.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("pronouns")) {
			commandResult = CommandPronouns.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("mute")) {
			commandResult = CommandMute.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unmute")) {
			commandResult = CommandUnmute.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("ban")) {
			commandResult = CommandBan.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unban")) {
			commandResult = CommandUnban.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("dominion")) {
			commandResult = CommandDominion.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("sethome")) {
			commandResult = CommandSethome.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("home")) {
			commandResult = CommandHome.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("delhome")) {
			commandResult = CommandDelhome.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("compress")) {
			commandResult = CommandCompress.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("tp")) {
			commandResult = CommandTp.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("tphere")) {
			commandResult = CommandTphere.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("back")) {
			commandResult = CommandBack.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("seen")) {
			commandResult = CommandSeen.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("hat")) {
			commandResult = CommandHat.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("toggle")) {
			commandResult = CommandToggle.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("rules")) {
			commandResult = CommandRules.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("warp")) {
			commandResult = CommandWarp.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("msg")) {
			commandResult = CommandMsg.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("pay")) {
			commandResult = CommandPay.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("baltop")) {
			commandResult = CommandBaltop.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("tables")) {
			commandResult = CommandTables.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("trash")) {
			commandResult = CommandTrash.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("spy")) {
			commandResult = CommandSpy.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("invsee")) {
			commandResult = CommandInvsee.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("tpaccept")) {
			commandResult = CommandTpaccept.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("tpdeny")) {
			commandResult = CommandTpdecline.onCommand(sender, args);
		}
		return commandResult;
	}
}
