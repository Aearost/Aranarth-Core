package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.enums.FireType;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.FireParticleRegistry;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Intercepts outgoing flame particle packets and substitutes them with custom fire textures.
 */
public class FireParticleListener extends PacketListenerAbstract {

    private final Random random = new Random();
    private final Map<UUID, Integer> currentSpreeIndex = new HashMap<>();
    private final Map<UUID, Integer> spreeRemaining = new HashMap<>();

    // Cached reflective handles - populated once in initReflection()
    private static Object SOUL_FIRE_FLAME_NMS;
    private static Constructor<?> PARTICLES_PACKET_CTOR;
    private static Method GET_HANDLE;
    private static Field CONNECTION_FIELD;
    private static Method SEND_METHOD;
    private static boolean reflectionReady = false;

    /**
     * Call once from AranarthCore.onEnable() after PacketEvents is initialised.
     */
    public static void initReflection() {
        try {
            // SOUL_FIRE_FLAME from vanilla NMS ParticleTypes
            Class<?> nmsParticleTypesClass = Class.forName("net.minecraft.core.particles.ParticleTypes");
            Field soulFireField = nmsParticleTypesClass.getDeclaredField("SOUL_FIRE_FLAME");
            soulFireField.setAccessible(true);
            SOUL_FIRE_FLAME_NMS = soulFireField.get(null);

            // ClientboundLevelParticlesPacket
            Class<?> particleOptionsClass = Class.forName("net.minecraft.core.particles.ParticleOptions");
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket");
            PARTICLES_PACKET_CTOR = findParticlePacketCtor(packetClass, particleOptionsClass);
            PARTICLES_PACKET_CTOR.setAccessible(true);

            // CraftPlayer.getHandle() - ServerPlayer
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            GET_HANDLE = craftPlayerClass.getMethod("getHandle");

            // ServerPlayer.connection (public field)
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            CONNECTION_FIELD = serverPlayerClass.getField("connection");

            // ServerGamePacketListenerImpl.send(Packet)
            Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
            SEND_METHOD = CONNECTION_FIELD.getType().getMethod("send", packetInterface);

            reflectionReady = true;
            Bukkit.getLogger().info("[AC] Fire particle reflection initialised successfully");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[AC] Fire particle reflection failed - custom fire particles will not work: " + e.getMessage());
        }
    }

    private static Constructor<?> findParticlePacketCtor(Class<?> packetClass, Class<?> particleOptionsClass) throws NoSuchMethodException {
        for (Constructor<?> c : packetClass.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 10 && p[0].equals(particleOptionsClass) && p[1].equals(boolean.class)) {
                return c;
            }
        }
        throw new NoSuchMethodException("ClientboundLevelParticlesPacket 10-arg constructor not found");
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

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null) {
            return;
        }

        FireType fireType = aranarthPlayer.getFireType();
        if (fireType == FireType.DEFAULT) {
            return;
        }

        WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
        if (packet.getParticle().getType() != ParticleTypes.FLAME) {
            return;
        }

        Object nmsType = switch (fireType) {
            case BLUE -> SOUL_FIRE_FLAME_NMS;
            case WHITE -> FireParticleRegistry.getWhiteType();
            case RAINBOW -> {
                int i = nextSpreeIndex(player.getUniqueId(), FireParticleRegistry.getRainbowCount(), 10, 18);
                yield FireParticleRegistry.getRainbowType(i);
            }
            case IRIDESCENT -> {
                int i = nextSpreeIndex(player.getUniqueId(), FireParticleRegistry.getIridescentCount(), 14, 24);
                yield FireParticleRegistry.getIridescentType(i);
            }
            default -> null;
        };

        if (nmsType == null) {
            return;
        }

        event.setCancelled(true);
        sendNmsParticle(player, nmsType, packet);
    }

    private void sendNmsParticle(Player player, Object nmsType, WrapperPlayServerParticle orig) {
        try {
            Vector3d pos = orig.getPosition();
            Vector3f off = orig.getOffset();
            Object nmsPacket = PARTICLES_PACKET_CTOR.newInstance(
                    nmsType,
                    orig.isLongDistance(),
                    pos.x, pos.y, pos.z,
                    off.x, off.y, off.z,
                    orig.getMaxSpeed(),
                    orig.getParticleCount()
            );
            Object serverPlayer = GET_HANDLE.invoke(player);
            Object connection = CONNECTION_FIELD.get(serverPlayer);
            SEND_METHOD.invoke(connection, nmsPacket);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to send custom fire particle: " + e.getMessage());
        }
    }

    private int nextSpreeIndex(UUID playerId, int paletteSize, int minCount, int maxCount) {
        int remaining = spreeRemaining.getOrDefault(playerId, 0);
        if (remaining <= 0) {
            int newIndex = random.nextInt(paletteSize);
            currentSpreeIndex.put(playerId, newIndex);
            spreeRemaining.put(playerId, minCount + random.nextInt(maxCount - minCount + 1));
            return newIndex;
        }
        spreeRemaining.put(playerId, remaining - 1);
        return currentSpreeIndex.getOrDefault(playerId, 0);
    }

    public void clearPlayer(UUID playerId) {
        currentSpreeIndex.remove(playerId);
        spreeRemaining.remove(playerId);
    }
}
