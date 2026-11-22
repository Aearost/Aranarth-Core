package com.aearost.aranarthcore.crops;

import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceHeads;
import com.aearost.aranarthcore.items.produce.ProduceType;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * What a crop looks like at one of its stages, including all blocks and ItemDisplay components that make up the crop.
 */
public class CropLayout {

    /**
     * A block offset from the base of the crop.
     */
    public record Offset(int x, int y, int z) {
        public Offset add(int dx, int dy, int dz) {
            return new Offset(x + dx, y + dy, z + dz);
        }
    }

    /**
     * A head showing a piece of fruit.
     * @param texture The base64 head texture.
     * @param facing The direction of a wall head, or the rotation of a head on the ground.
     * @param wall Whether it clings to the side of the block next to it.
     */
    public record Head(Offset at, String texture, BlockFace facing, boolean wall) {}

    /**
     * A small item shown floating in or around the crop, such as a berry on a bush.
     * @param faceViewer Whether it turns to face whoever looks at it, like a dropped item, for flat items.
     * @param yaw The direction a flat item that does not face the viewer is turned to, in degrees.
     */
    public record Display(Vector at, ItemStack item, float scale, boolean faceViewer, float yaw) {
        public Display(Vector at, ItemStack item, float scale, boolean faceViewer) {
            this(at, item, scale, faceViewer, 0F);
        }

