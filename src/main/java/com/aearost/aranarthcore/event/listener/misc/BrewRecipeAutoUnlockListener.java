package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.dre.brewery.Brew;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

import static com.aearost.aranarthcore.objects.CustomKeys.BREW_BREWER;

/**
 * Automatically unlocks a brew recipe in the brew book when they pick up a perfect-quality brew.
 */
public class BrewRecipeAutoUnlockListener implements Listener {

    /** BreweryX quality required to trigger a recipe unlock — 10 means every criterion was met exactly. */
    private static final int PERFECT_QUALITY = 10;

    public BrewRecipeAutoUnlockListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        // We only care about the item the player is interacting with in the clicked inventory
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() != Material.POTION) {
            return;
        }

        // Only the player who originally filled this brew from the cauldron can unlock the recipe
        if (!item.hasItemMeta()) {
            return;
        }
        String brewerStr = item.getItemMeta().getPersistentDataContainer().get(BREW_BREWER, PersistentDataType.STRING);
        if (brewerStr == null || !brewerStr.equals(player.getUniqueId().toString())) {
            return;
        }

        Brew brew = Brew.get(item);
        if (brew == null || !brew.hasRecipe()) {
            return;
        }
        if (brew.getQuality() != PERFECT_QUALITY) {
            return;
        }

        String recipeId = brew.getCurrentRecipe().getId();
        BrewRecipe recipe = BrewRecipe.fromId(recipeId);
        if (recipe == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (BrewRecipeUtils.isMastered(uuid, recipe)) {
            return; // Already mastered — nothing new to do
        }

        boolean wasAlreadyUnlocked = BrewRecipeUtils.isUnlocked(uuid, recipe);
        boolean hasSecret = recipe.getSecretIngredientIndex() >= 0 && recipe.getTier() == BrewRecipe.Tier.LEGENDARY;

        BrewRecipeUtils.master(uuid, recipeId);

        if (!wasAlreadyUnlocked) {
            player.sendMessage(ChatUtils.chatMessage("&6[Brew Book] &fYou've mastered &e" + recipe.getDisplayName()
                    + "&f! The recipe has been added to your brew book."));
        } else if (hasSecret) {
            player.sendMessage(ChatUtils.chatMessage("&6[Brew Book] &fYou've mastered &e" + recipe.getDisplayName()
                    + "&f! The secret ingredient has been revealed in your brew book."));
        }
    }
}
