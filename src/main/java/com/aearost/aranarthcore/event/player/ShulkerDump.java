package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
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
 * Handles dumping a held shulker box's contents into a container,
 * or filling a held empty shulker box from a container.
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
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.access_containers")));
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
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "lock.no_permission_access")));
                    return;
                }
            }
        }

        // Shop container check - mirrors ContainerInteract.attemptOpen
        if (ShopUtils.getShopForContainer(block) != null) {
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
        Container containerBlock = (Container) block.getState();

        // An empty shulker is filled from the container instead
        if (!hasItems) {
            e.setCancelled(true);
            fillShulker(player, heldItem, im, shulker, containerBlock);
            return;
        }

        // === Get the target container inventory ===
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
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "inventory.shulker_not_all_fit")));
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "inventory.emptied_shulker")));
        }
    }

    /**
     * Moves the contents of the clicked container into the held empty shulker box.
     * For a double chest, only the clicked half is used.
     *
     * @param player The player holding the shulker box.
     * @param heldItem The held shulker box item.
     * @param im The held shulker box's meta.
     * @param shulker The held shulker box's block state.
     * @param containerBlock The clicked container.
     */
    private void fillShulker(Player player, ItemStack heldItem, BlockStateMeta im, ShulkerBox shulker, Container containerBlock) {
        // Prevents filling a whole stack of shulker boxes with one set of items
        if (heldItem.getAmount() > 1) {
            return;
        }

        // Chest.getBlockInventory() only covers the clicked half of a double chest
        Inventory containerInventory = containerBlock instanceof Chest chest ? chest.getBlockInventory() : containerBlock.getInventory();
        ItemStack[] containerContents = containerInventory.getContents();

        boolean hasItems = false;
        for (ItemStack item : containerContents) {
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                break;
            }
        }
        if (!hasItems) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "inventory.container_empty")));
            return;
        }

        Inventory shulkerInventory = shulker.getInventory();
        boolean anyLeftover = false;
        boolean anyMoved = false;
        for (int i = 0; i < containerContents.length; i++) {
            ItemStack item = containerContents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            // Shulker boxes cannot be nested
            if (Tag.SHULKER_BOXES.isTagged(item.getType())) {
                anyLeftover = true;
                continue;
            }
            HashMap<Integer, ItemStack> leftover = shulkerInventory.addItem(item.clone());
            if (leftover.isEmpty()) {
                containerInventory.setItem(i, null);
                anyMoved = true;
            } else {
                ItemStack remaining = leftover.values().iterator().next();
                if (remaining.getAmount() != item.getAmount()) {
                    anyMoved = true;
                }
                containerInventory.setItem(i, remaining);
                anyLeftover = true;
            }
        }

        if (anyMoved) {
            // Persist updated shulker contents back onto the held item
            im.setBlockState(shulker);
            heldItem.setItemMeta(im);
            player.playSound(player, Sound.BLOCK_SHULKER_BOX_CLOSE, 1F, 1F);
        }

        if (anyLeftover) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "inventory.container_not_all_moved")));
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "inventory.filled_shulker")));
        }
    }
}
