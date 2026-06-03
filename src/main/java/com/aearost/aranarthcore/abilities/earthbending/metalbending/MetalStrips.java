package com.aearost.aranarthcore.abilities.earthbending.metalbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.Set;

public class MetalStrips extends MetalAbility implements AddonAbility {

    private static final double SPEED = 3.0;
    private static final double STEP = 0.3;
    private static final double HIT_RADIUS = 0.6;
    private static final double RECALL_RANGE = 32.0;
    private static final double RECALL_SPEED = 1.2;
    private static final double PICKUP_RADIUS = 1.5;

    private static final Particle.DustOptions INGOT_TRAIL =
            new Particle.DustOptions(Color.fromRGB(160, 160, 165), 0.7f);

    private static final Set<Material> METAL_MATERIALS = Set.of(
            Material.IRON_INGOT,
            Material.IRON_NUGGET,
            Material.GOLD_INGOT,
            Material.GOLD_NUGGET,
            Material.COPPER_INGOT,
            Material.RAW_IRON,
            Material.RAW_GOLD,
            Material.RAW_COPPER,
            Material.NETHERITE_INGOT,
            Material.QUARTZ
    );

    private static final int MAX_BURST_SHOTS = 8;
    private static final long BURST_WINDOW_TICKS = 50L; // 2.5 seconds
    private static final long COOLDOWN_MS = 5000L;

    private static final Set<UUID> pendingLeftClicks = new HashSet<>();

    private static final Map<UUID, List<Item>> trackedStrips = new HashMap<>();

    private static final Map<UUID, BukkitTask> recallTasks = new HashMap<>();

    private static final Map<UUID, BurstData> activeBursts = new HashMap<>();

    private static NamespacedKey stripOwnerKey;

    private static NamespacedKey instanceKey;

    private static class BurstData {
        int shotsFired = 0;
        BukkitTask endTask;
        MetalStrips lastInstance;
    }

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private Item firedItem;
    private Location startLocation;
    private Vector direction;
    private double distanceTraveled;

    /**
     * Must be called immediately before constructing a MetalStrips instance from the left-click
     * handler so that PK's own right-click activation is silently ignored.
     */
    public static void markLeftClick(final UUID uuid) {
        pendingLeftClicks.add(uuid);
    }

    public MetalStrips(final Player player) {
        super(player);

        final UUID uuid = player.getUniqueId();

        // Only allow activation triggered explicitly by left-click, not PK's right-click handling
        if (!pendingLeftClicks.remove(uuid)) {
            return;
        }

        if (!bPlayer.canBend(this)) {
            return;
        }
        BurstData burst = activeBursts.get(uuid);

        if (burst != null && burst.shotsFired >= MAX_BURST_SHOTS) {
            return;
        }

        if (!hasIronIngot(player)) {
            return;
        }
        if (!consumeIronIngot(player)) {
            return;
        }

        // Start a new burst on the first shot
        if (burst == null) {
            burst = new BurstData();
            activeBursts.put(uuid, burst);

            final BurstData finalBurst = burst;
            burst.endTask = new BukkitRunnable() {
                @Override
                public void run() {
                    endBurst(player, uuid);
                }
            }.runTaskLater(AranarthCore.getInstance(), BURST_WINDOW_TICKS);
        }

        burst.shotsFired++;
        burst.lastInstance = this;

        this.cooldown = COOLDOWN_MS;
        this.damage = 6.0;
        this.range = 20.0;

        fireShot();
        this.start();

        // If all shots used, end burst immediately instead of waiting for the timer
        if (burst.shotsFired >= MAX_BURST_SHOTS) {
            burst.endTask.cancel();
            endBurst(player, uuid);
        }
    }

    private static void endBurst(final Player player, final UUID uuid) {
        final BurstData burst = activeBursts.remove(uuid);
        if (burst != null && burst.lastInstance != null) {
            burst.lastInstance.bPlayer.addCooldown(burst.lastInstance);
        }
    }

    private void fireShot() {
        final Location eyeLoc = player.getEyeLocation();
        this.direction = eyeLoc.getDirection().normalize();
        this.startLocation = eyeLoc.clone();

        // Build a unique ItemStack so the shot items never merge with each other
        final ItemStack ingotStack = new ItemStack(Material.IRON_INGOT, 1);
        final ItemMeta meta = ingotStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    getInstanceKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
            ingotStack.setItemMeta(meta);
        }

