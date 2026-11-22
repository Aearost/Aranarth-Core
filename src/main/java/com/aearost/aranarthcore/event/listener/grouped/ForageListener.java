package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.crops.CropType;
import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import com.aearost.aranarthcore.objects.Boost;
import com.aearost.aranarthcore.trees.WildGrowth;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.FruitUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Handles what is found in the wild: acorns falling from oak and dark oak leaves, wild crops found in grass, and pigs
 * eating acorns.
 */
public class ForageListener implements Listener {

    /** The chance of an acorn falling from oak or dark oak leaves that decay or are broken. */
    private static final double ACORN_CHANCE = 0.03;
    /** The chance of grass dropping something that plants a crop growing wild in its biome. */
    private static final double WILD_CROP_CHANCE = 0.02;
    /** How far away pigs notice a player holding an acorn. */
    private static final double TEMPT_RANGE = 10;
    /** How close pigs come to a player holding an acorn. */
    private static final double TEMPT_STOP = 2.5;
    private static final double TEMPT_SPEED = 1.2;
    private static final long TEMPT_TICKS = 10L;
    /** How long a pig stays ready to breed once fed, as in vanilla. */
    private static final int LOVE_TICKS = 600;
    /** How often acorns may fall from the trees around each player: every 5 seconds. */
    private static final long FALL_TICKS = 100L;
    /** How many spots around each player are checked for a tree each time. */
    private static final int FALL_SAMPLES = 3;
    /** How far around a player acorns fall. */
    private static final int FALL_RADIUS = 12;
    /** How far above and below a player the canopy is searched for. */
    private static final int FALL_ABOVE = 16;
    private static final int FALL_BELOW = 6;
    /** The furthest an acorn falls before landing, so none fall from leaves high over a ravine or the void. */
    private static final int FALL_DEPTH = 24;
    /**
     * The chance of an acorn falling from a checked spot under oak or dark oak leaves.
     */
    private static final double FALL_CHANCE = 0.12;
    /**
     * How close fruit must be for an oak or dark oak leaf to count as part of a fruit tree, which never drops acorns.
     */
    private static final int FRUIT_TREE_RADIUS = 6;

    private static final Set<Material> GRASS = EnumSet.of(Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN,
            Material.LARGE_FERN, Material.SHORT_DRY_GRASS, Material.TALL_DRY_GRASS);

    private final Random random = new Random();

