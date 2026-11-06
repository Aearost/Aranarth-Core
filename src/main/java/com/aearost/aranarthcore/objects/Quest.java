package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.QuestType;

import java.util.HashSet;
import java.util.Set;

/**
 * A quest on a player's board, with its rolled amount, reward and progress.
 * It points to a QuestDefinition, or to a QuestContract when it is a Job Contract.
 */
public class Quest {

    private final String id;
    private final boolean isContract;
    private final QuestType type;
    private final int rank;
    // Normal quests use the first entry only; contracts use one entry per part
    private final String[] variants;
    private final int[] required;
    private final int[] progress;
    private final Set<String> seen = new HashSet<>();
    private final double baseReward;
    private double reward;
    /** A reward item descriptor such as key:epic or frames:8, or null when the quest pays money. */
    private String itemReward;
    private boolean isCompleted;
    private boolean isClaimed;

    public Quest(String id, boolean isContract, QuestType type, int rank, String[] variants, int[] required,
                 double baseReward) {
        this.id = id;
        this.isContract = isContract;
        this.type = type;
        this.rank = rank;
        this.variants = variants;
        this.required = required;
        this.progress = new int[required.length];
        this.baseReward = baseReward;
        this.reward = baseReward;
    }

    public String getId() {
        return id;
    }

    public boolean isContract() {
        return isContract;
    }

    public QuestType getType() {
        return type;
    }

    /**
     * @return The rank the quest was rolled for.
     */
    public int getRank() {
        return rank;
    }

    public int getPartCount() {
        return required.length;
    }

    public String getVariant(int part) {
        return variants[part];
    }

    public String getVariant() {
        return variants[0];
    }

    public String[] getVariants() {
        return variants;
    }

    public int getRequired(int part) {
        return required[part];
    }

    public int getRequired() {
        return required[0];
    }

    public int[] getRequiredArray() {
        return required;
    }

    public int getProgress(int part) {
        return progress[part];
    }

    public int getProgress() {
        return progress[0];
    }

    public int[] getProgressArray() {
        return progress;
    }

    public void setProgress(int part, int value) {
        progress[part] = Math.max(0, Math.min(value, required[part]));
    }

    public boolean isPartDone(int part) {
        return progress[part] >= required[part];
    }

    public boolean hasStarted() {
        for (int value : progress) {
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The distinct targets already counted, such as biomes or Dominions, prefixed by part index.
     */
    public Set<String> getSeen() {
        return seen;
    }

    /**
     * @return The money the quest is worth, used for re-roll costs and the board bonus even when an item replaces it.
     */
    public double getBaseReward() {
        return baseReward;
    }

    /**
     * @return The money paid on claim, which is 0 when an item reward replaces it.
     */
    public double getReward() {
        return reward;
    }

    public String getItemReward() {
        return itemReward;
    }

    public boolean hasItemReward() {
        return itemReward != null;
    }

    /**
     * Replaces the money reward with an item, or restores the money when given null.
     */
    public void setItemReward(String itemReward) {
        this.itemReward = itemReward;
        this.reward = itemReward == null ? baseReward : 0;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public boolean isClaimed() {
        return isClaimed;
    }

    public void setClaimed(boolean isClaimed) {
        this.isClaimed = isClaimed;
    }
}
