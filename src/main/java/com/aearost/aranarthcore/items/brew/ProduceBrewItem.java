package com.aearost.aranarthcore.items.brew;

import com.aearost.aranarthcore.items.produce.Produce;
import com.aearost.aranarthcore.items.produce.ProduceType;
import com.dre.brewery.recipe.PluginItem;
import org.bukkit.inventory.ItemStack;

/**
 * Lets BreweryX recipes use AranarthCore produce as an ingredient.
 */
public class ProduceBrewItem extends PluginItem {

    /** The plugin prefix used in BreweryX recipes.yml. */
    public static final String PREFIX = "aranarthcore";

    @Override
    public boolean matches(ItemStack item) {
        ProduceType type = Produce.getType(item);
        return type != null && type.getId().equalsIgnoreCase(getItemId());
    }
}
