package com.aearost.aranarthcore.abilities.earthbending.metalbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class MetalShots extends MetalAbility implements AddonAbility {

    private static final int MAX_SOURCES = 3;
    private static final int SHOTS_PER_SOURCE = 4;
    private static final double SOURCE_RANGE = 6.0;
    private static final double ORBIT_RADIUS = 1.8;
    private static final double ORBIT_HEIGHT = 0.75;
    private static final double PROJECTILE_SPEED = 3.0;
    private static final double STEP = 0.3;
    private static final double HIT_RADIUS = 0.6;
    private static final long SHOT_COOLDOWN_MS = 200L;
    private static final long MAX_DURATION_MS = 30_000L;

    private static final Particle.DustOptions SOURCE_DUST =
            new Particle.DustOptions(Color.fromRGB(120, 120, 130), 0.6f);
    private static final Particle.DustOptions SHOT_TRAIL_DUST =
            new Particle.DustOptions(Color.fromRGB(160, 160, 165), 0.5f);

    private static final Map<UUID, MetalShots> ACTIVE_INSTANCES = new HashMap<>();

    private static NamespacedKey sourceKey;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private final List<SourceBlock> sources = new ArrayList<>();
    private final List<FlyingShot> shots = new ArrayList<>();
    private long startTime;
    private long lastShotTime;

    /**
     * A metal block that has been lifted into the air as a floating projectile source.
     */
    private static final class SourceBlock {
        final BlockDisplay entity;
        final TempBlock tempBlock;
        final Location originalLocation;
        final Material projectileMaterial;
        final double orbitAngle;
        int shotsRemaining;

        SourceBlock(final BlockDisplay entity, final TempBlock tempBlock,
                    final Location originalLocation, final Material projectileMaterial,
                    final double orbitAngle) {
            this.entity = entity;
            this.tempBlock = tempBlock;
            this.originalLocation = originalLocation.clone();
            this.projectileMaterial = projectileMaterial;
            this.orbitAngle = orbitAngle;
            this.shotsRemaining = SHOTS_PER_SOURCE;
        }
    }

    /**
     * An item entity fired as a projectile from a floating source block.
     */
    private static final class FlyingShot {
        final Item entity;
        final Location startLocation;
        final Vector direction;

        FlyingShot(final Item entity, final Location start, final Vector direction) {
            this.entity = entity;
            this.startLocation = start.clone();
            this.direction = direction.clone();
        }
    }

    public MetalShots(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.cooldown = 10_000L;
        this.damage = 2.0;    // 1 full heart
        this.range = 16.0;
        this.startTime = System.currentTimeMillis();
        this.lastShotTime = 0L;

        if (!trySourceBlock()) {
            return;
        }

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    /**
     * Attempts to source a metal block in the player's line of sight.
     *
     * @return true if a block was successfully sourced, false otherwise.
     */
    private boolean trySourceBlock() {
        if (sources.size() >= MAX_SOURCES) {
            return false;
        }

        final Block targetBlock = player.getTargetBlock(null, (int) SOURCE_RANGE);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            return false;
        }
        if (!this.isMetalbendable(targetBlock)) {
            return false;
        }
        if (GeneralMethods.isRegionProtectedFromBuild(this, targetBlock.getLocation())) {
            return false;
        }

        // Prevent the same block from being sourced more than once
        final Location blockLoc = targetBlock.getLocation();
        for (final SourceBlock existing : sources) {
            if (existing.originalLocation.equals(blockLoc)) {
                return false;
            }
        }

        // Assign a fixed orbit angle so the position persists even when other sources are removed
        final double orbitAngle = Math.toRadians(sources.size() * (360.0 / MAX_SOURCES));

        // Block data must be captured before TempBlock replaces the block with AIR
        final BlockData originalBlockData = targetBlock.getBlockData();
        final TempBlock tempBlock = new TempBlock(targetBlock, Material.AIR);
        final Location displayLoc = orbitTarget(orbitAngle).subtract(0.25, 0.25, 0.25);
        final BlockDisplay display = player.getWorld().spawn(displayLoc, BlockDisplay.class, d -> {
            d.setBlock(originalBlockData);
            d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()
            ));
        });

        sources.add(new SourceBlock(display, tempBlock, blockLoc, resolveProjectileMaterial(targetBlock.getType()), orbitAngle));

        player.getWorld().playSound(blockLoc, Sound.BLOCK_METAL_HIT, 0.8f, 0.8f);
        player.getWorld().playSound(blockLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.2f);

        sendActionBarMessage();
        return true;
    }

    public void onSneak() {
        trySourceBlock();
    }

    /**
     * Fires one projectile from the first available floating source block.
     */
    public void onLeftClick() {
        if (sources.isEmpty()) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - lastShotTime < SHOT_COOLDOWN_MS) {
            return;
        }

        // Pick a random source block that still has shots remaining
        final List<SourceBlock> available = new ArrayList<>();
        for (final SourceBlock s : sources) {
            if (s.shotsRemaining > 0) {
                available.add(s);
            }
        }
        if (available.isEmpty()) {
            return;
        }
        final SourceBlock source = available.get((int) (Math.random() * available.size()));

        lastShotTime = now;
        source.shotsRemaining--;

        final Location fireFrom = source.entity.isValid()
                ? source.entity.getLocation().clone().add(0.25, 0.25, 0.25)
                : player.getEyeLocation();
        final Vector direction = player.getEyeLocation().getDirection().normalize();

        final ItemStack shotStack = new ItemStack(source.projectileMaterial, 1);
        final ItemMeta meta = shotStack.getItemMeta();
        if (meta != null) {
            // Unique per-shot tag prevents items from merging with each other on the ground
            meta.getPersistentDataContainer().set(
                    getSourceKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
            shotStack.setItemMeta(meta);
        }

        final Item shotItem = player.getWorld().dropItem(fireFrom, shotStack);
        shotItem.setPickupDelay(Integer.MAX_VALUE);
        shotItem.setCanMobPickup(false);
        shotItem.setCanPlayerPickup(false);
        shotItem.setGravity(false);
        shotItem.setVelocity(direction.clone().multiply(PROJECTILE_SPEED));

        shots.add(new FlyingShot(shotItem, fireFrom, direction));

        player.getWorld().playSound(fireFrom, Sound.BLOCK_METAL_HIT, 0.9f, 1.8f);
        player.getWorld().playSound(fireFrom, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 2.0f);

        if (source.shotsRemaining <= 0) {
            returnSourceBlock(source);
            sources.remove(source);
        }

        sendActionBarMessage();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (System.currentTimeMillis() - startTime >= MAX_DURATION_MS) {
            endWithCooldown();
            return;
        }

        // Slowness III is applied every tick while floating blocks are present as a mobility nerf
        if (!sources.isEmpty()) {
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.SLOWNESS, 40, 2, false, false));
        }

        updateFloatingBlocks();
        progressShots();

        if (sources.isEmpty() && shots.isEmpty()) {
            endWithCooldown();
        }
    }

    private void updateFloatingBlocks() {
        for (int i = sources.size() - 1; i >= 0; i--) {
            final SourceBlock source = sources.get(i);

            if (source.entity.isDead() || !source.entity.isValid()) {
                source.tempBlock.revertBlock();
                sources.remove(i);
                sendActionBarMessage();
                continue;
            }

            // Teleport the BlockDisplay directly to its orbit position each tick
            final Location orbitPos = orbitTarget(source.orbitAngle).subtract(0.25, 0.25, 0.25);
            source.entity.teleport(orbitPos);

            source.entity.getWorld().spawnParticle(
                    Particle.DUST,
                    source.entity.getLocation().clone().add(0.25, 0.25, 0.25),
                    1, 0.15, 0.15, 0.15, 0, SOURCE_DUST
            );
        }
    }

    private void progressShots() {
        for (int i = shots.size() - 1; i >= 0; i--) {
            final FlyingShot shot = shots.get(i);

            if (shot.entity.isDead() || !shot.entity.isValid()) {
                shots.remove(i);
                continue;
            }

            final Location current = shot.entity.getLocation();
            final Location checkPos = current.clone();
            double remaining = PROJECTILE_SPEED;
            boolean shouldRemove = false;

            // Sub-step scan so fast-moving projectiles never skip past a thin target
            while (remaining > 0 && !shouldRemove) {
                final double step = Math.min(STEP, remaining);
                checkPos.add(shot.direction.clone().multiply(step));
                remaining -= step;

                if (checkPos.distanceSquared(shot.startLocation) > range * range) {
                    shouldRemove = true;
                    break;
                }

                // Passable blocks such as tall grass and flowers are intentionally not treated as hits
                if (checkPos.getBlock().getType().isSolid() && !checkPos.getBlock().isPassable()) {
                    shouldRemove = true;
                    break;
                }

                for (final Entity entity : checkPos.getWorld().getNearbyEntities(
                        checkPos, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                    if (!(entity instanceof LivingEntity)) {
                        continue;
                    }
                    if (entity.equals(player)) {
                        continue;
                    }

                    DamageHandler.damageEntity(entity, damage, this);
                    entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_METAL_HIT, 1.0f, 0.8f);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 1.5f);
                    shouldRemove = true;
                    break;
                }
            }

            if (shouldRemove) {
                shot.entity.remove();
                shots.remove(i);
            } else {
                shot.entity.setVelocity(shot.direction.clone().multiply(PROJECTILE_SPEED));
                current.getWorld().spawnParticle(
                        Particle.DUST, current, 1, 0.04, 0.04, 0.04, 0, SHOT_TRAIL_DUST);
            }
        }
    }

    /**
     * Returns the world-space position this source block should float toward.
     */
    private Location orbitTarget(final double angle) {
        final Location base = player.getLocation();
        return base.clone().add(
                ORBIT_RADIUS * Math.cos(angle),
                player.getEyeHeight() + ORBIT_HEIGHT,
                ORBIT_RADIUS * Math.sin(angle)
        );
    }

    private void sendActionBarMessage() {
        final int totalShots = sources.stream().mapToInt(s -> s.shotsRemaining).sum();
        if (totalShots > 0) {
            player.sendActionBar(Component.text(totalShots + " shot" + (totalShots == 1 ? "" : "s") + " remaining")
                    .color(NamedTextColor.GRAY));
        }
    }

    /**
     * Removes the entity and reverts the TempBlock to restore the original block.
     */
    private void returnSourceBlock(final SourceBlock source) {
        if (source.entity.isValid()) {
            source.entity.remove();
        }
        source.tempBlock.revertBlock();
        player.getWorld().playSound(
                source.originalLocation.clone().add(0.5, 0.5, 0.5),
                Sound.BLOCK_METAL_HIT, 0.6f, 0.7f);
    }

    /**
     * Applies the ability cooldown and removes all floating blocks and in-flight shots.
     */
    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void remove() {
        ACTIVE_INSTANCES.remove(player.getUniqueId());

        for (final SourceBlock source : sources) {
            if (source.entity.isValid()) {
                source.entity.remove();
            }
            source.tempBlock.revertBlock();
        }
        sources.clear();

        for (final FlyingShot shot : shots) {
            if (shot.entity.isValid()) {
                shot.entity.remove();
            }
        }
        shots.clear();

        super.remove();
    }

    @Override
    public void stop() {
        ACTIVE_INSTANCES.clear();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static MetalShots getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public static NamespacedKey getSourceKey() {
        if (sourceKey == null) {
            sourceKey = new NamespacedKey(AranarthCore.getInstance(), "metalshots_source");
        }
        return sourceKey;
    }

    /**
     * Maps a source block's material to the closest matching projectile item.
     * Defaults to iron ingots for any unrecognized metal type.
     */
    private static Material resolveProjectileMaterial(final Material blockMaterial) {
        final String name = blockMaterial.name().toLowerCase();
        if (name.contains("quartz")) {
            return Material.QUARTZ;
        }
        if (name.contains("copper") || blockMaterial == Material.LIGHTNING_ROD) {
            return Material.COPPER_INGOT;
        }
        if (name.contains("gold") || blockMaterial == Material.GILDED_BLACKSTONE) {
            return Material.GOLD_INGOT;
        }
        if (name.contains("netherite") || blockMaterial == Material.ANCIENT_DEBRIS) {
            return Material.NETHERITE_SCRAP;
        }
        return Material.IRON_INGOT;
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
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "MetalShots";
    }

    @Override
    public void load() {
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
        return "Raise up to 3 metal blocks into the air around you, "
                + "and rapidly fire up to 4 metal projectiles from each floating block.\n"
                + ChatUtils.translateToColor("&fUsage: Tap Sneak (up to 3 blocks) > Left-click (multiple times)");
    }
}
