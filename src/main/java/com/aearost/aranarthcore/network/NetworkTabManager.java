package com.aearost.aranarthcore.network;

import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

/**
 * Injects and removes fake tab-list entries for players on remote servers.
 * Uses reflection to access NMS packet classes at runtime so that no compile-time
 * dependency on the server jar or ProtocolLib is required.
 */
public class NetworkTabManager {

    // NMS class references resolved once on first use
    private static boolean initialised = false;
    private static boolean available   = false;

    private static Class<?> gameProfileClass;
    private static Class<?> propertyClass;
    private static Class<?> updatePacketClass;
    private static Class<?> removePacketClass;
    private static Class<?> actionClass;
    private static Class<?> entryClass;
    private static Class<?> gameTypeClass;
    private static Class<?> nmsComponentClass;
    private static Class<?> chatSessionDataClass;

    private static Object survivalGameType;

    private static synchronized void ensureInitialised() {
        if (initialised) return;
        initialised = true;
        try {
            gameProfileClass     = Class.forName("com.mojang.authlib.GameProfile");
            updatePacketClass    = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            removePacketClass    = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
            actionClass          = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            entryClass           = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
            gameTypeClass        = Class.forName("net.minecraft.world.level.GameType");
            nmsComponentClass    = Class.forName("net.minecraft.network.chat.Component");
            try {
                chatSessionDataClass = Class.forName("net.minecraft.network.chat.RemoteChatSession$Data");
            } catch (ClassNotFoundException ignored) {
                chatSessionDataClass = null; // not present in all versions
            }
            try {
                propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            } catch (ClassNotFoundException ignored) {
                propertyClass = null;
            }

            //noinspection unchecked,rawtypes
            survivalGameType = Enum.valueOf((Class<Enum>) gameTypeClass, "SURVIVAL");

            available = true;
            Bukkit.getLogger().info("[AC] NetworkTabManager: NMS tab injection enabled");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] NetworkTabManager: NMS classes not found, remote tab entries disabled (" + e.getMessage() + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Adds a remote player's tab entry to every currently online local player. */
    public static void addToTab(NetworkPlayer np) {
        addToTab(np, Bukkit.getOnlinePlayers());
    }

    public static void addToTab(NetworkPlayer np, Collection<? extends Player> viewers) {
        ensureInitialised();
        if (!available || viewers.isEmpty()) return;
        try {
            String profileName  = !np.getUsername().isEmpty() ? np.getUsername() : "Unknown";
            Object gameProfile  = createGameProfile(np.getUuid(), profileName,
                    np.getTextureValue(), np.getTextureSignature());
            int latency = np.getPing() >= 0 ? np.getPing() : 0;
            EnumSet<?> actionsSet = createActionEnumSet();

            // One packet per set of prefix styles, as each viewer sees the prefix in their own styles
            for (Map.Entry<String, List<Player>> group : groupByPrefixStyles(viewers).entrySet()) {
                PrefixStyle[] styles = PrefixStyle.decode(group.getKey());
                Object displayName  = toNmsComponent(buildDisplayName(np, styles)); // null on failure → falls back to profile name
                Object entry        = createEntry(np.getUuid(), gameProfile, displayName, 0, latency);
                Object packet       = updatePacketClass
                        .getDeclaredConstructor(EnumSet.class, List.class)
                        .newInstance(actionsSet, List.of(entry));
                for (Player viewer : group.getValue()) {
                    sendPacket(viewer, packet);
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[AC] addToTab failed", ex);
        }
    }

    /** Removes a remote player's tab entry from every currently online local player. */
    public static void removeFromTab(UUID uuid) {
        removeFromTab(uuid, Bukkit.getOnlinePlayers());
    }

    public static void removeFromTab(UUID uuid, Collection<? extends Player> viewers) {
        ensureInitialised();
        if (!available || viewers.isEmpty()) return;
        try {
            Object packet = removePacketClass
                    .getDeclaredConstructor(List.class)
                    .newInstance(List.of(uuid));
            for (Player viewer : viewers) {
                sendPacket(viewer, packet);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[AC] removeFromTab failed", ex);
        }
    }

    /**
     * Sends an UPDATE_LIST_ORDER packet for a remote player's fake tab entry so it
     * appears at the correct position in the combined cross-server sort.
     * This uses a Paper-specific action; silently does nothing if the action is not
     * present in the running server version.
     */
    public static void sendListOrder(UUID uuid, int listOrder) {
        sendListOrder(uuid, listOrder, Bukkit.getOnlinePlayers());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void sendListOrder(UUID uuid, int listOrder, Collection<? extends Player> viewers) {
        ensureInitialised();
        if (!available || viewers.isEmpty()) return;
        try {
            Enum updateOrderAction;
            try {
                updateOrderAction = Enum.valueOf((Class<Enum>) actionClass, "UPDATE_LIST_ORDER");
            } catch (IllegalArgumentException ignored) {
                return; // Action not present in this server version - skip silently
            }
            // For UPDATE_LIST_ORDER only the UUID and listOrder fields are read by the codec;
            // all other Entry fields can be null / default.
            Object entry = createEntry(uuid, null, null, listOrder, 0);
            EnumSet set = (EnumSet) EnumSet.class.getMethod("of", Enum.class)
                    .invoke(null, updateOrderAction);
            Object packet = updatePacketClass
                    .getDeclaredConstructor(EnumSet.class, List.class)
                    .newInstance(set, List.of(entry));
            for (Player viewer : viewers) {
                sendPacket(viewer, packet);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.FINE, "[AC] sendListOrder failed for " + uuid, ex);
        }
    }

    /**
     * Sends an UPDATE_DISPLAY_NAME | UPDATE_LATENCY packet for a remote player's tab entry
     * so both the [XXms] tag in their name and the signal bars reflect their latest ping.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void updatePingInTab(NetworkPlayer np) {
        ensureInitialised();
        if (!available) return;
        try {
            EnumSet set = (EnumSet) EnumSet.class.getMethod("noneOf", Class.class).invoke(null, actionClass);
            set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_DISPLAY_NAME"));
            try {
                set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_LATENCY"));
            } catch (IllegalArgumentException ignored) {
                // Not present in all versions
            }
            int latency = np.getPing() >= 0 ? np.getPing() : 0;
            for (Map.Entry<String, List<Player>> group : groupByPrefixStyles(Bukkit.getOnlinePlayers()).entrySet()) {
                Object displayName = toNmsComponent(buildDisplayName(np, PrefixStyle.decode(group.getKey())));
                Object entry = createEntry(np.getUuid(), null, displayName, 0, latency);
                Object packet = updatePacketClass
                        .getDeclaredConstructor(EnumSet.class, List.class)
                        .newInstance(set, List.of(entry));
                for (Player viewer : group.getValue()) {
                    sendPacket(viewer, packet);
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[AC] updatePingInTab failed for " + np.getUuid(), ex);
        }
    }

    /**
     * Sends a single UPDATE_DISPLAY_NAME packet to one viewer, overriding the tab names of the given
     * players for that viewer only. Used so each viewer sees everyone's prefixes in their own styles.
     *
     * @param viewer       The player whose tab list is being updated.
     * @param displayNames The §-formatted display name of each player, keyed by their UUID.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void sendDisplayNames(Player viewer, Map<UUID, String> displayNames) {
        ensureInitialised();
        if (!available || displayNames.isEmpty()) return;
        try {
            EnumSet set = (EnumSet) EnumSet.class.getMethod("of", Enum.class)
                    .invoke(null, Enum.valueOf((Class<Enum>) actionClass, "UPDATE_DISPLAY_NAME"));
            List<Object> entries = new ArrayList<>();
            for (Map.Entry<UUID, String> displayName : displayNames.entrySet()) {
                // For UPDATE_DISPLAY_NAME only the UUID and display name fields are read by the codec
                entries.add(createEntry(displayName.getKey(), null, toNmsComponent(displayName.getValue()), 0, 0));
            }
            Object packet = updatePacketClass
                    .getDeclaredConstructor(EnumSet.class, List.class)
                    .newInstance(set, entries);
            sendPacket(viewer, packet);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[AC] sendDisplayNames failed for " + viewer.getName(), ex);
        }
    }

    /**
     * Groups the viewers by their encoded prefix styles, so a packet only needs to be built once per set of styles.
     */
    private static Map<String, List<Player>> groupByPrefixStyles(Collection<? extends Player> viewers) {
        Map<String, List<Player>> groups = new HashMap<>();
        for (Player viewer : viewers) {
            PrefixStyle[] styles = AranarthUtils.getPrefixStyles(viewer);
            groups.computeIfAbsent(PrefixStyle.encode(styles[0], styles[1], styles[2]), k -> new ArrayList<>()).add(viewer);
        }
        return groups;
    }

    /**
     * Sends all currently tracked remote players to a player who just joined
     * so their tab is populated immediately.
     */
    public static void syncAllToPlayer(Player joiner) {
        ensureInitialised();
        if (!available) return;
        NetworkManager nm = NetworkManager.getInstance();
        if (nm == null) return;
        for (NetworkPlayer np : nm.getRemoteRoster().values()) {
            if (!np.isVanished()) {
                addToTab(np, Collections.singletonList(joiner));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a com.mojang.authlib.GameProfile via Paper's PlayerProfile API, optionally adding
     * the skin texture property so the client renders the correct player head in the tab list.
     * Uses Paper's abstraction rather than authlib internals because newer authlib versions
     * (shipped with Paper 1.20.2+) make PropertyMap immutable, causing direct put() calls to fail.
     */
    private static Object createGameProfile(UUID uuid, String name,
                                            String textureValue, String textureSignature) throws Exception {
        com.destroystokyo.paper.profile.PlayerProfile paperProfile = Bukkit.createProfile(uuid, name);
        if (textureValue != null && !textureValue.isEmpty()) {
            try {
                String sig = (textureSignature != null && !textureSignature.isEmpty()) ? textureSignature : null;
                paperProfile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", textureValue, sig));
            } catch (Exception e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                Bukkit.getLogger().warning("[AC] NetworkTabManager: texture injection failed for UUID " + uuid + ": " + cause);
            }
        }
        // Extract the underlying com.mojang.authlib.GameProfile from Paper's CraftPlayerProfile
        for (Method m : paperProfile.getClass().getMethods()) {
            if (gameProfileClass.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                return m.invoke(paperProfile);
            }
        }
        // Fallback: bare profile without skin
        return gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, name);
    }

    /** Creates a ClientboundPlayerInfoUpdatePacket.Entry record via reflection.
     *  Uses record-component names (Java 16+) to distinguish int fields so that
     *  {@code listOrder} and {@code latency} are set correctly, even as the
     *  NMS record gains or loses fields across Paper versions. */
    private static Object createEntry(UUID uuid, Object gameProfile, Object nmsComponent,
                                      int listOrder, int latency) throws Exception {
        Constructor<?> ctor = entryClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Class<?>[] params = ctor.getParameterTypes();
        Object[] args = new Object[params.length];

        // Resolve record-component names once so we can identify int fields precisely.
        java.lang.reflect.RecordComponent[] components = null;
        try { components = entryClass.getRecordComponents(); } catch (Exception ignored) {}

        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            String name = (components != null && i < components.length) ? components[i].getName() : "";
            if (p == UUID.class) {
                args[i] = uuid;
            } else if (p == gameProfileClass) {
                args[i] = gameProfile;
            } else if (p == boolean.class) {
                args[i] = true; // listed=true; showHat=true (1.21.4+)
            } else if (p == int.class) {
                // Use the component name to differentiate latency from listOrder.
                args[i] = name.equals("listOrder") ? listOrder : latency;
            } else if (p == gameTypeClass) {
                args[i] = survivalGameType;
            } else if (nmsComponentClass != null && p == nmsComponentClass) {
                args[i] = nmsComponent;
            } else if (chatSessionDataClass != null && p == chatSessionDataClass) {
                args[i] = null; // no chat session for fake player
            } else {
                args[i] = null;
            }
        }
        return ctor.newInstance(args);
    }

    /** Creates an EnumSet<Action> with ADD_PLAYER | UPDATE_LISTED | UPDATE_DISPLAY_NAME | UPDATE_LATENCY. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EnumSet<?> createActionEnumSet() throws Exception {
        EnumSet set = (EnumSet) EnumSet.class.getMethod("noneOf", Class.class).invoke(null, actionClass);
        set.add(Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER"));
        set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_LISTED"));
        set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_DISPLAY_NAME"));
        try {
            set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_LATENCY"));
        } catch (IllegalArgumentException ignored) {
            // Not present in all versions
        }
        return set;
    }

    /**
     * Extracts the skin texture value and signature from a locally-online player via NMS reflection.
     * Returns a two-element array {value, signature}; both are empty strings if unavailable.
     *
     * <p>Supports both old authlib API ({@code getValue()}/{@code getSignature()}) and the newer
     * record-based API ({@code value()}/{@code signature()}) introduced in authlib 3.x (Minecraft 1.20+).</p>
     */
    static String[] extractPlayerTexture(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);
            // Find getGameProfile() on ServerPlayer (may be obfuscated in some mappings)
            Method getGP = null;
            for (Method m : serverPlayer.getClass().getMethods()) {
                if (m.getName().equals("getGameProfile") && m.getParameterCount() == 0) {
                    getGP = m;
                    break;
                }
            }
            if (getGP == null) {
                Bukkit.getLogger().warning("[AC] NetworkTabManager.extractPlayerTexture: getGameProfile not found on ServerPlayer for " + player.getName());
                return new String[]{"", ""};
            }
            Object gp = getGP.invoke(serverPlayer);
            // Support both old authlib (getProperties) and new record-based authlib (properties)
            Method propMapGetter = null;
            for (String mName : new String[]{"getProperties", "properties"}) {
                try { propMapGetter = gp.getClass().getMethod(mName); break; }
                catch (NoSuchMethodException ignored) {}
            }
            if (propMapGetter == null) {
                Bukkit.getLogger().warning("[AC] NetworkTabManager.extractPlayerTexture: no property map accessor (getProperties/properties) on GameProfile for " + player.getName());
                return new String[]{"", ""};
            }
            Object propMap = propMapGetter.invoke(gp);
            // PropertyMap.get(Object) → Collection<Property>
            Collection<?> props = (Collection<?>) propMap.getClass()
                    .getMethod("get", Object.class).invoke(propMap, "textures");
            if (props == null || props.isEmpty()) {
                Bukkit.getLogger().warning("[AC] NetworkTabManager.extractPlayerTexture: no 'textures' property in GameProfile for " + player.getName());
                return new String[]{"", ""};
            }
            Object prop = props.iterator().next();

            // In authlib 3.x+ (Paper 1.20+) Property is a record: accessors are value()/signature().
            // In older authlib it was a plain class: getValue()/getSignature().
            // Try both so this works across server versions.
            String value = null;
            for (String methodName : new String[]{"getValue", "value"}) {
                try {
                    Object result = prop.getClass().getMethod(methodName).invoke(prop);
                    if (result instanceof String s && !s.isEmpty()) {
                        value = s;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            if (value == null || value.isEmpty()) {
                Bukkit.getLogger().warning("[AC] NetworkTabManager.extractPlayerTexture: texture value is null/empty for " + player.getName()
                        + " (tried getValue/value on " + prop.getClass().getName() + ")");
                return new String[]{"", ""};
            }

            String signature = "";
            for (String methodName : new String[]{"getSignature", "signature"}) {
                try {
                    Object result = prop.getClass().getMethod(methodName).invoke(prop);
                    if (result instanceof String s && !s.isEmpty()) {
                        signature = s;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            return new String[]{value, signature};
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] NetworkTabManager.extractPlayerTexture failed for " + player.getName() + ": " + e.getMessage());
            return new String[]{"", ""};
        }
    }

    /**
     * Converts a §-formatted legacy string to an NMS Component via Paper's Adventure bridge.
     * Returns null if the conversion fails (client will show the profile name instead).
     */
    private static Object toNmsComponent(String legacyFormatted) {
        try {
            net.kyori.adventure.text.Component adventure =
                    LegacyComponentSerializer.legacySection().deserialize(legacyFormatted);
            Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method asVanilla = paperAdventure.getMethod("asVanilla", net.kyori.adventure.text.Component.class);
            return asVanilla.invoke(null, adventure);
        } catch (Exception e) {
            return null;
        }
    }

    /** Sends an NMS packet to a player via their connection, resolved entirely through reflection. */
    private static void sendPacket(Player viewer, Object packet) {
        try {
            // CraftPlayer.getHandle() → ServerPlayer
            Method getHandle = viewer.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(viewer);

            // ServerPlayer.connection → ServerGamePacketListenerImpl
            Field connectionField = serverPlayer.getClass().getField("connection");
            Object connection = connectionField.get(serverPlayer);
            if (connection == null) return;

            // Find send(Packet) by name - avoids needing the Packet interface at compile time
            for (Method m : connection.getClass().getMethods()) {
                if (m.getName().equals("send") && m.getParameterCount() == 1) {
                    m.invoke(connection, packet);
                    return;
                }
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.WARNING, "[AC] sendPacket failed for " + viewer.getName(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Display name builder
    // -------------------------------------------------------------------------

    /**
     * Builds a §-formatted display name for a remote player, mirroring the local tab format:
     * rank badges + rank title + nickname + [SMP] tag (without the ⊰⊱ outer brackets).
     *
     * @param np     The remote player.
     * @param styles The council, saint, and rank styles chosen by the viewer via /prefix.
     */
    public static String buildDisplayName(NetworkPlayer np, PrefixStyle[] styles) {
        // Rank title - use female variants when applicable
        boolean isFemale = "FEMALE".equals(np.getPronouns());
        String display = AranarthUtils.buildPrefixElements(
                np.getCouncilRank(), styles[0], np.getArchitectRank() >= 1,
                np.getSaintRank(), styles[1], false,
                np.getRank(), isFemale, styles[2]) + " ";

        String name = np.getNickname().isEmpty() ? "Unknown" : np.getNickname();
        display += name;

        if (np.isAfk()) display += " &7[AFK]";

        // Ping tag - omitted until the first ping update arrives from the remote server
        int ping = np.getPing();
        if (ping >= 0) {
            if (ping <= 150) {
                display += " &8[&a" + ping + "ms&8]";
            } else if (ping <= 250) {
                display += " &8[&e" + ping + "ms&8]";
            } else {
                display += " &8[&c" + ping + "ms&8]";
            }
        }

        return ChatUtils.translateToColor(display);
    }
}
