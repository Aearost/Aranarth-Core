package com.aearost.aranarthcore.event.mob;

import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sniffer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Digs a circular tunnel in the direction the sniffer rider is looking.
 * Diameter: 7 blocks (radius 3.5). Max depth: 15 blocks.
 * Active only while the rider holds space; cancelled externally when released.
 */
public class SnifferTunnel extends BukkitRunnable {

    private static final double MAX_RADIUS = 3.5;
    private static final double RADIUS_INCREMENT = 0.25;
    private static final double RANGE = 25.0;
    /**
     * Blocks removed per tick. High value keeps the tunnel ahead of even the fastest mount.
     */
    private static final int BLOCKS_PER_TICK = 600;
    /**
     * How long (ms) before TempBlock reverts the tunnel back to original terrain.
     */
    private static final long REVERT_TIME_MS = 200_000L; // ~3 minutes 20 seconds

    private static final Set<Material> NON_DIGGABLE = Set.of(
            // Admin / structural blocks
            Material.BEDROCK,
            Material.END_PORTAL_FRAME,
            Material.END_PORTAL,
            Material.NETHER_PORTAL,
            Material.REINFORCED_DEEPSLATE,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.STRUCTURE_BLOCK,
            Material.BARRIER,
            Material.LIGHT,
            // Ores — preserved so players don't lose resources
            Material.COAL_ORE,
            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private final Map<UUID, SnifferTunnel> activeTunnels;
    private final UUID snifferUUID;
    private final org.bukkit.Location startLocation;
    private final Vector direction;

    private double depth;
    private double radius;
    private double angle;

    public SnifferTunnel(org.bukkit.entity.Player rider, Sniffer sniffer,
                         Map<UUID, SnifferTunnel> activeTunnels) {
        this.activeTunnels = activeTunnels;
        this.snifferUUID = sniffer.getUniqueId();
        this.startLocation = sniffer.getLocation().clone().add(0, 1.0, 0);
        this.direction = rider.getEyeLocation().getDirection().clone().normalize();
        this.depth = 0;
        this.radius = RADIUS_INCREMENT;
        this.angle = 0;
    }

    @Override
    public void run() {
        Entity entity = Bukkit.getEntity(snifferUUID);
        if (!(entity instanceof Sniffer) || entity.isDead()) {
            finish();
            return;
        }

        for (int i = 0; i < BLOCKS_PER_TICK; i++) {
            if (depth > RANGE) {
                finish();
                return;
            }

            Vector ortho = getOrthogonalVector(direction, angle, radius);
            org.bukkit.Location blockLoc = startLocation.clone()
                    .add(direction.clone().multiply(depth))
                    .add(ortho);
            Block block = blockLoc.getBlock();

            if (!block.getType().isAir() && isDiggable(block)) {
                if (TempBlock.isTempBlock(block)) {
                    // Already managed by ProjectKorra — just clear it directly
                    block.setType(Material.AIR, false);
                } else {
                    new TempBlock(block, Material.AIR).setRevertTime(REVERT_TIME_MS);
                }
            }

            // Advance spiral: angle fastest, then radius, then depth
            angle += 20;
            if (angle >= 360) {
                angle = 0;
                radius += RADIUS_INCREMENT;
                if (radius > MAX_RADIUS) {
                    radius = RADIUS_INCREMENT;
                    depth += 0.5;
                }
            }
        }
    }

    private boolean isDiggable(Block block) {
        Material type = block.getType();
        if (NON_DIGGABLE.contains(type)) {
            return false;
        }
        if (block.isLiquid()) {
            return false;
        }
        return type.getHardness() >= 0;
    }

    private void finish() {
        activeTunnels.remove(snifferUUID);
        this.cancel();
    }

    private static Vector getOrthogonalVector(Vector axis, double degrees, double length) {
        Vector ortho;
        if (Math.abs(axis.getX()) < 0.001 && Math.abs(axis.getZ()) < 0.001) {
            ortho = new Vector(1, 0, 0);
        } else {
            ortho = new Vector(axis.getY(), -axis.getX(), 0).normalize();
        }
        ortho = ortho.multiply(length);
        return rotateVectorAroundVector(axis, ortho, degrees);
    }

    /**
     * Rodrigues' rotation formula.
     */
    private static Vector rotateVectorAroundVector(Vector axis, Vector v, double degrees) {
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double ax = axis.getX(), ay = axis.getY(), az = axis.getZ();
        double vx = v.getX(), vy = v.getY(), vz = v.getZ();

        double rx = vx * (cos + ax * ax * (1 - cos))
                + vy * (ax * ay * (1 - cos) - az * sin)
                + vz * (ax * az * (1 - cos) + ay * sin);
        double ry = vx * (ay * ax * (1 - cos) + az * sin)
                + vy * (cos + ay * ay * (1 - cos))
                + vz * (ay * az * (1 - cos) - ax * sin);
        double rz = vx * (az * ax * (1 - cos) - ay * sin)
                + vy * (az * ay * (1 - cos) + ax * sin)
                + vz * (cos + az * az * (1 - cos));
        return new Vector(rx, ry, rz);
    }
}
