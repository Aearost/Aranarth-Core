package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiInvsee;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/**
 * Centralizes all event handling for the /ac invsee GUI.
 */
public class InvseeListener implements Listener {

    private final AranarthCore plugin;

    public InvseeListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isInvsee(String title) {
        return title.startsWith("Viewing ") && title.endsWith("'s Inventory");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!isInvsee(e.getView().getTitle())) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player viewer)) {
            return;
        }

        UUID targetUUID = GuiInvsee.getOpenInvsees().get(e.getInventory());
        if (targetUUID == null) {
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            e.setCancelled(true);
            viewer.closeInventory();
            viewer.sendMessage(ChatUtils.chatMessage("&cThis player is no longer online"));
            return;
        }

        // Viewer's own inventory (bottom section)
        if (e.getClickedInventory() != null && e.getClickedInventory() != e.getInventory()) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack item = normalize(e.getCurrentItem());
                if (item == null) {
                    return;
                }

                int targetSlot = findAvailableSlot(target, item);
                if (targetSlot == -1) {
                    return;
                }

                ItemStack existing = normalize(target.getInventory().getItem(targetSlot));
                if (existing != null && existing.isSimilar(item)) {
                    int space = existing.getMaxStackSize() - existing.getAmount();
                    int toMove = Math.min(space, item.getAmount());
                    existing.setAmount(existing.getAmount() + toMove);
                    target.getInventory().setItem(targetSlot, existing);
                    e.getInventory().setItem(toGuiSlot(targetSlot), existing.clone());
                    item.setAmount(item.getAmount() - toMove);
                    e.setCurrentItem(item.getAmount() <= 0 ? null : item);
                } else {
                    target.getInventory().setItem(targetSlot, item.clone());
                    e.getInventory().setItem(toGuiSlot(targetSlot), item.clone());
                    e.setCurrentItem(null);
                }
            }
            return;
        }

        // Top (invsee) inventory
        e.setCancelled(true);
        if (e.getClickedInventory() == null) {
            return;
        }

        int slot = e.getSlot();

        // Filler pane slots
        if (slot == 0 || slot == 5 || slot == 7 || slot == 8) {
            return;
        }

        ClickType clickType = e.getClick();
        ItemStack slotItem = normalize(e.getCurrentItem());
        ItemStack cursor = normalize(e.getView().getCursor());

        switch (clickType) {
            case LEFT -> handleLeft(e, target, slot, slotItem, cursor);
            case RIGHT -> handleRight(e, target, slot, slotItem, cursor);
            case SHIFT_LEFT, SHIFT_RIGHT -> handleShift(e, viewer, target, slot, slotItem);
            case NUMBER_KEY -> handleNumberKey(e, viewer, target, slot, slotItem);
            case DROP -> handleDrop(e, target, slot, slotItem, false);
            case CONTROL_DROP -> handleDrop(e, target, slot, slotItem, true);
            default -> {
            } // Double-click, middle-click, etc.
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (isInvsee(e.getView().getTitle())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (isInvsee(e.getView().getTitle())) {
            GuiInvsee.close(e.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GuiInvsee.isBeingWatched(player.getUniqueId())) {
            return;
        }
        // Skip clicks the viewer makes inside an invsee GUI — already handled above
        if (isInvsee(e.getView().getTitle())) {
            return;
        }
        scheduleRefresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetDrop(PlayerDropItemEvent e) {
        if (!GuiInvsee.isBeingWatched(e.getPlayer().getUniqueId())) {
            return;
        }
        scheduleRefresh(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        if (!GuiInvsee.isBeingWatched(player.getUniqueId())) {
            return;
        }
        scheduleRefresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetConsume(PlayerItemConsumeEvent e) {
        if (!GuiInvsee.isBeingWatched(e.getPlayer().getUniqueId())) {
            return;
        }
        scheduleRefresh(e.getPlayer());
    }

    private void scheduleRefresh(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> GuiInvsee.refreshForTarget(target));
    }

    private void handleLeft(InventoryClickEvent e, Player target, int slot,
                            ItemStack slotItem, ItemStack cursor) {
        if (cursor == null && slotItem == null) {
            return;
        }

        if (cursor == null) {
            setTargetItem(target, slot, null);
            e.getInventory().setItem(slot, null);
            e.getView().setCursor(slotItem);
        } else if (slotItem == null) {
            setTargetItem(target, slot, cursor.clone());
            e.getInventory().setItem(slot, cursor.clone());
            e.getView().setCursor(null);
        } else if (slotItem.isSimilar(cursor)) {
            int space = slotItem.getMaxStackSize() - slotItem.getAmount();
            int toAdd = Math.min(space, cursor.getAmount());
            slotItem.setAmount(slotItem.getAmount() + toAdd);
            cursor.setAmount(cursor.getAmount() - toAdd);
            setTargetItem(target, slot, slotItem.clone());
            e.getInventory().setItem(slot, slotItem.clone());
            e.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
        } else {
            setTargetItem(target, slot, cursor.clone());
            e.getInventory().setItem(slot, cursor.clone());
            e.getView().setCursor(slotItem);
        }
    }

    private void handleRight(InventoryClickEvent e, Player target, int slot,
                             ItemStack slotItem, ItemStack cursor) {
        if (cursor == null && slotItem == null) {
            return;
        }

        if (cursor == null) {
            int half = (int) Math.ceil(slotItem.getAmount() / 2.0);
            ItemStack taken = slotItem.clone();
            taken.setAmount(half);
            slotItem.setAmount(slotItem.getAmount() - half);
            setTargetItem(target, slot, slotItem.getAmount() <= 0 ? null : slotItem.clone());
            e.getInventory().setItem(slot, slotItem.getAmount() <= 0 ? null : slotItem.clone());
            e.getView().setCursor(taken);
        } else if (slotItem == null) {
            ItemStack one = cursor.clone();
            one.setAmount(1);
            setTargetItem(target, slot, one.clone());
            e.getInventory().setItem(slot, one.clone());
            cursor.setAmount(cursor.getAmount() - 1);
            e.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
        } else if (slotItem.isSimilar(cursor) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
            slotItem.setAmount(slotItem.getAmount() + 1);
            setTargetItem(target, slot, slotItem.clone());
            e.getInventory().setItem(slot, slotItem.clone());
            cursor.setAmount(cursor.getAmount() - 1);
            e.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
        } else {
            setTargetItem(target, slot, cursor.clone());
            e.getInventory().setItem(slot, cursor.clone());
            e.getView().setCursor(slotItem);
        }
    }

    private void handleShift(InventoryClickEvent e, Player viewer, Player target,
                             int slot, ItemStack slotItem) {
        if (slotItem == null) {
            return;
        }
        HashMap<Integer, ItemStack> leftover = viewer.getInventory().addItem(slotItem.clone());
        if (leftover.isEmpty()) {
            setTargetItem(target, slot, null);
            e.getInventory().setItem(slot, null);
        } else {
            ItemStack remaining = leftover.get(0);
            int moved = slotItem.getAmount() - remaining.getAmount();
            if (moved > 0) {
                ItemStack newAmount = slotItem.clone();
                newAmount.setAmount(remaining.getAmount());
                setTargetItem(target, slot, newAmount.clone());
                e.getInventory().setItem(slot, newAmount.clone());
            }
        }
    }

    private void handleNumberKey(InventoryClickEvent e, Player viewer, Player target,
                                 int slot, ItemStack slotItem) {
        int hotbarSlot = e.getHotbarButton();
        ItemStack hotbarItem = normalize(viewer.getInventory().getItem(hotbarSlot));
        setTargetItem(target, slot, hotbarItem != null ? hotbarItem.clone() : null);
        e.getInventory().setItem(slot, hotbarItem != null ? hotbarItem.clone() : null);
        viewer.getInventory().setItem(hotbarSlot, slotItem != null ? slotItem.clone() : null);
    }

    private void handleDrop(InventoryClickEvent e, Player target, int slot,
                            ItemStack slotItem, boolean all) {
        if (slotItem == null) {
            return;
        }
        if (all) {
            setTargetItem(target, slot, null);
            e.getInventory().setItem(slot, null);
            target.getWorld().dropItemNaturally(target.getLocation(), slotItem.clone());
        } else {
            ItemStack drop = slotItem.clone();
            drop.setAmount(1);
            slotItem.setAmount(slotItem.getAmount() - 1);
            setTargetItem(target, slot, slotItem.getAmount() <= 0 ? null : slotItem.clone());
            e.getInventory().setItem(slot, slotItem.getAmount() <= 0 ? null : slotItem.clone());
            target.getWorld().dropItemNaturally(target.getLocation(), drop);
        }
    }

    private void setTargetItem(Player target, int guiSlot, ItemStack item) {
        switch (guiSlot) {
            case 1 -> target.getInventory().setHelmet(item);
            case 2 -> target.getInventory().setChestplate(item);
            case 3 -> target.getInventory().setLeggings(item);
            case 4 -> target.getInventory().setBoots(item);
            case 6 -> target.getInventory().setItemInOffHand(item);
            default -> {
                if (guiSlot >= 9 && guiSlot <= 35) {
                    target.getInventory().setItem(guiSlot, item);
                } else if (guiSlot >= 36 && guiSlot <= 44) {
                    target.getInventory().setItem(guiSlot - 36, item);
                }
            }
        }
    }

    private int toGuiSlot(int playerSlot) {
        if (playerSlot >= 9 && playerSlot <= 35) {
            return playerSlot;
        }
        if (playerSlot >= 0 && playerSlot <= 8) {
            return playerSlot + 36;
        }
        return -1;
    }

    private int findAvailableSlot(Player target, ItemStack item) {
        for (int i = 9; i <= 35; i++) {
            ItemStack s = target.getInventory().getItem(i);
            if (!isEmpty(s) && s.isSimilar(item) && s.getAmount() < s.getMaxStackSize()) {
                return i;
            }
        }
        for (int i = 0; i <= 8; i++) {
            ItemStack s = target.getInventory().getItem(i);
            if (!isEmpty(s) && s.isSimilar(item) && s.getAmount() < s.getMaxStackSize()) {
                return i;
            }
        }
        for (int i = 9; i <= 35; i++) {
            if (isEmpty(target.getInventory().getItem(i))) {
                return i;
            }
        }
        for (int i = 0; i <= 8; i++) {
            if (isEmpty(target.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack normalize(ItemStack item) {
        return (item == null || item.getType() == Material.AIR) ? null : item;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}
