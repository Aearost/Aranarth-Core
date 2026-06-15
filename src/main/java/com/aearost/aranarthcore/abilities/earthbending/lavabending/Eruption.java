package com.aearost.aranarthcore.abilities.earthbending.lavabending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class Eruption extends LavaAbility implements AddonAbility {

    public enum Phase {SOURCING, ERUPTING, DESCENDING}

    private static final int SOURCE_LOOK_RANGE = 15;
    private static final double LAVA_RADIUS = 2.0;
    private static final double ANIM_RADIUS = 4.0;
    private static final int MAX_GEYSERS = 3;
    private static final long CLICK_WINDOW = 1000L;
    private static final long WARMUP_DURATION = 1500L;
    private static final long PEAK_HOLD_DURATION = 1000L;
    private static final int MIN_GEYSER_HEIGHT = 8;
    private static final int MAX_GEYSER_HEIGHT = 15;
    private static final long RISE_TICKS_PER_BLOCK = 1L;
    private static final int DESCEND_LAYERS_PER_TICK = 1;
    private static final double BASE_KNOCKUP = 1.2;
    private static final double MAX_KNOCKUP = 2.0;

    private static final Particle.DustOptions SOURCE_OUTLINE_DUST = new Particle.DustOptions(Color.fromRGB(255, 90, 10), 1.0f);
    private static final Particle.DustOptions SOURCE_FILL_DUST = new Particle.DustOptions(Color.fromRGB(200, 40, 0), 0.9f);

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private Phase phase;
    private Location targetLocation;
    private long firstClickTime;
    private final List<Geyser> geysers = new ArrayList<>();
    private final Map<Block, BlockData> trueOriginals = new HashMap<>();
    private long eruptingTickCount;
    private final Set<UUID> hitEntities = new HashSet<>();
    private final Set<UUID> launchedEntities = new HashSet<>();
    private final Random random = new Random();

    private static final Map<UUID, Eruption> activeInstances = new HashMap<>();

    private static long encodeOffset(int dx, int dz) {
        return (long) (dx + 32) * 100L + (dz + 32);
    }

    private static class Geyser {
        final Location base;
        final int maxHeight;
        final long peakHoldMs;
        final long clickTime;
        final Set<Long> footprint;
        final Set<Block> alteredBlocks = new LinkedHashSet<>();
        final List<List<Block>> columnLayers = new ArrayList<>();
        int warmupStage = -1;
        int currentColumnHeight;
        boolean peaked;
        long peakStartTime;

        Geyser(Location base, int maxHeight, long clickTime, Random random) {
            this.base = base;
            this.maxHeight = maxHeight;
            this.clickTime = clickTime;
            this.peakHoldMs = PEAK_HOLD_DURATION + ((MAX_GEYSER_HEIGHT - maxHeight) * 70L);
            this.footprint = buildFootprint(random);
        }

        private Set<Long> buildFootprint(Random random) {
            Set<Long> fp = new HashSet<>();
            int r = (int) Math.ceil(LAVA_RADIUS + 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist <= LAVA_RADIUS - 0.6) {
                        fp.add(encodeOffset(dx, dz));
                    } else if (dist <= LAVA_RADIUS + 0.5) {
                        if (random.nextFloat() < 0.65f) {
                            fp.add(encodeOffset(dx, dz));
                        }
                    }
                }
            }
            return fp;
        }
    }

    public Eruption(Player player) {
        super(player);
        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 12000;
        range = 12.0;
        damage = 6.0;

        targetLocation = findTargetLocation();
        firstClickTime = 0;
        phase = Phase.SOURCING;

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (!player.isOnline() || player.isDead()) {
            cancelInstantly();
            return;
        }
        switch (phase) {
            case SOURCING -> progressSourcing();
            case ERUPTING -> progressErupting();
            case DESCENDING -> progressDescending();
        }
    }

    private void progressSourcing() {
        // Before any click, the player must hold sneak to keep the ability alive
        if (firstClickTime == 0 && !player.isSneaking()) {
            cancelInstantly();
            return;
        }

        long now = System.currentTimeMillis();

        // While no click yet, keep the target up to date and show the AOE preview
        if (firstClickTime == 0) {
            Location updated = findTargetLocation();
            if (updated != null) {
                targetLocation = updated;
            }
            spawnSourcingParticles();
            return;
        }

        // Show preview for where the next geyser would land
        if (now - firstClickTime < CLICK_WINDOW) {
            Location updated = findTargetLocation();
            if (updated != null) {
                targetLocation = updated;
            }
            if (geysers.size() < MAX_GEYSERS) {
                spawnSourcingParticles();
            }
        }

        // Advance each geyser's individual warm-up animation
        for (Geyser geyser : geysers) {
            int targetStage = Math.min(3, (int) ((now - geyser.clickTime) * 4 / WARMUP_DURATION));
            if (targetStage > geyser.warmupStage) {
                geyser.warmupStage = targetStage;
                applyWarmupBlockStage(geyser);
                playWarmupStageSound(geyser);
            }
        }

        // Transition to ERUPTING once the full warm-up has elapsed from the first click
        if (now - firstClickTime >= WARMUP_DURATION) {
            beginErupting();
        }
    }

    private void progressErupting() {
        eruptingTickCount++;

        for (Geyser geyser : geysers) {
            if (geyser.peaked) {
                continue;
            }
            if (eruptingTickCount % RISE_TICKS_PER_BLOCK == 0 && geyser.currentColumnHeight < geyser.maxHeight) {
                // Scan entities before placing lava so velocity is applied before physics takes effect
                applyKnockupAndDamage(geyser);
                extendColumn(geyser);
            }
            if (geyser.currentColumnHeight >= geyser.maxHeight) {
                geyser.peaked = true;
                geyser.peakStartTime = System.currentTimeMillis();
            }
        }

        spawnGeyserParticles();

        boolean allReady = geysers.stream().allMatch(g ->
                g.peaked && System.currentTimeMillis() - g.peakStartTime >= g.peakHoldMs);
        if (allReady) {
            phase = Phase.DESCENDING;
        }
    }

    private void progressDescending() {
        boolean anyRemaining = false;
        for (Geyser geyser : geysers) {
            if (geyser.columnLayers.isEmpty()) {
                continue;
            }
            for (int i = 0; i < DESCEND_LAYERS_PER_TICK && !geyser.columnLayers.isEmpty(); i++) {
                List<Block> topLayer = geyser.columnLayers.remove(geyser.columnLayers.size() - 1);
                for (Block block : topLayer) {
                    restoreBlock(block);
                }
            }
            if (!geyser.columnLayers.isEmpty()) {
                anyRemaining = true;
            }
        }

        if (!anyRemaining) {
            finishAbility();
        }
    }

    /**
     * Finalises all warm-up blocks into their eruption state and transitions to ERUPTING.
     */
    private void beginErupting() {
        for (Geyser geyser : geysers) {
            for (Block block : geyser.alteredBlocks) {
                int ring = ringOf(block, geyser);
                Material mat = switch (ring) {
                    case 0 -> Material.LAVA;
                    case 1 -> Material.MAGMA_BLOCK;
                    case 2 -> Material.NETHERRACK;
                    case 3 -> Material.GRANITE;
                    default -> Material.STONE;
                };
                block.setType(mat, false);
            }
            Location soundLoc = geyser.base.clone().add(0.5, 0.5, 0.5);
            soundLoc.getWorld().playSound(soundLoc, Sound.BLOCK_LAVA_AMBIENT, 3.0f, 0.3f);
            soundLoc.getWorld().playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.4f);
            soundLoc.getWorld().playSound(soundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.35f);
        }

        phase = Phase.ERUPTING;
        eruptingTickCount = 0;
    }

    /**
     * Extends a geyser's lava column by one layer using its pre-built footprint, giving
     * each geyser a unique organic shape rather than an identical circle.
     */
    private void extendColumn(Geyser geyser) {
        int y = geyser.currentColumnHeight;
        geyser.currentColumnHeight++;

        List<Block> layer = new ArrayList<>();
        Block origin = geyser.base.getBlock();
        int r = (int) Math.ceil(LAVA_RADIUS + 1);

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!geyser.footprint.contains(encodeOffset(dx, dz))) {
                    continue;
                }
                Block block = origin.getRelative(dx, y + 1, dz);
                if (!block.getType().isSolid()) {
                    trueOriginals.putIfAbsent(block, block.getBlockData().clone());
                    geyser.alteredBlocks.add(block);
                    block.setType(Material.LAVA, false);
                    layer.add(block);
                }
            }
        }

        if (!layer.isEmpty()) {
            geyser.columnLayers.add(layer);
        }

        if (y == 0) {
            Location soundLoc = geyser.base.clone().add(0.5, 1.0, 0.5);
            soundLoc.getWorld().playSound(soundLoc, Sound.BLOCK_LAVA_AMBIENT, 1.8f, 0.5f);
        }
    }

    private void applyKnockupAndDamage(Geyser geyser) {
        double topY = geyser.base.getY() + geyser.currentColumnHeight + 2.0;
        Location center = geyser.base.clone().add(0.5, 0, 0.5);
        World world = geyser.base.getWorld();

        double t = (geyser.maxHeight - MIN_GEYSER_HEIGHT) / (double) (MAX_GEYSER_HEIGHT - MIN_GEYSER_HEIGHT);
        double knockup = BASE_KNOCKUP + t * (MAX_KNOCKUP - BASE_KNOCKUP);

        for (LivingEntity entity : world.getLivingEntities()) {
            Location eLoc = entity.getLocation();
            if (horizontalDistance(eLoc, center) > LAVA_RADIUS + 1.0) {
                continue;
            }
            if (eLoc.getY() > topY) {
                continue;
            }

            // Damage
            if (!entity.equals(player) && !hitEntities.contains(entity.getUniqueId())) {
                hitEntities.add(entity.getUniqueId());
                DamageHandler.damageEntity(entity, damage, this);
                world.playSound(eLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.7f);
            }

            // Knockup
            if (!launchedEntities.contains(entity.getUniqueId())) {
                launchedEntities.add(entity.getUniqueId());
                entity.setVelocity(new Vector(entity.getVelocity().getX(), knockup, entity.getVelocity().getZ()));
            }
        }
    }

    private void populateAoeBlocks(Geyser geyser) {
        Block center = geyser.base.getBlock();
        int r = (int) Math.ceil(ANIM_RADIUS);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double bx = x + 0.5;
                double bz = z + 0.5;
                double dist = Math.sqrt(bx * bx + bz * bz);
                double threshold = ANIM_RADIUS + (random.nextDouble() * 0.5 - 0.15);
                if (dist > threshold) {
                    continue;
                }
                Block surface = findSurface(center.getRelative(x, 0, z));
                if (surface == null) {
                    continue;
                }
                trueOriginals.putIfAbsent(surface, surface.getBlockData().clone());
                geyser.alteredBlocks.add(surface);
            }
        }
    }

    /**
     * Updates a single geyser's AOE blocks to reflect its current warm-up stage.
     */
    private void applyWarmupBlockStage(Geyser geyser) {
        for (Block block : geyser.alteredBlocks) {
            int ring = ringOf(block, geyser);
            int effectiveStage = geyser.warmupStage - ring;
            if (effectiveStage < 0) {
                block.setType(Material.STONE);
            } else {
                block.setType(AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[
                        Math.min(effectiveStage, AranarthBendingUtils.LAVA_WARMUP_SEQUENCE.length - 1)]);
            }
        }
    }

    /**
     * Traces the player's line of sight to find a valid surface target within range.
     */
    private Location findTargetLocation() {
        Block target = player.getTargetBlock(null, SOURCE_LOOK_RANGE);
        if (target == null || target.getType().isAir()) {
            return null;
        }
        // getTargetBlock treats only air as transparent, so non-solid blocks like short grass
        // can be returned. Walk down to the solid surface beneath them.
        if (!target.getType().isSolid()) {
            Block below = target.getRelative(BlockFace.DOWN);
            while (!below.getType().isSolid() && !below.getType().isAir()) {
                below = below.getRelative(BlockFace.DOWN);
            }
            if (!below.getType().isSolid()) {
                return null;
            }
            target = below;
        }
        if (target.getRelative(BlockFace.UP).getType().isSolid()) {
            return null;
        }
        if (horizontalDistance(target.getLocation().add(0.5, 0, 0.5), player.getLocation()) > range) {
            return null;
        }
        return target.getLocation();
    }

    /**
     * Searches 3 blocks vertically from the given block to locate the topmost solid surface block.
     */
    private Block findSurface(Block start) {
        for (int dy = 3; dy >= -3; dy--) {
            Block candidate = start.getRelative(0, dy, 0);
            if (candidate.getType().isSolid() && !candidate.getRelative(BlockFace.UP).getType().isSolid()) {
                return candidate;
            }
        }
        return null;
    }

    private int ringOf(Block block, Geyser geyser) {
        double bx = block.getX() - geyser.base.getBlockX() + 0.5;
        double bz = block.getZ() - geyser.base.getBlockZ() + 0.5;
        return (int) Math.sqrt(bx * bx + bz * bz);
    }

    private int randomGeyserHeight() {
        return MIN_GEYSER_HEIGHT + random.nextInt(MAX_GEYSER_HEIGHT - MIN_GEYSER_HEIGHT + 1);
    }

    private double horizontalDistance(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void spawnSourcingParticles() {
        if (targetLocation == null) {
            return;
        }
        Location center = targetLocation.clone().add(0.5, 0.08, 0.5);
        World world = center.getWorld();

        for (double a = 0; a < Math.PI * 2; a += Math.PI / 14) {
            Location loc = center.clone().add(Math.cos(a) * ANIM_RADIUS, 0, Math.sin(a) * ANIM_RADIUS);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, SOURCE_OUTLINE_DUST);
        }
        for (int i = 0; i < 4; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double dist = random.nextDouble() * (ANIM_RADIUS - 0.5);
            Location loc = center.clone().add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, SOURCE_FILL_DUST);
        }
    }

    /**
     * Spawns lava ember and flame particles above the top of each active column.
     */
    private void spawnGeyserParticles() {
        for (Geyser geyser : geysers) {
            if (geyser.columnLayers.isEmpty()) {
                continue;
            }
            Block top = geyser.columnLayers.get(geyser.columnLayers.size() - 1).get(0);
            Location loc = top.getLocation().add(0.5, 1.0, 0.5);
            World world = loc.getWorld();

            for (int i = 0; i < 8; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double dr = random.nextDouble() * LAVA_RADIUS * 0.5;
                Location p = loc.clone().add(Math.cos(a) * dr, random.nextDouble() * 0.4, Math.sin(a) * dr);
                world.spawnParticle(Particle.FLAME, p, 1, 0.1, 0.25, 0.1, 0.06);
                world.spawnParticle(Particle.LAVA, p, 1, 0, 0, 0, 0);
            }
        }
    }

    private void playWarmupStageSound(Geyser geyser) {
        Location loc = geyser.base.clone().add(0.5, 0.5, 0.5);
        float pitch = 0.55f + geyser.warmupStage * 0.15f;
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 1.3f, pitch);
        if (geyser.warmupStage == 3) {
            loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 1.2f);
            loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.9f, 0.5f);
        }
    }

    /**
     * Restores a single block to its true pre-eruption state using the shared map.
     */
    private void restoreBlock(Block block) {
        BlockData original = trueOriginals.remove(block);
        if (original != null) {
            block.setBlockData(original, false);
        } else {
            block.setType(Material.AIR, false);
        }
    }

    /**
     * Restores every block touched by any geyser to its true pre-eruption state.
     */
    private void restoreAllBlocks() {
        if (geysers == null) return;
        // First restore column layers (top-to-bottom) for all geysers
        for (Geyser geyser : geysers) {
            for (int i = geyser.columnLayers.size() - 1; i >= 0; i--) {
                for (Block block : geyser.columnLayers.get(i)) {
                    restoreBlock(block);
                }
            }
            geyser.columnLayers.clear();
            geyser.alteredBlocks.clear();
        }
        // Restore any remaining AOE ground blocks not yet restored by the descent phase
        for (Map.Entry<Block, BlockData> entry : new HashMap<>(trueOriginals).entrySet()) {
            entry.getKey().setBlockData(entry.getValue(), false);
        }
        trueOriginals.clear();
    }

    public void onLeftClick() {
        if (phase != Phase.SOURCING || targetLocation == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (firstClickTime > 0 && now - firstClickTime >= CLICK_WINDOW) {
            return;
        }
        if (geysers.size() >= MAX_GEYSERS) {
            return;
        }

        Geyser g = new Geyser(targetLocation, randomGeyserHeight(), now, random);
        populateAoeBlocks(g);
        geysers.add(g);

        if (firstClickTime == 0) {
            firstClickTime = now;
        }
    }

    private void finishAbility() {
        restoreAllBlocks();
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        restoreAllBlocks();
        remove();
    }

    public void endWithCooldown() {
        restoreAllBlocks();
        bPlayer.addCooldown(this);
        remove();
    }

    /**
     * Returns true if lava and fire damage should be suppressed for the given entity.
     */
    public boolean isLavaProtected(LivingEntity entity) {
        if (entity.equals(player)) {
            return true;
        }
        if (phase != Phase.ERUPTING) {
            return false;
        }
        for (Geyser geyser : geysers) {
            if (horizontalDistance(entity.getLocation(), geyser.base.clone().add(0.5, 0, 0.5)) <= LAVA_RADIUS + 1.0) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Eruption getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public static Map<UUID, Eruption> getActiveInstances() {
        return Collections.unmodifiableMap(activeInstances);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public void remove() {
        restoreAllBlocks();
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

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
    public Location getLocation() {
        return targetLocation != null ? targetLocation : player.getLocation();
    }

    @Override
    public String getName() {
        return "Eruption";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        restoreAllBlocks();
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
        return "Heat the lava beneath the surface, and raise it through the ground to ignite up to 3 " +
                "devastating geysers of molten rock into the air. The taller the geyser, the more " +
                "violently caught targets are thrown.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (hold) > Left-click (up to 3 times)");
    }
}
