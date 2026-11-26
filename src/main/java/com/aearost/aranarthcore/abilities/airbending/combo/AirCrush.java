package com.aearost.aranarthcore.abilities.airbending.combo;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AirCrush extends AirAbility implements AddonAbility, ComboAbility {

    public enum Phase { CHARGING, READY }

    private static final Map<UUID, AirCrush> ACTIVE = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;
    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeTime;

    private double minDamage;
    private long streamFormTime;
    private int streams;
    private double outerRadius;
    private double innerRadius;

    private Phase phase;
    private LivingEntity target;
    private long chargeStartTime;
    private double animAngle;
    private int ticks;

    public AirCrush(final Player player) {
        super(player);

        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
            return;
        }
        if (this.bPlayer.isOnCooldown(this)) {
            return;
        }
        if (ACTIVE.containsKey(player.getUniqueId())) {
            return;
        }

        this.cooldown = AbilityConfig.getLong("AirCrush.Cooldown");
        this.damage = AbilityConfig.getDouble("AirCrush.Damage");
        this.minDamage = AbilityConfig.getDouble("AirCrush.MinDamage");
        this.range = AbilityConfig.getDouble("AirCrush.Range");
        this.chargeTime = AbilityConfig.getLong("AirCrush.ChargeTime");
        this.streamFormTime = AbilityConfig.getLong("AirCrush.StreamFormTime");
        this.streams = AbilityConfig.getInt("AirCrush.Streams");
        this.outerRadius = AbilityConfig.getDouble("AirCrush.OuterRadius");
        this.innerRadius = AbilityConfig.getDouble("AirCrush.InnerRadius");

        // The Suffocate from the combo's final SHIFT_DOWN is blocked by the bending listener
        this.target = findTarget();
        if (this.target == null) {
            return;
        }

        this.phase = Phase.CHARGING;
        this.chargeStartTime = System.currentTimeMillis();
        this.animAngle = 0;

        player.getWorld().playSound(this.target.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 0.8f);

        ACTIVE.put(player.getUniqueId(), this);
        this.start();
    }

    /**
     * Finds the living entity the caster is looking at within range.
     * @return The targeted entity, or null if there is no valid target.
     */
    private LivingEntity findTarget() {
        Entity entity = null;
        for (int i = 0; i < this.range; i++) {
            final Location location = GeneralMethods.getTargetedLocation(player, i, getTransparentMaterials());
            final List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(location, 0.5);
            entities.remove(player);
            if (!entities.isEmpty()) {
                entity = entities.getFirst();
                break;
            }
        }
        if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand) {
            return null;
        }
        if (entity instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
            return null;
        }
        if (GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())) {
            return null;
        }
        return living;
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || !player.isSneaking()) {
            this.remove();
            return;
        }
        if (!this.bPlayer.canBendIgnoreBindsCooldowns(this) || !isTargetInRange()) {
            this.remove();
            return;
        }

        switch (this.phase) {
            case CHARGING -> progressCharging();
            case READY    -> progressReady();
        }

        if (this.ticks % 4 == 0) {
            playAirbendingSound(getTargetCenter());
        }
        this.ticks++;
    }

    /**
     * Determines if the target is still alive and within range of the caster.
     */
    private boolean isTargetInRange() {
        if (this.target.isDead() || !this.target.isValid()) {
            return false;
        }
        if (!this.target.getWorld().equals(player.getWorld())) {
            return false;
        }
        return this.target.getLocation().distance(player.getLocation()) <= this.range;
    }

    private Location getTargetCenter() {
        return this.target.getLocation().add(0, this.target.getHeight() / 2.0, 0);
    }

    /**
     * Gets how far the charge has progressed, from 0 when the combo starts to 1 when fully charged.
     */
    private double getChargeProgress() {
        return Math.min((double) (System.currentTimeMillis() - this.chargeStartTime) / this.chargeTime, 1.0);
    }

    /**
     * Gets the current distance of the streams from the target, tightening as the charge builds.
     */
    private double getStreamRadius() {
        return this.outerRadius - (this.outerRadius - this.innerRadius) * getChargeProgress();
    }

    private void progressCharging() {
        final long elapsed = System.currentTimeMillis() - this.chargeStartTime;
        // The streams finish forming around the target first, then keep tightening until fully charged
        final double formProgress = Math.min((double) elapsed / this.streamFormTime, 1.0);

        spawnStreams(formProgress, getStreamRadius(), 0.25);

        if (elapsed >= this.chargeTime) {
            this.phase = Phase.READY;
            // Airy ping to signal the target is fully surrounded and ready to crush
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_LAND, 0.7f, 2.0f);
            this.target.getWorld().playSound(this.target.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1.0f, 1.6f);
        }
    }

    private void progressReady() {
        // Fully formed streams spinning rapidly around the target
        spawnStreams(1.0, this.innerRadius, 0.45);
    }

    /**
     * Draws the streams of air spiralling up and around the target.
     *
     * @param formProgress How much of each stream has formed, from 0 to 1.
     * @param radius The distance of the streams from the target.
     * @param spinSpeed How far the streams rotate each tick.
     */
    private void spawnStreams(final double formProgress, final double radius, final double spinSpeed) {
        final Location center = getTargetCenter();
        final double height = this.target.getHeight() * 1.3;
        final int pointsPerStream = 12;
        final int drawnPoints = Math.max(1, (int) Math.ceil(pointsPerStream * formProgress));

        for (int s = 0; s < this.streams; s++) {
            final double streamOffset = s * (Math.PI * 2.0 / this.streams);
            // Alternate streams rise and fall so they cross over each other around the target
            final double direction = s % 2 == 0 ? 1 : -1;
            for (int i = 0; i < drawnPoints; i++) {
                final double t = (double) i / (pointsPerStream - 1);
                final double angle = this.animAngle + streamOffset + t * Math.PI * 1.5;
                final double y = (t - 0.5) * height * direction;
                final Location point = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                AranarthBendingUtils.playAirbendingParticles(point, 1, 0.02, 0.02, 0.02);
            }
        }
        this.animAngle += spinSpeed;
    }

    /**
     * Called by the bending listener when the player left-clicks, crushing the target.
     */
    public void crush() {
        if (!isTargetInRange() || GeneralMethods.isRegionProtectedFromBuild(this, this.target.getLocation())) {
            this.remove();
            return;
        }
        if (this.target instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
            this.remove();
            return;
        }

        this.bPlayer.addCooldown(this);

        final double chargeProgress = getChargeProgress();
        final double radius = getStreamRadius();
        final Location center = getTargetCenter();
        // The streams collapse inward onto the target, with a bigger burst the longer it was charged
        for (int i = 0; i < 24; i++) {
            final double angle = i * (Math.PI * 2.0 / 24);
            final double y = ((i % 3) - 1) * this.target.getHeight() * 0.4;
            AranarthBendingUtils.playAirbendingParticles(
                    center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                    1, 0.02, 0.02, 0.02);
        }
        AranarthBendingUtils.playAirbendingParticles(center, 4 + (int) (chargeProgress * 8), 0.2, this.target.getHeight() * 0.3, 0.2);

        // Deeper, louder crush the longer it was charged
        center.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, (float) (0.8 + chargeProgress * 0.7), (float) (1.2 - chargeProgress * 0.6));
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);

        DamageHandler.damageEntity(this.target, this.minDamage + (this.damage - this.minDamage) * chargeProgress, this);
        this.remove();
    }

    @Override
    public void remove() {
        super.remove();
        ACTIVE.remove(player.getUniqueId());
    }

    public static AirCrush getActiveInstance(final UUID uuid) {
        return ACTIVE.get(uuid);
    }

    public Phase getPhase() {
        return this.phase;
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
        return this.target != null && this.target.isValid() ? this.target.getLocation() : player.getLocation();
    }

    @Override
    public String getName() {
        return "AirCrush";
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
    public Object createNewComboInstance(final Player player) {
        return new AirCrush(player);
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        final ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("Tornado", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("AirBlast", ClickType.LEFT_CLICK));
        combo.add(new AbilityInformation("AirBlast", ClickType.SHIFT_UP));
        combo.add(new AbilityInformation("Suffocate", ClickType.SHIFT_DOWN));
        return combo;
    }

    @Override
    public String getDescription() {
        return "Wrap streams of air tightly around your target, and crush them with some or all of your strength! " +
                "The longer you hold the streams, the stronger the crush.\n" +
                ChatUtils.translateToColor("&7Usage: Tornado (Hold Sneak) > AirBlast (Left Click + Release Sneak) > Suffocate (Hold Sneak to charge + Left Click)");
    }
}
