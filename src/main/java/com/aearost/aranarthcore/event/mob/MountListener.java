package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthMount;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.Mount;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Centralised listener for all Aranarth mount events.
 */
public class MountListener implements Listener {

    /**
     * Encapsulates mount-type-specific behaviour triggered by the rider pressing space.
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

        default boolean isPassive() {
            return false;
        }
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
                    ? (int) mountData.getThirdAttribute().doubleValue() : 100;
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

    static final class RavagerRamAction implements SpecialAction {

        final Map<UUID, RavagerRam> activeRams = new HashMap<>();

        @Override
        public void start(LivingEntity mount, Player rider, AranarthMount mountData) {
            UUID id = mount.getUniqueId();
            if (activeRams.containsKey(id)) {
                return;
            }
            double maxDamage = mountData.getThirdAttribute() != null
                    ? mountData.getThirdAttribute()
                    : 14.0;
            RavagerRam ram = new RavagerRam((Ravager) mount, rider, activeRams, maxDamage);
            activeRams.put(id, ram);
            ram.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
        }

        @Override
        public void stop(UUID mountId) {
            // Passive ability — always active while mounted; space-bar release is ignored.
        }

        @Override
        public void cleanup(UUID mountId) {
            RavagerRam ram = activeRams.remove(mountId);
            if (ram != null) {
                ram.cancel();
            }
        }

        @Override
        public boolean isPassive() {
            return true;
        }
    }

    /**
     * Configuration for a single mount entity type.
     *
     * @param entityClass          Bukkit entity class (used for spawning).
     * @param minHealth            Minimum randomised max-health (half-hearts).
     * @param maxHealth            Maximum randomised max-health (half-hearts).
     * @param minSpeed             Minimum movement speed (blocks/tick).
     * @param maxSpeed             Maximum movement speed (blocks/tick).
     * @param minThirdAttr         Minimum third-attribute value, or {@code null}.
     * @param maxThirdAttr         Maximum third-attribute value, or {@code null}.
     * @param thirdAttrLabel       Display name for the third attribute, or {@code null}.
     * @param despawnOnOwnerLogout Whether the mount is removed when its rider logs out.
     * @param specialAction        Optional space-bar ability, or {@code null}.
     */
    record MountTypeConfig(
            Class<? extends LivingEntity> entityClass,
            double minHealth, double maxHealth,
            double minSpeed, double maxSpeed,
            Double minThirdAttr, Double maxThirdAttr,
            String thirdAttrLabel,
            boolean despawnOnOwnerLogout,
            SpecialAction specialAction) {
        boolean hasThirdAttr() {
            return thirdAttrLabel != null && minThirdAttr != null;
        }
    }

    private static final SnifferTunnelAction SNIFFER_TUNNEL = new SnifferTunnelAction();
    private static final RavagerRamAction RAVAGER_RAM = new RavagerRamAction();

    /**
     * Foods accepted by all mounts (mirrors vanilla horse diet).
     */
    private static final Map<Material, Double> BASE_MOUNT_FOODS = Map.of(
            Material.WHEAT, 2.0,
            Material.HAY_BLOCK, 20.0,
            Material.APPLE, 5.0,
            Material.GOLDEN_APPLE, 10.0,
            Material.ENCHANTED_GOLDEN_APPLE, 50.0,
            Material.GOLDEN_CARROT, 5.0
    );

    /**
     * Extra foods accepted only by the Ravager (Fire mount).
     */
    private static final Map<Material, Double> RAVAGER_EXTRA_FOODS = Map.of(
            Material.BEEF, 15.0,
            Material.PORKCHOP, 15.0,
            Material.CHICKEN, 15.0,
            Material.MUTTON, 20.0,
            Material.RABBIT, 20.0
    );

    /**
     * Extra foods accepted only by the future Polar Bear (Water mount).
     */
    private static final Map<Material, Double> POLAR_BEAR_EXTRA_FOODS = Map.of(
            Material.COD, 20.0,
            Material.SALMON, 20.0
    );

    /**
     * Extra foods accepted only by the future Happy Ghast (Air mount).
     */
    private static final Map<Material, Double> GHAST_EXTRA_FOODS = Map.of(
            Material.CARROT, 20.0,
            Material.POTATO, 20.0,
            Material.BEETROOT, 20.0
    );

    static final Map<Class<? extends LivingEntity>, MountTypeConfig> MOUNT_CONFIGS = new LinkedHashMap<>();

