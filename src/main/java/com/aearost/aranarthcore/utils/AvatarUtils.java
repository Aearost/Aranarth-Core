package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.OfflineBendingPlayer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * Provides a large variety of utility methods for everything related to AranarthCore.
 */
public class AvatarUtils {

    /**
     * The result of an attempt to select a new Avatar.
     */
    public enum SelectionResult {
        SELECTED, ALREADY_REIGNING, NOT_ELIGIBLE, NONE_FOUND, BUSY
    }

    // The one-time automatic selection of the first Avatar (Survival only)
    private static final ZonedDateTime AUTO_SELECT_TIME = ZonedDateTime.of(2026, 10, 31, 13, 0, 0, 0, ZoneId.of("America/New_York"));
    // Stops the automatic selection from ever re-running later if the avatar list somehow failed to load
    private static final Duration AUTO_SELECT_WINDOW = Duration.ofDays(7);
    private static final Duration INACTIVITY_LIMIT = Duration.ofDays(7);
    private static final String KEY_AVATAR_LAST_SEEN = "avatar_last_seen:";
    private static final String MANNEQUIN_TAG = "aranarth_avatar_mannequin";
    // x, y, z, yaw of each mannequin, the first being the current Avatar's spot
    private static final double[][] MANNEQUIN_SPOTS = {
            {16.5, 102, 13.5, 45}, {18.5, 103, 15.5, 90}, {18.5, 103, 18.5, 90}, {18.5, 103, 21.5, 120},
            {16.5, 103, 24.5, 135}, {13.5, 104, 25.5, 180}, {10.5, 104, 24.5, 180}, {7.5, 104, 23.5, 180},
            {4.5, 105, 25.5, 180}, {1.5, 105, 24.5, 180}, {-1.5, 105, 23.5, -135}, {-2.5, 105, 20.5, -90},
            {-0.5, 106, 17.5, -90}, {1.5, 106, 14.5, -25}, {4.5, 106, 13.5, 0}, {7.5, 107, 12.5, 0},
            {10.5, 107, 12.5, 0}, {13.5, 107, 12.5, 0}, {16.5, 108, 12.5, 0}, {19.5, 108, 14.5, 45},
            {21.5, 108, 17.5, 90}, {21.5, 109, 20.5, 90}, {20.5, 109, 23.5, 150}, {18.5, 109, 26.5, 135},
            {15.5, 110, 27.5, 180}, {12.5, 110, 27.5, 180}, {9.5, 110, 26.5, 180}, {6.5, 110, 25.5, 180},
            {3.5, 111, 25.5, 180}, {0.5, 111, 24.5, 180}, {-2.5, 111, 23.5, -135}, {-3.5, 111, 20.5, -90},
            {-3.5, 112, 17.5, -90}, {-1.5, 112, 14.5, -45}, {1.5, 112, 12.5, 0}, {4.5, 113, 10.5, 0},
            {7.5, 113, 9.5, 0}, {10.5, 114, 10.5, 0}, {13.5, 114, 11.5, 0}, {16.5, 114, 12.5, 90}
    };

    private static List<Avatar> avatars = new ArrayList<>();
    private static boolean isSelecting = false;
    private static long lastSeenSavedAt = 0;
    // Incremented on every refresh so that mannequins from an older refresh are never spawned
    private static int mannequinGeneration = 0;

    /**
     * Provides the list of all avatars.
     *
     * @return The list of all avatars.
     */
    public static List<Avatar> getAvatars() {
        return avatars;
    }

    /**
     * Updates the list of all avatars.
     *
     * @param newAvatars The list of all avatars.
     */
    public static void setAvatars(List<Avatar> newAvatars) {
        avatars = newAvatars;
    }

    /**
     * Adds a new avatar.
     *
     * @param avatar The new avatar.
     */
    public static void addAvatar(Avatar avatar) {
        avatars.add(avatar);
    }

    /**
     * Provides the current reigning Avatar.
     *
     * @return The current reigning Avatar.
     */
    public static Avatar getCurrentAvatar() {
        if (!avatars.isEmpty()) {
            return avatars.get(avatars.size() - 1);
        }
        return null;
    }

    /**
     * Provides the most recent Avatar whose reign has ended.
     *
     * @return The most recent Avatar whose reign has ended, or null if there is none.
     */
    private static Avatar getPreviousAvatar() {
        for (int i = avatars.size() - 1; i >= 0; i--) {
            Avatar avatar = avatars.get(i);
            if (avatar != null && avatar != getCurrentAvatar()) {
                return avatar;
            }
        }
        return null;
    }

