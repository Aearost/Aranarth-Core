package com.aearost.aranarthcore.enums;

public enum JobType {
    BUILDER,
    FARMER,
    MINER,
    EXCAVATOR,
    LUMBERJACK,
    EXPLORER,
    ALCHEMIST,
    HUNTER;

    public String getDisplayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
