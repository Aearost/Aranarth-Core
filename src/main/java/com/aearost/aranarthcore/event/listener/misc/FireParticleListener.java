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
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intercepts outgoing flame particle packets and substitutes replacements based on the
 * fire type of the player who owns the ability or is on fire at that position.
 * The source's fire type is applied for all observers, not the receiver's.
 */
public class FireParticleListener extends PacketListenerAbstract {

    private final JavaPlugin plugin;

    private static Object SOUL_FIRE_FLAME_NMS;
    private static Constructor<?> PARTICLES_PACKET_CTOR;
    private static Method GET_HANDLE;
    private static Field CONNECTION_FIELD;
    private static Method SEND_METHOD;
    private static boolean reflectionReady = false;

    // White fire particles
    private static final Particle.DustOptions WHITE_DUST = new Particle.DustOptions(Color.WHITE, 1.2f);

    // Prismatic fire
    private static final Particle.DustTransition[] PRISMATIC_PAIRS = {
        new Particle.DustTransition(Color.fromRGB(235,  40,  40), Color.fromRGB(235, 140,  40), 1.1f), // red - orange
        new Particle.DustTransition(Color.fromRGB(235, 140,  40), Color.fromRGB(235, 235,  40), 1.1f), // orange - yellow
        new Particle.DustTransition(Color.fromRGB(235, 235,  40), Color.fromRGB( 40, 210,  40), 1.1f), // yellow - green
        new Particle.DustTransition(Color.fromRGB( 40, 210,  40), Color.fromRGB( 40, 210, 235), 1.1f), // green - cyan
        new Particle.DustTransition(Color.fromRGB( 40, 210, 235), Color.fromRGB( 40,  70, 235), 1.1f), // cyan - blue
        new Particle.DustTransition(Color.fromRGB( 40,  70, 235), Color.fromRGB(150,  40, 210), 1.1f), // blue - violet
        new Particle.DustTransition(Color.fromRGB(150,  40, 210), Color.fromRGB(235,  40, 185), 1.1f), // violet - magenta
        new Particle.DustTransition(Color.fromRGB(235,  40, 185), Color.fromRGB(235,  40,  40), 1.1f), // magenta - red
    };

    // Cycles through PRISMATIC_PAIRS in order
    private static final AtomicInteger prismaticPairIndex = new AtomicInteger(0);

    // Per-receiver flame packet counter for even flame distribution in PRISMATIC
    private static final ConcurrentHashMap<UUID, AtomicInteger> prismaticFlameCounters = new ConcurrentHashMap<>();
    private static final int FLAME_EVERY_N = 7;

    // Position registry: encodePosition key -> [fireType ordinal, timestamp ms]
    // Populated each tick from fire-type players' active ability positions and own-fire positions.
    private static final ConcurrentHashMap<Long, long[]> sourceRegistry = new ConcurrentHashMap<>();
    private static final long SOURCE_TTL_MS = 300;

    public FireParticleListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // Advance the prismatic color pair every 20 ticks (1 s), cycling through all 8 pairs
        Bukkit.getScheduler().runTaskTimer(plugin, () ->
                prismaticPairIndex.set((prismaticPairIndex.get() + 1) % PRISMATIC_PAIRS.length), 20L, 20L);

        // Every tick: register fire-type players' ability positions and self-fire positions as sources
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            sourceRegistry.entrySet().removeIf(e -> now - e.getValue()[1] > SOURCE_TTL_MS);

