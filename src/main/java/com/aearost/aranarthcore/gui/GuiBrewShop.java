package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.BrewRecipeUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for purchasing BASIC-tier brewery recipes with in-game money.
 */
public class GuiBrewShop {

    public static final String TITLE_KEY = "gui.brewshop.title";
    private static final int PAGE_SIZE = 36;

    private final Player player;
    private final int page;
    private final Inventory gui;

    public GuiBrewShop(Player player, int page) {
        this.player = player;
        this.page = page;
        this.gui = build();
    }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(player, 54, Lang.getFor(player, TITLE_KEY));

        List<BrewRecipe> locked = BrewRecipeUtils.getLockedCommonRecipes(player.getUniqueId());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, locked.size());

        ItemStack glass = makeGlass();

        // Top row: gray glass panes
        for (int slot = 0; slot < 9; slot++) {
            inv.setItem(slot, glass);
        }

        for (int i = start; i < end; i++) {
            inv.setItem(9 + (i - start), BrewRecipeUtils.createShopPotionDisplay(locked.get(i)));
        }

        ItemStack previous = new ItemStack(Material.RED_WOOL);
        ItemMeta previousMeta = previous.getItemMeta();
        previousMeta.setDisplayName(Lang.getFor(player, "gui.page_prev"));
        previous.setItemMeta(previousMeta);
        inv.setItem(45, page > 0 ? previous : glass);

        inv.setItem(46, glass);
        inv.setItem(47, glass);
        inv.setItem(48, glass);

        // Slot 49: Back to brew book
        ItemStack back = new ItemStack(Material.BOOK);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(Lang.getFor(player, "gui.brewbook.title"));
        List<String> backLore = new ArrayList<>();
        if (locked.isEmpty()) {
            backLore.add(Lang.getFor(player, "gui.brewshop.all_unlocked"));
        } else {
            backLore.add(Lang.getFor(player, "gui.brewshop.available", "n", String.valueOf(locked.size())));
        }
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        inv.setItem(50, glass);
        inv.setItem(51, glass);
        inv.setItem(52, glass);

        ItemStack next = new ItemStack(Material.LIME_WOOL);
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(Lang.getFor(player, "gui.page_next"));
        next.setItemMeta(nextMeta);
        inv.setItem(53, end < locked.size() ? next : glass);

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

    /**
     * Updates an already-open brew shop inventory in-place without closing it.
     */
    public void populateInto(org.bukkit.inventory.Inventory inv) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setCurrentGuiPageNum(page);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        for (int i = 0; i < gui.getSize(); i++) {
            inv.setItem(i, gui.getItem(i));
        }
    }

    public int getPage() {
        return page;
    }
}
