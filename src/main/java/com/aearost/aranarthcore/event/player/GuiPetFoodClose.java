package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.PetInventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Saves the pet food inventory contents back to the pet entity's PDC when the GUI is closed.
 */
public class GuiPetFoodClose {

    public void execute(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        UUID petUUID = PetInventoryUtils.getOpenPet(player.getUniqueId());
        if (petUUID == null) return;

        PetInventoryUtils.clearOpen(player.getUniqueId());

        Entity pet = Bukkit.getEntity(petUUID);
        if (pet == null) return; // Pet no longer loaded or was removed

        ItemStack[] items = e.getView().getTopInventory().getContents();
        PetInventoryUtils.setFoodItems(pet, items);
        player.playSound(player, Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
    }
}
