package com.aearost.aranarthcore.abilities.waterbending.combo;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class IceDiscs extends IceAbility implements AddonAbility, ComboAbility {

    private static final Map<UUID, IceDiscs> ACTIVE_INSTANCES = new HashMap<>();

    public enum Phase {CHARGING, FIRING}

    private static final double SPEED = 3;
    private static final double STEP = 0.3;
    private static final double HIT_RADIUS = 0.7;
    private static final long INTER_DISC_COOLDOWN = 100L;
    private static final int MAX_LAYERS = 12;
    private static final long CHARGE_TIME = 2000L;
    private static final long LAYER_INTERVAL = CHARGE_TIME / MAX_LAYERS; // 250 ms per layer

    private static NamespacedKey discKey;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private Phase phase;
    private int heldSlot = -1;

    private Location pillarBase;
    private int currentLayers = 0;
    private TempBlock pillarLower;
    private TempBlock pillarUpper;
    private long lastLayerTime;
    private long lastSoundTime;

    private final List<Disc> discs = new ArrayList<>();
    private long lastFireTime = -1L;

    private static final class Disc {
        final FallingBlock entity;
        Vector velocity;
        final Location startLocation;
        boolean pastRange;

        Disc(final FallingBlock entity, final Vector velocity, final Location start) {
            this.entity = entity;
            this.velocity = velocity.clone();
            this.startLocation = start.clone();
            this.pastRange = false;
        }
    }

    public IceDiscs(final Player player) {
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

        // Find a snow or ice source block via ray trace (includes passable snow layers)
        final RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                8.0,
                FluidCollisionMode.NEVER,
                false
        );
        if (result == null || result.getHitBlock() == null) {
            return;
        }
        final Block sourceBlock = result.getHitBlock();
        if (!isValidSource(sourceBlock.getType())) {
            return;
        }

        // Need at least 2 passable blocks above the source to build the pillar
        final Block above1 = sourceBlock.getRelative(0, 1, 0);
        final Block above2 = sourceBlock.getRelative(0, 2, 0);
        if ((!above1.isPassable() && !TempBlock.isTempBlock(above1))
                || (!above2.isPassable() && !TempBlock.isTempBlock(above2))) {
            return;
        }
        if (GeneralMethods.isRegionProtectedFromBuild(this, above1.getLocation())) {
            return;
        }

        this.cooldown = 12000L;
        this.damage = 2.0;
        this.range = 20.0;
        this.pillarBase = above1.getLocation();
        this.phase = Phase.CHARGING;
        this.lastLayerTime = System.currentTimeMillis();
        this.lastSoundTime = 0L;
        this.heldSlot = player.getInventory().getHeldItemSlot();

        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "PhaseChange");
        AranarthBendingUtils.suppressComboTrigger(this.bPlayer, player, "IceSpike");

        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        this.start();
    }

    private static boolean isValidSource(final Material mat) {
        return mat == Material.SNOW
                || mat == Material.SNOW_BLOCK
                || mat == Material.ICE
                || mat == Material.PACKED_ICE
                || mat == Material.BLUE_ICE
                || mat == Material.FROSTED_ICE;
    }

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

    private void progressCharging() {
        if (this.player.getInventory().getHeldItemSlot() != this.heldSlot) {
            this.cancelWithCooldown();
            return;
        }
        if (!this.player.isSneaking()) {
            this.cancelWithCooldown();
            return;
        }
        if (!this.player.getWorld().equals(this.pillarBase.getWorld())
                || this.player.getLocation().distanceSquared(this.pillarBase) > 100.0) {
            this.cancelWithCooldown();
            return;
        }

        // Keep IceSpike suppressed
        this.bPlayer.addCooldown("IceSpike", 500L);

        final long now = System.currentTimeMillis();

        // Add one layer every LAYER_INTERVAL ms until MAX_LAYERS
        if (this.currentLayers < MAX_LAYERS && now - this.lastLayerTime >= LAYER_INTERVAL) {
            this.lastLayerTime = now;
            this.currentLayers++;
            this.updatePillar();
            this.spawnFormationParticles();
        }

        // Snow sound while forming
        if (now - this.lastSoundTime >= 400L) {
            this.lastSoundTime = now;
            final float pitch = 1.0f + (this.currentLayers * 0.03f);
            this.player.getWorld().playSound(this.pillarBase, Sound.BLOCK_SNOW_PLACE, 0.7f, pitch);
        }
    }

    private void progressFiring() {
        if (this.player.getInventory().getHeldItemSlot() != this.heldSlot) {
            this.cancelWithCooldown();
            return;
        }
        if (!this.player.getWorld().equals(this.pillarBase.getWorld())
                || this.player.getLocation().distanceSquared(this.pillarBase) > 100.0) {
            this.cancelWithCooldown();
            return;
        }

        // Keep IceSpike suppressed
        for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(this.player))) {
            if (ability.getName().equals("IceSpike")) {
                ability.remove();
            }
        }
        this.bPlayer.addCooldown("IceSpike", 500L);

        for (int i = this.discs.size() - 1; i >= 0; i--) {
            final Disc disc = this.discs.get(i);

            if (disc.entity.isDead()) {
                this.discs.remove(i);
                continue;
            }

            final Location current = disc.entity.getLocation();

            if (!disc.pastRange) {
                // Sub-step scan along the path this tick so fast-moving discs never skip over entities
                final Vector direction = disc.velocity.clone().normalize();
                final Location checkPos = current.clone();
                double remaining = disc.velocity.length();
                boolean stopped = false;

                while (remaining > 0) {
                    final double step = Math.min(STEP, remaining);
                    checkPos.add(direction.clone().multiply(step));
                    remaining -= step;

                    // Once range is exceeded, hand movement off to natural gravity
                    if (checkPos.distanceSquared(disc.startLocation) >= this.range * this.range) {
                        disc.pastRange = true;
                        disc.entity.setGravity(true);
                        disc.entity.setVelocity(disc.velocity.clone().multiply(0.4));
                        stopped = true;
                        break;
                    }

                    if (GeneralMethods.isRegionProtectedFromBuild(this, checkPos)) {
                        this.spawnImpactParticles(checkPos);
                        disc.entity.remove();
                        this.discs.remove(i);
                        stopped = true;
                        break;
                    }

                    // Solid, non-passable block in the path
                    if (checkPos.getBlock().getType().isSolid() && !checkPos.getBlock().isPassable()) {
                        this.spawnImpactParticles(checkPos);
                        disc.entity.remove();
                        this.discs.remove(i);
                        stopped = true;
                        break;
                    }

                    // Each disc hits at most one entity, but any entity can be hit by multiple discs
                    for (final Entity ent : GeneralMethods.getEntitiesAroundPoint(checkPos, HIT_RADIUS)) {
                        if (!(ent instanceof LivingEntity living)) {
                            continue;
                        }
                        if (living.equals(this.player)) {
                            continue;
                        }
                        if (living instanceof Player p && Commands.invincible.contains(p.getName())) {
                            continue;
                        }

                        DamageHandler.damageEntity(living, this.damage, this);
                        this.spawnImpactParticles(checkPos);
                        disc.entity.remove();
                        this.discs.remove(i);
                        stopped = true;
                        break;
                    }
                    if (stopped) {
                        break;
                    }
                }

                if (!stopped) {
                    this.spawnTrailParticles(current);
                    disc.entity.setVelocity(disc.velocity);
                }
            } else {
                // Past range so clean up if the disc landed on a solid block
                if (current.getBlock().getType().isSolid() && !current.getBlock().isPassable()) {
                    disc.entity.remove();
                    this.discs.remove(i);
                }
            }
        }

        // All layers fired and no discs remain
        if (this.currentLayers <= 0 && this.discs.isEmpty()) {
            this.bPlayer.addCooldown(this);
            this.remove();
        }
    }

    public void fireDisc() {
        // Left-clicking while still charging stops the pillar growth and begins firing
        if (this.phase == Phase.CHARGING) {
            this.phase = Phase.FIRING;
        }
        if (this.phase != Phase.FIRING) {
            return;
        }
        if (this.currentLayers <= 0) {
            return;
        }

        final long now = System.currentTimeMillis();
        if (this.lastFireTime != -1L && now - this.lastFireTime < INTER_DISC_COOLDOWN) {
            return;
        }
        this.lastFireTime = now;

        // Decrement the pillar by one layer
        this.currentLayers--;
        this.updatePillar();

        // Spawn the FallingBlock disc from the top of the remaining pillar
        final double layerHeight = 0.125; // each snow layer is 2/16 of a block
        final double spawnY;
        if (this.currentLayers >= 8) {
            spawnY = this.pillarBase.getY() + 1.0 + (this.currentLayers - 8) * layerHeight;
        } else {
            spawnY = this.pillarBase.getY() + Math.max(this.currentLayers, 1) * layerHeight;
        }
        final Location spawnLoc = new Location(
                this.pillarBase.getWorld(),
                this.pillarBase.getX() + 0.5,
                spawnY,
                this.pillarBase.getZ() + 0.5);

        final Location eyeLoc = this.player.getEyeLocation();
        final Vector direction = eyeLoc.getDirection().normalize();

        final Snow snowData = (Snow) Material.SNOW.createBlockData();
        snowData.setLayers(1);

        final FallingBlock disc = this.player.getWorld().spawnFallingBlock(spawnLoc, snowData);
        disc.setGravity(false);
        disc.setDropItem(false);
        disc.setHurtEntities(false);
        disc.getPersistentDataContainer().set(getDiscKey(), PersistentDataType.BYTE, (byte) 1);
        disc.setVelocity(direction.clone().multiply(SPEED));

        this.discs.add(new Disc(disc, direction.clone().multiply(SPEED), spawnLoc));

        // Ice break sound at a random pitch between 1.15 and 1.35
        final float pitch = 1.15f + new Random().nextFloat() * 0.20f;
        this.player.getWorld().playSound(eyeLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, pitch);
    }

    public void cancelWithCooldown() {
        this.crumblePillar();
        this.bPlayer.addCooldown(this);
        this.remove();
    }

    private void updatePillar() {
        if (this.pillarLower != null) {
            this.pillarLower.revertBlock();
            this.pillarLower = null;
        }
        if (this.pillarUpper != null) {
            this.pillarUpper.revertBlock();
            this.pillarUpper = null;
        }

        if (this.currentLayers <= 0) {
            return;
        }

        final Block lowerBlock = this.pillarBase.getBlock();

        if (this.currentLayers > 8) {
            final Snow lowerData = (Snow) Material.SNOW.createBlockData();
            lowerData.setLayers(8);
            this.pillarLower = new TempBlock(lowerBlock, lowerData);

            final Snow upperData = (Snow) Material.SNOW.createBlockData();
            upperData.setLayers(this.currentLayers - 8);
            this.pillarUpper = new TempBlock(this.pillarBase.clone().add(0, 1, 0).getBlock(), upperData);
        } else {
            final Snow lowerData = (Snow) Material.SNOW.createBlockData();
            lowerData.setLayers(this.currentLayers);
            this.pillarLower = new TempBlock(lowerBlock, lowerData);
        }
    }

    private void spawnFormationParticles() {
        final double yOffset = this.currentLayers > 8 ? 1.1 : 0.1;
        final Location loc = this.pillarBase.clone().add(0.5, yOffset, 0.5);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.25, 0.1, 0.25, 0.03);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 4, 0.25, 0.1, 0.25, 0.05, Material.SNOW.createBlockData());
    }

    private void spawnTrailParticles(final Location loc) {
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.1, 0.1, 0.02);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 2, 0.1, 0.1, 0.1, 0.02, Material.ICE.createBlockData());
    }

    private void spawnImpactParticles(final Location loc) {
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 8, 0.2, 0.2, 0.2, 0.05);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 6, 0.2, 0.2, 0.2, 0.06, Material.SNOW.createBlockData());
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 4, 0.2, 0.2, 0.2, 0.06, Material.ICE.createBlockData());
    }

    private void crumblePillar() {
        if (this.currentLayers > 0) {
            final Location crumbleLoc = this.pillarBase.clone().add(0.5, 0.5, 0.5);
            crumbleLoc.getWorld().spawnParticle(Particle.BLOCK, crumbleLoc, 18, 0.4, 0.5, 0.4, 0.1, Material.SNOW.createBlockData());
            crumbleLoc.getWorld().spawnParticle(Particle.BLOCK, crumbleLoc, 12, 0.4, 0.5, 0.4, 0.1, Material.ICE.createBlockData());
        }
        this.currentLayers = 0;
        if (this.pillarLower != null) {
            this.pillarLower.revertBlock();
            this.pillarLower = null;
        }
        if (this.pillarUpper != null) {
            this.pillarUpper.revertBlock();
            this.pillarUpper = null;
        }
    }

    @Override
    public void remove() {
        super.remove();
        ACTIVE_INSTANCES.remove(this.player.getUniqueId());
        this.crumblePillar();
        for (final Disc disc : this.discs) {
            if (!disc.entity.isDead()) {
                disc.entity.remove();
            }
        }
        this.discs.clear();
    }

    public Phase getPhase() {
        return this.phase;
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static IceDiscs getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    public static NamespacedKey getDiscKey() {
        if (discKey == null) {
            discKey = new NamespacedKey(AranarthCore.getInstance(), "icediscs_disc");
        }
        return discKey;
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
        return this.cooldown;
    }

    @Override
    public Location getLocation() {
        return this.player.getLocation();
    }

    @Override
    public String getName() {
        return "IceDiscs";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
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

    @Override
    public Object createNewComboInstance(final Player player) {
        return new IceDiscs(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("PhaseChange", ClickType.LEFT_CLICK));
        combo.add(new AbilityInformation("PhaseChange", ClickType.LEFT_CLICK));
        combo.add(new AbilityInformation("IceSpike", ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Form a pillar of compressed snow, and rapidly fire icy discs out from it. " +
                "Each left-click launches a disc of ice at your target, depleting the pillar one layer at a time.\n" +
                ChatUtils.translateToColor("&fUsage: PhaseChange (Left-click) > PhaseChange (Left-click) > IceSpike (Hold Sneak on snow/ice) > IceSpike (Left-click multiple times)");
    }
}
