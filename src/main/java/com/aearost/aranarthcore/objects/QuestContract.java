package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.JobType;

import java.util.List;

/**
 * A weekly Job Contract from quests.yml.
 */
public class QuestContract {

    /**
     * One of the two quests a contract is made of.
     *
     * @param questId The quest id from quests.yml.
     * @param amount The amount needed at each rank.
     * @param variant The variant the part always uses, or null to roll one.
     */
    public record Part(String questId, int[] amount, String variant) {
    }

    private final String id;
    private final List<JobType> jobs;
    private final List<Part> parts;
    private final int[] reward;

    public QuestContract(String id, List<JobType> jobs, List<Part> parts, int[] reward) {
        this.id = id;
        this.jobs = jobs;
        this.parts = parts;
        this.reward = reward;
    }

    public String getId() {
        return id;
    }

    public List<JobType> getJobs() {
        return jobs;
    }

    public List<Part> getParts() {
        return parts;
    }

    public int getReward(int rank) {
        if (rank < 0 || rank >= reward.length) {
            return 0;
        }
        return reward[rank];
    }

    public boolean isOffered(int rank) {
        if (getReward(rank) <= 0) {
            return false;
        }
        for (Part part : parts) {
            if (rank >= part.amount().length || part.amount()[rank] <= 0) {
                return false;
            }
        }
        return true;
    }
}
