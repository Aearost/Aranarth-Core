package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for managing per-player permission overrides within a Dominion.
 */
public class GuiDominionPlayerPermissions {


    private static final Map<UUID, UUID> activeTargets = new HashMap<>();
    private static final Set<UUID> awaitingSearch = new HashSet<>();

    /**
     * Closes the leader's inventory and prompts them to type a username in chat.
     */
    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(
                "&7Enter the username of the player to manage"));
        player.sendMessage(ChatUtils.chatMessage("&7Type &ccancel &7to abort"));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    /**
     * Processes a chat message from a leader who is awaiting user-search input.
     *
     * @param leader The leader who typed.
     * @param input  The raw chat message.
     */
    public static void handleSearchInput(Player leader, String input) {
        awaitingSearch.remove(leader.getUniqueId());

        Dominion dominion = DominionUtils.getPlayerDominion(leader.getUniqueId());
        if (dominion == null) {
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            leader.sendMessage(ChatUtils.chatMessage("&7The search has been cancelled"));
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                    () -> new GuiDominionPermissions(leader).openGui());
            return;
        }

        // Prefer an exact online-player match first (case-insensitive)
        OfflinePlayer target = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(input)) {
                target = online;
                break;
            }
        }

        // Fall back to cached offline player
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(input);
        }

        if (target == null || !target.hasPlayedBefore()) {
            leader.sendMessage(ChatUtils.chatMessage(
                    "&e" + input + " &ccould not be found"));
            return;
        }

        final OfflinePlayer finalTarget = target;
        final String targetName = finalTarget.getName() != null ? finalTarget.getName() : input;

        // Leaders always have all permissions
        if (finalTarget.getUniqueId().equals(dominion.getLeader())) {
            leader.sendMessage(ChatUtils.chatMessage(
                    "&cThe dominion leader always has all permissions"));
            return;
        }

        // Load skull profile asynchronously, then open the GUI on the main thread
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            PlayerProfile profile = Bukkit.createProfile(finalTarget.getUniqueId(), targetName);
            profile.complete(true);
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                    () -> open(leader, dominion, finalTarget.getUniqueId(), targetName, profile));
        });
    }

    /**
     * Opens the player-specific permission GUI for the given target player.
     *
     * @param leader     The dominion leader opening the screen.
     * @param dominion   The leader's dominion.
     * @param targetUuid UUID of the player whose permissions are being edited.
     * @param targetName Display name of the target player.
     * @param profile    Pre-loaded player profile (may be null if unavailable).
     */
    public static void open(Player leader, Dominion dominion, UUID targetUuid, String targetName, PlayerProfile profile) {
        activeTargets.put(leader.getUniqueId(), targetUuid);

        // Determine whether the target is a member or an outsider
        boolean isMember = dominion.getMembers().contains(targetUuid);
        DominionRank effectiveRank;
        boolean isRelation;

        if (isMember) {
            effectiveRank = dominion.getMemberRank(targetUuid);
            if (effectiveRank == null) {
                effectiveRank = DominionRank.NEWCOMER;
            }
            isRelation = false;
        } else {
            Dominion targetDominion = DominionUtils.getPlayerDominion(targetUuid);
            effectiveRank = DominionUtils.getRelationKey(targetDominion, dominion);
            isRelation = true;
        }

        Map<Integer, DominionPermission> slotMap = isRelation
                ? GuiDominionPermissions.getRelationSlotPermissions()
                : GuiDominionPermissions.getRankSlotPermissions();

        Set<DominionPermission> inheritedPerms = dominion.getDominionPermissions().getPermissions(effectiveRank);
        Map<DominionPermission, Boolean> overrides =
                dominion.getPlayerPermissionOverrides().getOrDefault(targetUuid, Collections.emptyMap());

        AranarthPlayer aranarthTarget = AranarthUtils.getPlayer(targetUuid);
        String nickname = aranarthTarget != null ? aranarthTarget.getNickname() : targetName;
        String title = ChatUtils.translateToColor(nickname + "&r's Permissions");
        Inventory gui = Bukkit.createInventory(leader, 45, title);

        // Filler top and bottom rows
        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        // Section headers
        gui.setItem(9, buildSectionHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&b&lInteractions"));
        gui.setItem(18, buildSectionHeader(Material.YELLOW_STAINED_GLASS_PANE, "&e&lBlock Interactions"));
        gui.setItem(27, buildSectionHeader(Material.RED_STAINED_GLASS_PANE, "&c&lCommands"));

        // Permission items
        for (Map.Entry<Integer, DominionPermission> entry : slotMap.entrySet()) {
            DominionPermission perm = entry.getValue();

            // PVP not shown for these relation ranks
            if (isRelation && perm == DominionPermission.PVP
                    && (effectiveRank == DominionRank.NEUTRAL
                    || effectiveRank == DominionRank.ENEMIED
                    || effectiveRank == DominionRank.WANDERER)) {
                continue;
            }

            boolean inherited = inheritedPerms.contains(perm);
            Boolean overrideVal = overrides.get(perm);
            boolean effective = overrideVal != null ? overrideVal : inherited;

            gui.setItem(entry.getKey(), buildPlayerPermissionItem(perm, effective, overrideVal != null));
        }

        // Restore Defaults at slot 4, Back at slot 40
        gui.setItem(4, GuiDominionPermissions.buildRestoreDefaultsButton(
                "&7Restores to base permissions"));
        gui.setItem(40, GuiDominionPermissions.buildBackButton());

        leader.closeInventory();
        leader.openInventory(gui);
    }

    /**
     * Returns the target player UUID for the leader's currently open player-perm screen.
     */
    public static UUID getTarget(UUID leaderUuid) {
        return activeTargets.get(leaderUuid);
    }

    /**
     * Clears the session for the given leader (called on back-navigation or close).
     */
    public static void clearTarget(UUID leaderUuid) {
        activeTargets.remove(leaderUuid);
    }

    /**
     * Builds a permission item showing the effective value.
     */
    public static ItemStack buildPlayerPermissionItem(DominionPermission permission,
                                                      boolean effective,
                                                      boolean isOverridden) {
        ItemStack item = new ItemStack(getPermissionMaterial(permission));
        ItemMeta meta = item.getItemMeta();

        String statusColor = effective ? "&a" : "&c";
        String statusText = effective ? "Yes" : "No";
        String overrideTag = isOverridden ? " &e&l*" : "";
        meta.setDisplayName(ChatUtils.translateToColor(
                "&6&l" + GuiDominionPermissions.formatPermissionName(permission)
                        + " &7&l- " + statusColor + "&l" + statusText + overrideTag));

        item.setItemMeta(meta);
        return item;
    }

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
            case HOME -> Material.RED_BED;
            case OUTPOST_HOME -> Material.GREEN_BED;
            case FOOD -> Material.COOKED_BEEF;
            case RESOURCES -> Material.DIAMOND_PICKAXE;
            case INVITE -> Material.PAPER;
            case REMOVE_MEMBER -> Material.IRON_AXE;
            case SURRENDER -> Material.WHITE_BANNER;
            case REBEL -> Material.CROSSBOW;
            case RETREAT -> Material.FEATHER;
            case MANAGE_OUTPOSTS -> Material.BROWN_BED;
            case MANAGE_DEFENDERS -> Material.SHIELD;
            case WITHDRAW -> Material.GOLD_NUGGET;
        };
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
}
