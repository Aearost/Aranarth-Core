package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.dre.brewery.Brew;
import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.recipe.BRecipe;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.alchemy.AlchemyManager;
import com.gmail.nossr50.util.EventUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static com.aearost.aranarthcore.objects.CustomKeys.BREW_ALCHEMY_XP;
import static com.aearost.aranarthcore.objects.CustomKeys.BREW_BREWER;

/**
 * Awards mcMMO Alchemy experience when a player first obtains a finished brew that they filled themselves.
 */
public class BrewAlchemyXpListener implements Listener {

    private static final int PERFECT_QUALITY = 10;
    private static final int COMMON_XP = 225;
    private static final int RARE_XP = 450;
    private static final int LEGENDARY_XP = 900;

    public BrewAlchemyXpListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!isPotion(e.getCurrentItem()) && !isPotion(e.getCursor())) {
            return;
        }
        // Scan one tick later so the brew has been moved into the player's possession
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> awardHeldBrews(player), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }
        if (!isPotion(e.getItem().getItemStack())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> awardHeldBrews(player), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewFill(BrewModifyEvent e) {
        if (e.getType() != BrewModifyEvent.Type.FILL) {
            return;
        }
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }
        // Cooking-only recipes are finished as soon as they are filled into a bottle
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> awardHeldBrews(player), 1L);
    }

    private boolean isPotion(ItemStack item) {
        return item != null && item.getType() == Material.POTION;
    }

    /**
     * Awards Alchemy experience for every eligible brew in the player's inventory and on their cursor.
     * @param player The player.
     */
    private void awardHeldBrews(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
            return;
        }
        // mcMMO discards all experience in creative - leave the brews untagged so they can award later
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (tryAward(player, item)) {
                inventory.setItem(slot, item);
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (tryAward(player, cursor)) {
            player.setItemOnCursor(cursor);
        }
    }

    /**
     * Awards Alchemy experience for the brew if it is finished, was filled by the player, and has not yet awarded experience.
     * @param player The player.
     * @param item The item being verified.
     * @return Whether experience was awarded and the item was tagged.
     */
    private boolean tryAward(Player player, ItemStack item) {
        if (!isPotion(item) || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(BREW_ALCHEMY_XP, PersistentDataType.BYTE)) {
            return false;
        }

        // Only the player who originally filled this brew from the cauldron receives experience
        String brewerStr = pdc.get(BREW_BREWER, PersistentDataType.STRING);
        if (brewerStr == null || !brewerStr.equals(player.getUniqueId().toString())) {
            return false;
        }

        Brew brew = Brew.get(item);
        if (brew == null || !brew.hasRecipe() || brew.getQuality() <= 0) {
            return false;
        }

        BRecipe bRecipe = brew.getCurrentRecipe();
        if (bRecipe.needsDistilling() && brew.getDistillRuns() <= 0) {
            return false;
        }
        if (bRecipe.needsToAge() && brew.getAgeTime() < 1) {
            return false;
        }

        BrewRecipe recipe = BrewRecipe.fromId(bRecipe.getId());
        int baseXp = COMMON_XP;
        if (recipe != null) {
            baseXp = switch (recipe.getTier()) {
                case COMMON -> COMMON_XP;
                case RARE -> RARE_XP;
                case LEGENDARY -> LEGENDARY_XP;
            };
        }
        int quality = Math.min(brew.getQuality(), PERFECT_QUALITY);
        int xp = Math.max(1, baseXp * quality / PERFECT_QUALITY);

        // mcMMO profile not loaded yet - leave the brew untagged so it can award later
        McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
        if (mcMMOPlayer == null) {
            return false;
        }

        new AlchemyManager(mcMMOPlayer).applyXpGain(xp, XPGainReason.PVE, XPGainSource.CUSTOM);

        pdc.set(BREW_ALCHEMY_XP, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return true;
    }
}
