package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthMount;
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
 * Centralised listener for all Aranarth-managed custom mounts.
 */
public class MountListener implements Listener {

    /**
     * Encapsulates optional, mount-type-specific behaviour triggered by the
     * rider pressing or releasing space while mounted.
     */
    interface SpecialAction {
        /**
         * Called once when the rider starts holding space.
         */
        void start(LivingEntity mount, Player rider, AranarthMount mountData);

        /**
         * Called once when the rider releases space.
         */
        void stop(UUID mountId);

        /**
         * Called on any cleanup path (dismount, death, logout).
         */
        void cleanup(UUID mountId);
    }

    static final class SnifferTunnelAction implements SpecialAction {

        final Map<UUID, SnifferTunnel> activeTunnels = new HashMap<>();

        @Override
        public void start(LivingEntity mount, Player rider, AranarthMount mountData) {
            UUID id = mount.getUniqueId();
            if (activeTunnels.containsKey(id)) {
                return;
            }
            int tunnelSpeed = mountData.getThirdAttribute() != null
                    ? (int) mountData.getThirdAttribute().doubleValue()
                    : 450;
            SnifferTunnel tunnel = new SnifferTunnel(rider, (Sniffer) mount, activeTunnels, tunnelSpeed);
            activeTunnels.put(id, tunnel);
            tunnel.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
            rider.playSound(mount.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, 1.2f, 0.7f);
        }

        @Override
        public void stop(UUID mountId) {
            SnifferTunnel tunnel = activeTunnels.remove(mountId);
            if (tunnel != null) {
                tunnel.cancel();
            }
        }

        @Override
        public void cleanup(UUID mountId) {
            stop(mountId);
        }
    }

    /**
     * Immutable configuration for a single mount entity type.
     *
     * @param entityClass          Bukkit entity class (used for spawning).
     * @param minHealth            Minimum randomised max-health (half-hearts).
     * @param maxHealth            Maximum randomised max-health (half-hearts).
     * @param minSpeed             Minimum movement speed (blocks/tick).
     * @param maxSpeed             Maximum movement speed (blocks/tick).
     * @param minThirdAttr         Minimum third-attribute value, or {@code null}.
     * @param maxThirdAttr         Maximum third-attribute value, or {@code null}.
     * @param thirdAttrLabel       Display name for the third attribute, or {@code null}.
     * @param requiresSaddle       Whether a saddle must be equipped before riding.
     * @param despawnOnOwnerLogout Whether the mount is removed when its rider logs out
     *                             and re-spawned on login (true for Sniffers; typically
     *                             false for mounts that persist in the world).
     * @param specialAction        Optional space-bar ability, or {@code null}.
     */
    record MountTypeConfig(
            Class<? extends LivingEntity> entityClass,
            double minHealth, double maxHealth,
            double minSpeed, double maxSpeed,
            Double minThirdAttr, Double maxThirdAttr,
            String thirdAttrLabel,
            boolean requiresSaddle,
            boolean despawnOnOwnerLogout,
            SpecialAction specialAction) {
        boolean hasThirdAttr() {
            return thirdAttrLabel != null && minThirdAttr != null;
        }
    }

    private static final SnifferTunnelAction SNIFFER_TUNNEL = new SnifferTunnelAction();

    static final Map<Class<? extends LivingEntity>, MountTypeConfig> MOUNT_CONFIGS = new LinkedHashMap<>();

    static {
        MOUNT_CONFIGS.put(Sniffer.class, new MountTypeConfig(
                Sniffer.class,
                32, 100, // Health
                0.40, 1.00, // Speed (blocks/tick - 8–20 m/s)
                200.0, 600.0, // Tunnel speed range (blocks/tick)
                "Dig Speed",
                true,
                true,
                SNIFFER_TUNNEL
        ));
//         Template for future mounts
//
//         MOUNT_CONFIGS.put(NewMob.class, new MountTypeConfig(
//                 NewMob.class,
//                 20, 20, 0.5, 1.0,
//                 null, null, null,   // no third attribute
//                 true, false, null   // saddle required, persists, no special action
//         ));
    }

