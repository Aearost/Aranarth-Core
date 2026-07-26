package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PetInventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A 9-slot inventory for storing food that a pet will auto-consume to heal.
 */
public class GuiPetFood {

    public static final String TITLE = "Pet Food";

    private final Player player;
    private final Entity pet;

    public GuiPetFood(Player player, Entity pet) {
        this.player = player;
        this.pet = pet;
    }

    public void openGui() {
        ItemStack[] food = PetInventoryUtils.getFoodItems(pet);
        Inventory gui = Bukkit.createInventory(player, PetInventoryUtils.FOOD_SLOTS,
                ChatUtils.translateToColor(TITLE));
        for (int i = 0; i < Math.min(food.length, PetInventoryUtils.FOOD_SLOTS); i++) {
            gui.setItem(i, food[i]);
        }
        player.closeInventory();
        player.openInventory(gui);
        PetInventoryUtils.trackOpen(player.getUniqueId(), pet.getUniqueId());
        player.playSound(player, Sound.BLOCK_CHEST_OPEN, 1F, 1F);
    }
}
