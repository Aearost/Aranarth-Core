package com.aearost.aranarthcore.crops;

import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.FruitUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.MobHeadUtils;
import com.gmail.nossr50.api.ExperienceAPI;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Plants, grows, picks and removes the custom crops.
 */
public class Crops {

    /**
     * A crop as it is saved.
     * @param stageStart When it reached its current stage.
     * @param harvests How many times it has been picked.
     * @param seed Decides its small differences, such as how many ears of corn it grows.
     * @param side For a stem, the side its fruit grows on.
     * @param frozen Whether it stays at its stage, like the crops shown in the test world, until it is first picked.
     */
    public record Crop(CropType type, int stage, long stageStart, int harvests, long seed, int side, boolean frozen) {
        public boolean isRipe() {
            return stage >= type.getHarvestStage();
        }

        /**
         * Determines if it has been picked as many times as it can be, so it stays barren until broken.
         */
        public boolean isBarren() {
            return type.getMaxHarvests() > 0 && harvests >= type.getMaxHarvests();
        }

        public CropLayout layout() {
            return CropLayout.of(type, stage, seed, side);
        }
    }

    private static final String KEY_PREFIX = "crop_";
    private static final String VINE_PREFIX = "cropvine_";
    public static final NamespacedKey DISPLAY_KEY = new NamespacedKey("aranarthcore", "crop_display");
    private static final Map<UUID, Map<Long, Long>> INDEX = new HashMap<>();
    private static final Map<UUID, Set<Long>> BASES = new HashMap<>();

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static Block unpack(World world, long key) {
        int x = (int) (key >> 38);
        int z = (int) (key << 26 >> 38);
        int y = (int) (key << 52 >> 52);
        return world.getBlockAt(x, y, z);
    }

    private static NamespacedKey getKey(Block base) {
        return new NamespacedKey("aranarthcore", KEY_PREFIX + (base.getX() & 15) + "_" + base.getY() + "_" + (base.getZ() & 15));
    }

