package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.IOException;

/**
 * Overrides spawning behaviour in overworld to spawn in the spawn shack.
 */
public class RespawnSurvival {
    public void execute(PlayerRespawnEvent e) {
        double x = e.getRespawnLocation().getBlockX();
        double z = e.getRespawnLocation().getBlockZ();
        Player player = e.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        try {
            AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), "world");
        } catch (IOException ex) {
            player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
            return;
        }
        if (x == 0 && z == 3) {
            e.setRespawnLocation(new Location(e.getRespawnLocation().getWorld(), x, 120, z, 180, 0));
        }
    }
}
