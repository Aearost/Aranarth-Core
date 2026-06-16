package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.*;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Provides a variety of utility methods for everything related to Discord integration.
 */
public class DiscordUtils {

	private static final TextChannel punishmentHistoryChannelId = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("punishment");
	private static final TextChannel roleChangesChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("roles");
	private static final TextChannel serverChatChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("chat");
	private static final TextChannel notifications = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("notifications");
	private static final TextChannel dominions = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("dominions");
	private static final TextChannel welcome = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("welcome");

	private static String discordRole(String key) {
		return AranarthCore.getInstance().getConfig().getString("discord.roles." + key);
	}

	private static String ownerUserId() {
		return AranarthCore.getInstance().getConfig().getString("discord.owner-user-id");
	}

	/**
	 * Returns a failure handler for role operations that automatically unlinks a player's
	 * Discord account if they are no longer in the server (error 10007: Unknown Member).
	 * @param uuid The Minecraft UUID of the player whose role is being updated.
	 */
	private static Consumer<Throwable> unlinkIfUnknownMember(UUID uuid) {
		return error -> {
			String msg = error.getMessage();
			if (msg != null && (msg.contains("10007") || msg.contains("Unknown Member"))) {
				Bukkit.getLogger().info("[DiscordUtils] Unlinking " + Bukkit.getOfflinePlayer(uuid).getName()
						+ " as they are no longer in the Discord server");
				DiscordSRV.getPlugin().getAccountLinkManager().unlink(uuid);
			} else {
				Bukkit.getLogger().warning("[DiscordUtils] Role update failed for " + uuid + ": " + msg);
			}
		};
	}

	/**
	 * Provides the Discord Guild.
	 * @return The Discord Guild.
	 */
	public static Guild getGuild() {
		JDA jda = DiscordSRV.getPlugin().getJda();
		return jda.getGuildById(AranarthCore.getInstance().getConfig().getString("discord.guild-id"));
	}

	public static void sendChatMessage(String message) {
		String discordMessage = ChatUtils.stripColorFormatting(message);
		List<String> indexRangesOfTaggedPlayers = new ArrayList<>();

		char[] characters = discordMessage.toCharArray();
		int pingStart = -1;
		String finalMessage = "";
		for (int i = 0; i < discordMessage.length(); i++) {
			if (characters[i] == '@') {
				pingStart = i;
			}
			// Not currently trying to find a user ID
			else if (pingStart == -1) {
				finalMessage += characters[i];
			} else {
				// If the ping ended
				if (characters[i] == ' ' || i == discordMessage.length() - 1) {
					String fullInputUsername = "";
					// Start after the @ and end at the last character
					for (int j = pingStart + 1; j <= i; j++) {
						fullInputUsername += characters[j];
					}

					if (fullInputUsername.endsWith(" ")) {
						fullInputUsername = fullInputUsername.substring(0, fullInputUsername.length() - 1);
					}
					String id = findMemberToPing(fullInputUsername);
					if (id != null) {
						finalMessage += "<@" + id + "> ";
					} else {
						finalMessage += fullInputUsername + " ";
					}
					pingStart = -1;
				}
			}
		}

		// Architect symbol - force text presentation
		finalMessage = finalMessage.replace("\uD83D\uDD28", "\uD83D\uDD28\uFE0E");
		// Saint symbol - force text presentation
		finalMessage = finalMessage.replace("\u269C", "\u269C\uFE0E");
		serverChatChannel.sendMessage(finalMessage).queue();
	}

	/**
	 * Provides the ID of the user to ping based on the input String.
	 * @param input The string that
	 * @return The ID associated to the input username.
	 */
	private static String findMemberToPing(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}

		List<Member> username = getGuild().getMembersByName(input, true);
		if (!username.isEmpty()) {
			return username.getFirst().getId();
		}

		List<Member> nickname = getGuild().getMembersByNickname(input, true);
		if (!nickname.isEmpty()) {
			return nickname.getFirst().getId();
		}

		List<Member> effectiveName = getGuild().getMembersByEffectiveName(input, true);
		if (!effectiveName.isEmpty()) {
			return effectiveName.getFirst().getId();
		}

