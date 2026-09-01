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
            200000, 275000, 375000, 515000, 700000, 950000, 1300000, 1800000, 2500000
    };

    // Builder anti-exploit: Map<playerUUID, Set<blockLocationKey>>
    private static final Map<UUID, Set<Long>> placedBlockLocations = new HashMap<>();

    public static int getMaxJobs(int rank) {
        if (rank >= 8) {
            return 5;
        }
        if (rank >= 6) {
            return 4;
        }
        if (rank >= 4) {
            return 3;
        }
        if (rank >= 2) {
            return 2;
        }
        return 1;
    }

    public static double getLevelMultiplier(int level) {
        int idx = Math.max(0, Math.min(level - 1, LEVEL_MULTIPLIERS.length - 1));
        return LEVEL_MULTIPLIERS[idx];
    }

    public static int computeLevel(double totalXp) {
        int level = 1;
        double remaining = totalXp;
        for (long threshold : XP_REQUIRED) {
            if (remaining < threshold) {
                break;
            }
            remaining -= threshold;
            level++;
            if (level >= 10) {
                return 10;
            }
        }
        return level;
    }

    public static double computeWithinLevelXp(double totalXp) {
        double remaining = totalXp;
        for (long threshold : XP_REQUIRED) {
            if (remaining < threshold) {
                return remaining;
            }
            remaining -= threshold;
        }
        return 0;
    }

    public static long getXpRequired(int level) {
        if (level <= 0 || level > 9) {
            return -1;
        }
        return XP_REQUIRED[level - 1];
    }

    public static double getRankPayMultiplier(int rank) {
        return switch (rank) {
            case 0 -> 1.00;
            case 1 -> 1.05;
            case 2 -> 1.15;
            case 3 -> 1.30;
            case 4 -> 1.50;
            case 5 -> 1.75;
            case 6 -> 2.00;
            case 7 -> 2.25;
            default -> 2.50;
        };
    }

    public static void awardJob(Player player, JobType job, double basePay) {
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) {
            return;
        }
        JobData jobData = ap.getJobData();
        if (!jobData.hasJob(job)) {
            return;
        }

        int level = jobData.getLevel(job);
        double levelMultiplier = getLevelMultiplier(level);
        double rankMultiplier = getRankPayMultiplier(ap.getRank());
        double actualPay = basePay * levelMultiplier * rankMultiplier;

        ap.setBalance(ap.getBalance() + actualPay);
        PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), actualPay);
        }

        if (level < 10) {
            double xpGain = basePay * 100;
            jobData.addTotalXp(job, xpGain);
            int newLevel = jobData.getLevel(job);
            for (int lvl = level + 1; lvl <= newLevel; lvl++) {
                handleLevelUp(player, ap, job, lvl);
            }
        }

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
            if (blocks.contains(locationKey)) {
                return true;
            }
        }
        return false;
    }

    public static void clearBuilderPlacedBlocks() {
        placedBlockLocations.clear();
    }

    public static long toLocationKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) (y & 0xFFF);
    }

    /**
     * Awards the Farmer job pay for a harvested crop block.
     * Called from CropHarvest (which cancels the BlockBreakEvent before JobEventListener sees it).
     */
    public static void awardFarmerCropHarvest(Player player, org.bukkit.Material cropType) {
        double pay = switch (cropType) {
            case WHEAT -> 1.20;
            case CARROTS -> 0.60;
            case POTATOES -> 0.60;
            case BEETROOTS -> 1.45;
            case NETHER_WART -> 1.80;
            case COCOA -> 1.20;
            case MELON -> 2.40;
            case PUMPKIN -> 2.40;
            case SWEET_BERRY_BUSH -> 0.95;
            case CAVE_VINES, CAVE_VINES_PLANT -> 0.73;
            case SUGAR_CANE -> 1.90;
            case CACTUS -> 1.90;
            default -> 0;
        };
        if (pay > 0) {
            awardJob(player, JobType.FARMER, pay);
        }
    }

    public static String getJobDescription(JobType job) {
        return switch (job) {
            case BUILDER -> "Place blocks to build the world around you";
            case FARMER -> "Harvest crops, tend animals, and collect honey";
            case MINER -> "Mine ores, stone, and precious materials underground";
            case EXCAVATOR -> "Dig up dirt, sand, gravel, clay, and ancient artifacts";
            case LUMBERJACK -> "Chop wood, strip logs, and craft wooden items";
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
            case EXPLORER -> org.bukkit.Material.COMPASS;
            case ALCHEMIST -> org.bukkit.Material.BREWING_STAND;
            case HUNTER -> org.bukkit.Material.BOW;
        };
    }
}
