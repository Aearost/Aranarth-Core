package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PlotUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Reverts the player's level and EXP when dying in the arena or creative world.
 * Clears the inventory when dying in a creative world, while the arena inventory is kept.
 * Additionally, forces the player to respawn in the same non-survival world when they die in it.
 */
public class RespawnNonSurvival {
    public void execute(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        String world = e.getPlayer().getWorld().getName();
        if (world.equalsIgnoreCase("arena")) {
            // The arena inventory is kept on death
            e.setRespawnLocation(AranarthUtils.getArenaRespawnLocation());
        } else if (PlotUtils.isSmpCreativeWorld(world)) {
            player.getInventory().clear();
            e.setRespawnLocation(PlotUtils.getSmpCreativeSpawnLocation());
            player.setGameMode(GameMode.CREATIVE);
        } else {
            player.getInventory().clear();
            e.setRespawnLocation(PlotUtils.getSpawnLocation());
            player.setGameMode(GameMode.CREATIVE);
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        int level = aranarthPlayer.getLevelBeforeDeath();
        float exp = aranarthPlayer.getExpBeforeDeath();
        boolean isArena = world.equalsIgnoreCase("arena");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                // The arena keeps the armour equipped, but the Curse of Binding must still release on death
                if (isArena && AranarthUtils.releaseBindingCurseArmor(player)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "access.curse_binding_released")));
                }
                player.setLevel(level);
                player.setExp(exp);
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
        aranarthPlayer.setLevelBeforeDeath(0);
        aranarthPlayer.setExpBeforeDeath(0);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
    }
}
