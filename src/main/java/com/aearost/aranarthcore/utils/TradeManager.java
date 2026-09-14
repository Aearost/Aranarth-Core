package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Trade;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.text.NumberFormat;
import java.util.*;

/**
 * Manages pending trade invites, active trades, and trade lifecycle tasks.
 */
public class TradeManager {

    public static final int PAY_LEFT = 2;  // My money
    public static final int PAY_RIGHT = 6;  // Their money
    public static final int CONFIRM_SLOT = 47;
    public static final int CANCEL_SLOT = 49;
    public static final int STATUS_OTHER_SLOT = 51;

    private static final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private static final Map<UUID, UUID> pendingCrossServerInvites = new HashMap<>();
    private static final Map<UUID, Trade> activeTrades = new HashMap<>();
    private static final Set<UUID> crossServerTrades = new HashSet<>();
    private static final Set<UUID> awaitingPayInput = new HashSet<>();
    private static final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private static final Map<UUID, Boolean> animationToggle = new HashMap<>();
    private static final Map<UUID, List<BukkitTask>> completionTasks = new HashMap<>();

    private TradeManager() {
    }


    public static void addInvite(UUID invitee, UUID inviter) {
        pendingInvites.put(invitee, inviter);
    }

    public static UUID getInviter(UUID invitee) {
        return pendingInvites.get(invitee);
    }

    public static boolean hasInviteFrom(UUID invitee, UUID inviter) {
        return inviter.equals(pendingInvites.get(invitee));
    }

    public static void removeInvite(UUID invitee) {
        pendingInvites.remove(invitee);
    }

    public static void addCrossServerInvite(UUID invitee, UUID inviter) {
        pendingCrossServerInvites.put(invitee, inviter);
    }

    public static boolean hasCrossServerInviteFrom(UUID invitee, UUID inviter) {
        return inviter.equals(pendingCrossServerInvites.get(invitee));
    }

    public static void removeCrossServerInvite(UUID invitee) {
        pendingCrossServerInvites.remove(invitee);
    }

    public static boolean isInTrade(UUID uuid) {
        return activeTrades.containsKey(uuid);
    }

    public static Trade getTrade(UUID uuid) {
        return activeTrades.get(uuid);
    }

    public static void registerTrade(Trade trade) {
        activeTrades.put(trade.getInitiatorUuid(), trade);
        activeTrades.put(trade.getTargetUuid(), trade);
    }

    /**
     * Removes trade from all maps and stops associated tasks.
     */
    public static void unregisterTrade(Trade trade) {
        activeTrades.remove(trade.getInitiatorUuid());
        activeTrades.remove(trade.getTargetUuid());
        crossServerTrades.remove(trade.getInitiatorUuid());
        stopAnimation(trade);
        cancelCompletion(trade);
    }

    /**
     * Marks this trade as spanning two different servers.
     */
    public static void markCrossServer(Trade trade) {
        crossServerTrades.add(trade.getInitiatorUuid());
    }

    /**
     * Returns true if this trade involves a player on a different server.
     */
    public static boolean isCrossServer(Trade trade) {
        return crossServerTrades.contains(trade.getInitiatorUuid());
    }

    public static void setAwaitingPayInput(UUID uuid) {
        awaitingPayInput.add(uuid);
    }

    public static boolean isAwaitingPayInput(UUID uuid) {
        return awaitingPayInput.contains(uuid);
    }

    public static void clearAwaitingPayInput(UUID uuid) {
        awaitingPayInput.remove(uuid);
    }

    public static void startAnimation(Trade trade) {
        UUID key = trade.getInitiatorUuid();
        if (animationTasks.containsKey(key)) {
            return;
        }
        animationToggle.put(key, false);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(AranarthCore.getInstance(), () -> {
            boolean toggle = animationToggle.getOrDefault(key, false);
            animationToggle.put(key, !toggle);
            refreshPaySlots(trade, toggle);
        }, 0L, 10L);
        animationTasks.put(key, task);
    }

    public static void stopAnimation(Trade trade) {
        UUID key = trade.getInitiatorUuid();
        BukkitTask task = animationTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
        animationToggle.remove(key);
    }