        public Display(Vector at, ItemStack item, float scale) {
            this(at, item, scale, false, 0F);
        }
    }

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final BlockFace[] ROTATIONS = {BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

    private final Map<Offset, BlockData> blocks = new LinkedHashMap<>();
    private final List<Head> heads = new ArrayList<>();
    private final List<Display> displays = new ArrayList<>();

    public Map<Offset, BlockData> getBlocks() {
        return blocks;
    }

    public List<Head> getHeads() {
        return heads;
    }

    public List<Display> getDisplays() {
        return displays;
    }

    /**
     * Provides every block the crop takes up, heads included.
     */
    public List<Offset> getOffsets() {
        List<Offset> offsets = new ArrayList<>(blocks.keySet());
        for (Head head : heads) {
            offsets.add(head.at());
        }
        return offsets;
    }

    /**
     * Determines if the block is a head of fruit, which is picked when broken.
     */
    public boolean isFruit(Offset offset) {
        for (Head head : heads) {
            if (head.at().equals(offset)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Works out what a crop looks like at a stage.
     * @param seed The crop's seed, which decides its small differences, such as how many ears of corn it grows.
     * @param side For a stem, the side its fruit grows on, from 0 to 3.
     */
    public static CropLayout of(CropType type, int stage, long seed, int side) {
        CropLayout layout = new CropLayout();
        Random r = new Random(seed);
        Offset base = new Offset(0, 0, 0);
        switch (type) {
            case BLUEBERRY, RASPBERRY, BLACKBERRY, STRAWBERRY -> {
                // A small bush, a full bush, a few small berries, then many full-sized ones
                layout.blocks.put(base, berryBush(stage == 0 ? 0 : 1));
                if (stage >= 2) {
                    boolean full = stage >= 3;
                    int count = full ? type.getYieldMin() + r.nextInt(type.getYieldMax() - type.getYieldMin() + 1)
                            : 1 + r.nextInt(3);
                    for (int i = 0; i < count; i++) {
                        layout.berry(type.getYield(), outerPoint(r), full ? 0.22F : 0.15F);
                    }
                }
            }
            case TOMATO -> layout = tomato(stage, -1);
            case BELL_PEPPER -> layout = pepper(stage, seed, defaultPieces(type, stage, seed, 0, 0));
            case CORN -> {
                int ears = 2 + r.nextInt(3);
                switch (stage) {
                    case 0 -> layout.blocks.put(base, pitcher(0));
                    case 1 -> layout.blocks.put(base, Material.SHORT_GRASS.createBlockData());
                    case 2 -> layout.blocks.put(base, half(Material.TALL_GRASS, Bisected.Half.BOTTOM));
                    default -> {
                        layout.blocks.put(base, half(Material.TALL_GRASS, Bisected.Half.BOTTOM));
                        layout.blocks.put(base.add(0, 1, 0), half(Material.TALL_GRASS, Bisected.Half.TOP));
                        if (stage >= 4) {
                            Material candle = stage == 4 ? Material.GREEN_CANDLE : stage == 5 ? Material.LIME_CANDLE
                                    : Material.YELLOW_CANDLE;
                            Candle data = (Candle) candle.createBlockData();
                            data.setCandles(stage == 4 ? 1 : ears);
                            data.setLit(false);
                            layout.blocks.put(base.add(0, 2, 0), data);
                        }
                    }
                }
            }
            case RICE -> layout.blocks.put(base, (stage == 0 ? Material.SHORT_GRASS : stage == 1 ? Material.FERN
                    : Material.TALL_DRY_GRASS).createBlockData());
            case ONION -> layout.blocks.put(base, pitcher(Math.min(stage, 2)));
            case GARLIC -> {
                // Short grass, then a seed pod among tufts of grass, which makes it bushier than an onion
                if (stage == 0) {
                    layout.blocks.put(base, Material.SHORT_GRASS.createBlockData());
                } else {
                    layout.blocks.put(base, pitcher(stage == 1 ? 0 : 1));
                    ItemStack grass = new ItemStack(Material.SHORT_GRASS);
                    int tufts = stage == 1 ? 2 : 4;
                    float scale = stage == 1 ? 0.55F : 0.7F;
                    List<BlockFace> sides = shuffled(r);
                    for (int i = 0; i < tufts; i++) {
                        BlockFace face = sides.get(i);
                        Vector at = new Vector(0.5 + face.getModX() * 0.22, scale * 0.5, 0.5 + face.getModZ() * 0.22);
                        // Two crossed sprites per tuft, like a placed plant
                        float yaw = r.nextInt(90);
                        layout.displays.add(new Display(at, grass, scale, false, yaw + 45));
                        layout.displays.add(new Display(at, grass, scale, false, yaw + 135));
                    }
                }
            }
            case SWEET_POTATO -> {
                // A beetroot plant, whose red-veined leaves are close to a real sweet potato's
                Ageable plant = (Ageable) Material.BEETROOTS.createBlockData();
                plant.setAge(Math.min(stage, plant.getMaximumAge()));
                layout.blocks.put(base, plant);
            }
            case CABBAGE, LETTUCE -> {
                // Short grass, with the head growing bigger among it until it is full-sized
                layout.blocks.put(base, Material.SHORT_GRASS.createBlockData());
                if (stage >= 1) {
                    float scale = new float[]{0.3F, 0.55F, 0.85F}[Math.min(stage, 3) - 1];
                    String texture = ProduceHeads.get(type.getYield().getId());
                    if (texture != null) {
                        layout.displays.add(new Display(new Vector(0.5, scale * 0.5, 0.5), CropHeads.item(texture), scale));
                    }
                }
            }
            case CANTALOUPE -> {
                BlockFace face = SIDES[Math.floorMod(side, 4)];
                if (stage < 4) {
                    Ageable stem = (Ageable) Material.MELON_STEM.createBlockData();
                    stem.setAge(new int[]{0, 3, 5, 7}[stage]);
                    layout.blocks.put(base, stem);
                } else {
                    Directional stem = (Directional) Material.ATTACHED_MELON_STEM.createBlockData();
                    stem.setFacing(face);
                    layout.blocks.put(base, stem);
                    layout.heads.add(new Head(base.add(face.getModX(), 0, face.getModZ()),
                            ProduceHeads.get(type.getYield().getId()), ROTATIONS[r.nextInt(8)], false));
                }
            }
            case GREEN_GRAPES, RED_GRAPES, KIWI -> layout = vine(type, stage, seed, new Vine());
            case COFFEE -> layout = coffee(stage, seed, defaultPieces(type, stage, seed, 0, 0));
            case PINEAPPLE -> {
                // A fern, then a + of azalea leaves, with a pineapple growing from mini to full-sized in grass on the middle one
                if (stage == 0) {
                    layout.blocks.put(base, Material.FERN.createBlockData());
                } else {
                    layout.blocks.put(base, shrubLeaves(Material.AZALEA_LEAVES));
                }
                if (stage >= 2) {
                    for (BlockFace face : SIDES) {
                        layout.blocks.put(base.add(face.getModX(), 0, face.getModZ()), shrubLeaves(Material.AZALEA_LEAVES));
                    }
                }
                if (stage >= 3) {
                    layout.blocks.put(base.add(0, 1, 0), Material.SHORT_GRASS.createBlockData());
                    String texture = ProduceHeads.get(ProduceType.PINEAPPLE.getId());
                    if (texture != null) {
                        float scale = stage == 3 ? 0.25F : stage == 4 ? 0.55F : 1F;
                        layout.displays.add(new Display(new Vector(0.5, 1 + scale * 0.5, 0.5), CropHeads.item(texture), scale,
                                false, r.nextInt(360)));
                    }
                }
            }
        }
        layout.heads.removeIf(head -> head.texture() == null);
        return layout;
    }

    /**
     * One piece of fruit on a crop that grows and is picked and grows back on its own.
     * @param stage -1 while it has not grown, or else the stage it has grown to: a pepper's colour from 2 (green) to 5
     * (red), or the coffee plant's ripe stage for a bean.
     * @param start When it reached its stage, or was picked.
     * @param picks How many times it has been picked.
     */
    public record Piece(int stage, long start, int picks) {}

    /**
     * Provides the pieces of fruit a crop has while nothing has been saved for them yet.
     */
    public static List<Piece> defaultPieces(CropType type, int stage, long seed, long start, int picks) {
        int count = type == CropType.BELL_PEPPER ? pepperFaces(type, seed).size() : coffeeSpots(type, seed).size();
        int grown = type == CropType.BELL_PEPPER ? (stage >= 2 ? stage : -1) : (stage >= type.getRipeStage() ? stage : -1);
        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            pieces.add(new Piece(grown, start, picks));
        }
        return pieces;
    }

    /**
     * Provides the sides of its leaf block a bell pepper plant grows its peppers on, one pepper on each.
     */
    public static List<BlockFace> pepperFaces(CropType type, long seed) {
        Random r = new Random(seed);
        int count = type.getYieldMin() + r.nextInt(Math.max(1, type.getYieldMax() - type.getYieldMin() + 1));
        return shuffled(r).subList(0, Math.min(4, count));
    }

    /**
     * Provides where each coffee bean shows on the sides of the plant's leaves.
     */
    public static List<Vector> coffeeSpots(CropType type, long seed) {
        Random r = new Random(seed);
        int count = type.getYieldMin() + r.nextInt(Math.max(1, type.getYieldMax() - type.getYieldMin() + 1));
        List<Vector> spots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockFace face = SIDES[i % 4];
            double y = 1.15 + r.nextDouble() * 1.7;
            spots.add(new Vector(0.5 + face.getModX() * 0.56, y, 0.5 + face.getModZ() * 0.56));
        }
        return spots;
    }

    /**
     * Works out what a bell pepper plant looks like.
     */
    public static CropLayout pepper(int stage, long seed, List<Piece> pieces) {
        CropLayout layout = new CropLayout();
        Offset base = new Offset(0, 0, 0);
        layout.blocks.put(base, stage == 0 ? Material.JUNGLE_SAPLING.createBlockData() : shrubLeaves(Material.JUNGLE_LEAVES));
        if (stage == 0) {
            return layout;
        }
        List<BlockFace> faces = pepperFaces(CropType.BELL_PEPPER, seed);
        for (int i = 0; i < faces.size() && i < pieces.size(); i++) {
            if (pieces.get(i).stage() < 2) {
                continue;
            }
            String texture = ProduceHeads.get(pepperColour(pieces.get(i).stage()).getId());
            if (texture == null) {
                texture = ProduceHeads.get(ProduceType.ORANGE_BELL_PEPPER.getId());
            }
            BlockFace face = faces.get(i);
            layout.heads.add(new Head(base.add(face.getModX(), 0, face.getModZ()), texture, face, true));
        }
        layout.heads.removeIf(head -> head.texture() == null);
        return layout;
    }

    /**
     * Works out what a coffee plant looks like.
     */
    public static CropLayout coffee(int stage, long seed, List<Piece> pieces) {
        CropLayout layout = new CropLayout();
        Offset base = new Offset(0, 0, 0);
        layout.blocks.put(base, Material.AZALEA.createBlockData());
        if (stage >= 1) {
            layout.blocks.put(base.add(0, 1, 0), shrubLeaves(Material.AZALEA_LEAVES));
        }
        if (stage >= 2) {
            layout.blocks.put(base.add(0, 2, 0), shrubLeaves(Material.AZALEA_LEAVES));
        }
        if (stage >= CropType.COFFEE.getRipeStage()) {
            ItemStack beans = new Produce(ProduceType.COFFEE_BEANS).getItem();
            List<Vector> spots = coffeeSpots(CropType.COFFEE, seed);
            for (int i = 0; i < spots.size() && i < pieces.size(); i++) {
                if (pieces.get(i).stage() >= 0) {
                    layout.displays.add(new Display(spots.get(i), beans, 0.35F));
                }
            }
        }
        return layout;
    }

    /**
     * Works out what a tomato plant looks like.
     * @param lower The stage of the bush at the bottom: 0 small, 1 large, 2 ripe.
     * @param upper The stage of the bush on top of it: -1 not grown yet, 1 large, 2 ripe.
     */
    public static CropLayout tomato(int lower, int upper) {
        CropLayout layout = new CropLayout();
        layout.blocks.put(new Offset(0, 0, 0), berryBush(lower <= 0 ? 0 : lower == 1 ? 1 : 3));
        if (upper >= 1) {
            layout.blocks.put(new Offset(0, 1, 0), berryBush(upper == 1 ? 1 : 3));
        }
        return layout;
    }

    /**
     * Works out what a vine looks like, involving the leaves over the trellis and the fruits hanging from it.
     */
    public static CropLayout vine(CropType type, int stage, long seed, Vine vine) {
        CropLayout layout = new CropLayout();
        layout.blocks.put(new Offset(0, 0, 0), (stage == 0 ? Material.SHORT_GRASS : Material.FERN).createBlockData());
        for (Vine.Leaf leaf : vine.getLeaves()) {
            layout.blocks.put(leaf.at(), shrubLeaves(Material.JUNGLE_LEAVES));
            if (leaf.face() == null) {
                continue;
            }
            for (int k = 0; k < leaf.slots().size(); k++) {
                if (leaf.slots().get(k) == 0) {
                    layout.berry(type.getYield(), fruitSpot(type, seed, leaf, k), fruitScale(type));
                }
            }
        }
        return layout;
    }

    private static float fruitScale(CropType type) {
        return type == CropType.KIWI ? 0.3F : 0.4F;
    }

    /**
     * Provides where a piece of fruit on a vine's leaf shows, which always stays the same for that leaf and piece.
     * @param slot Which piece of fruit on the leaf.
     */
    public static Vector fruitSpot(CropType type, long seed, Vine.Leaf leaf, int slot) {
        Offset at = leaf.at();
        Random r = new Random(seed * 31 + (at.x() * 73856093L ^ at.y() * 19349663L ^ at.z() * 83492791L) * 7 + slot);
        float scale = fruitScale(type);
        double a = 0.22 + r.nextDouble() * 0.56;
        double b = 0.22 + r.nextDouble() * 0.56;
        BlockFace face = leaf.face();
        if (face == BlockFace.DOWN) {
            // Hanging, touching the bottom of the leaf
            return new Vector(at.x() + a, at.y() - 0.01, at.z() + b);
        }
        if (face == BlockFace.UP) {
            return new Vector(at.x() + a, at.y() + 1 + scale * 0.5, at.z() + b);
        }
        double top = at.y() + scale * 0.5 + r.nextDouble() * (0.92 - scale * 0.5);
        return new Vector(at.x() + (face.getModX() != 0 ? 0.5 + face.getModX() * 0.6 : a), top,
                at.z() + (face.getModZ() != 0 ? 0.5 + face.getModZ() * 0.6 : b));
    }

    /**
     * Provides the colour a bell pepper has ripened to at a stage: green, then yellow, orange and red.
     */
    public static ProduceType pepperColour(int stage) {
        return switch (stage) {
            case 3 -> ProduceType.YELLOW_BELL_PEPPER;
            case 4 -> ProduceType.ORANGE_BELL_PEPPER;
            case 5 -> ProduceType.RED_BELL_PEPPER;
            default -> ProduceType.GREEN_BELL_PEPPER;
        };
    }

    private void berry(ProduceType type, Vector at, float scale) {
        String texture = ProduceHeads.get(type.getId());
        ItemStack item = texture == null ? new Produce(type).getItem() : CropHeads.item(texture);
        displays.add(new Display(at, item, scale));
    }

    /**
     * Provides a spot on the outside of a bush, on one of its sides or its top, so its berries are seen straight away.
     */
    private static Vector outerPoint(Random r) {
        int face = r.nextInt(5);
        double a = 0.18 + r.nextDouble() * 0.64;
        double b = 0.18 + r.nextDouble() * 0.64;
        if (face == 4) {
            return new Vector(a, 0.9, b);
        }
        double y = 0.3 + r.nextDouble() * 0.5;
        double edge = face % 2 == 0 ? 0.08 : 0.92;
        return face < 2 ? new Vector(edge, y, a) : new Vector(a, y, edge);
    }

    private static List<BlockFace> shuffled(Random r) {
        List<BlockFace> sides = new ArrayList<>(List.of(SIDES));
        java.util.Collections.shuffle(sides, r);
        return sides;
    }

    private static BlockData berryBush(int age) {
        Ageable bush = (Ageable) Material.SWEET_BERRY_BUSH.createBlockData();
        bush.setAge(age);
        return bush;
    }

    private static BlockData pitcher(int age) {
        Ageable crop = (Ageable) Material.PITCHER_CROP.createBlockData();
        crop.setAge(age);
        return crop;
    }

    private static BlockData half(Material material, Bisected.Half half) {
        Bisected data = (Bisected) material.createBlockData();
        data.setHalf(half);
        return data;
    }

    private static BlockData vineTip() {
        Ageable vine = (Ageable) Material.TWISTING_VINES.createBlockData();
        vine.setAge(vine.getMaximumAge());
        return vine;
    }

    private static BlockData vineLeaves() {
        return shrubLeaves(Material.JUNGLE_LEAVES);
    }

    private static BlockData shrubLeaves(Material material) {
        Leaves leaves = (Leaves) material.createBlockData();
        leaves.setPersistent(true);
        return leaves;
    }
}
