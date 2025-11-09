package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import java.util.List;

import java.awt.*;
import java.util.UUID;

/**
 * Provides a variety of utility methods for everything related to Discord integration.
 */
public class DiscordUtils {

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
			return;
		}

		JDA jda = DiscordSRV.getPlugin().getJda();
		Guild guild = jda.getGuildById("664319732446396416");
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
		TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
		channel.sendMessageEmbeds(embed.build()).queue();
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
			return;
		}

		JDA jda = DiscordSRV.getPlugin().getJda();
		Guild guild = jda.getGuildById("664319732446396416");
		boolean isSaint = false;

		// If they are a Saint
		if (newRankNum > 0) {
			isSaint = true;
			guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436839449626542161")).queue();
		} else {
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436839449626542161")).queue();
		}

		if (isSaint) {
			String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor(player.getName() + " has donated and become a Saint!", null, url)
					.setColor(Color.MAGENTA);
			TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
			channel.sendMessageEmbeds(embed.build()).queue();
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
			return;
		}

		JDA jda = DiscordSRV.getPlugin().getJda();
		Guild guild = jda.getGuildById("664319732446396416");
		boolean isArchitect = false;

		// If they are an Architect
		if (newRankNum > 0) {
			isArchitect = true;
			guild.addRoleToMember(playerDiscordId, guild.getRoleById("1436842029274632293")).queue();
		} else {
			guild.removeRoleFromMember(playerDiscordId, guild.getRoleById("1436842029274632293")).queue();
		}

		if (isArchitect) {
			String uuidNoDashes = player.getUniqueId().toString().replaceAll("-", "");
			String url = "https://crafthead.net/avatar/" + uuidNoDashes + "/128";
			EmbedBuilder embed = new EmbedBuilder()
					.setAuthor(player.getName() + " has become an Architect!", null, url)
					.setColor(Color.YELLOW);
			TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
			channel.sendMessageEmbeds(embed.build()).queue();
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
			return;
		}

		JDA jda = DiscordSRV.getPlugin().getJda();
		Guild guild = jda.getGuildById("664319732446396416");
		boolean isHelper = false;
		boolean isModerator = false;
		boolean isAdmin = false;

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
			TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("global");
			channel.sendMessageEmbeds(embed.build()).queue();
		}
	}

	/**
	 * Update all Discord roles to stay aligned with in-game ranks in case of misalignment.
	 */
	public static void updateAllDiscordRoles() {
		for (UUID uuid : DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().values()) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			updateRank(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getRank(), false);
			updateSaint(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getSaintRank(), false);
			updateArchitect(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getArchitectRank(), false);
			updateCouncil(Bukkit.getOfflinePlayer(uuid), aranarthPlayer.getCouncilRank(), false);
		}
	}

}
