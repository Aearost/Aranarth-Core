package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiMctop;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.gmail.nossr50.api.exceptions.InvalidSkillException;
import com.gmail.nossr50.datatypes.database.PlayerStat;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.skills.SkillTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opens a GUI showing the top players in a given mcMMO skill.
 */
public class CommandMctop implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.chatMessage("&cInvalid syntax: &e/mctop <skill>"));
            return true;
        }

        PrimarySkillType skill;
        try {
            skill = PrimarySkillType.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.chatMessage("&cUnknown skill &e" + args[0]));
            return true;
        }

        if (SkillTools.isChildSkill(skill)) {
            player.sendMessage(ChatUtils.chatMessage("&c" + args[0] + " &cis a child skill and does not have a leaderboard"));
            return true;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(0);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

        openGui(player, skill, 0);
        return true;
    }

    /**
     * Fetches the leaderboard of the input skill and the page number.
     */
    public static void openGui(Player player, PrimarySkillType skill, int pageNum) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            List<PlayerStat> leaderboard = new ArrayList<>();
            try {
                leaderboard = mcMMO.getDatabaseManager().readLeaderboard(skill, pageNum + 1, 45);
            } catch (InvalidSkillException ignored) {
            }

            // If the requested page is empty and it's not page 0, wrap to the first page
            if (leaderboard.isEmpty() && pageNum > 0) {
                openGui(player, skill, 0);
                return;
            }

            // Complete each player's profile
            Map<String, PlayerProfile> profiles = new LinkedHashMap<>();
            for (PlayerStat stat : leaderboard) {
                PlayerProfile profile = Bukkit.createProfile(null, stat.playerName());
                profile.complete(true);
                profiles.put(stat.playerName(), profile);
            }

            final List<PlayerStat> finalLeaderboard = leaderboard;
            final Map<String, PlayerProfile> finalProfiles = profiles;

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setCurrentGuiPageNum(pageNum);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                new GuiMctop(player, skill, pageNum, finalLeaderboard, finalProfiles).openGui();
            });
        });
    }

}
