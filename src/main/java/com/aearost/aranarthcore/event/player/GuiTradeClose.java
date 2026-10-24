package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Trade;
import com.aearost.aranarthcore.utils.TradeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

/**
 * Cancels an active trade when either participant closes the GUI.
 */
public class GuiTradeClose {

    public void execute(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();

        Trade trade = TradeManager.getTrade(uuid);
        if (trade == null) {
            return;
        }

        // If the trade is mid-completion the close is triggered by us - don't cancel it
        if (trade.isCompleting()) {
            return;
        }

        // Also skip if the player is being redirected to the pay-amount chat prompt
        if (TradeManager.isAwaitingPayInput(uuid)) {
            return;
        }

        TradeManager.cancelTrade(trade, uuid);
    }
}
