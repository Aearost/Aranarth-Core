package com.aearost.aranarthcore.abilities.airbending.spiritual;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LightOfRaava extends AvatarAbility implements AddonAbility {

    private static final Set<EntityType> UNDEAD_TYPES = EnumSet.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIE_HORSE,
            EntityType.HUSK, EntityType.DROWNED, EntityType.SKELETON,
            EntityType.SKELETON_HORSE, EntityType.STRAY, EntityType.WITHER_SKELETON,
            EntityType.WITHER, EntityType.PHANTOM, EntityType.ZOGLIN,
            EntityType.ZOMBIFIED_PIGLIN
    );

    private static final Particle.DustOptions READY_WHITE_DUST = new Particle.DustOptions(Color.WHITE, 1.3f);
    private static final Particle.DustOptions READY_BLUE_DUST = new Particle.DustOptions(Color.fromRGB(85, 255, 255), 1.1f);
    private static final Particle.DustOptions BURST_WHITE_DUST = new Particle.DustOptions(Color.WHITE, 1.2f);
    private static final Particle.DustOptions BURST_BLUE_DUST = new Particle.DustOptions(Color.fromRGB(85, 255, 255), 1.1f);

    private static final double AVATAR_STATE_RANGE = 13.0;
    private static final long BURST_DURATION = 750L;

    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    private enum State { CHARGING, READY, BURSTING }

    private State state;
    private final int heldSlot;
    private Location burstCenter;
    private double burstRange;
    private long burstStart;
    private final Set<UUID> hitEntities = new HashSet<>();

    public LightOfRaava(final Player player) {
        super(player);

        this.chargeDuration = 1000L;
        this.range = 7.0;
        this.cooldown = 20000L;
        this.state = State.CHARGING;
        this.heldSlot = player.getInventory().getHeldItemSlot();

        if (!bPlayer.canBend(this)) {
            return;
        }

        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        switch (state) {
            case CHARGING -> {
                if (player.getInventory().getHeldItemSlot() != heldSlot) {
                    remove();
                    return;
                }
                if (!bPlayer.canBend(this)) {
                    remove();
                    return;
                }
                handleCharging();
            }
            case READY -> {
                if (player.getInventory().getHeldItemSlot() != heldSlot) {
                    remove();
                    return;
                }
                if (!bPlayer.canBend(this)) {
                    remove();
                    return;
                }
                handleReady();
            }
            case BURSTING -> handleBursting();
        }
    }

    private void handleCharging() {
        if (!player.isSneaking()) {
            remove(); // Sneak released before charge completed
            return;
        }
        if (System.currentTimeMillis() - getStartTime() >= chargeDuration) {
            state = State.READY;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        }
    }

    private void handleReady() {
        spawnReadyParticles();
        if (!player.isSneaking()) {
            activate();
        }
    }

    private void activate() {
        burstRange = bPlayer.isAvatarState() ? AVATAR_STATE_RANGE : range;
        burstCenter = player.getLocation().add(0, 1, 0);
        burstStart = System.currentTimeMillis();
        state = State.BURSTING;

        player.getWorld().playSound(burstCenter, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.0f, 0.4f);
        player.getWorld().playSound(burstCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 2.0f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4));

        bPlayer.addCooldown(this);
    }

    private void handleBursting() {
        final long elapsed = System.currentTimeMillis() - burstStart;
        if (elapsed >= BURST_DURATION) {
            remove();
            return;
        }

        final double progress = (double) elapsed / BURST_DURATION;
        final double currentRadius = burstRange * progress;

        spawnBurstShell(burstCenter, currentRadius);

        for (final LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player)) continue;
            if (hitEntities.contains(entity.getUniqueId())) continue;
            if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) continue;

            final Location entityCenter = entity.getLocation().add(0, entity.getHeight() / 2.0, 0);
            if (entityCenter.distance(burstCenter) > currentRadius) continue;

            hitEntities.add(entity.getUniqueId());
            if (isHostileTarget(entity)) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1));
            } else {
                // Undead mobs (zombies, skeletons, etc.) are harmed by instant health — use instant damage instead
                if (UNDEAD_TYPES.contains(entity.getType())) {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 2));
                } else {
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2));
                }
                entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4));
            }
        }
    }

    private boolean isHostileTarget(final LivingEntity entity) {
        if (entity instanceof Player target) {
            return canDamagePlayer(target);
        }
        if (DefenderUtils.isDefender(entity.getUniqueId())) {
            return canDamageDefender(entity.getUniqueId());
        }
        return entity instanceof Monster;
    }

    private boolean canDamagePlayer(final Player target) {
        final Dominion targetDominion = DominionUtils.getPlayerDominion(target.getUniqueId());
        return canDamageByDominion(targetDominion, target.getLocation());
    }

    private boolean canDamageDefender(final UUID defenderEntityId) {
        final UUID defenderDominionId = DefenderUtils.getDefenderDominionId(defenderEntityId);
        final Dominion defenderDominion = DominionUtils.getDominionById(defenderDominionId);
        return canDamageByDominion(defenderDominion, null);
    }

    private boolean canDamageByDominion(final Dominion targetDominion, final Location targetLocation) {
        final Dominion casterDominion = DominionUtils.getPlayerDominion(player.getUniqueId());

        // Same dominion, check PvP flag
        if (casterDominion != null && targetDominion != null && casterDominion.isSameDominion(targetDominion)) {
            return casterDominion.isMemberPvpEnabled();
        }

        // Both belong to different dominions
        if (casterDominion != null && targetDominion != null) {
            final DominionRank relation = DominionUtils.getRelationKey(casterDominion, targetDominion);
            if (relation == DominionRank.ALLIED || relation == DominionRank.TRUCED) {
                final boolean casterPvp = casterDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                final boolean targetPvp = targetDominion.getDominionPermissions().hasPermission(relation, DominionPermission.PVP);
                return casterPvp && targetPvp;
            }
            if (relation == DominionRank.NEUTRAL && targetLocation != null) {
                final Dominion chunkDominion = DominionUtils.getDominionOfChunk(targetLocation.getChunk());
                return chunkDominion == null || !chunkDominion.isSameDominion(targetDominion);
            }
            // Enemied or neutral without a location, always allowed
            return true;
        }

        // Caster has a dominion, target is a wanderer, always allowed
        if (casterDominion != null) {
            return true;
        }

        // Caster is a wanderer, target has a dominion, blocked if target is in their own land
        if (targetDominion != null && targetLocation != null) {
            final Dominion chunkDominion = DominionUtils.getDominionOfChunk(targetLocation.getChunk());
            return chunkDominion == null || !chunkDominion.isSameDominion(targetDominion);
        }

        // Both wanderers, always allowed
        return true;
    }

    private void spawnReadyParticles() {
        final Location center = player.getLocation().add(0, 1.5, 0);
        final int ringPoints = 24;
        final double ringRadius = 0.65;
        for (int i = 0; i < ringPoints; i++) {
            final double angle = (2.0 * Math.PI / ringPoints) * i;
            final double x = Math.cos(angle) * ringRadius;
            final double z = Math.sin(angle) * ringRadius;
            final Location loc = center.clone().add(x, 0, z);
            final Particle.DustOptions dust = (i % 2 == 0) ? READY_WHITE_DUST : READY_BLUE_DUST;
            loc.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnBurstShell(final Location center, final double radius) {
        if (radius <= 0) return;

        final int dustPoints = (int) Math.max(16, radius * 18);
        for (int i = 0; i < dustPoints; i++) {
            final double theta = Math.random() * 2 * Math.PI;
            final double cosPhi = 2 * Math.random() - 1;
            final double sinPhi = Math.sqrt(1.0 - cosPhi * cosPhi);
            final double x = radius * sinPhi * Math.cos(theta);
            final double y = radius * cosPhi;
            final double z = radius * sinPhi * Math.sin(theta);
            final Particle.DustOptions dust = (Math.random() < 0.65) ? BURST_WHITE_DUST : BURST_BLUE_DUST;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 1, 0, 0, 0, 0, dust);
        }

        final int rodPoints = (int) Math.max(6, radius * 6);
        for (int i = 0; i < rodPoints; i++) {
            final double theta = Math.random() * 2 * Math.PI;
            final double cosPhi = 2 * Math.random() - 1;
            final double sinPhi = Math.sqrt(1.0 - cosPhi * cosPhi);
            final double x = radius * sinPhi * Math.cos(theta);
            final double y = radius * cosPhi;
            final double z = radius * sinPhi * Math.sin(theta);
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
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
        return state == State.BURSTING ? burstCenter : player.getLocation();
    }

    @Override
    public String getName() {
        return "LightOfRaava";
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
        return "Channel the boundless energy of Raava in a great sphere of light. " +
                "Passive mobs and players protected from your harm are healed and provided with sharp Regeneration, " +
                "while hostile mobs and players take damage and are affected by the Wither effect.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (Charge) > Release Sneak");
    }
}
