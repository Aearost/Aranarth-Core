package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiVoteShopPurchase {

	private final Player player;
	private final Inventory initializedGui;
	private final ItemStack item;

	public GuiVoteShopPurchase(Player player, ItemStack item) {
		this.player = player;
		this.item = item;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}
	
	private Inventory initializeGui(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, ChatUtils.translateToColor("&a&lVote Shop Purchase"));
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		// Initialize Items
		ItemStack yellowPane = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
		ItemStack blackPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemStack back = new ItemStack(Material.RED_CONCRETE);

		// Removing name of panes
		ItemMeta yellowPaneMeta = yellowPane.getItemMeta();
		yellowPaneMeta.setDisplayName(" ");
		yellowPane.setItemMeta(yellowPaneMeta);
		ItemMeta blackPaneMeta = blackPane.getItemMeta();
		blackPaneMeta.setDisplayName(" ");
		blackPane.setItemMeta(blackPaneMeta);

		ItemMeta backMeta = back.getItemMeta();
		backMeta.setDisplayName(ChatUtils.translateToColor("&c&lBack"));
		back.setItemMeta(backMeta);
		ItemMeta itemMeta = item.getItemMeta();
		itemMeta.setDisplayName(ChatUtils.translateToColor("&a&lPurchase " + item.getItemMeta().getDisplayName()));
		item.setItemMeta(itemMeta);

		// Initialize GUI
		for (int position = 0; position < 27; position++) {
			// Top and bottom lines
			if (position < 9 || position >= 18) {
				gui.setItem(position, blackPane);
			}
		}

		gui.setItem(9, blackPane);
		gui.setItem(10, yellowPane);
		gui.setItem(11, yellowPane);
		gui.setItem(12, back);
		gui.setItem(13, yellowPane);
		gui.setItem(14, item);
		gui.setItem(15, yellowPane);
		gui.setItem(16, yellowPane);
		gui.setItem(17, blackPane);

		return gui;
	}

}
