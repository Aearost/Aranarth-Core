package com.aearost.aranarthcore.gui;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.utils.AranarthUtils;

public class GuiPotions {

	private Player player;
	private Inventory initializedGui;

	public GuiPotions(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}
	
	public Inventory getInitializedGui() {
		return initializedGui;
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 54, "Potions");
		
		List<ItemStack> potions = AranarthUtils.getPotions(player.getUniqueId());
		
		if (Objects.nonNull(potions)) {
			for (int i = 0; i < potions.size(); i++) {
				ItemStack potion = potions.get(i);
				gui.setItem(i, potion);
			}
		}

		return gui;
	}

}
