package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.EchoArmourUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;

/**
 * Centralizes all logic to be called when a game event (i.e a vibration picked up by sculk sensors) is triggered.
 */
public class GenericGameEventListener implements Listener {

    public GenericGameEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGenericGameEvent(GenericGameEvent e) {
        EchoArmourUtils.onGameEvent(e);
    }
}
