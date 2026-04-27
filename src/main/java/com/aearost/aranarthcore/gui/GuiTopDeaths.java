package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.PlayerKillDeathScore;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GuiTopDeaths {

	private final Player player;
	private final Inventory initializedGui;

	public GuiTopDeaths(Player player, int pageNum) {
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
		HashMap<UUID, List<PlayerKillDeathScore>> scores = AranarthUtils.getKillDeathScores();
		int startIndex = pageNum * 45;
		Inventory gui = null;
		gui = Bukkit.getServer().createInventory(player, 54, "Top Deaths");

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
		gui.setItem(45, previous);
		gui.setItem(46, blank);
		gui.setItem(47, blank);
		gui.setItem(48, blank);
		gui.setItem(49, barrier);
		gui.setItem(50, blank);
		gui.setItem(51, blank);
		gui.setItem(52, blank);
		gui.setItem(53, next);

		List<UUID> uuidList = AranarthUtils.getTopDeaths(player.getWorld());

		for (int i = 0; i < 45; i++) {
			// If the current UUID being iterated is the last in the list (none come after)
			if (i >= uuidList.size()) {
				gui.setItem(i, blank);
				continue;
			}

			UUID uuid = uuidList.get(startIndex + i);
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
			skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
			skullMeta.setDisplayName(ChatUtils.translateToColor("&e" + aranarthPlayer.getNickname()));
			List<String> lore = new ArrayList<>();
			int deathCount = AranarthUtils.getKillsOrDeathsInWorld(uuid, player.getWorld(), false);
			lore.add(ChatUtils.translateToColor("&e" + deathCount + " deaths"));
			skullMeta.setLore(lore);
			head.setItemMeta(skullMeta);
			gui.setItem(i, head);
		}
		return gui;
	}

}
