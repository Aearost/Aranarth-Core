package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Handles adding or removing a lock from a container.
 */
public class ContainerInteract {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        if (AranarthUtils.isContainerBlock(block)) {
            Player player = e.getPlayer();
            UUID uuid = player.getUniqueId();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);
            if (container == null) {
                return;
            }

            // If attempting to add a trusted player to the container
            if (aranarthPlayer.getTrustedPlayerUUID() != null) {
                // Only the owner can add players
                if (container.getOwner() == uuid) {
                    AranarthUtils.addPlayerToContainer(aranarthPlayer.getTrustedPlayerUUID(), block.getLocation());
                    String username = Bukkit.getOfflinePlayer(aranarthPlayer.getTrustedPlayerUUID()).getName();
                    player.sendMessage(ChatUtils.chatMessage("&e" + username + " &7has been trusted to this container!"));
                    aranarthPlayer.setTrustedPlayerUUID(null);
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                    e.setCancelled(true);
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
                }
            } else {
                // Logic to remove a lock from a container
                // Make sure unlock functionality removes lock from BOTH chests if it's double
                // Logic to untrust a player from a container
            }
        }
    }
}
