package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.PetInventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Prevents items that the pet cannot eat from being placed into the pet food inventory.
 */
public class GuiPetFoodClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        UUID petUUID = PetInventoryUtils.getOpenPet(player.getUniqueId());
        Entity pet = petUUID != null ? Bukkit.getEntity(petUUID) : null;

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            // From player inventory: cancel shift-click for disallowed items.
            ItemStack current = e.getCurrentItem();
            if (current != null && current.getType() != Material.AIR
                    && !isAllowed(pet, current.getType())) {
                e.setCancelled(true);
            }
        } else {
            // Inside the pet food GUI
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR
                    && !isAllowed(pet, cursor.getType())) {
                e.setCancelled(true);
                return;
            }
            // Prevent swapping a disallowed hotbar item into a GUI slot
            if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
                ItemStack hotbarItem = e.getView().getBottomInventory().getItem(e.getHotbarButton());
                if (hotbarItem != null && hotbarItem.getType() != Material.AIR
                        && !isAllowed(pet, hotbarItem.getType())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    private static boolean isAllowed(Entity pet, Material material) {
        if (pet == null) return false;
        return PetInventoryUtils.isAllowedFood(pet, material);
    }
}
