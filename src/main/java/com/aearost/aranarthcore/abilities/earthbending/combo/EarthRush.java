package com.aearost.aranarthcore.abilities.earthbending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Create up to 3 boulders from the ground and hurl them at your target.
 */
public class EarthRush extends EarthAbility implements AddonAbility, ComboAbility {

    private static final Map<UUID, EarthRush> ACTIVE_INSTANCES = new HashMap<>();
    private static final Map<UUID, Long> SHOCKWAVE_SNEAK_TIME = new HashMap<>();
    private static final Map<UUID, Long> RAISEEARTH_TRANSITION_TIME = new HashMap<>();
    private static final Map<UUID, Long> WINDOW_END_TIME = new HashMap<>();
    private static final Map<UUID, Integer> SHOT_COUNT = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> PENDING_COOLDOWN_TASKS = new HashMap<>();
    private static final Set<UUID> LISTENER_TRIGGERED = new HashSet<>();

    private static final long SNEAK_WINDOW_MS = 5000L;
    private static final long LOADED_TIMEOUT_MS = 8000L;
    private static final long MOVE_INTERVAL_MS = 17L;
    private static final int SOURCE_SEARCH_RANGE = 6;
    private static final double HIT_RADIUS = 1.5;
    private static final long HOLE_LINGER_TICKS = 200L;
    private static final long HOLE_REVERT_PERIOD_TICKS = 2L;
    private static final long WINDOW_DURATION_MS = 5000L;
    private static final long WINDOW_DURATION_TICKS = 100L;
    private static final int MAX_SHOTS = 3;
    private static final int RISING_TICKS_PER_STEP = 2;

    public enum Phase { RISING, LOADED, FLYING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private Phase phase;
    private boolean removed = false;
    private boolean holeScheduled = false;

    private final List<TempBlock> sourceTempBlocks = new ArrayList<>();
    private final List<TempBlock> projectileTempBlocks = new ArrayList<>();
    private BlockData projectileData;

    private Location boulderCenter;
    private Vector direction;
    private int groundY;
    private int srcBx;
    private int srcBz;
    private double traveledDistance = 0.0;
    private long lastMoveTime;
    private long loadedStartTime;
    private int risingTickCounter = 0;
    private final Set<UUID> hitEntities = new HashSet<>();

    public EarthRush(final Player player) {
        super(player);

        this.phase = Phase.LOADED;

        // Only allow creation via our bending listener - not via PK's combo completion path
        if (!LISTENER_TRIGGERED.remove(player.getUniqueId())) {
            return;
        }

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (ACTIVE_INSTANCES.containsKey(player.getUniqueId())) {
            return;
        }

        this.cooldown = 8000L;
        this.damage = 5.0;
        this.range = 20.0;

        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "RaiseEarth");

        if (!formBoulder()) {
            return;
        }

        this.phase = Phase.RISING;

        RAISEEARTH_TRANSITION_TIME.remove(player.getUniqueId());
        SHOCKWAVE_SNEAK_TIME.remove(player.getUniqueId());

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();

