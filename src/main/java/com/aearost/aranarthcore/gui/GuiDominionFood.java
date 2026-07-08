package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiDominionFood {

	public static final int FOOD_SLOTS_PER_PAGE = 45;

	private final Player player;
	private final Inventory initializedGui;

	public GuiDominionFood(Player player) {
		this(player, 0);
	}

	public GuiDominionFood(Player player, int pageNum) {
		this.player = player;
		this.initializedGui = initializeGui(player, pageNum);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	public static int getTotalPages(int level) {
		if (level == 3) return 2;
		if (level == 4) return 3;
		if (level >= 5) return 5;
		return 1;
	}

	private Inventory initializeGui(Player player, int pageNum) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		int level = dominion.getDominionLevel();
		boolean multiPage = level >= 3;

		int inventorySize = multiPage ? 54 : DominionUtils.getFoodArraySize(dominion);
		int totalPages = getTotalPages(level);
		String title = multiPage
				? "&e" + dominion.getName() + "'s &rFood &r(" + (pageNum + 1) + "/" + totalPages + ")"
				: "&e" + dominion.getName() + "'s &rFood";
		Inventory gui = Bukkit.getServer().createInventory(player, inventorySize,
				ChatUtils.translateToColor(title));

		ItemStack[] food = dominion.getFood();
		int foodOffset = pageNum * FOOD_SLOTS_PER_PAGE;
		int slotsToFill = multiPage ? FOOD_SLOTS_PER_PAGE : DominionUtils.getFoodArraySize(dominion);
		for (int i = 0; i < slotsToFill && foodOffset + i < food.length; i++) {
			gui.setItem(i, food[foodOffset + i]);
		}

		if (multiPage) {

			ItemStack previous = new ItemStack(Material.RED_WOOL);
			ItemStack barrier = new ItemStack(Material.BARRIER);
			ItemStack next = new ItemStack(Material.LIME_WOOL);
			ItemStack blank = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

			ItemMeta prevMeta = previous.getItemMeta();
			if (prevMeta != null) {
				prevMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
				previous.setItemMeta(prevMeta);
			}
			ItemMeta barrierMeta = barrier.getItemMeta();
			if (barrierMeta != null) {
				barrierMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
				barrier.setItemMeta(barrierMeta);
			}
			ItemMeta nextMeta = next.getItemMeta();
			if (nextMeta != null) {
				nextMeta.setDisplayName(ChatUtils.translateToColor("&a&lNext"));
				next.setItemMeta(nextMeta);
			}
			ItemMeta blankMeta = blank.getItemMeta();
			if (blankMeta != null) {
				blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
				blank.setItemMeta(blankMeta);
			}

			gui.setItem(45, previous);
			gui.setItem(46, blank);
			gui.setItem(47, blank);
			gui.setItem(48, blank);
			gui.setItem(49, barrier);
			gui.setItem(50, blank);
			gui.setItem(51, blank);
			gui.setItem(52, blank);
			gui.setItem(53, next);
		}
		return gui;
	}

}
