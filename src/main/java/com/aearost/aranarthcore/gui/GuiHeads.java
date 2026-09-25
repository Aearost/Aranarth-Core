package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.HeadEntry;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiHeads {

    public static final String TITLE_KEY = "gui.heads.title";
    public static final String ADMIN_TITLE_KEY = "gui.heads.admin_title";
    public static final int HEADS_START = 9;
    public static final int HEADS_END = 44; // inclusive
    public static final int HEADS_PER_PAGE = HEADS_END - HEADS_START + 1; // 36
    public static final int SLOT_SEARCH = 4;
    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;

    // Tracks which page each player is on so click handlers can navigate
    public static final Map<UUID, Integer> playerPage = new HashMap<>();

    private static final Set<UUID> awaitingSearch = new HashSet<>();
    private static final Map<UUID, String> activeFilters = new HashMap<>();

    private final Player player;
    private final int page;
    private final boolean adminMode;
    private final String filter;
    private final Inventory gui;

    public GuiHeads(Player player, int page, boolean adminMode) {
        this(player, page, adminMode, null);
    }

    public GuiHeads(Player player, int page, boolean adminMode, String filter) {
        this.player = player;
        this.page = page;
        this.adminMode = adminMode;
        this.filter = filter;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54,
                Lang.getFor(player, adminMode ? ADMIN_TITLE_KEY : TITLE_KEY));

        ItemStack glass = makeGlass(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack grayGlass = makeGlass(Material.GRAY_STAINED_GLASS_PANE);

        // Top row decoration
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }

        // Search/back button at slot 4
        if (filter != null) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(Lang.getFor(player, "gui.heads.back"));
            List<String> backLore = new ArrayList<>();
            backLore.add(Lang.getFor(player, "gui.heads.searching", "filter", filter));
            backLore.add(Lang.getFor(player, "gui.heads.return_all"));
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
            inv.setItem(SLOT_SEARCH, back);
        } else {
            ItemStack search = new ItemStack(Material.SPYGLASS);
            ItemMeta searchMeta = search.getItemMeta();
            searchMeta.setDisplayName(Lang.getFor(player, "gui.heads.search"));
            List<String> searchLore = new ArrayList<>();
            searchLore.add(Lang.getFor(player, "gui.heads.search_lore"));
            searchMeta.setLore(searchLore);
            search.setItemMeta(searchMeta);
            inv.setItem(SLOT_SEARCH, search);
        }

        // Filter head list
        List<HeadEntry> heads = HeadsDatabaseManager.getExchangeableHeads();
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            heads = heads.stream()
                    .filter(h -> h.name().toLowerCase().contains(lowerFilter))
                    .toList();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) heads.size() / HEADS_PER_PAGE));
        int safePage = Math.min(page, totalPages - 1);

        // Bottom row navigation
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayGlass);
        }

        // Prev button
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, makeNavButton(Material.ARROW, Lang.getFor(player, "gui.heads.prev"),
                    Lang.getFor(player, "gui.heads.page", "n", String.valueOf(safePage), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_PREV, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Page indicator / close
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(Lang.getFor(player, "gui.heads.close"));
        List<String> closeLore = new ArrayList<>();
        closeLore.add(Lang.getFor(player, "gui.heads.page", "n", String.valueOf(safePage + 1), "total", String.valueOf(totalPages)));
        closeMeta.setLore(closeLore);
        closeBtn.setItemMeta(closeMeta);
        inv.setItem(SLOT_CLOSE, closeBtn);

        // Next button
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeNavButton(Material.ARROW, Lang.getFor(player, "gui.heads.next"),
                    Lang.getFor(player, "gui.heads.page", "n", String.valueOf(safePage + 2), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_NEXT, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Fill head slots
        int start = safePage * HEADS_PER_PAGE;
        int end = Math.min(start + HEADS_PER_PAGE, heads.size());
        int guiSlot = HEADS_START;
        for (int i = start; i < end; i++) {
            HeadEntry entry = heads.get(i);
            ItemStack headItem = MobHeadUtils.createCustomHead(entry.texture(), "&f" + entry.name());
            ItemMeta meta = headItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            if (adminMode) {
                lore.add(Lang.getFor(player, "gui.heads.click_take"));
            } else {
                lore.add(Lang.getFor(player, "gui.heads.requires", "material", formatMaterialName(entry.material())));
                lore.add("");
                lore.add(Lang.getFor(player, "gui.heads.click_exchange"));
            }
            meta.setLore(lore);
            headItem.setItemMeta(meta);
            inv.setItem(guiSlot++, headItem);
        }

        // Fill remaining head area slots with gray glass
        while (guiSlot <= HEADS_END) {
            inv.setItem(guiSlot++, grayGlass);
        }

        return inv;
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

    // Search state

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.get("heads.search_prompt")));
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
                    () -> new GuiHeads(player, 0, true).openGui());
            return;
        }
        activeFilters.put(player.getUniqueId(), query);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                () -> new GuiHeads(player, 0, true, query).openGui());
    }

    public static void clearFilter(UUID uuid) {
        activeFilters.remove(uuid);
    }

    public static String getActiveFilter(UUID uuid) {
        return activeFilters.get(uuid);
    }

    // Helpers

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

    static String formatMaterialName(org.bukkit.Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