    private static final double STEP_UP_VELOCITY = 0.42;
    private static final Map<UUID, AranarthMount> activeMounts = new HashMap<>();
    private static final Map<UUID, double[]> playerInputs = new HashMap<>();
    private static final Map<UUID, SpecialAction> activeSpecialActions = new HashMap<>();
    private final Random random = new Random();

    public MountListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID id : new HashSet<>(activeMounts.keySet())) {
                    Entity e = Bukkit.getEntity(id);
                    if (!(e instanceof LivingEntity mount) || mount.isDead()) {
                        cleanupMount(id);
                        continue;
                    }
                    List<Entity> passengers = mount.getPassengers();
                    if (passengers.isEmpty()) {
                        cleanupMount(id);
                        continue;
                    }
                    if (!(passengers.get(0) instanceof Player rider)) {
                        continue;
                    }
                    applyMovement(mount, rider);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private MountTypeConfig getConfig(LivingEntity entity) {
        for (Map.Entry<Class<? extends LivingEntity>, MountTypeConfig> entry : MOUNT_CONFIGS.entrySet()) {
            if (entry.getKey().isInstance(entity)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @EventHandler
    public void onMountSpawn(CreatureSpawnEvent event) {
        MountTypeConfig config = getConfig(event.getEntity());
        if (config == null) {
            return;
        }
        LivingEntity mount = event.getEntity();

        // Skip if stats already set - indicates restoration from player login data
        if (mount.getPersistentDataContainer().has(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE)) {
            return;
        }
        randomizeStats(mount, config);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity clickedEntity)) {
            return;
        }
        MountTypeConfig config = getConfig(clickedEntity);
        if (config == null) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // Let vanilla attach it without interference
        if (mainHand.getType() == Material.LEAD) {
            return;
        }

        event.setCancelled(true);

        // Check ownership before any other interaction
        UUID ownerUUID = readOwner(clickedEntity);
        if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage("&cThis mount belongs to someone else!"));
            return;
        }

        if (config.requiresSaddle()) {
            // Equip it
            if (mainHand.getType() == Material.SADDLE) {
                if (!hasSaddle(clickedEntity)) {
                    setSaddle(clickedEntity, true);
                    mainHand.setAmount(mainHand.getAmount() - 1);
                    player.playSound(clickedEntity.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);
                }
                return;
            }

            // Remove saddle
            if (player.isSneaking() && mainHand.getType().isAir()) {
                if (hasSaddle(clickedEntity)) {
                    setSaddle(clickedEntity, false);
                    player.getInventory().addItem(new ItemStack(Material.SADDLE));
                    player.playSound(clickedEntity.getLocation(), Sound.ENTITY_HORSE_SADDLE, 1.0f, 0.6f);
                }
                return;
            }

            // Attempting to mount without a saddle
            if (!hasSaddle(clickedEntity)) {
                if (mainHand.getType().isAir()) {
                    player.sendMessage(ChatUtils.chatMessage("&cYou need a saddle to ride this mount!"));
                }
                return;
            }
        }

        // Mount (up to 2 riders)
        List<Entity> passengers = clickedEntity.getPassengers();
        if (passengers.contains(player)) {
            return;
        }
        if (passengers.size() >= 2) {
            player.sendMessage(ChatUtils.chatMessage("&cThis mount already has two riders!"));
            return;
        }

        mountEntity(clickedEntity, player, config);
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof LivingEntity mount)) {
            return;
        }
        if (!activeMounts.containsKey(mount.getUniqueId())) {
            return;
        }

        List<Entity> passengers = mount.getPassengers();
        if (passengers.isEmpty() || !passengers.get(0).equals(player)) {
            return;
        }

        var input = event.getInput();
        double forward = (input.isForward() ? 1 : 0) - (input.isBackward() ? 1 : 0);
        double strafe = (input.isLeft() ? 1 : 0) - (input.isRight() ? 1 : 0);
        playerInputs.put(player.getUniqueId(), new double[]{forward, strafe});

        UUID mountId = mount.getUniqueId();
        SpecialAction action = activeSpecialActions.get(mountId);
        if (action != null) {
            if (input.isJump()) {
                action.start(mount, player, activeMounts.get(mountId));
            } else {
                action.stop(mountId);
            }
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getDismounted() instanceof LivingEntity mount)) {
            return;
        }
        if (getConfig(mount) == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        playerInputs.remove(player.getUniqueId());

        UUID mountId = mount.getUniqueId();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            Entity e = Bukkit.getEntity(mountId);
            if (e instanceof LivingEntity m && m.getPassengers().isEmpty()) {
                cleanupMount(mountId);
            }
        }, 1L);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mount = event.getEntity();
        MountTypeConfig config = getConfig(mount);
        if (config == null) {
            return;
        }
        UUID id = mount.getUniqueId();
        cleanupMount(id);

        if (config.requiresSaddle() && hasSaddle(mount)) {
            mount.getWorld().dropItemNaturally(mount.getLocation(), new ItemStack(Material.SADDLE));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof LivingEntity mount)) {
            return;
        }
        MountTypeConfig config = getConfig(mount);
        if (config == null || !config.despawnOnOwnerLogout()) {
            return;
        }

        PersistentDataContainer playerPdc = player.getPersistentDataContainer();

        var maxHpAttr = mount.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHpAttr != null ? maxHpAttr.getBaseValue() : 60;
        double health = mount.getHealth();
        double speed = readSpeed(mount);
        boolean saddle = hasSaddle(mount);
        UUID ownerUUID = readOwner(mount);

        playerPdc.set(CustomKeys.PLAYER_MOUNT_TYPE, PersistentDataType.STRING, mount.getClass().getSimpleName());
        playerPdc.set(CustomKeys.PLAYER_MOUNT_MAX_HEALTH, PersistentDataType.DOUBLE, maxHealth);
        playerPdc.set(CustomKeys.PLAYER_MOUNT_HEALTH, PersistentDataType.DOUBLE, health);
        playerPdc.set(CustomKeys.PLAYER_MOUNT_SPEED, PersistentDataType.DOUBLE, speed);
        playerPdc.set(CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE, saddle ? (byte) 1 : (byte) 0);
        if (ownerUUID != null) {
            playerPdc.set(CustomKeys.PLAYER_MOUNT_OWNER, PersistentDataType.STRING, ownerUUID.toString());
        }
        if (config.hasThirdAttr()) {
            playerPdc.set(CustomKeys.PLAYER_MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE, readThirdAttr(mount, config));
        }

        cleanupMount(mount.getUniqueId());
        mount.remove();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerPdc = player.getPersistentDataContainer();

        // Detect new-style save data
        boolean hasNewKey = playerPdc.has(CustomKeys.PLAYER_MOUNT_MAX_HEALTH, PersistentDataType.DOUBLE);
        boolean hasLegacyKey = playerPdc.has(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH, PersistentDataType.DOUBLE);
        if (!hasNewKey && !hasLegacyKey) {
            return;
        }

        final String entityTypeName;
        final double maxHealth, health, speed;
        final double thirdAttr;
        final byte saddleByte;
        final String ownerString;

        if (hasNewKey) {
            entityTypeName = playerPdc.getOrDefault(CustomKeys.PLAYER_MOUNT_TYPE, PersistentDataType.STRING, "Sniffer");
            maxHealth = playerPdc.get(CustomKeys.PLAYER_MOUNT_MAX_HEALTH, PersistentDataType.DOUBLE);
            health = playerPdc.get(CustomKeys.PLAYER_MOUNT_HEALTH, PersistentDataType.DOUBLE);
            speed = playerPdc.get(CustomKeys.PLAYER_MOUNT_SPEED, PersistentDataType.DOUBLE);
            thirdAttr = playerPdc.getOrDefault(CustomKeys.PLAYER_MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE, 450.0);
            saddleByte = playerPdc.getOrDefault(CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE, (byte) 1);
            ownerString = playerPdc.get(CustomKeys.PLAYER_MOUNT_OWNER, PersistentDataType.STRING);

            playerPdc.remove(CustomKeys.PLAYER_MOUNT_TYPE);
            playerPdc.remove(CustomKeys.PLAYER_MOUNT_MAX_HEALTH);
            playerPdc.remove(CustomKeys.PLAYER_MOUNT_HEALTH);
            playerPdc.remove(CustomKeys.PLAYER_MOUNT_SPEED);
            playerPdc.remove(CustomKeys.PLAYER_MOUNT_THIRD_ATTR);
            playerPdc.remove(CustomKeys.MOUNT_SADDLE);
            playerPdc.remove(CustomKeys.PLAYER_MOUNT_OWNER);
        } else {
            // Migrate from legacy sniffer-specific keys
            entityTypeName = "Sniffer";
            maxHealth = playerPdc.get(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH, PersistentDataType.DOUBLE);
            health = playerPdc.get(CustomKeys.PLAYER_SNIFFER_HEALTH, PersistentDataType.DOUBLE);
            speed = playerPdc.get(CustomKeys.PLAYER_SNIFFER_SPEED, PersistentDataType.DOUBLE);
            thirdAttr = 450.0;
            saddleByte = playerPdc.getOrDefault(CustomKeys.SNIFFER_SADDLE, PersistentDataType.BYTE, (byte) 1);
            ownerString = null;

            playerPdc.remove(CustomKeys.PLAYER_SNIFFER_MAX_HEALTH);
            playerPdc.remove(CustomKeys.PLAYER_SNIFFER_HEALTH);
            playerPdc.remove(CustomKeys.PLAYER_SNIFFER_SPEED);
            playerPdc.remove(CustomKeys.SNIFFER_SADDLE);
        }

        // Resolve the config for the saved entity type
        MountTypeConfig config = MOUNT_CONFIGS.entrySet().stream()
                .filter(e -> e.getKey().getSimpleName().equalsIgnoreCase(entityTypeName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (config == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }

            LivingEntity mount = player.getWorld().spawn(
                    player.getLocation(), config.entityClass(), entity -> {
                        var maxHpAttr = entity.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHpAttr != null) {
                            maxHpAttr.setBaseValue(maxHealth);
                        }
                        entity.setHealth(Math.min(health, maxHealth));
                        PersistentDataContainer pdc = entity.getPersistentDataContainer();
                        pdc.set(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE, speed);
                        pdc.set(CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE, saddleByte);
                        if (config.hasThirdAttr()) {
                            pdc.set(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE, thirdAttr);
                        }
                        if (ownerString != null) {
                            pdc.set(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING, ownerString);
                        }
                    });

            mountEntity(mount, player, config);
        }, 10L);
    }

    private void applyMovement(LivingEntity mount, Player rider) {
        float riderYaw = rider.getLocation().getYaw();
        mount.setRotation(riderYaw, 0);
        if (mount instanceof Mob mob) {
            mob.setBodyYaw(riderYaw);
        }

        double[] in = playerInputs.getOrDefault(rider.getUniqueId(), new double[]{0, 0});
        double forward = in[0];
        double strafe = in[1];
        double speed = activeMounts.get(mount.getUniqueId()).getSpeed();

        double yaw = Math.toRadians(riderYaw);
        double vx = -Math.sin(yaw) * forward + Math.cos(yaw) * strafe;
        double vz = Math.cos(yaw) * forward + Math.sin(yaw) * strafe;
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len > 0.001) {
            vx = (vx / len) * speed;
            vz = (vz / len) * speed;
        }

        double vertY = mount.getVelocity().getY();

        // Auto step-up over 1-block solid obstacles (not fences/walls)
        if (mount.isOnGround() && len > 0.001) {
            double nx = vx / speed;
            double nz = vz / speed;
            Location loc = mount.getLocation();
            int iy = (int) Math.floor(loc.getY());

            for (double ld : new double[]{1.3, 1.6, 1.9}) {
                int cx = (int) Math.floor(loc.getX() + nx * ld);
                int cz = (int) Math.floor(loc.getZ() + nz * ld);
                Block foot = mount.getWorld().getBlockAt(cx, iy, cz);
                Block clear1 = mount.getWorld().getBlockAt(cx, iy + 1, cz);
                Block clear2 = mount.getWorld().getBlockAt(cx, iy + 2, cz);
                if (isSteppableObstacle(foot) && isClearForPassage(clear1) && isClearForPassage(clear2)) {
                    vertY = STEP_UP_VELOCITY;
                    break;
                }
            }
        }

        // Zeroing horizontal velocity when idle suppresses AI wandering
        mount.setVelocity(new Vector(vx, vertY, vz));
    }

    private boolean isSteppableObstacle(Block block) {
        if (block.isPassable()) {
            return false;
        }
        double h = block.getBoundingBox().getHeight();
        return h > 0 && h <= 1.0;
    }

    private boolean isClearForPassage(Block block) {
        if (block.isPassable()) {
            return true;
        }
        return block.getType() == Material.SNOW && block.getBoundingBox().getHeight() < 0.5;
    }

    private void mountEntity(LivingEntity mount, Player player, MountTypeConfig config) {
        UUID ownerUUID = readOwner(mount);
        if (ownerUUID == null) {
            ownerUUID = player.getUniqueId();
            mount.getPersistentDataContainer()
                    .set(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING, ownerUUID.toString());
        }

        double speed = readSpeed(mount);
        Double thirdAttr = config.hasThirdAttr() ? readThirdAttr(mount, config) : null;
        boolean hasSaddle = config.requiresSaddle() && hasSaddle(mount);
        AranarthMount aMount = new AranarthMount(ownerUUID, speed, hasSaddle, thirdAttr, config.thirdAttrLabel());

        mount.addPassenger(player);
        activeMounts.put(mount.getUniqueId(), aMount);

        if (config.specialAction() != null) {
            activeSpecialActions.put(mount.getUniqueId(), config.specialAction());
        }

        // Zero the AI speed so the mob's goal selector cannot override our velocity control
        var speedAttr = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.0);
        }
    }

    private void cleanupMount(UUID id) {
        activeMounts.remove(id);
        SpecialAction action = activeSpecialActions.remove(id);
        if (action != null) {
            action.cleanup(id);
        }
        Entity e = Bukkit.getEntity(id);
        if (e instanceof LivingEntity living && !living.isDead()) {
            var speedAttr = living.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(speedAttr.getDefaultValue());
            }
        }
    }

    private void randomizeStats(LivingEntity mount, MountTypeConfig config) {
        double health = config.minHealth()
                + random.nextInt((int) (config.maxHealth() - config.minHealth() + 1));
        var maxHp = mount.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(health);
            mount.setHealth(health);
        }

        double speed = config.minSpeed() + random.nextDouble() * (config.maxSpeed() - config.minSpeed());
        mount.getPersistentDataContainer().set(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE, speed);

        if (config.hasThirdAttr()) {
            double thirdAttr = config.minThirdAttr()
                    + random.nextDouble() * (config.maxThirdAttr() - config.minThirdAttr());
            mount.getPersistentDataContainer().set(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE, thirdAttr);
        }
    }

    private boolean hasSaddle(LivingEntity mount) {
        PersistentDataContainer pdc = mount.getPersistentDataContainer();
        return pdc.has(CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE)
                && pdc.get(CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE) == 1;
    }

    private void setSaddle(LivingEntity mount, boolean value) {
        mount.getPersistentDataContainer().set(
                CustomKeys.MOUNT_SADDLE, PersistentDataType.BYTE, value ? (byte) 1 : (byte) 0);
    }

    private double readSpeed(LivingEntity mount) {
        PersistentDataContainer pdc = mount.getPersistentDataContainer();
        if (pdc.has(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE)) {
            return pdc.get(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE);
        }
        // Legacy key written before the mount generalisation
        if (pdc.has(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE)) {
            return pdc.get(CustomKeys.SNIFFER_SPEED, PersistentDataType.DOUBLE);
        }
        return 0.70;
    }

    private double readThirdAttr(LivingEntity mount, MountTypeConfig config) {
        PersistentDataContainer pdc = mount.getPersistentDataContainer();
        if (pdc.has(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE)) {
            return pdc.get(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE);
        }
        // Fallback to midpoint of the configured range
        return (config.minThirdAttr() + config.maxThirdAttr()) / 2.0;
    }

    private UUID readOwner(LivingEntity mount) {
        PersistentDataContainer pdc = mount.getPersistentDataContainer();
        if (pdc.has(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING)) {
            try {
                return UUID.fromString(pdc.get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
