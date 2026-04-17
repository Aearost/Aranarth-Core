package com.aearost.aranarthcore.objects;

import java.util.*;

/**
 * Holds the configurable permission sets for each DominionRank, including both member ranks
 * (NEWCOMER, CITIZEN, CLERGY, LEADER) and inter-dominion relation ranks
 * (ALLIED, TRUCED, NEUTRAL, WANDERER, ENEMIED).
 */
public class DominionPermissions {

    private final Map<DominionRank, Set<DominionPermission>> permissions;

    /**
     * Creates a new DominionPermissions with the given permission map.
     * @param permissions The permissions per rank (covers both member ranks and relation ranks).
     */
    public DominionPermissions(Map<DominionRank, Set<DominionPermission>> permissions) {
        this.permissions = permissions;
    }

    /**
     * Creates a new DominionPermissions with server defaults.
     * Members and allies default to full interaction access.
     * Newcomers, enemies, neutrals, wanderers, and truced have no interaction access.
     * @return A new DominionPermissions instance with default values.
     */
    public static DominionPermissions createDefaults() {
        Map<DominionRank, Set<DominionPermission>> perms = new EnumMap<>(DominionRank.class);

        Set<DominionPermission> fullInteraction = Set.of(
                DominionPermission.DOOR, DominionPermission.BUTTON, DominionPermission.FENCE_GATE,
                DominionPermission.TRAPDOOR, DominionPermission.LEVER, DominionPermission.PRESSURE_PLATE,
                DominionPermission.CONTAINER, DominionPermission.MISC_INTERACT,
                DominionPermission.ARMOR_STAND, DominionPermission.ITEM_FRAME, DominionPermission.VILLAGER
        );

        // LEADER gets all permissions
        perms.put(DominionRank.LEADER, new HashSet<>(Arrays.asList(DominionPermission.values())));

        // CLERGY gets most permissions — excluded: WITHDRAW
        Set<DominionPermission> clergyPerms = new HashSet<>(fullInteraction);
        clergyPerms.addAll(Arrays.asList(
                DominionPermission.BUILD,
                DominionPermission.HOME, DominionPermission.FOOD,
                DominionPermission.RESOURCES, DominionPermission.INVITE, DominionPermission.REMOVE_MEMBER
        ));
        perms.put(DominionRank.CLERGY, clergyPerms);

        // CITIZEN gets basic permissions
        Set<DominionPermission> citizenPerms = new HashSet<>(fullInteraction);
        citizenPerms.addAll(Arrays.asList(
                DominionPermission.BUILD,
                DominionPermission.HOME, DominionPermission.FOOD
        ));
        perms.put(DominionRank.CITIZEN, citizenPerms);

        // NEWCOMER gets very restricted permissions — no interaction
        perms.put(DominionRank.NEWCOMER, new HashSet<>(List.of(
                DominionPermission.HOME
        )));

        // ALLIED dominions get full interaction access, building rights, and home access
        Set<DominionPermission> alliedPerms = new HashSet<>(fullInteraction);
        alliedPerms.add(DominionPermission.BUILD);
        alliedPerms.add(DominionPermission.HOME);
        perms.put(DominionRank.ALLIED, alliedPerms);

        // TRUCED dominions — no access by default
        perms.put(DominionRank.TRUCED, new HashSet<>());

        // NEUTRAL dominions — no access by default
        perms.put(DominionRank.NEUTRAL, new HashSet<>());

        // WANDERER (no dominion) — no access by default
        perms.put(DominionRank.WANDERER, new HashSet<>());

        // ENEMIED dominions — no access, PvP enabled by default
        Set<DominionPermission> enemiedPerms = new HashSet<>();
        enemiedPerms.add(DominionPermission.PVP);
        perms.put(DominionRank.ENEMIED, enemiedPerms);

        return new DominionPermissions(perms);
    }

    /**
     * Checks if the given rank has the given permission.
     * @param rank The rank to check.
     * @param permission The permission to check.
     * @return True if the rank has the permission.
     */
    public boolean hasPermission(DominionRank rank, DominionPermission permission) {
        Set<DominionPermission> perms = permissions.get(rank);
        return perms != null && perms.contains(permission);
    }

    /**
     * Toggles a permission for the given rank.
     * @param rank The rank to toggle the permission for.
     * @param permission The permission to toggle.
     */
    public void togglePermission(DominionRank rank, DominionPermission permission) {
        Set<DominionPermission> perms = permissions.computeIfAbsent(rank, k -> new HashSet<>());
        if (perms.contains(permission)) {
            perms.remove(permission);
        } else {
            perms.add(permission);
        }
    }

    /**
     * Provides the permissions for the given rank.
     * @param rank The rank.
     * @return The mutable set of permissions for that rank.
     */
    public Set<DominionPermission> getPermissions(DominionRank rank) {
        return permissions.getOrDefault(rank, new HashSet<>());
    }

    /**
     * Provides the full permissions map.
     * @return The permissions map.
     */
    public Map<DominionRank, Set<DominionPermission>> getPermissionsMap() {
        return permissions;
    }
}
