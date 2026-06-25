package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI displaying all Defender types for a Dominion, with purchase and sell options.
 */
public class GuiDefenders {

    public static final String TITLE_PREFIX = "Defenders (";
    private static final int[] DEFENDER_SLOTS = {20, 24};

    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) return;

        int total = DefenderUtils.getTotalDefenderCount(dominion.getId());
        int limit = DefenderUtils.getDefenderLimit(dominion.getDominionLevel());
        String title = ChatUtils.translateToColor(TITLE_PREFIX + total + "/" + limit + ")");
        Inventory gui = Bukkit.createInventory(player, 54, title);

        // Top and bottom border rows
        ItemStack border = buildBorder();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(45 + i, border);
        }

        // Fill middle rows with gray panes
        ItemStack filler = buildFiller();
        for (int slot = 9; slot < 45; slot++) {
            gui.setItem(slot, filler);
        }

        // Defender type icons
        DefenderType[] types = DefenderType.values();
        for (int i = 0; i < types.length && i < DEFENDER_SLOTS.length; i++) {
            gui.setItem(DEFENDER_SLOTS[i], buildDefenderItem(types[i], dominion));
        }

        // Back button in the centre of the bottom border row
        gui.setItem(49, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    private static ItemStack buildDefenderItem(DefenderType type, Dominion dominion) {
        int count = DefenderUtils.getDefenderCount(dominion.getId(), type);
        int total = DefenderUtils.getTotalDefenderCount(dominion.getId());
        int limit = DefenderUtils.getDefenderLimit(dominion.getDominionLevel());
        NumberFormat fmt = NumberFormat.getInstance();

        ItemStack item = new ItemStack(type.getSpawnEgg());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(
                "&c&l" + type.getDisplayName() + " &7(" + count + " active)"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&6Role: &e" + type.getRole()));
        lore.add(ChatUtils.translateToColor("&7Max Health: &e" + (int) (type.getMaxHealth() / 2) + " hearts"));
        lore.add(ChatUtils.translateToColor("&7Damage: &e" + (int) (type.getMinDamage() / 2) + "–" + (int) (type.getMaxDamage() / 2) + " hearts"));
        lore.add("");
        lore.add(ChatUtils.translateToColor(
                "&a▶ Right-Click &7to purchase &e($" + fmt.format((long) type.getPurchasePrice()) + ")"));
        lore.add(ChatUtils.translateToColor(
                "&c◀ Left-Click &7to sell &e($" + fmt.format((long) type.getSellPrice()) + ")"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
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
}
