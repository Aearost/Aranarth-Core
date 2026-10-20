package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI showing the dominion's 4 outpost slots.
 */
public class GuiOutposts {

    public static final String TITLE_KEY = "gui.outposts.title";

    private static final int[] OUTPOST_SLOTS = {10, 12, 14, 16};

    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        String title = Lang.getFor(player, TITLE_KEY);
        Inventory gui = GuiHolder.create(player, 27, title);

        ItemStack filler = buildFiller();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());
        int allowedCount = OutpostUtils.allowedOutpostCount(dominion.getDominionLevel());

        for (int i = 0; i < 4; i++) {
            int outpostIndex = i + 1; // 1-based
            Outpost outpost = outposts.stream()
                    .filter(o -> o.getOutpostIndex() == outpostIndex)
                    .findFirst().orElse(null);

            boolean unlocked = outpostIndex <= allowedCount;
            gui.setItem(OUTPOST_SLOTS[i], buildOutpostItem(player, outpostIndex, outpost, unlocked, dominion));
        }

        gui.setItem(22, buildBackButton(player));

        player.closeInventory();
        player.openInventory(gui);
    }

    private static ItemStack buildOutpostItem(Player player, int outpostIndex, Outpost outpost, boolean unlocked, Dominion dominion) {
        if (!unlocked) {
            return buildLockedItem(player, outpostIndex);
        }
        if (outpost == null) {
            return buildEmptySlotItem(player, outpostIndex, dominion);
        }
        return buildActiveOutpostItem(player, outpost, dominion);
    }

    private static ItemStack buildLockedItem(Player player, int outpostIndex) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.outposts.locked", "n", String.valueOf(outpostIndex)));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.outposts_requires_level", "level", outpostIndex + 1));
        lore.add(Lang.getFor(player, "gui.outposts_unlock_cost", "cost", OutpostUtils.getFormattedOutpostCost(outpostIndex)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmptySlotItem(Player player, int outpostIndex, Dominion dominion) {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.outposts.empty", "n", String.valueOf(outpostIndex)));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "dominion.outpost_how_to_create"));
        lore.add(Lang.getFor(player, "gui.outposts_create_cost", "cost", OutpostUtils.getFormattedOutpostCost(outpostIndex)));
        lore.add(Lang.getFor(player, "gui.outposts_chunk_limit", "limit", OutpostUtils.getBaseChunkLimit(outpostIndex)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildActiveOutpostItem(Player player, Outpost outpost, Dominion dominion) {
        ItemStack item = new ItemStack(outpost.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(outpost.getName()));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.outposts.teleport"));
        meta.setLore(lore);
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

    private static ItemStack buildBackButton(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.getFor(player, "gui.back_red"));
        item.setItemMeta(meta);
        return item;
    }

    public static int[] getOutpostSlots() {
        return OUTPOST_SLOTS;
    }
}
