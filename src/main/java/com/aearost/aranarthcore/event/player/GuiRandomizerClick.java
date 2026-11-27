package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiRandomizer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomizerPreset;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.RandomizerUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Deals with all clicks of the /randomizer GUI.
 */
public class GuiRandomizerClick {

	public void execute(InventoryClickEvent e) {
		e.setCancelled(true);
		if (!(e.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (e.getClickedInventory() == null || e.getClickedInventory().getType() == InventoryType.PLAYER) {
			return;
		}
		if (!player.hasPermission("aranarth.randomizer")) {
			player.closeInventory();
			return;
		}

		int slot = e.getSlot();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		if (slot == GuiRandomizer.TOGGLE_SLOT) {
			RandomizerUtils.toggle(player);
			player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
			GuiRandomizer.populate(player.getOpenInventory().getTopInventory(), player);
			return;
		}

		if (slot == GuiRandomizer.SAVE_SLOT) {
			player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
			RandomizerUtils.promptPresetName(player);
			return;
		}

		if (slot == GuiRandomizer.EXIT_SLOT) {
			player.closeInventory();
			player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
			return;
		}

		int presetIndex = GuiRandomizer.getPresetIndex(aranarthPlayer, slot);
		if (presetIndex < 0) {
			return;
		}
		RandomizerPreset preset = aranarthPlayer.getRandomizerPresets().get(presetIndex);
		if (e.getClick() == ClickType.SHIFT_RIGHT) {
			RandomizerUtils.deletePreset(player, preset.getName());
		} else if (e.getClick() == ClickType.LEFT) {
			RandomizerUtils.bindPreset(player, preset.getName());
		} else {
			return;
		}
		player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
		GuiRandomizer.populate(player.getOpenInventory().getTopInventory(), player);
	}

}