    /**
     * Runs the scheduled Avatar logic. Must only be called on the Survival server.
     * Selects the first-ever Avatar once the automatic selection time has passed, and ends the reign
     * of an Avatar that has been inactive on every server for too long.
     */
    public static void runScheduledChecks() {
        ZonedDateTime now = ZonedDateTime.now(AUTO_SELECT_TIME.getZone());

        // The first-ever Avatar is selected automatically, retrying each check until someone eligible is online
        if (avatars.isEmpty()) {
            if (!now.isBefore(AUTO_SELECT_TIME) && now.isBefore(AUTO_SELECT_TIME.plus(AUTO_SELECT_WINDOW))) {
                selectAvatar(null, result -> {
                    if (result == SelectionResult.NONE_FOUND) {
                        Bukkit.getLogger().info("[AC] A new Avatar could not be selected, will try again next check");
                    }
                });
            }
            return;
        }

        Avatar avatar = getCurrentAvatar();
        if (avatar == null) {
            return;
        }

        UUID uuid = avatar.getUuid();
        if (isOnlineAnywhere(uuid)) {
            // Tracks when the Avatar was last seen on any server, as getLastPlayed() only covers this server
            if (DatabaseManager.isActive() && System.currentTimeMillis() - lastSeenSavedAt >= 600_000) {
                lastSeenSavedAt = System.currentTimeMillis();
                String value = String.valueOf(lastSeenSavedAt);
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                        DatabaseManager.getInstance().saveTempData(KEY_AVATAR_LAST_SEEN + uuid, value, 30 * 24 * 60 * 60));
            }
            return;
        }

