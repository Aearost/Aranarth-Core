package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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

		NamespacedKey randomKeyTag = new NamespacedKey(AranarthCore.getInstance(), "random_key");
		if (item.getItemMeta().getPersistentDataContainer().has(randomKeyTag, PersistentDataType.STRING)) {
			int[] randomKeyIndex = {1};
			int[] taskId = {-1};
			taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), () -> {
				if (player.getOpenInventory() != null
						&& ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals("Vote Shop Purchase")) {
					updateRandomKeyItem(randomKeyIndex[0]);
					randomKeyIndex[0] = (randomKeyIndex[0] + 1) % 4;
				} else {
					Bukkit.getScheduler().cancelTask(taskId[0]);
				}
			}, 20, 20);
		}
	}

	private void updateRandomKeyItem(int index) {
		NamespacedKey randomKeyNSKey = new NamespacedKey(AranarthCore.getInstance(), "random_key");
		ItemStack randomKey;
		switch (index) {
			case 1 -> randomKey = new KeyRare().getItem();
			case 2 -> randomKey = new KeyEpic().getItem();
			case 3 -> randomKey = new KeyGodly().getItem();
			default -> randomKey = new KeyVote().getItem();
		}
		ItemMeta meta = randomKey.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&a&lPurchase &4&lRandom Crate Key"));
		List<String> lore = new ArrayList<>();
		lore.add(ChatUtils.translateToColor("&e10 vote points"));
		lore.add(ChatUtils.translateToColor("&a&oVote &7&o- 65%"));
		lore.add(ChatUtils.translateToColor("&6&oRare &7&o- 25%"));
		lore.add(ChatUtils.translateToColor("&3&oEpic &7&o- 7%"));
		lore.add(ChatUtils.translateToColor("&5&oGodly &7&o- 3%"));
		meta.getPersistentDataContainer().set(randomKeyNSKey, PersistentDataType.STRING, "true");
		meta.setLore(lore);
		randomKey.setItemMeta(meta);
		initializedGui.setItem(14, randomKey);
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
