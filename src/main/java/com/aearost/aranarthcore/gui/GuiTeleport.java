package com.aearost.aranarthcore.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;

public class GuiTeleport {

	private final Player player;
	private final Inventory initializedGui;

	public GuiTeleport(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player, 0);
	}
	
	public GuiTeleport(Player player, int pageNum) {
		this.player = player;
		this.initializedGui = initializeGui(player, pageNum);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player, int pageNum) {
		List<Home> homes = AranarthUtils.getHomes();
		int totalHomesOnPage = homes.size();
		int homeNumber = pageNum * 27;
		
		Inventory gui = Bukkit.getServer().createInventory(player, 36, "Teleport");

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

		for (int i = 0; i < 27; i++) {
			// If the current home being iterated is the last home in the list (none come after)
			if (totalHomesOnPage <= homeNumber) {
				gui.setItem(i, blank);
				continue;
			}
			Home home = homes.get(homeNumber);
			
			ItemStack homePad = new ItemStack(home.getIcon());
			ItemMeta homeMeta = homePad.getItemMeta();

			List<String> lore = new ArrayList<>();
			if (Objects.nonNull(home.getLocation().getWorld())) {
				lore.add(ChatUtils.translateToColor("&6world: &7" + home.getLocation().getWorld().getName()));
			}

			lore.add(ChatUtils.translateToColor("&6x: &7" + home.getLocation().getBlockX()));
			lore.add(ChatUtils.translateToColor("&6y: &7" + home.getLocation().getBlockY()));
			lore.add(ChatUtils.translateToColor("&6z: &7" + home.getLocation().getBlockZ()));

			if (Objects.nonNull(homeMeta)) {
				homeMeta.setDisplayName(ChatUtils.translateToColor(home.getHomeName()));
				homeMeta.setLore(lore);
			}
			homePad.setItemMeta(homeMeta);
			gui.setItem(i, homePad);
			homeNumber++;
		}

		return gui;
	}

}
