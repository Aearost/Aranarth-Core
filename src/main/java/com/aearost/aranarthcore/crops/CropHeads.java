package com.aearost.aranarthcore.crops;

import com.aearost.aranarthcore.utils.MobHeadUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the textured head items shown by item displays on crops, such as the tiny berries on a bush.
 */
public class CropHeads {

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    /**
     * Provides a head item with the texture. These items are only ever shown in displays, never given to players.
     */
    public static ItemStack item(String texture) {
        return CACHE.computeIfAbsent(texture, key -> {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setPlayerProfile(MobHeadUtils.createTextureProfile(key));
                head.setItemMeta(meta);
            }
            return head;
        }).clone();
    }
}
