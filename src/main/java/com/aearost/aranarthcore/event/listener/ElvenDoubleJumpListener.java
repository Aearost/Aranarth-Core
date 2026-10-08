package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Handles the Elven Aranarthium double-jump passive ability.
 * Players wearing a full set of Elven Aranarthium can double-jump once per jump by sneaking mid-air.
 */
public class ElvenDoubleJumpListener implements Listener {

    public ElvenDoubleJumpListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Mark the player as eligible for a double-jump when they leave the ground.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJump(PlayerJumpEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "elven")) {
            return;
        }

        AranarthUtils.elvenCanDoubleJump.add(player.getUniqueId());
    }

    /**
     * When sneak is pressed while already airborne and eligible, apply the double-jump boost.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) {
            return;
        }

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!AranarthUtils.elvenCanDoubleJump.contains(uuid)) {
            return;
        }
        if (player.isOnGround() || player.isClimbing() || player.isGliding() || player.isFlying()
                || player.isInWater() || player.isInsideVehicle()) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Sneaking mid-air is also how many bending abilities are used, so do not double-jump on a bound slot
        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
        if (bendingPlayer != null && bendingPlayer.getBoundAbility() != null) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "elven")) {
            AranarthUtils.elvenCanDoubleJump.remove(uuid);
            return;
        }

        AranarthUtils.elvenCanDoubleJump.remove(uuid);

        // Horizontal direction from where the player is looking, plus upward boost
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(new Vector(dir.getX() * 0.5, 0.5, dir.getZ() * 0.5));

        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            int arVol = ap.getAranarthiumSoundVolume();
            if (arVol > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 0.2f * (arVol / 100f), 1.2f);
            }
        }

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.2, 0.1, 0.2, 0.02);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        AranarthUtils.elvenCanDoubleJump.remove(e.getPlayer().getUniqueId());
    }

}
