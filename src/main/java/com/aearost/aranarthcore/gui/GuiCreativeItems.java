package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.*;
import com.aearost.aranarthcore.items.aranarthium.armour.*;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.items.arrow.*;
import com.aearost.aranarthcore.items.arrowhead.*;
import com.aearost.aranarthcore.items.essence.*;
import com.aearost.aranarthcore.items.netherite.NetheriteElytra;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.RecipeGuideUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI listing every custom item so that players in the Creative worlds can take them.
 */
public class GuiCreativeItems {

    public static final String TITLE_KEY = "gui.creativeitems.title";
    public static final int PAGE_SIZE = 36;
    public static final int CONTENT_START = 9;
    public static final int SLOT_SEARCH = 4;
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_NEXT = 53;

    private static List<ItemStack> items;

    private static final Set<UUID> awaitingSearch = new HashSet<>();
    private static final Map<UUID, String> activeFilters = new HashMap<>();

    private final Player player;
    private final int page;
    private final String filter;
    private final Inventory gui;

    public GuiCreativeItems(Player player, int page) {
        this(player, page, null);
    }

    public GuiCreativeItems(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.gui = build();
    }

    /**
     * Provides whether the player may take items from this GUI, being in creative mode in a Creative world.
     */
    public static boolean canUse(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
                && AranarthUtils.getInventoryGroup(player.getWorld().getName()).equals("creative");
    }

    /**
     * Provides every custom item available to the player in display order, or only those whose name matches the filter.
     */
    private static List<ItemStack> getItems(Player player, String filter) {
        if (items == null) {
            items = createItems();
        }
        boolean hasFramePerk = player.hasPermission("aranarth.invisible_item_frame");
        String lowerFilter = filter == null || filter.isEmpty() ? null : filter.toLowerCase(Locale.ROOT);
        if (hasFramePerk && lowerFilter == null) {
            return items;
        }
        ItemStack invisibleItemFrame = new InvisibleItemFrame().getItem();
        return items.stream()
                .filter(item -> hasFramePerk || !item.isSimilar(invisibleItemFrame))
                .filter(item -> lowerFilter == null || RecipeGuideUtils.getName(item).toLowerCase(Locale.ROOT).contains(lowerFilter))
                .toList();
    }

    private static List<ItemStack> createItems() {
        List<ItemStack> list = new ArrayList<>();
        // Clusters and ingots
        list.add(new CopperCluster().getItem());
        list.add(new IronCluster().getItem());
        list.add(new GoldCluster().getItem());
        list.add(new RedstoneCluster().getItem());
        list.add(new LapisCluster().getItem());
        list.add(new QuartzCluster().getItem());
        list.add(new DiamondCluster().getItem());
        list.add(new EmeraldCluster().getItem());
        list.add(new AranarthiumIngot().getItem());
        list.add(new AranarthiumAquatic().getItem());
        list.add(new AranarthiumArdent().getItem());
        list.add(new AranarthiumDwarven().getItem());
        list.add(new AranarthiumElven().getItem());
        list.add(new AranarthiumFae().getItem());
        list.add(new AranarthiumScorched().getItem());
        list.add(new AranarthiumSoulbound().getItem());
        list.add(new NetheriteElytra().getItem());
        list.add(new GodAppleFragment().getItem());

        // Aranarthium armour sets
        list.add(new AquaticAranarthiumHelmet().getItem());
        list.add(new AquaticAranarthiumChestplate().getItem());
        list.add(new AquaticAranarthiumLeggings().getItem());
        list.add(new AquaticAranarthiumBoots().getItem());
        list.add(new AquaticAranarthiumElytra().getItem());
        list.add(new ArdentAranarthiumHelmet().getItem());
        list.add(new ArdentAranarthiumChestplate().getItem());
        list.add(new ArdentAranarthiumLeggings().getItem());
        list.add(new ArdentAranarthiumBoots().getItem());
        list.add(new ArdentAranarthiumElytra().getItem());
        list.add(new DwarvenAranarthiumHelmet().getItem());
        list.add(new DwarvenAranarthiumChestplate().getItem());
        list.add(new DwarvenAranarthiumLeggings().getItem());
        list.add(new DwarvenAranarthiumBoots().getItem());
        list.add(new DwarvenAranarthiumElytra().getItem());
        list.add(new ElvenAranarthiumHelmet().getItem());
        list.add(new ElvenAranarthiumChestplate().getItem());
        list.add(new ElvenAranarthiumLeggings().getItem());
        list.add(new ElvenAranarthiumBoots().getItem());
        list.add(new ElvenAranarthiumElytra().getItem());
        list.add(new FaeAranarthiumHelmet().getItem());
        list.add(new FaeAranarthiumChestplate().getItem());
        list.add(new FaeAranarthiumLeggings().getItem());
        list.add(new FaeAranarthiumBoots().getItem());
        list.add(new FaeAranarthiumElytra().getItem());
        list.add(new ScorchedAranarthiumHelmet().getItem());
        list.add(new ScorchedAranarthiumChestplate().getItem());
        list.add(new ScorchedAranarthiumLeggings().getItem());
        list.add(new ScorchedAranarthiumBoots().getItem());
        list.add(new ScorchedAranarthiumElytra().getItem());
        list.add(new SoulboundAranarthiumHelmet().getItem());
        list.add(new SoulboundAranarthiumChestplate().getItem());
        list.add(new SoulboundAranarthiumLeggings().getItem());
        list.add(new SoulboundAranarthiumBoots().getItem());
        list.add(new SoulboundAranarthiumElytra().getItem());

        // Arrowheads and arrows
        list.add(new Arrowhead().getItem());
        list.add(new ArrowheadIron().getItem());
        list.add(new ArrowheadGold().getItem());
        list.add(new ArrowheadAmethyst().getItem());
        list.add(new ArrowheadObsidian().getItem());
        list.add(new ArrowheadDiamond().getItem());
        list.add(new ArrowheadExplosive().getItem());
        list.add(new ArrowheadLightning().getItem());
        list.add(new ArrowheadSpectral().getItem());
        list.add(new ArrowheadRooting().getItem());
        list.add(new ArrowheadGust().getItem());
        list.add(new ArrowheadDragon().getItem());
        list.add(new ArrowIron().getItem());
        list.add(new ArrowGold().getItem());
        list.add(new ArrowAmethyst().getItem());
        list.add(new ArrowObsidian().getItem());
        list.add(new ArrowDiamond().getItem());
        list.add(new ArrowExplosive().getItem());
        list.add(new ArrowLightning().getItem());
        list.add(new ArrowRooting().getItem());
        list.add(new ArrowGust().getItem());
        list.add(new ArrowDragon().getItem());

        // Essences
        list.add(new EssenceBlank().getItem());
        list.add(new EssenceBeheading().getItem());
        list.add(new EssenceLifesteal().getItem());
        list.add(new EssenceMagnetism().getItem());
        list.add(new EssencePlentiful().getItem());
        list.add(new EssencePreservation().getItem());
        list.add(new EssenceResilience().getItem());

        // Miscellaneous
        list.add(new Quiver().getItem());
        list.add(new Wrench().getItem());
        list.add(new InvisibleItemFrame().getItem());
        list.add(new ChorusDiamond().getItem());
        list.add(new SugarcaneBlock().getItem());
        list.add(new HoneyGlazedHam().getItem());
        return List.copyOf(list);
    }

