package com.aearost.aranarthcore.objects;

/**
 * Represents an individual permission that can be toggled per rank or relation within a Dominion.
 */
public enum DominionPermission {
    // Physical world interactions
    BUILD,

    // Specific block interaction permissions
    DOOR,
    BUTTON,
    FENCE_GATE,
    TRAPDOOR,
    LEVER,
    PRESSURE_PLATE,
    CONTAINER,
    LOCK_CONTAINER,
    MISC_INTERACT,

    // Entity interaction permissions
    ARMOR_STAND,
    ITEM_FRAME,
    VILLAGER,

    // Combat
    PVP,

    // Member commands
    HOME,
    OUTPOST_HOME,
    FOOD,
    RESOURCES,
    INVITE,
    REMOVE_MEMBER,

    // War commands (lieutenant-accessible by default, configurable)
    SURRENDER,
    REBEL,
    RETREAT,

    // Outpost management (create is leader-only, but rename/sethome/claim are configurable)
    MANAGE_OUTPOSTS,

    // Defender management (buy/sell defenders)
    MANAGE_DEFENDERS,

    // Region management (assign/unassign chunks to members; also grants bypass of region restrictions)
    MANAGE_REGIONS,

    // Leader-restricted commands (enforced in commands, not configurable per rank)
    WITHDRAW;

    /**
     * Provides the permission with the given stored name, including names that were since renamed.
     *
     * @param name The stored permission name.
     * @return The permission.
     * @throws IllegalArgumentException If no permission has that name.
     */
    public static DominionPermission fromName(String name) {
        // Regions were previously called plots
        if (name.equals("MANAGE_PLOTS")) {
            return MANAGE_REGIONS;
        }
        return valueOf(name);
    }
}
