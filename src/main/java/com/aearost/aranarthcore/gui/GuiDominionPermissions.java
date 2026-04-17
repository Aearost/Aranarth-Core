package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages the Dominion Permissions GUI.
 *
 * <p>The main screen shows selectable groups (ranks and relations).
 * Clicking a group opens the permissions list for that group.
 *
 * <p>Title format for the main screen: "Dominion Permissions"
 * Title format for a rank screen: "Permissions: {RANK_NAME}"
 * Title format for a relation screen: "Permissions: {RELATION_NAME}"
 */
public class GuiDominionPermissions {

    private static final int MEMBERS_SLOT = 22;

    private final Player player;
    private final Inventory initializedGui;

    public GuiDominionPermissions(Player player) {
        this.player = player;
        this.initializedGui = initializeMainGui(player);
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(initializedGui);
        startHeadCycleTask();
    }

    /**
     * Returns the list of permissions shown in the GUI for a rank sub-screen.
     * Excludes permissions not relevant to member ranks.
     */
    public static DominionPermission[] getRankDisplayPermissions() {
        return new DominionPermission[]{
                DominionPermission.BUILD,
                DominionPermission.DOOR, DominionPermission.BUTTON, DominionPermission.FENCE_GATE,
                DominionPermission.TRAPDOOR, DominionPermission.LEVER, DominionPermission.PRESSURE_PLATE,
                DominionPermission.CONTAINER, DominionPermission.MISC_INTERACT,
                DominionPermission.ARMOR_STAND, DominionPermission.ITEM_FRAME, DominionPermission.VILLAGER,
                DominionPermission.MOB_SPAWNING,
                DominionPermission.HOME, DominionPermission.FOOD,
                DominionPermission.RESOURCES, DominionPermission.INVITE, DominionPermission.REMOVE_MEMBER
        };
    }

    /**
     * Returns the list of permissions shown in the GUI for a relation sub-screen.
     */
    public static DominionPermission[] getRelationDisplayPermissions() {
        return new DominionPermission[]{
                DominionPermission.BUILD,
                DominionPermission.DOOR, DominionPermission.BUTTON, DominionPermission.FENCE_GATE,
                DominionPermission.TRAPDOOR, DominionPermission.LEVER, DominionPermission.PRESSURE_PLATE,
                DominionPermission.CONTAINER, DominionPermission.MISC_INTERACT,
                DominionPermission.ARMOR_STAND, DominionPermission.ITEM_FRAME, DominionPermission.VILLAGER,
                DominionPermission.PVP, DominionPermission.MOB_SPAWNING
        };
    }

    /**
     * Returns the natural-language title for a permission sub-screen.
     */
    public static String getPermissionsTitle(DominionRank rank) {
        return "Perms for " + switch (rank) {
            case NEWCOMER -> "Newcomers";
            case CITIZEN -> "Citizens";
            case CLERGY -> "the Clergy";
            case LEADER -> "the Leader";
            case ALLIED -> "Allied Dominions";
            case TRUCED -> "Truced Dominions";
            case NEUTRAL -> "Neutral Dominions";
            case ENEMIED -> "Enemied Dominions";
            case WANDERER -> "Wanderers";
        };
    }

    /**
     * Parses the DominionRank from a permission sub-screen title.
     * @return The matching rank, or null if unrecognised.
     */
    public static DominionRank getRankFromTitle(String strippedTitle) {
        for (DominionRank rank : DominionRank.values()) {
            if (strippedTitle.equals(getPermissionsTitle(rank))) {
                return rank;
            }
        }
        return null;
    }

    /**
     * Opens the permission toggle screen for a specific member rank.
     */
    public static void openRankGui(Player player, DominionRank rank) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        DominionPermission[] displayPerms = getRankDisplayPermissions();
        int size = calculateSize(displayPerms.length + 1);

        String title = ChatUtils.translateToColor(getPermissionsTitle(rank));
        Inventory gui = Bukkit.createInventory(player, size, title);

