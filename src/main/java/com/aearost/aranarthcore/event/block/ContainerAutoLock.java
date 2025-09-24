package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.objects.LockedContainer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
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
        // Only has the player placing the chest
        LockedContainer container = new LockedContainer(uuid, trustedPlayers, loc);

        if (lockedContainers == null) {
            lockedContainers = new ArrayList<>();
        }
        lockedContainers.add(container);
        AranarthUtils.setLockedContainers(lockedContainers);
    }
}
