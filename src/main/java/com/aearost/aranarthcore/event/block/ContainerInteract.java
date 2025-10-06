package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
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

            // Logic to trust a player to the container
            if (aranarthPlayer.getTrustedPlayerUUID() != null) {
                if (container == null) {
                    return;
                }

                // Only the owner can add players
                if (container.getOwner().equals(uuid)) {
                    AranarthUtils.addPlayerToContainer(aranarthPlayer.getTrustedPlayerUUID(), block.getLocation());
                    String username = Bukkit.getOfflinePlayer(aranarthPlayer.getTrustedPlayerUUID()).getName();
                    player.sendMessage(ChatUtils.chatMessage("&e" + username + " &7has been trusted to this container!"));
                    aranarthPlayer.setTrustedPlayerUUID(null);
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
                }
                e.setCancelled(true);
            }
            // Logic to untrust a player from the container
            else if (aranarthPlayer.getUntrustedPlayerUUID() != null) {
                if (container == null) {
                    return;
                }

                // Only the owner can remove players
                if (container.getOwner().equals(uuid)) {
                    boolean wasRemoved = AranarthUtils.removePlayerFromContainer(aranarthPlayer.getUntrustedPlayerUUID(), block.getLocation());
                    String username = Bukkit.getOfflinePlayer(aranarthPlayer.getUntrustedPlayerUUID()).getName();
                    if (wasRemoved) {
                        player.sendMessage(ChatUtils.chatMessage("&e" + username + " &7is no longer trusted to this container!"));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&e" + username + " &ccould not be removed from this container!"));
                    }
                    aranarthPlayer.setUntrustedPlayerUUID(null);
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                    e.setCancelled(true);
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
                }
            }
            // Logic to lock the container
            else if (aranarthPlayer.getIsLockingContainer()) {
                List<UUID> trusted = new ArrayList<>();
                trusted.add(player.getUniqueId());
                Location[] locations = AranarthUtils.getLocationsOfContainer(block);
                LockedContainer lockedContainer = new LockedContainer(player.getUniqueId(), trusted, locations);
                AranarthUtils.addLockedContainer(lockedContainer);
                aranarthPlayer.setIsLockingContainer(false);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage("&7This container has been locked!"));
                e.setCancelled(true);
            }
            // Logic to unlock the container
            else if (aranarthPlayer.getIsUnlockingContainer()) {
                if (container == null) {
                    return;
                }

                // Only the owner can remove a lock
                if (container.getOwner().equals(uuid)) {
                    boolean wasRemoved = AranarthUtils.removeLockedContainer(block.getLocation());
                    if (wasRemoved) {
                        player.sendMessage(ChatUtils.chatMessage("&7The lock was successfully removed from this container!"));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&cThe lock could not be removed from this container!"));
                    }
                    aranarthPlayer.setIsUnlockingContainer(false);
                    AranarthUtils.setPlayer(uuid, aranarthPlayer);
                    e.setCancelled(true);
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
                }
            }
        }
    }
}
