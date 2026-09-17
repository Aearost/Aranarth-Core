package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.enums.WorldEvent;
import com.aearost.aranarthcore.event.world.WorldEventManager;
import com.aearost.aranarthcore.items.HoneyGlazedHam;
import com.aearost.aranarthcore.items.Quiver;
import com.aearost.aranarthcore.items.arrow.ArrowIron;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkTabManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.event.BendingPlayerLoadEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Adds a new entry to the players HashMap if the player is not being tracked.
 * Additionally, customizes the join/leave server message format.
 */
public class PlayerServerJoinListener implements Listener {

    public PlayerServerJoinListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Displays the MOTD to the player when they join.
     *
     * @param player The player.
     */
    public static void displayMotd(Player player) {
        // Displays the MOTD when the player joins
        int day = AranarthUtils.getDay();
        String weekday = DateUtils.provideWeekdayName(AranarthUtils.getWeekday());
        String month = DateUtils.provideMonthName(AranarthUtils.getMonth());
        int year = AranarthUtils.getYear();

        player.sendMessage(Lang.getFor(player, "motd.divider"));

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        List<String> toggling = new ArrayList<>();
        if (aranarthPlayer.isTogglingChat()) {
            toggling.add(Lang.getFor(player, "motd.toggle_chat"));
        }
        if (aranarthPlayer.isTogglingMessages()) {
            toggling.add(Lang.getFor(player, "motd.toggle_messages"));
        }
        if (aranarthPlayer.isTogglingTp()) {
            toggling.add(Lang.getFor(player, "motd.toggle_tp"));
        }
        if (!aranarthPlayer.isUsingSpawnBoost()) {
            toggling.add(Lang.getFor(player, "motd.toggle_spawn_boost"));
        }
        if (aranarthPlayer.isTogglingChangeClaim()) {
            toggling.add(Lang.getFor(player, "motd.toggle_claim_changes"));
        }
        if (aranarthPlayer.isTogglingInventoryAssist()) {
            toggling.add(Lang.getFor(player, "motd.toggle_inventory_assist"));
        }
        if (!aranarthPlayer.isAddingToShulker()) {
            toggling.add(Lang.getFor(player, "motd.toggle_shulker_assist"));
        }
        if (aranarthPlayer.getBlacklistingMethod() == -1) {
            toggling.add(Lang.getFor(player, "motd.toggle_blacklist"));
        }
        if (!aranarthPlayer.isCompressingItems()) {
            toggling.add(Lang.getFor(player, "motd.toggle_compressor"));
        }
        if (!aranarthPlayer.isAutoLockingChests()) {
            toggling.add(Lang.getFor(player, "motd.toggle_chest_locks"));
        }
        if (aranarthPlayer.getFireType() != FireType.DEFAULT) {
            toggling.add(Lang.getFor(player, "motd.toggle_fire_type", "type", aranarthPlayer.getFireType().getDisplayName()));
        }
        if (aranarthPlayer.isDayMessageDisabled()) {
            toggling.add(Lang.getFor(player, "motd.toggle_day_message"));
        }
        if (aranarthPlayer.isWeatherMessageDisabled()) {
            toggling.add(Lang.getFor(player, "motd.toggle_weather_message"));
        }
        if (aranarthPlayer.isBulkSellShulkerEnabled()) {
            toggling.add(Lang.getFor(player, "motd.toggle_bulk_sell"));
        }

        if (!toggling.isEmpty()) {
            String and = Lang.getFor(player, "motd.toggled_and");
            String toggledFeatures = "  " + Lang.getFor(player, "motd.toggled_prefix") + " ";
            for (int i = 0; i < toggling.size(); i++) {
                toggledFeatures += toggling.get(i);
                if (i < toggling.size() - 2) {
                    toggledFeatures += "&7&o, ";
                } else if (i < toggling.size() - 1) {
                    toggledFeatures += " " + and + " ";
                }
            }
            toggledFeatures += " " + Lang.getFor(player, "motd.toggled_suffix");
            player.sendMessage(ChatUtils.translateToColor(toggledFeatures));
        }

        Avatar avatar = AvatarUtils.getCurrentAvatar();
        if (avatar == null) {
            player.sendMessage("  " + Lang.getFor(player, "motd.no_avatar"));
        } else {
            String avatarNickname = AranarthUtils.getPlayer(avatar.getUuid()).getNickname();
            String element = "";
            if (avatar.getElement() == 'W') {
                element = "&b水";
            } else if (avatar.getElement() == 'E') {
                element = "&a土";
            } else if (avatar.getElement() == 'F') {
                element = "&c火";
            } else {
                element = "&7気";
            }
            player.sendMessage("  " + Lang.getFor(player, "motd.avatar", "element", element, "name", avatarNickname));
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance();

        player.sendMessage("  " + Lang.getFor(player, "motd.balance", "balance", formatter.format(aranarthPlayer.getBalance())));
        HashMap<String, String> activeBoosts = AranarthUtils.getActiveServerBoostsMessages();
        if (activeBoosts.isEmpty()) {
            player.sendMessage("  " + Lang.getFor(player, "motd.no_boosts"));
        } else {
            for (Map.Entry<String, String> boost : activeBoosts.entrySet()) {
                player.sendMessage("  " + Lang.getFor(player, "motd.boost_active", "name", boost.getKey(), "duration", boost.getValue()));
            }
        }

        WorldEvent activeWorldEvent = AranarthUtils.getActiveWorldEvent();
        if (activeWorldEvent != null) {
            int eventIntensity = AranarthUtils.getActiveWorldEventIntensity();
            boolean useThe = activeWorldEvent != WorldEvent.SEIKOS_COMET
                    && activeWorldEvent != WorldEvent.HARMONIC_CONVERGENCE_OF_SACHSI
                    && activeWorldEvent != WorldEvent.AEAROSTS_METEORITE;
            String eventKey = useThe ? "motd.world_event" : "motd.world_event_no_the";
            player.sendMessage("  " + Lang.getFor(player, eventKey, "event", activeWorldEvent.getName(eventIntensity)));
        }

        player.sendMessage("  " + Lang.getFor(player, "motd.date", "weekday", weekday, "day", DateUtils.getDayNumWithSuffix(day), "month", month, "year", String.valueOf(year)));

        int mailCount = MailUtils.getMail(player.getUniqueId()).size();
        if (mailCount > 0) {
            String mailKey = mailCount == 1 ? "motd.mail" : "motd.mail_plural";
            player.sendMessage("  " + Lang.getFor(player, mailKey, "count", String.valueOf(mailCount)));
        }

        // Login streak notification
        boolean streakReset = LoginStreakUtils.ensureStreakValid(player.getUniqueId());
        if (streakReset) {
            player.sendMessage("  " + Lang.getFor(player, "motd.streak_reset"));
        } else if (LoginStreakUtils.canClaim(player.getUniqueId())) {
            player.sendMessage("  " + Lang.getFor(player, "motd.streak_claim"));
        }

        // Pending crate key notification - reload from MySQL first so we display the
        // authoritative count (another server may have received votes or the player may
        // have claimed on another server since this server last loaded from DB).
        UUID uuid = player.getUniqueId();
        PersistenceUtils.reloadVoteKeysForPlayerFromDatabase(uuid);
        int pendingKeys = 0;
        Integer pv = AranarthUtils.getPendingVoteKeys().get(uuid);
        Integer pr = AranarthUtils.getPendingRareKeys().get(uuid);
        Integer pe = AranarthUtils.getPendingEpicKeys().get(uuid);
        Integer pg = AranarthUtils.getPendingGodlyKeys().get(uuid);
		if (pv != null) {
			pendingKeys += pv;
		}
		if (pr != null) {
			pendingKeys += pr;
		}
		if (pe != null) {
			pendingKeys += pe;
		}
		if (pg != null) {
			pendingKeys += pg;
		}
        if (pendingKeys > 0) {
            String keyWord = pendingKeys == 1 ? Lang.getFor(player, "motd.pending_key") : Lang.getFor(player, "motd.pending_keys_plural");
            player.sendMessage("  " + Lang.getFor(player, "motd.pending_keys", "count", String.valueOf(pendingKeys), "keys", keyWord));
        }

        player.sendMessage(Lang.getFor(player, "motd.divider"));
        player.sendMessage("");

        // Adds all aranarth recipes
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                if (key.getNamespace().equalsIgnoreCase("aranarthcore")) {
                    player.discoverRecipe(key);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        Player player = e.getPlayer();

        // Clear any stale BendingPlayer data PK may have cached from a previous session on
        // this server. Without this, players who switch to SMP, change element, then return
        // here would still see their old element because PK skips the DB load when the player
        // is already present in its cache. Clearing here forces PK's own join listener (which
        // fires at NORMAL priority, after this) to do a fresh MySQL read.
        BendingPlayer.getPlayers().remove(player.getUniqueId());
        BendingPlayer.getOfflinePlayers().remove(player.getUniqueId());

        // Load the player's last logout location for login routing and position restoration.
        // Done synchronously here (matching the getPendingTeleport pattern below) so the result
        // is available immediately and can be captured by the delayed-task lambda.
        final DatabaseManager.LastLocation lastLoc = DatabaseManager.isActive()
                ? DatabaseManager.getInstance().loadLastLocation(player.getUniqueId())
                : null;

        // Detect cross-server transfers: either a pending TP from a normal cross-server action,
        // or the player logged off on a different server and needs to be routed there on login.
        String thisServerName = NetworkManager.isActive() ? NetworkManager.getInstance().getThisServer() : null;
        boolean needsServerRouting = lastLoc != null && thisServerName != null && !lastLoc.server.equals(thisServerName);
        boolean hasPendingTp = NetworkManager.isActive() && NetworkManager.getInstance().getPendingTeleport(player.getUniqueId()) != null;
        boolean isCrossServerTransfer = needsServerRouting || hasPendingTp;
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                + " - lastLoc=" + (lastLoc == null ? "null" : lastLoc.server + "/" + lastLoc.world
                + " @ (" + String.format("%.1f", lastLoc.x) + "," + String.format("%.1f", lastLoc.y) + "," + String.format("%.1f", lastLoc.z) + ")")
                + " thisServer=" + thisServerName
                + " needsServerRouting=" + needsServerRouting
                + " hasPendingTp=" + hasPendingTp);
        // If the player was offline when the resource world was reset, teleport them to spawn
        long resetTime = AranarthUtils.getLastResourceWorldResetTime();
        if (resetTime > 0 && player.getWorld().getName().startsWith("resource")
                && player.getLastPlayed() < resetTime) {
            player.teleport(new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0));
            player.sendMessage(ChatUtils.chatMessage(Lang.get("teleport.resource_reset_offline")));
        }

        boolean isNewPlayer = false;
        if (!AranarthUtils.hasPlayedBefore(player)) {
            // Player is not in this server's memory - try loading from the shared DB first.
            // They may have played on Survival (or the other server) but never joined this one.
            if (DatabaseManager.isActive()) {
                PersistenceUtils.reloadPlayerFromDatabase(player.getUniqueId());
            }
            if (!AranarthUtils.hasPlayedBefore(player)) {
                // Truly new to the network - no data anywhere
                AranarthUtils.addPlayer(player.getUniqueId(), new AranarthPlayer(player.getName()));
                LocalDateTime now = LocalDateTime.now();
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setFirstJoinDate(now.getMonthValue() + "/" + now.getDayOfMonth() + "/" + now.getYear());
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.teleport(new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0));
                isNewPlayer = true;
            }
        }
        // If the player changed their username
        else if (AranarthUtils.getUsername(player) != null && !AranarthUtils.getUsername(player).equals(player.getName())) {
            AranarthUtils.setUsername(player);
        }

