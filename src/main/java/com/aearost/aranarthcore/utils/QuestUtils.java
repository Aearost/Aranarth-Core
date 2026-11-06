package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestTrigger;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.*;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static com.aearost.aranarthcore.objects.CustomKeys.QUEST_NPC;

/**
 * Centralized utility class for all quest-related functionality: assigning boards, progress, rewards,
 * re-rolls, the full-board bonus and resets. The quests themselves are defined in quests.yml (see QuestConfig).
 */
public class QuestUtils {

    /**
     * The outcome of a re-roll attempt.
     */
    public enum RerollResult {
        SUCCESS, ALREADY_USED, COMPLETED, NOT_ENOUGH_MONEY, NONE_AVAILABLE
    }

    private static final HashMap<UUID, PlayerQuestBoard> boards = new HashMap<>();

    // UUIDs that had quest actions on THIS server in the current session.
    // Only these are synced to the shared DB so each server doesn't stomp the other's data.
    private static final Set<UUID> locallyModifiedUuids = new HashSet<>();

    // Superset of locallyModifiedUuids - never cleared during the session, only reset on startup.
    // Used at shutdown to safely write back all player data this server touched.
    private static final Set<UUID> sessionModifiedUuids = new HashSet<>();

    // Killer:victim to the time the victim can count again for Defeat Players
    private static final HashMap<String, Long> pvpVictimCooldowns = new HashMap<>();

    private static long lastDailyReset = 0;
    private static long lastWeeklyReset = 0;

    private static final ZoneId EST = ZoneId.of("America/New_York");
    private static final Random RANDOM = new Random();
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    static {
        NUMBER_FORMAT.setGroupingUsed(true);
    }

    /**
     * Loads quests.yml.
     */
    public static void initialize(Plugin plugin) {
        QuestConfig.load(plugin);
    }

    /**
     * @return The player's board, or null if they have none in memory. Does not assign quests.
     */
    public static PlayerQuestBoard getBoardIfPresent(UUID uuid) {
        return boards.get(uuid);
    }

    public static HashMap<UUID, PlayerQuestBoard> getBoards() {
        return boards;
    }

    public static void setBoard(UUID uuid, PlayerQuestBoard board) {
        boards.put(uuid, board);
    }

    /**
     * Provides the player's board, clearing any board left over from an earlier reset and assigning new quests.
     */
    public static PlayerQuestBoard getBoard(UUID uuid) {
        PlayerQuestBoard board = boards.computeIfAbsent(uuid, k -> new PlayerQuestBoard());
        for (QuestType type : QuestType.values()) {
            long period = getPeriod(type);
            if (board.getPeriod(type) < period) {
                board.clear(type);
            }
            if (board.getQuests(type).isEmpty() && board.getPeriod(type) == 0) {
                assignBoard(uuid, board, type);
            }
        }
        return board;
    }

    public static List<Quest> getQuests(UUID uuid, QuestType type) {
        return getBoard(uuid).getQuests(type);
    }

    private static long getPeriod(QuestType type) {
        return type == QuestType.DAILY ? lastDailyReset : lastWeeklyReset;
    }

    /**
     * Assigns a new set of quests to one board.
     */
    private static void assignBoard(UUID uuid, PlayerQuestBoard board, QuestType type) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        // Jobs load from the database just after joining, so wait for them rather than rolling a jobless board
        if (aranarthPlayer == null || (!aranarthPlayer.isJobDataLoaded() && DatabaseManager.isActive())) {
            return;
        }
        long period = getPeriod(type);
        // Deterministic per player and period so both servers roll the same board
        Random random = new Random(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits() ^ period
                ^ (type == QuestType.WEEKLY ? 0x5DEECE66DL : 0L) ^ board.getSalt(type));
        PlayerContext ctx = new PlayerContext(uuid, aranarthPlayer, type);

        List<Quest> quests = new ArrayList<>();
        List<Set<String>> usedActs = new ArrayList<>();
        Set<String> usedFamilies = new HashSet<>();
        Set<JobType> usedJobs = new HashSet<>();
        List<JobType> held = new ArrayList<>(ctx.heldJobs);
        Collections.shuffle(held, random);

        for (int i = 0; i < 3; i++) {
            QuestDefinition pick = pickForSlot(ctx, board, held, i, usedActs, usedFamilies, usedJobs, random);
            if (pick == null) {
                continue;
            }
            usedActs.add(pick.getActs());
            usedFamilies.add(pick.getFamily());
            usedJobs.addAll(pick.getJobs());
            quests.add(createQuest(ctx, pick, random));
        }

        if (type == QuestType.WEEKLY && !quests.isEmpty() && random.nextDouble() < QuestConfig.getContractChance()) {
            placeContract(ctx, quests, random);
        }

        attachItemRewards(ctx, quests, random);

