package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.objects.Punishment;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Provides a variety of utility methods for everything related to Discord integration.
 */
public class DiscordUtils {

	private static final TextChannel punishmentHistoryChannelId = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("punishment");
	private static final TextChannel roleChangesChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("roles");
	private static final TextChannel serverChatChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");

	/**
	 * Provides the Discord Guild.
	 * @return The Discord Guild.
	 */
	public static Guild getGuild() {
		JDA jda = DiscordSRV.getPlugin().getJda();
		return jda.getGuildById("664319732446396416");
	}

	/**
	 * Updates the player's in-game rank accordingly in Discord's roles.
	 * @param player The player whose rank is changing.
	 * @param newRankNum The player's new rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e. due to rankup or manual command change.
	 */
	public static void updateRank(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();
		Role roleToAdd = switch (newRankNum) {
			case 1 -> guild.getRoleById("1436839935964352543"); // Esquire
			case 2 -> guild.getRoleById("1436840295768784928"); // Knight
			case 3 -> guild.getRoleById("1436840332703563968"); // Baron
			case 4 -> guild.getRoleById("1436840423334084668"); // Count
			case 5 -> guild.getRoleById("1436840444771438752"); // Duke
			case 6 -> guild.getRoleById("1436840565982498968"); // Prince
			case 7 -> guild.getRoleById("1436840642331410634"); // King
			case 8 -> guild.getRoleById("1436840682881945630"); // Emperor
            default -> guild.getRoleById("1436839882268872744"); // Peasant
        };

		if (playerDiscordId != null) {
			List<Role> playerDiscordRoles = guild.getMemberById(playerDiscordId).getRoles();
			for (Role role : playerDiscordRoles) {
				// Any of the rank-based roles
				if (role.getId().equals("1436839935964352543") || role.getId().equals("1436840295768784928")
						|| role.getId().equals("1436840332703563968") || role.getId().equals("1436840423334084668")
						|| role.getId().equals("1436840444771438752") || role.getId().equals("1436840565982498968")
						|| role.getId().equals("1436840642331410634") || role.getId().equals("1436840682881945630")
						|| role.getId().equals("1436839882268872744")) {
					guild.removeRoleFromMember(playerDiscordId, role).queue();
				}
			}
			guild.addRoleToMember(playerDiscordId, roleToAdd).queue();
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			String aOrAn = "a";
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			String rankName = AranarthUtils.getRank(aranarthPlayer).substring(5);
			String[] rankNameNoBrackets = rankName.split("]");
			rankName = rankNameNoBrackets[0].substring(0, rankNameNoBrackets[0].length() - 2);
			if (rankName.equals("Esquire") || rankName.equals("Emperor") || rankName.equals("Empress")) {
				aOrAn = "an";
			}

			String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor(player.getName() + " has become " + aOrAn + " " + rankName + "!", null, url)
					.setColor(Color.CYAN);

			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
		}
    }

