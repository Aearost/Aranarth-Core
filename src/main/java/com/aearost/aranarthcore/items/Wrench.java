package com.aearost.aranarthcore.items;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

public class Wrench implements AranarthItem {

    public static final int MAX_DURABILITY = 100;

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta meta = item.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "wrench");
            meta.setItemModel(key);
            meta.getPersistentDataContainer().set(WRENCH, PersistentDataType.STRING, "wrench");
            meta.getPersistentDataContainer().set(WRENCH_LAST_BLOCK, PersistentDataType.STRING, "");
            meta.setDisplayName(ChatUtils.translateToColor(getName()));
            meta.setLore(List.of(
                    ChatUtils.translateToColor("&7&oRight-click a block to edit its"),
                    ChatUtils.translateToColor("&7&oorientation and placement state")
            ));
            if (meta instanceof Damageable damageable) {
                damageable.setMaxDamage(MAX_DURABILITY);
            }
            meta.setMaxStackSize(1);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getName() {
        return "&8&lWrench";
    }
}
