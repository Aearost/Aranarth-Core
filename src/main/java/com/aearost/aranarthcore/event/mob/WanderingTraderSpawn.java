package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.WanderingTraderType;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.WanderingTraderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Assigns the type of trader and announces the spawning of a new Wandering Trader.
 */
public class WanderingTraderSpawn {
    public void execute(EntitySpawnEvent e) {
        WanderingTrader wanderingTrader = (WanderingTrader) e.getEntity();

        // Delay 1 tick so the entity is fully initialised before we overwrite its trades.
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (wanderingTrader.getPersistentDataContainer().has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
                return;
            }
            WanderingTraderType type = WanderingTraderUtils.applyTrader(wanderingTrader);
            String typeName = type.getPlainName();

            for (Player player : Bukkit.getOnlinePlayers()) {
                String worldName = player.getLocation().getWorld().getName();
                if (worldName.equalsIgnoreCase("world")
                        || AranarthUtils.isSmpWorld(worldName)
                        || worldName.equalsIgnoreCase("resource")) {
                    if (player.getWorld().getName().equals(wanderingTrader.getLocation().getWorld().getName())) {
                        if (player.getLocation().distance(wanderingTrader.getLocation()) <= 100) {
                            Bukkit.broadcastMessage(ChatUtils.chatMessage(
                                "&7A " + typeName + " has spawned nearby &e" + AranarthUtils.getNickname(player)
                            ));
                            return;
                        }
                    }
                }
            }
        }, 1L);
    }
}
