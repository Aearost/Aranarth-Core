package com.aearost.aranarthcore.integration;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integrates with the SquareMap live-map plugin to display dominion land claims, outposts and plots.
 * The layer refreshes every 30 seconds.
 */
public class SquaremapIntegration {

    private static final Key LAYER_KEY = Key.of("aranarthcore-dominions");
    private static final Key OUTPOST_LAYER_KEY = Key.of("aranarthcore-outposts");
    private static final Key PLOT_LAYER_KEY = Key.of("aranarthcore-plots");
    // One provider per world (keyed by squaremap world identifier string)
    private final Map<String, SimpleLayerProvider> providers = new HashMap<>();
    private final Map<String, SimpleLayerProvider> outpostProviders = new HashMap<>();
    private final Map<String, SimpleLayerProvider> plotProviders = new HashMap<>();
    private BukkitTask refreshTask;

    /**
     * Registers the dominion layer on all squaremap-enabled worlds and starts the
     * periodic refresh task.
     */
    public void enable() {
        Squaremap api = SquaremapProvider.get();

        for (MapWorld mapWorld : api.mapWorlds()) {
            SimpleLayerProvider provider = SimpleLayerProvider.builder("Dominions")
                    .showControls(true)
                    .defaultHidden(false)
                    .layerPriority(5)
                    .zIndex(250)
                    .build();
            if (!mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
                mapWorld.layerRegistry().register(LAYER_KEY, provider);
            }
            providers.put(mapWorld.identifier().asString(), provider);

            SimpleLayerProvider outpostProvider = SimpleLayerProvider.builder("Outposts")
                    .showControls(true)
                    .defaultHidden(false)
                    .layerPriority(6)
                    .zIndex(251)
                    .build();
            if (!mapWorld.layerRegistry().hasEntry(OUTPOST_LAYER_KEY)) {
                mapWorld.layerRegistry().register(OUTPOST_LAYER_KEY, outpostProvider);
            }
            outpostProviders.put(mapWorld.identifier().asString(), outpostProvider);

            // Rendered above dominions and outposts so hovering a plot shows the plot's tooltip
            SimpleLayerProvider plotProvider = SimpleLayerProvider.builder("Dominion Plots")
                    .showControls(true)
                    .defaultHidden(false)
                    .layerPriority(7)
                    .zIndex(252)
                    .build();
            if (!mapWorld.layerRegistry().hasEntry(PLOT_LAYER_KEY)) {
                mapWorld.layerRegistry().register(PLOT_LAYER_KEY, plotProvider);
            }
            plotProviders.put(mapWorld.identifier().asString(), plotProvider);
        }

        // Refresh every 30 seconds (600 ticks) asynchronously
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                AranarthCore.getInstance(), this::refresh, 20L, 600L);

