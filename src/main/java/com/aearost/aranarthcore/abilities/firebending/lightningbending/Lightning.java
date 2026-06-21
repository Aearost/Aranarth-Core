package com.aearost.aranarthcore.abilities.firebending.lightningbending;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.attribute.markers.DayNightFactor;
import com.projectkorra.projectkorra.firebending.FireJet;
import com.projectkorra.projectkorra.region.RegionProtection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.LightningAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;

public class Lightning extends LightningAbility implements AddonAbility {

    public enum State {
        START, STRIKE, MAINBOLT, CHAIN
    }

    private static final int POINT_GENERATION = 5;

    @Attribute("Charged")
    private boolean charged;
    private boolean hitWater;
    private boolean hitIce;
    private boolean hitCopper;
    private boolean selfHitWater;
    private boolean selfHitClose;
    private boolean allowOnFireJet;
    private boolean chainLightningRods;
    private boolean startChaining;
    private boolean grounded;
    @Attribute("ArcOnIce")
    private boolean arcOnIce;
    @Attribute("ArcOnCopper")
    private boolean arcOnCopper;
    private int waterArcs;
    @Attribute("MaxCopperArcs")
    private int maxCopperArcs;
    private int copperArcs;
    private int copperChains;
    @Attribute(Attribute.RANGE) @DayNightFactor
    private double range;
    @Attribute(Attribute.CHARGE_DURATION) @DayNightFactor(invert = true)
    private double chargeTime;
    @Attribute("SubArcChance")
    private double subArcChance;
    @Attribute(Attribute.DAMAGE) @DayNightFactor
    private double damage;
    @Attribute("MaxChainArcs")
    private double maxChainArcs;
    @Attribute("Chain" + Attribute.RANGE) @DayNightFactor
    private double chainRange;
    @Attribute("WaterArc" + Attribute.RANGE) @DayNightFactor
    private double waterArcRange;
    @Attribute("Conductivity" + Attribute.RANGE) @DayNightFactor
    private double conductivityRange;
    @Attribute("ChainArcChance")
    private double chainArcChance;
    @Attribute("StunChance")
    private double stunChance;
    @Attribute("Stun" + Attribute.DURATION)
    private double stunDuration;
    @Attribute("MaxArcAngle")
    private double maxArcAngle;
    private double particleRotation;
    @Attribute(Attribute.COOLDOWN) @DayNightFactor(invert = true)
    private long cooldown;
    private State state;
    private Location origin;
    private Location destination;
    private Location chainOrigin, chainDestination;
    private Arc currentCopperChainArc;
    private ArrayList<Entity> affectedEntities;
    private ArrayList<Arc> arcs;
    private ArrayList<BukkitRunnable> tasks;
    private ArrayList<Location> locations;
    private Block[] chargedCopperBlocks;

    public Lightning(final Player player) {
        super(player);

        if (!this.bPlayer.canBend(this)) {
            return;
        }
        if (hasAbility(player, Lightning.class)) {
            if (!getAbility(player, Lightning.class).isCharged()) {
                return;
            }
        }

        this.charged = false;
        this.hitWater = false;
        this.hitIce = false;
        this.hitCopper = false;
        this.state = State.START;
        this.affectedEntities = new ArrayList<>();
        this.arcs = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.locations = new ArrayList<>();

        this.damage = 10.0;
        this.range = 20.0;
        this.chargeTime = 3000;
        this.cooldown = 10000;
        this.stunChance = 0.3;
        this.stunDuration = 30.0;
        this.maxArcAngle = 2.5;
        this.subArcChance = 0.00125;
        this.chainRange = 6.0;
        this.chainArcChance = 0.5;
        this.maxChainArcs = 2;
        this.waterArcs = 4;
        this.waterArcRange = 20.0;
        this.conductivityRange = 5.0;
        this.maxCopperArcs = 8;
        this.selfHitWater = true;
        this.selfHitClose = true;
        this.arcOnIce = true;
        this.arcOnCopper = true;
        this.allowOnFireJet = true;
        this.chainLightningRods = true;

        this.chargedCopperBlocks = new Block[this.maxCopperArcs];

        this.start();
    }

