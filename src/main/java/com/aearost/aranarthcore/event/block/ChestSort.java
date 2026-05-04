package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.ChestSortOrder;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Handles the chest sort logic.
 */
public class ChestSort {

    public void execute(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!AranarthUtils.isContainerBlock(e.getClickedBlock())) {
            return;
        }
        if (e.getPlayer().getGameMode() != GameMode.SURVIVAL || !e.getPlayer().isSneaking()) {
            return;
        }

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();

        if (!player.hasPermission("aranarth.inventory")) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to sort chests!"));
            return;
        }

        // Deny sort if the container is locked and the player is not trusted (mirrors attemptOpen logic)
        LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(block);
        if (lockedContainer != null) {
            UUID playerUuid = player.getUniqueId();
            boolean isTrusted = lockedContainer.getTrusted().contains(playerUuid);
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(playerUuid);
            if (!isTrusted && !aranarthPlayer.isInAdminMode()) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to sort this container!"));
                return;
            }
        }

        BlockState state = block.getState();
        Container container = (Container) state;
        Inventory inventory = container.getInventory();
        if (inventory.getHolder() instanceof DoubleChest doubleChest) {
            inventory = doubleChest.getInventory();
        }

        // Collect clones of all real items (skip null/air slots)
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        items = stackItems(items);
        items.sort(Comparator.comparingInt(this::getSortOrder));

        inventory.clear();
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
        }

        player.sendMessage(ChatUtils.chatMessage("&7The chest has been sorted!"));
        player.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT, 1F, 1F);
    }

    /**
     * Consolidates partial stacks of the same item type into full stacks.
     * Works on cloned items, so the original inventory is not touched until we write back.
     */
    private List<ItemStack> stackItems(List<ItemStack> items) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack incoming : items) {
            int remaining = incoming.getAmount();
            for (ItemStack existing : result) {
                if (remaining <= 0) {
                    break;
                }
                if (!existing.isSimilar(incoming)) {
                    continue;
                }
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space <= 0) {
                    continue;
                }
                int transfer = Math.min(space, remaining);
                existing.setAmount(existing.getAmount() + transfer);
                remaining -= transfer;
            }
            if (remaining > 0) {
                ItemStack leftover = incoming.clone();
                leftover.setAmount(remaining);
                result.add(leftover);
            }
        }
        return result;
    }

    private int getSortOrder(ItemStack item) {
        try {
            return ChestSortOrder.valueOf(item.getType().name()).ordinal();
        } catch (IllegalArgumentException e) {
            return Integer.MAX_VALUE;
        }
    }
}
