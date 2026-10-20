package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.popcraft.chunky.api.ChunkyAPI;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Handles the yearly reset of the resource worlds.
 */
public class ResourceWorldUtils {

    private static final String LOG = AranarthCore.LOG_PREFIX + "[ResourceReset] ";
    private static final String[] WORLD_NAMES = {"resource", "resource_nether", "resource_the_end"};
    private static final World.Environment[] ENVIRONMENTS = {
            World.Environment.NORMAL,
            World.Environment.NETHER,
            World.Environment.THE_END
    };
    private static final int BORDER_SIZE = 5000;
    private static final long STEP_DELAY_TICKS = 100L;
    private static final long POLL_TICKS = 20L;
    private static final long PROGRESS_LOG_INTERVAL_MS = 60_000L;
    private static final long CHUNKY_PAUSE_TIMEOUT_MS = 30_000L;
    private static final long LOCKED_MESSAGE_COOLDOWN_MS = 3_000L;

    private static final Set<String> lockedWorlds = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> lastLockedMessage = new HashMap<>();
    private static volatile boolean isRemoteResourceLocked = false;
    private static long resetStartTime = 0L;

    /**
     * Whether the resource world reset is still in progress on this server.
     */
    public static boolean isResetting() {
        return !lockedWorlds.isEmpty();
    }

    /**
     * Whether the given world is a resource world that cannot be entered yet.
     */
    public static boolean isLocked(String worldName) {
        if (worldName.equals("resource") && isRemoteResourceLocked) {
            return true;
        }
        return lockedWorlds.contains(worldName);
    }

