package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Map;

import static com.aearost.aranarthcore.objects.CustomKeys.CRATE_KEY;
import static com.aearost.aranarthcore.objects.CustomKeys.QUEST_NPC;

/**
 * Centralized utility class for all quest-related functionality.
 */
public class QuestUtils {

    // All possible quests per rank
    private static final HashMap<Integer, List<Quest>> dailyQuestPool = new HashMap<>();
    private static final HashMap<Integer, List<Quest>> weeklyQuestPool = new HashMap<>();

    // Per-player active quests
    private static final HashMap<UUID, List<Quest>> playerActiveDailyQuests = new HashMap<>();
    private static final HashMap<UUID, List<Quest>> playerActiveWeeklyQuests = new HashMap<>();

    // Player quest progress
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

    /**
     * Initializes all quest pools by loading from quests.yml.
     */
    public static void initialize(Plugin plugin) {
        loadQuestPools(plugin);
    }

    /**
     * Loads quest pools from quests.yml in the plugin data folder.
     * Creates the file from the bundled default if it does not exist.
     */
    private static void loadQuestPools(Plugin plugin) {
        dailyQuestPool.clear();
        weeklyQuestPool.clear();
        for (int i = 0; i <= 8; i++) {
            dailyQuestPool.put(i, new ArrayList<>());
            weeklyQuestPool.put(i, new ArrayList<>());
        }

        File file = new File(plugin.getDataFolder(), "quests.yml");
        if (!file.exists()) {
            plugin.saveResource("quests.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadSection(config, "daily", QuestType.DAILY, dailyQuestPool);
        loadSection(config, "weekly", QuestType.WEEKLY, weeklyQuestPool);
    }

    /**
     * Parses one section (daily or weekly) from the quests.yml config into the given pool map.
     */
    private static void loadSection(YamlConfiguration config, String section,
                                    QuestType questType, HashMap<Integer, List<Quest>> pool) {
        List<?> entries = config.getList(section);
        if (entries == null) {
            Bukkit.getLogger().warning("No '" + section + "' section found in quests.yml");
            return;
        }
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            String taskName = (String) map.get("task");
            if (taskName == null) continue;
            QuestTaskType taskType;
            try {
                taskType = QuestTaskType.valueOf(taskName);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Unknown quest task type in quests.yml: " + taskName);
                continue;
            }
            List<?> values = (List<?>) map.get("values");
            if (values == null || values.size() < 9) continue;
            for (int rank = 0; rank <= 8; rank++) {
                Object valObj = values.get(rank);
                int value = valObj instanceof Number ? ((Number) valObj).intValue() : 0;
                if (value > 0) {
                    pool.get(rank).add(new Quest(
                            taskType, value, 0.0, questType, rank,
                            generateDisplayName(taskType, value)
                    ));
                }
            }
        }
    }

    /**
     * Generates a human-readable display name for a quest based on its task type and required count.
     */
    public static String generateDisplayName(QuestTaskType type, int count) {
        return switch (type) {
            case BREAK_LOG -> "Break " + count + " Logs";
            case MINE_STONE -> "Mine " + count + " Stone";
            case MINE_COAL_ORE -> "Mine " + count + " Coal Ore";
            case MINE_IRON_ORE -> "Mine " + count + " Iron Ore";
            case MINE_GOLD_ORE -> "Mine " + count + " Gold Ore";
            case MINE_DIAMOND -> "Mine " + count + " Diamond Ore";
            case MINE_ANCIENT_DEBRIS -> "Mine " + count + " Ancient Debris";
            case BREAK_SAND -> "Break " + count + " Sand";
            case BREAK_GRAVEL -> "Break " + count + " Gravel";
            case BREAK_DIRT -> "Break " + count + " Dirt";
            case HARVEST_CROPS -> "Harvest " + count + " Crops";
            case PLANT_CROPS -> "Plant " + count + " Crops";
            case BREED_ANIMALS -> "Breed " + count + " Animals";
            case FISH -> "Fish " + count + " Fish";
            case COOK_FOOD -> "Cook " + count + " Food";
            case KILL_HOSTILE_MOB -> "Kill " + count + " Hostile Mobs";
            case KILL_PASSIVE_MOB -> "Kill " + count + " Passive Mobs";
            case KILL_WITH_MELEE -> "Kill " + count + " Mobs with a Melee Weapon";
            case KILL_WITH_RANGED -> "Kill " + count + " Mobs with a Ranged Weapon";
            case KILL_WITH_SWORD -> "Kill " + count + " Mobs with a Sword";
            case KILL_WITH_BOW -> "Kill " + count + " Mobs with a Bow";
            case KILL_SKELETON -> "Kill " + count + " Skeletons";
            case KILL_ZOMBIE -> "Kill " + count + " Zombies";
            case KILL_CREEPER -> "Kill " + count + " Creepers";
            case KILL_ENDERMAN -> "Kill " + count + " Endermen";
            case KILL_SPIDER -> "Kill " + count + " Spiders";
            case KILL_WITCH -> "Kill " + count + " Witches";
            case KILL_BLAZE -> "Kill " + count + " Blazes";
            case KILL_GHAST -> "Kill " + count + " Ghasts";
            case KILL_COW -> "Kill " + count + " Cows";
            case KILL_PIG -> "Kill " + count + " Pigs";
            case KILL_CHICKEN -> "Kill " + count + " Chickens";
            case KILL_SHEEP -> "Kill " + count + " Sheep";
            case KILL_RABBIT -> "Kill " + count + " Rabbits";
            case KILL_PLAYER -> "Kill " + count + " Players";
            case TRAVEL_BLOCKS -> "Travel " + count + " Blocks";
            case CRAFT_PLANKS -> "Craft " + count + " Planks";
            case CRAFT_TORCHES -> "Craft " + count + " Torches";
            case CRAFT_BREAD -> "Craft " + count + " Bread";
            case CRAFT_GLASS -> "Craft " + count + " Glass";
            case CRAFT_IRON_INGOTS -> "Craft " + count + " Iron Ingots";
            case CRAFT_GOLDEN_APPLE -> "Craft " + count + " Golden Apples";
        };
    }

    /**
     * Returns a copy of the given quest with its required count randomised +/- 20%.
     * The display name is updated to reflect the new count.
     */
    private static Quest randomizeRequired(Quest q) {
        double factor = 0.8 + RANDOM.nextDouble() * 0.4; // uniform [0.8, 1.2]
        int randomized = Math.max(1, (int) Math.round(q.getRequired() * factor));
        return new Quest(
                q.getTaskType(), randomized, q.getReward(), q.getQuestType(), q.getRank(),
                generateDisplayName(q.getTaskType(), randomized), q.getItemReward()
        );
    }

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
        if (pool == null || pool.isEmpty()) {
            return;
        }
        List<Quest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);
        List<Quest> selected = shuffled.subList(0, Math.min(3, shuffled.size()));
        List<Quest> assigned = new ArrayList<>();
        for (Quest q : selected) {
            Quest randomized = randomizeRequired(q);
            assigned.add(randomized.withReward(generateRandomReward(rank, QuestType.DAILY, q.getTaskType())));
        }