        Bukkit.getLogger().info("[AC] SquareMap dominion layer registered.");
    }

    /**
     * Cancels the refresh task and unregisters the dominion layer from all worlds.
     */
    public void disable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }

        try {
            Squaremap api = SquaremapProvider.get();
            for (MapWorld mapWorld : api.mapWorlds()) {
                if (mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
                    mapWorld.layerRegistry().unregister(LAYER_KEY);
                }
                if (mapWorld.layerRegistry().hasEntry(OUTPOST_LAYER_KEY)) {
                    mapWorld.layerRegistry().unregister(OUTPOST_LAYER_KEY);
                }
                if (mapWorld.layerRegistry().hasEntry(PLOT_LAYER_KEY)) {
                    mapWorld.layerRegistry().unregister(PLOT_LAYER_KEY);
                }
            }
        } catch (IllegalStateException ignored) {
            // SquareMap already shut down
        }

        providers.clear();
        outpostProviders.clear();
        plotProviders.clear();
    }

    /**
     * Clears and re-renders all dominion boundary polygons across every registered world.
     */
    public void refresh() {
        for (SimpleLayerProvider provider : providers.values()) {
            provider.clearMarkers();
        }
        for (SimpleLayerProvider provider : outpostProviders.values()) {
            provider.clearMarkers();
        }
        for (SimpleLayerProvider provider : plotProviders.values()) {
            provider.clearMarkers();
        }

        // Snapshot to avoid ConcurrentModificationException on the async thread
        List<Dominion> snapshot = new ArrayList<>(DominionUtils.getDominions());

        for (Dominion dominion : snapshot) {
            // Only render dominions that belong to this server.
            // SMP dominions store their home world with the "smp:" prefix; Survival dominions do not.
            String homeWorldName = dominion.getDominionHomeWorldName();
            boolean isSmpDominion = homeWorldName != null && homeWorldName.startsWith("smp:");
            if (AranarthCore.isSmpServer() != isSmpDominion) continue;

            Color fillColor = parseColor(dominion.getMapColor());
            // Two shades darker for the border so it contrasts against the fill
            Color strokeColor = fillColor.darker().darker();
            String dominionName = ChatUtils.stripColorFormatting(dominion.getName());

            // Dominion land claims
            List<Chunk> domChunks = dominion.getChunks();
            if (!domChunks.isEmpty()) {
                MarkerOptions options = MarkerOptions.builder()
                        .strokeColor(strokeColor)
                        .strokeWeight(2)
                        .strokeOpacity(1.0)
                        .fillColor(fillColor)
                        .fillOpacity(0.45)
                        .hoverTooltip("<b>\uD83C\uDFF0 " + dominionName + " \uD83C\uDFF0</b>")
                        .build();

                Map<String, Set<String>> chunksByWorld = new HashMap<>();
                for (Chunk chunk : domChunks) {
                    String worldKey = BukkitAdapter.worldIdentifier(chunk.getWorld()).asString();
                    chunksByWorld
                            .computeIfAbsent(worldKey, k -> new HashSet<>())
                            .add(chunk.getX() + "," + chunk.getZ());
                }

                for (Map.Entry<String, Set<String>> entry : chunksByWorld.entrySet()) {
                    SimpleLayerProvider provider = providers.get(entry.getKey());
                    if (provider == null) continue;

                    List<Region> regions = buildRegions(entry.getValue());
                    int regionIdx = 0;
                    for (Region region : regions) {
                        Marker polygon = Marker.polygon(region.outer(), region.holes());
                        polygon.markerOptions(options);
                        Key markerKey = Key.of("d-" + dominion.getId() + "-" + entry.getKey().hashCode() + "-r" + regionIdx++);
                        provider.addMarker(markerKey, polygon);
                    }
                }
            }

            // Outpost land claims
            for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                List<Chunk> outpostChunks = outpost.getChunks();
                if (outpostChunks.isEmpty()) continue;

                String outpostName = ChatUtils.stripColorFormatting(outpost.getName());
                MarkerOptions outpostOptions = MarkerOptions.builder()
                        .strokeColor(strokeColor)
                        .strokeWeight(2)
                        .strokeOpacity(1.0)
                        .fillColor(fillColor)
                        .fillOpacity(0.45)
                        .hoverTooltip("<b>\uD83D\uDEA9 " + outpostName + " \uD83D\uDEA9</b><br><i>Outpost of " + dominionName + "</i>")
                        .build();

                Map<String, Set<String>> outpostChunksByWorld = new HashMap<>();
                for (Chunk chunk : outpostChunks) {
                    String worldKey = BukkitAdapter.worldIdentifier(chunk.getWorld()).asString();
                    outpostChunksByWorld
                            .computeIfAbsent(worldKey, k -> new HashSet<>())
                            .add(chunk.getX() + "," + chunk.getZ());
                }

                for (Map.Entry<String, Set<String>> entry : outpostChunksByWorld.entrySet()) {
                    SimpleLayerProvider provider = outpostProviders.get(entry.getKey());
                    if (provider == null) continue;

                    List<Region> regions = buildRegions(entry.getValue());
                    int regionIdx = 0;
                    for (Region region : regions) {
                        Marker polygon = Marker.polygon(region.outer(), region.holes());
                        polygon.markerOptions(outpostOptions);
                        Key markerKey = Key.of("op-" + outpost.getId() + "-" + entry.getKey().hashCode() + "-r" + regionIdx++);
                        provider.addMarker(markerKey, polygon);
                    }
                }
            }

            // Dominion plots - grouped by plot name, then by world
            // Chunk keys are stored as "worldName:cx:cz"
            Map<String, Map<String, Set<String>>> plotChunksByWorld = new HashMap<>();
            for (Map.Entry<String, String> entry : new HashMap<>(dominion.getPlotChunkNames()).entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length != 3) continue;
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;
                String worldKey = BukkitAdapter.worldIdentifier(world).asString();
                plotChunksByWorld
                        .computeIfAbsent(entry.getValue(), k -> new HashMap<>())
                        .computeIfAbsent(worldKey, k -> new HashSet<>())
                        .add(parts[1] + "," + parts[2]);
            }

            for (Map.Entry<String, Map<String, Set<String>>> plotEntry : plotChunksByWorld.entrySet()) {
                String plotName = plotEntry.getKey();
                MarkerOptions plotOptions = MarkerOptions.builder()
                        .strokeColor(Color.WHITE)
                        .strokeWeight(1)
                        .strokeOpacity(0.9)
                        .fillColor(fillColor.brighter())
                        .fillOpacity(0.3)
                        .hoverTooltip("<b>🏠 " + plotName + " 🏠</b><br><i>Plot of " + dominionName + "</i>")
                        .build();

                for (Map.Entry<String, Set<String>> entry : plotEntry.getValue().entrySet()) {
                    SimpleLayerProvider provider = plotProviders.get(entry.getKey());
                    if (provider == null) continue;

                    List<Region> regions = buildRegions(entry.getValue());
                    int regionIdx = 0;
                    for (Region region : regions) {
                        Marker polygon = Marker.polygon(region.outer(), region.holes());
                        polygon.markerOptions(plotOptions);
                        Key markerKey = Key.of("p-" + dominion.getId() + "-" + plotName + "-" + entry.getKey().hashCode() + "-r" + regionIdx++);
                        provider.addMarker(markerKey, polygon);
                    }
                }
            }
        }
    }

    /**
     * A contiguous claimed region: its outer boundary plus any unclaimed gaps enclosed by it.
     */
    private record Region(List<Point> outer, List<List<Point>> holes) {}

    /**
     * Computes the boundary of every contiguous region, separating outer boundaries from the
     * enclosed unclaimed gaps so the gaps render as negative space instead of a second fill layer.
     * @param chunkCoords Set of "cx,cz" strings for a single dominion in one world.
     * @return A list of regions, one per contiguous area.
     */
    private static List<Region> buildRegions(Set<String> chunkCoords) {
        // Collect directed boundary edges - sides of chunks that have no same-dominion neighbor.
        // Each chunk is wound clockwise (x east, z south), so the claimed side is always on the right
        // of travel. Outer rings therefore have a positive signed area and holes a negative one.
        List<int[]> edges = new ArrayList<>();

        for (String coord : chunkCoords) {
            String[] parts = coord.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            int bx = cx * 16;
            int bz = cz * 16;

            // No northern neighbor - travel east
            if (!chunkCoords.contains(cx + "," + (cz - 1))) {
                edges.add(new int[]{bx, bz, bx + 16, bz});
            }
            // No eastern neighbor - travel south
            if (!chunkCoords.contains((cx + 1) + "," + cz)) {
                edges.add(new int[]{bx + 16, bz, bx + 16, bz + 16});
            }
            // No southern neighbor - travel west
            if (!chunkCoords.contains(cx + "," + (cz + 1))) {
                edges.add(new int[]{bx + 16, bz + 16, bx, bz + 16});
            }
            // No western neighbor - travel north
            if (!chunkCoords.contains((cx - 1) + "," + cz)) {
                edges.add(new int[]{bx, bz + 16, bx, bz});
            }
        }

        if (edges.isEmpty()) return List.of();

        // Edge indices that start at each point
        Map<String, List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            int[] e = edges.get(i);
            outgoing.computeIfAbsent(e[0] + "," + e[1], k -> new ArrayList<>()).add(i);
        }

        // Trace each closed ring of edges
        boolean[] used = new boolean[edges.size()];
        List<List<int[]>> outerRings = new ArrayList<>();
        List<List<int[]>> holeRings = new ArrayList<>();

        for (int startIdx = 0; startIdx < edges.size(); startIdx++) {
            if (used[startIdx]) continue;

            List<int[]> ring = new ArrayList<>();
            int curIdx = startIdx;
            while (true) {
                used[curIdx] = true;
                int[] curEdge = edges.get(curIdx);
                ring.add(new int[]{curEdge[0], curEdge[1]});

                int nextIdx = pickNextEdge(edges, outgoing, curEdge);
                if (nextIdx == -1 || nextIdx == startIdx || used[nextIdx]) break;
                curIdx = nextIdx;
            }

            if (ring.size() < 3) continue;
            if (signedArea(ring) > 0) {
                outerRings.add(ring);
            } else {
                holeRings.add(ring);
            }
        }

        // Assign each hole to the smallest outer ring that contains it
        List<List<List<Point>>> holesPerOuter = new ArrayList<>();
        for (int i = 0; i < outerRings.size(); i++) {
            holesPerOuter.add(new ArrayList<>());
        }
        for (List<int[]> hole : holeRings) {
            // Test point just inside the hole - the unclaimed side is on the left of travel
            int[] a = hole.get(0);
            int[] b = hole.get(1);
            int dx = Integer.signum(b[0] - a[0]);
            int dz = Integer.signum(b[1] - a[1]);
            double testX = (a[0] + b[0]) / 2.0 + dz * 0.5;
            double testZ = (a[1] + b[1]) / 2.0 - dx * 0.5;

            int bestOuter = -1;
            long bestArea = Long.MAX_VALUE;
            for (int i = 0; i < outerRings.size(); i++) {
                List<int[]> outer = outerRings.get(i);
                long area = signedArea(outer);
                if (area < bestArea && containsPoint(outer, testX, testZ)) {
                    bestOuter = i;
                    bestArea = area;
                }
            }
            if (bestOuter != -1) {
                holesPerOuter.get(bestOuter).add(toPoints(hole));
            }
        }

        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < outerRings.size(); i++) {
            regions.add(new Region(toPoints(outerRings.get(i)), holesPerOuter.get(i)));
        }
        return regions;
    }

    /**
     * Picks the edge that continues the ring from the end of the current edge. Where two regions
     * touch diagonally at a corner, the rightmost turn is preferred so each ring hugs its own chunks.
     */
    private static int pickNextEdge(List<int[]> edges, Map<String, List<Integer>> outgoing, int[] curEdge) {
        List<Integer> candidates = outgoing.getOrDefault(curEdge[2] + "," + curEdge[3], List.of());
        int dx = Integer.signum(curEdge[2] - curEdge[0]);
        int dz = Integer.signum(curEdge[3] - curEdge[1]);
        // Right, straight, then left (z points south, so right of (dx, dz) is (-dz, dx))
        int[][] preferred = {{-dz, dx}, {dx, dz}, {dz, -dx}};
        for (int[] dir : preferred) {
            for (int idx : candidates) {
                int[] e = edges.get(idx);
                if (Integer.signum(e[2] - e[0]) == dir[0] && Integer.signum(e[3] - e[1]) == dir[1]) {
                    return idx;
                }
            }
        }
        return -1;
    }

    /**
     * Shoelace signed area (doubled). Positive for clockwise rings in x east / z south space.
     */
    private static long signedArea(List<int[]> ring) {
        long sum = 0;
        for (int i = 0; i < ring.size(); i++) {
            int[] p = ring.get(i);
            int[] q = ring.get((i + 1) % ring.size());
            sum += (long) p[0] * q[1] - (long) q[0] * p[1];
        }
        return sum;
    }

    /**
     * Ray-casting point-in-polygon test.
     */
    private static boolean containsPoint(List<int[]> ring, double x, double z) {
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            int[] p = ring.get(i);
            int[] q = ring.get(j);
            if ((p[1] > z) != (q[1] > z)
                    && x < (double) (q[0] - p[0]) * (z - p[1]) / (q[1] - p[1]) + p[0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static List<Point> toPoints(List<int[]> ring) {
        List<Point> points = new ArrayList<>(ring.size());
        for (int[] p : ring) {
            points.add(Point.of(p[0], p[1]));
        }
        return points;
    }

    /**
     * Converts a stored mapColor string into a Color.
     */
    private static Color parseColor(String mapColor) {
        if (mapColor == null || mapColor.isEmpty()) {
            // Cornflower blue - clearly visible on terrain maps
            return new Color(100, 149, 237);
        }
        // Hex - &#RRGGBB
        if (mapColor.startsWith("&#") && mapColor.length() == 8) {
            try {
                return Color.decode("#" + mapColor.substring(2));
            } catch (NumberFormatException e) {
                return Color.WHITE;
            }
        }
        // Named Minecraft color codes
        return switch (mapColor) {
            case "&0" -> new Color(0, 0, 0);
            case "&1" -> new Color(0, 0, 170);
            case "&2" -> new Color(0, 170, 0);
            case "&3" -> new Color(0, 170, 170);
            case "&4" -> new Color(170, 0, 0);
            case "&5" -> new Color(170, 0, 170);
            case "&6" -> new Color(255, 170, 0);
            case "&7" -> new Color(170, 170, 170);
            case "&8" -> new Color(85, 85, 85);
            case "&9" -> new Color(85, 85, 255);
            case "&a" -> new Color(85, 255, 85);
            case "&b" -> new Color(85, 255, 255);
            case "&c" -> new Color(255, 85, 85);
            case "&d" -> new Color(255, 85, 255);
            case "&e" -> new Color(255, 255, 85);
            case "&f" -> Color.WHITE;
            default -> Color.WHITE;
        };
    }
}
