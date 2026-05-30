package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * Sets a custom death message when a player is killed by another player's mount.
 */
public class MountKillDeathMessage {

    public void execute(PlayerDeathEvent e) {
        Entity damager = e.getDamageSource().getCausingEntity();
        if (damager == null) {
            return;
        }

        String[] info = MountUtils.getActiveMountInfo(damager.getUniqueId());
        if (info == null) {
            return;
        }

        UUID ownerUUID;
        try {
            ownerUUID = UUID.fromString(info[0]);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner == null) {
            return;
        }

        String element = info[1];
        String mountName = MountUtils.getDisplayName(ownerUUID, element);
        String color = MountUtils.getElementColor(element);
        String verb = getMountKillVerb(element);

        Player victim = e.getEntity();
        e.setDeathMessage(victim.getName() + " has been " + verb + " by "
                + owner.getName() + "'s " + color + mountName);
    }

    private String getMountKillVerb(String element) {
        return switch (element) {
            case "FIRE" -> "rammed";
            case "WATER" -> "mauled";
            case "EARTH" -> "crushed";
            case "AIR" -> "bellowed away";
            default -> "killed";
        };
    }
}
