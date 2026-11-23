package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestTrigger;
import com.aearost.aranarthcore.items.produce.FruitSeason;
import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceHeads;
import com.aearost.aranarthcore.items.produce.ProduceType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.objects.Boost;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.random.ProbabilityUtil;
import com.gmail.nossr50.util.skills.RankUtils;
import org.bukkit.Bukkit;
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
import org.bukkit.block.data.type.HangingMoss;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Handles the fruit that grows on custom trees: where it grows, picking it, and growing it back.
 */
public class FruitUtils {

    public enum Kind {
        /** A textured player head clinging to the side of a leaf or log. */
        HEAD,
        /** A tip of pale hanging moss under a leaf, for small fruit such as olives. */
        MOSS,
        /** A coloured leaf that is itself a bunch of fruit, such as dates or bananas. */
        BUNCH
    }

    /**
     * A remembered piece of fruit.
     * @param id The produce id of the fruit.
     * @param kind How the fruit shows.
     * @param data The facing of a head, or the leaf of a ripe bunch.
     * @param pickedAt When it was picked, or 0 while it is ripe.
     */
    public record Spot(String id, Kind kind, String data, long pickedAt) {
        public boolean isRipe() {
            return pickedAt == 0;
        }
    }

    private static final String KEY_PREFIX = "fruit_";
    /**
     * How long picked fruit takes to grow back in a month of normal growth.
     */
    private static final long REGROW_MS = 2 * 60 * 60 * 1000L;
    private static final Material[] BUNCH_STAGES = {Material.JUNGLE_LEAVES, Material.AZALEA_LEAVES};
    private static final Set<Location> PICKED = new HashSet<>();
    private static final Random RANDOM = new Random();
    /** The month whose season the fruit of every loaded tree was last thinned for. */
    private static Month seasonMonth = null;

    private static NamespacedKey getKey(Block block) {
        return new NamespacedKey("aranarthcore", KEY_PREFIX + (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15));
    }

    /**
     * Remembers a piece of fruit that a tree has just grown.
     */
    public static void register(Block block, String id, Kind kind) {
        String data;
        switch (kind) {
            case HEAD -> {
                if (block.getType() != Material.PLAYER_WALL_HEAD) {
                    return;
                }
                data = ((Directional) block.getBlockData()).getFacing().name();
            }
            case MOSS -> {
                if (block.getType() != Material.PALE_HANGING_MOSS) {
                    return;
                }
                data = "-";
            }
            default -> {
                if (!Tag.LEAVES.isTagged(block.getType())) {
                    return;
                }
                data = block.getType().name();
            }
        }
        save(block, new Spot(id, kind, data, 0));
        // A tree only grows the share of its fruit that is in season, and the rest fills in as its season comes
        if (!FruitSeason.of(id).bears(block, AranarthUtils.getMonth())) {
            unripen(block, 0);
        }
    }

    private static void save(Block block, Spot spot) {
        block.getChunk().getPersistentDataContainer().set(getKey(block), PersistentDataType.STRING,
                spot.id() + ";" + spot.kind().name() + ";" + spot.data() + ";" + spot.pickedAt());
        if (spot.isRipe()) {
            PICKED.remove(block.getLocation());
        } else {
            PICKED.add(block.getLocation());
        }
    }

    /**
     * Forgets a piece of fruit, such as when its leaf is gone.
     */
    public static void forget(Block block) {
        block.getChunk().getPersistentDataContainer().remove(getKey(block));
        PICKED.remove(block.getLocation());
    }

