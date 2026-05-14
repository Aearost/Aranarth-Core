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

public class MetalShots extends MetalAbility implements AddonAbility {

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

    private static final Map<UUID, List<Item>> trackedShots = new HashMap<>();

    private static final Map<UUID, BukkitTask> recallTasks = new HashMap<>();

    private static NamespacedKey shotOwnerKey;

    private static NamespacedKey instanceKey;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double damage;
    @Attribute(Attribute.RANGE)
    private double range;

    private Item firedItem;
    private Location startLocation;
    private Vector direction;

    public MetalShots(final Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }
        if (!hasIronIngot(player)) {
            return;
        }
        if (!consumeIronIngot(player)) {
            return;
        }

        this.cooldown = 1000L;
        this.damage = 3.0;  // 1.5 hearts
        this.range = 20.0;

        fireShot();
        this.bPlayer.addCooldown(this);
        this.start();
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
        this.firedItem = player.getWorld().dropItem(
                eyeLoc.clone().add(this.direction.clone().multiply(0.5)),
                ingotStack
        );
        this.firedItem.setPickupDelay(Integer.MAX_VALUE); // Not able to be picked up
        this.firedItem.setCanMobPickup(false);
        this.firedItem.setCanPlayerPickup(true);  // Owner can step on it after landing via event
        this.firedItem.setGravity(false);
        this.firedItem.setVelocity(this.direction.clone().multiply(SPEED));

        // Tag the item so the recall system can verify it belongs to this player
        this.firedItem.getPersistentDataContainer().set(
                getShotOwnerKey(),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        // Register for potential recall
        trackedShots.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(this.firedItem);

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

        final Location current = this.firedItem.getLocation();

        // Passable blocks such as short grass, flowers, and vines are intentionally skipped
        if (current.getBlock().getType().isSolid() && !current.getBlock().isPassable()) {
            this.firedItem.setVelocity(new Vector(0, 0, 0));
            landItem();
            this.remove();
            return;
        }

        // Examine every position this ingot will pass through next tick so it never enters a solid block
        Location checkPos = current.clone();
        double remaining = SPEED;

        while (remaining > 0) {
            final double step = Math.min(STEP, remaining);
            checkPos.add(this.direction.clone().multiply(step));

            // Stunt velocity to a short arc then let gravity take over as range was exceeded
            if (checkPos.distanceSquared(this.startLocation) > this.range * this.range) {
                this.firedItem.setVelocity(this.direction.clone().multiply(0.4));
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
                if (!(entity instanceof LivingEntity)) continue;
                if (entity.equals(player)) continue;

                DamageHandler.damageEntity(entity, this.damage, this);

                entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_METAL_HIT, 1.1f, 0.7f);
                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 1.4f);

                removeFromTracking();
                this.firedItem.remove();
                this.firedItem = null;
                this.remove();
                return;
            }

            remaining -= step;
        }

        // Path is clear
        current.getWorld().spawnParticle(
                Particle.DUST,
                current,
                1, 0.05, 0.05, 0.05, 0,
                INGOT_TRAIL
        );
        this.firedItem.setVelocity(this.direction.clone().multiply(SPEED));
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
        if (this.firedItem == null) return;
        final List<Item> shots = trackedShots.get(player.getUniqueId());
        if (shots != null) {
            shots.remove(this.firedItem);
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
                final List<Item> shots = trackedShots.get(player.getUniqueId());
                if (shots != null && !shots.isEmpty()) {
                    final Iterator<Item> iter = shots.iterator();
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
                    if (!(entity instanceof Item item)) continue;
                    if (!METAL_MATERIALS.contains(item.getItemStack().getType())) continue;
                    // Skip items belonging to any player's MetalShots
                    if (item.getPersistentDataContainer().has(
                            getShotOwnerKey(), PersistentDataType.STRING)) continue;

                    final double distSq = item.getLocation().distanceSquared(playerAnchor);
                    if (distSq > RECALL_RANGE * RECALL_RANGE) continue;

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

    public static NamespacedKey getShotOwnerKey() {
        if (shotOwnerKey == null) {
            shotOwnerKey = new NamespacedKey(AranarthCore.getInstance(), "metalshots_owner");
        }
        return shotOwnerKey;
    }

    private static NamespacedKey getInstanceKey() {
        if (instanceKey == null) {
            instanceKey = new NamespacedKey(AranarthCore.getInstance(), "metalshots_instance");
        }
        return instanceKey;
    }

    public static void removeTrackedItem(final Item item, final UUID playerUuid) {
        final List<Item> shots = trackedShots.get(playerUuid);
        if (shots != null) {
            shots.remove(item);
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
        return "MetalShots";
    }

    @Override
    public Location getLocation() {
        if (this.firedItem != null && !this.firedItem.isDead()) {
            return this.firedItem.getLocation();
        }
        return player.getLocation();
    }

    @Override
    public void load() {}

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
