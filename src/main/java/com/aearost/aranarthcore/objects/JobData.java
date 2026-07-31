package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.utils.JobUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobData {

    private List<JobType> activeJobs = new ArrayList<>();
    private final Map<JobType, Double> totalXp = new HashMap<>();

    public JobData() {
    }

    public List<JobType> getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(List<JobType> activeJobs) {
        this.activeJobs = activeJobs;
    }

    public double getTotalXp(JobType job) {
        return totalXp.getOrDefault(job, 0.0);
    }

    public void setTotalXp(JobType job, double amount) {
        totalXp.put(job, amount);
    }

    public void addTotalXp(JobType job, double amount) {
        totalXp.merge(job, amount, Double::sum);
    }

    public Map<JobType, Double> getTotalXpMap() {
        return totalXp;
    }

    // Level computed from totalXp against current thresholds
    public int getLevel(JobType job) {
        return JobUtils.computeLevel(getTotalXp(job));
    }

    // XP accumulated within the current level, for progress-bar display
    public double getCurrentXp(JobType job) {
        return JobUtils.computeWithinLevelXp(getTotalXp(job));
    }

    // Used for logging only
    public Map<JobType, Integer> getLevels() {
        Map<JobType, Integer> result = new HashMap<>();
        for (JobType job : activeJobs) {
            result.put(job, getLevel(job));
        }
        return result;
    }

    public boolean hasJob(JobType job) {
        return activeJobs.contains(job);
    }

    public void addJob(JobType job) {
        if (!activeJobs.contains(job)) {
            activeJobs.add(job);
            totalXp.putIfAbsent(job, 0.0);
        }
    }

    public void removeJob(JobType job) {
        activeJobs.remove(job);
    }
}
