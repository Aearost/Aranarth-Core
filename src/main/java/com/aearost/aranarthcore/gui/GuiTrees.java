package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CustomTreeUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a player choose which custom trees their saplings may grow into.
 */
public class GuiTrees {

    public static final int SLOT_ALL = 4;
    public static final int CONTENT_START = 9;
    public static final int PAGE_SIZE = 36;
    public static final int SLOT_ENABLE_ALL = 45;
    public static final int SLOT_SECTION = 46;
    public static final int SLOT_PREVIOUS = 48;
    public static final int SLOT_EXIT = 49;
    public static final int SLOT_NEXT = 50;
    public static final int SLOT_DISABLE_ALL = 53;

    private final Player player;
    private final int page;

    public GuiTrees(Player player, int page) {
        this.player = player;
        this.page = Math.max(0, Math.min(page, pageCount() - 1));
    }

    public void openGui() {
        player.closeInventory();
        Inventory gui = GuiHolder.create(player, 54, Lang.getFor(player, "gui.trees.title"));
        populateInto(gui);
        player.openInventory(gui);
    }

    /**
     * Fills an inventory with this page, so that an open GUI can change page or refresh without closing.
     */
    public void populateInto(Inventory gui) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        gui.clear();

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, blank);
        }
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, blank);
        }
        gui.setItem(SLOT_ALL, buildAllItem(player, aranarthPlayer));

        List<Variant> variants = section(page);
        int start = pageInSection(page) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, variants.size());
        for (int i = start; i < end; i++) {
            gui.setItem(CONTENT_START + i - start, buildTreeItem(player, aranarthPlayer, variants.get(i)));
        }

        gui.setItem(SLOT_ENABLE_ALL, buildItem(Material.LIME_WOOL, Lang.getFor(player, "gui.compressor.enable_all")));
        gui.setItem(SLOT_DISABLE_ALL, buildItem(Material.RED_WOOL, Lang.getFor(player, "gui.compressor.disable_all")));
        gui.setItem(SLOT_EXIT, buildItem(Material.BARRIER, Lang.getFor(player, "gui.exit")));
        // Switches between the common trees and the rare trees
        boolean onRare = isRarePage(page);
        ItemStack sectionItem = buildItem(onRare ? Material.OAK_SAPLING : Material.SPRUCE_SAPLING,
                Lang.getFor(player, onRare ? "gui.trees.common_page" : "gui.trees.rare_page"));
        ItemMeta sectionMeta = sectionItem.getItemMeta();
        sectionMeta.setLore(List.of(ChatUtils.translateToColor(Lang.getFor(player,
                onRare ? "gui.trees.common_page_lore" : "gui.trees.rare_page_lore"))));
        sectionItem.setItemMeta(sectionMeta);
        gui.setItem(SLOT_SECTION, sectionItem);
        if (page > 0) {
            gui.setItem(SLOT_PREVIOUS, buildItem(Material.ARROW, Lang.getFor(player, "gui.page_prev")));
        }
        if (page < pageCount() - 1) {
            gui.setItem(SLOT_NEXT, buildItem(Material.ARROW, Lang.getFor(player, "gui.page_next")));
        }
    }

    public int getPage() {
        return page;
    }

    /**
     * Provides how many pages the trees take up: the common trees first, then the rare trees on pages of their own.
     */
    public static int pageCount() {
        return firstRarePage() + Math.max(1, (rare().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /**
     * Provides the first page of rare trees, which comes after every page of common trees.
     */
    public static int firstRarePage() {
        return Math.max(1, (choosable().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /**
     * Determines if the page shows rare trees.
     */
    public static boolean isRarePage(int page) {
        return page >= firstRarePage();
    }

    /**
     * Provides the trees of the section the page is in, either the common trees or the rare trees.
     */
    public static List<Variant> section(int page) {
        return isRarePage(page) ? rare() : choosable();
    }

    private static int pageInSection(int page) {
        return isRarePage(page) ? page - firstRarePage() : page;
    }

    /**
     * Provides the tree shown in a slot of a page, or null if the slot does not hold a tree.
     */
    public static Variant treeAt(int page, int slot) {
        if (slot < CONTENT_START || slot >= CONTENT_START + PAGE_SIZE) {
            return null;
        }
        List<Variant> variants = section(page);
        int index = pageInSection(page) * PAGE_SIZE + slot - CONTENT_START;
        return index < variants.size() ? variants.get(index) : null;
    }

    /**
     * Provides every common tree a player can turn on or off, grouped by sapling in the order the saplings first appear.
     * Rare trees are on pages of their own, and fruit trees are left out, as they only grow from fruit saplings.
     */
    public static List<Variant> choosable() {
        return grouped(Variant.forAnySapling());
    }

    /**
     * Provides every rare tree, which a player can also turn on or off, grouped by sapling.
     */
    public static List<Variant> rare() {
        return grouped(Variant.rare());
    }

    /**
     * Provides every tree a player can turn on or off, common and rare.
     */
    public static List<Variant> toggleable() {
        List<Variant> variants = new ArrayList<>(choosable());
        variants.addAll(rare());
        return variants;
    }

    /**
     * Groups the trees by sapling, in the order the saplings first appear.
     */
    private static List<Variant> grouped(List<Variant> trees) {
        List<Material> saplings = new ArrayList<>();
        for (Variant variant : trees) {
            if (!saplings.contains(variant.getSapling())) {
                saplings.add(variant.getSapling());
            }
        }
        List<Variant> variants = new ArrayList<>();
        for (Material sapling : saplings) {
            for (Variant variant : trees) {
                if (variant.getSapling() == sapling) {
                    variants.add(variant);
                }
            }
        }
        return variants;
    }

    /**
     * Builds the item for one tree: its sapling, its name, whether it is on, and the square of saplings it grows from.
     */
    public static ItemStack buildTreeItem(Player player, AranarthPlayer aranarthPlayer, Variant variant) {
        boolean enabled = !aranarthPlayer.isTreeDisabled(variant.name());
        ItemStack item = new ItemStack(variant.getSapling());
        ItemMeta meta = item.getItemMeta();
        String status = enabled ? Lang.getFor(player, "gui.active") : Lang.getFor(player, "gui.inactive");
        meta.setDisplayName(ChatUtils.translateToColor("&f&l" + Lang.getFor(player, "trees.variant_" + variant.name().toLowerCase())
                + " " + Lang.getFor(player, "gui.separator") + " " + status));
        int size = Math.max(1, variant.getSize());
        List<String> lore = new ArrayList<>();
        if (variant.isRare()) {
            lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.rare")));
        }
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.grows_from",
                "sapling", Lang.getFor(player, "trees.sapling_" + saplingKey(variant.getSapling())),
                "size", size + "x" + size)));
        if (variant.isRare()) {
            lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.where",
                    "condition", Lang.getFor(player, "trees.condition_" + variant.name().toLowerCase()))));
        }
        if (size >= CustomTreeUtils.LARGE_TREE_SIZE) {
            lore.add(ChatUtils.translateToColor(Lang.getFor(player, "mechanics.treetypes_duke_hover")));
        }
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.click")));
        meta.setLore(lore);
        meta.setEnchantmentGlintOverride(enabled);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Builds the switch for custom trees as a whole.
     */
    public static ItemStack buildAllItem(Player player, AranarthPlayer aranarthPlayer) {
        boolean active = !aranarthPlayer.isBigTreeDisabled();
        ItemStack item = new ItemStack(active ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        String status = active ? Lang.getFor(player, "gui.active") : Lang.getFor(player, "gui.inactive");
        meta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, "gui.toggle.big_trees") + " "
                + Lang.getFor(player, "gui.separator") + " " + status));
        meta.setLore(List.of(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.all_lore"))));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Provides the lang key suffix of the name of a sapling, after "trees.sapling_".
     */
    public static String saplingKey(Material sapling) {
        if (sapling == Material.FLOWERING_AZALEA) {
            return "azalea";
        }
        return sapling.name().toLowerCase().replace("_sapling", "").replace("_fungus", "").replace("_propagule", "");
    }
}
