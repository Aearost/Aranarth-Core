package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Shows a GUI for the Defenders system.
 */
public class GuiDefenderAreaSelect {

    public static final String TITLE_KEY = "gui.defenderareaselect.title";

    private static final int BACK_SLOT = 22;

    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());

        Inventory gui = Bukkit.createInventory(player, 27, Lang.getFor(player, TITLE_KEY));
        populate(gui, dominion, outposts);
        gui.setItem(BACK_SLOT, GuiDominionPermissions.buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    private static void populate(Inventory gui, Dominion dominion, List<Outpost> outposts) {
        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(18 + i, filler);
        }

        int domSlot = getDominionSlot(outposts.size());
        gui.setItem(domSlot, buildDominionItem(dominion.getName()));

        for (int i = 0; i < outposts.size(); i++) {
            // domSlot + 1 is the gap; outposts start at domSlot + 2
            gui.setItem(domSlot + 2 + i, buildOutpostItem(outposts.get(i)));
        }
    }

    /**
     * Returns the middle-row slot for the dominion item.
     */
    public static int getDominionSlot(int outpostCount) {
        if (outpostCount == 0) {
            return 13;
        }
        return 9 + (8 - outpostCount) / 2;
    }

    private static ItemStack buildDominionItem(String dominionName) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defenderareaselect.dominion_name", "name", dominionName));
        meta.setLore(List.of(Lang.get("gui.defenderareaselect.dominion_lore")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildOutpostItem(Outpost outpost) {
        ItemStack item = new ItemStack(outpost.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defenderareaselect.outpost_name", "name", outpost.getName()));
        meta.setLore(List.of(Lang.get("gui.defenderareaselect.outpost_lore")));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
