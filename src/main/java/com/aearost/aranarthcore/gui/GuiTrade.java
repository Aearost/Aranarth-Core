package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Trade;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The GUI used when Trading.
 */
public class GuiTrade {

    private final Player player;
    private final Trade trade;

    public GuiTrade(Player player, Trade trade) {
        this.player = player;
        this.trade = trade;
    }

    public void openGui() {
        player.closeInventory();
        Inventory gui = buildInventory();
        player.openInventory(gui);
    }

    private Inventory buildInventory() {
        AranarthPlayer other = AranarthUtils.getPlayer(trade.getOther(player.getUniqueId()));
        String otherNick = other != null ? other.getNickname() : "Unknown";
        String title = Lang.getFor(player, "gui.trade.title", "player", otherNick);

        Inventory gui = Bukkit.createInventory(player, 54, title);

        // Full gray border row
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, TradeManager.buildBorderItem());
        }

        // Full gray border row
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, TradeManager.buildBorderItem());
        }

        for (int row = 1; row <= 4; row++) {
            gui.setItem(row * 9, TradeManager.buildBorderItem()); // col 0
            gui.setItem(row * 9 + 4, TradeManager.buildBorderItem()); // col 4
            gui.setItem(row * 9 + 8, TradeManager.buildBorderItem()); // col 8
        }

        // Money slots (override border)
        gui.setItem(TradeManager.PAY_LEFT, TradeManager.buildPayItem(player,
                trade.getMyMoney(player.getUniqueId()), true, false, null));
        gui.setItem(TradeManager.PAY_RIGHT, TradeManager.buildPayItem(player,
                trade.getMyMoney(trade.getOther(player.getUniqueId())), false, false, otherNick));

        // Functional buttons (override border)
        gui.setItem(TradeManager.CONFIRM_SLOT, TradeManager.buildConfirmItem(player,
                trade.isMyConfirmed(player.getUniqueId()), false));
        gui.setItem(TradeManager.CANCEL_SLOT, TradeManager.buildCancelAreaItem(player, trade));
        gui.setItem(TradeManager.STATUS_OTHER_SLOT, TradeManager.buildOtherStatusItem(player, trade));

        // Populate my offered items (left side, slots already empty)
        ItemStack[] myItems = trade.getMyItems(player.getUniqueId());
        for (int i = 0; i < 12; i++) {
            if (myItems[i] != null) {
                gui.setItem(TradeManager.leftIndexToGuiSlot(i), myItems[i]);
            }
        }

        // Populate their offered items (right side)
        // Empty slots get a light-gray filler so shift-clicks cannot accidentally land in the read-only area
        ItemStack[] otherItems = trade.getOtherItems(player.getUniqueId());
        for (int i = 0; i < 12; i++) {
            int slot = TradeManager.rightIndexToGuiSlot(i);
            gui.setItem(slot, otherItems[i] != null ? otherItems[i] : TradeManager.buildRightOfferFillerItem());
        }

        return gui;
    }
}
