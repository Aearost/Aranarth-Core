package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.network.CrossServerTpContext;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Allows the player to accept a pending teleport request.
 */
public class CommandTpAccept implements CommandExecutor {

    /**
     * Clears the pending teleport requests that the player has.
     *
     * @param player1 The first player.
     * @param player2 The second player.
     */
    private static void clearTeleportRequests(Player player1, Player player2) {
        AranarthPlayer aranarthPlayer1 = AranarthUtils.getPlayer(player1.getUniqueId());
        AranarthPlayer aranarthPlayer2 = AranarthUtils.getPlayer(player2.getUniqueId());
        aranarthPlayer1.setTeleportToUuid(null);
        aranarthPlayer1.setTeleportFromUuid(null);
        aranarthPlayer2.setTeleportToUuid(null);
        aranarthPlayer2.setTeleportFromUuid(null);
        AranarthUtils.setPlayer(player1.getUniqueId(), aranarthPlayer1);
        AranarthUtils.setPlayer(player2.getUniqueId(), aranarthPlayer2);
    }

    /**
     * @param sender  The user that entered the command.
     * @param command The command itself.
     * @param alias   The alias of the command.
     * @param args    The arguments of the command.
     * @return Confirmation of whether the command was a success or not.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            // Player is accepting somebody's /tphere request
            if (aranarthPlayer.getTeleportToUuid() != null) {
                Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportToUuid());
                String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportToUuid()));
                // If both players are still online
                if (target != null) {
                    String destinationWorld = target.getLocation().getWorld().getName();
                    if (AranarthUtils.isSmpWorld(destinationWorld) || destinationWorld.equals("creative")) {
                        if (!AranarthUtils.isOriginalPlayer(player.getUniqueId())) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("access.smp_restricted")));
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.target_smp_restricted", "player", aranarthPlayer.getNickname())));
                            clearTeleportRequests(player, target);
                            return true;
                        }
                    }

                    target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.request_accepted_notify", "player", aranarthPlayer.getNickname())));
                    AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(), aranarthPlayer.isInAdminMode(), targetNickname, "&7You have teleported to " + targetNickname, success -> {
                        if (success) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.teleported_to", "player", targetNickname)));
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.teleported_to_you", "player", aranarthPlayer.getNickname())));
                            int tpVol = AranarthUtils.getPlayer(target.getUniqueId()).getTeleportSoundVolume();
							if (tpVol > 0) {
								target.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
							}
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_player", "player", targetNickname)));
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_tp_to_you", "player", aranarthPlayer.getNickname())));
                        }
                        clearTeleportRequests(player, target);
                    });
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.no_longer_online", "player", targetNickname)));
                    clearTeleportRequests(player, target);
                }
                return true;
            }
            // Player is accepting somebody's /tp request
            else if (aranarthPlayer.getTeleportFromUuid() != null) {
                Player target = Bukkit.getPlayer(aranarthPlayer.getTeleportFromUuid());
                String targetNickname = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(aranarthPlayer.getTeleportFromUuid()));
                // If both players are still online
                if (target != null) {
                    AranarthPlayer targetPlayer = AranarthUtils.getPlayer(target.getUniqueId());
                    String destinationWorld = player.getLocation().getWorld().getName();
                    if (AranarthUtils.isSmpWorld(destinationWorld) || destinationWorld.equals("creative")) {
                        if (!AranarthUtils.isOriginalPlayer(target.getUniqueId())) {
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("access.smp_restricted")));
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.target_smp_restricted", "player", targetPlayer.getNickname())));
                            clearTeleportRequests(player, target);
                            return true;
                        }
                    }

                    player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.accepted_request", "player", targetPlayer.getNickname())));
                    target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.request_accepted_notify", "player", aranarthPlayer.getNickname())));
                    // Cancel any stale teleport task the acceptor (Player B) might have so that
                    // their movement does not falsely trigger a "cannot move" cancel on the
                    // requester's (Player A's) countdown that is about to start.
                    BukkitTask staleTask = AranarthUtils.getTeleportTask(player.getUniqueId());
                    if (staleTask != null) {
                        staleTask.cancel();
                        AranarthUtils.removeTeleportTask(player.getUniqueId());
                    }
                    AranarthUtils.teleportPlayer(target, target.getLocation(), player.getLocation(), targetPlayer.isInAdminMode(), aranarthPlayer.getNickname(), "&7You have teleported to " + aranarthPlayer.getNickname(), success -> {
                        if (success) {
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.teleported_to", "player", aranarthPlayer.getNickname())));
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.teleported_to_you", "player", targetPlayer.getNickname())));
                            int tpVol = AranarthUtils.getPlayer(player.getUniqueId()).getTeleportSoundVolume();
							if (tpVol > 0) {
								player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, tpVol / 100f, 0.9F);
							}
                        } else {
                            target.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_player", "player", aranarthPlayer.getNickname())));
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.could_not_tp_to_you", "player", targetPlayer.getNickname())));
                        }
                        clearTeleportRequests(player, target);
                    });
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.no_longer_online", "player", targetNickname)));
                    aranarthPlayer.setTeleportFromUuid(null);
                }
                return true;
            } else if (NetworkManager.isActive()) {
                // Check for a cross-server request received from another server
                CrossServerTpContext ctx = NetworkManager.getInstance().getCrossServerTpContext(player.getUniqueId());
                if (ctx != null) {
                    NetworkManager.getInstance().clearCrossServerTpContext(player.getUniqueId());
                    String localNickname = aranarthPlayer.getNickname().isEmpty() ? player.getName() : aranarthPlayer.getNickname();
                    // Clear the mirrored UUID so /tpaccept can't double-fire
                    aranarthPlayer.setTeleportFromUuid(null);
                    aranarthPlayer.setTeleportToUuid(null);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

                    player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.accepted_request", "player", ctx.remotePlayerNickname())));
                    NetworkManager.getInstance().publishTpAccepted(
                            player.getUniqueId(), localNickname,
                            ctx.remotePlayerUuid(), ctx.isTpHere());
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.no_pending_request")));
                }
                return true;
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.no_pending_request")));
                return true;
            }
        } else {
            sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
            return true;
        }
    }

}
