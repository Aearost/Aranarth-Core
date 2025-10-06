package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles automatically locking a placed container.
 */
public class ContainerAutoLock {

    public void execute(BlockPlaceEvent e) {
        List<LockedContainer> lockedContainers = AranarthUtils.getLockedContainers();
        UUID uuid = e.getPlayer().getUniqueId();
        Location loc = e.getBlock().getLocation();
        List<UUID> trustedPlayers = new ArrayList<>();
        trustedPlayers.add(uuid);
        // Only has the player placing the chest trusted by default
        LockedContainer container = new LockedContainer(uuid, trustedPlayers, AranarthUtils.getLocationsOfContainer(e.getBlock()));
        AranarthUtils.addLockedContainer(container);
        e.getPlayer().sendMessage(ChatUtils.chatMessage("&7This container has been locked"));
    }
}
