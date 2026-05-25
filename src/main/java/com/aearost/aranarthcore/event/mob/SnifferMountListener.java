package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Makes Sniffers rideable mounts with saddle-based access, randomised speed/health,
 * two-passenger support, auto step-up, and a space-hold tunnel-digging ability.
 */
public class SnifferMountListener implements Listener {

    /** Upward impulse for stepping up a 1-block obstacle. */
    private static final double STEP_UP_VELOCITY = 0.42;

    // Speed range: 8–20 blocks/second  →  0.40–1.00 blocks/tick (at 20 tps)
    private static final double MIN_SPEED = 0.40;
    private static final double MAX_SPEED = 1.00;

    /** Sniffer UUIDs that currently have at least one rider. */
    private static final Set<UUID> mountedSniffers = new HashSet<>();
    /** Per-sniffer mounted speed cached on first mount, loaded from PDC. */
    private static final Map<UUID, Double> snifferSpeeds = new HashMap<>();
    /**
     * Stored WASD input per rider UUID: {forward, strafe}.
     * Updated each PlayerInputEvent; consumed every tick by the movement scheduler.
     */
    private static final Map<UUID, double[]> playerInputs = new HashMap<>();
    /** Active tunnel tasks keyed by sniffer UUID. */
    static final Map<UUID, SnifferTunnel> activeTunnels = new HashMap<>();

    private final Random random = new Random();
    private final AranarthCore plugin;

