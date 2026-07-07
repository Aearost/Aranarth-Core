package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.abilities.waterbending.bloodbending.LifeRip;
import com.aearost.aranarthcore.event.mob.MountListener;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Mount;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Handles preventing or adding custom logic to commands being executed by players.
 */
public class CommandOverrides {

    public void execute(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String[] parts = e.getMessage().split(" ");

        if (!parts[0].equals("/afk")) {
            if (aranarthPlayer.getAfkLocation() != null) {
                if (aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
                    Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
                        }
                    }, 1);
                } else {
                    aranarthPlayer.setAfkLocation(null);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                }
            }
        }

        if (parts[0].equals("/plugins") || parts[0].equals("/pl")) {
            Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    sb.append("&r, ");
                }
                sb.append(plugins[i].isEnabled() ? "&7" : "&c");
                sb.append(plugins[i].getName());
            }
            player.sendMessage(ChatUtils.chatMessage("&e&lPlugins (" + plugins.length + ") &7&l- " + sb));
            e.setCancelled(true);
            return;
        }

        if (aranarthPlayer.getCouncilRank() != 3) {
            if (parts[0].equals("/w")) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
                e.setCancelled(true);
                return;
            }
        }

        // Prevent the command entirely
        if (parts[0].equals("/time") && AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            if (aranarthPlayer.getCouncilRank() < 2) {
                player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&cUse &e/ac time <time> &cinstead!"));
            }
            e.setCancelled(true);
            return;
        }

        // Prevent the command entirely
        if (parts[0].equals("/me")) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to execute this command!"));
            e.setCancelled(true);
            return;
        }

        // Adding and removing the sub-elements upon changing element without relogging
        if (parts[0].startsWith("/b")) {
            if (parts[0].equalsIgnoreCase("/b") || parts[0].toLowerCase().startsWith("/bend")) {
                if (parts.length > 1) {
                    if (parts[1].equalsIgnoreCase("ch") || parts[1].equalsIgnoreCase("choose")) {
                        // Player executing this in the arena world prevents sub-elements from being removed when changing world
                        if (!player.getWorld().getName().equalsIgnoreCase("arena")) {
                            PermissionUtils.evaluatePlayerPermissions(player);
                        }
                        // Dismiss any active mount as the new element may have a different mount
                        dismissActiveMountIfPresent(player);
                        // Strip any LifeRip health bonus
                        LifeRip.resetCasterGain(player);
                    }
                }
            }
        }
    }

    private void dismissActiveMountIfPresent(Player player) {
        UUID mountId = MountUtils.getActiveMountEntityUUID(player.getUniqueId());
        if (mountId == null) {
            return;
        }

        String[] info = MountUtils.getActiveMountInfo(mountId);
        Entity entity = Bukkit.getEntity(mountId);

        if (entity instanceof LivingEntity mount) {
            if (info != null) {
                Mount mountData = MountUtils.get(player.getUniqueId(), info[1]);
                if (mountData != null) {
                    mountData.setCurrentHealth(mount.getHealth());
                }
            }
            MountListener ml = MountListener.getInstance();
            if (ml != null) {
                ml.cleanupMountPublic(mountId);
            }
            mount.eject();
            mount.remove();
        } else {
            MountUtils.unregisterActive(mountId);
        }

        if (info != null) {
            String displayName = MountUtils.getDisplayName(player.getUniqueId(), info[1]);
            player.sendMessage(ChatUtils.chatMessage(
                    MountUtils.getElementColor(info[1]) + displayName + " &7has returned."));
        }
    }

}
