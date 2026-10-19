package com.aearost.aranarthcore.event.block;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Allows non-op creative players to cycle the brightness of a light block by right-clicking it while holding a light.
 */
public class LightLevelCycle {

    public void execute(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.LIGHT) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.LIGHT) {
            return;
        }

        Player player = e.getPlayer();
        // Ops are already handled by vanilla, and sneaking places a new light instead
        if (player.getGameMode() != GameMode.CREATIVE || player.isOp() || player.isSneaking()) {
            return;
        }

        // Changing the light level modifies the block, so it must pass the same protections as placing one
        // A full stack is passed so the inventory auto-replenish does not treat this check as using up the last item
        BlockPlaceEvent placeCheck = new BlockPlaceEvent(block, block.getState(), block,
                item.asQuantity(item.getMaxStackSize()), player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(placeCheck);
        if (placeCheck.isCancelled() || !placeCheck.canBuild()) {
            return;
        }

        Levelled light = (Levelled) block.getBlockData();
        light.setLevel((light.getLevel() + 1) % (light.getMaximumLevel() + 1));
        block.setBlockData(light, false);
        e.setCancelled(true);
    }
}
