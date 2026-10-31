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
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
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
import java.util.logging.Level;


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

    // TODO Remove AUTO_SELECT_TIME, AUTO_SELECT_WINDOW and their checks once this time has passed. Successors must
    //  stay automatic, and an empty list must then never auto-select, as it means the list failed to load
    // The one-time automatic selection of the first Avatar (Survival only)
    private static final ZonedDateTime AUTO_SELECT_TIME = ZonedDateTime.of(2026, 10, 31, 13, 0, 0, 0, ZoneId.of("America/New_York"));
    // Stops the automatic selection from ever re-running later if the avatar list somehow failed to load
    private static final Duration AUTO_SELECT_WINDOW = Duration.ofDays(7);
    private static final Duration INACTIVITY_LIMIT = Duration.ofDays(7);
    // Lets the end of a reign play out before the next Avatar rises
    private static final Duration SUCCESSOR_DELAY = Duration.ofMinutes(1);
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
    private static long lastReignEndedAt = 0;
    private static long lastAvatarChangeAt = 0;
    // Stops a selection that keeps failing the same way from repeating in the logs every minute
    private static String lastSelectionSummary = null;
    private static boolean isAutoSelectSkipLogged = false;
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
     * Confirms if the player is the current reigning Avatar.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the player is the current reigning Avatar.
     */
    public static boolean isCurrentAvatar(UUID uuid) {
        Avatar avatar = getCurrentAvatar();
        return avatar != null && avatar.getUuid().equals(uuid);
    }

    /**
     * Provides the most recent Avatar whose reign has ended.
     *
     * @return The most recent Avatar whose reign has ended, or null if there is none.
     */
    public static Avatar getPreviousAvatar() {
        for (int i = avatars.size() - 1; i >= 0; i--) {
            Avatar avatar = avatars.get(i);
            if (avatar != null && avatar != getCurrentAvatar()) {
                return avatar;
            }
        }
        return null;
    }

    /**
     * Logs a message about the Avatar system.
     *
     * @param message The message.
     */
    public static void logInfo(String message) {
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Avatar] " + message);
    }

    /**
     * Logs a problem with the Avatar system.
     *
     * @param message The message.
     */
    public static void logWarning(String message) {
        Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Avatar] " + message);
    }

    /**
     * Logs a failure of the Avatar system along with its stack trace.
     *
     * @param message The message.
     * @param ex      The cause of the failure.
     */
    public static void logError(String message, Throwable ex) {
        Bukkit.getLogger().log(Level.SEVERE, AranarthCore.LOG_PREFIX + "[Avatar] " + message, ex);
    }

    /**
     * Runs one step of a reign starting or ending. A step that fails is logged and the remaining steps
     * still run, so that a Discord outage for example cannot stop the reign from being saved or announced.
     *
     * @param step The name of the step.
     * @param task The step.
     */
    private static void runStep(String step, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            logError("FAILED step: " + step, e);
        }
    }

    /**
     * Provides the player's name and UUID for the logs.
     *
     * @param uuid The player's UUID.
     * @return The player's name and UUID.
     */
    public static String describe(UUID uuid) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
        String name = aranarthPlayer != null ? aranarthPlayer.getUsername() : Bukkit.getOfflinePlayer(uuid).getName();
        return name + " (" + uuid + ")";
    }

    /**
     * Provides the state of the avatar list for the logs.
     *
     * @return The state of the avatar list.
     */
    public static String describeState() {
        Avatar current = getCurrentAvatar();
        Avatar previous = getPreviousAvatar();
        return "entries=" + avatars.size()
                + ", current=" + (current == null ? "none" : describe(current.getUuid()) + "/" + current.getElement())
                + ", previous=" + (previous == null ? "none" : describe(previous.getUuid()) + "/" + previous.getElement());
    }

    /**
     * Provides the names of the elements for the logs, as their toString() includes colour codes.
     *
     * @param elements The elements.
     * @return The names of the elements.
     */
    public static List<String> elementNames(Collection<? extends Element> elements) {
        List<String> names = new ArrayList<>();
        for (Element element : elements) {
            names.add(element.getName());
        }
        return names;
    }

    /**
     * Provides where the player currently is for the logs.
     *
     * @param uuid The player's UUID.
     * @return Where the player currently is.
     */
    private static String describeLocation(UUID uuid) {
        if (Bukkit.getPlayer(uuid) != null) {
            return "online here";
        }
        return isOnlineRemotely(uuid) ? "online on the other server" : "offline";
    }

    /**
     * Logs the state of the Avatar system once the server has started.
     */
    public static void logStartupStatus() {
        ZonedDateTime now = ZonedDateTime.now(AUTO_SELECT_TIME.getZone());
        boolean isSmp = AranarthCore.isSmpServer();
        logInfo("Startup status: " + describeState() + ", scheduledChecks=" + !isSmp
                + ", publicServer=" + AranarthCore.isPublicServer()
                + ", database=" + DatabaseManager.isActive() + ", network=" + NetworkManager.isActive());
        logInfo("Clock check: now=" + now + ", autoSelectTime=" + AUTO_SELECT_TIME
                + ", minutesUntil=" + Duration.between(now, AUTO_SELECT_TIME).toMinutes());
        if (!isSmp && !NetworkManager.isActive()) {
            logWarning("The network is not active, so players on the other server cannot be selected");
        }
        if (!AranarthCore.isPublicServer()) {
            logWarning("This is a test server, so isSmpServer() is false and any test SMP runs the scheduled checks too");
        }
        if (!isSmp && now.isBefore(AUTO_SELECT_TIME) && getCurrentAvatar() != null) {
            logWarning("The automatic selection will be skipped, as there is already a reigning Avatar");
        }
    }

    /**
     * Compares the avatar list on this server with the list in MySQL and logs any difference, as a server
     * that missed an update would overwrite the correct list on its next periodic save.
     */
    public static void verifyAgainstDatabase() {
        if (!DatabaseManager.isActive() || System.currentTimeMillis() - lastAvatarChangeAt < 30_000) {
            return;
        }
        JsonArray local = PersistenceUtils.avatarsToJson();
        String localState = describeState();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            try {
                String json = DatabaseManager.getInstance().loadAvatars();
                if (json == null || json.isEmpty()) {
                    if (!local.isEmpty()) {
                        logWarning("DESYNC: this server has avatars but MySQL has none. Local: " + localState);
                    }
                    return;
                }
                JsonArray stored = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("avatars");
                if (!local.equals(stored)) {
                    logWarning("DESYNC: this server does not match MySQL. Local: " + localState
                            + " | MySQL entries=" + stored.size() + ", last=" + (stored.isEmpty() ? "none" : stored.get(stored.size() - 1)));
                }
            } catch (Exception e) {
                logError("Failed to compare the avatar list with MySQL", e);
            }
        });
    }

    /**
     * Confirms that the player ended up with the correct elements, sub-elements, permissions and binds, once
     * everything that changes them has run. Never changes anything, it only logs the result.
     *
     * @param uuid          The player's UUID.
     * @param reason        What caused this check.
     * @param formerElement The element the player should have if their reign ended, or null if they are the Avatar.
     */
    public static void scheduleStateCheck(UUID uuid, String reason, Character formerElement) {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                logInfo("State check (" + reason + ") skipped for " + describe(uuid) + ", they are " + describeLocation(uuid));
                return;
            }
            // The reign changed again during the delay, and that change runs its own check
            Avatar current = getCurrentAvatar();
            boolean isAvatar = current != null && current.getUuid().equals(uuid);
            if (isAvatar != (formerElement == null)) {
                logInfo("State check (" + reason + ") skipped for " + describe(uuid) + ", the reign has since changed");
                return;
            }
            BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
            if (bendingPlayer == null) {
                logWarning("State check (" + reason + ") FAILED for " + describe(uuid) + ": their bending data never loaded");
                return;
            }

            List<String> problems = new ArrayList<>();
            boolean isInArena = player.getWorld().getName().equalsIgnoreCase("arena");
            if (formerElement == null) {
                for (Element element : List.of(Element.AIR, Element.WATER, Element.EARTH, Element.FIRE)) {
                    if (!bendingPlayer.getElements().contains(element)) {
                        problems.add("missing element " + element.getName());
                    }
                }
                for (Element.SubElement sub : Element.SubElement.getSubElements()) {
                    if (sub != Element.SubElement.BLOOD && sub != Element.SubElement.FLIGHT
                            && sub != Element.SubElement.BLUE_FIRE && !bendingPlayer.hasSubElement(sub)) {
                        problems.add("missing sub-element " + sub.getName());
                    }
                }
                if (bendingPlayer.hasSubElement(Element.SubElement.BLOOD) == isInArena) {
                    problems.add((isInArena ? "has" : "missing") + " Blood");
                }
                if (isInArena && bendingPlayer.hasSubElement(Element.SubElement.FLIGHT)) {
                    problems.add("has Flight in the arena");
                }
                if (!player.hasPermission("bending.avatar")) {
                    problems.add("missing permission bending.avatar");
                }
                if (player.hasPermission("bending.command.rechoose")) {
                    problems.add("can still use rechoose");
                }
            } else {
                List<Element> elements = bendingPlayer.getElements();
                if (elements.size() != 1 || elements.getFirst().getName().charAt(0) != formerElement) {
                    problems.add("expected only element " + formerElement);
                }
                if (player.hasPermission("bending.avatar")) {
                    problems.add("still has permission bending.avatar");
                }
                for (String ability : bendingPlayer.getAbilities().values()) {
                    if (CoreAbility.getAbility(ability) instanceof AvatarAbility) {
                        problems.add("still has " + ability + " bound");
                    }
                }
            }

            String state = "elements=" + elementNames(bendingPlayer.getElements())
                    + ", subElements=" + elementNames(bendingPlayer.getSubElements())
                    + ", binds=" + bendingPlayer.getAbilities() + ", world=" + player.getWorld().getName();
            if (problems.isEmpty()) {
                logInfo("State check (" + reason + ") OK for " + describe(uuid) + ": " + state);
            } else {
                logWarning("State check (" + reason + ") FOUND PROBLEMS for " + describe(uuid) + ": " + problems + " | " + state);
            }
        }, 140L);
    }

    /**
     * Logs the time remaining until the automatic selection, more often as it gets closer.
     *
     * @param now The current time.
     */
    private static void logCountdown(ZonedDateTime now) {
        long minutes = Duration.between(now, AUTO_SELECT_TIME).toMinutes();
        if (minutes <= 5 || minutes % 60 == 0 || (minutes <= 60 && minutes % 10 == 0)) {
            logInfo("Automatic selection in " + minutes + " minute(s) at " + AUTO_SELECT_TIME + ": " + describeState()
                    + ", onlineHere=" + Bukkit.getOnlinePlayers().size()
                    + ", onlineRemote=" + (NetworkManager.isActive() ? NetworkManager.getInstance().getRemoteRoster().size() : "network inactive"));
        }
    }

    /**
     * Runs the scheduled Avatar logic. Must only be called on the Survival server.
     * Selects a new Avatar once the automatic selection time has passed if there currently is none, and
     * ends the reign of an Avatar that has been inactive on every server for too long.
     */
    public static void runScheduledChecks() {
        ZonedDateTime now = ZonedDateTime.now(AUTO_SELECT_TIME.getZone());

        // A new Avatar is selected automatically, retrying each check until someone eligible is online
        Avatar avatar = getCurrentAvatar();
        if (avatar == null) {
            if (now.isBefore(AUTO_SELECT_TIME)) {
                logCountdown(now);
            } else if (avatars.isEmpty() && !now.isBefore(AUTO_SELECT_TIME.plus(AUTO_SELECT_WINDOW))) {
                logSelection("Automatic selection refused, the list is empty after the selection window: was it loaded?");
            } else if (System.currentTimeMillis() - lastReignEndedAt < SUCCESSOR_DELAY.toMillis()) {
                logInfo("Waiting for the end of the last reign to play out before selecting a successor");
            } else {
                selectAvatar(null, result -> {
                });
            }
            return;
        }

        if (!isAutoSelectSkipLogged && !now.isBefore(AUTO_SELECT_TIME)) {
            isAutoSelectSkipLogged = true;
            logInfo("Automatic selection not needed: " + describeState());
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
            if (lastSeen <= 0) {
                logWarning("The last seen time of Avatar " + uuid + " is unknown, so their inactivity cannot be checked");
                return;
            }
            if (System.currentTimeMillis() - lastSeen < INACTIVITY_LIMIT.toMillis()) {
                return;
            }
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                // The reign may have changed while the last seen time was being read
                Avatar current = getCurrentAvatar();
                if (current != null && current.getUuid().equals(uuid) && !isOnlineAnywhere(uuid)) {
                    removeCurrentAvatar("inactive for 7 days");
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
            finishSelection(SelectionResult.ALREADY_REIGNING, "current=" + describe(getCurrentAvatar().getUuid()), callback);
            return;
        }
        if (isSelecting) {
            finishSelection(SelectionResult.BUSY, "a selection is already running", callback);
            return;
        }

        List<UUID> candidates = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (target != null) {
            if (!isOnlineAnywhere(target)) {
                finishSelection(SelectionResult.NOT_ELIGIBLE, describe(target) + " is not online", callback);
                return;
            }
            if (!isLoadable(target)) {
                finishSelection(SelectionResult.NOT_ELIGIBLE, describe(target) + " has never been on this server, run this on the server they are on", callback);
                return;
            }
            candidates.add(target);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                if (aranarthPlayer == null || !aranarthPlayer.isVanished()) {
                    candidates.add(player.getUniqueId());
                } else {
                    rejected.add(player.getName() + ": vanished");
                }
            }
            if (NetworkManager.isActive()) {
                for (NetworkPlayer networkPlayer : NetworkManager.getInstance().getRemoteRoster().values()) {
                    if (networkPlayer.isVanished()) {
                        rejected.add(networkPlayer.getUsername() + ": vanished");
                    } else if (!isLoadable(networkPlayer.getUuid())) {
                        rejected.add(networkPlayer.getUsername() + ": has never been on this server");
                    } else if (!candidates.contains(networkPlayer.getUuid())) {
                        candidates.add(networkPlayer.getUuid());
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            finishSelection(SelectionResult.NONE_FOUND, "no candidates, rejected=" + sorted(rejected), callback);
            return;
        }

        // Players on the other server are not loaded here, so their bending data is read from the shared database
        isSelecting = true;
        Map<UUID, CompletableFuture<OfflineBendingPlayer>> futures = new LinkedHashMap<>();
        for (UUID uuid : candidates) {
            // ProjectKorra keeps a copy from when they were last here, which may be from before they changed element
            if (isOnlineRemotely(uuid)) {
                BendingPlayer.getOfflinePlayers().remove(uuid);
            }
            // Copied so that the timeout cannot fail the future that ProjectKorra itself is waiting on
            futures.put(uuid, BendingPlayer.getOrLoadOfflineAsync(Bukkit.getOfflinePlayer(uuid))
                    .copy()
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        logWarning("Failed to load the bending data of candidate " + uuid + ": " + ex);
                        return null;
                    }));
        }

        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).whenComplete((ignored, ex) ->
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                    isSelecting = false;
                    // Another selection may have completed in the meantime
                    if (getCurrentAvatar() != null) {
                        finishSelection(SelectionResult.ALREADY_REIGNING, "selected while the candidates were loading", callback);
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
                        OfflineBendingPlayer bendingPlayer = futures.get(uuid).getNow(null);
                        Character element = getEligibleElement(bendingPlayer);
                        String name = describe(uuid);
                        if (element == null) {
                            rejected.add(name + ": no eligible element, data="
                                    + (bendingPlayer == null ? "not loaded" : elementNames(bendingPlayer.getElements()).toString()));
                            continue;
                        }
                        if (!isOnlineAnywhere(uuid)) {
                            rejected.add(name + ": went offline");
                            continue;
                        }
                        // The element cycle is always respected
                        if (requiredElement != null && !requiredElement.equals(element)) {
                            rejected.add(name + ": element " + element + " is not next in the cycle");
                            continue;
                        }
                        // A manually chosen player skips the last five rule
                        if (target == null && isOneOfLastFiveAvatars(uuid)) {
                            rejected.add(name + ": one of the last five Avatars");
                            continue;
                        }

                        Avatar avatar = new Avatar(uuid, DateUtils.getRawInGameDate(), "",
                                DateUtils.getRawInRealLifeDate(), "", element);
                        // Removing the null placeholder of the previous reign
                        if (!avatars.isEmpty() && avatars.get(avatars.size() - 1) == null) {
                            avatars.remove(avatars.size() - 1);
                        }
                        logInfo("Selected " + name + " with element " + element + ", requiredElement=" + requiredElement
                                + ", candidates=" + shuffled.size() + ", rejected=" + sorted(rejected));
                        setNewAvatar(avatar);
                        runStep("posting the new Avatar to Discord", () -> DiscordUtils.addAvatarMessageToDiscord(avatar, true));
                        callback.accept(SelectionResult.SELECTED);
                        return;
                    }
                    finishSelection(target != null ? SelectionResult.NOT_ELIGIBLE : SelectionResult.NONE_FOUND,
                            "requiredElement=" + requiredElement + ", candidates=" + shuffled.size()
                                    + ", rejected=" + sorted(rejected), callback);
                }));
    }

    /**
     * Logs the result of a selection that did not select anyone and passes it on to the callback.
     *
     * @param result   The result of the selection.
     * @param details  The reason for the result.
     * @param callback Called with the result of the selection.
     */
    private static void finishSelection(SelectionResult result, String details, Consumer<SelectionResult> callback) {
        logSelection("Selection result " + result + ": " + details);
        callback.accept(result);
    }

    /**
     * Logs the outcome of a selection, unless it is identical to the last one.
     *
     * @param summary The outcome of the selection.
     */
    private static void logSelection(String summary) {
        if (!summary.equals(lastSelectionSummary)) {
            lastSelectionSummary = summary;
            logInfo(summary + " (identical repeats are not logged)");
        }
    }

    /**
     * Provides the list in a stable order, so that the same outcome always produces the same log message.
     *
     * @param list The list.
     * @return The sorted list.
     */
    private static List<String> sorted(List<String> list) {
        List<String> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Determines if ProjectKorra can load the player's data on this server. Its loader fails when this server
     * has no name for the player, which leaves them stuck as loading so that they could never bend here.
     *
     * @param uuid The player's UUID.
     * @return Confirmation if the player's data can be loaded.
     */
    public static boolean isLoadable(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName() != null;
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
        char element = bendingPlayer.getElements().getFirst().getName().charAt(0);
        return "AWEF".indexOf(element) >= 0 ? element : null;
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
        UUID uuid = avatar.getUuid();
        avatars.add(avatar);
        lastAvatarChangeAt = System.currentTimeMillis();
        lastSelectionSummary = null;
        // The last seen time of the new Avatar is saved on the next check
        lastSeenSavedAt = 0;
        logInfo("REIGN STARTED: " + describe(uuid) + ", element=" + avatar.getElement() + ", they are "
                + describeLocation(uuid) + ", " + describeState());

        // An Avatar on the other server is handled by that server once it receives the update
        Player onlineAvatar = Bukkit.getPlayer(uuid);
        if (onlineAvatar != null) {
            runStep("applying the new Avatar's permissions", () -> PermissionUtils.evaluatePlayerPermissions(onlineAvatar));
        }
        runStep("saving the avatars", PersistenceUtils::saveAvatars);
        if (NetworkManager.isActive()) {
            runStep("sending the new Avatar to the other server", () -> NetworkManager.getInstance().publishAvatarUpdate(uuid, true));
        } else {
            logWarning("The network is not active, so the other server does not know about the new Avatar");
        }

        // Despawns the mannequins and then respawns them with the updated values
        runStep("refreshing the mannequins", AvatarUtils::refreshAvatarMannequins);
        runStep("updating the tab list", AranarthUtils::updateTab);

        runStep("playing the new Avatar sound", () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
				if (avVol > 0) {
					player.playSound(player, Sound.ENTITY_BREEZE_INHALE, avVol / 100f, 0.1F);
				}
            }
			if (NetworkManager.isActive()) {
				NetworkManager.getInstance().publishSoundAll("minecraft:entity.breeze.inhale", 1F, 0.1F);
			}
        });

        // Adds a 2-second delay
        new BukkitRunnable() {
            @Override
            public void run() {
                runStep("announcing the new Avatar", () -> {
                    Lang.broadcast("avatar.new_risen", "player", AranarthUtils.getNickname(Bukkit.getOfflinePlayer(uuid)));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
						if (avVol > 0) {
							player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, avVol / 100f, 0.8F);
						}
                    }
					if (NetworkManager.isActive()) {
						NetworkManager.getInstance().publishSoundAll("minecraft:ui.toast.challenge_complete", 1F, 0.8F);
					}
                });
                runStep("saving the new Avatar's binds", PersistenceUtils::saveAvatarBinds);
            }
        }.runTaskLater(AranarthCore.getInstance(), 30);

        scheduleStateCheck(uuid, "reign started", null);
    }

    /**
     * Removes the current Avatar.
     *
     * @param reason Why the reign ended, for the logs.
     */
    public static void removeCurrentAvatar(String reason) {
        Avatar oldAvatar = getCurrentAvatar();
        if (oldAvatar == null) {
            logWarning("Asked to end the reign (" + reason + ") but there is no current Avatar");
            return;
        }

        UUID uuid = oldAvatar.getUuid();
        oldAvatar.setEndInGame(DateUtils.getRawInGameDate());
        oldAvatar.setEndInRealLife(DateUtils.getRawInRealLifeDate());
        avatars.set(avatars.size() - 1, oldAvatar);
        avatars.add(null);
        lastReignEndedAt = System.currentTimeMillis();
        lastAvatarChangeAt = lastReignEndedAt;
        lastSelectionSummary = null;
        logInfo("REIGN ENDED: " + describe(uuid) + ", reason=" + reason + ", element=" + oldAvatar.getElement()
                + ", they are " + describeLocation(uuid) + ", " + describeState());

        // An Avatar on the other server is handled by that server once it receives the update
        if (!isOnlineRemotely(uuid)) {
            runStep("restoring the old Avatar's element", () -> PermissionUtils.updateAvatarPermissions(uuid, true));
        }
        runStep("saving the avatars", PersistenceUtils::saveAvatars);
        if (NetworkManager.isActive()) {
            runStep("sending the end of the reign to the other server", () -> NetworkManager.getInstance().publishAvatarUpdate(uuid, false));
        } else {
            logWarning("The network is not active, so the other server does not know the reign ended");
        }

        // Despawns the mannequins and then respawns them with the updated values
        runStep("refreshing the mannequins", AvatarUtils::refreshAvatarMannequins);
        runStep("updating the tab list", AranarthUtils::updateTab);

        runStep("posting the end of the reign to Discord", () -> DiscordUtils.addAvatarMessageToDiscord(oldAvatar, false));

        runStep("announcing the end of the reign", () -> {
            Lang.broadcast("avatar.deceased", "player", AranarthUtils.getNickname(Bukkit.getOfflinePlayer(uuid)));
            for (Player player : Bukkit.getOnlinePlayers()) {
                int avVol = AranarthUtils.getPlayer(player.getUniqueId()).getAvatarSoundVolume();
				if (avVol > 0) {
					player.playSound(player, Sound.ENTITY_WITHER_DEATH, avVol / 100f, 1.5F);
				}
            }
			if (NetworkManager.isActive()) {
				NetworkManager.getInstance().publishSoundAll("minecraft:entity.wither.death", 1F, 1.5F);
			}
        });
        scheduleStateCheck(uuid, "reign ended: " + reason, oldAvatar.getElement());

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
        Avatar localAvatar = getCurrentAvatar();
        if (isNewAvatar && localAvatar != null && !localAvatar.getUuid().equals(uuid)) {
            logWarning("TWO AVATARS: this server selected " + describe(localAvatar.getUuid()) + " while the other server selected "
                    + describe(uuid) + ". The other server's choice is being applied here, check both servers with /avatar");
        }
        if (newAvatars.size() < avatars.size()) {
            logWarning("The other server sent a shorter list (" + newAvatars.size() + ") than this server has (" + avatars.size() + ")");
        }

        avatars = newAvatars;
        lastAvatarChangeAt = System.currentTimeMillis();
        lastSelectionSummary = null;
        if (!isNewAvatar) {
            lastReignEndedAt = lastAvatarChangeAt;
        }
        logInfo("Update from the other server: " + (isNewAvatar ? "reign started" : "reign ended") + " for "
                + describe(uuid) + ", they are " + describeLocation(uuid) + ", " + describeState());

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            if (isNewAvatar) {
                runStep("applying the new Avatar's permissions", () -> PermissionUtils.evaluatePlayerPermissions(player));
                scheduleStateCheck(uuid, "reign started on the other server", null);
            } else {
                runStep("restoring the old Avatar's element", () -> PermissionUtils.updateAvatarPermissions(uuid, true));
                Avatar previousAvatar = getPreviousAvatar();
                if (previousAvatar != null && previousAvatar.getUuid().equals(uuid)) {
                    scheduleStateCheck(uuid, "reign ended on the other server", previousAvatar.getElement());
                }
            }
        }
        runStep("refreshing the mannequins", AvatarUtils::refreshAvatarMannequins);
        runStep("updating the tab list", AranarthUtils::updateTab);
    }

    /**
     * Despawns the Avatar mannequins and then respawns them with the current values.
     */
    public static void refreshAvatarMannequins() {
        World spawn = Bukkit.getWorld("spawn");
        if (spawn == null) {
            logInfo("Mannequins not refreshed, this server has no spawn world");
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
        int removed = 0;
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
                        removed++;
                    } else if (entity instanceof TextDisplay display && display.getScoreboardTags().contains(MANNEQUIN_TAG)) {
                        display.remove();
                    }
                }
            }
        }
        logInfo("Despawned " + removed + " mannequin(s)");
    }

    /**
     * Respawns all the Avatar mannequins, the first spot being reserved for the current Avatar.
     *
     * @param spawn      The spawn world.
     * @param generation The refresh that these mannequins belong to.
     */
    private static void respawnAvatarMannequins(World spawn, int generation) {
        if (avatars.isEmpty()) {
            logInfo("There are no avatars, so no mannequins are spawned");
            return;
        }

        int queued = 0;
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
            queued++;
        }
        logInfo("Queued " + queued + " mannequin(s) out of " + avatars.size() + " list entries, generation=" + generation);
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
            boolean isProfileComplete;
            try {
                isProfileComplete = profile.complete(true); // Fetches the skin from Mojang
            } catch (Exception e) {
                logWarning("The skin lookup failed for the mannequin of " + uuid + ": " + e);
                isProfileComplete = false;
            }
            boolean completed = isProfileComplete;

            // Jump back to main thread to spawn the mannequin
            Bukkit.getScheduler().runTask(plugin, () -> {
                // A newer refresh has already despawned and respawned the mannequins
                if (generation != mannequinGeneration || loc.getWorld() == null) {
                    return;
                }

                runStep("spawning the mannequin of " + describe(uuid), () -> {
                    // The Mojang lookup can outlast the chunks staying loaded after the despawn
                    loc.getChunk().load();
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
                            + AranarthUtils.getNickname(Bukkit.getOfflinePlayer(uuid)) + " " + elementSymbol);

                    // Always will create a new text display
                    mannequin.getWorld().spawn(above, TextDisplay.class, displayEntity -> {
                        displayEntity.setText(name);
                        displayEntity.setBillboard(Display.Billboard.VERTICAL); // Pivots only around the vertical axis
                        displayEntity.addScoreboardTag(MANNEQUIN_TAG);
                    });
                    logInfo("Spawned the mannequin of " + describe(uuid) + " at " + loc.getBlockX() + ", " + loc.getBlockY()
                            + ", " + loc.getBlockZ() + (completed ? "" : " without a Mojang skin lookup"));
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
