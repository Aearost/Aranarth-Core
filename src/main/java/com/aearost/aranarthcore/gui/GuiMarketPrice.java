package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.TradeMarketUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiMarketPrice {

    public static final String TITLE_KEY = "gui.marketprice.title";
    public static final int ITEMS_START = 9;
    public static final int ITEMS_END = 44; // Inclusive
    public static final int ITEMS_PER_PAGE = ITEMS_END - ITEMS_START + 1; // 36
    public static final int SLOT_SEARCH = 4;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;

    public static final Map<UUID, Integer> playerPage = new HashMap<>();
    private static final Set<UUID> awaitingSearch = new HashSet<>();
    private static final Map<UUID, String> activeFilters = new HashMap<>();

    private final Player player;
    private final int page;
    private final String filter;
    private final Inventory gui;

    public GuiMarketPrice(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54,
                Lang.getFor(player, TITLE_KEY));

        ItemStack grayGlass = makeGlass(Material.GRAY_STAINED_GLASS_PANE);

        // Top row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayGlass);
        }

        // Search/back button at slot 4
        if (filter != null) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(Lang.getFor(player, "gui.marketprice.back"));
            List<String> backLore = new ArrayList<>();
            backLore.add(Lang.getFor(player, "gui.marketprice.searching", "filter", filter));
            backLore.add(Lang.getFor(player, "gui.marketprice.return_all"));
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
            inv.setItem(SLOT_SEARCH, back);
        } else {
            ItemStack search = new ItemStack(Material.SPYGLASS);
            ItemMeta searchMeta = search.getItemMeta();
            searchMeta.setDisplayName(Lang.getFor(player, "gui.marketprice.search"));
            List<String> searchLore = new ArrayList<>();
            searchLore.add(Lang.getFor(player, "gui.marketprice.search_lore"));
            searchMeta.setLore(searchLore);
            search.setItemMeta(searchMeta);
            inv.setItem(SLOT_SEARCH, search);
        }

        // Build entry list with optional filter
        List<TradeMarketUtils.MarketEntry> entries = TradeMarketUtils.getAllEntries();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            entries = entries.stream()
                    .filter(e -> ChatUtils.stripColorFormatting(e.displayName()).toLowerCase().contains(lowerFilter)
                            || e.key().toLowerCase().contains(lowerFilter))
                    .toList();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        int safePage = Math.min(page, totalPages - 1);

        // Bottom row navigation
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayGlass);
        }

        // Prev button
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, makeNavButton(Material.ARROW, Lang.getFor(player, "gui.marketprice.prev"),
                    Lang.getFor(player, "gui.marketprice.page", "n", String.valueOf(safePage), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_PREV, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Close / page indicator
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(Lang.getFor(player, "gui.marketprice.close"));
        List<String> closeLore = new ArrayList<>();
        closeLore.add(Lang.getFor(player, "gui.marketprice.page", "n", String.valueOf(safePage + 1), "total", String.valueOf(totalPages)));
        closeMeta.setLore(closeLore);
        closeBtn.setItemMeta(closeMeta);
        inv.setItem(SLOT_CLOSE, closeBtn);

        // Next button
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeNavButton(Material.ARROW, Lang.getFor(player, "gui.marketprice.next"),
                    Lang.getFor(player, "gui.marketprice.page", "n", String.valueOf(safePage + 2), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_NEXT, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Fill item slots
        int start = safePage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());
        int guiSlot = ITEMS_START;
        for (int i = start; i < end; i++) {
            TradeMarketUtils.MarketEntry entry = entries.get(i);
            ItemStack displayItem = entry.displayItem().clone();
            displayItem.setAmount(1);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
            }
            if (meta != null) {
                if (!meta.hasDisplayName()) {
                    meta.setDisplayName(ChatUtils.translateToColor("&f" + entry.displayName()));
                }
                List<String> lore = buildEntryLore(entry);
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(guiSlot++, displayItem);
        }

        return inv;
    }

    private List<String> buildEntryLore(TradeMarketUtils.MarketEntry entry) {
        List<String> lore = new ArrayList<>();

        if (entry.fromServerShop()) {
            lore.add(Lang.getFor(player, "gui.marketprice.shop_current_price",
                    "price", TradeMarketUtils.formatPrice(entry.currentPrice())));
            lore.add(Lang.getFor(player, "gui.marketprice.shop_base_price",
                    "price", TradeMarketUtils.formatPrice(entry.basePrice())));
            if (entry.tradePrice() > 0) {
                lore.add(Lang.getFor(player, "gui.marketprice.market_price",
                        "price", TradeMarketUtils.formatPrice(entry.tradePrice())));
            }
        } else {
            lore.add(Lang.getFor(player, "gui.marketprice.market_price",
                    "price", TradeMarketUtils.formatPrice(entry.currentPrice())));
        }

        return lore;
    }

    public void openGui() {
        playerPage.put(player.getUniqueId(), page);
        player.openInventory(gui);
    }

    public void populateInto(Inventory inv) {
        playerPage.put(player.getUniqueId(), page);
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.get("marketprice.search_prompt")));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.type_cancel_abort")));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    public static void handleSearchInput(Player player, String query) {
        awaitingSearch.remove(player.getUniqueId());
        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.search_cancelled")));
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                    () -> new GuiMarketPrice(player, 0, null).openGui());
            return;
        }
        activeFilters.put(player.getUniqueId(), query);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                () -> new GuiMarketPrice(player, 0, query).openGui());
    }

    public static void clearFilter(UUID uuid) {
        activeFilters.remove(uuid);
    }

    public static String getActiveFilter(UUID uuid) {
        return activeFilters.get(uuid);
    }

    private static ItemStack makeGlass(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeNavButton(Material mat, String name, String loreText) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(loreText);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
