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

public class IncantationLifesteal implements Incantation {

    @Override
    public ItemStack getItem() {
        ItemStack incantation = new ItemStack(Material.LIME_DYE, 1);

        ItemMeta meta = incantation.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "lifesteal");
            meta.getPersistentDataContainer().set(INCANTATION_TYPE, PersistentDataType.STRING, "lifesteal");
            meta.getPersistentDataContainer().set(INCANTATION_LEVEL, PersistentDataType.INTEGER, 1);
            meta.setDisplayName(ChatUtils.translateToColor("&aIncantation of " + getIncantationName()));
            incantation.setItemMeta(meta);
        }
        return incantation;
    }

    @Override
    public String getIncantationName() {
        return "Lifesteal";
    }

    @Override
    public int getLevelLimit() {
        return 3;
    }
}
