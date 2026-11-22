package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Visually hides the armor of players with the Invisible Armor perk enabled, or who have Invisibility.
 * The armor stays worn, so it still protects the player.
 * The toggle is stored on the AranarthPlayer so that it applies on both Survival and SMP.
 */
public class InvisibleArmorManager implements Listener {

    private static final ItemStack AIR = new ItemStack(Material.AIR);

    // Players whose armor is currently being hidden from viewers on this server
    private static final Set<UUID> fakedPlayers = new HashSet<>();
    private static AranarthCore plugin;

    public InvisibleArmorManager(AranarthCore instance) {
        plugin = instance;
        migrateLegacyFile();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Re-broadcast fake packets every 3 seconds as a fallback for any equipment re-sync
        // that is not covered by an event, and to pick up toggle or perk changes
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refresh(player);
            }
        }, 60L, 60L);
    }

    /**
     * Provides whether the player has the Invisible Armor toggle enabled.
     */
    public static boolean isArmorHidden(UUID uuid) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        return aranarthPlayer != null && aranarthPlayer.isArmorHidden();
    }

    /**
     * Hides the player's armor visually from all other online players.
     */
    public static void hideArmor(Player player) {
        setArmorHidden(player, true);
    }

    /**
     * Restores the player's armor visuals for all other online players.
     */
    public static void showArmor(Player player) {
        setArmorHidden(player, false);
    }

    private static void setArmorHidden(Player player, boolean isHidden) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null) return;
        aranarthPlayer.setArmorHidden(isHidden);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        persistToggles(player.getUniqueId());
        refresh(player);
    }

    /**
     * Whether the toggle is on and the player still has the perk.
     */
    private static boolean isPerkActive(Player player) {
        return isArmorHidden(player.getUniqueId()) && player.hasPermission("aranarth.invisiblearmor");
    }

    /**
     * Hides the armor while the perk is active, or while the player has Invisibility.
     */
    private static boolean shouldHide(Player player) {
        return isPerkActive(player) || player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    /**
     * Sends the hidden or real armor to all viewers depending on the player's current state.
     */
    public static void refresh(Player player) {
        if (shouldHide(player)) {
            fakedPlayers.add(player.getUniqueId());
            sendFakeEquipmentToAll(player);
        } else if (fakedPlayers.remove(player.getUniqueId())) {
            sendRealEquipmentToAll(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player joined = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!joined.isOnline()) return;

            refresh(joined);

            for (UUID uuid : new HashSet<>(fakedPlayers)) {
                if (uuid.equals(joined.getUniqueId())) continue;
                Player hidden = Bukkit.getPlayer(uuid);
                if (hidden != null) {
                    sendFakeEquipmentToViewer(joined, hidden);
                }
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        fakedPlayers.remove(e.getPlayer().getUniqueId());
    }

    /**
     * A player coming into view (i.e render distance, respawn, world change) is sent their real armor.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent e) {
        if (!(e.getEntity() instanceof Player target)) return;
        if (!fakedPlayers.contains(target.getUniqueId())) return;
        Player viewer = e.getPlayer();
        // The spawn and equipment packets are only sent after this event
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline() && target.isOnline() && fakedPlayers.contains(target.getUniqueId())) {
                sendFakeEquipmentToViewer(viewer, target);
            }
        }, 1L);
    }

    /**
     * Any change to the worn armor is broadcast to every viewer.
     */
    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        if (!fakedPlayers.contains(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && fakedPlayers.contains(player.getUniqueId())) {
                sendFakeEquipmentToAll(player);
            }
        }, 1L);
    }

    /**
     * Clicking in the inventory makes the server re-send the real armor slots to the wearer, so they are hidden again.
     * This also runs for cancelled clicks, as the server re-sends the slots to revert them.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            rehideForSelf(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            rehideForSelf(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player player) {
            rehideForSelf(player);
        }
    }

    /**
     * Re-sends the hidden armor to the wearer themselves 1 tick later.
     */
    public static void rehideForSelf(Player player) {
        if (!fakedPlayers.contains(player.getUniqueId())) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && fakedPlayers.contains(player.getUniqueId())) {
                sendFakeEquipmentToViewer(player, player);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!fakedPlayers.contains(player.getUniqueId())) return;
        // Re-send 1 tick later to override the equipment re-sync the server sends on hit
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && fakedPlayers.contains(player.getUniqueId())) {
                sendFakeEquipmentToAll(player);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        Player player = e.getPlayer();
        if (!player.isGliding()) return;
        if (!fakedPlayers.contains(player.getUniqueId())) return;
        // Elytra durability loss while gliding makes the server re-broadcast the real chest item,
        // so re-send 2 ticks later to keep the wings hidden for the whole flight
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && fakedPlayers.contains(player.getUniqueId())) {
                sendFakeEquipmentToAll(player);
            }
        }, 2L);
    }

    private static void sendFakeEquipmentToAll(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            sendFakeEquipmentToViewer(viewer, target);
        }
    }

    private static void sendFakeEquipmentToViewer(Player viewer, Player target) {
        viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, AIR);
        // The client only lets a player glide if it sees a glider in their own chest slot, so it is never hidden from the wearer.
        // To others, a glider stays visible while only invisible, and is only hidden by the perk
        boolean isGlider = isGlider(target.getInventory().getChestplate());
        boolean hideChest = viewer.equals(target) ? !isGlider : (isPerkActive(target) || !isGlider);
        if (hideChest) {
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, AIR);
        }
        viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, AIR);
        viewer.sendEquipmentChange(target, EquipmentSlot.FEET, AIR);
    }

    private static boolean isGlider(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() == Material.ELYTRA) return true;
        return item.hasItemMeta() && item.getItemMeta().isGlider();
    }

    private static void sendRealEquipmentToAll(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, target.getInventory().getHelmet());
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, target.getInventory().getChestplate());
            viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, target.getInventory().getLeggings());
            viewer.sendEquipmentChange(target, EquipmentSlot.FEET, target.getInventory().getBoots());
        }
    }

    /**
     * Saves the player's toggles to MySQL right away so the change applies on the other server.
     */
    private static void persistToggles(UUID uuid) {
        if (!DatabaseManager.isActive()) return;
        String toggleJson = PersistenceUtils.buildPlayerToggleJson(uuid);
        if (toggleJson == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                DatabaseManager.getInstance().savePlayerToggles(uuid, toggleJson));
    }

    /**
     * Moves the toggles from the old per-server file onto the AranarthPlayer, which is shared by both servers.
     */
    private static void migrateLegacyFile() {
        Path legacyFile = plugin.getDataFolder().toPath().resolve("invisible_armor_players.dat");
        if (!Files.exists(legacyFile)) return;
        // Without MySQL the toggle would not be saved anywhere, so the file is kept
        if (!DatabaseManager.isActive()) return;
        try {
            List<String> toggleJsons = new ArrayList<>();
            List<UUID> uuids = new ArrayList<>();
            for (String line : Files.readAllLines(legacyFile)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(line);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                if (aranarthPlayer == null) continue;
                aranarthPlayer.setArmorHidden(true);
                AranarthUtils.setPlayer(uuid, aranarthPlayer);
                String toggleJson = PersistenceUtils.buildPlayerToggleJson(uuid);
                if (toggleJson != null) {
                    uuids.add(uuid);
                    toggleJsons.add(toggleJson);
                }
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (int i = 0; i < uuids.size(); i++) {
                    DatabaseManager.getInstance().savePlayerToggles(uuids.get(i), toggleJsons.get(i));
                }
                try {
                    Files.deleteIfExists(legacyFile);
                } catch (IOException ex) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[InvisibleArmor] Failed to delete the legacy file: " + ex.getMessage());
                }
            });
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[InvisibleArmor] Migrated " + uuids.size() + " player(s) from the legacy file");
        } catch (IOException ex) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[InvisibleArmor] Failed to read the legacy file: " + ex.getMessage());
        }
    }
}
