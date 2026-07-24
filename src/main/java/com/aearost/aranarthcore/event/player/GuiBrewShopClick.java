package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.gui.GuiBrewShop;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;

/**
 * Handles clicks inside the Brew Recipe Shop GUI.
 */
public class GuiBrewShopClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = e.getSlot();

        // Previous page
        if (slot == 45 && clicked.getType() == Material.RED_WOOL) {
            new GuiBrewShop(player, 0).openGui();
            return;
        }

        // Next page
        if (slot == 53 && clicked.getType() == Material.LIME_WOOL) {
            new GuiBrewShop(player, 0).openGui();
            return;
        }

        // Back to brew book
        if (slot == 49 && clicked.getType() == Material.BOOK) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            new GuiBrewBook(player, 0).openGui();
            return;
        }

        // Recipe purchase
        if (slot < 45 && clicked.getType() == Material.POTION) {
            // Find which recipe this is by matching display name
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;
            String rawName = ChatUtils.stripColorFormatting(clicked.getItemMeta().getDisplayName()).trim();

            BrewRecipe target = null;
            for (BrewRecipe r : BrewRecipe.values()) {
                if (r.getTier() == BrewRecipe.Tier.COMMON && r.getDisplayName().equalsIgnoreCase(rawName)) {
                    target = r;
                    break;
                }
            }
            if (target == null) return;

            // Already unlocked
            if (BrewRecipeUtils.isUnlocked(player.getUniqueId(), target)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou already know this recipe!"));
                return;
            }

            // Check balance
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            if (ap.getBalance() < target.getPrice()) {
                NumberFormat fmt = NumberFormat.getInstance();
                player.sendMessage(ChatUtils.chatMessage("&cYou need &6$" + fmt.format(target.getPrice())
                        + " &cto unlock this recipe! (you have &6$" + fmt.format(ap.getBalance()) + "&c)"));
                return;
            }

            // Deduct and unlock
            NumberFormat fmt = NumberFormat.getInstance();
            ap.setBalance(ap.getBalance() - target.getPrice());
            AranarthUtils.setPlayer(player.getUniqueId(), ap);
            BrewRecipeUtils.unlock(player.getUniqueId(), target.getId());

            player.sendMessage(ChatUtils.chatMessage("&7You've unlocked the recipe for &f&l"
                    + target.getDisplayName() + "&7!"));
            player.sendMessage(ChatUtils.chatMessage("&7Use the &e/brewbook &7to view your unlocked recipes"));

            player.closeInventory();
        }
    }
}