    public ForageListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::temptPigs, TEMPT_TICKS, TEMPT_TICKS);
        Bukkit.getScheduler().runTaskTimer(plugin, this::dropFallingAcorns, FALL_TICKS, FALL_TICKS);
    }

    /**
     * Acorns now and then fall from the oak and dark oak trees around players wandering a forest
     */
    private void dropFallingAcorns() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                    || !AranarthUtils.isSurvivalWorld(player.getWorld().getName())
                    || AranarthUtils.isAfk(AranarthUtils.getPlayer(player.getUniqueId()))) {
                continue;
            }
            for (int i = 0; i < FALL_SAMPLES; i++) {
                int x = player.getLocation().getBlockX() + random.nextInt(FALL_RADIUS * 2 + 1) - FALL_RADIUS;
                int z = player.getLocation().getBlockZ() + random.nextInt(FALL_RADIUS * 2 + 1) - FALL_RADIUS;
                if (!player.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block leaf = findCanopyBottom(player.getWorld().getBlockAt(x, player.getLocation().getBlockY(), z));
                if (leaf != null && random.nextDouble() < FALL_CHANCE * getHarvestBoost()) {
                    leaf.getWorld().dropItem(leaf.getLocation().add(0.5, -0.3, 0.5), new Produce(ProduceType.ACORN).getItem());
                }
            }
        }
    }

    /**
     * Provides the lowest oak or dark oak leaf above or near the block that an acorn can fall from and land where players
     * can reach it, never on other leaves or in water.
     * @param around A block at the player's height in the column to search.
     * @return The leaf, or null if the column has no such leaf.
     */
    private static Block findCanopyBottom(Block around) {
        for (int dy = -FALL_BELOW; dy <= FALL_ABOVE; dy++) {
            Block block = around.getRelative(0, dy, 0);
            if (isOakLeaf(block) && landsReachably(block)) {
                return isOnFruitTree(block) ? null : block;
            }
        }
        return null;
    }

    /**
     * Determines if an acorn falling from under the leaf lands on the ground, such as grass or dirt, rather than being stuck
     * on a lower layer of the canopy, a branch or in water.
     * @param leaf The leaf it falls from.
     * @return Whether it lands somewhere reachable.
     */
    private static boolean landsReachably(Block leaf) {
        Block block = leaf.getRelative(0, -1, 0);
        if (!block.isPassable() || block.isLiquid()) {
            return false;
        }
        for (int fall = 0; fall < FALL_DEPTH; fall++) {
            if (block.isLiquid()) {
                return false;
            }
            if (!block.isPassable()) {
                return !Tag.LEAVES.isTagged(block.getType()) && !Tag.LOGS.isTagged(block.getType());
            }
            block = block.getRelative(0, -1, 0);
        }
        return false;
    }

    /**
     * Breaking oak or dark oak leaves may drop an acorn, and breaking grass by hand may drop a wild crop.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Block block = e.getBlock();
        if (player.getGameMode() == GameMode.CREATIVE || !e.isDropItems()
                || !AranarthUtils.isSurvivalWorld(block.getWorld().getName())) {
            return;
        }
        if (isAcornLeaf(block)) {
            dropAcorn(block);
        } else if (GRASS.contains(block.getType()) && player.getInventory().getItemInMainHand().getType() != Material.SHEARS) {
            dropWildCrop(block);
        }
    }

    /**
     * Oak and dark oak leaves decaying may drop an acorn.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        Block block = e.getBlock();
        if (isAcornLeaf(block) && AranarthUtils.isSurvivalWorld(block.getWorld().getName())) {
            dropAcorn(block);
        }
    }

    /**
     * Feeding an acorn to a pig readies it to breed, or helps a piglet grow up, like a carrot would.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFeedPig(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Pig pig)) {
            return;
        }
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItem(e.getHand());
        if (Produce.getType(hand) != ProduceType.ACORN) {
            return;
        }
        // An acorn in the main hand is fed first, so the off hand is never fed in the same click
        if (e.getHand() == EquipmentSlot.OFF_HAND
                && Produce.getType(player.getInventory().getItemInMainHand()) == ProduceType.ACORN) {
            return;
        }
        if (pig.isAdult()) {
            if (!pig.canBreed() || pig.isLoveMode()) {
                return;
            }
            pig.setLoveModeTicks(LOVE_TICKS);
            pig.setBreedCause(player.getUniqueId());
            pig.getWorld().spawnParticle(Particle.HEART, pig.getLocation().add(0, pig.getHeight(), 0), 7, 0.4, 0.3, 0.4);
        } else {
            // Vanilla takes a tenth off the time left to grow up
            int age = pig.getAge();
            int seconds = (int) ((-age / 20) * 0.1F);
            pig.setAge(Math.min(0, age + seconds * 20));
            pig.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pig.getLocation().add(0, pig.getHeight(), 0), 7, 0.4, 0.3, 0.4);
        }
        e.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }
        if (e.getHand() == EquipmentSlot.HAND) {
            player.swingMainHand();
        } else {
            player.swingOffHand();
        }
        pig.getWorld().playSound(pig.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
    }

    /**
     * Pigs walk towards nearby players holding an acorn, like they do for a carrot.
     */
    private void temptPigs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR || !isHoldingAcorn(player)) {
                continue;
            }
            for (Pig pig : player.getWorld().getNearbyEntitiesByType(Pig.class, player.getLocation(), TEMPT_RANGE)) {
                if (pig.isLeashed() || pig.isInsideVehicle() || !pig.getPassengers().isEmpty()) {
                    continue;
                }
                if (pig.getLocation().distanceSquared(player.getLocation()) > TEMPT_STOP * TEMPT_STOP) {
                    pig.getPathfinder().moveTo(player.getLocation(), TEMPT_SPEED);
                } else {
                    pig.getPathfinder().stopPathfinding();
                }
                pig.lookAt(player.getEyeLocation());
            }
        }
    }

    private static boolean isHoldingAcorn(Player player) {
        return Produce.getType(player.getInventory().getItemInMainHand()) == ProduceType.ACORN
                || Produce.getType(player.getInventory().getItemInOffHand()) == ProduceType.ACORN;
    }

    /**
     * Determines if the block is oak or dark oak leaves grown by a tree, rather than placed by a player.
     */
    private static boolean isAcornLeaf(Block block) {
        return isOakLeaf(block) && !isOnFruitTree(block);
    }

    /**
     * Determines if the block is oak or dark oak leaves grown by a tree, rather than placed by a player.
     */
    private static boolean isOakLeaf(Block block) {
        return (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES)
                && block.getBlockData() instanceof Leaves leaves && !leaves.isPersistent();
    }

    /**
     * Determines if the leaf belongs to a fruit tree, such as an apple, plum or avocado tree.
     */
    private static boolean isOnFruitTree(Block leaf) {
        return FruitUtils.isNearFruit(leaf, FRUIT_TREE_RADIUS);
    }

    private void dropAcorn(Block block) {
        if (random.nextDouble() < ACORN_CHANCE * getHarvestBoost()) {
            block.getWorld().dropItemNaturally(block.getLocation(), new Produce(ProduceType.ACORN).getItem());
        }
    }

    /**
     * Drops what plants one of the crops that grow wild in the grass's biome, such as corn seeds on the plains.
     */
    private void dropWildCrop(Block block) {
        if (random.nextDouble() >= WILD_CROP_CHANCE * getHarvestBoost()) {
            return;
        }
        List<CropType> crops = WildGrowth.cropsFor(block.getBiome());
        if (crops.isEmpty()) {
            return;
        }
        ItemStack item = new Produce(crops.get(random.nextInt(crops.size())).getPlant()).getItem();
        CropUtils.updateSeedLore(item, block.getWorld());
        block.getWorld().dropItemNaturally(block.getLocation(), item);
    }

    /**
     * The Harvest boost makes leaf drops a third more likely, as it does for apples and God Apple Fragments.
     */
    private static double getHarvestBoost() {
        return AranarthUtils.getServerBoosts().containsKey(Boost.HARVEST) ? 1 / 0.75 : 1;
    }
}
