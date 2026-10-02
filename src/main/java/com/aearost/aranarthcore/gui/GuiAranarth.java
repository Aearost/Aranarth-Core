package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.essence.EssencePlentiful;
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

    public static final int SLOT_RULES = 10;
    public static final int SLOT_BENDING = 12;
    public static final int SLOT_DOMINIONS = 14;
    public static final int SLOT_RANKS = 16;
    public static final int SLOT_ECONOMY = 19;
    public static final int SLOT_CALENDAR = 21;
    public static final int SLOT_ARANARTHIUM = 23;
    public static final int SLOT_ESSENCES = 25;
    public static final int SLOT_PERKS = 29;
    public static final int SLOT_MECHANICS = 31;
    public static final int SLOT_RECIPES = 33;

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
        Inventory inv = Bukkit.createInventory(player, 45, Lang.getFor(player, TITLE_KEY));

        ItemStack grayPane = buildPane(Material.GRAY_STAINED_GLASS_PANE);

        // Fill all slots with gray panes
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, grayPane);
        }

        // Row 1 items
        inv.setItem(SLOT_RULES, buildItem(
                Material.WRITABLE_BOOK,
                Lang.getFor(player, "gui.aranarth.rules_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.rules_lore1"),
                        Lang.getFor(player, "gui.aranarth.rules_lore2")
                )
        ));
        inv.setItem(SLOT_BENDING, buildItem(
                Material.BLAZE_POWDER,
                Lang.getFor(player, "gui.aranarth.bending_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.bending_lore1"),
                        Lang.getFor(player, "gui.aranarth.bending_lore2")
                )
        ));
        inv.setItem(SLOT_DOMINIONS, buildItem(
                Material.WHITE_BANNER,
                Lang.getFor(player, "gui.aranarth.dominions_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.dominions_lore1"),
                        Lang.getFor(player, "gui.aranarth.dominions_lore2")
                )
        ));
        inv.setItem(SLOT_RANKS, buildItem(
                Material.GOLDEN_HELMET,
                Lang.getFor(player, "gui.aranarth.ranks_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.ranks_lore1"),
                        Lang.getFor(player, "gui.aranarth.ranks_lore2")
                )
        ));

        // Row 2 items
        inv.setItem(SLOT_ECONOMY, buildItem(
                Material.GOLD_INGOT,
                Lang.getFor(player, "gui.aranarth.economy_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.economy_lore1"),
                        Lang.getFor(player, "gui.aranarth.economy_lore2")
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

        ItemStack essenceItem = new EssencePlentiful().getItem();
        ItemMeta essenceMeta = essenceItem.getItemMeta();
        essenceMeta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.essences_name")));
        essenceMeta.setLore(List.of(
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.essences_lore1")),
                ChatUtils.translateToColor(Lang.getFor(player, "gui.aranarth.essences_lore2"))
        ));
        essenceItem.setItemMeta(essenceMeta);
        inv.setItem(SLOT_ESSENCES, essenceItem);

        // Row 3 items
        inv.setItem(SLOT_PERKS, buildItem(
                Material.NETHER_STAR,
                Lang.getFor(player, "gui.aranarth.perks_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.perks_lore1"),
                        Lang.getFor(player, "gui.aranarth.perks_lore2")
                )
        ));
        inv.setItem(SLOT_MECHANICS, buildItem(
                Material.PISTON,
                Lang.getFor(player, "gui.aranarth.mechanics_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.mechanics_lore1"),
                        Lang.getFor(player, "gui.aranarth.mechanics_lore2")
                )
        ));
        inv.setItem(SLOT_RECIPES, buildItem(
                Material.KNOWLEDGE_BOOK,
                Lang.getFor(player, "gui.aranarth.recipes_name"),
                List.of(
                        Lang.getFor(player, "gui.aranarth.recipes_lore1"),
                        Lang.getFor(player, "gui.aranarth.recipes_lore2")
                )
        ));

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
