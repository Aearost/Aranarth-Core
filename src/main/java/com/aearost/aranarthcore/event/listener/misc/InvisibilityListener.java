package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.airbending.spiritual.AstralProjection;
import com.aearost.aranarthcore.event.mob.InvisibilityMobTargetPrevent;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles armor interactions and changes while a player has Invisibility.
 */
public class InvisibilityListener implements Listener {

    private static Path armorStoreDir;
    private static AranarthCore plugin;

    public InvisibilityListener(AranarthCore instance) {
        plugin = instance;
        armorStoreDir = plugin.getDataFolder().toPath().resolve("invisibility_armor");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Armor used to be removed while invisible, so any left over from before is given back
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
            // Delay 1 tick so the effect is fully applied
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                InvisibleArmorManager.refresh(player);
                if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    InvisibilityMobTargetPrevent.clearTargets(player);
                }
            }, 1L);
        } else if ((action == Action.REMOVED || action == Action.CLEARED)
                && oldEffect != null
                && oldEffect.getType() == PotionEffectType.INVISIBILITY) {
            // Delay so hasPotionEffect would no longer return true at this point
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    InvisibleArmorManager.refresh(player);
                }
            }, 1L);
        }
    }

    /**
     * Prevents any change to the worn armor through the inventory while invisible.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getClickedInventory() instanceof PlayerInventory inv)) return;
        if (!isArmorLocked(player)) return;

        boolean isChangingArmor = false;
        if (e.getSlotType() == InventoryType.SlotType.ARMOR) {
            // Any click on a worn armor slot, whether taking, placing, swapping or a hotbar key
            isChangingArmor = true;
        } else if (e.isShiftClick() && e.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            // Shift-clicking from the player's own inventory screen equips into an empty armor slot
            EquipmentSlot targetSlot = getArmorSlot(e.getCurrentItem());
            isChangingArmor = targetSlot != null && isEmpty(inv.getItem(targetSlot));
        }

        if (isChangingArmor) {
            e.setCancelled(true);
            denyArmorChange(player);
        }
    }

    /**
     * Prevents armor from being dragged into an armor slot while invisible.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onArmorDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!isArmorLocked(player)) return;
        for (int rawSlot : e.getRawSlots()) {
            if (e.getView().getSlotType(rawSlot) == InventoryType.SlotType.ARMOR
                    && e.getView().getInventory(rawSlot) instanceof PlayerInventory) {
                e.setCancelled(true);
                denyArmorChange(player);
                return;
            }
        }
    }

    /**
     * Prevents armor from being equipped or swapped by right-clicking it while invisible.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorRightClick(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (e.useItemInHand() == Event.Result.DENY) return;
        ItemStack item = e.getItem();
        if (isEmpty(item)) return;
        Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || !equippable.swappable() || getArmorSlot(item) == null) return;
        Player player = e.getPlayer();
        if (!isArmorLocked(player)) return;
        e.setUseItemInHand(Event.Result.DENY);
        denyArmorChange(player);
    }

    /**
     * Prevents dispensers from putting armor on an invisible player.
     */
    @EventHandler(ignoreCancelled = true)
    public void onArmorDispense(BlockDispenseArmorEvent e) {
        if (!(e.getTargetEntity() instanceof Player player)) return;
        if (!isArmorLocked(player)) return;
        e.setCancelled(true);
    }

    /**
     * Puts any armor left over from the old system back on before the quit listener saves the inventory.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        restoreArmorBeforeLeaving(player, player.getWorld().getName());
    }

    /**
     * Death drops do not include armor left over from the old system, so it is added to them.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        // Any armor it displaces is already part of the drops, so it must not be dropped a second time
        e.getDrops().addAll(restoreArmor(player, player.getWorld().getName(), false, false));
    }

    /**
     * Gives back any armor that the old system had removed and not yet returned (i.e after a crash).
     * Waits for the login inventory to be applied, as it would otherwise replace the armor.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            restoreArmor(player, player.getWorld().getName(), true, true);
        }, 60L);
    }

    /**
     * Whether the player's worn armor must not be changed.
     */
    private static boolean isArmorLocked(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return false;
        // AstralProjection manages the armor itself
        return !AstralProjection.isProjecting(player.getUniqueId());
    }

    /**
     * Notifies the player and re-syncs their inventory so that the cancelled change is fully reverted on their screen.
     */
    private static void denyArmorChange(Player player) {
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "armor.invisibility_blocked")));
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    /**
     * Provides the armor slot that the item is equipped into, or null if it is not worn in an armor slot.
     */
    private static EquipmentSlot getArmorSlot(ItemStack item) {
        if (isEmpty(item)) return null;
        Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable == null) return null;
        EquipmentSlot slot = equippable.slot();
        return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> slot;
            default -> null;
        };
    }

    public static void restoreArmor(Player player) {
        restoreArmor(player, player.getWorld().getName(), true, false);
    }

    /**
     * Puts any armor left over from the old system back on before the player's inventory is saved and replaced.
     *
     * @param player The player.
     * @param fromWorld The name of the world that the inventory being saved belongs to.
     */
    public static void restoreArmorBeforeLeaving(Player player, String fromWorld) {
        // Some callers have already teleported the player, and creative or arena items must not land outside of their world
        String fromGroup = AranarthUtils.getInventoryGroup(fromWorld);
        boolean canDrop = fromGroup.equals("world") || fromGroup.equals(AranarthUtils.getInventoryGroup(player.getWorld().getName()));
        restoreArmor(player, fromWorld, canDrop, false);
    }

    /**
     * Puts the armor stored by the old system for the given world's inventory back on the player.
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
                boolean isWearing = !isEmpty(worn[i]);
                // Only the exact same item counts as already restored, so that a different piece of the same material is not lost
                if (isEmpty(armor[i]) || (isRecovery && isWearing && worn[i].isSimilar(armor[i]))) {
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
        return restored;
    }

    /**
     * Provides the file storing the armor removed by the old system from the given world's inventory.
     */
    private static Path armorFile(UUID uuid, String world) {
        String group = AranarthUtils.getInventoryGroup(world);
        return armorStoreDir.resolve(group.equals("world") ? uuid + ".dat" : uuid + "_" + group + ".dat");
    }
}
