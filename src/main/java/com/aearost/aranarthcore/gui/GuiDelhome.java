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

public class GuiDelhome {

	private final Player player;
	private final Inventory initializedGui;

	public GuiDelhome(Player player) {
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
		int guiSize = aranarthPlayer.getHomes().size();
		String guiName = "Delete Home";

		if (guiSize % 9 != 0) {
			guiSize = ((int) (double) (guiSize / 9) + 1) * 9;
		}

		Inventory gui = Bukkit.getServer().createInventory(player, guiSize, guiName);

		for (Home home : aranarthPlayer.getHomes()) {
			ItemStack homeItem = new ItemStack(home.getIcon(), 1);
			ItemMeta homeItemMeta = homeItem.getItemMeta();
			homeItemMeta.setDisplayName(ChatUtils.translateToColor("&c" + home.getName()));
			homeItem.setItemMeta(homeItemMeta);
			gui.addItem(homeItem);
		}

		return gui;
	}

}
