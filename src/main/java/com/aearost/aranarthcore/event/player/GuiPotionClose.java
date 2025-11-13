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
public class GuiPotionClose {

	public void execute(final InventoryCloseEvent e) {
        Inventory inventory = e.getInventory();
        if (inventory.getContents().length > 0) {
            Player player = (Player) e.getPlayer();
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
            List<ItemStack> inventoryPotions = new LinkedList<>(Arrays.asList(inventory.getContents()));


            if (Objects.isNull(potions)) {
                potions = new HashMap<>();
            }

            int potionAmountAdded = 0;
            for (ItemStack inventoryPotion : inventoryPotions) {
                if (Objects.nonNull(inventoryPotion)) {
                    // Rare chance that inventory glitches and stores air
                    if (inventoryPotion.getType() == Material.AIR) {
                        continue;
                    }

                    if (potions.containsKey(inventoryPotion)) {
                        int amount = potions.get(inventoryPotion);
                        potions.put(inventoryPotion, amount + 1);
                        potionAmountAdded++;
                    } else {
                        potions.put(inventoryPotion, 1);
                        potionAmountAdded++;
                    }
                }
            }
            aranarthPlayer.setPotions(potions);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            if (potionAmountAdded > 0) {
                player.sendMessage(ChatUtils.chatMessage("&7You have added &e" + potionAmountAdded + " &7potions!"));
            }
        }
	}
}
