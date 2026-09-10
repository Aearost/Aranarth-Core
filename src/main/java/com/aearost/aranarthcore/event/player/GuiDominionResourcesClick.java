package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionResources;
import com.aearost.aranarthcore.gui.GuiDominionResourcesPreview;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles preventing the items from being added to and from the Dominion Resources inventory.
 */
public class GuiDominionResourcesClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);

		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
			return;
		}

		Player player = (Player) e.getWhoClicked();
		int slot = e.getSlot();
		int invSize = e.getClickedInventory().getSize();

		// Ignore top filler row
		if (slot < 9) {
			return;
		}

		// Bottom row - filler or pagination buttons
		if (slot >= invSize - 9) {
			ItemStack navItem = e.getClickedInventory().getItem(slot);
			if (navItem == null || !navItem.hasItemMeta()) {
				return;
			}
			String navName = ChatUtils.stripColorFormatting(navItem.getItemMeta().getDisplayName());
			int currentPage = GuiDominionResources.playerPage.getOrDefault(player.getUniqueId(), 0);
			if (navName.equals("Previous Page")) {
				new GuiDominionResources(player, currentPage - 1).openGui();
			} else if (navName.equals("Next Page")) {
				new GuiDominionResources(player, currentPage + 1).openGui();
			}
			return;
		}

		// Content area - biome items
		ItemStack clickedItem = e.getClickedInventory().getItem(slot);
		if (clickedItem == null || !clickedItem.hasItemMeta()) {
			return;
		}
		String biomeNameUnformatted = ChatUtils.stripColorFormatting(clickedItem.getItemMeta().getDisplayName());
		String biomeNameFormatted = "minecraft:" + biomeNameUnformatted.replaceAll(" ", "_").toLowerCase();
		Biome biome = Registry.BIOME.get(NamespacedKey.fromString(biomeNameFormatted));
		if (biome == null) {
			return;
		}

		if (e.getClick() == ClickType.RIGHT) {
			new GuiDominionResourcesPreview(player, biome).openGui();
			return;
		}

		if (e.getClick() != ClickType.LEFT) {
			return;
		}

		// Left-click - begin claim flow
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		dominion.setBiomeResourcesBeingClaimed(biome);
		DominionUtils.updateDominion(dominion);

		player.sendMessage(ChatUtils.chatMessage("&7Enter the number of claims for the &e" + biomeNameUnformatted + " &7biome"));
		player.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &e"
				+ dominion.getClaimableResources() + "/" + DominionUtils.getMaxClaimableResourcesAmount(dominion)
				+ " &7available"));
		// Show average claim yield across all pending slots
		List<Double> yields = dominion.getClaimFoodYields();
		int claimable = dominion.getClaimableResources();
		double avgYield;
		if (claimable == 0) {
			avgYield = DominionUtils.foodPercentageToYield(DominionUtils.getBaseFoodPercentage(dominion));
		} else {
			double sum = yields.stream().mapToDouble(Double::doubleValue).sum();
			int missing = Math.max(0, claimable - yields.size());
			avgYield = (sum + missing) / claimable;
		}
		int avgYieldPct = (int) Math.round(avgYield * 100);
		String yieldColor = avgYieldPct >= 80 ? "&a" : avgYieldPct >= 50 ? "&e" : "&c";
		player.sendMessage(ChatUtils.chatMessage("&7Average claim yield - " + yieldColor + avgYieldPct + "%"));
		player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
		player.closeInventory();
	}
}
