package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.JobType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobData {

    private List<JobType> activeJobs = new ArrayList<>();
    private Map<JobType, Integer> levels = new HashMap<>();
    private Map<JobType, Double> xp = new HashMap<>();

    public JobData() {
    }

    public List<JobType> getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(List<JobType> activeJobs) {
        this.activeJobs = activeJobs;
    }

    public Map<JobType, Integer> getLevels() {
        return levels;
    }

    public void setLevels(Map<JobType, Integer> levels) {
        this.levels = levels;
    }

    public Map<JobType, Double> getXp() {
        return xp;
    }

    public void setXp(Map<JobType, Double> xp) {
        this.xp = xp;
    }

    public int getLevel(JobType job) {
        return levels.getOrDefault(job, 1);
    }

    public void setLevel(JobType job, int level) {
        levels.put(job, level);
    }

    public double getCurrentXp(JobType job) {
        return xp.getOrDefault(job, 0.0);
    }

    public void setCurrentXp(JobType job, double amount) {
        xp.put(job, amount);
    }

    public boolean hasJob(JobType job) {
        return activeJobs.contains(job);
    }

    public void addJob(JobType job) {
        if (!activeJobs.contains(job)) {
            activeJobs.add(job);
            levels.putIfAbsent(job, 1);
            xp.putIfAbsent(job, 0.0);
        }
    }

    public void removeJob(JobType job) {
        activeJobs.remove(job);
    }
}