        long now = System.currentTimeMillis();
        for (Quest quest : quests) {
            board.getHistory().put(historyKey(type, quest.getId()), now + getCooldownMs(type));
        }
        board.getHistory().values().removeIf(expiry -> expiry < now);
        board.setQuests(type, quests);
        board.setPeriod(type, period);
        board.setRerollUsed(type, false);
        board.setBonusClaimed(type, false);
        markModified(uuid);
    }

    private static long getCooldownMs(QuestType type) {
        // An hour short, so a quest is free again exactly at the reset that ends its cooldown
        return type == QuestType.DAILY
                ? QuestConfig.getDailyCooldownDays() * DAY_MS - 3600_000L
                : QuestConfig.getWeeklyCooldownWeeks() * 7 * DAY_MS - 3600_000L;
    }

    private static String historyKey(QuestType type, String id) {
        return (type == QuestType.DAILY ? "d:" : "w:") + id;
    }

    /**
     * Picks a quest for one slot.
     *
     * @param usedJobs The jobs of the quests already on the board, so players with no jobs get different jobs.
     */
    private static QuestDefinition pickForSlot(PlayerContext ctx, PlayerQuestBoard board, List<JobType> held, int slot,
                                               List<Set<String>> usedActs, Set<String> usedFamilies, Set<JobType> usedJobs,
                                               Random random) {
        String excludeId = null;
        QuestDefinition pick;
        if (!held.isEmpty()) {
            List<JobType> order = new ArrayList<>(held);
            Collections.rotate(order, -(slot % held.size()));
            for (JobType job : order) {
                pick = pick(ctx, board, jobPool(ctx, job), usedActs, usedFamilies, excludeId, true, random);
                if (pick != null) {
                    return pick;
                }
            }
            pick = pick(ctx, board, generalPool(ctx), usedActs, usedFamilies, excludeId, true, random);
            if (pick != null) {
                return pick;
            }
            for (JobType job : order) {
                pick = pick(ctx, board, jobPool(ctx, job), usedActs, usedFamilies, excludeId, false, random);
                if (pick != null) {
                    return pick;
                }
            }
            pick = pick(ctx, board, openPool(ctx), usedActs, usedFamilies, excludeId, true, random);
            return pick != null ? pick : pick(ctx, board, openPool(ctx), usedActs, usedFamilies, excludeId, false, random);
        }

        // No jobs: at most one General quest per board (only ever in the first slot), picked only when it is off cooldown
        if (slot == 0) {
            pick = pick(ctx, board, generalPool(ctx), usedActs, usedFamilies, excludeId, true, random);
            if (pick != null) {
                return pick;
            }
        }
        // Level 1 quests from different jobs, rotating which jobs, before repeating a job already on the board
        List<JobType> jobs = new ArrayList<>(Arrays.asList(JobType.values()));
        Collections.shuffle(jobs, random);
        jobs.sort(Comparator.comparing(usedJobs::contains));
        for (JobType job : jobs) {
            pick = pick(ctx, board, levelOnePool(ctx, job), usedActs, usedFamilies, excludeId, true, random);
            if (pick != null) {
                return pick;
            }
        }
        return pick(ctx, board, openPool(ctx), usedActs, usedFamilies, excludeId, false, random);
    }

    private static List<QuestDefinition> jobPool(PlayerContext ctx, JobType job) {
        List<QuestDefinition> pool = new ArrayList<>();
        for (QuestDefinition def : QuestConfig.getDefinitions()) {
            if (def.getJobs().contains(job) && ctx.getJobLevel(job) >= def.getLevel() && isAvailable(ctx, def)) {
                pool.add(def);
            }
        }
        return pool;
    }

    private static List<QuestDefinition> generalPool(PlayerContext ctx) {
        List<QuestDefinition> pool = new ArrayList<>();
        for (QuestDefinition def : QuestConfig.getDefinitions()) {
            if (def.isGeneral() && isAvailable(ctx, def)) {
                pool.add(def);
            }
        }
        return pool;
    }

    /**
     * @return Level 1 quests of one job, for players who do not hold it.
     */
    private static List<QuestDefinition> levelOnePool(PlayerContext ctx, JobType job) {
        List<QuestDefinition> pool = new ArrayList<>();
        for (QuestDefinition def : QuestConfig.getDefinitions()) {
            if (def.getJobs().contains(job) && def.getLevel() <= 1 && isAvailable(ctx, def)) {
                pool.add(def);
            }
        }
        return pool;
    }

    /**
     * @return Level 1 quests from jobs the player does not hold, used when their own jobs cannot fill a slot.
     */
    private static List<QuestDefinition> openPool(PlayerContext ctx) {
        List<QuestDefinition> pool = new ArrayList<>();
        for (QuestDefinition def : QuestConfig.getDefinitions()) {
            if (def.isGeneral() || def.getLevel() > 1 || !isAvailable(ctx, def)) {
                continue;
            }
            boolean isHeld = false;
            for (JobType job : def.getJobs()) {
                if (ctx.heldJobs.contains(job)) {
                    isHeld = true;
                    break;
                }
            }
            if (!isHeld) {
                pool.add(def);
            }
        }
        return pool;
    }

    /**
     * @return Whether the quest is offered at the player's rank, they meet its eligibility check,
     * and at least one of its variants is available to them.
     */
    private static boolean isAvailable(PlayerContext ctx, QuestDefinition def) {
        return def.isOffered(ctx.type, ctx.rank) && isEligible(ctx, def.getEligibility())
                && (!def.hasVariants() || !availableVariants(ctx, def).isEmpty());
    }

    private static QuestDefinition pick(PlayerContext ctx, PlayerQuestBoard board, List<QuestDefinition> pool,
                                        List<Set<String>> usedActs, Set<String> usedFamilies, String excludeId,
                                        boolean isStrict, Random random) {
        long now = System.currentTimeMillis();
        String month = AranarthUtils.getMonth() == null ? "" : AranarthUtils.getMonth().name();
        List<QuestDefinition> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        for (QuestDefinition def : pool) {
            if (def.getId().equals(excludeId) || usedFamilies.contains(def.getFamily()) || sharesAction(usedActs, def.getActs())) {
                continue;
            }
            if (isStrict) {
                Long expiry = board.getHistory().get(historyKey(ctx.type, def.getId()));
                if (expiry != null && expiry > now) {
                    continue;
                }
            }
            int boost = def.getBoostMonth() != null && def.getBoostMonth().equalsIgnoreCase(month) ? def.getBoostWeight() : 1;
            int weight = def.getWeight() * boost;
            candidates.add(def);
            weights.add(weight);
            totalWeight += weight;
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static boolean sharesAction(List<Set<String>> usedActs, Set<String> acts) {
        for (Set<String> used : usedActs) {
            for (String act : acts) {
                if (used.contains(act)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return The variants the player can be given: unlocked by job level and mcMMO Alchemy tier,
     * offered at their rank, and in season when limited to a month.
     */
    private static List<QuestDefinition.Variant> availableVariants(PlayerContext ctx, QuestDefinition def) {
        List<QuestDefinition.Variant> available = new ArrayList<>();
        String month = AranarthUtils.getMonth() == null ? "" : AranarthUtils.getMonth().name();
        int jobLevel = ctx.getBestLevel(def);
        for (QuestDefinition.Variant variant : def.getVariants().values()) {
            if (variant.getLevel() > jobLevel) {
                continue;
            }
            if (variant.getAlchemyTier() > 0 && variant.getAlchemyTier() > ctx.getAlchemyTier()) {
                continue;
            }
            if (variant.getMonth() != null && !variant.getMonth().equalsIgnoreCase(month)) {
                continue;
            }
            if (def.getBaseAmount(ctx.type, ctx.rank, variant.getKey()) <= 0) {
                continue;
            }
            available.add(variant);
        }
        return available;
    }

    private static String rollVariant(PlayerContext ctx, QuestDefinition def, Random random) {
        if (!def.hasVariants()) {
            return null;
        }
        List<QuestDefinition.Variant> available = availableVariants(ctx, def);
        if (available.isEmpty()) {
            return null;
        }
        int total = 0;
        for (QuestDefinition.Variant variant : available) {
            total += weightOf(ctx, variant);
        }
        int roll = random.nextInt(total);
        for (QuestDefinition.Variant variant : available) {
            roll -= weightOf(ctx, variant);
            if (roll < 0) {
                return variant.getKey();
            }
        }
        return available.get(0).getKey();
    }

    private static int weightOf(PlayerContext ctx, QuestDefinition.Variant variant) {
        // Seasonal variants (such as Order and Chaos in Florivor) are more likely on dailies during their month
        return ctx.type == QuestType.DAILY && variant.getMonth() != null ? Math.max(1, variant.getDailyWeight()) : 1;
    }

    private static double rollFactor(QuestType type, Random random) {
        double[] roll = type == QuestType.DAILY ? QuestConfig.getDailyRoll() : QuestConfig.getWeeklyRoll();
        return roll[0] + random.nextDouble() * (roll[1] - roll[0]);
    }

    private static Quest createQuest(PlayerContext ctx, QuestDefinition def, Random random) {
        String variant = rollVariant(ctx, def, random);
        int base = def.getBaseAmount(ctx.type, ctx.rank, variant);
        double baseReward = def.getBaseReward(ctx.type, ctx.rank);
        int required;
        double reward;
        if (def.isFixed(ctx.type)) {
            int[] range = def.getWeeklyRange(ctx.rank);
            required = range != null ? range[0] + random.nextInt(range[1] - range[0] + 1) : base;
            reward = baseReward;
        } else {
            double factor = rollFactor(ctx.type, random);
            required = roundAmount(base * factor);
            reward = roundReward(baseReward * factor);
        }
        return new Quest(def.getId(), false, ctx.type, ctx.rank, new String[]{variant}, new int[]{Math.max(1, required)}, reward);
    }

    /**
     * Gives one weekly slot a Job Contract, for players who hold both of its jobs.
     */
    private static void placeContract(PlayerContext ctx, List<Quest> quests, Random random) {
        List<QuestContract> candidates = new ArrayList<>();
        for (QuestContract contract : QuestConfig.getContracts()) {
            if (isContractAvailable(ctx, contract)) {
                candidates.add(contract);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        QuestContract contract = candidates.get(random.nextInt(candidates.size()));
        Set<String> contractActs = getContractActs(contract);
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < quests.size(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, random);
        for (int slot : slots) {
            List<Set<String>> others = new ArrayList<>();
            for (int i = 0; i < quests.size(); i++) {
                if (i != slot) {
                    others.add(getActs(quests.get(i)));
                }
            }
            if (!sharesAction(others, contractActs)) {
                quests.set(slot, createContract(ctx, contract, random));
                return;
            }
        }
    }

    private static boolean isContractAvailable(PlayerContext ctx, QuestContract contract) {
        if (!new HashSet<>(ctx.heldJobs).containsAll(contract.getJobs()) || !contract.isOffered(ctx.rank)) {
            return false;
        }
        for (QuestContract.Part part : contract.getParts()) {
            QuestDefinition def = QuestConfig.getDefinition(part.questId());
            if (def == null || ctx.getBestLevel(def) < def.getLevel() || !isEligible(ctx, def.getEligibility())) {
                return false;
            }
            if (part.variant() == null && def.hasVariants() && availableVariantsForContract(ctx, def).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<QuestDefinition.Variant> availableVariantsForContract(PlayerContext ctx, QuestDefinition def) {
        PlayerContext weekly = ctx.type == QuestType.WEEKLY ? ctx : ctx.withType(QuestType.WEEKLY);
        List<QuestDefinition.Variant> available = new ArrayList<>();
        int jobLevel = weekly.getBestLevel(def);
        String month = AranarthUtils.getMonth() == null ? "" : AranarthUtils.getMonth().name();
        for (QuestDefinition.Variant variant : def.getVariants().values()) {
            if (variant.getLevel() <= jobLevel && (variant.getMonth() == null || variant.getMonth().equalsIgnoreCase(month))
                    && (variant.getAlchemyTier() <= 0 || variant.getAlchemyTier() <= weekly.getAlchemyTier())) {
                available.add(variant);
            }
        }
        return available;
    }

    private static Quest createContract(PlayerContext ctx, QuestContract contract, Random random) {
        double factor = rollFactor(QuestType.WEEKLY, random);
        List<QuestContract.Part> parts = contract.getParts();
        String[] variants = new String[parts.size()];
        int[] required = new int[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            QuestContract.Part part = parts.get(i);
            QuestDefinition def = QuestConfig.getDefinition(part.questId());
            String variant = part.variant();
            if (variant == null && def.hasVariants()) {
                List<QuestDefinition.Variant> available = availableVariantsForContract(ctx, def);
                variant = available.isEmpty() ? null : available.get(random.nextInt(available.size())).getKey();
            }
            variants[i] = variant;
            required[i] = Math.max(1, roundAmount(part.amount()[ctx.rank] * factor));
        }
        return new Quest(contract.getId(), true, QuestType.WEEKLY, ctx.rank, variants, required,
                roundReward(contract.getReward(ctx.rank) * factor));
    }

    /**
     * @return Everything that can progress the quest, used so no two quests on a board share an action.
     */
    private static Set<String> getActs(Quest quest) {
        if (quest.isContract()) {
            QuestContract contract = QuestConfig.getContract(quest.getId());
            return contract == null ? Set.of() : getContractActs(contract);
        }
        QuestDefinition def = QuestConfig.getDefinition(quest.getId());
        return def == null ? Set.of() : def.getActs();
    }

    private static Set<String> getContractActs(QuestContract contract) {
        Set<String> acts = new HashSet<>();
        for (QuestContract.Part part : contract.getParts()) {
            QuestDefinition def = QuestConfig.getDefinition(part.questId());
            if (def != null) {
                acts.addAll(def.getActs());
            }
        }
        return acts;
    }

    private static String getFamily(Quest quest) {
        if (quest.isContract()) {
            return "contract";
        }
        QuestDefinition def = QuestConfig.getDefinition(quest.getId());
        return def == null ? quest.getId() : def.getFamily();
    }

    /**
     * Rolls crate keys and recipes onto Hard and Master quests, and at most one job item reward per board.
     */
    private static void attachItemRewards(PlayerContext ctx, List<Quest> quests, Random random) {
        if (quests.isEmpty()) {
            return;
        }
        List<Quest> hard = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.isContract()) {
                hard.add(quest);
                continue;
            }
            QuestDefinition def = QuestConfig.getDefinition(quest.getId());
            if (def != null && def.getTier().isHard()) {
                hard.add(quest);
            }
        }

        double keyChance = ctx.type == QuestType.DAILY ? QuestConfig.getDailyKeyChance(ctx.rank) : QuestConfig.getWeeklyKeyChance(ctx.rank);
        if (!hard.isEmpty() && random.nextDouble() < keyChance) {
            Quest quest = hard.get(random.nextInt(hard.size()));
            quest.setItemReward(ctx.type == QuestType.DAILY
                    ? QuestRewardUtils.rollDailyKey(random)
                    : QuestRewardUtils.rollWeeklyKey(ctx.rank, random));
        }

        // Legendary recipes stay with weekly quests at Duke and up
        if (ctx.type == QuestType.WEEKLY && ctx.rank >= 5 && !BrewRecipeUtils.allLegendaryUnlocked(ctx.uuid)
                && random.nextDouble() < QuestConfig.getLegendaryRecipeChance()) {
            List<Quest> open = new ArrayList<>();
            for (Quest quest : hard) {
                if (!quest.hasItemReward()) {
                    open.add(quest);
                }
            }
            BrewRecipe recipe = BrewRecipeUtils.getRandomLockedLegendary(ctx.uuid);
            if (!open.isEmpty() && recipe != null) {
                open.get(random.nextInt(open.size())).setItemReward("recipe:" + recipe.getId());
            }
        }

        double itemChance = ctx.type == QuestType.DAILY ? QuestConfig.getDailyItemChance() : QuestConfig.getWeeklyItemChance();
        if (random.nextDouble() < itemChance) {
            int minerLevel = ctx.getJobLevel(JobType.MINER);
            List<Quest> candidates = new ArrayList<>();
            for (Quest quest : quests) {
                if (quest.isContract() || quest.hasItemReward()) {
                    continue;
                }
                QuestDefinition def = QuestConfig.getDefinition(quest.getId());
                // Job items are a perk of the job, so they only go to players who hold it
                boolean isHeldJob = def != null && def.getJobs().stream().anyMatch(ctx.heldJobs::contains);
                if (isHeldJob && QuestRewardUtils.canGiveJobItem(def, ctx.type, minerLevel)) {
                    candidates.add(quest);
                }
            }
            if (!candidates.isEmpty()) {
                Quest quest = candidates.get(random.nextInt(candidates.size()));
                String item = QuestRewardUtils.rollJobItem(ctx.uuid, minerLevel, QuestConfig.getDefinition(quest.getId()),
                        ctx.type, ctx.rank, random);
                if (item != null) {
                    quest.setItemReward(item);
                }
            }
        }
    }

    /**
     * Checks whether the player can do a quest that needs something specific, such as a Dominion or an Elytra.
     */
    private static boolean isEligible(PlayerContext ctx, String check) {
        if (check == null) {
            return true;
        }
        Player player = Bukkit.getPlayer(ctx.uuid);
        Dominion dominion = DominionUtils.getPlayerDominion(ctx.uuid);
        return switch (check) {
            case "xp_elytra" -> player != null && player.getStatistic(Statistic.AVIATE_ONE_CM) > 0;
            case "xp_mount", "ge_mount" -> player != null && MountUtils.getElementForPlayer(player) != null;
            case "bu_dominion", "ge_homekill", "ge_food" -> dominion != null;
            case "bu_outpost" -> dominion != null && !OutpostUtils.getDominionOutposts(dominion.getId()).isEmpty();
            case "ge_resource" -> player != null && dominion != null
                    && DominionUtils.hasPermission(player, dominion, DominionPermission.RESOURCES);
            case "al_legend" -> hasLegendaryRecipe(ctx.uuid);
            case "xp_nether" -> hasAdvancement(player, "story/enter_the_nether");
            case "xp_end" -> hasAdvancement(player, "story/enter_the_end");
            case "ge_shop" -> countOtherShopOwners(ctx.uuid) >= 3;
            default -> true;
        };
    }

    private static boolean hasLegendaryRecipe(UUID uuid) {
        for (BrewRecipe recipe : BrewRecipeUtils.getUnlockedRecipes(uuid)) {
            if (recipe.getTier() == BrewRecipe.Tier.LEGENDARY) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAdvancement(Player player, String key) {
        if (player == null) {
            return false;
        }
        Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft(key));
        return advancement != null && player.getAdvancementProgress(advancement).isDone();
    }

    private static int countOtherShopOwners(UUID uuid) {
        int count = 0;
        for (Map.Entry<UUID, List<Shop>> entry : ShopUtils.getShops().entrySet()) {
            if (!entry.getKey().equals(uuid) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * The player's rank, jobs and levels at the time a board is rolled.
     */
    private static class PlayerContext {
        final UUID uuid;
        final int rank;
        final QuestType type;
        final List<JobType> heldJobs;
        final Map<JobType, Integer> levels;
        private Integer alchemyTier;

        PlayerContext(UUID uuid, AranarthPlayer aranarthPlayer, QuestType type) {
            this.uuid = uuid;
            this.rank = aranarthPlayer.getRank();
            this.type = type;
            this.heldJobs = new ArrayList<>();
            this.levels = new HashMap<>();
            if (aranarthPlayer.getJobData() != null) {
                JobData jobData = aranarthPlayer.getJobData();
                heldJobs.addAll(jobData.getActiveJobs());
                for (JobType job : JobType.values()) {
                    levels.put(job, jobData.getLevel(job));
                }
            }
        }

        private PlayerContext(PlayerContext other, QuestType type) {
            this.uuid = other.uuid;
            this.rank = other.rank;
            this.type = type;
            this.heldJobs = other.heldJobs;
            this.levels = other.levels;
            this.alchemyTier = other.alchemyTier;
        }

        private PlayerContext(PlayerContext other, int rank) {
            this.uuid = other.uuid;
            this.rank = rank;
            this.type = other.type;
            this.heldJobs = other.heldJobs;
            this.levels = other.levels;
            this.alchemyTier = other.alchemyTier;
        }

        PlayerContext withType(QuestType newType) {
            return new PlayerContext(this, newType);
        }

        PlayerContext atRank(int newRank) {
            return new PlayerContext(this, newRank);
        }

        int getJobLevel(JobType job) {
            return heldJobs.contains(job) ? levels.getOrDefault(job, 1) : 0;
        }

        /**
         * @return The best level the player holds among the quest's jobs, or 1 when they hold none of them.
         */
        int getBestLevel(QuestDefinition def) {
            int best = 0;
            for (JobType job : def.getJobs()) {
                best = Math.max(best, getJobLevel(job));
            }
            return def.getJobs().isEmpty() || best == 0 ? 1 : best;
        }

        int getAlchemyTier() {
            if (alchemyTier == null) {
                alchemyTier = 1;
                Player player = Bukkit.getPlayer(uuid);
                try {
                    McMMOPlayer mcMMOPlayer = player == null ? null : UserManager.getPlayer(player);
                    if (mcMMOPlayer != null) {
                        alchemyTier = mcMMOPlayer.getAlchemyManager().getTier();
                    }
                } catch (Exception ignored) {
                }
            }
            return alchemyTier;
        }
    }

    public static void progress(Player player, QuestTrigger trigger, String key, int amount) {
        progress(player, trigger, key, amount, Set.of(), null);
    }

    public static void progress(Player player, QuestTrigger trigger, String key, int amount, Set<String> tags) {
        progress(player, trigger, key, amount, tags, null);
    }

    /**
     * Progresses every unfinished quest on the player's boards that the action counts towards.
     *
     * @param key The material, entity, biome or structure name of the action.
     * @param tags Extra facts about the action, such as melee, bred or dominion.
     * @param distinctId For quests where each target only counts once, what identifies this target. Defaults to the key.
     */
    public static void progress(Player player, QuestTrigger trigger, String key, int amount, Set<String> tags, String distinctId) {
        if (amount <= 0) {
            return;
        }
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        // Quests pause while the player is AFK
        if (aranarthPlayer == null || AranarthUtils.isAfk(aranarthPlayer)) {
            return;
        }
        // Quests only ever progress in the survival worlds, and never in creative or spectator mode
        // (Architects can be in creative at spawn, which counts as a survival world)
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        PlayerQuestBoard board = getBoard(uuid);
        boolean isChanged = false;
        for (QuestType type : QuestType.values()) {
            for (Quest quest : board.getQuests(type)) {
                if (quest.isCompleted()) {
                    continue;
                }
                if (quest.isContract()) {
                    QuestContract contract = QuestConfig.getContract(quest.getId());
                    if (contract == null) {
                        continue;
                    }
                    for (int part = 0; part < contract.getParts().size(); part++) {
                        QuestDefinition def = QuestConfig.getDefinition(contract.getParts().get(part).questId());
                        if (def != null && def.getTrigger() == trigger && !quest.isPartDone(part)
                                && def.matches(quest.getVariant(part), key, tags)) {
                            isChanged |= advance(player, board, quest, part, def, key, amount, distinctId);
                        }
                    }
                } else {
                    QuestDefinition def = QuestConfig.getDefinition(quest.getId());
                    if (def != null && def.getTrigger() == trigger && def.matches(quest.getVariant(), key, tags)) {
                        isChanged |= advance(player, board, quest, 0, def, key, amount, distinctId);
                    }
                }
            }
        }
        if (isChanged) {
            markModified(uuid);
        }
    }

    private static boolean advance(Player player, PlayerQuestBoard board, Quest quest, int part, QuestDefinition def,
                                   String key, int amount, String distinctId) {
        int add = amount;
        if (def.isDistinct()) {
            String id = part + ":" + (distinctId != null ? distinctId : key).toLowerCase(Locale.ROOT);
            if (!quest.getSeen().add(id)) {
                return false;
            }
            add = 1;
        }
        boolean wasStarted = quest.hasStarted();
        quest.setProgress(part, quest.getProgress(part) + add);
        if (!wasStarted) {
            String typeLabel = Lang.getFor(player, quest.getType() == QuestType.DAILY ? "quest.type_daily_lower" : "quest.type_weekly_lower");
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.started", "type", typeLabel, "name", getDisplayName(player, quest))));
        }
        for (int i = 0; i < quest.getPartCount(); i++) {
            if (!quest.isPartDone(i)) {
                return true;
            }
        }
        quest.setCompleted(true);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.completed_hint")));
        if (quest.getType() == QuestType.WEEKLY && board.isAllCompleted(QuestType.WEEKLY)) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            Lang.broadcast("quest.all_weekly_broadcast", "player", aranarthPlayer.getNickname());
        }
        return true;
    }

    /**
     * Applies the Defeat Players limits.
     */
    public static boolean canCountPlayerKill(Player killer, Player victim) {
        Dominion killerDominion = DominionUtils.getPlayerDominion(killer.getUniqueId());
        Dominion victimDominion = DominionUtils.getPlayerDominion(victim.getUniqueId());
        if (killerDominion != null && victimDominion != null
                && (killerDominion.getId().equals(victimDominion.getId())
                || killerDominion.getAllied().contains(victimDominion.getId()))) {
            return false;
        }
        String key = killer.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long until = pvpVictimCooldowns.get(key);
        if (until != null && until > now) {
            return false;
        }
        pvpVictimCooldowns.values().removeIf(time -> time < now);
        pvpVictimCooldowns.put(key, now + QuestConfig.getPvpVictimCooldownMinutes() * 60_000L);
        return true;
    }

    /**
     * Claims the reward for a completed, unclaimed quest, along with its job XP or level 10 bonus.
     */
    public static boolean claimQuestReward(Player player, QuestType type, int index) {
        UUID uuid = player.getUniqueId();
        PlayerQuestBoard board = getBoard(uuid);
        List<Quest> quests = board.getQuests(type);
        if (index < 0 || index >= quests.size()) {
            return false;
        }
        Quest quest = quests.get(index);
        if (!quest.isCompleted() || quest.isClaimed()) {
            return false;
        }

        if (quest.hasItemReward()) {
            // Item rewards can only be claimed in a survival world, though crate keys are sent to /keyclaim
            if (!QuestRewardUtils.giveItems(player, quest)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.must_be_gameplay_world")));
                return false;
            }
            String itemName = QuestRewardUtils.getDisplayName(quest.getItemReward());
            boolean isPendingKey = QuestRewardUtils.isKey(quest.getItemReward())
                    && !AranarthUtils.isSurvivalWorld(player.getWorld().getName());
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player,
                    isPendingKey ? "quest.reward_item_keyclaim" : "quest.reward_item", "item", itemName)));
        } else {
            addMoney(uuid, quest.getReward());
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reward_money", "amount", "$" + NUMBER_FORMAT.format(quest.getReward()))));
        }
        quest.setClaimed(true);
        giveJobRewards(player, quest);
        markModified(uuid);
        return true;
    }

    /**
     * @return The jobs a quest belongs to: both jobs for a contract, none for a General quest.
     */
    public static List<JobType> getQuestJobs(Quest quest) {
        if (quest.isContract()) {
            QuestContract contract = QuestConfig.getContract(quest.getId());
            return contract == null ? List.of() : contract.getJobs();
        }
        QuestDefinition def = QuestConfig.getDefinition(quest.getId());
        return def == null ? List.of() : def.getJobs();
    }

    /**
     * Job quests give a share of the XP needed for the current level in each job the player still holds as XP only.
     */
    private static void giveJobRewards(Player player, Quest quest) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null || aranarthPlayer.getJobData() == null) {
            return;
        }
        JobData jobData = aranarthPlayer.getJobData();
        double percent = quest.getType() == QuestType.DAILY ? QuestConfig.getDailyXpPercent() : QuestConfig.getWeeklyXpPercent();
        int masterBonus = 0;
        for (JobType job : getQuestJobs(quest)) {
            if (!jobData.hasJob(job)) {
                continue;
            }
            int level = jobData.getLevel(job);
            if (level >= 10) {
                masterBonus += quest.getType() == QuestType.DAILY
                        ? QuestConfig.getMasterDailyBonus(quest.getRank())
                        : QuestConfig.getMasterWeeklyBonus(quest.getRank());
                continue;
            }
            long needed = JobUtils.getXpRequired(level);
            if (needed <= 0) {
                continue;
            }
            jobData.addTotalXp(job, needed * percent / 100.0);
            int newLevel = jobData.getLevel(job);
            for (int lvl = level + 1; lvl <= newLevel; lvl++) {
                JobUtils.handleLevelUp(player, aranarthPlayer, job, lvl);
            }
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reward_xp",
                    "percent", formatPercent(percent), "job", Lang.key(job.getNameKey()))));
        }
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        if (masterBonus > 0) {
            addMoney(player.getUniqueId(), masterBonus);
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reward_master",
                    "amount", "$" + NUMBER_FORMAT.format(masterBonus))));
        }
    }

    private static String formatPercent(double percent) {
        return percent == Math.floor(percent) ? String.valueOf((int) percent) : String.valueOf(percent);
    }

    private static void addMoney(UUID uuid, double amount) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + amount);
        AranarthUtils.setPlayer(uuid, aranarthPlayer);
        PersistenceUtils.saveAranarthPlayerImmediately(uuid);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishBalanceAdjust(uuid, amount);
        }
    }

    /**
     * @return The money all quests on the board are worth, counting quests whose money was replaced by an item.
     */
    public static double getBoardValue(PlayerQuestBoard board, QuestType type) {
        double total = 0;
        for (Quest quest : board.getQuests(type)) {
            total += quest.getBaseReward();
        }
        return total;
    }

    /**
     * Claims the bonus for finishing a whole board: a crate key, extra money, or (weekly, Duke and up) a brew recipe.
     */
    public static boolean claimBoardBonus(Player player, QuestType type) {
        UUID uuid = player.getUniqueId();
        PlayerQuestBoard board = getBoard(uuid);
        if (!board.isAllClaimed(type) || board.isBonusClaimed(type)) {
            return false;
        }
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.must_be_gameplay_world")));
            return false;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        int rank = aranarthPlayer.getRank();

        List<String> options = new ArrayList<>(List.of("key", "money"));
        BrewRecipe recipe = null;
        if (type == QuestType.WEEKLY && rank >= 5) {
            recipe = BrewRecipeUtils.getRandomLockedLegendary(uuid);
            if (recipe == null) {
                recipe = BrewRecipeUtils.getRandomLockedRare(uuid);
            }
            if (recipe != null) {
                options.add("recipe");
            }
        }
        String option = options.get(RANDOM.nextInt(options.size()));
        board.setBonusClaimed(type, true);
        markModified(uuid);

        switch (option) {
            case "money" -> {
                double amount = roundReward(getBoardValue(board, type) * QuestConfig.getBoardBonusPercent() / 100.0);
                addMoney(uuid, amount);
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.bonus_money", "amount", "$" + NUMBER_FORMAT.format(amount))));
            }
            case "recipe" -> giveBonusItem(player, "recipe:" + recipe.getId());
            default -> giveBonusItem(player, type == QuestType.DAILY
                    ? (RANDOM.nextDouble() < QuestConfig.getDailyBonusEpicChance(rank) ? "key:epic" : "key:rare")
                    : QuestRewardUtils.rollWeeklyKey(rank, RANDOM));
        }
        return true;
    }

    private static void giveBonusItem(Player player, String descriptor) {
        for (org.bukkit.inventory.ItemStack item : QuestRewardUtils.createItems(descriptor)) {
            for (org.bukkit.inventory.ItemStack overflow : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.bonus_item", "item", QuestRewardUtils.getDisplayName(descriptor))));
    }

    /**
     * @return What re-rolling the quest costs: a share of the money it is worth, even when an item replaces that money.
     */
    public static double getRerollCost(Quest quest) {
        return roundReward(quest.getBaseReward() * QuestConfig.getRerollCostFraction());
    }

    /**
     * Swaps an unfinished quest for a new one, once per board per reset.
     */
    public static RerollResult reroll(Player player, QuestType type, int index) {
        UUID uuid = player.getUniqueId();
        PlayerQuestBoard board = getBoard(uuid);
        List<Quest> quests = board.getQuests(type);
        if (index < 0 || index >= quests.size()) {
            return RerollResult.NONE_AVAILABLE;
        }
        Quest old = quests.get(index);
        if (old.isCompleted()) {
            return RerollResult.COMPLETED;
        }
        if (board.isRerollUsed(type)) {
            return RerollResult.ALREADY_USED;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        double cost = getRerollCost(old);
        if (aranarthPlayer == null || aranarthPlayer.getBalance() < cost) {
            return RerollResult.NOT_ENOUGH_MONEY;
        }

        // The rank the board was rolled at decides the new quest's size, so it matches the rest of the board
        PlayerContext ctx = new PlayerContext(uuid, aranarthPlayer, type);
        PlayerContext boardCtx = old.getRank() == ctx.rank ? ctx : ctx.atRank(old.getRank());
        List<Set<String>> usedActs = new ArrayList<>();
        Set<String> usedFamilies = new HashSet<>();
        for (int i = 0; i < quests.size(); i++) {
            if (i != index) {
                usedActs.add(getActs(quests.get(i)));
                usedFamilies.add(getFamily(quests.get(i)));
            }
        }
        List<QuestDefinition> pool = new ArrayList<>();
        if (!boardCtx.heldJobs.isEmpty()) {
            for (JobType job : boardCtx.heldJobs) {
                pool.addAll(jobPool(boardCtx, job));
            }
            pool.addAll(generalPool(boardCtx));
        } else {
            // Players with no jobs keep at most one General quest on a board
            boolean hasGeneral = false;
            for (int i = 0; i < quests.size(); i++) {
                QuestDefinition def = QuestConfig.getDefinition(quests.get(i).getId());
                if (i != index && def != null && def.isGeneral()) {
                    hasGeneral = true;
                }
            }
            if (!hasGeneral) {
                pool.addAll(generalPool(boardCtx));
            }
            pool.addAll(openPool(boardCtx));
        }
        QuestDefinition pick = pick(boardCtx, board, pool, usedActs, usedFamilies, old.getId(), true, RANDOM);
        if (pick == null) {
            pick = pick(boardCtx, board, pool, usedActs, usedFamilies, old.getId(), false, RANDOM);
        }
        if (pick == null && !boardCtx.heldJobs.isEmpty()) {
            pick = pick(boardCtx, board, openPool(boardCtx), usedActs, usedFamilies, old.getId(), false, RANDOM);
        }
        if (pick == null) {
            return RerollResult.NONE_AVAILABLE;
        }

        addMoney(uuid, -cost);
        quests.set(index, createQuest(boardCtx, pick, RANDOM));
        board.getHistory().put(historyKey(type, pick.getId()), System.currentTimeMillis() + getCooldownMs(type));
        board.setRerollUsed(type, true);
        markModified(uuid);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "quest.reroll_success",
                "amount", "$" + NUMBER_FORMAT.format(cost), "name", getDisplayName(player, quests.get(index)))));
        return RerollResult.SUCCESS;
    }

    /**
     * Provides the quest's name in the player's language.
     */
    public static String getDisplayName(Player player, Quest quest) {
        if (quest.isContract()) {
            return Lang.getFor(player, "quest.name_" + quest.getId());
        }
        return getPartName(player, quest.getId(), quest.getVariant(), quest.getRequired());
    }

    /**
     * Provides the name of one quest, or of one part of a contract, with its amount and variant filled in.
     */
    public static String getPartName(Player player, String questId, String variant, int required) {
        String variantName = variant == null ? "" : Lang.getFor(player, "quest.var_" + questId + "_" + variant);
        return Lang.getFor(player, "quest.name_" + questId, "count", NUMBER_FORMAT.format(required), "variant", variantName);
    }

    public static String formatNumber(double value) {
        return NUMBER_FORMAT.format(value);
    }

    private static int roundAmount(double value) {
        if (value < 20) {
            return (int) Math.round(value);
        }
        int step = value < 100 ? 5 : value < 1000 ? 10 : value < 10000 ? 50 : 100;
        return (int) (Math.round(value / step) * step);
    }

    private static double roundReward(double value) {
        if (value <= 0) {
            return 0;
        }
        int step = value < 100 ? 5 : value < 1000 ? 10 : value < 10000 ? 50 : 100;
        return Math.max(step, Math.round(value / step) * step);
    }

    /**
     * Checks whether a daily or weekly reset is due and performs it if so.
     */
    public static void checkAndPerformResets() {
        LocalDateTime nowEst = LocalDateTime.now(EST);
        long todayReset3amMillis = nowEst.toLocalDate().atTime(3, 0).atZone(EST).toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();

        if (nowMillis >= todayReset3amMillis && lastDailyReset < todayReset3amMillis) {
            // Sync from DB first - the other server may have already reset this period
            long dbReset = loadResetStamp("quest:lastDailyReset");
            if (dbReset > lastDailyReset) {
                lastDailyReset = dbReset;
            }
            if (lastDailyReset < todayReset3amMillis) {
                lastDailyReset = nowMillis;
                saveResetStamp("quest:lastDailyReset", lastDailyReset);
                Bukkit.getLogger().info("[AC] [AranarthCore] Daily quests have been reset.");
            } else {
                Bukkit.getLogger().info("[AC] [AranarthCore] Daily quests synced from remote reset.");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(ChatUtils.chatMessage(Lang.getFor(online, "quest.daily_reset")));
            }
        }

        if (nowEst.getDayOfWeek() == DayOfWeek.SUNDAY && nowMillis >= todayReset3amMillis
                && lastWeeklyReset < todayReset3amMillis) {
            long dbReset = loadResetStamp("quest:lastWeeklyReset");
            if (dbReset > lastWeeklyReset) {
                lastWeeklyReset = dbReset;
            }
            if (lastWeeklyReset < todayReset3amMillis) {
                lastWeeklyReset = nowMillis;
                saveResetStamp("quest:lastWeeklyReset", lastWeeklyReset);
                Bukkit.getLogger().info("[AC] [AranarthCore] Weekly quests have been reset.");
            } else {
                Bukkit.getLogger().info("[AC] [AranarthCore] Weekly quests synced from remote reset.");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(ChatUtils.chatMessage(Lang.getFor(online, "quest.weekly_reset")));
            }
        }
    }

    private static long loadResetStamp(String key) {
        if (!DatabaseManager.isActive()) {
            return 0;
        }
        try {
            String stamp = DatabaseManager.getInstance().loadTempData(key);
            return stamp == null ? 0 : Long.parseLong(stamp);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void saveResetStamp(String key, long stamp) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                DatabaseManager.getInstance().saveTempData(key, String.valueOf(stamp), 2592000);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Clears one of a player's boards so a fresh one is assigned the next time it is read, informing the other server as well.
     *
     * @param uuid The player.
     * @param type The board to reset.
     */
    public static void resetPlayerQuests(UUID uuid, QuestType type) {
        long salt = System.currentTimeMillis();
        applyPlayerQuestReset(uuid, type, salt);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishQuestReset(uuid, type, salt);
        }
    }

    /**
     * Clears one of a player's boards with the given salt. Called directly and when the other server resets a board.
     */
    public static void applyPlayerQuestReset(UUID uuid, QuestType type, long salt) {
        PlayerQuestBoard board = boards.computeIfAbsent(uuid, k -> new PlayerQuestBoard());
        // A reset is a clean re-roll from every quest the player can get, so that board's cooldowns are cleared
        String prefix = historyKey(type, "");
        board.getHistory().keySet().removeIf(key -> key.startsWith(prefix));
        board.clear(type);
        board.setSalt(type, salt);
        if (Bukkit.getPlayer(uuid) != null) {
            markModified(uuid);
        }
    }

    /**
     * If an unclaimed weekly quest rewards the given brew recipe, its money reward is restored instead.
     *
     * @return Whether a quest was updated.
     */
    public static boolean regenerateWeeklyQuestIfBrewReward(UUID uuid, String recipeId) {
        PlayerQuestBoard board = boards.get(uuid);
        if (board == null) {
            return false;
        }
        for (Quest quest : board.getQuests(QuestType.WEEKLY)) {
            if (!quest.isClaimed() && ("recipe:" + recipeId).equals(quest.getItemReward())) {
                quest.setItemReward(null);
                markModified(uuid);
                return true;
            }
        }
        return false;
    }

    public static void markModified(UUID uuid) {
        locallyModifiedUuids.add(uuid);
        sessionModifiedUuids.add(uuid);
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

    public static Set<UUID> getLocallyModifiedUuids() {
        return locallyModifiedUuids;
    }

    public static Set<UUID> getSessionModifiedUuids() {
        return sessionModifiedUuids;
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
}