    /**
     * Whether the given world cannot be entered by the player yet.
     */
    public static boolean isLockedFor(Player player, String worldName) {
        if (!isLocked(worldName)) {
            return false;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        return aranarthPlayer == null || !aranarthPlayer.isInAdminMode();
    }

    /**
     * Informs the player that the resource world is still being generated.
     */
    public static void sendLockedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastLockedMessage.get(player.getUniqueId());
        if (last != null && now - last < LOCKED_MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastLockedMessage.put(player.getUniqueId(), now);
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "resource.generating")));
    }

    /**
     * Updates the SMP server's copy of whether Survival's resource world is locked.
     */
    public static void setRemoteResourceLocked(boolean isLocked) {
        isRemoteResourceLocked = isLocked;
        Bukkit.getLogger().info(LOG + "Survival reported the resource world as " + (isLocked ? "locked" : "unlocked"));
    }

    /**
     * Starts the reset of all resource worlds.
     */
    public static void startReset() {
        if (isResetting()) {
            Bukkit.getLogger().warning(LOG + "A reset is already in progress for " + lockedWorlds + " - skipping");
            return;
        }
        resetStartTime = System.currentTimeMillis();
        Bukkit.getLogger().info(LOG + "Starting resource world reset | " + getPerformance());
        lockedWorlds.addAll(Arrays.asList(WORLD_NAMES));
        publishLock(true);

        evacuatePlayers();
        // Wait a tick so that the teleports and kicks have fully left the worlds before unloading them
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), ResourceWorldUtils::unloadWorlds, 1L);
    }

    /**
     * Teleports every player in a resource world to Spawn, kicking them if the teleport fails.
     */
    private static void evacuatePlayers() {
        Location spawnLocation = new Location(Bukkit.getWorld("spawn"), 0.5, 101, 0.5, 180, 0);
        int teleported = 0;
        int kicked = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!lockedWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            player.leaveVehicle();
            if (!player.isDead() && player.teleport(spawnLocation)) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "teleport.resource_reset")));
                teleported++;
            } else {
                player.kick(LegacyComponentSerializer.legacySection().deserialize(
                        ChatUtils.translateToColor(Lang.getFor(player, "resource.reset_kick"))));
                kicked++;
                Bukkit.getLogger().warning(LOG + "Could not teleport " + player.getName() + " out of "
                        + player.getWorld().getName() + " - kicked them instead");
            }
        }
        Bukkit.getLogger().info(LOG + "Evacuated players | teleported=" + teleported + " kicked=" + kicked);
    }

    /**
     * Unloads each resource world, then deletes their folders asynchronously.
     */
    private static void unloadWorlds() {
        ChunkyAPI chunky = getChunky();
        List<Integer> worldsToCreate = new ArrayList<>();
        List<File> foldersToDelete = new ArrayList<>();

        for (int i = 0; i < WORLD_NAMES.length; i++) {
            String worldName = WORLD_NAMES[i];
            if (chunky != null && chunky.isRunning(worldName)) {
                chunky.cancelTask(worldName);
                Bukkit.getLogger().info(LOG + "Cancelled the existing Chunky task for " + worldName);
            }

            World world = Bukkit.getWorld(worldName);
            File worldFolder;
            if (world != null) {
                worldFolder = world.getWorldFolder();
                int remainingPlayers = world.getPlayers().size();
                long start = System.nanoTime();
                boolean isUnloaded = Bukkit.unloadWorld(world, false);
                if (!isUnloaded) {
                    // Never delete the files of a world that is still loaded, or it will be corrupted
                    Bukkit.getLogger().severe(LOG + "Failed to unload " + worldName + " (players remaining: "
                            + remainingPlayers + ") - this world will not be reset");
                    lockedWorlds.remove(worldName);
                    continue;
                }
                Bukkit.getLogger().info(LOG + "Unloaded " + worldName + " in " + msSince(start) + "ms");
            } else {
                worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                Bukkit.getLogger().info(LOG + worldName + " was not loaded");
            }
            foldersToDelete.add(worldFolder);
            worldsToCreate.add(i);
        }

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            for (File folder : foldersToDelete) {
                if (!folder.exists()) {
                    continue;
                }
                long start = System.nanoTime();
                int[] deleted = new int[1];
                deleteDirectory(folder, deleted);
                Bukkit.getLogger().info(LOG + "Deleted " + folder.getName() + " (" + deleted[0] + " files) in "
                        + msSince(start) + "ms" + (folder.exists() ? " - WARNING: the folder still exists" : ""));
            }
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> createNextWorld(worldsToCreate, 0));
        });
    }

    /**
     * Creates the next resource world in the queue, then pre-generates it before moving on to the following one.
     */
    private static void createNextWorld(List<Integer> worldsToCreate, int position) {
        if (position >= worldsToCreate.size()) {
            finishReset();
            return;
        }
        int index = worldsToCreate.get(position);
        String worldName = WORLD_NAMES[index];

        pauseOtherChunkyTasks(pausedWorlds -> {
            Bukkit.getLogger().info(LOG + "Creating " + worldName + " | " + getPerformance());
            long start = System.nanoTime();
            World world = null;
            try {
                WorldCreator wc = new WorldCreator(worldName);
                wc.environment(ENVIRONMENTS[index]);
                wc.type(WorldType.NORMAL);
                world = wc.createWorld();
            } catch (Exception e) {
                Bukkit.getLogger().severe(LOG + "Exception while creating " + worldName + ": " + e.getMessage());
            }
            Bukkit.getLogger().info(LOG + "Created " + worldName + " in " + msSince(start) + "ms"
                    + (world == null ? " - FAILED" : ""));

            resumeChunkyTasks(pausedWorlds);

            if (world == null) {
                unlockWorld(worldName);
                scheduleNextWorld(worldsToCreate, position);
                return;
            }

            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            world.setGameRule(GameRules.LOCATOR_BAR, false);
            world.getWorldBorder().setCenter(0, 0);
            world.getWorldBorder().setSize(BORDER_SIZE);
            pregenerateWorld(worldName, () -> scheduleNextWorld(worldsToCreate, position));
        });
    }

    private static void scheduleNextWorld(List<Integer> worldsToCreate, int position) {
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> createNextWorld(worldsToCreate, position + 1), STEP_DELAY_TICKS);
    }

    /**
     * Pre-generates the world border area with Chunky, unlocking the world once it is complete.
     */
    private static void pregenerateWorld(String worldName, Runnable onComplete) {
        ChunkyAPI chunky = getChunky();
        double radius = BORDER_SIZE / 2.0;
        if (chunky == null || !chunky.startTask(worldName, "square", 0, 0, radius, radius, "region")) {
            Bukkit.getLogger().warning(LOG + "Chunky pre-generation could not be started for " + worldName
                    + " - unlocking it without pre-generation");
            unlockWorld(worldName);
            onComplete.run();
            return;
        }
        Bukkit.getLogger().info(LOG + "Chunky pre-generation started for " + worldName + " (radius " + (int) radius + ")");
        long start = System.currentTimeMillis();

        new BukkitRunnable() {
            long lastProgressLog = start;

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (chunky.isRunning(worldName)) {
                    if (now - lastProgressLog >= PROGRESS_LOG_INTERVAL_MS) {
                        lastProgressLog = now;
                        Bukkit.getLogger().info(LOG + "Pre-generating " + worldName + " for "
                                + ((now - start) / 1000) + "s | " + getPerformance());
                    }
                    return;
                }
                cancel();
                Bukkit.getLogger().info(LOG + "Chunky pre-generation finished for " + worldName + " in "
                        + ((now - start) / 1000) + "s | " + getPerformance());
                unlockWorld(worldName);
                onComplete.run();
            }
        }.runTaskTimer(AranarthCore.getInstance(), POLL_TICKS, POLL_TICKS);
    }

    /**
     * Pauses all running Chunky tasks and waits for them to stop before running the given action.
     */
    private static void pauseOtherChunkyTasks(Consumer<List<String>> action) {
        ChunkyAPI chunky = getChunky();
        List<String> pausedWorlds = new ArrayList<>();
        if (chunky != null) {
            for (World world : Bukkit.getWorlds()) {
                if (chunky.isRunning(world.getName()) && chunky.pauseTask(world.getName())) {
                    pausedWorlds.add(world.getName());
                }
            }
        }
        if (pausedWorlds.isEmpty()) {
            action.accept(pausedWorlds);
            return;
        }
        Bukkit.getLogger().info(LOG + "Paused Chunky tasks for " + pausedWorlds + " while a world is created");

        long start = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean isStillRunning = pausedWorlds.stream().anyMatch(chunky::isRunning);
                boolean isTimedOut = System.currentTimeMillis() - start >= CHUNKY_PAUSE_TIMEOUT_MS;
                if (isStillRunning && !isTimedOut) {
                    return;
                }
                cancel();
                if (isStillRunning) {
                    Bukkit.getLogger().warning(LOG + "Chunky tasks did not stop within "
                            + (CHUNKY_PAUSE_TIMEOUT_MS / 1000) + "s - creating the world anyway");
                }
                action.accept(pausedWorlds);
            }
        }.runTaskTimer(AranarthCore.getInstance(), POLL_TICKS, POLL_TICKS);
    }

    private static void resumeChunkyTasks(List<String> pausedWorlds) {
        ChunkyAPI chunky = getChunky();
        if (chunky == null || pausedWorlds.isEmpty()) {
            return;
        }
        for (String worldName : pausedWorlds) {
            chunky.continueTask(worldName);
        }
        Bukkit.getLogger().info(LOG + "Resumed Chunky tasks for " + pausedWorlds);
    }

    private static void unlockWorld(String worldName) {
        lockedWorlds.remove(worldName);
        Bukkit.getLogger().info(LOG + worldName + " can now be entered");
        if (worldName.equals("resource")) {
            publishLock(false);
            Lang.broadcast("resource.reset_complete");
        }
    }

    private static void finishReset() {
        // Safety net in case a world was skipped before being unlocked
        if (lockedWorlds.contains("resource")) {
            publishLock(false);
        }
        lockedWorlds.clear();
        lastLockedMessage.clear();
        Bukkit.getLogger().info(LOG + "Resource world reset complete in "
                + ((System.currentTimeMillis() - resetStartTime) / 1000) + "s | " + getPerformance());
    }

    private static void publishLock(boolean isLocked) {
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishResourceWorldLock(isLocked);
        }
    }

    private static ChunkyAPI getChunky() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Chunky")) {
            return null;
        }
        return Bukkit.getServicesManager().load(ChunkyAPI.class);
    }

    private static String getPerformance() {
        return "TPS=" + String.format("%.1f", Bukkit.getServer().getTPS()[0])
                + " MSPT=" + String.format("%.1f", Bukkit.getServer().getAverageTickTime());
    }

    private static long msSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static void deleteDirectory(File directory, int[] deletedCount) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file, deletedCount);
                } else if (file.delete()) {
                    deletedCount[0]++;
                }
            }
        }
        boolean result = directory.delete();
    }
}
