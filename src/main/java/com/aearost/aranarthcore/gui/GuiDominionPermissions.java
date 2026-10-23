package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
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
 * Manages the Dominion Hub GUI - the main menu opened by /dominion (no args).
 *
 * <p>The hub screen shows rank/relation permission groups, toggles (Mob Spawning, Member PvP),
 * and navigation buttons for Members, User Search, Guardians, and Outposts.
 * Clicking a rank or relation group opens a 45-slot permissions sub-screen.
 *
 * <p>Title of the hub screen: {@link #HUB_TITLE_KEY} ("Dominion Hub")
 * Title of rank/relation sub-screens: "[Group] Permissions"
 */
public class GuiDominionPermissions {

    public static final String HUB_TITLE_KEY = "gui.dominionperms.title_hub";

    private static final int MEMBERS_SLOT = 9;
    private static final int USER_SEARCH_SLOT = 10;
    public static final int REGIONS_SLOT = 16;
    public static final int OUTPOSTS_SLOT = 17;
    public static final int RESOURCES_SLOT = 28;
    public static final int BENDING_SLOT = 29;
    public static final int GUARDIANS_SLOT = 30;
    public static final int MOB_SPAWNING_SLOT = 32;
    public static final int MEMBER_PVP_SLOT = 33;
    public static final int EXPLOSIONS_SLOT = 34;

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
     * Row 0 - Buffer:              slots 0-8  (gray panes)
     * Row 1 - Interactions:       slots 10-14 (BUILD, MISC_INTERACT, ARMOR_STAND, ITEM_FRAME, VILLAGER)
     * Row 2 - Block Interactions: slots 19-25 (DOOR, TRAPDOOR, FENCE_GATE, LEVER, BUTTON, PRESSURE_PLATE, CONTAINER)
     * Row 3 - Commands:           slots 28-35 (HOME, FOOD, RESOURCES, INVITE, REMOVE_MEMBER, SURRENDER, REBEL, RETREAT)
     * Row 4 - (no section header) slots 36-37 (OUTPOST_HOME, MANAGE_OUTPOSTS)
     * Row 5 - Buffer:             slots 45-53 (gray panes)
     * </pre>
     * The rank sub-screen uses 54 slots to accommodate the extra row.
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
        map.put(26, DominionPermission.LOCK_CONTAINER);
        map.put(28, DominionPermission.HOME);
        map.put(29, DominionPermission.FOOD);
        map.put(30, DominionPermission.RESOURCES);
        map.put(31, DominionPermission.INVITE);
        map.put(32, DominionPermission.REMOVE_MEMBER);
        map.put(33, DominionPermission.SURRENDER);
        map.put(34, DominionPermission.REBEL);
        map.put(35, DominionPermission.RETREAT);
        map.put(36, DominionPermission.OUTPOST_HOME);
        map.put(37, DominionPermission.MANAGE_OUTPOSTS);
        map.put(38, DominionPermission.MANAGE_DEFENDERS);
        map.put(39, DominionPermission.MANAGE_REGIONS);
        return map;
    }

    /**
     * Returns the slot-to-permission map for a relation sub-screen (27-slot grouped layout).
     * <pre>
     * Row 0 - Buffer:              slots 0-8  (gray panes)
     * Row 1 - Interactions:       slots 10-15 (BUILD, MISC_INTERACT, ARMOR_STAND, ITEM_FRAME, VILLAGER, PVP)
     * Row 2 - Block Interactions: slots 19-25 (DOOR, TRAPDOOR, FENCE_GATE, LEVER, BUTTON, PRESSURE_PLATE, CONTAINER)
     * Row 3 - Commands:           slots 28-29 (HOME, OUTPOST_HOME)
     * Row 4 - Buffer:             slots 36-44 (gray panes)
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
        map.put(26, DominionPermission.LOCK_CONTAINER);
        map.put(28, DominionPermission.HOME);
        map.put(29, DominionPermission.OUTPOST_HOME);
        return map;
    }

    /**
     * Returns the natural-language title for a permission sub-screen.
     */
    public static String getPermissionsTitle(Player player, DominionRank rank) {
        return Lang.getFor(player, switch (rank) {
            case NEWCOMER -> "gui.dominionperms.title_newcomer";
            case CITIZEN -> "gui.dominionperms.title_citizen";
            case LIEUTENANT -> "gui.dominionperms.title_lieutenant";
            case LEADER -> "gui.dominionperms.title_leader";
            case ALLIED -> "gui.dominionperms.title_allied";
            case TRUCED -> "gui.dominionperms.title_truced";
            case NEUTRAL -> "gui.dominionperms.title_neutral";
            case ENEMIED -> "gui.dominionperms.title_enemied";
            case WANDERER -> "gui.dominionperms.title_wanderer";
        });
    }

    /**
     * Marks a rank or relation permission screen and the rank it edits. Click handling identifies the screen by
     * this rather than by its title, as titles are worded differently in each language.
     */
    public record RankScreen(DominionRank rank) {
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
        Inventory gui = GuiHolder.create(player, 54, getPermissionsTitle(player, rank));
        GuiHolder.of(gui).setData(new RankScreen(rank));

        gui.setItem(9, buildSectionHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_interactions")));
        gui.setItem(18, buildSectionHeader(Material.YELLOW_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_blocks")));
        gui.setItem(27, buildSectionHeader(Material.RED_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_commands")));

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(45 + i, filler);
        }

        for (Map.Entry<Integer, DominionPermission> entry : getRankSlotPermissions().entrySet()) {
            gui.setItem(entry.getKey(), buildPermissionItem(player, entry.getValue(), enabled.contains(entry.getValue())));
        }
        gui.setItem(4, buildRestoreDefaultsButton(player, Lang.getFor(player, "gui.dominionperms.restore_rank_lore")));
        gui.setItem(49, buildBackButton(player));

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
        Inventory gui = GuiHolder.create(player, 45, getPermissionsTitle(player, rank));
        GuiHolder.of(gui).setData(new RankScreen(rank));

        gui.setItem(9, buildSectionHeader(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_interactions")));
        gui.setItem(18, buildSectionHeader(Material.YELLOW_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_blocks")));
        gui.setItem(27, buildSectionHeader(Material.RED_STAINED_GLASS_PANE, Lang.getFor(player, "gui.dominionperms.section_commands")));

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        boolean omitPvp = rank == DominionRank.NEUTRAL || rank == DominionRank.ENEMIED || rank == DominionRank.WANDERER;
        for (Map.Entry<Integer, DominionPermission> entry : getRelationSlotPermissions().entrySet()) {
            if (omitPvp && entry.getValue() == DominionPermission.PVP) {
                continue;
            }
            gui.setItem(entry.getKey(), buildPermissionItem(player, entry.getValue(), enabled.contains(entry.getValue())));
        }
        gui.setItem(4, buildRestoreDefaultsButton(player, Lang.getFor(player, "gui.dominionperms.restore_relation_lore")));
        gui.setItem(40, buildBackButton(player));

        player.closeInventory();
        player.openInventory(gui);
    }

    private Inventory initializeMainGui(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return GuiHolder.create(player, 9, Lang.getFor(player, HUB_TITLE_KEY));
        }

        Inventory gui = GuiHolder.create(player, 45, Lang.getFor(player, HUB_TITLE_KEY));

        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(36 + i, filler);
        }

        String groupLore = Lang.getFor(player, "gui.dominionperms.group_lore");
        // Row 1: Rank groups
        gui.setItem(12, buildGroupItem(Material.IRON_BLOCK, DominionUtils.getFormattedRankName(player, DominionRank.NEWCOMER), groupLore));
        gui.setItem(13, buildGroupItem(Material.GOLD_BLOCK, DominionUtils.getFormattedRankName(player, DominionRank.CITIZEN), groupLore));
        gui.setItem(14, buildGroupItem(Material.DIAMOND_BLOCK, DominionUtils.getFormattedRankName(player, DominionRank.LIEUTENANT), groupLore));

        // Row 2: Relation groups
        gui.setItem(20, buildGroupItem(Material.PURPLE_BANNER, Lang.getFor(player, "dominion.group_allied"), groupLore));
        gui.setItem(21, buildGroupItem(Material.PINK_BANNER, Lang.getFor(player, "dominion.group_truced"), groupLore));
        gui.setItem(22, buildGroupItem(Material.WHITE_BANNER, Lang.getFor(player, "dominion.group_neutral"), groupLore));
        gui.setItem(23, buildGroupItem(Material.RED_BANNER, Lang.getFor(player, "dominion.group_enemied"), groupLore));
        gui.setItem(24, buildGroupItem(Material.LIGHT_GRAY_BANNER, Lang.getFor(player, "dominion.group_wanderers"), groupLore));

        // Row 1: navigation
        gui.setItem(MEMBERS_SLOT, buildMembersHeadItem(player, dominion.getLeader()));
        gui.setItem(USER_SEARCH_SLOT, buildUserSearchItem(player));
        gui.setItem(REGIONS_SLOT, buildRegionsItem(player));
        gui.setItem(OUTPOSTS_SLOT, buildOutpostsItem(player));

        // Row 3: Dominion settings
        gui.setItem(RESOURCES_SLOT, buildResourcesItem(player));
        gui.setItem(BENDING_SLOT, buildBendingNavItem(player, dominion.isBendingEnabled()));
        gui.setItem(GUARDIANS_SLOT, buildDefendersItem(player));
        gui.setItem(MOB_SPAWNING_SLOT, buildMobSpawningNavItem(player, dominion.isMobSpawningEnabled()));
        gui.setItem(MEMBER_PVP_SLOT, buildMemberPvpNavItem(player, dominion.isMemberPvpEnabled()));
        gui.setItem(EXPLOSIONS_SLOT, buildExplosionNavItem(player, dominion.isExplosionEnabled()));

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
                initializedGui.setItem(MEMBERS_SLOT, buildMembersHeadItem(player, memberUuid));
                player.updateInventory();
                index[0]++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 20L);
    }

    /**
     * Builds the Explosions nav item for the main screen (opens per-area config).
     */
    public static ItemStack buildExplosionNavItem(Player player, boolean enabled) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        String status = Lang.getFor(player, enabled ? "gui.dominionperms.enabled" : "gui.dominionperms.disabled");
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.toggle_explosions_name", "status", status));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.toggle_explosions")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Mob Spawning nav item for the main screen (opens per-area config).
     */
    public static ItemStack buildMobSpawningNavItem(Player player, boolean enabled) {
        ItemStack item = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        String status = Lang.getFor(player, enabled ? "gui.dominionperms.enabled" : "gui.dominionperms.disabled");
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.toggle_monsters_name", "status", status));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.toggle_monsters")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Bending nav item for the main screen (opens per-area config).
     */
    public static ItemStack buildBendingNavItem(Player player, boolean enabled) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        String status = Lang.getFor(player, enabled ? "gui.dominionperms.enabled" : "gui.dominionperms.disabled");
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.toggle_bending_name", "status", status));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.toggle_bending")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Member PvP nav item for the main screen (opens per-area config).
     */
    public static ItemStack buildMemberPvpNavItem(Player player, boolean enabled) {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        String status = Lang.getFor(player, enabled ? "gui.dominionperms.enabled" : "gui.dominionperms.disabled");
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.toggle_pvp_name", "status", status));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.toggle_pvp")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Defenders hub button.
     */
    public static ItemStack buildDefendersItem(Player player) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.defenders"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.defenders_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Resources hub button.
     */
    public static ItemStack buildResourcesItem(Player player) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.resources_nav"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.resources_nav_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Outposts hub button.
     */
    public static ItemStack buildOutpostsItem(Player player) {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.outposts"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.outposts_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Regions hub button.
     */
    public static ItemStack buildRegionsItem(Player player) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.regions"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.regions_lore")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildMembersHeadItem(Player player, UUID memberUuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUuid);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.members"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.members_lore")));
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildGroupItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(Collections.singletonList(lore));
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
            case LOCK_CONTAINER -> Material.TRIAL_KEY;
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
            case MANAGE_OUTPOSTS -> Material.OAK_LOG;
            case MANAGE_DEFENDERS -> Material.SHIELD;
            case MANAGE_REGIONS -> Material.GRASS_BLOCK;
            case WITHDRAW -> Material.GOLD_NUGGET;
        };
    }

    public static String getPermissionName(Player player, DominionPermission permission) {
        return Lang.getFor(player, "gui.dominionperms.perm_" + permission.name().toLowerCase());
    }

    public static ItemStack buildPermissionItem(Player player, DominionPermission permission, boolean enabled) {
        ItemStack item = new ItemStack(getPermissionMaterial(permission));
        ItemMeta meta = item.getItemMeta();
        String status = Lang.getFor(player, enabled ? "gui.dominionperms.enabled" : "gui.dominionperms.disabled");
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.perm_name", "name", getPermissionName(player, permission), "status", status));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSectionHeader(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
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
     * Builds the User Search button for the main permissions screen.
     */
    public static ItemStack buildUserSearchItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.user_search"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionperms.user_search_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the Restore Defaults button used in permission sub-screens.
     *
     * @param loreLine Descriptive lore explaining what will be restored.
     */
    public static ItemStack buildRestoreDefaultsButton(Player player, String loreLine) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.restore_defaults"));
        meta.setLore(List.of(loreLine));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the back button item used in sub-screens.
     */
    public static ItemStack buildBackButton(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionperms.back"));
        item.setItemMeta(meta);
        return item;
    }


}
