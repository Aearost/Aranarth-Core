package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Mail;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MailUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Allows players to send and read in-game mail.
 */
public class CommandMail implements CommandExecutor {

    private static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "mail <send|read|clear|clearall>")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "send" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "mail send <player> <message>")));
                    return true;
                }

                UUID targetUUID = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
                if (targetUUID == null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                    return true;
                }

                AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUUID);
                String targetDisplayName;
                if (targetAranarthPlayer != null) {
                    targetDisplayName = targetAranarthPlayer.getNickname();
                } else {
                    com.aearost.aranarthcore.network.NetworkPlayer remoteTarget = NetworkManager.isActive()
                            ? NetworkManager.getInstance().getRemotePlayer(targetUUID) : null;
                    if (remoteTarget != null) {
                        String nick = remoteTarget.getNickname();
                        targetDisplayName = (nick == null || nick.isEmpty()) ? remoteTarget.getUsername() : ChatUtils.stripColorFormatting(nick);
                    } else {
                        String bukkit = Bukkit.getOfflinePlayer(targetUUID).getName();
                        targetDisplayName = bukkit != null ? bukkit : args[1];
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) sb.append(" ");
                    sb.append(args[i]);
                }
                String rawMessage = sb.toString();

                String processedMessage;
                if (player.hasPermission("aranarth.chat.hex")) {
                    processedMessage = ChatUtils.translateToColor(rawMessage);
                } else if (player.hasPermission("aranarth.chat.color")) {
                    String colored = ChatUtils.playerColorChat(rawMessage);
                    processedMessage = (colored != null) ? colored : rawMessage;
                } else {
                    processedMessage = rawMessage;
                }

                MailUtils.addMail(targetUUID, new Mail(player.getUniqueId(), targetUUID, System.currentTimeMillis(), processedMessage));
                AranarthPlayer senderPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.sent", "player", targetDisplayName, "message", processedMessage)));
                if (Bukkit.getOfflinePlayer(targetUUID).isOnline()) {
                    // Recipient is on this server - notify directly
                    Player target = Bukkit.getPlayer(targetUUID);
                    target.sendMessage(ChatUtils.chatMessage(Lang.get("mail.received", "sender", senderPlayer.getNickname())));
                    target.sendMessage(ChatUtils.chatMessage(Lang.get("mail.view_prompt")));
                } else if (NetworkManager.isActive()
                        && NetworkManager.getInstance().getRemotePlayer(targetUUID) != null) {
                    // Recipient is on a remote server
                    final UUID recipientUUID = targetUUID;
                    final String senderNickname = senderPlayer.getNickname();
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
                        PersistenceUtils.syncPlayerMailToDatabaseNow(recipientUUID);
                        NetworkManager.getInstance().publishMailNotification(recipientUUID, senderNickname);
                    });
                }
            }
            case "read" -> {
                List<Mail> mailList = new ArrayList<>(MailUtils.getMail(player.getUniqueId()));
                if (mailList.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.no_mail")));
                    return true;
                }

                int totalPages = (int) Math.ceil(mailList.size() / (double) PAGE_SIZE);
                int page = 1;

                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
                        return true;
                    }
                }

                if (page < 1 || page > totalPages) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.invalid_page", "pages", String.valueOf(totalPages))));
                    return true;
                }

                player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lMail &e(Page " + page + "/" + totalPages + ") &8- - -"));

                int start = (page - 1) * PAGE_SIZE;
                int end = Math.min(start + PAGE_SIZE, mailList.size());

                for (int i = start; i < end; i++) {
                    Mail mail = mailList.get(i);
                    int entryNum = i + 1;

                    AranarthPlayer senderPlayer = AranarthUtils.getPlayer(mail.getSenderUUID());
                    String senderName;
                    if (senderPlayer != null) {
                        senderName = senderPlayer.getNickname();
                    } else {
                        com.aearost.aranarthcore.network.NetworkPlayer remoteSender = NetworkManager.isActive()
                                ? NetworkManager.getInstance().getRemotePlayer(mail.getSenderUUID()) : null;
                        if (remoteSender != null) {
                            String nick = remoteSender.getNickname();
                            senderName = (nick == null || nick.isEmpty()) ? remoteSender.getUsername() : ChatUtils.stripColorFormatting(nick);
                        } else {
                            String bukkit = Bukkit.getOfflinePlayer(mail.getSenderUUID()).getName();
                            senderName = bukkit != null ? bukkit : "Unknown";
                        }
                    }

                    LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(mail.getTimestamp()),
                            ZoneId.systemDefault()
                    );
                    String date = dateTime.format(DATE_FORMAT);
                    String time = dateTime.format(TIME_FORMAT);

                    player.sendMessage(ChatUtils.translateToColor(
                            "&8[&6" + entryNum + "&8] &e" + senderName + " &7- &6" + date + " &eat &6" + time
                    ));
                    player.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + mail.getMessage());
                }
            }
            case "clear" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "mail clear <number>")));
                    return true;
                }
                List<Mail> mailList = MailUtils.getMail(player.getUniqueId());
                if (mailList.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.no_mail")));
                    return true;
                }
                int num;
                try {
                    num = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
                    return true;
                }
                if (num < 1 || num > mailList.size()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.not_found", "number", String.valueOf(num))));
                    return true;
                }
                MailUtils.removeMail(player.getUniqueId(), num - 1);
                player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.deleted", "number", String.valueOf(num))));
                PersistenceUtils.syncPlayerMailToDatabase(player.getUniqueId());
            }
            case "clearall" -> {
                if (MailUtils.getMail(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.no_mail")));
                    return true;
                }
                MailUtils.clearMail(player.getUniqueId());
                player.sendMessage(ChatUtils.chatMessage(Lang.get("mail.all_deleted")));
                PersistenceUtils.syncPlayerMailToDatabase(player.getUniqueId());
            }
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "mail <send|read|clear|clearall>")));
        }

        return true;
    }
}