    /**
     * Damages an entity and delegates stun/electrocution to AranarthBendingUtils.
     *
     * @param lent The LivingEntity that is being damaged
     */
    public void electrocute(final LivingEntity lent) {
        playLightningbendingSound(lent.getLocation());
        playLightningbendingSound(this.player.getLocation());
        playLightningbendingHitSound(lent.getLocation());
        playLightningbendingHitSound(this.player.getLocation());
        DamageHandler.damageEntity(lent, this.damage, this);
        AranarthBendingUtils.applyElectrocution(lent, (long) this.stunDuration, this.stunChance);
    }

    /**
     * Checks if a block is transparent, also considers the arcOnIce setting.
     *
     * @param player the player that is viewing the block
     * @param block  the block
     * @return true if the block is transparent
     */
    private boolean isTransparentForLightning(final Player player, final Block block) {
        if (this.isTransparent(block)) {
            if (RegionProtection.isRegionProtected(this, block.getLocation())) {
                return false;
            } else if (isIce(block)) {
                return this.arcOnIce;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Charges lightning rods. If chainLightningRods is enabled, it will power all lightning rods
     * below the block that was hit.
     *
     * @param block the block that was struck
     */
    private void powerLightningRods(final Block block) {
        if (isLightningRod(block)) {
            block.getWorld().spawnParticle(Particle.valueOf("ELECTRIC_SPARK"), block.getLocation().clone().add(0.5, 0.5, 0.5), 6, 0.125, 0.125, 0.125, 0.05);

            List<Block> blocks = new ArrayList<>();
            Block down = block.getRelative(BlockFace.DOWN);

            if (this.chainLightningRods) {
                if (isLightningRod(down)) {
                    while (isLightningRod(down)) {
                        updateLightningRod(down, true);
                        blocks.add(down);
                        down = down.getRelative(BlockFace.DOWN);
                    }
                } else {
                    updateLightningRod(block, true);
                }
            } else {
                updateLightningRod(block, true);
            }
            Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
                if (blocks.isEmpty()) {
                    updateLightningRod(block, false);
                    return;
                }
                for (Block powerable : blocks) {
                    updateLightningRod(powerable, false);
                }
            }, 8);
        }
    }

    private boolean rodIsGrounded(final Block block) {
        if (!isLightningRod(block)) {
            return false;
        }
        Block down = block.getRelative(BlockFace.DOWN);

        if (GeneralMethods.isSolid(down) && !isLightningRod(down)) {
            return true;
        } else if (isLightningRod(down)) {
            while (isLightningRod(down)) {
                down = down.getRelative(BlockFace.DOWN);
            }
            if (GeneralMethods.isSolid(down)) {
                return true;
            }
        }
        return false;
    }

    private void updateLightningRod(final Block block, final boolean powered) {
        Powerable powerable = (Powerable) block.getBlockData();
        powerable.setPowered(powered);
        block.setBlockData(powerable);
        if (!powered) {
            for (Block nearby : GeneralMethods.getBlocksAroundPoint(block.getLocation(), 1.5)) {
                if (nearby.getBlockData() instanceof AnaloguePowerable redstone) {
                    redstone.setPower(0);
                    nearby.setBlockData(redstone);
                }
            }
        }
    }

    /**
     * Recursively graphs out nearby copper blocks when first hit.
     *
     * @param hit Block that was hit/block that was detected recursively
     */
    private void setupCopperGraph(final Block hit) {
        if (this.copperArcs < this.maxCopperArcs && !this.grounded) {
            this.chargedCopperBlocks[this.copperArcs] = hit;
            hit.setMetadata("chargedcopper", new FixedMetadataValue(ProjectKorra.plugin, 0));
            this.copperArcs++;

            if (this.rodIsGrounded(hit)) {
                this.grounded = true;
                return;
            }
            List<Block> rods = GeneralMethods.getBlocksAroundPoint(hit.getLocation(), this.conductivityRange).stream().filter(b -> isLightningRod(b) && !b.hasMetadata("chargedcopper")).toList();

            if (!rods.isEmpty()) {
                rods.forEach(rod -> this.setupCopperGraph(rod));
            }
            if (!this.grounded) {
                for (Block block : GeneralMethods.getBlocksAroundPoint(hit.getLocation(), this.conductivityRange)) {
                    if (this.copperArcs >= this.maxCopperArcs) {
                        break;
                    }
                    if (isCopper(block) && !block.hasMetadata("chargedcopper")) {
                        this.setupCopperGraph(block);
                    }
                }
            }
        }
    }

    /**
     * Chains lightning arcs between the mapped out copper blocks.
     *
     * @param location current location of the arc
     */
    private void chainCopperLightning(final Location location) {
        if (this.currentCopperChainArc == null && this.copperChains + 1 < this.copperArcs) {
            Block originBlock = this.chargedCopperBlocks[this.copperChains], destinationBlock = this.chargedCopperBlocks[this.copperChains + 1];

            this.chainOrigin = originBlock.getLocation().clone().add(0.5, 0.65, 0.5);
            this.chainDestination = destinationBlock.getLocation().clone().add(0.5, 0.8, 0.5);

            Arc arc = new Arc(this.chainOrigin, this.chainDestination);
            arc.generatePoints(POINT_GENERATION);

            this.arcs.add(arc);

            this.currentCopperChainArc = arc;
        } else if (this.currentCopperChainArc != null) {
            if (location.getBlock().equals(this.chargedCopperBlocks[this.copperChains + 1]) || location.distanceSquared(this.chainDestination) <= 0.8 * 0.8) {
                this.copperChains++;
                this.currentCopperChainArc = null;
            }
            if (!this.isTransparentForLightning(player, location.getBlock()) && !isCopper(location.getBlock()) && !isLightningRod(location.getBlock())) {
                this.remove();
                return;
            }
        } else if (this.copperChains == this.copperArcs) {
            this.remove();
            return;
        }
    }

    /**
     * Checks if a block is of copper (minus lightning rods).
     *
     * @param block the block to check
     * @return true if the block is a copper type
     */
    private boolean isCopper(final Block block) {
        return block.getType().name().contains("COPPER");
    }

    /**
     * Checks if the block is a lightning rod.
     *
     * @param block the block to check
     * @return true if the block is a lightning rod
     */
    private boolean isLightningRod(final Block block) {
        return block.getType() == Material.LIGHTNING_ROD;
    }

    @Override
    public void progress() {
        if (this.player.isDead() || !this.player.isOnline()) {
            this.removeWithTasks();
            return;
        } else if (!this.bPlayer.canBendIgnoreCooldowns(this)) {
            this.remove();
            return;
        } else if (CoreAbility.hasAbility(player, FireJet.class) && !allowOnFireJet) {
            this.removeWithTasks();
            return;
        }

        this.locations.clear();

        if (this.state == State.START) {
            if (this.bPlayer.isOnCooldown(this)) {
                this.remove();
                return;
            } else if (System.currentTimeMillis() - this.getStartTime() > this.chargeTime) {
                this.charged = true;
            }

            if (this.charged) {
                if (this.player.isSneaking()) {
                    final Location loc = this.player.getEyeLocation().add(this.player.getEyeLocation().getDirection().normalize().multiply(1.2));
                    loc.add(0, 0.3, 0);
                    Particle.DustOptions chargedDust = (ThreadLocalRandom.current().nextBoolean())
                            ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                            : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0.2, 0.2, 0.2, 0, chargedDust);
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                    emitFirebendingLight(loc);
                    if (ThreadLocalRandom.current().nextDouble() < .2) {
                        playLightningbendingChargingSound(loc);
                    }
                } else {
                    this.state = State.MAINBOLT;
                    this.bPlayer.addCooldown(this);
                    final Entity target = GeneralMethods.getTargetedEntity(this.player, this.range);
                    this.origin = this.player.getEyeLocation();

                    if (target != null) {
                        this.destination = target.getLocation();
                    } else {
                        Block targetBlock = GeneralMethods.getTargetedLocation(this.player, this.range, false).getBlock();
                        boolean foundCopper = isCopper(targetBlock), foundRod = isLightningRod(targetBlock);

                        if (!foundCopper) {
                            if (!foundRod) {
                                for (Block block : GeneralMethods.getBlocksAroundPoint(targetBlock.getLocation(), 1.25)) {
                                    if (isLightningRod(block)) {
                                        foundRod = true;
                                        targetBlock = block;
                                        break;
                                    }
                                }
                            }
                        }
                        if (foundCopper || foundRod) {
                            this.destination = targetBlock.getLocation().clone().add(0.5, 0.5, 0.5);
                        } else {
                            this.destination = this.player.getEyeLocation().add(this.player.getEyeLocation().getDirection().normalize().multiply(this.range));
                        }
                    }
                }
            } else {
                if (!this.player.isSneaking()) {
                    this.remove();
                    return;
                }

                final Location localLocation1 = this.player.getLocation();
                final double d1 = 0.1570796326794897D;
                final double d2 = 0.06283185307179587D;
                final double d3 = 1.0D;
                final double d4 = 1.0D;
                final double d5 = d1 * this.particleRotation;
                final double d6 = d2 * this.particleRotation;
                final double d7 = localLocation1.getX() + d4 * Math.cos(d5);
                final double d8 = localLocation1.getZ() + d4 * Math.sin(d5);
                final double newY = (localLocation1.getY() + 1.0D + d4 * Math.cos(d6));
                final Location localLocation2 = new Location(this.player.getWorld(), d7, newY, d8);
                Particle.DustOptions spiralDust = (ThreadLocalRandom.current().nextBoolean())
                        ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                        : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
                localLocation2.getWorld().spawnParticle(Particle.DUST, localLocation2, 1, 0, 0, 0, 0, spiralDust);
                emitFirebendingLight(localLocation2);
                this.particleRotation += 1.0D / d3;
                if (ThreadLocalRandom.current().nextDouble() < .2) {
                    playLightningbendingChargingSound(this.player.getLocation());
                }
            }

        } else if (this.state == State.MAINBOLT) {
            final Arc mainArc = new Arc(this.origin, this.destination);
            mainArc.generatePoints(POINT_GENERATION);
            this.arcs.add(mainArc);
            final ArrayList<Arc> subArcs = mainArc.generateArcs(this.subArcChance, this.range / 2.0, this.maxArcAngle);
            this.arcs.addAll(subArcs);
            this.state = State.STRIKE;
        } else if (this.state == State.STRIKE || this.state == State.CHAIN) {
            for (int i = 0; i < this.arcs.size(); i++) {
                final Arc arc = this.arcs.get(i);

                for (int j = 0; j < arc.getAnimationLocations().size() - 1; j++) {
                    final Location iterLoc = arc.getAnimationLocations().get(j).getLocation().clone();
                    final Location dest = arc.getAnimationLocations().get(j + 1).getLocation().clone();
                    if (this.selfHitClose && this.player.getLocation().distanceSquared(iterLoc) < 9 && !this.isTransparentForLightning(this.player, iterLoc.getBlock()) && !this.affectedEntities.contains(this.player)) {
                        this.affectedEntities.add(this.player);
                        this.electrocute(this.player);
                    }

                    while (iterLoc.distanceSquared(dest) > 0.15 * 0.15) {
                        final BukkitRunnable task = new LightningParticle(arc, iterLoc.clone(), this.selfHitWater, this.waterArcs);
                        final double timer = this.state == State.CHAIN ? arc.getAnimationLocations().get(j).getAnimCounter() / 8 : arc.getAnimationLocations().get(j).getAnimCounter() / 2;
                        task.runTaskTimer(ProjectKorra.plugin, (long) timer, 1);
                        this.tasks.add(task);
                        iterLoc.add(GeneralMethods.getDirection(iterLoc, dest).normalize().multiply(0.15));
                    }
                }
                this.arcs.remove(i);
                i--;
            }
            if (this.state == State.STRIKE) {
                if (this.tasks.isEmpty()) {
                    this.remove();
                    return;
                }
            } else if (this.state == State.CHAIN) {
                if (this.currentCopperChainArc == null && this.copperChains < this.copperArcs && this.tasks.isEmpty()) {
                    this.chainCopperLightning(null);
                }
                if (this.copperChains == this.copperArcs - 1) {
                    this.remove();
                    return;
                }
            }
        }
    }

