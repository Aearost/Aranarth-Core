package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.HeadEntry;
import com.aearost.aranarthcore.utils.HeadsDatabaseManager;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Head Exchange GUI.
 */
public class GuiHeadExchange {

    public static final String TITLE_KEY = "gui.headexchange.title";
    public static final int SLOT_INPUT = 11;
    public static final int SLOT_INFO = 13;
    public static final int SLOT_OUTPUT = 15;
    public static final int SLOT_PREV_VARIANT = 6;
    public static final int SLOT_EXIT = 18;
    public static final int SLOT_NEXT_VARIANT = 24;

    public static final Map<UUID, Integer> playerVariantIndex = new HashMap<>();
    public static final Map<UUID, Material> playerCurrentMaterial = new HashMap<>();

    private final Player player;

    public GuiHeadExchange(Player player) {
        this.player = player;
    }

    public void openGui() {
        Inventory gui = Bukkit.createInventory(player, 27, Lang.getFor(player, TITLE_KEY));

        ItemStack glass = makeGlass();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass);
        }

        gui.setItem(SLOT_INPUT, null);
        gui.setItem(SLOT_INFO, makeGlass());
        gui.setItem(SLOT_OUTPUT, makeOutputPlaceholder());
        gui.setItem(SLOT_PREV_VARIANT, makeGlass());
        gui.setItem(SLOT_EXIT, makeExit());
        gui.setItem(SLOT_NEXT_VARIANT, makeGlass());

        player.openInventory(gui);
    }

    /**
     * Refreshes COST, OUTPUT, and variant nav buttons in-place based on what is in the INPUT slot.
     */
    public static void updateExchangeSlots(Inventory inv, Player player) {
        ItemStack inputItem = inv.getItem(SLOT_INPUT);
        boolean isEmpty = inputItem == null || inputItem.getType() == Material.AIR;

        if (isEmpty) {
            playerVariantIndex.remove(player.getUniqueId());
            playerCurrentMaterial.remove(player.getUniqueId());
            inv.setItem(SLOT_INFO, makeGlass());
            inv.setItem(SLOT_OUTPUT, makeOutputPlaceholder());
            inv.setItem(SLOT_PREV_VARIANT, makeGlass());
            inv.setItem(SLOT_NEXT_VARIANT, makeGlass());
            return;
        }

        Material mat = inputItem.getType();

        // Reset variant index when the material changes
        Material prevMat = playerCurrentMaterial.get(player.getUniqueId());
        if (prevMat != mat) {
            playerVariantIndex.put(player.getUniqueId(), 0);
            playerCurrentMaterial.put(player.getUniqueId(), mat);
        }

        List<HeadEntry> variants = HeadsDatabaseManager.getExchangeableHeads().stream()
                .filter(h -> h.material() == mat)
                .toList();

        if (variants.isEmpty()) {
            inv.setItem(SLOT_INFO, makeWrongInfoDisplay());
            inv.setItem(SLOT_OUTPUT, makeWrongItemDisplay());
            inv.setItem(SLOT_PREV_VARIANT, makeGlass());
            inv.setItem(SLOT_NEXT_VARIANT, makeGlass());
            return;
        }

        int total = variants.size();
        int idx = playerVariantIndex.getOrDefault(player.getUniqueId(), 0) % total;
        playerVariantIndex.put(player.getUniqueId(), idx);

        HeadEntry entry = variants.get(idx);
        int qty = inputItem.getAmount();

        inv.setItem(SLOT_INFO, total > 1 ? makeVariantInfo(total, idx) : makeGlass());
        inv.setItem(SLOT_OUTPUT, makeOutputHead(entry, qty));
        inv.setItem(SLOT_PREV_VARIANT, total > 1 ? makePrevVariant() : makeGlass());
        inv.setItem(SLOT_NEXT_VARIANT, total > 1 ? makeNextVariant() : makeGlass());
    }

    static ItemStack makeGlass() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeVariantInfo(int totalVariants, int variantIdx) {
        ItemStack book = new ItemStack(Material.PAPER);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(Lang.get("gui.headexchange.variant", "n", String.valueOf(variantIdx + 1), "total", String.valueOf(totalVariants)));
        book.setItemMeta(meta);
        return book;
    }

    private static ItemStack makeWrongInfoDisplay() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Lang.get("gui.headexchange.no_head"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.headexchange.no_head_lore"));
        meta.setLore(lore);
        pane.setItemMeta(meta);
        return pane;
    }

    static ItemStack makeOutputPlaceholder() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Lang.get("gui.headexchange.output"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.headexchange.place_item"));
        meta.setLore(lore);
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeWrongItemDisplay() {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Lang.get("gui.headexchange.no_head"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.headexchange.no_matching"));
        meta.setLore(lore);
        pane.setItemMeta(meta);
        return pane;
    }

    static ItemStack makeOutputHead(HeadEntry entry, int qty) {
        ItemStack head = MobHeadUtils.createCustomHead(entry.texture(), "&f" + entry.name());
        head.setAmount(Math.min(qty, 64));
        ItemMeta meta = head.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.headexchange.click_exchange"));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(CustomKeys.HEAD_TEXTURE, PersistentDataType.STRING, entry.texture());
        meta.getPersistentDataContainer().set(CustomKeys.HEAD_REQUIRED_MATERIAL, PersistentDataType.STRING, entry.material().name());
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack makePrevVariant() {
        ItemStack pane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Lang.get("gui.page_prev"));
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeNextVariant() {
        ItemStack pane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(Lang.get("gui.page_next"));
        pane.setItemMeta(meta);
        return pane;
    }

    private static ItemStack makeExit() {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        meta.setDisplayName(Lang.get("gui.exit"));
        barrier.setItemMeta(meta);
        return barrier;
    }
}
