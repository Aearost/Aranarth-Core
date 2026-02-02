package com.aearost.aranarthcore.items.enchantment;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ENCHANTMENT;

public class BookBeheading implements AranarthEnchantment {

    @Override
    public ItemStack getItem(int level) {
        // Basic enchanted book with no enchantments applied
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);

        ItemMeta meta = book.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "beheading");
            List<String> lore = new ArrayList<>();
            meta.getPersistentDataContainer().set(ENCHANTMENT, PersistentDataType.STRING, "beheading");
            lore.add(ChatUtils.translateToColor("&9Ingredients"));
            lore.add(ChatUtils.translateToColor("&7" + getEnchantmentName() + " " + AranarthUtils.getEnchantmentLevelLetters(level)));
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    @Override
    public String getEnchantmentName() {
        return "Beheading";
    }
}
