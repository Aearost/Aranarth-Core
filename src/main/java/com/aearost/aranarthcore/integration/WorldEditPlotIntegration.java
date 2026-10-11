package com.aearost.aranarthcore.integration;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.PlotPattern;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.PlotUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Connects the Creative plots world to WorldEdit.
 */
public class WorldEditPlotIntegration {

    private static boolean registered = false;

    /**
     * Registers the edit mask. Safe to call more than once.
     */
    public static void enable() {
        if (registered) {
            return;
        }
        WorldEdit.getInstance().getEventBus().register(new WorldEditPlotIntegration());
        registered = true;
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Plots] WorldEdit plot mask enabled");
    }

    /**
     * Limits every WorldEdit change in the plots world to the plots the player can build in.
     */
    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE || event.getWorld() == null
                || !PlotUtils.isPlotWorld(event.getWorld().getName())) {
            return;
        }

        Actor actor = event.getActor();
        UUID uuid = actor != null && actor.isPlayer() ? actor.getUniqueId() : null;
        boolean bypass = false;
        if (uuid != null) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            bypass = aranarthPlayer != null && aranarthPlayer.isInAdminMode();
        }
        final boolean isAdmin = bypass;

        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
            private boolean isAllowed(int x, int y, int z) {
                if (y <= PlotPattern.BEDROCK_Y) {
                    return false;
                }
                if (isAdmin) {
                    return true;
                }
                return uuid != null && PlotUtils.canBuild(uuid, x, z);
            }

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
                if (!isAllowed(location.x(), location.y(), location.z())) {
                    return false;
                }
                return super.setBlock(location, block);
            }

            @Override
            public boolean setBiome(BlockVector3 position, BiomeType biome) {
                if (!isAllowed(position.x(), position.y(), position.z())) {
                    return false;
                }
                return super.setBiome(position, biome);
            }

            @Override
            public Entity createEntity(Location location, BaseEntity entity) {
                BlockVector3 position = location.toVector().toBlockPoint();
                if (!isAllowed(position.x(), position.y(), position.z())) {
                    return null;
                }
                return super.createEntity(location, entity);
            }
        });
    }

    /**
     * Builds the plot pattern from the schematics in the folder, or returns null to use the default pattern.
     */
    public static PlotPattern loadPattern(File folder, int plotSize, int roadWidth) {
        int tileSize = plotSize + roadWidth;
        File tileFile = new File(folder, "plot_tile.schem");
        if (!tileFile.exists()) {
            return null;
        }
        try {
            PlotPattern.PatternGrid tile = readGrid(tileFile, tileSize, tileSize);
            if (tile == null) {
                return null;
            }
            File roadFile = new File(folder, "road_merged.schem");
            File intersectionFile = new File(folder, "intersection_merged.schem");
            PlotPattern.PatternGrid mergedRoad = roadFile.exists() ? readGrid(roadFile, roadWidth, plotSize) : null;
            PlotPattern.PatternGrid mergedIntersection = intersectionFile.exists() ? readGrid(intersectionFile, roadWidth, roadWidth) : null;
            // Sides in the same order as PlotPattern's T-junction rotations
            String[] sides = {"south", "west", "north", "east"};
            PlotPattern.PatternGrid tJunction = null;
            int tJunctionSide = 0;
            for (int i = 0; i < sides.length; i++) {
                File tJunctionFile = new File(folder, "intersection_t_" + sides[i] + ".schem");
                if (tJunctionFile.exists()) {
                    tJunction = readIntersectionGrid(tJunctionFile, plotSize, roadWidth);
                    tJunctionSide = i;
                    break;
                }
            }
            // Corners in the same order as PlotPattern's corner rotations
            String[] corners = {"se", "sw", "nw", "ne"};
            PlotPattern.PatternGrid corner = null;
            int cornerSide = 0;
            for (int i = 0; i < corners.length; i++) {
                File cornerFile = new File(folder, "intersection_corner_" + corners[i] + ".schem");
                if (cornerFile.exists()) {
                    corner = readIntersectionGrid(cornerFile, plotSize, roadWidth);
                    cornerSide = i;
                    break;
                }
            }
            return new PlotPattern(plotSize, roadWidth, tile, mergedRoad, mergedIntersection, tJunction, tJunctionSide, corner, cornerSide);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Plots] Failed to load the plot schematics: " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads a schematic into a grid, relative to the position it was copied from.
     */
    private static PlotPattern.PatternGrid readGrid(File file, int width, int depth) throws Exception {
        Clipboard clipboard = readClipboard(file);
        return clipboard == null ? null : toGrid(file, clipboard, width, depth, 0);
    }

    /**
     * Reads a T-junction or corner schematic.
     */
    private static PlotPattern.PatternGrid readIntersectionGrid(File file, int plotSize, int roadWidth) throws Exception {
        Clipboard clipboard = readClipboard(file);
        if (clipboard == null) {
            return null;
        }
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        int margin = Math.max(0, Math.min(origin.x() - min.x(), origin.z() - min.z()));
        // The ends of a road can't overlap the other intersection's ends
        margin = Math.min(margin, plotSize / 2 - 1);
        int size = roadWidth + margin * 2;
        return toGrid(file, clipboard, size, size, margin);
    }

    private static Clipboard readClipboard(File file) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Plots] Unknown schematic format: " + file.getName());
            return null;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    /**
     * Converts a clipboard into a grid, shifting every position by the offset on the X and Z axes.
     */
    private static PlotPattern.PatternGrid toGrid(File file, Clipboard clipboard, int width, int depth, int offset) {
        BlockVector3 origin = clipboard.getOrigin();
        Map<int[], BlockData> blocks = new HashMap<>();
        int outside = 0;
        for (BlockVector3 position : clipboard.getRegion()) {
            int x = position.x() - origin.x() + offset;
            // Schematics are relative to the copier's feet, which stand at the plots world's ground height
            int y = position.y() - origin.y() + PlotPattern.FEET_Y;
            int z = position.z() - origin.z() + offset;
            if (x < 0 || x >= width || z < 0 || z >= depth) {
                outside++;
                continue;
            }
            blocks.put(new int[]{x, y, z}, BukkitAdapter.adapt(clipboard.getBlock(position)));
        }
        if (outside > 0) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[Plots] " + outside + " blocks of " + file.getName()
                    + " are outside the expected " + width + "x" + depth + " area and were ignored."
                    + " Make sure it was copied from its north-west corner");
        }
        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "[Plots] Loaded " + file.getName());
        return PlotPattern.buildGrid(width, depth, blocks);
    }
}
