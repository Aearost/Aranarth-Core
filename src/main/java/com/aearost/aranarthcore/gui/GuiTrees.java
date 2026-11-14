package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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

        List<Variant> variants = choosable();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, variants.size());
        for (int i = start; i < end; i++) {
            gui.setItem(CONTENT_START + i - start, buildTreeItem(player, aranarthPlayer, variants.get(i)));
        }

        gui.setItem(SLOT_ENABLE_ALL, buildItem(Material.LIME_WOOL, Lang.getFor(player, "gui.compressor.enable_all")));
        gui.setItem(SLOT_DISABLE_ALL, buildItem(Material.RED_WOOL, Lang.getFor(player, "gui.compressor.disable_all")));
        gui.setItem(SLOT_EXIT, buildItem(Material.BARRIER, Lang.getFor(player, "gui.exit")));
        if (page > 0) {
            gui.setItem(SLOT_PREVIOUS, buildItem(Material.ARROW, Lang.getFor(player, "gui.page_prev")));
        }
        if (end < variants.size()) {
            gui.setItem(SLOT_NEXT, buildItem(Material.ARROW, Lang.getFor(player, "gui.page_next")));
        }
    }

    public int getPage() {
        return page;
    }

    /**
     * Provides how many pages the trees take up.
     */
    public static int pageCount() {
        return Math.max(1, (choosable().size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    /**
     * Provides the tree shown in a slot of a page, or null if the slot does not hold a tree.
     */
    public static Variant treeAt(int page, int slot) {
        if (slot < CONTENT_START || slot >= CONTENT_START + PAGE_SIZE) {
            return null;
        }
        List<Variant> variants = choosable();
        int index = page * PAGE_SIZE + slot - CONTENT_START;
        return index < variants.size() ? variants.get(index) : null;
    }

    /**
     * Provides every tree a player can turn on or off, grouped by sapling in the order the saplings first appear. Rare
     * trees are left out, as they always grow wherever their conditions are met.
     */
    public static List<Variant> choosable() {
        List<Variant> variants = new ArrayList<>();
        List<Material> saplings = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            if (!variant.isRare() && !saplings.contains(variant.getSapling())) {
                saplings.add(variant.getSapling());
            }
        }
        for (Material sapling : saplings) {
            for (Variant variant : Variant.values()) {
                if (!variant.isRare() && variant.getSapling() == sapling) {
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
        lore.add(ChatUtils.translateToColor(Lang.getFor(player, "gui.trees.grows_from",
                "sapling", Lang.getFor(player, "trees.sapling_" + saplingKey(variant.getSapling())),
                "size", size + "x" + size)));
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
    private static String saplingKey(Material sapling) {
        if (sapling == Material.FLOWERING_AZALEA) {
            return "azalea";
        }
        return sapling.name().toLowerCase().replace("_sapling", "").replace("_fungus", "").replace("_propagule", "");
    }
}
