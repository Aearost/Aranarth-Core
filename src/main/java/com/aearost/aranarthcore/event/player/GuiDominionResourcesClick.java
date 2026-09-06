package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Handles preventing the items from being added to and from the Dominion Resources inventory.
 */
public class GuiDominionResourcesClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);

		if (e.getClickedInventory().getType() == InventoryType.CHEST) {
			Player player = (Player) e.getWhoClicked();
			Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());

			// Obtains the Biome object from the name of the item
			ItemStack clickedItem = e.getClickedInventory().getItem(e.getSlot());
			if (clickedItem == null || !clickedItem.hasItemMeta()) {
				return;
			}
			String biomeNameUnformatted = clickedItem.getItemMeta().getDisplayName();
			String biomeNameFormatted = biomeNameUnformatted.replaceAll(" ", "_");
			biomeNameFormatted = "minecraft:" + biomeNameFormatted.toLowerCase();
			Biome biome = Registry.BIOME.get(NamespacedKey.fromString(biomeNameFormatted));

			dominion.setBiomeResourcesBeingClaimed(biome);
			DominionUtils.updateDominion(dominion);

			player.sendMessage(ChatUtils.chatMessage("&7Enter the number of claims for the &e" + biomeNameUnformatted + " &7biome"));
			player.sendMessage(ChatUtils.chatMessage("&7Your Dominion has &e"
					+ dominion.getClaimableResources() + "/" + DominionUtils.getMaxClaimableResourcesAmount(dominion)
					+ " &7available"));
			// Show average claim yield across all pending slots
			java.util.List<Double> yields = dominion.getClaimFoodYields();
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
}