        // On the first shot of a window, schedule the window-expiry cooldown task
        if (!PENDING_COOLDOWN_TASKS.containsKey(player.getUniqueId())) {
            final EarthRush self = this;
            final BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    PENDING_COOLDOWN_TASKS.remove(self.player.getUniqueId());
                    WINDOW_END_TIME.remove(self.player.getUniqueId());
                    SHOT_COUNT.remove(self.player.getUniqueId());
                    self.bPlayer.addCooldown(self);
                }
            };
            PENDING_COOLDOWN_TASKS.put(player.getUniqueId(), task);
            task.runTaskLater(AranarthCore.getInstance(), WINDOW_DURATION_TICKS);
        }
    }

    /**
     * Searches in front of the player for a valid 2x2x2 earthbendable area and forms the boulder.
     * @return true if the boulder was successfully formed.
     */
    private boolean formBoulder() {
        final Location feet = player.getLocation();
        final World world = feet.getWorld();
        final Vector horiz = player.getEyeLocation().getDirection().clone().setY(0).normalize();

        // Start at dist of 3 so the boulder forms a comfortable distance ahead of the player
        for (int dist = 3; dist <= SOURCE_SEARCH_RANGE + 2; dist++) {
            final int bx = (int) Math.floor(feet.getX() + horiz.getX() * dist) - 1;
            final int bz = (int) Math.floor(feet.getZ() + horiz.getZ() * dist) - 1;
            final Integer by = findGroundY(world, bx, bz, feet.getBlockY() + 3);
            if (by == null) {
                continue;
            }
            if (!isValidSourceArea(world, bx, by, bz)) {
                continue;
            }
            if (GeneralMethods.isRegionProtectedFromBuild(this,
                    new Location(world, bx + 1.0, by, bz + 1.0))) {
                continue;
            }

            // Convert gravity-affected materials to their stable equivalents
            projectileData = selectMaterial(world.getBlockAt(bx, by, bz).getType()).createBlockData();

            // Create source hole
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    sourceTempBlocks.add(new TempBlock(world.getBlockAt(bx + dx, by, bz + dz),
                            Material.AIR.createBlockData()));
                    sourceTempBlocks.add(new TempBlock(world.getBlockAt(bx + dx, by - 1, bz + dz),
                            Material.AIR.createBlockData()));
                }
            }

            // Projectile blocks are placed during progressRising()
            groundY = by;
            srcBx = bx;
            srcBz = bz;
            // Center Y starts at the bottom of the rising animation
            boulderCenter = new Location(world, bx + 1.0, by - 0.5, bz + 1.0);

            // Rising sounds
            final Location soundLoc = new Location(world, bx + 1.0, by, bz + 1.0);
            world.playSound(soundLoc, Sound.ENTITY_GHAST_SHOOT,    0.9f, 0.55f);
            world.playSound(soundLoc, Sound.BLOCK_GRAVEL_BREAK,    1.0f, 0.6f);
            world.playSound(soundLoc, Sound.BLOCK_STONE_BREAK,     0.7f, 0.5f);

            return true;
        }
        return false;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        switch (phase) {
            case RISING -> progressRising();
            case LOADED -> progressLoaded();
            case FLYING -> progressFlying();
        }
    }

    private void progressRising() {
        risingTickCounter++;

        // Determine which Y-step we are on
        final int step = risingTickCounter / RISING_TICKS_PER_STEP;
        final int currentBottomY = groundY - 1 + step;

        clearProjectile();

        if (currentBottomY > groundY + 1) {
            // Animation complete
            placeProjectileAt(groundY + 1);
            boulderCenter.setY(groundY + 1.5);
            phase = Phase.LOADED;
            loadedStartTime = System.currentTimeMillis();
            return;
        }

        placeProjectileAt(currentBottomY);
        boulderCenter.setY(currentBottomY + 0.5);

        // Rumble particles during the rise
        boulderCenter.getWorld().spawnParticle(
                Particle.BLOCK, boulderCenter, 5, 0.5, 0.3, 0.5, 0.0, projectileData);
    }

    /**
     * Places the 2x2x2 projectile with its bottom layer at {@code bottomY}.
     */
    private void placeProjectileAt(final int bottomY) {
        final World world = boulderCenter.getWorld();
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int py = bottomY; py <= bottomY + 1; py++) {
                    projectileTempBlocks.add(new TempBlock(
                            world.getBlockAt(srcBx + dx, py, srcBz + dz), projectileData));
                }
            }
        }
    }

    private void progressLoaded() {
        if (System.currentTimeMillis() - loadedStartTime > LOADED_TIMEOUT_MS) {
            cancelInstantly();
        }
    }

    private void progressFlying() {
        final long now = System.currentTimeMillis();
        if (now - lastMoveTime < MOVE_INTERVAL_MS) {
            return;
        }
        lastMoveTime = now;

        traveledDistance += 1.0;
        if (traveledDistance > range) {
            impact(false);
            return;
        }

        clearProjectile();

        // Advance center in horizontal direction (Y determined by terrain)
        boulderCenter.add(direction.getX(), 0.0, direction.getZ());

        final int bx = (int) Math.floor(boulderCenter.getX() - 1.0);
        final int bz = (int) Math.floor(boulderCenter.getZ() - 1.0);
        final World world = boulderCenter.getWorld();

        // Skip ground detection when flying over the source hole
        final boolean overSourceHole = bx <= srcBx + 1 && bx + 1 >= srcBx
                && bz <= srcBz + 1 && bz + 1 >= srcBz;
        if (!overSourceHole) {
            final Integer newBy = findGroundY(world, bx, bz, groundY + 4);
            if (newBy == null) {
                impact(false);
                return;
            }
            groundY = newBy;
        }
        boulderCenter.setY(groundY + 1.5);

        if (GeneralMethods.isRegionProtectedFromBuild(this,
                new Location(world, bx + 1.0, groundY + 1, bz + 1.0))) {
            impact(false);
            return;
        }

        // Entity collision - damage each entity once, push continuously while in range
        for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(boulderCenter, HIT_RADIUS)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            // Continuously push the entity along the travel direction with slight lift
            entity.setVelocity(direction.clone().multiply(1.2).add(new Vector(0, 0.25, 0)));
            if (!hitEntities.contains(entity.getUniqueId())) {
                hitEntities.add(entity.getUniqueId());
                DamageHandler.damageEntity(entity, damage, this);
            }
        }

        // Place projectile at new position
        boolean anyPlaced = false;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int py = groundY + 1; py <= groundY + 2; py++) {
                    final Block b = world.getBlockAt(bx + dx, py, bz + dz);
                    if (ElementalAbility.isAir(b.getType()) || TempBlock.isTempBlock(b)) {
                        projectileTempBlocks.add(new TempBlock(b, projectileData));
                        anyPlaced = true;
                    } else if (!b.isPassable()) {
                        impact(false);
                        return;
                    }
                }
            }
        }

        if (!anyPlaced) {
            impact(false);
            return;
        }

        world.spawnParticle(Particle.BLOCK, boulderCenter, 6, 0.6, 0.5, 0.6, 0.05, projectileData);
    }

    /**
     * Fires the loaded boulder in the player's current horizontal look direction.
     * Called by the bending listener when the player left-clicks with RaiseEarth bound.
     *
     * <p>Up to {@value #MAX_SHOTS} boulders can be fired within the 5-second window that
     * begins when the combo sequence completes. The cooldown is applied after the final
     * shot or when the window expires, whichever comes first.</p>
     */
    public void fire() {
        if (phase != Phase.LOADED) {
            return;
        }
        direction = player.getEyeLocation().getDirection().clone().setY(0).normalize();
        lastMoveTime = System.currentTimeMillis();
        phase = Phase.FLYING;

        // Remove from active map so the next shot can be created
        ACTIVE_INSTANCES.remove(player.getUniqueId());

        boulderCenter.getWorld().playSound(boulderCenter, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.8f);

        AranarthBendingUtils.suppressComboTrigger(bPlayer, player, "RaiseEarth");

        final int shots = SHOT_COUNT.merge(player.getUniqueId(), 1, Integer::sum);
        final Long windowEnd = WINDOW_END_TIME.get(player.getUniqueId());
        final boolean windowExpired = windowEnd == null || System.currentTimeMillis() >= windowEnd;

        if (shots >= MAX_SHOTS || windowExpired) {
            // Final shot or window expired - apply cooldown immediately
            final BukkitRunnable pending = PENDING_COOLDOWN_TASKS.remove(player.getUniqueId());
            if (pending != null) {
                try { pending.cancel(); } catch (final IllegalStateException ignored) {}
            }
            WINDOW_END_TIME.remove(player.getUniqueId());
            SHOT_COUNT.remove(player.getUniqueId());
            bPlayer.addCooldown(this);
        }
        // else: window still active, player may create and fire another shot
    }

    /**
     * Ends the ability on impact: clears the projectile and schedules the source hole to revert.
     * Cooldown is managed by {@link #fire()} and the deferred task, not here.
     *
     * @param hit true if an entity was struck.
     */
    private void impact(final boolean hit) {
        clearProjectile();
        holeScheduled = true;
        scheduleHoleRevert();
        removed = true;
        super.remove();
        ACTIVE_INSTANCES.remove(player.getUniqueId(), this);
    }

    /**
     * Waits {@value #HOLE_LINGER_TICKS} ticks then reverts the source hole one block at a time.
     */
    private void scheduleHoleRevert() {
        final List<TempBlock> toRevert = new ArrayList<>(sourceTempBlocks);
        sourceTempBlocks.clear();
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= toRevert.size()) {
                    cancel();
                    return;
                }
                toRevert.get(index).revertBlock();
                index++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), HOLE_LINGER_TICKS, HOLE_REVERT_PERIOD_TICKS);
    }

    private void clearProjectile() {
        for (final TempBlock tb : projectileTempBlocks) {
            tb.revertBlock();
        }
        projectileTempBlocks.clear();
    }

    /**
     * Cancels without applying the cooldown (e.g. combo interrupted before firing).
     */
    public void cancelInstantly() {
        remove();
    }

    /**
     * Removes the ability; cooldown is managed by {@link #fire()} and the deferred task.
     */
    public void endWithCooldown() {
        remove();
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;
        super.remove();
        clearProjectile();
        if (!holeScheduled) {
            for (final TempBlock tb : sourceTempBlocks) {
                tb.revertBlock();
            }
            sourceTempBlocks.clear();
        }
        ACTIVE_INSTANCES.remove(player.getUniqueId(), this);
    }

    /**
     * Finds the highest solid ground block at the given (bx, bz) column, searching
     * downward from startY.
     *
     * @return the Y of the surface block, or null if none found.
     */
    private Integer findGroundY(final World world, final int bx, final int bz, final int startY) {
        for (int y = startY; y >= startY - 15; y--) {
            final Block here = world.getBlockAt(bx, y, bz);
            final Block above = world.getBlockAt(bx, y + 1, bz);
            if (!here.isPassable() && !ElementalAbility.isAir(here.getType())
                    && (ElementalAbility.isAir(above.getType()) || above.isPassable())) {
                return y;
            }
        }
        return null;
    }

    /**
     * Checks that the 2x2x2 source area (surface + one layer below) is fully earthbendable
     * and that the 2x2x2 space above it is clear for the projectile to form.
     */
    private boolean isValidSourceArea(final World world, final int bx, final int by, final int bz) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (!isValidEarthBlock(world.getBlockAt(bx + dx, by, bz + dz))) {
                    return false;
                }
                if (!isValidEarthBlock(world.getBlockAt(bx + dx, by - 1, bz + dz))) {
                    return false;
                }
                final Block above1 = world.getBlockAt(bx + dx, by + 1, bz + dz);
                if (!ElementalAbility.isAir(above1.getType()) && !above1.isPassable()) {
                    return false;
                }
                final Block above2 = world.getBlockAt(bx + dx, by + 2, bz + dz);
                if (!ElementalAbility.isAir(above2.getType()) && !above2.isPassable()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidEarthBlock(final Block block) {
        return (this.isEarthbendable(block) || this.isSandbendable(block) || this.isMetalbendable(block))
                && !TempBlock.isTempBlock(block);
    }

    /**
     * Converts gravity-affected materials to stable equivalents so the projectile
     * does not fall: SAND -> SANDSTONE, RED_SAND -> RED_SANDSTONE, GRAVEL -> STONE.
     */
    private static Material selectMaterial(final Material mat) {
        if (mat == Material.SAND) return Material.SANDSTONE;
        if (mat == Material.RED_SAND) return Material.RED_SANDSTONE;
        if (mat == Material.GRAVEL) return Material.STONE;
        return mat;
    }

    public static void markListenerTriggered(final UUID uuid) {
        LISTENER_TRIGGERED.add(uuid);
    }

    public static void markShockwaveSneak(final UUID uuid) {
        SHOCKWAVE_SNEAK_TIME.put(uuid, System.currentTimeMillis());
    }

    public static boolean hasRecentShockwaveSneak(final UUID uuid) {
        final Long ts = SHOCKWAVE_SNEAK_TIME.get(uuid);
        if (ts == null) {
            return false;
        }
        if (System.currentTimeMillis() - ts > SNEAK_WINDOW_MS) {
            SHOCKWAVE_SNEAK_TIME.remove(uuid);
            return false;
        }
        return true;
    }

    public static void markRaiseEarthTransition(final UUID uuid) {
        RAISEEARTH_TRANSITION_TIME.put(uuid, System.currentTimeMillis());
        // Reset window state so the constructor schedules a fresh expiry task
        WINDOW_END_TIME.put(uuid, System.currentTimeMillis() + WINDOW_DURATION_MS);
        SHOT_COUNT.put(uuid, 0);
        final BukkitRunnable existing = PENDING_COOLDOWN_TASKS.remove(uuid);
        if (existing != null) {
            try { existing.cancel(); } catch (final IllegalStateException ignored) {}
        }
    }

    public static boolean hasRecentRaiseEarthTransition(final UUID uuid) {
        final Long ts = RAISEEARTH_TRANSITION_TIME.get(uuid);
        if (ts == null) {
            return false;
        }
        if (System.currentTimeMillis() - ts > SNEAK_WINDOW_MS) {
            RAISEEARTH_TRANSITION_TIME.remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Returns true if the player can create a new EarthRush shot. This is true for
     * the first shot (combo transition just performed) or for subsequent shots while
     * the 5-second window is active and fewer than {@value #MAX_SHOTS} shots have fired.
     */
    public static boolean canCreateShot(final UUID uuid) {
        if (hasRecentRaiseEarthTransition(uuid)) {
            return true;
        }
        final Long end = WINDOW_END_TIME.get(uuid);
        if (end == null || System.currentTimeMillis() >= end) {
            return false;
        }
        return SHOT_COUNT.getOrDefault(uuid, 0) < MAX_SHOTS;
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static EarthRush getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
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
        return boulderCenter != null ? boulderCenter : player.getLocation();
    }

    @Override
    public String getName() {
        return "EarthRush";
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        for (final BukkitRunnable task : PENDING_COOLDOWN_TASKS.values()) {
            try { task.cancel(); } catch (final IllegalStateException ignored) {}
        }
        ACTIVE_INSTANCES.clear();
        SHOCKWAVE_SNEAK_TIME.clear();
        RAISEEARTH_TRANSITION_TIME.clear();
        WINDOW_END_TIME.clear();
        SHOT_COUNT.clear();
        PENDING_COOLDOWN_TASKS.clear();
        LISTENER_TRIGGERED.clear();
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
    public Object createNewComboInstance(final Player player) {
        return new EarthRush(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("Shockwave", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("RaiseEarth", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("RaiseEarth", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("RaiseEarth", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public void handleCollision(final Collision collision) {}

    @Override
    public String getDescription() {
        return "Raise a large boulder from the ground in front of you, firing it forward along the ground. "
                + "Fire up to three boulders within the next few seconds before the cooldown applies.\n"
                + ChatUtils.translateToColor("&fUsage: Shockwave (Hold Sneak) > RaiseEarth (Release Sneak) "
                + "> RaiseEarth (Hold Sneak) > RaiseEarth (Left Click) - repeat Tap Sneak + Left Click for additional shots");
    }
}
