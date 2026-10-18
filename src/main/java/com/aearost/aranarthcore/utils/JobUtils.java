package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.JobData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
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
    // Builder pay is only given for blocks that are still in place after this delay
    public static final long BUILDER_PAY_DELAY_MS = 3 * 60 * 1000L;
    private static final ArrayDeque<PendingBuild> pendingBuilds = new ArrayDeque<>();

    /**
     * A placed block whose Builder pay is held until it has stayed in place long enough.
     */
    private record PendingBuild(UUID playerUuid, UUID worldUuid, int x, int y, int z, Material material,
                                double basePay, long placedAt) {
    }

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
        // Jobs only pay in the survival worlds, never in the arena, creative or other worlds
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            return;
        }
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

        String broadcastMsg = ChatUtils.chatMessage(Lang.get("jobs.level_up_broadcast", "player", nickname, "level", newLevel, "job", jobName));

        Bukkit.broadcastMessage(broadcastMsg);
    }

    /**
     * Holds the Builder pay for a placed block until it has stayed in place for BUILDER_PAY_DELAY_MS.
     */
    public static void queueBuilderPay(Player player, Block block, double basePay) {
        pendingBuilds.addLast(new PendingBuild(player.getUniqueId(), block.getWorld().getUID(), block.getX(), block.getY(),
                block.getZ(), block.getType(), basePay, System.currentTimeMillis()));
    }

    /**
     * Pays Builders for every held block that is still in place after the delay.
     */
    public static void processPendingBuilderPay() {
        long now = System.currentTimeMillis();
        Map<UUID, Double> payByPlayer = new HashMap<>();
        while (!pendingBuilds.isEmpty() && now - pendingBuilds.peekFirst().placedAt() >= BUILDER_PAY_DELAY_MS) {
            PendingBuild build = pendingBuilds.pollFirst();
            World world = Bukkit.getWorld(build.worldUuid());
            if (world == null) {
                continue;
            }
            // An unloaded chunk cannot have been changed since it unloaded, so only loaded chunks are checked
            if (world.isChunkLoaded(build.x() >> 4, build.z() >> 4)
                    && world.getBlockAt(build.x(), build.y(), build.z()).getType() != build.material()) {
                continue;
            }
            payByPlayer.merge(build.playerUuid(), build.basePay(), Double::sum);
        }
        for (Map.Entry<UUID, Double> entry : payByPlayer.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                awardJob(player, JobType.BUILDER, entry.getValue());
            }
        }
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
