package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiDominionResourcesPreview {

	public static final String TITLE_SUFFIX = " Resources";

	private final Player player;
	private final Inventory initializedGui;

	public GuiDominionResourcesPreview(Player player, Biome biome) {
		this.player = player;
		this.initializedGui = initializeGui(player, biome);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player, Biome biome) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		String biomeName = DominionUtils.getBiomeName(biome);
		List<ItemStack> items = DominionUtils.getResourcesByDominionAndBiome(dominion, biome, 1.0);

		// Content rows between top and bottom filler rows
		int contentSlots = Math.min(items.size(), 36);
		int contentRows = Math.max(1, (int) Math.ceil((double) contentSlots / 9.0));
		int size = contentRows * 9 + 18;

		// Back button sits in the center of the bottom row
		int backSlot = size - 5;

		Inventory gui = Bukkit.getServer().createInventory(player, size, biomeName + TITLE_SUFFIX);

		ItemStack filler = makeFiller();

		// Top filler row
		for (int i = 0; i < 9; i++) {
			gui.setItem(i, filler);
		}
		// Bottom filler row
		for (int i = size - 9; i < size; i++) {
			gui.setItem(i, filler);
		}

		// Resource items in content area
		for (int i = 0; i < items.size() && i < size - 18; i++) {
			gui.setItem(9 + i, items.get(i));
		}

		// Back button in center of bottom row
		gui.setItem(backSlot, buildBackButton());

		return gui;
	}

	private ItemStack makeFiller() {
		ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = pane.getItemMeta();
		meta.setDisplayName(" ");
		pane.setItemMeta(meta);
		return pane;
	}

	private ItemStack buildBackButton() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&7Back"));
		item.setItemMeta(meta);
		return item;
	}
}
