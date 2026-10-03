package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import com.aearost.aranarthcore.utils.TradeMarketUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GuiShopLocation {

    public static final String TITLE_KEY = "gui.shoplocation.title";
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

    // Thread pool for parallel skin fetches - keeps fetches concurrent without spawning unbounded threads
    private static final ExecutorService SKIN_EXECUTOR = Executors.newFixedThreadPool(8);

    private final Player player;
    private final Inventory gui;

    private GuiShopLocation(Player player, int pageNum, String filter, Map<UUID, PlayerProfile> profiles) {
        this.player = player;
        this.gui = build(pageNum, filter, profiles);
    }

    public void openGui() {
        player.closeInventory();
        if (gui != null) {
            player.openInventory(gui);
        }
    }

    public void populateInto(Inventory inv) {
        if (gui == null) return;
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }

    /**
     * Opens the shop location GUI. If a filter is active, builds synchronously (no skulls needed).
     * Otherwise, fetches all skull profiles in parallel before opening so the delay is
     * one request latency instead of N * request latency.
     */
    public static void open(Player player, int pageNum) {
        String filter = activeFilters.get(player.getUniqueId());
        if (filter != null) {
            playerPage.put(player.getUniqueId(), pageNum);
            new GuiShopLocation(player, pageNum, filter, Collections.emptyMap()).openGui();
            return;
        }

        HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
        List<UUID> uuidList = new ArrayList<>(shopLocations.keySet());
        int startIndex = pageNum * ITEMS_PER_PAGE;
        List<UUID> pageUuids = new ArrayList<>();
        for (int i = startIndex; i < Math.min(startIndex + ITEMS_PER_PAGE, uuidList.size()); i++) {
            pageUuids.add(uuidList.get(i));
        }

        // Kick off all skin fetches in parallel, then open the GUI once all are ready
        List<CompletableFuture<Map.Entry<UUID, PlayerProfile>>> futures = new ArrayList<>();
        for (UUID uuid : pageUuids) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                PlayerProfile profile = Bukkit.createProfile(uuid, op.getName());
                profile.complete(true);
                return Map.entry(uuid, profile);
            }, SKIN_EXECUTOR));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
            for (CompletableFuture<Map.Entry<UUID, PlayerProfile>> f : futures) {
                Map.Entry<UUID, PlayerProfile> entry = f.join();
                profiles.put(entry.getKey(), entry.getValue());
            }
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                playerPage.put(player.getUniqueId(), pageNum);
                new GuiShopLocation(player, pageNum, null, profiles).openGui();
            });
        });
    }

    /**
     * Rebuilds the GUI in-place into an already-open inventory (filter mode only, no async needed).
     */
    public static void refreshInPlace(Player player, int pageNum, String filter, Inventory inv) {
        playerPage.put(player.getUniqueId(), pageNum);
        new GuiShopLocation(player, pageNum, filter, Collections.emptyMap()).populateInto(inv);
    }

    private Inventory build(int pageNum, String filter, Map<UUID, PlayerProfile> profiles) {
        Inventory inv = Bukkit.createInventory(player, 54, Lang.getFor(player, TITLE_KEY));

        ItemStack grayPane = makeGlass(Material.GRAY_STAINED_GLASS_PANE);

        // Top border row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, grayPane);
        }

        // Search or back button at slot 4
        if (filter != null) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(Lang.getFor(player, "gui.shoplocation.back"));
            List<String> backLore = new ArrayList<>();
            backLore.add(Lang.getFor(player, "gui.shoplocation.searching", "filter", filter));
            backLore.add(Lang.getFor(player, "gui.shoplocation.return_all"));
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
            inv.setItem(SLOT_SEARCH, back);
        } else {
            ItemStack search = new ItemStack(Material.SPYGLASS);
            ItemMeta searchMeta = search.getItemMeta();
            searchMeta.setDisplayName(Lang.getFor(player, "gui.shoplocation.search"));
            List<String> searchLore = new ArrayList<>();
            searchLore.add(Lang.getFor(player, "gui.shoplocation.search_lore"));
            searchMeta.setLore(searchLore);
            search.setItemMeta(searchMeta);
            inv.setItem(SLOT_SEARCH, search);
        }

        // Compute total pages
        int totalEntries = (filter != null)
                ? getFilteredShops(filter).size()
                : AranarthUtils.getShopLocations().size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / ITEMS_PER_PAGE));
        int safePage = Math.min(pageNum, totalPages - 1);

        // Bottom border row
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, grayPane);
        }

        // Prev button
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, makeNavButton(Material.ARROW,
                    Lang.getFor(player, "gui.shoplocation.prev"),
                    Lang.getFor(player, "gui.shoplocation.page", "n", String.valueOf(safePage), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_PREV, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Close / page indicator button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(Lang.getFor(player, "gui.shoplocation.close"));
        closeMeta.setLore(Collections.singletonList(
                Lang.getFor(player, "gui.shoplocation.page", "n", String.valueOf(safePage + 1), "total", String.valueOf(totalPages))));
        closeBtn.setItemMeta(closeMeta);
        inv.setItem(SLOT_CLOSE, closeBtn);

        // Next button
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, makeNavButton(Material.ARROW,
                    Lang.getFor(player, "gui.shoplocation.next"),
                    Lang.getFor(player, "gui.shoplocation.page", "n", String.valueOf(safePage + 2), "total", String.valueOf(totalPages))));
        } else {
            inv.setItem(SLOT_NEXT, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Fill content area
        int guiSlot = ITEMS_START;
        if (filter != null) {
            // Search results: show shop items matching the filter
            List<Shop> results = getFilteredShops(filter);
            int start = safePage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, results.size());
            for (int i = start; i < end; i++) {
                Shop shop = results.get(i);
                AranarthPlayer owner = AranarthUtils.getPlayer(shop.getUuid());
                String defaultName = owner.getNickname() + "&7's Shop";
                String shopName = AranarthUtils.getShopName(shop.getUuid(), defaultName);

                ItemStack displayItem = shop.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta == null) {
                    meta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
                }
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add(Lang.getFor(player, "gui.shoplocation.result_seller", "name", ChatUtils.translateToColor(shopName)));
                    if (shop.getBuyPrice() > 0) {
                        lore.add(Lang.getFor(player, "gui.shoplocation.result_buy",
                                "price", TradeMarketUtils.formatPrice(shop.getBuyPrice()),
                                "qty", String.valueOf(shop.getQuantity())));
                    }
                    if (shop.getSellPrice() > 0) {
                        lore.add(Lang.getFor(player, "gui.shoplocation.result_sell",
                                "price", TradeMarketUtils.formatPrice(shop.getSellPrice()),
                                "qty", String.valueOf(shop.getQuantity())));
                    }
                    lore.add(Lang.getFor(player, "gui.shoplocation.result_click"));
                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                }
                inv.setItem(guiSlot++, displayItem);
            }
        } else {
            // Default view: show player shop owner heads
            HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
            List<UUID> uuidList = new ArrayList<>(shopLocations.keySet());
            int start = safePage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, uuidList.size());
            for (int i = start; i < end; i++) {
                UUID uuid = uuidList.get(i);
                AranarthPlayer shopOwnerPlayer = AranarthUtils.getPlayer(uuid);
                ItemStack shopItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta shopItemMeta = (SkullMeta) shopItem.getItemMeta();

                PlayerProfile profile = profiles.get(uuid);
                if (profile != null) {
                    shopItemMeta.setPlayerProfile(profile);
                }

                String defaultName = shopOwnerPlayer.getNickname() + "'s Shop";
                String shopDisplayName = AranarthUtils.getShopName(uuid, defaultName);
                shopItemMeta.setDisplayName(Lang.getFor(player, "gui.shoplocation.shop_name", "name", shopDisplayName));
                shopItem.setItemMeta(shopItemMeta);
                inv.setItem(guiSlot++, shopItem);
            }
        }

        // Fill remaining content slots with gray pane
        while (guiSlot <= ITEMS_END) {
            inv.setItem(guiSlot++, grayPane);
        }

        return inv;
    }

    /**
     * Returns all shops in the shops world matching the given filter (case-insensitive item name match),
     * sorted alphabetically by item name then by shop owner name.
     */
    public static List<Shop> getFilteredShops(String filter) {
        String lowerFilter = filter.toLowerCase();
        List<Shop> results = new ArrayList<>();
        for (Map.Entry<UUID, List<Shop>> entry : ShopUtils.getShops().entrySet()) {
            if (entry.getKey() == null) continue; // skip server shops
            for (Shop shop : entry.getValue()) {
                if (!ShopIslandUtils.SHOPS_WORLD.equals(shop.getWorldName())) continue;
                ItemStack item = shop.getItem();
                if (item == null) continue;
                String itemName;
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    itemName = ChatUtils.stripColorFormatting(item.getItemMeta().getDisplayName()).toLowerCase();
                } else {
                    itemName = item.getType().name().replace("_", " ").toLowerCase();
                }
                if (itemName.contains(lowerFilter)) {
                    results.add(shop);
                }
            }
        }
        results.sort(Comparator.comparing((Shop s) -> {
            ItemStack item = s.getItem();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return ChatUtils.stripColorFormatting(item.getItemMeta().getDisplayName()).toLowerCase();
            }
            return item != null ? item.getType().name().toLowerCase() : "";
        }).thenComparing(s -> {
            AranarthPlayer owner = AranarthUtils.getPlayer(s.getUuid());
            return owner != null ? owner.getNickname().toLowerCase() : "";
        }));
        return results;
    }

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.get("shoplocation.search_prompt")));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.type_cancel_abort")));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    public static void handleSearchInput(Player player, String query) {
        awaitingSearch.remove(player.getUniqueId());
        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.search_cancelled")));
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> open(player, 0));
            return;
        }
        activeFilters.put(player.getUniqueId(), query);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            playerPage.put(player.getUniqueId(), 0);
            new GuiShopLocation(player, 0, query, Collections.emptyMap()).openGui();
        });
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
        meta.setLore(Collections.singletonList(loreText));
        item.setItemMeta(meta);
        return item;
    }
}
