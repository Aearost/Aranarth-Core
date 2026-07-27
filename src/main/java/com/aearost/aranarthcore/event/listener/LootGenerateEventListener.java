package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.block.StructureLootLuck;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

/**
 * Centralizes all logic to be called when a loot table generates items for a container.
 */
public class LootGenerateEventListener implements Listener {

    public LootGenerateEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent e) {
        new StructureLootLuck().execute(e);
    }
}
