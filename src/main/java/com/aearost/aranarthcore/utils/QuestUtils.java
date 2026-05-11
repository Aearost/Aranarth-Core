package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.aearost.aranarthcore.objects.CustomKeys.QUEST_NPC;

/**
 * Centralized utility class for all quest-related functionality.
 */
public class QuestUtils {

    // All possible quests per rank: rank (0-8) -> list of quests
    private static final HashMap<Integer, List<Quest>> dailyQuestPool = new HashMap<>();
    private static final HashMap<Integer, List<Quest>> weeklyQuestPool = new HashMap<>();

    // Per-player active quests (lazily assigned from pool based on rank)
    private static final HashMap<UUID, List<Quest>> playerActiveDailyQuests = new HashMap<>();
    private static final HashMap<UUID, List<Quest>> playerActiveWeeklyQuests = new HashMap<>();

    // Player quest progress: UUID -> int[3] progress per active quest
    private static final HashMap<UUID, int[]> playerDailyProgress = new HashMap<>();
    private static final HashMap<UUID, boolean[]> playerDailyCompleted = new HashMap<>();
    private static final HashMap<UUID, boolean[]> playerDailyClaimed = new HashMap<>();
    private static final HashMap<UUID, int[]> playerWeeklyProgress = new HashMap<>();
    private static final HashMap<UUID, boolean[]> playerWeeklyCompleted = new HashMap<>();
    private static final HashMap<UUID, boolean[]> playerWeeklyClaimed = new HashMap<>();

    // Tracks which rank the player's progress data applies to
    private static final HashMap<UUID, Integer> playerQuestRank = new HashMap<>();

    // Reset timestamps (milliseconds since epoch)
    private static long lastDailyReset = 0;
    private static long lastWeeklyReset = 0;

    private static final ZoneId EST = ZoneId.of("America/New_York");
    private static final Random RANDOM = new Random();
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getInstance();

