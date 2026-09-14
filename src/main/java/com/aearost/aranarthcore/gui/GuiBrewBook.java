package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI displaying all brewery recipes the player has unlocked.
 */
public class GuiBrewBook {

    public static final String TITLE_KEY = "gui.brewbook.title";
    private static final int PAGE_SIZE = 36;
    private static final int CONTENT_START = 9;

    private static final Set<UUID> awaitingSearch = new HashSet<>();
    private static final Map<UUID, String> activeFilters = new HashMap<>();

    private final Player player;
    private final int page;
    private final String filter;
    private final Inventory gui;

    public GuiBrewBook(Player player, int page) {
        this(player, page, null);
    }

    public GuiBrewBook(Player player, int page, String filter) {
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

        List<BrewRecipe> unlocked = BrewRecipeUtils.getUnlockedRecipes(player.getUniqueId());
        if (filter != null && !filter.isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            unlocked = unlocked.stream()
                    .filter(r -> r.getDisplayName().toLowerCase().contains(lowerFilter))
                    .toList();
        }

        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, unlocked.size());

        for (int i = start; i < end; i++) {
            inv.setItem(CONTENT_START + (i - start), BrewRecipeUtils.createPotionDisplay(unlocked.get(i), player.getUniqueId()));
        }

        // Search and Back button
        if (filter != null) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(Lang.getFor(player, "gui.brewbook.back"));
            List<String> backLore = new ArrayList<>();
            backLore.add(Lang.getFor(player, "gui.brewbook.searching", "filter", filter));
            backLore.add(Lang.getFor(player, "gui.brewbook.return_all"));
            backMeta.setLore(backLore);
            back.setItemMeta(backMeta);
            inv.setItem(4, back);
        } else {
            ItemStack search = new ItemStack(Material.SPYGLASS);
            ItemMeta searchMeta = search.getItemMeta();
            searchMeta.setDisplayName(Lang.getFor(player, "gui.brewbook.search"));
            List<String> searchLore = new ArrayList<>();
            searchLore.add(Lang.getFor(player, "gui.brewbook.search_lore"));
            searchMeta.setLore(searchLore);
            search.setItemMeta(searchMeta);
            inv.setItem(4, search);
        }

        // Previous page
        ItemStack previous = new ItemStack(Material.RED_WOOL);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(Lang.getFor(player, "gui.page_prev"));
        previous.setItemMeta(previousMeta);
        inv.setItem(45, page > 0 ? previous : glass);

        inv.setItem(46, glass);
        inv.setItem(47, glass);
        inv.setItem(48, glass);

        // Brewing Guide book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName(Lang.getFor(player, "gui.brewbook.guide"));
        List<String> bookLore = new ArrayList<>();
        bookLore.add(Lang.getFor(player, "gui.brewbook.recipes_unlocked", "n", String.valueOf(unlocked.size())));
        bookLore.add(Lang.getFor(player, "gui.brewbook.guide_lore"));
        bookMeta.setLore(bookLore);
        book.setItemMeta(bookMeta);
        inv.setItem(49, book);

        inv.setItem(50, glass);
        inv.setItem(51, glass);

        // Recipe Shop
        ItemStack shopBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta shopMeta = shopBtn.getItemMeta();
        shopMeta.setDisplayName(Lang.getFor(player, "gui.brewbook.shop"));
        List<String> shopLore = new ArrayList<>();
        shopLore.add(Lang.getFor(player, "gui.brewbook.shop_lore"));
        shopMeta.setLore(shopLore);
        shopBtn.setItemMeta(shopMeta);
        inv.setItem(52, shopBtn);

        // Next page
        ItemStack next = new ItemStack(Material.LIME_WOOL);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(Lang.getFor(player, "gui.page_next"));
        next.setItemMeta(nextMeta);
        inv.setItem(53, end < unlocked.size() ? next : glass);

        return inv;
    }

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.get("brew.search_prompt")));
        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.type_cancel_abort")));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    public static void handleSearchInput(Player player, String query) {
        awaitingSearch.remove(player.getUniqueId());
        if (query.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.search_cancelled")));
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> new GuiBrewBook(player, 0).openGui());
            return;
        }
        activeFilters.put(player.getUniqueId(), query);
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> new GuiBrewBook(player, 0, query).openGui());
    }

    public static void clearFilter(UUID uuid) {
        activeFilters.remove(uuid);
    }

    public static String getActiveFilter(UUID uuid) {
        return activeFilters.get(uuid);
    }

    private ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        m.setDisplayName(ChatUtils.translateToColor("&f"));
        g.setItemMeta(m);
        return g;
    }

    public void openGui() {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Updates an already-open brew book inventory in-place without closing it.
     */
    public void populateInto(org.bukkit.inventory.Inventory inv) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }

    public int getPage() { return page; }
}
