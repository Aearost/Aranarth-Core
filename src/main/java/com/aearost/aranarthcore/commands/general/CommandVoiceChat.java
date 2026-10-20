package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.voice.VoiceChannel;
import com.aearost.aranarthcore.voice.VoiceChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Joins or leaves a voice chat channel if the player has the Simple Voice Chat mod.
 */
public class CommandVoiceChat implements CommandExecutor {

    /**
     * @param sender  The user that entered the command.
     * @param command The command itself.
     * @param alias   The alias of the command.
     * @param args    The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return false;
        }

        if (!VoiceChatManager.isAvailable()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.unavailable")));
            return true;
        }

        VoiceChannel currentChannel = VoiceChatManager.getChannel(player.getUniqueId());

        if (args.length == 0) {
            if (currentChannel == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.status_none")));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.status", "channel", Lang.getFor(player, currentChannel.getLangKey()))));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("who")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.who_header")));
            // Matches tab list vanish visibility - only admins can see vanished players
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            boolean canSeeVanished = aranarthPlayer != null && aranarthPlayer.getCouncilRank() == 3;
            boolean canUseCouncil = VoiceChatManager.canUseCouncil(player);
            for (VoiceChannel channel : VoiceChannel.values()) {
                // The council channel is only shown to those who can join it
                if (channel == VoiceChannel.COUNCIL && !canUseCouncil) {
                    continue;
                }
                List<String> nicknames = VoiceChatManager.getNicknamesInChannel(channel, canSeeVanished);
                String list;
                if (nicknames.isEmpty()) {
                    list = Lang.getFor(player, "voicechat.who_empty");
                } else {
                    List<String> formatted = new ArrayList<>();
                    for (String nickname : nicknames) {
                        formatted.add(Lang.getFor(player, "voicechat.who_player", "player", nickname));
                    }
                    list = String.join(Lang.getFor(player, "voicechat.who_separator"), formatted);
                }
                player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "voicechat.who_channel",
                        "channel", Lang.getFor(player, channel.getLangKey()), "list", list)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            if (currentChannel == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.not_in_channel")));
            } else {
                VoiceChatManager.leaveChannel(player, true);
            }
            return true;
        }

        VoiceChannel channel = VoiceChannel.fromName(args[0]);
        if (channel == null) {
            String usage = VoiceChatManager.canUseCouncil(player)
                    ? "vc <global|local|dominion|council|leave|who>" : "vc <global|local|dominion|leave|who>";
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.invalid_syntax", "usage", usage)));
            return true;
        }
        if (channel == VoiceChannel.COUNCIL && !VoiceChatManager.canUseCouncil(player)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
            return true;
        }

        if (!VoiceChatManager.isInstalled(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.no_mod")));
            return true;
        }
        if (!VoiceChatManager.isConnected(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.not_connected")));
            return true;
        }
        if (channel == currentChannel) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "voicechat.already_in", "channel", Lang.getFor(player, channel.getLangKey()))));
            return true;
        }
        if (channel == VoiceChannel.DOMINION && DominionUtils.getPlayerDominion(player.getUniqueId()) == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.not_in_dominion")));
            return true;
        }

        VoiceChatManager.joinChannel(player, channel);
        return true;
    }
}
