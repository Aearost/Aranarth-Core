package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.aearost.aranarthcore.objects.CustomKeys.FAE_BREWING_COPY;

/**
 * Gives Fae Aranarthium wearers a 25% chance to brew an additional potion.
 */
public class FaeBrewingBonus {

    public static final Set<Location> activeCopyLocations = new HashSet<>();
    private static final Map<Location, Integer> cleanupTasks = new HashMap<>();

    public void execute(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "fae")) {
            return;
        }
        if (e.getView().getType() != InventoryType.BREWING) {
            return;
        }
        // Only care about clicks on the brewing stand inventory and only output slots
        Inventory clicked = e.getClickedInventory();
        if (!(clicked instanceof BrewerInventory brewer)) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot > 2) {
            return; // Only potion output slots 0-2
        }

        ItemStack item = clicked.getItem(slot);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // If this slot already has a Fae copy waiting, allow normally
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(FAE_BREWING_COPY, PersistentDataType.BYTE)) {
            // Remove tracking
            Location loc = brewer.getLocation();
            cancelCleanup(loc);
            activeCopyLocations.remove(loc);
            return; // Let vanilla handle the pickup
        }

        // 25% chance to trigger double
        if (ThreadLocalRandom.current().nextInt(4) != 0) {
            return;
        }

        // Let the click proceed normally and then place a tagged copy back
        final ItemStack copy = item.clone();
        copy.setAmount(1);
        if (copy.getItemMeta() != null) {
            var copyMeta = copy.getItemMeta();
            copyMeta.getPersistentDataContainer().set(FAE_BREWING_COPY, PersistentDataType.BYTE, (byte) 1);
            copy.setItemMeta(copyMeta);
        }

        final int finalSlot = slot;
        final Location standLoc = brewer.getLocation();

        // Schedule 1 tick later to place the copy (after the pickup completes)
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            // Re-check that the slot is actually empty (pickup happened)
            ItemStack current = brewer.getItem(finalSlot);
            if (current == null || current.getType() == Material.AIR) {
                brewer.setItem(finalSlot, copy);
                activeCopyLocations.add(standLoc);

                // Schedule removal after 5 seconds
                int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(AranarthCore.getInstance(), () -> {
                    ItemStack inSlot = brewer.getItem(finalSlot);
                    if (inSlot != null && inSlot.hasItemMeta()
                            && inSlot.getItemMeta().getPersistentDataContainer().has(FAE_BREWING_COPY, PersistentDataType.BYTE)) {
                        brewer.setItem(finalSlot, null);
                    }
                    activeCopyLocations.remove(standLoc);
                    cleanupTasks.remove(standLoc);
                }, 100L);
                cleanupTasks.put(standLoc, taskId);
            }
        }, 1L);
    }

    private static void cancelCleanup(Location loc) {
        Integer taskId = cleanupTasks.remove(loc);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