    private static void refreshPaySlots(Trade trade, boolean showFilled) {
        AranarthPlayer initiatorAp = AranarthUtils.getPlayer(trade.getInitiatorUuid());
        AranarthPlayer targetAp = AranarthUtils.getPlayer(trade.getTargetUuid());
        String initiatorNick = initiatorAp != null ? initiatorAp.getNickname() : "Unknown";
        String targetNick = targetAp != null ? targetAp.getNickname() : "Unknown";

        Player initiator = Bukkit.getPlayer(trade.getInitiatorUuid());
        if (isTradeGuiOpen(initiator)) {
            Inventory inv = initiator.getOpenInventory().getTopInventory();
            inv.setItem(PAY_LEFT, buildPayItem(initiator, trade.getMyMoney(trade.getInitiatorUuid()), true, showFilled, null));
            inv.setItem(PAY_RIGHT, buildPayItem(initiator, trade.getMyMoney(trade.getTargetUuid()), false, showFilled, targetNick));
        }
        Player target = Bukkit.getPlayer(trade.getTargetUuid());
        if (isTradeGuiOpen(target)) {
            Inventory inv = target.getOpenInventory().getTopInventory();
            inv.setItem(PAY_LEFT, buildPayItem(target, trade.getMyMoney(trade.getTargetUuid()), true, showFilled, null));
            inv.setItem(PAY_RIGHT, buildPayItem(target, trade.getMyMoney(trade.getInitiatorUuid()), false, showFilled, initiatorNick));
        }
    }

    /**
     * Reads the 12 left-offer GUI slots into trade.getMyItems(player).
     */
    public static void syncLeftSlots(Player player, Trade trade) {
        if (!isTradeGuiOpen(player)) {
            return;
        }
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack[] myItems = trade.getMyItems(player.getUniqueId());
        for (int i = 0; i < 12; i++) {
            ItemStack item = inv.getItem(leftIndexToGuiSlot(i));
            myItems[i] = (item != null && !item.getType().isAir()) ? item.clone() : null;
        }
    }

    /**
     * Pushes changedPlayer's current offer into the other player's right-side GUI slots.
     */
    public static void updateOtherRightSlots(Trade trade, UUID changedPlayer) {
        UUID otherUuid = trade.getOther(changedPlayer);
        Player other = Bukkit.getPlayer(otherUuid);
        ItemStack[] changedItems = trade.getMyItems(changedPlayer);

        if (isTradeGuiOpen(other)) {
            // Same-server: update the other player's right GUI directly
            Inventory inv = other.getOpenInventory().getTopInventory();
            for (int i = 0; i < 12; i++) {
                inv.setItem(rightIndexToGuiSlot(i), changedItems[i] != null ? changedItems[i] : buildRightOfferFillerItem());
            }
        } else if (isCrossServer(trade) && NetworkManager.isActive()) {
            // Cross-server: publish item update to the other server
            NetworkManager.getInstance().publishTradeItems(changedPlayer, trade.getInitiatorUuid(), changedItems);
        }
    }

    /**
     * Updates confirm, cancel/modify, and other-status slots for all local participants.
     */
    public static void updateConfirmButtons(Trade trade) {
        Player initiator = Bukkit.getPlayer(trade.getInitiatorUuid());
        Player target = Bukkit.getPlayer(trade.getTargetUuid());
        if (isTradeGuiOpen(initiator)) {
            Inventory inv = initiator.getOpenInventory().getTopInventory();
            inv.setItem(CONFIRM_SLOT, buildConfirmItem(initiator,
                    trade.isMyConfirmed(trade.getInitiatorUuid()), trade.isCompleting()));
            inv.setItem(CANCEL_SLOT, buildCancelAreaItem(initiator, trade));
            inv.setItem(STATUS_OTHER_SLOT, buildOtherStatusItem(initiator, trade));
        }
        if (isTradeGuiOpen(target)) {
            Inventory inv = target.getOpenInventory().getTopInventory();
            inv.setItem(CONFIRM_SLOT, buildConfirmItem(target,
                    trade.isMyConfirmed(trade.getTargetUuid()), trade.isCompleting()));
            inv.setItem(CANCEL_SLOT, buildCancelAreaItem(target, trade));
            inv.setItem(STATUS_OTHER_SLOT, buildOtherStatusItem(target, trade));
        }
    }

