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

public class IncantationBeheading implements Incantation {

    @Override
    public ItemStack getItem() {
        ItemStack incantation = new ItemStack(Material.BROWN_DYE, 1);

        ItemMeta meta = incantation.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "beheading");
            meta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "beheading");
            meta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, 1);
            meta.setDisplayName(ChatUtils.translateToColor("&cIncantation of " + getIncantationName()));
            meta.setMaxStackSize(1);
            incantation.setItemMeta(meta);
        }
        return incantation;
    }

    @Override
    public String getIncantationName() {
        return "Beheading";
    }

    @Override
    public int getLevelLimit() {
        return 3;
    }
}
