package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.trees.CustomTrees;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.trees.TreeBuilder;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Temporary command that teleports to a superflat world showing a grid of every custom tree.
 */
public class CommandTreeTest implements CommandExecutor {

    private static final String WORLD_NAME = "creative_treetest";
    private static final String FLAT_SETTINGS = "{\"layers\":["
            + "{\"block\":\"minecraft:bedrock\",\"height\":1},"
            + "{\"block\":\"minecraft:stone\",\"height\":40},"
            + "{\"block\":\"minecraft:dirt\",\"height\":6},"
            + "{\"block\":\"minecraft:grass_block\",\"height\":1}],"
            + "\"biome\":\"minecraft:plains\",\"lakes\":false,\"features\":false}";
    private static final int SAMPLES = 4;
    private static final int CELL = 48;
    private static final List<Variant> REVIEW = List.of(Variant.ROUND_OAK, Variant.TALL_FOREST_OAK, Variant.JUNGLE_PARASOL,
            Variant.ELDER_CHERRY);
    private static final List<Variant> ALL = showcase();
    private static final NamespacedKey GRID_KEY = new NamespacedKey(AranarthCore.getInstance(), "treetest_grid");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage(Lang.getDefault("general.no_permission_console")));
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission")));
            return true;
        }

        World world = getOrCreateWorld();
        Integer grid = world.getPersistentDataContainer().get(GRID_KEY, PersistentDataType.INTEGER);
        // "regen" grows the designs under review, "all" grows every custom tree
        boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");
        boolean regenerate = all || args.length > 0 && args[0].equalsIgnoreCase("regen");
        if (grid == null || regenerate) {
            // Every new grid is grown further along so the old one never needs to be cleared
            grid = grid == null ? 0 : grid + 1;
            world.getPersistentDataContainer().set(GRID_KEY, PersistentDataType.INTEGER, grid);
            buildGrid(player, world, grid * 1000, all ? ALL : REVIEW);
        }

        int originX = grid * 1000;
        Location start = new Location(world, originX + 4, world.getHighestBlockYAt(originX + 4, 4) + 12, 4, -45, 20);
        AranarthUtils.teleportPlayer(player, player.getLocation(), start, true,
                Lang.getFor(player, "treetest.title"), "", success -> {});
        return true;
    }

    /**
     * Lists every custom tree with custom grouping.
     */
    private static List<Variant> showcase() {
        List<Variant> normal = new ArrayList<>();
        List<Variant> rare = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            (variant.isRare() ? rare : normal).add(variant);
        }
        List<Material> saplingOrder = normal.stream().map(Variant::getSapling).distinct().toList();
        normal.sort(Comparator.comparingInt(variant -> saplingOrder.indexOf(variant.getSapling())));
        normal.addAll(rare);
        return normal;
    }

    private World getOrCreateWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) {
            return world;
        }
        world = new WorldCreator(WORLD_NAME).type(WorldType.FLAT).generatorSettings(FLAT_SETTINGS)
                .generateStructures(false).createWorld();
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        world.setTime(6000);
        return world;
    }

    /**
     * Grows each of the trees several times, one tree per tick, each labelled with its name, where it grows,
     * its sapling and the size of its square of saplings.
     */
    private void buildGrid(Player player, World world, int originX, List<Variant> trees) {
        List<int[]> cells = new ArrayList<>();
        Variant[] variants = trees.toArray(new Variant[0]);
        // Huge trees get wider cells so that they never grow into their neighbours
        int[] rowWidth = new int[variants.length];
        int[] rowZ = new int[variants.length];
        int nextZ = 0;
        for (int row = 0; row < variants.length; row++) {
            rowWidth[row] = Math.max(CELL, variants[row].getSize() * 15);
            rowZ[row] = nextZ + rowWidth[row] / 2;
            nextZ += rowWidth[row];
            for (int sample = 0; sample < SAMPLES; sample++) {
                cells.add(new int[] {row, sample});
            }
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "treetest.generating", "count", String.valueOf(cells.size()))));

        Random random = new Random();
        List<String> manifest = new ArrayList<>();
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= cells.size()) {
                    writeManifest(originX / 1000, manifest);
                    if (player.isOnline()) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "treetest.done")));
                    }
                    cancel();
                    return;
                }
                int[] cell = cells.get(index++);
                Variant variant = variants[cell[0]];
                int width = rowWidth[cell[0]];
                int x = originX + width / 2 + cell[1] * width;
                int z = rowZ[cell[0]];
                world.getChunkAt(x >> 4, z >> 4).load(true);
                int y = world.getHighestBlockYAt(x, z) + 1;

                long seed = random.nextLong();
                // A variant still being compared between several ideas grows a different idea for each sample
                TreeBuilder tree = CustomTrees.design(variant, world, new Random(seed), x, y, z, cell[1]);
                StringBuilder line = new StringBuilder("#" + variant.getId() + (char) ('a' + cell[1])
                        + " " + variant.getDisplayName() + " seed=" + seed);
                if (tree != null) {
                    tree.getInfo().forEach((key, value) -> line.append(" ").append(key).append("=").append(value));
                    List<BlockState> states = tree.resolve(new Location(world, x, y, z), new HashSet<>());
                    if (states == null) {
                        line.append(" blocked=true");
                    } else {
                        for (BlockState state : states) {
                            state.getBlock().setBlockData(state.getBlockData(), false);
                        }
                    }
                }
                manifest.add(line.toString());
                String idea = tree == null ? null : tree.getInfo().get("idea");
                spawnLabel(player, world, x, y, z - width / 3, variant, cell[1], idea);
            }
        }.runTaskTimer(AranarthCore.getInstance(), 1L, 1L);
    }

    /**
     * Saves the seed and settings of every tree in the grid, so that liked trees can be traced back to their settings.
     */
    private void writeManifest(int grid, List<String> lines) {
        File file = new File(AranarthCore.getInstance().getDataFolder(), "treetest_grid_" + grid + ".txt");
        try {
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to save the tree test grid: " + e.getMessage());
        }
    }

    private void spawnLabel(Player player, World world, int x, int y, int z, Variant variant, int sample, String idea) {
        Location location = new Location(world, x + 0.5, y + 3, z + 0.5);
        String saplingKey = variant.getSapling().name().toLowerCase().replace("_sapling", "").replace("_fungus", "")
                .replace("_propagule", "");
        int size = Math.max(1, variant.getSize());
        String name = idea == null ? variant.getDisplayName() : idea.replace('_', ' ');
        String text = "&e&l#" + variant.getId() + (char) ('a' + sample) + " &f&l" + name
                + "\n&7" + (variant.getCondition() == null ? "Anywhere" : variant.getCondition())
                + "\n" + Lang.getFor(player, "trees.sapling_" + saplingKey) + " &7" + size + "x" + size;
        world.spawn(location, TextDisplay.class, display -> {
            display.setText(ChatUtils.translateToColor(text));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(3, 3, 3), new AxisAngle4f()));
        });
    }
}
