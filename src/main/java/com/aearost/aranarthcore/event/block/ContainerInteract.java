package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.UUID;

/**
 * Handles adding or removing a lock from a container.
 */
public class ContainerInteract {

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (isContainer(block)) {
            Bukkit.getLogger().info("A");
            UUID uuid = e.getPlayer().getUniqueId();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            // If attempting to add a lock to the container
            if (aranarthPlayer.getTrustedPlayerUUID() != null) {
                Bukkit.getLogger().info("B");
                List<LockedContainer> lockedContainers = AranarthUtils.getLockedContainers();
                for (LockedContainer container : lockedContainers) {
                    Bukkit.getLogger().info("C");
                    Location location = block.getLocation();
                    if (container.getLocation().getBlockX() == location.getBlockX()
                            && container.getLocation().getBlockY() == location.getBlockY()
                            && container.getLocation().getBlockZ() == location.getBlockZ()) {
                        Bukkit.getLogger().info("D");
                        // Only the owner can add players
                        if (container.getOwner() == uuid) {
                            Bukkit.getLogger().info("E");
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
                // Logic to untrust a player from a container
            }
        }
    }

    private boolean isContainer(Block block) {
        return block.getType() == Material.CHEST
                || block.getType() == Material.TRAPPED_CHEST
                || block.getType() == Material.BARREL
                || block.getType().name().endsWith("SHULKER_BOX");
    }

}
