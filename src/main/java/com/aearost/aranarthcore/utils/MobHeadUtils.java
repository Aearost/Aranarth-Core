package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and provides custom mob head ItemStacks for the Beheading incantation.
 */
public class MobHeadUtils {

    // [0] = display name, [1] = texture override, [2] = search-name override
    private static final Map<EntityType, String[]> headData = new EnumMap<>(EntityType.class);
    private static final Map<EntityType, Material> vanillaHeads = new EnumMap<>(EntityType.class);

    static {
        vanillaHeads.put(EntityType.CREEPER, Material.CREEPER_HEAD);
        vanillaHeads.put(EntityType.ZOMBIE, Material.ZOMBIE_HEAD);
        vanillaHeads.put(EntityType.SKELETON, Material.SKELETON_SKULL);
        vanillaHeads.put(EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL);
        vanillaHeads.put(EntityType.PIGLIN, Material.PIGLIN_HEAD);
        vanillaHeads.put(EntityType.ENDER_DRAGON, Material.DRAGON_HEAD);
    }

    /**
     * Loads mob head data from mob_heads.yml.
     */
    public static void initialize(Plugin plugin) {
        headData.clear();

        File file = new File(plugin.getDataFolder(), "mob_heads.yml");
        if (!file.exists()) {
            plugin.saveResource("mob_heads.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("mob-heads");
        if (section == null) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "mob_heads.yml is missing the 'mob-heads' section");
            return;
        }

        for (String key : section.getKeys(false)) {
            EntityType type;
            try {
                type = EntityType.valueOf(key);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + " Unknown entity type '" + key + "' in mob_heads.yml - skipping");
                continue;
            }

            String name = section.getString(key + ".name", "&f" + key + " Head");
            String texture = section.getString(key + ".texture", "");
            String searchName = section.getString(key + ".search-name", "");
            headData.put(type, new String[]{name, texture, searchName});
        }

        Bukkit.getLogger().info(AranarthCore.LOG_PREFIX + "Loaded " + headData.size()
                + " custom mob head entries (textures resolved from API) + "
                + vanillaHeads.size() + " vanilla head types");
    }

    /**
     * Returns whether this entity type can drop a head (either vanilla or custom).
     */
    public static boolean hasHead(EntityType type) {
        return vanillaHeads.containsKey(type) || headData.containsKey(type);
    }

    /**
     * Creates a PLAYER_HEAD with an arbitrary texture and display name.
     */
    public static ItemStack createCustomHead(String texture, String displayName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
        profile.setProperty(new ProfileProperty("textures", texture));
        meta.setPlayerProfile(profile);
        meta.setDisplayName(ChatUtils.translateToColor(displayName));
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Creates the head ItemStack for the given entity type, or null if unsupported.
     */
    public static ItemStack createHead(EntityType type) {
        // Vanilla head blocks
        if (vanillaHeads.containsKey(type)) {
            return new ItemStack(vanillaHeads.get(type));
        }

        String[] data = headData.get(type);
        if (data == null) {
            return null;
        }

        String displayName = data[0];
        String textureValue = data[1];

        // If no YAML override, look up from the API texture database
        if (textureValue.isEmpty()) {
            String searchName = data[2].isEmpty() ? toSearchName(type) : data[2];
            textureValue = HeadsDatabaseManager.getMobHeadTexture(searchName);
        }

        if (textureValue == null || textureValue.isEmpty()) {
            return null;
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "");
        profile.setProperty(new ProfileProperty("textures", textureValue));
        meta.setPlayerProfile(profile);

        meta.setDisplayName(ChatUtils.translateToColor(displayName));
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Converts an EntityType to a title-case search name for the minecraft-heads.com API.
     * e.g. CAVE_SPIDER -> "Cave Spider", SHEEP -> "Sheep"
     */
    private static String toSearchName(EntityType type) {
        String[] parts = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
