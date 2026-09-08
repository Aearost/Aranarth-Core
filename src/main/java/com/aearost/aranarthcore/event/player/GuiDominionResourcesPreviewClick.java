package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDominionResources;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class GuiDominionResourcesPreviewClick {

	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);

		if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
			return;
		}

		int slot = e.getSlot();
		int invSize = e.getClickedInventory().getSize();

		// Only act on bottom row clicks (back button lives there)
		if (slot < invSize - 9) {
			return;
		}

		ItemStack clickedItem = e.getClickedInventory().getItem(slot);
		if (clickedItem == null || !clickedItem.hasItemMeta()) {
			return;
		}

		String displayName = ChatUtils.stripColorFormatting(clickedItem.getItemMeta().getDisplayName());
		if (displayName.equals("Back")) {
			Player player = (Player) e.getWhoClicked();
			int page = GuiDominionResources.playerPage.getOrDefault(player.getUniqueId(), 0);
			new GuiDominionResources(player, page).openGui();
		}
	}
}