    public SnifferMountListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Server-side movement + rotation — runs every tick for all mounted sniffers.
        // Velocity is ALWAYS set (even 0 horizontal) to override the sniffer's own AI movement.
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : new HashSet<>(mountedSniffers)) {
                    Entity e = Bukkit.getEntity(id);
                    if (!(e instanceof Sniffer sniffer) || sniffer.isDead()) {
                        cleanupSniffer(id);
                        continue;
                    }
                    List<Entity> passengers = sniffer.getPassengers();
                    if (passengers.isEmpty()) {
                        cleanupSniffer(id);
                        continue;
                    }
                    if (!(passengers.get(0) instanceof Player rider)) continue;

                    float riderYaw = rider.getLocation().getYaw();
                    sniffer.setRotation(riderYaw, 0);
                    sniffer.setBodyYaw(riderYaw);

                    double[] in = playerInputs.getOrDefault(rider.getUniqueId(), new double[]{0, 0});
                    double forward = in[0];
                    double strafe  = in[1];

                    double speed = snifferSpeeds.computeIfAbsent(id, uid -> readSpeed(sniffer));

                    double yaw = Math.toRadians(riderYaw);
                    double vx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
                    double vz =  Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
                    double len = Math.sqrt(vx * vx + vz * vz);
                    if (len > 0.001) {
                        vx = (vx / len) * speed;
                        vz = (vz / len) * speed;
                    }

                    double vertY = sniffer.getVelocity().getY();

                    // -------------------------------------------------------
                    // Auto step-up over 1-block obstacles (not fences/walls).
                    // Only attempt step-up when there is actual horizontal movement.
                    // -------------------------------------------------------
                    if (sniffer.isOnGround() && len > 0.001) {
                        double nx = vx / speed;
                        double nz = vz / speed;
                        Location loc = sniffer.getLocation();
                        int iy = (int) Math.floor(loc.getY());

                        for (double ld : new double[]{1.3, 1.6, 1.9}) {
                            int cx = (int) Math.floor(loc.getX() + nx * ld);
                            int cz = (int) Math.floor(loc.getZ() + nz * ld);
                            Block foot = sniffer.getWorld().getBlockAt(cx, iy, cz);
                            Block clear1 = sniffer.getWorld().getBlockAt(cx, iy + 1, cz);
                            Block clear2 = sniffer.getWorld().getBlockAt(cx, iy + 2, cz);
                            if (isSteppableObstacle(foot)
                                    && isClearForPassage(clear1)
                                    && isClearForPassage(clear2)) {
                                vertY = STEP_UP_VELOCITY;
                                break;
                            }
                        }
                    }

                    // Always set velocity — zeroing horizontal when idle suppresses AI wandering
                    sniffer.setVelocity(new Vector(vx, vertY, vz));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // -------------------------------------------------------------------------
    // Spawn — randomise health (32–100 half-hearts) and mounted speed (8–20 b/s)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onSnifferSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Sniffer sniffer)) return;

        // Skip randomisation for sniffers restored from player login data
        if (sniffer.getPersistentDataContainer().has(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE)) return;

        // Health: 32–100 half-hearts (16–50 full hearts)
        double health = 32 + random.nextInt(69);
        var maxHp = sniffer.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(health);
            sniffer.setHealth(health);
        }

        // Mounted speed: 8–20 blocks/second  →  0.40–1.00 blocks/tick
        double speed = MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED);
        sniffer.getPersistentDataContainer()
                .set(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE, speed);
    }

    // -------------------------------------------------------------------------
    // Interaction — right-click to saddle/unsaddle/mount
    // -------------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Sniffer sniffer)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Holding saddle → equip it
        if (mainHand.getType() == Material.SADDLE) {
            if (!hasSaddle(sniffer)) {
                setSaddle(sniffer, true);
                mainHand.setAmount(mainHand.getAmount() - 1);
                player.playSound(sniffer.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);
            }
            return;
        }

        // Sneak + empty hand → remove saddle
        if (player.isSneaking() && mainHand.getType().isAir()) {
            if (hasSaddle(sniffer)) {
                setSaddle(sniffer, false);
                player.getInventory().addItem(new ItemStack(Material.SADDLE));
                player.playSound(sniffer.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 0.6f);
            }
            return;
        }

        // No saddle → tell the player
        if (!hasSaddle(sniffer)) {
            player.sendMessage(ChatUtils.translateToColor("&cYou need a saddle to ride this sniffer!"));
            return;
        }

        // Mount (up to 2 riders)
        List<Entity> passengers = sniffer.getPassengers();
        if (passengers.contains(player)) return;
        if (passengers.size() >= 2) {
            player.sendMessage(ChatUtils.translateToColor("&cThis sniffer already has two riders!"));
            return;
        }

        mountSniffer(sniffer, player);
    }

    // -------------------------------------------------------------------------
    // Movement input (WASD + space)
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Sniffer sniffer)) return;
        if (!mountedSniffers.contains(sniffer.getUniqueId())) return;

        List<Entity> passengers = sniffer.getPassengers();
        if (passengers.isEmpty() || !passengers.get(0).equals(player)) return;

        var input = event.getInput();

        double forward = 0;
        if (input.isForward())  forward += 1;
        if (input.isBackward()) forward -= 1;
        double strafe = 0;
        if (input.isLeft())  strafe += 1;
        if (input.isRight()) strafe -= 1;

        playerInputs.put(player.getUniqueId(), new double[]{forward, strafe});

        UUID snifferUUID = sniffer.getUniqueId();

        if (input.isJump()) {
            // Start tunnel if one isn't already running
            if (!activeTunnels.containsKey(snifferUUID)) {
                SnifferTunnel tunnel = new SnifferTunnel(player, sniffer, activeTunnels);
                activeTunnels.put(snifferUUID, tunnel);
                tunnel.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
                player.playSound(sniffer.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 1.2f, 0.7f);
            }
        } else {
            // Space released — stop digging
            SnifferTunnel tunnel = activeTunnels.remove(snifferUUID);
            if (tunnel != null) tunnel.cancel();
        }
    }

    // -------------------------------------------------------------------------
    // Dismount cleanup
    // -------------------------------------------------------------------------

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getDismounted() instanceof Sniffer sniffer)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        playerInputs.remove(player.getUniqueId());

        UUID snifferUUID = sniffer.getUniqueId();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            Entity e = Bukkit.getEntity(snifferUUID);
            if (e instanceof Sniffer s && s.getPassengers().isEmpty()) {
                cleanupSniffer(snifferUUID);
            }
        }, 1L);
    }

    // -------------------------------------------------------------------------
    // Death cleanup + saddle drop
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Sniffer sniffer)) return;

        UUID id = sniffer.getUniqueId();
        cleanupSniffer(id);

        if (hasSaddle(sniffer)) {
            sniffer.getWorld().dropItemNaturally(sniffer.getLocation(), new ItemStack(Material.SADDLE));
        }
    }

    // -------------------------------------------------------------------------
    // Logout — save sniffer stats to player PDC, despawn the sniffer
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof Sniffer sniffer)) return;

        // Save stats to player PDC so we can restore them on next login
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();

        var maxHpAttr = sniffer.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHpAttr != null ? maxHpAttr.getBaseValue() : 60;
        double health    = sniffer.getHealth();
        double speed     = readSpeed(sniffer);
        boolean hasSaddle = hasSaddle(sniffer);

        playerPdc.set(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH, PersistentDataType.DOUBLE, maxHealth);
        playerPdc.set(CustomKeys.PLAYER_SNIFFER_HEALTH,     PersistentDataType.DOUBLE, health);
        playerPdc.set(CustomKeys.PLAYER_SNIFFER_SPEED,      PersistentDataType.DOUBLE, speed);
        // Re-use SNIFFER_SADDLE key but stored on the player PDC
        playerPdc.set(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE, hasSaddle ? (byte) 1 : (byte) 0);

        cleanupSniffer(sniffer.getUniqueId());
        sniffer.remove();
    }

    // -------------------------------------------------------------------------
    // Login — restore saved sniffer and auto-mount the player
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();

        if (!playerPdc.has(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH, PersistentDataType.DOUBLE)) return;

        double maxHealth = playerPdc.get(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH, PersistentDataType.DOUBLE);
        double health    = playerPdc.get(CustomKeys.PLAYER_SNIFFER_HEALTH,     PersistentDataType.DOUBLE);
        double speed     = playerPdc.get(CustomKeys.PLAYER_SNIFFER_SPEED,      PersistentDataType.DOUBLE);
        byte   saddleByte = playerPdc.getOrDefault(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE, (byte) 1);

        // Clear the saved data
        playerPdc.remove(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH);
        playerPdc.remove(CustomKeys.PLAYER_SNIFFER_HEALTH);
        playerPdc.remove(CustomKeys.PLAYER_SNIFFER_SPEED);
        playerPdc.remove(CustomKeys.SNIFFER_SADDLE);

        // Delay slightly to ensure the player is fully loaded into the world
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            World world = player.getWorld();
            Location spawnLoc = player.getLocation();

            Sniffer sniffer = world.spawn(spawnLoc, Sniffer.class, s -> {
                // Apply stats before the spawn event randomises them
                var maxHpAttr = s.getAttribute(Attribute.MAX_HEALTH);
                if (maxHpAttr != null) {
                    maxHpAttr.setBaseValue(maxHealth);
                }
                s.setHealth(Math.min(health, maxHealth));
                s.getPersistentDataContainer().set(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE, speed);
                s.getPersistentDataContainer().set(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE, saddleByte);
            });

            mountSniffer(sniffer, player);
        }, 10L);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Adds the player as a passenger and registers the sniffer as mounted. */
    private void mountSniffer(Sniffer sniffer, Player player) {
        sniffer.addPassenger(player);
        mountedSniffers.add(sniffer.getUniqueId());
        snifferSpeeds.put(sniffer.getUniqueId(), readSpeed(sniffer));
        // Zero out the AI movement speed so the sniffer's goal selector cannot
        // move it on its own. Our velocity-based control bypasses this attribute.
        var speedAttr = sniffer.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.0);
    }

    private void cleanupSniffer(UUID id) {
        mountedSniffers.remove(id);
        snifferSpeeds.remove(id);
        SnifferTunnel tunnel = activeTunnels.remove(id);
        if (tunnel != null) tunnel.cancel();
        // Restore the sniffer's natural movement speed so it can wander again
        Entity e = Bukkit.getEntity(id);
        if (e instanceof Sniffer s && !s.isDead()) {
            var speedAttr = s.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(speedAttr.getDefaultValue());
        }
    }

    /**
     * Returns true when the block is a solid obstacle the sniffer can step over —
     * its collision height must be ≤ 1.0 (excludes fences/walls at 1.5 blocks).
     */
    private boolean isSteppableObstacle(Block block) {
        if (block.isPassable()) return false;
        double h = block.getBoundingBox().getHeight();
        return h > 0 && h <= 1.0;
    }

    /**
     * Returns true when the block does not meaningfully obstruct the sniffer's body
     * above a step (air, plants, thin snow layers, etc.).
     */
    private boolean isClearForPassage(Block block) {
        if (block.isPassable()) return true;
        // Thin snow layers (< 8 layers) are passable in practice
        return block.getType() == Material.SNOW && block.getBoundingBox().getHeight() < 0.5;
    }

    private boolean hasSaddle(Sniffer sniffer) {
        PersistentDataContainer pdc = sniffer.getPersistentDataContainer();
        return pdc.has(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE)
                && pdc.get(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE) == 1;
    }

    private void setSaddle(Sniffer sniffer, boolean value) {
        sniffer.getPersistentDataContainer().set(
                CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }

    private double readSpeed(Sniffer sniffer) {
        PersistentDataContainer pdc = sniffer.getPersistentDataContainer();
        if (pdc.has(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE)) {
            return pdc.get(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE);
        }
        // Fallback for sniffers that spawned before this feature was added
        return (MIN_SPEED + MAX_SPEED) / 2.0;
    }
}
