package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;

public class PlayerTeleportBetweenWorldsListener implements Listener {

    public PlayerTeleportBetweenWorldsListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Ensures that a player's inventory is updated when teleported manually via /tp.
     *
     * @param e The event.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == TeleportCause.COMMAND) {
            Player player = e.getPlayer();
            String currentWorld = e.getFrom().getWorld().getName();
            String destinationWorld = e.getTo().getWorld().getName();
            if (!currentWorld.equals(destinationWorld)) {
                GameMode newMode = GameMode.SURVIVAL;
                if (destinationWorld.startsWith("creative")) {
                    newMode = GameMode.CREATIVE;
                }
                // Shops world uses the survival game mode
                if (destinationWorld.equalsIgnoreCase("shops")) {
                    newMode = GameMode.SURVIVAL;
                }

                try {
                    AranarthUtils.switchInventory(player, currentWorld, destinationWorld);
                    player.setGameMode(newMode);
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                } catch (IOException exception) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.error")));
                    e.setCancelled(true);
                }
            } else {
                try {
                    AranarthUtils.switchInventory(player, currentWorld, currentWorld);
                } catch (IOException exception) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.error")));
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Prevents teleports that do not switch the player's inventory from moving the player between worlds that use different inventories.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCrossInventoryTeleport(PlayerTeleportEvent e) {
        // Command teleports are handled above
        if (e.getCause() == TeleportCause.COMMAND) {
            return;
        }
        // AranarthCore's own teleports switch the inventory themselves, but other plugins' teleports
        // (i.e mcMMO party teleport) never do, so only ours may cross inventory groups
        if (e.getCause() == TeleportCause.PLUGIN && isCalledByAranarthCore()) {
            return;
        }
        if (e.getTo() == null || e.getTo().getWorld() == null || e.getFrom().getWorld() == null) {
            return;
        }
        String fromGroup = AranarthUtils.getInventoryGroup(e.getFrom().getWorld().getName());
        String toGroup = AranarthUtils.getInventoryGroup(e.getTo().getWorld().getName());
        if (!fromGroup.equals(toGroup)) {
            e.setCancelled(true);
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Inv] Blocked " + e.getCause() + " teleport for "
                    + e.getPlayer().getName() + " from " + e.getFrom().getWorld().getName() + " to " + e.getTo().getWorld().getName());
        }
    }

    /**
     * Provides whether the current teleport was started by AranarthCore's own code.
     */
    private boolean isCalledByAranarthCore() {
        String ownPackage = AranarthCore.class.getPackageName();
        String thisClass = PlayerTeleportBetweenWorldsListener.class.getName();
        return StackWalker.getInstance().walk(frames -> frames
                .map(StackWalker.StackFrame::getClassName)
                .anyMatch(name -> name.startsWith(ownPackage) && !name.equals(thisClass)));
    }

    /**
     * Removes creative mode from a player who ends up outside of a creative world without it being changed.
     *
     * @param e The event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        // Checked a tick later, as teleports that switch the inventory change the game mode right after the world
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getGameMode() != GameMode.CREATIVE
                        || AranarthUtils.getInventoryGroup(player.getWorld().getName()).equals("creative")) {
                    return;
                }
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (aranarthPlayer != null && aranarthPlayer.getCouncilRank() == 3) {
                    return;
                }
                player.setGameMode(GameMode.SURVIVAL);
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] " + player.getName() + " was in creative mode in "
                        + player.getWorld().getName() + " after coming from " + e.getFrom().getName() + " - set to survival mode");
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSoulboundTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == TeleportCause.SPECTATE || e.getCause() == TeleportCause.DISMOUNT) {
            return;
        }
        Player player = e.getPlayer();
        if (!AranarthUtils.isWearingArmorType(player, "soulbound")) {
            return;
        }
        int arVol = AranarthUtils.getPlayer(player.getUniqueId()).getAranarthiumSoundVolume();
        if (arVol == 0) {
            return;
        }
        Location dest = e.getTo();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f * (arVol / 100f), 0.6f);
                    player.playSound(dest, Sound.ENTITY_VEX_AMBIENT, 0.25f * (arVol / 100f), 0.5f);
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
    }
}
