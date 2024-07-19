package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiVillager {

	private Player player;
	private Inventory initializedGui;

	public GuiVillager(Player player, Villager villager) {
		this.player = player;
		this.initializedGui = initializeGui(player, villager);
	}
	
	public Inventory getInitializedGui() {
		return initializedGui;
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player, Villager villager) {
		Inventory gui = Bukkit.getServer().createInventory(player, 9, "Villager");
		Inventory villagerInventory = villager.getInventory();
		
		ItemStack barrier = new ItemStack(Material.BARRIER);
		ItemMeta barrierMeta = barrier.getItemMeta();
		barrierMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
		barrier.setItemMeta(barrierMeta);
		gui.setItem(8, barrier);
		
		for (int i = 0; i < villagerInventory.getContents().length; i++) {
			ItemStack villagerItem = villagerInventory.getContents()[i];
			gui.setItem(i, villagerItem);
		}

		return gui;
	}

}
