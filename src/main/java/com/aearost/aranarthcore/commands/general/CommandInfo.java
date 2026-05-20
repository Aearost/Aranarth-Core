package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
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
				sender.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/info <player>"));
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

			if (uuid != null) {
				sendInfo(uuid, sender);
				return true;
			} else {
				sender.sendMessage(ChatUtils.chatMessage("&cThis player could not be found"));
			}
		}
		return false;
	}

	/**
	 * Provides the formatted name of the world the player is in.
	 * @param name The unformatted name of the world the player is in.
	 * @return The formatted name of the world the player is in.
	 */
	private static String getWorldName(String name) {
		switch (name) {
			case "world", "world_nether", "world_the_end" -> {
				return "in &eSurvival";
			}
			case "resource", "resource_nether", "resource_the_end" -> {
				return "in the &eResource World";
			}
			case "smp", "smp_nether", "smp_the_end" -> {
				return "in the &eSMP";
			}
			case "arena" -> {
				return "in the &eArena";
			}
			case "creative" -> {
				return "in &eCreative";
			}
			case "shops" -> {
				return "in the &eShops World";
			}
			default -> {
				return "at &eSpawn";
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

		boolean isNicknameSameAsUsername = ChatUtils.stripColorFormatting(aranarthPlayer.getNickname()).equalsIgnoreCase(offlinePlayer.getName());
		String username = !isNicknameSameAsUsername ? " &e(" + offlinePlayer.getName() + "&e)" : "";
		sender.sendMessage(ChatUtils.translateToColor("&8      - - - &e"
				+ ChatUtils.providePrefixAndName(uuid) + username + " &8- - -"));

		Dominion dominion = DominionUtils.getPlayerDominion(uuid);
		String dominionName = "None";
		if (dominion != null) {
			dominionName = dominion.getName();
		}
		sender.sendMessage(ChatUtils.translateToColor("&6Dominion: &e" + dominionName));

		NumberFormat formatter = NumberFormat.getCurrencyInstance();
		sender.sendMessage(ChatUtils.translateToColor("&6Balance: &e" + formatter.format(aranarthPlayer.getBalance())));

		String elementString = "";
		if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(uuid)) {
			elementString = "&c火 &7気 &b水 &a土 &5The Current Avatar &a土 &b水 &7気 &c火";
		} else {
			OfflineBendingPlayer offlineBendingPlayer = BendingPlayer.getOfflineBendingPlayer(offlinePlayer.getName());
			if (offlineBendingPlayer == null || offlineBendingPlayer.getElements().isEmpty()) {
				elementString = "&eNone";
			} else {
				Element element = offlineBendingPlayer.getElements().getFirst();
				if (element == Element.WATER) {
					elementString = "&b水 Water 水";
				} else if (element == Element.EARTH) {
					elementString = "&a土 Earth 土";
				} else if (element == Element.FIRE) {
					elementString = "&c火 Fire 火";
				} else if (element == Element.CHI) {
					elementString = "&6ち Chi ち";
				} else {
					elementString = "&7気 Air 気";
				}
			}
		}
		sender.sendMessage(ChatUtils.translateToColor("&6Element: " + elementString));

		String pronouns = aranarthPlayer.getPronouns().name();
		pronouns = pronouns.substring(0, 1).toUpperCase() + pronouns.substring(1).toLowerCase();
		sender.sendMessage(ChatUtils.translateToColor("&6Pronouns: &e" + pronouns));

		List<String> toggling = new ArrayList<>();
		if (aranarthPlayer.isTogglingChat()) {
			toggling.add("&eChat Messages");
		}
		if (aranarthPlayer.isTogglingMessages()) {
			toggling.add("&eDirect Messages");
		}
		if (aranarthPlayer.isTogglingTp()) {
			toggling.add("&eTeleport Requests");
		}
		if (!aranarthPlayer.isUsingSpawnBoost()) {
			toggling.add("&eSpawn Boost");
		}
		if (aranarthPlayer.isTogglingChangeClaim()) {
			toggling.add("&eClaim Changes");
		}
		if (aranarthPlayer.isTogglingInventoryAssist()) {
			toggling.add("&eInventory Assist");
		}
		if (!aranarthPlayer.isAddingToShulker()) {
			toggling.add("&eShulker Assist");
		}
		if (aranarthPlayer.getBlacklistingMethod() == -1) {
			toggling.add("&eBlacklist");
		}
		if (!aranarthPlayer.isCompressingItems()) {
			toggling.add("&eCompressor");
		}
		if (!aranarthPlayer.isAutoLockingChests()) {
			toggling.add("&eChest Locks");
		}
		if (aranarthPlayer.hasBlueFireDisabled()) {
			toggling.add("&eBlue Fire");
		}
		if (aranarthPlayer.isDayMessageDisabled()) {
			toggling.add("&eDay Message");
		}
		if (aranarthPlayer.isWeatherMessageDisabled()) {
			toggling.add("&eWeather Message");
		}
		if (aranarthPlayer.isBulkSellShulkerEnabled()) {
			toggling.add("&eBulk Sell Shulker");
		}
		String toggledFeatures = "";
		if (toggling.isEmpty()) {
			toggledFeatures = "&eNone";
		} else {
			for (int i = 0; i < toggling.size(); i++) {
				toggledFeatures += toggling.get(i);
				if  (i < toggling.size() - 2) {
					toggledFeatures += ", ";
				} else if (i < toggling.size() - 1) {
					toggledFeatures += " and ";
				}
			}
		}
		sender.sendMessage(ChatUtils.translateToColor("&6Currently toggling: &e" + toggledFeatures));

		if (sender instanceof Player player) {
			if (player.hasPermission("aranarth.seen")) {
				if (offlinePlayer.isOnline()) {
					Player onlinePlayer = offlinePlayer.getPlayer();
					boolean isAfk = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getAfkLocation() != null &&
							AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount();
					if (isAfk) {
						sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &aCurrently AFK " + getWorldName(onlinePlayer.getWorld().getName())));
					} else {
						sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &aCurrently online " + getWorldName(onlinePlayer.getWorld().getName())));
					}
				} else {
					AranarthUtils.getPlayerTimezone(player, zoneId -> {
						String result = CommandSeen.calculateDisplayDate(
								offlinePlayer,
								aranarthPlayer,
								zoneId,
								sender
						);
						sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &e" + result));
					});
				}
			}
		} else {
			if (offlinePlayer.isOnline()) {
				Player onlinePlayer = offlinePlayer.getPlayer();
				boolean isAfk = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getAfkLocation() != null
						&& AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount();
				if (isAfk) {
					sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &aCurrently AFK " + getWorldName(onlinePlayer.getWorld().getName())));
				} else {
					sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &aCurrently online " + getWorldName(onlinePlayer.getWorld().getName())));
				}
			} else {
				String result = CommandSeen.calculateDisplayDate(offlinePlayer, aranarthPlayer, ZoneId.systemDefault(), sender);
				sender.sendMessage(ChatUtils.translateToColor("&6Last Online: &e" + result));
			}
		}
	}

}