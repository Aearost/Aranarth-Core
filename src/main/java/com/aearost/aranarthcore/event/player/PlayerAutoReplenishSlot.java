package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Provides functionality to automatically replenish a player's slot with a stack from their inventory.
 */
public class PlayerAutoReplenishSlot {
    public void execute(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        if (e.getItemInHand().getMaxStackSize() > 1 && e.getItemInHand().getAmount() - 1 == 0) {
            PlayerInventory inventory = player.getInventory();
            int placedSlot;
            if (e.getHand() == EquipmentSlot.HAND) {
                placedSlot = inventory.getHeldItemSlot();
            } else {
                placedSlot = 40; // Hardcoded value of off-hand slot
            }
            replenishSlot(player, e.getItemInHand(), placedSlot, ItemStack::isSimilar);
        }
    }

    public void execute(PlayerItemConsumeEvent e, Plugin plugin) {
        Player player = e.getPlayer();

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        // Only replenish if consuming the last item in the stack
        if (e.getItem().getAmount() != 1) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        final ItemStack referenceItem = e.getItem().clone();

        // Determine which hand the item is being consumed from
        final int consumedSlot;
        if (inventory.getItemInMainHand().isSimilar(e.getItem())) {
            consumedSlot = inventory.getHeldItemSlot();
        } else {
            consumedSlot = 40; // Offhand
        }

        // Schedule replenishment to run after the item has been consumed
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            replenishSlot(player, referenceItem, consumedSlot, ItemStack::isSimilar);
        });
    }

    public void execute(PlayerDropItemEvent e) {
        Player player = e.getPlayer();

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        // At event time the inventory is already updated, check which hand slot became empty
        PlayerInventory inventory = player.getInventory();
        final int droppedSlot;
        if (inventory.getItemInMainHand().getType() == Material.AIR) {
            droppedSlot = inventory.getHeldItemSlot();
        } else if (inventory.getItemInOffHand().getType() == Material.AIR) {
            droppedSlot = 40;
        } else {
            return;
        }

        replenishSlot(player, e.getItemDrop().getItemStack(), droppedSlot, ItemStack::isSimilar);
    }

    public void execute(ProjectileLaunchEvent e, Plugin plugin) {
        if (!(e.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        Material projectileMaterial = getProjectileMaterial(e.getEntity());
        if (projectileMaterial == null) {
            return;
        }

        // At event time the item is still in the player's hand, find which slot holds it
        PlayerInventory inventory = player.getInventory();
        final int thrownSlot;
        if (inventory.getItemInMainHand().getType() == projectileMaterial
                && inventory.getItemInMainHand().getAmount() == 1) {
            thrownSlot = inventory.getHeldItemSlot();
        } else if (inventory.getItemInOffHand().getType() == projectileMaterial
                && inventory.getItemInOffHand().getAmount() == 1) {
            thrownSlot = 40;
        } else {
            return;
        }

        final ItemStack referenceItem = inventory.getItem(thrownSlot).clone();

        // Schedule replenishment to run after the item has been removed
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            replenishSlot(player, referenceItem, thrownSlot, ItemStack::isSimilar);
        });
    }

    public void execute(PlayerItemBreakEvent e, Plugin plugin) {
        Player player = e.getPlayer();

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        final ItemStack brokenItem = e.getBrokenItem().clone();

        // Determine which slot the tool broke from
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();
        final int brokenSlot;
        if (mainHand.getType() == brokenItem.getType()) {
            brokenSlot = inventory.getHeldItemSlot();
        } else if (offHand.getType() == brokenItem.getType()) {
            brokenSlot = 40;
        } else if (mainHand.getType() == Material.AIR) {
            brokenSlot = inventory.getHeldItemSlot();
        } else {
            brokenSlot = 40;
        }

        // Schedule replenishment to run after the item has been removed
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            replenishSlot(player, brokenItem, brokenSlot, this::isSameToolType);
        });
    }

    public void execute(PlayerInteractEvent e, Plugin plugin) {
        if (e.getItem() == null || e.getItem().getType() != Material.BONE_MEAL) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();

        if (!player.hasPermission("aranarth.inventory")) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            return;
        }

        if (e.getItem().getAmount() != 1) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        final int usedSlot = e.getHand() == EquipmentSlot.HAND ? inventory.getHeldItemSlot() : 40;
        final ItemStack referenceItem = e.getItem().clone();

        // Schedule replenishment for after the server processes the bone meal use
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            ItemStack current = inventory.getItem(usedSlot);
            if (current != null && current.getType() != Material.AIR) {
                return;
            }
            replenishSlot(player, referenceItem, usedSlot, ItemStack::isSimilar);
        });
    }

    /**
     * Maps a thrown projectile entity to the inventory item that produced it.
     */
    private Material getProjectileMaterial(Entity projectile) {
        if (projectile instanceof Egg) {
            return Material.EGG;
        }
        if (projectile instanceof Snowball) {
            return Material.SNOWBALL;
        }
        if (projectile instanceof ThrownExpBottle) {
            return Material.EXPERIENCE_BOTTLE;
        }
        return null;
    }

    private boolean isSameToolType(ItemStack a, ItemStack b) {
        if (a.getType() != b.getType()) {
            return false;
        }
        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();
        if (metaA == null && metaB == null) {
            return true;
        }
        if (metaA == null || metaB == null) {
            return false;
        }
        if (metaA.hasDisplayName() != metaB.hasDisplayName()) {
            return false;
        }
        if (metaA.hasDisplayName() && !metaA.getDisplayName().equals(metaB.getDisplayName())) {
            return false;
        }
        if (!Objects.equals(metaA.getLore(), metaB.getLore())) {
            return false;
        }
        if (!metaA.getEnchants().equals(metaB.getEnchants())) {
            return false;
        }
        return true;
    }

    private void replenishSlot(Player player, ItemStack referenceItem, int slot, BiPredicate<ItemStack, ItemStack> matcher) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = contents[i];
            if (itemStack == null) {
                continue;
            }

            // If the slot is another one of the same item, switch it
            if (i != slot && matcher.test(itemStack, referenceItem)) {
                if (!(itemStack.getItemMeta() instanceof BlockStateMeta)) {
                    inventory.setItem(slot, new ItemStack(contents[i]));
                    inventory.setItem(i, null);
                    player.updateInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.5F);
                    return;
                }
            }

            // If that slot is a shulker box, cycle through it as well
            if (itemStack.getItemMeta() instanceof BlockStateMeta im) {
                if (im.getBlockState() instanceof ShulkerBox shulker) {
                    if (player.hasPermission("aranarth.shulker")) {
                        Inventory shulkerInventory = shulker.getInventory();
                        ItemStack[] shulkerContents = shulkerInventory.getContents();
                        for (int j = 0; j < shulkerInventory.getSize(); j++) {
                            if (shulkerContents[j] != null) {
                                if (matcher.test(shulkerContents[j], referenceItem)) {
                                    inventory.setItem(slot, new ItemStack(shulkerContents[j]));
                                    shulkerInventory.setItem(j, null);
                                    shulker.update();
                                    im.setBlockState(shulker);
                                    itemStack.setItemMeta(im);
                                    inventory.setItem(i, itemStack);
                                    contents[i].setItemMeta(im);
                                    player.updateInventory();
                                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0F, 1.5F);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
