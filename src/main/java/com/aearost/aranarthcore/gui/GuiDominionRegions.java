package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.commands.general.CommandDominion;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the Dominion Regions GUI, opened from the Dominion Hub or with /d region.
 *
 * <p>Every screen has a row of panes above and below its content. List screens grow with their
 * content up to {@link #MAX_CONTENT_ROWS} rows, after which they are split into pages.
 */
public class GuiDominionRegions {

    public static final int MAX_CONTENT_ROWS = 3;
    public static final int PAGE_SIZE = MAX_CONTENT_ROWS * 9;

    // Region screen
    public static final int INFO_SLOT = 10;
    public static final int ACCESS_SLOT = 12;
    public static final int MEMBERS_SLOT = 13;
    public static final int COLOR_SLOT = 14;
    public static final int DELETE_SLOT = 16;
    public static final int REGION_BACK_SLOT = 22;

    // Color screen
    public static final int RESET_COLOR_SLOT = 25;
    public static final int HEX_COLOR_SLOT = 26;
    public static final int COLOR_BACK_SLOT = 31;

    /**
     * A map color that can be picked in the color screen, along with its dye and the name used by /d mapcolor.
     */
    public record ColorOption(Material dye, String code, String name) {
    }

    public static final List<ColorOption> COLOR_OPTIONS = List.of(
            new ColorOption(Material.WHITE_DYE, "&f", "white"),
            new ColorOption(Material.LIGHT_GRAY_DYE, "&7", "gray"),
            new ColorOption(Material.GRAY_DYE, "&8", "dark_gray"),
            new ColorOption(Material.BLACK_DYE, "&0", "black"),
            new ColorOption(Material.RED_DYE, "&c", "red"),
            new ColorOption(Material.BROWN_DYE, "&4", "dark_red"),
            new ColorOption(Material.ORANGE_DYE, "&6", "gold"),
            new ColorOption(Material.YELLOW_DYE, "&e", "yellow"),
            new ColorOption(Material.LIME_DYE, "&a", "green"),
            new ColorOption(Material.GREEN_DYE, "&2", "dark_green"),
            new ColorOption(Material.LIGHT_BLUE_DYE, "&b", "aqua"),
            new ColorOption(Material.CYAN_DYE, "&3", "dark_aqua"),
            new ColorOption(Material.BLUE_DYE, "&9", "blue"),
            new ColorOption(Material.LAPIS_LAZULI, "&1", "dark_blue"),
            new ColorOption(Material.PINK_DYE, "&d", "pink"),
            new ColorOption(Material.PURPLE_DYE, "&5", "purple"));

    // Players typing a hex code in chat, and the region it is for. Read from the asynchronous chat thread
    private static final Map<UUID, String> awaitingHex = new ConcurrentHashMap<>();

    /**
     * Marks every Dominion Regions screen.
     */
    public sealed interface Screen permits ListScreen, RegionScreen, MembersScreen, ColorScreen {
    }

    /**
     * Marks the region list screen, along with the regions in the order they are displayed.
     */
    public record ListScreen(int page, List<String> regions) implements Screen {
    }

    /**
     * Marks the screen of a single region.
     */
    public record RegionScreen(String regionName) implements Screen {
    }

    /**
     * Marks a region's members screen, along with the Dominion members in the order they are displayed.
     */
    public record MembersScreen(String regionName, int page, List<UUID> members) implements Screen {
    }

    /**
     * Marks a region's map color screen.
     */
    public record ColorScreen(String regionName) implements Screen {
    }

    /**
     * Provides the size of a paged screen for the given amount of content, including the rows of panes.
     */
    public static int getPagedSize(int count) {
        int rows = Math.max(1, Math.min(MAX_CONTENT_ROWS, (count + 8) / 9));
        return (rows + 2) * 9;
    }

    /**
     * Provides the number of the last page for the given amount of content.
     */
    public static int getMaxPage(int count) {
        return Math.max(0, (count - 1) / PAGE_SIZE);
    }

    /**
     * Opens the first page of the region list.
     */
    public static void open(Player player) {
        openList(player, 0);
    }

    /**
     * Opens the given page of the region list.
     */
    public static void openList(Player player, int page) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        List<String> regions = new ArrayList<>(dominion.getRegionMembers().keySet());
        regions.sort(String.CASE_INSENSITIVE_ORDER);
        page = Math.max(0, Math.min(page, getMaxPage(regions.size())));

        Inventory gui = GuiHolder.create(player, getPagedSize(regions.size()), Lang.getFor(player, "gui.dominionregions.title", "name", dominion.getName()));
        GuiHolder.of(gui).setData(new ListScreen(page, regions));
        fillBorders(gui);

        if (regions.isEmpty()) {
            gui.setItem(13, buildNoRegionsItem(player));
        }
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < regions.size(); i++) {
            gui.setItem(9 + i, buildRegionItem(player, dominion, regions.get(page * PAGE_SIZE + i)));
        }
        setPageButtons(player, gui, page, regions.size());

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Opens the screen of a single region, where it is managed.
     */
    public static void openRegion(Player player, String regionName) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null || !dominion.getRegionMembers().containsKey(regionName)) {
            return;
        }

        Inventory gui = GuiHolder.create(player, 27, Lang.getFor(player, "gui.dominionregions.title_region", "name", regionName));
        GuiHolder.of(gui).setData(new RegionScreen(regionName));
        fillBorders(gui);

        boolean isPublic = dominion.isRegionPublic(regionName);
        gui.setItem(INFO_SLOT, buildRegionInfoItem(player, dominion, regionName));
        gui.setItem(ACCESS_SLOT, buildAccessItem(player, isPublic));
        gui.setItem(MEMBERS_SLOT, isPublic ? null : buildMembersItem(player, dominion, regionName, null));
        gui.setItem(COLOR_SLOT, buildColorItem(player, dominion, regionName));
        gui.setItem(DELETE_SLOT, buildDeleteItem(player));
        gui.setItem(REGION_BACK_SLOT, GuiDominionPermissions.buildBackButton(player));

        player.closeInventory();
        player.openInventory(gui);
        startMembersCycleTask(player, gui, dominion, regionName);
    }

    /**
     * Opens the given page of a region's members screen, where Dominion members are added to and removed from it.
     */
    public static void openMembers(Player player, String regionName, int page) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null || !dominion.getRegionMembers().containsKey(regionName)) {
            return;
        }

        // The leader can already build everywhere, so only the other members are listed
        List<UUID> members = new ArrayList<>();
        for (UUID uuid : dominion.getMembers()) {
            if (!uuid.equals(dominion.getLeader())) {
                members.add(uuid);
            }
        }
        page = Math.max(0, Math.min(page, getMaxPage(members.size())));

        Inventory gui = GuiHolder.create(player, getPagedSize(members.size()), Lang.getFor(player, "gui.dominionregions.title_members", "name", regionName));
        GuiHolder.of(gui).setData(new MembersScreen(regionName, page, members));
        fillBorders(gui);

        if (members.isEmpty()) {
            gui.setItem(13, buildNoMembersItem(player));
        }
        Set<UUID> regionMembers = dominion.getRegionMembers().get(regionName);
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < members.size(); i++) {
            UUID uuid = members.get(page * PAGE_SIZE + i);
            gui.setItem(9 + i, buildMemberItem(player, uuid, regionMembers.contains(uuid)));
        }
        setPageButtons(player, gui, page, members.size());

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Opens a region's map color screen.
     */
    public static void openColor(Player player, String regionName) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null || !dominion.getRegionMembers().containsKey(regionName)) {
            return;
        }

        Inventory gui = GuiHolder.create(player, 36, Lang.getFor(player, "gui.dominionregions.title_color", "name", regionName));
        GuiHolder.of(gui).setData(new ColorScreen(regionName));
        fillBorders(gui);
        updateColorOptions(player, gui, dominion, regionName);
        gui.setItem(COLOR_BACK_SLOT, GuiDominionPermissions.buildBackButton(player));

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Sets every color option of an open color screen, marking the region's current color.
     */
    public static void updateColorOptions(Player player, Inventory gui, Dominion dominion, String regionName) {
        String current = dominion.getRegionMapColors().getOrDefault(regionName, "");
        for (int i = 0; i < COLOR_OPTIONS.size(); i++) {
            ColorOption option = COLOR_OPTIONS.get(i);
            gui.setItem(9 + i, buildColorOptionItem(player, option.dye(), getColorDisplayName(player, option), option.code().equals(current)));
        }
        gui.setItem(RESET_COLOR_SLOT, buildColorOptionItem(player, Material.BRUSH, Lang.getFor(player, "gui.dominionregions.color_default"), current.isEmpty()));
        gui.setItem(HEX_COLOR_SLOT, buildHexItem(player, current.startsWith("&#") ? current : null));
    }

    /**
     * Provides the translated, colored name of a color option.
     */
    public static String getColorDisplayName(Player player, ColorOption option) {
        return Lang.getFor(player, "gui.dominionregions.color_" + option.name());
    }

    /**
     * Closes the GUI and prompts the player to type a hex code in chat for the region's map color.
     */
    public static void initiateHexInput(Player player, String regionName) {
        awaitingHex.put(player.getUniqueId(), regionName);
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_hex_prompt", "region", regionName)));
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.type_cancel_abort")));
    }

    public static boolean isAwaitingHex(UUID uuid) {
        return awaitingHex.containsKey(uuid);
    }

    /**
     * Processes a chat message from a player who is typing a hex code for a region's map color.
     *
     * @param player The player who typed.
     * @param input  The raw chat message.
     */
    public static void handleHexInput(Player player, String input) {
        String regionName = awaitingHex.remove(player.getUniqueId());
        // Chat is handled asynchronously, so the change is made on the main thread
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (regionName == null || dominion == null) {
                return;
            }
            // Re-checked, as these may have changed while the player was typing
            if (!DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_REGIONS)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.no_permission_regions")));
                return;
            }
            if (!dominion.getRegionMembers().containsKey(regionName)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_not_found", "region", regionName)));
                return;
            }
            if (!player.hasPermission("aranarth.chat.gradient")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.mapcolor_no_hex_perm")));
                return;
            }

            String trimmed = input.trim();
            if (trimmed.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_hex_cancelled")));
                openColor(player, regionName);
                return;
            }

            String hex = trimmed.startsWith("#") ? trimmed : "#" + trimmed;
            String colorCode = CommandDominion.parseMapColor(hex.toLowerCase());
            if (colorCode == null || !colorCode.startsWith("&#")) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_hex_invalid")));
                openColor(player, regionName);
                return;
            }
            DominionUtils.setRegionMapColor(player, dominion, regionName, colorCode, hex.toUpperCase());
            openColor(player, regionName);
        });
    }

    /**
     * Cycles the members button through the heads of the region's members while the region screen is open.
     */
    private static void startMembersCycleTask(Player player, Inventory gui, Dominion dominion, String regionName) {
        final int[] index = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !gui.equals(player.getOpenInventory().getTopInventory())) {
                    cancel();
                    return;
                }
                Set<UUID> regionMembers = dominion.getRegionMembers().get(regionName);
                // The button is hidden while the region is public
                if (regionMembers == null || dominion.isRegionPublic(regionName)) {
                    return;
                }
                List<UUID> members = new ArrayList<>(regionMembers);
                UUID shown = members.isEmpty() ? null : members.get(index[0] % members.size());
                gui.setItem(MEMBERS_SLOT, buildMembersItem(player, dominion, regionName, shown));
                index[0]++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 20L, 20L);
    }

    /**
     * Builds the item representing a region in the list screen.
     */
    public static ItemStack buildRegionItem(Player player, Dominion dominion, String regionName) {
        ItemStack item = new ItemStack(dominion.getRegionIcon(regionName));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.entry", "name", regionName));
        List<String> lore = buildRegionLore(player, dominion, regionName);
        lore.add(Lang.getFor(player, "gui.dominionregions.click_manage"));
        lore.add(Lang.getFor(player, "gui.dominionregions.icon_hint"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the summary item shown on a region's screen.
     */
    public static ItemStack buildRegionInfoItem(Player player, Dominion dominion, String regionName) {
        ItemStack item = new ItemStack(dominion.getRegionIcon(regionName));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.entry", "name", regionName));
        List<String> lore = buildRegionLore(player, dominion, regionName);
        lore.add(Lang.getFor(player, "gui.dominionregions.icon_hint"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the lore describing a region. Members are left out while the region is public, as they have no effect.
     */
    private static List<String> buildRegionLore(Player player, Dominion dominion, String regionName) {
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.dominionregions.chunks", "count", DominionUtils.getRegionChunkCount(dominion, regionName)));
        boolean isPublic = dominion.isRegionPublic(regionName);
        if (!isPublic) {
            lore.add(Lang.getFor(player, "gui.dominionregions.members", "count", dominion.getRegionMembers().get(regionName).size()));
        }
        lore.add(Lang.getFor(player, isPublic ? "gui.dominionregions.access_public" : "gui.dominionregions.access_private"));
        return lore;
    }

    /**
     * Builds the button that makes a region public or private.
     */
    public static ItemStack buildAccessItem(Player player, boolean isPublic) {
        ItemStack item = new ItemStack(isPublic ? Material.OAK_DOOR : Material.IRON_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, isPublic ? "gui.dominionregions.public" : "gui.dominionregions.private"));
        meta.setLore(List.of(
                Lang.getFor(player, isPublic ? "gui.dominionregions.public_lore" : "gui.dominionregions.private_lore"),
                Lang.getFor(player, isPublic ? "gui.dominionregions.click_private" : "gui.dominionregions.click_public")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the button that opens a region's members screen, showing the head of the given member.
     *
     * @param shown The member whose head is shown, or null for a plain head.
     */
    public static ItemStack buildMembersItem(Player player, Dominion dominion, String regionName, UUID shown) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (shown != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(shown));
        }
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.members_button"));
        meta.setLore(List.of(
                Lang.getFor(player, "gui.dominionregions.members", "count", dominion.getRegionMembers().get(regionName).size()),
                Lang.getFor(player, "gui.dominionregions.members_button_lore")));
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Builds the button that opens a region's map color screen.
     */
    public static ItemStack buildColorItem(Player player, Dominion dominion, String regionName) {
        String colorCode = dominion.getRegionMapColors().get(regionName);
        ItemStack item = new ItemStack(getColorDye(colorCode));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.color_button"));
        meta.setLore(List.of(
                Lang.getFor(player, "gui.dominionregions.color_current", "color", getColorName(player, colorCode)),
                Lang.getFor(player, "gui.dominionregions.color_button_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Provides the dye shown for a map color.
     */
    private static Material getColorDye(String colorCode) {
        if (colorCode != null) {
            for (ColorOption option : COLOR_OPTIONS) {
                if (option.code().equals(colorCode)) {
                    return option.dye();
                }
            }
            if (colorCode.startsWith("&#")) {
                return Material.NAME_TAG;
            }
        }
        return Material.BRUSH;
    }

    /**
     * Provides the colored name of a map color as it is shown to the player.
     */
    private static String getColorName(Player player, String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return Lang.getFor(player, "gui.dominionregions.color_default");
        }
        for (ColorOption option : COLOR_OPTIONS) {
            if (option.code().equals(colorCode)) {
                return getColorDisplayName(player, option);
            }
        }
        return ChatUtils.translateToColor(colorCode + "#" + colorCode.substring(2));
    }

    private static ItemStack buildColorOptionItem(Player player, Material material, String name, boolean selected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(Lang.getFor(player, selected ? "gui.dominionregions.color_selected" : "gui.dominionregions.color_select")));
        meta.setEnchantmentGlintOverride(selected);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the option to type a hex code, marked as selected when the region uses one.
     *
     * @param currentHex The region's hex color, or null if it does not use one.
     */
    private static ItemStack buildHexItem(Player player, String currentHex) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.color_hex"));
        List<String> lore = new ArrayList<>();
        if (currentHex != null) {
            lore.add(Lang.getFor(player, "gui.dominionregions.color_current", "color", getColorName(player, currentHex)));
        }
        lore.add(Lang.getFor(player, player.hasPermission("aranarth.chat.gradient") ? "gui.dominionregions.color_hex_lore" : "gui.dominionregions.color_hex_locked"));
        meta.setLore(lore);
        meta.setEnchantmentGlintOverride(currentHex != null);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the head of a Dominion member on a region's members screen.
     */
    public static ItemStack buildMemberItem(Player player, UUID uuid, boolean inRegion) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.member", "player", AranarthUtils.getNickname(offlinePlayer)));
        meta.setLore(List.of(
                Lang.getFor(player, inRegion ? "gui.dominionregions.in_region" : "gui.dominionregions.not_in_region"),
                Lang.getFor(player, inRegion ? "gui.dominionregions.click_remove" : "gui.dominionregions.click_add")));
        meta.setEnchantmentGlintOverride(inRegion);
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildNoRegionsItem(Player player) {
        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.none"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionregions.none_lore")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildNoMembersItem(Player player) {
        ItemStack item = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.no_members"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDeleteItem(Player player) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.dominionregions.delete"));
        meta.setLore(List.of(Lang.getFor(player, "gui.dominionregions.delete_lore")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Places the back button, along with the page buttons when there is more than one page, on the bottom row.
     */
    private static void setPageButtons(Player player, Inventory gui, int page, int count) {
        int bottomRow = gui.getSize() - 9;
        if (page > 0) {
            gui.setItem(bottomRow, buildNavItem(Material.ARROW, Lang.getFor(player, "gui.page_prev")));
        }
        if (page < getMaxPage(count)) {
            gui.setItem(bottomRow + 8, buildNavItem(Material.ARROW, Lang.getFor(player, "gui.page_next")));
        }
        gui.setItem(bottomRow + 4, GuiDominionPermissions.buildBackButton(player));
    }

    /**
     * Fills the top and bottom rows with panes.
     */
    private static void fillBorders(Inventory gui) {
        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(gui.getSize() - 9 + i, filler);
        }
    }

    private static ItemStack buildNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
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
