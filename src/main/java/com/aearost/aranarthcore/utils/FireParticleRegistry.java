package com.aearost.aranarthcore.utils;

import com.projectkorra.projectkorra.Element;
import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Injects custom fire particle types into Minecraft's particle registry at server startup.
 */
public class FireParticleRegistry {

    // ProjectKorra sub-elements - set from AranarthCore.onEnable()
    public static Element.SubElement WHITE_FIRE_SUB;
    public static Element.SubElement RAINBOW_FIRE_SUB;
    public static Element.SubElement IRIDESCENT_FIRE_SUB;

    public static final String[] RAINBOW_KEYS = {
            "fire_rainbow_red", "fire_rainbow_orange", "fire_rainbow_yellow",
            "fire_rainbow_green", "fire_rainbow_blue", "fire_rainbow_purple"
    };

    public static final String[] IRIDESCENT_KEYS = {
            "fire_iridescent_pink", "fire_iridescent_blue", "fire_iridescent_mint",
            "fire_iridescent_lavender", "fire_iridescent_peach", "fire_iridescent_lemon"
    };

    private static Object whiteType;
    private static final List<Object> rainbowPalette = new ArrayList<>();
    private static final List<Object> iridescentPalette = new ArrayList<>();

    public static void register() {
        try {
            Object registry = getParticleRegistry();
            setFrozen(registry, false);

            whiteType = registerType(registry, "fire_white");
            for (String key : RAINBOW_KEYS) {
                rainbowPalette.add(registerType(registry, key));
            }
            for (String key : IRIDESCENT_KEYS) {
                iridescentPalette.add(registerType(registry, key));
            }

            setFrozen(registry, true);
            Bukkit.getLogger().info("[AC] Custom fire particle types registered successfully");
        } catch (Exception e) {
            throw new RuntimeException("[AC] Failed to register custom fire particle types", e);
        }
    }

    private static Object getParticleRegistry() throws Exception {
        Class<?> cls = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
        Field f = cls.getDeclaredField("PARTICLE_TYPE");
        f.setAccessible(true);
        return f.get(null);
    }

    private static Object registerType(Object registry, String key) throws Exception {
        // Create new SimpleParticleType(false)
        Class<?> typeClass = Class.forName("net.minecraft.core.particles.SimpleParticleType");
        Constructor<?> ctor = typeClass.getDeclaredConstructor(boolean.class);
        ctor.setAccessible(true);
        Object type = ctor.newInstance(false);

        // Build ResourceLocation for "aranarth:key"
        Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");
        Object location;
        try {
            // 1.20.5+ uses fromNamespaceAndPath factory method
            Method factory = rlClass.getMethod("fromNamespaceAndPath", String.class, String.class);
            location = factory.invoke(null, "aranarth", key);
        } catch (NoSuchMethodException e) {
            // Older versions use constructor(String, String)
            Constructor<?> rlCtor = rlClass.getDeclaredConstructor(String.class, String.class);
            rlCtor.setAccessible(true);
            location = rlCtor.newInstance("aranarth", key);
        }

        // Find and invoke Registry.register(Registry, ResourceLocation, Object)
        Class<?> registryClass = Class.forName("net.minecraft.core.Registry");
        Method registerMethod = findRegisterMethod(registryClass, rlClass);
        registerMethod.invoke(null, registry, location, type);

        return type;
    }

    private static Method findRegisterMethod(Class<?> registryClass, Class<?> rlClass) throws NoSuchMethodException {
        for (Method m : registryClass.getMethods()) {
            if (!m.getName().equals("register")) {
                continue;
            }
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 3 && params[1].equals(rlClass)) {
                return m;
            }
        }
        throw new NoSuchMethodException("Registry.register(Registry, ResourceLocation, Object) not found");
    }

    private static void setFrozen(Object registry, boolean frozen) throws Exception {
        Class<?> clazz = registry.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField("frozen");
                f.setAccessible(true);
                f.setBoolean(registry, frozen);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("'frozen' field not found in registry class hierarchy");
    }

    public static Object getWhiteType() {
        return whiteType;
    }

    public static Object getRainbowType(int i) {
        return rainbowPalette.get(i);
    }

    public static int getRainbowCount() {
        return rainbowPalette.size();
    }

    public static Object getIridescentType(int i) {
        return iridescentPalette.get(i);
    }

    public static int getIridescentCount() {
        return iridescentPalette.size();
    }
}