        for (int i = 0; i < displayPerms.length; i++) {
            DominionPermission perm = displayPerms[i];
            gui.setItem(i, buildPermissionItem(perm, enabled.contains(perm)));
        }
        gui.setItem(gui.getSize() - 1, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Opens the permission toggle screen for a specific relation rank.
     */
    public static void openRelationGui(Player player, DominionRank rank) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        Set<DominionPermission> enabled = dominion.getDominionPermissions().getPermissions(rank);
        DominionPermission[] displayPerms = getRelationDisplayPermissions();
        int size = calculateSize(displayPerms.length + 1);

        String title = ChatUtils.translateToColor(getPermissionsTitle(rank));
        Inventory gui = Bukkit.createInventory(player, size, title);

        for (int i = 0; i < displayPerms.length; i++) {
            DominionPermission perm = displayPerms[i];
            gui.setItem(i, buildPermissionItem(perm, enabled.contains(perm)));
        }
        gui.setItem(gui.getSize() - 1, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    private Inventory initializeMainGui(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return Bukkit.createInventory(player, 9, ChatUtils.translateToColor("Permissions"));
        }

        String title = ChatUtils.translateToColor("Dominion Permissions");
        Inventory gui = Bukkit.createInventory(player, 27, title);

        // Row 0: Rank groups
        gui.setItem(3, buildGroupItem(Material.IRON_BLOCK, DominionUtils.getFormattedRankName(DominionRank.NEWCOMER), ""));
        gui.setItem(4, buildGroupItem(Material.GOLD_BLOCK, DominionUtils.getFormattedRankName(DominionRank.CITIZEN), ""));
        gui.setItem(5, buildGroupItem(Material.DIAMOND_BLOCK, DominionUtils.getFormattedRankName(DominionRank.CLERGY), ""));

        // Row 1: Relation groups
        gui.setItem(11, buildGroupItem(Material.PURPLE_BANNER, DominionUtils.getFormattedRankName(DominionRank.ALLIED) + " &rDominions", ""));
        gui.setItem(12, buildGroupItem(Material.PINK_BANNER, DominionUtils.getFormattedRankName(DominionRank.TRUCED) + " &rDominions", ""));
        gui.setItem(13, buildGroupItem(Material.WHITE_BANNER, DominionUtils.getFormattedRankName(DominionRank.NEUTRAL) + " &rDominions", ""));
        gui.setItem(14, buildGroupItem(Material.RED_BANNER, DominionUtils.getFormattedRankName(DominionRank.ENEMIED) + " &rDominions", ""));
        gui.setItem(15, buildGroupItem(Material.LIGHT_GRAY_BANNER, DominionUtils.getFormattedRankName(DominionRank.WANDERER) + "s", ""));

        // Row 2: Member PvP toggle and Members button
        gui.setItem(26, buildMemberPvpToggleItem(dominion.isMemberPvpEnabled()));
        gui.setItem(MEMBERS_SLOT, buildMembersHeadItem(dominion.getLeader()));

        return gui;
    }

    private void startHeadCycleTask() {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        List<UUID> members = new ArrayList<>(dominion.getMembers());
        if (members.isEmpty()) {
            return;
        }

        final int[] index = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()
                        || !initializedGui.equals(player.getOpenInventory().getTopInventory())) {
                    cancel();
                    return;
                }
                UUID memberUuid = members.get(index[0] % members.size());
                initializedGui.setItem(MEMBERS_SLOT, buildMembersHeadItem(memberUuid));
                player.updateInventory();
                index[0]++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 20L);
    }

    /**
     * Builds the Member PvP toggle item for the main screen.
     */
    public static ItemStack buildMemberPvpToggleItem(boolean enabled) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        String statusColor = enabled ? "&a" : "&c";
        String statusText = enabled ? "Enabled" : "Disabled";
        meta.setDisplayName(ChatUtils.translateToColor("&6&lMember PvP &7&l- " + statusColor + "&l" + statusText));
        meta.setLore(List.of(ChatUtils.translateToColor("&7Allows dominion members to harm each other")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildMembersHeadItem(UUID memberUuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUuid);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(ChatUtils.translateToColor("&eMembers"));
        meta.setLore(List.of(ChatUtils.translateToColor("&7Click to manage member ranks")));
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildGroupItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(Collections.singletonList(ChatUtils.translateToColor(lore)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns the thematic material for a given permission.
     */
    private static Material getPermissionMaterial(DominionPermission permission) {
        return switch (permission) {
            case BUILD -> Material.CRAFTING_TABLE;
            case DOOR -> Material.OAK_DOOR;
            case BUTTON -> Material.OAK_BUTTON;
            case FENCE_GATE -> Material.OAK_FENCE_GATE;
            case TRAPDOOR -> Material.OAK_TRAPDOOR;
            case LEVER -> Material.LEVER;
            case PRESSURE_PLATE -> Material.OAK_PRESSURE_PLATE;
            case CONTAINER -> Material.CHEST;
            case MISC_INTERACT -> Material.MAGENTA_GLAZED_TERRACOTTA;
            case ARMOR_STAND -> Material.ARMOR_STAND;
            case ITEM_FRAME -> Material.ITEM_FRAME;
            case VILLAGER -> Material.EMERALD;
            case PVP -> Material.IRON_SWORD;
            case MOB_SPAWNING -> Material.ZOMBIE_SPAWN_EGG;
            case HOME -> Material.COMPASS;
            case FOOD -> Material.COOKED_BEEF;
            case RESOURCES -> Material.DIAMOND_PICKAXE;
            case INVITE -> Material.PAPER;
            case REMOVE_MEMBER -> Material.IRON_AXE;
            case WITHDRAW -> Material.GOLD_NUGGET;
        };
    }

    private static ItemStack buildPermissionItem(DominionPermission permission, boolean enabled) {
        ItemStack item = new ItemStack(getPermissionMaterial(permission));
        ItemMeta meta = item.getItemMeta();
        String statusColor = enabled ? "&a" : "&c";
        String statusText = enabled ? "Yes" : "No";
        meta.setDisplayName(ChatUtils.translateToColor("&6&l" + formatPermissionName(permission) + " &7&l- " + statusColor + "&l" + statusText));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the back button item used in sub-screens.
     */
    public static ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&c&lBack"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Formats a DominionPermission enum name into a human-readable string.
     */
    public static String formatPermissionName(DominionPermission permission) {
        String raw = permission.name().replace("_", " ");
        StringBuilder formatted = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                if (word.equalsIgnoreCase("pvp")) {
                    formatted.append("PvP")
                            .append(" ");
                } else {
                    formatted.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1).toLowerCase())
                            .append(" ");
                }
            }
        }
        return formatted.toString().trim();
    }

    private static int calculateSize(int items) {
        if (items <= 9) return 9;
        if (items <= 18) return 18;
        if (items <= 27) return 27;
        if (items <= 36) return 36;
        if (items <= 45) return 45;
        return 54;
    }
}
