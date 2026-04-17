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
 * Clicking a group opens a 27-slot permissions screen grouped into three sections:
 * Interactions, Block Interactions, and Commands.
 *
 * <p>Title format for the main screen: "Dominion Permissions"
 * Title format for a rank screen: "Permissions: {RANK_NAME}"
 * Title format for a relation screen: "Permissions: {RELATION_NAME}"
 */
public class GuiDominionPermissions {

    private static final int MEMBERS_SLOT = 31;

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
     * Returns the slot-to-permission map for a rank sub-screen (27-slot grouped layout).
     * <pre>
     * Row 0 — Buffer:              slots 0-8  (gray panes)
     * Row 1 — Interactions:       slots 10-14 (BUILD, MISC_INTERACT, ARMOR_STAND, ITEM_FRAME, VILLAGER)
     * Row 2 — Block Interactions: slots 19-25 (DOOR, TRAPDOOR, FENCE_GATE, LEVER, BUTTON, PRESSURE_PLATE, CONTAINER)
     * Row 3 — Commands:           slots 28-32 (HOME, FOOD, RESOURCES, INVITE, REMOVE_MEMBER)
     * Row 4 — Buffer:             slots 36-44 (gray panes)
     * </pre>
     */
    public static Map<Integer, DominionPermission> getRankSlotPermissions() {
        Map<Integer, DominionPermission> map = new LinkedHashMap<>();
        map.put(10, DominionPermission.BUILD);
        map.put(11, DominionPermission.MISC_INTERACT);
        map.put(12, DominionPermission.ARMOR_STAND);
        map.put(13, DominionPermission.ITEM_FRAME);
        map.put(14, DominionPermission.VILLAGER);
        map.put(19, DominionPermission.DOOR);
        map.put(20, DominionPermission.TRAPDOOR);
        map.put(21, DominionPermission.FENCE_GATE);
        map.put(22, DominionPermission.LEVER);
        map.put(23, DominionPermission.BUTTON);
        map.put(24, DominionPermission.PRESSURE_PLATE);
        map.put(25, DominionPermission.CONTAINER);
        map.put(28, DominionPermission.HOME);
        map.put(29, DominionPermission.FOOD);
        map.put(30, DominionPermission.RESOURCES);
        map.put(31, DominionPermission.INVITE);
        map.put(32, DominionPermission.REMOVE_MEMBER);
        return map;
    }

    /**
     * Returns the slot-to-permission map for a relation sub-screen (27-slot grouped layout).
     * <pre>
     * Row 0 — Buffer:              slots 0-8  (gray panes)
     * Row 1 — Interactions:       slots 10-15 (BUILD, MISC_INTERACT, ARMOR_STAND, ITEM_FRAME, VILLAGER, PVP)
     * Row 2 — Block Interactions: slots 19-25 (DOOR, TRAPDOOR, FENCE_GATE, LEVER, BUTTON, PRESSURE_PLATE, CONTAINER)
     * Row 3 — Commands:           slot 28    (HOME)
     * Row 4 — Buffer:             slots 36-44 (gray panes)
     * </pre>
     */
    public static Map<Integer, DominionPermission> getRelationSlotPermissions() {
        Map<Integer, DominionPermission> map = new LinkedHashMap<>();
        map.put(10, DominionPermission.BUILD);
        map.put(11, DominionPermission.MISC_INTERACT);
        map.put(12, DominionPermission.ARMOR_STAND);
        map.put(13, DominionPermission.ITEM_FRAME);
        map.put(14, DominionPermission.VILLAGER);
        map.put(15, DominionPermission.PVP);
        map.put(19, DominionPermission.DOOR);
        map.put(20, DominionPermission.TRAPDOOR);
        map.put(21, DominionPermission.FENCE_GATE);
        map.put(22, DominionPermission.LEVER);
        map.put(23, DominionPermission.BUTTON);
        map.put(24, DominionPermission.PRESSURE_PLATE);
        map.put(25, DominionPermission.CONTAINER);
        map.put(28, DominionPermission.HOME);
        return map;
    }

