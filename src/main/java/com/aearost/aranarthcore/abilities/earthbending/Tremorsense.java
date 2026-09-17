package com.aearost.aranarthcore.abilities.earthbending;

import com.aearost.aranarthcore.utils.DominionUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tremorsense - Earth ability with two behaviors:
 * <p>
 * Passive (static management, no PK ability instance):
 *   - In low light on earthbendable ground, shows a glowstone block clientside under the player.
 *   - Call managePassiveGlow(Server) every few ticks from AranarthCore.
 * <p>
 * Left-click (static, no PK ability instance):
 *   - Scans nearby ground for underground hollow spaces and emits smoke particles above them.
 *   - Call activateSmokeReveal(Player) from the left-click handler.
 * <p>
 * Active (PK ability instance, sneak-triggered):
 *   - Expands entity glow outward up to 50 blocks over 5 seconds.
 *   - Only visible to the caster; only glows players where PvP is permitted.
 *   - Ends on sneak release or after 15 seconds, then applies cooldown.
 */
public class Tremorsense extends EarthAbility implements AddonAbility {

    // Passive glow block settings
    static final byte LIGHT_THRESHOLD = 4;
    private static final int STICKY_RANGE = 3;
    private static final int MAX_DEPTH = 5;
    private static final int SMOKE_RADIUS = 10;

    // Active entity glow settings
    private static final int MAX_ENTITY_RADIUS = 50;
    private static final long EXPAND_DURATION_MS = 5000L;
    private static final long MAX_ACTIVE_DURATION_MS = 15000L;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    private long startTime;
    private final Set<UUID> glowedEntities = new HashSet<>();

    private static final Map<UUID, Tremorsense> activeInstances = new HashMap<>();
    private static final Map<UUID, Block> passiveGlowBlocks = new HashMap<>();

    // Creates an active entity-glow instance
    public Tremorsense(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 10000L;
        startTime = System.currentTimeMillis();
        activeInstances.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (!player.isOnline() || !player.isSneaking()) {
            endWithCooldown();
            return;
        }
        if (!bPlayer.canBind(this) || bPlayer.isChiBlocked()) {
            endWithCooldown();
            return;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= MAX_ACTIVE_DURATION_MS) {
            endWithCooldown();
            return;
        }

        double currentRadius = Math.min(MAX_ENTITY_RADIUS,
                (double) elapsed / EXPAND_DURATION_MS * MAX_ENTITY_RADIUS);
        double radiusSq = currentRadius * currentRadius;

        for (Entity entity : player.getNearbyEntities(currentRadius, currentRadius, currentRadius)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (glowedEntities.contains(entity.getUniqueId())) continue;
            if (entity.getLocation().distanceSquared(player.getLocation()) > radiusSq) continue;
            if (entity instanceof Player target && !DominionUtils.canAttackPlayer(player, target)) continue;
            glowedEntities.add(entity.getUniqueId());
            sendGlowPacket(entity, true);
        }
    }

