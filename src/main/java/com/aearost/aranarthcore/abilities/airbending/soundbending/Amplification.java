package com.aearost.aranarthcore.abilities.airbending.soundbending;

import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Amplification extends SoundAbility implements AddonAbility {

    // Tracks players with an active amplification buff so the static listener can apply it
    private static final Map<UUID, Amplification> activeAmplifications = new HashMap<>();

    @Attribute(Attribute.CHARGE_DURATION)
    private long chargeDuration;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    private static final double MAX_MULTIPLIER = 3.0;
    private static final long ACTIVE_DURATION = 8000L;

    private long chargeStartTime;
    private long activeStartTime;
    private boolean isCharged;
    private boolean isActive;
    private boolean consumed;
    private CoreAbility triggeredAbility;
    private double lockedMultiplier;

    public Amplification(Player player) {
        super(player);

        this.chargeDuration = 5000L;
        this.cooldown = 8000L;

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        this.chargeStartTime = System.currentTimeMillis();
        this.isCharged = false;
        this.isActive = false;
        this.consumed = false;
        this.lockedMultiplier = MAX_MULTIPLIER;

        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (isActive) {
            progressActive();
            return;
        }

        if (!bPlayer.canBend(this)) {
            remove();
            return;
        }

        if (!player.isSneaking()) {
            if (isCharged) {
                activate();
            } else {
                remove(); // silently cancel as the charge is not complete
            }
            return;
        }

        long elapsed = System.currentTimeMillis() - chargeStartTime;
        if (elapsed >= chargeDuration) {
            isCharged = true;
        }

        spawnChargeRings(elapsed);
    }

    private void activate() {
        isActive = true;
        activeStartTime = System.currentTimeMillis();
        activeAmplifications.put(player.getUniqueId(), this);
        // Cooldown covers the active window + post-window cooldown so the
        // next use is always gated 8 full seconds after the active window expires.
        bPlayer.addCooldown(this, cooldown + ACTIVE_DURATION);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 1.25f);
    }

    private void progressActive() {
        long elapsed = System.currentTimeMillis() - activeStartTime;

        if (elapsed >= ACTIVE_DURATION) {
            remove();
            return;
        }

        if (consumed) {
            // Stay alive until the triggered ability ends so all its hits receive the multiplier
            if (!CoreAbility.hasAbility(player, triggeredAbility.getClass())) {
                remove();
            }
            return;
        }

        // Consume immediately when the player starts any SoundAbility, even before it hits anything
        CoreAbility active = findActiveSoundAbility();
        if (active != null) {
            consumed = true;
            lockedMultiplier = getCurrentMultiplier();
            triggeredAbility = active;
            return;
        }

        spawnBodyRings(elapsed);
    }

    /**
     * Returns the first active SoundAbility for this player that is not Amplification itself,
     * or null if none is currently running.
     */
    private CoreAbility findActiveSoundAbility() {
        for (final Class<? extends SoundAbility> clazz : List.of(
                DeafeningScream.class, SonicPulse.class, SonicClap.class, SonicBoom.class)) {
            for (final CoreAbility ability : CoreAbility.getAbilities(clazz)) {
                if (ability.getPlayer().equals(player)) {
                    return ability;
                }
            }
        }
        return null;
    }

    private double getCurrentMultiplier() {
        long elapsed = System.currentTimeMillis() - activeStartTime;
        double t = Math.min((double) elapsed / ACTIVE_DURATION, 1.0);
        // Linearly interpolate from MAX_MULTIPLIER (3x) down to 1x over 8 seconds
        return MAX_MULTIPLIER + (1.0 - MAX_MULTIPLIER) * t;
    }

    private static final double HALO_HEIGHT_STAND = 2.0;
    private static final double HALO_HEIGHT_SNEAK = 1.5;
    private static final double HALO_RADIUS = 0.45;
    private static final int HALO_POINTS = 18;
    private static final double HAND_RADIUS = 0.35;
    private static final int HAND_POINTS = 14;
    private static final double HAND_OFFSET = 0.5;
    private static final double MERGE_START = 0.75;
    private static final int FLASH_STEPS = 12;

    private double getHaloHeight() {
        return player.isSneaking() ? HALO_HEIGHT_SNEAK : HALO_HEIGHT_STAND;
    }

    /**
     * Two halos (one beside each hand) rise in discrete flashing steps from the player's feet
     * to above their head. Past MERGE_START they gradually converge into a single centred halo.
     */
    private void spawnChargeRings(long elapsed) {
        double chargeProgress = Math.min((double) elapsed / chargeDuration, 1.0);
        Location base = player.getLocation();

        // Discrete stepping: height jumps at each step boundary
        long stepDuration = chargeDuration / FLASH_STEPS;
        int currentStep = Math.min((int) (elapsed / stepDuration), FLASH_STEPS);
        long stepElapsed = elapsed % stepDuration;
        long flashOn = stepDuration / 2; // visible for the first half of each step

        // Once fully charged keep the halo permanently visible; otherwise respect the flash gap
        if (chargeProgress < 1.0 && stepElapsed >= flashOn) {
            return;
        }

        // Show only every other flash (steps 1, 3, 5 ... in 1-indexed terms)
        if (chargeProgress < 1.0 && currentStep % 2 != 0) {
            return;
        }

        double y = ((double) currentStep / FLASH_STEPS) * HALO_HEIGHT_SNEAK;

        // Merge factor: 0 at MERGE_START, 1.0 at full charge
        double mergeProgress = chargeProgress >= MERGE_START
                ? Math.min((chargeProgress - MERGE_START) / (1.0 - MERGE_START), 1.0)
                : 0.0;

        double currentOffset = HAND_OFFSET * (1.0 - mergeProgress);
        double currentRadius = HAND_RADIUS + (HALO_RADIUS - HAND_RADIUS) * mergeProgress;
        int currentPoints = HAND_POINTS + (int) ((HALO_POINTS - HAND_POINTS) * mergeProgress);

        if (currentOffset < 0.02) {
            spawnHorizontalRing(base.clone().add(0, y, 0), currentRadius, currentPoints);
        } else {
            Vector facing = base.getDirection().setY(0).normalize();
            Vector right = new Vector(facing.getZ(), 0, -facing.getX());
            spawnHorizontalRing(base.clone().add(right.clone().multiply(-currentOffset)).add(0, y, 0), currentRadius, currentPoints);
            spawnHorizontalRing(base.clone().add(right.clone().multiply(currentOffset)).add(0, y, 0), currentRadius, currentPoints);
        }
    }

    /**
     * Single centred halo above the player's head, height-adjusted for sneaking.
     */
    private void spawnBodyRings(long elapsed) {
        spawnHorizontalRing(player.getLocation().add(0, getHaloHeight(), 0), HALO_RADIUS, HALO_POINTS);
    }

    /**
     * Spawns a horizontal (ground-parallel) ring of turquoise dust particles.
     */
    private void spawnHorizontalRing(Location center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (2.0 * Math.PI / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0, AranarthBendingUtils.SOUND_RING_DUST);
        }
    }

    @Override
    public void remove() {
        activeAmplifications.remove(player.getUniqueId());
        super.remove();
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "Amplification";
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    /**
     * Returns the active Amplification buff for {@code player}, or null if none.
     */
    public static Amplification getActiveAmplification(Player player) {
        return activeAmplifications.get(player.getUniqueId());
    }

    /**
     * Called by the bending listener when a non-Amplification SoundAbility deals damage.
     * Locks in the multiplier on first contact and applies it to every hit from the same cast.
     */
    public void applyMultiplier(AbilityDamageEntityEvent event) {
        if (!isActive) {
            return;
        }

        Ability ability = event.getAbility();
        if (!consumed) {
            consumed = true;
            lockedMultiplier = getCurrentMultiplier();
            triggeredAbility = (CoreAbility) ability;
        }

        if (triggeredAbility == ability) {
            event.setDamage(event.getDamage() * lockedMultiplier);
        }
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
        return "Compress the air around you and release it in a burst of amplification, supercharging your next damaging Sound ability. " +
                "Hold sneak to charge, then release to empower your next soundbending ability - " +
                "up to 3x damage if used immediately, decaying over time.\n" +
                ChatUtils.translateToColor("&fUsage: Hold Sneak (to charge) > Release Sneak > Use a " + SoundAbility.SOUND.getColor() + "Sound &fability");
    }

}
