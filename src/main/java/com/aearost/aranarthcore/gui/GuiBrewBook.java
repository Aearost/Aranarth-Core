package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI displaying all brewery recipes the player has unlocked.
 */
public class GuiBrewBook {

    public static final String TITLE = "Brew Book";
    private static final int PAGE_SIZE = 36;
    private static final int CONTENT_START = 9;

    private final Player player;
    private final int page;
    private final Inventory gui;

    public GuiBrewBook(Player player, int page) {
        this.player = player;
        this.page = page;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54, TITLE);

        ItemStack glass = makeGlass();

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }

        List<BrewRecipe> unlocked = BrewRecipeUtils.getUnlockedRecipes(player.getUniqueId());
        int start = page * PAGE_SIZE;
        int end   = Math.min(start + PAGE_SIZE, unlocked.size());

        for (int i = start; i < end; i++) {
            inv.setItem(CONTENT_START + (i - start), BrewRecipeUtils.createPotionDisplay(unlocked.get(i)));
        }

        // Previous page
        ItemStack previous = new ItemStack(Material.RED_WOOL);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
        previous.setItemMeta(previousMeta);
        inv.setItem(45, page > 0 ? previous : glass);

        inv.setItem(46, glass);
        inv.setItem(47, glass);
        inv.setItem(48, glass);

        // Brewing Guide book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName(ChatUtils.translateToColor("&6&lBrewing Guide"));
        List<String> bookLore = new ArrayList<>();
        bookLore.add(ChatUtils.translateToColor("&7" + unlocked.size() + " recipe(s) unlocked"));
        bookLore.add(ChatUtils.translateToColor("&7Click to receive a guide book"));
        bookMeta.setLore(bookLore);
        book.setItemMeta(bookMeta);
        inv.setItem(49, book);

        inv.setItem(50, glass);
        inv.setItem(51, glass);

        // Recipe Shop
        ItemStack shopBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta shopMeta = shopBtn.getItemMeta();
        shopMeta.setDisplayName(ChatUtils.translateToColor("&6&lRecipe Shop"));
        List<String> shopLore = new ArrayList<>();
        shopLore.add(ChatUtils.translateToColor("&7Unlock Basic recipes with money!"));
        shopMeta.setLore(shopLore);
        shopBtn.setItemMeta(shopMeta);
        inv.setItem(52, shopBtn);

        // Next page
        ItemStack next = new ItemStack(Material.LIME_WOOL);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(ChatUtils.translateToColor("&a&lNext"));
        next.setItemMeta(nextMeta);
        inv.setItem(53, end < unlocked.size() ? next : glass);

        return inv;
    }

    private ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        m.setDisplayName(ChatUtils.translateToColor("&f"));
        g.setItemMeta(m);
        return g;
    }

    public void openGui() {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        player.closeInventory();
        player.openInventory(gui);
    }

    public int getPage() { return page; }
}
