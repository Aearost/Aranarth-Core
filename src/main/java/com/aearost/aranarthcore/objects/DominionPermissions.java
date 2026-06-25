package com.aearost.aranarthcore.objects;

import java.util.*;

/**
 * Holds the configurable permission sets for each DominionRank, including both member ranks
 * (NEWCOMER, CITIZEN, LIEUTENANT, LEADER) and inter-dominion relation ranks
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

        // LIEUTENANT gets most permissions — excluded: WITHDRAW
        Set<DominionPermission> lieutenantPerms = new HashSet<>(fullInteraction);
        lieutenantPerms.addAll(Arrays.asList(
                DominionPermission.BUILD,
                DominionPermission.HOME, DominionPermission.OUTPOST_HOME, DominionPermission.FOOD,
                DominionPermission.RESOURCES, DominionPermission.INVITE, DominionPermission.REMOVE_MEMBER,
                DominionPermission.SURRENDER, DominionPermission.REBEL, DominionPermission.RETREAT,
                DominionPermission.MANAGE_OUTPOSTS, DominionPermission.MANAGE_DEFENDERS
        ));
        perms.put(DominionRank.LIEUTENANT, lieutenantPerms);

        // CITIZEN gets basic permissions
        Set<DominionPermission> citizenPerms = new HashSet<>(fullInteraction);
        citizenPerms.addAll(Arrays.asList(
                DominionPermission.BUILD,
                DominionPermission.HOME, DominionPermission.OUTPOST_HOME, DominionPermission.FOOD
        ));
        perms.put(DominionRank.CITIZEN, citizenPerms);

        // NEWCOMER gets very restricted permissions — no interaction
        perms.put(DominionRank.NEWCOMER, new HashSet<>(List.of(
                DominionPermission.HOME, DominionPermission.OUTPOST_HOME
        )));

        // ALLIED dominions get full interaction access, building rights, and home access
        Set<DominionPermission> alliedPerms = new HashSet<>(fullInteraction);
        alliedPerms.add(DominionPermission.HOME);
        alliedPerms.add(DominionPermission.OUTPOST_HOME);
        perms.put(DominionRank.ALLIED, alliedPerms);

        // TRUCED dominions — no access by default
        perms.put(DominionRank.TRUCED, new HashSet<>());

        // NEUTRAL dominions — no access by default
        perms.put(DominionRank.NEUTRAL, new HashSet<>());

        // WANDERER (no dominion) — no access by default
        perms.put(DominionRank.WANDERER, new HashSet<>());

        // ENEMIED dominions — no access by default (PvP is hardcoded, not permission-based)
        perms.put(DominionRank.ENEMIED, new HashSet<>());

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

    /**
     * Resets a single rank's permissions to the server-defined defaults.
     * @param rank The rank to reset.
     */
    public void restoreDefaults(DominionRank rank) {
        DominionPermissions defaults = DominionPermissions.createDefaults();
        permissions.put(rank, new HashSet<>(defaults.getPermissions(rank)));
    }
}
