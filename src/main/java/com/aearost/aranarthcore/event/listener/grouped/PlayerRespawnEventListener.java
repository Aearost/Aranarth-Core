package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.player.RespawnNonSurvival;
import com.aearost.aranarthcore.event.player.RespawnSurvival;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Centralizes all logic to be called by clicking in an inventory.
 */
public class PlayerRespawnEventListener implements Listener {

    // Players who have died and not yet respawned
    private static final Set<UUID> deadPlayers = new HashSet<>();
    // Players whose currently processing death event is a duplicate of a death that already happened
    private static final Set<UUID> duplicateDeaths = new HashSet<>();

    public PlayerRespawnEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Flags a death event as a duplicate if the player has already died and not yet respawned.
     * Some damage (such as bending fire tick) can cause the death to be processed a second time.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathDuplicateCheck(final PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (!deadPlayers.add(player.getUniqueId())) {
            duplicateDeaths.add(player.getUniqueId());
            Bukkit.getLogger().log(Level.WARNING, AranarthCore.LOG_PREFIX + "Suppressed a duplicate death of "
                    + player.getName() + " - stack trace of the cause:", new Throwable("Duplicate death"));
        }
    }

    /**
     * Suppresses the message and any drops of a duplicate death, as they were already handled by the original death.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeathDuplicateSuppress(final PlayerDeathEvent e) {
        if (isDuplicateDeath(e.getEntity().getUniqueId())) {
            e.setDeathMessage(null);
            e.getDrops().clear();
            e.setDroppedExp(0);
            e.setKeepInventory(true);
            e.setKeepLevel(true);
        }
    }

    /**
     * Clears the duplicate flag once all listeners have processed the death event.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathDuplicateClear(final PlayerDeathEvent e) {
        duplicateDeaths.remove(e.getEntity().getUniqueId());
    }

    /**
     * Determines if the death event currently being processed for the player is a duplicate.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the death is a duplicate and should be ignored.
     */
    public static boolean isDuplicateDeath(UUID uuid) {
        return duplicateDeaths.contains(uuid);
    }

    /**
     * Saves the player's level and EXP before dying in the arena or creative world.
     *
     * @param e The event.
     */
    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        if (isDuplicateDeath(e.getEntity().getUniqueId())) {
            return;
        }

        String world = e.getEntity().getWorld().getName();
        Player player = e.getEntity();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        aranarthPlayer.setLastKnownTeleportLocation(player.getLocation());

        if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
            aranarthPlayer.setLevelBeforeDeath(player.getLevel());
            aranarthPlayer.setExpBeforeDeath(player.getExp());
            e.getDrops().clear();
            e.setDroppedExp(0);
        } else {
            if (AranarthUtils.isWearingArmorType(player, "soulbound")) {
                e.setKeepInventory(true);
                e.getDrops().clear();
                e.setDroppedExp(0);
                aranarthPlayer.setLevelBeforeDeath(player.getLevel());
                aranarthPlayer.setExpBeforeDeath(player.getExp());
            }
        }
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

        Avatar avatar = AvatarUtils.getCurrentAvatar();
        if (avatar != null) {
            if (avatar.getUuid().equals(player.getUniqueId())) {
                AvatarUtils.removeCurrentAvatar();
            }
        }
    }

    /**
     * Handles logic on the player's actual respawn.
     *
     * @param e The event.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        deadPlayers.remove(e.getPlayer().getUniqueId());

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(e.getPlayer().getUniqueId());
        Location deathLocation = aranarthPlayer.getLastKnownTeleportLocation();
        String world = (deathLocation != null && deathLocation.getWorld() != null)
                ? deathLocation.getWorld().getName()
                : e.getPlayer().getWorld().getName();
        if (world.equalsIgnoreCase("arena") || world.equalsIgnoreCase("creative")) {
            new RespawnNonSurvival().execute(e);
        } else {
            new RespawnSurvival().execute(e);
        }
    }

    /**
     * Stops tracking a player as dead if they leave before respawning.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        deadPlayers.remove(e.getPlayer().getUniqueId());
        duplicateDeaths.remove(e.getPlayer().getUniqueId());
    }
}
