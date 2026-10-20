package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.gui.GuiBrewShop;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Handles clicks inside the Brew Book GUI.
 */
public class GuiBrewBookClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        // Only the GUI's own buttons count, never the player's inventory
        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = e.getSlot();

        // Search button
        if (slot == 4 && clicked.getType() == Material.SPYGLASS) {
            GuiBrewBook.initiateSearch(player);
            return;
        }

        // Back button (from search results)
        if (slot == 4 && clicked.getType() == Material.ARROW) {
            GuiBrewBook.clearFilter(player.getUniqueId());
            new GuiBrewBook(player, 0).populateInto(e.getView().getTopInventory());
            return;
        }

        // Previous page
        if (slot == 45 && clicked.getType() == Material.RED_WOOL) {
            int currentPage = getCurrentPage(player);
            if (currentPage > 0) {
                String filter = GuiBrewBook.getActiveFilter(player.getUniqueId());
                new GuiBrewBook(player, currentPage - 1, filter).populateInto(e.getView().getTopInventory());
            }
            return;
        }

        // Next page
        if (slot == 53 && clicked.getType() == Material.LIME_WOOL) {
            int currentPage = getCurrentPage(player);
            String filter = GuiBrewBook.getActiveFilter(player.getUniqueId());
            new GuiBrewBook(player, currentPage + 1, filter).populateInto(e.getView().getTopInventory());
            return;
        }

        // Brewing Guide book
        if (slot == 49 && clicked.getType() == Material.BOOK) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            player.closeInventory();
            player.getInventory().addItem(createBrewingGuide(player));
            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "brew.guide_received")));
            return;
        }

        // Open shop
        if (slot == 52 && clicked.getType() == Material.GOLD_INGOT) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            new GuiBrewShop(player, 0).openGui();
            return;
        }
    }

    private ItemStack createBrewingGuide(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(Lang.getFor(player, "book.brew_title_name"));
        meta.setAuthor("Aranarth");

        meta.addPage(Lang.getFor(player, "book.brew_title"));
        meta.addPage(Lang.getFor(player, "book.brew_overview"));
        meta.addPage(Lang.getFor(player, "book.brew_cauldron"));
        meta.addPage(Lang.getFor(player, "book.brew_stand"));
        meta.addPage(Lang.getFor(player, "book.brew_small_barrel"));
        meta.addPage(Lang.getFor(player, "book.brew_large_barrel"));
        meta.addPage(Lang.getFor(player, "book.brew_quality"));

        book.setItemMeta(meta);
        return book;
    }

    private int getCurrentPage(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        return aranarthPlayer.getCurrentGuiPageNum();
    }
}