    public void removeWithTasks() {
        for (int i = 0; i < this.tasks.size(); i++) {
            this.tasks.get(i).cancel();
            i--;
        }
        this.remove();
    }

    @Override
    public void remove() {
        for (Block block : this.chargedCopperBlocks) {
            if (block != null) {
                block.removeMetadata("chargedcopper", ProjectKorra.plugin);
            }
        }
        super.remove();
    }

    /**
     * Represents a lightning arc point particle animation. Holds a location and counts the number
     * of times a particle has been animated at that point.
     */
    public static class AnimationLocation {
        private Location location;
        private int animationCounter;

        public AnimationLocation(final Location loc, final int animationCounter) {
            this.location = loc;
            this.animationCounter = animationCounter;
        }

        public int getAnimCounter() {
            return this.animationCounter;
        }

        public Location getLocation() {
            return this.location;
        }

        public void setAnimationCounter(final int animationCounter) {
            this.animationCounter = animationCounter;
        }

        public void setLocation(final Location location) {
            this.location = location;
        }
    }

    /**
     * Represents a lightning arc. Arcs contain a list of particles used to display the full arc
     * and can generate sub-arcs that chain off of their own instance.
     */
    public class Arc {
        private int animationCounter;
        private Vector direction;
        private final ArrayList<Location> points;
        private final ArrayList<AnimationLocation> animationLocations;
        private final ArrayList<LightningParticle> particles;
        private final ArrayList<Arc> subArcs;

