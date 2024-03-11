package com.aearost.aranarthcore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiPlayerInvView {

	private Player playerExecutor;
	private Inventory initializedGui;

	public GuiPlayerInvView(Player playerExecutor, Player playerToView) {
		this.playerExecutor = playerExecutor;
		this.initializedGui = initializeGui(playerExecutor, playerToView);
	}
	
	public Inventory getInitializedGui() {
		return initializedGui;
	}

	public void openGui() {
		playerExecutor.closeInventory();
		playerExecutor.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player playerExecutor, Player playerToView) {
		Inventory gui = Bukkit.getServer().createInventory(playerExecutor, 45, playerToView.getDisplayName());
		Inventory viewedInventory = playerToView.getInventory();
		
		ItemStack blank = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
		ItemMeta blankMeta = blank.getItemMeta();
		blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
		blank.setItemMeta(blankMeta);
//		gui.setItem(0, blank);
		
		for (int i = 0; i < viewedInventory.getContents().length; i++) {
			ItemStack playerItem = viewedInventory.getContents()[i];
			gui.setItem(i, playerItem);
		}

		return gui;
	}

}
