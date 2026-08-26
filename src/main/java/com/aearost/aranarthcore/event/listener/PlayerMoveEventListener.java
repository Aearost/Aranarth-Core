package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Centralizes all logic to be called by a player moving.
 */
public class PlayerMoveEventListener implements Listener {

    public PlayerMoveEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        String worldName = e.getPlayer().getLocation().getWorld().getName();
        if (AranarthUtils.isSmpWorld(worldName)) {
            new HomepadStep().execute(e);
            new DominionChunkChange().execute(e);
        } else if (worldName.startsWith("world")) {
            new DominionChunkChange().execute(e);
            new SpawnChangeLocation().execute(e);
        }

        new AfkCancelByMove().execute(e);
        new PlayerTeleportCancelByMove().execute(e);

        Player player = e.getPlayer();
        if (AranarthUtils.isWearingArmorType(player, "elven")) {
            boolean onGround = player.isOnGround();
            Boolean wasOnGround = AranarthUtils.playerWasOnGround.get(player.getUniqueId());
            if (wasOnGround != null && wasOnGround && !onGround
                    && e.getTo() != null && e.getTo().getY() > e.getFrom().getY()) {
                int arVol = AranarthUtils.getPlayer(player.getUniqueId()).getAranarthiumSoundVolume();
                if (arVol > 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 0.2f * (arVol / 100f), 1.2f);
                }
            }
            AranarthUtils.playerWasOnGround.put(player.getUniqueId(), onGround);
        }
    }

}
