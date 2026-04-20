package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;

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

    public Quest(QuestTaskType taskType, int required, double reward, QuestType questType, int rank, String displayName) {
        this.taskType = taskType;
        this.required = required;
        this.reward = reward;
        this.questType = questType;
        this.rank = rank;
        this.displayName = displayName;
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

    /** Returns a copy of this quest with the given reward value. */
    public Quest withReward(double newReward) {
        return new Quest(this.taskType, this.required, newReward, this.questType, this.rank, this.displayName);
    }
}
