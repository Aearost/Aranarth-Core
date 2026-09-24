package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.*;

/**
 * GUI displaying all Defender types for a Dominion, with purchase and sell options.
 */
public class GuiDefenders {

    public static final String TITLE_PREFIX_KEY = "gui.defenders.title";
    private static final int[] DEFENDER_SLOTS = {11, 13, 15, 20, 22, 24, 29, 31, 33};

    private static final Map<UUID, UUID> playerOutpostContext = new HashMap<>();

    /**
     * Opens the defenders GUI with the given area context.
     */
    public static void open(Player player, Outpost assignTo) {
        if (assignTo != null) {
            playerOutpostContext.put(player.getUniqueId(), assignTo.getId());
        } else {
            playerOutpostContext.remove(player.getUniqueId());
        }
        open(player);
    }

    /**
     * Returns the outpost that was last selected.
     */
    public static Outpost getContext(UUID playerId) {
        UUID outpostId = playerOutpostContext.get(playerId);
        return outpostId != null ? OutpostUtils.getOutpostById(outpostId) : null;
    }

    public static void open(Player player) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        int total = DefenderUtils.getTotalDefenderCount(dominion.getId());
        int limit = DefenderUtils.getDefenderLimit(dominion.getDominionLevel());
        String title = Lang.getFor(player, TITLE_PREFIX_KEY, "total", String.valueOf(total), "limit", String.valueOf(limit));
        Inventory gui = Bukkit.createInventory(player, 45, title);

        // Top and bottom border rows
        ItemStack border = buildBorder();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(36 + i, border);
        }

        // Fill middle rows with gray panes
        ItemStack filler = buildFiller();
        for (int slot = 9; slot < 36; slot++) {
            gui.setItem(slot, filler);
        }

        // Defender type icons
        DefenderType[] types = DefenderType.values();
        for (int i = 0; i < types.length && i < DEFENDER_SLOTS.length; i++) {
            gui.setItem(DEFENDER_SLOTS[i], buildDefenderItem(types[i], dominion));
        }

        // Back button in the centre of the bottom border row
        gui.setItem(40, buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Updates only the defender-type item slots in an already-open inventory.
     */
    public static void populate(Inventory inv, Dominion dominion) {
        DefenderType[] types = DefenderType.values();
        for (int i = 0; i < types.length && i < DEFENDER_SLOTS.length; i++) {
            inv.setItem(DEFENDER_SLOTS[i], buildDefenderItem(types[i], dominion));
        }
    }

    private static ItemStack buildDefenderItem(DefenderType type, Dominion dominion) {
        int count = DefenderUtils.getDefenderCount(dominion.getId(), type);
        int total = DefenderUtils.getTotalDefenderCount(dominion.getId());
        int limit = DefenderUtils.getDefenderLimit(dominion.getDominionLevel());
        NumberFormat fmt = NumberFormat.getInstance();

        ItemStack item = new ItemStack(type.getSpawnEgg());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defenders.type_count", "type", type.getDisplayName(), "count", String.valueOf(count)));

        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defenders.role", "role", type.getRole()));
        lore.add(Lang.get("gui.defenders.max_hp", "hp", String.valueOf((int) (type.getMaxHealth() / 2))));
        lore.add(Lang.get("gui.defenders.damage", "min", String.valueOf((int) (type.getMinDamage() / 2)), "max", String.valueOf((int) (type.getMaxDamage() / 2))));
        if (type.getSpecialAbility() != null) {
            lore.add(Lang.get("gui.defenders.special", "ability", type.getSpecialAbility()));
        }
        if (type.getPerRankLimit() > 0) {
            int typeCount = DefenderUtils.getDefenderCount(dominion.getId(), type);
            int typeMax = type.getPerRankLimit() * dominion.getDominionLevel();
            lore.add(Lang.get("gui.defenders.type_limit", "count", String.valueOf(typeCount), "max", String.valueOf(typeMax)));
        }
        lore.add("");
        lore.add(Lang.get("gui.defenders.buy", "price", fmt.format((long) type.getPurchasePrice())));
        lore.add(Lang.get("gui.defenders.sell", "price", fmt.format((long) type.getSellPrice())));
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
        meta.setDisplayName(Lang.get("gui.back_red"));
        item.setItemMeta(meta);
        return item;
    }
}
