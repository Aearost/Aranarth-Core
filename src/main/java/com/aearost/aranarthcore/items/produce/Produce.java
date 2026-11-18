package com.aearost.aranarthcore.items.produce;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.aearost.aranarthcore.objects.CustomKeys.PRODUCE;

/**
 * Provides the necessary components of a fruit, berry, crop, seed or nut item.
 */
public class Produce implements AranarthItem {

    private static final Random RANDOM = new Random();

    private final ProduceType type;

    public Produce(ProduceType type) {
        this.type = type;
    }

    /**
     * @return The produce item.
     */
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.FIREWORK_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        if (Objects.nonNull(meta)) {
            meta.setItemModel(new NamespacedKey(AranarthCore.getInstance(), type.getId()));
            meta.getPersistentDataContainer().set(PRODUCE, PersistentDataType.STRING, type.getId());
            meta.setDisplayName(ChatUtils.translateToColor(getName()));
            item.setItemMeta(meta);
        }
        ProduceType.Food food = type.getFood();
        if (food != null) {
            item.setData(DataComponentTypes.FOOD, FoodProperties.food().nutrition(food.nutrition())
                    .saturation(food.saturation()).build());
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(food.seconds())
                    .animation(ItemUseAnimation.EAT).build());
        }
        return item;
    }

    public String getName() {
        return Lang.getDefault(type.getLangKey());
    }

    public ProduceType getType() {
        return type;
    }

    /**
     * Provides the produce type of the item.
     * @param item The item.
     * @return The produce type, or null if the item is not produce.
     */
    public static ProduceType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(PRODUCE, PersistentDataType.STRING)) {
            return null;
        }
        return ProduceType.fromId(pdc.get(PRODUCE, PersistentDataType.STRING));
    }

    /**
     * Rolls a random tree fruit for a Vote Crate, the vanilla Apple included.
     * @param amount The amount of fruit.
     * @return The fruit.
     */
    public static ItemStack randomVoteCrateFruit(int amount) {
        List<ProduceType> fruit = ProduceType.getVoteCrateFruit();
        int roll = RANDOM.nextInt(fruit.size() + 1);
        ItemStack item = roll == fruit.size() ? new ItemStack(Material.APPLE) : new Produce(fruit.get(roll)).getItem();
        item.setAmount(amount);
        return item;
    }

    /**
     * Provides every fruit that can be rolled from a Vote Crate, the vanilla Apple included, in display order.
     * @return The fruit items, each with an amount of 1.
     */
    public static List<ItemStack> getVoteCrateFruitItems() {
        List<ItemStack> items = new ArrayList<>();
        for (ProduceType type : ProduceType.getVoteCrateFruit()) {
            items.add(new Produce(type).getItem());
        }
        items.add(new ItemStack(Material.APPLE));
        return items;
    }

    /**
     * Provides the lang key of the fruit's name, the vanilla Apple included.
     * @param item The fruit.
     * @return The lang key.
     */
    public static String getLangKey(ItemStack item) {
        ProduceType type = getType(item);
        return type == null ? "produce.apple" : type.getLangKey();
    }
}
