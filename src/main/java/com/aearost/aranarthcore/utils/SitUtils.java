package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the logic for players sitting on cushions via /sit or by clicking stairs and slabs.
 */
public class SitUtils {

    // Player UUID -> cushion entity UUID
    private static final Map<UUID, UUID> sittingPlayers = new HashMap<>();

    /**
     * Sits the player down on a cushion at their current location.
     *
     * @param player The player.
     * @return True if the player was sat down.
     */
    public static boolean sitOnGround(Player player) {
        if (!canSit(player)) {
            return false;
        }
        Location location = player.getLocation();
        location.setPitch(0);
        return spawnCushionAndSit(player, location);
    }

    /**
     * Sits the player down on a cushion placed on top of the input stair or slab.
     *
     * @param player The player.
     * @param block The stair or slab block.
     * @return True if the player was sat down.
     */
    public static boolean sitOnBlock(Player player, Block block) {
        if (!isSittableBlock(block) || !canSit(player)) {
            return false;
        }

        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        if (isCushionAt(location)) {
            return false;
        }

        // Face away from the back of the stairs, otherwise keep the player's facing
        if (block.getBlockData() instanceof Stairs stairs) {
            BlockFace facing = stairs.getFacing().getOppositeFace();
            location.setDirection(facing.getDirection());
        } else {
            location.setYaw(player.getLocation().getYaw());
            location.setPitch(0);
        }
        return spawnCushionAndSit(player, location);
    }

    /**
     * Stands the player up and removes their cushion.
     *
     * @param player The player.
     */
    public static void standUp(Player player) {
        UUID cushionUuid = sittingPlayers.remove(player.getUniqueId());
        if (cushionUuid == null) {
            return;
        }
        Entity cushion = Bukkit.getEntity(cushionUuid);
        if (cushion != null) {
            cushion.eject();
            cushion.remove();
        }
    }

    /**
     * Removes the cushion of the player once they have dismounted it.
     *
     * @param player The player.
     * @param cushion The cushion that was dismounted.
     */
    public static void onDismount(Player player, Entity cushion) {
        sittingPlayers.remove(player.getUniqueId());
        cushion.remove();
    }

    /**
     * Updates the color of the player's cushion if they are currently sitting.
     *
     * @param player The player.
     * @param color The new color of the cushion.
     */
    public static void updateCushionColor(Player player, DyeColor color) {
        UUID cushionUuid = sittingPlayers.get(player.getUniqueId());
        if (cushionUuid == null) {
            return;
        }
        if (Bukkit.getEntity(cushionUuid) instanceof Cushion cushion) {
            cushion.setColor(color);
        }
    }

    /**
     * Stands up all sitting players and removes their cushions.
     */
    public static void standUpAll() {
        for (UUID uuid : new HashMap<>(sittingPlayers).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                standUp(player);
            } else {
                Entity cushion = Bukkit.getEntity(sittingPlayers.remove(uuid));
                if (cushion != null) {
                    cushion.remove();
                }
            }
        }
    }

    /**
     * @param player The player.
     * @return True if the player is currently sitting on a cushion.
     */
    public static boolean isSitting(Player player) {
        return sittingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * @param entity The entity.
     * @return True if the entity is a cushion spawned by the sit functionality.
     */
    public static boolean isSitCushion(Entity entity) {
        return entity instanceof Cushion
                && entity.getPersistentDataContainer().has(CustomKeys.SIT_CUSHION, PersistentDataType.BYTE);
    }

    /**
     * @param block The block.
     * @return True if the block is a bottom stair or bottom slab with room above it to sit.
     */
    public static boolean isSittableBlock(Block block) {
        BlockData data = block.getBlockData();
        boolean isBottomHalf;
        if (data instanceof Stairs stairs) {
            isBottomHalf = stairs.getHalf() == Bisected.Half.BOTTOM;
        } else if (data instanceof Slab slab) {
            isBottomHalf = slab.getType() == Slab.Type.BOTTOM;
        } else {
            return false;
        }
        return isBottomHalf && block.getRelative(BlockFace.UP).isPassable();
    }

    /**
     * @param color The name of the DyeColor.
     * @return The matching DyeColor, or white if it is invalid.
     */
    public static DyeColor getCushionColor(String color) {
        try {
            return DyeColor.valueOf(color);
        } catch (IllegalArgumentException | NullPointerException e) {
            return DyeColor.WHITE;
        }
    }

    private static boolean canSit(Player player) {
        return !player.isInsideVehicle()
                && !player.isGliding()
                && !player.isSwimming()
                && !player.isFlying()
                && !player.isSleeping()
                && !player.isDead();
    }

    private static boolean isCushionAt(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        return !world.getNearbyEntities(location, 0.4, 0.4, 0.4, entity -> entity instanceof Cushion).isEmpty();
    }

    private static boolean spawnCushionAndSit(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        DyeColor color = player.hasPermission("aranarth.sit.color")
                ? getCushionColor(aranarthPlayer.getSitCushionColor())
                : DyeColor.WHITE;

        Cushion cushion = world.spawn(location, Cushion.class, c -> {
            c.setColor(color);
            c.setPersistent(false);
            c.setInvulnerable(true);
            c.getPersistentDataContainer().set(CustomKeys.SIT_CUSHION, PersistentDataType.BYTE, (byte) 1);
        });

        if (!cushion.addPassenger(player)) {
            cushion.remove();
            return false;
        }
        sittingPlayers.put(player.getUniqueId(), cushion.getUniqueId());
        return true;
    }

}
