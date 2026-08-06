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
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Intercepts outgoing flame particle packets and substitutes soul fire flame for blue fire players.
 */
public class FireParticleListener extends PacketListenerAbstract {

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

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer == null) {
            return;
        }

        if (aranarthPlayer.getFireType() != FireType.BLUE) {
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

        event.setCancelled(true);
        sendNmsParticle(player, SOUL_FIRE_FLAME_NMS, packet);
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
