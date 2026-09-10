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

public class GuiBlacklistSelect {

    public static final String TITLE_CLEAR = "Blacklist - Clear Preset";
    public static final String TITLE_USE = "Blacklist - Select Preset";

    private final Player player;
    private final Inventory gui;

    public GuiBlacklistSelect(Player player, boolean isClear) {
        this.player = player;
        this.gui = initializeGui(isClear);
    }

    public void openGui() {
        player.closeInventory();
        player.openInventory(gui);
    }

    private Inventory initializeGui(boolean isClear) {
        Inventory inv = Bukkit.createInventory(player, 36, isClear ? TITLE_CLEAR : TITLE_USE);

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        List<BlacklistPreset> presets = ap.getBlacklistPresets();
        int activeIndex = ap.getActivePresetIndex();

        // Gray pane filler
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);

        // Top row and bottom row
        for (int i = 0; i <= 8; i++) {
            inv.setItem(i, pane.clone());
        }
        for (int i = 27; i <= 35; i++) {
            inv.setItem(i, pane.clone());
        }

        // Back arrow
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back to Blacklist"));
        back.setItemMeta(backMeta);
        inv.setItem(31, back);

        // Row 1 presets
        inv.setItem(11, buildPresetButton(0, presets, activeIndex, isClear));
        inv.setItem(13, buildPresetButton(1, presets, activeIndex, isClear));
        inv.setItem(15, buildPresetButton(2, presets, activeIndex, isClear));

        // Row 2 presets
        inv.setItem(20, buildPresetButton(3, presets, activeIndex, isClear));
        inv.setItem(22, buildPresetButton(4, presets, activeIndex, isClear));
        inv.setItem(24, buildPresetButton(5, presets, activeIndex, isClear));

        return inv;
    }

    private ItemStack buildPresetButton(int i, List<BlacklistPreset> presets, int activeIndex, boolean isClear) {
        boolean isActive = i == activeIndex;

        ItemStack item = new ItemStack(isActive ? Material.MAP : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        String presetName = (i < presets.size() && !presets.get(i).getName().isEmpty()) ? presets.get(i).getName() : "Preset " + (i + 1);
        meta.setDisplayName(ChatUtils.translateToColor("&7&l" + presetName));

        if (isActive) {
            meta.setLore(List.of(ChatUtils.translateToColor("&a&oCurrently active")));
        } else {
            meta.setLore(null);
        }
        item.setItemMeta(meta);
        return item;
    }
}
