package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiMcstats;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.api.ExperienceAPI;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Opens a GUI showing the mcMMO skill levels and leaderboard rankings of a player.
 */
public class CommandMcstats implements CommandExecutor {

    public static final List<PrimarySkillType> DISPLAY_SKILLS = new ArrayList<>();

    private static final long CATEGORY_CACHE_MS = 5 * 60 * 1000;
    private static final int LEADERBOARD_SIZE = 1_000_000;
    private static List<int[]> cachedCategoryTotals = null;
    private static long cachedCategoryTotalsTime = 0;

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
            sender.sendMessage(ChatUtils.chatMessage(Lang.getFor(sender, "general.no_permission_console")));
            return true;
        }

        if (args.length == 0) {
            openGui(viewer, viewer.getUniqueId(), viewer.getName(), true);
        } else {
            UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[0]);
            if (targetUuid == null) {
                viewer.sendMessage(ChatUtils.chatMessage(Lang.getFor(viewer, "player.not_found", "name", args[0])));
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
            long t0 = System.currentTimeMillis();
            Map<PrimarySkillType, Integer> targetLevels = new LinkedHashMap<>();
            Map<PrimarySkillType, Integer> targetRanks = new LinkedHashMap<>();
            for (PrimarySkillType skill : DISPLAY_SKILLS) {
                try {
                    targetLevels.put(skill, ExperienceAPI.getLevelOffline(targetUuid, skill.name()));
                } catch (Exception ignored) {
                    targetLevels.put(skill, 0);
                }
                long tRankStart = System.currentTimeMillis();
                try {
                    targetRanks.put(skill, ExperienceAPI.getPlayerRankSkill(targetUuid, skill.name()));
                } catch (Exception ignored) {
                    targetRanks.put(skill, 0);
                }
                long tRankMs = System.currentTimeMillis() - tRankStart;
                if (tRankMs > 100) {
                    AranarthCore.getInstance().getLogger().info("[mcstats] getPlayerRankSkill(" + skill.name() + ") took " + tRankMs + "ms");
                }
            }
            AranarthCore.getInstance().getLogger().info("[mcstats] skill loop took " + (System.currentTimeMillis() - t0) + "ms");

            long tPower = System.currentTimeMillis();
            int targetPowerLevel = 0;
            try {
                targetPowerLevel = ExperienceAPI.getPowerLevelOffline(targetUuid);
            } catch (Exception ignored) {}
            AranarthCore.getInstance().getLogger().info("[mcstats] getPowerLevelOffline took " + (System.currentTimeMillis() - tPower) + "ms");

            long tOverall = System.currentTimeMillis();
            int targetOverallRank = 0;
            try {
                targetOverallRank = ExperienceAPI.getPlayerRankOverall(targetUuid);
            } catch (Exception ignored) {}
            AranarthCore.getInstance().getLogger().info("[mcstats] getPlayerRankOverall took " + (System.currentTimeMillis() - tOverall) + "ms");

            // A category is ranked by how many players have a strictly higher total in it
            int[] categoryTotals = AranarthUtils.getMcmmoCategoryTotals(skill -> GuiMcstats.getLevel(skill, targetLevels));
            int[] categoryRanks = new int[categoryTotals.length];
            List<int[]> allCategoryTotals = getAllCategoryTotals();
            for (int i = 0; i < categoryTotals.length; i++) {
                if (categoryTotals[i] <= 0 || allCategoryTotals == null) {
                    continue;
                }
                int rank = 1;
                for (int[] otherTotals : allCategoryTotals) {
                    if (otherTotals[i] > categoryTotals[i]) {
                        rank++;
                    }
                }
                categoryRanks[i] = rank;
            }

            Map<PrimarySkillType, Integer> viewerRanks = new LinkedHashMap<>();
            if (!isSelf) {
                for (PrimarySkillType skill : DISPLAY_SKILLS) {
                    try {
                        viewerRanks.put(skill, ExperienceAPI.getPlayerRankSkill(viewer.getUniqueId(), skill.name()));
                    } catch (Exception ignored) {
                        viewerRanks.put(skill, 0);
                    }
                }
            }

            final int finalTargetPowerLevel = targetPowerLevel;
            final int finalTargetOverallRank = targetOverallRank;

            AranarthCore.getInstance().getLogger().info("[mcstats] total async time: " + (System.currentTimeMillis() - t0) + "ms");
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                new GuiMcstats(viewer, targetName, isSelf, targetLevels, finalTargetPowerLevel, finalTargetOverallRank,
                        categoryTotals, categoryRanks, targetRanks, viewerRanks).openGui();
            });
        });
    }

    /**
     * Provides the Gathering, Combat and Miscellaneous totals of every player in the mcMMO database.
     * @return The category totals of every player, or null if the leaderboards could not be read.
     */
    private static synchronized List<int[]> getAllCategoryTotals() {
        if (cachedCategoryTotals != null && System.currentTimeMillis() - cachedCategoryTotalsTime < CATEGORY_CACHE_MS) {
            return cachedCategoryTotals;
        }

        long tStart = System.currentTimeMillis();
        // Leaderboards are keyed by username, and only contain players with a level above 0 in that skill
        Map<String, Map<PrimarySkillType, Integer>> levelsByPlayer = new HashMap<>();
        try {
            for (PrimarySkillType skill : AranarthUtils.getMcmmoCategoryParentSkills()) {
                for (PlayerStat stat : mcMMO.getDatabaseManager().readLeaderboard(skill, 1, LEADERBOARD_SIZE)) {
                    levelsByPlayer.computeIfAbsent(stat.playerName().toLowerCase(), k -> new HashMap<>()).put(skill, stat.value());
                }
            }
        } catch (Exception e) {
            AranarthCore.getInstance().getLogger().warning("[mcstats] Failed to read the mcMMO leaderboards: " + e.getMessage());
            return cachedCategoryTotals;
        }

        List<int[]> allTotals = new ArrayList<>();
        for (Map<PrimarySkillType, Integer> levels : levelsByPlayer.values()) {
            allTotals.add(AranarthUtils.getMcmmoCategoryTotals(skill -> GuiMcstats.getLevel(skill, levels)));
        }
        cachedCategoryTotals = allTotals;
        cachedCategoryTotalsTime = System.currentTimeMillis();
        AranarthCore.getInstance().getLogger().info("[mcstats] category leaderboards took " + (cachedCategoryTotalsTime - tStart) + "ms");
        return allTotals;
    }
}
