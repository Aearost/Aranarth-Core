package com.aearost.aranarthcore.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;

public class GuiQuiver {

	private Player player;
	private Inventory initializedGui;

	public GuiQuiver(Player player) {
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
		Inventory gui = Bukkit.getServer().createInventory(player, 18, "Quiver");
		
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<ItemStack> arrows = aranarthPlayer.getArrows();
		List<ItemStack> initializedArrows = new ArrayList<>();
		
		if (Objects.nonNull(arrows)) {
			for (int i = 0; i < arrows.size(); i++) {
				ItemStack arrow = arrows.get(i);
				if (Objects.isNull(arrow)) {
					continue;
				} else {
					initializedArrows.add(arrow);
				}
			}
			
			for (int i = 0; i < initializedArrows.size(); i++) {
				gui.addItem(initializedArrows.get(i));
			}
		}
		
		return gui;
	}

}
