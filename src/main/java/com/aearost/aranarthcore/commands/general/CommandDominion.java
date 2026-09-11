package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.*;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.utils.*;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Centralizes all functionality relating to dominions.
 */
public class CommandDominion implements CommandExecutor {

    private static final Map<UUID, Integer> pendingChunkPurchases = new HashMap<>();
    private static final Map<UUID, Map.Entry<UUID, Integer>> pendingOutpostChunkPurchases = new HashMap<>();
    private static final Map<UUID, String> pendingConfirmations = new HashMap<>();
    private static final int[] PLOT_LIMITS_BY_LEVEL = {0, 4, 12, 25, Integer.MAX_VALUE};

    /**
     * Teleports the player to their dominion's home.
     *
     * @param player The player.
     */
    private static void teleportToDominionHome(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion != null) {
            if (player.hasPermission("aranarth.dominion.home")) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                AranarthUtils.teleportPlayer(player, player.getLocation(), dominion.getDominionHome(), aranarthPlayer.isInAdminMode(), dominion.getName(), "&7You have teleported to your dominion", success -> {
                    if (success) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.teleported_to", "name", dominion.getName())));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.could_not_teleport", "name", dominion.getName())));
                    }
                });
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_permission_home")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Teleports the player to an allied dominion's home if that dominion has HOME enabled for allies.
     *
     * @param player             The player.
     * @param targetDominionName The name of the allied dominion.
     */
    private static void teleportToAllyDominionHome(Player player, String targetDominionName) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        boolean isAdmin = aranarthPlayer.isInAdminMode();

        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion == null && !isAdmin) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        Dominion target = DominionUtils.getDominions().stream().filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(targetDominionName)).findFirst().orElse(null);

        if (target == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
            return;
        }

        if (!isAdmin) {
            boolean isConquered = playerDominion.getConquered().contains(target.getLeader());
            if (!isConquered) {
                DominionRank relationRank = DominionUtils.getRelationKey(playerDominion, target);
                if (!target.getDominionPermissions().hasPermission(relationRank, DominionPermission.HOME)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_access_disabled", "name", target.getName())));
                    return;
                }
            }
        }

        AranarthUtils.teleportPlayer(player, player.getLocation(), target.getDominionHome(), isAdmin, target.getName(), "&7You have teleported to " + target.getName(), success -> {
            if (success) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.teleported_to", "name", target.getName())));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.could_not_teleport", "name", target.getName())));
            }
        });
    }

    /**
     * Teleports the player to a specific outpost home of another dominion.
     */
    private static void teleportToAllyOutpostHome(Player player, Dominion target, String outpostName) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        boolean isAdmin = aranarthPlayer.isInAdminMode();

        if (!isAdmin) {
            Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (playerDominion == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                return;
            }
            boolean isConquered = playerDominion.getConquered().contains(target.getLeader());
            if (!isConquered) {
                DominionRank relationRank = DominionUtils.getRelationKey(playerDominion, target);
                if (!target.getDominionPermissions().hasPermission(relationRank, DominionPermission.OUTPOST_HOME)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_home_access_disabled", "name", target.getName())));
                    return;
                }
            }
        }

        Outpost outpost = OutpostUtils.getDominionOutposts(target.getId()).stream()
                .filter(o -> ChatUtils.stripColorFormatting(o.getName()).equalsIgnoreCase(outpostName))
                .findFirst().orElse(null);

        if (outpost == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_outpost_named", "name", target.getName(), "outpost", outpostName)));
            return;
        }

        AranarthUtils.teleportPlayer(player, player.getLocation(), outpost.getHome(), isAdmin, outpost.getName(), "&7You have teleported to " + target.getName() + "&7's outpost", success -> {
            if (success) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.teleported_to_outpost", "name", target.getName(), "outpost", outpost.getName())));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.could_not_teleport_outpost", "name", target.getName(), "outpost", outpost.getName())));
            }
        });
    }

    /**
     * Creates a new dominion.
     *
     * @param args   The arguments of the command.
     * @param player The player that executed the command.
     */
    private static void createDominion(String[] args, Player player) {
        double dominionCost = AranarthCore.getInstance().getConfig().getDouble("economy.dominion-creation-cost", 5000.0);
        if (player.hasPermission("aranarth.dominion.create")) {
            if (args.length >= 2) {
                String dominionName = verifyDominionName(args, player);
                if (dominionName == null) {
                    return;
                }

                // Ensures the player is not in a dominion
                if (DominionUtils.getPlayerDominion(player.getUniqueId()) == null) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                    long cooldownEnd = aranarthPlayer.getConquestDisbandCooldownEnd();
                    if (cooldownEnd > System.currentTimeMillis()) {
                        long daysLeft = (cooldownEnd - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1;
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.create_cooldown_conquered", "days", String.valueOf(daysLeft))));
                        return;
                    }
                    Dominion dominionOfChunk = DominionUtils.getDominionOfChunk(player.getLocation().getChunk());
                    // Ensures the chunk is not already claimed
                    if (dominionOfChunk == null) {
                        if (aranarthPlayer.getBalance() >= dominionCost) {
                            if (isGameplayWorld(player.getWorld().getName())) {
                                if (AranarthUtils.isSpawnLocation(player.getLocation())) {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_create_here")));
                                    return;
                                }
                                if (isInEndSpawnProtectedZone(player.getLocation())) {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_create_end")));
                                    return;
                                }

                                List<UUID> members = new ArrayList<>();
                                members.add(player.getUniqueId());
                                List<UUID> allies = new ArrayList<>();
                                List<UUID> truced = new ArrayList<>();
                                List<UUID> enemies = new ArrayList<>();

                                Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
                                if (loc == null) {
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_unsafe")));
                                    return;
                                }
                                List<Chunk> chunks = new ArrayList<>();
                                chunks.add(player.getLocation().getChunk());
                                aranarthPlayer.setBalance(aranarthPlayer.getBalance() - dominionCost);
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), -dominionCost);
                                }

                                List<UUID> conquered = new ArrayList<>();

                                Map<UUID, DominionRank> memberRanks = new HashMap<>();
                                memberRanks.put(player.getUniqueId(), DominionRank.LEADER);

                                Dominion dominion = new Dominion(
                                        null, dominionName, player.getUniqueId(), members, memberRanks, allies, truced, enemies, AranarthUtils.toStoredDominionWorldName(loc.getWorld().getName()), chunks,
                                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), new ItemStack[18], 0,
                                        conquered, null,
                                        // Keep the balance at the end
                                        dominionCost);
                                dominion.setFoundedTimestamp(DominionLevelUtils.getCurrentInGameDayTotal());
                                DominionUtils.createDominion(dominion);
                                UUID newDominionId = dominion.getId();
                                PersistenceUtils.saveSingleDominionToDatabase(dominion, () -> {
                                    if (NetworkManager.isActive()) {
                                        NetworkManager.getInstance().publishDominionCreate(newDominionId);
                                    }
                                });
                                Bukkit.broadcastMessage(ChatUtils.chatMessage(Lang.get("dominion.created_broadcast", "player", AranarthUtils.getNickname(player), "name", dominionName)));
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
                                }
                                DiscordUtils.dominionMessage(dominion, AranarthUtils.getNickname(player) + " has created the Dominion of " + dominionName, Color.GREEN);
                            } else {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_be_gameplay_world")));
                            }
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_afford_create", "cost", NumberFormat.getNumberInstance().format((long) dominionCost))));
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_already_owned", "name", dominionOfChunk.getName())));
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_in")));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion create [gradient|gradientbold] <name>")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_permission_create")));
        }
    }

    /**
     * Disbands an existing dominion.
     *
     * @param dominion The dominion of the executing player, if any.
     * @param player   The player attempting to disband the dominion.
     * @param args     The full command arguments.
     */
    private static void disbandDominion(Dominion dominion, Player player, String[] args) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        // Admin path of /d disband <name>
        if (args.length >= 2 && aranarthPlayer.isInAdminMode() && aranarthPlayer.getCouncilRank() == 3) {
            String targetName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Dominion target = DominionUtils.getDominions().stream()
                    .filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(targetName))
                    .findFirst().orElse(null);
            if (target == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.named_not_found", "name", targetName)));
                return;
            }
            String key = "admin-disband:" + target.getId();
            if (key.equals(pendingConfirmations.get(player.getUniqueId()))) {
                pendingConfirmations.remove(player.getUniqueId());
                DominionUtils.disbandDominion(target);
            } else {
                pendingConfirmations.put(player.getUniqueId(), key);
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_confirm", "name", target.getName())));
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_confirm_hint", "name", targetName)));
            }
            return;
        }

        // Regular leader path
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.only_owner_disband")));
            return;
        }
        if (dominion.getConqueredRequest() != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_conquest_active")));
            return;
        }
        long oneWeekMs = 7L * 24 * 60 * 60 * 1000;
        boolean wasConquered = DominionUtils.getConquerorOfDominion(dominion) != null;
        if (wasConquered) {
            long timeElapsed = System.currentTimeMillis() - dominion.getConqueredTimestamp();
            if (timeElapsed < oneWeekMs) {
                long daysLeft = (oneWeekMs - timeElapsed) / (1000 * 60 * 60 * 24) + 1;
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_cooldown_conquered", "days", String.valueOf(daysLeft))));
                return;
            }
        }
        if ("disband".equals(pendingConfirmations.get(player.getUniqueId()))) {
            pendingConfirmations.remove(player.getUniqueId());
            if (wasConquered) {
                // Past the 7-day lock, apply a 1-week creation/join cooldown on confirmed disband
                aranarthPlayer.setConquestDisbandCooldownEnd(System.currentTimeMillis() + oneWeekMs);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            }
            // Must update the relations between Dominions, and then will disband
            DominionUtils.updateDominionLeader(dominion, null, true);
        } else {
            pendingConfirmations.put(player.getUniqueId(), "disband");
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_self_confirm")));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.disband_self_confirm_hint")));
        }
    }

    /**
     * Provides the dominion name of the input name in the command arguments.
     *
     * @param args   The arguments of the command.
     * @param player The player who executed the command.
     */
    private static void getDominionWho(String[] args, Player player) {
        if (args.length == 1) {
            Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (dominion != null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.you_are_in", "name", dominion.getName())));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            }
            return;
        }

        if (!args[1].isEmpty()) {
            UUID uuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
            if (uuid == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                return;
            }

            if (uuid.equals(player.getUniqueId())) {
                Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                if (dominion != null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.you_are_in", "name", dominion.getName())));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                }
                return;
            }

            if (uuid != null) {
                Dominion searchedPlayerDominion = DominionUtils.getPlayerDominion(uuid);
                if (searchedPlayerDominion != null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.player_is_in", "player", AranarthUtils.getPlayer(uuid).getNickname(), "name", searchedPlayerDominion.getName())));
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.player_not_in", "player", AranarthUtils.getPlayer(uuid).getNickname())));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion who <player>")));
        }
    }

    /**
     * Updates the home of the dominion to the player's current location.
     *
     * @param dominion The dominion of the player.
     * @param player   The player executing the command.
     */
    private static void updateDominionHome(Dominion dominion, Player player) {
        List<Chunk> chunks = dominion.getChunks();
        if (chunks.contains(player.getLocation().getChunk())) {
            Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
            if (loc == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_unsafe")));
                return;
            }
            dominion.setDominionHome(loc);
            DominionUtils.updateDominion(dominion);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_updated")));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.5F);
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_be_in_land")));
        }
    }

    /**
     * Adds the input player to the dominion.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void invitePlayerToDominion(String[] args, Dominion dominion, Player player) {
        if (args.length == 1) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion invite <player>")));
            return;
        } else {
            if (dominion == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                return;
            }

            if (DominionUtils.hasPermission(player, dominion, DominionPermission.INVITE)) {
                UUID inputUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
                if (inputUuid == null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                    return;
                }
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(inputUuid);
                Dominion inputDominion = DominionUtils.getPlayerDominion(inputUuid);

                // If the player is not already in a Dominion
                if (inputDominion == null) {
                    aranarthPlayer.setPendingDominion(dominion);
                    AranarthUtils.setPlayer(inputUuid, aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.invite_sent", "player", aranarthPlayer.getNickname())));
                    Player invitedPlayer = Bukkit.getPlayer(inputUuid);
                    if (invitedPlayer != null) {
                        if (invitedPlayer.isOnline()) {
                            invitedPlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.invite_received", "name", dominion.getName())));
                            invitedPlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.invite_hint")));
                        }
                    }
                } else {
                    if (inputDominion.isSameDominion(dominion)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_member", "player", aranarthPlayer.getNickname())));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_in_other", "player", aranarthPlayer.getNickname(), "name", inputDominion.getName())));
                    }
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.only_leader")));
            }
        }
    }

    /**
     * Allows the player to accept a pending Dominion invitation.
     *
     * @param player The player executing the command.
     */
    private static void acceptDominionInvite(Player player) {
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        // Trying to join a Dominion when already in one
        if (playerDominion != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_leave_first")));
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        long cooldownEnd = aranarthPlayer.getConquestDisbandCooldownEnd();
        if (cooldownEnd > System.currentTimeMillis()) {
            long daysLeft = (cooldownEnd - System.currentTimeMillis()) / (1000 * 60 * 60 * 24) + 1;
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.join_cooldown_conquered", "days", String.valueOf(daysLeft))));
            return;
        }
        Dominion dominion = aranarthPlayer.getPendingDominion();
        // Always clear the pending invite so stale accepts can't resurrect disbanded dominions
        aranarthPlayer.setPendingDominion(null);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        // If the player has a pending Dominion invitation
        if (dominion != null) {
            // Guard against stale invites where the dominion was disbanded before acceptance
            if (DominionUtils.getDominionById(dominion.getId()) == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_longer_exists")));
                return;
            }
            dominion.getMembers().add(player.getUniqueId());
            dominion.setMemberRank(player.getUniqueId(), DominionRank.NEWCOMER);
            DominionUtils.updateDominion(dominion);
            DominionLevelUtils.reevaluateDominion(dominion);

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                    onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.member_joined", "player", aranarthPlayer.getNickname())));
                    onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1.2F);
                }
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_invitation")));
        }
    }

    /**
     * Forces a farmland recount for a dominion. Op-only.
     * Usage: /d rescan [dominion name]
     */
    private static void rescanDominion(String[] args, Player player) {
        if (!player.isOp()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
            return;
        }
        Dominion target;
        if (args.length >= 2) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            target = DominionUtils.getDominions().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
            if (target == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.named_not_found", "name", name)));
                return;
            }
        } else {
            target = DominionUtils.getPlayerDominion(player.getUniqueId());
            if (target == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                return;
            }
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rescan_start", "name", target.getName())));
        DominionLevelUtils.rescanFarmland(target, count ->
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rescan_complete", "count", String.valueOf(count), "name", target.getName()))));
    }

    /**
     * Dispatches /d plot sub-commands.
     */
    private static void handlePlot(String[] args, Dominion dominion, Player player) {
        if (args.length < 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot <create|claim|rename|add|remove>")));
            return;
        }

        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        if (!DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_PLOTS)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_permission_plots")));
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "create" -> plotCreate(args, dominion, player);
            case "claim" -> plotClaim(args, dominion, player);
            case "rename" -> plotRename(args, dominion, player);
            case "add" -> plotAdd(args, dominion, player);
            case "remove" -> plotRemove(args, dominion, player);
            default -> player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot <create|claim|rename|add|remove>")));
        }
    }

    private static void plotCreate(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot create <name>")));
            return;
        }

        int level = dominion.getDominionLevel();
        int maxPlots = PLOT_LIMITS_BY_LEVEL[level - 1];
        if (maxPlots == 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_level_required")));
            return;
        }
        int currentPlots = dominion.getPlotMembers().size();
        if (maxPlots != Integer.MAX_VALUE && currentPlots >= maxPlots) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_limit_reached", "limit", String.valueOf(maxPlots), "level", String.valueOf(level))));
            return;
        }

        String plotName = args[2];
        if (!plotName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_name_invalid")));
            return;
        }

        Chunk currentChunk = player.getLocation().getChunk();
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(currentChunk);
        if (chunkDominion == null || !chunkDominion.isSameDominion(dominion)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_stand_in_chunk")));
            return;
        }

        String chunkKey = currentChunk.getWorld().getName() + ":" + currentChunk.getX() + ":" + currentChunk.getZ();
        String existingPlot = dominion.getPlotChunkNames().get(chunkKey);
        if (existingPlot != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_already_in_plot", "plot", existingPlot)));
            return;
        }

        if (dominion.getPlotMembers().containsKey(plotName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_already_exists", "plot", plotName)));
            return;
        }

        dominion.getPlotChunkNames().put(chunkKey, plotName);
        dominion.getPlotMembers().put(plotName, new HashSet<>());
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_created", "plot", plotName)));
    }

    private static void plotClaim(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot claim <name>")));
            return;
        }

        String plotName = args[2];
        if (!dominion.getPlotMembers().containsKey(plotName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_not_found", "plot", plotName)));
            return;
        }

        Chunk currentChunk = player.getLocation().getChunk();
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(currentChunk);
        if (chunkDominion == null || !chunkDominion.isSameDominion(dominion)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_stand_in_chunk")));
            return;
        }

        String chunkKey = currentChunk.getWorld().getName() + ":" + currentChunk.getX() + ":" + currentChunk.getZ();
        String existingPlot = dominion.getPlotChunkNames().get(chunkKey);
        if (existingPlot != null) {
            if (existingPlot.equals(plotName)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_already_in_plot", "plot", plotName)));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_already_in_plot", "plot", existingPlot)));
            }
            return;
        }

        dominion.getPlotChunkNames().put(chunkKey, plotName);
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_chunk_added", "plot", plotName)));
    }

    private static void plotRename(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot rename <new name>")));
            return;
        }

        Chunk currentChunk = player.getLocation().getChunk();
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(currentChunk);
        if (chunkDominion == null || !chunkDominion.isSameDominion(dominion)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_stand_in_chunk")));
            return;
        }

        String chunkKey = currentChunk.getWorld().getName() + ":" + currentChunk.getX() + ":" + currentChunk.getZ();
        String plotName = dominion.getPlotChunkNames().get(chunkKey);
        if (plotName == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_plot")));
            return;
        }

        String newName = args[2];
        if (!newName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_name_invalid")));
            return;
        }
        if (dominion.getPlotMembers().containsKey(newName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_already_exists", "plot", newName)));
            return;
        }

        dominion.getPlotChunkNames().replaceAll((k, v) -> v.equals(plotName) ? newName : v);
        Set<UUID> members = dominion.getPlotMembers().remove(plotName);
        dominion.getPlotMembers().put(newName, members != null ? members : new HashSet<>());
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_renamed", "plot", plotName, "name", newName)));
    }

    private static void plotAdd(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot add <player>")));
            return;
        }

        Chunk currentChunk = player.getLocation().getChunk();
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(currentChunk);
        if (chunkDominion == null || !chunkDominion.isSameDominion(dominion)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_stand_in_chunk")));
            return;
        }

        String chunkKey = currentChunk.getWorld().getName() + ":" + currentChunk.getX() + ":" + currentChunk.getZ();
        String plotName = dominion.getPlotChunkNames().get(chunkKey);
        if (plotName == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_plot")));
            return;
        }

        UUID inputUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[2]);
        if (inputUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[2])));
            return;
        }

        AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(inputUuid);
        if (!dominion.getMembers().contains(inputUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.player_not_member", "player", targetAranarthPlayer.getNickname())));
            return;
        }

        Set<UUID> members = dominion.getPlotMembers().computeIfAbsent(plotName, k -> new HashSet<>());
        if (members.contains(inputUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_already_member", "player", targetAranarthPlayer.getNickname(), "plot", plotName)));
            return;
        }
        members.add(inputUuid);
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_member_added", "player", targetAranarthPlayer.getNickname(), "plot", plotName)));
        Player targetPlayer = Bukkit.getPlayer(inputUuid);
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_you_were_added", "plot", plotName, "name", dominion.getName())));
        }
    }

    private static void plotRemove(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "d plot remove <player>")));
            return;
        }

        Chunk currentChunk = player.getLocation().getChunk();
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(currentChunk);
        if (chunkDominion == null || !chunkDominion.isSameDominion(dominion)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_stand_in_chunk")));
            return;
        }

        String chunkKey = currentChunk.getWorld().getName() + ":" + currentChunk.getX() + ":" + currentChunk.getZ();
        String plotName = dominion.getPlotChunkNames().get(chunkKey);
        if (plotName == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_plot")));
            return;
        }

        UUID inputUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[2]);
        if (inputUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[2])));
            return;
        }

        AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(inputUuid);
        Set<UUID> members = dominion.getPlotMembers().getOrDefault(plotName, new HashSet<>());
        if (!members.contains(inputUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_not_member", "player", targetAranarthPlayer.getNickname(), "plot", plotName)));
            return;
        }
        members.remove(inputUuid);
        DominionUtils.updateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.plot_member_removed", "player", targetAranarthPlayer.getNickname(), "plot", plotName)));
    }

    private static void leaveDominion(Dominion dominion, Player player) {
        if (dominion != null) {
            if (dominion.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_leave_own")));
                return;
            }

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            DominionUtils.removePlayerFromDominion(player.getUniqueId());
            dominion.getMembers().remove(player.getUniqueId());
            dominion.getMemberRanks().remove(player.getUniqueId());
            dominion.getPlotMembers().values().forEach(members -> members.remove(player.getUniqueId()));
            DominionUtils.updateDominion(dominion);
            DominionLevelUtils.reevaluateDominion(dominion);
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                    onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.member_left", "player", aranarthPlayer.getNickname())));
                }
            }
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.you_left", "name", dominion.getName())));
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Removes the specified Player from the Dominion.
     *
     * @param args     The command arguments.
     * @param dominion The Dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void removePlayer(String[] args, Dominion dominion, Player player) {
        if (args.length == 1) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion remove <player>")));
            return;
        }

        if (dominion != null) {
            if (DominionUtils.hasPermission(player, dominion, DominionPermission.REMOVE_MEMBER)) {
                if (player.getName().equalsIgnoreCase(args[1])) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_remove_self")));
                    return;
                }

                UUID inputUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
                if (inputUuid != null) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(inputUuid);
                    if (!dominion.getMembers().contains(inputUuid)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.player_not_member", "player", aranarthPlayer.getNickname())));
                        return;
                    }

                    DominionUtils.removePlayerFromDominion(inputUuid);
                    dominion.getMembers().remove(inputUuid);
                    dominion.getMemberRanks().remove(inputUuid);
                    dominion.getPlotMembers().values().forEach(members -> members.remove(inputUuid));
                    DominionUtils.updateDominion(dominion);
                    DominionLevelUtils.reevaluateDominion(dominion);
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                            onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.member_removed_broadcast", "player", aranarthPlayer.getNickname())));
                        }
                    }

                    OfflinePlayer removedPlayer = Bukkit.getOfflinePlayer(inputUuid);
                    if (removedPlayer.isOnline()) {
                        removedPlayer.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.get("dominion.you_were_removed", "name", dominion.getName())));
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_permission_remove")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Handles sending or accepting an alliance request with another Dominion.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void allyDominion(String[] args, Dominion dominion, Player player) {
        if (dominion != null) {
            StringBuilder dominionNameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                dominionNameBuilder.append(args[i]);
                if (i < args.length - 1) {
                    dominionNameBuilder.append(" ");
                }
            }

            List<Dominion> dominions = DominionUtils.getDominions();
            boolean wasDominionFound = false;
            for (Dominion dominionFromList : dominions) {
                if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                    if (dominion.isSameDominion(dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.ally_self")));
                        return;
                    }

                    if (isInConquestRelation(dominion, dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.relation_conquest_locked", "name", dominionFromList.getName())));
                        return;
                    }

                    wasDominionFound = true;
                    if (dominion.getLeader().equals(player.getUniqueId())) {
                        if (dominionFromList.getAllianceRequests().contains(dominion.getLeader())) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.ally_request_already_sent", "name", dominionFromList.getName())));
                            return;
                        }

                        boolean wasAllied = dominion.isAllied(dominionFromList);
                        boolean wasTruced = dominion.isTruced(dominionFromList);
                        boolean wasEnemied = dominion.isEnemied(dominionFromList);

                        if (wasAllied) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_allied", "name", dominionFromList.getName())));
                            return;
                        }

                        // If accepting a request for an alliance
                        if (dominion.getAllianceRequests().contains(dominionFromList.getLeader())) {
                            resetDominionRelations(dominion, dominionFromList);

                            dominion.getAllied().add(dominionFromList.getLeader());
                            dominionFromList.getAllied().add(dominion.getLeader());
                            DominionUtils.updateDominion(dominion);
                            DominionUtils.updateDominion(dominionFromList);

                            String allyMsg = ChatUtils.chatMessage(Lang.get("dominion.now_allied", "name", dominion.getName(), "other", dominionFromList.getName()));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(allyMsg);
                            }
                            Bukkit.getConsoleSender().sendMessage(allyMsg);
                            DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " is now allied with " + dominionFromList.getName(), new Color(170, 0, 170));
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishBroadcast(allyMsg);
                                NetworkManager.getInstance().publishDominionRelationUpdate(dominion.getId(), dominionFromList.getId(), "ally");
                            }
                        }
                        // If sending a new request for an alliance
                        else {
                            List<UUID> allianceRequests = dominionFromList.getAllianceRequests();
                            allianceRequests.add(dominion.getLeader());
                            dominionFromList.setAllianceRequests(allianceRequests);

                            DominionUtils.updateDominion(dominionFromList);

                            if (wasAllied) {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_allied", "name", dominionFromList.getName())));
                            } else {
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.ally_request_sent_notify", "name", dominionFromList.getName())));
                                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.ally_request_received_notify", "name", dominion.getName())));
                                    }
                                }
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishDominionDiploRequest(dominionFromList.getId(), dominion.getLeader(), "ally");
                                }
                            }
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return;
                    }
                    break;
                }
            }

            if (!wasDominionFound) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Handles sending or accepting a truce request with another Dominion.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void truceDominion(String[] args, Dominion dominion, Player player) {
        if (dominion != null) {
            StringBuilder dominionNameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                dominionNameBuilder.append(args[i]);
                if (i < args.length - 1) {
                    dominionNameBuilder.append(" ");
                }
            }

            List<Dominion> dominions = DominionUtils.getDominions();
            boolean wasDominionFound = false;
            for (Dominion dominionFromList : dominions) {
                if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                    if (dominion.isSameDominion(dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.truce_self")));
                        return;
                    }

                    if (isInConquestRelation(dominion, dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.relation_conquest_locked", "name", dominionFromList.getName())));
                        return;
                    }

                    wasDominionFound = true;
                    if (dominion.getLeader().equals(player.getUniqueId())) {
                        if (dominionFromList.getTruceRequests().contains(dominion.getLeader())) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.truce_request_already_sent", "name", dominionFromList.getName())));
                            return;
                        }

                        boolean wasAllied = dominion.isAllied(dominionFromList);
                        boolean wasTruced = dominion.isTruced(dominionFromList);
                        boolean wasEnemied = dominion.isEnemied(dominionFromList);

                        if (wasTruced) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_truced", "name", dominionFromList.getName())));
                            return;
                        }

                        // If accepting a request for a truce
                        if (dominion.getTruceRequests().contains(dominionFromList.getLeader())) {
                            resetDominionRelations(dominion, dominionFromList);

                            dominion.getTruced().add(dominionFromList.getLeader());
                            dominionFromList.getTruced().add(dominion.getLeader());
                            DominionUtils.updateDominion(dominion);
                            DominionUtils.updateDominion(dominionFromList);

                            String truceMsg = ChatUtils.chatMessage(Lang.get("dominion.now_truced", "name", dominion.getName(), "other", dominionFromList.getName()));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(truceMsg);
                            }
                            Bukkit.getConsoleSender().sendMessage(truceMsg);
                            DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " is now truced with " + dominionFromList.getName(), new Color(255, 85, 255));
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishBroadcast(truceMsg);
                                NetworkManager.getInstance().publishDominionRelationUpdate(dominion.getId(), dominionFromList.getId(), "truce");
                            }
                        }
                        // If sending a new request for a truce
                        else {
                            List<UUID> truceRequests = dominionFromList.getTruceRequests();
                            truceRequests.add(dominion.getLeader());
                            dominionFromList.setTruceRequests(truceRequests);

                            DominionUtils.updateDominion(dominionFromList);

                            if (wasTruced) {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_truced", "name", dominionFromList.getName())));
                            } else {
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.truce_request_sent_notify", "name", dominionFromList.getName())));
                                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.truce_request_received_notify", "name", dominion.getName())));
                                    }
                                }
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishDominionDiploRequest(dominionFromList.getId(), dominion.getLeader(), "truce");
                                }
                            }
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return;
                    }
                    break;
                }
            }

            if (!wasDominionFound) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Handles setting another Dominion as an enemy.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void enemyDominion(String[] args, Dominion dominion, Player player) {
        if (dominion != null) {
            StringBuilder dominionNameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                dominionNameBuilder.append(args[i]);
                if (i < args.length - 1) {
                    dominionNameBuilder.append(" ");
                }
            }

            List<Dominion> dominions = DominionUtils.getDominions();
            boolean wasDominionFound = false;
            for (Dominion dominionFromList : dominions) {
                if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                    if (dominion.isSameDominion(dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.enemy_self")));
                        return;
                    }

                    if (isInConquestRelation(dominion, dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.relation_conquest_locked", "name", dominionFromList.getName())));
                        return;
                    }

                    wasDominionFound = true;
                    if (dominion.getLeader().equals(player.getUniqueId())) {
                        // Enemy the opposing Dominion
                        if (!dominion.getEnemied().contains(dominionFromList.getLeader())) {
                            dominion.getEnemied().add(dominionFromList.getLeader());
                            dominion.getAllied().remove(dominionFromList.getLeader());
                            dominion.getTruced().remove(dominionFromList.getLeader());

                            DominionUtils.updateDominion(dominion);
                            // Only add to the other dominion if they are not already enemies
                            if (!dominionFromList.getEnemied().contains(dominion.getLeader())) {
                                dominionFromList.getEnemied().add(dominion.getLeader());
                                dominionFromList.getAllied().remove(dominion.getLeader());
                                dominionFromList.getTruced().remove(dominion.getLeader());
                                DominionUtils.updateDominion(dominionFromList);
                            }

                            String enemyMsg = ChatUtils.chatMessage(Lang.get("dominion.now_enemied", "name", dominion.getName(), "other", dominionFromList.getName()));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(enemyMsg);
                            }
                            Bukkit.getConsoleSender().sendMessage(enemyMsg);
                            DiscordUtils.dominionMessage(dominion, "The Dominion of " + dominion.getName() + " has enemied " + dominionFromList.getName(), new Color(255, 85, 85));
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishBroadcast(enemyMsg);
                                NetworkManager.getInstance().publishDominionRelationUpdate(dominion.getId(), dominionFromList.getId(), "enemy");
                            }
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_enemied", "name", dominionFromList.getName())));
                            return;
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return;
                    }
                    break;
                }
            }

            if (!wasDominionFound) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Handles setting another Dominion as neutral.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void neutralDominion(String[] args, Dominion dominion, Player player) {
        if (dominion != null) {
            StringBuilder dominionNameBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                dominionNameBuilder.append(args[i]);
                if (i < args.length - 1) {
                    dominionNameBuilder.append(" ");
                }
            }

            List<Dominion> dominions = DominionUtils.getDominions();
            boolean wasDominionFound = false;
            for (Dominion dominionFromList : dominions) {
                if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                    if (dominion.isSameDominion(dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.neutral_self")));
                        return;
                    }

                    if (isInConquestRelation(dominion, dominionFromList)) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.relation_conquest_locked", "name", dominionFromList.getName())));
                        return;
                    }

                    wasDominionFound = true;
                    if (dominion.getLeader().equals(player.getUniqueId())) {
                        if (dominionFromList.getNeutralRequests().contains(dominion.getLeader())) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.neutral_request_already_sent", "name", dominionFromList.getName())));
                            return;
                        }

                        boolean wasAllied = dominion.isAllied(dominionFromList);
                        boolean wasTruced = dominion.isTruced(dominionFromList);
                        boolean wasEnemied = dominion.isEnemied(dominionFromList);

                        // If accepting a request for neutrality (only when currently enemied)
                        if (wasEnemied && dominion.getNeutralRequests().contains(dominionFromList.getLeader())) {
                            resetDominionRelations(dominion, dominionFromList);

                            String neutralMsg = ChatUtils.chatMessage(Lang.get("dominion.now_neutral", "name", dominion.getName(), "other", dominionFromList.getName()));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendMessage(neutralMsg);
                            }
                            Bukkit.getConsoleSender().sendMessage(neutralMsg);
                            DiscordUtils.dominionMessage(dominion, "The Dominions " + dominion.getName() + " and " + dominionFromList.getName() + " are now neutral", Color.WHITE);
                            if (NetworkManager.isActive()) {
                                NetworkManager.getInstance().publishBroadcast(neutralMsg);
                                NetworkManager.getInstance().publishDominionRelationUpdate(dominion.getId(), dominionFromList.getId(), "neutral");
                            }
                        }
                        // If sending a new request for neutrality
                        else {
                            if (wasEnemied) {
                                List<UUID> neutralRequests = dominionFromList.getNeutralRequests();
                                neutralRequests.add(dominion.getLeader());
                                dominionFromList.setNeutralRequests(neutralRequests);

                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.neutral_request_sent_notify", "name", dominionFromList.getName())));
                                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.neutral_request_received_notify", "name", dominion.getName())));
                                    }
                                }
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishDominionDiploRequest(dominionFromList.getId(), dominion.getLeader(), "neutral");
                                }
                            } else if (wasAllied || wasTruced) {
                                resetDominionRelations(dominion, dominionFromList);

                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.now_neutral_with", "name", dominionFromList.getName())));
                                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                                        if (domVol > 0) {
                                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 0.9F);
                                        }
                                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.now_neutral_with", "name", dominion.getName())));
                                    }
                                }
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishDominionRelationUpdate(dominion.getId(), dominionFromList.getId(), "neutral_members");
                                }
                            } else {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_neutral", "name", dominionFromList.getName())));
                                return;
                            }

                            DominionUtils.updateDominion(dominion);
                            DominionUtils.updateDominion(dominionFromList);
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return;
                    }
                    break;
                }
            }

            if (!wasDominionFound) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
        }
    }

    /**
     * Returns true if the two Dominions are involved in a conquest relationship.
     */
    private static boolean isInConquestRelation(Dominion a, Dominion b) {
        if (a.getConquered().contains(b.getLeader())) {
            return true;
        }
        if (b.getConquered().contains(a.getLeader())) {
            return true;
        }
        if (a.getConqueredRequest() != null && a.getConqueredRequest().equals(b.getLeader())) {
            return true;
        }
        if (b.getConqueredRequest() != null && b.getConqueredRequest().equals(a.getLeader())) {
            return true;
        }
        return false;
    }

    /**
     * Resets the relations between the two Dominions.
     *
     * @param dominion1 The first Dominion.
     * @param dominion2 The second Dominion.
     */
    private static void resetDominionRelations(Dominion dominion1, Dominion dominion2) {
        dominion1.getAllianceRequests().remove(dominion2.getLeader());
        dominion1.getTruceRequests().remove(dominion2.getLeader());
        dominion1.getNeutralRequests().remove(dominion2.getLeader());
        dominion1.getAllied().remove(dominion2.getLeader());
        dominion1.getTruced().remove(dominion2.getLeader());
        dominion1.getEnemied().remove(dominion2.getLeader());

        dominion2.getAllianceRequests().remove(dominion1.getLeader());
        dominion2.getTruceRequests().remove(dominion1.getLeader());
        dominion2.getNeutralRequests().remove(dominion1.getLeader());
        dominion2.getAllied().remove(dominion1.getLeader());
        dominion2.getTruced().remove(dominion1.getLeader());
        dominion2.getEnemied().remove(dominion1.getLeader());

        DominionUtils.updateDominion(dominion1);
        DominionUtils.updateDominion(dominion2);
    }

    /**
     * Handles updating the leader of the Dominion.
     *
     * @param args     The arguments of the command.
     * @param dominion The dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void setLeader(String[] args, Dominion dominion, Player player) {
        if (args.length >= 2) {
            if (dominion != null) {
                if (dominion.getLeader().equals(player.getUniqueId())) {
                    UUID uuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
                    if (uuid != null) {
                        Dominion offlinePlayerDominion = DominionUtils.getPlayerDominion(uuid);
                        if (offlinePlayerDominion != null && dominion.isSameDominion(offlinePlayerDominion)) {
                            if (!uuid.equals(dominion.getLeader())) {
                                String key = "setleader:" + uuid;
                                if (key.equals(pendingConfirmations.get(player.getUniqueId()))) {
                                    pendingConfirmations.remove(player.getUniqueId());
                                    AranarthPlayer newLeader = AranarthUtils.getPlayer(uuid);
                                    Bukkit.broadcastMessage(ChatUtils.chatMessage(Lang.get("dominion.new_leader_broadcast", "player", newLeader.getNickname(), "name", dominion.getName())));
                                    DominionUtils.updateDominionLeader(dominion, uuid, false);
                                    DiscordUtils.dominionMessage(dominion, newLeader.getNickname() + " is the new leader of " + dominion.getName(), Color.CYAN);
                                } else {
                                    pendingConfirmations.put(player.getUniqueId(), key);
                                    AranarthPlayer newLeader = AranarthUtils.getPlayer(uuid);
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setleader_confirm", "player", newLeader.getNickname())));
                                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setleader_confirm_hint", "name", args[1])));
                                }
                            } else {
                                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setleader_self")));
                            }
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setleader_not_member")));
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion setleader <username>")));
        }
    }

    /**
     * Handles all /dominion outpost subcommands.
     */
    private static void handleOutpost(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (args.length < 2) {
            GuiOutposts.open(player);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "create" -> outpostCreate(args, dominion, player);
            case "rename" -> outpostRename(args, dominion, player);
            case "sethome" -> outpostSethome(dominion, player);
            case "home" -> outpostHome(args, dominion, player);
            case "buychunks" -> outpostBuyChunks(args, dominion, player);
            case "disband" -> outpostDisband(args, dominion, player);
            default ->
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost <create|disband|rename|sethome|home|buychunks>")));
        }
    }

    /**
     * Opens the Defenders GUI.
     */
    private static void handleDefender(Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        GuiDefenders.open(player);
    }

    private static void outpostCreate(String[] args, Dominion dominion, Player player) {
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_only_leader_create")));
            return;
        }
        String dominionServer = getServerForWorld(dominion.getDominionHomeWorldName());
        String playerServer = getServerForWorld(AranarthUtils.toStoredDominionWorldName(player.getWorld().getName()));
        if (dominionServer == null || !dominionServer.equals(playerServer)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_same_server")));
            return;
        }
        if (AranarthUtils.isSpawnLocation(player.getLocation())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_cannot_create_here")));
            return;
        }
        if (isInEndSpawnProtectedZone(player.getLocation())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_cannot_create_end")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost create <name>")));
            return;
        }

        // Build outpost name from remaining args
        String[] nameArgs = Arrays.copyOfRange(args, 2, args.length);
        String outpostName = verifyOutpostName(String.join(" ", nameArgs), player);
        if (outpostName == null) {
            return;
        }

        int nextIndex = OutpostUtils.getNextOutpostIndex(dominion);
        if (nextIndex == -1) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_limit_reached", "level", String.valueOf(dominion.getDominionLevel()))));
            return;
        }

        double cost = OutpostUtils.OUTPOST_COSTS[nextIndex - 1];
        if (dominion.getBalance() < cost) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_cannot_afford", "cost", formatter.format(cost))));
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        if (DominionUtils.getDominionOfChunk(chunk) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_chunk_claimed_dominion")));
            return;
        }
        if (OutpostUtils.getOutpostOfChunk(chunk) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_chunk_claimed_outpost")));
            return;
        }

        Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
        if (loc == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_unsafe")));
            return;
        }

        dominion.setBalance(dominion.getBalance() - cost);
        PersistenceUtils.saveSingleDominionToDatabase(dominion);
        DominionUtils.updateDominion(dominion);

        List<Chunk> outpostChunks = new ArrayList<>();
        outpostChunks.add(chunk);

        Outpost outpost = new Outpost(null, outpostName, dominion.getId(), nextIndex,
                AranarthUtils.toStoredDominionWorldName(loc.getWorld().getName()), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), outpostChunks, 0, System.currentTimeMillis());
        OutpostUtils.registerOutpost(outpost);
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), PersistenceUtils::saveOutposts);

        String foundedMsg = ChatUtils.chatMessage(Lang.get("dominion.outpost_founded", "name", dominion.getName(), "outpost", outpostName));
        Bukkit.broadcastMessage(foundedMsg);
        DiscordUtils.dominionMessage(dominion, dominion.getName() + " has founded the outpost, " + outpostName, new Color(245, 197, 66));
        NetworkManager nm = NetworkManager.getInstance();
        if (nm != null) {
            nm.publishOutpostCreate(outpost);
            nm.publishDominionBalanceAdjust(dominion.getId(), -cost);
        }
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
    }

    private static void outpostRename(String[] args, Dominion dominion, Player player) {
        if (!dominion.getLeader().equals(player.getUniqueId())
                && !DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_OUTPOSTS)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_no_permission_manage")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost rename <name>")));
            return;
        }

        Outpost outpost = OutpostUtils.getOutpostPlayerIsIn(player);
        if (outpost == null || !outpost.getDominionId().equals(dominion.getId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_not_standing_rename")));
            return;
        }

        String[] nameArgs = Arrays.copyOfRange(args, 2, args.length);
        String newName = verifyOutpostName(String.join(" ", nameArgs), player);
        if (newName == null) {
            return;
        }
        String oldName = outpost.getName();
        outpost.setName(newName);
        OutpostUtils.updateOutpost(outpost);
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), PersistenceUtils::saveOutposts);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_renamed", "name", newName)));
        String renamedMsg = ChatUtils.chatMessage(Lang.get("dominion.outpost_renamed_broadcast", "dominionName", dominion.getName(), "old", oldName, "name", newName));
        Bukkit.broadcastMessage(renamedMsg);
        DiscordUtils.dominionMessage(dominion, dominion.getName() + "'s outpost, " + oldName + ", has been renamed to " + newName, new Color(135, 245, 220));
        NetworkManager nm = NetworkManager.getInstance();
        if (nm != null) {
            nm.publishOutpostUpdate(outpost);
        }
    }

    private static void outpostSethome(Dominion dominion, Player player) {
        if (!dominion.getLeader().equals(player.getUniqueId())
                && !DominionUtils.hasPermission(player, dominion, DominionPermission.MANAGE_OUTPOSTS)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_no_permission_manage")));
            return;
        }

        Outpost outpost = OutpostUtils.getOutpostPlayerIsIn(player);
        if (outpost == null || !outpost.getDominionId().equals(dominion.getId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_not_standing_sethome")));
            return;
        }

        Location loc = AranarthUtils.getSafeTeleportLocation(player.getLocation());
        if (loc == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.home_unsafe")));
            return;
        }
        outpost.setHome(loc);
        OutpostUtils.updateOutpost(outpost);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_home_updated", "name", outpost.getName())));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1F, 0.5F);
    }

    private static void outpostHome(String[] args, Dominion dominion, Player player) {
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost home <name>")));
            return;
        }

        AranarthPlayer aranarthPlayerCheck = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayerCheck != null && !aranarthPlayerCheck.isInAdminMode()
                && !dominion.getLeader().equals(player.getUniqueId())
                && !DominionUtils.hasPermission(player, dominion, DominionPermission.OUTPOST_HOME)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_no_permission_home")));
            return;
        }

        String targetName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        Outpost outpost = outposts.stream()
                .filter(o -> ChatUtils.stripColorFormatting(o.getName()).equalsIgnoreCase(targetName))
                .findFirst().orElse(null);

        if (outpost == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_not_found", "name", targetName)));
            return;
        }

        // Cross-server: if the outpost lives on a different server, transfer there first.
        if (NetworkManager.isActive()) {
            String currentServer = AranarthCore.getInstance().getConfig().getString("network.this-server", "survival");
            // Use the dominion's world name rather than the outpost's homeWorldName
            String targetServer = getServerForWorld(dominion.getDominionHomeWorldName());
            if (targetServer != null && !targetServer.equals(currentServer)) {
                AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                String cmd = "dominion outpost home " + targetName;
                AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                        ap.isInAdminMode(), "&e&lDominion", "&7Transferring to your outpost...", success -> {
                            if (success) {
                                NetworkManager.getInstance().saveInventoryAndTransfer(player, targetServer,
                                        PendingTeleport.forCommand(cmd, "&e&lDominion", "&7You have teleported to your outpost"));
                            }
                        });
                return;
            }
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        AranarthUtils.teleportPlayer(player, player.getLocation(), outpost.getHome(), aranarthPlayer.isInAdminMode(), outpost.getName(), "&7You have teleported to your outpost", success -> {
            if (success) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_teleported", "name", outpost.getName())));
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_could_not_teleport", "name", outpost.getName())));
            }
        });
    }

    private static void outpostBuyChunks(String[] args, Dominion dominion, Player player) {
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_only_leader_buychunks")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost buychunks <amount>")));
            return;
        }

        Outpost outpost = OutpostUtils.getOutpostPlayerIsIn(player);
        if (outpost == null || !outpost.getDominionId().equals(dominion.getId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_must_stand_for_buychunks")));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.amount_must_be_positive")));
            return;
        }

        int currentBought = outpost.getBoughtChunks();
        double totalCost = OutpostUtils.calculateBuyOutpostChunksCost(currentBought, amount, dominion);

        Map.Entry<UUID, Integer> pending = pendingOutpostChunkPurchases.get(player.getUniqueId());
        if (pending != null && pending.getKey().equals(outpost.getId()) && pending.getValue() == amount) {
            pendingOutpostChunkPurchases.remove(player.getUniqueId());
            if (dominion.getBalance() < totalCost) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance();
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_afford_need", "cost", formatter.format(totalCost))));
                return;
            }
            dominion.setBalance(dominion.getBalance() - totalCost);
            PersistenceUtils.saveSingleDominionToDatabase(dominion);
            outpost.setBoughtChunks(currentBought + amount);
            OutpostUtils.updateOutpost(outpost);
            DominionUtils.updateDominion(dominion);
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishDominionBalanceAdjust(dominion.getId(), -totalCost);
            }
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_chunks_purchased",
                    "amount", amount + " chunk" + (amount > 1 ? "s" : ""),
                    "outpost", outpost.getName(), "cost", formatter.format(totalCost))));
        } else {
            pendingOutpostChunkPurchases.put(player.getUniqueId(), new AbstractMap.SimpleEntry<>(outpost.getId(), amount));
            String formattedCost = String.format("%,d", Math.round(totalCost));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_chunks_confirm",
                    "amount", String.valueOf(amount), "chunks", amount + " chunk" + (amount > 1 ? "s" : ""),
                    "outpost", outpost.getName(), "cost", formattedCost)));
        }
    }

    private static void outpostDisband(String[] args, Dominion dominion, Player player) {
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_only_leader_disband")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion outpost disband <name>")));
            return;
        }

        String targetName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        Outpost outpost = outposts.stream()
                .filter(o -> ChatUtils.stripColorFormatting(o.getName()).equalsIgnoreCase(targetName))
                .findFirst().orElse(null);

        if (outpost == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_not_found", "name", targetName)));
            return;
        }

        int disbandedIndex = outpost.getOutpostIndex();
        OutpostUtils.disbandOutpost(dominion, outpost);
        OutpostUtils.shiftOutpostIndicesDown(dominion.getId(), disbandedIndex);
        DominionLevelUtils.reevaluateDominion(dominion);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_disbanded", "name", targetName)));
    }

    /**
     * Verifies and returns a sanitized outpost name from a raw string.
     * Returns null and sends an error message if invalid.
     */
    private static String verifyOutpostName(String rawName, Player player) {
        if (rawName == null || rawName.isBlank()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_name_required")));
            return null;
        }
        String cleaned = ChatUtils.removeSpecialCharacters(rawName).trim();
        if (cleaned.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_name_invalid")));
            return null;
        }
        if (ChatUtils.stripColorFormatting(cleaned).length() > 30) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.outpost_name_too_long")));
            return null;
        }
        return cleaned;
    }

    private static double calculateBuyChunksCost(int currentBought, int amount, Dominion dominion) {
        // April 2, 2026 00:00:00 UTC - 17th of Solarvor, Year 109 (founding date for ancient/legacy dominions)
        final long ancientFoundedMs = 1775088000000L;
        long foundedTimestamp = dominion.getFoundedTimestamp() == 0L ? ancientFoundedMs : dominion.getFoundedTimestamp();
        long ageMs = System.currentTimeMillis() - foundedTimestamp;
        long msPerAranarthYear = 30L * 24 * 60 * 60 * 1000;
        long threeYears = 3 * msPerAranarthYear;
        long sixYears = 6 * msPerAranarthYear;
        double multiplier;
        if (ageMs >= sixYears) {
            multiplier = 1.001;
        } else if (ageMs >= threeYears) {
            double progress = (double) (ageMs - threeYears) / threeYears;
            multiplier = 1.01 - progress * (1.01 - 1.001);
        } else {
            double progress = (double) ageMs / threeYears;
            multiplier = 1.02 - progress * (1.02 - 1.01);
        }
        double total = 0;
        for (int i = 0; i < amount; i++) {
            total += 10000 * Math.pow(multiplier, currentBought + i);
        }
        return total;
    }

    /**
     * Allows the Dominion leader to purchase extra chunks beyond the member-based limit.
     *
     * @param args     The command arguments.
     * @param dominion The player's Dominion.
     * @param player   The player executing the command.
     */
    private static void buyChunks(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.only_leader_buychunks")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion buychunks <amount>")));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
            return;
        }
        if (amount <= 0) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.amount_must_be_positive")));
            return;
        }

        int currentMax = dominion.getMaxChunks();
        if (currentMax >= 25000) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.max_chunks_reached")));
            return;
        }
        int canBuy = 25000 - currentMax;
        if (amount > canBuy) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunk_cap_limit", "limit", String.valueOf(canBuy))));
            return;
        }

        int currentBought = dominion.getBoughtChunks();
        double totalCost = calculateBuyChunksCost(currentBought, amount, dominion);

        Integer pending = pendingChunkPurchases.get(player.getUniqueId());
        if (pending != null && pending == amount) {
            // Confirmed entry, complete the purchase of the extra chunks
            pendingChunkPurchases.remove(player.getUniqueId());
            if (dominion.getBalance() < totalCost) {
                NumberFormat formatter = NumberFormat.getCurrencyInstance();
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.cannot_afford_need", "cost", formatter.format(totalCost))));
                return;
            }
            dominion.setBalance(dominion.getBalance() - totalCost);
            dominion.setBoughtChunks(currentBought + amount);
            DominionUtils.updateDominion(dominion);
            int newLimit = dominion.getMaxChunks();
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunks_purchased",
                    "amount", amount + " chunk" + (amount > 1 ? "s" : ""),
                    "cost", formatter.format(totalCost), "limit", String.valueOf(newLimit))));
        } else {
            // First entry, show confirmation prompt
            pendingChunkPurchases.put(player.getUniqueId(), amount);
            String formattedCost = String.format("%,d", Math.round(totalCost));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.chunks_confirm",
                    "amount", String.valueOf(amount), "chunks", amount + " chunk" + (amount > 1 ? "s" : ""), "cost", formattedCost)));
        }
    }

    /**
     * Displays the info for the input dominion.
     *
     * @param player   The player who executed the command.
     * @param dominion The dominion to display the info for.
     */
    private static void displayInfoForDominion(Player player, Dominion dominion) {
        player.sendMessage(ChatUtils.translateToColor("&6&l---------------------------------"));
        String dominionName = "&8&lThe Dominion of &e" + dominion.getName();

        AranarthPlayer leader = AranarthUtils.getPlayer(dominion.getLeader());
        String leaderDisplayedName = "";
        leaderDisplayedName += AranarthUtils.getSaintRank(leader);
        leaderDisplayedName += AranarthUtils.getArchitectRank(leader);
        leaderDisplayedName += AranarthUtils.getCouncilRank(leader);
        leaderDisplayedName += leader.getNickname();

        dominionName += " &8&lled by &e" + leaderDisplayedName;
        player.sendMessage(ChatUtils.translateToColor(dominionName));

        if (DominionUtils.getConquerorOfDominion(dominion) != null) {
            Dominion conqueror = DominionUtils.getPlayerDominion(DominionUtils.getConquerorOfDominion(dominion));
            player.sendMessage(ChatUtils.translateToColor("&8Ruled by the Dominion of &e" + conqueror.getName()));
        } else if (!dominion.getConquered().isEmpty()) {
            String conquered = "&8Ruling over the Dominion";
            if (dominion.getConquered().size() > 1) {
                conquered += "s";
            }
            conquered += " of ";

            for (int i = 0; i < dominion.getConquered().size(); i++) {
                Dominion conqueredDominion = DominionUtils.getPlayerDominion(dominion.getConquered().get(i));
                conquered += "&e" + conqueredDominion.getName();
                if (i < dominion.getConquered().size() - 1) {
                    conquered += "&7, ";
                }
            }
            player.sendMessage(ChatUtils.translateToColor(conquered));
        }

        String foundedDate = DominionLevelUtils.formatFoundedDate(dominion.getFoundedTimestamp());
        if (foundedDate != null) {
            player.sendMessage(ChatUtils.translateToColor("&6Founded on the &e" + foundedDate));
        }

        List<Outpost> dominionOutposts = OutpostUtils.getDominionOutposts(dominion.getId());
        StringBuilder outpostsBuilder = new StringBuilder("&6Outposts: ");
        if (dominionOutposts.isEmpty()) {
            outpostsBuilder.append("&7&oNone");
        } else {
            for (int i = 0; i < dominionOutposts.size(); i++) {
                Outpost op = dominionOutposts.get(i);
                int maxChunks = OutpostUtils.getOutpostMaxChunks(op);
                outpostsBuilder.append("&e").append(op.getName())
                        .append(" &7(").append(op.getChunkCount()).append("/").append(maxChunks).append(" chunks)");
                if (i < dominionOutposts.size() - 1) {
                    outpostsBuilder.append("&7, ");
                }
            }
        }
        player.sendMessage(ChatUtils.translateToColor(outpostsBuilder.toString()));

        StringBuilder membersBuilder = new StringBuilder();
        membersBuilder.append("&7Members: &e");
        // If the only member is the ruler
        if (dominion.getMembers().size() == 1) {
            membersBuilder.append("&7&oNone");
        } else {
            for (int i = 0; i < dominion.getMembers().size(); i++) {
                if (dominion.getMembers().get(i).equals(dominion.getLeader())) {
                    continue;
                }

                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(dominion.getMembers().get(i));
                // Fixes issue where old players don't have entries and haven't joined since
                if (aranarthPlayer == null) {
                    continue;
                }
                String displayedName = "";
                displayedName += AranarthUtils.getSaintRank(aranarthPlayer);
                displayedName += AranarthUtils.getArchitectRank(aranarthPlayer);
                displayedName += AranarthUtils.getCouncilRank(aranarthPlayer);
                displayedName += aranarthPlayer.getNickname();

                membersBuilder.append(displayedName);
                if (i < dominion.getMembers().size() - 1) {
                    membersBuilder.append("&e, ");
                }
            }
        }
        player.sendMessage(ChatUtils.translateToColor(membersBuilder.toString()));

        StringBuilder alliesBuilder = new StringBuilder();
        alliesBuilder.append("&7Allies: ");
        if (dominion.getAllied().isEmpty()) {
            alliesBuilder.append("&7&oNone");
        } else {
            for (int i = 0; i < dominion.getAllied().size(); i++) {
                UUID uuid = dominion.getAllied().get(i);
                Dominion alliedDominion = DominionUtils.getPlayerDominion(uuid);
                if (dominion.isAllied(alliedDominion)) {
                    alliesBuilder.append("&5").append(alliedDominion.getName());
                    if (i < dominion.getAllied().size() - 1) {
                        alliesBuilder.append("&5, ");
                    }
                }
            }
        }
        player.sendMessage(ChatUtils.translateToColor(alliesBuilder.toString()));

        StringBuilder trucedBuilder = new StringBuilder();
        trucedBuilder.append("&7Truced: ");
        if (dominion.getTruced().isEmpty()) {
            trucedBuilder.append("&7&oNone");
        } else {
            for (int i = 0; i < dominion.getTruced().size(); i++) {
                UUID uuid = dominion.getTruced().get(i);
                Dominion trucedDominion = DominionUtils.getPlayerDominion(uuid);
                if (dominion.isTruced(trucedDominion)) {
                    trucedBuilder.append("&d").append(trucedDominion.getName());
                    if (i < dominion.getTruced().size() - 1) {
                        trucedBuilder.append("&d, ");
                    }
                }
            }
        }
        player.sendMessage(ChatUtils.translateToColor(trucedBuilder.toString()));

        StringBuilder enemyBuilder = new StringBuilder();
        enemyBuilder.append("&7Enemies: ");
        // Must search all Dominions as it is based on others as well
        List<UUID> leadersEnemiedToThisDominion = new ArrayList<>();
        for (Dominion otherDominion : DominionUtils.getDominions()) {
            if (otherDominion.getEnemied().contains(dominion.getLeader())) {
                leadersEnemiedToThisDominion.add(otherDominion.getLeader());
            }
        }
        for (UUID enemied : dominion.getEnemied()) {
            if (!leadersEnemiedToThisDominion.contains(enemied)) {
                leadersEnemiedToThisDominion.add(enemied);
            }
        }

        if (leadersEnemiedToThisDominion.isEmpty()) {
            enemyBuilder.append("&7&oNone");
        } else {
            for (int i = 0; i < leadersEnemiedToThisDominion.size(); i++) {
                Dominion enemiedDominion = DominionUtils.getPlayerDominion(leadersEnemiedToThisDominion.get(i));
                enemyBuilder.append("&c").append(enemiedDominion.getName());
                if (i < dominion.getEnemied().size() - 1) {
                    enemyBuilder.append("&c, ");
                }
            }
        }
        player.sendMessage(ChatUtils.translateToColor(enemyBuilder.toString()));

        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String valueWithTwoDecimals = formatter.format(dominion.getBalance());
        player.sendMessage(ChatUtils.translateToColor("&7Balance: &6" + valueWithTwoDecimals));
        int outpostChunkTotal = OutpostUtils.getTotalOutpostChunkCount(dominion.getId());
        String outpostChunkSuffix = outpostChunkTotal > 0 ? " &8(&e+" + outpostChunkTotal + " outpost&8)" : "";
        player.sendMessage(ChatUtils.translateToColor("&7Size: &e" + dominion.getChunkCount() + "/" + dominion.getMaxChunks() + " chunks" + outpostChunkSuffix));
        player.sendMessage(ChatUtils.translateToColor("&6&l---------------------------------"));
    }

    /**
     * Deposit money from the player's balance to the Dominion.
     *
     * @param args     The command arguments.
     * @param dominion The Dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void depositToDominion(String[] args, Dominion dominion, Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (args.length >= 2) {
            double amount = 0;
            try {
                DecimalFormat df = new DecimalFormat("0.00");
                amount = Double.parseDouble(args[1]);
                String valueWithTwoDecimals = df.format(amount);
                double trimmedAmount = Double.parseDouble(valueWithTwoDecimals);
                NumberFormat formatter = NumberFormat.getCurrencyInstance();

                if (trimmedAmount == 0) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.deposit_zero")));
                    return;
                }

                if (aranarthPlayer.getBalance() >= trimmedAmount) {
                    dominion.setBalance(dominion.getBalance() + trimmedAmount);
                    DominionUtils.updateDominion(dominion);
                    PersistenceUtils.saveSingleDominionToDatabase(dominion);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() - trimmedAmount);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), -trimmedAmount);
                        NetworkManager.getInstance().publishDominionBalanceAdjust(dominion.getId(), trimmedAmount);
                    }
                    DominionLevelUtils.reevaluateDominion(dominion);

                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.deposited", "amount", formatter.format(trimmedAmount))));
                    for (UUID uuid : dominion.getMembers()) {
                        if (!uuid.equals(player.getUniqueId())) {
                            if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
                                Player member = Bukkit.getPlayer(uuid);
                                member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.deposited_notify", "player", aranarthPlayer.getNickname(), "amount", formatter.format(trimmedAmount))));
                            }
                        }
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.deposit_insufficient", "amount", formatter.format(trimmedAmount))));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion deposit <amount>")));
        }
    }

    /**
     * Withdraw money from the dominion's balance.
     *
     * @param args     The command arguments.
     * @param dominion The Dominion of the player executing the command.
     * @param player   The player executing the command.
     */
    private static void withdrawFromDominion(String[] args, Dominion dominion, Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (args.length >= 2) {
            double amount = 0;
            if (DominionUtils.hasPermission(player, dominion, DominionPermission.WITHDRAW)) {
                try {
                    DecimalFormat df = new DecimalFormat("0.00");
                    amount = Double.parseDouble(args[1]);
                    String valueWithTwoDecimals = df.format(amount);
                    double trimmedAmount = Double.parseDouble(valueWithTwoDecimals);
                    NumberFormat formatter = NumberFormat.getCurrencyInstance();

                    if (trimmedAmount == 0) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.withdraw_zero")));
                        return;
                    }

                    if (dominion.getBalance() >= trimmedAmount) {
                        dominion.setBalance(dominion.getBalance() - trimmedAmount);
                        DominionUtils.updateDominion(dominion);
                        PersistenceUtils.saveSingleDominionToDatabase(dominion);
                        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + trimmedAmount);
                        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        if (NetworkManager.isActive()) {
                            NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), trimmedAmount);
                            NetworkManager.getInstance().publishDominionBalanceAdjust(dominion.getId(), -trimmedAmount);
                        }

                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.withdrawn", "amount", formatter.format(trimmedAmount))));
                        for (UUID uuid : dominion.getMembers()) {
                            if (!uuid.equals(player.getUniqueId())) {
                                if (Bukkit.getOfflinePlayer(uuid).isOnline()) {
                                    Player member = Bukkit.getPlayer(uuid);
                                    member.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.withdrawn_notify", "player", aranarthPlayer.getNickname(), "amount", formatter.format(trimmedAmount))));
                                }
                            }
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.withdraw_insufficient", "amount", formatter.format(trimmedAmount))));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_number")));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.only_leader_withdraw")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion withdraw <amount>")));
        }
    }

    /**
     * Verifies that the input Dominion name is valid.
     *
     * @param args   The arguments of the command.
     * @param player The player executing the command.
     * @return The Dominion's name.
     */
    private static String verifyDominionName(String[] args, Player player) {
        return verifyDominionName(args, player, null);
    }

    private static String verifyDominionName(String[] args, Player player, Dominion dominionToSkip) {
        if (args.length <= 1) {
            return null;
        }
        boolean isGradient = args[1].equalsIgnoreCase("gradient") || args[1].equalsIgnoreCase("gradientbold");
        boolean isBold = args[1].equalsIgnoreCase("gradientbold");
        int nameStart = isGradient ? 2 : 1;

        if (isGradient && args.length <= 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.gradient_name_required")));
            return null;
        }

        StringBuilder parts = new StringBuilder();
        for (int i = nameStart; i < args.length; i++) {
            if (i == args.length - 1) {
                parts.append(args[i]);
            } else {
                parts.append(args[i]).append(" ");
            }
        }
        String dominionName = parts.toString();
        if (ChatUtils.stripColorFormatting(dominionName).length() > 30) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.name_too_long")));
            return null;
        }

        if (isGradient) {
            String requiredPerm = isBold ? "aranarth.chat.gradientbold" : "aranarth.chat.gradient";
            if (!player.hasPermission(requiredPerm)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_gradient_perm")));
                return null;
            }
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            if (aranarthPlayer == null || aranarthPlayer.getGradientChatColors().isEmpty()) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_gradient_colors")));
                return null;
            }
            String gradientName = ChatUtils.translateToGradient(aranarthPlayer.getGradientChatColors(), dominionName, isBold);
            if (gradientName == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.gradient_no_color_codes")));
                return null;
            }
            dominionName = gradientName;
        } else if (player.hasPermission("aranarth.chat.hex")) {
            dominionName = ChatUtils.translateToColor(dominionName);
        } else if (player.hasPermission("aranarth.chat.color")) {
            dominionName = ChatUtils.playerColorChat(dominionName);
            if (dominionName == null) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.invalid_formatting")));
                return null;
            }
        }

        dominionName = ChatUtils.removeSpecialCharacters(dominionName);

        for (Dominion dominionInList : DominionUtils.getDominions()) {
            if (dominionToSkip != null && dominionInList.getId().equals(dominionToSkip.getId())) {
                continue;
            }
            if (ChatUtils.stripColorFormatting(dominionInList.getName()).equalsIgnoreCase(ChatUtils.stripColorFormatting(dominionName))) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.name_already_used")));
                return null;
            }
        }
        return dominionName;
    }

    /**
     * Displays a map of the chunks nearby the player, highlighting Dominion Chunks.
     *
     * @param player The player executing the command.
     */
    private static void showDominionMap(Player player) {
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        Chunk playerChunk = player.getLocation().getChunk();

        // Creates base empty map
        String[][] map = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                map[i][j] = "&7[] ";
            }
        }

        // To track what Dominions are in which chunks
        Chunk[][] chunks = new Chunk[15][15];
        int topX = playerChunk.getX() - 7;
        int topZ = playerChunk.getZ() - 7;
        List<Dominion> dominionsNearby = new ArrayList<>();
        Map<Outpost, Dominion> outpostsNearby = new LinkedHashMap<>();
        boolean hasSpawnChunks = false;

        player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lDominion Map &8- - -"));
        // Iterate row by row, instead of by column
        for (int z = 0; z < 15; z++) {
            String[] line = map[z];
            StringBuilder lineBuilder = new StringBuilder();
            for (int x = 0; x < 15; x++) {
                // Verifies if the chunk is owned by a Dominion and colors it based on the relation with the Dominion
                Chunk chunk = player.getWorld().getChunkAt(topX + x, topZ + z);
                Dominion chunkDominion = DominionUtils.getDominionOfChunk(chunk);
                if (chunkDominion != null) {
                    if (!dominionsNearby.contains(chunkDominion)) {
                        dominionsNearby.add(chunkDominion);
                    }

                    if (playerDominion != null) {
                        if (playerDominion.isAllied(chunkDominion)) {
                            line[x] = "&5[] ";
                        } else if (playerDominion.isTruced(chunkDominion)) {
                            line[x] = "&d[] ";
                        } else if (playerDominion.isEnemied(chunkDominion)) {
                            line[x] = "&c[] ";
                        } else if (playerDominion.isSameDominion(chunkDominion)) {
                            line[x] = "&a[] ";
                        } else {
                            line[x] = "&f[] ";
                        }
                    } else {
                        line[x] = "&f[] ";
                    }
                } else {
                    Outpost chunkOutpost = OutpostUtils.getOutpostOfChunk(chunk);
                    if (chunkOutpost != null) {
                        Dominion outpostOwner = DominionUtils.getDominionById(chunkOutpost.getDominionId());
                        if (outpostOwner != null) {
                            outpostsNearby.putIfAbsent(chunkOutpost, outpostOwner);
                            if (playerDominion != null && playerDominion.isSameDominion(outpostOwner)) {
                                line[x] = "&b[] ";
                            } else if (playerDominion != null) {
                                if (playerDominion.isAllied(outpostOwner)) {
                                    line[x] = "&5[] ";
                                } else if (playerDominion.isTruced(outpostOwner)) {
                                    line[x] = "&d[] ";
                                } else if (playerDominion.isEnemied(outpostOwner)) {
                                    line[x] = "&c[] ";
                                } else {
                                    line[x] = "&f[] ";
                                }
                            } else {
                                line[x] = "&f[] ";
                            }
                        }
                    } else {
                        int chunkBaseX = chunk.getX() * 16;
                        int chunkBaseZ = chunk.getZ() * 16;
                        if (AranarthUtils.isSpawnLocation(new Location(player.getWorld(), chunkBaseX, player.getY(), chunkBaseZ))) {
                            hasSpawnChunks = true;
                            line[x] = "&6[] ";
                        }
                    }
                }

                // Highlights the chunk the player is currently in
                if (x == 7 && z == 7) {
                    map[7][7] = "&e[] ";
                }

                lineBuilder.append(line[x]);

                if (x == 14) {
                    lineBuilder.append("\n");
                }
            }
            player.sendMessage(ChatUtils.translateToColor(lineBuilder.toString()));
        }
        player.sendMessage(ChatUtils.translateToColor("&e[] &7- Your Location"));
        if (hasSpawnChunks) {
            player.sendMessage(ChatUtils.translateToColor("&6[] &7- Spawn"));
        }

        if (playerDominion != null && dominionsNearby.contains(playerDominion)) {
            player.sendMessage(ChatUtils.translateToColor("&a[] &7- Your Dominion"));
            dominionsNearby.remove(playerDominion);
        }

        for (Dominion nearbyDominion : dominionsNearby) {
            if (playerDominion != null) {
                if (playerDominion.isAllied(nearbyDominion)) {
                    player.sendMessage(ChatUtils.translateToColor("&5[] &7- &e" + nearbyDominion.getName()));
                } else if (playerDominion.isTruced(nearbyDominion)) {
                    player.sendMessage(ChatUtils.translateToColor("&d[] &7- &e" + nearbyDominion.getName()));
                } else if (playerDominion.isEnemied(nearbyDominion)) {
                    player.sendMessage(ChatUtils.translateToColor("&c[] &7- &e" + nearbyDominion.getName()));
                } else {
                    player.sendMessage(ChatUtils.translateToColor("&f[] &7- &e" + nearbyDominion.getName()));
                }
            } else {
                player.sendMessage(ChatUtils.translateToColor("&f[] &7- &e" + nearbyDominion.getName()));
            }
        }

        for (Map.Entry<Outpost, Dominion> entry : outpostsNearby.entrySet()) {
            Outpost outpost = entry.getKey();
            Dominion outpostOwner = entry.getValue();
            String desc = "&e" + outpost.getName() + " &7(Outpost of &e" + outpostOwner.getName() + "&7)";
            String color;
            if (playerDominion != null && playerDominion.isSameDominion(outpostOwner)) {
                color = "&b";
            } else if (playerDominion != null && playerDominion.isAllied(outpostOwner)) {
                color = "&5";
            } else if (playerDominion != null && playerDominion.isTruced(outpostOwner)) {
                color = "&d";
            } else if (playerDominion != null && playerDominion.isEnemied(outpostOwner)) {
                color = "&c";
            } else {
                color = "&f";
            }
            player.sendMessage(ChatUtils.translateToColor(color + "[] &7- " + desc));
        }
    }

    /**
     * Sets the map color used to display this dominion on /dominion map.
     * Only the dominion leader may change this.
     * Accepts named colors (e.g. "red", "blue") or hex codes (e.g. "#FF5500").
     * Use "reset" or "none" to restore the default white color.
     *
     * @param args     The command arguments (args[1] is the color).
     * @param dominion The player's dominion.
     * @param player   The player.
     */
    private static void setMapColor(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_only_leader")));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_usage")));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_colors")));
            return;
        }
        String input = args[1].toLowerCase();
        String colorCode = parseMapColor(input);
        if (colorCode == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_invalid")));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_colors")));
            return;
        }
        dominion.setMapColor(colorCode);
        DominionUtils.updateDominion(dominion);
        AranarthCore.refreshSquaremap();
        if (colorCode.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_reset")));
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.mapcolor_set", "color", colorCode + input)));
        }
    }

    /**
     * Parses a color input string into a Minecraft color code (e.g. "&a") or hex code (e.g. "&#FF5500").
     * Returns an empty string for "reset"/"none", or null if the input is invalid.
     *
     * @param input The color name or hex code (lowercase, no leading &).
     * @return The color code string, empty string to reset, or null if invalid.
     */
    private static String parseMapColor(String input) {
        return switch (input) {
            case "reset", "none", "default" -> "";
            case "white" -> "&f";
            case "black" -> "&0";
            case "dark_blue", "navy" -> "&1";
            case "dark_green" -> "&2";
            case "dark_aqua", "teal", "dark_cyan" -> "&3";
            case "dark_red", "maroon" -> "&4";
            case "dark_purple", "purple" -> "&5";
            case "gold", "orange" -> "&6";
            case "gray", "grey" -> "&7";
            case "dark_gray", "dark_grey" -> "&8";
            case "blue" -> "&9";
            case "green" -> "&a";
            case "aqua", "cyan" -> "&b";
            case "red" -> "&c";
            case "light_purple", "magenta", "pink" -> "&d";
            case "yellow" -> "&e";
            default -> {
                if (input.startsWith("#") && input.length() == 7 && input.substring(1).matches("[0-9a-fA-F]{6}")) {
                    yield "&#" + input.substring(1).toUpperCase();
                }
                yield null;
            }
        };
    }

    /**
     * Toggles whether the player will automatically be claiming chunks as they enter them.
     *
     * @param player The player.
     */
    private static void claimToggle(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isAutoClaimEnabled()) {
            aranarthPlayer.setAutoClaimEnabled(false);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.autoclaim_disabled")));
        } else {
            aranarthPlayer.setAutoClaimEnabled(true);
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.autoclaim_enabled")));
        }
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
    }

    /**
     * Opens the Dominion's food storage.
     *
     * @param player The player.
     */
    private static void foodStorage(Player player) {
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.food_survival_only")));
            return;
        }
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (!DominionUtils.hasPermission(player, playerDominion, DominionPermission.FOOD)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.food_no_permission")));
            return;
        }
        if (DominionUtils.isFoodInventoryLockedByOther(playerDominion.getId(), player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.food_in_use")));
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(0);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        DominionUtils.lockFoodInventory(playerDominion.getId(), player.getUniqueId());
        GuiDominionFood gui = new GuiDominionFood(player, 0);
        gui.openGui();
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
    }

    /**
     * Opens the Dominion's resource claim options.
     *
     * @param dominion The player's Dominion.
     * @param player   The player.
     */
    private static void resources(Dominion dominion, Player player) {
        String worldName = player.getWorld().getName();
        if (!isGameplayWorld(worldName)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_gameplay_world")));
            return;
        }
        if (dominion != null) {
            if (DominionUtils.hasPermission(player, dominion, DominionPermission.RESOURCES)) {
                if (dominion.getClaimableResources() > 0) {
                    GuiDominionResources gui = new GuiDominionResources(player);
                    gui.openGui();
                    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_none")));
                }
            } else {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.resources_no_permission")));
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.must_be_leader_resources")));
        }
    }

    /**
     * Attempts to send a conquer request to a target Dominion
     *
     * @param args     The arguments of the parameter.
     * @param dominion The player's Dominion.
     * @param player   The player.
     */
    private static void conquer(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        // Only the leader can initiate a conquest
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquer_only_leader")));
            return;
        }

        if (DominionUtils.getConquerorOfDominion(dominion) != null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquered_cannot_conquer")));
            return;
        }

        if (AranarthUtils.isSmpWorld(dominion.getDominionHomeWorldName())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.smp_no_conquest")));
            return;
        }

        // Block if there is already an active conquest attempt from this dominion
        boolean alreadyConquering = DominionUtils.getDominions().stream()
                .anyMatch(d -> d.getConqueredRequest() != null && d.getConqueredRequest().equals(dominion.getLeader()));
        if (alreadyConquering) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquest_already_active")));
            return;
        }

        // If on cooldown for conquering again
        long lastAttempt = dominion.getLastConquerAttemptTimestamp();
        if (lastAttempt > 0 && System.currentTimeMillis() - lastAttempt < DominionUtils.CONQUER_COOLDOWN_MS) {
            long remainingMs = DominionUtils.CONQUER_COOLDOWN_MS - (System.currentTimeMillis() - lastAttempt);
            long remainingHours = remainingMs / (1000 * 60 * 60);
            long remainingDays = remainingHours / 24;
            long leftoverHours = remainingHours % 24;
            String timeLeft = remainingDays > 0 ? remainingDays + "d " + leftoverHours + "h" : leftoverHours + "h";
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquer_cooldown", "time", timeLeft)));
            return;
        }

        StringBuilder dominionNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            dominionNameBuilder.append(args[i]);
            if (i < args.length - 1) {
                dominionNameBuilder.append(" ");
            }
        }

        List<Dominion> dominions = DominionUtils.getDominions();
        boolean wasDominionFound = false;
        for (Dominion dominionFromList : dominions) {
            if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                wasDominionFound = true;
                if (dominion.isSameDominion(dominionFromList)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquer_self")));
                    return;
                }
                if (!dominion.isEnemied(dominionFromList)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquer_must_be_enemy")));
                    return;
                }
                if (dominion.getConquered().contains(dominionFromList.getLeader())) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.already_conquered", "name", dominionFromList.getName())));
                    return;
                }
                if (AranarthUtils.isSmpWorld(dominionFromList.getDominionHomeWorldName())) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.smp_no_conquest")));
                    return;
                }
                UUID conquerorUuid = DominionUtils.getConquerorOfDominion(dominionFromList);
                if (conquerorUuid != null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.target_already_conquered", "name", DominionUtils.getPlayerDominion(conquerorUuid).getName())));
                    return;
                }
                if (dominionFromList.getConqueredRequest() != null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.target_conquest_request_active")));
                    return;
                }

                long now = System.currentTimeMillis();
                dominionFromList.setConqueredRequest(dominion.getLeader());
                dominionFromList.setConqueredRequestTimestamp(now);
                dominionFromList.setConqueredRequestDefenderLastSeen(now);
                DominionUtils.updateDominion(dominionFromList);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_2, 2F * (domVol / 100f), 1F);
                        }
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquer_attempt_notify", "name", dominionFromList.getName())));
                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_4, 2F * (domVol / 100f), 1F);
                        }
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.being_conquered_notify", "name", dominion.getName())));
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.conquest_auto_warning")));
                    }
                }
                if (NetworkManager.isActive()) {
                    // a=conqueror, b=defender
                    NetworkManager.getInstance().publishDominionConquestUpdate("conquer_request", dominion, dominionFromList);
                }
                break;
            }
        }

        if (!wasDominionFound) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
        }
    }

    /**
     * Attempts to surrender a Dominion to a target Dominion
     *
     * @param args     The arguments of the parameter.
     * @param dominion The player's Dominion.
     * @param player   The player.
     */
    private static void surrender(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        DominionRank rank = dominion.getMemberRank(player.getUniqueId());
        if (!dominion.getDominionPermissions().hasPermission(rank, DominionPermission.SURRENDER)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.surrender_no_permission")));
            return;
        }

        StringBuilder dominionNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            dominionNameBuilder.append(args[i]);
            if (i < args.length - 1) {
                dominionNameBuilder.append(" ");
            }
        }

        List<Dominion> dominions = DominionUtils.getDominions();
        boolean wasDominionFound = false;
        for (Dominion dominionFromList : dominions) {
            if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                wasDominionFound = true;
                if (dominion.isSameDominion(dominionFromList)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.surrender_self")));
                    return;
                }

                if (dominion.getConqueredRequest() != null && dominion.getConqueredRequest().equals(dominionFromList.getLeader())) {
                    dominion.setConqueredRequest(null);
                    dominion.setConqueredRequestTimestamp(0L);
                    dominion.setConqueredRequestDefenderLastSeen(0L);

                    List<UUID> conquered = dominionFromList.getConquered();
                    conquered.add(dominion.getLeader());
                    conquered.addAll(dominion.getConquered());
                    dominion.setConquered(new ArrayList<>());
                    dominionFromList.setConquered(conquered);
                    dominionFromList.setLastConquerAttemptTimestamp(System.currentTimeMillis());
                    dominion.setLastRebelAttemptTimestamp(System.currentTimeMillis());
                    dominion.setConqueredTimestamp(System.currentTimeMillis());

                    DominionUtils.updateDominion(dominion);
                    DominionUtils.updateDominion(dominionFromList);

                    String surrenderMsg = ChatUtils.chatMessage(dominion.getName() + " &4has been conquered by &e" + dominionFromList.getName());
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_7, domVol / 100f, 1F);
                        }
                        onlinePlayer.sendMessage(surrenderMsg);
                    }
                    DiscordUtils.dominionMessage(dominion, dominion.getName() + " has been conquered by " + dominionFromList.getName(), new Color(101, 0, 0));
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBroadcast(surrenderMsg);
                        // a=defender, b=conqueror
                        NetworkManager.getInstance().publishDominionConquestUpdate("surrender", dominion, dominionFromList);
                    }
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_conquest_to_surrender")));
                }
                break;
            }
        }

        if (!wasDominionFound) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
        }
    }

    /**
     * Attempts to rebel against the conquering Dominion.
     *
     * @param args     The arguments of the parameter.
     * @param dominion The player's Dominion.
     * @param player   The player.
     */
    private static void rebel(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        DominionRank rank = dominion.getMemberRank(player.getUniqueId());
        if (!dominion.getDominionPermissions().hasPermission(rank, DominionPermission.REBEL)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebel_no_permission")));
            return;
        }

        long lastRebel = dominion.getLastRebelAttemptTimestamp();
        if (lastRebel > 0 && System.currentTimeMillis() - lastRebel < DominionUtils.REBEL_COOLDOWN_MS) {
            long remainingMs = DominionUtils.REBEL_COOLDOWN_MS - (System.currentTimeMillis() - lastRebel);
            long remainingHours = remainingMs / (1000 * 60 * 60);
            long remainingDays = remainingHours / 24;
            long leftoverHours = remainingHours % 24;
            String timeLeft = remainingDays > 0 ? remainingDays + "d " + leftoverHours + "h" : leftoverHours + "h";
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebel_cooldown", "time", timeLeft)));
            return;
        }

        StringBuilder dominionNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            dominionNameBuilder.append(args[i]);
            if (i < args.length - 1) {
                dominionNameBuilder.append(" ");
            }
        }

        List<Dominion> dominions = DominionUtils.getDominions();
        boolean wasDominionFound = false;
        for (Dominion dominionFromList : dominions) {
            if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                wasDominionFound = true;
                if (dominion.isSameDominion(dominionFromList)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebel_self")));
                    return;
                }

                if (!dominionFromList.getConquered().contains(dominion.getLeader())) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_conquered_by", "name", dominionFromList.getName())));
                    return;
                }

                if (dominionFromList.getRebelRequest() != null) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebellion_already_active", "name", dominionFromList.getName())));
                    return;
                }

                long now = System.currentTimeMillis();
                dominionFromList.setRebelRequest(dominion.getLeader());
                dominionFromList.setRebelRequestTimestamp(now);
                dominionFromList.setRebelRequestConquerorLastSeen(now);
                DominionUtils.updateDominion(dominionFromList);

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                    if (dominion.getMembers().contains(onlinePlayer.getUniqueId())) {
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_1, 2F * (domVol / 100f), 1F);
                        }
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebel_started_notify", "name", dominionFromList.getName())));
                    } else if (dominionFromList.getMembers().contains(onlinePlayer.getUniqueId())) {
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_1, 2F * (domVol / 100f), 1F);
                        }
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebellion_received_notify", "name", dominion.getName())));
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebellion_retreat_hint", "name", ChatUtils.stripColorFormatting(dominion.getName()))));
                        onlinePlayer.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rebellion_freedom_warning")));
                    }
                }
                if (NetworkManager.isActive()) {
                    // a=rebel, b=conqueror
                    NetworkManager.getInstance().publishDominionConquestUpdate("rebel_request", dominion, dominionFromList);
                }
                break;
            }
        }

        if (!wasDominionFound) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
        }
    }

    /**
     * Attempts to retreat from the conquered Dominion.
     *
     * @param args     The arguments of the parameter.
     * @param dominion The player's Dominion.
     * @param player   The player.
     */
    private static void retreat(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        DominionRank rank = dominion.getMemberRank(player.getUniqueId());
        if (!dominion.getDominionPermissions().hasPermission(rank, DominionPermission.RETREAT)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.retreat_no_permission")));
            return;
        }

        StringBuilder dominionNameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            dominionNameBuilder.append(args[i]);
            if (i < args.length - 1) {
                dominionNameBuilder.append(" ");
            }
        }

        List<Dominion> dominions = DominionUtils.getDominions();
        boolean wasDominionFound = false;
        for (Dominion dominionFromList : dominions) {
            if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                wasDominionFound = true;
                if (dominion.isSameDominion(dominionFromList)) {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.retreat_self")));
                    return;
                }

                long now = System.currentTimeMillis();

                // Case 1: Retreating from an active conquest attempt (before surrender)
                if (dominionFromList.getConqueredRequest() != null
                        && dominionFromList.getConqueredRequest().equals(dominion.getLeader())) {
                    dominionFromList.setConqueredRequest(null);
                    dominionFromList.setConqueredRequestTimestamp(0L);
                    dominionFromList.setConqueredRequestDefenderLastSeen(0L);
                    dominion.setLastConquerAttemptTimestamp(now);
                    DominionUtils.updateDominion(dominion);
                    DominionUtils.updateDominion(dominionFromList);

                    String retreatConquestMsg = ChatUtils.chatMessage(Lang.get("dominion.retreated_from_conquering", "name", dominion.getName(), "other", dominionFromList.getName()));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 1F);
                        }
                        onlinePlayer.sendMessage(retreatConquestMsg);
                    }
                    DiscordUtils.dominionMessage(dominionFromList, dominion.getName() + " has retreated from conquering " + dominionFromList.getName(), new Color(135, 245, 220));
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBroadcast(retreatConquestMsg);
                        // a=conqueror, b=defender
                        NetworkManager.getInstance().publishDominionConquestUpdate("retreat_conquest", dominion, dominionFromList);
                    }
                    return;
                }

                // Case 2: Voluntarily releasing an already-conquered dominion
                if (dominion.getConquered().contains(dominionFromList.getLeader())) {
                    // Clear any active rebellion too
                    if (dominion.getRebelRequest() != null && dominion.getRebelRequest().equals(dominionFromList.getLeader())) {
                        dominion.setRebelRequest(null);
                        dominion.setRebelRequestTimestamp(0L);
                        dominion.setRebelRequestConquerorLastSeen(0L);
                        dominionFromList.setLastRebelAttemptTimestamp(now);
                    }
                    List<UUID> conquered = dominion.getConquered();
                    conquered.remove(dominionFromList.getLeader());
                    dominion.setConquered(conquered);
                    dominion.setLastConquerAttemptTimestamp(now);
                    DominionUtils.updateDominion(dominion);
                    DominionUtils.updateDominion(dominionFromList);

                    String retreatReleaseMsg = ChatUtils.chatMessage(Lang.get("dominion.retreated_release", "name", dominion.getName(), "other", dominionFromList.getName()));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int domVol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getDominionSoundVolume();
                        if (domVol > 0) {
                            onlinePlayer.playSound(onlinePlayer, Sound.ITEM_GOAT_HORN_SOUND_0, domVol / 100f, 1F);
                        }
                        onlinePlayer.sendMessage(retreatReleaseMsg);
                    }
                    DiscordUtils.dominionMessage(dominion, dominion.getName() + " has retreated from " + dominionFromList.getName(), new Color(135, 245, 220));
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBroadcast(retreatReleaseMsg);
                        // a=conqueror, b=released
                        NetworkManager.getInstance().publishDominionConquestUpdate("retreat_release", dominion, dominionFromList);
                    }
                    return;
                }

                player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_conquering", "name", dominionFromList.getName())));
                break;
            }
        }

        if (!wasDominionFound) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
        }
    }

    /**
     * /**
     * Displays the dominion's current level, progress toward the next level across all 6
     * criteria, and the global leaderboard placement summary.
     * Usage: /dominion rank
     *
     * @param player   The player running the command.
     * @param dominion The player's dominion.
     */
    private static void showDominionLevel(Player player, Dominion dominion) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        int currentLevel = dominion.getDominionLevel();

        // --- Header ---
        String levelTag = "&6Lvl. " + currentLevel;
        player.sendMessage(ChatUtils.translateToColor(
                "&8      - - - &e" + dominion.getName() + " &8[" + levelTag + "&8] &8- - -"));

        // --- Level progress section ---
        if (currentLevel < DominionLevelUtils.MAX_LEVEL) {
            int nextLevel = currentLevel + 1;
            boolean[] criteria = DominionLevelUtils.getCriteriaStatus(dominion, nextLevel);

            player.sendMessage(ChatUtils.translateToColor(
                    "&e" + dominion.getName() + " &7must meet &e" + DominionLevelUtils.CRITERIA_REQUIRED + "&8/&e"
                            + DominionLevelUtils.CRITERIA_COUNT + " &7criteria to rank up"));

            // Members
            int membThresh = DominionLevelUtils.getMembersThreshold(nextLevel);
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[0] ? "&a✔" : "&cX") + "&8] &7Members - &e"
                            + dominion.getMembers().size() + " &8/ &e" + membThresh));

            // Balance
            double balThresh = DominionLevelUtils.getBalanceThreshold(nextLevel);
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[1] ? "&a✔" : "&cX") + "&8] &7Balance - &6"
                            + formatter.format(dominion.getBalance()) + " &8/ &6" + formatter.format(balThresh)));

            // Farmland
            int farmThresh = DominionLevelUtils.getFarmlandThreshold(nextLevel);
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[2] ? "&a✔" : "&cX") + "&8] &7Farmland - &e"
                            + dominion.getCachedFarmlandCount() + " &8/ &e" + farmThresh + " &7blocks"));

            // Livestock
            int liveThresh = DominionLevelUtils.getLivestockThreshold(nextLevel);
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[3] ? "&a✔" : "&cX") + "&8] &7Livestock - &e"
                            + dominion.getCachedLivestockCount() + " &8/ &e" + liveThresh + " &7mobs"));

            // Chunks
            int chunkThresh = DominionLevelUtils.getChunksThreshold(nextLevel);
            int totalChunks = dominion.getChunks().size() + OutpostUtils.getTotalOutpostChunkCount(dominion.getId());
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[4] ? "&a✔" : "&cX") + "&8] &7Chunks - &e"
                            + totalChunks + " &8/ &e" + chunkThresh));

            // Age
            int ageThresh = DominionLevelUtils.getAgeThreshold(nextLevel);
            player.sendMessage(ChatUtils.translateToColor(
                    "&8[" + (criteria[5] ? "&a✔" : "&cX") + "&8] &7Age - &e"
                            + DominionLevelUtils.getFormattedAge(dominion) + " &8/ &e" + ageThresh + " &7yrs"));
        } else {
            player.sendMessage(ChatUtils.translateToColor("&a✔ Maximum dominion level reached!"));
        }
    }

    /**
     * Sets the rank of a Dominion member. Only the leader can do this.
     * Usage: /dominion setrank <player> <rank>
     *
     * @param args     The command arguments.
     * @param dominion The player's dominion.
     * @param player   The player.
     */
    private static void setMemberRank(String[] args, Dominion dominion, Player player) {
        if (dominion == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
            return;
        }
        if (!dominion.getLeader().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setrank_only_leader")));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion setrank <player> <rank>")));
            return;
        }

        UUID targetUuid = AranarthUtils.getUUIDFromUsernameOrNickname(args[1]);
        if (targetUuid == null) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("player.not_found", "name", args[1])));
            return;
        }

        if (!dominion.getMembers().contains(targetUuid)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setrank_not_member", "player", args[1])));
            return;
        }

        if (targetUuid.equals(dominion.getLeader())) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.setrank_leader_not_allowed")));
            return;
        }

        DominionRank newRank;
        try {
            newRank = DominionRank.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rank_not_found")));
            return;
        }

        if (newRank == DominionRank.LEADER) {
            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.use_setleader_instead")));
            return;
        }

        dominion.setMemberRank(targetUuid, newRank);
        DominionUtils.updateDominion(dominion);

        AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(targetUuid);
        String rankName = DominionUtils.getFormattedRankName(newRank);
        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rank_set", "player", targetAranarthPlayer.getNickname(), "rank", rankName)));

        Player targetOnline = Bukkit.getPlayer(targetUuid);
        if (targetOnline != null && targetOnline.isOnline()) {
            targetOnline.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.your_rank_changed", "name", dominion.getName(), "rank", rankName)));
        }
    }

    private static boolean isGameplayWorld(String worldName) {
        return worldName.startsWith("world") || worldName.startsWith("smp");
    }

    // Radius (in blocks) from (0, 0) in the Survival End world that cannot be claimed.
    // Covers the main End island (~60 blocks) and the surrounding void before outer islands (~1000+ blocks).
    private static final double END_SPAWN_PROTECTED_RADIUS = 1000.0;

    /**
     * Returns true if the location is within the protected End spawn zone on the Survival server.
     * The zone covers the main island and surrounding void; outer End islands beyond this radius can be claimed.
     * This restriction does not apply to the SMP server.
     */
    private static boolean isInEndSpawnProtectedZone(Location location) {
        if (AranarthCore.isSmpServer()) {
            return false;
        }
        if (!"world_the_end".equals(location.getWorld().getName())) {
            return false;
        }
        double dx = location.getX();
        double dz = location.getZ();
        return dx * dx + dz * dz <= END_SPAWN_PROTECTED_RADIUS * END_SPAWN_PROTECTED_RADIUS;
    }

    private static String getServerForWorld(String worldName) {
        if (worldName == null) {
            return null;
        }
        String survivalServer = AranarthCore.getInstance().getConfig().getString("network.servers.survival", "survival");
        String smpServer = AranarthCore.getInstance().getConfig().getString("network.servers.smp", "smp");
        // Use stored-name logic, NOT isSmpWorld(), which is server-aware and would misidentify
        // survival's "world" as SMP when running on the SMP server.
        // Stored names: "smp*" / "smp:*" = SMP; "world*" = survival.
        if (worldName.startsWith("smp")) {
            return smpServer;
        }
        if (worldName.startsWith("world")) {
            return survivalServer;
        }
        return null;
    }

    private static String getHomeTargetServer(String[] args, Dominion playerDominion) {
        if (args.length < 2) {
            if (playerDominion == null) {
                return null;
            }
            return getServerForWorld(playerDominion.getDominionHomeWorldName());
        }
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        Dominion targetDominion = null;
        String outpostPart = null;
        for (int split = remaining.length; split >= 1; split--) {
            String dominionPart = String.join(" ", Arrays.copyOfRange(remaining, 0, split));
            Dominion match = DominionUtils.getDominions().stream()
                    .filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(dominionPart))
                    .findFirst().orElse(null);
            if (match != null) {
                targetDominion = match;
                if (split < remaining.length) {
                    outpostPart = String.join(" ", Arrays.copyOfRange(remaining, split, remaining.length));
                }
                break;
            }
        }
        if (targetDominion == null) {
            return null;
        }
        if (outpostPart != null) {
            String finalOutpostPart = outpostPart;
            Outpost outpost = OutpostUtils.getDominionOutposts(targetDominion.getId()).stream()
                    .filter(o -> ChatUtils.stripColorFormatting(o.getName()).equalsIgnoreCase(finalOutpostPart))
                    .findFirst().orElse(null);
            if (outpost != null && outpost.getHomeWorldName() != null) {
                return getServerForWorld(outpost.getHomeWorldName());
            }
        }
        return getServerForWorld(targetDominion.getDominionHomeWorldName());
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
        // No-args: open the Dominion Hub GUI
        if (args.length == 0) {
            if (sender instanceof Player player) {
                Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                if (dominion != null) {
                    new GuiDominionPermissions(player).openGui();
                    player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                }
                return true;
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
                return false;
            }
        } else {
            if (sender instanceof Player player) {
                Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());

                if (args[0].equalsIgnoreCase("create")) {
                    if (!player.hasPermission("aranarth.dominion.create")) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return true;
                    }
                    createDominion(args, player);
                } else if (args[0].equalsIgnoreCase("invite")) {
                    invitePlayerToDominion(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("accept")) {
                    acceptDominionInvite(player);
                } else if (args[0].equalsIgnoreCase("leave")) {
                    leaveDominion(dominion, player);
                } else if (args[0].equalsIgnoreCase("remove")) {
                    removePlayer(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("disband")) {
                    disbandDominion(dominion, player, args);
                } else if (args[0].equalsIgnoreCase("claim")) {
                    player.sendMessage(ChatUtils.chatMessage(DominionUtils.claimChunk(player, player.getChunk())));
                } else if (args[0].equalsIgnoreCase("unclaim")) {
                    player.sendMessage(ChatUtils.chatMessage(DominionUtils.unclaimChunk(player)));
                } else if (args[0].equalsIgnoreCase("balance")) {
                    if (dominion != null) {
                        NumberFormat formatter = NumberFormat.getCurrencyInstance();
                        String valueWithTwoDecimals = formatter.format(dominion.getBalance());
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.balance_display", "name", dominion.getName(), "amount", valueWithTwoDecimals)));
                        return true;
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                        return false;
                    }
                } else if (args[0].equalsIgnoreCase("deposit")) {
                    if (dominion != null) {
                        depositToDominion(args, dominion, player);
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                    }
                } else if (args[0].equalsIgnoreCase("withdraw")) {
                    if (dominion != null) {
                        withdrawFromDominion(args, dominion, player);
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                    }
                } else if (args[0].equalsIgnoreCase("home")) {
                    // Cross-server: transfer to whichever server the target dominion/outpost lives on.
                    if (NetworkManager.isActive()) {
                        String currentServer = AranarthCore.getInstance().getConfig()
                                .getString("network.this-server", "survival");
                        String targetServer = getHomeTargetServer(args, dominion);
                        if (targetServer != null && !targetServer.equals(currentServer)) {
                            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                            String cmd = args.length >= 2
                                    ? "dominion home " + String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                                    : "dominion home";
                            AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                                    ap.isInAdminMode(), "&e&lDominion", "&7Transferring to your Dominion...", success -> {
                                        if (success) {
                                            NetworkManager.getInstance().saveInventoryAndTransfer(player, targetServer,
                                                    PendingTeleport.forCommand(cmd, "&e&lDominion", "&7You have teleported to your Dominion"));
                                        }
                                    });
                            return true;
                        }
                    }
                    if (!player.hasPermission("aranarth.dominion.home")) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        return true;
                    }
                    if (args.length >= 2) {
                        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
                        // Try longest dominion-name prefix first
                        Dominion targetDominion = null;
                        String outpostPart = null;
                        for (int split = remaining.length; split >= 1; split--) {
                            String dominionPart = String.join(" ", Arrays.copyOfRange(remaining, 0, split));
                            Dominion match = DominionUtils.getDominions().stream()
                                    .filter(d -> ChatUtils.stripColorFormatting(d.getName()).equalsIgnoreCase(dominionPart))
                                    .findFirst().orElse(null);
                            if (match != null) {
                                targetDominion = match;
                                if (split < remaining.length) {
                                    outpostPart = String.join(" ", Arrays.copyOfRange(remaining, split, remaining.length));
                                }
                                break;
                            }
                        }
                        if (targetDominion != null && outpostPart != null) {
                            teleportToAllyOutpostHome(player, targetDominion, outpostPart);
                        } else {
                            teleportToAllyDominionHome(player, String.join(" ", remaining));
                        }
                    } else {
                        teleportToDominionHome(player);
                    }
                } else if (args[0].equalsIgnoreCase("sethome")) {
                    updateDominionHome(dominion, player);
                } else if (args[0].equalsIgnoreCase("who")) {
                    getDominionWho(args, player);
                } else if (args[0].equalsIgnoreCase("list")) {
                    List<Dominion> sortedDominions = DominionLevelUtils.getDominionsSortedByPlacement();
                    if (sortedDominions.isEmpty()) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.no_dominions_yet")));
                    } else {
                        player.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lDominion Leaderboard &8- - -"));
                        NumberFormat formatter = NumberFormat.getCurrencyInstance();
                        for (int i = 0; i < sortedDominions.size(); i++) {
                            Dominion dominionFromList = sortedDominions.get(i);
                            String valueWithTwoDecimals = formatter.format(dominionFromList.getBalance());
                            player.sendMessage(ChatUtils.translateToColor(
                                    "&8[&6" + (i + 1) + "&8] &e" + dominionFromList.getName()
                                            + " &7ruled by &e" + AranarthUtils.getNickname(Bukkit.getOfflinePlayer(dominionFromList.getLeader()))
                                            + " &7- &e" + dominionFromList.getChunkCount() + " chunks &7- &6" + valueWithTwoDecimals));
                        }
                    }
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (args.length == 1) {
                        if (dominion != null) {
                            displayInfoForDominion(player, dominion);
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_in_dominion")));
                        }
                    } else {
                        StringBuilder dominionNameBuilder = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            dominionNameBuilder.append(args[i]);
                            if (i < args.length - 1) {
                                dominionNameBuilder.append(" ");
                            }
                        }

                        List<Dominion> dominions = DominionUtils.getDominions();
                        boolean wasDominionFound = false;
                        for (Dominion dominionFromList : dominions) {
                            if (ChatUtils.stripColorFormatting(dominionFromList.getName()).equalsIgnoreCase(dominionNameBuilder.toString())) {
                                displayInfoForDominion(player, dominionFromList);
                                wasDominionFound = true;
                                return true;
                            }
                        }

                        if (!wasDominionFound) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.not_found")));
                        }
                    }
                } else if (args[0].equalsIgnoreCase("rename")) {
                    if (dominion != null) {
                        if (dominion.getLeader().equals(player.getUniqueId())) {
                            String dominionName = verifyDominionName(args, player, dominion);
                            if (dominionName != null) {
                                String oldName = dominion.getName();
                                boolean colorOnly = ChatUtils.stripColorFormatting(oldName).equalsIgnoreCase(ChatUtils.stripColorFormatting(dominionName));
                                dominion.setName(dominionName);
                                DominionUtils.updateDominion(dominion);
                                Bukkit.broadcastMessage(ChatUtils.chatMessage(Lang.get("dominion.renamed_broadcast", "old", oldName, "name", dominionName)));
                                if (!colorOnly) {
                                    DiscordUtils.dominionMessage(dominion, "&7The Dominion of &e" + oldName + " &7has been renamed to &e" + dominionName, Color.CYAN);
                                }
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    onlinePlayer.playSound(onlinePlayer, Sound.ENTITY_PLAYER_LEVELUP, 1.2F, 1.5F);
                                }
                            }
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.rename_no_permission")));
                        }
                    }
                } else if (args[0].equalsIgnoreCase("ally")) {
                    allyDominion(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("truce")) {
                    truceDominion(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("enemy")) {
                    enemyDominion(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("neutral")) {
                    neutralDominion(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("setleader")) {
                    setLeader(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("map")) {
                    showDominionMap(player);
                } else if (args[0].equalsIgnoreCase("mapcolor")) {
                    setMapColor(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("autoclaim")) {
                    claimToggle(player);
                } else if (args[0].equalsIgnoreCase("food")) {
                    foodStorage(player);
                } else if (args[0].equalsIgnoreCase("resources")) {
                    resources(dominion, player);
                } else if (args[0].equalsIgnoreCase("conquer")) {
                    conquer(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("surrender")) {
                    surrender(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("rebel")) {
                    rebel(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("retreat")) {
                    retreat(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("msg")) {
                    ChatUtils.evaluateDominionMessage(player, args, true);
                } else if (args[0].equalsIgnoreCase("guide")) {
                    CommandDominions.giveBook(player);
                } else if (args[0].equalsIgnoreCase("rank")) {
                    if (args.length >= 2 && args[1].equalsIgnoreCase("scan")) {
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                        if (aranarthPlayer.getCouncilRank() == 3) {
                            DominionLevelUtils.runPeriodicScan();
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("dominion.scan_triggered")));
                        } else {
                            player.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission")));
                        }
                    } else {
                        showDominionLevel(player, dominion);
                    }
                } else if (args[0].equalsIgnoreCase("setrank")) {
                    setMemberRank(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("buychunks")) {
                    buyChunks(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("outpost")) {
                    handleOutpost(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("defender")) {
                    handleDefender(dominion, player);
                } else if (args[0].equalsIgnoreCase("plot")) {
                    handlePlot(args, dominion, player);
                } else if (args[0].equalsIgnoreCase("rescan")) {
                    rescanDominion(args, player);
                } else {
                    player.sendMessage(ChatUtils.chatMessage(Lang.get("general.invalid_syntax", "usage", "dominion <command>")));
                    return false;
                }
                return true;
            } else {
                sender.sendMessage(ChatUtils.chatMessage(Lang.get("general.no_permission_console")));
                return false;
            }
        }
    }
}
