package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.EyeblossomLight;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

/**
 * Centralizes all logic to be called by blocks fading.
 */
public class BlockPhysicsEventListener implements Listener {

    public BlockPhysicsEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockFade(BlockPhysicsEvent e) {
        new EyeblossomLight().execute(e);
    }
}
