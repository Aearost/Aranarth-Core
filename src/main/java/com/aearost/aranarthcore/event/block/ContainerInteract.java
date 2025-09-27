package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Handles adding or removing a lock from a container.
 */
public class ContainerInteract {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (AranarthUtils.isContainerBlock(block)) {
            UUID uuid = e.getPlayer().getUniqueId();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

            // If attempting to add a trusted player to the container
            if (aranarthPlayer.getTrustedPlayerUUID() != null) {
                if (AranarthUtils.canOpenContainer(e.getPlayer(), block)) {
                    LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);
                    if (container != null) {
                        // Only the owner can add players
                        if (container.getOwner() == uuid) {
                            AranarthUtils.addPlayerToContainer(aranarthPlayer.getTrustedPlayerUUID(), container.getLocation());
                            String username = Bukkit.getOfflinePlayer(aranarthPlayer.getTrustedPlayerUUID()).getName();
                            e.getPlayer().sendMessage(ChatUtils.chatMessage("&e" + username + " &7has been trusted to this container!"));
                            aranarthPlayer.setTrustedPlayerUUID(null);
                            AranarthUtils.setPlayer(uuid, aranarthPlayer);
                            e.setCancelled(true);
                        }
                    }
                }
            } else {
                // Logic to remove a lock from a container
                // Make sure unlock functionality removes lock from BOTH chests if it's double
                // Logic to untrust a player from a container
            }
        }
    }
}
