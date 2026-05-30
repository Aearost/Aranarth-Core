package com.aearost.aranarthcore.objects;

/**
 * Represents one element's mount for a single player.
 */
public class Mount {

    public static final int MAX_LEVEL = 10;
    public static final long[] XP_THRESHOLDS = {100, 250, 500, 900, 1400, 2000, 2800, 3700, 5000};
    public static final long RECHARGE_DURATION_MS = 5L * 60L * 1000L; // 5 minutes
    private int healthLevel;
    private long healthXp;
    private int speedLevel;
    private long speedXp;
    private int thirdLevel;
    private long thirdXp;
    private long rechargeEndMs;
    private double currentHealth;
    private String nickname;
    private String harnessColor;

    public Mount() {
        healthLevel = 1;
        healthXp = 0;
        speedLevel = 1;
        speedXp = 0;
        thirdLevel = 1;
        thirdXp = 0;
        rechargeEndMs = 0;
        currentHealth = -1;
        harnessColor = "BROWN";
    }

    public Mount(int healthLevel, long healthXp,
                 int speedLevel, long speedXp,
                 int thirdLevel, long thirdXp,
                 long rechargeEndMs, double currentHealth) {
        this.healthLevel = healthLevel;
        this.healthXp = healthXp;
        this.speedLevel = speedLevel;
        this.speedXp = speedXp;
        this.thirdLevel = thirdLevel;
        this.thirdXp = thirdXp;
        this.rechargeEndMs = rechargeEndMs;
        this.currentHealth = currentHealth;
    }

    public boolean isRecharging() {
        return rechargeEndMs > 0 && System.currentTimeMillis() < rechargeEndMs;
    }

    public long getRechargeRemainingSeconds() {
        if (!isRecharging()) {
            return 0;
        }
        return Math.max(0, (rechargeEndMs - System.currentTimeMillis()) / 1000L);
    }

    /**
     * Begins the recovery countdown. Call when the mount entity dies.
     */
    public void startRecharge() {
        rechargeEndMs = System.currentTimeMillis() + RECHARGE_DURATION_MS;
        currentHealth = -1; // spawn at full health once recovered
    }

    /**
     * Adds XP to the Health skill.
     *
     * @return Whether the skill just leveled up.
     */
    public boolean addHealthXp(long amount) {
        if (healthLevel >= MAX_LEVEL) {
            return false;
        }
        healthXp += amount;
        if (healthXp >= XP_THRESHOLDS[healthLevel - 1]) {
            healthXp -= XP_THRESHOLDS[healthLevel - 1];
            healthLevel = Math.min(healthLevel + 1, MAX_LEVEL);
            return true;
        }
        return false;
    }

    /**
     * Adds XP to the Speed skill.
     *
     * @return Whether the skill just leveled up.
     */
    public boolean addSpeedXp(long amount) {
        if (speedLevel >= MAX_LEVEL) {
            return false;
        }
        speedXp += amount;
        if (speedXp >= XP_THRESHOLDS[speedLevel - 1]) {
            speedXp -= XP_THRESHOLDS[speedLevel - 1];
            speedLevel = Math.min(speedLevel + 1, MAX_LEVEL);
            return true;
        }
        return false;
    }

    /**
     * Adds XP to the third-attribute skill.
     *
     * @return Whether the skill just leveled up.
     */
    public boolean addThirdXp(long amount) {
        if (thirdLevel >= MAX_LEVEL) {
            return false;
        }
        thirdXp += amount;
        if (thirdXp >= XP_THRESHOLDS[thirdLevel - 1]) {
            thirdXp -= XP_THRESHOLDS[thirdLevel - 1];
            thirdLevel = Math.min(thirdLevel + 1, MAX_LEVEL);
            return true;
        }
        return false;
    }

    /**
     * Linearly interpolates a numeric stat across the full level range.
     */
    public static double statForLevel(double min, double max, int level) {
        if (level <= 1) {
            return min;
        }
        if (level >= MAX_LEVEL) {
            return max;
        }
        return min + (level - 1.0) * (max - min) / (MAX_LEVEL - 1.0);
    }

    public long xpNeededForNextHealthLevel() {
        return healthLevel >= MAX_LEVEL ? 0 : XP_THRESHOLDS[healthLevel - 1];
    }

    public long xpNeededForNextSpeedLevel() {
        return speedLevel >= MAX_LEVEL ? 0 : XP_THRESHOLDS[speedLevel - 1];
    }

    public long xpNeededForNextThirdLevel() {
        return thirdLevel >= MAX_LEVEL ? 0 : XP_THRESHOLDS[thirdLevel - 1];
    }

    public int getHealthLevel() {
        return healthLevel;
    }

    public long getHealthXp() {
        return healthXp;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public long getSpeedXp() {
        return speedXp;
    }

    public int getThirdLevel() {
        return thirdLevel;
    }

    public long getThirdXp() {
        return thirdXp;
    }

    public long getRechargeEndMs() {
        return rechargeEndMs;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double hp) {
        this.currentHealth = hp;
    }

    public void setRechargeEndMs(long ms) {
        this.rechargeEndMs = ms;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean hasNickname() {
        return nickname != null && !nickname.isEmpty();
    }

    public void setNickname(String name) {
        this.nickname = (name == null || name.isEmpty()) ? null : name;
    }

    public String getHarnessColor() {
        return harnessColor != null ? harnessColor : "BROWN";
    }

    public void setHarnessColor(String harnessColor) {
        this.harnessColor = (harnessColor == null || harnessColor.isEmpty()) ? "BROWN" : harnessColor;
    }
}