    /**
     * Schedules the 3-second completion countdown.
     */
    public static void scheduleCompletion(Trade trade) {
        cancelCompletion(trade);
        UUID key = trade.getInitiatorUuid();
        List<BukkitTask> tasks = Arrays.asList(
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                        () -> playCompletionPing(trade)),
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                    playCompletionPing(trade);
                    setCompletionAnimationSlots(trade, Material.YELLOW_TERRACOTTA);
                }, 20L),
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
                    playCompletionPing(trade);
                    setCompletionAnimationSlots(trade, Material.RED_TERRACOTTA);
                }, 40L),
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                        () -> executeTrade(trade), 60L)
        );
        completionTasks.put(key, tasks);
    }

    /**
     * Schedules the visual countdown animation on the non-initiator's server (cross-server trades).
     */
    public static void scheduleCompletionAnimation(Trade trade) {
        cancelCompletion(trade);
        UUID key = trade.getInitiatorUuid();
        List<BukkitTask> tasks = Arrays.asList(
                Bukkit.getScheduler().runTask(AranarthCore.getInstance(),
                        () -> playCompletionPing(trade)),
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                        () -> setCompletionAnimationSlots(trade, Material.YELLOW_TERRACOTTA), 20L),
                Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                        () -> setCompletionAnimationSlots(trade, Material.RED_TERRACOTTA), 40L)
        );
        completionTasks.put(key, tasks);
    }

    /**
     * Swaps slots 47 and 51 to the given terracotta material for all local participants.
     */
    private static void setCompletionAnimationSlots(Trade trade, Material material) {
        for (UUID uuid : List.of(trade.getInitiatorUuid(), trade.getTargetUuid())) {
            Player p = Bukkit.getPlayer(uuid);
            if (!isTradeGuiOpen(p)) {
                continue;
            }
            Inventory inv = p.getOpenInventory().getTopInventory();
            ItemStack confirm = inv.getItem(CONFIRM_SLOT);
            if (confirm != null && confirm.getType() != Material.AIR) {
                confirm.setType(material);
                inv.setItem(CONFIRM_SLOT, confirm);
            }
            ItemStack status = inv.getItem(STATUS_OTHER_SLOT);
            if (status != null && status.getType() != Material.AIR) {
                status.setType(material);
                inv.setItem(STATUS_OTHER_SLOT, status);
            }
        }
    }

    public static void cancelCompletion(Trade trade) {
        List<BukkitTask> tasks = completionTasks.remove(trade.getInitiatorUuid());
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }

    private static void playCompletionPing(Trade trade) {
        Player initiator = Bukkit.getPlayer(trade.getInitiatorUuid());
        if (initiator != null) {
            initiator.playSound(initiator, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
        }
        Player target = Bukkit.getPlayer(trade.getTargetUuid());
        if (target != null) {
            target.playSound(target, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
        }
    }

    private static void executeTrade(Trade trade) {
        if (!isInTrade(trade.getInitiatorUuid())) {
            return; // Already cancelled
        }

        Player initiator = Bukkit.getPlayer(trade.getInitiatorUuid());
        Player target = Bukkit.getPlayer(trade.getTargetUuid());
        boolean crossServer = isCrossServer(trade);

        // Final sync from GUIs (local players only)
        if (initiator != null) {
            syncLeftSlots(initiator, trade);
        }
        if (target != null) {
            syncLeftSlots(target, trade);
        }

        double initiatorMoney = trade.getMyMoney(trade.getInitiatorUuid());
        double targetMoney = trade.getMyMoney(trade.getTargetUuid());

        AranarthPlayer initiatorAp = AranarthUtils.getPlayer(trade.getInitiatorUuid());
        AranarthPlayer targetAp = AranarthUtils.getPlayer(trade.getTargetUuid());

        // Balance validation (same-server: both local; cross-server: initiator is local)
        if (initiatorMoney > 0 && initiatorAp != null && initiatorAp.getBalance() < initiatorMoney) {
            unregisterTrade(trade);
            if (initiator != null) {
                initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.not_enough_money")));
                returnItemsToPlayer(initiator, trade.getMyItems(trade.getInitiatorUuid()));
                initiator.closeInventory();
            }
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "trade.cancelled_money",
                        "player", initiatorAp.getNickname())));
                returnItemsToPlayer(target, trade.getMyItems(trade.getTargetUuid()));
                target.closeInventory();
            } else if (crossServer && NetworkManager.isActive()) {
                String nick = initiatorAp != null ? initiatorAp.getNickname() : trade.getInitiatorUuid().toString();
                NetworkManager.getInstance().publishTradeCancel(trade.getInitiatorUuid(),
                        trade.getInitiatorUuid(), nick);
            }
            return;
        }
        if (!crossServer && targetMoney > 0 && targetAp != null && targetAp.getBalance() < targetMoney) {
            unregisterTrade(trade);
            if (target != null) {
                target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "trade.not_enough_money")));
                returnItemsToPlayer(target, trade.getMyItems(trade.getTargetUuid()));
                target.closeInventory();
            }
            if (initiator != null) {
                initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.cancelled_money",
                        "player", targetAp.getNickname())));
                returnItemsToPlayer(initiator, trade.getMyItems(trade.getInitiatorUuid()));
                initiator.closeInventory();
            }
            return;
        }

        // Snapshot items before unregistering
        ItemStack[] toInitiator = cloneItemArray(trade.getOtherItems(trade.getInitiatorUuid()));
        ItemStack[] toTarget = cloneItemArray(trade.getOtherItems(trade.getTargetUuid()));

        unregisterTrade(trade);

        if (!crossServer) {
            // Same-server - apply balances for both players
            if (initiatorMoney != 0 || targetMoney != 0) {
                if (initiatorAp != null) {
                    initiatorAp.setBalance(initiatorAp.getBalance() - initiatorMoney + targetMoney);
                }
                if (targetAp != null) {
                    targetAp.setBalance(targetAp.getBalance() - targetMoney + initiatorMoney);
                }
                PersistenceUtils.saveAranarthPlayerImmediately(trade.getInitiatorUuid());
                PersistenceUtils.saveAranarthPlayerImmediately(trade.getTargetUuid());
            }
            if (initiator != null) {
                giveItems(initiator, toInitiator);
                initiator.closeInventory();
                initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.completed")));
                initiator.playSound(initiator, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            }
            if (target != null) {
                giveItems(target, toTarget);
                target.closeInventory();
                target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "trade.completed")));
                target.playSound(target, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            }
        } else {
            // Cross-server - handle initiator locally, then publish to target's server
            if ((initiatorMoney != 0 || targetMoney != 0) && initiatorAp != null) {
                initiatorAp.setBalance(initiatorAp.getBalance() - initiatorMoney + targetMoney);
                PersistenceUtils.saveAranarthPlayerImmediately(trade.getInitiatorUuid());
            }
            if (initiator != null) {
                giveItems(initiator, toInitiator);
                initiator.closeInventory();
                initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.completed")));
                initiator.playSound(initiator, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            }
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishTradeExecute(
                        trade.getInitiatorUuid(), toTarget, initiatorMoney, targetMoney);
            }
        }
    }

    /**
     * Cancels a trade, returns all offered items to each local player, and closes both GUIs.
     */
    public static void cancelTrade(Trade trade, UUID cancellerUuid) {
        boolean wasCrossServer = isCrossServer(trade);
        unregisterTrade(trade);

        Player initiator = Bukkit.getPlayer(trade.getInitiatorUuid());
        Player target = Bukkit.getPlayer(trade.getTargetUuid());

        AranarthPlayer cancellerAp = AranarthUtils.getPlayer(cancellerUuid);
        String cancellerNick = cancellerAp != null ? cancellerAp.getNickname() : "Unknown";

        if (initiator != null) {
            returnItemsToPlayer(initiator, trade.getMyItems(trade.getInitiatorUuid()));
            if (cancellerUuid.equals(trade.getInitiatorUuid())) {
                initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.cancelled_self")));
            } else {
                initiator.sendMessage(ChatUtils.chatMessage(
                        Lang.getFor(initiator, "trade.cancelled_other", "player", cancellerNick)));
            }
            initiator.closeInventory();
        }
        if (target != null) {
            returnItemsToPlayer(target, trade.getMyItems(trade.getTargetUuid()));
            if (cancellerUuid.equals(trade.getTargetUuid())) {
                target.sendMessage(ChatUtils.chatMessage(Lang.getFor(target, "trade.cancelled_self")));
            } else {
                target.sendMessage(ChatUtils.chatMessage(
                        Lang.getFor(target, "trade.cancelled_other", "player", cancellerNick)));
            }
            target.closeInventory();
        }

        // Notify the remote server if this was a cross-server trade
        if (wasCrossServer && NetworkManager.isActive()) {
            NetworkManager.getInstance().publishTradeCancel(
                    trade.getInitiatorUuid(), cancellerUuid, cancellerNick);
        }
    }

    /**
     * Called on the invitee's server when a cross-server invite arrives.
     */
    public static void onCrossServerInvite(UUID inviterUuid, String inviterName, UUID inviteeUuid) {
        addCrossServerInvite(inviteeUuid, inviterUuid);
        Player invitee = Bukkit.getPlayer(inviteeUuid);
        if (invitee == null) {
            return;
        }

        Component line1 = LegacyComponentSerializer.legacySection()
                .deserialize(ChatUtils.chatMessage(Lang.getFor(invitee, "trade.invite_received", "player", inviterName)));
        Component line2Pre = LegacyComponentSerializer.legacySection()
                .deserialize(ChatUtils.chatMessage(Lang.getFor(invitee, "trade.invite_line2_pre") + " "));
        Component line2Cmd = ChatUtils.clickableCommand(
                LegacyComponentSerializer.legacySection()
                        .deserialize(ChatUtils.translateToColor("&e/trade " + inviterName)),
                ChatUtils.translateToColor(Lang.getFor(invitee, "trade.invite_hover")),
                "/trade " + inviterName,
                true);
        Component line2Suf = LegacyComponentSerializer.legacySection()
                .deserialize(ChatUtils.translateToColor(Lang.getFor(invitee, "trade.invite_line2_suf")));
        Component hereLink = LegacyComponentSerializer.legacySection()
                .deserialize(ChatUtils.translateToColor(Lang.getFor(invitee, "trade.invite_here")))
                .clickEvent(ClickEvent.runCommand("/trade " + inviterName));
        Component message = line1
                .append(Component.newline())
                .append(line2Pre)
                .append(line2Cmd)
                .append(line2Suf)
                .append(Component.text(" "))
                .append(hereLink);
        invitee.sendMessage(message);
        AranarthUtils.playPingSound(invitee);
    }

    /**
     * Called on the initiator's server when the remote target accepts the trade.
     */
    public static void onCrossServerAccept(UUID initiatorUuid, UUID targetUuid) {
        removeInvite(targetUuid);
        Player initiator = Bukkit.getPlayer(initiatorUuid);
        if (initiator == null) {
            return;
        }

        Trade trade = new Trade(initiatorUuid, targetUuid);
        registerTrade(trade);
        markCrossServer(trade);
        startAnimation(trade);
        new com.aearost.aranarthcore.gui.GuiTrade(initiator, trade).openGui();

        AranarthPlayer targetAp = AranarthUtils.getPlayer(targetUuid);
        String targetNick = targetAp != null ? targetAp.getNickname() : targetUuid.toString();
        initiator.sendMessage(ChatUtils.chatMessage(Lang.getFor(initiator, "trade.accepted_other", "player", targetNick)));
    }

    /**
     * Called on a server when the remote player's offered items change.
     */
    public static void onRemoteItemUpdate(UUID changerUuid, UUID initiatorUuid, ItemStack[] items) {
        Trade trade = activeTrades.get(initiatorUuid);
        if (trade == null) {
            return;
        }

        // Copy incoming items into trade state
        ItemStack[] myItems = trade.getMyItems(changerUuid);
        System.arraycopy(items, 0, myItems, 0, Math.min(items.length, 12));

        // Reset confirmations since offer changed
        trade.resetConfirmations();
        trade.setCompleting(false);
        cancelCompletion(trade);
        updateConfirmButtons(trade);

        // Update the local player's right-side display
        UUID localUuid = trade.getOther(changerUuid);
        Player local = Bukkit.getPlayer(localUuid);
        if (!isTradeGuiOpen(local)) {
            return;
        }
        Inventory inv = local.getOpenInventory().getTopInventory();
        for (int i = 0; i < 12; i++) {
            inv.setItem(rightIndexToGuiSlot(i), items[i] != null ? items[i] : buildRightOfferFillerItem());
        }
    }

    /**
     * Called on a server when the remote player's money offer changes.
     */
    public static void onRemoteMoneyUpdate(UUID changerUuid, UUID initiatorUuid, double amount) {
        Trade trade = activeTrades.get(initiatorUuid);
        if (trade == null) {
            return;
        }

        trade.setMyMoney(changerUuid, amount);
        trade.resetConfirmations();
        trade.setCompleting(false);
        cancelCompletion(trade);
        updateConfirmButtons(trade);

        // Restart/stop animation based on whether either side has money
        boolean anyMoney = trade.getMyMoney(trade.getInitiatorUuid()) > 0
                || trade.getMyMoney(trade.getTargetUuid()) > 0;
        if (anyMoney) {
            startAnimation(trade);
        } else {
            stopAnimation(trade);
        }

        // Update the local player's pay-right display
        UUID localUuid = trade.getOther(changerUuid);
        Player local = Bukkit.getPlayer(localUuid);
        if (!isTradeGuiOpen(local)) {
            return;
        }
        AranarthPlayer changerAp = AranarthUtils.getPlayer(changerUuid);
        String changerNick = changerAp != null ? changerAp.getNickname() : "Unknown";
        Inventory inv = local.getOpenInventory().getTopInventory();
        inv.setItem(PAY_RIGHT, buildPayItem(local, amount, false, false, changerNick));
    }

    /**
     * Called on a server when the remote player toggles their confirm state.
     */
    public static void onRemoteConfirm(UUID confirmerUuid, UUID initiatorUuid, boolean confirmed) {
        Trade trade = activeTrades.get(initiatorUuid);
        if (trade == null) {
            return;
        }

        trade.setMyConfirmed(confirmerUuid, confirmed);
        updateConfirmButtons(trade);

        if (trade.areBothConfirmed()) {
            // Both confirmed - show completing state everywhere
            trade.setCompleting(true);
            updateConfirmButtons(trade);
            if (Bukkit.getPlayer(initiatorUuid) != null) {
                scheduleCompletion(trade); // initiator's server: full countdown + execution
            } else {
                scheduleCompletionAnimation(trade); // target's server: visual animation only
            }
        } else if (trade.isCompleting()) {
            // A confirm was revoked while completing - cancel the countdown
            trade.setCompleting(false);
            cancelCompletion(trade);
            updateConfirmButtons(trade);
        }
    }

    /**
     * Called on a server when the remote player (or their server) cancels the trade.
     */
    public static void onRemoteCancel(UUID initiatorUuid, UUID cancellerUuid, String cancellerNick) {
        Trade trade = activeTrades.get(initiatorUuid);
        if (trade == null) {
            return;
        }

        unregisterTrade(trade);

        UUID localUuid = trade.getOther(cancellerUuid);
        Player local = Bukkit.getPlayer(localUuid);
        if (local == null) {
            return;
        }

        returnItemsToPlayer(local, trade.getMyItems(localUuid));
        local.sendMessage(ChatUtils.chatMessage(
                Lang.getFor(local, "trade.cancelled_other", "player", cancellerNick)));
        local.closeInventory();
    }

    /**
     * Called on the target's server when the initiator's server executes the trade.
     */
    public static void onRemoteExecute(UUID initiatorUuid, ItemStack[] itemsForLocal,
                                       double initiatorMoney, double targetMoney) {
        Trade trade = activeTrades.get(initiatorUuid);
        if (trade == null) {
            return;
        }

        UUID localUuid = trade.getTargetUuid();
        Player local = Bukkit.getPlayer(localUuid);
        AranarthPlayer localAp = AranarthUtils.getPlayer(localUuid);

        // Validate local (target) player's balance
        if (targetMoney > 0 && localAp != null && localAp.getBalance() < targetMoney) {
            unregisterTrade(trade);
            if (local != null) {
                local.sendMessage(ChatUtils.chatMessage(Lang.getFor(local, "trade.not_enough_money")));
                returnItemsToPlayer(local, trade.getMyItems(localUuid));
                local.closeInventory();
            }
            return;
        }

        unregisterTrade(trade);

        if (localAp != null && (initiatorMoney != 0 || targetMoney != 0)) {
            localAp.setBalance(localAp.getBalance() - targetMoney + initiatorMoney);
            PersistenceUtils.saveAranarthPlayerImmediately(localUuid);
        }

        if (local != null) {
            giveItems(local, itemsForLocal);
            local.closeInventory();
            local.sendMessage(ChatUtils.chatMessage(Lang.getFor(local, "trade.completed")));
            local.playSound(local, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        }
    }

    private static void returnItemsToPlayer(Player player, ItemStack[] items) {
        giveItems(player, items);
    }

    public static void giveItems(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    private static ItemStack[] cloneItemArray(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = (source[i] != null && !source[i].getType().isAir()) ? source[i].clone() : null;
        }
        return copy;
    }

    /**
     * Builds the pay button item.
     *
     * @param otherNick the other player's display name; only used when isMine=false
     */
    public static ItemStack buildPayItem(Player player, double amount, boolean isMine,
                                         boolean showFilled, String otherNick) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        Material mat;
        if (amount <= 0) {
            mat = Material.PAPER;
        } else {
            mat = showFilled ? Material.FILLED_MAP : Material.MAP;
        }
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        String name;
        if (amount <= 0) {
            if (isMine) {
                name = Lang.getFor(player, "gui.trade.pay_none_mine");
                lore.add(Lang.getFor(player, "gui.trade.pay_click"));
            } else {
                name = Lang.getFor(player, "gui.trade.pay_none_other", "player",
                        otherNick != null ? otherNick : "?");
            }
        } else {
            if (isMine) {
                name = Lang.getFor(player, "gui.trade.pay_mine", "amount", nf.format(amount));
                lore.add(Lang.getFor(player, "gui.trade.pay_click_change"));
            } else {
                name = Lang.getFor(player, "gui.trade.pay_other",
                        "player", otherNick != null ? otherNick : "?",
                        "amount", nf.format(amount));
            }
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildConfirmItem(Player player, boolean myConfirmed, boolean completing) {
        if (completing) {
            return namedItem(Material.LIME_TERRACOTTA, Lang.getFor(player, "gui.trade.confirm_completing"));
        } else if (myConfirmed) {
            return namedItem(Material.LIME_DYE, Lang.getFor(player, "gui.trade.confirm_waiting"));
        } else {
            return namedItem(Material.GRAY_DYE, Lang.getFor(player, "gui.trade.confirm_button"));
        }
    }

    /**
     * Builds the slot-49 button with state-dependent appearance.
     */
    public static ItemStack buildCancelAreaItem(Player player, Trade trade) {
        UUID uuid = player.getUniqueId();
        if (trade.isCompleting() || trade.areBothConfirmed()) {
            ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(Lang.getFor(player, "gui.trade.in_progress"));
            meta.setLore(List.of(Lang.getFor(player, "gui.trade.in_progress_lore")));
            item.setItemMeta(meta);
            return item;
        } else if (trade.isMyConfirmed(uuid)) {
            return namedItem(Material.RED_DYE, Lang.getFor(player, "gui.trade.modify_button"));
        } else {
            return namedItem(Material.BARRIER, Lang.getFor(player, "gui.trade.cancel_button"));
        }
    }

    /**
     * Shows the other player's confirmation status in slot 51.
     */
    public static ItemStack buildOtherStatusItem(Player player, Trade trade) {
        UUID otherUuid = trade.getOther(player.getUniqueId());
        boolean otherConfirmed = trade.isMyConfirmed(otherUuid);
        AranarthPlayer otherAp = AranarthUtils.getPlayer(otherUuid);
        String otherNick = otherAp != null ? otherAp.getNickname() : "Unknown";
        if (otherConfirmed && trade.isCompleting()) {
            return namedItem(Material.LIME_TERRACOTTA, Lang.getFor(player, "gui.trade.other_confirmed", "player", otherNick));
        } else if (otherConfirmed) {
            return namedItem(Material.LIME_DYE, Lang.getFor(player, "gui.trade.other_confirmed", "player", otherNick));
        } else {
            return namedItem(Material.GRAY_DYE, Lang.getFor(player, "gui.trade.other_waiting", "player", otherNick));
        }
    }

    /**
     * Gray stained glass pane used for the outer border and center divider column.
     */
    public static ItemStack buildBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&r"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Light gray stained glass pane used as a placeholder for empty right-side offer slots.
     */
    public static ItemStack buildRightOfferFillerItem() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&r"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack namedItem(Material mat, String displayName) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTradeGuiOpen(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        String stripped = ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle());
        String prefix = ChatUtils.stripColorFormatting(Lang.getFor(player, "gui.trade.title")).split("\\{")[0];
        return stripped.startsWith(prefix);
    }

    public static int leftIndexToGuiSlot(int index) {
        return (index / 3 + 1) * 9 + (index % 3) + 1;
    }

    public static int rightIndexToGuiSlot(int index) {
        return (index / 3 + 1) * 9 + (index % 3) + 5;
    }

    /**
     * Returns true if the given top-inventory slot belongs to the left offer area (rows 1-4, cols 1-3).
     */
    public static boolean isLeftOfferSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 3;
    }

    /**
     * Returns true if the given top-inventory slot belongs to the right offer area (rows 1-4, cols 5-7).
     */
    public static boolean isRightOfferSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 5 && col <= 7;
    }

}
