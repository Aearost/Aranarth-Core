package com.aearost.aranarthcore.items.incantation;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.INCANTATION_TYPE;

public class IncantationMagnetism implements Incantation {

    @Override
    public ItemStack getItem() {
        ItemStack incantation = new ItemStack(Material.GRAY_DYE, 1);

        ItemMeta meta = incantation.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "incantation_magnetism");
            meta.setItemModel(key);
            meta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "incantation_magnetism");
            meta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, 1);
            meta.setDisplayName(ChatUtils.translateToColor(getColor() + "Incantation of " + getIncantationName()));
            meta.setMaxStackSize(1);
            incantation.setItemMeta(meta);
        }
        return incantation;
    }

    @Override
    public String getIncantationName() {
        return "Magnetism";
    }

    @Override
    public int getLevelLimit() {
        return 1;
    }

    @Override
    public String getColor() {
        return "&8";
    }
}
