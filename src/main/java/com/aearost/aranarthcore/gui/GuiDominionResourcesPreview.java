package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiDominionResourcesPreview {

	public static final String TITLE_SUFFIX = " Resources";

	private final Player player;
	private final Inventory initializedGui;

	public GuiDominionResourcesPreview(Player player, Biome biome) {
		this.player = player;
		this.initializedGui = initializeGui(player, biome);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGui(Player player, Biome biome) {
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
		String biomeName = DominionUtils.getBiomeName(biome);
		DominionUtils.BiomeTable table = DominionUtils.buildBiomeTable(dominion, biome);

		// Build the flat display list
		List<ItemStack> displayItems = new ArrayList<>();

		// Guaranteed drops (including previewOnly variety drops)
		for (DominionUtils.ResourceDrop drop : table.guaranteed) {
			displayItems.add(buildPreviewItem(drop.item, buildLore(null, drop.lore)));
		}

		// Variety group options (all shown with their lore)
		for (DominionUtils.VarietyGroup variety : table.varieties) {
			for (DominionUtils.ResourceDrop option : variety.options) {
				displayItems.add(buildPreviewItem(option.item, buildLore(null, option.lore)));
			}
		}

		// Simulation drops - all always shown with sim context lore
		for (DominionUtils.SimulationGroup sim : table.simulations) {
			String simLore = sim.name + " Simulation (" + DominionUtils.formatPct(sim.odds) + " chance)";
			for (DominionUtils.ResourceDrop drop : sim.drops) {
				displayItems.add(buildPreviewItem(drop.item, buildLore(simLore, drop.lore)));
			}
		}

		// Rare drops
		for (DominionUtils.ResourceDrop drop : table.rares) {
			displayItems.add(buildPreviewItem(drop.item, buildLore(null, drop.lore)));
		}

		// Size calculation
		int contentSlots = Math.min(displayItems.size(), 36);
		int contentRows = Math.max(1, (int) Math.ceil((double) contentSlots / 9.0));
		int size = contentRows * 9 + 18;
		int backSlot = size - 5;

		Inventory gui = Bukkit.getServer().createInventory(player, size, biomeName + TITLE_SUFFIX);

		ItemStack filler = makeFiller();
		for (int i = 0; i < 9; i++) { gui.setItem(i, filler); }
		for (int i = size - 9; i < size; i++) { gui.setItem(i, filler); }

		for (int i = 0; i < displayItems.size() && i < size - 18; i++) {
			gui.setItem(9 + i, displayItems.get(i));
		}

		gui.setItem(backSlot, buildBackButton());

		return gui;
	}

	// Builds a list of color-translated lore lines. simLine is the first line (or null). subLine is the second (or null).
	private List<String> buildLore(String simLine, String subLine) {
		List<String> lore = new ArrayList<>();
		if (simLine != null) lore.add(ChatUtils.translateToColor("&7&o" + simLine));
		if (subLine != null) lore.add(ChatUtils.translateToColor("&7&o" + subLine));
		return lore;
	}

	// Clones the item and applies lore if non-empty
	private ItemStack buildPreviewItem(ItemStack source, List<String> lore) {
		ItemStack display = source.clone();
		if (!lore.isEmpty()) {
			ItemMeta meta = display.getItemMeta();
			if (meta != null) {
				meta.setLore(lore);
				display.setItemMeta(meta);
			}
		}
		return display;
	}

	private ItemStack makeFiller() {
		ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = pane.getItemMeta();
		meta.setDisplayName(" ");
		pane.setItemMeta(meta);
		return pane;
	}

	private ItemStack buildBackButton() {
		ItemStack item = new ItemStack(Material.BARRIER);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&7Back"));
		item.setItemMeta(meta);
		return item;
	}
}
