package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.crops.CropType;
import com.aearost.aranarthcore.crops.Crops;
import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CropUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Plants, picks and breaks the custom crops, and keeps the vanilla blocks they are made of from growing, spreading,
 * decaying, popping off or being trampled on their own.
 */
public class CropListener implements Listener {

    /** How often crops are checked to grow: every 20 seconds. */
    private static final long GROW_TICKS = 400L;

    public CropListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                Crops.track(chunk);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, Crops::growAll, GROW_TICKS, GROW_TICKS);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Crops.track(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Crops.untrack(e.getChunk());
    }

    /**
     * Right-clicking plants a crop on its soil, picks a ripe crop that grows its fruit back, or grows a crop with bone meal.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        // Vanilla sweet berries and glow berries are eaten twice as fast, like every other berry
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            CropUtils.applyBerryEatSpeed(e.getPlayer().getInventory().getItemInMainHand());
            CropUtils.applyBerryEatSpeed(e.getPlayer().getInventory().getItemInOffHand());
        }
        Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }
        // Crops are never trampled
        if (e.getAction() == Action.PHYSICAL) {
            if (clicked.getType() == Material.FARMLAND && Crops.getBase(clicked.getRelative(BlockFace.UP)) != null) {
                e.setCancelled(true);
            }
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = e.getPlayer();
        Block base = Crops.getBase(clicked);
        if (base != null) {
            e.setCancelled(true);
            if (e.getHand() != EquipmentSlot.HAND || !canUse(player, clicked)) {
                return;
            }
            Crops.Crop crop = Crops.get(base);
            if (crop == null) {
                return;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            // A ripe crop is picked first, even with bone meal in hand, which is then only used once there is nothing to pick
            if (pick(base, clicked, crop, player)) {
                player.swingMainHand();
                return;
            }
            if (hand.getType() == Material.BONE_MEAL) {
                if (Crops.boneMeal(base)) {
                    if (player.getGameMode() != GameMode.CREATIVE) {
                        hand.setAmount(hand.getAmount() - 1);
                    }
                    clicked.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, clicked.getLocation().add(0.5, 0.5, 0.5), 10,
                            0.4, 0.4, 0.4);
                }
                return;
            }
            return;
        }
        // Planting on the top of the soil, or into grass growing on it, which it replaces like a sapling would
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        ProduceType produce = Produce.getType(hand);
        CropType type = produce == null ? null : CropType.fromPlant(produce);
        if (type == null) {
            return;
        }
        Block above = clicked.getRelative(BlockFace.UP);
        if (isGroundCover(clicked)) {
            above = clicked;
            clicked = clicked.getRelative(BlockFace.DOWN);
        } else if (e.getBlockFace() != BlockFace.UP) {
            return;
        }
        if (!(above.getType().isAir() || isGroundCover(above)) || !Crops.isSoilIgnoringWater(type, clicked)) {
            return;
        }
        e.setCancelled(true);
        if (!Crops.isSoil(type, clicked)) {
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "crops.needs_water")));
            return;
        }
        if (type.isVine()) {
            int post = Crops.findPost(above);
            if (post < 0) {
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, post == -2 ? "crops.post_taken" : "crops.needs_post")));
                return;
            }
        }
        if (!canBuild(player, above)) {
            return;
        }
        BlockData cover = above.getBlockData();
        if (!above.getType().isAir()) {
            above.setType(Material.AIR, false);
        }
        if (!Crops.plant(above, type)) {
            // Nothing was planted, so the grass it would have replaced comes back
            if (above.getType().isAir()) {
                above.setBlockData(cover, false);
            }
        } else {
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
            player.swingMainHand();
            above.getWorld().playSound(above.getLocation().add(0.5, 0.2, 0.5), Sound.ITEM_CROP_PLANT, 1F, 1F);
        }
    }

    /**
     * Breaking a ripe field crop harvests it, breaking the fruit of a regrowing crop picks it, and breaking any other part
     * of a crop, or the soil under it, breaks the whole crop.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        Block base = Crops.getBase(block);
        // Breaking a fence of a trellis takes off the leaves that no longer touch a fence, or the whole vine for its post.
        // It is checked once the fence is gone.
        if (base == null && Tag.FENCES.isTagged(block.getType())) {
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> Crops.checkTrellises(block, player));
            return;
        }
        if (base == null) {
            Block above = Crops.getBase(block.getRelative(BlockFace.UP));
            if (above != null && above.equals(block.getRelative(BlockFace.UP))) {
                Crops.uproot(above, player);
            }
            return;
        }
        e.setCancelled(true);
        Crops.Crop crop = Crops.get(base);
        if (crop == null) {
            block.setType(Material.AIR);
            return;
        }
        // Breaking a leaf of a vine trims it, and only breaking the plant itself breaks the whole vine
        if (crop.type().isVine() && !block.equals(base)) {
            Crops.trimVine(base, block, player);
            return;
        }
        // Breaking a ripe bush of a tomato plant picks it, and breaking any other part breaks the whole plant
        if (crop.type() == CropType.TOMATO) {
            if (!Crops.pickTomato(base, block, player)) {
                Crops.uproot(base, player);
            }
            return;
        }
        // Breaking a bell pepper picks only that pepper, and breaking any other part breaks the whole plant
        if (crop.type().hasPieces()) {
            if (!Crops.pickPiece(base, block, player, true)) {
                Crops.uproot(base, player);
            }
            return;
        }
        if (crop.type().getMode() == CropType.HarvestMode.REGROW && crop.isRipe() && !crop.isBarren()
                && Crops.isFruitBlock(base, block)) {
            Crops.harvest(base, player, false);
            return;
        }
        Crops.uproot(base, player);
    }

    /**
     * Picks a crop by right-clicking it.
     */
    private static boolean pick(Block base, Block clicked, Crops.Crop crop, Player player) {
        if (crop.type().isVine()) {
            return Crops.pickVineFruit(base, clicked, player);
        }
        if (crop.type().hasPieces()) {
            return Crops.pickPiece(base, clicked, player, false);
        }
        if (crop.type() == CropType.TOMATO) {
            return Crops.pickTomato(base, clicked, player);
        }
        return crop.isRipe() && !crop.isBarren() && (crop.type().getMode() == CropType.HarvestMode.REGROW
                || crop.type().isPickedByClicking() || Crops.isFruitBlock(base, clicked))
                && Crops.harvest(base, player, false);
    }

    /**
     * Determines if the block is grass or a fern growing on the ground, which a crop can be planted into, replacing it.
     */
    private static boolean isGroundCover(Block block) {
        Material material = block.getType();
        return (material == Material.SHORT_GRASS || material == Material.FERN || material == Material.SHORT_DRY_GRASS)
                && Crops.getBase(block) == null;
    }

    private static boolean canBuild(Player player, Block block) {
        if (AranarthUtils.isProtectedSpawnLocation(block.getLocation())) {
            return false;
        }
        Dominion dominion = DominionUtils.getDominionOfChunk(block.getChunk());
        return dominion == null || DominionUtils.hasPermission(player, dominion, DominionPermission.BUILD);
    }

    private static boolean canUse(Player player, Block block) {
        if (AranarthUtils.isProtectedSpawnLocation(block.getLocation())) {
            return false;
        }
        Dominion dominion = DominionUtils.getDominionOfChunk(block.getChunk());
        return dominion == null || DominionUtils.hasPermission(player, dominion, DominionPermission.MISC_INTERACT);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        if (Crops.isCropBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        Block block = e.getBlock();
        if (Crops.isCropBlock(block)) {
            e.setCancelled(true);
            return;
        }
        // A melon stem of a crop never grows a melon beside it
        Material grown = e.getNewState().getType();
        if (grown == Material.MELON || grown == Material.PUMPKIN) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                if (Crops.isCropBlock(block.getRelative(face))) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (Crops.isCropBlock(e.getSource()) || Crops.isCropBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        if (Crops.isCropBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (Crops.isCropBlock(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (Crops.isCropBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    /**
     * The farmland under a crop never dries back to dirt.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (Crops.isCropBlock(e.getBlock()) || e.getBlock().getType() == Material.FARMLAND
                && Crops.isCropBlock(e.getBlock().getRelative(BlockFace.UP))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTrample(EntityInteractEvent e) {
        Block block = e.getBlock();
        if (block.getType() == Material.FARMLAND && Crops.isCropBlock(block.getRelative(BlockFace.UP))) {
            e.setCancelled(true);
        }
    }

    /**
     * Mobs never eat, trample or pick up a crop.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        Block block = e.getBlock();
        if (Crops.isCropBlock(block) || block.getType() == Material.FARMLAND && Crops.isCropBlock(block.getRelative(BlockFace.UP))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (touchesCrop(e.getBlocks()) || Crops.isCropBlock(e.getBlock().getRelative(e.getDirection()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (touchesCrop(e.getBlocks())) {
            e.setCancelled(true);
        }
    }

    private static boolean touchesCrop(List<Block> blocks) {
        for (Block block : blocks) {
            if (Crops.isCropBlock(block) || Crops.isCropBlock(block.getRelative(BlockFace.UP))) {
                return true;
            }
        }
        return false;
    }

    /**
     * A crop caught in fire burns away, dropping nothing.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Block base = Crops.getBase(e.getBlock());
        if (base != null) {
            e.setCancelled(true);
            Crops.remove(base);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        explode(e.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        explode(e.blockList());
    }

    /**
     * A crop caught in an explosion is broken, giving back what it was planted from, and is not dropped as vanilla blocks.
     */
    private static void explode(List<Block> blocks) {
        blocks.removeIf(block -> {
            Block base = Crops.getBase(block);
            if (base == null) {
                return false;
            }
            Crops.uproot(base, null);
            return true;
        });
    }

    /**
     * Only blackberries have thorns. Every other crop grown from a sweet berry bush, such as tomatoes, is harmless.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onThorns(EntityDamageByBlockEvent e) {
        Block block = e.getDamager();
        if (block == null || block.getType() != Material.SWEET_BERRY_BUSH) {
            return;
        }
        Block base = Crops.getBase(block);
        if (base == null) {
            return;
        }
        Crops.Crop crop = Crops.get(base);
        if (crop != null && !crop.type().hasThorns()) {
            e.setCancelled(true);
        }
    }
}
