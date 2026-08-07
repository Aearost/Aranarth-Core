package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Intercepts outgoing flame particle packets and substitutes replacements for fire-type players.
 * Blue fire: client-side only (soul fire flame visible only to the caster).
 * White fire: server-side (replacement particles visible to all; original flame suppressed for bystanders).
 */
public class FireParticleListener extends PacketListenerAbstract {

    private final JavaPlugin plugin;

    private static Object SOUL_FIRE_FLAME_NMS;
    private static Constructor<?> PARTICLES_PACKET_CTOR;
    private static Method GET_HANDLE;
    private static Field CONNECTION_FIELD;
    private static Method SEND_METHOD;
    private static boolean reflectionReady = false;

    private static final Particle.DustOptions WHITE_DUST = new Particle.DustOptions(Color.WHITE, 1.2f);

    /**
     * Tracks positions where white fire particles were just spawned, so bystander FLAME packets
     * at the same location can be suppressed. Keys are encoded block positions; values are timestamps.
     */
    private static final ConcurrentHashMap<Long, Long> whiteFirePositions = new ConcurrentHashMap<>();
    private static final long WHITE_FIRE_TTL_MS = 200;

    public FireParticleListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Call once from AranarthCore.onEnable() after PacketEvents is initialised.
     */
    public static void initReflection() {
        try {
            Class<?> nmsParticleTypesClass = Class.forName("net.minecraft.core.particles.ParticleTypes");
            Field soulFireField = nmsParticleTypesClass.getDeclaredField("SOUL_FIRE_FLAME");
            soulFireField.setAccessible(true);
            SOUL_FIRE_FLAME_NMS = soulFireField.get(null);

            Class<?> particleOptionsClass = Class.forName("net.minecraft.core.particles.ParticleOptions");
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket");
            PARTICLES_PACKET_CTOR = findParticlePacketCtor(packetClass, particleOptionsClass);
            PARTICLES_PACKET_CTOR.setAccessible(true);

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            GET_HANDLE = craftPlayerClass.getMethod("getHandle");

            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            CONNECTION_FIELD = serverPlayerClass.getField("connection");

            Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
            SEND_METHOD = CONNECTION_FIELD.getType().getMethod("send", packetInterface);

            reflectionReady = true;
            Bukkit.getLogger().info("[AC] Fire particle reflection initialised successfully");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[AC] Fire particle reflection failed - blue fire particles will not work: " + e.getMessage());
        }
    }

    private static Constructor<?> findParticlePacketCtor(Class<?> packetClass, Class<?> particleOptionsClass) throws NoSuchMethodException {
        for (Constructor<?> c : packetClass.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p[0].equals(particleOptionsClass) && p[1].equals(boolean.class)
                    && (p.length == 10 || p.length == 11)) {
                return c;
            }
        }
        StringBuilder available = new StringBuilder();
        for (Constructor<?> c : packetClass.getDeclaredConstructors()) {
            available.append(c.getParameterCount()).append("-arg, ");
        }
        throw new NoSuchMethodException("ClientboundLevelParticlesPacket constructor not found. Available: " + available);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!reflectionReady) {
            return;
        }
        if (event.getPacketType() != PacketType.Play.Server.PARTICLE) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        WrapperPlayServerParticle packet;
        try {
            packet = new WrapperPlayServerParticle(event);
        } catch (IllegalStateException e) {
            return;
        }
        if (packet.getParticle().getType() != ParticleTypes.FLAME) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        FireType fireType = (aranarthPlayer != null) ? aranarthPlayer.getFireType() : FireType.DEFAULT;

        if (fireType == FireType.BLUE) {
            event.setCancelled(true);
            sendNmsParticle(player, SOUL_FIRE_FLAME_NMS, packet);
            return;
        }

        if (fireType == FireType.WHITE) {
            // Record this position so bystanders' packets at the same location are suppressed
            long posKey = encodePosition(packet.getPosition());
            whiteFirePositions.put(posKey, System.currentTimeMillis());
            event.setCancelled(true);
            sendWhiteFireParticles(player.getWorld(), packet);
            return;
        }

        // For all other players - suppress FLAME if a white fire player already spawned replacements here
        long posKey = encodePosition(packet.getPosition());
        Long stamp = whiteFirePositions.get(posKey);
        if (stamp != null) {
            if (System.currentTimeMillis() - stamp < WHITE_FIRE_TTL_MS) {
                event.setCancelled(true);
            } else {
                whiteFirePositions.remove(posKey);
            }
        }
    }

    private void sendWhiteFireParticles(World world, WrapperPlayServerParticle orig) {
        Vector3d pos = orig.getPosition();
        Vector3f off = orig.getOffset();
        int count = orig.getParticleCount();
        float speed = orig.getMaxSpeed();
        Location loc = new Location(world, pos.x, pos.y, pos.z);

        float spreadX = off.x * 1.8f;
        float spreadY = off.y * 1.8f;
        float spreadZ = off.z * 1.8f;
        double roll = ThreadLocalRandom.current().nextDouble();
        for (long delay = 0; delay <= 2; delay++) {
            if (roll < 0.125) {
                // 1/8 end rod
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        world.spawnParticle(Particle.END_ROD, loc, count, spreadX, spreadY, spreadZ, speed), delay);
            } else if (roll < 0.325) {
                // 1/5 white dust
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        world.spawnParticle(Particle.DUST, loc, count, spreadX, spreadY, spreadZ, 0, WHITE_DUST), delay);
            } else {
                // 67.5% electric spark
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        world.spawnParticle(Particle.ELECTRIC_SPARK, loc, count, spreadX, spreadY, spreadZ, speed), delay);
            }
        }
    }

    /**
     * Encodes a particle's center position to a long key at block precision.
     */
    private static long encodePosition(Vector3d pos) {
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (long)(z & 0xFFFFF);
    }

    private void sendNmsParticle(Player player, Object nmsType, WrapperPlayServerParticle orig) {
        try {
            Vector3d pos = orig.getPosition();
            Vector3f off = orig.getOffset();
            Object nmsPacket;
            if (PARTICLES_PACKET_CTOR.getParameterCount() == 11) {
                nmsPacket = PARTICLES_PACKET_CTOR.newInstance(
                        nmsType,
                        orig.isLongDistance(),
                        false,
                        pos.x, pos.y, pos.z,
                        off.x, off.y, off.z,
                        orig.getMaxSpeed(),
                        orig.getParticleCount()
                );
            } else {
                nmsPacket = PARTICLES_PACKET_CTOR.newInstance(
                        nmsType,
                        orig.isLongDistance(),
                        pos.x, pos.y, pos.z,
                        off.x, off.y, off.z,
                        orig.getMaxSpeed(),
                        orig.getParticleCount()
                );
            }
            Object serverPlayer = GET_HANDLE.invoke(player);
            Object connection = CONNECTION_FIELD.get(serverPlayer);
            SEND_METHOD.invoke(connection, nmsPacket);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to send blue fire particle: " + e.getMessage());
        }
    }

    public void clearPlayer(UUID playerId) {
        // No per-player state to clear currently
    }
}
