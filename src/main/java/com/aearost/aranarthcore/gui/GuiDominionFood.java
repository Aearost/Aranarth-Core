package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuiDominionFood {

	private final Player player;
	private final Inventory initializedGui;

	public GuiDominionFood(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());

		int size = 0;
		if (dominion.getChunks().size() <= 25) {
			size = 18;
		} else if (dominion.getChunks().size() <= 100) {
			size = 36;
		} else {
			size = 54;
		}

		Inventory gui = Bukkit.getServer().createInventory(player, size, ChatUtils.translateToColor("&e" + dominion.getName() +"'s &rFood Storage"));
		for (int i = 0; i < size; i++) {
			gui.setItem(i, dominion.getFood()[i]);
		}
		return gui;
	}

}
