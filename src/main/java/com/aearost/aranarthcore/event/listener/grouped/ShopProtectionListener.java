package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;

import java.util.UUID;

/**
 * Centralizes all protections for the shops world.
 */
public class ShopProtectionListener implements Listener {

    public ShopProtectionListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private boolean isShopsWorld(Location location) {
        return location.getWorld() != null && location.getWorld().getName().equals(ShopIslandUtils.SHOPS_WORLD);
    }

    /**
     * Returns true if the player is allowed to modify blocks at the given location.
     * Admins, the island owner, and invited collaborators may all modify.
     */
    private boolean canModify(Player player, Location location) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer.isInAdminMode()) {
            return true;
        }
        UUID owner = ShopIslandUtils.getIslandOwnerAtLocation(location);
        if (owner == null) {
            return false;
        }
        return player.getUniqueId().equals(owner)
                || AranarthUtils.isShopCollaborator(owner, player.getUniqueId());
    }

    /**
     * Teleports a player to the home of whichever island's grid cell contains.
     *
     * @param player    The player to teleport.
     * @param reference A location whose X/Z is used to look up the island owner (Y is ignored).
     */
    private void teleportToIslandHome(Player player, Location reference) {
        UUID islandOwner = (reference != null) ? ShopIslandUtils.getIslandOwnerAtLocation(reference) : null;
        Location dest = (islandOwner != null) ? AranarthUtils.getShopLocations().get(islandOwner) : null;
        if (dest == null) {
            dest = AranarthUtils.getSafeTeleportLocation(
                    new Location(Bukkit.getWorld("spawn"), 0.5, 100, 0.5, 180, 0));
        }
        player.teleport(dest);
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!isShopsWorld(e.getBlock().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place blocks on another player's shop island!"));
        } else {
            // Owner may only build within the 50x50 boundary
            UUID owner = ShopIslandUtils.getIslandOwnerAtLocation(e.getBlock().getLocation());
            if (owner != null) {
                int[] center = AranarthUtils.getShopIslandCenters().get(owner);
                if (center != null && !ShopIslandUtils.isWithinBuildBoundary(e.getBlock().getLocation(), center[0], center[1])) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(ChatUtils.chatMessage("&cThis block is outside of your shop island's space!"));
                }
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!isShopsWorld(e.getBlock().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot break blocks on another player's shop island!"));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!isShopsWorld(block.getLocation())) {
            return;
        }
        // Only restrict interactive blocks
        if (!isInteractableBlock(block)) {
            return;
        }
        if (!canModify(e.getPlayer(), block.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!isShopsWorld(e.getRightClicked().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getRightClicked().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!isShopsWorld(e.getRightClicked().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getRightClicked().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlaceEntity(EntityPlaceEvent e) {
        if (e.getEntity() == null || e.getPlayer() == null) {
            return;
        }
        if (!isShopsWorld(e.getEntity().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage("&cYou cannot place entities on another player's shop island!"));
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent e) {
        if (!isShopsWorld(e.getEntity().getLocation())) {
            return;
        }
        if (e.getRemover() instanceof Player player) {
            if (!canModify(player, e.getEntity().getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents non-owners from removing items from item frames by left-clicking.
     */
    @EventHandler
    public void onItemFrameAttack(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof ItemFrame)) {
            return;
        }
        if (!isShopsWorld(e.getEntity().getLocation())) {
            return;
        }
        if (e.getDamageSource().getCausingEntity() instanceof Player player) {
            if (!canModify(player, e.getEntity().getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!isShopsWorld(e.getEntity().getLocation())) {
            return;
        }
        if (e.getEntity() instanceof Player) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (!isShopsWorld(e.getEntity().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignOpen(PlayerOpenSignEvent e) {
        if (!isShopsWorld(e.getSign().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getSign().getLocation())) {
            e.setCancelled(true);
        }
    }


    /**
     * Prevents non-owners from taking books from lecterns on another player's shop island while still allowing them to view the contents.
     */
    @EventHandler
    public void onTakeLecternBook(PlayerTakeLecternBookEvent e) {
        if (!isShopsWorld(e.getLectern().getLocation())) {
            return;
        }
        if (!canModify(e.getPlayer(), e.getLectern().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (!isShopsWorld(e.getLocation())) {
            return;
        }
        e.setCancelled(true);
    }

    // -----------------------------------------------------------------------
    // No fire spreading
    // -----------------------------------------------------------------------

    @EventHandler
    public void onFireCreate(BlockIgniteEvent e) {
        if (!isShopsWorld(e.getBlock().getLocation())) {
            return;
        }
        // Allow the island owner to ignite blocks (e.g. lighting candles)
        if (e.getPlayer() != null && canModify(e.getPlayer(), e.getBlock().getLocation())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent e) {
        if (!isShopsWorld(e.getBlock().getLocation())) {
            return;
        }
        Material material = e.getSource().getType();
        if (material == Material.FIRE || material == Material.SOUL_FIRE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!isShopsWorld(player.getLocation())) {
            return;
        }
        if (e.getTo() == null) {
            return;
        }

        double y = e.getTo().getY();
        if (y <= 50) {
            teleportToIslandHome(player, e.getTo());
            return;
        }

        // Out-of-bounds
        int x = e.getTo().getBlockX();
        int z = e.getTo().getBlockZ();
        if (x < 0 || z < 0) {
            teleportToIslandHome(player, e.getFrom());
            return;
        }

        UUID owner = ShopIslandUtils.getIslandOwnerAtLocation(e.getTo());
        if (owner == null) {
            // Crossed into a grid cell that has no island
            teleportToIslandHome(player, e.getFrom());
            return;
        }

        int[] center = AranarthUtils.getShopIslandCenters().get(owner);
        if (center != null && !ShopIslandUtils.isWithinPlotBoundary(e.getTo(), center[0], center[1])) {
            teleportToIslandHome(player, e.getTo());
        }
    }

    private boolean isInteractableBlock(Block block) {
        String name = block.getType().name();
        return AranarthUtils.isContainerBlock(block)
                || name.endsWith("_SIGN")
                || name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_GATE")
                || name.endsWith("_BUTTON")
                || block.getType() == Material.NOTE_BLOCK
                || block.getType() == Material.LEVER
                || block.getType() == Material.JUKEBOX
                || block.getType() == Material.HOPPER
                || block.getType() == Material.CRAFTER
                || block.getType() == Material.FLOWER_POT
                || block.getType() == Material.CHISELED_BOOKSHELF
                || block.getType() == Material.DECORATED_POT
                || block.getType() == Material.SMOKER
                || block.getType() == Material.BLAST_FURNACE
                || block.getType() == Material.FURNACE;
    }
}
