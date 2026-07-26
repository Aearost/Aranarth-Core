package com.aearost.aranarthcore.integration;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
 * Integrates with the SquareMap live-map plugin to display dominion land claims.
 * The layer refreshes every 30 seconds.
 */
public class SquaremapIntegration {

    private static final Key LAYER_KEY = Key.of("aranarthcore-dominions");
    // One provider per world (keyed by squaremap world identifier string)
    private final Map<String, SimpleLayerProvider> providers = new HashMap<>();
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
            }
        } catch (IllegalStateException ignored) {
            // SquareMap already shut down
        }

        providers.clear();
    }

    /**
     * Clears and re-renders all dominion boundary polygons across every registered world.
     */
    public void refresh() {
        for (SimpleLayerProvider provider : providers.values()) {
            provider.clearMarkers();
        }

        // Snapshot to avoid ConcurrentModificationException on the async thread
        List<Dominion> snapshot = new ArrayList<>(DominionUtils.getDominions());

        for (Dominion dominion : snapshot) {
            List<Chunk> domChunks = dominion.getChunks();
            if (domChunks.isEmpty()) continue;

            Color fillColor = parseColor(dominion.getMapColor());
            // Two shades darker for the border so it contrasts against the fill
            Color strokeColor = fillColor.darker().darker();
            String dominionName = ChatUtils.stripColorFormatting(dominion.getName());

            MarkerOptions options = MarkerOptions.builder()
                    .strokeColor(strokeColor)
                    .strokeWeight(2)
                    .strokeOpacity(1.0)
                    .fillColor(fillColor)
                    .fillOpacity(0.45)
                    .hoverTooltip("<b>" + dominionName + "</b>")
                    .build();

            // Group chunks by world so we register markers on the correct map
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

                // Compute outer-boundary polygons (one per contiguous region)
                List<List<Point>> polygons = buildBoundaryPolygons(entry.getValue());

                int regionIdx = 0;
                for (List<Point> points : polygons) {
                    Marker polygon = Marker.polygon(points);
                    polygon.markerOptions(options);
                    // Key: "d-<uuid>-<worldIdx>-<regionIdx>"
                    Key markerKey = Key.of("d-" + dominion.getId() + "-" + entry.getKey().hashCode() + "-r" + regionIdx++);
                    provider.addMarker(markerKey, polygon);
                }
            }
        }
    }

    /**
     * Computes the outer boundary polygons for all contiguous regions.
     * @param chunkCoords Set of "cx,cz" strings for a single dominion in one world.
     * @return A list of vertex lists, one per contiguous region.
     */
    private static List<List<Point>> buildBoundaryPolygons(Set<String> chunkCoords) {
        // Collect all outer boundary edges - sides of chunks that have no same-dominion neighbor
        List<int[]> edges = new ArrayList<>();

        for (String coord : chunkCoords) {
            String[] parts = coord.split(",");
            int cx = Integer.parseInt(parts[0]);
            int cz = Integer.parseInt(parts[1]);
            int bx = cx * 16;
            int bz = cz * 16;

            // No northern neighbor
            if (!chunkCoords.contains(cx + "," + (cz - 1))) {
                edges.add(new int[]{bx, bz, bx + 16, bz});
            }
            // No southern neighbor
            if (!chunkCoords.contains(cx + "," + (cz + 1))) {
                edges.add(new int[]{bx, bz + 16, bx + 16, bz + 16});
            }
            // No western neighbor
            if (!chunkCoords.contains((cx - 1) + "," + cz)) {
                edges.add(new int[]{bx, bz, bx, bz + 16});
            }
            // No eastern neighbor
            if (!chunkCoords.contains((cx + 1) + "," + cz)) {
                edges.add(new int[]{bx + 16, bz, bx + 16, bz + 16});
            }
        }

        if (edges.isEmpty()) return List.of();

        // List of edge indices that touch that point
        Map<String, List<Integer>> adj = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) {
            int[] e = edges.get(i);
            String k1 = e[0] + "," + e[1];
            String k2 = e[2] + "," + e[3];
            adj.computeIfAbsent(k1, k -> new ArrayList<>()).add(i);
            adj.computeIfAbsent(k2, k -> new ArrayList<>()).add(i);
        }

        // Trace each connected chain of edges into a closed polygon
        boolean[] used = new boolean[edges.size()];
        List<List<Point>> polygons = new ArrayList<>();

        for (int startIdx = 0; startIdx < edges.size(); startIdx++) {
            if (used[startIdx]) continue;

            List<Point> polygon = new ArrayList<>();
            int[] startEdge = edges.get(startIdx);
            String startPtKey = startEdge[0] + "," + startEdge[1];
            String curPtKey = startPtKey;
            int curIdx = startIdx;

            do {
                used[curIdx] = true;
                String[] ptParts = curPtKey.split(",");
                polygon.add(Point.of(Integer.parseInt(ptParts[0]), Integer.parseInt(ptParts[1])));

                // Determine which end of this edge we're at, then move to the other end
                int[] curEdge = edges.get(curIdx);
                String k1 = curEdge[0] + "," + curEdge[1];
                String nextPtKey = curPtKey.equals(k1)
                        ? curEdge[2] + "," + curEdge[3]
                        : k1;

                // Pick the next unvisited edge from nextPtKey
                int nextIdx = -1;
                for (int neighborIdx : adj.getOrDefault(nextPtKey, List.of())) {
                    if (!used[neighborIdx]) {
                        nextIdx = neighborIdx;
                        break;
                    }
                }

                if (nextIdx == -1) break;
                curPtKey = nextPtKey;
                curIdx = nextIdx;
            } while (!curPtKey.equals(startPtKey));

            if (polygon.size() >= 3) {
                polygons.add(polygon);
            }
        }

        return polygons;
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