        long lastPlayedHere = Bukkit.getOfflinePlayer(uuid).getLastPlayed();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            long lastSeen = lastPlayedHere;
            if (DatabaseManager.isActive()) {
                String stored = DatabaseManager.getInstance().loadTempData(KEY_AVATAR_LAST_SEEN + uuid);
                if (stored != null) {
                    try {
                        lastSeen = Math.max(lastSeen, Long.parseLong(stored));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (lastSeen <= 0 || System.currentTimeMillis() - lastSeen < INACTIVITY_LIMIT.toMillis()) {
                return;
            }
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                // The reign may have changed while the last seen time was being read
                Avatar current = getCurrentAvatar();
                if (current != null && current.getUuid().equals(uuid) && !isOnlineAnywhere(uuid)) {
                    Bukkit.getLogger().info("[AC] Avatar " + Bukkit.getOfflinePlayer(uuid).getName() + " has been inactive for 7 days");
                    removeCurrentAvatar();
                }
            });
        });
    }

    /**
     * Determines if the player is online on this server or on another server in the network.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the player is online anywhere.
     */
    private static boolean isOnlineAnywhere(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            return true;
        }
        return NetworkManager.isActive() && NetworkManager.getInstance().getRemotePlayer(uuid) != null;
    }

    /**
     * Determines if the player is online on another server in the network but not this one.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the player is online on another server.
     */
    private static boolean isOnlineRemotely(UUID uuid) {
        return Bukkit.getPlayer(uuid) == null && NetworkManager.isActive()
                && NetworkManager.getInstance().getRemotePlayer(uuid) != null;
    }

    /**
     * Selects a new Avatar from the players online across the network.
     *
     * @param target   The player to make the Avatar, or null to select one at random.
     * @param callback Called on the main thread with the result of the selection.
     */
    public static void selectAvatar(UUID target, Consumer<SelectionResult> callback) {
        if (getCurrentAvatar() != null) {
            callback.accept(SelectionResult.ALREADY_REIGNING);
            return;
        }
        if (isSelecting) {
            callback.accept(SelectionResult.BUSY);
            return;
        }

        List<UUID> candidates = new ArrayList<>();
        if (target != null) {
            if (!isOnlineAnywhere(target)) {
                callback.accept(SelectionResult.NOT_ELIGIBLE);
                return;
            }
            candidates.add(target);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (aranarthPlayer == null || !aranarthPlayer.isVanished()) {
                    candidates.add(player.getUniqueId());
                }
            }
            if (NetworkManager.isActive()) {
                for (NetworkPlayer networkPlayer : NetworkManager.getInstance().getRemoteRoster().values()) {
                    if (!networkPlayer.isVanished() && !candidates.contains(networkPlayer.getUuid())) {
                        candidates.add(networkPlayer.getUuid());
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            callback.accept(SelectionResult.NONE_FOUND);
            return;
        }

        // Players on the other server are not loaded here, so their bending data is read from the shared database
        isSelecting = true;
        Map<UUID, CompletableFuture<OfflineBendingPlayer>> futures = new LinkedHashMap<>();
        for (UUID uuid : candidates) {
            futures.put(uuid, BendingPlayer.getOrLoadOfflineAsync(Bukkit.getOfflinePlayer(uuid))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> null));
        }

        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).whenComplete((ignored, ex) ->
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    isSelecting = false;
                    // Another selection may have completed in the meantime
                    if (getCurrentAvatar() != null) {
                        callback.accept(SelectionResult.ALREADY_REIGNING);
                        return;
                    }

                    // The element cycles Air -> Water -> Earth -> Fire, with the first Avatar being fully random
                    Avatar previousAvatar = getPreviousAvatar();
                    Character requiredElement = null;
                    if (previousAvatar != null) {
                        requiredElement = switch (previousAvatar.getElement()) {
                            case 'A' -> 'W';
                            case 'W' -> 'E';
                            case 'E' -> 'F';
                            default -> 'A';
                        };
                    }

                    List<UUID> shuffled = new ArrayList<>(futures.keySet());
                    Collections.shuffle(shuffled);
                    for (UUID uuid : shuffled) {
                        Character element = getEligibleElement(futures.get(uuid).getNow(null));
                        if (element == null || !isOnlineAnywhere(uuid)) {
                            continue;
                        }
                        // The element cycle is always respected
                        if (requiredElement != null && element != requiredElement) {
                            continue;
                        }
                        // A manually chosen player skips the last five rule
                        if (target == null && isOneOfLastFiveAvatars(uuid)) {
                            continue;
                        }

                        Avatar avatar = new Avatar(uuid, DateUtils.getRawInGameDate(), "",
                                DateUtils.getRawInRealLifeDate(), "", element);
                        // Removing the null placeholder of the previous reign
                        if (!avatars.isEmpty() && avatars.get(avatars.size() - 1) == null) {
                            avatars.remove(avatars.size() - 1);
                        }
                        setNewAvatar(avatar);
                        DiscordUtils.addAvatarMessageToDiscord(avatar, true);
                        callback.accept(SelectionResult.SELECTED);
                        return;
                    }
                    callback.accept(target != null ? SelectionResult.NOT_ELIGIBLE : SelectionResult.NONE_FOUND);
                }));
    }

    /**
     * Provides the element the player would become the Avatar with.
     *
     * @param bendingPlayer The player's bending data.
     * @return The first character of the player's element, or null if they cannot become the Avatar.
     */
    private static Character getEligibleElement(OfflineBendingPlayer bendingPlayer) {
        if (bendingPlayer == null || bendingPlayer.getElements().isEmpty()
                || bendingPlayer.getElements().contains(Element.CHI)) {
            return null;
        }
        return bendingPlayer.getElements().getFirst().getName().charAt(0);
    }

    /**
     * Determines if the player was one of the last five avatars.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the player was one of the last five avatars.
     */
    private static boolean isOneOfLastFiveAvatars(UUID uuid) {
        List<UUID> lastFive = new ArrayList<>();
        // Cycle from last to first
        for (int i = avatars.size() - 1; i >= 0; i--) {
            if (avatars.get(i) == null) {
                continue;
            }
            lastFive.add(avatars.get(i).getUuid());

            if (lastFive.size() == 5) {
                break;
            }
        }

        return lastFive.contains(uuid);
    }

    /**
     * Updates the Avatar to the new Avatar.
     *
     * @param avatar The new Avatar.
     */
    public static void setNewAvatar(Avatar avatar) {
        avatars.add(avatar);
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(avatar.getUuid());
        // An Avatar on the other server is handled by that server once it receives the update
        Player onlineAvatar = Bukkit.getPlayer(avatar.getUuid());
        if (onlineAvatar != null) {
            PermissionUtils.evaluatePlayerPermissions(onlineAvatar);
        }
        PersistenceUtils.saveAvatars();
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishAvatarUpdate(avatar.getUuid(), true);
        }

        // Despawns the mannequins and then respawns them with the updated values
        refreshAvatarMannequins();

        for (Player player : Bukkit.getOnlinePlayers()) {
            int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
			if (avVol > 0) {
				player.playSound(player, Sound.ENTITY_BREEZE_INHALE, avVol / 100f, 0.1F);
			}
        }
		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishSoundAll("minecraft:entity.breeze.inhale", 1F, 0.1F);
		}

        // Adds a 2-second delay
        new BukkitRunnable() {
            @Override
            public void run() {
                Lang.broadcast("avatar.new_risen", "player", aranarthPlayer.getNickname());
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
					if (avVol > 0) {
						player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, avVol / 100f, 0.8F);
					}
                }
				if (NetworkManager.isActive()) {
					NetworkManager.getInstance().publishSoundAll("minecraft:ui.toast.challenge_complete", 1F, 0.8F);
				}
                PersistenceUtils.saveAvatarBinds();
            }
        }.runTaskLater(AranarthCore.getInstance(), 30);
    }

    /**
     * Removes the current Avatar.
     */
    public static void removeCurrentAvatar() {
        Avatar oldAvatar = getCurrentAvatar();
        if (oldAvatar == null) {
            return;
        }

        oldAvatar.setEndInGame(DateUtils.getRawInGameDate());
        oldAvatar.setEndInRealLife(DateUtils.getRawInRealLifeDate());
        avatars.set(avatars.size() - 1, oldAvatar);
        avatars.add(null);
        // An Avatar on the other server is handled by that server once it receives the update
        if (!isOnlineRemotely(oldAvatar.getUuid())) {
            PermissionUtils.updateAvatarPermissions(oldAvatar.getUuid(), true);
        }
        PersistenceUtils.saveAvatars();
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishAvatarUpdate(oldAvatar.getUuid(), false);
        }

        // Despawns the mannequins and then respawns them with the updated values
        refreshAvatarMannequins();

        DiscordUtils.addAvatarMessageToDiscord(oldAvatar, false);

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(oldAvatar.getUuid());
        Lang.broadcast("avatar.deceased", "player", aranarthPlayer.getNickname());
        for (Player player : Bukkit.getOnlinePlayers()) {
            int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
			if (avVol > 0) {
				player.playSound(player, Sound.ENTITY_WITHER_DEATH, avVol / 100f, 1.5F);
			}
        }
		if (NetworkManager.isActive()) {
			NetworkManager.getInstance().publishSoundAll("minecraft:entity.wither.death", 1F, 1.5F);
		}

        // Adds a 2-second delay
        new BukkitRunnable() {
            @Override
            public void run() {
                Lang.broadcast("avatar.must_be_found");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
					if (avVol > 0) {
						player.playSound(player, Sound.ENTITY_BREEZE_IDLE_AIR, avVol / 100f, 0.4F);
					}
                }
				if (NetworkManager.isActive()) {
					NetworkManager.getInstance().publishSoundAll("minecraft:entity.breeze.idle_air", 1F, 0.4F);
				}
            }
        }.runTaskLater(AranarthCore.getInstance(), 70);
    }

    /**
     * Applies an Avatar change that was made on another server in the network.
     *
     * @param newAvatars  The updated list of all avatars.
     * @param uuid        The UUID of the player whose reign started or ended.
     * @param isNewAvatar Whether the player became the Avatar, or their reign ended.
     */
    public static void applyRemoteUpdate(List<Avatar> newAvatars, UUID uuid, boolean isNewAvatar) {
        avatars = newAvatars;
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (isNewAvatar) {
                PermissionUtils.evaluatePlayerPermissions(player);
            } else {
                PermissionUtils.updateAvatarPermissions(uuid, true);
            }
        }
        refreshAvatarMannequins();
    }

    /**
     * Despawns the Avatar mannequins and then respawns them with the current values.
     */
    public static void refreshAvatarMannequins() {
        World spawn = Bukkit.getWorld("spawn");
        if (spawn == null) {
            return;
        }
        int generation = ++mannequinGeneration;
        despawnAvatarMannequins(spawn);
        respawnAvatarMannequins(spawn, generation);
    }

    /**
     * Despawns all the Avatar mannequins.
     *
     * @param spawn The spawn world.
     */
    private static void despawnAvatarMannequins(World spawn) {
        // Loads the chunks so that mannequins in unloaded chunks are not left behind as duplicates
        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 3; chunkZ++) {
                for (Entity entity : spawn.getChunkAt(chunkX, chunkZ).getEntities()) {
                    if (entity instanceof Mannequin mannequin) {
                        for (Entity nearby : mannequin.getLocation().getNearbyEntities(2.5, 2.5, 2.5)) {
                            // Removes the nametag first
                            if (nearby instanceof TextDisplay display) {
                                display.remove();
                            }
                        }
                        mannequin.remove();
                    } else if (entity instanceof TextDisplay display && display.getScoreboardTags().contains(MANNEQUIN_TAG)) {
                        display.remove();
                    }
                }
            }
        }
    }

    /**
     * Respawns all the Avatar mannequins, the first spot being reserved for the current Avatar.
     *
     * @param spawn      The spawn world.
     * @param generation The refresh that these mannequins belong to.
     */
    private static void respawnAvatarMannequins(World spawn, int generation) {
        if (avatars.isEmpty()) {
            Bukkit.getLogger().info("[AC] There are no avatars");
            return;
        }

        for (int i = 0; i < MANNEQUIN_SPOTS.length; i++) {
            int avatarIndex = avatars.size() - 1 - i;
            if (avatarIndex < 0) {
                break;
            }
            // If there is currently no avatar, its spot is left empty
            Avatar avatar = avatars.get(avatarIndex);
            if (avatar == null) {
                continue;
            }
            double[] spot = MANNEQUIN_SPOTS[i];
            spawnAvatar(new Location(spawn, spot[0], spot[1], spot[2], (float) spot[3], 0), avatar, generation);
        }
    }

    /**
     * Spawns an individual avatar.
     *
     * @param loc        The Location of the mannequin.
     * @param avatar     The avatar being spawned.
     * @param generation The refresh that this mannequin belongs to.
     */
    private static void spawnAvatar(Location loc, Avatar avatar, int generation) {
        UUID uuid = avatar.getUuid();
        AranarthCore plugin = AranarthCore.getInstance();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Fetch and complete the profile (blocking Mojang API call)
            PlayerProfile profile = plugin.getServer().createProfile(uuid);
            boolean completed = profile.complete(true); // Fetches the skin from Mojang

            // Jump back to main thread to spawn the mannequin
            Bukkit.getScheduler().runTask(plugin, () -> {
                // A newer refresh has already despawned and respawned the mannequins
                if (generation != mannequinGeneration || loc.getWorld() == null) {
                    return;
                }

                Mannequin mannequin = (Mannequin) loc.getWorld().spawnEntity(loc, EntityType.MANNEQUIN);
                // Mojang servers unreachable, so the skin is resolved from the UUID later instead
                mannequin.setProfile(completed ? ResolvableProfile.resolvableProfile(profile)
                        : ResolvableProfile.resolvableProfile().uuid(uuid).build());
                mannequin.setInvulnerable(true);
                mannequin.setGravity(false);
                mannequin.setPersistent(true);
                mannequin.setNoPhysics(false);
                mannequin.setImmovable(true);
                mannequin.addScoreboardTag(MANNEQUIN_TAG);

                Location above = mannequin.getLocation().clone().add(0, 2, 0);
                String elementSymbol = getElementSymbol(uuid, avatar);
                String name = ChatUtils.translateToColor(elementSymbol + " &r"
                        + AranarthUtils.getPlayer(uuid).getNickname() + " " + elementSymbol);

                // Always will create a new text display
                mannequin.getWorld().spawn(above, TextDisplay.class, displayEntity -> {
                    displayEntity.setText(name);
                    displayEntity.setBillboard(Display.Billboard.VERTICAL); // Pivots only around the vertical axis
                    displayEntity.addScoreboardTag(MANNEQUIN_TAG);
                });
            });
        });
    }

    /**
     * Provides the symbol of the current element of the input player.
     *
     * @param uuid   The UUID of the player.
     * @param avatar The associated avatar iteration if the input is or was an avatar.
     * @return The symbol of the current element of the input player.
     */
    public static String getElementSymbol(UUID uuid, Avatar avatar) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        OfflineBendingPlayer bendingPlayer = new OfflineBendingPlayer(player);

        Element element = Element.AIR;
        if (avatar != null) {
            char elementCharacter = avatar.getElement();
            if (elementCharacter == 'W') {
                element = Element.WATER;
            } else if (elementCharacter == 'E') {
                element = Element.EARTH;
            } else if (elementCharacter == 'F') {
                element = Element.FIRE;
            }
        } else {
            element = bendingPlayer.getElements().getFirst();
        }

        String elementAsString = "";
        if (element == Element.WATER) {
            elementAsString = "&b水";
        } else if (element == Element.EARTH) {
            elementAsString = "&a土";
        } else if (element == Element.FIRE) {
            elementAsString = "&c火";
        } else {
            elementAsString = "&7気";
        }
        return elementAsString;
    }

}