        // Spawn the ingot just ahead of the player's eye so it does not immediately collide
        this.distanceTraveled = 0.5;
        this.firedItem = player.getWorld().dropItem(
                eyeLoc.clone().add(this.direction.clone().multiply(this.distanceTraveled)),
                ingotStack
        );
        this.firedItem.setPickupDelay(Integer.MAX_VALUE); // Not able to be picked up
        this.firedItem.setCanMobPickup(false);
        this.firedItem.setCanPlayerPickup(true);  // Owner can step on it after landing via event
        this.firedItem.setGravity(false);
        this.firedItem.setVelocity(this.direction.clone().multiply(SPEED));

        // Tag the item so the recall system can verify it belongs to this player
        this.firedItem.getPersistentDataContainer().set(
                getStripOwnerKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        // Register for potential recall
        trackedStrips.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(this.firedItem);

        final World world = eyeLoc.getWorld();
        world.playSound(eyeLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.85f, 1.9f);
        world.playSound(eyeLoc, Sound.BLOCK_METAL_HIT, 0.55f, 1.7f);
    }

    @Override
    public void progress() {
        if (!player.isOnline() || player.isDead()) {
            removeFiredItemFromWorld();
            this.remove();
            return;
        }

        // Item was removed externally (e.g. /kill, chunk unload)
        if (this.firedItem == null || this.firedItem.isDead() || !this.firedItem.isValid()) {
            removeFromTracking();
            this.remove();
            return;
        }

        // Advance along the exact ray in sub-steps for precise collision detection
        double remaining = SPEED;
        while (remaining > 0) {
            final double step = Math.min(STEP, remaining);
            this.distanceTraveled += step;

            final Location checkPos = this.startLocation.clone()
                    .add(this.direction.clone().multiply(this.distanceTraveled));

            // Range exceeded — let gravity finish it off
            if (this.distanceTraveled > this.range) {
                landItem();
                this.remove();
                return;
            }

            // Solid, non-passable block in the path
            if (checkPos.getBlock().getType().isSolid() && !checkPos.getBlock().isPassable()) {
                this.firedItem.setVelocity(new Vector(0, 0, 0));
                landItem();
                this.remove();
                return;
            }

            // Living entity in the path
            for (final Entity entity : checkPos.getWorld().getNearbyEntities(checkPos, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                if (entity.equals(player)) {
                    continue;
                }

                DamageHandler.damageEntity(entity, this.damage, this);

                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_METAL_HIT, 1.1f, 0.7f);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 1.4f);

                this.firedItem.setVelocity(new Vector(0, 0, 0));
                landItem();
                this.remove();
                return;
            }

            remaining -= step;
        }

        // Steer the item entity onto the exact ray position each tick.
        // Using (exactPos - actualPos) as velocity means any physics drift is
        // corrected in the very next tick without needing a laggy teleport call.
        final Location exactPos = this.startLocation.clone()
                .add(this.direction.clone().multiply(this.distanceTraveled));
        final Vector correction = exactPos.toVector()
                .subtract(this.firedItem.getLocation().toVector());
        this.firedItem.setVelocity(correction);

