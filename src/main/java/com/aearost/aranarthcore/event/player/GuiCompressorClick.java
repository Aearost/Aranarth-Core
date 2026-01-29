package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiCompressor;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Handles the page changing logic of the server store.
 */
public class GuiCompressorClick {
	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);

		if (e.getClickedInventory() == null) {
			return;
		}

		if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
			return;
		}

		Player player = (Player) e.getWhoClicked();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		int slot = e.getSlot();
		if (slot < 9 || slot >= 35) {
			// Toggle compressor
			if (slot == 4) {
				player.closeInventory();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
				if (aranarthPlayer.isCompressingItems()) {
					aranarthPlayer.setCompressingItems(false);
					player.sendMessage(ChatUtils.chatMessage("&7You are no longer compressing items"));
				} else {
					aranarthPlayer.setCompressingItems(true);
					player.sendMessage(ChatUtils.chatMessage("&7You are now compressing items"));
				}
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			}
			// Exit
			else if (slot == 40) {
				player.closeInventory();
				player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
			}
			// Enable All
			else if (slot == 36) {
				AranarthUtils.compressAllMaterials(player.getUniqueId());
				player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
				GuiCompressor gui = new GuiCompressor(player);
				gui.openGui();
			}
			// Disable All
			else if (slot == 44) {
				AranarthUtils.stopCompressingAllMaterials(player.getUniqueId());
				player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 0.5F, 1.75F);
				GuiCompressor gui = new GuiCompressor(player);
				gui.openGui();
			}
			return;
		}

		if (!AranarthUtils.isCompressible(e.getClickedInventory().getItem(slot), false)) {
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
		player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
		GuiCompressor gui = new GuiCompressor(player);
		gui.openGui();
	}
}