    /**
     * Returns the natural-language title for a permission sub-screen.
     */
    public static String getPermissionsTitle(DominionRank rank) {
        return "Perms for " + switch (rank) {
            case NEWCOMER -> "Newcomers";
            case CITIZEN -> "Citizens";
            case LIEUTENANT -> "the Lieutenant";
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
        String title = ChatUtils.translateToColor(getPermissionsTitle(rank));
        Inventory gui = Bukkit.createInventory(player, 45, title);

        gui.setItem(9,  buildSectionHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE,   "&b&lInteractions"));
        gui.setItem(18, buildSectionHeader(Material.YELLOW_STAINED_GLASS_PANE, "&e&lBlock Interactions"));
        gui.setItem(27, buildSectionHeader(Material.RED_STAINED_GLASS_PANE,  "&c&lCommands"));

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        for (Map.Entry<Integer, DominionPermission> entry : getRankSlotPermissions().entrySet()) {
            gui.setItem(entry.getKey(), buildPermissionItem(entry.getValue(), enabled.contains(entry.getValue())));
        }
        gui.setItem(40, buildBackButton());

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
        String title = ChatUtils.translateToColor(getPermissionsTitle(rank));
        Inventory gui = Bukkit.createInventory(player, 45, title);

        gui.setItem(9,  buildSectionHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE,   "&b&lInteractions"));
        gui.setItem(18, buildSectionHeader(Material.YELLOW_STAINED_GLASS_PANE, "&e&lBlock Interactions"));
        gui.setItem(27, buildSectionHeader(Material.RED_STAINED_GLASS_PANE,  "&c&lCommands"));

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        for (Map.Entry<Integer, DominionPermission> entry : getRelationSlotPermissions().entrySet()) {
            gui.setItem(entry.getKey(), buildPermissionItem(entry.getValue(), enabled.contains(entry.getValue())));
        }
        gui.setItem(40, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    private Inventory initializeMainGui(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return Bukkit.createInventory(player, 9, ChatUtils.translateToColor("Permissions"));
        }

        String title = ChatUtils.translateToColor("Dominion Permissions");
        Inventory gui = Bukkit.createInventory(player, 45, title);

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        // Row 1: Rank groups
        gui.setItem(12, buildGroupItem(Material.IRON_BLOCK, DominionUtils.getFormattedRankName(DominionRank.NEWCOMER), "&7Click to manage permissions"));
        gui.setItem(13, buildGroupItem(Material.GOLD_BLOCK, DominionUtils.getFormattedRankName(DominionRank.CITIZEN), "&7Click to manage permissions"));
        gui.setItem(14, buildGroupItem(Material.DIAMOND_BLOCK, DominionUtils.getFormattedRankName(DominionRank.LIEUTENANT), "&7Click to manage permissions"));

        // Row 2: Relation groups
        gui.setItem(20, buildGroupItem(Material.PURPLE_BANNER, DominionUtils.getFormattedRankName(DominionRank.ALLIED) + " &rDominions", "&7Click to manage permissions"));
        gui.setItem(21, buildGroupItem(Material.PINK_BANNER, DominionUtils.getFormattedRankName(DominionRank.TRUCED) + " &rDominions", "&7Click to manage permissions"));
        gui.setItem(22, buildGroupItem(Material.WHITE_BANNER, DominionUtils.getFormattedRankName(DominionRank.NEUTRAL) + " &rDominions", "&7Click to manage permissions"));
        gui.setItem(23, buildGroupItem(Material.RED_BANNER, DominionUtils.getFormattedRankName(DominionRank.ENEMIED) + " &rDominions", "&7Click to manage permissions"));
        gui.setItem(24, buildGroupItem(Material.LIGHT_GRAY_BANNER, DominionUtils.getFormattedRankName(DominionRank.WANDERER) + "s", "&7Click to manage permissions"));

        // Row 3: Mob Spawning toggle, Members button, and Member PvP toggle
        gui.setItem(27, buildMobSpawningToggleItem(dominion.isMobSpawningEnabled()));
        gui.setItem(35, buildMemberPvpToggleItem(dominion.isMemberPvpEnabled()));
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
     * Builds the Mob Spawning toggle item for the main screen.
     */
    public static ItemStack buildMobSpawningToggleItem(boolean enabled) {
        ItemStack item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        String statusColor = enabled ? "&a" : "&c";
        String statusText = enabled ? "Enabled" : "Disabled";
        meta.setDisplayName(ChatUtils.translateToColor("&6&lMob Spawning &7&l- " + statusColor + "&l" + statusText));
        meta.setLore(List.of(ChatUtils.translateToColor("&7Allows monsters to spawn in dominion chunks")));
        item.setItemMeta(meta);
        return item;
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

    private static ItemStack buildSectionHeader(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(Collections.emptyList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
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

}
