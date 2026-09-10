package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Automatically blocks the Wandering Enchanter over-leveled trades.
 */
public class EnchanterLockoutListener implements Listener {

    private static final int RESULT_SLOT = 2;

    private final AranarthCore plugin;

    public EnchanterLockoutListener(AranarthCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
            return;
        }
        if (e.getRawSlot() != RESULT_SLOT) {
            return;
        }
        if (e.getCurrentItem() == null) {
            return;
        }
        if (!isTakeAction(e.getAction())) {
            return;
        }

        if (!(merchantInv.getMerchant() instanceof WanderingTrader wt)) {
            return;
        }

        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (!pdc.has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
            return;
        }
        if (!"ENCHANTER".equals(pdc.get(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING))) {
            return;
        }
        if (!pdc.has(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING)) {
            return;
        }

        String lockoutData = pdc.get(CustomKeys.ENCHANTER_LOCKOUT, PersistentDataType.STRING);
        Set<Integer> overLeveledIndices = Arrays.stream(lockoutData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        int selectedIndex = merchantInv.getSelectedRecipeIndex();
        if (!overLeveledIndices.contains(selectedIndex)) {
            return;
        }

        // Defer setRecipes to the next tick so the click event fully completes
        // and the book is handed to the player before the recipe list is refreshed.
        // Calling setRecipes synchronously mid-event resets the result slot and
        // causes the book to disappear even though the emeralds were consumed.
        new BukkitRunnable() {
            @Override
            public void run() {
                // Lock all other over-leveled trades
                List<MerchantRecipe> recipes = wt.getRecipes();
                for (int idx : overLeveledIndices) {
                    if (idx == selectedIndex || idx >= recipes.size()) {
                        continue;
                    }
                    MerchantRecipe recipe = recipes.get(idx);
                    recipe.setUses(recipe.getMaxUses());
                }
                wt.setRecipes(recipes);
            }
        }.runTask(plugin);
    }

    private boolean isTakeAction(InventoryAction action) {
        return switch (action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                 MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP -> true;
            default -> false;
        };
    }
}
