package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Trade;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.UUID;

/**
 * Handles all click interactions inside the trade GUI.
 */
public class GuiTradeClick {

    public void execute(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (e.getClickedInventory() == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Trade trade = TradeManager.getTrade(uuid);
        if (trade == null) {
            return;
        }

        int slot = e.getRawSlot();

        // During the 3-second completion countdown, only the cancel slot may be clicked
        if (trade.isCompleting()) {
            boolean inTop = e.getClickedInventory().getType() != InventoryType.PLAYER;
            if (!inTop || slot != TradeManager.CANCEL_SLOT) {
                e.setCancelled(true);
                return;
            }
        }

        // Click outside the GUI window entirely - ignore
        if (slot < 0) {
            e.setCancelled(true);
            return;
        }

        // Click in the player's own bottom inventory
        boolean clickedBottom = e.getClickedInventory().getType() == InventoryType.PLAYER;
        if (clickedBottom) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Reset confirmations, then sync after event resolves
                if (trade.isMyConfirmed(uuid) || trade.isMyConfirmed(trade.getOther(uuid))) {
                    trade.resetConfirmations();
                    TradeManager.cancelCompletion(trade);
                    trade.setCompleting(false);
                    TradeManager.updateConfirmButtons(trade);
                    publishConfirmReset(trade, uuid);
                }
                scheduleSync(trade, uuid);
            }
            return;
        }

        // Clicks inside the top (trade) inventory

        // Right-side offer area (read-only - the other player's items)
        if (TradeManager.isRightOfferSlot(slot)) {
            e.setCancelled(true);
            return;
        }

        int row = slot / 9;
        int col = slot % 9;

        // Top border row (includes money slots)
        if (row == 0) {
            e.setCancelled(true);
            if (slot == TradeManager.PAY_LEFT) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
                TradeManager.setAwaitingPayInput(uuid);
                player.closeInventory();
                player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "trade.pay_prompt")));
            }
            return;
        }

        // Bottom button row
        if (row == 5) {
            e.setCancelled(true);
            if (slot == TradeManager.CANCEL_SLOT) {
                handleCancelSlot(player, uuid, trade);
            } else if (slot == TradeManager.CONFIRM_SLOT) {
                handleConfirmSlot(player, uuid, trade);
            }
            // STATUS_OTHER_SLOT and remaining border slots - display-only
            return;
        }

        if (col == 0 || col == 4 || col == 8) {
            e.setCancelled(true);
            return;
        }

        // Left offer area - player edits their own items
        if (TradeManager.isLeftOfferSlot(slot)) {
            // Reset confirmations when any offer changes
            if (trade.areBothConfirmed() || trade.isMyConfirmed(uuid) || trade.isMyConfirmed(trade.getOther(uuid))) {
                trade.resetConfirmations();
                TradeManager.cancelCompletion(trade);
                trade.setCompleting(false);
                TradeManager.updateConfirmButtons(trade);
                publishConfirmReset(trade, uuid);
            }
            // Allow the click - sync after the event resolves
            scheduleSync(trade, uuid);
        }
    }

    /**
     * Handles a click on CANCEL_SLOT (slot 49).
     */
    private void handleCancelSlot(Player player, UUID uuid, Trade trade) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);

        if (trade.isCompleting() || trade.areBothConfirmed()) {
            // Redstone block - reset both back to edit mode
            trade.resetConfirmations();
            TradeManager.cancelCompletion(trade);
            trade.setCompleting(false);
            TradeManager.updateConfirmButtons(trade);
            if (TradeManager.isCrossServer(trade) && NetworkManager.isActive()) {
                UUID initiatorUuid = trade.getInitiatorUuid();
                UUID otherUuid = trade.getOther(uuid);
                NetworkManager.getInstance().publishTradeConfirm(uuid, initiatorUuid, false);
                NetworkManager.getInstance().publishTradeConfirm(otherUuid, initiatorUuid, false);
            }
        } else if (trade.isMyConfirmed(uuid)) {
            // Red dye - unconfirm only me
            trade.setMyConfirmed(uuid, false);
            TradeManager.updateConfirmButtons(trade);
            if (TradeManager.isCrossServer(trade) && NetworkManager.isActive()) {
                NetworkManager.getInstance().publishTradeConfirm(uuid, trade.getInitiatorUuid(), false);
            }
        } else {
            // Barrier - cancel the trade entirely
            TradeManager.cancelTrade(trade, uuid);
        }
    }

    /**
     * Handles a click on CONFIRM_SLOT (slot 47).
     */
    private void handleConfirmSlot(Player player, UUID uuid, Trade trade) {
        boolean nowConfirmed = !trade.isMyConfirmed(uuid);

        // Validate balance before allowing the confirm
        if (nowConfirmed) {
            double myMoney = trade.getMyMoney(uuid);
            if (myMoney > 0) {
                AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
                if (ap == null || ap.getBalance() < myMoney) {
                    player.sendMessage(ChatUtils.chatMessage(
                            Lang.getFor(player, "trade.not_enough_money")));
                    return;
                }
            }
        }

        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
        trade.setMyConfirmed(uuid, nowConfirmed);
        TradeManager.updateConfirmButtons(trade);

        if (TradeManager.isCrossServer(trade) && NetworkManager.isActive()) {
            NetworkManager.getInstance().publishTradeConfirm(uuid, trade.getInitiatorUuid(), nowConfirmed);
        }

        if (trade.areBothConfirmed()) {
            trade.setCompleting(true);
            TradeManager.updateConfirmButtons(trade);
            // Only the initiator's server actually runs the countdown
            boolean initiatorIsLocal = Bukkit.getPlayer(trade.getInitiatorUuid()) != null;
            if (!TradeManager.isCrossServer(trade) || initiatorIsLocal) {
                TradeManager.scheduleCompletion(trade);
            }
        }
    }

    /**
     * Publishes a confirm=false to the remote server for the local player only.
     */
    private void publishConfirmReset(Trade trade, UUID uuid) {
        if (TradeManager.isCrossServer(trade) && NetworkManager.isActive()) {
            NetworkManager.getInstance().publishTradeConfirm(uuid, trade.getInitiatorUuid(), false);
        }
    }

    /**
     * Schedules a sync of the left-offer slots to the other player's right side, one tick later.
     */
    private void scheduleSync(Trade trade, UUID uuid) {
        com.aearost.aranarthcore.AranarthCore plugin = com.aearost.aranarthcore.AranarthCore.getInstance();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (!TradeManager.isTradeGuiOpen(p)) {
                return;
            }
            TradeManager.syncLeftSlots(p, trade);
            TradeManager.updateOtherRightSlots(trade, uuid);
        });
    }
}
