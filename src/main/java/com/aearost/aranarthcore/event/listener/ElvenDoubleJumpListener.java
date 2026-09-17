package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Handles the Elven Aranarthium double-jump passive ability.
 */
public class ElvenDoubleJumpListener implements Listener {

    private static final long COOLDOWN_MS = 2000L;

    public ElvenDoubleJumpListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJump(PlayerJumpEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "elven")) {
            return;
        }
        // Don't interfere if they already have flight for another reason
        if (player.getAllowFlight()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Long lastUsed = AranarthUtils.elvenDoubleJumpCooldown.get(uuid);
        if (lastUsed != null && System.currentTimeMillis() - lastUsed < COOLDOWN_MS) {
            return;
        }

        AranarthUtils.elvenCanDoubleJump.add(uuid);
        player.setAllowFlight(true);
    }

    /**
     * Intercepts the "start flying" toggle and converts it to a double-jump boost.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!AranarthUtils.elvenCanDoubleJump.contains(uuid)) {
            return;
        }
        if (!AranarthUtils.isWearingArmorType(player, "elven")) {
            clearDoubleJump(player);
            return;
        }
        if (!e.isFlying()) {
            return; // Only intercept the "start flying" toggle
        }

        e.setCancelled(true);
        clearDoubleJump(player);

        AranarthUtils.elvenDoubleJumpCooldown.put(uuid, System.currentTimeMillis());

        Vector vel = player.getVelocity();
        vel.setY(0.75);
        player.setVelocity(vel);

        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap != null) {
            int arVol = ap.getAranarthiumSoundVolume();
            if (arVol > 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.35f * (arVol / 100f), 1.3f);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearDoubleJump(e.getPlayer());
    }

    private void clearDoubleJump(Player player) {
        UUID uuid = player.getUniqueId();
        if (AranarthUtils.elvenCanDoubleJump.remove(uuid)) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
            }
        }
    }

}
