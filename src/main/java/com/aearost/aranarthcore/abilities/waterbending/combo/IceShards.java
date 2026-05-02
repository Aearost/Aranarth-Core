package com.aearost.aranarthcore.abilities.waterbending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Weather;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.event.BendingReloadEvent;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class IceShards extends IceAbility implements AddonAbility, ComboAbility {

    private static final Map<UUID, IceShards> ACTIVE_INSTANCES = new HashMap<>();
    private static Listener listener;

    private enum Phase { CHARGING, FIRING }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;

    private final int domeRadius = 10;
    private final long chargeTime = 6000L;
    private final long chargeHoldGrace = 5000L;
    private final double projectileSpeed = 2.5;
    private final long projectileMaxLifetime = 5000L;

    private Phase phase;
    private long chargeStartTime;
    private long fireTime;
    private int heldSlot = -1;

    private final List<Vector> domeTargets = new ArrayList<>();
    private final List<TempBlock> domeBlocks = new ArrayList<>();
    private int domeTargetIndex = 0;
    private int freezeIndex = 0;
    private boolean chargeComplete = false;
    private long lastWaterSoundTime = -600L;
    private final List<Shard> shards = new ArrayList<>();
    private final Set<UUID> hitEntities = new HashSet<>();

    // -----------------------------------------------------------------------
    // Shard — wraps a BlockDisplay with a manually-tracked velocity.
    // BlockDisplay entities are purely visual; they can never trigger
    // EntityChangeBlockEvent or place any block in the world.
    // -----------------------------------------------------------------------

    private static final class Shard {
        final BlockDisplay display;
        Vector velocity;

        Shard(final BlockDisplay display, final Vector velocity) {
            this.display = display;
            this.velocity = velocity.clone();
        }
    }

    public IceShards(final Player player) {
        super(player);

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (ACTIVE_INSTANCES.containsKey(player.getUniqueId())) {
            return;
        }

        String worldName = player.getWorld().getName().toLowerCase();
        if (worldName.endsWith("_nether") || worldName.endsWith("_the_end")) {
            player.sendMessage(ChatUtils.chatMessage("&7You can only use " + Element.ICE.getColor() + this.getName() + " &7in the Overworld!"));
            return;
        }

        final Weather weather = AranarthUtils.getWeather();
        if (weather != Weather.RAIN && weather != Weather.THUNDER) {
            player.sendMessage(ChatUtils.chatMessage("&7You can only use " + Element.ICE.getColor() + this.getName() + " &7while it is raining!"));
            return;
        }

        // Must be standing on solid ground — not mid-air and not in water.
        final Block feetBlock = player.getLocation().getBlock();
        final Block blockBelow = player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        if (feetBlock.getType() == Material.WATER || blockBelow.isPassable()) {
            return;
        }

        this.cooldown = 14000L;
        this.damage = 16.0;
        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.heldSlot = player.getInventory().getHeldItemSlot();

        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "WaterBubble");
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "PhaseChange");
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "IceSpike");

        this.generateDomeTargets();
        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    // -----------------------------------------------------------------------
    // Water displacement
    // -----------------------------------------------------------------------

    /**
     * Scans the interior of the dome sphere every tick and removes any water block
     * that is not a registered dome block, preventing external water from entering
     * via delayed fluid ticks that bypass event cancellation.
     */
    private void cleanupInteriorWater(final Location centre) {
        final int r = this.domeRadius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > r * r) continue;
                    final Block block = centre.clone().add(x, y, z).getBlock();
                    if (block.getType() != Material.WATER) continue;
                    if (TempBlock.isTempBlock(block)) continue;
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dome target generation
    // -----------------------------------------------------------------------

    private void generateDomeTargets() {
        final List<Vector> positions = new ArrayList<>();
        final int r = this.domeRadius;
        for (int x = -(r + 1); x <= r + 1; x++) {
            for (int y = -(r + 1); y <= r + 1; y++) {
                for (int z = -(r + 1); z <= r + 1; z++) {
                    final double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist >= r - 1.0 && dist <= r + 0.5) {
                        positions.add(new Vector(x, y, z));
                    }
                }
            }
        }
        Collections.shuffle(positions);
        this.domeTargets.addAll(positions);
    }

    // -----------------------------------------------------------------------
    // progress() – called every tick by PK
    // -----------------------------------------------------------------------

    @Override
    public void progress() {
        if (this.player.isDead() || !this.player.isOnline()) {
            this.remove();
            return;
        }
        if (this.phase == Phase.CHARGING) {
            this.progressCharging();
        } else {
            this.progressFiring();
        }
    }

    // -----------------------------------------------------------------------
    // Charging phase
    // -----------------------------------------------------------------------

    private void progressCharging() {
        if (!this.player.isSneaking()) {
            this.bPlayer.addCooldown(this);
            this.shatterDome();
            this.remove();
            return;
        }
        if (this.player.getInventory().getHeldItemSlot() != this.heldSlot) {
            this.bPlayer.addCooldown(this);
            this.shatterDome();
            this.remove();
            return;
        }

        final long elapsed = System.currentTimeMillis() - this.chargeStartTime;

        if (elapsed > this.chargeTime + this.chargeHoldGrace) {
            this.bPlayer.addCooldown(this);
            this.shatterDome();
            this.remove();
            return;
        }

        final Location playerCentre = this.player.getLocation().clone().add(0, 1, 0);
        final long waterFillEnd = this.chargeTime - 2000L;  // dome fully formed at 4 s
        final long freezeStart  = this.chargeTime - 1000L;  // freeze begins at 5 s

        // Cancel movement only when standing on a solid block
        final Block blockBelow = this.player.getLocation().clone().subtract(0, 0.1, 0).getBlock();
        if (!blockBelow.isPassable()) {
            this.player.setVelocity(new Vector(0, 0, 0));
        }

        // Every tick remove any non-dome water that has appeared inside the dome sphere.
        this.cleanupInteriorWater(playerCentre);

        // Gradually place water blocks to create the dome for the first 5~ seconds
        final double waterProgress = Math.min(1.0, (double) elapsed / waterFillEnd);
        final int targetCount = (int) (waterProgress * this.domeTargets.size());
        while (this.domeTargetIndex < targetCount && this.domeTargetIndex < this.domeTargets.size()) {
            this.placeWaterBlock(playerCentre, this.domeTargets.get(this.domeTargetIndex));
            this.domeTargetIndex++;
        }

        // Progressively convert water to ice in last second
        if (elapsed >= freezeStart && !this.chargeComplete) {
            final double freezeProgress = Math.min(1.0, (double) (elapsed - freezeStart) / 1000.0);
            final int freezeTarget = (int) (freezeProgress * this.domeBlocks.size());
            while (this.freezeIndex < freezeTarget && this.freezeIndex < this.domeBlocks.size()) {
                final TempBlock tb = this.domeBlocks.get(this.freezeIndex);
                if (tb.getBlock().getType() == Material.WATER) {
                    tb.setType(Material.ICE);
                }
                this.freezeIndex++;
            }
        }

        // Ambient water sound while dome is forming
        if (!this.chargeComplete && elapsed - this.lastWaterSoundTime >= 600L) {
            this.lastWaterSoundTime = elapsed;
            this.player.getWorld().playSound(playerCentre, Sound.BLOCK_WATER_AMBIENT, 0.7f, 1.0f);
        }

        // Freeze remaining water and play cue
        if (!this.chargeComplete && elapsed >= this.chargeTime) {
            this.chargeComplete = true;
            this.freezeDome();
            this.player.getWorld().playSound(playerCentre, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.8f);
            this.player.getWorld().playSound(playerCentre, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
        }
    }

    // -----------------------------------------------------------------------
    // Dome block helpers
    // -----------------------------------------------------------------------

    /**
     * Places a water block at the target position as a TempBlock so PK tracks
     * and reverts it automatically, and so isTempBlock() can guard event handlers.
     */
    private void placeWaterBlock(final Location playerCentre, final Vector targetOffset) {
        final Location targetLoc = playerCentre.clone().add(targetOffset);
        final Block block = targetLoc.getBlock();
        if (!block.isPassable()) return;
        if (TempBlock.isTempBlock(block)) return;
        this.domeBlocks.add(new TempBlock(block, Material.WATER.createBlockData()));
    }

    /**
     * Converts all remaining water dome blocks to ice.
     */
    private void freezeDome() {
        for (final TempBlock tb : this.domeBlocks) {
            if (tb.getBlock().getType() == Material.WATER) {
                tb.setType(Material.ICE);
            }
        }
    }

    /**
     * Removes all dome blocks (water or ice) and clears the tracking list.
     */
    private void removeDomeBlocks() {
        for (final TempBlock tb : this.domeBlocks) {
            tb.revertBlock();
        }
        this.domeBlocks.clear();
    }

    // -----------------------------------------------------------------------
    // Firing
    // -----------------------------------------------------------------------

    public void fire() {
        if (this.phase != Phase.CHARGING) return;

        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, this.player, "IceSpike");
        this.bPlayer.addCooldown(this);

        if (System.currentTimeMillis() - this.chargeStartTime < this.chargeTime) {
            this.shatterDome();
            this.remove();
            return;
        }

        this.phase = Phase.FIRING;
        this.fireTime = System.currentTimeMillis();

        this.removeDomeBlocks();

        final Location centre = this.player.getLocation().clone().add(0, 1, 0);
        final Vector direction = this.player.getEyeLocation().getDirection().normalize();

        this.player.getWorld().playSound(centre, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        this.player.getWorld().playSound(centre, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);

        final Random rng = new Random();
        final int count = 15 + rng.nextInt(6);
        final Material[] iceMats = { Material.ICE, Material.PACKED_ICE };

        final Transformation shardTransform = new Transformation(
                new Vector3f(-0.25f, -0.25f, -0.25f),
                new Quaternionf(),
                new Vector3f(0.5f, 0.5f, 0.5f),
                new Quaternionf());

        for (int i = 0; i < count; i++) {
            Location spawnLoc = centre.clone().add(this.domeTargets.get(i % this.domeTargets.size()));
            if (!spawnLoc.getBlock().isPassable()) {
                spawnLoc = centre.clone();
            }

            final Vector vel = direction.clone()
                    .add(new Vector(
                            (rng.nextDouble() - 0.5) * 0.20,
                            (rng.nextDouble() - 0.5) * 0.20,
                            (rng.nextDouble() - 0.5) * 0.20))
                    .normalize()
                    .multiply(this.projectileSpeed);

            final Material mat = iceMats[rng.nextInt(iceMats.length)];
            final Location finalSpawnLoc = spawnLoc;
            final BlockDisplay bd = player.getWorld().spawn(finalSpawnLoc, BlockDisplay.class, e -> {
                e.setBlock(mat.createBlockData());
                e.setTransformation(shardTransform);
                e.setTeleportDuration(1); // 1-tick client interpolation for smooth motion
                e.setPersistent(false);
            });

            this.shards.add(new Shard(bd, vel));
        }
    }

    // -----------------------------------------------------------------------
    // Firing phase
    // -----------------------------------------------------------------------

    private void progressFiring() {
        if (this.player.getInventory().getHeldItemSlot() != this.heldSlot) {
            this.shatterAll();
            this.remove();
            return;
        }
        if (System.currentTimeMillis() - this.fireTime > this.projectileMaxLifetime) {
            this.shatterAll();
            this.remove();
            return;
        }

        for (int i = this.shards.size() - 1; i >= 0; i--) {
            final Shard shard = this.shards.get(i);
            if (shard.display.isDead()) {
                this.shards.remove(i);
                continue;
            }

            // Apply gravity each tick to match vanilla falling-block acceleration.
            shard.velocity.add(new Vector(0, -0.04, 0));

            final Location currentLoc = shard.display.getLocation();
            final Location nextLoc = currentLoc.clone().add(shard.velocity);

            if (GeneralMethods.isRegionProtectedFromBuild(this, nextLoc)) {
                this.shatterShard(shard, currentLoc.clone().add(0, 0.25, 0));
                this.shards.remove(i);
                continue;
            }

            // Shatter before entering a solid block
            if (!nextLoc.getBlock().isPassable()) {
                this.shatterShard(shard, currentLoc.clone().add(0, 0.25, 0));
                this.shards.remove(i);
                continue;
            }

            // Move the display entity
            shard.display.teleport(nextLoc);

            // Each entity can only be hit once
            boolean hit = false;
            for (final Entity ent : GeneralMethods.getEntitiesAroundPoint(nextLoc, 1.2)) {
                if (!(ent instanceof LivingEntity entity)) continue;
                if (entity.equals(this.player)) continue;
                if (entity instanceof Player p && Commands.invincible.contains(p.getName())) continue;
                if (this.hitEntities.contains(entity.getUniqueId())) continue;

                this.hitEntities.add(entity.getUniqueId());
                DamageHandler.damageEntity(entity, this.damage, this);
                this.shatterShard(shard, nextLoc.clone().add(0, 0.25, 0));
                hit = true;
                this.shards.remove(i);
                break;
            }
            if (hit) continue;
        }

        if (this.shards.isEmpty()) {
            this.remove();
        }
    }

    // -----------------------------------------------------------------------
    // Shattering helpers
    // -----------------------------------------------------------------------

    private void shatterShard(final Shard shard, final Location loc) {
        if (!shard.display.isDead()) shard.display.remove();
        loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
        loc.getWorld().spawnParticle(
                Particle.BLOCK, loc, 14, 0.3, 0.3, 0.3, 0.15, Material.ICE.createBlockData());
        loc.getWorld().spawnParticle(
                Particle.SNOWFLAKE, loc, 8, 0.25, 0.25, 0.25, 0.05);
    }

    private void shatterDome() {
        this.removeDomeBlocks();
        this.player.getWorld().playSound(this.player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
    }

    private void shatterAll() {
        this.shatterDome();
        for (final Shard shard : new ArrayList<>(this.shards)) {
            if (!shard.display.isDead()) {
                this.shatterShard(shard, shard.display.getLocation().clone().add(0, 0.25, 0));
            }
        }
        this.shards.clear();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void remove() {
        super.remove();
        ACTIVE_INSTANCES.remove(this.player.getUniqueId());
        this.removeDomeBlocks();
        for (final Shard shard : this.shards) {
            if (!shard.display.isDead()) shard.display.remove();
        }
        this.shards.clear();
    }

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    public boolean isCharging() {
        return this.phase == Phase.CHARGING;
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static IceShards getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    // -----------------------------------------------------------------------
    // CoreAbility / AddonAbility boilerplate
    // -----------------------------------------------------------------------

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
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        return this.player.getLocation();
    }

    @Override
    public String getName() {
        return "IceShards";
    }

    @Override
    public void load() {
        listener = new Listener() {
            /**
             * Cancels any water flow that originates from a dome block OR whose
             * destination falls within the dome radius of an active instance.
             * The second check prevents external water from flowing into the dome
             * area and triggering vanilla infinite-source creation.
             */
            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onBlockFromTo(final BlockFromToEvent e) {
                if (e.getBlock().getType() != Material.WATER) return;
                // Source is a registered dome block
                if (TempBlock.isTempBlock(e.getBlock())) {
                    e.setCancelled(true);
                    return;
                }
                // Destination is inside (or just outside) an active dome — cancel
                // to stop external water from entering and creating new sources.
                if (ACTIVE_INSTANCES.isEmpty()) return;
                final Location toLoc = e.getToBlock().getLocation();
                for (final IceShards inst : ACTIVE_INSTANCES.values()) {
                    if (!inst.player.getWorld().equals(toLoc.getWorld())) continue;
                    final Location centre = inst.player.getLocation().clone().add(0, 1, 0);
                    final double r = inst.domeRadius + 2;
                    if (toLoc.distanceSquared(centre) <= r * r) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            /**
             * Suppresses fluid physics on dome water blocks and on any water
             * block within the dome radius, preventing the scheduler from
             * queueing new flow ticks that bypass BlockFromToEvent.
             */
            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onBlockPhysics(final BlockPhysicsEvent e) {
                if (e.getBlock().getType() != Material.WATER) return;
                if (TempBlock.isTempBlock(e.getBlock())) {
                    e.setCancelled(true);
                    return;
                }
                if (ACTIVE_INSTANCES.isEmpty()) return;
                final Location loc = e.getBlock().getLocation();
                for (final IceShards inst : ACTIVE_INSTANCES.values()) {
                    if (!inst.player.getWorld().equals(loc.getWorld())) continue;
                    final Location centre = inst.player.getLocation().clone().add(0, 1, 0);
                    final double r = inst.domeRadius + 2;
                    if (loc.distanceSquared(centre) <= r * r) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPKReload(final BendingReloadEvent e) {
                new ArrayList<>(CoreAbility.getAbilities(IceShards.class)).forEach(CoreAbility::remove);
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, AranarthCore.getInstance());
    }

    @Override
    public void stop() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        ACTIVE_INSTANCES.clear();
    }

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    // -----------------------------------------------------------------------
    // ComboAbility
    // -----------------------------------------------------------------------

    @Override
    public Object createNewComboInstance(final Player player) {
        return new IceShards(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("WaterBubble", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("PhaseChange", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("IceSpike", ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Prevent the rain from falling, and create a dome of rain water to shield you. " +
                "Once the dome has fully formed, it will rapidly freeze into ice, allowing you to " +
                "launch the deadly shards of ice at your targets.\n" +
                ChatUtils.translateToColor("&fUsage: WaterBubble (Hold Sneak) > PhaseChange (Release Sneak) > IceSpike (Hold Sneak to charge) > IceSpike (Left Click)");
    }
}
