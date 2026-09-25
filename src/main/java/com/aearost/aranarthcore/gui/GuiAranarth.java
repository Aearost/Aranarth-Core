package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI for the centralised Aranarth guide.
 */
public class GuiAranarth {

    public static final String TITLE_KEY = "gui.aranarth.title";

    // Slot positions for each topic
    public static final int SLOT_RULES = 4;
    public static final int SLOT_CALENDAR = 11;
    public static final int SLOT_ARANARTHIUM = 13;
    public static final int SLOT_INCANTATIONS = 15;

    private final Player player;
    private final Inventory gui;

    public GuiAranarth(Player player) {
        this.player = player;
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 27, Lang.getFor(player, TITLE_KEY));

        ItemStack grayPane = buildPane(Material.GRAY_STAINED_GLASS_PANE);

        // Top row (outline) - all slots except Rules
        for (int i = 0; i < 9; i++) {
            if (i != SLOT_RULES) {
                inv.setItem(i, grayPane);
            }
        }

        // Middle row - left and right edges only
        inv.setItem(9, grayPane);
        inv.setItem(17, grayPane);

        // Bottom row (outline) - all slots
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, grayPane);
        }

        // Topic items
        inv.setItem(SLOT_RULES, buildItem(
                Material.WRITABLE_BOOK,
                Lang.getFor(player, "gui.aranarth.rules_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.rules_lore1"),
                        Lang.getFor(player, "gui.aranarth.rules_lore2")
                )
        ));
        inv.setItem(SLOT_CALENDAR, buildItem(
                Material.CLOCK,
                Lang.getFor(player, "gui.aranarth.calendar_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.calendar_lore1"),
                        Lang.getFor(player, "gui.aranarth.calendar_lore2")
                )
        ));

        ItemStack ingotItem = new AranarthiumIngot().getItem();
        ItemMeta ingotMeta = ingotItem.getItemMeta();
        ingotMeta.setLore(List.of(
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.aranarthium_lore1")),
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.aranarthium_lore2"))
        ));
        ingotItem.setItemMeta(ingotMeta);
        inv.setItem(SLOT_ARANARTHIUM, ingotItem);

        ItemStack incantationItem = new IncantationPlentiful().getItem();
        ItemMeta incantationMeta = incantationItem.getItemMeta();
        incantationMeta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.incantations_name")));
        incantationMeta.setLore(List.of(
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.incantations_lore1")),
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.incantations_lore2"))
        ));
        incantationItem.setItemMeta(incantationMeta);
        inv.setItem(SLOT_INCANTATIONS, incantationItem);

        return inv;
    }

    private ItemStack buildPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(lore.stream().map(ChatUtils::translateToColor).toList());
        item.setItemMeta(meta);
        return item;
    }
}