        exactPos.getWorld().spawnParticle(
                Particle.DUST,
                exactPos,
                1, 0.05, 0.05, 0.05, 0,
                INGOT_TRAIL
        );
    }

    private void landItem() {
        if (this.firedItem != null && !this.firedItem.isDead()) {
            this.firedItem.setGravity(true);
            this.firedItem.setPickupDelay(20); // Owner can step on it 1 second after landing
        }
        this.firedItem = null;
    }

    private void removeFiredItemFromWorld() {
        if (this.firedItem != null && !this.firedItem.isDead()) {
            removeFromTracking();
            this.firedItem.remove();
        }
        this.firedItem = null;
    }

    private void removeFromTracking() {
        if (this.firedItem == null) {
            return;
        }
        final List<Item> strips = trackedStrips.get(player.getUniqueId());
        if (strips != null) {
            strips.remove(this.firedItem);
        }
    }

    public static void startRecall(final Player player) {
        if (recallTasks.containsKey(player.getUniqueId())) {
            return;
        }

        final BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !player.isSneaking()) {
                    stopRecall(player);
                    return;
                }

                final Location playerAnchor = player.getLocation().add(0, 1.0, 0);

                // Returned as regular iron ingots (no metadata)
                final List<Item> strips = trackedStrips.get(player.getUniqueId());
                if (strips != null && !strips.isEmpty()) {
                    final Iterator<Item> iter = strips.iterator();
                    while (iter.hasNext()) {
                        final Item item = iter.next();

                        if (item.isDead() || !item.isValid()) {
                            iter.remove();
                            continue;
                        }

                        final double distSq = item.getLocation().distanceSquared(playerAnchor);

                        if (distSq > RECALL_RANGE * RECALL_RANGE) {
                            continue;
                        }

                        // Within pickup radius
                        if (distSq <= PICKUP_RADIUS * PICKUP_RADIUS) {
                            final HashMap<Integer, ItemStack> overflow =
                                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 1));
                            if (!overflow.isEmpty()) {
                                player.getWorld().dropItemNaturally(
                                        player.getLocation(), new ItemStack(Material.IRON_INGOT, 1));
                            }
                            item.remove();
                            iter.remove();
                            continue;
                        }

                        final Vector toPlayer = playerAnchor.toVector()
                                .subtract(item.getLocation().toVector())
                                .normalize()
                                .multiply(RECALL_SPEED);
                        item.setGravity(false);
                        item.setVelocity(toPlayer);
                        item.getWorld().spawnParticle(
                                Particle.DUST, item.getLocation(), 1, 0.05, 0.05, 0.05, 0, INGOT_TRAIL);
                    }
                }

                // Pull nearby ambient metal items
                for (final Entity entity : player.getWorld().getNearbyEntities(
                        playerAnchor, RECALL_RANGE, RECALL_RANGE, RECALL_RANGE)) {
                    if (!(entity instanceof Item item)) {
                        continue;
                    }
                    if (!METAL_MATERIALS.contains(item.getItemStack().getType())) {
                        continue;
                    }
                    // Skip items belonging to any player's MetalStrips
                    if (item.getPersistentDataContainer().has(
                            getStripOwnerKey(), PersistentDataType.STRING)) {
                        continue;
                    }

                    final double distSq = item.getLocation().distanceSquared(playerAnchor);
                    if (distSq > RECALL_RANGE * RECALL_RANGE) {
                        continue;
                    }

                    // Within pickup radius, add the item's actual stack to inventory
                    if (distSq <= PICKUP_RADIUS * PICKUP_RADIUS) {
                        final ItemStack stack = item.getItemStack().clone();
                        final HashMap<Integer, ItemStack> overflow =
                                player.getInventory().addItem(stack);
                        overflow.values().forEach(leftover ->
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                        item.remove();
                        continue;
                    }

                    final Vector toPlayer = playerAnchor.toVector()
                            .subtract(item.getLocation().toVector())
                            .normalize()
                            .multiply(RECALL_SPEED);
                    item.setVelocity(toPlayer);
                    item.getWorld().spawnParticle(
                            Particle.DUST, item.getLocation(), 1, 0.05, 0.05, 0.05, 0, INGOT_TRAIL);
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);

        recallTasks.put(player.getUniqueId(), task);
    }

    public static void stopRecall(final Player player) {
        final BukkitTask task = recallTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private static boolean hasIronIngot(final Player player) {
        return player.getInventory().contains(Material.IRON_INGOT);
    }

    private static boolean consumeIronIngot(final Player player) {
        final ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack != null && stack.getType() == Material.IRON_INGOT) {
                if (stack.getAmount() > 1) {
                    stack.setAmount(stack.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    public static NamespacedKey getStripOwnerKey() {
        if (stripOwnerKey == null) {
            stripOwnerKey = new NamespacedKey(AranarthCore.getInstance(), "metalstrips_owner");
        }
        return stripOwnerKey;
    }

    private static NamespacedKey getInstanceKey() {
        if (instanceKey == null) {
            instanceKey = new NamespacedKey(AranarthCore.getInstance(), "metalstrips_instance");
        }
        return instanceKey;
    }

    public static void removeTrackedItem(final Item item, final UUID playerUuid) {
        final List<Item> strips = trackedStrips.get(playerUuid);
        if (strips != null) {
            strips.remove(item);
        }
    }

    @Override
    public void remove() {
        // If the item is still alive when the ability is removed unexpectedly, let it land
        if (this.firedItem != null && !this.firedItem.isDead()) {
            this.firedItem.setGravity(true);
            this.firedItem.setVelocity(new Vector(0, 0, 0));
        }
        this.firedItem = null;
        super.remove();
    }

    @Override
    public void stop() {
        // Clean up the in-flight item immediately
        if (this.firedItem != null && !this.firedItem.isDead()) {
            removeFromTracking();
            this.firedItem.remove();
        }
        this.firedItem = null;
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
    public String getName() {
        return "MetalStrips";
    }

    @Override
    public Location getLocation() {
        if (this.firedItem != null && !this.firedItem.isDead()) {
            return this.firedItem.getLocation();
        }
        return player.getLocation();
    }

    @Override
    public void load() {
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
        return "Rapidly fire metal ingots from your inventory as harmful projectiles, "
                + "requiring one ingot per shot, and striking hit targets. "
                + "Hold sneak to recall all nearby ingots you have fired back into your hand.\n"
                + ChatUtils.translateToColor("&fUsage: Left-click (Launch) | Hold Sneak (Pull)");
    }
}
