package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Physically removes a player's armor while they have Invisibility.
 */
public class InvisibilityListener implements Listener {

    private static final Set<UUID> invisiblePlayers = new HashSet<>();
    private static final Map<UUID, double[]> storedArmorValues = new HashMap<>();
    private static Path armorStoreDir;
    private final AranarthCore plugin;

    public InvisibilityListener(AranarthCore plugin) {
        this.plugin = plugin;
        armorStoreDir = plugin.getDataFolder().toPath().resolve("invisibility_armor");
        try {
            Files.createDirectories(armorStoreDir);
        } catch (IOException e) { }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Restore armor for any online players with a saved file (e.g. after /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            restoreArmor(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        PotionEffect oldEffect = e.getOldEffect();
        PotionEffect newEffect = e.getNewEffect();
        Action action = e.getAction();

        if (action == Action.ADDED && newEffect != null
                && newEffect.getType() == PotionEffectType.INVISIBILITY) {
            invisiblePlayers.add(player.getUniqueId());
            // Delay 1 tick so the effect is fully applied before we remove armor
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> saveAndRemoveArmor(player), 1L);
        } else if ((action == Action.REMOVED || action == Action.CLEARED)
                && oldEffect != null
                && oldEffect.getType() == PotionEffectType.INVISIBILITY) {
            // Delay so hasPotionEffect would still return true at this point
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)
                        && invisiblePlayers.remove(player.getUniqueId())) {
                    restoreArmor(player);
                }
            }, 1L);
        }
    }

    /**
     * Puts the armor back on before the quit listener saves the inventory, so that the saved
     * inventory is complete on whichever server the player logs in to next.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        restoreArmorBeforeLeaving(player, player.getWorld().getName());
        invisiblePlayers.remove(player.getUniqueId());
    }

    /**
     * Death drops do not include the removed armor, so it is put back on before the armor-based
     * death logic runs (i.e Soulbound) and is dropped along with the rest of the inventory.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (invisiblePlayers.contains(player.getUniqueId())) {
            // Any armor it displaces is already part of the drops, so it must not be dropped a second time
            e.getDrops().addAll(restoreArmor(player, player.getWorld().getName(), false, false));
        }
    }

    /**
     * A file only remains at this point if the server did not shut down cleanly.
     * Waits for the login inventory to be applied, as it would otherwise replace the armor.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || invisiblePlayers.contains(uuid)) {
                return;
            }
            // The inventory on this server is not the player's while they wait to be routed elsewhere
            if (NetworkManager.isActive() && NetworkManager.getInstance().isNonAuthoritative(uuid)) {
                return;
            }
            restoreArmor(player, player.getWorld().getName(), true, true);
        }, 60L);
    }

    public static boolean isInvisible(UUID uuid) {
        return invisiblePlayers.contains(uuid);
    }

    private static void saveAndRemoveArmor(Player player) {
        // The effect may have ended or the player may have left since this was scheduled
        if (!player.isOnline() || !player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return;

        // Overwriting an existing file would delete the armor stored in it
        Path file = armorFile(player.getUniqueId(), player.getWorld().getName());
        if (Files.exists(file)) return;

        PlayerInventory inv = player.getInventory();
        ItemStack[] armor = inv.getArmorContents();

        boolean hasArmor = false;
        for (ItemStack item : armor) {
            if (item != null && !item.getType().isAir()) {
                hasArmor = true;
                break;
            }
        }
        if (!hasArmor) return;

        // Snapshot armor and toughness NOW, before removing items, so entity
        // attributes are still accurate when damage reduction is calculated later
        AttributeInstance armorAttr = player.getAttribute(Attribute.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attribute.ARMOR_TOUGHNESS);
        double armorVal = armorAttr != null ? armorAttr.getValue() : 0;
        double toughnessVal = toughnessAttr != null ? toughnessAttr.getValue() : 0;
        storedArmorValues.put(player.getUniqueId(), new double[]{armorVal, toughnessVal});

        byte[] data = ItemStack.serializeItemsAsBytes(armor);
        try {
            Files.write(file, data);
        } catch (IOException ex) {
            // The armor must stay on the player if there is no copy of it
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to store the armor of " + player.getName() + ": " + ex.getMessage());
            return;
        }

        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
    }

    public static double[] getStoredArmorValues(UUID uuid) {
        return storedArmorValues.getOrDefault(uuid, new double[]{0, 0});
    }

    public static void restoreArmor(Player player) {
        restoreArmor(player, player.getWorld().getName(), true, false);
    }

    /**
     * Puts the armor back on before the player's inventory is saved and replaced.
     *
     * @param player The player.
     * @param fromWorld The name of the world that the inventory being saved belongs to.
     */
    public static void restoreArmorBeforeLeaving(Player player, String fromWorld) {
        if (!invisiblePlayers.contains(player.getUniqueId())) {
            return;
        }
        // Some callers have already teleported the player, and creative or arena items must not land outside of their world
        String fromGroup = AranarthUtils.getInventoryGroup(fromWorld);
        boolean canDrop = fromGroup.equals("world") || fromGroup.equals(AranarthUtils.getInventoryGroup(player.getWorld().getName()));
        restoreArmor(player, fromWorld, canDrop, false);
    }

    /**
     * Puts the stored armor of the given world's inventory back on the player.
     *
     * @param player The player.
     * @param world The name of the world that the armor was stored from.
     * @param canDrop Whether armor that does not fit in the inventory may be dropped.
     * @param isRecovery Whether this is after a crash, where the worn armor may already be the stored armor.
     * @return The pieces that were put back on.
     */
    private static List<ItemStack> restoreArmor(Player player, String world, boolean canDrop, boolean isRecovery) {
        List<ItemStack> restored = new ArrayList<>();
        Path file = armorFile(player.getUniqueId(), world);
        if (!Files.exists(file)) return restored;

        try {
            ItemStack[] armor = ItemStack.deserializeItemsFromBytes(Files.readAllBytes(file));
            PlayerInventory inv = player.getInventory();
            ItemStack[] worn = inv.getArmorContents();
            List<ItemStack> displaced = new ArrayList<>();
            for (int i = 0; i < armor.length; i++) {
                boolean isWearing = worn[i] != null && !worn[i].getType().isAir();
                if (armor[i] == null || armor[i].getType().isAir()
                        || (isRecovery && isWearing && worn[i].getType() == armor[i].getType())) {
                    armor[i] = worn[i];
                    continue;
                }
                if (isWearing) {
                    displaced.add(worn[i]);
                }
                restored.add(armor[i]);
            }
            inv.setArmorContents(armor);
            for (ItemStack piece : displaced) {
                for (ItemStack overflow : inv.addItem(piece).values()) {
                    if (canDrop) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                    }
                }
            }
            Files.delete(file);
        } catch (IOException ex) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to restore the armor of " + player.getName() + ": " + ex.getMessage());
        }
        storedArmorValues.remove(player.getUniqueId());
        invisiblePlayers.remove(player.getUniqueId());
        return restored;
    }

    /**
     * Provides the file storing the armor removed from the given world's inventory.
     */
    private static Path armorFile(UUID uuid, String world) {
        String group = AranarthUtils.getInventoryGroup(world);
        return armorStoreDir.resolve(group.equals("world") ? uuid + ".dat" : uuid + "_" + group + ".dat");
    }
}
