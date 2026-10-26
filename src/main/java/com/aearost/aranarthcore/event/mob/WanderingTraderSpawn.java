package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.WanderingTraderType;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.WanderingTraderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Assigns the type of trader and announces the spawning of a new Wandering Trader.
 */
public class WanderingTraderSpawn {
    public void execute(EntitySpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        WanderingTrader wanderingTrader = (WanderingTrader) e.getEntity();

        // Spawns nearby another player that is not AFK, otherwise the original spawn goes ahead
        if (e instanceof CreatureSpawnEvent creatureSpawnEvent
                && creatureSpawnEvent.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                && WanderingTraderUtils.shouldRelocateTrader(e.getLocation())
                && WanderingTraderUtils.spawnTraderNearRandomPlayer()) {
            e.setCancelled(true);
            return;
        }

        // Delay 1 tick so the entity is fully initialised before we overwrite its trades.
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            // The spawn may have been cancelled by a listener that ran after this one
            if (!wanderingTrader.isValid()) {
                return;
            }
            if (wanderingTrader.getPersistentDataContainer().has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
                return;
            }
            WanderingTraderType type = WanderingTraderUtils.applyTrader(wanderingTrader);
            String typeName = type.getPlainName();

            // Announce the nearby player that is not AFK, otherwise any nearby player
            Player nearbyPlayer = null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                String worldName = player.getLocation().getWorld().getName();
                if (worldName.equalsIgnoreCase("world")
                        || AranarthUtils.isSmpWorld(worldName)
                        || worldName.equalsIgnoreCase("resource")) {
                    if (player.getWorld().getName().equals(wanderingTrader.getLocation().getWorld().getName())) {
                        if (player.getLocation().distance(wanderingTrader.getLocation()) <= 100) {
                            if (!AranarthUtils.isAfk(AranarthUtils.getPlayer(player.getUniqueId()))) {
                                nearbyPlayer = player;
                                break;
                            }
                            if (nearbyPlayer == null) {
                                nearbyPlayer = player;
                            }
                        }
                    }
                }
            }
            if (nearbyPlayer != null) {
                Lang.broadcast("trader.spawned_nearby", "type", typeName, "player", AranarthUtils.getNickname(nearbyPlayer));
            }
        }, 1L);
    }
}
