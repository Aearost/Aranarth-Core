package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.InteractiveChatManager;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.InteractiveChatManager.Snapshot;
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

    /** Maps each open GUI inventory back to the snapshot UUID. */
    private static final HashMap<Inventory, UUID> openSnapshots = new HashMap<>();

    public static boolean isSnapshotGui(Inventory inv) {
        return openSnapshots.containsKey(inv);
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
            viewer.sendMessage(ChatUtils.chatMessage(Lang.getFor(viewer, "chat_snapshot.expired")));
            return;
        }
        Inventory gui = switch (snap.getType()) {
            case ITEM -> buildItemGui(viewer, snap);
            case INV  -> buildInvGui(viewer, snap);
            case EC   -> buildEcGui(viewer, snap);
        };
        openSnapshots.put(gui, snapshotId);
        viewer.openInventory(gui);
    }

    private static Inventory buildItemGui(Player viewer, Snapshot snap) {
        String title = Lang.getFor(viewer, "chat.snapshot_item", "player", snap.getOwnerDisplayName());
        Inventory gui = GuiHolder.create(null, 27, title);

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
    private static Inventory buildInvGui(Player viewer, Snapshot snap) {
        String title = Lang.getFor(viewer, "chat.snapshot_inv", "player", snap.getOwnerDisplayName());
        Inventory gui = GuiHolder.create(null, 45, title);

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
    private static Inventory buildEcGui(Player viewer, Snapshot snap) {
        String title = Lang.getFor(viewer, "chat.snapshot_ec", "player", snap.getOwnerDisplayName());
        Inventory gui = GuiHolder.create(null, 27, title);

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
