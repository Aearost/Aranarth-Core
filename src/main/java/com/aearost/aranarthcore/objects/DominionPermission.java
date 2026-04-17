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

    // World events
    MOB_SPAWNING,

    // Member commands
    HOME,
    FOOD,
    RESOURCES,
    INVITE,
    REMOVE_MEMBER,

    // Leader-restricted commands (enforced in commands, not configurable per rank)
    WITHDRAW
}
