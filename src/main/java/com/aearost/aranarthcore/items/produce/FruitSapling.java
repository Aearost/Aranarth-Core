package com.aearost.aranarthcore.items.produce;

import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.trees.CustomTrees.Variant;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.CustomItemUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.FRUIT_SAPLING;

/**
 * A sapling crafted from a fruit and the sapling of its tree, which always grows into that fruit tree.
 */
public record FruitSapling(Variant variant) implements AranarthItem {

    /**
     * The fruit of each rare tree, whose sapling only grows where the rare tree can.
     */
    private static final List<Variant> RARE_FRUIT = List.of(Variant.COCONUT_PALM, Variant.DATE_PALM, Variant.OLIVE_TREE);

    /**
     * @return The fruit sapling item.
     */
    public ItemStack getItem() {
        ItemStack item = new ItemStack(variant.getSapling(), 1);
        ItemMeta meta = item.getItemMeta();
        if (Objects.nonNull(meta)) {
            meta.getPersistentDataContainer().set(FRUIT_SAPLING, PersistentDataType.STRING, variant.name());
            meta.setDisplayName(ChatUtils.translateToColor(getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.translateToColor(Lang.getDefault("produce.fruit_sapling_lore")));
            if (variant.isRare()) {
                lore.add(ChatUtils.translateToColor(Lang.getDefault("produce.fruit_sapling_rare_lore",
                        "condition", Lang.getDefault(getConditionKey(variant)))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getName() {
        return Lang.getDefault("produce.fruit_sapling", "fruit", Lang.getDefault(getFruitLangKey(variant)));
    }

    /**
     * Provides the fruit tree that the item grows into.
     *
     * @param item The item.
     * @return The fruit tree, or null if the item is not a fruit sapling.
     */
    public static Variant getVariant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(FRUIT_SAPLING, PersistentDataType.STRING)) {
            return null;
        }
        try {
            Variant variant = Variant.valueOf(pdc.get(FRUIT_SAPLING, PersistentDataType.STRING));
            return getVariants().contains(variant) && item.getType() == variant.getSapling() ? variant : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Provides every tree that a fruit sapling can grow into: the fruit trees, then the rare trees that bear fruit.
     */
    public static List<Variant> getVariants() {
        List<Variant> variants = new ArrayList<>(Variant.fruit());
        variants.addAll(RARE_FRUIT);
        return Collections.unmodifiableList(variants);
    }

    /**
     * Provides the produce id of the fruit that crafts the sapling of the tree.
     *
     * @param variant The tree.
     * @return The produce id, where "apple" is the vanilla Apple.
     */
    public static String getFruitId(Variant variant) {
        return switch (variant) {
            case COCONUT_PALM -> "coconut";
            case DATE_PALM -> "date";
            case OLIVE_TREE -> "olive";
            default -> variant.getFruitId();
        };
    }

    /**
     * Provides the fruit tree whose sapling the fruit crafts.
     *
     * @param fruit The fruit, either produce or the vanilla Apple.
     * @return The tree, or null if the item is not a fruit with a sapling.
     */
    public static Variant forFruit(ItemStack fruit) {
        if (fruit == null) {
            return null;
        }
        String fruitId;
        if (fruit.getType() == Material.APPLE && !CustomItemUtils.isCustomItem(fruit)) {
            fruitId = "apple";
        } else {
            ProduceType type = Produce.getType(fruit);
            if (type == null) {
                return null;
            }
            fruitId = type.getId();
        }
        for (Variant variant : getVariants()) {
            if (getFruitId(variant).equals(fruitId)) {
                return variant;
            }
        }
        return null;
    }

    private static String getFruitLangKey(Variant variant) {
        return "produce." + getFruitId(variant);
    }

    /**
     * Provides the lang key of where a rare tree grows.
     */
    public static String getConditionKey(Variant variant) {
        return "trees.condition_" + variant.name().toLowerCase();
    }

    /**
     * @return The name of the tree used by /ac give, such as AppleSapling.
     */
    public static String getGiveName(Variant variant) {
        StringBuilder name = new StringBuilder();
        for (String part : getFruitId(variant).split("_")) {
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.append("Sapling").toString();
    }

    /**
     * @return The tree of the /ac give name, or null if there is none.
     */
    public static Variant fromGiveName(String giveName) {
        for (Variant variant : getVariants()) {
            if (getGiveName(variant).equals(giveName)) {
                return variant;
            }
        }
        return null;
    }

    /**
     * @return The names of every fruit sapling used by /ac give.
     */
    public static List<String> getGiveNames() {
        List<String> names = new ArrayList<>();
        for (Variant variant : getVariants()) {
            names.add(getGiveName(variant));
        }
        return names;
    }
}
