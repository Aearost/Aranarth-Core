package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.network.NetworkManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Reads and changes the slots of a player's inventory or ender chest by their slot in the invsee GUIs.
 */
public class InvseeUtils {

    /**
     * Provides the item in the slot, or null if it is empty or not a real slot.
     *
     * @param container Either NetworkManager.CONTAINER_INVENTORY or NetworkManager.CONTAINER_ENDER_CHEST.
     * @param guiSlot   The slot in the invsee GUI.
     */
    public static ItemStack getSlot(Player target, String container, int guiSlot) {
        ItemStack item;
        if (NetworkManager.CONTAINER_ENDER_CHEST.equals(container)) {
            Inventory enderChest = target.getEnderChest();
            item = guiSlot >= 0 && guiSlot < enderChest.getSize() ? enderChest.getItem(guiSlot) : null;
        } else {
            PlayerInventory inventory = target.getInventory();
            item = switch (guiSlot) {
                case 1 -> inventory.getHelmet();
                case 2 -> inventory.getChestplate();
                case 3 -> inventory.getLeggings();
                case 4 -> inventory.getBoots();
                case 6 -> inventory.getItemInOffHand();
                default -> {
                    int slot = toInventorySlot(guiSlot);
                    yield slot == -1 ? null : inventory.getItem(slot);
                }
            };
        }
        return item == null || item.getType().isAir() ? null : item;
    }

    /**
     * Sets the item in the slot, if it is a real slot.
     *
     * @param container Either NetworkManager.CONTAINER_INVENTORY or NetworkManager.CONTAINER_ENDER_CHEST.
     * @param guiSlot   The slot in the invsee GUI.
     */
    public static void setSlot(Player target, String container, int guiSlot, ItemStack item) {
        if (NetworkManager.CONTAINER_ENDER_CHEST.equals(container)) {
            Inventory enderChest = target.getEnderChest();
            if (guiSlot >= 0 && guiSlot < enderChest.getSize()) {
                enderChest.setItem(guiSlot, item);
            }
            return;
        }
        PlayerInventory inventory = target.getInventory();
        switch (guiSlot) {
            case 1 -> inventory.setHelmet(item);
            case 2 -> inventory.setChestplate(item);
            case 3 -> inventory.setLeggings(item);
            case 4 -> inventory.setBoots(item);
            case 6 -> inventory.setItemInOffHand(item);
            default -> {
                int slot = toInventorySlot(guiSlot);
                if (slot != -1) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    /**
     * Places the item into the slot, or stacks it onto a matching item there.
     *
     * @param guiSlot The slot in the invsee GUI, or -1 for any free space.
     */
    public static void place(Player target, String container, int guiSlot, ItemStack item) {
        ItemStack remaining = item.clone();
        if (guiSlot != -1 && isRealSlot(target, container, guiSlot)) {
            ItemStack current = getSlot(target, container, guiSlot);
            if (current == null) {
                setSlot(target, container, guiSlot, remaining);
                return;
            }
            if (current.isSimilar(remaining)) {
                int toAdd = Math.min(current.getMaxStackSize() - current.getAmount(), remaining.getAmount());
                if (toAdd > 0) {
                    ItemStack merged = current.clone();
                    merged.setAmount(current.getAmount() + toAdd);
                    setSlot(target, container, guiSlot, merged);
                    remaining.setAmount(remaining.getAmount() - toAdd);
                }
                if (remaining.getAmount() <= 0) {
                    return;
                }
            }
        }
        Inventory inventory = NetworkManager.CONTAINER_ENDER_CHEST.equals(container) ? target.getEnderChest() : target.getInventory();
        for (ItemStack leftover : inventory.addItem(remaining).values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover);
        }
    }

    private static boolean isRealSlot(Player target, String container, int guiSlot) {
        if (NetworkManager.CONTAINER_ENDER_CHEST.equals(container)) {
            return guiSlot >= 0 && guiSlot < target.getEnderChest().getSize();
        }
        return (guiSlot >= 1 && guiSlot <= 4) || guiSlot == 6 || toInventorySlot(guiSlot) != -1;
    }

    /**
     * Provides the player inventory slot of a storage or hotbar slot of the invsee GUI, or -1 if it is neither.
     */
    private static int toInventorySlot(int guiSlot) {
        if (guiSlot >= 9 && guiSlot <= 35) {
            return guiSlot;
        }
        if (guiSlot >= 36 && guiSlot <= 44) {
            return guiSlot - 36;
        }
        return -1;
    }
}
