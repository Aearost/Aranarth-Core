package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.integration.WorldEditPlotIntegration;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CreativePlot;
import com.aearost.aranarthcore.objects.PlotPattern;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the Creative plots world: plot lookups, permissions, merging, resetting, and syncing.
 */
public class PlotUtils {

    public static final String WORLD_NAME = "creative";
    public static final String MARKER_FILE = "aranarth_plots.marker";
    public static final int MAX_PLOTS = 36;
    public static final int MAX_ENTITIES_PER_PLOT = 50;
    private static final int MAX_NAME_LENGTH = 16;
    private static final long REGENERATE_NANOS_PER_TICK = 15_000_000L;

    private static final Map<String, CreativePlot> plots = new ConcurrentHashMap<>();
    private static final Set<String> busyPlots = ConcurrentHashMap.newKeySet();
    private static volatile PlotPattern pattern = PlotPattern.createDefault(100, 11);

    public static final PlotPattern.MergeLookup MERGE_LOOKUP = new PlotPattern.MergeLookup() {
        @Override
        public boolean isMergedEast(int gridX, int gridZ) {
            CreativePlot plot = plots.get(gridX + ";" + gridZ);
            return plot != null && plot.isMergedEast();
        }

        @Override
        public boolean isMergedSouth(int gridX, int gridZ) {
            CreativePlot plot = plots.get(gridX + ";" + gridZ);
            return plot != null && plot.isMergedSouth();
        }
    };

