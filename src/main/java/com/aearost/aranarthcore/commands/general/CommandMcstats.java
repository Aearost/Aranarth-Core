package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiMcstats;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.skills.SkillTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opens a GUI showing the mcMMO skill levels and leaderboard rankings of a player.
 */
public class CommandMcstats implements CommandExecutor {

    public static final List<PrimarySkillType> DISPLAY_SKILLS = new ArrayList<>();

    static {
        for (PrimarySkillType skill : PrimarySkillType.values()) {
            if (!SkillTools.isChildSkill(skill)) {
                DISPLAY_SKILLS.add(skill);
            }
        }
        DISPLAY_SKILLS.sort(Comparator.comparing(PrimarySkillType::name));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            openGui(viewer, viewer.getUniqueId(), viewer.getName(), true);
        } else {
            UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
            if (targetUuid == null) {
                viewer.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[0])));
                return true;
            }
            boolean isSelf = viewer.getUniqueId().equals(targetUuid);
            String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
            if (targetName == null) {
                targetName = args[0];
            }
            openGui(viewer, targetUuid, targetName, isSelf);
        }
        return true;
    }

    /**
     * Asynchronously fetches mcMMO skill levels and rankings, then opens the stats GUI.
     */
    public static void openGui(Player viewer, UUID targetUuid, String targetName, boolean isSelf) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            Map<PrimarySkillType, Integer> targetLevels = new LinkedHashMap<>();
            Map<PrimarySkillType, Integer> targetRanks = new LinkedHashMap<>();
            for (PrimarySkillType skill : DISPLAY_SKILLS) {
                try {
                    targetLevels.put(skill, ExperienceAPI.getLevelOffline(targetUuid, skill.name()));
                } catch (Exception ignored) {
                    targetLevels.put(skill, 0);
                }
                try {
                    targetRanks.put(skill, ExperienceAPI.getPlayerRankSkill(targetUuid, skill.name()));
                } catch (Exception ignored) {
                    targetRanks.put(skill, 0);
                }
            }

            int targetPowerLevel = 0;
            try {
                targetPowerLevel = ExperienceAPI.getPowerLevelOffline(targetUuid);
            } catch (Exception ignored) {}

            int targetOverallRank = 0;
            try {
                targetOverallRank = ExperienceAPI.getPlayerRankOverall(targetUuid);
            } catch (Exception ignored) {}

            Map<PrimarySkillType, Integer> viewerRanks = new LinkedHashMap<>();
            int viewerOverallRank = 0;
            if (!isSelf) {
                for (PrimarySkillType skill : DISPLAY_SKILLS) {
                    try {
                        viewerRanks.put(skill, ExperienceAPI.getPlayerRankSkill(viewer.getUniqueId(), skill.name()));
                    } catch (Exception ignored) {
                        viewerRanks.put(skill, 0);
                    }
                }
                try {
                    viewerOverallRank = ExperienceAPI.getPlayerRankOverall(viewer.getUniqueId());
                } catch (Exception ignored) {}
            }

            final int finalTargetPowerLevel = targetPowerLevel;
            final int finalTargetOverallRank = targetOverallRank;
            final int finalViewerOverallRank = viewerOverallRank;

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                new GuiMcstats(viewer, targetName, isSelf, targetLevels, finalTargetPowerLevel,
                        targetRanks, finalTargetOverallRank, viewerRanks, finalViewerOverallRank).openGui();
            });
        });
    }
}
