package com.aearost.aranarthcore.abilities.earthbending.lavabending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.LavaAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MoltenBlast extends LavaAbility implements AddonAbility, ComboAbility {

    private static final Map<UUID, MoltenBlast> ACTIVE_INSTANCES = new HashMap<>();
    private static final Map<UUID, Long> RECENT_EARTHSMASH_SNEAK = new HashMap<>();

    private static final long EARTHSMASH_SNEAK_WINDOW = 5000L;

    /**
     * Called by the listener when the player presses sneak while EarthSmash is bound.
     */
    public static void markEarthSmashSneak(final UUID uuid) {
        RECENT_EARTHSMASH_SNEAK.put(uuid, System.currentTimeMillis());
    }

    /**
     * Returns true if the player held sneak on EarthSmash within the last {@value #EARTHSMASH_SNEAK_WINDOW} ms.
     */
    public static boolean hasRecentEarthSmashSneak(final UUID uuid) {
        final Long ts = RECENT_EARTHSMASH_SNEAK.get(uuid);
        if (ts == null) {
            return false;
        }
        if (System.currentTimeMillis() - ts > EARTHSMASH_SNEAK_WINDOW) {
            RECENT_EARTHSMASH_SNEAK.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clearEarthSmashSneak(final UUID uuid) {
        RECENT_EARTHSMASH_SNEAK.remove(uuid);
    }

    public enum Phase {CHARGING, CONVERTING, LIFTING, LIFTED, GRABBED, FLYING}

    private static final int[][] BALL_OFFSETS;

    static {
        final List<int[]> offsets = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if ((Math.abs(x) + Math.abs(y) + Math.abs(z)) % 2 == 0) {
                        offsets.add(new int[]{x, y, z});
                    }
                }
            }
        }
        BALL_OFFSETS = offsets.toArray(new int[0][]);
    }

    private static final long CHARGE_DURATION = 1500L;
    private static final long CONVERT_DURATION = 1000L;
    private static final double CONVERT_RADIUS = 5.0;
    private static final long LIFT_INTERVAL = 75L;
    private static final int LIFT_STEPS = 3;
    private static final long LIFTED_DURATION = 10000L;
    private static final long MOVE_INTERVAL = 38L;
    private static final double HIT_RADIUS = 2.0;
    private static final double GRABBED_MAX_DIST = 12.0;
    private static final int MAX_BLOCKS_THROUGH = 2;
    private static final double SPLATTER_RADIUS = 7.0;
    private static final int SPLATTER_BLOCK_COUNT = 18;
    private static final int SPLATTER_FB_COUNT = 12;
    private static final long SPLATTER_REVERT_TICKS = 100L;

    private static final Particle.DustOptions LAVA_DEEP_RED = new Particle.DustOptions(Color.fromRGB(140, 10, 0), 1.1f);
    private static final Particle.DustOptions LAVA_RED = new Particle.DustOptions(Color.fromRGB(195, 30, 0), 1.0f);
    private static final Particle.DustOptions LAVA_ORANGE_RED = new Particle.DustOptions(Color.fromRGB(220, 65, 10), 0.9f);
    private static final Particle.DustOptions LAVA_ORANGE = new Particle.DustOptions(Color.fromRGB(250, 110, 25), 0.8f);
    private static final Particle.DustOptions[] LAVA_PALETTE = {LAVA_DEEP_RED, LAVA_RED, LAVA_ORANGE_RED, LAVA_ORANGE};

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute("Min" + Attribute.DAMAGE)
    private double minDamage;
    @Attribute(Attribute.DAMAGE)
    private double maxDamage;
    @Attribute(Attribute.SELECT_RANGE)
    private double selectRange;
    @Attribute(Attribute.RANGE)
    private double shootRange;

    private Phase phase;
    private boolean removed = false;
    private boolean isGrabbing = false;

    private Location ballCenter;

    private final List<TempBlock> ballBlocks = new ArrayList<>();
    private final List<TempBlock> convertBlocks = new ArrayList<>();
    private final Set<Block> lavaBlockSet = new HashSet<>();
    private final Set<Block> convertLavaSet = new HashSet<>();
    private final Set<Block> convertedSet = new HashSet<>();
    private final Set<UUID> hitEntities = new HashSet<>();
    private final Random random = new Random();

    private long chargeStartTime;
    private long convertStartTime;
    private long liftedStartTime;
    private long lastLiftTime;
    private long lastMoveTime;
    private int liftStepsDone = 0;

    private double grabbedDistance;
    private Location destination;

    public MoltenBlast(final Player player) {
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

        this.cooldown = 9000L;
        this.minDamage = 6.0;
        this.maxDamage = 10.0;
        this.selectRange = 12.0;
        this.shootRange = 30.0;

        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();

        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "LavaFlow");

        RECENT_EARTHSMASH_SNEAK.remove(player.getUniqueId());
        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        if (ballCenter != null && GeneralMethods.isRegionProtectedFromBuild(this, ballCenter)) {
            remove();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case CONVERTING -> progressConverting();
            case LIFTING -> progressLifting();
            case LIFTED -> progressLifted();
            case GRABBED -> progressGrabbed();
            case FLYING -> progressFlying();
        }
    }

    private void progressCharging() {
        if (!player.isSneaking()) {
            remove();
            return;
        }
        // Suppress LavaFlow so it doesn't activate while the player holds sneak
        bPlayer.addCooldown("LavaFlow", 500L);
        // Only show particles once the charge is complete
        final long elapsed = System.currentTimeMillis() - chargeStartTime;
        if (elapsed >= CHARGE_DURATION) {
            spawnChargingParticles();
        }
    }

    private void progressConverting() {
        final long elapsed = System.currentTimeMillis() - convertStartTime;
        final double progress = Math.min(1.0, (double) elapsed / CONVERT_DURATION);

        updateConvertBlocks(progress * CONVERT_RADIUS);
        spawnConvertParticles(progress);

        if (elapsed >= CONVERT_DURATION) {
            drawLavaBall();
            lastLiftTime = System.currentTimeMillis();
            phase = Phase.LIFTING;
        }
    }

    private void progressLifting() {
        final long now = System.currentTimeMillis();
        if (now - lastLiftTime < LIFT_INTERVAL) {
            return;
        }
        lastLiftTime = now;

        if (liftStepsDone >= LIFT_STEPS) {
            liftedStartTime = System.currentTimeMillis();
            phase = Phase.LIFTED;
            return;
        }

        clearBall();
        ballCenter = ballCenter.clone().add(0, 1, 0);
        drawLavaBall();
        liftStepsDone++;
    }

    private void progressLifted() {
        // Keep LavaThrow suppressed so PK doesn't fire it when the player holds sneak to grab
        bPlayer.addCooldown("LavaThrow", 500L);

        if (System.currentTimeMillis() - liftedStartTime > LIFTED_DURATION) {
            endWithCooldown();
            return;
        }
        spawnBallParticles();
    }

    private void progressGrabbed() {
        // Keep LavaThrow suppressed throughout the grab
        bPlayer.addCooldown("LavaThrow", 500L);

        if (!isGrabbing) {
            liftedStartTime = System.currentTimeMillis();
            phase = Phase.LIFTED;
            return;
        }

        final Location eye = player.getEyeLocation();
        final Location desired = eye.clone().add(eye.getDirection().normalize().multiply(grabbedDistance));

        boolean blocked = false;
        for (final int[] off : BALL_OFFSETS) {
            final Block block = desired.clone().add(off[0], off[1], off[2]).getBlock();
            if (!ElementalAbility.isAir(block.getType()) && !isTransparent(block)) {
                blocked = true;
                break;
            }
        }

        clearBall();
        if (!blocked) {
            ballCenter = desired;
        }
        drawLavaBall();
        spawnBallParticles();
    }

    private void progressFlying() {
        final long now = System.currentTimeMillis();
        if (now - lastMoveTime < MOVE_INTERVAL) {
            return;
        }
        lastMoveTime = now;

        clearBall();
        ballCenter.add(GeneralMethods.getDirection(ballCenter, destination).normalize());

        if (ballCenter.distanceSquared(destination) < 4.0) {
            // Reached the range limit without hitting anything
            clearBall();
            bPlayer.addCooldown(this);
            remove();
            return;
        }

        int blockedCount = 0;
        for (final int[] off : BALL_OFFSETS) {
            final Block block = ballCenter.clone().add(off[0], off[1], off[2]).getBlock();
            if (!ElementalAbility.isAir(block.getType()) && !isTransparent(block)) {
                blockedCount++;
            }
        }
        if (blockedCount > MAX_BLOCKS_THROUGH) {
            impact(ballCenter.clone());
            return;
        }

        if (GeneralMethods.isRegionProtectedFromBuild(this, ballCenter)) {
            remove();
            return;
        }

        collideWithEntities();

        if (!removed) {
            drawLavaBall();
            spawnBallParticles();
        }
    }

    public void onSneakRelease() {
        if (phase == Phase.CHARGING) {
            if (System.currentTimeMillis() - chargeStartTime < CHARGE_DURATION) {
                remove();
                return;
            }
            AranarthBendingUtils.suppressComboTrigger(bPlayer, player, "LavaFlow");
            final Block originBlock = getEarthSourceBlock(selectRange);
            if (originBlock == null) {
                remove();
                return;
            }
            ballCenter = originBlock.getLocation().clone().add(0.5, 0.5, 0.5);
            convertStartTime = System.currentTimeMillis();
            phase = Phase.CONVERTING;

            final Location loc = ballCenter.clone();
            loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.6f);
        } else if (phase == Phase.GRABBED) {
            isGrabbing = false;
            liftedStartTime = System.currentTimeMillis();
            phase = Phase.LIFTED;
        }
    }

    public void grab() {
        if (phase != Phase.LIFTED) {
            return;
        }
        grabbedDistance = Math.min(player.getEyeLocation().distance(ballCenter), GRABBED_MAX_DIST);
        isGrabbing = true;
        phase = Phase.GRABBED;
    }

    public void fire() {
        if (phase != Phase.GRABBED) {
            return;
        }

        final Location eyeLoc = player.getEyeLocation();
        destination = eyeLoc.clone().add(eyeLoc.getDirection().normalize().multiply(shootRange));
        lastMoveTime = System.currentTimeMillis();
        isGrabbing = false;
        phase = Phase.FLYING;

        final Location loc = ballCenter.clone();
        loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 1.0f);

        AranarthBendingUtils.suppressComboTrigger(bPlayer, player, "LavaThrow");
    }

    private void drawLavaBall() {
        clearBall();
        final BlockData magmaData = Material.MAGMA_BLOCK.createBlockData();
        for (final int[] off : BALL_OFFSETS) {
            final Block block = ballCenter.clone().add(off[0], off[1], off[2]).getBlock();
            if (isTransparent(block) || TempBlock.isTempBlock(block)) {
                ballBlocks.add(new TempBlock(block, magmaData));
            }
        }
    }

    private void clearBall() {
        for (final TempBlock tb : ballBlocks) {
            tb.revertBlock();
        }
        ballBlocks.clear();
        lavaBlockSet.clear();
    }

    private void updateConvertBlocks(final double currentRadius) {
        final int r = (int) Math.ceil(currentRadius);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                final double dist = Math.sqrt(x * x + z * z);
                if (dist >= currentRadius) {
                    continue;
                }
                final Location candidate = ballCenter.clone().add(x, 0, z);
                final Block ground = findGroundBlock(candidate);
                if (ground == null || convertedSet.contains(ground)) {
                    continue;
                }
                if (GeneralMethods.isRegionProtectedFromBuild(this, ground.getLocation())) {
                    continue;
                }
                convertedSet.add(ground);
                if (dist < 1.0) {
                    convertBlocks.add(new TempBlock(ground, createSourceLavaData()));
                    convertLavaSet.add(ground);
                } else {
                    convertBlocks.add(new TempBlock(ground, convertMaterialForDistance(dist).createBlockData()));
                }
            }
        }
    }

    private Material convertMaterialForDistance(final double dist) {
        if (dist <= 2.0) {
            return AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[3]; // MAGMA_BLOCK
        }
        if (dist <= 3.0) {
            return AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[2]; // NETHERRACK
        }
        if (dist <= 4.0) {
            return AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[1]; // GRANITE
        }
        return AranarthBendingUtils.LAVA_WARMUP_SEQUENCE[0];  // STONE
    }

    private void clearConvertBlocks() {
        for (final TempBlock tb : convertBlocks) {
            tb.revertBlock();
        }
        convertBlocks.clear();
        convertedSet.clear();
        convertLavaSet.clear();
    }

    private void collideWithEntities() {
        for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(ballCenter, HIT_RADIUS)) {
            if (!(entity instanceof final LivingEntity living)) {
                continue;
            }
            if (entity.equals(player)) {
                continue;
            }
            if (hitEntities.contains(entity.getUniqueId())) {
                continue;
            }
            if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
                continue;
            }

            hitEntities.add(entity.getUniqueId());

            final double damage = minDamage + random.nextDouble() * (maxDamage - minDamage);
            DamageHandler.damageEntity(living, damage, this);

            final Vector kb = GeneralMethods.getDirection(ballCenter, entity.getLocation()).setY(0.3).normalize().multiply(1.0);
            GeneralMethods.setVelocity(this, entity, kb);

            impact(ballCenter.clone());
            return;
        }
    }

    private void impact(final Location loc) {
        clearBall();
        final List<TempBlock> deferred = new ArrayList<>(convertBlocks);
        convertBlocks.clear();
        convertedSet.clear();
        convertLavaSet.clear();
        if (!deferred.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final TempBlock tb : deferred) {
                        tb.revertBlock();
                    }
                }
            }.runTaskLater(AranarthCore.getInstance(), SPLATTER_REVERT_TICKS);
        }
        playImpactSounds(loc);
        spawnSplatter(loc);
        bPlayer.addCooldown(this);
        remove();
    }

    private void playImpactSounds(final Location loc) {
        final ThreadLocalRandom rand = ThreadLocalRandom.current();
        loc.getWorld().playSound(loc,
                rand.nextBoolean() ? Sound.ENTITY_FIREWORK_ROCKET_BLAST : Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR,
                1.5f, 0.8f);
        loc.getWorld().playSound(loc,
                rand.nextBoolean() ? Sound.ENTITY_FIREWORK_ROCKET_TWINKLE : Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
                1.2f, 1.0f);
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.5f);
    }

    private void spawnSplatter(final Location loc) {
        final World world = loc.getWorld();

        world.spawnParticle(Particle.LAVA, loc, 25, 1.5, 1.0, 1.5, 0.4);
        world.spawnParticle(Particle.FLAME, loc, 20, 1.5, 1.0, 1.5, 0.18);
        world.spawnParticle(Particle.SMOKE, loc, 20, 1.5, 1.0, 1.5, 0.1);
        world.spawnParticle(Particle.FIREWORK, loc, 30, 2.0, 1.5, 2.0, 0.25);

        // FallingBlock debris is purely visual
        final List<FallingBlock> spawnedFbs = new ArrayList<>();
        for (int i = 0; i < SPLATTER_FB_COUNT; i++) {
            final Material mat = random.nextBoolean() ? Material.MAGMA_BLOCK : Material.NETHERRACK;
            final Location fbLoc = loc.clone().add(
                    (random.nextDouble() - 0.5) * 0.6,
                    random.nextDouble() * 0.5,
                    (random.nextDouble() - 0.5) * 0.6
            );
            final FallingBlock fb = world.spawnFallingBlock(fbLoc, mat.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            fb.setVelocity(randomExplosiveVector(1.2 + random.nextDouble() * 0.5));
            spawnedFbs.add(fb);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final FallingBlock fb : spawnedFbs) {
                    if (!fb.isDead()) {
                        fb.remove();
                    }
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), 60L);

        final List<TempBlock> splatterBlocks = new ArrayList<>();
        final BlockData lavaData = createSourceLavaData();

        for (int i = 0; i < SPLATTER_BLOCK_COUNT; i++) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double r = 1.0 + random.nextDouble() * (SPLATTER_RADIUS - 1.0);
            final Location candidate = loc.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            final Block surface = findSurface(candidate);
            if (surface == null || !isTransparent(surface)) {
                continue;
            }
            if (GeneralMethods.isRegionProtectedFromBuild(this, surface.getLocation())) {
                continue;
            }

            final Material mat = pickSplatterMaterial();
            final BlockData data = (mat == Material.LAVA) ? lavaData : mat.createBlockData();
            splatterBlocks.add(new TempBlock(surface, data));
        }

        if (!splatterBlocks.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final TempBlock tb : splatterBlocks) {
                        tb.revertBlock();
                    }
                }
            }.runTaskLater(AranarthCore.getInstance(), SPLATTER_REVERT_TICKS);
        }
    }

    private void spawnChargingParticles() {
        final Location eye = player.getEyeLocation();
        final Location loc = eye.clone().add(eye.getDirection().normalize().multiply(1.0));
        final World world = loc.getWorld();
        world.spawnParticle(Particle.LARGE_SMOKE, loc, 3, 0.15, 0.15, 0.15, 0.01);
        world.spawnParticle(Particle.DUST, loc, 2, 0.1, 0.1, 0.1, 0,
                LAVA_PALETTE[random.nextInt(LAVA_PALETTE.length)]);
    }

    private void spawnConvertParticles(final double progress) {
        final World world = ballCenter.getWorld();
        final int count = 4 + (int) (progress * 6);
        for (int i = 0; i < count; i++) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double r = progress * CONVERT_RADIUS;
            final Location loc = ballCenter.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
            world.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0.1, 0.1, 0);
            world.spawnParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.01);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                    LAVA_PALETTE[random.nextInt(LAVA_PALETTE.length)]);
        }
    }

    /**
     * Spawns lava, flame, and dust particles orbiting the ball in a sphere.
     */
    private void spawnBallParticles() {
        final World world = ballCenter.getWorld();
        for (int i = 0; i < 6; i++) {
            // Random point on a sphere of radius ~1.5 centered on the ball
            final double theta = random.nextDouble() * Math.PI * 2;
            final double phi = Math.acos(2 * random.nextDouble() - 1);
            final double r = 1.3 + random.nextDouble() * 0.4;
            final Location loc = ballCenter.clone().add(
                    r * Math.sin(phi) * Math.cos(theta),
                    r * Math.cos(phi),
                    r * Math.sin(phi) * Math.sin(theta)
            );
            world.spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.02);
            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0,
                    LAVA_PALETTE[random.nextInt(LAVA_PALETTE.length)]);
        }
    }

    private BlockData createSourceLavaData() {
        final BlockData data = Material.LAVA.createBlockData();
        if (data instanceof final Levelled levelled) {
            levelled.setLevel(0);
        }
        return data;
    }

    private Block findGroundBlock(final Location loc) {
        for (int dy = 5; dy >= -5; dy--) {
            final Block here = loc.clone().add(0, dy, 0).getBlock();
            final Block above = loc.clone().add(0, dy + 1, 0).getBlock();
            if (!isTransparent(here) && !ElementalAbility.isAir(here.getType())
                    && isTransparent(above)) {
                return here;
            }
        }
        return null;
    }

    private Block findSurface(final Location loc) {
        for (int dy = 5; dy >= -5; dy--) {
            final Block here = loc.clone().add(0, dy, 0).getBlock();
            final Block below = loc.clone().add(0, dy - 1, 0).getBlock();
            if (isTransparent(here) && !ElementalAbility.isAir(below.getType()) && !isTransparent(below)) {
                return here;
            }
        }
        return null;
    }

    private Material pickSplatterMaterial() {
        return switch (random.nextInt(3)) {
            case 0 -> Material.MAGMA_BLOCK;
            case 1 -> Material.NETHERRACK;
            default -> Material.LAVA;
        };
    }

    private Vector randomExplosiveVector(final double speed) {
        final double theta = random.nextDouble() * Math.PI * 2;
        final double phi = random.nextDouble() * Math.PI / 2;
        return new Vector(
                Math.cos(theta) * Math.cos(phi) * speed,
                (Math.abs(Math.sin(phi)) + 0.3) * speed,
                Math.sin(theta) * Math.cos(phi) * speed
        );
    }

    public static boolean isBallLavaBlock(final Block block) {
        for (final MoltenBlast inst : ACTIVE_INSTANCES.values()) {
            if (inst.lavaBlockSet.contains(block)) {
                return true;
            }
            if (inst.convertLavaSet.contains(block)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static MoltenBlast getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    public void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    public void cancelInstantly() {
        remove();
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;
        super.remove();
        clearBall();
        clearConvertBlocks();
        ACTIVE_INSTANCES.remove(player.getUniqueId());
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
        return ballCenter != null ? ballCenter : player.getLocation();
    }

    @Override
    public String getName() {
        return "MoltenBlast";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        ACTIVE_INSTANCES.clear();
        RECENT_EARTHSMASH_SNEAK.clear();
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
        return new MoltenBlast(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("EarthSmash", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("LavaFlow", ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public void handleCollision(final Collision collision) {
    }

    @Override
    public String getDescription() {
        return "Create a large molten sphere of magma and lava from the ground, splattering upon impact.\n" +
                ChatUtils.translateToColor("&fUsage: EarthSmash (Tap Sneak) > LavaFlow (Hold sneak until particles) > " +
                        "LavaFlow (Release Sneak) > LavaThrow (Hold Sneak to control, then Left-Click to Fire)");
    }
}
