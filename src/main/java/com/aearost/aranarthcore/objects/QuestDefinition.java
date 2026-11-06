package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestTrigger;
import com.aearost.aranarthcore.enums.QuestType;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A quest from quests.yml describing` what the quest counts and how large it is at each rank.
 */
public class QuestDefinition {

    /**
     * The difficulty tier of a quest. Keys and recipes only attach to Hard and Master quests.
     */
    public enum Tier {
        EASY, STANDARD, HARD, MASTER;

        public boolean isHard() {
            return this == HARD || this == MASTER;
        }
    }

    /**
     * A rotating option of a quest, such as the ore or mob it asks for.
     */
    public static class Variant {
        private final String key;
        private final TargetMatcher targets;
        private final int level;
        private final int alchemyTier;
        private final String month;
        private final int dailyWeight;
        private final int[] dailyAmount;
        private final int[] weeklyAmount;

        public Variant(String key, TargetMatcher targets, int level, int alchemyTier, String month, int dailyWeight,
                       int[] dailyAmount, int[] weeklyAmount) {
            this.key = key;
            this.targets = targets;
            this.level = level;
            this.alchemyTier = alchemyTier;
            this.month = month;
            this.dailyWeight = dailyWeight;
            this.dailyAmount = dailyAmount;
            this.weeklyAmount = weeklyAmount;
        }

        public String getKey() {
            return key;
        }

        public TargetMatcher getTargets() {
            return targets;
        }

        public int getLevel() {
            return level;
        }

        public int getAlchemyTier() {
            return alchemyTier;
        }

        /**
         * @return The Aranarth month this variant is limited to, or null if it can appear in any month.
         */
        public String getMonth() {
            return month;
        }

        public int getDailyWeight() {
            return dailyWeight;
        }

        /**
         * @return The variant's own amounts for the given board type, or null if it uses the quest's amounts.
         */
        public int[] getAmount(QuestType type) {
            return type == QuestType.DAILY ? dailyAmount : weeklyAmount;
        }
    }

    /**
     * Matches material, entity, biome or structure names against a list of patterns.
     * A * is a wildcard and a leading ! excludes. An empty list matches everything.
     */
    public static class TargetMatcher {
        private final List<Pattern> includes = new ArrayList<>();
        private final List<Pattern> excludes = new ArrayList<>();

        public TargetMatcher(List<String> patterns) {
            for (String raw : patterns) {
                boolean isExclude = raw.startsWith("!");
                String body = isExclude ? raw.substring(1) : raw;
                Pattern pattern = Pattern.compile(Pattern.quote(body.toUpperCase(Locale.ROOT)).replace("*", "\\E.*\\Q"));
                if (isExclude) {
                    excludes.add(pattern);
                } else {
                    includes.add(pattern);
                }
            }
        }

        public boolean matches(String key) {
            String upper = key.toUpperCase(Locale.ROOT);
            for (Pattern exclude : excludes) {
                if (exclude.matcher(upper).matches()) {
                    return false;
                }
            }
            if (includes.isEmpty()) {
                return true;
            }
            for (Pattern include : includes) {
                if (include.matcher(upper).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private final String id;
    private final List<JobType> jobs;
    private final Tier tier;
    private final String family;
    private final Set<String> acts;
    private final QuestTrigger trigger;
    private final TargetMatcher targets;
    private final Set<String> tags;
    private final boolean distinct;
    private final int level;
    private final String eligibility;
    private final String boostMonth;
    private final int boostWeight;
    private final int[] dailyAmount;
    private final int[] dailyReward;
    private final int[] weeklyAmount;
    private final int[] weeklyReward;
    private final boolean weeklyFixed;
    private final Map<Integer, int[]> weeklyRange;
    private final LinkedHashMap<String, Variant> variants;
    private int weight = 1;

    public QuestDefinition(String id, List<JobType> jobs, Tier tier, String family, Set<String> acts, QuestTrigger trigger,
                           TargetMatcher targets, Set<String> tags, boolean distinct, int level, String eligibility,
                           String boostMonth, int boostWeight, int[] dailyAmount, int[] dailyReward, int[] weeklyAmount,
                           int[] weeklyReward, boolean weeklyFixed, Map<Integer, int[]> weeklyRange,
                           LinkedHashMap<String, Variant> variants) {
        this.id = id;
        this.jobs = jobs;
        this.tier = tier;
        this.family = family;
        this.acts = acts;
        this.trigger = trigger;
        this.targets = targets;
        this.tags = tags;
        this.distinct = distinct;
        this.level = level;
        this.eligibility = eligibility;
        this.boostMonth = boostMonth;
        this.boostWeight = boostWeight;
        this.dailyAmount = dailyAmount;
        this.dailyReward = dailyReward;
        this.weeklyAmount = weeklyAmount;
        this.weeklyReward = weeklyReward;
        this.weeklyFixed = weeklyFixed;
        this.weeklyRange = weeklyRange;
        this.variants = variants;
    }

    public String getId() {
        return id;
    }

    /**
     * @return The jobs this quest belongs to. Empty for General quests.
     */
    public List<JobType> getJobs() {
        return jobs;
    }

    public boolean isGeneral() {
        return jobs.isEmpty();
    }

    public Tier getTier() {
        return tier;
    }

    public String getFamily() {
        return family;
    }

    public Set<String> getActs() {
        return acts;
    }

    public QuestTrigger getTrigger() {
        return trigger;
    }

    public Set<String> getTags() {
        return tags;
    }

    /**
     * @return Whether each target only counts once, such as each biome or each Dominion visited.
     */
    public boolean isDistinct() {
        return distinct;
    }

    public int getLevel() {
        return level;
    }

    public String getEligibility() {
        return eligibility;
    }

    public String getBoostMonth() {
        return boostMonth;
    }

    public int getBoostWeight() {
        return boostWeight;
    }

    /**
     * @return How likely the quest is to be picked compared to the others in its pool, where 1 is normal.
     */
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = Math.max(1, weight);
    }

    public LinkedHashMap<String, Variant> getVariants() {
        return variants;
    }

    public boolean hasVariants() {
        return !variants.isEmpty();
    }

    public Variant getVariant(String key) {
        return key == null ? null : variants.get(key);
    }

    /**
     * @return Whether the quest exists for the board type at all.
     */
    public boolean hasType(QuestType type) {
        return (type == QuestType.DAILY ? dailyAmount : weeklyAmount) != null;
    }

    /**
     * @param type The board type.
     * @param rank The rank.
     * @param variantKey The variant, or null.
     * @return The base amount at the rank, or 0 if the quest is not offered there.
     */
    public int getBaseAmount(QuestType type, int rank, String variantKey) {
        Variant variant = getVariant(variantKey);
        int[] amounts = variant != null && variant.getAmount(type) != null
                ? variant.getAmount(type)
                : (type == QuestType.DAILY ? dailyAmount : weeklyAmount);
        if (amounts == null || rank < 0 || rank >= amounts.length) {
            return 0;
        }
        return amounts[rank];
    }

    public int getBaseReward(QuestType type, int rank) {
        int[] rewards = type == QuestType.DAILY ? dailyReward : weeklyReward;
        if (rewards == null || rank < 0 || rank >= rewards.length) {
            return 0;
        }
        return rewards[rank];
    }

    /**
     * @return Whether the weekly amount is fixed rather than rolled each week.
     */
    public boolean isFixed(QuestType type) {
        return type == QuestType.WEEKLY && weeklyFixed;
    }

    /**
     * @return The [low, high] weekly range at the rank, or null if the quest has no range there.
     */
    public int[] getWeeklyRange(int rank) {
        return weeklyRange.get(rank);
    }

    /**
     * @return Whether the quest is offered at the rank for the board type.
     */
    public boolean isOffered(QuestType type, int rank) {
        if (!hasType(type)) {
            return false;
        }
        if (getBaseReward(type, rank) <= 0) {
            return false;
        }
        if (hasVariants()) {
            for (Variant variant : variants.values()) {
                if (getBaseAmount(type, rank, variant.getKey()) > 0) {
                    return true;
                }
            }
            return false;
        }
        return getBaseAmount(type, rank, null) > 0;
    }

    /**
     * @param variantKey The variant on the player's board, or null.
     * @param key The material, entity, biome or structure name of the action.
     * @param actionTags The tags describing the action, such as melee or bred.
     * @return Whether the action progresses this quest.
     */
    public boolean matches(String variantKey, String key, Set<String> actionTags) {
        if (!actionTags.containsAll(tags)) {
            return false;
        }
        Variant variant = getVariant(variantKey);
        TargetMatcher matcher = variant != null ? variant.getTargets() : targets;
        return matcher == null || matcher.matches(key);
    }
}
