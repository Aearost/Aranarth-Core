package com.aearost.aranarthcore.abilities.waterbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class VineWhip extends PlantAbility implements AddonAbility {

    public enum Phase { SELECTING, EXTENDING, HOLDING, RETRACTING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.SELECT_RANGE)
    private int selectRange;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute("FullExtendDuration")
    private long fullExtendDuration;
    @Attribute("HoldDuration")
    private long holdDuration;

    // Source block tracking
    private Block sourceBlock;
    private BlockData sourceOriginalData;

    // Vine state
    private Phase phase;
    private final List<Block> vineBlocks = new ArrayList<>();
    private final Map<Block, BlockData> originalBlockData = new HashMap<>();

    // Floating-point tip for smooth, gapless sub-stepping
    private Location tipPosition;
    private double distanceTraveled;

    // Timing
    private long extensionStartTime;
    private long holdStartTime;
    private long retractionStartTime;
    private int retractionBlocksRemoved;

    // Entity being pulled along the vine during retraction (null if no pull active)
    private LivingEntity pulledEntity;

    // Static instance registry (one active VineWhip per player)
    private static final Map<UUID, VineWhip> activeInstances = new HashMap<>();

    private final Random random = new Random();

    public VineWhip(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 4000;
        selectRange = 6;
        range = 25;
        damage = 5.0;
        fullExtendDuration = 750;
        holdDuration = 500;

        phase = Phase.SELECTING;
        activeInstances.put(player.getUniqueId(), this);

        start();
    }

    // -------------------------------------------------------------------------
    // PK tick
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        switch (phase) {
            case SELECTING  -> progressSelecting();
            case EXTENDING  -> progressExtending();
            case HOLDING    -> progressHolding();
            case RETRACTING -> progressRetracting();
        }
    }

    // -------------------------------------------------------------------------
    // Phase: SELECTING
    // -------------------------------------------------------------------------

    private void progressSelecting() {
        if (!player.isSneaking()) {
            cancelInstantly();
            return;
        }

        Block found = BlockSource.getWaterSourceBlock(player, selectRange, ClickType.SHIFT_DOWN, false, false, bPlayer.canPlantbend());
        if (found != null && isValidPlantSource(found)) {
            sourceBlock = found;
            // Show particles at the source block as visual feedback
            sourceBlock.getWorld().spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    sourceBlock.getLocation().add(0.5, 1.0, 0.5),
                    2, 0.2, 0.1, 0.2, 0
            );
        } else {
            sourceBlock = null;
        }
    }

    /**
     * Called by the listener when the player left-clicks while VineWhip is active.
     * <ul>
     *   <li>SELECTING: fires the vine if a source is locked in.</li>
     *   <li>EXTENDING / HOLDING: attempts to pull a nearby LivingEntity to the player.</li>
     *   <li>RETRACTING: ignored.</li>
     * </ul>
     */
    public void onLeftClick() {
        if (phase == Phase.SELECTING) {
            if (sourceBlock == null) return;

            // Turn the source block into oak leaves for the duration of the ability
            sourceOriginalData = sourceBlock.getBlockData().clone();
            Leaves leafData = (Leaves) Material.OAK_LEAVES.createBlockData();
            leafData.setPersistent(true);
            sourceBlock.setBlockData(leafData);

            // Tip begins at the center of the source block
            tipPosition = sourceBlock.getLocation().add(0.5, 0.5, 0.5);
            distanceTraveled = 0.0;
            extensionStartTime = System.currentTimeMillis();
            phase = Phase.EXTENDING;

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.5f, 0.65f);

        } else if (phase == Phase.EXTENDING || phase == Phase.HOLDING) {
            pullNearbyEntity();
        }
        // RETRACTING: ignored
    }

    /**
     * Searches every vine block for the nearest LivingEntity within a 3-block radius,
     * making it easy to latch on as long as the vine passes anywhere near the target.
     * No damage is dealt — the entity is pulled toward the player via velocity.
     */
    private void pullNearbyEntity() {
        if (vineBlocks.isEmpty()) return;

        double searchRadius = 5;
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        Set<UUID> checked = new HashSet<>();

        for (Block vineBlock : vineBlocks) {
            Location center = vineBlock.getLocation().add(0.5, 0.5, 0.5);
            for (Entity entity : center.getWorld().getNearbyEntities(center, searchRadius, searchRadius, searchRadius)) {
                if (!(entity instanceof LivingEntity living) || living.equals(player)) continue;
                if (!checked.add(living.getUniqueId())) continue; // skip already-evaluated entities
                double distSq = living.getLocation().distanceSquared(center);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = living;
                }
            }
        }

        if (nearest == null) return;

        pulledEntity = nearest;
        startRetracting();
    }

    // -------------------------------------------------------------------------
    // Phase: EXTENDING
    // -------------------------------------------------------------------------

    private void progressExtending() {
        if (!player.isSneaking()) {
            startRetracting();
            return;
        }

        long elapsed = System.currentTimeMillis() - extensionStartTime;
        double targetDistance = Math.min((double) elapsed / fullExtendDuration * range, range);

        // Sample the player's look direction once per tick so all sub-steps this tick
        // share the same direction — direction only changes at tick boundaries, creating
        // smooth curves as the player moves their mouse between ticks.
        Vector dir = player.getEyeLocation().getDirection().normalize();

        while (distanceTraveled < targetDistance) {
            double step = Math.min(0.2, targetDistance - distanceTraveled);
            tipPosition = tipPosition.clone().add(dir.clone().multiply(step));
            distanceTraveled += step;

            Block block = tipPosition.getBlock();
            Block lastPlaced = vineBlocks.isEmpty() ? sourceBlock : vineBlocks.get(vineBlocks.size() - 1);

            // Skip if we haven't crossed into a new block yet
            if (block.equals(lastPlaced) || block.equals(sourceBlock)) {
                continue;
            }

            // Solid block collision — begin retraction
            if (block.getType().isSolid()) {
                startRetracting();
                return;
            }

            // Entity hit — deal damage and retract
            LivingEntity hit = findNearbyLivingEntity(tipPosition, 2.0);
            if (hit != null) {
                hit.damage(damage, player);
                startRetracting();
                return;
            }

            // Save the block's original state (handles non-solid blocks like flowers, grass, etc.)
            originalBlockData.put(block, block.getBlockData().clone());

            // Place the leaf block
            Leaves leaf = (Leaves) Material.OAK_LEAVES.createBlockData();
            leaf.setPersistent(true);
            block.setBlockData(leaf);
            vineBlocks.add(block);

            // Occasional sounds and particles at the tip
            if (random.nextInt(4) == 0) {
                Sound sound = random.nextBoolean() ? Sound.BLOCK_GRASS_STEP : Sound.BLOCK_GRASS_BREAK;
                block.getWorld().playSound(block.getLocation(), sound, 0.3f, 0.8f + random.nextFloat() * 0.4f);
            }
            block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    block.getLocation().add(0.5, 0.5, 0.5), 1, 0.2, 0.2, 0.2, 0);
        }

        if (distanceTraveled >= range) {
            holdStartTime = System.currentTimeMillis();
            phase = Phase.HOLDING;
        }
    }

    // -------------------------------------------------------------------------
    // Phase: HOLDING
    // -------------------------------------------------------------------------

    private void progressHolding() {
        if (!player.isSneaking()) {
            startRetracting();
            return;
        }
        if (System.currentTimeMillis() - holdStartTime >= holdDuration) {
            startRetracting();
        }
    }

    // -------------------------------------------------------------------------
    // Phase: RETRACTING
    // -------------------------------------------------------------------------

    private void startRetracting() {
        retractionStartTime = System.currentTimeMillis();
        retractionBlocksRemoved = 0;
        phase = Phase.RETRACTING;
    }

    private void progressRetracting() {
        long elapsed = System.currentTimeMillis() - retractionStartTime;
        // Same speed as extension: range blocks over fullExtendDuration ms
        double blocksPerMs = (double) range / fullExtendDuration;
        int targetRemoved = (int) (elapsed * blocksPerMs);

        // Remove blocks from tip toward source
        while (retractionBlocksRemoved < targetRemoved) {
            int tipIndex = vineBlocks.size() - 1 - retractionBlocksRemoved;
            if (tipIndex < 0) break;

            Block block = vineBlocks.get(tipIndex);
            BlockData original = originalBlockData.get(block);
            block.setBlockData(original != null ? original : Material.AIR.createBlockData());

            if (random.nextInt(4) == 0) {
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GRASS_BREAK,
                        0.3f, 0.8f + random.nextFloat() * 0.4f);
            }
            block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    block.getLocation().add(0.5, 0.5, 0.5), 1, 0.15, 0.15, 0.15, 0);

            retractionBlocksRemoved++;
        }

        // Pull the entity along the vine path as it retracts
        if (pulledEntity != null && pulledEntity.isValid()) {
            Location target;
            int currentFrontIndex = vineBlocks.size() - 1 - retractionBlocksRemoved;
            if (currentFrontIndex >= 0) {
                // Chase the retracting vine front - this keeps them on the vine's path
                Block frontBlock = vineBlocks.get(currentFrontIndex);
                target = frontBlock.getLocation().add(0.5, 0.5, 0.5);
            } else {
                // All blocks removed - aim just in front of the player
                target = player.getLocation().add(
                        player.getLocation().getDirection().setY(0).normalize().multiply(1.5));
            }

            Vector toTarget = target.toVector().subtract(pulledEntity.getLocation().toVector());
            double distance = toTarget.length();
            if (distance > 0.3) {
                double speed = Math.min(0.5 + distance * 0.1, 1.6);
                Vector pull = toTarget.normalize().multiply(speed);
                // Tiny constant to counteract gravity - no aggressive lift
                pull.add(new Vector(0, 0.08, 0));
                pulledEntity.setVelocity(pull);
            }
        }

        if (retractionBlocksRemoved >= vineBlocks.size()) {
            finishAbility();
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup helpers
    // -------------------------------------------------------------------------

    /**
     * Restores all placed blocks and the source block, and unregisters this instance.
     * Safe to call multiple times (idempotent after the first call).
     */
    private void cleanupBlocks() {
        for (Map.Entry<Block, BlockData> entry : originalBlockData.entrySet()) {
            entry.getKey().setBlockData(entry.getValue());
        }
        vineBlocks.clear();
        originalBlockData.clear();

        if (sourceBlock != null && sourceOriginalData != null) {
            sourceBlock.setBlockData(sourceOriginalData);
            sourceOriginalData = null;
        }

        activeInstances.remove(player.getUniqueId());
        pulledEntity = null;
    }

    /**
     * Ends the ability with the retraction animation complete and applies the cooldown.
     */
    private void finishAbility() {
        cleanupBlocks();
        bPlayer.addCooldown(this);
        remove();
    }

    /**
     * Immediately removes all placed blocks without the retraction animation and ends
     * the ability without a cooldown. Used when the player releases sneak or changes slots.
     */
    public void cancelInstantly() {
        cleanupBlocks();
        remove();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

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

    /**
     * Returns the nearest LivingEntity (excluding the caster) within the given radius,
     * or null if none are found. Using entity.damage(amount, player) fires the standard
     * EntityDamageByEntityEvent, ensuring armour, damage resistance, and all other
     * entity-damage listeners are respected correctly.
     */
    private LivingEntity findNearbyLivingEntity(Location loc, double radius) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && !target.equals(player)) {
                return target;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static VineWhip getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    // -------------------------------------------------------------------------
    // PK ability interface
    // -------------------------------------------------------------------------

    @Override
    public boolean isSneakAbility() {
        return true;
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
        return "VineWhip";
    }

    @Override
    public Location getLocation() {
        if (tipPosition != null) return tipPosition;
        if (sourceBlock != null) return sourceBlock.getLocation();
        return player.getLocation();
    }

    @Override
    public void load() {}

    /**
     * Called on plugin shutdown. Restores all blocks without going through remove()
     * since PK handles ability unregistration itself.
     */
    @Override
    public void stop() {
        if (originalBlockData != null) cleanupBlocks();
    }

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
        return "Latch onto a nearby plant source, and launch a vine of leaves toward your target." +
                "The vine will curve with your gaze as it extends, and snaps back" +
                "to its source once it reaches its limit or strikes a foe." +
                "You can also left-click before your vine hits a target, pulling the caught mob towards you.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (plant source) > Left-click");
    }
}