    /**
     * Provides the fruit remembered at the block, whether or not the block still shows it.
     */
    public static Spot getStored(Block block) {
        String value = block.getChunk().getPersistentDataContainer().get(getKey(block), PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        String[] parts = value.split(";");
        try {
            return new Spot(parts[0], Kind.valueOf(parts[1]), parts[2], Long.parseLong(parts[3]));
        } catch (RuntimeException e) {
            forget(block);
            return null;
        }
    }

    /**
     * Provides the ripe fruit shown by the block, or null if the block is not ripe fruit.
     */
    public static Spot getRipe(Block block) {
        Spot spot = getStored(block);
        if (spot == null || !spot.isRipe()) {
            return null;
        }
        boolean shown = switch (spot.kind()) {
            case HEAD -> block.getType() == Material.PLAYER_WALL_HEAD;
            case MOSS -> block.getType() == Material.PALE_HANGING_MOSS;
            case BUNCH -> block.getType().name().equals(spot.data());
        };
        if (!shown) {
            forget(block);
            return null;
        }
        return spot;
    }

    /**
     * Provides the bunch of fruit still growing back at the block, or null if it is not one.
     */
    public static Spot getGrowingBunch(Block block) {
        Spot spot = getStored(block);
        if (spot == null || spot.isRipe() || spot.kind() != Kind.BUNCH) {
            return null;
        }
        for (Material stage : BUNCH_STAGES) {
            if (block.getType() == stage) {
                return spot;
            }
        }
        return null;
    }

    /**
     * Starts a bunch of fruit growing back from nothing again, such as when its unripe leaf is broken.
     */
    public static void restartBunch(Block block, Spot spot) {
        save(block, new Spot(spot.id(), spot.kind(), spot.data(), System.currentTimeMillis()));
    }

    /**
     * Determines if the block is a fruit head that was already picked but still shows.
     */
    public static boolean isLeftoverHead(Block block) {
        Spot spot = getStored(block);
        return spot != null && !spot.isRipe() && spot.kind() == Kind.HEAD && block.getType() == Material.PLAYER_WALL_HEAD;
    }

    /**
     * Determines if the block is part of a fruit tree, as fruit, ripe or picked, is remembered within a few blocks of it.
     * @param block The block, such as a leaf.
     * @param radius How far away fruit may be, in blocks on each axis.
     */
    public static boolean isNearFruit(Block block, int radius) {
        World world = block.getWorld();
        int minX = block.getX() - radius;
        int maxX = block.getX() + radius;
        int minZ = block.getZ() - radius;
        int maxZ = block.getZ() + radius;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                for (NamespacedKey key : world.getChunkAt(cx, cz).getPersistentDataContainer().getKeys()) {
                    if (!key.getNamespace().equals("aranarthcore") || !key.getKey().startsWith(KEY_PREFIX)) {
                        continue;
                    }
                    String[] parts = key.getKey().substring(KEY_PREFIX.length()).split("_");
                    if (parts.length != 3) {
                        continue;
                    }
                    try {
                        int x = (cx << 4) + Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = (cz << 4) + Integer.parseInt(parts[2]);
                        if (x >= minX && x <= maxX && z >= minZ && z <= maxZ && Math.abs(y - block.getY()) <= radius) {
                            return true;
                        }
                    } catch (NumberFormatException ignored) {
                        // Not a fruit spot key
                    }
                }
            }
        }
        return false;
    }

    /**
     * Provides the ripe fruit that hangs from the block, which falls if the block is removed.
     */
    public static List<Block> getAttached(Block support) {
        List<Block> attached = new ArrayList<>();
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Block side = support.getRelative(face);
            Spot spot = getRipe(side);
            if (spot != null && spot.kind() == Kind.HEAD && spot.data().equals(face.name())) {
                attached.add(side);
            }
        }
        Block below = support.getRelative(BlockFace.DOWN);
        Spot spot = getRipe(below);
        if (spot != null && spot.kind() == Kind.MOSS) {
            attached.add(below);
        }
        return attached;
    }

    /**
     * Picks a piece of ripe fruit, dropping the fruit and leaving the spot to grow back.
     * @param block The block showing the fruit.
     * @param player The player who picked it, or null if it fell on its own.
     * @param fortune The level of Fortune on the tool used, which adds to bunches of fruit.
     * @param removed Whether the block is being removed, rather than only picked.
     * @return Whether fruit was picked.
     */
    public static boolean harvest(Block block, Player player, int fortune, boolean removed) {
        Spot spot = getRipe(block);
        if (spot == null) {
            return false;
        }
        // Picked fruit is gone from the tree, a bunch included, and grows back in its place
        if (!removed) {
            block.setType(Material.AIR, false);
        }
        save(block, new Spot(spot.id(), spot.kind(), spot.data(), System.currentTimeMillis()));
        // Players in Creative mode get nothing, as items dropped there could be picked up again in Survival
        if (player != null && player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        ItemStack drop = createDrop(spot, fortune, player);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
        if (player != null) {
            reward(player, spot.kind(), "FRUIT_" + spot.id().toUpperCase(Locale.ROOT));
        }
        return true;
    }

    /**
     * Takes a piece of ripe fruit off its tree without dropping anything.
     * @param progress How far it has already grown back, from 0 when just picked to 1 when ripe.
     */
    public static void unripen(Block block, double progress) {
        Spot spot = getRipe(block);
        if (spot == null) {
            return;
        }
        block.setType(Material.AIR, false);
        long pickedAt = System.currentTimeMillis() - (long) (REGROW_MS * Math.max(0, Math.min(1, progress)));
        save(block, new Spot(spot.id(), spot.kind(), spot.data(), pickedAt));
    }

    /**
     * Picks a ripe bunch of fruit whose leaf decayed. It drops its fruit but does not grow back, as its tree is gone.
     */
    public static void dropDecayedBunch(Block block) {
        Spot spot = getRipe(block);
        if (spot == null || spot.kind() != Kind.BUNCH) {
            return;
        }
        forget(block);
        ItemStack drop = createDrop(spot, 0, null);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
    }

    /**
     * Drops the fruit of a head that fell off without being picked, such as when the leaf behind it decayed or was blown
     * up, as the head itself is never dropped.
     * @return Whether the head was a piece of fruit.
     */
    public static boolean dropFallenHead(Block block) {
        Spot spot = getStored(block);
        if (spot == null || !spot.isRipe() || spot.kind() != Kind.HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
            return false;
        }
        save(block, new Spot(spot.id(), spot.kind(), spot.data(), System.currentTimeMillis()));
        ItemStack drop = createDrop(spot, 0, null);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
        return true;
    }

    /**
     * Creates the fruit dropped by a spot.
     * @param player The player who picked it, or null if it fell on its own.
     */
    private static ItemStack createDrop(Spot spot, int fortune, Player player) {
        ProduceType type = ProduceType.fromId(spot.id());
        ItemStack item = type == null ? (spot.id().equals("apple") ? new ItemStack(Material.APPLE) : null)
                : new Produce(type).getItem();
        if (item == null) {
            return null;
        }
        int amount = switch (spot.id()) {
            case "cherry", "lychee" -> between(2, 3);
            case "olive", "banana" -> between(2, 4);
            // A bunch of dates drops 3 to 7, or up to 9 with Fortune
            case "date" -> Math.min(9, between(3, 7) + (fortune > 0 ? RANDOM.nextInt(fortune + 1) : 0));
            default -> 1;
        };
        item.setAmount(Math.min(item.getMaxStackSize(), amount));
        CropUtils.updateSeedLore(item, null);
        return item;
    }

    /**
     * Scales how much a harvest gives, the same way sweet berries are.
     * @param player The player who picked it, or null if it fell on its own.
     * @param season The vanilla crop whose seasonal yield it follows.
     * @return How much it gives, never less than 1.
     */
    public static int scaleYield(int amount, Player player, Material season) {
        if (player != null) {
            if (AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) && RANDOM.nextDouble() < 0.5) {
                amount++;
            }
            if (AranarthUtils.isWearingArmorType(player, "elven") && RANDOM.nextDouble() < 0.5) {
                amount++;
            }
        }
        Month month = AranarthUtils.getMonth();
        double scaled = amount * CropUtils.getCropYieldMultiplier(month, season);
        boolean winter = DateUtils.isWinterMonth(month) || month == Month.IGNIVOR;
        int total = Math.max(1, winter ? (int) scaled : (int) Math.ceil(scaled));
        if (player != null) {
            total *= herbalismMultiplier(player);
        }
        return total;
    }

    /**
     * Provides how many times over the player's Herbalism multiplies what they pick.
     */
    private static int herbalismMultiplier(Player player) {
        try {
            McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
            if (mcMMOPlayer == null) {
                return 1;
            }
            if (Permissions.isSubSkillEnabled(player, SubSkillType.HERBALISM_VERDANT_BOUNTY)
                    && RankUtils.hasUnlockedSubskill(player, SubSkillType.HERBALISM_VERDANT_BOUNTY)
                    && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.HERBALISM_VERDANT_BOUNTY, mcMMOPlayer)) {
                return 3;
            }
            if (Permissions.isSubSkillEnabled(player, SubSkillType.HERBALISM_DOUBLE_DROPS)
                    && RankUtils.hasUnlockedSubskill(player, SubSkillType.HERBALISM_DOUBLE_DROPS)
                    && ProbabilityUtil.isSkillRNGSuccessful(SubSkillType.HERBALISM_DOUBLE_DROPS, mcMMOPlayer)) {
                return 2;
            }
        } catch (RuntimeException e) {
            // mcMMO has not loaded the player yet, so there is no bonus
        }
        return 1;
    }

    /**
     * Gives Herbalism experience, and Farmer pay to Farmers, for picking fruit in a survival world.
     */
    private static void reward(Player player, Kind kind, String questKey) {
        reward(player, kind == Kind.BUNCH ? 60 : 30, kind == Kind.BUNCH ? 1.20 : 0.60, questKey);
    }

    /**
     * Gives Herbalism experience, Farmer pay and job experience to Farmers.
     * @param questKey What was harvested, for quests: FRUIT_ and the fruit's id, or PRODUCE_ and the crop's name.
     */
    public static void reward(Player player, int herbalismXp, double farmerPay, String questKey) {
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            return;
        }
        ExperienceAPI.addXP(player, "Herbalism", herbalismXp, "UNKNOWN");
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer != null && aranarthPlayer.getJobData().hasJob(JobType.FARMER)) {
            JobUtils.awardJob(player, JobType.FARMER, farmerPay);
        }
        QuestUtils.progress(player, QuestTrigger.HARVEST, questKey, 1);
    }

    /**
     * Plays the sound of fruit being picked by hand.
     */
    public static void playPickSound(Block block) {
        block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1F,
                between(9, 12) / 10F);
    }

    /**
     * Provides how long picked fruit of the season takes to grow back in the month.
     */
    public static long getRegrowTime(FruitSeason season, Month month) {
        return (long) (REGROW_MS / season.getSpeed(month));
    }

    private static int between(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }

    /**
     * Remembers every picked piece of fruit in the chunk, so that it grows back, and takes down the ripe fruit that is
     * out of season this month, without dropping it.
     */
    public static void trackPicked(Chunk chunk) {
        Month month = AranarthUtils.getMonth();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : new ArrayList<>(pdc.getKeys())) {
            if (!key.getNamespace().equals("aranarthcore") || !key.getKey().startsWith(KEY_PREFIX)) {
                continue;
            }
            String value = pdc.get(key, PersistentDataType.STRING);
            if (value == null) {
                continue;
            }
            String[] parts = key.getKey().substring(KEY_PREFIX.length()).split("_");
            try {
                int x = (chunk.getX() << 4) + Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int z = (chunk.getZ() << 4) + Integer.parseInt(parts[2]);
                if (value.endsWith(";0")) {
                    Block block = chunk.getWorld().getBlockAt(x, y, z);
                    Spot spot = getRipe(block);
                    if (spot != null && !FruitSeason.of(spot.id()).bears(block, month)) {
                        unripen(block, 0);
                    }
                } else {
                    PICKED.add(new Location(chunk.getWorld(), x, y, z));
                }
            } catch (RuntimeException ignored) {
                // A malformed key is left alone
            }
        }
    }

    /**
     * Forgets the picked fruit of a chunk that is no longer loaded.
     */
    public static void untrackPicked(Chunk chunk) {
        PICKED.removeIf(loc -> loc.getWorld().equals(chunk.getWorld())
                && loc.getBlockX() >> 4 == chunk.getX() && loc.getBlockZ() >> 4 == chunk.getZ());
    }

    /**
     * Grows back every picked piece of fruit that has waited long enough and is in season.
     */
    public static void regrow() {
        Month month = AranarthUtils.getMonth();
        if (month != seasonMonth) {
            seasonMonth = month;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    trackPicked(chunk);
                }
            }
        }
        long now = System.currentTimeMillis();
        for (Location location : new ArrayList<>(PICKED)) {
            if (!location.isChunkLoaded()) {
                PICKED.remove(location);
                continue;
            }
            Block block = location.getBlock();
            Spot spot = getStored(block);
            if (spot == null || spot.isRipe()) {
                PICKED.remove(location);
                continue;
            }
            FruitSeason season = FruitSeason.of(spot.id());
            boolean bears = season.bears(block, month);
            long wait = getRegrowTime(season, month);
            if (now - spot.pickedAt() < wait) {
                if (bears && spot.kind() == Kind.BUNCH) {
                    growBunch(block, spot, (now - spot.pickedAt()) / (double) wait);
                }
                continue;
            }
            if (!bears) {
                // Fruit out of season waits a full regrowth again, so that it fills in gradually once its season comes
                save(block, new Spot(spot.id(), spot.kind(), spot.data(), now));
                continue;
            }
            regrowSpot(block, spot);
        }
    }

    /**
     * Places the leaf a growing bunch of fruit shows once part of its time has passed, so that it grows back gradually.
     * @param progress How far it has grown back, from 0 when picked to 1 when ripe.
     */
    private static void growBunch(Block block, Spot spot, double progress) {
        int stage = (int) Math.floor(progress * (BUNCH_STAGES.length + 1)) - 1;
        if (stage < 0) {
            return;
        }
        int targetStage = Math.min(stage, BUNCH_STAGES.length - 1);
        Material target = BUNCH_STAGES[targetStage];
        if (block.getType() == target) {
            return;
        }
        // A bunch never shrinks back to an earlier stage, such as after waiting out of season
        for (int i = targetStage + 1; i < BUNCH_STAGES.length; i++) {
            if (block.getType() == BUNCH_STAGES[i]) {
                return;
            }
        }
        if (!block.getType().isAir() && getGrowingBunch(block) == null) {
            forget(block);
            return;
        }
        if (!hasTreeNextTo(block)) {
            forget(block);
            return;
        }
        BlockData data = target.createBlockData();
        if (data instanceof Leaves leaves) {
            leaves.setPersistent(true);
        }
        block.setBlockData(data, false);
    }

    /**
     * Determines if a bunch of fruit still has its tree next to it, so that it never grows back in mid-air.
     */
    private static boolean hasTreeNextTo(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Material type = block.getRelative(face).getType();
            if (Tag.LOGS.isTagged(type) || Tag.LEAVES.isTagged(type) || type == Material.STRIPPED_JUNGLE_WOOD) {
                return true;
            }
        }
        return false;
    }

    private static void regrowSpot(Block block, Spot spot) {
        switch (spot.kind()) {
            case HEAD -> {
                BlockFace facing;
                try {
                    facing = BlockFace.valueOf(spot.data());
                } catch (IllegalArgumentException e) {
                    forget(block);
                    return;
                }
                Material support = block.getRelative(facing.getOppositeFace()).getType();
                String texture = ProduceHeads.get(spot.id());
                // A head left floating after its tree was cut is cleared instead of growing back
                if (block.getType() == Material.PLAYER_WALL_HEAD
                        && !(Tag.LEAVES.isTagged(support) || Tag.LOGS.isTagged(support))) {
                    block.setType(Material.AIR, false);
                    forget(block);
                    return;
                }
                // Fruit only grows back where nothing else took its place and the leaf or log it hangs from remains
                if (!block.getType().isAir() || texture == null
                        || !(Tag.LEAVES.isTagged(support) || Tag.LOGS.isTagged(support))) {
                    forget(block);
                    return;
                }
                Directional head = (Directional) Material.PLAYER_WALL_HEAD.createBlockData();
                head.setFacing(facing);
                block.setBlockData(head, false);
                if (block.getState() instanceof Skull skull) {
                    skull.setPlayerProfile(MobHeadUtils.createTextureProfile(texture));
                    skull.update(true, false);
                }
            }
            case MOSS -> {
                if (!block.getType().isAir() || !Tag.LEAVES.isTagged(block.getRelative(BlockFace.UP).getType())) {
                    forget(block);
                    return;
                }
                HangingMoss moss = (HangingMoss) Material.PALE_HANGING_MOSS.createBlockData();
                moss.setTip(true);
                block.setBlockData(moss, false);
            }
            case BUNCH -> {
                Material ripe = Material.matchMaterial(spot.data());
                if (ripe == null || !Tag.LEAVES.isTagged(ripe) || !hasTreeNextTo(block)
                        || (!block.getType().isAir() && getGrowingBunch(block) == null)) {
                    forget(block);
                    return;
                }
                BlockData data = ripe.createBlockData();
                if (data instanceof Leaves leaves) {
                    leaves.setPersistent(true);
                }
                block.setBlockData(data, false);
            }
        }
        save(block, new Spot(spot.id(), spot.kind(), spot.data(), 0));
    }
}
