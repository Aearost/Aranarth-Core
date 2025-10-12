package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;

/**
 * Overrides spawning behaviour in Survival and SMP worlds.
 */
public class RespawnSurvival {
    public void execute(PlayerRespawnEvent e) {
        double x = e.getRespawnLocation().getBlockX();
        double z = e.getRespawnLocation().getBlockZ();
        Player player = e.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        try {
            AranarthUtils.switchInventory(player, player.getLocation().getWorld().getName(), player.getLocation().getWorld().getName());
        } catch (IOException ex) {
            player.sendMessage(ChatUtils.chatMessage("&cSomething went wrong with changing world."));
            return;
        }

        if (e.getRespawnLocation().getWorld().getName().equals("world")) {
            if (x == 0 && z == 3) {
                e.setRespawnLocation(new Location(e.getRespawnLocation().getWorld(), x, 120, z, 180, 0));
            }
        } else if (e.getRespawnLocation().getWorld().getName().equals("smp")) {
            e.setRespawnLocation(new Location(e.getRespawnLocation().getWorld(), x, 120, z, 180, 0));
        }


        if (AranarthUtils.isWearingArmorType(player, "soulbound")) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            int level = aranarthPlayer.getLevelBeforeDeath();
            float exp = aranarthPlayer.getExpBeforeDeath();
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.setLevel(level);
                    player.setExp(exp);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 220, 4));
                }
            }.runTaskLater(AranarthCore.getInstance(), 1L);
            aranarthPlayer.setLevelBeforeDeath(0);
            aranarthPlayer.setExpBeforeDeath(0);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        }
    }
}
