package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an immutable quest definition for the Aranarth quest system.
 */
public class Quest {

    private final QuestTaskType taskType;
    private final int required;
    private final double reward;
    private final QuestType questType;
    private final int rank;
    private final String displayName;
    /** Non-null when this quest rewards an item instead of money. */
    private final ItemStack itemReward;

    public Quest(QuestTaskType taskType, int required, double reward, QuestType questType, int rank, String displayName) {
        this(taskType, required, reward, questType, rank, displayName, null);
    }

    public Quest(QuestTaskType taskType, int required, double reward, QuestType questType, int rank, String displayName, ItemStack itemReward) {
        this.taskType = taskType;
        this.required = required;
        this.reward = reward;
        this.questType = questType;
        this.rank = rank;
        this.displayName = displayName;
        this.itemReward = itemReward;
    }

    public QuestTaskType getTaskType() {
        return taskType;
    }

    public int getRequired() {
        return required;
    }

    public double getReward() {
        return reward;
    }

    public QuestType getQuestType() {
        return questType;
    }

    public int getRank() {
        return rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Returns the item reward, or null if this quest rewards money. */
    public ItemStack getItemReward() {
        return itemReward;
    }

    /** Returns true if this quest rewards an item instead of money. */
    public boolean hasItemReward() {
        return itemReward != null;
    }

    /** Returns a copy of this quest with the given reward value, preserving any item reward. */
    public Quest withReward(double newReward) {
        return new Quest(this.taskType, this.required, newReward, this.questType, this.rank, this.displayName, this.itemReward);
    }

    /** Returns a copy of this quest with the given item reward (money reward set to 0). */
    public Quest withItemReward(ItemStack newItemReward) {
        return new Quest(this.taskType, this.required, 0.0, this.questType, this.rank, this.displayName, newItemReward);
    }
}
