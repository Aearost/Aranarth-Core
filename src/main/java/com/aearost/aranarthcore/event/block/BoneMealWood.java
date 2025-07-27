package com.aearost.aranarthcore.event.block;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Allows for bone meal to re-add bark to stripped logs, and make logs 6-sided bark.
 */
public class BoneMealWood {
    public void execute(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.HAND) {
            if (isHoldingBoneMeal(e.getPlayer())) {
                String name = e.getClickedBlock().getType().name();
                // Anything that's been stripped, wood or nether mushrooms
                if (name.startsWith("STRIPPED_")) {
                    String nonStrippedItem = name.substring(9);
                    e.getClickedBlock().setType(Material.valueOf(nonStrippedItem));
                }
                // Turning logs into six-sided wood
                else if (name.endsWith("_LOG")) {
                    String nonLog = name.substring(0, name.length() - 4);
                    String asWood = nonLog + "_WOOD";
                    e.getClickedBlock().setType(Material.valueOf(asWood));
                }
                // Nether mushroom stems
                else if (name.endsWith("_STEM") && !name.startsWith("MUSHROOM_")) {
                    String nonStem = name.substring(0, name.length() - 5);
                    String asHyphae = nonStem + "_HYPHAE";
                    e.getClickedBlock().setType(Material.valueOf(asHyphae));
                }
                // Only apply when clicking on wood
                else {
                    return;
                }

                int newAmount = e.getPlayer().getInventory().getItemInMainHand().getAmount() - 1;
                e.getPlayer().getInventory().getItemInMainHand().setAmount(newAmount);
                e.getClickedBlock().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, e.getClickedBlock().getLocation(), 5, 0.6, 0.3, 0.6);
                e.getClickedBlock().getWorld().playSound(e.getClickedBlock().getLocation(), Sound.ITEM_AXE_STRIP, 1F, 0.1F);
            }
        }
    }

    /**
     * Determines if the player is holding bone meal or not.
     * @param player The player.
     * @return Confirmation whether the player is holding bone meal or not.
     */
    private boolean isHoldingBoneMeal(Player player) {
        Material item = player.getInventory().getItemInMainHand().getType();
        return item == Material.BONE_MEAL;
    }
}
