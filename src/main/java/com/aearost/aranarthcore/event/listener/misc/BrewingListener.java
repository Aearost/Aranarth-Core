package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;

import static com.aearost.aranarthcore.objects.CustomKeys.BREW_BREWER;
import static com.aearost.aranarthcore.objects.CustomKeys.BREWED_POTION;
import static com.aearost.aranarthcore.objects.CustomKeys.BREWING_COPY;

/**
 * Tags potions in brewing stand output slots when a brew completes to avoid potion duplication.
 * Potions that were themselves produced by the double-brew bonus (BREWING_COPY) are ineligible
 * to trigger further duplication rolls - their outputs are not tagged with BREWED_POTION.
 */
public class BrewingListener implements Listener {

    /** Locations of brewing stands that currently hold at least one BREWING_COPY potion as a potion input. */
    private static final Set<Location> copyInputStands = new HashSet<>();

    public BrewingListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        BrewerInventory brewer = e.getContents();
        Location loc = brewer.getLocation();
        boolean hasCopyInput = copyInputStands.remove(loc);

        // Play the brewing bubble sound at the stand's world location for nearby players
        if (loc != null && loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 1.0F, 1.0F);
        }

        if (hasCopyInput) {
            // Outputs brewed from a BREWING_COPY input do not receive the duplication-eligible tag
            return;
        }

        // Tag each output slot one tick later so the brewed items are in place
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack item = brewer.getItem(slot);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    continue;
                }
                meta.getPersistentDataContainer().set(BREWED_POTION, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
            }
        }, 1L);
    }

    /**
     * Tracks when a player places or removes items from brewing stand potion slots,
     * keeping copyInputStands accurate.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        BrewerInventory brewer = null;

        // Direct click on a brewing stand potion slot (0–2)
        if (e.getClickedInventory() instanceof BrewerInventory b && e.getSlot() >= 0 && e.getSlot() <= 2) {
            brewer = b;
        }
        // Shift-click from the player's inventory moves the item into the top inventory (brewing stand)
        else if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && e.getView().getTopInventory() instanceof BrewerInventory b) {
            brewer = b;
        }

        if (brewer == null) {
            return;
        }

        final BrewerInventory finalBrewer = brewer;
        final Location loc = brewer.getLocation();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> updateCopyInputState(finalBrewer, loc), 1L);
    }

    /**
     * Tracks hopper transfers into and out of brewing stand potion slots,
     * keeping copyInputStands accurate. Only schedules a recheck when the
     * transferred item is a BREWING_COPY to avoid unnecessary overhead.
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();
        if (!item.hasItemMeta()) {
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(BREWING_COPY, PersistentDataType.BYTE)) {
            return;
        }

        BrewerInventory brewer = null;
        if (e.getDestination() instanceof BrewerInventory b) {
            brewer = b;
        } else if (e.getSource() instanceof BrewerInventory b) {
            brewer = b;
        }

        if (brewer == null) {
            return;
        }

        final BrewerInventory finalBrewer = brewer;
        final Location loc = brewer.getLocation();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> updateCopyInputState(finalBrewer, loc), 1L);
    }

    /**
     * Scans potion slots 0–2 and adds or removes the stand's location from
     * copyInputStands depending on whether any BREWING_COPY is present.
     */
    private static void updateCopyInputState(BrewerInventory brewer, Location loc) {
        for (int slot = 0; slot <= 2; slot++) {
            ItemStack item = brewer.getItem(slot);
            if (item != null && item.hasItemMeta()
                    && item.getItemMeta().getPersistentDataContainer().has(BREWING_COPY, PersistentDataType.BYTE)) {
                copyInputStands.add(loc);
                return;
            }
        }
        copyInputStands.remove(loc);
    }

    /**
     * When a player fills a brew from a cauldron into bottles, tag the resulting item with their
     * UUID so we can verify authorship when they later pick up the finished brew.
     */
    @EventHandler
    public void onBrewFill(BrewModifyEvent e) {
        if (e.getType() != BrewModifyEvent.Type.FILL) {
            return;
        }
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }
        e.getItemMeta().getPersistentDataContainer().set(BREW_BREWER, PersistentDataType.STRING, player.getUniqueId().toString());
    }
}
