package com.aearost.aranarthcore.objects;

/**
 * The behaviour mode of a defender entity.
 */
public enum DefenderMode {

    PATROL("Patrol", "Roams the Dominion's land and attacks hostiles"),
    FOLLOW("Follow", "Follows and defends a player"),
    IDLE("Idle", "A statue that can be moved to a desired position"),
    GUARD("Guard", "Returns to its position after attacking hostiles");

    private final String displayName;
    private final String description;

    DefenderMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the next mode in the cycle.
     */
    public DefenderMode next() {
        DefenderMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
