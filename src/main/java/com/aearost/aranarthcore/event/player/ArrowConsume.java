package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.items.arrow.*;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.aearost.aranarthcore.items.CustomItemKeys.ARROW;
import static com.aearost.aranarthcore.items.CustomItemKeys.QUIVER;

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
                        ItemStack arrowToAdd = verifyIsSameArrow(launchedArrow, quiverArrow);
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

    /**
     * Provides the ItemStack with a single quantity of the input arrow type.
     * @param arrowType The type of custom arrow.
     * @return The arrow with a single quantity.
     */
    private ItemStack getArrowFromType(String arrowType) {
        return switch (arrowType) {
            case "iron" -> new ArrowIron().getItem();
            case "gold" -> new ArrowGold().getItem();
            case "amethyst" -> new ArrowAmethyst().getItem();
            case "obsidian" -> new ArrowObsidian().getItem();
            case "diamond" -> new ArrowDiamond().getItem();
            default -> null;
        };
    }

    private ItemStack verifyIsSameArrow(ItemStack launchedArrow, ItemStack quiverArrow) {
        // Basic or special arrow
        if (launchedArrow.getType() == Material.ARROW) {
            if (launchedArrow.hasItemMeta()) {
                if (quiverArrow.hasItemMeta()) {
                    // Both have meta
                    ItemMeta launchedMeta = launchedArrow.getItemMeta();
                    ItemMeta quiverMeta = quiverArrow.getItemMeta();
                    if (launchedMeta.getPersistentDataContainer().has(ARROW)) {
                        if (quiverMeta.getPersistentDataContainer().has(ARROW)) {
                            String launchedType = launchedMeta.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
                            String quiverType = quiverMeta.getPersistentDataContainer().get(ARROW, PersistentDataType.STRING);
                            if (launchedType.equals(quiverType)) {
                                return launchedArrow;
                            } else {
                                return null;
                            }
                        }
                    }
                    // One of them is not a Special arrow but has meta somehow
                    Bukkit.getLogger().info("Something went wrong with identifying the arrows...");
                    return null;
                } else {
                    return null;
                }
            } else {
                if (quiverArrow.hasItemMeta()) {
                    return null;
                } else {
                    // Both are regular arrows
                    return launchedArrow;
                }
            }
        }
        // Spectral arrow
        else if (launchedArrow.getType() == Material.SPECTRAL_ARROW) {
            if (quiverArrow.getType() == Material.SPECTRAL_ARROW) {
                return launchedArrow;
            }
        }
        // Tipped arrow
        else {
            if (quiverArrow.hasItemMeta()) {
                PotionMeta launchedMeta = (PotionMeta) launchedArrow.getItemMeta();
                PotionMeta quiverMeta = (PotionMeta) quiverArrow.getItemMeta();

                if (launchedMeta.getBasePotionType() == quiverMeta.getBasePotionType()) {
                    return launchedArrow;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        return null;
    }

}
