package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.abilities.waterbending.plantbending.PetalRush.Petal;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.PetalRush.RushPath;
import com.aearost.aranarthcore.abilities.waterbending.plantbending.PetalRush.SourcedFlower;
import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.AranarthBendingUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class PetalStorm extends PlantAbility implements AddonAbility, ComboAbility {

    public enum Phase {FIRING, FINISHING}

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.DURATION)
    private long duration;
    @Attribute(Attribute.RANGE)
    private double range;

    private double speed;
    private double hitRadius;
    private double windAmplitude;
    private double windFrequency;
    private double lineLength;
    private int linePetals;
    private float linePetalSize;
    private int wispPetals;
    private float wispPetalSize;
    private double wispSpread;

    private Phase phase;
    private long startTime;
    private World world;
    private final List<SourcedFlower> sourcedFlowers = new ArrayList<>();
    private final List<Petal> petals = new ArrayList<>();
    private final List<StormShot> shots = new ArrayList<>();

    private static final double STEP = 0.25;

    private static final Map<UUID, PetalStorm> activeInstances = new HashMap<>();
    private final Random random = new Random();

    /**
     * A single in-flight line of petals, with smaller wisps drifting around it.
     */
    private static class StormShot {
        final RushPath path;
        final Particle.DustOptions lineDust;
        final Particle.DustOptions wispDust;
        double head;

        StormShot(RushPath path, Color color, float lineSize, float wispSize) {
            this.path = path;
            this.lineDust = new Particle.DustOptions(color, lineSize);
            this.wispDust = new Particle.DustOptions(color, wispSize);
        }
    }

    PetalStorm(Player player, List<SourcedFlower> flowers, List<Petal> heldPetals) {
        super(player);

        if (!bPlayer.canBendIgnoreBindsCooldowns(this) || bPlayer.isOnCooldown(this)
                || activeInstances.containsKey(player.getUniqueId())) {
            return;
        }

        cooldown = flowers.size() * AbilityConfig.getLong("PetalStorm.CooldownPerFlower");
        damage = AbilityConfig.getDouble("PetalStorm.Damage");
        duration = AbilityConfig.getLong("PetalStorm.Duration");
        range = AbilityConfig.getDouble("PetalStorm.Range");
        speed = AbilityConfig.getDouble("PetalStorm.Speed");
        hitRadius = AbilityConfig.getDouble("PetalStorm.HitRadius");
        windAmplitude = AbilityConfig.getDouble("PetalStorm.WindAmplitude");
        windFrequency = AbilityConfig.getDouble("PetalStorm.WindFrequency");
        lineLength = AbilityConfig.getDouble("PetalStorm.LineLength");
        linePetals = AbilityConfig.getInt("PetalStorm.LinePetals");
        linePetalSize = (float) AbilityConfig.getDouble("PetalStorm.LinePetalSize");
        wispPetals = AbilityConfig.getInt("PetalStorm.WispPetals");
        wispPetalSize = (float) AbilityConfig.getDouble("PetalStorm.WispPetalSize");
        wispSpread = AbilityConfig.getDouble("PetalStorm.WispSpread");

        createPetals(flowers, heldPetals);
        sourcedFlowers.addAll(flowers);

        phase = Phase.FIRING;
        startTime = System.currentTimeMillis();
        world = player.getWorld();
        world.playSound(player.getLocation(), Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.0f, 1.8f);
        start();
        if (isStarted()) {
            activeInstances.put(player.getUniqueId(), this);
        }
    }

    /**
     * Gives each flower one floating petal per shot, picking up where the petals held by PetalRush are swaying.
     */
    private void createPetals(List<SourcedFlower> flowers, List<Petal> heldPetals) {
        int shotsPerFlower = AbilityConfig.getInt("PetalStorm.ShotsPerFlower");
        float heldPetalSize = (float) AbilityConfig.getDouble("PetalRush.HeldPetalSize");
        for (SourcedFlower flower : flowers) {
            for (int i = 0; i < shotsPerFlower; i++) {
                Location start = heldPetals.isEmpty() ? player.getLocation()
                        : heldPetals.get(random.nextInt(heldPetals.size())).position;
                double angleOffset = random.nextDouble() * Math.PI * 2;
                double heightOffset = 0.6 + random.nextDouble() * 0.8;
                petals.add(new Petal(start, flower.color, heldPetalSize, angleOffset, heightOffset));
            }
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || !player.getWorld().equals(world)) {
            remove();
            return;
        }

        if (phase == Phase.FIRING && (!player.isSneaking()
                || !"PetalShots".equalsIgnoreCase(bPlayer.getBoundAbilityName())
                || !bPlayer.canBendIgnoreBindsCooldowns(this)
                || System.currentTimeMillis() - startTime >= duration)) {
            endFiring();
        }

        shots.removeIf(this::progressShot);

        if (phase == Phase.FINISHING && shots.isEmpty()) {
            remove();
            return;
        }
        PetalRush.tickPetals(player, petals);
    }

    /**
     * Advances a shot along its path by one tick's worth of movement.
     *
     * @return Whether the shot should be removed.
     */
    private boolean progressShot(StormShot shot) {
        double previousHead = shot.head;
        shot.head += speed;
        double end = Math.min(shot.head, shot.path.clearDistance);

        for (double distance = previousHead + STEP; distance <= end; distance += STEP) {
            if (hitEntity(shot, shot.path.getPosition(distance))) {
                return true;
            }
        }

        shot.path.drawLine(shot.head, linePetals, lineLength, shot.lineDust);
        drawWisps(shot);
        return shot.head - lineLength >= shot.path.clearDistance;
    }

    /**
     * Damages the first mob or player at the given point of the shot's path.
     *
     * @return Whether an entity was hit.
     */
    private boolean hitEntity(StormShot shot, Location point) {
        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(point, hitRadius)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand
                    || entity.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (living instanceof Player targetPlayer && !DominionUtils.canAttackPlayer(player, targetPlayer)) {
                continue;
            }
            DamageHandler.damageEntity(living, damage, this);
            point.getWorld().spawnParticle(Particle.DUST, point, 8, 0.2, 0.2, 0.2, 0, shot.lineDust);
            point.getWorld().playSound(point, Sound.BLOCK_PINK_PETALS_BREAK, 0.8f, 1.6f);
            return true;
        }
        return false;
    }

    /**
     * Scatters smaller petals around the length of the line so that it is not just a line.
     */
    private void drawWisps(StormShot shot) {
        for (int i = 0; i < wispPetals; i++) {
            double distance = shot.head - random.nextDouble() * lineLength;
            if (distance < 0 || distance > shot.path.clearDistance) {
                continue;
            }
            Location wisp = shot.path.getPosition(distance).add(
                    (random.nextDouble() - 0.5) * 2 * wispSpread,
                    (random.nextDouble() - 0.5) * 2 * wispSpread,
                    (random.nextDouble() - 0.5) * 2 * wispSpread);
            wisp.getWorld().spawnParticle(Particle.DUST, wisp, 1, 0, 0, 0, 0, shot.wispDust);
        }
    }

    /**
     * Fires a fully random floating petal at what the player is looking at, ending once none are left.
     */
    public void onLeftClick() {
        if (phase != Phase.FIRING || petals.isEmpty()) {
            return;
        }

        Petal petal = petals.remove(random.nextInt(petals.size()));
        Location eye = player.getEyeLocation();
        // A petal swaying through a wall must not be able to fire from the other side of it
        Location origin = AranarthBendingUtils.isPathClear(eye, petal.position)
                ? petal.position.clone() : eye.clone().subtract(0, 0.3, 0);

        shots.add(new StormShot(createPath(origin), petal.dust.getColor(), linePetalSize, wispPetalSize));

        float pitch = 0.8f + random.nextFloat() * 0.6f;
        origin.getWorld().playSound(origin, Sound.ENTITY_BREEZE_SLIDE, 1.0f, pitch);
        origin.getWorld().playSound(origin, Sound.BLOCK_PINK_PETALS_BREAK, 1.0f, pitch);

        if (petals.isEmpty()) {
            endFiring();
        }
    }

    /**
     * Creates a winding path that leads from the origin to what the player is looking at.
     */
    private RushPath createPath(Location origin) {
        Vector toTarget = getTargetLocation().toVector().subtract(origin.toVector());
        Vector direction = toTarget.lengthSquared() < 0.01
                ? player.getEyeLocation().getDirection().normalize() : toTarget.normalize();
        Vector reference = Math.abs(direction.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector right = direction.getCrossProduct(reference).normalize();
        Vector up = right.getCrossProduct(direction).normalize();
        return new RushPath(origin, direction, right, up, windAmplitude, windFrequency,
                random.nextDouble() * Math.PI * 2, random.nextDouble() * Math.PI * 2, range);
    }

    /**
     * Gets the targeted entity's center if there is one, as the petals do not fire from the player's eyes.
     */
    private Location getTargetLocation() {
        if (GeneralMethods.getTargetedEntity(player, range) instanceof LivingEntity living) {
            return living.getLocation().add(0, living.getHeight() / 2, 0);
        }
        return GeneralMethods.getTargetedLocation(player, range);
    }

    /**
     * Ends the storm's firing window: any unfired shots are lost, the cooldown begins, and in-flight shots finish.
     */
    private void endFiring() {
        if (phase == Phase.FIRING) {
            phase = Phase.FINISHING;
            petals.clear();
            bPlayer.addCooldown(this);
        }
    }

    /**
     * Ends the storm, as it can only be sustained while sneaking.
     */
    public void onSneakRelease() {
        endFiring();
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static PetalStorm getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    public Phase getPhase() {
        return phase;
    }

    @Override
    public void remove() {
        // Bending restrictions remove the ability directly, which must not skip the cooldown
        endFiring();
        super.remove();
        for (SourcedFlower flower : sourcedFlowers) {
            flower.revert();
        }
        sourcedFlowers.clear();
        petals.clear();
        shots.clear();
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
        return "PetalStorm";
    }

    @Override
    public Location getLocation() {
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
        return "Assemble an arsenal of flower petals, and rapidly fire them in the direction of your choice. " +
                "Like with other Petal abilities, the shots will be in the colors of the flowers that you've sourced.\n" +
                ChatUtils.translateToColor("&fUsage: PetalRush (Hold Sneak > Left-click flowers until charged) > PetalShots (Keep holding Sneak) > Left-click (Multiple Times)");
    }

    // Only started by PetalRush when switching slots, as PK's combo tracking cannot carry over the sourced flowers
    @Override
    public Object createNewComboInstance(Player player) {
        return null;
    }

    @Override
    public ArrayList<AbilityInformation> getCombination() {
        ArrayList<AbilityInformation> combo = new ArrayList<>();
        combo.add(new AbilityInformation("PetalRush", ClickType.SHIFT_DOWN));
        combo.add(new AbilityInformation("PetalRush", ClickType.LEFT_CLICK));
        combo.add(new AbilityInformation("PetalShots", ClickType.LEFT_CLICK));
        return combo;
    }
}