    static {
        MOUNT_CONFIGS.put(Sniffer.class, new MountTypeConfig(
                Sniffer.class,
                20, 60, // Health (high: 20–60 half-hearts)
                0.35, 0.90, // Speed (low: 7–18 m/s)
                50.0, 200.0, // Tunnel speed range (blocks/tick at Lv1/Lv10)
                "Dig Speed",
                true,
                SNIFFER_TUNNEL
        ));

        MOUNT_CONFIGS.put(Ravager.class, new MountTypeConfig(
                Ravager.class,
                15, 45, // Health (medium: 15–45 half-hearts)
                0.50, 1.25, // Speed (medium: 10–25 m/s)
                4.0, 17.0, // Ram Damage (medium: 4–17 half-hearts)
                "Ram Damage",
                true,
                RAVAGER_RAM
        ));

//         Template for future mounts
//
//         MOUNT_CONFIGS.put(NewMob.class, new MountTypeConfig(
//                 NewMob.class,
//                 20, 20, 0.5, 1.0,
//                 null, null, null,   // no third attribute
//                 false, null         // persists, no special action
//         ));
    }

    private static final double STEP_UP_VELOCITY = 0.42;
    private static final Map<UUID, AranarthMount> activeMounts = new HashMap<>();
    private static final Map<UUID, double[]> playerInputs = new HashMap<>();
    private static final Map<UUID, SpecialAction> activeSpecialActions = new HashMap<>();
    private static final Map<UUID, Double> originalSpeeds = new HashMap<>();
    private final Random random = new Random();
    private static MountListener instance;

    public static MountListener getInstance() {
        return instance;
    }

    public MountListener(AranarthCore plugin) {
        instance = this;
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
                        // Mounts stay in the world when dismounted
                        if (mount.getPersistentDataContainer()
                                .has(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING)) {
                            releaseMountControl(id);
                        } else {
                            cleanupMount(id);
                        }
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

        // Ignore wild vanilla mobs, only consider actual mounts summoned by /mount
        if (!clickedEntity.getPersistentDataContainer()
                .has(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);

        // Sneaking shows stats (handled by MountStats), never attempt to mount
        if (player.isSneaking()) {
            return;
        }

        // Check ownership before any other interaction
        UUID ownerUUID = readOwner(clickedEntity);
        if (ownerUUID != null && !ownerUUID.equals(player.getUniqueId())) {
            player.sendMessage(ChatUtils.chatMessage("&cThis is not your mount!"));
            return;
        }

        // You can only feed your own mount
        String mountElement = clickedEntity.getPersistentDataContainer()
                .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
        if (ownerUUID != null && ownerUUID.equals(player.getUniqueId())) {
            Double healAmount = getFoodHealAmount(mountElement, mainHand.getType());
            if (healAmount != null) {
                feedMount(clickedEntity, player, mainHand, healAmount);
                return;
            }
        }

        // Mount (up to 2 riders)
        List<Entity> passengers = clickedEntity.getPassengers();
        if (passengers.contains(player)) {
            return;
        }
        if (passengers.size() >= 2) {
            // Flying Bison should allow the same as normal Happy Ghasts
            if (!mountElement.equals("AIR")) {
                player.sendMessage(ChatUtils.chatMessage("&cThis mount already has two riders!"));
                return;
            }
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
        if (!activeMounts.containsKey(mount.getUniqueId())) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        playerInputs.remove(player.getUniqueId());

        UUID mountId = mount.getUniqueId();
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            Entity e = Bukkit.getEntity(mountId);
            if (!(e instanceof LivingEntity m) || !m.getPassengers().isEmpty()) {
                return; // Other passengers still present, do not clean up yet
            }

            // Mounts stay alive in the world with passive AI
            if (!m.isDead() && m.getPersistentDataContainer().has(
                    CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING)) {
                releaseMountControl(mountId); // Stop movement loop, restore AI, save HP
                return;
            }

            cleanupMount(mountId);
        }, 1L);
    }