    /**
     * Loads the plot layout from config.yml, using the schematics in plugins/AranarthCore/plots when available.
     */
    public static void loadPattern() {
        FileConfiguration config = AranarthCore.getInstance().getConfig();
        int plotSize = config.getInt("creative-plots.plot-size", 100);
        int roadWidth = config.getInt("creative-plots.road-width", 11);
        PlotPattern loaded = null;
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            File folder = new File(AranarthCore.getInstance().getDataFolder(), "plots");
            if (!folder.exists() && !folder.mkdirs()) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Plots] Could not create the plots schematic folder");
            }
            loaded = WorldEditPlotIntegration.loadPattern(folder, plotSize, roadWidth);
        }
        pattern = loaded != null ? loaded : PlotPattern.createDefault(plotSize, roadWidth);
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Plots] Using " + (loaded != null ? "schematic" : "default")
                + " plot pattern (plot " + plotSize + ", road " + roadWidth + ")");
    }

    public static PlotPattern getPattern() {
        return pattern;
    }

    /**
     * Provides whether the world is the Creative plots world. Always false on the SMP server.
     */
    public static boolean isPlotWorld(World world) {
        return world != null && isPlotWorld(world.getName());
    }

    public static boolean isPlotWorld(String worldName) {
        return WORLD_NAME.equals(worldName) && !AranarthCore.isSmpServer();
    }

    /**
     * Provides whether this server hosts the plots world.
     */
    public static boolean isPlotServer() {
        return !AranarthCore.isSmpServer();
    }

    public static Location getSpawnLocation() {
        return getSpawnLocation(Bukkit.getWorld(WORLD_NAME));
    }

    /**
     * Provides the Creative spawn, where /creative and /plot spawn teleport to.
     */
    public static Location getSpawnLocation(World world) {
        FileConfiguration config = AranarthCore.getInstance().getConfig();
        return new Location(world,
                config.getDouble("creative-plots.spawn.x", -5.5),
                config.getDouble("creative-plots.spawn.y", 91),
                config.getDouble("creative-plots.spawn.z", -5.5),
                (float) config.getDouble("creative-plots.spawn.yaw", 180),
                (float) config.getDouble("creative-plots.spawn.pitch", 0));
    }

    /**
     * Provides whether the world is the SMP's creative world, which shares its name with the plots world on Survival.
     */
    public static boolean isSmpCreativeWorld(String worldName) {
        return WORLD_NAME.equals(worldName) && AranarthCore.isSmpServer();
    }

    /**
     * Provides the spawn of the SMP creative world. The world is null when not on the SMP.
     */
    public static Location getSmpCreativeSpawnLocation() {
        World world = AranarthCore.isSmpServer() ? Bukkit.getWorld(WORLD_NAME) : null;
        return new Location(world, 0, -60, 0, 0, 2);
    }


    public static String getKey(int gridX, int gridZ) {
        return gridX + ";" + gridZ;
    }

    /**
     * Provides the grid coordinates of the plot whose buildable area contains the block, or null if it is a road.
     */
    public static int[] getPlotCoordinates(int x, int z) {
        int tileSize = pattern.getTileSize();
        if (Math.floorMod(x, tileSize) >= pattern.getPlotSize() || Math.floorMod(z, tileSize) >= pattern.getPlotSize()) {
            return null;
        }
        return new int[]{Math.floorDiv(x, tileSize), Math.floorDiv(z, tileSize)};
    }

    /**
     * Provides the grid coordinates of the plot the location is in, or null if it is a road or another world.
     */
    public static int[] getPlotCoordinates(Location loc) {
        if (!isPlotWorld(loc.getWorld())) {
            return null;
        }
        return getPlotCoordinates(loc.getBlockX(), loc.getBlockZ());
    }

    public static CreativePlot getPlot(int gridX, int gridZ) {
        return plots.get(getKey(gridX, gridZ));
    }

    public static CreativePlot getPlot(String key) {
        return plots.get(key);
    }

    /**
     * Provides the claimed plot that the location's column belongs to, including merged roads.
     */
    public static CreativePlot getAreaPlot(int x, int z) {
        int tileSize = pattern.getTileSize();
        int plotSize = pattern.getPlotSize();
        int gx = Math.floorDiv(x, tileSize);
        int gz = Math.floorDiv(z, tileSize);
        boolean roadX = Math.floorMod(x, tileSize) >= plotSize;
        boolean roadZ = Math.floorMod(z, tileSize) >= plotSize;
        CreativePlot plot = getPlot(gx, gz);

        if (!roadX && !roadZ) {
            return plot;
        }
        if (plot == null) {
            return null;
        }
        if (roadX && !roadZ) {
            return plot.isMergedEast() ? plot : null;
        }
        if (!roadX) {
            return plot.isMergedSouth() ? plot : null;
        }
        // An intersection is only part of a plot when all four surrounding plots are merged together
        boolean fullyMerged = plot.isMergedEast() && plot.isMergedSouth()
                && MERGE_LOOKUP.isMergedEast(gx, gz + 1) && MERGE_LOOKUP.isMergedSouth(gx + 1, gz);
        return fullyMerged ? plot : null;
    }

    public static CreativePlot getAreaPlot(Location loc) {
        if (!isPlotWorld(loc.getWorld())) {
            return null;
        }
        return getAreaPlot(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Provides whether the player can build at the location in the plots world.
     */
    public static boolean canBuild(Player player, Location loc) {
        if (loc.getBlockY() <= PlotPattern.BEDROCK_Y) {
            return false;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer != null && aranarthPlayer.isInAdminMode()) {
            return true;
        }
        if (canBuildInSpawnPlots(aranarthPlayer) && isSpawnPlotLocation(loc)) {
            return true;
        }
        return canBuild(player.getUniqueId(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Provides whether the player can build in the plots designated as part of the Creative spawn.
     */
    public static boolean canBuildInSpawnPlots(AranarthPlayer aranarthPlayer) {
        return aranarthPlayer != null && aranarthPlayer.getArchitectRank() >= 1;
    }

    /**
     * Provides whether the player owns or was added to the plot that the column belongs to.
     */
    public static boolean canBuild(UUID uuid, int x, int z) {
        CreativePlot plot = getAreaPlot(x, z);
        if (plot == null || plot.isSpawn()) {
            return false;
        }
        return uuid.equals(plot.getOwner()) || plot.getMembers().contains(uuid);
    }

    /**
     * Provides whether both locations belong to the same build area, used to stop fluids and pistons crossing plots.
     */
    public static boolean isSameArea(Location from, Location to) {
        CreativePlot fromPlot = getAreaPlot(from.getBlockX(), from.getBlockZ());
        CreativePlot toPlot = getAreaPlot(to.getBlockX(), to.getBlockZ());
        if (fromPlot == null || toPlot == null) {
            return fromPlot == toPlot;
        }
        if (fromPlot == toPlot) {
            return true;
        }
        if (fromPlot.isSpawn() || toPlot.isSpawn()) {
            return fromPlot.isSpawn() && toPlot.isSpawn();
        }
        return Objects.equals(fromPlot.getOwner(), toPlot.getOwner());
    }

    /**
     * Provides whether the location is inside a plot designated as part of the Creative spawn.
     */
    public static boolean isSpawnPlotLocation(Location loc) {
        CreativePlot plot = getAreaPlot(loc);
        return plot != null && plot.isSpawn();
    }

    public static List<CreativePlot> getOwnedPlots(UUID uuid) {
        List<CreativePlot> owned = new ArrayList<>();
        for (CreativePlot plot : plots.values()) {
            if (uuid.equals(plot.getOwner())) {
                owned.add(plot);
            }
        }
        owned.sort(Comparator.comparingLong(CreativePlot::getClaimedAt));
        return owned;
    }

    /**
     * Provides the player's plot with the name. Merged plots share one name, so the earliest claimed plot of the group is used.
     */
    public static CreativePlot getOwnedPlotByName(UUID uuid, String name) {
        // Names are matched without their color codes
        String stripped = stripName(name);
        for (CreativePlot plot : getOwnedPlots(uuid)) {
            if (stripName(plot.getName()).equalsIgnoreCase(stripped)) {
                return plot;
            }
        }
        return null;
    }

    /**
     * Provides the plot name without its color codes, which is what players type to refer to it.
     */
    public static String stripName(String name) {
        return ChatUtils.stripColorFormatting(name);
    }

    /**
     * Provides the player's plots with each merged group only included once, as its earliest claimed plot.
     */
    public static List<CreativePlot> getOwnedPlotGroups(UUID uuid) {
        List<CreativePlot> groups = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CreativePlot plot : getOwnedPlots(uuid)) {
            if (seen.contains(plot.getKey())) {
                continue;
            }
            for (CreativePlot groupPlot : getMergedGroup(plot)) {
                seen.add(groupPlot.getKey());
            }
            groups.add(plot);
        }
        return groups;
    }

    /**
     * Gives every plot in the merged group the same name.
     */
    public static void renameGroup(CreativePlot plot, String name) {
        for (CreativePlot groupPlot : getMergedGroup(plot)) {
            if (!groupPlot.getName().equals(name)) {
                groupPlot.setName(name);
                savePlot(groupPlot);
            }
        }
    }

    /**
     * Provides whether the plot name is allowed. Color codes may be used, and the rest of the name
     * can only contain letters, numbers, - and _, up to 16 characters.
     */
    public static boolean isValidName(String name) {
        if (!name.matches("([A-Za-z0-9_-]|&[0-9a-fA-Fk-oK-OrR])+")) {
            return false;
        }
        String stripped = stripName(name);
        return !stripped.isEmpty() && stripped.length() <= MAX_NAME_LENGTH;
    }

    /**
     * Provides the number of plots the player may own.
     */
    public static int getPlotLimit(AranarthPlayer aranarthPlayer) {
        if (aranarthPlayer.getCouncilRank() > 0) {
            return Integer.MAX_VALUE;
        }
        int rankPlots;
        if (aranarthPlayer.getRank() >= 8) {
            rankPlots = 9;
        } else if (aranarthPlayer.getRank() >= 4) {
            rankPlots = 4;
        } else {
            rankPlots = 1;
        }
        int saintTier = getSaintTier(aranarthPlayer);
        int saintPlots = switch (saintTier) {
            case 1 -> 7;
            case 2 -> 16;
            case 3 -> 27;
            default -> 0;
        };
        return Math.min(MAX_PLOTS, rankPlots + saintPlots);
    }

    /**
     * Provides the Saint tier used for plot limits and WorldEdit access. Architects count as Saint III.
     */
    public static int getSaintTier(AranarthPlayer aranarthPlayer) {
        int tier = aranarthPlayer.getSaintRank();
        if (aranarthPlayer.getArchitectRank() > 0) {
            tier = 3;
        }
        return tier;
    }

    /**
     * Provides every plot connected to the input plot through merges, including itself.
     */
    public static List<CreativePlot> getMergedGroup(CreativePlot start) {
        List<CreativePlot> group = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<CreativePlot> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start.getKey());
        while (!queue.isEmpty()) {
            CreativePlot plot = queue.poll();
            group.add(plot);
            int x = plot.getGridX();
            int z = plot.getGridZ();
            List<CreativePlot> neighbours = new ArrayList<>();
            if (plot.isMergedEast()) {
                neighbours.add(getPlot(x + 1, z));
            }
            if (plot.isMergedSouth()) {
                neighbours.add(getPlot(x, z + 1));
            }
            if (MERGE_LOOKUP.isMergedEast(x - 1, z)) {
                neighbours.add(getPlot(x - 1, z));
            }
            if (MERGE_LOOKUP.isMergedSouth(x, z - 1)) {
                neighbours.add(getPlot(x, z - 1));
            }
            for (CreativePlot neighbour : neighbours) {
                if (neighbour != null && visited.add(neighbour.getKey())) {
                    queue.add(neighbour);
                }
            }
        }
        return group;
    }

    public static boolean isBusy(CreativePlot plot) {
        return busyPlots.contains(plot.getKey());
    }

    /**
     * Provides whether the entity counts toward a plot's entity limit. Short-lived entities such as dropped items and projectiles do not.
     */
    public static boolean countsTowardEntityLimit(Entity entity) {
        return !(entity instanceof Player || entity instanceof Item || entity instanceof ExperienceOrb
                || entity instanceof Projectile || entity instanceof FallingBlock || entity instanceof TNTPrimed
                || entity instanceof AreaEffectCloud || entity instanceof LightningStrike || entity instanceof EvokerFangs);
    }

    /**
     * Provides whether the plot at the location already has the maximum number of entities.
     */
    public static boolean isEntityLimitReached(Location loc) {
        CreativePlot plot = getAreaPlot(loc);
        World world = loc.getWorld();
        if (plot == null || world == null) {
            return false;
        }
        // A plot's area, including the merged roads that belong to it, is always within its tile
        int tileSize = pattern.getTileSize();
        int minX = plot.getGridX() * tileSize;
        int minZ = plot.getGridZ() * tileSize;
        BoundingBox box = new BoundingBox(minX, world.getMinHeight(), minZ, minX + tileSize, world.getMaxHeight(), minZ + tileSize);
        int count = 0;
        for (Entity entity : world.getNearbyEntities(box, PlotUtils::countsTowardEntityLimit)) {
            if (getAreaPlot(entity.getLocation()) == plot) {
                count++;
            }
        }
        return count >= MAX_ENTITIES_PER_PLOT;
    }

    /**
     * Claims an unclaimed plot for the player.
     */
    public static void claimPlot(int gridX, int gridZ, UUID owner, String name) {
        CreativePlot plot = new CreativePlot(gridX, gridZ, owner, name, new HashSet<>(),
                false, false, false, System.currentTimeMillis());
        plots.put(plot.getKey(), plot);
        savePlot(plot);
    }

    /**
     * Merges two adjacent plots, replacing only the road between them.
     */
    public static void mergePlots(CreativePlot first, CreativePlot second) {
        CreativePlot west = first;
        CreativePlot east = second;
        if (first.getGridX() > second.getGridX() || first.getGridZ() > second.getGridZ()) {
            west = second;
            east = first;
        }
        if (west.getGridX() != east.getGridX()) {
            west.setMergedEast(true);
        } else {
            west.setMergedSouth(true);
        }

        List<CreativePlot> group = getMergedGroup(west);
        Set<UUID> members = new HashSet<>();
        for (CreativePlot plot : group) {
            members.addAll(plot.getMembers());
        }
        for (CreativePlot plot : group) {
            plot.getMembers().clear();
            plot.getMembers().addAll(members);
            savePlot(plot);
        }

        regenerateRoadsBetween(west, east, null);
    }

    /**
     * Unclaims the plot: splits it off from any merged plots, restores the roads around it, and resets it.
     */
    public static void unclaimPlot(CreativePlot plot, Runnable onComplete) {
        int x = plot.getGridX();
        int z = plot.getGridZ();
        List<CreativePlot> changed = splitFromNeighbours(plot);
        plots.remove(plot.getKey());
        deletePlot(plot);
        for (CreativePlot neighbour : changed) {
            savePlot(neighbour);
        }
        renameSplitGroups(plot);
        regenerateAroundPlot(x, z, true, true, onComplete);
    }

    /**
     * When unclaiming a plot splits its merged group into separate groups, every group after the first
     * gets a new name so that each one can still be reached with /plot home.
     */
    private static void renameSplitGroups(CreativePlot removed) {
        if (removed.getOwner() == null) {
            return;
        }
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        Set<String> seen = new HashSet<>();
        boolean first = true;
        for (int[] offset : offsets) {
            CreativePlot neighbour = getPlot(removed.getGridX() + offset[0], removed.getGridZ() + offset[1]);
            if (neighbour == null || !removed.getOwner().equals(neighbour.getOwner())
                    || !neighbour.getName().equals(removed.getName()) || seen.contains(neighbour.getKey())) {
                continue;
            }
            List<CreativePlot> group = getMergedGroup(neighbour);
            for (CreativePlot groupPlot : group) {
                seen.add(groupPlot.getKey());
            }
            if (first) {
                first = false;
                continue;
            }
            renameGroup(neighbour, getUnusedName(removed.getOwner(), removed.getName()));
        }
    }

    /**
     * Provides a name based on the given one that the player does not use yet, such as base_2.
     */
    private static String getUnusedName(UUID owner, String base) {
        for (int i = 2; ; i++) {
            String suffix = "_" + i;
            String stripped = stripName(base);
            // Keeps the color codes unless the name has to be shortened to fit the suffix
            String candidate = stripped.length() + suffix.length() <= MAX_NAME_LENGTH
                    ? base + suffix
                    : stripped.substring(0, MAX_NAME_LENGTH - suffix.length()) + suffix;
            if (getOwnedPlotByName(owner, candidate) == null) {
                return candidate;
            }
        }
    }

    /**
     * Provides the folder of a world whether or not it is loaded.
     * @return The folder, or null if the world has never been created.
     */
    public static File getWorldFolder(String worldName) {
        File container = Bukkit.getWorldContainer();
        File[] levels = container.listFiles(file -> new File(file, "dimensions").isDirectory());
        if (levels != null) {
            for (File level : levels) {
                File folder = new File(level, "dimensions" + File.separator + "minecraft" + File.separator + worldName);
                if (folder.isDirectory()) {
                    return folder;
                }
            }
        }
        File legacy = new File(container, worldName);
        return legacy.isDirectory() ? legacy : null;
    }

    /**
     * Unclaims every plot of a merged group, calling onComplete once all of them are done.
     */
    public static void unclaimGroup(CreativePlot plot, Runnable onComplete) {
        List<CreativePlot> group = getMergedGroup(plot);
        Runnable groupComplete = countDown(group.size(), onComplete);
        // Every merge is removed before any regeneration runs, so the roads come back everywhere
        for (CreativePlot groupPlot : group) {
            unclaimPlot(groupPlot, groupComplete);
        }
    }

    /**
     * Resets every plot of a merged group, unmerging them back into individual plots first.
     */
    public static void resetGroup(CreativePlot plot, Runnable onComplete) {
        List<CreativePlot> group = getMergedGroup(plot);
        if (group.size() == 1) {
            resetPlot(plot, onComplete);
            return;
        }
        splitGroup(group);
        Runnable groupComplete = countDown(group.size(), onComplete);
        // Every merge is removed before any regeneration runs, so the roads come back everywhere
        for (CreativePlot groupPlot : group) {
            regenerateAroundPlot(groupPlot.getGridX(), groupPlot.getGridZ(), true, true, groupComplete);
        }
    }

    /**
     * Unmerges every plot of a merged group back into individual plots.
     */
    public static void unmergeGroup(CreativePlot plot, Runnable onComplete) {
        List<CreativePlot> group = getMergedGroup(plot);
        List<CreativePlot[]> pairs = new ArrayList<>();
        for (CreativePlot groupPlot : group) {
            CreativePlot east = getPlot(groupPlot.getGridX() + 1, groupPlot.getGridZ());
            if (groupPlot.isMergedEast() && east != null) {
                pairs.add(new CreativePlot[]{groupPlot, east});
            }
            CreativePlot south = getPlot(groupPlot.getGridX(), groupPlot.getGridZ() + 1);
            if (groupPlot.isMergedSouth() && south != null) {
                pairs.add(new CreativePlot[]{groupPlot, south});
            }
        }
        splitGroup(group);
        if (pairs.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        Runnable groupComplete = countDown(pairs.size(), onComplete);
        for (CreativePlot[] pair : pairs) {
            regenerateRoadsBetween(pair[0], pair[1], groupComplete);
        }
    }

    /**
     * Removes every merge within the group and saves it.
     */
    private static void splitGroup(List<CreativePlot> group) {
        List<CreativePlot> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparingLong(CreativePlot::getClaimedAt));
        String name = sorted.get(0).getName();
        for (CreativePlot groupPlot : sorted) {
            groupPlot.setMergedEast(false);
            groupPlot.setMergedSouth(false);
        }
        for (int i = 1; i < sorted.size(); i++) {
            CreativePlot groupPlot = sorted.get(i);
            if (groupPlot.getOwner() != null) {
                // Each name is set before the next is generated so they all differ
                groupPlot.setName(getUnusedName(groupPlot.getOwner(), name));
            }
        }
        for (CreativePlot groupPlot : sorted) {
            savePlot(groupPlot);
        }
    }

    /**
     * Provides a runnable that only runs onComplete after it has been run the given number of times.
     */
    private static Runnable countDown(int count, Runnable onComplete) {
        int[] remaining = {count};
        return () -> {
            remaining[0]--;
            if (remaining[0] == 0 && onComplete != null) {
                onComplete.run();
            }
        };
    }

    /**
     * Resets only the buildable area of the plot. Merged roads and other merged plots are left alone.
     */
    public static void resetPlot(CreativePlot plot, Runnable onComplete) {
        regenerateAroundPlot(plot.getGridX(), plot.getGridZ(), true, false, onComplete);
    }

    /**
     * Resets an unclaimed plot's buildable area, used by staff in admin mode.
     */
    public static void resetPlotArea(int gridX, int gridZ, Runnable onComplete) {
        regenerateAroundPlot(gridX, gridZ, true, false, onComplete);
    }

    /**
     * Designates a plot as part of the Creative spawn. Neighbouring spawn plots are merged automatically.
     */
    public static void claimSpawnPlot(int gridX, int gridZ) {
        CreativePlot plot = new CreativePlot(gridX, gridZ, null, "spawn_" + gridX + "_" + gridZ, new HashSet<>(),
                false, false, true, System.currentTimeMillis());
        plots.put(plot.getKey(), plot);
        savePlot(plot);

        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            CreativePlot neighbour = getPlot(gridX + offset[0], gridZ + offset[1]);
            if (neighbour != null && neighbour.isSpawn()) {
                mergePlots(plot, neighbour);
            }
        }
    }

    /**
     * Removes the spawn designation of a plot and restores the roads around it.
     */
    public static void unclaimSpawnPlot(CreativePlot plot, Runnable onComplete) {
        unclaimWithoutReset(plot, onComplete);
    }

    private static void unclaimWithoutReset(CreativePlot plot, Runnable onComplete) {
        int x = plot.getGridX();
        int z = plot.getGridZ();
        List<CreativePlot> changed = splitFromNeighbours(plot);
        plots.remove(plot.getKey());
        deletePlot(plot);
        for (CreativePlot neighbour : changed) {
            savePlot(neighbour);
        }
        regenerateAroundPlot(x, z, false, true, onComplete);
    }

    /**
     * Removes every merge between the plot and its neighbours.
     * @return The neighbouring plots whose merge state changed.
     */
    private static List<CreativePlot> splitFromNeighbours(CreativePlot plot) {
        List<CreativePlot> changed = new ArrayList<>();
        plot.setMergedEast(false);
        plot.setMergedSouth(false);
        CreativePlot westPlot = getPlot(plot.getGridX() - 1, plot.getGridZ());
        if (westPlot != null && westPlot.isMergedEast()) {
            westPlot.setMergedEast(false);
            changed.add(westPlot);
        }
        CreativePlot northPlot = getPlot(plot.getGridX(), plot.getGridZ() - 1);
        if (northPlot != null && northPlot.isMergedSouth()) {
            northPlot.setMergedSouth(false);
            changed.add(northPlot);
        }
        return changed;
    }

    /**
     * Regenerates the road between two adjacent plots, along with the intersections at both of its ends.
     */
    private static void regenerateRoadsBetween(CreativePlot west, CreativePlot east, Runnable onComplete) {
        int tileSize = pattern.getTileSize();
        int plotSize = pattern.getPlotSize();
        int baseX = west.getGridX() * tileSize;
        int baseZ = west.getGridZ() * tileSize;
        int[] area;
        if (west.getGridX() != east.getGridX()) {
            // North-south road, extended to cover the intersections at both ends
            area = new int[]{baseX + plotSize, baseZ - (tileSize - plotSize), baseX + tileSize - 1, baseZ + tileSize - 1};
        } else {
            area = new int[]{baseX - (tileSize - plotSize), baseZ + plotSize, baseX + tileSize - 1, baseZ + tileSize - 1};
        }
        List<int[]> areas = new ArrayList<>();
        areas.add(area);
        if (west.getGridX() != east.getGridX()) {
            addIntersectionArms(areas, west.getGridX(), west.getGridZ() - 1);
        } else {
            addIntersectionArms(areas, west.getGridX() - 1, west.getGridZ());
        }
        addIntersectionArms(areas, west.getGridX(), west.getGridZ());
        regenerate(areas, Set.of(west.getKey(), east.getKey()), false, onComplete);
    }

    /**
     * Regenerates the plot's buildable area and/or the full ring of roads and intersections around it.
     */
    private static void regenerateAroundPlot(int gridX, int gridZ, boolean includeInterior, boolean includeRoads, Runnable onComplete) {
        int tileSize = pattern.getTileSize();
        int plotSize = pattern.getPlotSize();
        int minX = gridX * tileSize;
        int minZ = gridZ * tileSize;
        List<int[]> areas = new ArrayList<>();
        if (includeInterior) {
            areas.add(new int[]{minX, minZ, minX + plotSize - 1, minZ + plotSize - 1});
        }
        if (includeRoads) {
            int roadWidth = tileSize - plotSize;
            int ringMinX = minX - roadWidth;
            int ringMinZ = minZ - roadWidth;
            int ringMaxX = minX + tileSize - 1;
            int ringMaxZ = minZ + tileSize - 1;
            // North and south bands
            areas.add(new int[]{ringMinX, ringMinZ, ringMaxX, minZ - 1});
            areas.add(new int[]{ringMinX, minZ + plotSize, ringMaxX, ringMaxZ});
            // West and east bands
            areas.add(new int[]{ringMinX, minZ, minX - 1, minZ + plotSize - 1});
            areas.add(new int[]{minX + plotSize, minZ, ringMaxX, minZ + plotSize - 1});
            addIntersectionArms(areas, gridX - 1, gridZ - 1);
            addIntersectionArms(areas, gridX, gridZ - 1);
            addIntersectionArms(areas, gridX - 1, gridZ);
            addIntersectionArms(areas, gridX, gridZ);
        }
        regenerate(areas, Set.of(getKey(gridX, gridZ)), true, onComplete);
    }

    /**
     * Provides how many rings of plots surround the centre plot within a world border of the given size.
     */
    public static int getRingsForBorderSize(double borderSize) {
        double plotsAcross = (borderSize - pattern.getRoadWidth()) / pattern.getTileSize();
        return Math.max(0, (int) Math.floor((plotsAcross - 1) / 2));
    }

    /**
     * Provides how many rings of plots currently surround the centre plot of the plots world.
     */
    public static int getRings(World world) {
        return getRingsForBorderSize(world.getWorldBorder().getSize());
    }

    /**
     * Fits the world border around the centre plot, plot (0, 0), and the given number of rings of plots around it,
     * including the roads around the outside, then pre-generates the area with Chunky.
     *
     * @return The number of plots along each side of the area.
     */
    public static int setRings(World world, int rings) {
        int plotsAcross = rings * 2 + 1;
        int size = plotsAcross * pattern.getTileSize() + pattern.getRoadWidth();
        int center = pattern.getPlotSize() / 2;
        world.getWorldBorder().setCenter(center, center);
        world.getWorldBorder().setSize(size);

        // Chunky is only available once every plugin has enabled. Generated chunks are skipped quickly.
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Bukkit.dispatchCommand(console, "chunky cancel " + WORLD_NAME);
            Bukkit.dispatchCommand(console, "chunky world " + WORLD_NAME);
            Bukkit.dispatchCommand(console, "chunky center " + center + " " + center);
            Bukkit.dispatchCommand(console, "chunky radius " + (size + 1) / 2);
            Bukkit.dispatchCommand(console, "chunky start");
            Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Chunky pre-generation started for world: " + WORLD_NAME
                    + " (" + plotsAcross + "x" + plotsAcross + " plots)");
        });
        return plotsAcross;
    }

    /**
     * Adds rings of plots around the plots world and pre-generates them.
     *
     * @return The number of plots along each side of the area, or -1 if the plots world is not loaded.
     */
    public static int expand(int rings) {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null || !isPlotServer()) {
            return -1;
        }
        return setRings(world, getRings(world) + rings);
    }

    /**
     * Adds the ends of the unmerged roads leading into the intersection to the south-east of plot (gx, gz),
     * since custom intersection schematics can replace them. Merged roads are skipped so builds on them are kept.
     */
    private static void addIntersectionArms(List<int[]> areas, int gx, int gz) {
        int margin = pattern.getIntersectionMargin();
        if (margin == 0) {
            return;
        }
        int tileSize = pattern.getTileSize();
        int plotSize = pattern.getPlotSize();
        int minX = gx * tileSize + plotSize;
        int minZ = gz * tileSize + plotSize;
        int maxX = gx * tileSize + tileSize - 1;
        int maxZ = gz * tileSize + tileSize - 1;
        if (!MERGE_LOOKUP.isMergedEast(gx, gz)) {
            areas.add(new int[]{minX, minZ - margin, maxX, minZ - 1});
        }
        if (!MERGE_LOOKUP.isMergedEast(gx, gz + 1)) {
            areas.add(new int[]{minX, maxZ + 1, maxX, maxZ + margin});
        }
        if (!MERGE_LOOKUP.isMergedSouth(gx, gz)) {
            areas.add(new int[]{minX - margin, minZ, minX - 1, maxZ});
        }
        if (!MERGE_LOOKUP.isMergedSouth(gx + 1, gz)) {
            areas.add(new int[]{maxX + 1, minZ, maxX + margin, maxZ});
        }
    }

    /**
     * Regenerates the columns of each area to match the plot pattern, a few columns per tick.
     *
     * @param areas          Areas as {minX, minZ, maxX, maxZ}.
     * @param keys           Plot keys that are marked busy until the regeneration completes.
     * @param removeEntities Whether non-player entities in the areas should be removed.
     */
    private static void regenerate(List<int[]> areas, Set<String> keys, boolean removeEntities, Runnable onComplete) {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null || !isPlotServer()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        List<int[]> columns = new ArrayList<>();
        for (int[] area : areas) {
            for (int x = area[0]; x <= area[2]; x++) {
                for (int z = area[1]; z <= area[3]; z++) {
                    columns.add(new int[]{x, z});
                }
            }
            if (removeEntities) {
                BoundingBox box = new BoundingBox(area[0], world.getMinHeight(), area[1],
                        area[2] + 1, world.getMaxHeight(), area[3] + 1);
                for (Entity entity : world.getNearbyEntities(box)) {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                }
            }
        }

        busyPlots.addAll(keys);
        PlotPattern currentPattern = pattern;
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                // As many columns as fit in the time budget, so empty plots reset quickly and full ones don't lag
                long deadline = System.nanoTime() + REGENERATE_NANOS_PER_TICK;
                while (index < columns.size() && System.nanoTime() < deadline) {
                    int[] column = columns.get(index++);
                    regenerateColumn(world, currentPattern, column[0], column[1]);
                }
                if (index >= columns.size()) {
                    busyPlots.removeAll(keys);
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 1L, 1L);
    }

    /**
     * Sets one world column back to what the pattern defines for it, leaving the bedrock floor untouched.
     */
    private static void regenerateColumn(World world, PlotPattern currentPattern, int x, int z) {
        PlotPattern.Column column = currentPattern.resolve(x, z, MERGE_LOOKUP);
        int highestBlock = world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        int top = Math.max(Math.max(highestBlock, currentPattern.getMaxY()), PlotPattern.SURFACE_Y);
        for (int y = PlotPattern.BEDROCK_Y + 1; y <= top; y++) {
            BlockData target = currentPattern.getBlock(column, y);
            Block block = world.getBlockAt(x, y, z);
            if (target.getMaterial().isAir()) {
                if (!block.getType().isAir()) {
                    block.setBlockData(target, false);
                }
            } else if (!block.getBlockData().equals(target)) {
                block.setBlockData(target, false);
            }
        }
    }

    public static Collection<CreativePlot> getPlots() {
        return plots.values();
    }

    /**
     * Saves the plot immediately and sends it to the other server.
     */
    public static void savePlot(CreativePlot plot) {
        PersistenceUtils.saveCreativePlot(plot);
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishCreativePlotUpdate(plot);
        }
    }

    /**
     * Deletes the plot immediately and tells the other server.
     */
    public static void deletePlot(CreativePlot plot) {
        PersistenceUtils.deleteCreativePlot(plot.getKey());
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishCreativePlotDelete(plot.getKey());
        }
    }

    /**
     * Adds a plot to memory without saving it, used when loading or syncing.
     */
    public static void loadPlot(CreativePlot plot) {
        plots.put(plot.getKey(), plot);
    }

    /**
     * Removes a plot from memory without saving, used when syncing a deletion from the other server.
     */
    public static void unloadPlot(String key) {
        plots.remove(key);
    }

    public static JsonObject toJson(CreativePlot plot) {
        JsonObject json = new JsonObject();
        json.addProperty("x", plot.getGridX());
        json.addProperty("z", plot.getGridZ());
        if (plot.getOwner() != null) {
            json.addProperty("owner", plot.getOwner().toString());
        }
        json.addProperty("name", plot.getName());
        JsonArray members = new JsonArray();
        for (UUID member : plot.getMembers()) {
            members.add(member.toString());
        }
        json.add("members", members);
        json.addProperty("mergedEast", plot.isMergedEast());
        json.addProperty("mergedSouth", plot.isMergedSouth());
        json.addProperty("spawn", plot.isSpawn());
        json.addProperty("claimedAt", plot.getClaimedAt());
        return json;
    }

    public static CreativePlot fromJson(JsonObject json) {
        UUID owner = json.has("owner") ? UUID.fromString(json.get("owner").getAsString()) : null;
        Set<UUID> members = new HashSet<>();
        if (json.has("members")) {
            for (JsonElement element : json.getAsJsonArray("members")) {
                members.add(UUID.fromString(element.getAsString()));
            }
        }
        return new CreativePlot(
                json.get("x").getAsInt(),
                json.get("z").getAsInt(),
                owner,
                json.get("name").getAsString(),
                members,
                json.has("mergedEast") && json.get("mergedEast").getAsBoolean(),
                json.has("mergedSouth") && json.get("mergedSouth").getAsBoolean(),
                json.has("spawn") && json.get("spawn").getAsBoolean(),
                json.has("claimedAt") ? json.get("claimedAt").getAsLong() : 0L);
    }
}
