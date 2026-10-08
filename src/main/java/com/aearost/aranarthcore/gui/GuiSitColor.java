package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.SitUtils;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Allows the player to select the color of the cushion they sit on.
 */
public class GuiSitColor {

    public static final int[] COLOR_SLOTS = {10, 11, 12, 13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24, 25, 26};
    public static final DyeColor[] COLORS = {
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME,
            DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE,
            DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.BROWN,
            DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK
    };
    public static final int EXIT_SLOT = 31;

    private final Player player;
    private final Inventory initializedGui;

    public GuiSitColor(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    private Inventory initializeGui(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        Inventory gui = Bukkit.getServer().createInventory(player, 36, Lang.getFor(player, "gui.sitcolor.title"));

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, blank);
        }

        DyeColor selected = SitUtils.getCushionColor(aranarthPlayer.getSitCushionColor());
        for (int i = 0; i < COLORS.length; i++) {
            gui.setItem(COLOR_SLOTS[i], buildColorItem(player, COLORS[i], COLORS[i] == selected));
        }

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(EXIT_SLOT, exit);

        return gui;
    }

    /**
     * @param color The color of the cushion.
     * @return The GUI slot that the color is displayed in.
     */
    public static int getSlotOfColor(DyeColor color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) {
                return COLOR_SLOTS[i];
            }
        }
        return -1;
    }

    /**
     * Builds the cushion item displayed for the input color.
     *
     * @param player The player viewing the GUI.
     * @param color The color of the cushion.
     * @param isSelected Whether this is the player's currently selected color.
     * @return The cushion item.
     */
    public static ItemStack buildColorItem(Player player, DyeColor color, boolean isSelected) {
        ItemStack item = new ItemStack(Material.valueOf(color.name() + "_CUSHION"));
        ItemMeta meta = item.getItemMeta();
        String name = ChatUtils.translateToColor("&f&l" + ChatUtils.getFormattedItemName(color.name()));
        if (isSelected) {
            meta.setDisplayName(name + " " + Lang.getFor(player, "gui.separator") + " " + Lang.getFor(player, "gui.sitcolor.selected"));
            meta.setEnchantmentGlintOverride(true);
        } else {
            meta.setDisplayName(name);
        }
        item.setItemMeta(meta);
        return item;
    }

}