        if (AvatarUtils.getCurrentAvatar() != null) {
            // Called to bind the Avatar's abilities to prevent loss of avatar abilities
            if (AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
                PersistenceUtils.loadAvatarBinds();

                // Adds a 2-second delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
                        if (bendingPlayer != null) {
                            player.performCommand("b board");
                            player.performCommand("b board");
                        }
                    }
                }.runTaskLater(AranarthCore.getInstance(), 30);
            }
        }

        // Update conquest/rebellion activity tracking for this player's dominion
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion != null) {
            // Defender logging on during an active conquest, reset their inactivity clock
            if (playerDominion.getConqueredRequest() != null) {
                playerDominion.setConqueredRequestDefenderLastSeen(System.currentTimeMillis());
                DominionUtils.updateDominion(playerDominion);
            }
            // Conqueror logging on during an active rebellion, reset their inactivity clock
            if (playerDominion.getRebelRequest() != null) {
                playerDominion.setRebelRequestConquerorLastSeen(System.currentTimeMillis());
                DominionUtils.updateDominion(playerDominion);
            }
        }

        // Load job data from MySQL for this player
        if (DatabaseManager.isActive()) {
            final UUID joinUuid = player.getUniqueId();
            Bukkit.getLogger().info("[AC][Jobs] Dispatching async load for " + player.getName() + " on join");
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                    () -> PersistenceUtils.loadJobDataForPlayer(joinUuid));
        }

        // Permissions must be applied before nickname check is done
        PermissionUtils.evaluatePlayerPermissions(player);

        // Clears a player's nickname if they do not have permission for one
        if (!player.hasPermission("aranarth.nick")) {
            if (!AranarthUtils.getNickname(player).equals(player.getName())) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setNickname("");
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.sendMessage(ChatUtils.chatMessage(Lang.get("player.nickname_cleared")));
            }
        }

        // Announce this player to the network now that permissions and nickname are finalised.
        if (NetworkManager.isActive()) {
            AranarthPlayer apForNetwork = AranarthUtils.getPlayer(player.getUniqueId());
            NetworkManager.getInstance().publishPlayerJoin(player.getUniqueId(), apForNetwork);
        }

        if (isCrossServerTransfer) {
            e.setJoinMessage(null);
            PendingTeleport pendingForDiscord = NetworkManager.isActive()
                    ? NetworkManager.getInstance().getPendingTeleport(player.getUniqueId()) : null;
            boolean isLoginRoutingArrival = pendingForDiscord != null && pendingForDiscord.isLoginRouting();
            if (!isLoginRoutingArrival) {
                // Suppress DiscordSRV join announcement for server-switch arrivals
                player.addAttachment(AranarthCore.getInstance(), "discordsrv.silentjoin", true);
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().markCrossServerJoin(player.getUniqueId());
                }
            }
        } else {
            if (!isNewPlayer) {
                displayMotd(player);
            }
            DateUtils dateUtils = new DateUtils();
            String nameToDisplay;

            if (!AranarthUtils.getNickname(player).isEmpty()) {
                nameToDisplay = "&7" + AranarthUtils.getNickname(player);
            } else {
                nameToDisplay = "&7" + AranarthUtils.getUsername(player);
            }

            String joinMsgStr;
            if (dateUtils.isValentinesDay()) {
                joinMsgStr = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.VALENTINES));
            } else if (dateUtils.isEaster()) {
                joinMsgStr = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.EASTER));
            } else if (dateUtils.isHalloween()) {
                joinMsgStr = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.HALLOWEEN));
            } else if (dateUtils.isChristmas()) {
                joinMsgStr = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(nameToDisplay, SpecialDay.CHRISTMAS));
            } else {
                joinMsgStr = ChatUtils.translateToColor("&8[&a+&8] &7" + nameToDisplay);
            }
            e.setJoinMessage(joinMsgStr);

            // Notify other servers to display the join message and play the join sound
            if (NetworkManager.isActive()) {
                AranarthPlayer apVanishCheck = AranarthUtils.getPlayer(player.getUniqueId());
                if (apVanishCheck == null || !apVanishCheck.isVanished()) {
                    NetworkManager.getInstance().publishJoinMsg(joinMsgStr, isNewPlayer);
                }
            }
        }

        boolean finalIsNewPlayer = isNewPlayer;
        new BukkitRunnable() {
            @Override
            public void run() {
                // Inject existing remote-server players into this player's tab list.
                NetworkTabManager.syncAllToPlayer(player);

                // Execute any pending cross-server teleport (e.g. player transferred here from SMP
                // to complete a /tp or /tpaccept, or returning from SMP via /survival).
                boolean hadPendingTp = false;
                boolean isLoginRouting = false;
                if (NetworkManager.isActive()) {
                    PendingTeleport pending = NetworkManager.getInstance().getPendingTeleport(player.getUniqueId());
                    if (pending != null) {
                        hadPendingTp = true;
                        isLoginRouting = pending.isLoginRouting();
                        NetworkManager.getInstance().clearPendingTeleport(player.getUniqueId());

                        // If this transfer requires an inventory swap, reload player data from
                        // the source server (which saved it to MySQL just before transferring)
                        // and apply the stored survival inventory.
                        if (pending.isApplyInventory()) {
                            // Capture this server's balance delta before reloading
                            AranarthPlayer preReloadAp = AranarthUtils.getPlayer(player.getUniqueId());
                            double localDelta = 0.0;
                            if (preReloadAp != null && preReloadAp.getBalanceSnapshot() >= 0.0) {
                                localDelta = preReloadAp.getBalance() - preReloadAp.getBalanceSnapshot();
                            }
                            PersistenceUtils.reloadPlayerFromDatabase(player.getUniqueId());
                            PersistenceUtils.reloadPlayerSentinelsFromDatabase(player.getUniqueId());
                            PersistenceUtils.loadPlayerTogglesFromDatabase(player.getUniqueId());
                            // Prevents empty jobs data
                            if (DatabaseManager.isActive()) {
                                Bukkit.getLogger().info("[AC][Jobs] Reloading job data (sync) for "
                                        + player.getName() + " after isApplyInventory reload");
                                PersistenceUtils.loadJobDataForPlayer(player.getUniqueId());
                            }
                            if (localDelta != 0.0) {
                                AranarthPlayer apBalance = AranarthUtils.getPlayer(player.getUniqueId());
                                if (apBalance != null) {
                                    double mergedBalance = apBalance.getBalance() + localDelta;
                                    apBalance.setBalance(mergedBalance);
                                    apBalance.setBalanceSnapshot(mergedBalance);
                                }
                            }
                            AranarthPlayer apInv = AranarthUtils.getPlayer(player.getUniqueId());
                            if (apInv != null && !apInv.getSurvivalInventory().isEmpty()) {
                                try {
                                    player.getInventory().setContents(
                                            ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalInventory()));
                                } catch (Exception e) {
                                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to apply survival inventory for " + player.getName() + ": " + e.getMessage());
                                    player.getInventory().clear();
                                }
                            } else {
                                // survivaInventory is an empty string, meaning it was never
                                // serialized (default value) or the DB write failed before the
                                // transfer completed (e.g. the player crashed mid-transfer).
                                // Do NOT clear the inventory - doing so would wipe the player's
                                // items (Bukkit's clear() leaves armor slots, producing the
                                // "only armor remained" symptom). Leave Minecraft's native
                                // player data intact and log a warning for diagnosis.
                                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                                        + "[Inv] survivaInventory is empty for " + player.getName()
                                        + " on isApplyInventory arrival - skipping clear to protect items (stale pending teleport?)");
                            }
                            if (apInv != null && !apInv.getSurvivalEnderChest().isEmpty()) {
                                try {
                                    player.getEnderChest().setContents(
                                            ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalEnderChest()));
                                } catch (Exception e) {
                                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Failed to apply ender chest for " + player.getName() + ": " + e.getMessage());
                                    player.getEnderChest().clear();
                                }
                            }
                            if (apInv != null) {
                                player.setHealth(Math.min(apInv.getSurvivalHealth(), player.getAttribute(Attribute.MAX_HEALTH).getValue()));
                                player.setFoodLevel(apInv.getSurvivalFoodLevel());
                                player.setSaturation(apInv.getSurvivalSaturation());
                                player.setLevel(apInv.getSurvivalExpLevel());
                                player.setExp(apInv.getSurvivalExpProgress());
                            }
                            player.setGameMode(GameMode.SURVIVAL);
                            // If the player's last position on this server was in a non-survival world
                            // (e.g. creative or arena), Minecraft will have restored them there with the
                            // survival inventory we just applied - an inconsistent state. Any pending
                            // teleport to a survival world (e.g. "dominion home") would then trigger
                            // switchInventory(creative→world), wrongly saving the survival items as the
                            // creative inventory. Move the player to survival world spawn via a direct
                            // player.teleport() call (TeleportCause.PLUGIN) so the pending command fires
                            // from a survival-world context. PLUGIN cause is intentional: our
                            // PlayerTeleportBetweenWorldsListener only processes TeleportCause.COMMAND,
                            // so switchInventory is not triggered by this relocation.
                            if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
                                World fallbackWorld = Bukkit.getWorld("spawn");
								if (fallbackWorld == null) {
									fallbackWorld = Bukkit.getWorld("world");
								}
                                if (fallbackWorld != null) {
                                    player.teleport(new Location(fallbackWorld, 0.5, 101, 0.5, 180, 0));
                                } else {
                                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Inv] Could not find spawn/world to relocate " + player.getName()
                                            + " out of '" + player.getWorld().getName() + "' - pending teleport may corrupt creative/arena inventory");
                                }
                            }
                            // Re-publish with fresh data so remote servers see the correct nickname/rank
                            if (NetworkManager.isActive()) {
                                AranarthPlayer freshAp = AranarthUtils.getPlayer(player.getUniqueId());
                                if (freshAp != null) {
                                    NetworkManager.getInstance().publishPlayerJoin(player.getUniqueId(), freshAp);
                                }
                            }
                            // Publish the join announcement when this isApplyInventory transfer was
                            // also a login-routing transfer (player logged off on this server, Velocity
                            // routed them through the other server which suppressed the join message).
                            if (isLoginRouting) {
                                AranarthPlayer apJoin = AranarthUtils.getPlayer(player.getUniqueId());
                                if (apJoin != null && !apJoin.isVanished()) {
                                    String routedName = "&7" + AranarthUtils.getNickname(player);
                                    DateUtils routedDateUtils = new DateUtils();
                                    String routedJoinMsg;
                                    if (routedDateUtils.isValentinesDay()) {
                                        routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.VALENTINES));
                                    } else if (routedDateUtils.isEaster()) {
                                        routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.EASTER));
                                    } else if (routedDateUtils.isHalloween()) {
                                        routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.HALLOWEEN));
                                    } else if (routedDateUtils.isChristmas()) {
                                        routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.CHRISTMAS));
                                    } else {
                                        routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + routedName);
                                    }
                                    for (Player online : Bukkit.getOnlinePlayers()) {
                                        online.sendMessage(routedJoinMsg);
                                    }
                                    if (NetworkManager.isActive()) {
                                        NetworkManager.getInstance().publishJoinMsg(routedJoinMsg, false);
                                    }
                                    PlayerServerJoinListener.this.playJoinSound();
                                }
                            }
                        } else if (hadPendingTp) {
                            // Login routing: player arrived at their final destination via cross-server
                            // routing on login (e.g. last logged off on SMP, routed here from Survival).
                            // No inventory apply needed (Minecraft restores state from player data), but
                            // we DO need to publish a join announcement since the routing server suppressed it.
                            if (isLoginRouting) {
                                int nonNull = 0;
                                for (org.bukkit.inventory.ItemStack is : player.getInventory().getContents()) {
									if (is != null) {
										nonNull++;
									}
                                }
                                int armorNonNull = 0;
                                for (org.bukkit.inventory.ItemStack is : player.getInventory().getArmorContents()) {
									if (is != null) {
										armorNonNull++;
									}
                                }
                                Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Inv] LoginRouting arrival for "
                                        + player.getName() + " - player.dat inventory: " + nonNull
                                        + " non-null main slot(s), " + armorNonNull + " armor piece(s)."
                                        + (nonNull == 0 ? " WARNING: main inventory is empty - player.dat may be stale." : ""));
                            }
                            AranarthPlayer apJoin = AranarthUtils.getPlayer(player.getUniqueId());
                            if (apJoin != null && !apJoin.isVanished()) {
                                String routedName = "&7" + AranarthUtils.getNickname(player);
                                DateUtils routedDateUtils = new DateUtils();
                                String routedJoinMsg;
                                if (routedDateUtils.isValentinesDay()) {
                                    routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.VALENTINES));
                                } else if (routedDateUtils.isEaster()) {
                                    routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.EASTER));
                                } else if (routedDateUtils.isHalloween()) {
                                    routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.HALLOWEEN));
                                } else if (routedDateUtils.isChristmas()) {
                                    routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + ChatUtils.getSpecialJoinMessage(routedName, SpecialDay.CHRISTMAS));
                                } else {
                                    routedJoinMsg = ChatUtils.translateToColor("&8[&a+&8] &7" + routedName);
                                }
                                for (Player online : Bukkit.getOnlinePlayers()) {
                                    if (!online.getUniqueId().equals(player.getUniqueId())) {
                                        online.sendMessage(routedJoinMsg);
                                    }
                                }
                                if (NetworkManager.isActive()) {
                                    NetworkManager.getInstance().publishJoinMsg(routedJoinMsg, false);
                                }
                                PlayerServerJoinListener.this.playJoinSound();
                            }
                        }

                        if ("player".equals(pending.getType())) {
                            // TP to wherever the named player currently is
                            Player target = Bukkit.getPlayer(UUID.fromString(pending.getTargetUuid()));
                            if (target != null) {
                                AranarthUtils.teleportPlayer(player, player.getLocation(), target.getLocation(),
                                        true, pending.getTitleMain(), pending.getTitleSub(), success -> {
                                        });
                            }
                        } else if ("command".equals(pending.getType())) {
                            // Dispatch a command as if the player ran it on this server.
                            // Force admin mode so the command skips its own countdown
                            // (the countdown already happened on the source server).
                            AranarthPlayer apCmd = AranarthUtils.getPlayer(player.getUniqueId());
                            boolean wasAdmin = apCmd.isInAdminMode();
                            apCmd.setInAdminMode(true);
                            AranarthUtils.setPlayer(player.getUniqueId(), apCmd);
                            Bukkit.dispatchCommand(player, pending.getCommand());
                            AranarthPlayer apCmdAfter = AranarthUtils.getPlayer(player.getUniqueId());
                            apCmdAfter.setInAdminMode(wasAdmin);
                            AranarthUtils.setPlayer(player.getUniqueId(), apCmdAfter);
                        } else {
                            // TP to the stored coordinates
                            World w = Bukkit.getWorld(pending.getWorld());
                            if (w != null) {
                                Location dest = new Location(w,
                                        pending.getX(), pending.getY(), pending.getZ(),
                                        pending.getYaw(), pending.getPitch());
                                AranarthUtils.teleportPlayer(player, player.getLocation(), dest,
                                        true, pending.getTitleMain(), pending.getTitleSub(), success -> {
                                        });
                            }
                        }
                    }
                }

                // Reload quest progress from DB so assignments and progress match the source server.
                // Skip when this is a login-routing transfer (player being routed back to the server
                // they last logged off on) - the in-memory data here is already authoritative and
                // the async DB write from the quit event may not have finished yet.
                if (hadPendingTp && !isLoginRouting && DatabaseManager.isActive()) {
                    PersistenceUtils.reloadQuestProgressForPlayer(player.getUniqueId());
                    Bukkit.getLogger().info("[AC][Jobs] Reloading job data (sync) for " + player.getName() + " on cross-server arrival");
                    PersistenceUtils.loadJobDataForPlayer(player.getUniqueId());
                    // Reload the player's dominion in case it was created on the other server
                    PersistenceUtils.reloadDominionForPlayer(player.getUniqueId());
                    // Reload streak and mail so this server has authoritative state from MySQL.
                    // Without this, the source server's claim/clear changes would be invisible here
                    // (stale data from our startup load), and when the player quits from this server,
                    // the stale data would be written back to MySQL - overwriting the correct state.
                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[XServer] Reloading streak+mail from MySQL for "
                            + player.getName() + " on cross-server arrival");
                    PersistenceUtils.reloadPlayerLoginStreakFromDatabase(player.getUniqueId());
                    PersistenceUtils.reloadPlayerMailFromDatabase(player.getUniqueId());
                }

                // Apply the /back location saved by the source server before transfer.
                if (hadPendingTp && NetworkManager.isActive()) {
                    NetworkManager.getInstance().loadAndApplyCrossServerBack(player.getUniqueId());
                    NetworkManager.getInstance().loadAndApplyCrossServerLastMsg(player.getUniqueId());
                }

                // When arriving via cross-server transfer, force other clients to reload this
                // player's skin and tab entry by briefly hiding then re-showing them.
                if (hadPendingTp) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
							if (!player.isOnline()) {
								return;
							}
                            for (Player other : Bukkit.getOnlinePlayers()) {
                                if (!other.getUniqueId().equals(player.getUniqueId())) {
                                    other.hidePlayer(AranarthCore.getInstance(), player);
                                    other.showPlayer(AranarthCore.getInstance(), player);
                                }
                            }
                            // Also force a tab update for the joining player to see themselves
                            AranarthUtils.updateTab();
                        }
                    }.runTaskLater(AranarthCore.getInstance(), 20L);
                }

                // Restore the player to where they last logged out.
                // If they logged off on this server: teleport them to the saved position.
                // If they logged off on another server: route them there via a pending teleport,
                // which the receiving server will execute on arrival.
                if (!hadPendingTp && lastLoc != null && NetworkManager.isActive()) {
                    if (lastLoc.server.equals(NetworkManager.getInstance().getThisServer())) {
                        World w = Bukkit.getWorld(lastLoc.world);
                        if (w != null) {
                            Location dest = new Location(
                                    w, lastLoc.x, lastLoc.y, lastLoc.z, lastLoc.yaw, lastLoc.pitch);
                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                    + " - same-server login; restoring position to " + lastLoc.world
                                    + " @ (" + String.format("%.1f", lastLoc.x) + "," + String.format("%.1f", lastLoc.y) + "," + String.format("%.1f", lastLoc.z) + ")");
                            new BukkitRunnable() {
                                @Override
                                public void run() {
									if (player.isOnline()) {
										player.teleport(dest);
									}
                                }
                            }.runTaskLater(AranarthCore.getInstance(), 2L);
                        } else {
                            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                    + " - same-server login but world '" + lastLoc.world + "' is not loaded; position restore skipped.");
                        }
                    } else {
                        // Unplanned cross-server landing: the player's last known location is on
                        // another server and they arrived here without a pending teleport
                        // (e.g. the source server restarted and Velocity routed them here as a
                        // fallback). Paper loads this server's player.dat before any plugin code
                        // runs, so the player currently holds the wrong inventory.
                        //
                        // The strategy differs by which server we are on:
                        //
                        // On Survival (non-SMP): check MySQL for a fallback snapshot that SMP
                        // writes every 30s. If found, apply it and let the player stay with their
                        // correct SMP items. If not found (crash with no recent write), kick so
                        // no stale state is written to MySQL.
                        //
                        // On SMP: the player was last on Survival and Velocity sent them here
                        // instead. Route them back to Survival the same way the original code
                        // did - their last_loc.server = "survival" is authoritative, and
                        // Survival's player.dat holds the correct inventory for that server.
                        // Kicking is NOT safe here: it would leave last_loc.server = "survival"
                        // unchanged, so every reconnect would hit SMP first and loop forever.
                        final String sourceServer = lastLoc.server;
                        final UUID uuidForAsync = player.getUniqueId();
                        if (!AranarthCore.isSmpServer()) {
                            // --- Survival side: snapshot check ---
                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                    + " - unplanned landing from " + sourceServer
                                    + "; checking for fallback snapshot in MySQL.");
                            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
                                String snapJson = DatabaseManager.isActive()
                                        ? DatabaseManager.getInstance().loadTempData(
                                                NetworkManager.KEY_SMP_RESTART_INV + uuidForAsync)
                                        : null;
                                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                                    if (!player.isOnline()) return;
                                    if (snapJson != null) {
                                        // Apply the SMP snapshot so the player has their correct items
                                        try {
                                            JsonObject snap = JsonParser.parseString(snapJson).getAsJsonObject();
                                            player.getInventory().setContents(
                                                    ItemUtils.itemStackArrayFromBase64(snap.get("inventory").getAsString()));
                                            player.getEnderChest().setContents(
                                                    ItemUtils.itemStackArrayFromBase64(snap.get("enderChest").getAsString()));
                                            if (snap.has("health")) {
                                                double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                                                player.setHealth(Math.min(snap.get("health").getAsDouble(), maxHp));
                                            }
                                            if (snap.has("food")) player.setFoodLevel(snap.get("food").getAsInt());
                                            if (snap.has("saturation")) player.setSaturation(snap.get("saturation").getAsFloat());
                                            if (snap.has("expLevel")) player.setLevel(snap.get("expLevel").getAsInt());
                                            if (snap.has("expProgress")) player.setExp(snap.get("expProgress").getAsFloat());
                                            // Update in-memory AranarthPlayer so the quit listener saves
                                            // the SMP inventory (not the old Survival snapshot) to MySQL.
                                            AranarthPlayer ap = AranarthUtils.getPlayer(uuidForAsync);
                                            if (ap != null) {
                                                ap.setSurvivalInventory(snap.get("inventory").getAsString());
                                                ap.setSurvivalEnderChest(snap.get("enderChest").getAsString());
                                                if (snap.has("health")) ap.setSurvivalHealth(player.getHealth());
                                                if (snap.has("food")) ap.setSurvivalFoodLevel(player.getFoodLevel());
                                                if (snap.has("saturation")) ap.setSurvivalSaturation(player.getSaturation());
                                                if (snap.has("expLevel")) ap.setSurvivalExpLevel(player.getLevel());
                                                if (snap.has("expProgress")) ap.setSurvivalExpProgress(player.getExp());
                                                AranarthUtils.setPlayer(uuidForAsync, ap);
                                            }
                                            // Consume the snapshot (one-use) so it cannot be applied again.
                                            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(),
                                                    () -> DatabaseManager.getInstance().deleteTempData(
                                                            NetworkManager.KEY_SMP_RESTART_INV + uuidForAsync));
                                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                                    + " - SMP fallback snapshot applied; player landed on "
                                                    + NetworkManager.getInstance().getThisServer()
                                                    + " with SMP inventory.");
                                        } catch (Exception ex) {
                                            // If snapshot application fails, leave the player on Survival
                                            // with the inventory Paper loaded (Survival player.dat). Their
                                            // SMP items remain safe in SMP's player.dat.
                                            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                                    + " - failed to apply SMP fallback snapshot: " + ex.getMessage()
                                                    + "; player stays on Survival with existing Survival inventory.");
                                        }
                                    } else {
                                        // No snapshot available (crash or TTL expired). Let the player
                                        // stay on Survival with whatever Survival player.dat loaded -
                                        // we have no SMP inventory to apply. Their SMP items are safe
                                        // in SMP's player.dat and will be there when SMP comes back.
                                        // The quit listener will update MySQL (last_loc + survivalInventory)
                                        // correctly when they eventually leave.
                                        Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                                + " - no SMP fallback snapshot found for '" + sourceServer
                                                + "'; player stays on Survival with existing Survival inventory.");
                                    }
                                });
                            });
                        } else {
                            // --- SMP side: route back to source server ---
                            // Kicking here would be an infinite loop: last_loc.server = "survival"
                            // means Velocity always tries SMP first, so the player can never land
                            // anywhere else. Route them to their last server instead; from there
                            // they can /smp back once SMP is ready.
                            final String velocityTarget = AranarthCore.getInstance().getConfig()
                                    .getString("network.servers." + sourceServer, sourceServer);
                            final String locWorld = lastLoc.world;
                            final double locX = lastLoc.x, locY = lastLoc.y, locZ = lastLoc.z;
                            final float locYaw = lastLoc.yaw, locPitch = lastLoc.pitch;
                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                    + " - unplanned landing on SMP from " + sourceServer
                                    + "; routing back to " + velocityTarget + " (player.dat on source server is authoritative).");
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (!player.isOnline()) return;
                                    if (DatabaseManager.isActive()) {
                                        PersistenceUtils.reloadPlayerFromDatabase(uuidForAsync);
                                        PersistenceUtils.loadPlayerTogglesFromDatabase(uuidForAsync);
                                        PersistenceUtils.loadJobDataForPlayer(uuidForAsync);
                                    }
                                    Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                            + " - routing to " + velocityTarget + " after unplanned SMP landing.");
                                    PendingTeleport pt = new PendingTeleport(
                                            locWorld, locX, locY, locZ, locYaw, locPitch, "", "");
                                    pt.setLoginRouting(true);
                                    NetworkManager.getInstance().setPendingAndTransfer(player, velocityTarget, pt);
                                }
                            }.runTaskLater(AranarthCore.getInstance(), 40L);
                        }
                    }
                }

                // Log survivalInventory state for players arriving from a non-survival world
                // (e.g. creative, arena). These players won't have their survival inventory
                // applied on login, so if it's empty here it will be empty when they /home.
                if (lastLoc != null && !AranarthUtils.isSurvivalWorld(lastLoc.world)) {
                    AranarthPlayer apInvCheck = AranarthUtils.getPlayer(player.getUniqueId());
                    if (apInvCheck != null) {
                        boolean empty = apInvCheck.getSurvivalInventory().isEmpty();
                        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Inv] " + player.getName()
                                + " joined from non-survival world '" + lastLoc.world + "': survivalInventory "
                                + (empty ? "EMPTY - will skip clear if they switch to survival"
                                : "set (length=" + apInvCheck.getSurvivalInventory().length() + ")"));
                    }
                }

                // Apply MySQL survival inventory as a recovery fallback for same-server logins.
                // MySQL is kept current by quit-time and periodic snapshots, so it is more
                // reliable than a stale player.dat left by an ungraceful server shutdown.
                if (!hadPendingTp && lastLoc != null && NetworkManager.isActive()
                        && lastLoc.server.equals(NetworkManager.getInstance().getThisServer())
                        && AranarthUtils.isSurvivalWorld(lastLoc.world)) {
                    AranarthPlayer apInv = AranarthUtils.getPlayer(player.getUniqueId());
                    if (apInv != null && !apInv.getSurvivalInventory().isEmpty()) {
                        try {
                            player.getInventory().setContents(
                                    ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalInventory()));
                            int nonNull = 0;
                            for (org.bukkit.inventory.ItemStack is : player.getInventory().getContents()) {
								if (is != null) {
									nonNull++;
								}
                            }
                            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                    + " - same-server MySQL inventory restore applied: " + nonNull + " item(s).");
                        } catch (Exception ex) {
                            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                                    + "[Inv] Failed to apply survival inventory on login for " + player.getName() + ": " + ex.getMessage());
                        }
                        if (!apInv.getSurvivalEnderChest().isEmpty()) {
                            try {
                                player.getEnderChest().setContents(
                                        ItemUtils.itemStackArrayFromBase64(apInv.getSurvivalEnderChest()));
                            } catch (Exception ex) {
                                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX
                                        + "[Inv] Failed to apply ender chest on login for " + player.getName() + ": " + ex.getMessage());
                            }
                        }
                        if (apInv.getSurvivalHealth() > 0) {
                            player.setHealth(Math.min(apInv.getSurvivalHealth(), player.getAttribute(Attribute.MAX_HEALTH).getValue()));
                        }
                        player.setFoodLevel(apInv.getSurvivalFoodLevel());
                        player.setSaturation(apInv.getSurvivalSaturation());
                        player.setLevel(apInv.getSurvivalExpLevel());
                        player.setExp(apInv.getSurvivalExpProgress());
                    } else {
                        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Join] " + player.getName()
                                + " - same-server MySQL inventory restore skipped: snapshot is "
                                + (apInv == null ? "null (no AranarthPlayer)" : "empty"));
                    }
                }

                // Displays a welcome message after the join message
                if (finalIsNewPlayer) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage("");
                        online.sendMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
                        online.sendMessage(ChatUtils.translateToColor("                     &7Welcome, &e" + player.getName() + ","));
                        online.sendMessage(ChatUtils.translateToColor("                    &7to the &6&lRealm of Aranarth!"));
                        online.sendMessage(ChatUtils.translateToColor("                &6&l-------------------------"));
                        online.sendMessage("");
                    }
                    // Broadcast welcome to other servers in the network
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishChat("", "");
                        NetworkManager.getInstance().publishChat("", "                &6&l-------------------------");
                        NetworkManager.getInstance().publishChat("", "                     &7Welcome, &e" + player.getName() + ",");
                        NetworkManager.getInstance().publishChat("", "                    &7to the &6&lRealm of Aranarth!");
                        NetworkManager.getInstance().publishChat("", "                &6&l-------------------------");
                        NetworkManager.getInstance().publishChat("", "");
                    }

                    player.sendMessage(ChatUtils.chatMessage(Lang.get("join.welcome_rules")));

                    // Give starter kit
                    PlayerInventory inv = player.getInventory();
                    inv.setItem(0, new ItemStack(Material.STONE_SWORD));
                    inv.setItem(1, new ItemStack(Material.STONE_PICKAXE));
                    inv.setItem(2, new ItemStack(Material.STONE_AXE));
                    inv.setItem(3, new ItemStack(Material.STONE_SHOVEL));
                    ItemStack honeyGlazedHam = new HoneyGlazedHam().getItem();
                    honeyGlazedHam.setAmount(16);
                    inv.setItem(4, honeyGlazedHam);
                    inv.setItem(5, new ItemStack(Material.BUNDLE));
                    ItemStack ironArrows = new ArrowIron().getItem();
                    ironArrows.setAmount(16);
                    inv.setItem(6, ironArrows);
                    inv.setItem(7, new Quiver().getItem());
                    inv.setItem(8, new ItemStack(Material.BOW));

                    // Equip leather armor
                    inv.setHelmet(new ItemStack(Material.LEATHER_HELMET));
                    inv.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                    inv.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
                    inv.setBoots(new ItemStack(Material.LEATHER_BOOTS));
                    inv.setItemInOffHand(new ItemStack(Material.SHIELD));
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);

        if (finalIsNewPlayer) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                int vol = AranarthUtils.getPlayer(online.getUniqueId()).getJoinSoundVolume();
                if (vol > 0) {
                    online.playSound(online, Sound.UI_TOAST_CHALLENGE_COMPLETE, vol / 100f, 0.8F);
                }
            }
        } else if (!isCrossServerTransfer) {
            playJoinSound();
        }
        AranarthUtils.updateTab();
    }

    /**
     * Re-evaluates bending sub-elements once ProjectKorra has finished loading
     * the player's BendingPlayer object. This handles the race condition where
     * evaluatePlayerPermissions runs before the BendingPlayer is available in
     * ONLINE_PLAYERS (either because AranarthCore's listener fires before
     * PKListener's, or because the data was loaded asynchronously from the DB).
     */
    @EventHandler
    public void onBendingPlayerLoad(BendingPlayerLoadEvent e) {
        if (!e.isOnline()) {
            return;
        }
        BendingPlayer bendingPlayer = (BendingPlayer) e.getBendingPlayer();
        Player player = bendingPlayer.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        PermissionUtils.updateSubElements(player);

        // Apply active elemental world event permissions now that BendingPlayer is loaded
        WorldEvent activeWorldEvent = AranarthUtils.getActiveWorldEvent();
        if (activeWorldEvent != null && activeWorldEvent.isElementalEvent()
                && WorldEventManager.getInstance() != null) {
            WorldEventManager.getInstance().applyElementalEventToPlayer(player, activeWorldEvent);
        }
    }

    /**
     * Plays a sound effect when a player joins the server.
     */
    private void playJoinSound() {
        new BukkitRunnable() {
            int runs = 0;

            @Override
            public void run() {
                if (runs == 0) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int vol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getJoinSoundVolume();
						if (vol > 0) {
							onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1F);
						}
                    }
                    runs++;
                } else if (runs == 1) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int vol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getJoinSoundVolume();
						if (vol > 0) {
							onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.2F);
						}
                    }
                    runs++;
                } else {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        int vol = AranarthUtils.getPlayer(onlinePlayer.getUniqueId()).getJoinSoundVolume();
						if (vol > 0) {
							onlinePlayer.playSound(onlinePlayer, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, vol / 100f, 1.6F);
						}
                    }
                    cancel();
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0, 5); // Runs every 5 ticks
    }


}
