package com.aearost.aranarthcore.abilities.waterbending.plantbending;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

public class Regrowth extends PlantAbility implements AddonAbility {

    @Attribute(Attribute.RANGE)
    private double radius;
    @Attribute("MinGrowIntervalMs")
    private long minGrowIntervalMs;
    @Attribute("MaxGrowIntervalMs")
    private long maxGrowIntervalMs;

    private long lastGrowTime;
    private long nextGrowInterval;

    // Weighted plant pool
    private static final List<Material> PLANT_POOL = List.of(
            Material.SHORT_GRASS,
            Material.SHORT_GRASS,
            Material.SHORT_GRASS,
            Material.DANDELION,
            Material.POPPY
    );

    private static final Set<Material> VALID_SOIL = Set.of(
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.ROOTED_DIRT
    );

    private static final Particle.DustOptions GROW_DUST =
            new Particle.DustOptions(Color.fromRGB(60, 180, 40), 0.7f);

    private static final Map<UUID, Regrowth> activeInstances = new HashMap<>();
    private final Random random = new Random();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public Regrowth(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        radius = 5.0;
        minGrowIntervalMs = 500;
        maxGrowIntervalMs = 1000;

        lastGrowTime = System.currentTimeMillis();
        nextGrowInterval = randomInterval();

        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    // -------------------------------------------------------------------------
    // Progress
    // -------------------------------------------------------------------------

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }
        if (!player.isSneaking()) {
            remove();
            return;
        }

        spawnAmbientParticles();

        long now = System.currentTimeMillis();
        if (now - lastGrowTime >= nextGrowInterval) {
            tryGrowPlant();
            lastGrowTime = now;
            nextGrowInterval = randomInterval();
        }
    }

    // -------------------------------------------------------------------------
    // Growth logic
    // -------------------------------------------------------------------------

    private void tryGrowPlant() {
        Location playerLoc = player.getLocation();
        int r = (int) Math.ceil(radius);
        double radiusSq = radius * radius;

        List<Block> candidates = new ArrayList<>();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // Horizontal cylinder check
                if (x * x + z * z > radiusSq) {
                    continue;
                }
                // Scan a few blocks above and below the player's feet
                for (int dy = -3; dy <= 3; dy++) {
                    Block soil = playerLoc.getBlock().getRelative(x, dy, z);
                    if (!VALID_SOIL.contains(soil.getType())) {
                        continue;
                    }
                    Block above = soil.getRelative(BlockFace.UP);
                    if (above.getType() != Material.AIR) {
                        continue;
                    }
                    candidates.add(above);
                }
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        Block target = candidates.get(random.nextInt(candidates.size()));
        Material plant = PLANT_POOL.get(random.nextInt(PLANT_POOL.size()));
        target.setType(plant);

        Location loc = target.getLocation().add(0.5, 0.3, 0.5);
        player.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.25, 0.25, 0.25, 0, GROW_DUST);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.2, 0.2, 0.2, 0);
        player.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, 0.4f, 1.2f);
    }

    private void spawnAmbientParticles() {
        Location playerLoc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 0.5 + random.nextDouble() * (radius - 0.5);
            double dx = Math.cos(angle) * dist;
            double dz = Math.sin(angle) * dist;
            double dy = random.nextDouble() * 2 - 0.5;
            world.spawnParticle(Particle.DUST, playerLoc.clone().add(dx, dy, dz),
                    1, 0, 0, 0, 0, GROW_DUST);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long randomInterval() {
        return minGrowIntervalMs + (long) (random.nextDouble() * (maxGrowIntervalMs - minGrowIntervalMs));
    }

    // -------------------------------------------------------------------------
    // Static registry
    // -------------------------------------------------------------------------

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Regrowth getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
    }

    // -------------------------------------------------------------------------
    // PK ability interface
    // -------------------------------------------------------------------------

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "Regrowth";
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
        return "Channel your plantbending to speed up the growth of grass and flowers into bloom around you.\n" +
                ChatUtils.translateToColor("&fUsage: Sneak (Hold)");
    }
}