    private void endWithCooldown() {
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void remove() {
        super.remove();
        activeInstances.remove(player.getUniqueId());
        for (UUID uuid : glowedEntities) {
            Entity entity = player.getServer().getEntity(uuid);
            if (entity != null && entity.isValid()) {
                sendGlowPacket(entity, false);
            }
        }
        glowedEntities.clear();
    }

    private void sendGlowPacket(Entity entity, boolean glow) {
        byte flags = buildEntityFlags(entity);
        if (glow) {
            flags |= 0x40;
        } else {
            flags &= (byte) ~0x40;
        }
        List<EntityData<?>> data = Collections.singletonList(
            new EntityData<>(0, EntityDataTypes.BYTE, flags)
        );
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entity.getEntityId(), data);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    // Reconstructs entity flags byte so only the glow bit is changed
    private byte buildEntityFlags(Entity entity) {
        byte flags = 0;
        if (entity.getFireTicks() > 0) flags |= 0x01;
        if (entity instanceof Player p) {
            if (p.isSneaking()) flags |= 0x02;
            if (p.isSprinting()) flags |= 0x08;
        }
        if (entity.isGlowing()) flags |= 0x40;
        return flags;
    }

    /**
     * Manages the client-side glowstone block shown under eligible players' feet in dark areas.
     */
    public static void managePassiveGlow(Server server) {
        CoreAbility abilityDef = CoreAbility.getAbility("Tremorsense");
        if (abilityDef == null || !abilityDef.isEnabled()) return;

        for (Player player : server.getOnlinePlayers()) {
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer == null || !bPlayer.canBendIgnoreBinds(abilityDef) || !bPlayer.isTremorSensing() || bPlayer.isChiBlocked()) {
                clearPassiveGlow(player);
                continue;
            }

            if (player.getLocation().getBlock().getLightLevel() >= LIGHT_THRESHOLD) {
                clearPassiveGlow(player);
                continue;
            }

            Block standBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (standBlock.getBlockData() instanceof Slab) continue;

            if (!EarthAbility.isEarthbendable(player, standBlock)) {
                // Only clear if outside sticky range
                Block current = passiveGlowBlocks.get(player.getUniqueId());
                if (current != null && STICKY_RANGE > 0
                        && standBlock.getLocation().distanceSquared(current.getLocation()) > (double) STICKY_RANGE * STICKY_RANGE) {
                    clearPassiveGlow(player);
                } else if (current != null && STICKY_RANGE <= 0) {
                    clearPassiveGlow(player);
                }
                continue;
            }

            if (!player.getWorld().equals(standBlock.getWorld())) {
                clearPassiveGlow(player);
                continue;
            }

            Block current = passiveGlowBlocks.get(player.getUniqueId());
            if (current == null) {
                passiveGlowBlocks.put(player.getUniqueId(), standBlock);
                player.sendBlockChange(standBlock.getLocation(), Material.GLOWSTONE.createBlockData());
            } else if (!current.equals(standBlock)) {
                player.sendBlockChange(current.getLocation(), current.getBlockData());
                passiveGlowBlocks.put(player.getUniqueId(), standBlock);
                player.sendBlockChange(standBlock.getLocation(), Material.GLOWSTONE.createBlockData());
            }
        }
    }

    /** Reverts the client-side glow block for a player and removes them from the map. */
    public static void clearPassiveGlow(Player player) {
        Block glow = passiveGlowBlocks.remove(player.getUniqueId());
        if (glow != null) {
            player.sendBlockChange(glow.getLocation(), glow.getBlockData());
        }
    }

    /**
     * Scans nearby ground for underground hollow spaces and emits smoke particles above them.
     */
    public static void activateSmokeReveal(Player player) {
        Block base = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        for (int i = -SMOKE_RADIUS; i <= SMOKE_RADIUS; i++) {
            for (int j = -SMOKE_RADIUS; j <= SMOKE_RADIUS; j++) {
                boolean earth = false;
                boolean foundAir = false;
                Block smokeBlock = null;

                for (int k = 0; k <= MAX_DEPTH; k++) {
                    Block b = base.getRelative(BlockFace.EAST, i)
                                  .getRelative(BlockFace.NORTH, j)
                                  .getRelative(BlockFace.DOWN, k);
                    if (EarthAbility.isEarthbendable(player, b) && !earth) {
                        earth = true;
                        smokeBlock = b;
                    } else if (!EarthAbility.isEarthbendable(player, b) && earth) {
                        foundAir = true;
                        break;
                    } else if (!EarthAbility.isEarthbendable(player, b) && !earth && !ElementalAbility.isAir(b.getType())) {
                        break;
                    }
                }
                if (foundAir && smokeBlock != null) {
                    smokeBlock.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                            smokeBlock.getRelative(BlockFace.UP).getLocation().add(0.5, 0.5, 0.5),
                            3, 0.3, 0.3, 0.3, 0);
                }
            }
        }
    }

    public static boolean hasActiveInstance(UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static Tremorsense getActiveInstance(UUID uuid) {
        return activeInstances.get(uuid);
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
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "Tremorsense";
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
        return "Channel your awareness through the earth. In dark areas the ground glows " +
                "beneath your feet. Left-click to reveal underground hollow spaces through " +
                "rising smoke. Hold sneak to sense all enemies nearby, causing them to " +
                "glow outward up to 50 blocks - visible only to you.";
    }
}
