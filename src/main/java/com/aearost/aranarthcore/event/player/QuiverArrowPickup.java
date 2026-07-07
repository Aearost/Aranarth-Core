package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.QUIVER;

/**
 * When a player picks up arrows and has a quiver, prioritizes filling non-full
 * matching stacks in the quiver before letting vanilla handle the pickup.
 */
public class QuiverArrowPickup {

    public void execute(EntityPickupItemEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Player player = (Player) e.getEntity();
        ItemStack pickupItem = e.getItem().getItemStack();

        if (!isArrow(pickupItem)) {
            return;
        }

        // Check that the player has a quiver
        boolean hasQuiver = false;
        for (ItemStack item : player.getInventory()) {
            if (Objects.nonNull(item) && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(QUIVER)) {
                    hasQuiver = true;
                    break;
                }
            }
        }

        if (!hasQuiver) {
            return;
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        List<ItemStack> arrows = aranarthPlayer.getArrows();

        if (Objects.isNull(arrows)) {
            return;
        }

        int amountToPickup = pickupItem.getAmount();
        int originalAmount = amountToPickup;

        // Fill non-full matching stacks in the quiver
        for (ItemStack quiverStack : arrows) {
            if (amountToPickup <= 0) {
                break;
            }
            if (Objects.isNull(quiverStack)) {
                continue;
            }

            // Only fill stacks of the exact same arrow type
            if (AranarthUtils.verifyIsSameArrow(pickupItem, quiverStack) == null) {
                continue;
            }

            int maxStack = quiverStack.getMaxStackSize();
            if (quiverStack.getAmount() >= maxStack) {
                continue;
            }

            int space = maxStack - quiverStack.getAmount();
            int toAdd = Math.min(amountToPickup, space);
            quiverStack.setAmount(quiverStack.getAmount() + toAdd);
            amountToPickup -= toAdd;
        }

        // Nothing was added to the quiver
        if (amountToPickup == originalAmount) {
            return;
        }

        e.setCancelled(true);
        e.getItem().remove();
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 0.2F, 2F);

        // Drop any amount that didn't fit into a matching quiver stack into the player inventory
        if (amountToPickup > 0) {
            ItemStack remaining = pickupItem.clone();
            remaining.setAmount(amountToPickup);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(remaining);
            if (!leftovers.isEmpty()) {
                leftovers.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
            }
        }

        aranarthPlayer.setArrows(arrows);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
    }

    private boolean isArrow(ItemStack item) {
        return item.getType() == Material.ARROW
                || item.getType() == Material.SPECTRAL_ARROW
                || item.getType() == Material.TIPPED_ARROW;
    }
}
