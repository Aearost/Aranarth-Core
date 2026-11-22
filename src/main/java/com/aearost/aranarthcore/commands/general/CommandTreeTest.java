package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.trees.CustomTrees;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.trees.TreeBuilder;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.crops.CropType;
import com.aearost.aranarthcore.crops.Crops;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
    /** Every tree that bears fruit, grown with its fruit on it. */
    private static final List<Variant> REVIEW = List.of(Variant.APPLE_TREE, Variant.GREEN_APPLE_TREE, Variant.PEAR_TREE,
            Variant.BLOSSOM_CHERRY, Variant.DATE_PALM, Variant.COCONUT_PALM, Variant.BANANA_PLANT, Variant.LEMON_TREE,
            Variant.ORANGE_TREE, Variant.LIME_TREE, Variant.MANGO_TREE, Variant.OLIVE_TREE, Variant.PLUM_TREE,
            Variant.PEACH_TREE, Variant.POMEGRANATE_TREE, Variant.APRICOT_TREE, Variant.FIG_TREE, Variant.AVOCADO_TREE,
            Variant.PERSIMMON_TREE, Variant.LYCHEE_TREE);
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
        // "crops" grows every custom crop at each of its stages, one row per crop
        if (args.length > 0 && args[0].equalsIgnoreCase("crops")) {
            grid = grid == null ? 0 : grid + 1;
            world.getPersistentDataContainer().set(GRID_KEY, PersistentDataType.INTEGER, grid);
            int originX = grid * 1000;
            buildCrops(player, world, originX);
            Location start = new Location(world, originX - 6, world.getHighestBlockYAt(originX, 0) + 8, -6, -45, 30);
            AranarthUtils.teleportPlayer(player, player.getLocation(), start, true,
                    Lang.getFor(player, "treetest.title"), "", success -> {});
            return true;
        }
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
        List<Variant> fruit = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            (variant.isFruit() ? fruit : variant.isRare() ? rare : normal).add(variant);
        }
        List<Material> saplingOrder = normal.stream().map(Variant::getSapling).distinct().toList();
        normal.sort(Comparator.comparingInt(variant -> saplingOrder.indexOf(variant.getSapling())));
        normal.addAll(rare);
        normal.addAll(fruit);
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
                TreeBuilder tree = CustomTrees.design(variant, world, new Random(seed), x, y, z, cell[1], true);
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
                        tree.applyHeadTextures();
                    }
                }
                manifest.add(line.toString());
                String idea = tree == null ? null : tree.getInfo().get("idea");
                spawnLabel(player, world, x, y, z - width / 3, variant, cell[1], idea);
            }
        }.runTaskTimer(AranarthCore.getInstance(), 1L, 1L);
    }

    /**
     * Grows every custom crop at each of its stages.
     */
    private void buildCrops(Player player, World world, int originX) {
        CropType[] types = CropType.values();
        for (int row = 0; row < types.length; row++) {
            CropType type = types[row];
            int z = row * 8;
            for (int stage = 0; stage < type.getStages(); stage++) {
                int x = originX + stage * 7;
                world.getChunkAt(x >> 4, z >> 4).load(true);
                int y = world.getHighestBlockYAt(x, z);
                Block soil = world.getBlockAt(x, y, z);
                switch (type.getSoil()) {
                    case FARMLAND -> soil.setType(Material.FARMLAND, false);
                    case MUD_BANK -> {
                        soil.setType(Material.MUD, false);
                        soil.getRelative(BlockFace.NORTH).setType(Material.WATER, false);
                    }
                    default -> soil.setType(Material.GRASS_BLOCK, false);
                }
                // A vine grows beside a post of 4 fences, with a rail across its top to spread over
                if (type.isVine()) {
                    Block post = soil.getRelative(BlockFace.NORTH);
                    post.setType(Material.GRASS_BLOCK, false);
                    for (int h = 1; h <= 4; h++) {
                        post.getRelative(0, h, 0).setType(Material.OAK_FENCE, false);
                    }
                    for (int dx = -2; dx <= 2; dx++) {
                        if (dx != 0) {
                            post.getRelative(dx, 4, 0).setType(Material.OAK_FENCE, false);
                        }
                    }
                    for (int dx = -2; dx <= 2; dx++) {
                        post.getRelative(dx, 4, 0).getState().update(true, true);
                    }
                }
                // Each one stays at the stage it shows until it is first picked, so every stage can be compared
                Crops.place(soil.getRelative(BlockFace.UP), type, stage, true);
                String stageText = stage == type.getRipeStage() ? "&aRipe" : stage >= type.getHarvestStage() ? "&eCan be picked"
                        : "&7Stage " + (stage + 1);
                spawnText(world, x, y + 5, z, "&f" + Lang.getFor(player, type.getYield().getLangKey()) + "\n" + stageText);
            }
            String how = type.getMode() == CropType.HarvestMode.REPLANT ? "&7Break when ripe, then replant"
                    : "&7Right-click or break the fruit when ripe, grows back";
            spawnText(world, originX - 5, world.getHighestBlockYAt(originX, z) + 3, z,
                    "&e&l" + Lang.getFor(player, type.getYield().getLangKey()) + "\n" + how
                            + "\n&7Plant: " + Lang.getFor(player, type.getPlant().getLangKey()) + " &7on "
                            + type.getSoil().name().toLowerCase().replace('_', ' '));
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "treetest.done")));
    }

    private void spawnText(World world, int x, int y, int z, String text) {
        world.spawn(new Location(world, x + 0.5, y, z + 0.5), TextDisplay.class, display -> {
            display.setText(ChatUtils.translateToColor(text));
            display.setBillboard(Display.Billboard.CENTER);
        });
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
        String where = variant.isFruit() ? "Fruit sapling: " + Lang.getFor(player, "produce." + variant.getFruitId())
                : variant.getCondition() == null ? "Anywhere" : variant.getCondition();
        String text = "&e&l#" + variant.getId() + (char) ('a' + sample) + " &f&l" + name
                + "\n&7" + where
                + "\n" + Lang.getFor(player, "trees.sapling_" + saplingKey) + " &7" + size + "x" + size;
        world.spawn(location, TextDisplay.class, display -> {
            display.setText(ChatUtils.translateToColor(text));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(3, 3, 3), new AxisAngle4f()));
        });
    }
}
