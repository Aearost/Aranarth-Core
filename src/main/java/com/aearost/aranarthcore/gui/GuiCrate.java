package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.crates.KeyRare;
import com.aearost.aranarthcore.objects.CrateType;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiCrate {

	private final Player player;
	private final Inventory initializedGui;

	public GuiCrate(Player player, CrateType type) {
		this.player = player;
//
//		if (type == CrateType.RARE) {
//			this.initializedGui = initializeRareCrate(player);
//		} else if (type == CrateType.EPIC) {
//			this.initializedGui = initializeEpicCrate(player);
//		} else if (type == CrateType.GODLY) {
//			this.initializedGui = initializeGodlyCrate(player);
//		} else {
			this.initializedGui = initializeVoteCrate(player);
//		}
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	/**
	 * Initializes the GUI of the Vote Crate.
	 * @param player The player viewing the contents of a Vote Crate.
	 * @return The initialized inventory of the Vote Crate.
	 */
	private Inventory initializeVoteCrate(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Crate - Vote");

		ItemStack money50 = new ItemStack(Material.GOLD_INGOT);
		ItemMeta money50Meta = money50.getItemMeta();
		money50Meta.setDisplayName(ChatUtils.translateToColor("&6&l$50 In-Game Money"));
		List<String> money50Lore = new ArrayList<>();
		money50Lore.add(ChatUtils.translateToColor("&a12% Chance"));
		money50Meta.setLore(money50Lore);
		money50.setItemMeta(money50Meta);
		gui.setItem(2, money50);

		ItemStack bread = new ItemStack(Material.BREAD, 16);
		ItemMeta breadMeta = bread.getItemMeta();
		breadMeta.setDisplayName(ChatUtils.translateToColor("#ba8727&lBread"));
		List<String> breadLore = new ArrayList<>();
		breadLore.add(ChatUtils.translateToColor("&a12% Chance"));
		breadMeta.setLore(breadLore);
		bread.setItemMeta(breadMeta);
		gui.setItem(3, bread);

		ItemStack iron = new ItemStack(Material.IRON_INGOT, 16);
		ItemMeta ironMeta = iron.getItemMeta();
		ironMeta.setDisplayName(ChatUtils.translateToColor("#eeeeee&lIron Ingot"));
		List<String> ironLore = new ArrayList<>();
		ironLore.add(ChatUtils.translateToColor("&a12% Chance"));
		ironMeta.setLore(ironLore);
		iron.setItemMeta(ironMeta);
		gui.setItem(5, iron);

		ItemStack gold = new ItemStack(Material.GOLD_INGOT, 16);
		ItemMeta goldMeta = gold.getItemMeta();
		goldMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lGold Ingot"));
		List<String> goldLore = new ArrayList<>();
		goldLore.add(ChatUtils.translateToColor("&a12% Chance"));
		goldMeta.setLore(goldLore);
		gold.setItemMeta(goldMeta);
		gui.setItem(6, gold);

		ItemStack diamond = new ItemStack(Material.DIAMOND, 4);
		ItemMeta diamondMeta = diamond.getItemMeta();
		diamondMeta.setDisplayName(ChatUtils.translateToColor("#a0f0ed&lDiamond"));
		List<String> diamondLore = new ArrayList<>();
		diamondLore.add(ChatUtils.translateToColor("&e8% Chance"));
		diamondMeta.setLore(diamondLore);
		diamond.setItemMeta(diamondMeta);
		gui.setItem(10, diamond);

		ItemStack exp = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
		ItemMeta expMeta = exp.getItemMeta();
		expMeta.setDisplayName(ChatUtils.translateToColor("#c1e377&lBottle o' Enchanting"));
		List<String> expLore = new ArrayList<>();
		expLore.add(ChatUtils.translateToColor("&e8% Chance"));
		expMeta.setLore(expLore);
		exp.setItemMeta(expMeta);
		gui.setItem(11, exp);

		ItemStack godAppleFragment = new GodAppleFragment().getItem();
		godAppleFragment.setAmount(4);
		ItemMeta godAppleFragmentMeta = godAppleFragment.getItemMeta();
		godAppleFragmentMeta.setDisplayName(ChatUtils.translateToColor("&6&lGod Apple Fragment"));
		List<String> godAppleFragmentLore = new ArrayList<>();
		godAppleFragmentLore.add(ChatUtils.translateToColor("&e8% Chance"));
		godAppleFragmentMeta.setLore(godAppleFragmentLore);
		godAppleFragment.setItemMeta(godAppleFragmentMeta);
		gui.setItem(15, godAppleFragment);

		ItemStack emerald = new ItemStack(Material.EMERALD, 8);
		ItemMeta emeraldMeta = emerald.getItemMeta();
		emeraldMeta.setDisplayName(ChatUtils.translateToColor("#50c878&lEmerald"));
		List<String> emeraldLore = new ArrayList<>();
		emeraldLore.add(ChatUtils.translateToColor("&e8% Chance"));
		emeraldMeta.setLore(emeraldLore);
		emerald.setItemMeta(emeraldMeta);
		gui.setItem(16, emerald);

		ItemStack trialKey = new ItemStack(Material.TRIAL_KEY, 1);
		ItemMeta trialKeyMeta = trialKey.getItemMeta();
		trialKeyMeta.setDisplayName(ChatUtils.translateToColor("#515950&lTrial Key"));
		List<String> trialKeyLore = new ArrayList<>();
		trialKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		trialKeyMeta.setLore(trialKeyLore);
		trialKey.setItemMeta(trialKeyMeta);
		gui.setItem(20, trialKey);

		ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD, 8);
		ItemMeta blazeRodMeta = blazeRod.getItemMeta();
		blazeRodMeta.setDisplayName(ChatUtils.translateToColor("#fcbf00&lBlaze Rod"));
		List<String> blazeRodLore = new ArrayList<>();
		blazeRodLore.add(ChatUtils.translateToColor("&c5% Chance"));
		blazeRodMeta.setLore(blazeRodLore);
		blazeRod.setItemMeta(blazeRodMeta);
		gui.setItem(21, blazeRod);

		ItemStack breezeRod = new ItemStack(Material.BREEZE_ROD, 8);
		ItemMeta breezeRodMeta = breezeRod.getItemMeta();
		breezeRodMeta.setDisplayName(ChatUtils.translateToColor("#bdadc7&lBreeze Rod"));
		List<String> breezeRodLore = new ArrayList<>();
		breezeRodLore.add(ChatUtils.translateToColor("&c5% Chance"));
		breezeRodMeta.setLore(breezeRodLore);
		breezeRod.setItemMeta(breezeRodMeta);
		gui.setItem(23, breezeRod);

		ItemStack rareKey = new KeyRare().getItem();
		ItemMeta rareKeyMeta = rareKey.getItemMeta();
		rareKeyMeta.setDisplayName(ChatUtils.translateToColor("&6&lRare Crate Key"));
		List<String> rareKeyLore = new ArrayList<>();
		rareKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		rareKeyMeta.setLore(rareKeyLore);
		rareKey.setItemMeta(rareKeyMeta);
		gui.setItem(24, rareKey);

		// Fill empty slots with the blank item
		ItemStack blank = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta blankMeta = blank.getItemMeta();
		blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
		blank.setItemMeta(blankMeta);
		for (int i = 0; i < gui.getSize(); i++) {
			if (gui.getItem(i) == null) {
				gui.setItem(i, blank);
			}
		}
		return gui;
	}


}
