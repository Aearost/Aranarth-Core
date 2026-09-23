package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.airbending.AirScooter;
import com.projectkorra.projectkorra.airbending.AirSpout;
import com.projectkorra.projectkorra.airbending.Tornado;
import com.projectkorra.projectkorra.airbending.flight.FlightMultiAbility;
import com.projectkorra.projectkorra.firebending.FireJet;
import com.projectkorra.projectkorra.waterbending.WaterSpout;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Resets walk/fly speed and flight state to defaults when a player enters Survival mode.
 */
public class PlayerGameModeChangeEventListener implements Listener {

    private final AranarthCore plugin;

    public PlayerGameModeChangeEventListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startOrphanedFlightCheck();
    }

    /**
     * Revokes flight for any survival player with no active flight ability.
     */
    private void startOrphanedFlightCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        continue;
                    }
                    if (!player.getAllowFlight()) {
                        continue;
                    }
                    if (!hasActiveFlightAbility(player)) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.setFlySpeed(0.1f);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Returns true if the player has an active PK ability that legitimately grants flight.
     */
    private boolean hasActiveFlightAbility(Player player) {
        return CoreAbility.hasAbility(player, AirSpout.class)
                || CoreAbility.hasAbility(player, AirScooter.class)
                || CoreAbility.hasAbility(player, Tornado.class)
                || CoreAbility.hasAbility(player, WaterSpout.class)
                || CoreAbility.hasAbility(player, FireJet.class)
                || CoreAbility.hasAbility(player, FlightMultiAbility.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        GameMode to = e.getNewGameMode();

        if (to == GameMode.SURVIVAL) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.setWalkSpeed(0.2f);
                    player.setFlySpeed(0.1f);
                }
            }.runTaskLater(plugin, 1L);
        } else if (to == GameMode.CREATIVE) {
            // Reset on creative entry so the player always starts at a sane speed.
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (player.getFlySpeed() < 0.09f) {
                        player.setFlySpeed(0.1f);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /**
     * Cancel and explicitly revoke flight for survival players with no flight ability.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlightGranted(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL || !e.isFlying()) {
            return;
        }
        if (!hasActiveFlightAbility(player)) {
            e.setCancelled(true);
            player.setAllowFlight(false);
        }
    }

    /**
     * Reset flySpeed, and revoke allowFlight only if no flight ability is still active.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL || e.isFlying()) {
            return;
        }
        player.setFlySpeed(0.1f);
        if (!hasActiveFlightAbility(player)) {
            player.setAllowFlight(false);
        }
    }
}
