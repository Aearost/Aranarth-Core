package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.AvatarAbilityChange;
import com.aearost.aranarthcore.event.player.CommandOverrides;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Centralizes all logic to be called when a command is entered by a player.
 */
public class PlayerCommandPreprocessEventListener implements Listener {

    public PlayerCommandPreprocessEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // LOWEST priority - set active player before any other handler or command runs
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandSetLocale(PlayerCommandPreprocessEvent e) {
        Lang.setActivePlayer(e.getPlayer().getUniqueId());
        // Clear on the next tick so the thread-local does not leak into unrelated sync tasks
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), Lang::clearActivePlayer);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClickSetLocale(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            Lang.setActivePlayer(player.getUniqueId());
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), Lang::clearActivePlayer);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractSetLocale(PlayerInteractEvent e) {
        Lang.setActivePlayer(e.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), Lang::clearActivePlayer);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(e.getPlayer().getUniqueId())) {
            new AvatarAbilityChange().execute(e);
        }
        new CommandOverrides().execute(e);
    }
}
