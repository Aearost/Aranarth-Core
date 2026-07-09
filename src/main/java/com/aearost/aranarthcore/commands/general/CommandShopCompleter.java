package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the auto complete functionality while using the /shop command.
 */
public class CommandShopCompleter implements TabCompleter {

	private static final List<String> SUBCOMMANDS = List.of("create", "home", "sethome", "delete", "rename", "biome", "invite", "remove", "accept", "decline", "leave");

	private static final List<String> BIOME_NAMES = Registry.BIOME.stream()
			.map(b -> b.getKey().getKey())
			.collect(Collectors.toList());

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (!(sender instanceof Player player)) {
			return List.of();
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		if (args.length == 1) {
			return filter(SUBCOMMANDS, args[0]);
		}

		if (args.length == 2) {
			// /shop delete <username> — admin mode only
			if (args[0].equalsIgnoreCase("delete") && aranarthPlayer.isInAdminMode()) {
				return filterPlayers(args[1]);
			}
			// /shop biome <biome> — saint rank 1+
			if (args[0].equalsIgnoreCase("biome") && (aranarthPlayer.getSaintRank() >= 1 || aranarthPlayer.isInAdminMode())) {
				return filter(BIOME_NAMES, args[1]);
			}
			// /shop invite <username> - shop owner only
			if (args[0].equalsIgnoreCase("invite") && AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
				return filterPlayers(args[1]);
			}
			// /shop remove <username> - shop owner only, show current collaborators
			if (args[0].equalsIgnoreCase("remove") && AranarthUtils.getShopLocations().containsKey(player.getUniqueId())) {
				return filterCollaborators(player.getUniqueId(), args[1]);
			}
		}

		return List.of();
	}

	private static List<String> filter(List<String> options, String input) {
		return options.stream()
			.filter(s -> input.isEmpty() || s.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}

	private static List<String> filterPlayers(String input) {
		return Bukkit.getOnlinePlayers().stream()
			.map(Player::getName)
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}

	private static List<String> filterCollaborators(UUID ownerUuid, String input) {
		return AranarthUtils.getCollaboratorsForOwner(ownerUuid).stream()
			.map(uuid -> {
				OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
				return op.getName() != null ? op.getName() : uuid.toString();
			})
			.filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input.toLowerCase()))
			.collect(Collectors.toList());
	}
}
