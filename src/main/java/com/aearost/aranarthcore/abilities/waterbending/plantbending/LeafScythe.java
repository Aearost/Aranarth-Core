package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class LeafScythe extends PlantAbility implements AddonAbility {

    public enum Phase { SOURCED, SWINGING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.SELECT_RANGE)
    private int selectRange;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    private Block sourceBlock;
    private Phase phase;
    private long sourcedStartTime;
    private Vector prevDirection;
    private double prevYawDelta;
    private int slashTick;
    private int noMovementTicks;

    private final Set<UUID> hitEntities = new HashSet<>();

    private static final Particle.DustOptions SCYTHE_DUST =
            new Particle.DustOptions(Color.fromRGB(0, 90, 10), 0.6f);
    private static final long SOURCED_TIMEOUT_MS = 5000;
    private static final int SLASH_TICKS = 20;
    private static final int RENDER_SUB_STEPS = 8;
    private static final int HIT_SUB_STEPS = 6;
    private static final double YAW_THRESHOLD = 0.01;
    private static final int NO_MOVEMENT_TICKS = 3;
    private static final double HIT_RADIUS = 1.5;
    private static final double PARTICLE_SPACING = 0.4;

    /**
     * Full rendering path from the player's hand outward.
     */
    private static final int[][] SCYTHE_PATH = {
        {0, 0},
        {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0},
        {5, 1}, {5, 2}, {4, 3}, {4, 4}, {3, 4}
    };

    /**
     * Hit-detection points, same as the path minus the hand origin (index 0).
     */
    private static final int[][] SCYTHE_SHAPE = {
        {1, 0}, {2, 0}, {3, 0}, {4, 0}, {5, 0},
        {5, 1}, {5, 2}, {4, 3}, {4, 4}, {3, 4}
    };

    private static final Map<UUID, LeafScythe> activeInstances = new HashMap<>();

    public LeafScythe(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 6000;
        selectRange = 8;
        damage = 4.0; // 2 hearts (blade hits deal 1.5× — 3 hearts)

        Block found = BlockSource.getWaterSourceBlock(player, selectRange, ClickType.LEFT_CLICK, false, false, bPlayer.canPlantbend());
        if (found == null || !isValidPlantSource(found)) {
            return;
        }

        sourceBlock = found;
        sourceBlock.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                sourceBlock.getLocation().add(0.5, 0.5, 0.5),
                16, 0.35, 0.35, 0.35, 0);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.8f, 0.6f);

        phase = Phase.SOURCED;
        sourcedStartTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            cancelInstantly();
            return;
        }
        switch (phase) {
            case SOURCED  -> progressSourced();
            case SWINGING -> progressSwinging();
        }
    }

    private void progressSourced() {
        if (System.currentTimeMillis() - sourcedStartTime > SOURCED_TIMEOUT_MS) {
            cancelInstantly();
            return;
        }
        if (player.isSneaking()) {
            sourceBlock.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    sourceBlock.getLocation().add(0.5, 0.5, 0.5),
                    1, 0.15, 0.1, 0.15, 0);
        }
    }

    private void progressSwinging() {
        slashTick++;

        Vector curDirection = player.getEyeLocation().getDirection().normalize();

        double yawDelta = prevDirection.getX() * curDirection.getZ()
                        - prevDirection.getZ() * curDirection.getX();
        if (prevYawDelta != 0.0 && Math.abs(yawDelta) > YAW_THRESHOLD
                && Math.signum(yawDelta) != Math.signum(prevYawDelta)) {
            finishAbility();
            return;
        }
        if (Math.abs(yawDelta) > YAW_THRESHOLD) {
            prevYawDelta = yawDelta;
            noMovementTicks = 0;
        } else {
            noMovementTicks++;
            if (noMovementTicks >= NO_MOVEMENT_TICKS) {
                finishAbility();
                return;
            }
        }

        // Render at multiple interpolated positions between the previous and current direction
        for (int s = 1; s <= RENDER_SUB_STEPS; s++) {
            double t = (double) s / RENDER_SUB_STEPS;
            Vector dir = prevDirection.clone()
                    .add(curDirection.clone().subtract(prevDirection).multiply(t))
                    .normalize();
            renderFullScythe(dir);
        }

        // Interpolated hit detection to catch entities swept through between ticks
        for (int s = 1; s <= HIT_SUB_STEPS; s++) {
            double t = (double) s / HIT_SUB_STEPS;
            Vector dir = prevDirection.clone()
                    .add(curDirection.clone().subtract(prevDirection).multiply(t))
                    .normalize();
            checkEntityHits(dir);
        }

        prevDirection = curDirection;

        if (slashTick >= SLASH_TICKS) {
            finishAbility();
        }
    }

    public void onLeftClick() {
        if (phase != Phase.SOURCED || !player.isSneaking()) {
            return;
        }

        sourceBlock.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                sourceBlock.getLocation().add(0.5, 0.5, 0.5),
                16, 0.35, 0.35, 0.35, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.8f, 0.55f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.0f, 0.6f);

        prevDirection = player.getEyeLocation().getDirection().normalize();
        prevYawDelta = 0.0;
        slashTick = 0;
        noMovementTicks = 0;
        hitEntities.clear();

        phase = Phase.SWINGING;
    }

    private void renderFullScythe(Vector handleDir) {
        Location origin = player.getEyeLocation().add(0, -0.5, 0);
        World world = player.getWorld();
        Vector bladeDir = computeBladeDir(handleDir, prevYawDelta);

        Vector[] positions = new Vector[SCYTHE_PATH.length];
        for (int i = 0; i < SCYTHE_PATH.length; i++) {
            positions[i] = origin.toVector()
                    .add(handleDir.clone().multiply(SCYTHE_PATH[i][0]))
                    .add(bladeDir.clone().multiply(SCYTHE_PATH[i][1]));
        }

        for (int i = 0; i < positions.length - 1; i++) {
            Vector from = positions[i];
            Vector to = positions[i + 1];
            double segLen = from.distance(to);
            int steps = Math.max(1, (int) Math.ceil(segLen / PARTICLE_SPACING));
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                Vector pos = from.clone().multiply(1 - t).add(to.clone().multiply(t));
                world.spawnParticle(Particle.DUST, pos.getX(), pos.getY(), pos.getZ(),
                        1, 0, 0, 0, 0, SCYTHE_DUST);
            }
        }
    }

    private void checkEntityHits(Vector handleDir) {
        Location origin = player.getEyeLocation().add(0, -0.5, 0);
        World world = player.getWorld();
        Vector bladeDir = computeBladeDir(handleDir, prevYawDelta);

        for (int[] offset : SCYTHE_SHAPE) {
            Vector pos = origin.toVector()
                    .add(handleDir.clone().multiply(offset[0]))
                    .add(bladeDir.clone().multiply(offset[1]));
            Location center = pos.toLocation(world);
            boolean isBlade = offset[1] > 0;
            for (Entity entity : world.getNearbyEntities(center, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                    continue;
                }
                if (hitEntities.add(living.getUniqueId())) {
                    living.damage(isBlade ? damage * 1.5 : damage, player);
                }
            }
        }
    }

    /**
     * Returns the blade direction perpendicular to the horizontal plane.
     */
    private static Vector computeBladeDir(Vector handleDir, double yawDelta) {
        double sign = yawDelta >= 0 ? 1.0 : -1.0;
        Vector blade = handleDir.clone().crossProduct(new Vector(0, sign, 0));
        return blade.lengthSquared() > 1e-6 ? blade.normalize() : new Vector(1, 0, 0);
    }

    private boolean isValidPlantSource(Block block) {
        Material mat = block.getType();
        return mat != Material.WATER
                && mat != Material.ICE
                && mat != Material.PACKED_ICE
                && mat != Material.BLUE_ICE
                && mat != Material.FROSTED_ICE
                && mat != Material.SNOW
                && mat != Material.SNOW_BLOCK;
    }

    private void finishAbility() {
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        remove();
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static LeafScythe getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public void remove() {
        activeInstances.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public String getName() {
        return "LeafScythe";
    }

    @Override
    public Location getLocation() {
        if (sourceBlock != null) return sourceBlock.getLocation();
        return player.getLocation();
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Create a scythe of leaves, allowing you to sweep all targets, dealing more damage when hit by the pointed tip.\n" +
                ChatUtils.translateToColor("&fUsage: Left-click (plant source) > Hold Sneak > Left-click (and drag horizontally)");
    }
}
