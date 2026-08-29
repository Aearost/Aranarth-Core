package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages the Invisible Armor perk.
 */
public class InvisibleArmorManager implements Listener {

    private static final ItemStack AIR = new ItemStack(Material.AIR);

    private static final Set<UUID> hiddenPlayers = new HashSet<>();
    private static Path persistFile;
    private static AranarthCore plugin;

    public InvisibleArmorManager(AranarthCore instance) {
        plugin = instance;
        persistFile = plugin.getDataFolder().toPath().resolve("invisible_armor_players.dat");
        loadHiddenPlayers();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Re-broadcast fake packets every 3 seconds to cover players entering
        // render distance and any armor changes while the toggle is active
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(hiddenPlayers)) {
                Player hidden = Bukkit.getPlayer(uuid);
                if (hidden == null) continue;
                sendFakeEquipmentToAll(hidden);
            }
        }, 60L, 60L);
    }

    public static boolean isArmorHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    /**
     * Hides the player's armor visually from all other online players.
     */
    public static void hideArmor(Player player) {
        hiddenPlayers.add(player.getUniqueId());
        saveHiddenPlayers();
        sendFakeEquipmentToAll(player);
    }

    /**
     * Restores the player's armor visuals for all other online players.
     */
    public static void showArmor(Player player) {
        hiddenPlayers.remove(player.getUniqueId());
        saveHiddenPlayers();
        sendRealEquipmentToAll(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player joined = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!joined.isOnline()) return;

            if (hiddenPlayers.contains(joined.getUniqueId())) {
                sendFakeEquipmentToAll(joined);
            }

            for (UUID uuid : new HashSet<>(hiddenPlayers)) {
                if (uuid.equals(joined.getUniqueId())) continue;
                Player hidden = Bukkit.getPlayer(uuid);
                if (hidden != null) {
                    sendFakeEquipmentToViewer(joined, hidden);
                }
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!hiddenPlayers.contains(player.getUniqueId())) return;
        // Re-send 1 tick later to override the equipment re-sync the server sends on hit
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sendFakeEquipmentToAll(player);
            }
        }, 1L);
    }

    private static void sendFakeEquipmentToAll(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendFakeEquipmentToViewer(viewer, target);
        }
    }

    private static void sendFakeEquipmentToViewer(Player viewer, Player target) {
        viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, AIR);
        viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, AIR);
        viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, AIR);
        viewer.sendEquipmentChange(target, EquipmentSlot.FEET, AIR);
    }

    private static void sendRealEquipmentToAll(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, target.getInventory().getHelmet());
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, target.getInventory().getChestplate());
            viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, target.getInventory().getLeggings());
            viewer.sendEquipmentChange(target, EquipmentSlot.FEET, target.getInventory().getBoots());
        }
    }

    private static void loadHiddenPlayers() {
        if (!Files.exists(persistFile)) return;
        try {
            for (String line : Files.readAllLines(persistFile)) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        hiddenPlayers.add(UUID.fromString(line));
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        } catch (IOException ignored) { }
    }

    private static void saveHiddenPlayers() {
        try {
            List<String> lines = hiddenPlayers.stream().map(UUID::toString).toList();
            Files.write(persistFile, lines);
        } catch (IOException ignored) { }
    }
}
