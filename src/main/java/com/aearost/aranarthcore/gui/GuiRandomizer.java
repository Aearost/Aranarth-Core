package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomizerPreset;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.RandomizerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The /randomizer GUI, to toggle the randomizer and use, save, or delete presets.
 */
public class GuiRandomizer {

	public static final String TITLE_KEY = "gui.randomizer.title";
	public static final int TOGGLE_SLOT = 4;
	public static final int FIRST_PRESET_SLOT = 9;
	public static final int SAVE_SLOT = 20;
	public static final int EXIT_SLOT = 22;

	private final Player player;
	private final Inventory gui;

	public GuiRandomizer(Player player) {
		this.player = player;
		this.gui = GuiHolder.create(player, 27, Lang.getFor(player, TITLE_KEY));
		populate(gui, player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(gui);
	}

	/**
	 * Fills the GUI, also used to refresh an already-open GUI in-place.
	 */
	public static void populate(Inventory inv, Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta blankMeta = blank.getItemMeta();
		blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
		blank.setItemMeta(blankMeta);
		for (int i = 0; i < 27; i++) {
			if (i < FIRST_PRESET_SLOT || i >= FIRST_PRESET_SLOT + RandomizerUtils.MAX_PRESETS) {
				inv.setItem(i, blank);
			}
		}

		inv.setItem(TOGGLE_SLOT, buildToggleItem(player, aranarthPlayer));

		String currentPattern = RandomizerUtils.formatPattern(aranarthPlayer.getRandomItems());
		List<RandomizerPreset> presets = aranarthPlayer.getRandomizerPresets();
		for (int i = 0; i < RandomizerUtils.MAX_PRESETS; i++) {
			if (i < presets.size()) {
				inv.setItem(FIRST_PRESET_SLOT + i, buildPresetItem(player, presets.get(i), currentPattern));
			} else {
				ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
				ItemMeta emptyMeta = empty.getItemMeta();
				emptyMeta.setDisplayName(Lang.getFor(player, "gui.randomizer.empty_slot"));
				empty.setItemMeta(emptyMeta);
				inv.setItem(FIRST_PRESET_SLOT + i, empty);
			}
		}

		ItemStack save = new ItemStack(Material.WRITABLE_BOOK);
		ItemMeta saveMeta = save.getItemMeta();
		saveMeta.setDisplayName(Lang.getFor(player, "gui.randomizer.save_current"));
		saveMeta.setLore(buildCurrentPatternLore(player, currentPattern));
		save.setItemMeta(saveMeta);
		inv.setItem(SAVE_SLOT, save);

		ItemStack exit = new ItemStack(Material.BARRIER);
		ItemMeta exitMeta = exit.getItemMeta();
		exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
		exit.setItemMeta(exitMeta);
		inv.setItem(EXIT_SLOT, exit);
	}

	/**
	 * Lists each item of the player's current pattern, or says there is no pattern set.
	 */
	private static List<String> buildCurrentPatternLore(Player player, String currentPattern) {
		List<String> lore = RandomizerUtils.getPatternLore(player, currentPattern);
		if (lore.isEmpty()) {
			lore.add(Lang.getFor(player, "gui.randomizer.no_pattern"));
		}
		return lore;
	}

	private static ItemStack buildToggleItem(Player player, AranarthPlayer aranarthPlayer) {
		boolean isOn = aranarthPlayer.isRandomizing();
		ItemStack toggled = new ItemStack(isOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
		ItemMeta toggledMeta = toggled.getItemMeta();
		toggledMeta.setDisplayName(Lang.getFor(player, isOn ? "gui.randomizer.on" : "gui.randomizer.off"));
		String currentPattern = RandomizerUtils.formatPattern(aranarthPlayer.getRandomItems());
		toggledMeta.setLore(buildCurrentPatternLore(player, currentPattern));
		toggled.setItemMeta(toggledMeta);
		return toggled;
	}

	private static ItemStack buildPresetItem(Player player, RandomizerPreset preset, String currentPattern) {
		boolean isActive = preset.getPattern().equals(currentPattern);
		ItemStack item = new ItemStack(isActive ? Material.MAP : Material.PAPER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&e&l" + preset.getName()));
		List<String> lore = new ArrayList<>();
		lore.addAll(RandomizerUtils.getPatternLore(player, preset.getPattern()));
		if (isActive) {
			lore.add(Lang.getFor(player, "gui.blacklist.active_preset"));
		}
		lore.add("");
		lore.add(Lang.getFor(player, "gui.randomizer.bind_hint"));
		lore.add(Lang.getFor(player, "gui.randomizer.delete_hint"));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Gets the preset shown in the given slot.
	 *
	 * @return The preset's index, or -1 if the slot is not a filled preset slot.
	 */
	public static int getPresetIndex(AranarthPlayer aranarthPlayer, int slot) {
		int index = slot - FIRST_PRESET_SLOT;
		if (index < 0 || index >= RandomizerUtils.MAX_PRESETS || index >= aranarthPlayer.getRandomizerPresets().size()) {
			return -1;
		}
		return index;
	}

}
