package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Handles toggling a single door when shift + left clicking.
 * Bypasses the double-door logic to allow individual door toggling,
 * useful when doors get flipped out of sync (e.g. by airbending).
 */
public class SingleDoorOpen {

    public void execute(PlayerInteractEvent e) {
        if (!e.getPlayer().isSneaking()) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        if (!(block.getBlockData() instanceof Door door)) {
            return;
        }

        if (AranarthUtils.isSpawnLocation(block.getLocation())) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
            if (!aranarthPlayer.isInAdminMode()) {
                return;
            }
        }

        Dominion playerDominion = DominionUtils.getPlayerDominion(e.getPlayer().getUniqueId());
        Dominion blockDominion = DominionUtils.getDominionOfChunk(block.getChunk());
        if (blockDominion != null) {
            if (playerDominion == null || !playerDominion.isSameDominion(blockDominion)) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
                if (!aranarthPlayer.isInAdminMode()) {
                    return;
                }
            }
        }

        e.setCancelled(true);

        boolean nowOpen = !door.isOpen();
        door.setOpen(nowOpen);
        block.setBlockData(door, true);

        // Play door sound manually since the event is cancelled
        Sound sound;
        if (block.getType() == Material.IRON_DOOR) {
            sound = nowOpen ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
        } else {
            sound = nowOpen ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
        }
        block.getWorld().playSound(block.getLocation(), sound, 1.0f, 1.0f);
    }

}