        // Rank-scaled chance that exactly one daily quest rewards a crate key instead of money
        float dailyKeyChance = getDailyKeyChance(rank);
        if (dailyKeyChance > 0f && RANDOM.nextFloat() < dailyKeyChance) {
            int keyIndex = RANDOM.nextInt(assigned.size());
            assigned.set(keyIndex, assigned.get(keyIndex).withItemReward(getDailyKeyReward()));
        }

        playerActiveDailyQuests.put(uuid, assigned);
    }

    private static void assignWeeklyQuests(UUID uuid, int rank) {
        List<Quest> pool = weeklyQuestPool.get(rank);
        if (pool == null || pool.isEmpty()) {
            return;
        }
        List<Quest> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, RANDOM);
        List<Quest> selected = shuffled.subList(0, Math.min(3, shuffled.size()));
        List<Quest> assigned = new ArrayList<>();
        for (Quest q : selected) {
            Quest randomized = randomizeRequired(q);
            assigned.add(randomized.withReward(generateRandomReward(rank, QuestType.WEEKLY, q.getTaskType())));
        }

        // Rank-scaled chance that exactly one weekly quest rewards an epic or godly crate key
        // Higher ranks have a better chance of a key, and a higher chance that key is godly
        float weeklyKeyChance = getWeeklyKeyChance(rank);
        if (RANDOM.nextFloat() < weeklyKeyChance) {
            int keyIndex = RANDOM.nextInt(assigned.size());
            assigned.set(keyIndex, assigned.get(keyIndex).withItemReward(getWeeklyKeyReward(rank)));
        }

        playerActiveWeeklyQuests.put(uuid, assigned);
    }

    /**
     * Returns the probability that a daily quest assignment will include a crate key reward.
     */
    private static float getDailyKeyChance(int rank) {
        return switch (rank) {
            case 1 -> 0.05f;
            case 2 -> 0.10f;
            case 3 -> 0.15f;
            case 4 -> 0.25f;
            case 5 -> 0.35f;
            case 6 -> 0.45f;
            case 7 -> 0.55f;
            case 8 -> 0.65f;
            default -> 0f;
        };
    }

    /**
     * Returns a sentinel integer for persisting a crate key reward:
     * -1 = key_rare, -2 = key_epic, -3 = key_godly.
     */
    public static int getItemRewardSentinel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        String keyType = item.getItemMeta().getPersistentDataContainer().get(CRATE_KEY, PersistentDataType.STRING);
        return switch (keyType != null ? keyType : "") {
            case "key_epic" -> -2;
            case "key_godly" -> -3;
            default -> -1;
        };
    }

    /**
     * Returns the ItemStack for a persisted key sentinel (-1=rare, -2=epic, -3=godly).
     */
    public static ItemStack resolveKeyFromSentinel(int sentinel) {
        return switch (sentinel) {
            case -2 -> new KeyEpic().getItem();
            case -3 -> new KeyGodly().getItem();
            default -> new KeyRare().getItem();
        };
    }

    /**
     * Returns a crate key for a daily quest
     */
    private static ItemStack getDailyKeyReward() {
        // 85% rare, 15% epic
        return RANDOM.nextFloat() < 0.15f ? new KeyEpic().getItem() : new KeyRare().getItem();
    }

    /**
     * Returns the probability that a weekly quest assignment will include a crate key reward.
     */
    private static float getWeeklyKeyChance(int rank) {
        return switch (rank) {
            case 1 -> 0.20f;
            case 2 -> 0.25f;
            case 3 -> 0.35f;
            case 4 -> 0.45f;
            case 5 -> 0.55f;
            case 6 -> 0.65f;
            case 7 -> 0.75f;
            case 8 -> 0.80f;
            default -> 0.15f;
        };
    }

    /**
     * Returns a crate key for a weekly quest.
     */
    private static ItemStack getWeeklyKeyReward(int rank) {
        float godlyChance = switch (rank) {
            case 3, 4    -> 0.10f;
            case 5, 6    -> 0.15f;
            case 7       -> 0.20f;
            case 8       -> 0.25f;
            default      -> 0.05f;
        };
        return RANDOM.nextFloat() < godlyChance ? new KeyGodly().getItem() : new KeyEpic().getItem();
    }

    /**
     * Returns a human-readable display name for an item reward.
     */
    public static String getItemRewardDisplayName(ItemStack item) {
        String name;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            name = item.getItemMeta().getDisplayName();
        } else {
            String[] words = item.getType().name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
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

    /**
     * Generates a random reward within the rank-appropriate range.
     */
    public static double generateRandomReward(int rank, QuestType type) {
        return generateRandomReward(rank, type, null);
    }

    /**
     * Generates a random reward scaled by the difficulty of the given quest task type.
     */
    public static double generateRandomReward(int rank, QuestType type, QuestTaskType taskType) {
        int[] range = getRewardRange(rank, type);
        double multiplier = getDifficultyMultiplier(taskType);
        int min = (int) Math.round(range[0] * multiplier);
        int max = (int) Math.round(range[1] * multiplier);
        if (min > max) {
            min = max;
        }
        int raw = min + RANDOM.nextInt(max - min + 1);
        int rounded = roundReward(raw);
        rounded = Math.max(ceilRound(min), Math.min(floorRound(max), rounded));
        return rounded;
    }

    /**
     * Returns a difficulty multiplier for the given task type.
     */
    private static double getDifficultyMultiplier(QuestTaskType taskType) {
        if (taskType == null) {
            return 1.0;
        }
        return switch (taskType) {
            // Easy
            case HARVEST_CROPS, PLANT_CROPS, BREED_ANIMALS, COOK_FOOD,
                 CRAFT_PLANKS, CRAFT_TORCHES, CRAFT_GLASS, CRAFT_BREAD, CRAFT_IRON_INGOTS,
                 BREAK_LOG, BREAK_SAND, BREAK_DIRT, BREAK_GRAVEL,
                 TRAVEL_BLOCKS, KILL_PASSIVE_MOB,
                 KILL_COW, KILL_PIG, KILL_CHICKEN, KILL_SHEEP, KILL_RABBIT -> 0.5;
            // Medium
            case MINE_STONE, MINE_COAL_ORE, MINE_IRON_ORE,
                 KILL_HOSTILE_MOB, KILL_ZOMBIE, KILL_SKELETON, KILL_SPIDER, KILL_CREEPER,
                 KILL_WITH_SWORD, KILL_WITH_BOW, KILL_WITH_MELEE, KILL_WITH_RANGED -> 1.0;
            // Hard
            case FISH, MINE_GOLD_ORE, MINE_DIAMOND, MINE_ANCIENT_DEBRIS,
                 KILL_ENDERMAN, KILL_WITCH, KILL_BLAZE, KILL_GHAST,
                 CRAFT_GOLDEN_APPLE -> 1.5;
            // Very hard
            case KILL_PLAYER -> 2.0;
        };
    }

    private static int[] getRewardRange(int rank, QuestType type) {
        if (type == QuestType.DAILY) {
            return switch (rank) {
                case 0 -> new int[]{150, 450};
                case 1 -> new int[]{450, 1200};
                case 2 -> new int[]{1200, 2500};
                case 3 -> new int[]{2500, 4500};
                case 4 -> new int[]{4500, 7500};
                case 5 -> new int[]{7500, 11000};
                case 6 -> new int[]{10000, 14000};
                case 7 -> new int[]{14000, 21000};
                case 8 -> new int[]{21000, 60000};
                default -> new int[]{150, 450};
            };
        } else {
            return switch (rank) {
                case 0 -> new int[]{700, 1750};
                case 1 -> new int[]{1750, 3500};
                case 2 -> new int[]{3500, 7000};
                case 3 -> new int[]{7000, 15000};
                case 4 -> new int[]{15000, 25000};
                case 5 -> new int[]{25000, 35000};
                case 6 -> new int[]{35000, 52500};
                case 7 -> new int[]{50000, 87500};
                case 8 -> new int[]{87500, 140000};
                default -> new int[]{700, 1750};
            };
        }
    }

    private static int roundReward(int value) {
        if (value < 100) {
            return Math.round(value / 5.0f) * 5;
        }
        if (value < 500) {
            return Math.round(value / 10.0f) * 10;
        }
        if (value < 10000) {
            return Math.round(value / 100.0f) * 100;
        }
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
        if (value < 100) {
            return 5;
        }
        if (value < 500) {
            return 10;
        }
        if (value < 10000) {
            return 100;
        }
        return 1000;
    }

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

    /**
     * Updates a player's quest progress for the given task type by the given amount.
     * Rewards are NOT granted automatically — the player must click the quest in the GUI.
     */
    public static void updateProgress(Player player, QuestTaskType taskType, int amount) {
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        if (aranarthPlayer == null) {
            return;
        }

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
        if (active == null || active.isEmpty()) {
            return;
        }

        int[] progress = progressMap.computeIfAbsent(uuid, k -> new int[3]);
        boolean[] completed = completedMap.computeIfAbsent(uuid, k -> new boolean[3]);

        for (int i = 0; i < active.size(); i++) {
            Quest quest = active.get(i);
            if (quest.getTaskType() != taskType) {
                continue;
            }
            if (completed[i]) {
                continue;
            }

            if (progress[i] == 0) {
                String typeLabel = questType == QuestType.DAILY ? "daily" : "weekly";
                player.sendMessage(ChatUtils.chatMessage("&7You have started the " + typeLabel + " &e" + quest.getDisplayName() + " &7quest"));
            }
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
     * Claims the reward for a completed, unclaimed quest.
     */
    public static boolean claimQuestReward(Player player, QuestType type, int index) {
        UUID uuid = player.getUniqueId();

        HashMap<UUID, boolean[]> completedMap = type == QuestType.DAILY ? playerDailyCompleted : playerWeeklyCompleted;
        HashMap<UUID, boolean[]> claimedMap = type == QuestType.DAILY ? playerDailyClaimed : playerWeeklyClaimed;
        HashMap<UUID, List<Quest>> activeMap = type == QuestType.DAILY ? playerActiveDailyQuests : playerActiveWeeklyQuests;

        boolean[] completed = completedMap.get(uuid);
        if (completed == null || index >= completed.length || !completed[index]) {
            return false;
        }

        boolean[] claimed = claimedMap.computeIfAbsent(uuid, k -> new boolean[3]);
        if (claimed[index]) {
            return false;
        }

        List<Quest> active = activeMap.get(uuid);
        if (active == null || index >= active.size()) {
            return false;
        }

        claimed[index] = true;

        Quest quest = active.get(index);

        if (quest.hasItemReward()) {
            ItemStack rewardItem = quest.getItemReward().clone();
            String crateKeyType = rewardItem.hasItemMeta()
                    ? rewardItem.getItemMeta().getPersistentDataContainer().get(CRATE_KEY, PersistentDataType.STRING)
                    : null;
            boolean isCrateKey = crateKeyType != null;
            String worldName = player.getWorld().getName();
            String itemName = getItemRewardDisplayName(rewardItem);
            if (isCrateKey && !AranarthUtils.isSurvivalWorld(worldName)) {
                AranarthUtils.addPendingKey(player.getUniqueId(), rewardItem, rewardItem.getAmount());
                player.sendMessage(ChatUtils.chatMessage("&7You have been rewarded &f" + itemName
                        + " &7for completing the quest (use &e/keyclaim &7in Survival to claim)"));
            } else {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(rewardItem);
                for (ItemStack overflow : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
                player.sendMessage(ChatUtils.chatMessage("&7You have been rewarded &f" + itemName
                        + " &7for completing the quest"));
            }
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
        if (progress == null || index >= progress.length) {
            return 0;
        }
        return progress[index];
    }

    public static boolean isDailyCompleted(UUID uuid, int index) {
        boolean[] completed = playerDailyCompleted.get(uuid);
        if (completed == null || index >= completed.length) {
            return false;
        }
        return completed[index];
    }

    public static boolean isDailyClaimed(UUID uuid, int index) {
        boolean[] claimed = playerDailyClaimed.get(uuid);
        if (claimed == null || index >= claimed.length) {
            return false;
        }
        return claimed[index];
    }

    public static int getWeeklyProgress(UUID uuid, int index) {
        int[] progress = playerWeeklyProgress.get(uuid);
        if (progress == null || index >= progress.length) {
            return 0;
        }
        return progress[index];
    }

    public static boolean isWeeklyCompleted(UUID uuid, int index) {
        boolean[] completed = playerWeeklyCompleted.get(uuid);
        if (completed == null || index >= completed.length) {
            return false;
        }
        return completed[index];
    }

    public static boolean isWeeklyClaimed(UUID uuid, int index) {
        boolean[] claimed = playerWeeklyClaimed.get(uuid);
        if (claimed == null || index >= claimed.length) {
            return false;
        }
        return claimed[index];
    }

    public static boolean isQuestNpc(Entity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
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

    /**
     * Returns true if the world allows crafting quest progress.
     */
    public static boolean isCraftingAllowedWorld(String worldName) {
        return AranarthUtils.isSurvivalWorld(worldName);
    }

    /**
     * Returns true if the world allows player-kill quest progress.
     */
    public static boolean isAllowedKillWorld(String worldName) {
        return AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("arena");
    }
}
