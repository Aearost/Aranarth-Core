package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CreativePlot;
import com.aearost.aranarthcore.objects.PlotPattern;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.aearost.aranarthcore.utils.PlotUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Centralizes all protections in the Creative plots world.
 */
public class PlotProtectionListener implements Listener {

    /** The plot each player is standing in, so the plot title only shows when entering a different plot. */
    private final Map<UUID, String> currentPlots = new HashMap<>();

    public PlotProtectionListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Provides whether the player is not allowed to change the location.
     */
    private boolean isDenied(Player player, Location loc) {
        return PlotUtils.isPlotWorld(loc.getWorld()) && !PlotUtils.canBuild(player, loc);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (isDenied(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (isDenied(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (isDenied(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (isDenied(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents interacting with blocks outside of the player's plots. Spawn plots are left to the spawn protections.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null || !PlotUtils.isPlotWorld(block.getWorld()) || PlotUtils.isSpawnPlotLocation(block.getLocation())) {
            return;
        }
        if (!PlotUtils.canBuild(e.getPlayer(), block.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Location loc = e.getRightClicked().getLocation();
        if (PlotUtils.isSpawnPlotLocation(loc)) {
            return;
        }
        if (isDenied(e.getPlayer(), loc)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (isDenied(e.getPlayer(), e.getRightClicked().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent e) {
        if (e.getPlayer() != null && isDenied(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
            return;
        }
        if (e.getPlayer() != null && isEntityLimitReached(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent e) {
        if (e.getPlayer() != null && isDenied(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
            return;
        }
        if (e.getPlayer() != null && isEntityLimitReached(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents spawn eggs from being used anywhere in the plots world, as mobs can wander into other plots.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onSpawnEggUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (isSpawnEgg(e.getItem()) && PlotUtils.isPlotWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "plot.no_spawn_eggs")));
        }
    }

    /**
     * Prevents spawn eggs from being used on mobs in the plots world to spawn their babies.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onSpawnEggUseOnEntity(PlayerInteractEntityEvent e) {
        if (isSpawnEgg(e.getPlayer().getInventory().getItem(e.getHand())) && PlotUtils.isPlotWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "plot.no_spawn_eggs")));
        }
    }

    private boolean isSpawnEgg(ItemStack item) {
        return item != null && item.getType().name().endsWith("_SPAWN_EGG");
    }

    /**
     * Limits the number of entities in each plot, catching every way of spawning one (i.e dispensers).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent e) {
        Entity entity = e.getEntity();
        if (!PlotUtils.isPlotWorld(entity.getWorld()) || !PlotUtils.countsTowardEntityLimit(entity)) {
            return;
        }
        if (PlotUtils.isEntityLimitReached(e.getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Provides whether the plot at the location has reached its entity limit, notifying the player if so.
     */
    private boolean isEntityLimitReached(Player player, Location loc) {
        if (!PlotUtils.isPlotWorld(loc.getWorld()) || !PlotUtils.isEntityLimitReached(loc)) {
            return false;
        }
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "plot.entity_limit", "limit", PlotUtils.MAX_ENTITIES_PER_PLOT)));
        return true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (!PlotUtils.isPlotWorld(e.getEntity().getWorld())) {
            return;
        }
        if (!(e.getRemover() instanceof Player player) || !PlotUtils.canBuild(player, e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents players from attacking entities outside of their plots.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamageEntity(EntityDamageByEntityEvent e) {
        if (!PlotUtils.isPlotWorld(e.getEntity().getWorld()) || e.getEntity() instanceof Player) {
            return;
        }
        if (e.getDamageSource().getCausingEntity() instanceof Player player && !PlotUtils.canBuild(player, e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents water and lava from flowing out of a plot.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld())
                && !PlotUtils.isSameArea(e.getBlock().getLocation(), e.getToBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld()) && isPistonLeavingArea(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld()) && isPistonLeavingArea(e.getBlock(), e.getBlocks(), e.getDirection())) {
            e.setCancelled(true);
        }
    }

    /**
     * Provides whether a piston would move any block from or into a different area than the piston itself.
     */
    private boolean isPistonLeavingArea(Block piston, List<Block> moved, BlockFace direction) {
        Location pistonLoc = piston.getLocation();
        if (!PlotUtils.isSameArea(pistonLoc, piston.getRelative(direction).getLocation())) {
            return true;
        }
        for (Block block : moved) {
            if (!PlotUtils.isSameArea(pistonLoc, block.getLocation())
                    || !PlotUtils.isSameArea(pistonLoc, block.getRelative(direction).getLocation())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prevents trees and giant mushrooms from growing beyond the plot they were planted in.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        if (!PlotUtils.isPlotWorld(e.getWorld())) {
            return;
        }
        Location origin = e.getLocation();
        List<BlockState> blocks = e.getBlocks();
        blocks.removeIf(state -> !PlotUtils.isSameArea(origin, state.getLocation())
                || state.getY() <= PlotPattern.BEDROCK_Y);
    }

    /**
     * Prevents falling blocks and other entities from changing blocks outside of plots.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            return;
        }
        Entity entity = e.getEntity();
        if (entity instanceof Player player) {
            if (!PlotUtils.canBuild(player, e.getBlock().getLocation())) {
                e.setCancelled(true);
            }
        } else if (entity instanceof FallingBlock) {
            // Falling blocks can only land in the plot they started falling in
            Location origin = entity.getOrigin() != null ? entity.getOrigin() : entity.getLocation();
            if (PlotUtils.getAreaPlot(e.getBlock().getLocation()) == null
                    || !PlotUtils.isSameArea(origin, e.getBlock().getLocation())) {
                e.setCancelled(true);
            }
        } else {
            // Mobs from spawn eggs can wander into other plots, so they cannot change blocks (i.e endermen and withers)
            e.setCancelled(true);
        }
    }

    /**
     * Prevents dispensers and droppers from placing fluids, blocks, or entities outside of their plot.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        Block block = e.getBlock();
        if (!PlotUtils.isPlotWorld(block.getWorld()) || !(block.getBlockData() instanceof Directional directional)) {
            return;
        }
        if (!PlotUtils.isSameArea(block.getLocation(), block.getRelative(directional.getFacing()).getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents TNT from being primed in the plots world.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents entities from exploding in the plots world (i.e end crystals and TNT minecarts).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (PlotUtils.isPlotWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents any remaining explosions from destroying blocks in the plots world.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (PlotUtils.isPlotWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents explosions that cannot be stopped beforehand from hurting anything (i.e beds and respawn anchors).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if ((cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                && PlotUtils.isPlotWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosionKnockback(EntityKnockbackEvent e) {
        if (e.getCause() == EntityKnockbackEvent.Cause.EXPLOSION && PlotUtils.isPlotWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents projectiles from being launched in the plots world, as they can reach other plots.
     * This includes arrows, tridents, and splash and lingering potions. Ender pearls are still allowed.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        if (PlotUtils.isPlotWorld(e.getEntity().getWorld()) && !(e.getEntity() instanceof EnderPearl)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.FISHING && PlotUtils.isPlotWorld(e.getPlayer().getWorld())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents entities from forming blocks where they cannot build (i.e frost walker and snow golem trails).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent e) {
        if (!PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            return;
        }
        if (!(e.getEntity() instanceof Player player) || !PlotUtils.canBuild(player, e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents fire from spreading in the plots world. Players can only light fires in their own plots.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (!PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            return;
        }
        if (!(e.getPlayer() instanceof Player player) || !PlotUtils.canBuild(player, e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        if (!PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            return;
        }
        Location origin = e.getBlock().getLocation();
        if (e.getPlayer() != null && !PlotUtils.canBuild(e.getPlayer(), origin)) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(state -> !PlotUtils.isSameArea(origin, state.getLocation()));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (PlotUtils.isPlotWorld(e.getBlock().getWorld())
                && !PlotUtils.isSameArea(e.getSource().getLocation(), e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    /**
     * Prevents all mobs from spawning in the plots world, including spawners, breeding, and built golems and withers.
     * Armor stands and mannequins are not mobs, so they can still be placed.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (PlotUtils.isPlotWorld(e.getLocation().getWorld()) && e.getEntity() instanceof Mob) {
            e.setCancelled(true);
        }
    }

    /**
     * Updates the player's WorldEdit permissions when entering or leaving a creative world.
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        String from = e.getFrom().getName();
        String to = e.getPlayer().getWorld().getName();
        if (from.startsWith(PlotUtils.WORLD_NAME) || to.startsWith(PlotUtils.WORLD_NAME)) {
            PermissionUtils.evaluatePlayerPermissions(e.getPlayer());
        }
    }

    /**
     * Moves players out of solid blocks when they log in to the plots world.
     * Players who logged off in the creative world before it moved to the SMP can otherwise end up underground.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (!player.isOnline() || !PlotUtils.isPlotWorld(player.getWorld())) {
                return;
            }
            Block feet = player.getLocation().getBlock();
            Block head = feet.getRelative(BlockFace.UP);
            if (feet.getType().isOccluding() || head.getType().isOccluding()) {
                player.teleport(PlotUtils.getSpawnLocation());
            }
        }, 20L);
    }

    /**
     * Shows the plot's name and owner when a player walks into a claimed plot.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getWorld() == to.getWorld()) {
            return;
        }
        updateCurrentPlot(e.getPlayer(), to, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        // The teleport title already shows when teleporting to the Creative spawn
        updateCurrentPlot(e.getPlayer(), e.getTo(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        currentPlots.remove(e.getPlayer().getUniqueId());
    }

    /**
     * Tracks the plot the player is standing in, showing a title when it changes.
     * Only the inside of a plot counts, so walking along the roads and borders shows nothing.
     */
    private void updateCurrentPlot(Player player, Location to, boolean showSpawnTitle) {
        CreativePlot plot = PlotUtils.getAreaPlot(to);
        UUID uuid = player.getUniqueId();
        if (plot == null) {
            currentPlots.remove(uuid);
            return;
        }
        if (plot.isSpawn()) {
            // Spawn plots are merged together, so they count as a single area
            if (!"spawn".equals(currentPlots.put(uuid, "spawn")) && showSpawnTitle) {
                player.sendTitle(ChatUtils.translateToColor(Lang.getFor(player, "plot.title_spawn")),
                        ChatUtils.translateToColor(Lang.getFor(player, "plot.title_spawn_subtitle")));            }
            return;
        }
        // Merged plots share a name, so walking between them does not show the title again
        String identity = plot.getOwner() + ";" + plot.getName();
        if (identity.equals(currentPlots.put(uuid, identity))) {
            return;
        }
        String ownerName = AranarthUtils.getNickname(Bukkit.getOfflinePlayer(plot.getOwner()));
        player.sendTitle(ChatUtils.translateToColor("&e&l" + plot.getName()),
                ChatUtils.translateToColor(Lang.getFor(player, "plot.title_owner", "owner", ownerName)));
    }
}
