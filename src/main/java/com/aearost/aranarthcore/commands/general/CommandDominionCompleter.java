package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handles the auto complete functionality while using the /dominion command.
 */
public class CommandDominionCompleter implements TabCompleter {

	private static final List<String> DOMINION_OPTIONS = List.of(
		"accept", "ally", "autoclaim", "balance", "buychunks", "claim", "conquer", "create",
		"deposit", "disband", "enemy", "food", "guide", "home", "info", "invite",
		"leave", "list", "map", "msg", "neutral", "permissions", "perms", "rank",
		"rebel", "remove", "rename", "resources", "retreat", "sethome", "setleader",
		"surrender", "truce", "unclaim", "who", "withdraw"
	);

	private static final List<String> DOMINION_CHAT_TYPES = List.of("dominion", "ally", "truce", "allytruce");

	/**
	 * @param sender The user that entered the command.
	 * @param command The command itself.
	 * @param alias The alias of the command.
	 * @param args The arguments of the command.
	 * @return Confirmation of whether the command was a success or not.
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return filter(DOMINION_OPTIONS, args[0]);
		}
		return switch (args[0].toLowerCase()) {
			case "invite", "who" -> filterPlayers(args[1]);
			case "home" -> {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
					if (aranarthPlayer != null && aranarthPlayer.isInAdminMode()) {
						yield DominionUtils.getDominions().stream()
							.map(d -> ChatUtils.stripColorFormatting(d.getName()))
							.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
							.collect(Collectors.toList());
					}
					Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
					if (dominion != null) {
						Stream<String> alliedStream = dominion.getAllied().stream()
							.map(DominionUtils::getPlayerDominion)
							.filter(d -> d != null && dominion.isAllied(d) && d.getDominionPermissions().hasPermission(DominionRank.ALLIED, DominionPermission.HOME))
							.map(d -> ChatUtils.stripColorFormatting(d.getName()));
						Stream<String> conqueredStream = dominion.getConquered().stream()
							.map(DominionUtils::getPlayerDominion)
							.filter(d -> d != null)
							.map(d -> ChatUtils.stripColorFormatting(d.getName()));
						yield Stream.concat(alliedStream, conqueredStream)
							.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
							.distinct()
							.collect(Collectors.toList());
					}
				}
				yield List.of();
			}
			case "remove", "setleader" -> {
				if (sender instanceof Player player) {
					Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
					if (dominion != null) {
						yield dominion.getMembers().stream()
							.map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
							.filter(name -> name != null && (args[1].isEmpty() || name.toLowerCase().startsWith(args[1].toLowerCase())))
							.collect(Collectors.toList());
					}
				}
				yield List.of();
			}
			case "rank" -> {
				if (args.length == 2) {
					if (sender instanceof Player player) {
						Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
						if (dominion != null) {
							yield dominion.getMembers().stream()
								.map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
								.filter(name -> name != null && (args[1].isEmpty() || name.toLowerCase().startsWith(args[1].toLowerCase())))
								.collect(Collectors.toList());
						}
					}
					yield List.of();
				}
				if (args.length == 3) {
					yield filter(List.of("Newcomer", "Citizen", "Lieutenant"), args[2]);
				}
				yield List.of();
			}
			case "msg" -> filter(DOMINION_CHAT_TYPES, args[1]);
			case "info", "ally", "truce", "enemy", "neutral", "conquer", "surrender", "rebel", "retreat" -> {
				String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				yield DominionUtils.getDominions().stream()
					.map(d -> ChatUtils.stripColorFormatting(d.getName()))
					.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
					.collect(Collectors.toList());
			}
			case "buychunks" -> args[1].isEmpty() ? List.of("<amount>") : List.of();
			case "create" -> args[1].isEmpty() ? List.of("name") : List.of();
			default -> List.of();
		};
	}

	private static List<String> filter(List<String> options, String input) {
		if (input.isEmpty()) {
			return new ArrayList<>(options);
		}
		return options.stream()
			.filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}

	private static List<String> filterPlayers(String input) {
		return Bukkit.getOnlinePlayers().stream()
			.map(Player::getName)
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