        public Arc(final Location startPoint, final Location endPoint) {
            this.points = new ArrayList<>();
            this.points.add(startPoint.clone());
            this.points.add(endPoint.clone());
            this.direction = GeneralMethods.getDirection(startPoint, endPoint);
            this.particles = new ArrayList<>();
            this.subArcs = new ArrayList<>();
            this.animationLocations = new ArrayList<>();
            this.animationCounter = 0;
        }

        /**
         * Stops this arc from further animating or doing damage.
         */
        public void cancel() {
            for (LightningParticle particle : this.particles) {
                particle.cancel();
            }

            for (final Arc subArc : this.subArcs) {
                subArc.cancel();
            }
        }

        /**
         * Randomly generates sub-arcs off of this arc.
         *
         * @param chance      The chance that an arc will be generated for each specific point.
         * @param range       The length of each sub-arc.
         * @param maxArcAngle The maximum angle a sub-arc can deviate from the main direction.
         */
        public ArrayList<Arc> generateArcs(final double chance, final double range, final double maxArcAngle) {
            final ArrayList<Arc> arcs = new ArrayList<>();

            for (AnimationLocation animationLocation : this.animationLocations) {
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    final Location loc = animationLocation.getLocation();
                    final double angle = (ThreadLocalRandom.current().nextDouble() - 0.5) * maxArcAngle * 2;
                    final Vector dir = GeneralMethods.rotateXZ(this.direction.clone(), angle);
                    final double randRange = (ThreadLocalRandom.current().nextDouble() * range) + (range / 3.0);

                    final Location loc2 = loc.clone().add(dir.normalize().multiply(randRange));
                    final Arc arc = new Arc(loc, loc2);

                    this.subArcs.add(arc);
                    arc.setAnimationCounter(animationLocation.getAnimCounter());
                    arc.generatePoints(POINT_GENERATION);
                    arcs.add(arc);
                    arcs.addAll(arc.generateArcs(chance / 2.0, range / 2.0, maxArcAngle));
                }
            }
            return arcs;
        }