		return null;
	}

	/**
	 * Updates the player's in-game rank accordingly in Discord's roles.
	 * Updates #server-chat and #role-changes in Discord.
	 * @param player The player whose rank is changing.
	 * @param newRankNum The player's new rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e. due to rankup or manual command change.
	 */
	public static void updateRank(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();
		Role roleToAdd = switch (newRankNum) {
			case 1 -> guild.getRoleById(discordRole("esquire"));
			case 2 -> guild.getRoleById(discordRole("knight"));
			case 3 -> guild.getRoleById(discordRole("baron"));
			case 4 -> guild.getRoleById(discordRole("count"));
			case 5 -> guild.getRoleById(discordRole("duke"));
			case 6 -> guild.getRoleById(discordRole("prince"));
			case 7 -> guild.getRoleById(discordRole("king"));
			case 8 -> guild.getRoleById(discordRole("emperor"));
			default -> guild.getRoleById(discordRole("peasant"));
		};

		if (playerDiscordId != null && guild.getMemberById(playerDiscordId) != null) {
			List<Role> playerDiscordRoles = guild.getMemberById(playerDiscordId).getRoles();
			for (Role role : playerDiscordRoles) {
				// Any of the rank-based roles
				if (role.getId().equals(discordRole("esquire")) || role.getId().equals(discordRole("knight"))
						|| role.getId().equals(discordRole("baron")) || role.getId().equals(discordRole("count"))
						|| role.getId().equals(discordRole("duke")) || role.getId().equals(discordRole("prince"))
						|| role.getId().equals(discordRole("king")) || role.getId().equals(discordRole("emperor"))
						|| role.getId().equals(discordRole("peasant"))) {
					guild.removeRoleFromMember(playerDiscordId, role).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				}
			}
			guild.addRoleToMember(playerDiscordId, roleToAdd).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			String aOrAn = "a";
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			String rankName = AranarthUtils.getRank(aranarthPlayer).substring(5);
			String[] rankNameNoBrackets = rankName.split("]");
			rankName = ChatUtils.stripColorFormatting(rankNameNoBrackets[0].substring(0, rankNameNoBrackets[0].length() - 4));
			if (rankName.equals("Esquire") || rankName.equals("Emperor") || rankName.equals("Empress")) {
				aOrAn = "an";
			}

			String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor(player.getName() + " has become " + aOrAn + " " + rankName + "!", null, url)
					.setColor(Color.LIGHT_GRAY);

			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue(message -> {
				message.addReaction("\uD83C\uDF89").queue();
				message.addReaction("❤").queue();
			});
		}
    }

	/**
	 * Updates the player's Saint rank accordingly in Discord's roles.
	 * Only manually updates #role-changes, as the other updates are from donationNotification().
	 * @param player The player whose Saint rank is changing.
	 * @param newRankNum The player's new Saint rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e. due to rankup or manual command change.
	 */
	public static void updateSaint(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		Guild guild = getGuild();
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null || guild.getMemberById(playerDiscordId) == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		boolean isSaint = false;

		// If they are a Saint
		if (newRankNum > 0) {
			isSaint = true;
			if (playerDiscordId != null) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("saint"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("discord-linked"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			}
		} else {
			if (playerDiscordId != null) {
				guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("saint"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("discord-linked"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			}
		}

		// Only display intentional changes in Discord, not auto-assign
		if (isIntentionalChange) {
			if (isSaint) {
				donationNotification(player.getName() + " has donated and become a Saint!", player.getUniqueId(), Color.MAGENTA);

				// Must manually be sent for role changes
				String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
				String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
				EmbedBuilder embed = new EmbedBuilder()
						.setAuthor(player.getName() + " has donated and become a Saint!", null, url)
						.setColor(Color.MAGENTA);
				roleChangesChannel.sendMessageEmbeds(embed.build()).queue(message -> {
					message.addReaction("⚜").queue();
					message.addReaction("\uD83D\uDC9C").queue();
				});
			}
		}
	}

	/**
	 * Updates the player's Architect rank accordingly in Discord's roles.
	 * Updates #server-chat, #role-changes, and #notifications in Discord.
	 * @param player The player whose Architect rank is changing.
	 * @param newRankNum The player's new Architect rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e due to rankup or manual command change.
	 */
	public static void updateArchitect(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		Guild guild = getGuild();
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null || guild.getMemberById(playerDiscordId) == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
			return;
		}

		boolean isArchitect = false;

		// If they are an Architect
		if (newRankNum > 0) {
			isArchitect = true;
			guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("architect"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
		} else {
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("architect"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
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
				notifications.sendMessageEmbeds(embed.build()).queue();
				// Server owner's Discord User ID
				notifications.sendMessage("<@" + ownerUserId() + ">").queue();
			}
		}
	}

	/**
	 * Updates the player's Council rank accordingly in Discord's roles.
	 * Updates #server-chat, #role-changes, and #notifications in Discord.
	 * @param player The player whose Council rank is changing.
	 * @param newRankNum The player's new Council rank number.
	 * @param isIntentionalChange Whether the change to the rank is intentional i.e due to rankup or manual command change.
	 */
	public static void updateCouncil(OfflinePlayer player, int newRankNum, boolean isIntentionalChange) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		Guild guild = getGuild();
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null || guild.getMemberById(playerDiscordId) == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
			return;
		}

		boolean isHelper = false;
		boolean isModerator = false;
		boolean isAdmin = false;

		if (playerDiscordId != null) {
			// Remove all council ranks
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("council"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("helper"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("moderator"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("admin"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));

			// If they are a Council member
			if (newRankNum == 1) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("council"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("helper"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			} else if (newRankNum == 2) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("council"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("moderator"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			} else if (newRankNum == 3) {
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("council"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
				guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("admin"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			}
		}

		// If they are a Council member
		if (newRankNum == 1) {
			isHelper = true;
		} else if (newRankNum == 2) {
			isModerator = true;
		} else if (newRankNum == 3) {
			isAdmin = true;
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
				notifications.sendMessageEmbeds(embed.build()).queue();
				// Server owner's Discord User ID
				notifications.sendMessage("<@" + ownerUserId() + ">").queue();
			}
		}
	}

	/**
	 * Update all Discord roles to stay aligned with in-game ranks in case of misalignment.
	 */
	public static void updateAllDiscordRoles() {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
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
			updateDiscordRole(Bukkit.getOfflinePlayer(uuid), aranarthPlayer);
			updateAvatar(Bukkit.getOfflinePlayer(uuid));
		}
	}

	/**
	 * Updates the player's Discord role accordingly in Discord's roles.
	 * @param player The player whose Discord role is changing.
	 * @param aranarthPlayer The AranarthPlayer object of the player.
	 */
	public static void updateDiscordRole(OfflinePlayer player, AranarthPlayer aranarthPlayer) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		Guild guild = getGuild();
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (playerDiscordId == null || guild.getMemberById(playerDiscordId) == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord role could not be updated as they have not linked their Discord");
			return;
		}

		if (aranarthPlayer.getPerks().get(Perk.DISCORD) == 1) {
			guild.addRoleToMember(playerDiscordId, guild.getRoleById(discordRole("discord-linked"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
		} else {
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById(discordRole("discord-linked"))).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
		}
	}

	/**
	 * Updates the player's Avatar role in Discord. Not done automatically in addAvatarMessageToDiscord.
	 * @param player The player being verified.
	 */
	private static void updateAvatar(OfflinePlayer player) {
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		Guild guild = getGuild();
		if (playerDiscordId == null || guild.getMemberById(playerDiscordId) == null) {
			Bukkit.getLogger().info(player.getName() + "'s Discord roles could not be updated as they have not linked their Discord");
			return;
		}

		Role avatarRole = guild.getRoleById(discordRole("avatar"));
		List<Role> playerDiscordRoles = guild.getMemberById(playerDiscordId).getRoles();
		if (playerDiscordRoles.contains(avatarRole)) {
			// If the player is the current Avatar
			if (AvatarUtils.getCurrentAvatar() == null || !AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
				guild.removeRoleFromMember(playerDiscordId, avatarRole).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			}
		} else {
			// If the player is the current Avatar
			if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
				guild.addRoleToMember(playerDiscordId, avatarRole).queue(null, unlinkIfUnknownMember(player.getUniqueId()));
			}
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
                embed.setColor(new Color(253, 233, 146));
                embed.setDescription("**UUID:** " + punishment.getUuid().toString() + "\n" +
						"**Warned by:** " + appliedBy + "\n" +
                        "**Reason:** " + punishment.getReason());
            }
			case "MUTE" -> {
				embed.setAuthor(aranarthPlayer.getUsername() + " has been muted", null, url);
				embed.setColor(new Color(255, 255, 0));

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
				embed.setColor(new Color(70, 0, 0));
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
	 * Adds a message in Discord to reflect changes to the Avatar.
	 * Updates #server-chat, #role-changes, and #notifications in Discord.
	 * @param isNewAvatar Confirmation whether the message is for a new avatar, and if not, a deceased one.
	 */
	public static void addAvatarMessageToDiscord(Avatar avatar, boolean isNewAvatar) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(avatar.getUuid());
		String username = AranarthUtils.getUsername(Bukkit.getOfflinePlayer(avatar.getUuid()));
		if (playerDiscordId == null) {
			Bukkit.getLogger().info(username + "'s Discord roles could not be updated as they have not linked their Discord");
		}

		Guild guild = getGuild();

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(avatar.getUuid());
		String uuidNoDashes = avatar.getUuid().toString().replaceAll("-", "");
		String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
		Role role = guild.getRoleById(discordRole("avatar"));

		// If it is a player who has become the Avatar
		if (isNewAvatar) {
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor("Avatar " + username + " has risen!", null, url)
					.setColor(Color.MAGENTA);
			if (playerDiscordId != null) {
				guild.addRoleToMember(playerDiscordId, role).queue(null, unlinkIfUnknownMember(avatar.getUuid()));
			}
			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
			notifications.sendMessageEmbeds(embed.build()).queue();
		}
		// If the avatar has deceased
		else {
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor("Avatar " + username + " has deceased...", null, url)
					.setColor(Color.MAGENTA);
			if (playerDiscordId != null) {
				guild.removeRoleFromMember(playerDiscordId, role).queue(null, unlinkIfUnknownMember(avatar.getUuid()));
			}
			serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			roleChangesChannel.sendMessageEmbeds(embed.build()).queue();
		}
	}

	/**
	 * Adds a message in #server-chat in Discord to reflect the addition of a new server boost.
	 * Only updates #server-chat if a boost is expired.
	 * @param uuid The UUID of the player who purchased/is applying the boost.
	 * @param boost The boost being applied.
	 * @param isAdding Whether a boost is being added.
	 */
	public static void updateBoostInDiscord(UUID uuid, Boost boost, boolean isAdding) {
		Guild guild = getGuild();

		String name = "";
		Color color = null;
		if (boost == Boost.MINER) {
			name = "Boost of the Miner";
			color = new Color(85, 85, 85);
		} else if (boost == Boost.HARVEST) {
			name = "Boost of the Harvest";
			color = new Color(255, 170, 0);
		} else if (boost == Boost.HUNTER) {
			name = "Boost of the Hunter";
			color = new Color(255, 85, 85);
		} else if (boost == Boost.CHI) {
			name = "Boost of Chi";
			color = new Color(255, 255, 255);
		} else {
			name = "Unspecified Boost";
			color = new Color(255, 85, 255);
		}

		// If it was applied by a user
		if (uuid != null) {
			String username = AranarthUtils.getUsername(Bukkit.getOfflinePlayer(uuid));
			donationNotification(username + " has applied the " + name, uuid, color);
			serverChatChannel.sendMessage("<@&1515810206741823508>").allowedMentions(EnumSet.of(Message.MentionType.ROLE)).queue();
		} else {
			if (isAdding) {
				donationNotification("The " + name + " has been applied", null, color);
				serverChatChannel.sendMessage("<@&1515810206741823508>").allowedMentions(EnumSet.of(Message.MentionType.ROLE)).queue();
			} else {
				EmbedBuilder embed = new EmbedBuilder()
						.setAuthor("The " + name + " has expired")
						.setColor(color);
				serverChatChannel.sendMessageEmbeds(embed.build()).queue();
			}
		}
	}

	/**
	 * Sends a reminder embed to #server-chat when a boost is about to expire.
	 * @param boost The boost.
	 * @param timeLabel Human-readable time remaining (e.g. "1 hour", "30 minutes", "1 minute").
	 */
	public static void sendBoostReminderToDiscord(Boost boost, String timeLabel) {
		String name = "";
		Color color = null;
		if (boost == Boost.MINER) {
			name = "Boost of the Miner";
			color = new Color(85, 85, 85);
		} else if (boost == Boost.HARVEST) {
			name = "Boost of the Harvest";
			color = new Color(255, 170, 0);
		} else if (boost == Boost.HUNTER) {
			name = "Boost of the Hunter";
			color = new Color(255, 85, 85);
		} else if (boost == Boost.CHI) {
			name = "Boost of Chi";
			color = new Color(255, 255, 255);
		} else {
			name = "Unspecified Boost";
			color = new Color(255, 85, 255);
		}
		EmbedBuilder embed = new EmbedBuilder()
				.setAuthor("The " + name + " expires in " + timeLabel + "!")
				.setColor(color);
		serverChatChannel.sendMessageEmbeds(embed.build()).queue();
		serverChatChannel.sendMessage("<@&1515810206741823508>").allowedMentions(EnumSet.of(Message.MentionType.ROLE)).queue();
	}

	/**
	 * Sends a notification when a player donates.
	 * Updates both #notifications and #role-changes in Discord.
	 * @param message The message to be sent.
	 * @param uuid The UUID of the player who donated.
	 */
	public static void donationNotification(String message, UUID uuid, Color color) {
		// Plays a donation sound for all online players
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP , 1F, ThreadLocalRandom.current().nextFloat(1.2F, 1.6F));
		}

		Guild guild = getGuild();
		EmbedBuilder embed = new EmbedBuilder();
		message = ChatUtils.stripColorFormatting(message);
		message = message.replaceAll("&[a-z0-9]", "");

		if (uuid == null) {
			embed.setAuthor(message);
		} else {
			String uuidNoDashes = uuid.toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";

			embed.setAuthor(message, null, url);
		}
		embed.setColor(color);

		serverChatChannel.sendMessageEmbeds(embed.build()).queue();
		notifications.sendMessageEmbeds(embed.build()).queue();
		// Server owner's Discord User ID
		notifications.sendMessage("<@" + ownerUserId() + ">").queue();
	}

	/**
	 * Sends a miscellaneous notification when desired to be sent to the notifications channel.
	 * Updates only #notifications in Discord.
	 * @param message The message to be sent.
	 * @param uuid The UUID of the player who is involved with the notification.
	 */
	public static void createNotification(String message, UUID uuid) {
		Guild guild = getGuild();
		EmbedBuilder embed = new EmbedBuilder();
		message = ChatUtils.stripColorFormatting(message);
		message = message.replaceAll("&[a-z0-9]", "");

		if (uuid == null) {
			embed.setAuthor(message).setColor(Color.CYAN);
		} else {
			String uuidNoDashes = uuid.toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";

			embed.setAuthor(message, null, url).setColor(Color.CYAN);
		}

		notifications.sendMessageEmbeds(embed.build()).queue();
		// Server owner's Discord User ID
		notifications.sendMessage("<@" + ownerUserId() + ">").queue();
	}

	/**
	 * Sends a notification when it is a new month.
	 * Updates only #server-chat in Discord.
	 * @param month The new month.
	 * @param description The description of the month.
	 */
	public static void monthMessage(Month month, String description) {
		Guild guild = getGuild();
		EmbedBuilder embed = new EmbedBuilder();

		description = description.replaceAll("&.", "");

		embed.setTitle("The month of " + ChatUtils.getFormattedItemName(month.name()) + " has begun!")
				.setDescription(description)
				.setColor(Color.MAGENTA);

		serverChatChannel.sendMessageEmbeds(embed.build()).queue();
	}

	/**
	 * Handles applying the muted role in Discord.
	 * @param uuid The uuid of the player.
	 * @param isApplyingMute Whether the player is actively being muted.
	 */
	public static void toggleMuteRole(UUID uuid, boolean isApplyingMute) {
		if (!AranarthCore.isPublicServer()) {
			return;
		}
		Guild guild = getGuild();
		Role mutedRole = guild.getRoleById(discordRole("muted"));
		String playerDiscordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
		if (playerDiscordId == null) {
			Bukkit.getLogger().info("The Muted role could not be applied to " + Bukkit.getOfflinePlayer(uuid).getName() + " as they are not linked to Discord");
		} else {
			if (isApplyingMute) {
				guild.addRoleToMember(playerDiscordId, mutedRole).queue(null, unlinkIfUnknownMember(uuid));
			} else {
				guild.removeRoleFromMember(playerDiscordId, mutedRole).queue(null, unlinkIfUnknownMember(uuid));
			}
		}
	}

	/**
	 * Handles sending messages to the dominions relations channel.
	 * @param dominion The Dominion involved with the message that will show its leader's head in the message.
	 * @param message The message to be sent.
	 * @param color The color of the Discord message.
	 */
	public static void dominionMessage(Dominion dominion, String message, Color color) {
		Guild guild = getGuild();
		EmbedBuilder embed = new EmbedBuilder();
		message = ChatUtils.stripColorFormatting(message);
		message = message.replaceAll("&[a-z0-9]", "");
		String uuidNoDashes = dominion.getLeader().toString().replaceAll("-", "");
		String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
		embed.setAuthor(message, null, url).setColor(color);
		dominions.sendMessageEmbeds(embed.build()).queue();
	}

	/**
	 * Provides custom server join messages in Discord with emojis.
	 * @param name The display name of the user that joined the server.
	 * @param discordId The Discord user ID of the user that joined the server.
	 */
	public static void discordServerJoin(String name, String discordId) {
		String link = "<@" + discordId + ">";
		List<String> welcomeMessages = new ArrayList<>();
		welcomeMessages.add("**Welcome " + link + " to Aranarth's Discord!**");
		welcomeMessages.add("**Is that " + link + " who has come to join us? \uD83D\uDC40**");
		welcomeMessages.add("**Look - it's a wild " + link + "!**");
		welcomeMessages.add("**" + link + " has entered the realm! ⚔️**");
		welcomeMessages.add("**All hail " + link + ", our newest arrival! 👑**");
		welcomeMessages.add("**" + link + " joined the party! 🎉**");
		welcomeMessages.add("**The gates open for " + link + "! 🏰**");
		welcomeMessages.add("**A new challenger appears: " + link + "! ⚡**");

		int index = ThreadLocalRandom.current().nextInt(welcomeMessages.size());
		welcome.sendMessage(welcomeMessages.get(index)).allowedMentions(new ArrayList<>()).queue(message -> {
			message.addReaction("\uD83D\uDC4B").queue();
			message.addReaction("🎉").queue();
			message.addReaction("\uD83C\uDF88").queue();
		});
	}

	/**
	 * Provides custom server quit messages in Discord with emojis.
	 * @param name The display name of the user that quit the server.
	 * @param discordId The Discord user ID of the user that quit the server.
	 */
	public static void discordServerQuit(String name, String discordId) {
		String link = "<@" + discordId + ">";
		List<String> leaveMessages = new ArrayList<>();
		leaveMessages.add("**" + link + " has left the realm 🏰**");
		leaveMessages.add("**" + link + " has gone AFK... forever? 😶**");
		leaveMessages.add("**" + link + " went poof ✨**");
		leaveMessages.add("**" + link + " has left Aranarth... farewell! 🌙**");
		leaveMessages.add("**" + link + " has vanished into the void 🌀**");
		leaveMessages.add("**" + link + " has left us... for now ⏳**");
		leaveMessages.add("**" + link + " left without saying goodbye 😢**");
		leaveMessages.add("**" + link + " rage quit 😤**");

		int index = ThreadLocalRandom.current().nextInt(leaveMessages.size());
		welcome.sendMessage(leaveMessages.get(index)).allowedMentions(new ArrayList<>()).queue(message -> {
			message.addReaction("\uD83D\uDC94").queue();
			message.addReaction("\uD83D\uDE2D").queue();
		});
	}
}
