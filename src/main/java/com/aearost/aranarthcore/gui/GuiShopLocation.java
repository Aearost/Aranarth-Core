package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GuiShopLocation {

	private final Player player;
	private final Inventory initializedGui;

	public GuiShopLocation(Player player, int pageNum) {
		this.player = player;
		this.initializedGui = initializeGui(player, pageNum);
	}

	public void openGui() {
		player.closeInventory();
		if (initializedGui != null) {
			player.openInventory(initializedGui);
		}
	}
	
	private Inventory initializeGui(Player player, int pageNum) {
		HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
		int totalShopsOnPage = shopLocations.size();
		int shopLocationStartIndex = pageNum * 27;
		Inventory gui = null;

		gui = Bukkit.getServer().createInventory(player, 36, "Player Shops");

		// Initialize Items
		ItemStack previous = new ItemStack(Material.RED_WOOL);
		ItemStack barrier = new ItemStack(Material.BARRIER);
		ItemStack next = new ItemStack(Material.LIME_WOOL);
		ItemStack blank = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

		// Previous
		ItemMeta previousMeta = previous.getItemMeta();
		if (Objects.nonNull(previousMeta)) {
			previousMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
			previous.setItemMeta(previousMeta);
		}

		// Barrier
		ItemMeta barrierMeta = barrier.getItemMeta();
		if (Objects.nonNull(barrierMeta)) {
			barrierMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
			barrier.setItemMeta(barrierMeta);
		}

		// Next
		ItemMeta nextMeta = next.getItemMeta();
		if (Objects.nonNull(nextMeta)) {
			nextMeta.setDisplayName(ChatUtils.translateToColor("&a&lNext"));
			next.setItemMeta(nextMeta);
		}

		// Blank
		ItemMeta blankMeta = blank.getItemMeta();
		if (Objects.nonNull(blankMeta)) {
			blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
			blank.setItemMeta(blankMeta);
		}

		// Initialize GUI
		gui.setItem(27, previous);
		gui.setItem(28, blank);
		gui.setItem(29, blank);
		gui.setItem(30, blank);
		gui.setItem(31, barrier);
		gui.setItem(32, blank);
		gui.setItem(33, blank);
		gui.setItem(34, blank);
		gui.setItem(35, next);

		List<UUID> uuidList = new ArrayList<>();
        uuidList.addAll(shopLocations.keySet());

		for (int i = 0; i < 27; i++) {
			// If the current shop location being iterated is the last shop location in the list (none come after)
			if (i >= shopLocations.size()) {
				gui.setItem(i, blank);
				continue;
			}

			UUID uuid = uuidList.get(shopLocationStartIndex + i);
			AranarthPlayer shopLocationPlayer = AranarthUtils.getPlayer(uuid);
			ItemStack shopItem = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta shopItemMeta = (SkullMeta) shopItem.getItemMeta();
			shopItemMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
			shopItemMeta.setDisplayName(ChatUtils.translateToColor("&e" + shopLocationPlayer.getNickname() + "&e's Shop"));
			shopItem.setItemMeta(shopItemMeta);
			gui.setItem(i, shopItem);
		}
		return gui;
	}

}