        /**
         * Generates arc points using a recursive midpoint displacement algorithm. The starting and
         * ending points are split in halves with randomised offsets, repeated by the input number of times.
         *
         * @param times The number of times the arc will be subdivided (O(n^2) complexity).
         */
        public void generatePoints(final int times) {
            for (int i = 0; i < times; i++) {
                for (int j = 0; j < this.points.size() - 1; j += 2) {
                    final Location loc1 = this.points.get(j);
                    final Location loc2 = this.points.get(j + 1);
                    double adjac = 0;
                    if (loc1.getWorld().equals(loc2.getWorld())) {
                        adjac = loc1.distance(loc2) / 2;
                    }

                    double angle = (ThreadLocalRandom.current().nextDouble() - 0.5) * Lightning.this.maxArcAngle;

                    angle += angle >= 0 ? 10 : -10;

                    final double radians = Math.toRadians(angle);
                    final double hypot = adjac / Math.cos(radians);
                    final Vector dir = GeneralMethods.rotateXZ(this.direction.clone(), angle);
                    final Location newLoc = loc1.clone().add(dir.normalize().multiply(hypot));

                    newLoc.add(0, (ThreadLocalRandom.current().nextDouble() - 0.5) / 2.0, 0);
                    this.points.add(j + 1, newLoc);
                }
            }
            for (Location point : this.points) {
                this.animationLocations.add(new AnimationLocation(point, this.animationCounter));
                this.animationCounter++;
            }
        }

