package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RecipeGuideEntry;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.RecipeGuideUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI listing every custom recipe in the Aranarth guide, with search support.
 */
public class GuiRecipes {

    public static final String TITLE_KEY = "gui.recipes.title";
    public static final int PAGE_SIZE = 36;
    public static final int CONTENT_START = 9;
    public static final int SLOT_SEARCH = 4;
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_GUIDE = 49;
    public static final int SLOT_NEXT = 53;

    private static final Set<UUID> awaitingSearch = new HashSet<>();
    private static final Map<UUID, String> activeFilters = new HashMap<>();

    private final Player player;
    private final int page;
    private final String filter;
    private final Inventory gui;

    public GuiRecipes(Player player, int page) {
        this(player, page, null);
    }

    public GuiRecipes(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54, Lang.getFor(player, TITLE_KEY));

        ItemStack glass = makeGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        List<RecipeGuideEntry> entries = RecipeGuideUtils.getEntries(filter);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        for (int i = start; i < end; i++) {
            inv.setItem(CONTENT_START + (i - start), createEntryDisplay(entries.get(i)));
        }

        // Search and Back button
        if (filter != null) {
            inv.setItem(SLOT_SEARCH, buildItem(Material.ARROW, Lang.getFor(player, "gui.recipes.back"), List.of(
                    Lang.getFor(player, "gui.recipes.searching", "filter", filter),
                    Lang.getFor(player, "gui.recipes.return_all"))));
        } else {
            inv.setItem(SLOT_SEARCH, buildItem(Material.SPYGLASS, Lang.getFor(player, "gui.recipes.search"), List.of(
                    Lang.getFor(player, "gui.recipes.search_lore"))));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREVIOUS, buildItem(Material.RED_WOOL, Lang.getFor(player, "gui.page_prev"), List.of()));
        }

        inv.setItem(SLOT_GUIDE, buildItem(Material.KNOWLEDGE_BOOK, Lang.getFor(player, "gui.recipes.guide"), List.of(
                Lang.getFor(player, "gui.recipes.count", "n", String.valueOf(entries.size())),
                Lang.getFor(player, "gui.recipes.guide_lore"))));

        if (end < entries.size()) {
            inv.setItem(SLOT_NEXT, buildItem(Material.LIME_WOOL, Lang.getFor(player, "gui.page_next"), List.of()));
        }

        return inv;
    }

    private ItemStack createEntryDisplay(RecipeGuideEntry entry) {
        ItemStack display = entry.getIcon();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.recipes.station_lore",
                "station", Lang.getFor(player, entry.getStation().getLangKey()))));
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.recipes.view_lore")));
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    /**
     * Provides the recipe displayed in the given slot of the given page, or null if there is none.
     */
    public static RecipeGuideEntry getEntryAt(int page, String filter, int slot) {
        if (slot < CONTENT_START || slot >= CONTENT_START + PAGE_SIZE) {
            return null;
        }
        List<RecipeGuideEntry> entries = RecipeGuideUtils.getEntries(filter);
        int index = page * PAGE_SIZE + (slot - CONTENT_START);
        return index < entries.size() ? entries.get(index) : null;
    }

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.get("recipes.search_prompt")));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.type_cancel_abort")));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    public static void handleSearchInput(Player player, String query) {
        awaitingSearch.remove(player.getUniqueId());
        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.search_cancelled")));
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> new GuiRecipes(player, 0).openGui());
            return;
        }
        activeFilters.put(player.getUniqueId(), query);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> new GuiRecipes(player, 0, query).openGui());
    }

    public static void clearFilter(UUID uuid) {
        activeFilters.remove(uuid);
    }

    public static String getActiveFilter(UUID uuid) {
        return activeFilters.get(uuid);
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(lore.stream().map(ChatUtils::translateToColor).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        return glass;
    }

    public void openGui() {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        player.openInventory(gui);
    }

    /**
     * Updates an already-open recipes inventory in-place without closing it.
     */
    public void populateInto(Inventory inv) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }
}
