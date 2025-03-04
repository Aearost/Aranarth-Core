package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;

public class GuiBlacklist {

	private final Player player;
	private final Inventory initializedGui;

	public GuiBlacklist(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Blacklist");
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<ItemStack> blacklistedItems = aranarthPlayer.getBlacklist();
		if (Objects.nonNull(blacklistedItems)) {
			for (int i = 0; i < blacklistedItems.size(); i++) {
				ItemStack blacklistedItem = blacklistedItems.get(i);
				gui.setItem(i, blacklistedItem);
			}
		}

		return gui;
	}

}
