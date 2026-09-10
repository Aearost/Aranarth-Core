package com.aearost.aranarthcore.enums;

public enum FireType {

    DEFAULT("Default"),
    BLUE("Blue"),
    WHITE("White"),
    PRISMATIC("Prismatic");

    private final String displayName;

    FireType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FireType next() {
        FireType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static FireType fromString(String name) {
        for (FireType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return DEFAULT;
    }
}
