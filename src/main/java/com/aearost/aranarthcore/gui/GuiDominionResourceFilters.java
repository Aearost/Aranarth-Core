package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionResourceCategory;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

/**
 * GUI that lets the Dominion leader toggle resource drop categories on or off.
 */
public class GuiDominionResourceFilters {

    public static final String TITLE_SUFFIX_KEY = "gui.dominionresourcefilters.title_suffix";
    private static final int[] CATEGORY_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final DominionResourceCategory[] SLOT_CATEGORIES = {
            DominionResourceCategory.DIRTS,
            DominionResourceCategory.STONES,
            DominionResourceCategory.ORES,
            DominionResourceCategory.NATURAL_BLOCKS,
            DominionResourceCategory.WOODS,
            DominionResourceCategory.BRICKS,
            DominionResourceCategory.MISC_BLOCKS,
            DominionResourceCategory.FOOD,
            DominionResourceCategory.VALUABLES
    };

    private final Player player;
    private final Inventory initializedGui;

    public GuiDominionResourceFilters(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(initializedGui);
    }

    private Inventory initializeGui(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(player, 27,
                Lang.getFor(player, "gui.dominionresourcefilters.title", "dominion", dominion != null ? dominion.getName() : ""));

        Set<DominionResourceCategory> disabled = dominion != null ? dominion.getDisabledResourceCategories() : Set.of();

        ItemStack pane = makeFiller();

        // Top border row
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, pane);
        }
        // Bottom border row
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, i == 22 ? buildBackButton(player) : pane);
        }

        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            gui.setItem(CATEGORY_SLOTS[i], buildCategoryItem(SLOT_CATEGORIES[i], !disabled.contains(SLOT_CATEGORIES[i]), player));
        }

        return gui;
    }

    /**
     * Builds a toggle item for the given category.
     */
    public static ItemStack buildCategoryItem(DominionResourceCategory category, boolean enabled, Player player) {
        Material mat = getMaterialForCategory(category);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        String status = enabled ? Lang.get("gui.active") : Lang.get("gui.inactive");
        String nameKey = "gui.dominionresourcefilters.category_" + category.name().toLowerCase();
        meta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, nameKey) + " " + Lang.get("gui.separator") + " " + status));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns the slot index for the given slot number, or -1 if not a category slot.
     */
    public static int getCategorySlotIndex(int slot) {
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the category for the given slot index.
     */
    public static DominionResourceCategory getCategoryForSlotIndex(int index) {
        if (index < 0 || index >= SLOT_CATEGORIES.length) return null;
        return SLOT_CATEGORIES[index];
    }

    private static Material getMaterialForCategory(DominionResourceCategory category) {
        return switch (category) {
            case STONES -> Material.STONE;
            case ORES -> Material.IRON_ORE;
            case DIRTS -> Material.DIRT;
            case WOODS -> Material.OAK_LOG;
            case NATURAL_BLOCKS -> Material.MAGMA_BLOCK;
            case BRICKS -> Material.STONE_BRICKS;
            case MISC_BLOCKS -> Material.NETHER_BRICKS;
            case VALUABLES -> Material.ELYTRA;
            case FOOD -> Material.APPLE;
        };
    }

    private static ItemStack makeFiller() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack buildBackButton(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, "gui.dominionresourcefilters.back")));
        item.setItemMeta(meta);
        return item;
    }
}
