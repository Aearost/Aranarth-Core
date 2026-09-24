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
 * Shows a GUIto toggle the selected flags (Mob Spawning, Member PvP, Bending, or Explosions).
 */
public class GuiDominionFlagSelect {

    public static final String TITLE_MOB_SPAWNING_KEY = "gui.dominionflagselect.title_mob_spawning";
    public static final String TITLE_PVP_KEY = "gui.dominionflagselect.title_pvp";
    public static final String TITLE_BENDING_KEY = "gui.dominionflagselect.title_bending";
    public static final String TITLE_EXPLOSIONS_KEY = "gui.dominionflagselect.title_explosions";

    private static final int BACK_SLOT = 22;

    public static void openMobSpawning(Player player) {
        open(player, TITLE_MOB_SPAWNING_KEY);
    }

    public static void openMemberPvp(Player player) {
        open(player, TITLE_PVP_KEY);
    }

    public static void openBending(Player player) {
        open(player, TITLE_BENDING_KEY);
    }

    public static void openExplosions(Player player) {
        open(player, TITLE_EXPLOSIONS_KEY);
    }

    private static void open(Player player, String titleKey) {
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) {
            return;
        }

        List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominion.getId());

        Inventory gui = Bukkit.createInventory(player, 27, Lang.getFor(player, titleKey));
        populate(gui, dominion, outposts, titleKey);
        gui.setItem(BACK_SLOT, GuiDominionPermissions.buildBackButton());

        player.closeInventory();
        player.openInventory(gui);
    }

    /**
     * Fills the filler rows and places dominion + outpost items in the middle row.
     */
    private static void populate(Inventory gui, Dominion dominion, List<Outpost> outposts, String titleKey) {
        ItemStack filler = buildFiller();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(18 + i, filler);
        }
        refreshItems(gui, dominion, outposts, titleKey);
    }

    /**
     * Rebuilds all area items in the middle row without touching the filler or back button.
     */
    public static void refreshItems(Inventory gui, Dominion dominion, List<Outpost> outposts, String titleKey) {
        int domSlot = getDominionSlot(outposts.size());

        boolean dominionFlag = getDominionFlag(dominion, titleKey);
        Material dominionMaterial = getMaterialForFlag(titleKey);
        gui.setItem(domSlot, buildAreaItem(dominionMaterial, dominion.getName(), dominionFlag, null));

        for (int i = 0; i < outposts.size(); i++) {
            Outpost outpost = outposts.get(i);
            boolean outpostFlag = getOutpostFlag(outpost, titleKey);
            // domSlot + 1 is left as a gap; outposts start at domSlot + 2
            gui.setItem(domSlot + 2 + i, buildAreaItem(outpost.getIcon(), outpost.getName(), outpostFlag, outpost.getOutpostIndex()));
        }
    }

    /**
     * Returns the middle-row slot that holds the dominion's item, given the outpost count.
     */
    public static int getDominionSlot(int outpostCount) {
        if (outpostCount == 0) {
            return 13; // Center of the 9-slot row
        }
        // Center the whole span in 9 slots
        return 9 + (8 - outpostCount) / 2;
    }

    private static boolean getDominionFlag(Dominion dominion, String titleKey) {
        return switch (titleKey) {
            case TITLE_MOB_SPAWNING_KEY -> dominion.isMobSpawningEnabled();
            case TITLE_PVP_KEY -> dominion.isMemberPvpEnabled();
            case TITLE_BENDING_KEY -> dominion.isBendingEnabled();
            case TITLE_EXPLOSIONS_KEY -> dominion.isExplosionEnabled();
            default -> false;
        };
    }

    private static boolean getOutpostFlag(Outpost outpost, String titleKey) {
        return switch (titleKey) {
            case TITLE_MOB_SPAWNING_KEY -> outpost.isMobSpawningEnabled();
            case TITLE_PVP_KEY -> outpost.isMemberPvpEnabled();
            case TITLE_BENDING_KEY -> outpost.isBendingEnabled();
            case TITLE_EXPLOSIONS_KEY -> outpost.isExplosionEnabled();
            default -> false;
        };
    }

    private static Material getMaterialForFlag(String titleKey) {
        return switch (titleKey) {
            case TITLE_MOB_SPAWNING_KEY -> Material.ZOMBIE_SPAWN_EGG;
            case TITLE_PVP_KEY -> Material.IRON_SWORD;
            case TITLE_BENDING_KEY -> Material.BLAZE_POWDER;
            case TITLE_EXPLOSIONS_KEY -> Material.TNT;
            default -> Material.PAPER;
        };
    }

    /**
     * Builds the toggle item for a single area (dominion or outpost).
     */
    public static ItemStack buildAreaItem(Material material, String areaName, boolean enabled, Integer outpostIndex) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String status = Lang.get(enabled ? "gui.yes" : "gui.no");
        if (outpostIndex == null) {
            meta.setDisplayName(Lang.get("gui.dominionflagselect.area_name_dominion", "name", areaName, "status", status));
            meta.setLore(List.of(Lang.get("gui.dominionflagselect.area_lore_dominion")));
        } else {
            meta.setDisplayName(Lang.get("gui.dominionflagselect.area_name_outpost", "name", areaName, "status", status));
        }
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
