package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.AvatarAbilityChange;
import com.aearost.aranarthcore.utils.AvatarUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Centralizes all logic to be called when a command is entered by a player.
 */
public class PlayerCommandPreprocessEventListener implements Listener {

    public PlayerCommandPreprocessEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(e.getPlayer().getUniqueId())) {
            new AvatarAbilityChange().execute(e);
        }
    }
}
