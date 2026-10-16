package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/**
 * Adds the input potions to the player's potion inventory.
 */
public class GuiPotionAddClose {

	public void execute(final InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (inventory.getContents().length > 0) {
            Player player = (Player) e.getPlayer();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            if (aranarthPlayer.isAddingPotions()) {
                return;
            }

            HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
            List<ItemStack> inventoryPotions = new LinkedList<>(Arrays.asList(inventory.getContents()));


            if (Objects.isNull(potions)) {
                potions = new HashMap<>();
            }

            int potionAmountAdded = 0;
            int potionAmountUnableToAdd = 0;
            int storedPotionNum = AranarthUtils.getPlayerStoredPotionNum(player);

            for (ItemStack inventoryPotion : inventoryPotions) {
                if (Objects.nonNull(inventoryPotion)) {
                    // Rare chance that inventory glitches and stores air
                    if (inventoryPotion.getType() == Material.AIR) {
                        continue;
                    }

                    // Only potions can be stored, anything else goes back to the player
                    if (!isPotion(inventoryPotion)) {
                        giveBack(player, inventoryPotion);
                        continue;
                    }

                    // Strip brewing tags so that bonus copies (BREWING_COPY) and originals
                    // (BREWED_POTION already stripped, but defensive) are treated as the same key
                    ItemStack normalizedPotion = inventoryPotion.clone();
                    normalizedPotion.setAmount(1);
                    if (normalizedPotion.hasItemMeta()) {
                        var normalizedMeta = normalizedPotion.getItemMeta();
                        normalizedMeta.getPersistentDataContainer().remove(CustomKeys.BREWING_COPY);
                        normalizedMeta.getPersistentDataContainer().remove(CustomKeys.BREWED_POTION);
                        normalizedPotion.setItemMeta(normalizedMeta);
                    }

                    // Each potion in the stack is stored individually
                    for (int unit = 0; unit < inventoryPotion.getAmount(); unit++) {
                        // If they are not yet exceeding the limit
                        // +1 as it is attempting to be added but may not add successfully
                        if (storedPotionNum + potionAmountAdded + 1 <= AranarthUtils.getMaxPotionNum(player)) {
                            potions.merge(normalizedPotion, 1, Integer::sum);
                            potionAmountAdded++;
                        }
                        // The limit has been exceeded
                        else {
                            potionAmountUnableToAdd++;
                            giveBack(player, normalizedPotion.clone());
                        }
                    }
                }
            }
            aranarthPlayer.setPotions(potions);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            if (potionAmountAdded > 0) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.added", "count", potionAmountAdded)));
            }
            if (potionAmountUnableToAdd > 0) {
                player.sendMessage(ChatUtils.chatMessage(Lang.get("potions.add_failed", "count", potionAmountUnableToAdd)));
            }
        }
	}

	private boolean isPotion(ItemStack item) {
		return item.getType() == Material.POTION
				|| item.getType() == Material.SPLASH_POTION
				|| item.getType() == Material.LINGERING_POTION;
	}

	/**
	 * Returns the item to the player's inventory, dropping it at their feet if it is full.
	 */
	private void giveBack(Player player, ItemStack item) {
		HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
		for (ItemStack drop : leftover.values()) {
			player.getWorld().dropItemNaturally(player.getLocation(), drop);
		}
	}
}