    public static Crop get(Block base) {
        String value = base.getChunk().getPersistentDataContainer().get(getKey(base), PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        String[] p = value.split(";");
        try {
            CropType type = CropType.fromId(p[0]);
            if (type == null) {
                return null;
            }
            return new Crop(type, Integer.parseInt(p[1]), Long.parseLong(p[2]), Integer.parseInt(p[3]), Long.parseLong(p[4]),
                    Integer.parseInt(p[5]), p.length > 6 && p[6].equals("1"));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void save(Block base, Crop crop) {
        base.getChunk().getPersistentDataContainer().set(getKey(base), PersistentDataType.STRING, crop.type().getId() + ";"
                + crop.stage() + ";" + crop.stageStart() + ";" + crop.harvests() + ";" + crop.seed() + ";" + crop.side() + ";"
                + (crop.frozen() ? 1 : 0));
    }

    /**
     * Provides the key of what else is saved for a crop.
     */
    private static NamespacedKey getExtraKey(Block base) {
        return new NamespacedKey("aranarthcore", VINE_PREFIX + (base.getX() & 15) + "_" + base.getY() + "_" + (base.getZ() & 15));
    }

    /**
     * Provides the leaves a vine has grown and the fruit on them.
     */
    public static Vine vine(Block base) {
        return Vine.parse(base.getChunk().getPersistentDataContainer().get(getExtraKey(base), PersistentDataType.STRING));
    }

    private static void saveVine(Block base, Vine vine) {
        base.getChunk().getPersistentDataContainer().set(getExtraKey(base), PersistentDataType.STRING, vine.serialize());
    }

    /**
     * The bush on top of a tomato plant, which grows, ripens and is picked on its own.
     * @param stage -1 while it has not grown, 1 when large, 2 when ripe.
     * @param start When it reached its stage, or 0 while waiting for the bush below it to grow large.
     * @param picks How many times it has been picked.
     */
    private record TomatoTop(int stage, long start, int picks) {}

    private static TomatoTop tomatoTop(Block base) {
        String value = base.getChunk().getPersistentDataContainer().get(getExtraKey(base), PersistentDataType.STRING);
        if (value != null && value.startsWith("T:")) {
            String[] p = value.substring(2).split(",");
            try {
                return new TomatoTop(Integer.parseInt(p[0]), Long.parseLong(p[1]), Integer.parseInt(p[2]));
            } catch (RuntimeException ignored) {
                // A malformed value is read as a bush that has not grown
            }
        }
        return new TomatoTop(-1, 0, 0);
    }

    private static void saveTomatoTop(Block base, TomatoTop top) {
        base.getChunk().getPersistentDataContainer().set(getExtraKey(base), PersistentDataType.STRING,
                "T:" + top.stage() + "," + top.start() + "," + top.picks());
    }

    /**
     * Provides what a crop looks like now.
     */
    public static CropLayout layoutOf(Block base, Crop crop) {
        if (crop.type().isVine()) {
            return CropLayout.vine(crop.type(), crop.stage(), crop.seed(), vine(base));
        }
        if (crop.type() == CropType.TOMATO) {
            return CropLayout.tomato(crop.stage(), tomatoTop(base).stage());
        }
        if (crop.type() == CropType.BELL_PEPPER) {
            return CropLayout.pepper(crop.stage(), crop.seed(), pieces(base, crop));
        }
        if (crop.type() == CropType.COFFEE) {
            return CropLayout.coffee(crop.stage(), crop.seed(), pieces(base, crop));
        }
        return crop.layout();
    }

    /**
     * Provides each piece of fruit on a crop whose pieces grow and are picked on their own, such as each bell pepper.
     */
    private static List<CropLayout.Piece> pieces(Block base, Crop crop) {
        List<CropLayout.Piece> pieces = CropLayout.defaultPieces(crop.type(), crop.stage(), crop.seed(), crop.stageStart(),
                crop.harvests());
        String value = base.getChunk().getPersistentDataContainer().get(getExtraKey(base), PersistentDataType.STRING);
        if (value == null || !value.startsWith("P:") || value.length() == 2) {
            return pieces;
        }
        String[] entries = value.substring(2).split(";");
        try {
            // The number of pieces follows the crop's yield, so any it no longer has are dropped and new ones are not grown
            for (int i = 0; i < entries.length && i < pieces.size(); i++) {
                String[] p = entries[i].split(",");
                pieces.set(i, new CropLayout.Piece(Integer.parseInt(p[0]), Long.parseLong(p[1]), Integer.parseInt(p[2])));
            }
            for (int i = entries.length; i < pieces.size(); i++) {
                pieces.set(i, new CropLayout.Piece(-1, System.currentTimeMillis(), 0));
            }
        } catch (RuntimeException ignored) {
            // A malformed value is read as pieces following the crop's own stage
        }
        return pieces;
    }

    private static void savePieces(Block base, List<CropLayout.Piece> pieces) {
        StringBuilder value = new StringBuilder("P:");
        for (int i = 0; i < pieces.size(); i++) {
            CropLayout.Piece piece = pieces.get(i);
            if (i > 0) {
                value.append(';');
            }
            value.append(piece.stage()).append(',').append(piece.start()).append(',').append(piece.picks());
        }
        base.getChunk().getPersistentDataContainer().set(getExtraKey(base), PersistentDataType.STRING, value.toString());
    }

    /**
     * Determines if the block is part of a crop.
     */
    public static boolean isCropBlock(Block block) {
        Map<Long, Long> index = INDEX.get(block.getWorld().getUID());
        return index != null && index.containsKey(pack(block.getX(), block.getY(), block.getZ()));
    }

    /**
     * Provides the base of the crop the block is part of, or null if it is not part of one.
     */
    public static Block getBase(Block block) {
        Map<Long, Long> index = INDEX.get(block.getWorld().getUID());
        if (index == null) {
            return null;
        }
        Long base = index.get(pack(block.getX(), block.getY(), block.getZ()));
        return base == null ? null : unpack(block.getWorld(), base);
    }

    private static void index(Block base, CropLayout layout) {
        UUID world = base.getWorld().getUID();
        long baseKey = pack(base.getX(), base.getY(), base.getZ());
        Map<Long, Long> index = INDEX.computeIfAbsent(world, k -> new HashMap<>());
        index.values().removeIf(v -> v == baseKey);
        for (CropLayout.Offset offset : layout.getOffsets()) {
            index.put(pack(base.getX() + offset.x(), base.getY() + offset.y(), base.getZ() + offset.z()), baseKey);
        }
        index.put(baseKey, baseKey);
        BASES.computeIfAbsent(world, k -> new HashSet<>()).add(baseKey);
    }

    private static void unindex(Block base) {
        UUID world = base.getWorld().getUID();
        long baseKey = pack(base.getX(), base.getY(), base.getZ());
        Map<Long, Long> index = INDEX.get(world);
        if (index != null) {
            index.values().removeIf(v -> v == baseKey);
        }
        Set<Long> bases = BASES.get(world);
        if (bases != null) {
            bases.remove(baseKey);
        }
    }

    /**
     * Indexes every crop saved in the chunk.
     */
    public static void track(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals("aranarthcore") || !key.getKey().startsWith(KEY_PREFIX)) {
                continue;
            }
            String[] parts = key.getKey().substring(KEY_PREFIX.length()).split("_");
            try {
                Block base = chunk.getWorld().getBlockAt((chunk.getX() << 4) + Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]), (chunk.getZ() << 4) + Integer.parseInt(parts[2]));
                Crop crop = get(base);
                if (crop != null) {
                    index(base, layoutOf(base, crop));
                }
            } catch (RuntimeException ignored) {
                // A malformed key is left alone
            }
        }
    }

    /**
     * Forgets the crops of a chunk that is no longer loaded.
     */
    public static void untrack(Chunk chunk) {
        Set<Long> bases = BASES.get(chunk.getWorld().getUID());
        if (bases == null) {
            return;
        }
        for (Long key : new ArrayList<>(bases)) {
            Block base = unpack(chunk.getWorld(), key);
            if (base.getX() >> 4 == chunk.getX() && base.getZ() >> 4 == chunk.getZ()) {
                unindex(base);
            }
        }
    }

    /**
     * Determines if the crop may be planted on the soil block.
     */
    public static boolean isSoil(CropType type, Block soil) {
        Material material = soil.getType();
        return switch (type.getSoil()) {
            case FARMLAND -> material == Material.FARMLAND;
            case DIRT -> material != Material.MUD && (Tag.DIRT.isTagged(material) || material == Material.GRASS_BLOCK
                    || material == Material.PODZOL || material == Material.COARSE_DIRT || material == Material.ROOTED_DIRT
                    || material == Material.MYCELIUM || material == Material.MOSS_BLOCK);
            case MUD_BANK -> (material == Material.MUD || material == Material.FARMLAND) && nextToWater(soil);
        };
    }

    /**
     * Determines if the soil is the right kind for the crop, though it may still need water, so a player can be told why.
     */
    public static boolean isSoilIgnoringWater(CropType type, Block soil) {
        return type.getSoil() == CropType.Soil.MUD_BANK
                ? soil.getType() == Material.MUD || soil.getType() == Material.FARMLAND : isSoil(type, soil);
    }

    private static boolean nextToWater(Block soil) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            if (soil.getRelative(face).getType() == Material.WATER) {
                return true;
            }
        }
        return false;
    }

    /**
     * Plants a new crop at its first stage.
     * @param base The block just above the soil.
     * @return Whether it was planted.
     */
    public static boolean plant(Block base, CropType type) {
        return place(base, type, 0, false);
    }

    /**
     * Places a crop at any stage, such as to show every stage of a crop in the test world.
     * @param frozen Whether it stays at that stage until it is first picked.
     * @return Whether it was placed.
     */
    public static boolean place(Block base, CropType type, int stage, boolean frozen) {
        if (!base.getType().isAir() || get(base) != null) {
            return false;
        }
        // A vine remembers the post it grows on, as the side of the post it is on and how tall the post is
        int side = 0;
        if (type.isVine()) {
            side = findPost(base);
            if (side < 0) {
                return false;
            }
        }
        Crop crop = new Crop(type, 0, System.currentTimeMillis(), 0, ThreadLocalRandom.current().nextLong(), side, frozen);
        save(base, crop);
        index(base, new CropLayout());
        // A new crop takes up nothing yet, so its first look must find room like every later one
        if (!setStage(base, crop, 0, new CropLayout())) {
            // Nothing was placed, so only the saved crop is forgotten and whatever was in the way is left alone
            base.getChunk().getPersistentDataContainer().remove(getKey(base));
            unindex(base);
            return false;
        }
        if (type.isVine()) {
            // A vine grows a leaf at a time: a few for its middle stage, then all of them and all of its fruit
            for (int i = 0; i < Vine.MAX_LEAVES + 2; i++) {
                Crop current = get(base);
                if (current == null || current.stage() >= stage && (stage != 1 || vine(base).size() >= 6)) {
                    break;
                }
                if (!vineStep(base, current, false, 1)) {
                    break;
                }
            }
            Crop current = get(base);
            if (current != null && current.stage() >= type.getRipeStage()) {
                CropLayout old = layoutOf(base, current);
                Vine vine = vine(base);
                for (Vine.Leaf leaf : vine.getLeaves()) {
                    Collections.fill(leaf.slots(), 0L);
                }
                saveVine(base, vine);
                render(base, current, old);
            }
            return true;
        }
        for (int s = 1; s <= stage; s++) {
            if (!setStage(base, get(base), s, null)) {
                return false;
            }
        }
        // A tomato plant's top bush shows the same stage as the one below it
        if (type == CropType.TOMATO && stage >= 1) {
            Crop current = get(base);
            CropLayout old = layoutOf(base, current);
            saveTomatoTop(base, new TomatoTop(stage, System.currentTimeMillis(), 0));
            render(base, current, old);
        }
        return true;
    }

    /**
     * Grows or picks a crop to a stage, placing the blocks of its new look.
     * @param from The look it had, or null to work it out from the saved crop.
     * @return Whether there was room for its new look.
     */
    private static boolean setStage(Block base, Crop crop, int stage, CropLayout from) {
        CropLayout old = from != null ? from : layoutOf(base, crop);
        int side = crop.side();
        if (crop.type() == CropType.CANTALOUPE && stage == 4) {
            side = pickSide(base, crop);
            if (side < 0) {
                return false;
            }
        }
        Crop next = new Crop(crop.type(), stage, System.currentTimeMillis(), crop.harvests(), crop.seed(), side, crop.frozen());
        return render(base, next, old);
    }

    /**
     * Saves a crop and places the blocks, heads and displays of its look.
     * @param old The look it had, whose blocks it no longer takes up are cleared.
     * @return Whether there was room for its new look.
     */
    private static boolean render(Block base, Crop next, CropLayout old) {
        CropLayout layout = layoutOf(base, next);
        List<CropLayout.Offset> oldOffsets = old.getOffsets();
        // Only empty space, or what the crop already takes up, can be grown into
        for (CropLayout.Offset offset : layout.getOffsets()) {
            Block block = at(base, offset);
            if (!oldOffsets.contains(offset) && !(offset.x() == 0 && offset.y() == 0 && offset.z() == 0)
                    && !block.getType().isAir()) {
                return false;
            }
        }
        for (CropLayout.Offset offset : oldOffsets) {
            if (!layout.getOffsets().contains(offset)) {
                at(base, offset).setType(Material.AIR, false);
            }
        }
        // Only what changed is placed again, so picking one piece of fruit never makes the rest of the crop flicker
        for (Map.Entry<CropLayout.Offset, BlockData> entry : layout.getBlocks().entrySet()) {
            Block block = at(base, entry.getKey());
            if (!block.getBlockData().equals(entry.getValue())) {
                block.setBlockData(entry.getValue(), false);
            }
        }
        List<CropLayout.Head> oldHeads = old.getHeads();
        for (CropLayout.Head head : layout.getHeads()) {
            Block block = at(base, head.at());
            if (oldHeads.contains(head) && (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD)) {
                continue;
            }
            if (head.wall()) {
                Directional data = (Directional) Material.PLAYER_WALL_HEAD.createBlockData();
                data.setFacing(head.facing());
                block.setBlockData(data, false);
            } else {
                Rotatable data = (Rotatable) Material.PLAYER_HEAD.createBlockData();
                data.setRotation(head.facing());
                block.setBlockData(data, false);
            }
            if (block.getState() instanceof Skull skull) {
                skull.setPlayerProfile(MobHeadUtils.createTextureProfile(head.texture()));
                skull.update(true, false);
            }
        }
        syncDisplays(base, next.type().isVine(), layout.getDisplays());
        save(base, next);
        index(base, layout);
        return true;
    }

    /**
     * Picks the side a stem grows its fruit on: one with room on solid ground, starting from its own favourite side.
     */
    private static int pickSide(Block base, Crop crop) {
        BlockFace[] sides = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        int start = Math.floorMod((int) crop.seed(), 4);
        for (int i = 0; i < 4; i++) {
            int side = (start + i) % 4;
            Block spot = base.getRelative(sides[side]);
            if (spot.getType().isAir() && spot.getRelative(BlockFace.DOWN).getType().isSolid()) {
                return side;
            }
        }
        return -1;
    }

    private static Block at(Block base, CropLayout.Offset offset) {
        return base.getRelative(offset.x(), offset.y(), offset.z());
    }

    private static void spawnDisplay(Block base, CropLayout.Display display) {
        Location location = base.getLocation().add(display.at());
        location.setYaw(display.yaw());
        base.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(display.item());
            entity.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(display.scale(), display.scale(), display.scale()), new AxisAngle4f()));
            entity.setBillboard(display.faceViewer() ? Display.Billboard.VERTICAL : Display.Billboard.FIXED);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(DISPLAY_KEY, PersistentDataType.STRING, displayTag(base));
        });
    }

    private static String displayTag(Block base) {
        return base.getX() + "," + base.getY() + "," + base.getZ();
    }

    /**
     * Removes the item displays of a crop.
     * @param vine Whether it is a vine, whose fruit can hang anywhere on its trellis, so a much wider area is searched.
     */
    private static void removeDisplays(Block base, boolean vine) {
        for (ItemDisplay display : findDisplays(base, vine)) {
            display.remove();
        }
    }

    /**
     * Finds the item displays of a crop.
     * @param vine Whether it is a vine, whose fruit can hang anywhere on its trellis, so a much wider area is searched.
     */
    private static List<ItemDisplay> findDisplays(Block base, boolean vine) {
        String tag = displayTag(base);
        Location center = base.getLocation().add(0.5, vine ? 7 : 1, 0.5);
        double reach = vine ? TRELLIS_REACH + 3 : 3.5;
        List<ItemDisplay> displays = new ArrayList<>();
        for (Entity entity : base.getWorld().getNearbyEntities(center, reach, vine ? 10 : 4, reach,
                e -> e instanceof ItemDisplay && tag.equals(e.getPersistentDataContainer().get(DISPLAY_KEY, PersistentDataType.STRING)))) {
            displays.add((ItemDisplay) entity);
        }
        return displays;
    }

    /**
     * Makes a crop's item displays match its look.
     */
    private static void syncDisplays(Block base, boolean vine, List<CropLayout.Display> wanted) {
        List<CropLayout.Display> missing = new ArrayList<>(wanted);
        for (ItemDisplay entity : findDisplays(base, vine)) {
            int match = -1;
            for (int i = 0; i < missing.size(); i++) {
                if (isShowing(base, entity, missing.get(i))) {
                    match = i;
                    break;
                }
            }
            if (match >= 0) {
                missing.remove(match);
            } else {
                entity.remove();
            }
        }
        for (CropLayout.Display display : missing) {
            spawnDisplay(base, display);
        }
    }

    /**
     * Determines if an item display already shows the display, in the same spot, size, direction and item.
     */
    private static boolean isShowing(Block base, ItemDisplay entity, CropLayout.Display display) {
        Location location = entity.getLocation();
        if (location.toVector().distanceSquared(base.getLocation().toVector().add(display.at())) > 1.0E-4) {
            return false;
        }
        if (Math.abs(entity.getTransformation().getScale().x() - display.scale()) > 1.0E-3) {
            return false;
        }
        Display.Billboard billboard = display.faceViewer() ? Display.Billboard.VERTICAL : Display.Billboard.FIXED;
        if (entity.getBillboard() != billboard) {
            return false;
        }
        if (!display.faceViewer() && Math.abs(Location.normalizeYaw(location.getYaw() - display.yaw())) > 0.5) {
            return false;
        }
        return entity.getItemStack().isSimilar(display.item());
    }

    /**
     * Grows every loaded crop that has waited long enough for its next stage.
     */
    public static void growAll() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Set<Long>> entry : new HashMap<>(BASES).entrySet()) {
            World world = org.bukkit.Bukkit.getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            for (Long key : new ArrayList<>(entry.getValue())) {
                Block base = unpack(world, key);
                if (!world.isChunkLoaded(base.getX() >> 4, base.getZ() >> 4)) {
                    continue;
                }
                Crop crop = get(base);
                if (crop == null) {
                    unindex(base);
                    continue;
                }
                if (crop.frozen()) {
                    continue;
                }
                // Vines and tomato plants have parts that each grow on their own
                if (crop.type().isVine()) {
                    tickVine(base, crop, now);
                    continue;
                }
                if (crop.type() == CropType.TOMATO) {
                    tickTomato(base, crop, now);
                    continue;
                }
                if (crop.type().hasPieces()) {
                    tickPieces(base, crop, now);
                    continue;
                }
                if (crop.isBarren() || crop.stage() >= crop.type().getRipeStage()) {
                    continue;
                }
                double speed = speed(crop);
                if (speed <= 0) {
                    continue;
                }
                if (now - crop.stageStart() >= (long) (crop.type().getStageMillis() / speed)) {
                    setStage(base, crop, crop.stage() + 1, null);
                }
            }
        }
    }

    /**
     * Provides how fast a crop grows this month, from the seasons of the vanilla crop it follows.
     */
    private static double speed(Crop crop) {
        return CropUtils.getCropGrowthSpeed(AranarthUtils.getMonth(), crop.type().getSeason());
    }

    /**
     * Grows a crop one stage straight away with bone meal.
     * @return Whether it grew.
     */
    public static boolean boneMeal(Block base) {
        Crop crop = get(base);
        if (crop == null) {
            return false;
        }
        if (crop.type().isVine()) {
            // Bone meal only grows a vine's leaves, never its fruit, which would otherwise grow back in full with one use
            return (crop.stage() == 0 || vine(base).size() < Vine.MAX_LEAVES) && vineStep(base, crop, true, 1);
        }
        if (crop.type() == CropType.TOMATO) {
            return boneMealTomato(base, crop);
        }
        if (crop.type().hasPieces()) {
            return boneMealPieces(base, crop);
        }
        if (crop.isBarren()) {
            return false;
        }
        if (crop.stage() >= crop.type().getRipeStage()) {
            return false;
        }
        // Like a vanilla melon stem, a fully grown stem cannot be bone mealed into growing its fruit
        CropType type = crop.type();
        if (type == CropType.CANTALOUPE && crop.stage() >= type.getRegrowStage()) {
            return false;
        }
        return setStage(base, crop, crop.stage() + 1, null);
    }

    /**
     * Picks a ripe crop.
     * @param player The player picking it, or null.
     * @param removing Whether the crop is being removed as well, so a regrowing crop does not go back a stage.
     * @return Whether it was picked.
     */
    public static boolean harvest(Block base, Player player, boolean removing) {
        Crop crop = get(base);
        if (crop == null) {
            return false;
        }
        CropType type = crop.type();
        if (type == CropType.TOMATO) {
            return harvestTomato(base, player, -1, removing);
        }
        if (type.hasPieces()) {
            return harvestPieces(base, player, removing);
        }
        if (!crop.isRipe() || !canPick(player, type)) {
            return false;
        }
        CropLayout layout = layoutOf(base, crop);
        ProduceType yield = type.getYield();
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        // A vine broken with no fruit on it gives none, where the season or a boost would otherwise round it up to 1
        int shown = baseYield(crop, layout);
        int amount = shown <= 0 ? 0 : FruitUtils.scaleYield(shown, player, type.getSeason());
        int seeds = type.getMode() == CropType.HarvestMode.REPLANT && type.getSeeds() != null ? 1 + ThreadLocalRandom.current().nextInt(2) : 0;
        // Sneaking while harvesting plants it again straight away, paid for with one of what it was planted from
        boolean replant = type.getMode() == CropType.HarvestMode.REPLANT && player != null && player.isSneaking();
        if (replant && !creative) {
            if (seeds > 0 && type.getSeeds() == type.getPlant()) {
                seeds--;
            } else if (yield == type.getPlant() && amount > 0) {
                amount--;
            } else if (!takeFromInventory(player, type.getPlant())) {
                replant = false;
            }
        }
        if (!creative) {
            drop(base, yield, amount);
            drop(base, type.getSeeds(), seeds);
            if (player != null && shown > 0) {
                FruitUtils.reward(player, type.getMode() == CropType.HarvestMode.REPLANT ? 40 : 30,
                        type.getMode() == CropType.HarvestMode.REPLANT ? 0.80 : 0.60, questKey(type));
            }
        }
        base.getWorld().playSound(base.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F, 1F);
        if (type.getMode() == CropType.HarvestMode.REPLANT) {
            remove(base);
            if (replant && plant(base, type)) {
                base.getWorld().playSound(base.getLocation().add(0.5, 0.2, 0.5), Sound.ITEM_CROP_PLANT, 1F, 1F);
            }
        } else if (!removing && !type.isVine()) {
            // A crop shown in the test world grows normally once it is first picked
            Crop picked = new Crop(type, crop.stage(), crop.stageStart(), crop.harvests() + 1, crop.seed(), crop.side(), false);
            save(base, picked);
            setStage(base, picked, type.getRegrowStage(), layout);
        }
        return true;
    }

    /**
     * Provides what a harvest of the crop counts as for quests, such as PRODUCE_TOMATO.
     */
    private static String questKey(CropType type) {
        return "PRODUCE_" + type.name();
    }

    /**
     * Determines if the player has the Herbalism level to pick the crop, telling them if not.
     */
    private static boolean canPick(Player player, CropType type) {
        if (player != null && type.getHerbalismLevel() > 0
                && ExperienceAPI.getLevel(player, "Herbalism") < type.getHerbalismLevel()) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "crops.herbalism_required",
                    "level", String.valueOf(type.getHerbalismLevel()),
                    "crop", Lang.getFor(player, type.getYield().getLangKey()))));
            return false;
        }
        return true;
    }

    /**
     * Takes one of the produce from the player's inventory.
     * @return Whether they had one.
     */
    private static boolean takeFromInventory(Player player, ProduceType type) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && Produce.getType(item) == type) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    /**
     * Provides how much a ripe crop gives before any bonus, in proportion to how much fruit it shows.
     */
    private static int baseYield(Crop crop, CropLayout layout) {
        CropType type = crop.type();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return switch (type) {
            // Each ear of corn is a candle
            case CORN -> 2 + new Random(crop.seed()).nextInt(3);
            // A berry bush gives one berry for each it shows: a few when picked early, many when left to fill out
            case BLUEBERRY, RASPBERRY, BLACKBERRY, STRAWBERRY -> Math.max(1, layout.getDisplays().size());
            // A vine broken whole gives one for each bunch or kiwi on it
            case GREEN_GRAPES, RED_GRAPES, KIWI -> layout.getDisplays().size();
            default -> type.getYieldMin() + r.nextInt(type.getYieldMax() - type.getYieldMin() + 1);
        };
    }

    /**
     * Breaks a whole crop, giving back what it was planted from, and its seeds, plus its fruit if it was ripe.
     * @param player The player breaking it, or null.
     */
    public static void uproot(Block base, Player player) {
        Crop crop = get(base);
        if (crop == null) {
            return;
        }
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        boolean fruited = false;
        if (crop.type() == CropType.TOMATO) {
            harvestTomato(base, player, -1, true);
        } else if (crop.type().hasPieces()) {
            fruited = harvestPieces(base, player, true);
        } else if (crop.isRipe() && !crop.isBarren()) {
            if (harvest(base, player, true) && crop.type().getMode() == CropType.HarvestMode.REPLANT) {
                return;
            }
        }
        if (!creative) {
            CropType type = crop.type();
            // Only a plant that has grown gives extra seeds, so seeds can never be multiplied by planting and breaking
            boolean grown = crop.stage() >= type.getHarvestStage() || crop.harvests() > 0 || fruited;
            if (grown && type.getSeeds() != null && type.getSeeds() == type.getPlant()) {
                drop(base, type.getSeeds(), 1 + ThreadLocalRandom.current().nextInt(2));
            } else {
                drop(base, type.getPlant(), 1);
            }
        }
        remove(base);
    }

    /**
     * Removes a crop and everything it is made of, without dropping anything.
     */
    public static void remove(Block base) {
        Crop crop = get(base);
        if (crop != null) {
            for (CropLayout.Offset offset : layoutOf(base, crop).getOffsets()) {
                at(base, offset).setType(Material.AIR, false);
            }
        }
        removeDisplays(base, crop == null || crop.type().isVine());
        base.getChunk().getPersistentDataContainer().remove(getKey(base));
        base.getChunk().getPersistentDataContainer().remove(getExtraKey(base));
        unindex(base);
    }

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP,
            BlockFace.DOWN};
    private static final BlockFace[] OPEN_SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP};
    private static final int TRELLIS_REACH = 8;
    private static final int TRELLIS_HEIGHT = 14;
    private static final int MAX_POST = 11;
    private static final int MAX_TRELLIS = 256;

    /**
     * Finds the fence post a vine planted at the block would grow on.
     */
    public static int findPost(Block base) {
        boolean taken = false;
        for (int i = 0; i < 4; i++) {
            Block post = base.getRelative(SIDES[i]);
            Material below = post.getRelative(BlockFace.DOWN).getType();
            if (!Tag.FENCES.isTagged(post.getType()) || !below.isSolid() || Tag.FENCES.isTagged(below)) {
                continue;
            }
            int height = postHeight(post);
            if (height < 2 || height > MAX_POST) {
                continue;
            }
            if (hasVine(post, base)) {
                taken = true;
                continue;
            }
            // The vine is on the side of the post facing back towards where it was planted
            return Math.floorMod(i + 2, 4) + 4 * height;
        }
        return taken ? -2 : -1;
    }

    private static int postHeight(Block post) {
        int height = 0;
        while (height <= MAX_POST && Tag.FENCES.isTagged(post.getRelative(0, height, 0).getType())) {
            height++;
        }
        return height;
    }

    /**
     * Provides the lowest fence of the post a vine grows on.
     */
    private static Block postOf(Block base, Crop crop) {
        BlockFace side = SIDES[Math.floorMod(crop.side(), 4)];
        return base.getRelative(side.getOppositeFace());
    }

    /**
     * Determines if a vine other than the one at the base already grows from the post.
     */
    private static boolean hasVine(Block post, Block base) {
        for (BlockFace face : SIDES) {
            Block spot = post.getRelative(face);
            if (spot.equals(base)) {
                continue;
            }
            Crop other = get(spot);
            if (other != null && other.type().isVine() && postOf(spot, other).equals(post)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds every fence of the trellis a vine grows on.
     */
    private static Set<Block> trellis(Block base, Crop crop) {
        Set<Block> fences = new HashSet<>();
        Block post = postOf(base, crop);
        if (!Tag.FENCES.isTagged(post.getType())) {
            return fences;
        }
        List<Block> queue = new ArrayList<>();
        queue.add(post);
        fences.add(post);
        for (int i = 0; i < queue.size() && fences.size() < MAX_TRELLIS; i++) {
            for (BlockFace face : FACES) {
                Block next = queue.get(i).getRelative(face);
                if (Math.abs(next.getX() - post.getX()) > TRELLIS_REACH || Math.abs(next.getZ() - post.getZ()) > TRELLIS_REACH
                        || next.getY() < base.getY() || next.getY() > base.getY() + TRELLIS_HEIGHT
                        || fences.contains(next) || !Tag.FENCES.isTagged(next.getType())) {
                    continue;
                }
                fences.add(next);
                queue.add(next);
            }
        }
        return fences;
    }

    private static boolean touches(Block block, Set<Block> fences) {
        for (BlockFace face : FACES) {
            if (fences.contains(block.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    private static CropLayout.Offset offsetOf(Block base, Block block) {
        return new CropLayout.Offset(block.getX() - base.getX(), block.getY() - base.getY(), block.getZ() - base.getZ());
    }

    /**
     * Grows a leaf in a random free spot on the trellis.
     */
    private static boolean addLeaf(Block base, Crop crop, Vine vine) {
        Set<Block> fences = trellis(base, crop);
        if (fences.isEmpty()) {
            return false;
        }
        List<Block> sources = new ArrayList<>();
        sources.add(base);
        for (Vine.Leaf leaf : vine.getLeaves()) {
            sources.add(at(base, leaf.at()));
        }
        Map<Block, Integer> spots = new java.util.LinkedHashMap<>();
        int total = 0;
        for (Block source : sources) {
            for (BlockFace face : FACES) {
                Block spot = source.getRelative(face);
                CropLayout.Offset offset = offsetOf(base, spot);
                if (offset.y() < 1 || spots.containsKey(spot) || !spot.getType().isAir() || vine.has(offset)
                        || vine.isTrimmed(offset) || isCropBlock(spot) || !touches(spot, fences)) {
                    continue;
                }
                // Like a real vine it climbs onto the top of the trellis and wraps around its fences
                int weight = 1;
                int leaves = 0;
                for (BlockFace side : FACES) {
                    Block next = spot.getRelative(side);
                    if (fences.contains(next)) {
                        weight += side == BlockFace.DOWN ? 4 : 1;
                    }
                    if (vine.has(offsetOf(base, next))) {
                        leaves++;
                    }
                }
                if (leaves <= 1) {
                    weight += 2;
                }
                spots.put(spot, weight);
                total += weight;
            }
        }
        if (spots.isEmpty()) {
            return false;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Block, Integer> entry : spots.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                vine.add(offsetOf(base, entry.getKey()));
                break;
            }
        }
        return true;
    }

    /**
     * Provides the side a leaf shows its fruit on: below it when there is air under it.
     */
    private static BlockFace openFace(Block leaf) {
        if (leaf.getRelative(BlockFace.DOWN).getType().isAir()) {
            return BlockFace.DOWN;
        }
        List<BlockFace> sides = new ArrayList<>();
        for (BlockFace face : OPEN_SIDES) {
            if (leaf.getRelative(face).getType().isAir()) {
                sides.add(face);
            }
        }
        return sides.isEmpty() ? null : sides.get(ThreadLocalRandom.current().nextInt(sides.size()));
    }

    /**
     * Provides how long a piece of fruit on a vine takes to grow, and to grow back once picked: a quarter of its grow time.
     */
    private static long fruitWait(Crop crop, double speed) {
        return (long) (crop.type().getGrowMillis() / 4 / Math.max(speed, 0.1));
    }

    /**
     * Gives a vine the fruit it can hold: about 1.5 for each of its leaves, at most 3 on a leaf, and only on leaves open to the air.
     */
    private static void planFruit(Block base, Crop crop, Vine vine, long now, double speed) {
        int target = Math.min(Vine.MAX_FRUIT, (int) Math.round(vine.size() * Vine.FRUIT_PER_LEAF));
        int total = vine.slotCount();
        if (total >= target) {
            return;
        }
        List<Vine.Leaf> leaves = vine.getLeaves();
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i++) {
            Vine.Leaf leaf = leaves.get(i);
            if (leaf.face() == null) {
                BlockFace face = openFace(at(base, leaf.at()));
                if (face == null) {
                    continue;
                }
                leaf = new Vine.Leaf(leaf.at(), face, leaf.slots());
                vine.set(i, leaf);
            }
            if (leaf.slots().size() < Vine.MAX_FRUIT_PER_LEAF) {
                open.add(i);
            }
        }
        Collections.shuffle(open);
        long wait = fruitWait(crop, speed);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        boolean placed = true;
        while (total < target && placed) {
            placed = false;
            for (int i : open) {
                List<Long> slots = leaves.get(i).slots();
                if (total < target && slots.size() < Vine.MAX_FRUIT_PER_LEAF) {
                    slots.add(now + (long) (wait * (0.4 + r.nextDouble() * 0.6)));
                    total++;
                    placed = true;
                }
            }
        }
    }

    /**
     * Grows a vine one step.
     * @param leavesOnly Whether only leaves may grow, as with bone meal.
     * @param speed How fast it grows this month.
     * @return Whether it grew.
     */
    private static boolean vineStep(Block base, Crop crop, boolean leavesOnly, double speed) {
        CropLayout old = layoutOf(base, crop);
        Vine vine = vine(base);
        String before = vine.serialize();
        long now = System.currentTimeMillis();
        int ripe = crop.type().getRipeStage();
        int next = crop.stage();
        boolean grew = false;
        if (crop.stage() == 0) {
            addLeaf(base, crop, vine);
            next = 1;
            grew = true;
        } else if (vine.size() < Vine.MAX_LEAVES && addLeaf(base, crop, vine)) {
            grew = true;
            if (crop.stage() >= ripe) {
                planFruit(base, crop, vine, now, speed);
            }
        } else if (!leavesOnly && crop.stage() < ripe) {
            planFruit(base, crop, vine, now, speed);
            next = ripe;
            grew = vine.slotCount() > 0;
        }
        if (!grew) {
            if (!leavesOnly) {
                // Nothing to grow into yet, so it waits a while before looking again
                save(base, new Crop(crop.type(), crop.stage(), now, crop.harvests(), crop.seed(), crop.side(), crop.frozen()));
            }
            return false;
        }
        saveVine(base, vine);
        if (!render(base, new Crop(crop.type(), next, now, crop.harvests(), crop.seed(), crop.side(), crop.frozen()), old)) {
            base.getChunk().getPersistentDataContainer().set(getExtraKey(base), PersistentDataType.STRING, before);
            return false;
        }
        return true;
    }

    /**
     * Grows a vine: a new leaf every so often while it has fewer than it can have, and each piece of its fruit once it is due.
     */
    private static void tickVine(Block base, Crop crop, long now) {
        double speed = speed(crop);
        if (speed <= 0) {
            return;
        }
        boolean leaves = crop.stage() < crop.type().getRipeStage() || vine(base).size() < Vine.MAX_LEAVES;
        long leafWait = (long) (crop.type().getGrowMillis() * 3 / 4 / Vine.MAX_LEAVES / speed);
        if (leaves && now - crop.stageStart() >= leafWait) {
            vineStep(base, crop, false, speed);
            crop = get(base);
            if (crop == null) {
                return;
            }
        }
        if (crop.stage() < crop.type().getRipeStage()) {
            return;
        }
        CropLayout old = layoutOf(base, crop);
        Vine vine = vine(base);
        boolean grown = false;
        for (Vine.Leaf leaf : vine.getLeaves()) {
            List<Long> slots = leaf.slots();
            for (int k = 0; k < slots.size(); k++) {
                if (slots.get(k) > 0 && now >= slots.get(k)) {
                    slots.set(k, 0L);
                    grown = true;
                }
            }
        }
        if (grown) {
            saveVine(base, vine);
            render(base, crop, old);
        }
    }

    /**
     * Picks one piece of fruit off a vine's leaf: the one the player is looking at most directly. It grows back on its own.
     * @param player The player picking it, or null.
     * @return Whether one was picked.
     */
    public static boolean pickVineFruit(Block base, Block block, Player player) {
        Crop crop = get(base);
        if (crop == null || !crop.type().isVine() || crop.stage() < crop.type().getRipeStage()) {
            return false;
        }
        CropLayout old = layoutOf(base, crop);
        Vine vine = vine(base);
        Vine.Leaf leaf = vine.find(offsetOf(base, block));
        if (leaf == null || leaf.fruit() == 0 || !canPick(player, crop.type())) {
            return false;
        }
        int pick = -1;
        double best = Double.MAX_VALUE;
        for (int k = 0; k < leaf.slots().size(); k++) {
            if (leaf.slots().get(k) != 0) {
                continue;
            }
            double distance = 0;
            if (player != null) {
                org.bukkit.util.Vector spot = base.getLocation().toVector().add(CropLayout.fruitSpot(crop.type(), crop.seed(), leaf, k));
                org.bukkit.util.Vector eye = player.getEyeLocation().toVector();
                distance = spot.subtract(eye).crossProduct(player.getEyeLocation().getDirection()).length();
            }
            if (distance < best) {
                best = distance;
                pick = k;
            }
        }
        long now = System.currentTimeMillis();
        leaf.slots().set(pick, now + fruitWait(crop, speed(crop)));
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        if (!creative) {
            drop(block, crop.type().getYield(), FruitUtils.scaleYield(1, player, crop.type().getSeason()));
            if (player != null) {
                FruitUtils.reward(player, 10, 0.20, questKey(crop.type()));
            }
        }
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F, 1F);
        saveVine(base, vine);
        // A vine shown in the test world grows normally once it is first picked
        render(base, new Crop(crop.type(), crop.stage(), crop.stageStart(), crop.harvests() + 1, crop.seed(), crop.side(), false), old);
        return true;
    }

    /**
     * Trims a leaf off a vine. It never grows back in that spot, but the vine can grow another leaf somewhere else.
     * @param player The player trimming it, or null.
     */
    public static void trimVine(Block base, Block block, Player player) {
        Crop crop = get(base);
        if (crop == null) {
            return;
        }
        CropLayout old = layoutOf(base, crop);
        Vine vine = vine(base);
        Vine.Leaf leaf = vine.remove(offsetOf(base, block), true);
        if (leaf == null) {
            return;
        }
        List<Vine.Leaf> lost = prune(vine, null, base);
        lost.add(leaf);
        dropFruit(base, crop, lost, player);
        saveVine(base, vine);
        render(base, crop, old);
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.ENTITY_SHEEP_SHEAR, 1F, 1.2F);
    }

    /**
     * Removes the leaves of a vine that no longer touch a fence of its trellis.
     * @param fences The fences of its trellis, or null to only check that its leaves are joined to the plant.
     * @return The leaves removed.
     */
    private static List<Vine.Leaf> prune(Vine vine, Set<Block> fences, Block base) {
        List<Vine.Leaf> removed = new ArrayList<>();
        if (fences != null) {
            for (Vine.Leaf leaf : new ArrayList<>(vine.getLeaves())) {
                if (!touches(at(base, leaf.at()), fences)) {
                    removed.add(vine.remove(leaf.at(), false));
                }
            }
        }
        Set<CropLayout.Offset> joined = new HashSet<>();
        List<CropLayout.Offset> queue = new ArrayList<>();
        queue.add(new CropLayout.Offset(0, 0, 0));
        for (int i = 0; i < queue.size(); i++) {
            CropLayout.Offset from = queue.get(i);
            for (BlockFace face : FACES) {
                CropLayout.Offset next = from.add(face.getModX(), face.getModY(), face.getModZ());
                if (vine.has(next) && joined.add(next)) {
                    queue.add(next);
                }
            }
        }
        for (Vine.Leaf leaf : new ArrayList<>(vine.getLeaves())) {
            if (!joined.contains(leaf.at())) {
                removed.add(vine.remove(leaf.at(), false));
            }
        }
        return removed;
    }

    /**
     * Drops the fruit grown on leaves that came off a vine.
     */
    private static void dropFruit(Block base, Crop crop, List<Vine.Leaf> leaves, Player player) {
        if (player != null && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        for (Vine.Leaf leaf : leaves) {
            drop(at(base, leaf.at()), crop.type().getYield(), leaf.fruit());
        }
    }

    /**
     * Checks the vines near a fence that was broken.
     * @param player The player who broke the fence, or null.
     */
    public static void checkTrellises(Block fence, Player player) {
        Set<Long> bases = BASES.get(fence.getWorld().getUID());
        if (bases == null) {
            return;
        }
        for (Long key : new ArrayList<>(bases)) {
            Block base = unpack(fence.getWorld(), key);
            int dy = fence.getY() - base.getY();
            if (Math.abs(fence.getX() - base.getX()) > TRELLIS_REACH + 2 || Math.abs(fence.getZ() - base.getZ()) > TRELLIS_REACH + 2
                    || dy < -1 || dy > TRELLIS_HEIGHT + 1) {
                continue;
            }
            Crop crop = get(base);
            if (crop == null || !crop.type().isVine()) {
                continue;
            }
            if (postHeight(postOf(base, crop)) < 2) {
                uproot(base, player);
                continue;
            }
            CropLayout old = layoutOf(base, crop);
            Vine vine = vine(base);
            List<Vine.Leaf> lost = prune(vine, trellis(base, crop), base);
            if (!lost.isEmpty()) {
                dropFruit(base, crop, lost, player);
                saveVine(base, vine);
                render(base, crop, old);
            }
        }
    }

    /**
     * Grows a tomato plant.
     */
    private static void tickTomato(Block base, Crop crop, long now) {
        double speed = speed(crop);
        if (speed <= 0) {
            return;
        }
        long step = (long) (crop.type().getStageMillis() / speed);
        int max = crop.type().getMaxHarvests();
        CropLayout old = layoutOf(base, crop);
        TomatoTop top = tomatoTop(base);
        int lower = crop.stage();
        long lowerStart = crop.stageStart();
        boolean changed = false;
        if ((lower < 1 || lower < 2 && crop.harvests() < max) && now - lowerStart >= step) {
            lower++;
            lowerStart = now;
            changed = true;
        }
        if (lower >= 1) {
            if (top.stage() < 1) {
                if (top.start() == 0) {
                    // The top bush starts growing once the bottom one is large
                    top = new TomatoTop(-1, now, top.picks());
                    saveTomatoTop(base, top);
                } else if (now - top.start() >= step) {
                    top = new TomatoTop(1, now, top.picks());
                    changed = true;
                }
            } else if (top.stage() == 1 && top.picks() < max && now - top.start() >= step) {
                top = new TomatoTop(2, now, top.picks());
                changed = true;
            }
        }
        if (changed) {
            saveTomatoTop(base, top);
            render(base, new Crop(crop.type(), lower, lowerStart, crop.harvests(), crop.seed(), crop.side(), crop.frozen()), old);
        }
    }

    /**
     * Grows a tomato plant one step with bone meal.
     * @return Whether it grew.
     */
    private static boolean boneMealTomato(Block base, Crop crop) {
        int max = crop.type().getMaxHarvests();
        CropLayout old = layoutOf(base, crop);
        long now = System.currentTimeMillis();
        if (crop.stage() < 1 || crop.stage() < 2 && crop.harvests() < max) {
            return render(base, new Crop(crop.type(), crop.stage() + 1, now, crop.harvests(), crop.seed(), crop.side(),
                    crop.frozen()), old);
        }
        TomatoTop top = tomatoTop(base);
        if (top.stage() < 1) {
            top = new TomatoTop(1, now, top.picks());
        } else if (top.stage() == 1 && top.picks() < max) {
            top = new TomatoTop(2, now, top.picks());
        } else {
            return false;
        }
        saveTomatoTop(base, top);
        return render(base, crop, old);
    }

    /**
     * Picks the ripe bush of a tomato plant that was clicked or broken.
     * @param player The player picking it, or null.
     * @return Whether it was picked.
     */
    public static boolean pickTomato(Block base, Block block, Player player) {
        return harvestTomato(base, player, block.getY() - base.getY() == 1 ? 1 : 0, false);
    }

    /**
     * Picks the ripe bushes of a tomato plant, each going back to large and ripening again on its own.
     * @param part 0 for the bottom bush, 1 for the top one, or -1 for both.
     * @param player The player picking it, or null.
     * @param removing Whether the plant is being removed as well, so nothing goes back a stage.
     * @return Whether anything was picked.
     */
    private static boolean harvestTomato(Block base, Player player, int part, boolean removing) {
        Crop crop = get(base);
        if (crop == null) {
            return false;
        }
        TomatoTop top = tomatoTop(base);
        boolean lower = crop.stage() >= 2 && part != 1;
        boolean upper = top.stage() >= 2 && part != 0;
        if (!lower && !upper || !canPick(player, crop.type())) {
            return false;
        }
        CropLayout old = layoutOf(base, crop);
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        CropType type = crop.type();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int bush = 0; bush < 2; bush++) {
            if (bush == 0 ? !lower : !upper) {
                continue;
            }
            if (!creative) {
                int amount = type.getYieldMin() + r.nextInt(type.getYieldMax() - type.getYieldMin() + 1);
                drop(base.getRelative(0, bush, 0), type.getYield(), FruitUtils.scaleYield(amount, player, type.getSeason()));
                if (player != null) {
                    FruitUtils.reward(player, 15, 0.30, questKey(type));
                }
            }
        }
        base.getWorld().playSound(base.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F, 1F);
        if (removing) {
            return true;
        }
        long now = System.currentTimeMillis();
        // A plant shown in the test world grows normally once it is first picked
        Crop next = lower ? new Crop(type, 1, now, crop.harvests() + 1, crop.seed(), crop.side(), false)
                : new Crop(type, crop.stage(), crop.stageStart(), crop.harvests(), crop.seed(), crop.side(), false);
        if (upper) {
            saveTomatoTop(base, new TomatoTop(1, now, top.picks() + 1));
        }
        render(base, next, old);
        return true;
    }

    /**
     * Provides the stage a piece of fruit grows to first: green for a pepper, or ripe for a coffee bean.
     */
    private static int firstPieceStage(CropType type) {
        return type == CropType.BELL_PEPPER ? 2 : type.getRipeStage();
    }

    /**
     * Determines if a piece of fruit has been picked as many times as it can be, so it never grows back.
     */
    private static boolean isSpent(CropType type, CropLayout.Piece piece) {
        return type.getMaxHarvests() > 0 && piece.picks() >= type.getMaxHarvests();
    }

    /**
     * Grows a crop whose pieces of fruit each grow on their own.
     */
    private static void tickPieces(Block base, Crop crop, long now) {
        double speed = speed(crop);
        if (speed <= 0) {
            return;
        }
        CropType type = crop.type();
        long step = (long) (type.getStageMillis() / speed);
        if (crop.stage() < type.getPlantStage()) {
            if (now - crop.stageStart() >= step) {
                setStage(base, crop, crop.stage() + 1, null);
            }
            return;
        }
        CropLayout old = layoutOf(base, crop);
        List<CropLayout.Piece> pieces = pieces(base, crop);
        boolean changed = false;
        for (int i = 0; i < pieces.size(); i++) {
            CropLayout.Piece piece = pieces.get(i);
            if (piece.stage() >= type.getRipeStage() || piece.stage() < 0 && isSpent(type, piece) || now - piece.start() < step) {
                continue;
            }
            int next = piece.stage() < 0 ? firstPieceStage(type) : piece.stage() + 1;
            pieces.set(i, new CropLayout.Piece(next, now, piece.picks()));
            changed = true;
        }
        if (changed) {
            savePieces(base, pieces);
            render(base, crop, old);
        }
    }

    /**
     * Grows a crop whose pieces of fruit each grow on their own one step with bone meal.
     * @return Whether it grew.
     */
    private static boolean boneMealPieces(Block base, Crop crop) {
        CropType type = crop.type();
        if (crop.stage() < type.getPlantStage()) {
            return setStage(base, crop, crop.stage() + 1, null);
        }
        CropLayout old = layoutOf(base, crop);
        List<CropLayout.Piece> pieces = pieces(base, crop);
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < pieces.size(); i++) {
            CropLayout.Piece piece = pieces.get(i);
            if (piece.stage() >= type.getRipeStage() || piece.stage() < 0 && isSpent(type, piece)) {
                continue;
            }
            int next = piece.stage() < 0 ? firstPieceStage(type) : piece.stage() + 1;
            pieces.set(i, new CropLayout.Piece(next, now, piece.picks()));
            changed = true;
            if (type == CropType.COFFEE) {
                break;
            }
        }
        if (!changed) {
            return false;
        }
        savePieces(base, pieces);
        return render(base, crop, old);
    }

    /**
     * Provides where a piece of fruit shows, relative to the crop's base.
     */
    private static org.bukkit.util.Vector pieceSpot(Crop crop, int index) {
        if (crop.type() == CropType.BELL_PEPPER) {
            BlockFace face = CropLayout.pepperFaces(crop.type(), crop.seed()).get(index);
            return new org.bukkit.util.Vector(0.5 + face.getModX(), 0.5, 0.5 + face.getModZ());
        }
        return CropLayout.coffeeSpots(crop.type(), crop.seed()).get(index);
    }

    /**
     * Picks one piece of fruit off a crop whose pieces grow and are picked on their own.
     * @param exact Whether only a pepper head itself can be picked, as when breaking a block, so breaking the leaf block
     * breaks the whole plant instead.
     * @param player The player picking it, or null.
     * @return Whether one was picked.
     */
    public static boolean pickPiece(Block base, Block block, Player player, boolean exact) {
        Crop crop = get(base);
        if (crop == null || !crop.type().hasPieces() || crop.stage() < crop.type().getPlantStage()) {
            return false;
        }
        CropType type = crop.type();
        List<CropLayout.Piece> pieces = pieces(base, crop);
        int pick = -1;
        if (type == CropType.BELL_PEPPER) {
            CropLayout.Offset clicked = offsetOf(base, block);
            List<BlockFace> faces = CropLayout.pepperFaces(type, crop.seed());
            for (int i = 0; i < faces.size() && i < pieces.size(); i++) {
                if (pieces.get(i).stage() >= 0 && clicked.equals(new CropLayout.Offset(faces.get(i).getModX(), 0, faces.get(i).getModZ()))) {
                    pick = i;
                }
            }
        }
        if (pick < 0 && exact) {
            return false;
        }
        if (pick < 0) {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < pieces.size(); i++) {
                if (pieces.get(i).stage() < 0) {
                    continue;
                }
                double distance = 0;
                if (player != null) {
                    org.bukkit.util.Vector spot = base.getLocation().toVector().add(pieceSpot(crop, i));
                    org.bukkit.util.Vector eye = player.getEyeLocation().toVector();
                    distance = spot.subtract(eye).crossProduct(player.getEyeLocation().getDirection()).length();
                }
                if (distance < best) {
                    best = distance;
                    pick = i;
                }
            }
        }
        if (pick < 0 || !canPick(player, type)) {
            return false;
        }
        CropLayout old = layoutOf(base, crop);
        CropLayout.Piece piece = pieces.get(pick);
        long now = System.currentTimeMillis();
        pieces.set(pick, new CropLayout.Piece(-1, now, piece.picks() + 1));
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        if (!creative) {
            ProduceType yield = type == CropType.BELL_PEPPER ? CropLayout.pepperColour(piece.stage()) : type.getYield();
            Block at = type == CropType.BELL_PEPPER ? base.getLocation().add(pieceSpot(crop, pick)).getBlock() : block;
            drop(at, yield, FruitUtils.scaleYield(1, player, type.getSeason()));
            if (player != null) {
                FruitUtils.reward(player, 10, 0.20, questKey(type));
            }
        }
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F, 1F);
        savePieces(base, pieces);
        // A crop shown in the test world grows normally once it is first picked
        render(base, new Crop(type, crop.stage(), crop.stageStart(), crop.harvests() + 1, crop.seed(), crop.side(), false), old);
        return true;
    }

    /**
     * Picks every grown piece of fruit off a crop whose pieces grow and are picked on their own, such as when it is broken.
     * @param player The player picking it, or null.
     * @param removing Whether the crop is being removed as well, so nothing is set to grow back.
     * @return Whether anything was picked.
     */
    private static boolean harvestPieces(Block base, Player player, boolean removing) {
        Crop crop = get(base);
        if (crop == null || crop.stage() < crop.type().getPlantStage()) {
            return false;
        }
        CropType type = crop.type();
        List<CropLayout.Piece> pieces = pieces(base, crop);
        boolean any = false;
        for (CropLayout.Piece piece : pieces) {
            any |= piece.stage() >= 0;
        }
        if (!any || !canPick(player, type)) {
            return false;
        }
        CropLayout old = layoutOf(base, crop);
        boolean creative = player != null && player.getGameMode() == GameMode.CREATIVE;
        long now = System.currentTimeMillis();
        for (int i = 0; i < pieces.size(); i++) {
            CropLayout.Piece piece = pieces.get(i);
            if (piece.stage() < 0) {
                continue;
            }
            if (!creative) {
                ProduceType yield = type == CropType.BELL_PEPPER ? CropLayout.pepperColour(piece.stage()) : type.getYield();
                drop(base, yield, FruitUtils.scaleYield(1, player, type.getSeason()));
                if (player != null) {
                    FruitUtils.reward(player, 10, 0.20, questKey(type));
                }
            }
            pieces.set(i, new CropLayout.Piece(-1, now, piece.picks() + 1));
        }
        base.getWorld().playSound(base.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F, 1F);
        if (!removing) {
            savePieces(base, pieces);
            render(base, new Crop(type, crop.stage(), crop.stageStart(), crop.harvests() + 1, crop.seed(), crop.side(), false), old);
        }
        return true;
    }

    /**
     * Determines if the block is a head of fruit on the crop, which is picked when broken.
     */
    public static boolean isFruitBlock(Block base, Block block) {
        Crop crop = get(base);
        return crop != null && layoutOf(base, crop).isFruit(new CropLayout.Offset(block.getX() - base.getX(),
                block.getY() - base.getY(), block.getZ() - base.getZ()));
    }

    private static void drop(Block base, ProduceType type, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack item = new Produce(type).getItem();
        item.setAmount(Math.min(item.getMaxStackSize(), amount));
        CropUtils.updateSeedLore(item, base.getWorld());
        base.getWorld().dropItemNaturally(base.getLocation().add(0.5, 0.5, 0.5), item);
    }
}
