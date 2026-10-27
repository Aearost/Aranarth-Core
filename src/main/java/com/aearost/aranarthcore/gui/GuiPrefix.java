package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.PrefixStyle;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Allows the player to cycle how their council, saint, and rank prefixes are displayed.
 */
public class GuiPrefix {

    public static final String[] TYPES = {"council", "saint", "rank"};
    public static final int[] TYPE_SLOTS = {11, 13, 15};
    public static final int EXIT_SLOT = 22;

    private final Player player;
    private final Inventory initializedGui;

    public GuiPrefix(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    private Inventory initializeGui(Player player) {
        Inventory gui = GuiHolder.create(player, 27, Lang.getFor(player, "gui.prefix.title"));

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, blank);
        }

        for (int type = 0; type < TYPES.length; type++) {
            gui.setItem(TYPE_SLOTS[type], buildTypeItem(player, type));
        }

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(EXIT_SLOT, exit);

        return gui;
    }

    /**
     * @param aranarthPlayer The AranarthPlayer that is being analyzed.
     * @param type           The index of the type in TYPES.
     * @return The player's current style for the type.
     */
    public static PrefixStyle getStyle(AranarthPlayer aranarthPlayer, int type) {
        return switch (type) {
            case 0 -> aranarthPlayer.getCouncilPrefixStyle();
            case 1 -> aranarthPlayer.getSaintPrefixStyle();
            default -> aranarthPlayer.getRankPrefixStyle();
        };
    }

    /**
     * Builds the button of a prefix type, showing its current style and a preview of it.
     *
     * @param player The player viewing the GUI.
     * @param type   The index of the type in TYPES.
     * @return The button of the prefix type.
     */
    public static ItemStack buildTypeItem(Player player, int type) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        PrefixStyle style = getStyle(aranarthPlayer, type);
        // Players without a Council or Saint rank are shown the first tier as an example
        String example = switch (type) {
            case 0 -> AranarthUtils.getCouncilPrefix(Math.max(1, aranarthPlayer.getCouncilRank()), style);
            case 1 -> AranarthUtils.getSaintPrefix(Math.max(1, aranarthPlayer.getSaintRank()), style);
            default -> AranarthUtils.getRankPrefix(aranarthPlayer.getRank(), aranarthPlayer.getPronouns() != Pronouns.MALE, style);
        };

        ItemStack item = switch (type) {
            case 0 -> new ItemStack(Material.NETHER_STAR);
            case 1 -> new AranarthiumIngot().getItem();
            default -> new ItemStack(Material.DIAMOND);
        };
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(Lang.getFor(player, "gui.prefix." + TYPES[type])));
        meta.setLore(List.of(
                ChatUtils.translateToColor(Lang.getFor(player, "gui.prefix.style",
                        "style", Lang.getFor(player, "gui.prefix." + style.name().toLowerCase()))),
                ChatUtils.translateToColor(Lang.getFor(player, "prefix.preview", "preview", example)),
                ChatUtils.translateToColor(Lang.getFor(player, "gui.prefix.cycle"))));
        item.setItemMeta(meta);
        return item;
    }

}
