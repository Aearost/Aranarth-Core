package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiCompressor;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles the page changing logic of the server store.
 */
public class GuiCompressorClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);

		if (e.getClickedInventory() == null) {
			return;
		}

		Player player = (Player) e.getWhoClicked();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		int slot = e.getSlot();
		if (slot < 9 || slot >= 36) {
			if (slot == 40) {
				player.closeInventory();
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 0.8F);
			}
			return;
		}

		Material material = e.getClickedInventory().getItem(slot).getType();
		boolean isCompressing = AranarthUtils.isItemBeingCompressed(player.getUniqueId(), material);

		// Make it no longer be compressible
		if (isCompressing) {
			AranarthUtils.removeCompressibleItem(player.getUniqueId(), material);
		}
		// Make it compressible
		else {
			AranarthUtils.addCompressibleItem(player.getUniqueId(), material);
		}
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1F, 0.8F);
		GuiCompressor gui = new GuiCompressor(player);
		gui.openGui();
	}
}