    @EventHandler
    public void onMountDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mount)) {
            return;
        }
        if (!MountUtils.isActiveMount(mount.getUniqueId())) {
            return;
        }
        // Award health XP for damage absorbed by the mount
        MountUtils.addHealthXp(mount.getUniqueId(), event.getFinalDamage());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mount = event.getEntity();
        MountTypeConfig config = getConfig(mount);
        if (config == null) {
            return;
        }
        UUID id = mount.getUniqueId();
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Start recharge instead of normal drop/auto-respawn behaviour
        String mountElement = mount.getPersistentDataContainer()
                .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
        if (mountElement != null) {
            String ownerStr = mount.getPersistentDataContainer()
                    .get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING);
            if (ownerStr != null) {
                try {
                    UUID ownerUUID = UUID.fromString(ownerStr);
                    Mount mountData = MountUtils.getOrCreate(ownerUUID, mountElement);
                    mountData.startRecharge();
                    Player owner = Bukkit.getPlayer(ownerUUID);
                    if (owner != null) {
                        String mountDisplayName = MountUtils.getDisplayName(ownerUUID, mountElement);
                        owner.sendMessage(ChatUtils.chatMessage(
                                MountUtils.getElementColor(mountElement) + mountDisplayName
                                        + " &7needs some time to recover! "
                                        + "Use &e/mounts &7to check their status"));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            cleanupMount(id);
            return;
        }

        cleanupMount(id);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MountUtils.cleanupPlayerBars(player.getUniqueId());

        // If the mount is dismounted but alive in the world, save its HP and remove it
        UUID wanderingId = MountUtils.getActiveMountEntityUUID(player.getUniqueId());
        if (wanderingId != null) {
            if (Bukkit.getEntity(wanderingId) instanceof LivingEntity wandering && wandering.getPassengers().isEmpty()) {
                String mountElem = wandering.getPersistentDataContainer()
                        .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
                if (mountElem != null) {
                    Mount mountData = MountUtils.get(player.getUniqueId(), mountElem);
                    if (mountData != null) {
                        mountData.setCurrentHealth(wandering.getHealth());
                    }
                    cleanupMount(wanderingId);
                    wandering.remove();
                    return;
                }
            }
        }

        if (!(player.getVehicle() instanceof LivingEntity mount)) {
            return;
        }
        MountTypeConfig config = getConfig(mount);
        if (config == null || !config.despawnOnOwnerLogout()) {
            return;
        }

        String mountElement = mount.getPersistentDataContainer()
                .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
        if (mountElement != null) {
            Mount mountData = MountUtils.getOrCreate(player.getUniqueId(), mountElement);
            mountData.setCurrentHealth(mount.getHealth());
            cleanupMount(mount.getUniqueId());
            mount.remove();
        }
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
        // Track speed XP for mounts
        boolean isMoving = Math.abs(forward) > 0.001 || Math.abs(strafe) > 0.001;

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

        // Award speed XP when the mount is actually moving
        if (isMoving) {
            MountUtils.accumulateSpeedXp(mount.getUniqueId(), speed);
        }
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
        AranarthMount aMount = new AranarthMount(ownerUUID, speed, thirdAttr, config.thirdAttrLabel());

        mount.addPassenger(player);
        activeMounts.put(mount.getUniqueId(), aMount);

        if (config.specialAction() != null) {
            activeSpecialActions.put(mount.getUniqueId(), config.specialAction());
            if (config.specialAction().isPassive()) {
                config.specialAction().start(mount, player, aMount);
            }
        }

        // Register mounts in MountUtils for XP tracking
        String mountElement = mount.getPersistentDataContainer()
                .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
        if (mountElement != null) {
            MountUtils.registerActive(mount.getUniqueId(), ownerUUID, mountElement);
        }

        // Clear any existing target so the mount doesn't continue aggro on mount
        if (mount instanceof Mob mob) {
            mob.setTarget(null);
        }

        // Zero the AI speed so the mob's goal selector cannot override our velocity control.
        var speedAttr = mount.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            originalSpeeds.put(mount.getUniqueId(), speedAttr.getBaseValue());
            speedAttr.setBaseValue(0.0);
        }
    }

    /**
     * Stops movement-loop control of a mount and saves its current HP when its rider dismounts.
     */
    private void releaseMountControl(UUID id) {
        activeMounts.remove(id);
        SpecialAction action = activeSpecialActions.remove(id);
        if (action != null) {
            action.cleanup(id);
        }

        Entity e = Bukkit.getEntity(id);
        if (e instanceof LivingEntity living && !living.isDead()) {
            var speedAttr = living.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttr != null) {
                double restore = originalSpeeds.getOrDefault(id, speedAttr.getDefaultValue());
                speedAttr.setBaseValue(restore);
            }
            originalSpeeds.remove(id);

            // Zero residual horizontal velocity left over from the movement loop so the
            // mount doesn't launch away in a straight line when the rider dismounts
            Vector vel = living.getVelocity();
            living.setVelocity(new Vector(0, vel.getY(), 0));

            String mountElement = living.getPersistentDataContainer()
                    .get(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING);
            if (mountElement != null) {
                String ownerStr = living.getPersistentDataContainer()
                        .get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING);
                if (ownerStr != null) {
                    try {
                        UUID ownerUUID = UUID.fromString(ownerStr);
                        Mount mountData = MountUtils.get(ownerUUID, mountElement);
                        if (mountData != null) {
                            mountData.setCurrentHealth(living.getHealth());
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
    }

    private void cleanupMount(UUID id) {
        releaseMountControl(id);
        MountUtils.unregisterActive(id);
    }

    public void cleanupMountPublic(UUID id) {
        cleanupMount(id);
    }

    /**
     * Returns how many half-hearts the given food item heals the element's mount.
     */
    private Double getFoodHealAmount(String element, Material item) {
        Double base = BASE_MOUNT_FOODS.get(item);
        if (base != null) {
            return base;
        }

        return switch (element) {
            case "FIRE" -> RAVAGER_EXTRA_FOODS.get(item);
            case "EARTH" -> AranarthUtils.isFlower(item) ? 20.0 : null;
            case "WATER" -> POLAR_BEAR_EXTRA_FOODS.get(item);
            case "AIR" -> GHAST_EXTRA_FOODS.get(item);
            default -> null;
        };
    }

    /**
     * Heals a mount and increases their Health skill XP.
     */
    private void feedMount(LivingEntity mount, Player player, ItemStack food, double baseHeal) {
        var maxHpAttr = mount.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : mount.getHealth();
        double currentHp = mount.getHealth();

        if (currentHp >= maxHp) {
            player.sendMessage(ChatUtils.chatMessage("&7Your mount is already at full health!"));
            return;
        }

        double actualHeal = Math.min(baseHeal, maxHp - currentHp);
        mount.setHealth(currentHp + actualHeal);

        // Award health XP proportional to HP actually recovered
        MountUtils.addHealthXp(mount.getUniqueId(), actualHeal);

        // Consume one item
        food.setAmount(food.getAmount() - 1);

        // Feedback
        mount.getWorld().spawnParticle(Particle.HEART,
                mount.getLocation().add(0, mount.getHeight() + 0.3, 0),
                3, 0.4, 0.3, 0.4, 0);
        player.playSound(mount.getLocation(), Sound.ENTITY_HORSE_EAT, 1.0f, 1.0f);
    }

    /**
     * Spawns and mounts the player's elemental mount.
     *
     * @param player    The player summoning their mount.
     * @param element   The player/mount's element name as a string.
     * @param mountData The mount's current level/XP data.
     */
    public void summonMount(Player player, String element, Mount mountData) {
        Class<? extends LivingEntity> entityClass = MountUtils.getEntityClassForElement(element);
        if (entityClass == null) {
            return;
        }

        MountTypeConfig config = getConfig(entityClass);
        if (config == null) {
            return;
        }

        // Calculate stats from skill levels
        double maxHealth = Mount.statForLevel(config.minHealth(), config.maxHealth(),
                mountData.getHealthLevel());
        double speed = Mount.statForLevel(config.minSpeed(), config.maxSpeed(),
                mountData.getSpeedLevel());
        double thirdAttr = config.hasThirdAttr()
                ? Mount.statForLevel(config.minThirdAttr(), config.maxThirdAttr(),
                mountData.getThirdLevel())
                : 0;

        // Restore last saved health
        double spawnHealth = mountData.getCurrentHealth() > 0
                ? Math.min(mountData.getCurrentHealth(), maxHealth) : maxHealth;

        UUID playerUUID = player.getUniqueId();

        LivingEntity mount = player.getWorld().spawn(
                player.getLocation(), entityClass, entity -> {
                   // If the server restarts while the pet is out, the entity simply disappears
                    entity.setPersistent(false);

                    var maxHpAttr = entity.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHpAttr != null) {
                        maxHpAttr.setBaseValue(maxHealth);
                    }
                    entity.setHealth(spawnHealth);

                    PersistentDataContainer pdc = entity.getPersistentDataContainer();
                    pdc.set(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE, speed);
                    pdc.set(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING, playerUUID.toString());
                    pdc.set(CustomKeys.MOUNT_ELEMENT, PersistentDataType.STRING, element);
                    if (config.hasThirdAttr()) {
                        pdc.set(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE, thirdAttr);
                    }
                });

        mountEntity(mount, player, config);

        // Apply the player's display name (nickname or species name) as the entity nametag
        String displayName = MountUtils.getDisplayName(playerUUID, element);
        mount.setCustomName(displayName);
        mount.setCustomNameVisible(true);

        player.sendMessage(ChatUtils.chatMessage(
                MountUtils.getElementColor(element) + displayName + " &7has been summoned!"));
    }

    private MountTypeConfig getConfig(Class<? extends LivingEntity> entityClass) {
        for (Map.Entry<Class<? extends LivingEntity>, MountTypeConfig> entry : MOUNT_CONFIGS.entrySet()) {
            if (entry.getKey().isAssignableFrom(entityClass)) {
                return entry.getValue();
            }
        }
        return null;
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

    private double readSpeed(LivingEntity mount) {
        PersistentDataContainer pdc = mount.getPersistentDataContainer();
        if (pdc.has(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE)) {
            return pdc.get(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE);
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
