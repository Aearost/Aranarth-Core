package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionLevelUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiDominionFood {

	public static final int FOOD_SLOTS_PER_PAGE = 45;
	public static final String TITLE_PREFIX = "Power - ";

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

	/**
	 * Updates an already-open food inventory in-place for the given page, without closing it.
	 * The caller is responsible for saving the current page's items to the dominion before calling this.
	 */
	public static void populatePage(org.bukkit.inventory.Inventory inv, com.aearost.aranarthcore.objects.Dominion dominion, int pageNum) {
		org.bukkit.inventory.ItemStack[] food = dominion.getFood();
		int foodOffset = pageNum * FOOD_SLOTS_PER_PAGE;

		// Clear food slots
		for (int i = 0; i < FOOD_SLOTS_PER_PAGE; i++) {
			inv.setItem(i, foodOffset + i < food.length ? food[foodOffset + i] : null);
		}
	}

	/**
	 * Builds the inventory title string including the current food power, max capacity, and estimated duration.
	 */
	public static String buildFoodTitle(Dominion dominion, int totalPower) {
		int amplifier = dominion.getConquered().size();
		if (amplifier == 0 && DominionUtils.getConquerorOfDominion(dominion) != null) {
			amplifier = -1;
		}
		int maxPower = DominionUtils.getClaimFoodPower(Material.ENCHANTED_GOLDEN_APPLE, amplifier) * 64 * DominionUtils.getFoodArraySize(dominion);
		int dailyFoodPower = DominionLevelUtils.getDailyFoodPower(dominion.getDominionLevel());
		String duration = formatDuration(totalPower, dailyFoodPower);
		double percentage = maxPower > 0 ? (totalPower * 100.0 / maxPower) : 0.0;
		String percentageStr = percentage == Math.floor(percentage)
				? String.valueOf((int) percentage)
				: String.format("%.2f", percentage).replaceAll("0+$", "");
		return "Power - " + percentageStr + "% (" + duration + ")";
	}

	/**
	 * Formats the estimated duration that the given food power will last based on daily consumption.
	 */
	public static String formatDuration(int totalPower, int dailyFoodPower) {
		if (dailyFoodPower <= 0) return "0d";
		long totalMinutes = (long) totalPower * 24 * 60 / dailyFoodPower;
		long days = totalMinutes / (24 * 60);
		long hours = (totalMinutes % (24 * 60)) / 60;
		long minutes = totalMinutes % 60;
		return days + "d " + hours + "h " + minutes + "m";
	}

	/**
	 * Calculates the total food power from the currently open inventory combined with stored pages in the dominion.
	 */
	public static int calculatePowerFromOpenGui(Dominion dominion, Inventory openTop, int currentPage) {
		int amplifier = dominion.getConquered().size();
		if (amplifier == 0 && DominionUtils.getConquerorOfDominion(dominion) != null) {
			amplifier = -1;
		}

		int totalPower = 0;
		boolean multiPage = dominion.getDominionLevel() >= 3;
		int foodSlotsInView = multiPage ? FOOD_SLOTS_PER_PAGE : openTop.getSize();

		// Power from the current page shown in the open inventory
		for (int i = 0; i < foodSlotsInView; i++) {
			ItemStack item = openTop.getItem(i);
			if (item != null && item.getType() != Material.AIR) {
				totalPower += DominionUtils.getClaimFoodPower(item.getType(), amplifier) * item.getAmount();
			}
		}

		// Power from other pages stored in the dominion
		if (multiPage) {
			ItemStack[] storedFood = dominion.getFood();
			int totalPages = getTotalPages(dominion.getDominionLevel());
			for (int page = 0; page < totalPages; page++) {
				if (page == currentPage) continue;
				int offset = page * FOOD_SLOTS_PER_PAGE;
				for (int i = 0; i < FOOD_SLOTS_PER_PAGE && offset + i < storedFood.length; i++) {
					ItemStack food = storedFood[offset + i];
					if (food != null && food.getType() != Material.AIR) {
						totalPower += DominionUtils.getClaimFoodPower(food.getType(), amplifier) * food.getAmount();
					}
				}
			}
		}

		return totalPower;
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
		int totalPower = DominionUtils.getTotalFoodPower(dominion);
		Inventory gui = Bukkit.getServer().createInventory(player, inventorySize, buildFoodTitle(dominion, totalPower));

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
