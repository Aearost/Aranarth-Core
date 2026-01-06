package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
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

                    // If they are not yet exceeding the limit
                    // +1 as it is attempting to be added but may not add successfully
                    if (storedPotionNum + potionAmountAdded + 1 <= AranarthUtils.getMaxPotionNum(player)) {
                        if (potions.containsKey(inventoryPotion)) {
                            int amount = potions.get(inventoryPotion);
                            potions.put(inventoryPotion, amount + 1);
                            potionAmountAdded++;
                        } else {
                            potions.put(inventoryPotion, 1);
                            potionAmountAdded++;
                        }
                    }
                    // The limit has been exceeded
                    else {
                        potionAmountUnableToAdd++;
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(inventoryPotion);
                        // If the player's inventory was full, drop it to the ground
                        if (!leftover.isEmpty()) {
                            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                        }
                    }
                }
            }
            aranarthPlayer.setPotions(potions);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            if (potionAmountAdded > 0) {
                player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + potionAmountAdded + " &7potions!"));
            }
            if (potionAmountUnableToAdd > 0) {
                player.sendMessage(ChatUtils.chatMessage("&7You could not add &e" + potionAmountUnableToAdd + " &7potions"));
            }
        }
	}
}
