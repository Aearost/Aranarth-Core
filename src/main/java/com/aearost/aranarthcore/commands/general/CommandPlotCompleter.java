package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CreativePlot;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.PlotUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /plot command.
 */
public class CommandPlotCompleter implements TabCompleter {

	private static final List<String> SUBCOMMANDS = List.of("claim", "unclaim", "reset", "merge", "rename", "add", "remove", "home", "tp", "list", "info", "spawn");

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}

		if (args.length == 1) {
			return filter(SUBCOMMANDS, args[0]);
		}

		String subcommand = args[0].toLowerCase();
		if (args.length == 2) {
			switch (subcommand) {
				case "claim" -> {
					return args[1].isEmpty() ? List.of("name") : List.of();
				}
				case "home", "merge", "rename" -> {
					return filter(getPlotNames(player.getUniqueId()), args[1]);
				}
				case "add", "tp" -> {
					return AranarthUtils.getNetworkPlayerNames(args[1]);
				}
				case "unclaim", "reset" -> {
					// Staff in admin mode can also target another player's plot by name
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					List<String> options = new ArrayList<>();
					if ("confirm".startsWith(args[1].toLowerCase())) {
						options.add("confirm");
					}
					if (aranarthPlayer.isInAdminMode()) {
						options.addAll(AranarthUtils.getNetworkPlayerNames(args[1]));
					}
					return options;
				}
				case "remove" -> {
					return filter(getStandingPlotMembers(player), args[1]);
				}
				case "spawn" -> {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					return aranarthPlayer.getCouncilRank() == 3 ? filter(List.of("claim", "unclaim"), args[1]) : List.of();
				}
				default -> {
					return List.of();
				}
			}
		}

		if (args.length == 3 && subcommand.equals("merge")) {
			return filter(getPlotNames(player.getUniqueId()), args[2]);
		}
		if (args.length == 3 && (subcommand.equals("tp") || subcommand.equals("unclaim") || subcommand.equals("reset"))) {
			UUID target = AranarthUtils.getUUIDFromUsername(args[1]);
			return target == null ? List.of() : filter(getPlotNames(target), args[2]);
		}
		if (args.length == 3 && subcommand.equals("rename")) {
			return args[2].isEmpty() ? List.of("name") : List.of();
		}
		if (args.length == 4 && (subcommand.equals("unclaim") || subcommand.equals("reset"))) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			return aranarthPlayer.isInAdminMode() ? filter(List.of("confirm"), args[3]) : List.of();
		}
		if (args.length == 4 && subcommand.equals("merge")) {
			return args[3].isEmpty() ? List.of("name") : List.of();
		}
		return List.of();
	}

	private static List<String> getPlotNames(UUID uuid) {
		List<String> names = new ArrayList<>();
		for (CreativePlot plot : PlotUtils.getOwnedPlotGroups(uuid)) {
			names.add(PlotUtils.stripName(plot.getName()));
		}
		return names;
	}

	private static List<String> getStandingPlotMembers(Player player) {
		int[] coordinates = PlotUtils.getPlotCoordinates(player.getLocation());
		if (coordinates == null) {
			return List.of();
		}
		CreativePlot plot = PlotUtils.getPlot(coordinates[0], coordinates[1]);
		if (plot == null) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (UUID member : plot.getMembers()) {
			String name = Bukkit.getOfflinePlayer(member).getName();
			if (name != null) {
				names.add(name);
			}
		}
		return names;
	}

	private static List<String> filter(List<String> options, String input) {
		return options.stream()
			.filter(s -> input.isEmpty() || s.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
