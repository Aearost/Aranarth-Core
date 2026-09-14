package com.aearost.aranarthcore.objects;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents an active item/money trade between two players.
 */
public class Trade {

    private final UUID initiatorUuid;
    private final UUID targetUuid;
    private final ItemStack[] initiatorItems = new ItemStack[12];
    private final ItemStack[] targetItems = new ItemStack[12];
    private double initiatorMoney = 0.0;
    private double targetMoney = 0.0;
    private boolean initiatorConfirmed = false;
    private boolean targetConfirmed = false;
    private boolean completing = false;

    public Trade(UUID initiatorUuid, UUID targetUuid) {
        this.initiatorUuid = initiatorUuid;
        this.targetUuid = targetUuid;
    }

    public UUID getInitiatorUuid() {
        return initiatorUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public boolean isInitiator(UUID uuid) {
        return initiatorUuid.equals(uuid);
    }

    /**
     * Returns the UUID of the other player in this trade.
     */
    public UUID getOther(UUID uuid) {
        return isInitiator(uuid) ? targetUuid : initiatorUuid;
    }

    /**
     * Returns the items offered BY the given player.
     */
    public ItemStack[] getMyItems(UUID uuid) {
        return isInitiator(uuid) ? initiatorItems : targetItems;
    }

    /**
     * Returns the items offered BY the other player (what the given player will receive).
     */
    public ItemStack[] getOtherItems(UUID uuid) {
        return isInitiator(uuid) ? targetItems : initiatorItems;
    }

    public double getMyMoney(UUID uuid) {
        return isInitiator(uuid) ? initiatorMoney : targetMoney;
    }

    public void setMyMoney(UUID uuid, double amount) {
        if (isInitiator(uuid)) {
            initiatorMoney = amount;
        } else {
            targetMoney = amount;
        }
    }

    public boolean isMyConfirmed(UUID uuid) {
        return isInitiator(uuid) ? initiatorConfirmed : targetConfirmed;
    }

    public void setMyConfirmed(UUID uuid, boolean confirmed) {
        if (isInitiator(uuid)) {
            initiatorConfirmed = confirmed;
        } else {
            targetConfirmed = confirmed;
        }
    }

    public boolean areBothConfirmed() {
        return initiatorConfirmed && targetConfirmed;
    }

    public void resetConfirmations() {
        initiatorConfirmed = false;
        targetConfirmed = false;
    }

    /**
     * True during the 1-second completion countdown; blocks further interaction.
     */
    public boolean isCompleting() {
        return completing;
    }

    public void setCompleting(boolean completing) {
        this.completing = completing;
    }
}
