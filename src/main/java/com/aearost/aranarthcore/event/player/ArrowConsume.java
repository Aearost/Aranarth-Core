package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.QUIVER;

/**
 * Handles the auto-refill functionality when consuming of arrows.
 */
public class ArrowConsume {

    public void execute(EntityShootBowEvent e) {
        ItemStack bow = e.getBow();
        if (Objects.nonNull(bow)) {
            Map<Enchantment, Integer> enchantments = bow.getEnchantments();
            boolean hasInfinity = false;
            for (Enchantment enchantment : enchantments.keySet()) {
                if (enchantment == Enchantment.INFINITY) {
                    hasInfinity = true;
                    break;
                }
            }

            if (!hasInfinity) {
                if (e.getProjectile() instanceof Arrow || e.getProjectile() instanceof SpectralArrow) {
                    Player player = (Player) e.getEntity();
                    replaceArrow(player, e.getConsumable());
                }
            }
        }
    }

    private void replaceArrow(Player player, ItemStack launchedArrow) {
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        // Ensures that the user has a Quiver in their inventory
        boolean hasQuiver = false;
        for (ItemStack item : player.getInventory()) {
            if (Objects.isNull(item)) {
                continue;
            }
            if (item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(QUIVER)) {
                    hasQuiver = true;
                    break;
                }
            }
        }

        // Refill inventory with the shot arrow if it was in the quiver
        if (hasQuiver) {
            List<ItemStack> arrows = aranarthPlayer.getArrows();
            if (Objects.nonNull(arrows)) {
                for (ItemStack quiverArrow : arrows) {
                    if (Objects.nonNull(quiverArrow)) {
                        ItemStack arrowToAdd = AranarthUtils.verifyIsSameArrow(launchedArrow, quiverArrow);
                        if (arrowToAdd != null) {
                            player.getInventory().addItem(arrowToAdd);

                            int newAmountInQuiver = quiverArrow.getAmount() - 1;
                            if (newAmountInQuiver > 0) {
                                quiverArrow.setAmount(newAmountInQuiver);
                            } else {
                                arrows.remove(quiverArrow);
                            }

                            aranarthPlayer.setArrows(arrows);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                            return;
                        }
                    }
                }
            }
        }
    }
}
