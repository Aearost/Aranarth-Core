package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiBrewBook;
import com.aearost.aranarthcore.gui.GuiBrewShop;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = e.getSlot();

        // Previous page
        if (slot == 45 && clicked.getType() == Material.RED_WOOL) {
            int currentPage = getCurrentPage(player);
            if (currentPage > 0) {
                new GuiBrewBook(player, currentPage - 1).openGui();
            }
            return;
        }

        // Next page
        if (slot == 53 && clicked.getType() == Material.LIME_WOOL) {
            int currentPage = getCurrentPage(player);
            new GuiBrewBook(player, currentPage + 1).openGui();
            return;
        }

        // Brewing Guide book
        if (slot == 49 && clicked.getType() == Material.BOOK) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            player.closeInventory();
            player.getInventory().addItem(createBrewingGuide());
            player.sendMessage(ChatUtils.chatMessage("&7You have received the &6Brewing Guide"));
            return;
        }

        // Open shop
        if (slot == 52 && clicked.getType() == Material.GOLD_INGOT) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            new GuiBrewShop(player, 0).openGui();
            return;
        }
    }

    private ItemStack createBrewingGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(ChatUtils.translateToColor("&6Brewing Guide"));
        meta.setAuthor("Aranarth");

        meta.addPage(brewingTitle());
        meta.addPage(brewingOverview());
        meta.addPage(brewingCauldron());
        meta.addPage(brewingStand());
        meta.addPage(brewingSmallBarrel());
        meta.addPage(brewingLargeBarrel());
        meta.addPage(brewingQuality());

        book.setItemMeta(meta);
        return book;
    }

    private String brewingTitle() {
        return ChatUtils.translateToColor(
                "\n\n\n\n      &6&lBrewing in\n      Aranarth"
        );
    }

    private String brewingOverview() {
        return ChatUtils.translateToColor(
                "&6&lBrewing Overview&r\n\n" +
                        "The &lBrewery &rplugin lets you brew drinks with unique effects. " +
                        "Most recipes involve up to 3 stages:\n" +
                        "1. &rCauldron\n" +
                        "2. &rBrewing Stand\n" +
                        "3. &rBarrel\n\n" +
                        "Not every recipe uses all 3. Check &o/brewbook&r for recipe details!"
        );
    }

    private String brewingCauldron() {
        return ChatUtils.translateToColor(
                "&6&lStep 1 - Cooking&r\n" +
                        "Place a Cauldron over a heat source &o(fire, campfire, magma block)&r and fill it with water.\n\n" +
                        "Add ingredients by right-clicking, or right-click with a &oClock&r to see the cook time.\n\n" +
                        "Fill up &oGlass Bottles&r by right-clicking the cauldron when done."
        );
    }

    private String brewingStand() {
        return ChatUtils.translateToColor(
                "&6&lStep 2 - Distilling&r\n\n" +
                        "Some recipes require distillation. Place your bottles in a Brewing Stand with Blaze Powder.\n\n" +
                        "The number of passes matters &o- too few or too many will reduce the quality!"
        );
    }

    private String brewingSmallBarrel() {
        return ChatUtils.translateToColor(
                "&6&lStep 3 - Aging&r\n\n" +
                        "Brews are aged by storing them in a barrel.\n\n" +
                        "&lSmall Barrel:&r Arrange 8 Stairs in a barrel shape (any wood). Place a Sign on the lower-right front block and write &o\"Barrel\"&r on line 1 to seal it."
        );
    }

    private String brewingLargeBarrel() {
        return ChatUtils.translateToColor(
                "&6&lStep 3 (cont.)&r\n\n" +
                        "&lLarge Barrel:&r Build a 5-wide, 4-tall frame using Logs, Stairs, and Planks (any wood).\n\n" +
                        "Place a Sign on the lower-right front block with &o\"Barrel\"&r on line 1 to seal it.\n\n" +
                        "Place bottles inside and wait for aging!"
        );
    }

    private String brewingQuality() {
        return ChatUtils.translateToColor(
                "&6&lQuality&r\n\n" +
                        "Brews have a quality from 1 to 10:\n\n" +
                        "&a\u2605\u2605\u2605 &rPerfect (8-10)\n" +
                        "&e\u2605\u2605\u2606 &rGood (5-7)\n" +
                        "&c\u2605\u2606\u2606 &rPoor (1-4)\n\n" +
                        "Quality depends on:\n" +
                        "- Cauldron timing\n" +
                        "- Distillation passes\n" +
                        "- Barrel wood type\n" +
                        "- Aging time in years"
        );
    }

    private int getCurrentPage(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        return aranarthPlayer.getCurrentGuiPageNum();
    }
}