            for (Player p : Bukkit.getOnlinePlayers()) {
                AranarthPlayer ap = AranarthUtils.getPlayer(p.getUniqueId());
                if (ap == null || ap.getFireType() == FireType.DEFAULT) continue;
                FireType ft = ap.getFireType();

                // Player's own position when they are on fire (foot + eye level)
                if (p.getFireTicks() > 0) {
                    registerFireSource(p.getLocation(), ft);
                    registerFireSource(p.getEyeLocation(), ft);
                }

                // All active fire ability positions for this player (+ y+1 to cover chest/head offsets)
                UUID pid = p.getUniqueId();
                for (CoreAbility ability : CoreAbility.getAbilitiesByInstances()) {
                    if (!(ability instanceof FireAbility)) continue;
                    if (ability.getPlayer() == null || !ability.getPlayer().getUniqueId().equals(pid)) continue;
                    for (Location loc : ability.getLocations()) {
                        if (loc == null) continue;
                        registerFireSource(loc, ft);
                        registerFireSource(loc.clone().add(0, 1, 0), ft);
                    }
                }
            }
        }, 0L, 1L);
    }

    /**
     * Registers a world position as belonging to a specific fire type source.
     * Called from the tick scheduler for active abilities and self-fire.
     */
    public static void registerFireSource(Location loc, FireType type) {
        long key = encodePosition(loc.getX(), loc.getY(), loc.getZ());
        sourceRegistry.put(key, new long[]{type.ordinal(), System.currentTimeMillis()});
    }

    private static FireType lookupSource(Vector3d pos) {
        long key = encodePosition(pos.x, pos.y, pos.z);
        long[] entry = sourceRegistry.get(key);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry[1] > SOURCE_TTL_MS) {
            sourceRegistry.remove(key);
            return null;
        }
        return FireType.values()[(int) entry[0]];
    }

    private static long encodePosition(double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        return ((long)(bx & 0xFFFFF) << 40) | ((long)(by & 0xFFFFF) << 20) | (long)(bz & 0xFFFFF);
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

        // Determine the fire type of whoever owns this fire source (ability or self-fire).
        // If no source is registered at this position, let the regular FLAME through.
        FireType sourceType = lookupSource(packet.getPosition());
        if (sourceType == null || sourceType == FireType.DEFAULT) {
            return;
        }

        if (sourceType == FireType.BLUE) {
            event.setCancelled(true);
            sendNmsParticle(player, SOUL_FIRE_FLAME_NMS, packet);
            return;
        }

        if (sourceType == FireType.WHITE) {
            event.setCancelled(true);
            sendWhiteFireParticles(player, packet);
            return;
        }

        if (sourceType == FireType.PRISMATIC) {
            // Every Nth packet lets the original flame through for even distribution
            AtomicInteger counter = prismaticFlameCounters.computeIfAbsent(player.getUniqueId(), k -> new AtomicInteger(0));
            if (counter.incrementAndGet() % FLAME_EVERY_N == 0) {
                return;
            }
            event.setCancelled(true);
            sendPrismaticFireParticles(player, packet);
        }
    }

    private void sendWhiteFireParticles(Player player, WrapperPlayServerParticle orig) {
        Vector3d pos = orig.getPosition();
        Vector3f off = orig.getOffset();
        int count = orig.getParticleCount();
        float speed = orig.getMaxSpeed();
        World world = player.getWorld();
        Location loc = new Location(world, pos.x, pos.y, pos.z);

        float spreadX = off.x * 1.8f;
        float spreadY = off.y * 1.8f;
        float spreadZ = off.z * 1.8f;
        double roll = ThreadLocalRandom.current().nextDouble();
        for (long delay = 0; delay <= 2; delay++) {
            if (roll < 0.125) {
                // ~1/8 end rod
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.spawnParticle(Particle.END_ROD, loc, count, spreadX, spreadY, spreadZ, speed), delay);
            } else if (roll < 0.325) {
                // ~1/5 white dust
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.spawnParticle(Particle.DUST, loc, count, spreadX, spreadY, spreadZ, 0, WHITE_DUST), delay);
            } else {
                // ~67.5% electric spark
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        player.spawnParticle(Particle.ELECTRIC_SPARK, loc, count, spreadX, spreadY, spreadZ, speed), delay);
            }
        }
    }

    private void sendPrismaticFireParticles(Player player, WrapperPlayServerParticle orig) {
        Vector3d pos = orig.getPosition();
        Vector3f off = orig.getOffset();
        int count = orig.getParticleCount();
        World world = player.getWorld();
        Location loc = new Location(world, pos.x, pos.y, pos.z);

        float spreadX = off.x * 1.8f;
        float spreadY = off.y * 1.8f;
        float spreadZ = off.z * 1.8f;
        int spawnCount = Math.max(1, count / 2);
        Particle.DustTransition colorPair = PRISMATIC_PAIRS[prismaticPairIndex.get()];
        for (long delay = 0; delay <= 2; delay++) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    player.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, spawnCount, spreadX, spreadY, spreadZ, 0, colorPair), delay);
        }
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
        prismaticFlameCounters.remove(playerId);
    }
}