    static {
        MONEY_FORMAT.setGroupingUsed(true);
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Initializes all quest pools. Per-player quest assignments are loaded from
     * persistence or assigned lazily when players first interact with the system.
     */
    public static void initialize() {
        initializeQuestPools();
    }

    private static void initializeQuestPools() {
        // Rewards are all 0.0 — they are randomized per-player at assignment time.

        // ---- RANK 0 (getRank() == 0) — Starter ----
        List<Quest> r0d = new ArrayList<>();
        r0d.add(new Quest(QuestTaskType.BREAK_LOG, 25, 0.0, QuestType.DAILY, 0, "Break 25 Logs"));
        r0d.add(new Quest(QuestTaskType.MINE_STONE, 50, 0.0, QuestType.DAILY, 0, "Mine 50 Stone"));
        r0d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 8, 0.0, QuestType.DAILY, 0, "Kill 8 Hostile Mobs"));
        r0d.add(new Quest(QuestTaskType.KILL_PASSIVE_MOB, 5, 0.0, QuestType.DAILY, 0, "Kill 5 Passive Mobs"));
        r0d.add(new Quest(QuestTaskType.CRAFT_PLANKS, 32, 0.0, QuestType.DAILY, 0, "Craft 32 Planks"));
        r0d.add(new Quest(QuestTaskType.CRAFT_TORCHES, 16, 0.0, QuestType.DAILY, 0, "Craft 16 Torches"));
        r0d.add(new Quest(QuestTaskType.HARVEST_CROPS, 32, 0.0, QuestType.DAILY, 0, "Harvest 32 Crops"));
        r0d.add(new Quest(QuestTaskType.HARVEST_CROPS, 64, 0.0, QuestType.DAILY, 0, "Harvest 64 Crops"));
        r0d.add(new Quest(QuestTaskType.FISH, 5, 0.0, QuestType.DAILY, 0, "Fish 5 Fish"));
        r0d.add(new Quest(QuestTaskType.COOK_FOOD, 16, 0.0, QuestType.DAILY, 0, "Cook 16 Food"));
        r0d.add(new Quest(QuestTaskType.BREED_ANIMALS, 3, 0.0, QuestType.DAILY, 0, "Breed 3 Animals"));
        r0d.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 8, 0.0, QuestType.DAILY, 0, "Kill 8 Mobs with a Sword"));
        r0d.add(new Quest(QuestTaskType.BREAK_SAND, 32, 0.0, QuestType.DAILY, 0, "Break 32 Sand"));
        r0d.add(new Quest(QuestTaskType.BREAK_DIRT, 32, 0.0, QuestType.DAILY, 0, "Break 32 Dirt"));
        r0d.add(new Quest(QuestTaskType.TRAVEL_BLOCKS, 500, 0.0, QuestType.DAILY, 0, "Travel 500 Blocks"));
        r0d.add(new Quest(QuestTaskType.KILL_PLAYER, 1, 0.0, QuestType.DAILY, 0, "Kill 1 Player"));
        dailyQuestPool.put(0, r0d);

        List<Quest> r0w = new ArrayList<>();
        r0w.add(new Quest(QuestTaskType.BREAK_LOG, 250, 0.0, QuestType.WEEKLY, 0, "Break 250 Logs"));
        r0w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 60, 0.0, QuestType.WEEKLY, 0, "Kill 60 Hostile Mobs"));
        r0w.add(new Quest(QuestTaskType.HARVEST_CROPS, 150, 0.0, QuestType.WEEKLY, 0, "Harvest 150 Crops"));
        r0w.add(new Quest(QuestTaskType.FISH, 20, 0.0, QuestType.WEEKLY, 0, "Fish 20 Fish"));
        r0w.add(new Quest(QuestTaskType.BREED_ANIMALS, 10, 0.0, QuestType.WEEKLY, 0, "Breed 10 Animals"));
        r0w.add(new Quest(QuestTaskType.KILL_PLAYER, 2, 0.0, QuestType.WEEKLY, 0, "Kill 2 Players"));
        weeklyQuestPool.put(0, r0w);

        // ---- RANK 1 (getRank() == 1) — $250 ----
        List<Quest> r1d = new ArrayList<>();
        r1d.add(new Quest(QuestTaskType.MINE_STONE, 128, 0.0, QuestType.DAILY, 1, "Mine 128 Stone"));
        r1d.add(new Quest(QuestTaskType.BREAK_LOG, 64, 0.0, QuestType.DAILY, 1, "Break 64 Logs"));
        r1d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 20, 0.0, QuestType.DAILY, 1, "Kill 20 Hostile Mobs"));
        r1d.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 12, 0.0, QuestType.DAILY, 1, "Kill 12 Mobs with a Sword"));
        r1d.add(new Quest(QuestTaskType.CRAFT_TORCHES, 64, 0.0, QuestType.DAILY, 1, "Craft 64 Torches"));
        r1d.add(new Quest(QuestTaskType.HARVEST_CROPS, 64, 0.0, QuestType.DAILY, 1, "Harvest 64 Crops"));
        r1d.add(new Quest(QuestTaskType.BREED_ANIMALS, 5, 0.0, QuestType.DAILY, 1, "Breed 5 Animals"));
        r1d.add(new Quest(QuestTaskType.FISH, 8, 0.0, QuestType.DAILY, 1, "Fish 8 Fish"));
        r1d.add(new Quest(QuestTaskType.COOK_FOOD, 32, 0.0, QuestType.DAILY, 1, "Cook 32 Food"));
        r1d.add(new Quest(QuestTaskType.BREAK_GRAVEL, 48, 0.0, QuestType.DAILY, 1, "Break 48 Gravel"));
        r1d.add(new Quest(QuestTaskType.MINE_COAL_ORE, 16, 0.0, QuestType.DAILY, 1, "Mine 16 Coal Ore"));
        r1d.add(new Quest(QuestTaskType.KILL_SKELETON, 10, 0.0, QuestType.DAILY, 1, "Kill 10 Skeletons"));
        r1d.add(new Quest(QuestTaskType.KILL_ZOMBIE, 10, 0.0, QuestType.DAILY, 1, "Kill 10 Zombies"));
        r1d.add(new Quest(QuestTaskType.BREAK_SAND, 64, 0.0, QuestType.DAILY, 1, "Break 64 Sand"));
        r1d.add(new Quest(QuestTaskType.CRAFT_GLASS, 16, 0.0, QuestType.DAILY, 1, "Craft 16 Glass"));
        r1d.add(new Quest(QuestTaskType.KILL_PLAYER, 1, 0.0, QuestType.DAILY, 1, "Kill 1 Player"));
        dailyQuestPool.put(1, r1d);

        List<Quest> r1w = new ArrayList<>();
        r1w.add(new Quest(QuestTaskType.MINE_COAL_ORE, 64, 0.0, QuestType.WEEKLY, 1, "Mine 64 Coal Ore"));
        r1w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 120, 0.0, QuestType.WEEKLY, 1, "Kill 120 Hostile Mobs"));
        r1w.add(new Quest(QuestTaskType.HARVEST_CROPS, 350, 0.0, QuestType.WEEKLY, 1, "Harvest 350 Crops"));
        r1w.add(new Quest(QuestTaskType.BREED_ANIMALS, 20, 0.0, QuestType.WEEKLY, 1, "Breed 20 Animals"));
        r1w.add(new Quest(QuestTaskType.COOK_FOOD, 96, 0.0, QuestType.WEEKLY, 1, "Cook 96 Food"));
        r1w.add(new Quest(QuestTaskType.KILL_PLAYER, 3, 0.0, QuestType.WEEKLY, 1, "Kill 3 Players"));
        weeklyQuestPool.put(1, r1w);

        // ---- RANK 2 (getRank() == 2) — $1,250 ----
        List<Quest> r2d = new ArrayList<>();
        r2d.add(new Quest(QuestTaskType.MINE_STONE, 256, 0.0, QuestType.DAILY, 2, "Mine 256 Stone"));
        r2d.add(new Quest(QuestTaskType.MINE_COAL_ORE, 32, 0.0, QuestType.DAILY, 2, "Mine 32 Coal Ore"));
        r2d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 35, 0.0, QuestType.DAILY, 2, "Kill 35 Hostile Mobs"));
        r2d.add(new Quest(QuestTaskType.KILL_WITH_BOW, 15, 0.0, QuestType.DAILY, 2, "Kill 15 Mobs with a Bow"));
        r2d.add(new Quest(QuestTaskType.KILL_SKELETON, 12, 0.0, QuestType.DAILY, 2, "Kill 12 Skeletons"));
        r2d.add(new Quest(QuestTaskType.HARVEST_CROPS, 96, 0.0, QuestType.DAILY, 2, "Harvest 96 Crops"));
        r2d.add(new Quest(QuestTaskType.BREED_ANIMALS, 8, 0.0, QuestType.DAILY, 2, "Breed 8 Animals"));
        r2d.add(new Quest(QuestTaskType.FISH, 12, 0.0, QuestType.DAILY, 2, "Fish 12 Fish"));
        r2d.add(new Quest(QuestTaskType.CRAFT_BREAD, 32, 0.0, QuestType.DAILY, 2, "Craft 32 Bread"));
        r2d.add(new Quest(QuestTaskType.BREAK_SAND, 96, 0.0, QuestType.DAILY, 2, "Break 96 Sand"));
        r2d.add(new Quest(QuestTaskType.CRAFT_GLASS, 32, 0.0, QuestType.DAILY, 2, "Craft 32 Glass"));
        r2d.add(new Quest(QuestTaskType.KILL_CREEPER, 5, 0.0, QuestType.DAILY, 2, "Kill 5 Creepers"));
        r2d.add(new Quest(QuestTaskType.MINE_IRON_ORE, 16, 0.0, QuestType.DAILY, 2, "Mine 16 Iron Ore"));
        r2d.add(new Quest(QuestTaskType.COOK_FOOD, 64, 0.0, QuestType.DAILY, 2, "Cook 64 Food"));
        r2d.add(new Quest(QuestTaskType.BREAK_LOG, 96, 0.0, QuestType.DAILY, 2, "Break 96 Logs"));
        r2d.add(new Quest(QuestTaskType.KILL_PLAYER, 2, 0.0, QuestType.DAILY, 2, "Kill 2 Players"));
        dailyQuestPool.put(2, r2d);

        List<Quest> r2w = new ArrayList<>();
        r2w.add(new Quest(QuestTaskType.MINE_IRON_ORE, 64, 0.0, QuestType.WEEKLY, 2, "Mine 64 Iron Ore"));
        r2w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 200, 0.0, QuestType.WEEKLY, 2, "Kill 200 Hostile Mobs"));
        r2w.add(new Quest(QuestTaskType.KILL_WITH_BOW, 50, 0.0, QuestType.WEEKLY, 2, "Kill 50 Mobs with a Bow"));
        r2w.add(new Quest(QuestTaskType.HARVEST_CROPS, 500, 0.0, QuestType.WEEKLY, 2, "Harvest 500 Crops"));
        r2w.add(new Quest(QuestTaskType.FISH, 50, 0.0, QuestType.WEEKLY, 2, "Fish 50 Fish"));
        r2w.add(new Quest(QuestTaskType.KILL_PLAYER, 5, 0.0, QuestType.WEEKLY, 2, "Kill 5 Players"));
        weeklyQuestPool.put(2, r2w);

        // ---- RANK 3 (getRank() == 3) — $5,000 ----
        List<Quest> r3d = new ArrayList<>();
        r3d.add(new Quest(QuestTaskType.MINE_IRON_ORE, 48, 0.0, QuestType.DAILY, 3, "Mine 48 Iron Ore"));
        r3d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 50, 0.0, QuestType.DAILY, 3, "Kill 50 Hostile Mobs"));
        r3d.add(new Quest(QuestTaskType.KILL_WITH_BOW, 20, 0.0, QuestType.DAILY, 3, "Kill 20 Mobs with a Bow"));
        r3d.add(new Quest(QuestTaskType.KILL_CREEPER, 10, 0.0, QuestType.DAILY, 3, "Kill 10 Creepers"));
        r3d.add(new Quest(QuestTaskType.HARVEST_CROPS, 128, 0.0, QuestType.DAILY, 3, "Harvest 128 Crops"));
        r3d.add(new Quest(QuestTaskType.BREED_ANIMALS, 10, 0.0, QuestType.DAILY, 3, "Breed 10 Animals"));
        r3d.add(new Quest(QuestTaskType.FISH, 15, 0.0, QuestType.DAILY, 3, "Fish 15 Fish"));
        r3d.add(new Quest(QuestTaskType.CRAFT_BREAD, 48, 0.0, QuestType.DAILY, 3, "Craft 48 Bread"));
        r3d.add(new Quest(QuestTaskType.BREAK_LOG, 128, 0.0, QuestType.DAILY, 3, "Break 128 Logs"));
        r3d.add(new Quest(QuestTaskType.MINE_GOLD_ORE, 16, 0.0, QuestType.DAILY, 3, "Mine 16 Gold Ore"));
        r3d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 6, 0.0, QuestType.DAILY, 3, "Kill 6 Endermen"));
        r3d.add(new Quest(QuestTaskType.COOK_FOOD, 96, 0.0, QuestType.DAILY, 3, "Cook 96 Food"));
        r3d.add(new Quest(QuestTaskType.BREAK_GRAVEL, 64, 0.0, QuestType.DAILY, 3, "Break 64 Gravel"));
        r3d.add(new Quest(QuestTaskType.CRAFT_IRON_INGOTS, 32, 0.0, QuestType.DAILY, 3, "Craft 32 Iron Ingots"));
        r3d.add(new Quest(QuestTaskType.KILL_ZOMBIE, 30, 0.0, QuestType.DAILY, 3, "Kill 30 Zombies"));
        r3d.add(new Quest(QuestTaskType.KILL_PLAYER, 2, 0.0, QuestType.DAILY, 3, "Kill 2 Players"));
        dailyQuestPool.put(3, r3d);

        List<Quest> r3w = new ArrayList<>();
        r3w.add(new Quest(QuestTaskType.MINE_IRON_ORE, 128, 0.0, QuestType.WEEKLY, 3, "Mine 128 Iron Ore"));
        r3w.add(new Quest(QuestTaskType.MINE_GOLD_ORE, 48, 0.0, QuestType.WEEKLY, 3, "Mine 48 Gold Ore"));
        r3w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 350, 0.0, QuestType.WEEKLY, 3, "Kill 350 Hostile Mobs"));
        r3w.add(new Quest(QuestTaskType.KILL_ENDERMAN, 20, 0.0, QuestType.WEEKLY, 3, "Kill 20 Endermen"));
        r3w.add(new Quest(QuestTaskType.HARVEST_CROPS, 800, 0.0, QuestType.WEEKLY, 3, "Harvest 800 Crops"));
        r3w.add(new Quest(QuestTaskType.KILL_PLAYER, 8, 0.0, QuestType.WEEKLY, 3, "Kill 8 Players"));
        weeklyQuestPool.put(3, r3w);

        // ---- RANK 4 (getRank() == 4) — $25,000 ----
        List<Quest> r4d = new ArrayList<>();
        r4d.add(new Quest(QuestTaskType.MINE_IRON_ORE, 96, 0.0, QuestType.DAILY, 4, "Mine 96 Iron Ore"));
        r4d.add(new Quest(QuestTaskType.MINE_GOLD_ORE, 32, 0.0, QuestType.DAILY, 4, "Mine 32 Gold Ore"));
        r4d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 75, 0.0, QuestType.DAILY, 4, "Kill 75 Hostile Mobs"));
        r4d.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 30, 0.0, QuestType.DAILY, 4, "Kill 30 Mobs with a Sword"));
        r4d.add(new Quest(QuestTaskType.KILL_CREEPER, 15, 0.0, QuestType.DAILY, 4, "Kill 15 Creepers"));
        r4d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 10, 0.0, QuestType.DAILY, 4, "Kill 10 Endermen"));
        r4d.add(new Quest(QuestTaskType.HARVEST_CROPS, 160, 0.0, QuestType.DAILY, 4, "Harvest 160 Crops"));
        r4d.add(new Quest(QuestTaskType.BREED_ANIMALS, 12, 0.0, QuestType.DAILY, 4, "Breed 12 Animals"));
        r4d.add(new Quest(QuestTaskType.FISH, 20, 0.0, QuestType.DAILY, 4, "Fish 20 Fish"));
        r4d.add(new Quest(QuestTaskType.CRAFT_IRON_INGOTS, 64, 0.0, QuestType.DAILY, 4, "Craft 64 Iron Ingots"));
        r4d.add(new Quest(QuestTaskType.BREAK_LOG, 160, 0.0, QuestType.DAILY, 4, "Break 160 Logs"));
        r4d.add(new Quest(QuestTaskType.MINE_DIAMOND, 12, 0.0, QuestType.DAILY, 4, "Mine 12 Diamonds"));
        r4d.add(new Quest(QuestTaskType.COOK_FOOD, 96, 0.0, QuestType.DAILY, 4, "Cook 96 Food"));
        r4d.add(new Quest(QuestTaskType.KILL_SKELETON, 40, 0.0, QuestType.DAILY, 4, "Kill 40 Skeletons"));
        r4d.add(new Quest(QuestTaskType.MINE_STONE, 256, 0.0, QuestType.DAILY, 4, "Mine 256 Stone"));
        r4d.add(new Quest(QuestTaskType.KILL_PLAYER, 3, 0.0, QuestType.DAILY, 4, "Kill 3 Players"));
        dailyQuestPool.put(4, r4d);

        List<Quest> r4w = new ArrayList<>();
        r4w.add(new Quest(QuestTaskType.MINE_DIAMOND, 48, 0.0, QuestType.WEEKLY, 4, "Mine 48 Diamonds"));
        r4w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 450, 0.0, QuestType.WEEKLY, 4, "Kill 450 Hostile Mobs"));
        r4w.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 100, 0.0, QuestType.WEEKLY, 4, "Kill 100 Mobs with a Sword"));
        r4w.add(new Quest(QuestTaskType.HARVEST_CROPS, 1000, 0.0, QuestType.WEEKLY, 4, "Harvest 1000 Crops"));
        r4w.add(new Quest(QuestTaskType.CRAFT_IRON_INGOTS, 128, 0.0, QuestType.WEEKLY, 4, "Craft 128 Iron Ingots"));
        r4w.add(new Quest(QuestTaskType.KILL_PLAYER, 10, 0.0, QuestType.WEEKLY, 4, "Kill 10 Players"));
        weeklyQuestPool.put(4, r4w);

        // ---- RANK 5 (getRank() == 5) — $100,000 ----
        List<Quest> r5d = new ArrayList<>();
        r5d.add(new Quest(QuestTaskType.MINE_DIAMOND, 24, 0.0, QuestType.DAILY, 5, "Mine 24 Diamonds"));
        r5d.add(new Quest(QuestTaskType.MINE_GOLD_ORE, 48, 0.0, QuestType.DAILY, 5, "Mine 48 Gold Ore"));
        r5d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 100, 0.0, QuestType.DAILY, 5, "Kill 100 Hostile Mobs"));
        r5d.add(new Quest(QuestTaskType.KILL_WITH_BOW, 40, 0.0, QuestType.DAILY, 5, "Kill 40 Mobs with a Bow"));
        r5d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 20, 0.0, QuestType.DAILY, 5, "Kill 20 Endermen"));
        r5d.add(new Quest(QuestTaskType.KILL_WITCH, 10, 0.0, QuestType.DAILY, 5, "Kill 10 Witches"));
        r5d.add(new Quest(QuestTaskType.HARVEST_CROPS, 192, 0.0, QuestType.DAILY, 5, "Harvest 192 Crops"));
        r5d.add(new Quest(QuestTaskType.BREED_ANIMALS, 14, 0.0, QuestType.DAILY, 5, "Breed 14 Animals"));
        r5d.add(new Quest(QuestTaskType.FISH, 25, 0.0, QuestType.DAILY, 5, "Fish 25 Fish"));
        r5d.add(new Quest(QuestTaskType.CRAFT_GOLDEN_APPLE, 24, 0.0, QuestType.DAILY, 5, "Craft 24 Golden Apples"));
        r5d.add(new Quest(QuestTaskType.MINE_IRON_ORE, 64, 0.0, QuestType.DAILY, 5, "Mine 64 Iron Ore"));
        r5d.add(new Quest(QuestTaskType.KILL_SPIDER, 20, 0.0, QuestType.DAILY, 5, "Kill 20 Spiders"));
        r5d.add(new Quest(QuestTaskType.COOK_FOOD, 128, 0.0, QuestType.DAILY, 5, "Cook 128 Food"));
        r5d.add(new Quest(QuestTaskType.BREAK_LOG, 200, 0.0, QuestType.DAILY, 5, "Break 200 Logs"));
        r5d.add(new Quest(QuestTaskType.KILL_ZOMBIE, 40, 0.0, QuestType.DAILY, 5, "Kill 40 Zombies"));
        r5d.add(new Quest(QuestTaskType.KILL_PLAYER, 3, 0.0, QuestType.DAILY, 5, "Kill 3 Players"));
        dailyQuestPool.put(5, r5d);

        List<Quest> r5w = new ArrayList<>();
        r5w.add(new Quest(QuestTaskType.MINE_DIAMOND, 80, 0.0, QuestType.WEEKLY, 5, "Mine 80 Diamonds"));
        r5w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 600, 0.0, QuestType.WEEKLY, 5, "Kill 600 Hostile Mobs"));
        r5w.add(new Quest(QuestTaskType.KILL_WITH_BOW, 150, 0.0, QuestType.WEEKLY, 5, "Kill 150 Mobs with a Bow"));
        r5w.add(new Quest(QuestTaskType.KILL_ENDERMAN, 40, 0.0, QuestType.WEEKLY, 5, "Kill 40 Endermen"));
        r5w.add(new Quest(QuestTaskType.HARVEST_CROPS, 1400, 0.0, QuestType.WEEKLY, 5, "Harvest 1400 Crops"));
        r5w.add(new Quest(QuestTaskType.KILL_PLAYER, 12, 0.0, QuestType.WEEKLY, 5, "Kill 12 Players"));
        weeklyQuestPool.put(5, r5w);

        // ---- RANK 6 (getRank() == 6) — $500,000 ----
        List<Quest> r6d = new ArrayList<>();
        r6d.add(new Quest(QuestTaskType.MINE_DIAMOND, 40, 0.0, QuestType.DAILY, 6, "Mine 40 Diamonds"));
        r6d.add(new Quest(QuestTaskType.MINE_GOLD_ORE, 80, 0.0, QuestType.DAILY, 6, "Mine 80 Gold Ore"));
        r6d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 150, 0.0, QuestType.DAILY, 6, "Kill 150 Hostile Mobs"));
        r6d.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 50, 0.0, QuestType.DAILY, 6, "Kill 50 Mobs with a Sword"));
        r6d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 25, 0.0, QuestType.DAILY, 6, "Kill 25 Endermen"));
        r6d.add(new Quest(QuestTaskType.KILL_WITCH, 8, 0.0, QuestType.DAILY, 6, "Kill 8 Witches"));
        r6d.add(new Quest(QuestTaskType.KILL_BLAZE, 10, 0.0, QuestType.DAILY, 6, "Kill 10 Blaze"));
        r6d.add(new Quest(QuestTaskType.HARVEST_CROPS, 160, 0.0, QuestType.DAILY, 6, "Harvest 160 Crops"));
        r6d.add(new Quest(QuestTaskType.BREED_ANIMALS, 16, 0.0, QuestType.DAILY, 6, "Breed 16 Animals"));
        r6d.add(new Quest(QuestTaskType.FISH, 20, 0.0, QuestType.DAILY, 6, "Fish 20 Fish"));
        r6d.add(new Quest(QuestTaskType.CRAFT_GOLDEN_APPLE, 48, 0.0, QuestType.DAILY, 6, "Craft 48 Golden Apples"));
        r6d.add(new Quest(QuestTaskType.MINE_IRON_ORE, 96, 0.0, QuestType.DAILY, 6, "Mine 96 Iron Ore"));
        r6d.add(new Quest(QuestTaskType.KILL_SKELETON, 50, 0.0, QuestType.DAILY, 6, "Kill 50 Skeletons"));
        r6d.add(new Quest(QuestTaskType.COOK_FOOD, 160, 0.0, QuestType.DAILY, 6, "Cook 160 Food"));
        r6d.add(new Quest(QuestTaskType.BREAK_LOG, 192, 0.0, QuestType.DAILY, 6, "Break 192 Logs"));
        r6d.add(new Quest(QuestTaskType.KILL_PLAYER, 4, 0.0, QuestType.DAILY, 6, "Kill 4 Players"));
        dailyQuestPool.put(6, r6d);

        List<Quest> r6w = new ArrayList<>();
        r6w.add(new Quest(QuestTaskType.MINE_DIAMOND, 128, 0.0, QuestType.WEEKLY, 6, "Mine 128 Diamonds"));
        r6w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 800, 0.0, QuestType.WEEKLY, 6, "Kill 800 Hostile Mobs"));
        r6w.add(new Quest(QuestTaskType.KILL_BLAZE, 40, 0.0, QuestType.WEEKLY, 6, "Kill 40 Blaze"));
        r6w.add(new Quest(QuestTaskType.KILL_WITCH, 50, 0.0, QuestType.WEEKLY, 6, "Kill 50 Witches"));
        r6w.add(new Quest(QuestTaskType.HARVEST_CROPS, 2000, 0.0, QuestType.WEEKLY, 6, "Harvest 2000 Crops"));
        r6w.add(new Quest(QuestTaskType.KILL_PLAYER, 15, 0.0, QuestType.WEEKLY, 6, "Kill 15 Players"));
        weeklyQuestPool.put(6, r6w);

        // ---- RANK 7 (getRank() == 7) — $2,500,000 ----
        List<Quest> r7d = new ArrayList<>();
        r7d.add(new Quest(QuestTaskType.MINE_DIAMOND, 64, 0.0, QuestType.DAILY, 7, "Mine 64 Diamonds"));
        r7d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 180, 0.0, QuestType.DAILY, 7, "Kill 180 Hostile Mobs"));
        r7d.add(new Quest(QuestTaskType.KILL_WITH_BOW, 60, 0.0, QuestType.DAILY, 7, "Kill 60 Mobs with a Bow"));
        r7d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 35, 0.0, QuestType.DAILY, 7, "Kill 35 Endermen"));
        r7d.add(new Quest(QuestTaskType.KILL_BLAZE, 20, 0.0, QuestType.DAILY, 7, "Kill 20 Blaze"));
        r7d.add(new Quest(QuestTaskType.KILL_GHAST, 8, 0.0, QuestType.DAILY, 7, "Kill 8 Ghasts"));
        r7d.add(new Quest(QuestTaskType.HARVEST_CROPS, 288, 0.0, QuestType.DAILY, 7, "Harvest 288 Crops"));
        r7d.add(new Quest(QuestTaskType.BREED_ANIMALS, 20, 0.0, QuestType.DAILY, 7, "Breed 20 Animals"));
        r7d.add(new Quest(QuestTaskType.FISH, 35, 0.0, QuestType.DAILY, 7, "Fish 35 Fish"));
        r7d.add(new Quest(QuestTaskType.CRAFT_GOLDEN_APPLE, 80, 0.0, QuestType.DAILY, 7, "Craft 80 Golden Apples"));
        r7d.add(new Quest(QuestTaskType.MINE_ANCIENT_DEBRIS, 6, 0.0, QuestType.DAILY, 7, "Mine 6 Ancient Debris"));
        r7d.add(new Quest(QuestTaskType.KILL_SKELETON, 75, 0.0, QuestType.DAILY, 7, "Kill 75 Skeletons"));
        r7d.add(new Quest(QuestTaskType.COOK_FOOD, 192, 0.0, QuestType.DAILY, 7, "Cook 192 Food"));
        r7d.add(new Quest(QuestTaskType.BREAK_LOG, 320, 0.0, QuestType.DAILY, 7, "Break 320 Logs"));
        r7d.add(new Quest(QuestTaskType.KILL_ZOMBIE, 75, 0.0, QuestType.DAILY, 7, "Kill 75 Zombies"));
        r7d.add(new Quest(QuestTaskType.KILL_PLAYER, 5, 0.0, QuestType.DAILY, 7, "Kill 5 Players"));
        dailyQuestPool.put(7, r7d);

        List<Quest> r7w = new ArrayList<>();
        r7w.add(new Quest(QuestTaskType.MINE_DIAMOND, 200, 0.0, QuestType.WEEKLY, 7, "Mine 200 Diamonds"));
        r7w.add(new Quest(QuestTaskType.MINE_ANCIENT_DEBRIS, 24, 0.0, QuestType.WEEKLY, 7, "Mine 24 Ancient Debris"));
        r7w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 1100, 0.0, QuestType.WEEKLY, 7, "Kill 1100 Hostile Mobs"));
        r7w.add(new Quest(QuestTaskType.KILL_BLAZE, 50, 0.0, QuestType.WEEKLY, 7, "Kill 50 Blaze"));
        r7w.add(new Quest(QuestTaskType.HARVEST_CROPS, 2500, 0.0, QuestType.WEEKLY, 7, "Harvest 2500 Crops"));
        r7w.add(new Quest(QuestTaskType.KILL_PLAYER, 18, 0.0, QuestType.WEEKLY, 7, "Kill 18 Players"));
        weeklyQuestPool.put(7, r7w);

        // ---- RANK 8 (getRank() == 8) — $10,000,000 ----
        List<Quest> r8d = new ArrayList<>();
        r8d.add(new Quest(QuestTaskType.MINE_DIAMOND, 80, 0.0, QuestType.DAILY, 8, "Mine 80 Diamonds"));
        r8d.add(new Quest(QuestTaskType.MINE_ANCIENT_DEBRIS, 10, 0.0, QuestType.DAILY, 8, "Mine 10 Ancient Debris"));
        r8d.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 250, 0.0, QuestType.DAILY, 8, "Kill 250 Hostile Mobs"));
        r8d.add(new Quest(QuestTaskType.KILL_WITH_SWORD, 75, 0.0, QuestType.DAILY, 8, "Kill 75 Mobs with a Sword"));
        r8d.add(new Quest(QuestTaskType.KILL_WITH_BOW, 60, 0.0, QuestType.DAILY, 8, "Kill 60 Mobs with a Bow"));
        r8d.add(new Quest(QuestTaskType.KILL_ENDERMAN, 50, 0.0, QuestType.DAILY, 8, "Kill 50 Endermen"));
        r8d.add(new Quest(QuestTaskType.KILL_BLAZE, 30, 0.0, QuestType.DAILY, 8, "Kill 30 Blaze"));
        r8d.add(new Quest(QuestTaskType.KILL_GHAST, 15, 0.0, QuestType.DAILY, 8, "Kill 15 Ghasts"));
        r8d.add(new Quest(QuestTaskType.HARVEST_CROPS, 352, 0.0, QuestType.DAILY, 8, "Harvest 352 Crops"));
        r8d.add(new Quest(QuestTaskType.BREED_ANIMALS, 25, 0.0, QuestType.DAILY, 8, "Breed 25 Animals"));
        r8d.add(new Quest(QuestTaskType.FISH, 45, 0.0, QuestType.DAILY, 8, "Fish 45 Fish"));
        r8d.add(new Quest(QuestTaskType.CRAFT_GOLDEN_APPLE, 128, 0.0, QuestType.DAILY, 8, "Craft 128 Golden Apples"));
        r8d.add(new Quest(QuestTaskType.KILL_SKELETON, 100, 0.0, QuestType.DAILY, 8, "Kill 100 Skeletons"));
        r8d.add(new Quest(QuestTaskType.COOK_FOOD, 256, 0.0, QuestType.DAILY, 8, "Cook 256 Food"));
        r8d.add(new Quest(QuestTaskType.KILL_ZOMBIE, 100, 0.0, QuestType.DAILY, 8, "Kill 100 Zombies"));
        r8d.add(new Quest(QuestTaskType.KILL_PLAYER, 5, 0.0, QuestType.DAILY, 8, "Kill 5 Players"));
        dailyQuestPool.put(8, r8d);

        List<Quest> r8w = new ArrayList<>();
        r8w.add(new Quest(QuestTaskType.MINE_DIAMOND, 300, 0.0, QuestType.WEEKLY, 8, "Mine 300 Diamonds"));
        r8w.add(new Quest(QuestTaskType.MINE_ANCIENT_DEBRIS, 48, 0.0, QuestType.WEEKLY, 8, "Mine 48 Ancient Debris"));
        r8w.add(new Quest(QuestTaskType.KILL_HOSTILE_MOB, 1600, 0.0, QuestType.WEEKLY, 8, "Kill 1600 Hostile Mobs"));
        r8w.add(new Quest(QuestTaskType.KILL_ENDERMAN, 100, 0.0, QuestType.WEEKLY, 8, "Kill 100 Endermen"));
        r8w.add(new Quest(QuestTaskType.HARVEST_CROPS, 3200, 0.0, QuestType.WEEKLY, 8, "Harvest 3200 Crops"));
        r8w.add(new Quest(QuestTaskType.KILL_PLAYER, 20, 0.0, QuestType.WEEKLY, 8, "Kill 20 Players"));
        weeklyQuestPool.put(8, r8w);
    }

    // -------------------------------------------------------------------------
    // Per-Player Quest Assignment
    // -------------------------------------------------------------------------

    /**
     * Assigns daily and weekly quests to a player if they don't have any assigned yet.
     */
    public static void assignQuestsIfNeeded(UUID uuid, int rank) {
        if (!playerActiveDailyQuests.containsKey(uuid)) {
            assignDailyQuests(uuid, rank);
        }
        if (!playerActiveWeeklyQuests.containsKey(uuid)) {
            assignWeeklyQuests(uuid, rank);
        }
        playerQuestRank.putIfAbsent(uuid, rank);
    }

    private static void assignDailyQuests(UUID uuid, int rank) {
        List<Quest> pool = dailyQuestPool.get(rank);
        if (pool == null || pool.isEmpty()) return;
        List<Quest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);
        List<Quest> selected = shuffled.subList(0, Math.min(3, shuffled.size()));
        List<Quest> assigned = new ArrayList<>();
        for (Quest q : selected) {
            assigned.add(q.withReward(generateRandomReward(rank, QuestType.DAILY)));
        }
        playerActiveDailyQuests.put(uuid, assigned);
    }

    private static void assignWeeklyQuests(UUID uuid, int rank) {
        List<Quest> pool = weeklyQuestPool.get(rank);
        if (pool == null || pool.isEmpty()) return;
        List<Quest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);
        List<Quest> selected = shuffled.subList(0, Math.min(3, shuffled.size()));
        List<Quest> assigned = new ArrayList<>();
        for (Quest q : selected) {
            assigned.add(q.withReward(generateRandomReward(rank, QuestType.WEEKLY)));
        }

        // 25% chance that exactly one of the three weekly quests rewards an item instead of money
        List<ItemStack> itemOptions = getWeeklyItemRewardOptions(rank);
        if (!itemOptions.isEmpty() && RANDOM.nextFloat() < 0.25f) {
            int itemIndex = RANDOM.nextInt(assigned.size());
            ItemStack itemReward = itemOptions.get(RANDOM.nextInt(itemOptions.size()));
            assigned.set(itemIndex, assigned.get(itemIndex).withItemReward(itemReward));
        }

        playerActiveWeeklyQuests.put(uuid, assigned);
    }

    /**
     * Returns the list of possible item rewards for a weekly quest of the given rank.
     * Each item has equal probability of being selected when the 25% roll triggers.
     */
    private static List<ItemStack> getWeeklyItemRewardOptions(int rank) {
        List<ItemStack> options = new ArrayList<>();
        switch (rank) {
            case 0 -> options.add(new KeyVote().getItem());
            case 1 -> {
                options.add(new KeyVote().getItem());
                options.add(new ItemStack(Material.DIAMOND, 16));
            }
            case 2 -> {
                options.add(new KeyVote().getItem());
                options.add(new KeyRare().getItem());
                options.add(new ItemStack(Material.DIAMOND, 24));
            }
            case 3 -> {
                options.add(new KeyRare().getItem());
                options.add(new ItemStack(Material.TRIDENT, 1));
                options.add(new ItemStack(Material.HEART_OF_THE_SEA, 1));
            }
            case 4 -> {
                options.add(new KeyRare().getItem());
                options.add(new KeyEpic().getItem());
                options.add(new ItemStack(Material.TRIDENT, 1));
                options.add(new ItemStack(Material.NETHERITE_INGOT, 1));
            }
            case 5 -> {
                options.add(new KeyEpic().getItem());
                options.add(new ItemStack(Material.ELYTRA, 1));
                options.add(new ItemStack(Material.NETHERITE_INGOT, 1));
            }
            case 6 -> {
                options.add(new KeyEpic().getItem());
                options.add(new KeyGodly().getItem());
                options.add(new AranarthiumIngot().getItem());
            }
            case 7 -> {
                options.add(new KeyGodly().getItem());
                options.add(new AranarthiumIngot().getItem());
                options.add(new ItemStack(Material.NETHER_STAR, 1));
            }
            case 8 -> {
                options.add(new KeyGodly().getItem());
                options.add(new AranarthiumIngot().getItem());
                options.add(new ItemStack(Material.NETHER_STAR, 1));
                options.add(new ItemStack(Material.HEAVY_CORE, 1));
            }
        }
        return options;
    }

    /**
     * Returns a human-readable display name for an item reward.
     * Uses the item's custom display name if present, otherwise formats the material name.
     * Prepends the amount when greater than 1.
     */
    public static String getItemRewardDisplayName(ItemStack item) {
        String name;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            name = item.getItemMeta().getDisplayName();
        } else {
            String[] words = item.getType().name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
            name = sb.toString();
        }
        if (item.getAmount() > 1) {
            return item.getAmount() + "x " + name;
        }
        return name;
    }

    // -------------------------------------------------------------------------
    // Reward Generation
    // -------------------------------------------------------------------------

    /**
     * Generates a random reward within the rank-appropriate range, rounded to the
     * correct interval (×5 under $100, ×10 under $500, ×100 under $10,000, ×1,000 at $10,000+).
     */
    public static double generateRandomReward(int rank, QuestType type) {
        int[] range = getRewardRange(rank, type);
        int min = range[0];
        int max = range[1];
        int raw = min + RANDOM.nextInt(max - min + 1);
        int rounded = roundReward(raw);
        // Clamp to valid rounded boundaries so we never exceed the intended range
        rounded = Math.max(ceilRound(min), Math.min(floorRound(max), rounded));
        return rounded;
    }

    private static int[] getRewardRange(int rank, QuestType type) {
        if (type == QuestType.DAILY) {
            return switch (rank) {
                case 0 -> new int[]{25, 75};
                case 1 -> new int[]{75, 200};
                case 2 -> new int[]{200, 450};
                case 3 -> new int[]{450, 750};
                case 4 -> new int[]{750, 1200};
                case 5 -> new int[]{1200, 1750};
                case 6 -> new int[]{1750, 2250};
                case 7 -> new int[]{2250, 3500};
                case 8 -> new int[]{3500, 10000};
                default -> new int[]{25, 75};
            };
        } else {
            return switch (rank) {
                case 0 -> new int[]{100, 250};
                case 1 -> new int[]{250, 500};
                case 2 -> new int[]{500, 1000};
                case 3 -> new int[]{1000, 2250};
                case 4 -> new int[]{2250, 3500};
                case 5 -> new int[]{3500, 5000};
                case 6 -> new int[]{5000, 7500};
                case 7 -> new int[]{7500, 12500};
                case 8 -> new int[]{12500, 20000};
                default -> new int[]{100, 250};
            };
        }
    }

    private static int roundReward(int value) {
        if (value < 100) return Math.round(value / 5.0f) * 5;
        if (value < 500) return Math.round(value / 10.0f) * 10;
        if (value < 10000) return Math.round(value / 100.0f) * 100;
        return Math.round(value / 1000.0f) * 1000;
    }

    private static int ceilRound(int value) {
        int step = stepFor(value);
        return ((value + step - 1) / step) * step;
    }

    private static int floorRound(int value) {
        int step = stepFor(value);
        return (value / step) * step;
    }

    private static int stepFor(int value) {
        if (value < 100) return 5;
        if (value < 500) return 10;
        if (value < 10000) return 100;
        return 1000;
    }

    // -------------------------------------------------------------------------
    // Reset Logic
    // -------------------------------------------------------------------------

    /**
     * Checks whether a daily or weekly reset is due and performs it if so.
     */
    public static void checkAndPerformResets() {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        long todayReset3amMillis = nowEst.toLocalDate()
                .atTime(3, 0)
                .atZone(EST)
                .toInstant()
                .toEpochMilli();

        long nowMillis = System.currentTimeMillis();

        if (nowMillis >= todayReset3amMillis && lastDailyReset < todayReset3amMillis) {
            resetDailyQuests();
        }

        if (nowEst.getDayOfWeek() == DayOfWeek.SUNDAY
                && nowMillis >= todayReset3amMillis
                && lastWeeklyReset < todayReset3amMillis) {
            resetWeeklyQuests();
        }
    }

    private static void resetDailyQuests() {
        lastDailyReset = System.currentTimeMillis();
        playerActiveDailyQuests.clear();
        playerDailyProgress.clear();
        playerDailyCompleted.clear();
        playerDailyClaimed.clear();
        Bukkit.getLogger().info("[AranarthCore] Daily quests have been reset.");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ChatUtils.chatMessage("&6Daily quests have reset! Visit the Quest Master for new quests."));
        }
    }

    private static void resetWeeklyQuests() {
        lastWeeklyReset = System.currentTimeMillis();
        playerActiveWeeklyQuests.clear();
        playerWeeklyProgress.clear();
        playerWeeklyCompleted.clear();
        playerWeeklyClaimed.clear();
        Bukkit.getLogger().info("[AranarthCore] Weekly quests have been reset.");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ChatUtils.chatMessage("&bWeekly quests have reset! Visit the Quest Master for new quests."));
        }
    }

    // -------------------------------------------------------------------------
    // Progress Tracking
    // -------------------------------------------------------------------------

    /**
     * Updates a player's quest progress for the given task type by the given amount.
     * Rewards are NOT granted automatically — the player must click the quest in the GUI.
     */
    public static void updateProgress(Player player, QuestTaskType taskType, int amount) {
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        if (aranarthPlayer == null) return;

        int rank = aranarthPlayer.getRank();

        // Reset player state if their rank changed
        Integer storedRank = playerQuestRank.get(uuid);
        if (storedRank == null || storedRank != rank) {
            playerActiveDailyQuests.remove(uuid);
            playerActiveWeeklyQuests.remove(uuid);
            playerDailyProgress.remove(uuid);
            playerDailyCompleted.remove(uuid);
            playerDailyClaimed.remove(uuid);
            playerWeeklyProgress.remove(uuid);
            playerWeeklyCompleted.remove(uuid);
            playerWeeklyClaimed.remove(uuid);
            playerQuestRank.put(uuid, rank);
        }

        assignQuestsIfNeeded(uuid, rank);
        updateProgressForType(player, uuid, taskType, amount, QuestType.DAILY);
        updateProgressForType(player, uuid, taskType, amount, QuestType.WEEKLY);
    }

    private static void updateProgressForType(Player player, UUID uuid,
                                               QuestTaskType taskType, int amount, QuestType questType) {
        HashMap<UUID, int[]> progressMap = questType == QuestType.DAILY ? playerDailyProgress : playerWeeklyProgress;
        HashMap<UUID, boolean[]> completedMap = questType == QuestType.DAILY ? playerDailyCompleted : playerWeeklyCompleted;
        HashMap<UUID, List<Quest>> activeMap = questType == QuestType.DAILY ? playerActiveDailyQuests : playerActiveWeeklyQuests;

        List<Quest> active = activeMap.get(uuid);
        if (active == null || active.isEmpty()) return;

        int[] progress = progressMap.computeIfAbsent(uuid, k -> new int[3]);
        boolean[] completed = completedMap.computeIfAbsent(uuid, k -> new boolean[3]);

        for (int i = 0; i < active.size(); i++) {
            Quest quest = active.get(i);
            if (quest.getTaskType() != taskType) continue;
            if (completed[i]) continue;

            progress[i] = Math.min(progress[i] + amount, quest.getRequired());

            if (progress[i] >= quest.getRequired()) {
                completed[i] = true;
                player.sendMessage(ChatUtils.chatMessage("&7You've completed a quest! Use &e/quests &7to claim your reward"));

                if (questType == QuestType.WEEKLY) {
                    boolean allComplete = true;
                    for (int j = 0; j < active.size(); j++) {
                        if (!completed[j]) {
                            allComplete = false;
                            break;
                        }
                    }
                    if (allComplete) {
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                        Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has completed all of their &eweekly quests"));
                    }
                }
            }
        }
    }

    /**
     * Claims the reward for a completed, unclaimed quest. Returns true if successful.
     */
    public static boolean claimQuestReward(Player player, QuestType type, int index) {
        UUID uuid = player.getUniqueId();

        HashMap<UUID, boolean[]> completedMap = type == QuestType.DAILY ? playerDailyCompleted : playerWeeklyCompleted;
        HashMap<UUID, boolean[]> claimedMap = type == QuestType.DAILY ? playerDailyClaimed : playerWeeklyClaimed;
        HashMap<UUID, List<Quest>> activeMap = type == QuestType.DAILY ? playerActiveDailyQuests : playerActiveWeeklyQuests;

        boolean[] completed = completedMap.get(uuid);
        if (completed == null || index >= completed.length || !completed[index]) return false;

        boolean[] claimed = claimedMap.computeIfAbsent(uuid, k -> new boolean[3]);
        if (claimed[index]) return false;

        List<Quest> active = activeMap.get(uuid);
        if (active == null || index >= active.size()) return false;

        claimed[index] = true;

        Quest quest = active.get(index);

        if (quest.hasItemReward()) {
            ItemStack rewardItem = quest.getItemReward().clone();
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(rewardItem);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            String itemName = getItemRewardDisplayName(rewardItem);
            player.sendMessage(ChatUtils.chatMessage("&7You have been rewarded &f" + itemName
                    + " &7for completing the quest"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            aranarthPlayer.setBalance(aranarthPlayer.getBalance() + quest.getReward());
            AranarthUtils.setPlayer(uuid, aranarthPlayer);

            String formattedReward = "$" + MONEY_FORMAT.format(quest.getReward());
            player.sendMessage(ChatUtils.chatMessage("&7You have been rewarded &6" + formattedReward
                    + " &7for completing the quest"));
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters for GUI
    // -------------------------------------------------------------------------

    public static List<Quest> getActiveDailyQuests(UUID uuid, int rank) {
        assignQuestsIfNeeded(uuid, rank);
        return playerActiveDailyQuests.getOrDefault(uuid, new ArrayList<>());
    }

    public static List<Quest> getActiveWeeklyQuests(UUID uuid, int rank) {
        assignQuestsIfNeeded(uuid, rank);
        return playerActiveWeeklyQuests.getOrDefault(uuid, new ArrayList<>());
    }

    public static int getDailyProgress(UUID uuid, int index) {
        int[] progress = playerDailyProgress.get(uuid);
        if (progress == null || index >= progress.length) return 0;
        return progress[index];
    }

    public static boolean isDailyCompleted(UUID uuid, int index) {
        boolean[] completed = playerDailyCompleted.get(uuid);
        if (completed == null || index >= completed.length) return false;
        return completed[index];
    }

    public static boolean isDailyClaimed(UUID uuid, int index) {
        boolean[] claimed = playerDailyClaimed.get(uuid);
        if (claimed == null || index >= claimed.length) return false;
        return claimed[index];
    }

    public static int getWeeklyProgress(UUID uuid, int index) {
        int[] progress = playerWeeklyProgress.get(uuid);
        if (progress == null || index >= progress.length) return 0;
        return progress[index];
    }

    public static boolean isWeeklyCompleted(UUID uuid, int index) {
        boolean[] completed = playerWeeklyCompleted.get(uuid);
        if (completed == null || index >= completed.length) return false;
        return completed[index];
    }

    public static boolean isWeeklyClaimed(UUID uuid, int index) {
        boolean[] claimed = playerWeeklyClaimed.get(uuid);
        if (claimed == null || index >= claimed.length) return false;
        return claimed[index];
    }

    // -------------------------------------------------------------------------
    // NPC Helpers
    // -------------------------------------------------------------------------

    public static boolean isQuestNpc(Entity entity) {
        if (!(entity instanceof Villager)) return false;
        return entity.getPersistentDataContainer().has(QUEST_NPC, PersistentDataType.STRING);
    }

    public static void spawnQuestNpc(Location location) {
        location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setPersistent(true);
            villager.setGravity(false);
            villager.setCustomName(ChatUtils.translateToColor("&6&lQuest Master"));
            villager.setCustomNameVisible(true);
            villager.setProfession(Villager.Profession.CLERIC);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.getPersistentDataContainer().set(QUEST_NPC, PersistentDataType.STRING, "true");
        });
    }

    public static boolean removeNearestQuestNpc(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 5, 5, 5)) {
            if (isQuestNpc(entity)) {
                entity.remove();
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Persistence Support
    // -------------------------------------------------------------------------

    public static HashMap<UUID, List<Quest>> getPlayerActiveDailyQuestsMap() {
        return playerActiveDailyQuests;
    }

    public static HashMap<UUID, List<Quest>> getPlayerActiveWeeklyQuestsMap() {
        return playerActiveWeeklyQuests;
    }

    public static HashMap<UUID, int[]> getPlayerDailyProgress() {
        return playerDailyProgress;
    }

    public static HashMap<UUID, boolean[]> getPlayerDailyCompleted() {
        return playerDailyCompleted;
    }

    public static HashMap<UUID, boolean[]> getPlayerDailyClaimed() {
        return playerDailyClaimed;
    }

    public static HashMap<UUID, int[]> getPlayerWeeklyProgress() {
        return playerWeeklyProgress;
    }

    public static HashMap<UUID, boolean[]> getPlayerWeeklyCompleted() {
        return playerWeeklyCompleted;
    }

    public static HashMap<UUID, boolean[]> getPlayerWeeklyClaimed() {
        return playerWeeklyClaimed;
    }

    public static HashMap<UUID, Integer> getPlayerQuestRank() {
        return playerQuestRank;
    }

    public static long getLastDailyReset() {
        return lastDailyReset;
    }

    public static void setLastDailyReset(long lastDailyReset) {
        QuestUtils.lastDailyReset = lastDailyReset;
    }

    public static long getLastWeeklyReset() {
        return lastWeeklyReset;
    }

    public static void setLastWeeklyReset(long lastWeeklyReset) {
        QuestUtils.lastWeeklyReset = lastWeeklyReset;
    }

    public static void setPlayerActiveDailyQuests(UUID uuid, List<Quest> quests) {
        playerActiveDailyQuests.put(uuid, quests);
    }

    public static void setPlayerActiveWeeklyQuests(UUID uuid, List<Quest> quests) {
        playerActiveWeeklyQuests.put(uuid, quests);
    }

    public static List<Quest> getDailyQuestPool(int rank) {
        return dailyQuestPool.getOrDefault(rank, new ArrayList<>());
    }

    public static List<Quest> getWeeklyQuestPool(int rank) {
        return weeklyQuestPool.getOrDefault(rank, new ArrayList<>());
    }

    // -------------------------------------------------------------------------
    // World Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the world allows crafting quest progress (survival worlds + spawn).
     */
    public static boolean isCraftingAllowedWorld(String worldName) {
        return AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn");
    }

    /**
     * Returns true if the world allows player-kill quest progress (survival + arena).
     */
    public static boolean isAllowedKillWorld(String worldName) {
        return AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("arena");
    }
}
