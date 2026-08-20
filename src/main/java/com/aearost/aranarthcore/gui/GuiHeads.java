package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.HeadEntry;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiHeads {

    public static final String TITLE = "Custom Heads";
    public static final int HEADS_START = 9;
    public static final int HEADS_END = 44; // inclusive
    public static final int HEADS_PER_PAGE = HEADS_END - HEADS_START + 1; // 36
    public static final int SLOT_PREV = 45;
    public static final int SLOT_CLOSE = 49;
    public static final int SLOT_NEXT = 53;

    // Tracks which page each player is on so click handlers can navigate
    public static final Map<UUID, Integer> playerPage = new HashMap<>();

    private final Player player;
    private final int page;

    public GuiHeads(Player player, int page) {
        this.player = player;
        this.page = page;
    }

    public void openGui() {
        playerPage.put(player.getUniqueId(), page);
        List<HeadEntry> heads = HeadsDatabaseManager.getExchangeableHeads();
        int totalPages = Math.max(1, (int) Math.ceil((double) heads.size() / HEADS_PER_PAGE));
        int safePage = Math.min(page, totalPages - 1);

        Inventory gui = Bukkit.createInventory(player, 54,
                ChatUtils.translateToColor("&5&l" + TITLE));

        ItemStack glass = makeGlass(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack grayGlass = makeGlass(Material.GRAY_STAINED_GLASS_PANE);

        // Top row decoration
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glass);
        }

        // Info item at slot 4
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatUtils.translateToColor("&5&lCustom Heads"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatUtils.translateToColor("&7Click any head to exchange"));
        infoLore.add(ChatUtils.translateToColor("&7the corresponding item for it."));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        // Bottom row navigation
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, grayGlass);
        }

        // Prev button
        if (safePage > 0) {
            gui.setItem(SLOT_PREV, makeNavButton(Material.ARROW, "&aPrevious Page",
                    "&7Page " + safePage + " / " + totalPages));
        } else {
            gui.setItem(SLOT_PREV, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Page indicator / close
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatUtils.translateToColor("&cClose"));
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatUtils.translateToColor("&7Page &e" + (safePage + 1) + " &7/ &e" + totalPages));
        closeMeta.setLore(closeLore);
        closeBtn.setItemMeta(closeMeta);
        gui.setItem(SLOT_CLOSE, closeBtn);

        // Next button
        if (safePage < totalPages - 1) {
            gui.setItem(SLOT_NEXT, makeNavButton(Material.ARROW, "&aNext Page",
                    "&7Page " + (safePage + 2) + " / " + totalPages));
        } else {
            gui.setItem(SLOT_NEXT, makeGlass(Material.RED_STAINED_GLASS_PANE));
        }

        // Fill head slots
        int start = safePage * HEADS_PER_PAGE;
        int end = Math.min(start + HEADS_PER_PAGE, heads.size());
        int guiSlot = HEADS_START;
        for (int i = start; i < end; i++) {
            HeadEntry entry = heads.get(i);
            ItemStack headItem = MobHeadUtils.createCustomHead(entry.texture(), "&f" + entry.name());
            ItemMeta meta = headItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.translateToColor("&7Requires: &e" + formatMaterialName(entry.material())));
            lore.add("");
            lore.add(ChatUtils.translateToColor("&aClick to exchange"));
            meta.setLore(lore);
            headItem.setItemMeta(meta);
            gui.setItem(guiSlot++, headItem);
        }

        // Fill remaining head area slots with gray glass
        while (guiSlot <= HEADS_END) {
            gui.setItem(guiSlot++, grayGlass);
        }

        player.openInventory(gui);
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
        meta.setDisplayName(ChatUtils.translateToColor(name));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor(loreText));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static String formatMaterialName(org.bukkit.Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }
}
