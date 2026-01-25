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
			String biomeNameUnformatted = clickedItem.getItemMeta().getDisplayName();
			String biomeNameFormatted = biomeNameUnformatted.replaceAll(" ", "_");
			biomeNameFormatted = "minecraft:" + biomeNameFormatted.toLowerCase();
			Biome biome = Registry.BIOME.get(NamespacedKey.fromString(biomeNameFormatted));

			dominion.setBiomeResourcesBeingClaimed(biome);
			DominionUtils.updateDominion(dominion);

			player.sendMessage(ChatUtils.chatMessage("&7Enter the number of claims for the &e" + biomeNameUnformatted + " &7biome"));
			player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
			player.closeInventory();
		}
	}
}
