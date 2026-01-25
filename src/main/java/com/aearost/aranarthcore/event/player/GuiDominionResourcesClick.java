package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

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
			String biomeNameUnformatted = clickedItem.getItemMeta().getDisplayName();
			String biomeNameFormatted = biomeNameUnformatted.replaceAll(" ", "_");
			biomeNameFormatted = "minecraft:" + biomeNameFormatted.toLowerCase();
			Biome biome = Registry.BIOME.get(NamespacedKey.fromString(biomeNameFormatted));

			List<ItemStack> resourcesToClaim = DominionUtils.getResourcesByDominionAndBiome(dominion, biome);
			Location loc = player.getLocation();
			for (ItemStack resource : resourcesToClaim) {
				HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(resource);
				if (!remainder.isEmpty()) {
					loc.getWorld().dropItemNaturally(loc, remainder.get(0));
				}
			}
			dominion.setClaimableResources(dominion.getClaimableResources() - 1);
			DominionUtils.updateDominion(dominion);
			player.sendMessage(ChatUtils.chatMessage("&7You have claimed resources from the &e" + biomeNameUnformatted + " &7biome"));

			player.closeInventory();
		}
	}
}
