package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Refills the firework rocket used to boost elytra flight from the player's Quiver.
 */
public class QuiverFireworkBoost {

    // Raw PlayerInventory slot index of the off-hand
    private static final int OFF_HAND_SLOT = 40;

    public void execute(PlayerElytraBoostEvent e) {
        if (e.isCancelled() || !e.shouldConsume()) {
            return;
        }

        Player player = e.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        ItemStack launchedFirework = e.getItemStack().clone();
        launchedFirework.setAmount(1);
        int slot = e.getHand() == EquipmentSlot.OFF_HAND ? OFF_HAND_SLOT : player.getInventory().getHeldItemSlot();

        // The firework is only consumed after the event, so the refill runs on the next tick
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            if (player.isOnline()) {
                new ArrowConsume().replaceArrow(player, launchedFirework, slot);
            }
        });
    }
}
