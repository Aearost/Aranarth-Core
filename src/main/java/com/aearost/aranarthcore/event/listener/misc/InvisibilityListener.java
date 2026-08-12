package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
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
import java.util.HashMap;
import java.util.HashSet;
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
     * If a player logs off while invisible, their armor file stays on disk.
     * On next join it will be restored.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        invisiblePlayers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        restoreArmor(e.getPlayer());
    }

    public static boolean isInvisible(UUID uuid) {
        return invisiblePlayers.contains(uuid);
    }

    private static void saveAndRemoveArmor(Player player) {
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
            Files.write(armorStoreDir.resolve(player.getUniqueId() + ".dat"), data);
        } catch (IOException ex) { }

        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
    }

    public static double[] getStoredArmorValues(UUID uuid) {
        return storedArmorValues.getOrDefault(uuid, new double[]{0, 0});
    }

    public static void restoreArmor(Player player) {
        Path file = armorStoreDir.resolve(player.getUniqueId() + ".dat");
        if (!Files.exists(file)) return;

        try {
            byte[] data = Files.readAllBytes(file);
            ItemStack[] armor = ItemStack.deserializeItemsFromBytes(data);
            player.getInventory().setArmorContents(armor);
            Files.delete(file);
        } catch (IOException ex) { }
        storedArmorValues.remove(player.getUniqueId());
        invisiblePlayers.remove(player.getUniqueId());
    }
}
