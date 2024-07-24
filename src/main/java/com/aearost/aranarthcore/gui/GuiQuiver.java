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

	private final Player player;
	private final Inventory initializedGui;

	public GuiQuiver(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
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
            for (ItemStack arrow : arrows) {
                if (Objects.nonNull(arrow)) {
					initializedArrows.add(arrow);
                }
            }

            for (ItemStack initializedArrow : initializedArrows) {
                gui.addItem(initializedArrow);
            }
		}
		
		return gui;
	}

}
