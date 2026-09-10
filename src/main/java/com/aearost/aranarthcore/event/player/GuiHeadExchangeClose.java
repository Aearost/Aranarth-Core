package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiHeadExchange;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Returns any item left in the input slot when the Head Exchange GUI is closed.
 */
public class GuiHeadExchangeClose {

    public void execute(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        ItemStack input = e.getInventory().getItem(GuiHeadExchange.SLOT_INPUT);
        if (input != null && input.getType() != Material.AIR) {
            e.getInventory().setItem(GuiHeadExchange.SLOT_INPUT, null);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(input);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        GuiHeadExchange.playerVariantIndex.remove(player.getUniqueId());
        GuiHeadExchange.playerCurrentMaterial.remove(player.getUniqueId());
    }
}
