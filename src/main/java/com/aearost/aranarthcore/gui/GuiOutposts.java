package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
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

    public static final String TITLE = "Dominion Outposts";

    private static final int[] OUTPOST_SLOTS = {10, 12, 14, 16};

    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        String title = ChatUtils.translateToColor(TITLE);
        Inventory gui = Bukkit.createInventory(player, 27, title);

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
            gui.setItem(OUTPOST_SLOTS[i], buildOutpostItem(outpostIndex, outpost, unlocked, dominion));
        }

        gui.setItem(22, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    private static ItemStack buildOutpostItem(int outpostIndex, Outpost outpost, boolean unlocked, Dominion dominion) {
        if (!unlocked) {
            return buildLockedItem(outpostIndex);
        }
        if (outpost == null) {
            return buildEmptySlotItem(outpostIndex, dominion);
        }
        return buildActiveOutpostItem(outpost, dominion);
    }

    private static ItemStack buildLockedItem(int outpostIndex) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&8&lOutpost #" + outpostIndex + " &8&l- Locked"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Requires &eDominion Level " + (outpostIndex + 1)));
        lore.add(ChatUtils.translateToColor("&7Cost to unlock: &6" + OutpostUtils.getFormattedOutpostCost(outpostIndex)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEmptySlotItem(int outpostIndex, Dominion dominion) {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e&lOutpost Slot " + outpostIndex + " &7- Available"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Use &e/d outpost create <name> &7to establish an outpost"));
        lore.add(ChatUtils.translateToColor("&7Cost to create: &6" + OutpostUtils.getFormattedOutpostCost(outpostIndex)));
        lore.add(ChatUtils.translateToColor("&7Base chunk limit: &e" + OutpostUtils.getBaseChunkLimit(outpostIndex)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildActiveOutpostItem(Outpost outpost, Dominion dominion) {
        ItemStack item = new ItemStack(Material.OAK_LOG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e" + ChatUtils.stripColorFormatting(outpost.getName())));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Click to teleport to this outpost"));
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

    private static ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&c&lBack"));
        item.setItemMeta(meta);
        return item;
    }

    public static int[] getOutpostSlots() {
        return OUTPOST_SLOTS;
    }
}
