package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.JobData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public class JobUtils {

    private static final double[] LEVEL_MULTIPLIERS = {
        1.00, 1.05, 1.12, 1.25, 1.50, 2.00, 3.00, 5.00, 7.50, 10.00
    };

    private static final long[] XP_REQUIRED = {
        500, 2000, 6000, 15000, 40000, 90000, 175000, 300000, 500000
    };

    // Builder anti-exploit: Map<playerUUID, Set<blockLocationKey>>
    private static final Map<UUID, Set<Long>> placedBlockLocations = new HashMap<>();

    public static int getMaxJobs(int rank) {
        if (rank >= 8) return 5;
        if (rank >= 6) return 4;
        if (rank >= 4) return 3;
        if (rank >= 2) return 2;
        return 1;
    }

    public static double getLevelMultiplier(int level) {
        int idx = Math.max(0, Math.min(level - 1, LEVEL_MULTIPLIERS.length - 1));
        return LEVEL_MULTIPLIERS[idx];
    }

    public static long getXpRequired(int level) {
        if (level <= 0 || level > 9) return -1;
        return XP_REQUIRED[level - 1];
    }

    public static void awardJob(Player player, JobType job, double basePay) {
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();
        if (!jobData.hasJob(job)) return;

        int level = jobData.getLevel(job);
        double multiplier = getLevelMultiplier(level);
        double actualPay = basePay * multiplier;

        ap.setBalance(ap.getBalance() + actualPay);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), actualPay);
        }

        double xpGain = actualPay * 100;
        double currentXp = jobData.getCurrentXp(job) + xpGain;

        if (level < 10) {
            long required = getXpRequired(level);
            while (level < 10 && currentXp >= required) {
                currentXp -= required;
                level++;
                jobData.setLevel(job, level);
                handleLevelUp(player, ap, job, level);
                if (level < 10) {
                    required = getXpRequired(level);
                } else {
                    currentXp = 0;
                    break;
                }
            }
        } else {
            currentXp = 0;
        }

        jobData.setCurrentXp(job, currentXp);
        AranarthUtils.setPlayer(player.getUniqueId(), ap);
    }

    public static void handleLevelUp(Player player, AranarthPlayer ap, JobType job, int newLevel) {
        String jobName = job.getDisplayName();
        String nickname = ap.getNickname();

        player.sendTitle(
            ChatUtils.translateToColor("&6&lLevel Up!"),
            ChatUtils.translateToColor("&e" + jobName + " &fis now &e&lLevel " + newLevel),
            10, 60, 20
        );

        String broadcastMsg = ChatUtils.chatMessage("&7" + nickname + " &7has reached &e&lLevel " + newLevel + " &7in &e" + jobName + "&7!");

        Bukkit.broadcastMessage(broadcastMsg);
    }

    public static void trackPlacedBlock(UUID playerUuid, long locationKey) {
        placedBlockLocations.computeIfAbsent(playerUuid, k -> new HashSet<>()).add(locationKey);
    }

    public static boolean isRecentlyPlaced(long locationKey) {
        for (Set<Long> blocks : placedBlockLocations.values()) {
            if (blocks.contains(locationKey)) return true;
        }
        return false;
    }

    public static void clearBuilderPlacedBlocks() {
        placedBlockLocations.clear();
    }

    public static long toLocationKey(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)(y & 0xFFF);
    }

    /**
     * Awards the Farmer job pay for a harvested crop block.
     * Called from CropHarvest (which cancels the BlockBreakEvent before JobEventListener sees it).
     */
    public static void awardFarmerCropHarvest(Player player, org.bukkit.Material cropType) {
        double pay = switch (cropType) {
            case WHEAT -> 0.10;
            case CARROTS -> 0.05;
            case POTATOES -> 0.05;
            case BEETROOTS -> 0.12;
            case NETHER_WART -> 0.15;
            case COCOA -> 0.10;
            case MELON -> 0.20;
            case PUMPKIN -> 0.20;
            case SWEET_BERRY_BUSH -> 0.08;
            case CAVE_VINES, CAVE_VINES_PLANT -> 0.06;
            case SUGAR_CANE -> 0.04;
            case CACTUS -> 0.04;
            default -> 0;
        };
        if (pay > 0) awardJob(player, JobType.FARMER, pay);
    }

    public static String formatPay(double amount) {
        return String.format("$%.2f", amount);
    }

    public static String getJobDescription(JobType job) {
        return switch (job) {
            case BUILDER -> "Place blocks to build the world around you";
            case FARMER -> "Harvest crops, tend animals, and collect honey";
            case MINER -> "Mine ores, stone, and precious materials underground";
            case EXCAVATOR -> "Dig up dirt, sand, gravel, clay, and ancient artifacts";
            case LUMBERJACK -> "Chop wood, strip logs, and craft wooden items";
            case SMITH -> "Craft tools, armor, and metallic items";
            case EXPLORER -> "Travel the world, ride horses, and open new chests";
            case ALCHEMIST -> "Brew potions, enchant items, and visit the grindstone";
            case HUNTER -> "Hunt mobs, catch fish, and take on powerful enemies";
        };
    }

    public static String getJobColor(JobType job) {
        return switch (job) {
            case BUILDER -> "&f&l";
            case MINER -> "&8&l";
            case EXCAVATOR -> "&6&l";
            case LUMBERJACK -> "&4&l";
            case FARMER -> "&e&l";
            case HUNTER -> "&c&l";
            case SMITH -> "&7&l";
            case ALCHEMIST -> "&5&l";
            case EXPLORER -> "&b&l";
        };
    }

    public static org.bukkit.Material getJobIcon(JobType job) {
        return switch (job) {
            case BUILDER -> org.bukkit.Material.BRICKS;
            case FARMER -> org.bukkit.Material.WHEAT;
            case MINER -> org.bukkit.Material.IRON_PICKAXE;
            case EXCAVATOR -> org.bukkit.Material.IRON_SHOVEL;
            case LUMBERJACK -> org.bukkit.Material.OAK_LOG;
            case SMITH -> org.bukkit.Material.ANVIL;
            case EXPLORER -> org.bukkit.Material.COMPASS;
            case ALCHEMIST -> org.bukkit.Material.BREWING_STAND;
            case HUNTER -> org.bukkit.Material.BOW;
        };
    }
}
