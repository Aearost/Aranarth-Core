package com.aearost.aranarthcore.enums;

public enum JobType {
    BUILDER,
    FARMER,
    MINER,
    EXCAVATOR,
    LUMBERJACK,
    SMITH,
    EXPLORER,
    ALCHEMIST,
    HUNTER;

    public String getDisplayName() {
        String name = name().charAt(0) + name().substring(1).toLowerCase();
        return name;
    }
}
