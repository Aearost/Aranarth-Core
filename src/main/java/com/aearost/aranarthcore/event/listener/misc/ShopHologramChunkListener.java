package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.DefenderChunkLoad;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.ShopUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.List;

public class ShopHologramChunkListener implements Listener {

    public ShopHologramChunkListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent e) {
        new DefenderChunkLoad().execute(e);
        Chunk chunk = e.getChunk();
        for (List<Shop> shopList : ShopUtils.getShops().values()) {
            for (Shop shop : shopList) {
                Location loc = shop.getLocation();
                if (loc.getWorld() != null && loc.getWorld().equals(chunk.getWorld())
                        && (loc.getBlockX() >> 4) == chunk.getX()
                        && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                    ShopUtils.initializeShopHologram(shop);
                }
            }
        }
    }
}
