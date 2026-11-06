package com.aearost.aranarthcore.enums;

import com.aearost.aranarthcore.utils.PlotUtils;

/**
 * The groups that players are split into in the tab list, based on the world they are in.
 */
public enum TabGroup {

    ARENA("tab.group_arena", false),
    CREATIVE("tab.group_creative", false),
    SPAWN("tab.group_spawn", false),
    SURVIVAL("tab.group_survival", true);

    private final String langKey;
    private final boolean isRightSide;

    TabGroup(String langKey, boolean isRightSide) {
        this.langKey = langKey;
        this.isRightSide = isRightSide;
    }

    public String getLangKey() {
        return langKey;
    }

    public boolean isRightSide() {
        return isRightSide;
    }

    /**
     * Provides the tab group of the input world.
     *
     * @param worldName The name of the world.
     * @return The tab group of the world.
     */
    public static TabGroup fromWorld(String worldName) {
        if (worldName.startsWith("arena")) {
            return ARENA;
        }
        if (worldName.startsWith(PlotUtils.WORLD_NAME)) {
            return CREATIVE;
        }
        if (worldName.startsWith("world") || worldName.startsWith("resource") || worldName.startsWith("smp")) {
            return SURVIVAL;
        }
        return SPAWN;
    }

    /**
     * Provides the tab group matching the input name, sent between servers.
     *
     * @param name The name of the tab group.
     * @return The matching tab group, or Survival if it does not match one.
     */
    public static TabGroup fromName(String name) {
        for (TabGroup group : values()) {
            if (group.name().equals(name)) {
                return group;
            }
        }
        return SURVIVAL;
    }
}
