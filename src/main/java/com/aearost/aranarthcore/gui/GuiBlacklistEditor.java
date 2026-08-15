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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiBlacklistEditor {

    public static final String TITLE_PREFIX = "Blacklist - ";
    private static final Map<UUID, Integer> openPresets = new HashMap<>();
    private static final Map<UUID, Integer> awaitingRename = new HashMap<>();

    private final Player player;
    private final int presetIndex;
    private final Inventory gui;

    public GuiBlacklistEditor(Player player, int presetIndex) {
        this.player = player;
        this.presetIndex = presetIndex;
        this.gui = initializeGui();
    }

    public void openGui() {
        openPresets.put(player.getUniqueId(), presetIndex);
        player.closeInventory();
        player.openInventory(gui);
    }

    public static int getPresetIndex(UUID uuid) {
        return openPresets.getOrDefault(uuid, 0);
    }

    public static void clearPresetIndex(UUID uuid) {
        openPresets.remove(uuid);
    }

    public static void setAwaitingRename(UUID uuid, int presetIndex) {
        awaitingRename.put(uuid, presetIndex);
    }

    public static boolean isAwaitingRename(UUID uuid) {
        return awaitingRename.containsKey(uuid);
    }

    public static int getAwaitingRenameIndex(UUID uuid) {
        return awaitingRename.getOrDefault(uuid, 0);
    }

    public static void clearAwaitingRename(UUID uuid) {
        awaitingRename.remove(uuid);
    }

    private Inventory initializeGui() {
        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        List<BlacklistPreset> presets = ap.getBlacklistPresets();

        // Title
        String presetName = presetIndex < presets.size() ? presets.get(presetIndex).getName() : "";
        String title = presetName.isEmpty()
                ? TITLE_PREFIX + "Preset " + (presetIndex + 1)
                : TITLE_PREFIX + presetName;
        Inventory inv = Bukkit.createInventory(player, 54, ChatUtils.translateToColor(title));

        // Gray pane filler
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        pane.setItemMeta(paneMeta);

        // Top row
        for (int i = 0; i <= 8; i++) {
            inv.setItem(i, pane.clone());
        }
        // Bottom row
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, pane.clone());
        }

        // Rename button
        ItemStack rename = new ItemStack(Material.NAME_TAG);
        ItemMeta renameMeta = rename.getItemMeta();
        renameMeta.setDisplayName(ChatUtils.translateToColor("&f&lRename Preset"));
        rename.setItemMeta(renameMeta);
        inv.setItem(4, rename);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatUtils.translateToColor("&7Back to Blacklist"));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        // Load existing preset items
        if (presetIndex < presets.size()) {
            List<ItemStack> items = presets.get(presetIndex).getItems();
            for (int i = 0; i < items.size() && i < 36; i++) {
                inv.setItem(9 + i, items.get(i));
            }
        }

        return inv;
    }
}
