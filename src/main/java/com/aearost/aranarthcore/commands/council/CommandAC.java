package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
		if (sender instanceof Player player) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			if (aranarthPlayer.getCouncilRank() == 0) {
				player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
				return false;
			}
		}

		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage("&cIncorrect syntax: &e/ac <sub-command>"));
			return false;
		} else {
			boolean commandResult = false;
			commandResult = isCouncil(sender, args);

			if (!commandResult) {
				sender.sendMessage(ChatUtils.chatMessage("&cPlease enter a valid sub-command!"));
			}
			return commandResult;
		}
	}

	private boolean isCouncil(CommandSender sender, String[] args) {
		boolean commandResult = false;
		if (args[0].equalsIgnoreCase("whereis")) {
			CommandWhereIs.onCommand(sender, args);
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("give")) {
			CommandGive.onCommand(sender, args);
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("mute")) {
			commandResult = CommandMute.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unmute")) {
			commandResult = CommandUnmute.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("ban")) {
			commandResult = CommandBan.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unban")) {
			commandResult = CommandUnban.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("invsee")) {
			commandResult = CommandInvsee.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("warn")) {
			commandResult = CommandWarn.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("punishments")) {
			commandResult = CommandPunishments.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("broadcast")) {
			commandResult = CommandBroadcast.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("perks")) {
			commandResult = CommandPerks.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("admin")) {
			commandResult = CommandAdmin.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("speed")) {
			commandResult = CommandSpeed.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("time")) {
			commandResult = CommandTime.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("rankset")) {
			commandResult = CommandRankSet.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("msg")) {
			commandResult = CommandCouncilMessage.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("skull")) {
			commandResult = CommandSkull.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("dateset")) {
			commandResult = CommandDateSet.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("tpf")) {
			commandResult = CommandAdminTeleport.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("clearchat")) {
			commandResult = CommandClearChat.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("sudo")) {
			commandResult = CommandSudo.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("vanish")) {
			commandResult = CommandVanish.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("weather")) {
			commandResult = CommandWeather.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("questnpc")) {
			commandResult = CommandQuestNpc.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("home")) {
			commandResult = CommandAdminHome.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("vpedit")) {
			commandResult = CommandVpEdit.onCommand(sender, args);
		}
		return commandResult;
	}
}
