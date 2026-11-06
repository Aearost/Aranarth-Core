package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.QuestType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's daily and weekly quests, along with their re-rolls, board bonuses and recent quest history.
 */
public class PlayerQuestBoard {

    private List<Quest> daily = new ArrayList<>();
    private List<Quest> weekly = new ArrayList<>();
    private long dailyPeriod;
    private long weeklyPeriod;
    private boolean isDailyRerollUsed;
    private boolean isWeeklyRerollUsed;
    private boolean isDailyBonusClaimed;
    private boolean isWeeklyBonusClaimed;
    private long dailySalt;
    private long weeklySalt;
    private final Map<String, Long> history = new HashMap<>();

    public List<Quest> getQuests(QuestType type) {
        return type == QuestType.DAILY ? daily : weekly;
    }

    public void setQuests(QuestType type, List<Quest> quests) {
        if (type == QuestType.DAILY) {
            daily = quests;
        } else {
            weekly = quests;
        }
    }

    public long getPeriod(QuestType type) {
        return type == QuestType.DAILY ? dailyPeriod : weeklyPeriod;
    }

    public void setPeriod(QuestType type, long period) {
        if (type == QuestType.DAILY) {
            dailyPeriod = period;
        } else {
            weeklyPeriod = period;
        }
    }

    public boolean isRerollUsed(QuestType type) {
        return type == QuestType.DAILY ? isDailyRerollUsed : isWeeklyRerollUsed;
    }

    public void setRerollUsed(QuestType type, boolean isUsed) {
        if (type == QuestType.DAILY) {
            isDailyRerollUsed = isUsed;
        } else {
            isWeeklyRerollUsed = isUsed;
        }
    }

    public boolean isBonusClaimed(QuestType type) {
        return type == QuestType.DAILY ? isDailyBonusClaimed : isWeeklyBonusClaimed;
    }

    public void setBonusClaimed(QuestType type, boolean isClaimed) {
        if (type == QuestType.DAILY) {
            isDailyBonusClaimed = isClaimed;
        } else {
            isWeeklyBonusClaimed = isClaimed;
        }
    }

    public long getSalt(QuestType type) {
        return type == QuestType.DAILY ? dailySalt : weeklySalt;
    }

    public void setSalt(QuestType type, long salt) {
        if (type == QuestType.DAILY) {
            dailySalt = salt;
        } else {
            weeklySalt = salt;
        }
    }

    public Map<String, Long> getHistory() {
        return history;
    }

    /**
     * Clears one board so that new quests are assigned the next time it is read. History is kept.
     */
    public void clear(QuestType type) {
        setQuests(type, new ArrayList<>());
        setPeriod(type, 0);
        setRerollUsed(type, false);
        setBonusClaimed(type, false);
    }

    /**
     * @return Whether every quest on the board has been claimed.
     */
    public boolean isAllClaimed(QuestType type) {
        List<Quest> quests = getQuests(type);
        if (quests.isEmpty()) {
            return false;
        }
        for (Quest quest : quests) {
            if (!quest.isClaimed()) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllCompleted(QuestType type) {
        List<Quest> quests = getQuests(type);
        if (quests.isEmpty()) {
            return false;
        }
        for (Quest quest : quests) {
            if (!quest.isCompleted()) {
                return false;
            }
        }
        return true;
    }
}
