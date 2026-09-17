package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Handles the Elven Aranarthium double-jump passive ability.
 * Players wearing a full set of Elven Aranarthium can double-jump once every 2 seconds.
 */
public class ElvenDoubleJumpListener implements Listener {

    private static final long COOLDOWN_MS = 2000L;

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

        UUID uuid = player.getUniqueId();
        Long lastUsed = AranarthUtils.elvenDoubleJumpCooldown.get(uuid);
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < COOLDOWN_MS) {
            return;
        }

        AranarthUtils.elvenCanDoubleJump.add(uuid);
    }

    /**
     * When the jump key is pressed while already airborne and eligible, apply the double-jump boost.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInput(PlayerInputEvent e) {
        if (!e.getInput().isJump()) {
            return;
        }

        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!AranarthUtils.elvenCanDoubleJump.contains(uuid)) {
            return;
        }
        if (player.isOnGround()) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "elven")) {
            AranarthUtils.elvenCanDoubleJump.remove(uuid);
            return;
        }

        AranarthUtils.elvenCanDoubleJump.remove(uuid);
        AranarthUtils.elvenDoubleJumpCooldown.put(uuid, System.currentTimeMillis());

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
