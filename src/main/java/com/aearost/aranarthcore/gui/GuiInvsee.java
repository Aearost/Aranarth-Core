package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Custom 5-row inventory GUI for /ac invsee.
 */
public class GuiInvsee {

    /** Maps each open invsee GUI inventory to the UUID of the player being viewed. */
    private static final HashMap<Inventory, UUID> openInvsees = new HashMap<>();

    /** Reverse map: target UUID → all open GUIs currently watching that player. */
    private static final HashMap<UUID, Set<Inventory>> watchedBy = new HashMap<>();

    /** GUIs that show a remote-server snapshot - all interactions are blocked. */
    private static final HashSet<Inventory> remoteInvsees = new HashSet<>();

    /** Reverse map for remote GUIs: target UUID -> all open remote GUIs watching that player. */
    private static final HashMap<UUID, Set<Inventory>> remoteWatchedBy = new HashMap<>();

    public static HashMap<Inventory, UUID> getOpenInvsees() {
        return openInvsees;
    }

    public static boolean isBeingWatched(UUID uuid) {
        Set<Inventory> guis = watchedBy.get(uuid);
        return guis != null && !guis.isEmpty();
    }

    public static boolean isRemote(Inventory inventory) {
        return remoteInvsees.contains(inventory);
    }

    public static boolean hasRemoteInvseeOpen(UUID targetUuid) {
        Set<Inventory> guis = remoteWatchedBy.get(targetUuid);
        return guis != null && !guis.isEmpty();
    }

    public static void open(Player viewer, Player target) {
        Inventory gui = buildGui(target);
        openInvsees.put(gui, target.getUniqueId());
        watchedBy.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(gui);
        viewer.openInventory(gui);
    }

    public static void close(Inventory inventory) {
        UUID targetUUID = openInvsees.remove(inventory);
        remoteInvsees.remove(inventory);
        if (targetUUID != null) {
            Set<Inventory> localGuis = watchedBy.get(targetUUID);
            if (localGuis != null) {
                localGuis.remove(inventory);
                if (localGuis.isEmpty()) watchedBy.remove(targetUUID);
            }
            Set<Inventory> remoteGuis = remoteWatchedBy.get(targetUUID);
            if (remoteGuis != null) {
                remoteGuis.remove(inventory);
                if (remoteGuis.isEmpty()) remoteWatchedBy.remove(targetUUID);
            }
        }
    }

    /**
     * Opens a read-only snapshot of a remote player's inventory for the given viewer.
     * The guiItems array must be 45 elements matching the GUI slot layout.
     */
    public static void openRemote(Player viewer, UUID targetUuid, String targetName, ItemStack[] guiItems) {
        String title = "Viewing " + targetName + "'s Inventory";
        Inventory gui = Bukkit.createInventory(null, 45, title);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < guiItems.length && i < 45; i++) {
            gui.setItem(i, guiItems[i]);
        }
        gui.setItem(0, filler.clone());
        gui.setItem(5, filler.clone());
        gui.setItem(7, filler.clone());
        gui.setItem(8, filler.clone());

        openInvsees.put(gui, targetUuid);
        remoteInvsees.add(gui);
        remoteWatchedBy.computeIfAbsent(targetUuid, k -> new HashSet<>()).add(gui);
        viewer.openInventory(gui);
    }

    /** Updates all remote invsee GUIs currently watching the given target with a fresh snapshot. */
    public static void refreshRemoteForTarget(UUID targetUuid, ItemStack[] guiItems) {
        Set<Inventory> guis = remoteWatchedBy.get(targetUuid);
        if (guis == null) return;
        for (Inventory gui : guis) {
            for (int i = 0; i < guiItems.length && i < 45; i++) {
                if (i == 0 || i == 5 || i == 7 || i == 8) continue; // preserve filler slots
                gui.setItem(i, guiItems[i]);
            }
        }
    }

    /** Closes all remote invsee GUIs that are watching the given target (e.g. when they go offline). */
    public static void closeAllRemoteFor(UUID targetUuid) {
        Set<Inventory> guis = remoteWatchedBy.get(targetUuid);
        if (guis == null || guis.isEmpty()) return;
        Set<Inventory> copy = new HashSet<>(guis);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (copy.contains(p.getOpenInventory().getTopInventory())) {
                p.closeInventory(); // fires InventoryCloseEvent -> GuiInvsee.close()
            }
        }
    }

    /** Refreshes all open invsee GUIs that are watching the given target. */
    public static void refreshForTarget(Player target) {
        Set<Inventory> guis = watchedBy.get(target.getUniqueId());
        if (guis == null) return;
        for (Inventory gui : guis) {
            updateGuiContent(gui, target);
        }
    }

    private static void updateGuiContent(Inventory gui, Player target) {
        gui.setItem(1, target.getInventory().getHelmet());
        gui.setItem(2, target.getInventory().getChestplate());
        gui.setItem(3, target.getInventory().getLeggings());
        gui.setItem(4, target.getInventory().getBoots());
        gui.setItem(6, target.getInventory().getItemInOffHand());

        for (int i = 9; i <= 35; i++) {
            gui.setItem(i, target.getInventory().getItem(i));
        }

        for (int i = 0; i <= 8; i++) {
            gui.setItem(i + 36, target.getInventory().getItem(i));
        }
    }

    private static Inventory buildGui(Player target) {
        String title = "Viewing " + target.getName() + "'s Inventory";
        Inventory gui = Bukkit.createInventory(null, 45, title);

        // Row 0 (armor and off-hand)
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        gui.setItem(0, filler.clone());
        gui.setItem(1, target.getInventory().getHelmet());
        gui.setItem(2, target.getInventory().getChestplate());
        gui.setItem(3, target.getInventory().getLeggings());
        gui.setItem(4, target.getInventory().getBoots());
        gui.setItem(5, filler.clone());
        gui.setItem(6, target.getInventory().getItemInOffHand());
        gui.setItem(7, filler.clone());
        gui.setItem(8, filler.clone());

        // Rows 1–3 (main storage)
        for (int i = 9; i <= 35; i++) {
            gui.setItem(i, target.getInventory().getItem(i));
        }

        // Row 4 (hotbar)
        for (int i = 0; i <= 8; i++) {
            gui.setItem(i + 36, target.getInventory().getItem(i));
        }

        return gui;
    }
}
