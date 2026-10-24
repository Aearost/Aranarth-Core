package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.aearost.aranarthcore.objects.CustomKeys.QUIVER;

/**
 * Handles the auto-refill functionality when consuming of arrows and firework rockets.
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
                if (e.getProjectile() instanceof Arrow || e.getProjectile() instanceof SpectralArrow
                        || e.getProjectile() instanceof Firework) {
                    Player player = (Player) e.getEntity();
                    replaceArrow(player, e.getConsumable());
                }
            }
        }
    }

    /**
     * Refills the player's inventory with a matching arrow from their Quiver.
     * @param player The player.
     * @param launchedArrow The arrow that was launched.
     */
    public void replaceArrow(Player player, ItemStack launchedArrow) {
        replaceArrow(player, launchedArrow, -1);
    }

    /**
     * Refills the player's inventory with a matching arrow or firework rocket from their Quiver.
     * @param player The player.
     * @param launchedArrow The arrow or firework rocket that was launched.
     * @param preferredSlot The inventory slot the refill should be placed in if possible, or -1 for any slot.
     */
    public void replaceArrow(Player player, ItemStack launchedArrow, int preferredSlot) {
        if (player.getGameMode() != GameMode.SURVIVAL || Objects.isNull(launchedArrow)) {
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
                        if (AranarthUtils.verifyIsSameArrow(launchedArrow, quiverArrow) != null) {
                            // Only a single item is refilled regardless of the launched stack size
                            ItemStack arrowToAdd = quiverArrow.clone();
                            arrowToAdd.setAmount(1);
                            addToInventory(player, arrowToAdd, preferredSlot);

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

                boolean isFirework = launchedArrow.getType() == Material.FIREWORK_ROCKET;
                boolean hasArrowsInInventory = false;
                for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
                    if (inventoryItem != null) {
                        if (isSameCategory(inventoryItem, isFirework)) {
                            hasArrowsInInventory = true;
                            break;
                        }
                    }
                }

                // The off-hand is not part of the storage contents but can still hold firework rockets
                if (isFirework && isSameCategory(player.getInventory().getItemInOffHand(), true)) {
                    hasArrowsInInventory = true;
                }

                if (!hasArrowsInInventory) {
                    // If none match but there are arrows in the Quiver, provide the first stack
                    for (ItemStack quiverArrow : arrows) {
                        if (Objects.nonNull(quiverArrow)) {
                            if (isSameCategory(quiverArrow, isFirework)) {
                                addToInventory(player, quiverArrow.clone(), preferredSlot);

                                // As the whole stack was taken, remove the whole stack from the Quiver
                                arrows.remove(quiverArrow);

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

    /**
     * Adds the item to the preferred slot if possible, otherwise anywhere in the inventory.
     * @param player The player.
     * @param item The item being added.
     * @param preferredSlot The preferred inventory slot, or -1 for any slot.
     */
    private void addToInventory(Player player, ItemStack item, int preferredSlot) {
        PlayerInventory inventory = player.getInventory();
        if (preferredSlot >= 0) {
            ItemStack current = inventory.getItem(preferredSlot);
            if (Objects.isNull(current) || current.getType() == Material.AIR) {
                inventory.setItem(preferredSlot, item);
                return;
            }
            if (current.isSimilar(item) && current.getAmount() + item.getAmount() <= current.getMaxStackSize()) {
                current.setAmount(current.getAmount() + item.getAmount());
                return;
            }
        }
        Map<Integer, ItemStack> leftovers = inventory.addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItem(player.getLocation(), leftover));
    }

    /**
     * Determines whether the input item is a firework rocket when a firework was launched, otherwise an arrow.
     * @param item The item.
     * @param isFirework Whether the launched item was a firework rocket.
     * @return Whether the input item is in the same category as the launched item.
     */
    private boolean isSameCategory(ItemStack item, boolean isFirework) {
        if (isFirework) {
            return item.getType() == Material.FIREWORK_ROCKET;
        }
        return isArrow(item);
    }

    /**
     * Determines whether the input item is an arrow, spectral arrow, or tipped arrow.
     * @param item The item.
     * @return Whether the input item is an arrow, spectral arrow, or tipped arrow.
     */
    private boolean isArrow(ItemStack item) {
        return item.getType() == Material.ARROW || item.getType() == Material.SPECTRAL_ARROW
                || item.getType() == Material.TIPPED_ARROW;
    }
}