	/**
	 * Updates the player's Saint rank accordingly in Discord's roles.
	 * @param player The player whose Saint rank is changing.
	 * @param newRankNum The player's new Saint rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e. due to rankup or manual command change.
	 */
	public static void updateSaint(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();
		boolean isSaint = false;

		// If they are a Saint
		if (newRankNum > 0) {
			isSaint = true;
			if (playerDiscordId != null) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436839449626542161")).queue();
			}
		} else {
			if (playerDiscordId != null) {
				guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436839449626542161")).queue();
			}
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			if (isSaint) {
				String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
				String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
				EmbedBuilder embed = new EmbedBuilder()
						.setAuthor(player.getName() + " has donated and become a Saint!", null, url)
						.setColor(Color.MAGENTA);
				serverChatChannel.sendMessageEmbeds(embed.build()).queue();
				roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
			}
		}
	}

	/**
	 * Updates the player's Architect rank accordingly in Discord's roles.
	 * @param player The player whose Architect rank is changing.
	 * @param newRankNum The player's new Architect rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e due to rankup or manual command change.
	 */
	public static void updateArchitect(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();
		boolean isArchitect = false;

		// If they are an Architect
		if (newRankNum > 0) {
			isArchitect = true;
			if (playerDiscordId != null) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436842029274632293")).queue();
			}
		} else {
			if (playerDiscordId != null) {
				guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436842029274632293")).queue();
			}
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			if (isArchitect) {
				String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
				String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
				EmbedBuilder embed = new EmbedBuilder()
						.setAuthor(player.getName() + " has become an Architect!", null, url)
						.setColor(Color.YELLOW);
				serverChatChannel.sendMessageEmbeds(embed.build()).queue();
				roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
			}
		}
	}

	/**
	 * Updates the player's Council rank accordingly in Discord's roles.
	 * @param player The player whose Council rank is changing.
	 * @param newRankNum The player's new Council rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e due to rankup or manual command change.
	 */
	public static void updateCouncil(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();
		boolean isHelper = false;
		boolean isModerator = false;
		boolean isAdmin = false;

		if (playerDiscordId != null) {
			// Remove all council ranks
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436877816796020836")).queue(); // The Council role
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436841788634697922")).queue(); // The Helper role
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436842179594031358")).queue(); // The Moderator role
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436842548412027011")).queue(); // The Admin role

			// If they are a Council member
			if (newRankNum == 1) {
				isHelper = true;
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436877816796020836")).queue();
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436841788634697922")).queue();
			} else if (newRankNum == 2) {
				isModerator = true;
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436877816796020836")).queue();
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436842179594031358")).queue();
			} else if (newRankNum == 3) {
				isAdmin = true;
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436877816796020836")).queue();
				guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436842548412027011")).queue();
			}
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			if (isHelper || isModerator || isAdmin) {
				String rankName = "";
				if (isAdmin) {
					rankName = "an Admin";
				} else if (isModerator) {
					rankName = "a Moderator";
				} else {
					rankName = "a Helper";
				}

				String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
				String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
				EmbedBuilder embed = new EmbedBuilder()
						.setAuthor(player.getName() + " has become " + rankName + "!", null, url)
						.setColor(Color.YELLOW);
				serverChatChannel.sendMessageEmbeds(embed.build()).queue();
				roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
			}
		}
	}

	/**
	 * Update all Discord roles to stay aligned with in-game ranks in case of misalignment.
	 */
	public static void updateAllDiscordRoles() {
		for (UUID uuid : DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().values()) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			// Strange bug where a player links their account but they do not have an AranarthPlayer
			if (aranarthPlayer == null) {
				continue;
			}
			updateRank(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getRank(), false);
			updateSaint(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getSaintRank(), false);
			updateArchitect(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getArchitectRank(), false);
			updateCouncil(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getCouncilRank(), false);
		}
	}

	/**
	 * Adds a message in #punishment-history in Discord to reflect a punishment.
	 * @param punishment The punishment being listed.
	 */
	public static void addPunishmentToDiscord(Punishment punishment) {
		Guild guild = getGuild();

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(punishment.getUuid());
		String uuidNoDashes = punishment.getUuid().toString().replaceAll("-", "");
		String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
		EmbedBuilder embed = new EmbedBuilder();
		String appliedBy = "Console";
		if (punishment.getAppliedBy() != null) {
			appliedBy = Bukkit.getOfflinePlayer(punishment.getAppliedBy()).getName();
		}

        switch (punishment.getType()) {
            case "WARN" -> {
                embed.setAuthor(aranarthPlayer.getUsername() + " has been warned", null, url);
                embed.setColor(Color.LIGHT_GRAY);
                embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Warned by:** " + appliedBy + "\n" +
                        "**Reason:** " + punishment.getReason());
            }
			case "MUTE" -> {
				embed.setAuthor(aranarthPlayer.getUsername() + " has been muted", null, url);
				embed.setColor(Color.YELLOW);

				String formattedEndDate = "None";
				if (!aranarthPlayer.getMuteEndDate().equals("none")) {
					LocalDateTime endDate = ChatUtils.getMuteEndAsLocalDateTime(aranarthPlayer);
					String year = endDate.getYear() + "";
					String month = endDate.getMonthValue() < 10 ? "0" + endDate.getMonthValue() : endDate.getMonthValue() + "";
					String day = endDate.getDayOfMonth() < 10 ? "0" + endDate.getDayOfMonth() : endDate.getDayOfMonth() + "";
					String hour = endDate.getHour() < 10 ? "0" + endDate.getHour() : endDate.getHour() + "";
					String minute = endDate.getMinute() < 10 ? "0" + endDate.getMinute() : endDate.getMinute() + "";
					formattedEndDate = month + "/" + day + "/" + year + " at " + hour + ":" + minute + " EST";
				}

				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Muted by:** " + appliedBy + "\n" +
						"**Mute end date:** " + formattedEndDate + "\n" +
						"**Reason:** " + punishment.getReason());
			}
			case "BAN" -> {
				OfflinePlayer player = Bukkit.getOfflinePlayer(punishment.getUuid());
				ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
				String formattedEndDate = "None";

				// If it's a temporary ban
				if (profileBanList.getBanEntry(player.getPlayerProfile()).getExpiration() != null) {
					Instant banEndInstant = profileBanList.getBanEntry(player.getPlayerProfile()).getExpiration().toInstant();
					LocalDateTime endDate = LocalDateTime.ofInstant(banEndInstant, ZoneId.systemDefault());
					String year = endDate.getYear() + "";
					String month = endDate.getMonthValue() < 10 ? "0" + endDate.getMonthValue() : endDate.getMonthValue() + "";
					String day = endDate.getDayOfMonth() < 10 ? "0" + endDate.getDayOfMonth() : endDate.getDayOfMonth() + "";
					String hour = endDate.getHour() < 10 ? "0" + endDate.getHour() : endDate.getHour() + "";
					String minute = endDate.getMinute() < 10 ? "0" + endDate.getMinute() : endDate.getMinute() + "";
					formattedEndDate = month + "/" + day + "/" + year + " at " + hour + ":" + minute + " EST";
				}
				embed.setAuthor(aranarthPlayer.getUsername() + " has been banned", null, url);
				embed.setColor(Color.RED);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Banned by:** " + appliedBy + "\n" +
						"**Ban end date:** " + formattedEndDate + "\n" +
						"**Reason:** " + punishment.getReason());
			}
			case "UNMUTE" -> {
				embed.setAuthor(aranarthPlayer.getUsername() + " has been unmuted", null, url);
				embed.setColor(Color.GREEN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Unmuted by:** " + appliedBy + "\n" +
						"**Reason:** " + punishment.getReason());
			}
			case "UNBAN" -> {
				embed.setAuthor(aranarthPlayer.getUsername() + " has been unbanned", null, url);
				embed.setColor(Color.GREEN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Unbanned by:** " + appliedBy + "\n" +
						"**Reason:** " + punishment.getReason());
			}
			case "REMOVE_WARN" -> {
				embed.setAuthor("A warning has been removed from " + aranarthPlayer.getUsername(), null, url);
				embed.setColor(Color.CYAN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Warning removed by:** " + appliedBy + "\n" +
						"**Original Warn Reason:** " + punishment.getReason());
			}
			case "REMOVE_MUTE" -> {
				embed.setAuthor("A mute has been removed from " + aranarthPlayer.getUsername(), null, url);
				embed.setColor(Color.CYAN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Mute removed by:** " + appliedBy + "\n" +
						"**Original Mute Reason:** " + punishment.getReason());
			}
			case "REMOVE_BAN" -> {
				embed.setAuthor("A ban has been removed from " + aranarthPlayer.getUsername(), null, url);
				embed.setColor(Color.CYAN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Ban removed by:** " + appliedBy + "\n" +
						"**Original Ban Reason:** " + punishment.getReason());
			}
			case "REMOVE_UNMUTE" -> {
				embed.setAuthor("An unmute has been removed from " + aranarthPlayer.getUsername(), null, url);
				embed.setColor(Color.CYAN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Unmute removed by:** " + appliedBy + "\n" +
						"**Original Unmute Reason:** " + punishment.getReason());
			}
			case "REMOVE_UNBAN" -> {
				embed.setAuthor("An unban has been removed from " + aranarthPlayer.getUsername(), null, url);
				embed.setColor(Color.CYAN);
				embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Unban removed by:** " + appliedBy + "\n" +
						"**Original Unban Reason:** " + punishment.getReason());
			}
        }
		punishmentHistoryChannelId.sendMessageEmbeds(embed.build()).queue();
	}

	/**
	 * Adds a message in #server-chat in Discord to reflect changes to the Avatar.
	 * @param isNewAvatar Confirmation whether the message is for a new avatar, and if not, a deceased one.
	 */
	public static void addAvatarMessageToDiscord(Avatar avatar, boolean isNewAvatar) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(avatar.getUuid());
		String username = AranarthUtils.getUsername(Bukkit.getOfflinePlayer(avatar.getUuid()));
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(username + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(avatar.getUuid());
		String uuidNoDashes = avatar.getUuid().toString().replaceAll("-", "");
		String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
		Role role = guild.getRoleById("1440165603687137461"); // Avatar

		// If it is a player who has become the Avatar
		if (isNewAvatar) {
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor("Avatar " + username + " has risen!", null, url)
					.setColor(Color.MAGENTA);
			if (playerDiscordId != null) {
				guild.addRoleToMember(playerDiscordId, role).queue();
			}
			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
		}
		// If the avatar has deceased
		else {
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor("Avatar " + username + " has deceased...", null, url)
					.setColor(Color.MAGENTA);
			if (playerDiscordId != null) {
				guild.removeRoleFromMember(playerDiscordId, role).queue();
			}
			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
		}
	}

}
