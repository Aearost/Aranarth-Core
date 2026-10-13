package com.aearost.aranarthcore.items.essence;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EssenceImbueUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ESSENCE_IMBUED_XP;

/**
 * Provides the necessary components of a blank Essence, which is imbued with XP to craft an essence.
 */
public class EssenceBlank implements AranarthItem {

    /**
     * @return The blank Essence.
     */
    public ItemStack getItem() {
        ItemStack essence = new ItemStack(Material.PRISMARINE_SHARD, 1);

        ItemMeta meta = essence.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "essence_blank");
            meta.setItemModel(key);
            meta.getPersistentDataContainer().set(ESSENCE_IMBUED_XP, PersistentDataType.INTEGER, 0);
            meta.setDisplayName(ChatUtils.translateToColor(getName()));
            meta.setMaxStackSize(1);
            meta.setLore(EssenceImbueUtils.buildLore(0));
            essence.setItemMeta(meta);
        }
        return essence;
    }

    public String getName() {
        return "&fEssence";
    }
}