        public int getAnimationCounter() {
            return this.animationCounter;
        }

        public void setAnimationCounter(final int animationCounter) {
            this.animationCounter = animationCounter;
        }

        public Vector getDirection() {
            return this.direction;
        }

        public void setDirection(final Vector direction) {
            this.direction = direction;
        }

        public ArrayList<Location> getPoints() {
            return this.points;
        }

        public ArrayList<AnimationLocation> getAnimationLocations() {
            return this.animationLocations;
        }

        public ArrayList<LightningParticle> getParticles() {
            return this.particles;
        }

        public ArrayList<Arc> getSubArcs() {
            return this.subArcs;
        }
    }

    public class LightningParticle extends BukkitRunnable {
        private boolean selfHitWater;
        private int count = 0;
        private int waterArcs;
        private Arc arc;
        private Location location;

        public LightningParticle(final Arc arc, final Location location, final boolean selfHitWater, final int waterArcs) {
            this.arc = arc;
            this.location = location;
            this.selfHitWater = selfHitWater;
            this.waterArcs = waterArcs;
            arc.particles.add(this);
        }

        @Override
        public void cancel() {
            super.cancel();
            Lightning.this.tasks.remove(this);
        }

        @Override
        public void run() {
            Particle.DustOptions boltDust = (count % 2 == 0)
                    ? AranarthBendingUtils.LIGHTNING_DUST_BRIGHT
                    : AranarthBendingUtils.LIGHTNING_DUST_BLUE;
            this.location.getWorld().spawnParticle(Particle.DUST, this.location, 1, 0, 0, 0, 0, boltDust);
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                this.location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, this.location, 1, 0, 0, 0, 0);
            }
            emitFirebendingLight(this.location);

            this.count++;
            if (this.count > 5) {
                this.cancel();
            } else if (this.count == 1) {
                if (ThreadLocalRandom.current().nextDouble() < .1) {
                    playLightningbendingSound(location);
                }
                if (Lightning.this.state == State.CHAIN) {
                    Lightning.this.chainCopperLightning(this.location);
                }
                powerLightningRods(this.location.getBlock());

                if (!Lightning.this.isTransparentForLightning(Lightning.this.player, this.location.getBlock()) && !isCopper(this.location.getBlock()) && !isLightningRod(this.location.getBlock())) {
                    this.arc.cancel();
                    return;
                } else if (isLightningRod(this.location.getBlock())) {
                    if (Lightning.this.state != State.CHAIN) {
                        if (rodIsGrounded(this.location.getBlock())) {
                            this.arc.cancel();
                        } else {
                            Lightning.this.setupCopperGraph(this.location.getBlock());
                            Lightning.this.chainCopperLightning(this.location);
                            Lightning.this.state = State.CHAIN;
                        }
                    }
                }
                final Block block = this.location.getBlock();
                Lightning.this.locations.add(block.getLocation());

                if ((!Lightning.this.hitWater && isWater(block) || (Lightning.this.arcOnIce && isIce(block)))) {
                    if (isWater(block) || isIce(block)) {
                        Lightning.this.hitWater = true;
                        if (isIce(block)) {
                            Lightning.this.hitIce = true;
                        }
                        for (int i = 0; i < this.waterArcs; i++) {
                            final Location origin = this.location.clone();
                            origin.add(new Vector((ThreadLocalRandom.current().nextDouble() - 0.5) * 2, 0, (ThreadLocalRandom.current().nextDouble() - 0.5) * 2));
                            Lightning.this.destination = origin.clone().add(new Vector((ThreadLocalRandom.current().nextDouble() - 0.5) * Lightning.this.waterArcRange, ThreadLocalRandom.current().nextDouble() - 0.7, (ThreadLocalRandom.current().nextDouble() - 0.5) * Lightning.this.waterArcRange));
                            final Arc newArc = new Arc(origin, Lightning.this.destination);
                            newArc.generatePoints(POINT_GENERATION);
                            Lightning.this.arcs.add(newArc);
                        }
                    }
                } else if (!Lightning.this.hitCopper && Lightning.this.arcOnCopper && isCopper(block)) {
                    Lightning.this.hitCopper = true;

                    if (Lightning.this.state != State.CHAIN && Lightning.this.arcOnCopper) {
                        Lightning.this.setupCopperGraph(block);
                        Lightning.this.chainCopperLightning(this.location);
                        Lightning.this.state = State.CHAIN;
                        this.arc.cancel();
                    }
                }

                for (final Entity entity : GeneralMethods.getEntitiesAroundPoint(this.location, 2.5)) {
                    if (entity.equals(Lightning.this.player) && !(this.selfHitWater && Lightning.this.hitWater && isWater(Lightning.this.player.getLocation().getBlock())) && !(this.selfHitWater && Lightning.this.hitCopper) && !(this.selfHitWater && Lightning.this.hitIce)) {
                        continue;
                    }

                    if (entity instanceof LivingEntity lent && !Lightning.this.affectedEntities.contains(entity)) {
                        Lightning.this.affectedEntities.add(entity);
                        if (lent instanceof Player p) {
                            playLightningbendingSound(lent.getLocation());
                            playLightningbendingSound(Lightning.this.player.getLocation());
                            final Lightning light = getAbility(p, Lightning.class);
                            if (light != null && light.state == State.START) {
                                light.charged = true;
                                Lightning.this.remove();
                                return;
                            }
                        }

                        Lightning.this.electrocute(lent);

                        if (Lightning.this.maxChainArcs >= 1 && ThreadLocalRandom.current().nextDouble() <= Lightning.this.chainArcChance) {
                            Lightning.this.maxChainArcs--;
                            for (final Entity ent : GeneralMethods.getEntitiesAroundPoint(lent.getLocation(), Lightning.this.chainRange)) {
                                if (!ent.equals(Lightning.this.player) && !ent.equals(lent) && ent instanceof LivingEntity && !Lightning.this.affectedEntities.contains(ent)) {
                                    Lightning.this.origin = lent.getLocation().add(0, 1, 0);
                                    Lightning.this.destination = ent.getLocation().add(0, 1, 0);
                                    final Arc newArc = new Arc(Lightning.this.origin, Lightning.this.destination);
                                    newArc.generatePoints(POINT_GENERATION);
                                    Lightning.this.arcs.add(newArc);
                                    this.cancel();
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        public boolean isSelfHitWater() {
            return this.selfHitWater;
        }

        public void setSelfHitWater(final boolean selfHitWater) {
            this.selfHitWater = selfHitWater;
        }

        public int getCount() {
            return this.count;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public int getWaterArcs() {
            return this.waterArcs;
        }

        public void setWaterArcs(final int waterArcs) {
            this.waterArcs = waterArcs;
        }

        public Arc getArc() {
            return this.arc;
        }

        public void setArc(final Arc arc) {
            this.arc = arc;
        }

        public Location getLocation() {
            return this.location;
        }

        public void setLocation(final Location location) {
            this.location = location;
        }
    }

    @Override
    public String getName() {
        return "Lightning";
    }

    @Override
    public Location getLocation() {
        return this.origin;
    }

    @Override
    public long getCooldown() {
        return this.cooldown;
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
    public boolean isCollidable() {
        return !this.arcs.isEmpty();
    }

    @Override
    public List<Location> getLocations() {
        return this.locations;
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return "ProjectKorra, Aearost (Maintainer)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Hold sneak to charge a bolt of lightning, then release to fire it at your target. "
                + "The bolt branches into arcs, chains between nearby entities, and conducts through water and copper. "
                + "Direct hits electrocute targets and may stun them. Lightning rods and copper blocks interact naturally.\n"
                + ChatUtils.translateToColor("&fUsage: Hold Sneak (charge), Release Sneak (fire)");
    }

    public boolean isCharged() {
        return this.charged;
    }

    public void setCharged(final boolean charged) {
        this.charged = charged;
    }

    public boolean isHitWater() {
        return this.hitWater;
    }

    public void setHitWater(final boolean hitWater) {
        this.hitWater = hitWater;
    }

    public boolean isHitIce() {
        return this.hitIce;
    }

    public void setHitIce(final boolean hitIce) {
        this.hitIce = hitIce;
    }

    public boolean isHitCopper() {
        return this.hitCopper;
    }

    public void setHitCopper(final boolean hitCopper) {
        this.hitCopper = hitCopper;
    }

    public boolean isChainLightningRods() {
        return this.chainLightningRods;
    }

    public void setChainLightningRods(final boolean chainLightningRods) {
        this.chainLightningRods = chainLightningRods;
    }

    public boolean isSelfHitWater() {
        return this.selfHitWater;
    }

    public void setSelfHitWater(final boolean selfHitWater) {
        this.selfHitWater = selfHitWater;
    }

    public boolean isSelfHitClose() {
        return this.selfHitClose;
    }

    public void setSelfHitClose(final boolean selfHitClose) {
        this.selfHitClose = selfHitClose;
    }

    public boolean isArcOnIce() {
        return this.arcOnIce;
    }

    public void setArcOnIce(final boolean arcOnIce) {
        this.arcOnIce = arcOnIce;
    }

    public boolean isArcOnCopper() {
        return this.arcOnCopper;
    }

    public void setArcOnCopper(final boolean arcOnCopper) {
        this.arcOnCopper = arcOnCopper;
    }

    public int getWaterArcs() {
        return this.waterArcs;
    }

    public void setWaterArcs(final int waterArcs) {
        this.waterArcs = waterArcs;
    }

    public double getRange() {
        return this.range;
    }

    public void setRange(final double range) {
        this.range = range;
    }

    public double getChargeTime() {
        return this.chargeTime;
    }

    public void setChargeTime(final double chargeTime) {
        this.chargeTime = chargeTime;
    }

    public double getSubArcChance() {
        return this.subArcChance;
    }

    public void setSubArcChance(final double subArcChance) {
        this.subArcChance = subArcChance;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(final double damage) {
        this.damage = damage;
    }

    public double getMaxChainArcs() {
        return this.maxChainArcs;
    }

    public void setMaxChainArcs(final double maxChainArcs) {
        this.maxChainArcs = maxChainArcs;
    }

    public double getChainRange() {
        return this.chainRange;
    }

    public void setChainRange(final double chainRange) {
        this.chainRange = chainRange;
    }

    public double getWaterArcRange() {
        return this.waterArcRange;
    }

    public void setWaterArcRange(final double waterArcRange) {
        this.waterArcRange = waterArcRange;
    }

    public double getChainArcChance() {
        return this.chainArcChance;
    }

    public void setChainArcChance(final double chainArcChance) {
        this.chainArcChance = chainArcChance;
    }

    public double getStunChance() {
        return this.stunChance;
    }

    public void setStunChance(final double stunChance) {
        this.stunChance = stunChance;
    }

    public double getStunDuration() {
        return this.stunDuration;
    }

    public void setStunDuration(final double stunDuration) {
        this.stunDuration = stunDuration;
    }

    public double getMaxArcAngle() {
        return this.maxArcAngle;
    }

    public void setMaxArcAngle(final double maxArcAngle) {
        this.maxArcAngle = maxArcAngle;
    }

    public double getParticleRotation() {
        return this.particleRotation;
    }

    public void setParticleRotation(final double particleRotation) {
        this.particleRotation = particleRotation;
    }

    public State getState() {
        return this.state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public Location getOrigin() {
        return this.origin;
    }

    public void setOrigin(final Location origin) {
        this.origin = origin;
    }

    public Location getDestination() {
        return this.destination;
    }

    public void setDestination(final Location destination) {
        this.destination = destination;
    }

    public static int getPointGeneration() {
        return POINT_GENERATION;
    }

    public ArrayList<Entity> getAffectedEntities() {
        return this.affectedEntities;
    }

    public ArrayList<Arc> getArcs() {
        return this.arcs;
    }

    public ArrayList<BukkitRunnable> getTasks() {
        return this.tasks;
    }

    public void setCooldown(final long cooldown) {
        this.cooldown = cooldown;
    }
}
