package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.Arrays;

/**
 * Displays a player's Reaper Inventory - the items lost on their last death.
 */
public class GuiReaper {

    public static final String TITLE = "Reaper Inventory";

    private final Inventory inventory;

    public GuiReaper(Player player, ItemStack[] drops, double cost, long deathTime, Location deathLocation) {
        this.inventory = initializeGui(player, drops, cost, deathTime, deathLocation);
    }

    private Inventory initializeGui(Player player, ItemStack[] drops, double cost, long deathTime, Location deathLocation) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatUtils.translateToColor("&4" + TITLE));

        // Display the drop items
        int displaySlots = Math.min(drops.length, 45);
        for (int i = 0; i < displaySlots; i++) {
            if (drops[i] != null) {
                inv.setItem(i, drops[i].clone());
            }
        }

        // Bottom row filler
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(ChatUtils.translateToColor("&r"));
        pane.setItemMeta(paneMeta);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Purchase button (slot 49)
        NumberFormat nf = NumberFormat.getNumberInstance();
        long hoursLeft = getHoursRemaining(deathTime);
        ItemStack purchase = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta purchaseMeta = purchase.getItemMeta();
        purchaseMeta.setDisplayName(ChatUtils.translateToColor("&a&lRecover Inventory"));
        purchaseMeta.setLore(Arrays.asList(
                ChatUtils.translateToColor("&7Cost: &6$" + nf.format((long) cost)),
                ChatUtils.translateToColor("&7Expires in: &e" + hoursLeft + " hour" + (hoursLeft == 1 ? "" : "s"))
        ));
        purchase.setItemMeta(purchaseMeta);
        inv.setItem(49, purchase);

        // Drop to death location button (slot 47)
        ItemStack dropButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta dropMeta = dropButton.getItemMeta();
        dropMeta.setDisplayName(ChatUtils.translateToColor("&c&lDrop to Death Location"));
        String worldName = deathLocation.getWorld() != null ? deathLocation.getWorld().getName() : "world";
        dropMeta.setLore(Arrays.asList(
                ChatUtils.translateToColor("&7Drops your items at your death location"),
                ChatUtils.translateToColor("&7and discards this Reaper Inventory")
        ));
        dropButton.setItemMeta(dropMeta);
        inv.setItem(47, dropButton);

        // Close button (slot 45)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatUtils.translateToColor("&c&lExit"));
        close.setItemMeta(closeMeta);
        inv.setItem(45, close);

        return inv;
    }

    private long getHoursRemaining(long deathTime) {
        long expiryTime = deathTime + (24L * 60 * 60 * 1000);
        long millisLeft = expiryTime - System.currentTimeMillis();
        return Math.max(1, millisLeft / (1000 * 60 * 60));
    }

    public void openGui(Player player) {
        player.openInventory(inventory);
    }
}
