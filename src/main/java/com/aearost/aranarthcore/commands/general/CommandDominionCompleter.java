package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
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
		"leave", "list", "map", "msg", "neutral", "outpost", "plot", "rank", "setrank",
		"rebel", "remove", "rename", "resources", "retreat", "sethome", "setleader",
		"surrender", "truce", "unclaim", "who", "withdraw"
	);

	private static final List<String> OUTPOST_OPTIONS = List.of("create", "disband", "home", "rename", "sethome", "buychunks");

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
					Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
					boolean isAdmin = aranarthPlayer != null && aranarthPlayer.isInAdminMode();

					List<String> results = new ArrayList<>();

					// Try every possible split point as outpost query
					if (args.length >= 3) {
						for (int split = 2; split < args.length; split++) {
							String dominionPart = String.join(" ", Arrays.copyOfRange(args, 1, split));
							Dominion target = DominionUtils.getDominions().stream()
								.filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(dominionPart))
								.findFirst().orElse(null);
							if (target == null) continue;

							boolean canAccess = isAdmin;
							if (!canAccess && playerDominion != null) {
								DominionRank relationRank = DominionUtils.getRelationKey(playerDominion, target);
								canAccess = target.getDominionPermissions().hasPermission(relationRank, DominionPermission.OUTPOST_HOME)
										|| playerDominion.getConquered().contains(target.getLeader());
							}
							if (!canAccess) continue;

							String outpostQuery = String.join(" ", Arrays.copyOfRange(args, split, args.length)).trim();
							boolean lastArgEmpty = args[args.length - 1].isEmpty();
							OutpostUtils.getDominionOutposts(target.getId()).stream()
								.map(o -> ChatUtils.stripColorFormatting(o.getName()))
								.filter(name -> {
									if (!outpostQuery.isEmpty() && !name.toLowerCase().startsWith(outpostQuery.toLowerCase())) return false;
									if (lastArgEmpty && name.equalsIgnoreCase(outpostQuery)) return false;
									return true;
								})
								.forEach(results::add);
						}
					}

					// Always also suggest dominion names for the full typed query
					String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
					Stream<String> dominionNames;
					if (isAdmin) {
						dominionNames = DominionUtils.getDominions().stream()
							.map(d -> ChatUtils.stripColorFormatting(d.getName()));
					} else if (playerDominion != null) {
						Stream<String> alliedStream = DominionUtils.getDominions().stream()
							.filter(d -> {
								DominionRank rel = DominionUtils.getRelationKey(playerDominion, d);
								return d.getDominionPermissions().hasPermission(rel, DominionPermission.HOME)
									|| d.getDominionPermissions().hasPermission(rel, DominionPermission.OUTPOST_HOME);
							})
							.map(d -> ChatUtils.stripColorFormatting(d.getName()));
						Stream<String> conqueredStream = playerDominion.getConquered().stream()
							.map(DominionUtils::getPlayerDominion)
							.filter(d -> d != null)
							.map(d -> ChatUtils.stripColorFormatting(d.getName()));
						dominionNames = Stream.concat(alliedStream, conqueredStream).distinct();
					} else {
						dominionNames = Stream.empty();
					}
					dominionNames
						.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
						.forEach(results::add);

					yield results.stream().distinct().collect(Collectors.toList());
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
			case "plot" -> {
				if (args.length == 2) {
					yield filter(List.of("add", "claim", "create", "remove", "rename"), args[1]);
				}
				if (args.length == 3) {
					String plotSub = args[1].toLowerCase();
					if (sender instanceof Player player) {
						Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
						if (dominion != null) {
							yield switch (plotSub) {
								case "add", "remove" -> dominion.getMembers().stream()
										.map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
										.filter(name -> name != null && (args[2].isEmpty() || name.toLowerCase().startsWith(args[2].toLowerCase())))
										.collect(Collectors.toList());
								case "claim", "rename" -> dominion.getPlotMembers().keySet().stream()
										.filter(name -> args[2].isEmpty() || name.toLowerCase().startsWith(args[2].toLowerCase()))
										.collect(Collectors.toList());
								case "create" -> args[2].isEmpty() ? List.of("name") : List.of();
								default -> List.of();
							};
						}
					}
				}
				yield List.of();
			}
			case "rank" -> {
				if (args.length == 2 && sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer != null && aranarthPlayer.getCouncilRank() == 3) {
						yield filter(List.of("scan"), args[1]);
					}
				}
				yield List.of();
			}
			case "setrank" -> {
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
			case "disband" -> {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					if (aranarthPlayer != null && aranarthPlayer.isInAdminMode() && aranarthPlayer.getCouncilRank() == 3) {
						String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
						yield DominionUtils.getDominions().stream()
							.map(d -> ChatUtils.stripColorFormatting(d.getName()))
							.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
							.collect(Collectors.toList());
					}
				}
				yield List.of();
			}
			case "info", "ally", "truce", "enemy", "neutral", "conquer", "surrender", "rebel", "retreat" -> {
				String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				yield DominionUtils.getDominions().stream()
					.map(d -> ChatUtils.stripColorFormatting(d.getName()))
					.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
					.collect(Collectors.toList());
			}
			case "buychunks" -> args[1].isEmpty() ? List.of("amount") : List.of();
			case "create" -> args[1].isEmpty() ? List.of("name") : List.of();
			case "outpost" -> {
				if (args.length == 2) {
					yield filter(OUTPOST_OPTIONS, args[1]);
				}
				if (args.length == 3) {
					String sub = args[1].toLowerCase();
					yield switch (sub) {
						case "create", "rename" -> args[2].isEmpty() ? List.of("name") : List.of();
						case "buychunks" -> args[2].isEmpty() ? List.of("amount") : List.of();
						case "home", "disband" -> {
							if (sender instanceof Player player) {
								Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
								if (dominion != null) {
									String query = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
									yield OutpostUtils.getDominionOutposts(dominion.getId()).stream()
											.map(o -> ChatUtils.stripColorFormatting(o.getName()))
											.filter(name -> query.isEmpty() || name.toLowerCase().startsWith(query.toLowerCase()))
											.collect(Collectors.toList());
								}
							}
							yield List.of();
						}
						default -> List.of();
					};
				}
				yield List.of();
			}
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
