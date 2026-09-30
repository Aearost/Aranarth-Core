package com.aearost.aranarthcore.items.essence;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_LEVEL;
import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_TYPE;

public class EssenceBeheading implements Essence {

    @Override
    public ItemStack getItem() {
        ItemStack essence = new ItemStack(Material.BROWN_DYE, 1);

        ItemMeta meta = essence.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "essence_beheading");
            meta.setItemModel(key);
            meta.getPersistentDataContainer().set(ESSENCE_TYPE, PersistentDataType.STRING, "essence_beheading");
            meta.getPersistentDataContainer().set(ESSENCE_LEVEL, PersistentDataType.INTEGER, 1);
            meta.setDisplayName(ChatUtils.translateToColor(getColor() + "Essence of " + getEssenceName()));
            meta.setMaxStackSize(1);
            essence.setItemMeta(meta);
        }
        return essence;
    }

    @Override
    public String getEssenceName() {
        return "Beheading";
    }

    @Override
    public int getLevelLimit() {
        return 3;
    }

    @Override
    public String getColor() {
        return "&#5b0001";
    }
}
