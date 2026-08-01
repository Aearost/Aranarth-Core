package com.aearost.aranarthcore.enums;

public enum WanderingTraderType {
    MASON("&8Wandering Mason", "Wandering Mason"),
    LUMBERJACK("&2Wandering Lumberjack", "Wandering Lumberjack"),
    FARMER("&aWandering Farmer", "Wandering Farmer"),
    SMITH("&7Wandering Smith", "Wandering Smith"),
    ALCHEMIST("&5Wandering Alchemist", "Wandering Alchemist"),
    TRAVELER("&6Wandering Traveler", "Wandering Traveler"),
    ENCHANTER("&9Wandering Enchanter", "Wandering Enchanter"),
    VANILLA("Wandering Trader", "Wandering Trader");

    private final String coloredName;
    private final String plainName;

    WanderingTraderType(String coloredName, String plainName) {
        this.coloredName = coloredName;
        this.plainName = plainName;
    }

    public String getColoredName() {
        return coloredName;
    }

    public String getPlainName() {
        return plainName;
    }
}
