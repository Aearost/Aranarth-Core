package com.aearost.aranarthcore.commands.council;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * This is the master command for all commands related to AranarthCore.
 * All sub-commands have their own classes that are called from here.
 */
public class CommandAC implements CommandExecutor {

	/**
	 * Sub-commands that are destructive or affect the whole server, limited to Council rank 3 and the console.
	 */
	private static final Set<String> OWNER_SUBCOMMANDS = Set.of("disband", "reload", "reloadlang", "dateset", "discordreload");

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
			boolean isCouncil = aranarthPlayer.getCouncilRank() > 0;
			boolean isArchitect = aranarthPlayer.getArchitectRank() >= 1;
			if (!isCouncil && !isArchitect) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return false;
			}
			if (!isCouncil && isArchitect) {
				if (args.length == 0 || (!args[0].equalsIgnoreCase("msg") && !args[0].equalsIgnoreCase("speed"))) {
					player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
					return false;
				}
			}
		}

		if (args.length == 0) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac <sub-command>")));
			return false;
		} else {
			boolean commandResult = false;
			commandResult = isCouncil(sender, args);

			if (!commandResult) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("ac.invalid_subcommand")));
			}
			return commandResult;
		}
	}

	private boolean isCouncil(CommandSender sender, String[] args) {
		boolean commandResult = false;
		String subCommand = args[0].toLowerCase();
		if (sender instanceof Player player) {
			// Pushing this server's player rows to the database can roll back the whole network
			if (subCommand.equals("migrate")) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
			// Destructive and server-wide sub-commands are limited to the highest Council rank
			if (OWNER_SUBCOMMANDS.contains(subCommand) && AranarthUtils.getPlayer(player.getUniqueId()).getCouncilRank() < 3) {
				player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
				return true;
			}
		}
		if (OWNER_SUBCOMMANDS.contains(subCommand) || subCommand.equals("migrate")) {
			Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + sender.getName() + " used /ac " + String.join(" ", args));
		}
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
		} else if (args[0].equalsIgnoreCase("spy")) {
			commandResult = CommandSpy.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("speed")) {
			commandResult = CommandSpeed.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("time")) {
			commandResult = CommandTime.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("rankset")) {
			commandResult = CommandRankSet.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("msg")) {
			commandResult = CommandCouncilMessage.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("dateset")) {
			commandResult = CommandDateSet.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("teleport") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("tpf") || args[0].equalsIgnoreCase("tpw")) {
			commandResult = CommandAdminTeleport.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("clearchat")) {
			commandResult = CommandClearChat.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("sudo")) {
			commandResult = CommandSudo.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("vanish")) {
			commandResult = CommandVanish.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("weather")) {
			commandResult = CommandWeather.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("resetquest")) {
			commandResult = CommandResetQuest.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("questnpc")) {
			commandResult = CommandQuestNpc.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("home")) {
			commandResult = CommandAdminHome.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("vpedit")) {
			commandResult = CommandVpEdit.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("invswap")) {
			commandResult = CommandInvSwap.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("discordreload")) {
			commandResult = CommandDiscordReload.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("reload")) {
			commandResult = CommandReload.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("reloadlang")) {
			LangManager.getInstance().load();
			sender.sendMessage(ChatUtils.chatMessage(Lang.get("ac.lang_reloaded")));
			commandResult = true;
		} else if (args[0].equalsIgnoreCase("migrate")) {
			commandResult = CommandMigrate.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("unscramble")) {
			commandResult = CommandUnscramble.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("og")) {
			commandResult = CommandOG.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("worldevent")) {
			commandResult = CommandWorldEvent.onCommand(sender, args);
		} else if (args[0].equalsIgnoreCase("disband")) {
			if (args.length < 2) {
				sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "ac disband <dominion name>")));
			} else {
				StringBuilder nameBuilder = new StringBuilder();
				for (int i = 1; i < args.length; i++) {
					if (i > 1) nameBuilder.append(" ");
					nameBuilder.append(args[i]);
				}
				String targetName = nameBuilder.toString();
				Dominion target = DominionUtils.getDominions().stream()
						.filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(targetName))
						.findFirst().orElse(null);
				if (target == null) {
					sender.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found", "name", targetName)));
				} else {
					DominionUtils.disbandDominion(target);
					commandResult = true;
				}
			}
		}
		return commandResult;
	}
}