    /**
     * Provides a fresh copy of the custom item displayed in the given slot of the given page, or null if there is none.
     */
    public static ItemStack getItemAt(Player player, int page, String filter, int slot) {
        if (slot < CONTENT_START || slot >= CONTENT_START + PAGE_SIZE) {
            return null;
        }
        int index = page * PAGE_SIZE + (slot - CONTENT_START);
        List<ItemStack> all = getItems(player, filter);
        return index >= 0 && index < all.size() ? all.get(index).clone() : null;
    }

    public static void initiateSearch(Player player) {
        awaitingSearch.add(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.items_search_prompt")));
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.type_cancel_abort")));
    }

    public static boolean isAwaitingSearch(UUID uuid) {
        return awaitingSearch.contains(uuid);
    }

    /**
     * Applies the typed search and reopens the GUI, as long as the player is still in creative mode in a Creative world.
     */
    public static void handleSearchInput(Player player, String query) {
        awaitingSearch.remove(player.getUniqueId());
        boolean cancelled = query.equalsIgnoreCase("cancel");
        if (cancelled) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.search_cancelled")));
            activeFilters.remove(player.getUniqueId());
        } else {
            activeFilters.put(player.getUniqueId(), query);
        }
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!canUse(player)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "creative.items_creative_only")));
                return;
            }
            new GuiCreativeItems(player, 0, cancelled ? null : query).openGui();
        });
    }

    public static void clearFilter(UUID uuid) {
        activeFilters.remove(uuid);
    }

    public static String getActiveFilter(UUID uuid) {
        return activeFilters.get(uuid);
    }

    private Inventory build() {
        Inventory inv = GuiHolder.create(player, 54, Lang.getFor(player, TITLE_KEY));

        ItemStack glass = makeGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        List<ItemStack> all = getItems(player, filter);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        for (int i = start; i < end; i++) {
            inv.setItem(CONTENT_START + (i - start), createDisplay(all.get(i).clone()));
        }

        // Search and Back button
        if (filter != null) {
            inv.setItem(SLOT_SEARCH, buildItem(Material.ARROW, Lang.getFor(player, "gui.recipes.back"), List.of(
                    Lang.getFor(player, "gui.recipes.searching", "filter", filter),
                    Lang.getFor(player, "gui.marketvalue.return_all"))));
        } else {
            inv.setItem(SLOT_SEARCH, buildItem(Material.SPYGLASS, Lang.getFor(player, "gui.recipes.search"), List.of(
                    Lang.getFor(player, "gui.creativeitems.search_lore"))));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREVIOUS, buildItem(Material.RED_WOOL, Lang.getFor(player, "gui.page_prev"), List.of()));
        }
        if (end < all.size()) {
            inv.setItem(SLOT_NEXT, buildItem(Material.LIME_WOOL, Lang.getFor(player, "gui.page_next"), List.of()));
        }
        return inv;
    }

    /**
     * Adds the take hint to the displayed copy only. The item given out is always a fresh copy without it.
     */
    private ItemStack createDisplay(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.creativeitems.take_lore")));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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
     * Updates an already-open custom items inventory in-place without closing it.
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
