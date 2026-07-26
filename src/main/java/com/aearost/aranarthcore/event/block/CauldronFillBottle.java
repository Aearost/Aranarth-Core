package com.aearost.aranarthcore.event.block;

import com.dre.brewery.BCauldron;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.HashMap;

/**
 * When a player right-clicks a normal WATER_CAULDRON with a glass bottle, fills the bottle
 * without decreasing the cauldron's water level. Brewery brew cauldrons are excluded so
 * brewing workflows are unaffected.
 */
public class CauldronFillBottle {

    public void execute(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.WATER_CAULDRON) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.GLASS_BOTTLE) {
            return;
        }
        // Leave brewery brew cauldrons alone
        if (BCauldron.get(block) != null) {
            return;
        }
        e.setCancelled(true);

        ItemStack waterBottle = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) waterBottle.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        waterBottle.setItemMeta(meta);

        Player player = e.getPlayer();
        PlayerInventory inv = player.getInventory();
        if (item.getAmount() == 1) {
            inv.setItemInMainHand(waterBottle);
        } else {
            item.setAmount(item.getAmount() - 1);
            HashMap<Integer, ItemStack> leftover = inv.addItem(waterBottle);
            leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }

        player.playSound(block.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
    }
}
