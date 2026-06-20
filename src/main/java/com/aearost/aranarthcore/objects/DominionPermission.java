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

    // Leader-restricted commands (enforced in commands, not configurable per rank)
    WITHDRAW
}
