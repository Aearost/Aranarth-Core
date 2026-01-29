package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiHomes {

	private final Player player;
	private final Inventory initializedGui;

	public GuiHomes(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}
	
	private Inventory initializeGui(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Inventory gui = null;
		int guiSize = aranarthPlayer.getHomes().size();
		String guiName = "Your Homes";

		// Size is based on which method is used
		// If the amount is a multiple of 9, use a full row
		if (guiSize % 9 != 0) {
			guiSize = ((int) (double) (guiSize / 9) + 1) * 9;
		}

		gui = Bukkit.getServer().createInventory(player, guiSize, guiName);

		for (Home home : aranarthPlayer.getHomes()) {
			ItemStack homeItem = new ItemStack(home.getIcon(), 1);
			ItemMeta homeItemMeta = homeItem.getItemMeta();
			homeItemMeta.setDisplayName(ChatUtils.translateToColor("&e" + home.getName()));
			homeItem.setItemMeta(homeItemMeta);
			gui.addItem(homeItem);
		}

		return gui;
	}

}
