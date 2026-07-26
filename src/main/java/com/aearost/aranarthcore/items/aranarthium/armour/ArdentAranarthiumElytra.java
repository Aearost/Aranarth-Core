package com.aearost.aranarthcore.items.aranarthium.armour;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.ARMOR_TYPE;

public class ArdentAranarthiumElytra implements AranarthItem {

    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.ELYTRA, 1);
        ItemMeta meta = item.getItemMeta();
        if (Objects.nonNull(meta)) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), "aranarthium_ardent_elytra");
            meta.setItemModel(key);
            meta.setDisplayName(ChatUtils.translateToColor(getName()));
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ChatUtils.translateToColor(getLore()));
            meta.setLore(lore);
            meta.addAttributeModifier(Attribute.ARMOR,
                    new AttributeModifier(new NamespacedKey(AranarthCore.getInstance(), "ardent_elytra_armor"),
                            8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS,
                    new AttributeModifier(new NamespacedKey(AranarthCore.getInstance(), "ardent_elytra_toughness"),
                            3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(new NamespacedKey(AranarthCore.getInstance(), "ardent_elytra_knockback"),
                            0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
            meta.getPersistentDataContainer().set(ARMOR_TYPE, PersistentDataType.STRING, "ardent");
            ((Damageable) meta).setMaxDamage(592);
            meta.setFireResistant(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getName() {
        return "&#696969&lArdent Aranarthium Elytra";
    }

    public String getLore() {
        return "&7&oYou feel its strength...";
    }
}
