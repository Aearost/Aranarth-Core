package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
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
                Bukkit.getLogger().info("Will trust");
                trust(e);
            }
            // Logic to untrust a player from the container
            else if (aranarthPlayer.getUntrustedPlayerUUID() != null) {
                Bukkit.getLogger().info("Will untrust");
                untrust(e);
            }
            // Logic to lock the container
            else if (aranarthPlayer.getIsLockingContainer()) {
                Bukkit.getLogger().info("Will lock");
                lock(e);
            }
            // Logic to unlock the container
            else if (aranarthPlayer.getIsUnlockingContainer()) {
                Bukkit.getLogger().info("Will unlock");
                unlock(e);
            }
            // Trying to open the container
            else {
                attemptOpen(e);
            }
        }
    }

    /**
     * Logic to trust a player to the container.
     * @param e The event.
     */
    private void trust(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);

        if (container == null) {
            player.sendMessage(ChatUtils.chatMessage("&cThis is not a locked container!"));
        } else {
            // Only the owner can add players
            if (container.getOwner().equals(uuid)) {
                if (uuid.equals(aranarthPlayer.getTrustedPlayerUUID())) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou already own this container!"));
                } else {
                    AranarthUtils.addPlayerToContainer(aranarthPlayer.getTrustedPlayerUUID(), block.getLocation());
                    String username = Bukkit.getOfflinePlayer(aranarthPlayer.getTrustedPlayerUUID()).getName();
                    player.sendMessage(ChatUtils.chatMessage("&e" + username + " &7has been trusted to this container!"));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
            }
        }
        aranarthPlayer.setTrustedPlayerUUID(null);
        AranarthUtils.setPlayer(uuid, aranarthPlayer);
        e.setCancelled(true);
    }

    /**
     * Logic to untrust a player from the container.
     * @param e The event.
     */
    private void untrust(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);

        if (container == null) {
            player.sendMessage(ChatUtils.chatMessage("&cThis is not a locked container!"));
        } else {
            // Only the owner can remove players
            if (container.getOwner().equals(uuid)) {
                if (uuid.equals(aranarthPlayer.getUntrustedPlayerUUID())) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou must unlock this container instead!"));
                } else {
                    boolean wasRemoved = AranarthUtils.removePlayerFromContainer(aranarthPlayer.getUntrustedPlayerUUID(), block.getLocation());
                    String username = Bukkit.getOfflinePlayer(aranarthPlayer.getUntrustedPlayerUUID()).getName();
                    if (wasRemoved) {
                        player.sendMessage(ChatUtils.chatMessage("&e" + username + " &7is no longer trusted to this container!"));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&e" + username + " &ccould not be removed from this container!"));
                    }
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
            }
        }
        aranarthPlayer.setUntrustedPlayerUUID(null);
        AranarthUtils.setPlayer(uuid, aranarthPlayer);
        e.setCancelled(true);
    }

    /**
     * Logic to lock a container.
     * @param e The event.
     */
    private void lock(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);

        if (container != null) {
            player.sendMessage(ChatUtils.chatMessage("&cThis container is already locked!"));
        } else {
            List<UUID> trusted = new ArrayList<>();
            trusted.add(player.getUniqueId());
            Location[] locations = AranarthUtils.getLocationsOfContainer(block);
            LockedContainer lockedContainer = new LockedContainer(player.getUniqueId(), trusted, locations);
            AranarthUtils.addLockedContainer(lockedContainer);
            player.sendMessage(ChatUtils.chatMessage("&7This container has been locked!"));
        }
        aranarthPlayer.setIsLockingContainer(false);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        e.setCancelled(true);
    }

    /**
     * Logic to unlock a container.
     * @param e The event.
     */
    private void unlock(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);

        if (container == null) {
            player.sendMessage(ChatUtils.chatMessage("&cThis is not a locked container!"));
        } else {
            // Only the owner can remove a lock
            if (container.getOwner().equals(uuid)) {
                int breakResult = AranarthUtils.removeLockedContainer(container.getLocations());
                if (breakResult == 0) {
                    player.sendMessage(ChatUtils.chatMessage("&7The lock was successfully removed from this container!"));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&cThe lock could not be removed from this container!"));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cYou are not the owner of this container!"));
            }
        }
        aranarthPlayer.setIsUnlockingContainer(false);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        e.setCancelled(true);
    }

    /**
     * Logic to attempt the opening of a locked container.
     * @param e The event.
     */
    private void attemptOpen(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(block.getChunk());

        if (chunkDominion != null) {
            return;
        }

        LockedContainer container = AranarthUtils.getLockedContainerAtBlock(block);

        if (container != null) {
            List<UUID> trusted = container.getTrusted();
            for (UUID trustedUuid : trusted) {
                if (player.getUniqueId().equals(trustedUuid)) {
                    return;
                }
            }
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to open this container!"));
        }
    }
}
