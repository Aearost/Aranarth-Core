package com.aearost.aranarthcore.event.crafting;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import io.papermc.paper.event.player.CartographyItemEvent;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static com.aearost.aranarthcore.objects.CustomKeys.MAP_ORIGINAL;

/**
 * Prevents map copies from being further copied. Only original maps may be duplicated.
 */
public class CraftingOverridesMap {

    private static final String COPY = "copy";
    private static final String LORE_ORIGINAL = ChatUtils.translateToColor("&7Original");
    private static final String LORE_COPY = ChatUtils.translateToColor("&7Copy of original");


    /**
     * Returns true when the crafting matrix represents a map-copy recipe:
     * at least one FILLED_MAP source and at least one blank MAP.
     */
    public static boolean isMapCopyRecipe(ItemStack[] matrix, ItemStack result) {
        if (result == null || result.getType() != Material.FILLED_MAP) {
            return false;
        }
        boolean hasFilledMap = false;
        boolean hasBlankMap = false;
        for (ItemStack ingredient : matrix) {
            if (ingredient == null) {
                continue;
            }
            if (ingredient.getType() == Material.FILLED_MAP) {
                hasFilledMap = true;
            }
            if (ingredient.getType() == Material.MAP) {
                hasBlankMap = true;
            }
        }
        return hasFilledMap && hasBlankMap;
    }

    /**
     * Cancels the recipe preview if the source map is a copy.
     */
    public void preCraft(PrepareItemCraftEvent e) {
        if (!isMapCopyRecipe(e.getInventory().getMatrix(), e.getInventory().getResult())) {
            return;
        }
        for (ItemStack ingredient : e.getInventory().getMatrix()) {
            if (ingredient == null || ingredient.getType() != Material.FILLED_MAP) {
                continue;
            }
            if (isCopy(ingredient)) {
                e.getInventory().setResult(null);
                return;
            }
        }
        // Source is valid, show only 1 result in the output slot
        ItemStack result = e.getInventory().getResult();
        if (result != null) {
            ItemStack preview = result.clone();
            preview.setAmount(1);
            e.getInventory().setResult(preview);
        }
    }

    /**
     * Cancels the craft if the source map is a copy, otherwise tags the result as a copy.
     */
    public void onCraft(CraftItemEvent e, HumanEntity player) {
        if (!isMapCopyRecipe(e.getInventory().getMatrix(), e.getInventory().getResult())) {
            return;
        }
        for (ItemStack ingredient : e.getInventory().getMatrix()) {
            if (ingredient == null || ingredient.getType() != Material.FILLED_MAP) {
                continue;
            }
            if (isCopy(ingredient)) {
                e.setCancelled(true);
                player.sendMessage(ChatUtils.chatMessage("&cMap copies cannot be copied further!"));
                return;
            }
        }
        // Source is an original, tag the result as a copy with amount 1
        ItemStack result = e.getInventory().getResult();
        if (result != null) {
            ItemStack tagged = result.clone();
            tagged.setAmount(1);
            tagAsCopy(tagged);
            e.getInventory().setResult(tagged);
        }
    }

    /**
     * Returns true when a cartography table is being used to clone a map.
     */
    public static boolean isCartographyMapCopyRecipe(CartographyInventory inv) {
        ItemStack slot0 = inv.getItem(0);
        ItemStack slot1 = inv.getItem(1);
        ItemStack result = inv.getResult();
        return slot0 != null && slot0.getType() == Material.FILLED_MAP
                && slot1 != null && slot1.getType() == Material.MAP
                && result != null && result.getType() == Material.FILLED_MAP;
    }

    /**
     * Clears the cartography table result preview if the source map is a copy,
     * otherwise pre-tags the result as a copy so the player receives the tagged item.
     */
    public void preCartographyCraft(PrepareResultEvent e) {
        CartographyInventory inv = (CartographyInventory) e.getInventory();
        if (!isCartographyMapCopyRecipe(inv)) {
            return;
        }
        ItemStack sourceMap = inv.getItem(0);
        if (isCopy(sourceMap)) {
            e.setResult(null);
            return;
        }
        // Set result to 1 tagged copy
        ItemStack tagged = e.getResult().clone();
        tagged.setAmount(1);
        tagAsCopy(tagged);
        e.setResult(tagged);
    }

    /**
     * Safety net to cancel taking the result from a cartography table if the source map is a copy.
     */
    public void onCartographyCraft(CartographyItemEvent e) {
        CartographyInventory inv = e.getInventory();
        if (!isCartographyMapCopyRecipe(inv)) {
            return;
        }
        ItemStack sourceMap = inv.getItem(0);
        if (isCopy(sourceMap)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage(ChatUtils.chatMessage("&cMap copies cannot be copied further!"));
        }
    }

    /**
     * Marks the given map as a copy: sets the PDC tag and applies copy lore.
     */
    public void tagAsCopy(ItemStack map) {
        ItemMeta meta = map.getItemMeta();
        meta.getPersistentDataContainer().set(MAP_ORIGINAL, PersistentDataType.STRING, COPY);
        setOrReplaceLoreLine(meta, LORE_COPY);
        map.setItemMeta(meta);
    }

    public void tagAsOriginal(ItemStack map) {
        ItemMeta meta = map.getItemMeta();
        if (!hasMapLore(meta)) {
            setOrReplaceLoreLine(meta, LORE_ORIGINAL);
            map.setItemMeta(meta);
        }
    }

    public boolean isCopy(ItemStack map) {
        if (map == null || !map.hasItemMeta()) {
            return false;
        }
        String status = map.getItemMeta().getPersistentDataContainer().get(MAP_ORIGINAL, PersistentDataType.STRING);
        return COPY.equals(status);
    }

    private boolean hasMapLore(ItemMeta meta) {
        if (!meta.hasLore()) {
            return false;
        }
        List<String> lore = meta.getLore();
        return lore.contains(LORE_ORIGINAL) || lore.contains(LORE_COPY);
    }

    private void setOrReplaceLoreLine(ItemMeta meta, String line) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l.equals(LORE_ORIGINAL) || l.equals(LORE_COPY));
        lore.add(line);
        meta.setLore(lore);
    }
}
