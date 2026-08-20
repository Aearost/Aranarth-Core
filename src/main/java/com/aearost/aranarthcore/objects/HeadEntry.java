package com.aearost.aranarthcore.objects;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a custom decorative head fetched from the minecraft-heads.com API.
 */
public record HeadEntry(String name, String texture, @Nullable Material material, String category) {

    public boolean isExchangeable() {
        return material != null;
    }
}
