package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

/**
 * Freezes players whose inventory is in the middle of a cross-server handoff.
 */
public class TransferLockListener implements Listener {

    public TransferLockListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isLocked(Player player) {
        return NetworkManager.isActive() && NetworkManager.getInstance().isLocked(player.getUniqueId());
    }

    private void cancelIfLocked(Player player, Cancellable e) {
        if (isLocked(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(final PlayerDropItemEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(final EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(final InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(final PlayerInteractEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(final PlayerInteractEntityEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(final PlayerSwapHandItemsEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(final PlayerItemConsumeEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(final BlockPlaceEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(final BlockBreakEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(final PlayerTeleportEvent e) {
        cancelIfLocked(e.getPlayer(), e);
    }

    /**
     * Commands can move money or items (/pay, /sell, /trash), which would not be in the copy that
     * was already sent to the other server.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        if (isLocked(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "general.already_teleporting")));
        }
    }

    /**
     * A death here would drop the inventory that the other server is about to restore.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDealDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player) {
            cancelIfLocked(player, e);
        }
    }

    /**
     * Damage that skips the damage event (setHealth) can still kill, so keep the items in place.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(final PlayerDeathEvent e) {
        if (isLocked(e.getEntity())) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
    }
}
