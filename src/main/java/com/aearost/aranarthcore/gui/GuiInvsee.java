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

    public static HashMap<Inventory, UUID> getOpenInvsees() {
        return openInvsees;
    }

    public static boolean isBeingWatched(UUID uuid) {
        Set<Inventory> guis = watchedBy.get(uuid);
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
        if (targetUUID != null) {
            Set<Inventory> guis = watchedBy.get(targetUUID);
            if (guis != null) {
                guis.remove(inventory);
                if (guis.isEmpty()) {
                    watchedBy.remove(targetUUID);
                }
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
