package com.aearost.aranarthcore.network;

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

            //noinspection unchecked
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
            Object gameProfile  = createGameProfile(np.getUuid(), np.getNickname().isEmpty() ? "Unknown" : np.getNickname());
            Object displayName  = toNmsComponent(buildDisplayName(np)); // null on failure → falls back to profile name
            Object entry        = createEntry(np.getUuid(), gameProfile, displayName);
            Object actionsSet   = createActionEnumSet();
            Object packet       = updatePacketClass
                    .getDeclaredConstructor(EnumSet.class, List.class)
                    .newInstance(actionsSet, List.of(entry));

            for (Player viewer : viewers) {
                sendPacket(viewer, packet);
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

    /** Creates a com.mojang.authlib.GameProfile via reflection. */
    private static Object createGameProfile(UUID uuid, String name) throws Exception {
        return gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, name);
    }

    /** Creates a ClientboundPlayerInfoUpdatePacket.Entry record via reflection.
     *  Uses the first declared constructor and fills arguments by type so that
     *  changes to the record's field list (e.g. showHat added in 1.21.4) are
     *  handled automatically without needing to update this code. */
    @SuppressWarnings("unchecked")
    private static Object createEntry(UUID uuid, Object gameProfile, Object nmsComponent) throws Exception {
        Constructor<?> ctor = entryClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Class<?>[] params = ctor.getParameterTypes();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Class<?> p = params[i];
            if (p == UUID.class) {
                args[i] = uuid;
            } else if (p == gameProfileClass) {
                args[i] = gameProfile;
            } else if (p == boolean.class) {
                args[i] = true; // listed=true; showHat=true (1.21.4+)
            } else if (p == int.class) {
                args[i] = 0; // latency
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

    /** Creates an EnumSet<Action> with ADD_PLAYER | UPDATE_LISTED | UPDATE_DISPLAY_NAME. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object createActionEnumSet() throws Exception {
        Set set = (Set) EnumSet.class.getMethod("noneOf", Class.class).invoke(null, actionClass);
        set.add(Enum.valueOf((Class<Enum>) actionClass, "ADD_PLAYER"));
        set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_LISTED"));
        set.add(Enum.valueOf((Class<Enum>) actionClass, "UPDATE_DISPLAY_NAME"));
        return set;
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

            // Find send(Packet) by name — avoids needing the Packet interface at compile time
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
     * Rank titles default to their male variants since pronouns are not stored in NetworkPlayer.
     */
    private static String buildDisplayName(NetworkPlayer np) {
        String display = "";

        // Council badge
        display += switch (np.getCouncilRank()) {
            case 1 -> "&3۞&r";
            case 2 -> "&6۞&r";
            case 3 -> "&4۞&r";
            default -> "";
        };

        // Architect badge
        if (np.getArchitectRank() >= 1) display += "&a&l\uD83D\uDD28&r";

        // Saint badge
        display += switch (np.getSaintRank()) {
            case 1 -> "&b⚜&r";
            case 2 -> "&e⚜&r";
            case 3 -> "&c⚜&r";
            default -> "";
        };

        if (!display.isEmpty()) display += " ";

        // Rank title
        display += switch (np.getRank()) {
            case 1 -> "&d&l[&a&lEsquire&d&l] &r";
            case 2 -> "&7&l[&f&lKnight&7&l] &r";
            case 3 -> "&5&l[&d&lBaron&5&l] &r";
            case 4 -> "&8&l[&7&lCount&8&l] &r";
            case 5 -> "&6&l[&e&lDuke&6&l] &r";
            case 6 -> "&6&l[&b&lPrince&6&l] &r";
            case 7 -> "&6&l[&9&lKing&6&l] &r";
            case 8 -> "&6&l[&4&lEmperor&6&l] &r";
            default -> "&8&l[&a&lPeasant&8&l] &r";
        };

        String name = np.getNickname().isEmpty() ? "Unknown" : np.getNickname();
        display += name + "&r &8[&7SMP&8]";

        return ChatUtils.translateToColor(display);
    }
}
