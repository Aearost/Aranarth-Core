package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.PunishmentCategory;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.objects.Punishment;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Applies bans, mutes and warnings on this server, whether they were issued here or on the other
 * server, so both servers always agree on who is punished.
 */
public class PunishmentUtils {

    public static final String BAN = "BAN";
    public static final String UNBAN = "UNBAN";
    public static final String MUTE = "MUTE";
    public static final String UNMUTE = "UNMUTE";
    public static final String WARN = "WARN";

    /**
     * Called by the commands after a punishment was applied on this server, saves it, and sends it to
     * the other server.
     *
     * @param banExpiresAt When a ban ends in epoch milliseconds, or -1 for a permanent ban.
     */
    public static void issued(Punishment punishment, String nickname, String duration, long banExpiresAt) {
        issued(punishment, nickname, duration, banExpiresAt, false);
    }

    /**
     * Called by the commands after a punishment was applied on this server, saves it, and sends it to
     * the other server.
     *
     * @param banExpiresAt  When a ban ends in epoch milliseconds, or -1 for a permanent ban.
     * @param notifyCouncil Whether the punishment is an offence the Council must be alerted about.
     */
    public static void issued(Punishment punishment, String nickname, String duration, long banExpiresAt, boolean notifyCouncil) {
        UUID uuid = punishment.getUuid();
        if (notifyCouncil) {
            notifyCouncil(nickname, punishment.getCategory());
        }
        savePunishmentsNow(uuid);
        switch (punishment.getType()) {
            case BAN -> {
                if (DatabaseManager.isActive()) {
                    String reason = punishment.getReason();
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                            DatabaseManager.getInstance().upsertNetworkBan(uuid, reason, banExpiresAt));
                }
                // A banned Avatar's reign ends right away
                Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
                if (currentAvatar != null && currentAvatar.getUuid().equals(uuid)) {
                    AvatarUtils.removeCurrentAvatar("banned");
                }
            }
            case UNBAN -> {
                if (DatabaseManager.isActive()) {
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                            DatabaseManager.getInstance().deleteNetworkBan(uuid));
                }
            }
            case MUTE, UNMUTE -> persistMute(uuid);
        }
        if (NetworkManager.isActive()) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            String muteEndDate = aranarthPlayer != null ? aranarthPlayer.getMuteEndDate() : "";
            NetworkManager.getInstance().publishPunishment(punishment, nickname, duration, banExpiresAt, muteEndDate, notifyCouncil);
        }
    }

    /**
     * Deletes a punishment from the player's history on both servers, and logs the removal to Discord.
     *
     * @param punishment The punishment being deleted.
     * @param removedBy  The UUID of the Council member deleting it, or null for the console.
     */
    public static void removed(Punishment punishment, UUID removedBy) {
        UUID uuid = punishment.getUuid();
        DiscordUtils.addPunishmentToDiscord(new Punishment(uuid, LocalDateTime.now(), "REMOVE_" + punishment.getType(),
                punishment.getReason(), removedBy));
        AranarthUtils.removePunishment(uuid, punishment);
        savePunishmentsNow(uuid);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishPunishmentRemoval(uuid, punishment.getDateMillis(), punishment.getType());
        }
    }

    /**
     * Deletes a punishment that was deleted on the other server. The other server already saved the history.
     */
    public static void onRemoteRemoval(UUID uuid, long dateMillis, String type) {
        List<Punishment> punishments = AranarthUtils.getPunishments(uuid);
        if (punishments == null) {
            return;
        }
        punishments.removeIf(punishment -> punishment.getDateMillis() == dateMillis && punishment.getType().equals(type));
    }

    /**
     * Alerts every Council member online on this server that the player has reached an offence that the
     * Council must be told about.
     *
     * @param nickname The nickname of the punished player.
     * @param category The key of the offence category, or null if there is none.
     */
    public static void notifyCouncil(String nickname, String category) {
        PunishmentCategory punishmentCategory = category == null ? null : PunishmentCategory.fromKey(category);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("aranarth.punishments")) {
                String categoryName = punishmentCategory == null ? Lang.getFor(onlinePlayer, "punishment.type_other")
                        : Lang.getFor(onlinePlayer, "user.category_" + punishmentCategory.getKey());
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "user.council_alert",
                        "player", nickname, "category", categoryName)));
                onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 0.8F);
            }
        }
    }

    /**
     * Applies a punishment that was issued on the other server.
     */
    public static void onRemotePunishment(Punishment punishment, String nickname, String duration,
                                          long banExpiresAt, String muteEndDate, boolean notifyCouncil) {
        UUID uuid = punishment.getUuid();
        if (notifyCouncil) {
            notifyCouncil(nickname, punishment.getCategory());
        }
        // The issuing server already posted it to Discord
        AranarthUtils.addPunishment(uuid, punishment, true);
        String reason = punishment.getReason();

        switch (punishment.getType()) {
            case BAN -> {
                // Also kicks the player if they are online on this server
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                offlinePlayer.ban(reason, banExpiresAt < 0 ? null : new Date(banExpiresAt), null);
                notifyBan(nickname, duration, reason, banExpiresAt < 0);
            }
            case UNBAN -> {
                ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
                profileBanList.pardon(Bukkit.getOfflinePlayer(uuid).getPlayerProfile());
            }
            case MUTE -> {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                if (aranarthPlayer != null) {
                    aranarthPlayer.setMuteEndDate(muteEndDate);
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                    AranarthUtils.addMutedPlayer(uuid);
                    saveIfOnlineHere(uuid);
                }
                notifyMute(uuid, nickname, duration, reason);
            }
            case UNMUTE -> {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                if (aranarthPlayer != null) {
                    aranarthPlayer.setMuteEndDate("");
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                    AranarthUtils.removeMutedPlayer(uuid);
                    saveIfOnlineHere(uuid);
                }
                notifyUnmute(uuid);
            }
            case WARN -> notifyWarn(uuid, nickname, reason);
        }
    }

    /**
     * Provides whether the sender ranks above the target, so staff can never punish or change the ranks
     * of a peer or a superior.
     *
     * @param sender     The staff member using the command.
     * @param targetUuid The player being acted upon.
     * @return Whether the sender may act on the target.
     */
    public static boolean outranks(CommandSender sender, UUID targetUuid) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        AranarthPlayer senderPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        AranarthPlayer targetPlayer = AranarthUtils.getPlayer(targetUuid);
        if (targetPlayer == null) {
            return true;
        }
        int senderRank = senderPlayer == null ? 0 : senderPlayer.getCouncilRank();
        if (player.isOp()) {
            senderRank = Math.max(senderRank, 3);
        }
        return targetPlayer.getCouncilRank() < senderRank;
    }

    public static void notifyBan(String nickname, String duration, String reason, boolean isPermanent) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("aranarth.punishments")) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "ban.issued", "player", nickname, "duration", duration, "reason", reason)));
            } else {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "ban.permanent", "player", nickname, "reason", reason)));
            }
        }
    }

    public static void notifyMute(UUID targetUuid, String nickname, String duration, String reason) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(targetUuid)) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "mute.received", "duration", duration, "reason", reason)));
                onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_GHAST_HURT, 1F, 1.1F);
            } else if (onlinePlayer.hasPermission("aranarth.punishments")) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "mute.issued", "player", nickname, "duration", duration, "reason", reason)));
            } else {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "mute.permanent", "player", nickname, "reason", reason)));
            }
        }
    }

    public static void notifyUnmute(UUID targetUuid) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null) {
            target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "mute.unmuted")));
        }
    }

    public static void notifyWarn(UUID targetUuid, String nickname, String reason) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(targetUuid)) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "warn.received", "reason", reason)));
                onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_GHAST_HURT, 1F, 1.1F);
            } else if (onlinePlayer.hasPermission("aranarth.punishments")) {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "warn.issued", "player", nickname, "reason", reason)));
            } else {
                onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.getFor(onlinePlayer, "warn.issued_public", "player", nickname)));
            }
        }
    }

    /**
     * The message shown to a banned player who tries to join, or null if the ban has ended.
     *
     * @param uuid The UUID of the banned player.
     * @param ban {reason, expiresAt} as loaded from the database.
     */
    public static String getBanKickMessage(UUID uuid, String[] ban) {
        long expiresAt = Long.parseLong(ban[1]);
        if (expiresAt < 0) {
            return ChatUtils.translateToColor(Lang.getFor(uuid, "ban.kick_permanent", "reason", ban[0]));
        }
        if (expiresAt <= System.currentTimeMillis()) {
            return null;
        }
        LocalDateTime until = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAt), ZoneId.systemDefault());
        String untilText = until.getYear() + "-" + pad(until.getMonthValue()) + "-" + pad(until.getDayOfMonth())
                + " " + pad(until.getHour()) + ":" + pad(until.getMinute());
        return ChatUtils.translateToColor(Lang.getFor(uuid, "ban.kick_temporary", "until", untilText, "reason", ban[0]));
    }

    private static String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    /**
     * Saves the player's punishment history right away, so a periodic save of the other server cannot
     * overwrite it with a copy that is missing this punishment.
     */
    private static void savePunishmentsNow(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        String json = PersistenceUtils.buildPunishmentsJson(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                DatabaseManager.getInstance().savePunishments(uuid, json));
    }

    /**
     * Persists a mute change. The server the player is online on saves their whole row. If they are
     * offline everywhere, only the mute field of their stored row is changed, so a stale copy of the
     * rest of their data is never written.
     */
    private static void persistMute(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        if (Bukkit.getPlayer(uuid) != null) {
            PersistenceUtils.saveAranarthPlayerImmediately(uuid);
            return;
        }
        // The other server saves it when the punishment arrives there
        if (NetworkManager.isActive() && NetworkManager.getInstance().getRemotePlayer(uuid) != null) {
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        String muteEndDate = aranarthPlayer != null ? aranarthPlayer.getMuteEndDate() : "";
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                DatabaseManager.getInstance().setRawPlayerField(uuid, 15, muteEndDate));
    }

    private static void saveIfOnlineHere(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            PersistenceUtils.saveAranarthPlayerImmediately(uuid);
        }
    }
}
