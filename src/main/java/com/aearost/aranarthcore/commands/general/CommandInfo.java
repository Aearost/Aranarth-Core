package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.OfflineBendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Displays information regarding the input player.
 */
public class CommandInfo implements CommandExecutor {

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
			if (sender instanceof Player player) {
				sendInfo(player.getUniqueId(), sender);
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.invalid_syntax", "usage", "info <player>")));
				return false;
			}
		} else {
			UUID uuid = null;
			// Cycles through usernames first
			for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
				if (player.getName().equalsIgnoreCase(args[0])) {
					uuid = player.getUniqueId();
					break;
				}
			}

			// Cycles through nicknames second
			if (uuid == null) {
				for (AranarthPlayer aranarthPlayer : AranarthUtils.getAranarthPlayers().values()) {
					if (aranarthPlayer.getNickname() == null || aranarthPlayer.getNickname().isEmpty()) {
						continue;
					}

					if (ChatUtils.stripColorFormatting(aranarthPlayer.getNickname()).equalsIgnoreCase(args[0])) {
						uuid = AranarthUtils.getUuidOfAranarthPlayer(aranarthPlayer);
						break;
					}
				}
			}

			// Cycles through remote-server players third (players who have never joined this server)
			if (uuid == null && NetworkManager.isActive()) {
				for (Map.Entry<UUID, NetworkPlayer> entry : NetworkManager.getInstance().getRemoteRoster().entrySet()) {
					NetworkPlayer np = entry.getValue();
					if (np.getUsername().equalsIgnoreCase(args[0]) ||
							ChatUtils.stripColorFormatting(np.getNickname()).equalsIgnoreCase(args[0])) {
						uuid = entry.getKey();
						break;
					}
				}
			}

			if (uuid != null) {
				// Ensure AranarthPlayer is loaded; remote players may not be in local memory
				if (AranarthUtils.getPlayer(uuid) == null && DatabaseManager.isActive()) {
					PersistenceUtils.reloadPlayerFromDatabase(uuid);
				}
				sendInfo(uuid, sender);
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.not_found", "name", args[0])));
			}
		}
		return false;
	}

	/**
	 * Returns the display string for a player on a remote server (e.g. "in the &eSMP").
	 */
	private static String getRemoteServerDisplay(CommandSender sender, String server) {
		return switch (server.toLowerCase()) {
			case "smp" -> Lang.getFor(sender, "info.in_smp");
			case "survival" -> Lang.getFor(sender, "info.in_survival");
			default -> Lang.getFor(sender, "info.on_server", "server", server);
		};
	}

	/**
	 * Provides the formatted name of the world the player is in.
	 * @param name The unformatted name of the world the player is in.
	 * @return The formatted name of the world the player is in.
	 */
	private static String getWorldName(CommandSender sender, String name) {
		if (AranarthUtils.isSmpWorld(name)) {
			return Lang.getFor(sender, "info.in_smp");
		}
		switch (name) {
			case "world", "world_nether", "world_the_end" -> {
				return Lang.getFor(sender, "info.in_survival");
			}
			case "resource", "resource_nether", "resource_the_end" -> {
				return Lang.getFor(sender, "info.in_resource");
			}
			case "arena" -> {
				return Lang.getFor(sender, "info.in_arena");
			}
			case "creative" -> {
				return Lang.getFor(sender, AranarthCore.isSmpServer() ? "info.in_smp_creative" : "info.in_creative");
			}
			case "shops" -> {
				return Lang.getFor(sender, "info.in_shops");
			}
			default -> {
				return Lang.getFor(sender, "info.at_spawn");
			}
		}
	}

	/**
	 * Lists the information to be displayed in the command.
	 * @param uuid The UUID of the player being displayed.
	 * @param sender The user that entered the command.
	 */
	private static void sendInfo(UUID uuid, CommandSender sender) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

		if (aranarthPlayer == null) {
			sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "player.data_load_failed")));
			return;
		}

		// Players who only joined the other server may not be in this server's user cache
		String offlineName = offlinePlayer.getName();
		if (offlineName == null) {
			offlineName = aranarthPlayer.getUsername();
			if (offlineName == null || offlineName.isEmpty()) {
				NetworkPlayer np = NetworkManager.isActive() ? NetworkManager.getInstance().getRemotePlayer(uuid) : null;
				offlineName = np != null ? np.getUsername() : Lang.getFor(sender, "info.unknown");
			}
		}
		final String resolvedName = offlineName;

		boolean isNicknameSameAsUsername = ChatUtils.stripColorFormatting(aranarthPlayer.getNickname()).equalsIgnoreCase(resolvedName);
		String username = !isNicknameSameAsUsername ? " &e(" + resolvedName + "&e)" : "";
		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &e"
				+ ChatUtils.providePrefixAndName(uuid, sender) + username + " &8- - -"));

		String firstJoinDate = aranarthPlayer.getFirstJoinDate();
		boolean isOG = AranarthUtils.isOriginalPlayer(uuid);
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.first_joined", "date", isOG || firstJoinDate.isEmpty() ? Lang.getFor(sender, "info.unknown") : firstJoinDate)));

		Dominion dominion = DominionUtils.getPlayerDominion(uuid);
		String dominionName = Lang.getFor(sender, "general.none");
		if (dominion != null) {
			dominionName = dominion.getName();
		}
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.dominion", "name", dominionName)));

		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.balance", "balance", formatter.format(aranarthPlayer.getBalance()))));

		String elementString = "";
		if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(uuid)) {
			elementString = Lang.getFor(sender, "info.element_avatar");
		} else {
			OfflineBendingPlayer offlineBendingPlayer = BendingPlayer.getOfflineBendingPlayer(resolvedName);
			if (offlineBendingPlayer == null || offlineBendingPlayer.getElements().isEmpty()) {
				elementString = Lang.getFor(sender, "info.element_none");
			} else {
				Element element = offlineBendingPlayer.getElements().getFirst();
				if (element == Element.WATER) {
					elementString = Lang.getFor(sender, "info.element_water");
				} else if (element == Element.EARTH) {
					elementString = Lang.getFor(sender, "info.element_earth");
				} else if (element == Element.FIRE) {
					elementString = Lang.getFor(sender, "info.element_fire");
				} else if (element == Element.CHI) {
					elementString = Lang.getFor(sender, "info.element_chi");
				} else {
					elementString = Lang.getFor(sender, "info.element_air");
				}
			}
		}
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.element", "element", elementString)));

		String pronouns = aranarthPlayer.getPronouns().name();
		pronouns = pronouns.substring(0, 1).toUpperCase() + pronouns.substring(1).toLowerCase();
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.pronouns", "pronouns", pronouns)));

		List<String> toggling = new ArrayList<>();
		if (aranarthPlayer.isTogglingChat()) {
			toggling.add(Lang.getFor(sender, "info.toggle_chat"));
		}
		if (aranarthPlayer.isTogglingMessages()) {
			toggling.add(Lang.getFor(sender, "info.toggle_messages"));
		}
		if (aranarthPlayer.isTogglingTp()) {
			toggling.add(Lang.getFor(sender, "info.toggle_tp"));
		}
		if (!aranarthPlayer.isUsingSpawnBoost()) {
			toggling.add(Lang.getFor(sender, "info.toggle_spawn_boost"));
		}
		if (aranarthPlayer.isTogglingChangeClaim()) {
			toggling.add(Lang.getFor(sender, "info.toggle_claim_changes"));
		}
		if (aranarthPlayer.isTogglingInventoryAssist()) {
			toggling.add(Lang.getFor(sender, "info.toggle_inventory_assist"));
		}
		if (!aranarthPlayer.isAddingToShulker()) {
			toggling.add(Lang.getFor(sender, "info.toggle_shulker_assist"));
		}
		if (aranarthPlayer.getBlacklistingMethod() == -1) {
			toggling.add(Lang.getFor(sender, "info.toggle_blacklist"));
		}
		if (!aranarthPlayer.isCompressingItems()) {
			toggling.add(Lang.getFor(sender, "info.toggle_compressor"));
		}
		if (!aranarthPlayer.isAutoLockingChests()) {
			toggling.add(Lang.getFor(sender, "info.toggle_chest_locks"));
		}
		if (aranarthPlayer.getFireType() != FireType.DEFAULT) {
			toggling.add(Lang.getFor(sender, "info.toggle_fire_type", "type", aranarthPlayer.getFireType().getDisplayName()));
		}
		if (aranarthPlayer.isDayMessageDisabled()) {
			toggling.add(Lang.getFor(sender, "info.toggle_day_message"));
		}
		if (aranarthPlayer.isWeatherMessageDisabled()) {
			toggling.add(Lang.getFor(sender, "info.toggle_weather_message"));
		}
		if (aranarthPlayer.isBulkSellShulkerEnabled()) {
			toggling.add(Lang.getFor(sender, "info.toggle_bulk_sell_shulker"));
		}
		String toggledFeatures = "";
		if (toggling.isEmpty()) {
			toggledFeatures = Lang.getFor(sender, "info.toggle_none");
		} else {
			for (int i = 0; i < toggling.size(); i++) {
				toggledFeatures += toggling.get(i);
				if  (i < toggling.size() - 2) {
					toggledFeatures += ", ";
				} else if (i < toggling.size() - 1) {
					toggledFeatures += Lang.getFor(sender, "general.list_and");
				}
			}
		}
		sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.toggling", "list", toggledFeatures)));

		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.seen")) {
				// Vanished players appear offline
				boolean isVanished = AranarthUtils.isVanishedFrom(sender, uuid);
				if (offlinePlayer.isOnline() && !isVanished) {
					Player onlinePlayer = offlinePlayer.getPlayer();
					boolean isAfk = AranarthUtils.isAfk(AranarthUtils.getPlayer(onlinePlayer.getUniqueId()));
					if (isAfk) {
						sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_afk", "location", getWorldName(sender, onlinePlayer.getWorld().getName()))));
					} else {
						sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_now", "location", getWorldName(sender, onlinePlayer.getWorld().getName()))));
					}
				} else if (NetworkManager.isActive()) {
					NetworkPlayer networkPlayer = NetworkManager.getInstance().getRemotePlayer(uuid);
					if (networkPlayer != null && !isVanished) {
						sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_now", "location", getRemoteServerDisplay(sender, networkPlayer.getServer()))));
					} else {
						AranarthUtils.getPlayerTimezone(player, zoneId -> {
							String result = CommandSeen.calculateDisplayDate(
									offlinePlayer,
									aranarthPlayer,
									zoneId,
									sender
							);
							sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online", "date", result)));
						});
					}
				} else {
					AranarthUtils.getPlayerTimezone(player, zoneId -> {
						String result = CommandSeen.calculateDisplayDate(
								offlinePlayer,
								aranarthPlayer,
								zoneId,
								sender
						);
						sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online", "date", result)));
					});
				}
			}
		} else {
			if (offlinePlayer.isOnline()) {
				Player onlinePlayer = offlinePlayer.getPlayer();
				boolean isAfk = AranarthUtils.isAfk(AranarthUtils.getPlayer(onlinePlayer.getUniqueId()));
				if (isAfk) {
					sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_afk", "location", getWorldName(sender, onlinePlayer.getWorld().getName()))));
				} else {
					sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_now", "location", getWorldName(sender, onlinePlayer.getWorld().getName()))));
				}
			} else if (NetworkManager.isActive()) {
				NetworkPlayer networkPlayer = NetworkManager.getInstance().getRemotePlayer(uuid);
				if (networkPlayer != null) {
					sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online_now", "location", getRemoteServerDisplay(sender, networkPlayer.getServer()))));
				} else {
					String result = CommandSeen.calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
					sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online", "date", result)));
				}
			} else {
				String result = CommandSeen.calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
				sender.sendMessage(ChatUtils.translateToColor(Lang.getFor(sender, "info.last_online", "date", result)));
			}
		}
	}

}