package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.UUID;

/**
 * Handles dumping a held shulker box's contents into a container.
 */
public class ShulkerDump {

    public void execute(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = e.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (!AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
            return;
        }

        Block block = e.getClickedBlock();
        if (!AranarthUtils.isContainerBlock(block)) {
            return;
        }

        if (!player.hasPermission("aranarth.shulker")) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return;
        }
        if (!(heldItem.getItemMeta() instanceof BlockStateMeta im)) {
            return;
        }
        if (!(im.getBlockState() instanceof ShulkerBox shulker)) {
            return;
        }

        // === Block interaction checks ===

        // Dominion CONTAINER permission - check explicitly since DominionProtectionListener
        // targets RIGHT_CLICK_BLOCK for container access and would not cover this left-click.
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());
        if (chunkDominion != null && !DominionUtils.hasPermission(player, chunkDominion, DominionPermission.CONTAINER)) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to access containers in this dominion!"));
            return;
        }

        // Locked container check (unclaimed land only - mirrors ContainerInteract.attemptOpen)
        if (chunkDominion == null) {
            LockedContainer lockedContainer = AranarthUtils.getLockedContainerAtBlock(block);
            if (lockedContainer != null) {
                UUID uuid = player.getUniqueId();
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                if (!lockedContainer.getTrusted().contains(uuid) && !aranarthPlayer.isInAdminMode()) {
                    e.setCancelled(true);
                    player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to access this container!"));
                    return;
                }
            }
        }

        // Shop container check - mirrors ContainerInteract.attemptOpen
        if (ShopUtils.getShopFromLocation(block.getRelative(BlockFace.UP).getLocation()) != null) {
            e.setCancelled(true);
            return;
        }

        // === Verify the shulker has items to dump ===
        Inventory shulkerInventory = shulker.getInventory();
        ItemStack[] shulkerContents = shulkerInventory.getContents();

        boolean hasItems = false;
        for (ItemStack item : shulkerContents) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYour shulker box is empty!"));
            return;
        }

        // === Get the target container inventory ===
        Container containerBlock = (Container) block.getState();
        Inventory containerInventory = containerBlock.getInventory();
        if (containerInventory.getHolder() instanceof DoubleChest doubleChest) {
            containerInventory = doubleChest.getInventory();
        }

        // === Transfer items - addItem() stacks into partial stacks before using empty slots ===
        boolean anyLeftover = false;
        for (int i = 0; i < shulkerContents.length; i++) {
            ItemStack item = shulkerContents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            HashMap<Integer, ItemStack> leftover = containerInventory.addItem(item.clone());
            if (leftover.isEmpty()) {
                shulkerContents[i] = null;
            } else {
                shulkerContents[i] = leftover.values().iterator().next();
                anyLeftover = true;
            }
        }

        // Persist updated shulker contents back onto the held item
        shulkerInventory.setContents(shulkerContents);
        im.setBlockState(shulker);
        heldItem.setItemMeta(im);

        e.setCancelled(true);
        player.playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE, 1F, 1F);
        if (anyLeftover) {
            player.sendMessage(ChatUtils.chatMessage("&eNot all items fit - remaining items are still in your shulker box."));
        } else {
            player.sendMessage(ChatUtils.chatMessage("&7Items emptied from your shulker box into the container!"));
        }
    }
}
