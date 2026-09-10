package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.InteractiveChatManager;
import com.aearost.aranarthcore.utils.InteractiveChatManager.Snapshot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.UUID;

/**
 * View-only GUI snapshots opened by clicking interactive chat components.
 */
public class GuiChatSnapshot {

    // Title prefixes used to detect these GUIs in inventory events
    static final String TITLE_SUFFIX_ITEM = "'s Item";
    static final String TITLE_SUFFIX_INV  = "'s Inventory";
    static final String TITLE_SUFFIX_EC   = "'s Ender Chest";

    /** Maps each open GUI inventory back to the snapshot UUID. */
    private static final HashMap<Inventory, UUID> openSnapshots = new HashMap<>();

    public static boolean isSnapshotGui(String title) {
        return title.endsWith(TITLE_SUFFIX_ITEM)
                || title.endsWith(TITLE_SUFFIX_INV)
                || title.endsWith(TITLE_SUFFIX_EC);
    }

    public static UUID getSnapshotId(Inventory inv) {
        return openSnapshots.get(inv);
    }

    public static void close(Inventory inv) {
        openSnapshots.remove(inv);
    }

    /**
     * Opens the appropriate view-only GUI for the snapshot.
     */
    public static void open(Player viewer, UUID snapshotId) {
        Snapshot snap = InteractiveChatManager.getSnapshot(snapshotId);
        if (snap == null) {
            viewer.sendMessage(ChatUtils.chatMessage("&cThis item is no longer available"));
            return;
        }
        Inventory gui = switch (snap.getType()) {
            case ITEM -> buildItemGui(snap);
            case INV  -> buildInvGui(snap);
            case EC   -> buildEcGui(snap);
        };
        openSnapshots.put(gui, snapshotId);
        viewer.openInventory(gui);
    }

    private static Inventory buildItemGui(Snapshot snap) {
        String title = snap.getOwnerDisplayName() + TITLE_SUFFIX_ITEM;
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ItemStack filler = makeFiller();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler.clone());
        }

        ItemStack item = snap.getItems()[0];
        if (item != null && item.getType() != Material.AIR) {
            gui.setItem(13, item.clone());
        }
        return gui;
    }

    /**
     * 5-row (45-slot) GUI matching the GuiInvsee layout but from snapshot data (view-only).
     */
    private static Inventory buildInvGui(Snapshot snap) {
        String title = snap.getOwnerDisplayName() + TITLE_SUFFIX_INV;
        Inventory gui = Bukkit.createInventory(null, 45, title);

        ItemStack filler = makeFiller();
        gui.setItem(0, filler.clone());
        gui.setItem(5, filler.clone());
        gui.setItem(7, filler.clone());
        gui.setItem(8, filler.clone());

        ItemStack[] items = snap.getItems();
        gui.setItem(1, cloneOrNull(items[0]));   // helmet
        gui.setItem(2, cloneOrNull(items[1]));   // chestplate
        gui.setItem(3, cloneOrNull(items[2]));   // leggings
        gui.setItem(4, cloneOrNull(items[3]));   // boots
        gui.setItem(6, cloneOrNull(items[4]));   // offhand

        // Main storage
        for (int i = 0; i < 27; i++) {
            gui.setItem(9 + i, cloneOrNull(items[5 + i]));
        }

        // Hotbar
        for (int i = 0; i < 9; i++) {
            gui.setItem(36 + i, cloneOrNull(items[32 + i]));
        }

        return gui;
    }

    /**
     * 3-row (27-slot) GUI with ender chest contents (view-only).
     */
    private static Inventory buildEcGui(Snapshot snap) {
        String title = snap.getOwnerDisplayName() + TITLE_SUFFIX_EC;
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ItemStack[] items = snap.getItems();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, cloneOrNull(items[i]));
        }
        return gui;
    }

    private static ItemStack makeFiller() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return (item != null && item.getType() != Material.AIR) ? item.clone() : null;
    }
}
