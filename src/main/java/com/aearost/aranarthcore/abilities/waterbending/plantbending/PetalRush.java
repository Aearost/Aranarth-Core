package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class PetalRush extends PlantAbility implements AddonAbility {

    public enum Phase {CHARGING, RUSH}

    @Attribute(Attribute.COOLDOWN)
    private long cooldownPerFlower;
    @Attribute(Attribute.DAMAGE)
    private double damagePerFlower;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute("SourceRange")
    private double sourceRange;
    @Attribute("MaxFlowers")
    private int maxFlowers;
    @Attribute(Attribute.RANGE)
    private double range;

    private double sourceAngle;
    private double rangePerFlower;
    private double baseRange;
    private double startRadius;
    private double minConeAngle;
    private double maxConeAngle;
    private long travelDuration;
    private long rushDuration;
    private long lineDuration;
    private double lineLength;
    private int linePetals;
    private float linePetalSize;
    private double windAmplitude;
    private double windFrequency;
    private long fadeDuration;
    private int petalsPerFlower;
    private float heldPetalSize;
    private int rushPetalsPerFlower;
    private double minPetalSize;
    private double maxPetalSize;
    private double maxPetalSizeMultiplier;

    private Phase phase;
    private long chargeStartTime;
    private boolean chargeNotified;

    private Location origin;
    private Vector direction;
    private Vector coneRight;
    private Vector coneUp;
    private double coneAngle;
    private long rushStartTime;
    private boolean finalDamageApplied;
    private int nextSoundLayer;
    private int soundLayers;
    private float soundVolumeFactor;
    private long cooldown;
    private double damage;
    private final Set<UUID> hitEntities = new HashSet<>();

    private static final double PETAL_ORBIT_RADIUS = 1.0;
    private static final double PETAL_BACK_ARC = Math.toRadians(100);
    private static final double PETAL_PULL_FACTOR = 0.2;
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    private static final double RUSH_START_DISTANCE = 1.0;
    private static final double RUSH_COLLISION_STEP = 0.5;
    private static final double HIT_MARGIN = 0.5;
    private static final long[] RUSH_SOUND_DELAYS = {0, 60, 130, 210, 300, 380, 470, 560, 650};
    private static final float[] RUSH_SOUND_PITCHES = {1.3f, 0.8f, 1.6f, 1.0f, 0.65f, 1.45f, 0.9f, 1.15f, 0.7f};
    private static final float RUSH_SOUND_MIN_VOLUME_FACTOR = 0.3f;

    private final List<SourcedFlower> sourcedFlowers = new ArrayList<>();
    private final List<Petal> petals = new ArrayList<>();
    private final List<RushPetal> rushPetals = new ArrayList<>();
    private final List<RushLine> rushLines = new ArrayList<>();

    private static final Map<UUID, PetalRush> activeInstances = new HashMap<>();
    private final Random random = new Random();

    /**
     * A flower that was turned to air, reverted when the ability ends
     */
    private static class SourcedFlower {
        final List<TempBlock> tempBlocks = new ArrayList<>();
        final Color color;

        SourcedFlower(Color color) {
            this.color = color;
        }

        void revert() {
            // Revert in reverse order so the upper half of a tall flower is restored first
            for (int i = tempBlocks.size() - 1; i >= 0; i--) {
                TempBlock tempBlock = tempBlocks.get(i);
                if (!tempBlock.isReverted()) {
                    tempBlock.revertBlock();
                }
            }
            tempBlocks.clear();
        }
    }

    /*
     * A petal drifting from its flower towards the caster, then swaying behind them.
     */
    private static class Petal {
        final Location position;
        final Particle.DustOptions dust;
        final double angleOffset;
        final double heightOffset;

        Petal(Location start, Color color, float size, double angleOffset, double heightOffset) {
            this.position = start.clone();
            this.dust = new Particle.DustOptions(color, size);
            this.angleOffset = angleOffset;
            this.heightOffset = heightOffset;
        }
    }

    /**
     * A path through the cone that stops at the first solid block, optionally winding as if blown by the wind.
     */
    private static class RushPath {
        final Location start;
        final Vector direction;
        final Vector windAxisA;
        final Vector windAxisB;
        final double windAmplitude;
        final double windFrequency;
        final double windPhaseA;
        final double windPhaseB;
        double clearDistance;

        RushPath(Location start, Vector direction, Vector windAxisA, Vector windAxisB, double windAmplitude,
                 double windFrequency, double windPhaseA, double windPhaseB, double range) {
            this.start = start;
            this.direction = direction;
            this.windAxisA = windAxisA;
            this.windAxisB = windAxisB;
            this.windAmplitude = windAmplitude;
            this.windFrequency = windFrequency;
            this.windPhaseA = windPhaseA;
            this.windPhaseB = windPhaseB;
            while (clearDistance < range
                    && !getPosition(clearDistance + RUSH_COLLISION_STEP).getBlock().getType().isSolid()) {
                clearDistance += RUSH_COLLISION_STEP;
            }
        }

        Location getPosition(double distance) {
            Location position = start.clone().add(direction.clone().multiply(distance));
            double sway = windAmplitude * Math.min(1.0, distance / 2);
            return position
                    .add(windAxisA.clone().multiply(sway * Math.sin(distance * windFrequency + windPhaseA)))
                    .add(windAxisB.clone().multiply(sway * Math.cos(distance * windFrequency * 0.7 + windPhaseB)));
        }
    }

    /**
     * A petal flowing along its path through the cone, looping back to the base when it reaches the end.
     */
    private static class RushPetal {
        final RushPath path;
        final Particle.DustOptions dust;
        final double flowOffset;

        RushPetal(RushPath path, Color color, float size, double flowOffset) {
            this.path = path;
            this.dust = new Particle.DustOptions(color, size);
            this.flowOffset = flowOffset;
        }
    }

    /**
     * A distinct line of one flower's petals that dashes through the cone once.
     */
    private static class RushLine {
        final RushPath path;
        final Particle.DustOptions dust;
        final long launchDelay;

        RushLine(RushPath path, Color color, float size, long launchDelay) {
            this.path = path;
            this.dust = new Particle.DustOptions(color, size);
            this.launchDelay = launchDelay;
        }
    }

    public PetalRush(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldownPerFlower = AbilityConfig.getLong("PetalRush.CooldownPerFlower");
        damagePerFlower = AbilityConfig.getDouble("PetalRush.DamagePerFlower");
        chargeDuration = AbilityConfig.getLong("PetalRush.ChargeDuration");
        sourceRange = AbilityConfig.getDouble("PetalRush.SourceRange");
        sourceAngle = AbilityConfig.getDouble("PetalRush.SourceAngle");
        maxFlowers = AbilityConfig.getInt("PetalRush.MaxFlowers");
        rangePerFlower = AbilityConfig.getDouble("PetalRush.RangePerFlower");
        baseRange = AbilityConfig.getDouble("PetalRush.BaseRange");
        startRadius = AbilityConfig.getDouble("PetalRush.StartRadius");
        minConeAngle = AbilityConfig.getDouble("PetalRush.MinConeAngle");
        maxConeAngle = AbilityConfig.getDouble("PetalRush.MaxConeAngle");
        travelDuration = AbilityConfig.getLong("PetalRush.TravelDuration");
        rushDuration = AbilityConfig.getLong("PetalRush.RushDuration");
        lineDuration = AbilityConfig.getLong("PetalRush.LineDuration");
        lineLength = AbilityConfig.getDouble("PetalRush.LineLength");
        linePetals = AbilityConfig.getInt("PetalRush.LinePetals");
        linePetalSize = (float) AbilityConfig.getDouble("PetalRush.LinePetalSize");
        windAmplitude = AbilityConfig.getDouble("PetalRush.WindAmplitude");
        windFrequency = AbilityConfig.getDouble("PetalRush.WindFrequency");
        fadeDuration = AbilityConfig.getLong("PetalRush.FadeDuration");
        petalsPerFlower = AbilityConfig.getInt("PetalRush.PetalsPerFlower");
        heldPetalSize = (float) AbilityConfig.getDouble("PetalRush.HeldPetalSize");
        rushPetalsPerFlower = AbilityConfig.getInt("PetalRush.RushPetalsPerFlower");
        minPetalSize = AbilityConfig.getDouble("PetalRush.MinPetalSize");
        maxPetalSize = AbilityConfig.getDouble("PetalRush.MaxPetalSize");
        maxPetalSizeMultiplier = AbilityConfig.getDouble("PetalRush.MaxPetalSizeMultiplier");

        phase = Phase.CHARGING;
        chargeStartTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    /**
     * Sources the flower the player is looking at while charging.
     */
    public void onLeftClick() {
        if (phase != Phase.CHARGING || sourcedFlowers.size() >= maxFlowers) {
            return;
        }

        Block block = findTargetedFlower();
        if (block == null) {
            return;
        }
        Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);

        Color color = AranarthBendingUtils.getFlowerPetalColor(block.getType());
        SourcedFlower flower = new SourcedFlower(color);

        // Tall flowers are cleared as a whole so no floating half is left behind
        if (block.getBlockData() instanceof Bisected bisected) {
            Block lower = bisected.getHalf() == Bisected.Half.TOP ? block.getRelative(BlockFace.DOWN) : block;
            Block upper = lower.getRelative(BlockFace.UP);
            Material flowerType = block.getType();
            if (lower.getType() == flowerType && !TempBlock.isTempBlock(lower)) {
                flower.tempBlocks.add(new TempBlock(lower, Material.AIR.createBlockData()));
            }
            if (upper.getType() == flowerType && !TempBlock.isTempBlock(upper)) {
                flower.tempBlocks.add(new TempBlock(upper, Material.AIR.createBlockData()));
            }
        } else {
            flower.tempBlocks.add(new TempBlock(block, Material.AIR.createBlockData()));
        }
        sourcedFlowers.add(flower);

        World world = block.getWorld();
        Particle.DustOptions sourceDust = new Particle.DustOptions(color, heldPetalSize);
        world.spawnParticle(Particle.DUST, blockCenter, 12, 0.3, 0.3, 0.3, 0, sourceDust);
        world.playSound(blockCenter, Sound.BLOCK_PINK_PETALS_BREAK, 1.0f, 1.2f);

        for (int i = 0; i < petalsPerFlower; i++) {
            double angleOffset = random.nextDouble() * Math.PI * 2;
            double heightOffset = 0.6 + random.nextDouble() * 0.8;
            Location start = blockCenter.clone().add(
                    (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.6);
            petals.add(new Petal(start, color, heldPetalSize, angleOffset, heightOffset));
        }
    }

    /**
     * Fires the cone when released with a full charge and at least one flower.
     */
    public void onSneakRelease() {
        if (phase != Phase.CHARGING) {
            return;
        }
        if (!isCharged() || sourcedFlowers.isEmpty()) {
            remove();
            return;
        }

        int flowerCount = sourcedFlowers.size();
        double flowerFraction = maxFlowers <= 1 ? 1.0 : (double) (flowerCount - 1) / (maxFlowers - 1);
        cooldown = flowerCount * cooldownPerFlower;
        damage = flowerCount * damagePerFlower;
        range = baseRange + flowerCount * rangePerFlower;
        coneAngle = minConeAngle + (maxConeAngle - minConeAngle) * flowerFraction;
        float petalSize = (float) (minPetalSize + (maxPetalSize - minPetalSize) * flowerFraction);
        soundLayers = Math.min(flowerCount, RUSH_SOUND_DELAYS.length);
        soundVolumeFactor = RUSH_SOUND_MIN_VOLUME_FACTOR + (1 - RUSH_SOUND_MIN_VOLUME_FACTOR) * (float) flowerFraction;
        direction = player.getEyeLocation().getDirection().normalize();
        origin = player.getEyeLocation().subtract(0, 0.3, 0);
        Vector reference = Math.abs(direction.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        coneRight = direction.getCrossProduct(reference).normalize();
        coneUp = coneRight.getCrossProduct(direction).normalize();
        createRushPetals(flowerCount * rushPetalsPerFlower, petalSize);
        createRushLines();

        rushStartTime = System.currentTimeMillis();
        petals.clear();
        phase = Phase.RUSH;
        bPlayer.addCooldown(this);

    }

    /**
     * Cancels the ability without a cooldown if the player switches slots while charging.
     */
    public void cancelFromSlotChange() {
        if (phase == Phase.CHARGING) {
            remove();
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (phase) {
            case CHARGING -> progressCharging();
            case RUSH -> progressRush();
        }
    }

    private void progressCharging() {
        // Fallback in case the sneak release event was missed
        if (!player.isSneaking()) {
            onSneakRelease();
            return;
        }

        if (!chargeNotified && isCharged()) {
            chargeNotified = true;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.0f, 1.4f);
        }
        if (!isCharged()) {
            drawChargeSwirl();
        }
        tickPetals();
    }

    private void progressRush() {
        if (!player.getWorld().equals(origin.getWorld())) {
            remove();
            return;
        }

        long elapsed = System.currentTimeMillis() - rushStartTime;
        playRushSounds(elapsed);
        if (elapsed >= rushDuration + fadeDuration) {
            remove();
            return;
        }

        drawRushPetals(elapsed);
        drawRushLines(elapsed);
        if (elapsed < travelDuration) {
            tickDamage(getFront(elapsed));
        } else if (!finalDamageApplied) {
            // Final pass at full range so entities near the end are not skipped between ticks
            finalDamageApplied = true;
            tickDamage(range);
        }
    }

    /**
     * Gets how far petals have travelled after the given time, easing out so they slow down as they spread.
     */
    private double getFront(long age) {
        double fraction = Math.min(1.0, (double) age / travelDuration);
        double eased = 1 - (1 - fraction) * (1 - fraction);
        return RUSH_START_DISTANCE + Math.max(0, range - RUSH_START_DISTANCE) * eased;
    }

    /**
     * Gets when a flower's line launches, spaced so that the last line arrives as the rush ends.
     */
    private long getLineDelay(int flowerIndex) {
        int flowerCount = sourcedFlowers.size();
        return flowerCount <= 1 ? 0 : flowerIndex * Math.max(0, rushDuration - lineDuration) / (flowerCount - 1);
    }

    /**
     * Layers the breeze sounds at various pitches, spread apart in start time.
     */
    private void playRushSounds(long elapsed) {
        while (nextSoundLayer < soundLayers && elapsed >= RUSH_SOUND_DELAYS[nextSoundLayer]) {
            boolean isSlide = nextSoundLayer % 2 == 0;
            Sound sound = isSlide ? Sound.ENTITY_BREEZE_SLIDE : Sound.ENTITY_BREEZE_IDLE_GROUND;
            float volume = (isSlide ? 2.0f : 1.0f) * soundVolumeFactor;
            origin.getWorld().playSound(origin, sound, volume, RUSH_SOUND_PITCHES[nextSoundLayer]);
            nextSoundLayer++;
        }
    }

    private void drawChargeSwirl() {
        double chargeFraction = (double) (System.currentTimeMillis() - chargeStartTime) / chargeDuration;
        double angle = getBackAngle() + Math.sin(chargeFraction * Math.PI * 6) * PETAL_BACK_ARC;
        Location loc = player.getLocation().add(
                Math.cos(angle) * PETAL_ORBIT_RADIUS, 0.1 + chargeFraction * 1.6, Math.sin(angle) * PETAL_ORBIT_RADIUS);
        player.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 1, 0, 0, 0, 0);
    }

    /**
     * Keeps the sourced petals swaying in an arc behind the player so they only show in peripheral vision.
     */
    private void tickPetals() {
        if (petals.isEmpty()) {
            return;
        }
        World world = player.getWorld();
        Location base = player.getLocation();
        double time = System.currentTimeMillis() / 1000.0;
        double backAngle = getBackAngle();

        for (Petal petal : petals) {
            if (!world.equals(petal.position.getWorld())) {
                petal.position.setWorld(world);
            }
            double angle = backAngle + Math.sin(time * 1.5 + petal.angleOffset) * PETAL_BACK_ARC;
            double bob = Math.sin(time * 3 + petal.angleOffset) * 0.15;
            Location target = base.clone().add(
                    Math.cos(angle) * PETAL_ORBIT_RADIUS,
                    petal.heightOffset + bob,
                    Math.sin(angle) * PETAL_ORBIT_RADIUS);

            Vector pull = target.toVector().subtract(petal.position.toVector()).multiply(PETAL_PULL_FACTOR);
            petal.position.add(pull);
            world.spawnParticle(Particle.DUST, petal.position, 1, 0, 0, 0, 0, petal.dust);
        }
    }

    /**
     * Spreads paths evenly across a circle of startRadius, each angling outward the further it starts from the
     * center, so the rush has a flat base instead of a sharp point.
     */
    private RushPath createPath(int index, int count, double amplitude) {
        double spread = Math.sqrt((index + 0.5) / count);
        double theta = spread * Math.toRadians(coneAngle);
        double phi = index * GOLDEN_ANGLE;
        Vector outward = coneRight.clone().multiply(Math.cos(phi)).add(coneUp.clone().multiply(Math.sin(phi)));
        Vector pathDirection = direction.clone().multiply(Math.cos(theta))
                .add(outward.clone().multiply(Math.sin(theta)));
        Location start = origin.clone().add(outward.multiply(spread * startRadius));
        return new RushPath(start, pathDirection, coneRight, coneUp, amplitude, windFrequency,
                random.nextDouble() * Math.PI * 2, random.nextDouble() * Math.PI * 2, range);
    }

    private void createRushPetals(int petalCount, float petalSize) {
        for (int i = 0; i < petalCount; i++) {
            Color color = sourcedFlowers.get(i % sourcedFlowers.size()).color;
            float size = petalSize * (float) (1 + random.nextDouble() * (maxPetalSizeMultiplier - 1));
            rushPetals.add(new RushPetal(createPath(i, petalCount, 0), color, size, random.nextDouble()));
        }
    }

    private void createRushLines() {
        int flowerCount = sourcedFlowers.size();
        for (int i = 0; i < flowerCount; i++) {
            RushPath path = createPath(i, flowerCount, windAmplitude);
            rushLines.add(new RushLine(path, sourcedFlowers.get(i).color, linePetalSize, getLineDelay(i)));
        }
    }

    /**
     * Keeps the petals flowing through the cone, which grows to its full range and fades when the rush ends.
     */
    private void drawRushPetals(long elapsed) {
        World world = origin.getWorld();
        double front = getFront(elapsed);
        double fadeFactor = 1.0 - (double) (elapsed - rushDuration) / fadeDuration;
        for (RushPetal petal : rushPetals) {
            double distance = (petal.flowOffset + (double) elapsed / travelDuration) % 1.0 * range;
            if (distance > front || distance > petal.path.clearDistance || random.nextDouble() > fadeFactor) {
                continue;
            }
            world.spawnParticle(Particle.DUST, petal.path.getPosition(distance), 1, 0, 0, 0, 0, petal.dust);
        }
    }

    /**
     * Dashes each launched line through the cone as a trail of petals behind its head.
     */
    private void drawRushLines(long elapsed) {
        World world = origin.getWorld();
        for (RushLine line : rushLines) {
            long age = elapsed - line.launchDelay;
            if (age < 0 || age >= lineDuration) {
                continue;
            }
            double head = range * age / lineDuration;
            for (int i = 0; i < linePetals; i++) {
                double distance = head - i * lineLength / linePetals;
                if (distance < 0) {
                    break;
                }
                if (distance <= line.path.clearDistance) {
                    world.spawnParticle(Particle.DUST, line.path.getPosition(distance), 1, 0, 0, 0, 0, line.dust);
                }
            }
        }
    }

    /**
     * Damages every mob and player once when the front of the cone reaches them.
     */
    private void tickDamage(double front) {
        double reach = front + HIT_MARGIN;
        for (Entity entity : origin.getWorld().getNearbyEntities(origin, reach, reach, reach)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand
                    || entity.getUniqueId().equals(player.getUniqueId())
                    || hitEntities.contains(entity.getUniqueId())) {
                continue;
            }
            if (!isCaughtInCone(living, reach)) {
                continue;
            }
            if (living instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                continue;
            }
            hitEntities.add(entity.getUniqueId());
            // Targets are only hit once, so recent damage from another source must not absorb the hit
            living.setNoDamageTicks(0);
            DamageHandler.damageEntity(living, damage, this);
        }
    }

    /**
     * Checks the entity's feet, center and eyes, so that large mobs are caught if any part of them is in the cone.
     */
    private boolean isCaughtInCone(LivingEntity living, double reach) {
        Location feet = living.getLocation();
        Location center = feet.clone().add(0, living.getHeight() / 2, 0);
        double spreadPerBlock = Math.tan(Math.toRadians(coneAngle));

        for (Location point : List.of(center, living.getEyeLocation(), feet)) {
            Vector toPoint = point.toVector().subtract(origin.toVector());
            double distance = toPoint.length();
            double forward = toPoint.dot(direction);
            if (distance > reach || forward < 0) {
                continue;
            }
            // The cone is startRadius wide at its base and widens the further forward the point is
            double sideways = Math.sqrt(Math.max(0, distance * distance - forward * forward));
            if (sideways > startRadius + forward * spreadPerBlock + HIT_MARGIN) {
                continue;
            }
            if (isPathClear(origin, point)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCharged() {
        return System.currentTimeMillis() - chargeStartTime >= chargeDuration;
    }

    /**
     * Gets the horizontal angle pointing directly behind the player.
     */
    private double getBackAngle() {
        Vector dir = player.getLocation().getDirection();
        return Math.atan2(-dir.getZ(), -dir.getX());
    }

    /**
     * Finds the sourceable flower closest to the player's look direction within sourceAngle and sourceRange.
     */
    private Block findTargetedFlower() {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();
        double minDot = Math.cos(Math.toRadians(sourceAngle));
        double rangeSq = sourceRange * sourceRange;
        int r = (int) Math.ceil(sourceRange);
        Block eyeBlock = eye.getBlock();

        Block best = null;
        double bestDot = minDot;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Block candidate = eyeBlock.getRelative(x, y, z);
                    if (!AranarthUtils.isFlower(candidate.getType())) {
                        continue;
                    }
                    Location candidateCenter = candidate.getLocation().add(0.5, 0.5, 0.5);
                    Vector toFlower = candidateCenter.toVector().subtract(eye.toVector());
                    double distSq = toFlower.lengthSquared();
                    if (distSq > rangeSq || distSq < 0.01) {
                        continue;
                    }
                    double dot = toFlower.clone().normalize().dot(look);
                    if (dot <= bestDot) {
                        continue;
                    }
                    if (TempBlock.isTempBlock(candidate) || !isPathClear(eye, candidateCenter)
                            || !AranarthBendingUtils.canBendBlock(player, candidate)
                            || GeneralMethods.isRegionProtectedFromBuild(this, candidate.getLocation())) {
                        continue;
                    }
                    best = candidate;
                    bestDot = dot;
                }
            }
        }
        return best;
    }

    /**
     * Checks that no solid block sits between the two locations.
     */
    private boolean isPathClear(Location from, Location to) {
        Vector path = to.toVector().subtract(from.toVector());
        double length = path.length();
        if (length < 0.01) {
            return true;
        }
        Block target = to.getBlock();
        Vector step = path.normalize().multiply(0.25);
        Location point = from.clone();
        for (double d = 0; d < length; d += 0.25) {
            Block block = point.getBlock();
            if (block.equals(target)) {
                return true;
            }
            if (block.getType().isSolid()) {
                return false;
            }
            point.add(step);
        }
        return true;
    }

    private void revertFlowers() {
        for (SourcedFlower flower : sourcedFlowers) {
            flower.revert();
        }
        sourcedFlowers.clear();
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static PetalRush getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public void remove() {
        super.remove();
        revertFlowers();
        petals.clear();
        rushPetals.clear();
        rushLines.clear();
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
    public String getName() {
        return "PetalRush";
    }

    @Override
    public Location getLocation() {
        if (origin != null) {
            return origin;
        }
        return player.getLocation();
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
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
        return "Draw the petals from up to 9 nearby flowers and send them rushing forward in front of you, " +
                "rapidly slashing through any entity caught within. Each flower sourced extends the reach of the rush, but also extends the cooldown.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak > Left-Click flowers > Release Sneak");
    }

    public Phase getPhase() {
        return phase;
    }
}
