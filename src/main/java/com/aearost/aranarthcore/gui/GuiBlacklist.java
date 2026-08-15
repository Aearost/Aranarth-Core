package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.BlacklistPreset;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiBlacklist {

    public static final String TITLE = "Blacklist";

    private final Player player;
    private final Inventory gui;

    public GuiBlacklist(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 45, TITLE);
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        List<BlacklistPreset> presets = ap.getBlacklistPresets();
        int activeIndex = ap.getActivePresetIndex();
        int method = ap.getBlacklistingMethod();

        // Gray pane filler for top (0-8) and bottom (36-44) rows
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);
        for (int i = 0; i <= 8; i++) inv.setItem(i, pane.clone());
        for (int i = 36; i <= 44; i++) inv.setItem(i, pane.clone());

        // Row 1 (slots 9-17): presets 1-3 at indices 2, 4, 6 (slots 11, 13, 15)
        inv.setItem(11, buildPresetButton(0, presets, activeIndex));
        inv.setItem(13, buildPresetButton(1, presets, activeIndex));
        inv.setItem(15, buildPresetButton(2, presets, activeIndex));

        // Row 2 (slots 18-26): presets 4-6 at indices 2, 4, 6 (slots 20, 22, 24)
        inv.setItem(20, buildPresetButton(3, presets, activeIndex));
        inv.setItem(22, buildPresetButton(4, presets, activeIndex));
        inv.setItem(24, buildPresetButton(5, presets, activeIndex));

        // Row 3 (slots 27-35): toggle at index 1 (28), select preset centered (31), clear at index 7 (34)
        inv.setItem(28, buildToggleButton(method));
        inv.setItem(31, buildActionButton(Material.NETHER_STAR, "&a&lSelect Preset"));
        inv.setItem(34, buildActionButton(Material.TNT, "&c&lClear Preset"));

        return inv;
    }

    private ItemStack buildPresetButton(int i, List<BlacklistPreset> presets, int activeIndex) {
        boolean isActive = i == activeIndex;

        ItemStack item = new ItemStack(isActive ? Material.MAP : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        String name = (i < presets.size() && !presets.get(i).getName().isEmpty())
                ? presets.get(i).getName() : "Preset " + (i + 1);
        meta.setDisplayName(ChatUtils.translateToColor("&7&l" + name));

        if (isActive) {
            meta.setLore(List.of(ChatUtils.translateToColor("&a&oCurrently active")));
        } else {
            meta.setLore(null);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildToggleButton(int method) {
        Material mat;
        String name;
        if (method == 0) {
            mat = Material.HOPPER;
            name = "&f&lIgnoring Blacklisted Items";
        } else if (method == 1) {
            mat = Material.FIRE_CHARGE;
            name = "&f&lTrashing Blacklisted Items";
        } else {
            mat = Material.BARRIER;
            name = "&f&lBlacklist Disabled";
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildActionButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        item.setItemMeta(meta);
        return item;
    }
}
