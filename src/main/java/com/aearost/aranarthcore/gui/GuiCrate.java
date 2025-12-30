package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.items.crates.KeyEpic;
import com.aearost.aranarthcore.items.crates.KeyGodly;
import com.aearost.aranarthcore.items.crates.KeyRare;
import com.aearost.aranarthcore.objects.CrateType;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiCrate {

	private final Player player;
	private final Inventory initializedGui;

	public GuiCrate(Player player, CrateType type, List<Integer> indexes) {
		this.player = player;

		if (type == CrateType.RARE) {
			this.initializedGui = initializeRareCrate(player);
			// Updating the cluster item here to allow for dynamic updates
			updateRareCrateItems(indexes.get(0));
		} else if (type == CrateType.EPIC) {
			this.initializedGui = initializeEpicCrate(player);
			// Updating the cluster and armor trim item here to allow for dynamic updates
			updateEpicCrateItems(indexes.get(0), indexes.get(1));
		} else if (type == CrateType.GODLY) {
			this.initializedGui = initializeGodlyCrate(player);
			// Updating the cluster item here to allow for dynamic updates
			updateGodlyCrateItems(indexes.get(0));
		} else {
			this.initializedGui = initializeVoteCrate(player);
		}
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
		money50Meta.setDisplayName(ChatUtils.translateToColor("&6&l$50 of In-Game Currency"));
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

	/**
	 * Initializes the GUI of the Rare Crate.
	 * @param player The player viewing the contents of a Rare Crate.
	 * @return The initialized inventory of the Rare Crate.
	 */
	private Inventory initializeRareCrate(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Crate - Rare");

		ItemStack money250 = new ItemStack(Material.GOLD_INGOT);
		ItemMeta money250Meta = money250.getItemMeta();
		money250Meta.setDisplayName(ChatUtils.translateToColor("&6&l$250 of In-Game Currency"));
		List<String> money250Lore = new ArrayList<>();
		money250Lore.add(ChatUtils.translateToColor("&a12% Chance"));
		money250Meta.setLore(money250Lore);
		money250.setItemMeta(money250Meta);
		gui.setItem(2, money250);

		ItemStack mending = new ItemStack(Material.ENCHANTED_BOOK, 16);
		EnchantmentStorageMeta mendingMeta = (EnchantmentStorageMeta) mending.getItemMeta();
		mendingMeta.addStoredEnchant(Enchantment.MENDING, 0, true);
		mendingMeta.setDisplayName(ChatUtils.translateToColor("#9f1c43&lMending Book"));
		List<String> mendingLore = new ArrayList<>();
		mendingLore.add(ChatUtils.translateToColor("&a12% Chance"));
		mendingMeta.setLore(mendingLore);
		mending.setItemMeta(mendingMeta);
		gui.setItem(3, mending);

		ItemStack goldenCarrot = new ItemStack(Material.GOLDEN_CARROT, 32);
		ItemMeta goldenCarrotMeta = goldenCarrot.getItemMeta();
		goldenCarrotMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lGolden Carrot"));
		List<String> goldenCarrotLore = new ArrayList<>();
		goldenCarrotLore.add(ChatUtils.translateToColor("&a12% Chance"));
		goldenCarrotMeta.setLore(goldenCarrotLore);
		goldenCarrot.setItemMeta(goldenCarrotMeta);
		gui.setItem(5, goldenCarrot);

		ItemStack diamond = new ItemStack(Material.DIAMOND, 16);
		ItemMeta diamondMeta = diamond.getItemMeta();
		diamondMeta.setDisplayName(ChatUtils.translateToColor("#a0f0ed&lDiamond"));
		List<String> diamondLore = new ArrayList<>();
		diamondLore.add(ChatUtils.translateToColor("&a12% Chance"));
		diamondMeta.setLore(diamondLore);
		diamond.setItemMeta(diamondMeta);
		gui.setItem(6, diamond);

		ItemStack enchantedGoldenApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4);
		ItemMeta enchantedGoldenAppleMeta = enchantedGoldenApple.getItemMeta();
		enchantedGoldenAppleMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lEnchanted Golden Apple"));
		List<String> enchantedGoldenAppleLore = new ArrayList<>();
		enchantedGoldenAppleLore.add(ChatUtils.translateToColor("&e8% Chance"));
		enchantedGoldenAppleMeta.setLore(enchantedGoldenAppleLore);
		enchantedGoldenApple.setItemMeta(enchantedGoldenAppleMeta);
		gui.setItem(10, enchantedGoldenApple);

		ItemStack ominousTrialKey = new ItemStack(Material.OMINOUS_TRIAL_KEY, 1);
		ItemMeta ominousTrialKeyMeta = ominousTrialKey.getItemMeta();
		ominousTrialKeyMeta.setDisplayName(ChatUtils.translateToColor("#515950&lOminous Trial Key"));
		List<String> ominousTrialKeyLore = new ArrayList<>();
		ominousTrialKeyLore.add(ChatUtils.translateToColor("&e8% Chance"));
		ominousTrialKeyMeta.setLore(ominousTrialKeyLore);
		ominousTrialKey.setItemMeta(ominousTrialKeyMeta);
		gui.setItem(11, ominousTrialKey);

		ItemStack netheriteIngot = new ItemStack(Material.NETHERITE_INGOT, 1);
		ItemMeta netheriteIngotMeta = netheriteIngot.getItemMeta();
		netheriteIngotMeta.setDisplayName(ChatUtils.translateToColor("#3a383a&lNetherite Ingot"));
		List<String> netheriteIngotLore = new ArrayList<>();
		netheriteIngotLore.add(ChatUtils.translateToColor("&e8% Chance"));
		netheriteIngotMeta.setLore(netheriteIngotLore);
		netheriteIngot.setItemMeta(netheriteIngotMeta);
		gui.setItem(15, netheriteIngot);

		ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
		ItemMeta totemMeta = totem.getItemMeta();
		totemMeta.setDisplayName(ChatUtils.translateToColor("#f5eba3&lTotem of Undying"));
		List<String> totemLore = new ArrayList<>();
		totemLore.add(ChatUtils.translateToColor("&e8% Chance"));
		totemMeta.setLore(totemLore);
		totem.setItemMeta(totemMeta);
		gui.setItem(16, totem);

		ItemStack driedGhast = new ItemStack(Material.DRIED_GHAST, 1);
		ItemMeta driedGhastMeta = driedGhast.getItemMeta();
		driedGhastMeta.setDisplayName(ChatUtils.translateToColor("#9b8d8d&lDried Ghast"));
		List<String> driedGhastLore = new ArrayList<>();
		driedGhastLore.add(ChatUtils.translateToColor("&c5% Chance"));
		driedGhastMeta.setLore(driedGhastLore);
		driedGhast.setItemMeta(driedGhastMeta);
		gui.setItem(20, driedGhast);

		ItemStack snifferEgg = new ItemStack(Material.SNIFFER_EGG, 1);
		ItemMeta snifferEggMeta = snifferEgg.getItemMeta();
		snifferEggMeta.setDisplayName(ChatUtils.translateToColor("#4e9c70&lSniffer Egg"));
		List<String> snifferEggLore = new ArrayList<>();
		snifferEggLore.add(ChatUtils.translateToColor("&c5% Chance"));
		snifferEggMeta.setLore(snifferEggLore);
		snifferEgg.setItemMeta(snifferEggMeta);
		gui.setItem(21, snifferEgg);

		ItemStack epicKey = new KeyRare().getItem();
		ItemMeta epicKeyMeta = epicKey.getItemMeta();
		epicKeyMeta.setDisplayName(ChatUtils.translateToColor("&3&lEpic Crate Key"));
		List<String> epicKeyLore = new ArrayList<>();
		epicKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		epicKeyMeta.setLore(epicKeyLore);
		epicKey.setItemMeta(epicKeyMeta);
		gui.setItem(24, epicKey);

		// Fill empty slots with the blank item
		ItemStack blank = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
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

	/**
	 * Initializes the GUI of the Epic Crate.
	 * @param player The player viewing the contents of a Epic Crate.
	 * @return The initialized inventory of the Epic Crate.
	 */
	private Inventory initializeEpicCrate(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Crate - Epic");

		ItemStack money1500 = new ItemStack(Material.GOLD_INGOT);
		ItemMeta money1500Meta = money1500.getItemMeta();
		money1500Meta.setDisplayName(ChatUtils.translateToColor("&6&l$1500 of In-Game Currency"));
		List<String> money1500Lore = new ArrayList<>();
		money1500Lore.add(ChatUtils.translateToColor("&a12% Chance"));
		money1500Meta.setLore(money1500Lore);
		money1500.setItemMeta(money1500Meta);
		gui.setItem(2, money1500);

		ItemStack shulkerBox = new ItemStack(Material.SHULKER_BOX, 1);
		ItemMeta shulkerBoxMeta = shulkerBox.getItemMeta();
		shulkerBoxMeta.setDisplayName(ChatUtils.translateToColor("#956895&lShulker Box"));
		List<String> shulkerBoxLore = new ArrayList<>();
		shulkerBoxLore.add(ChatUtils.translateToColor("&a12% Chance"));
		shulkerBoxMeta.setLore(shulkerBoxLore);
		shulkerBox.setItemMeta(shulkerBoxMeta);
		gui.setItem(3, shulkerBox);

		ItemStack diamond = new ItemStack(Material.DIAMOND, 64);
		ItemMeta diamondMeta = diamond.getItemMeta();
		diamondMeta.setDisplayName(ChatUtils.translateToColor("#a0f0ed&lDiamond"));
		List<String> diamondLore = new ArrayList<>();
		diamondLore.add(ChatUtils.translateToColor("&a12% Chance"));
		diamondMeta.setLore(diamondLore);
		diamond.setItemMeta(diamondMeta);
		gui.setItem(6, diamond);

		ItemStack trident = new ItemStack(Material.TRIDENT, 1);
		ItemMeta tridentMeta = trident.getItemMeta();
		tridentMeta.setDisplayName(ChatUtils.translateToColor("#579b8c&lTrident"));
		List<String> tridentLore = new ArrayList<>();
		tridentLore.add(ChatUtils.translateToColor("&e8% Chance"));
		tridentMeta.setLore(tridentLore);
		trident.setItemMeta(tridentMeta);
		gui.setItem(10, trident);

		ItemStack elytra = new ItemStack(Material.ELYTRA, 1);
		ItemMeta elytraMeta = elytra.getItemMeta();
		elytraMeta.setDisplayName(ChatUtils.translateToColor("#7d7d96&lElytra"));
		List<String> elytraLore = new ArrayList<>();
		elytraLore.add(ChatUtils.translateToColor("&e8% Chance"));
		elytraMeta.setLore(elytraLore);
		elytra.setItemMeta(elytraMeta);
		gui.setItem(11, elytra);

		ItemStack enchantedGoldenApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8);
		ItemMeta enchantedGoldenAppleMeta = enchantedGoldenApple.getItemMeta();
		enchantedGoldenAppleMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lEnchanted Golden Apple"));
		List<String> enchantedGoldenAppleLore = new ArrayList<>();
		enchantedGoldenAppleLore.add(ChatUtils.translateToColor("&e8% Chance"));
		enchantedGoldenAppleMeta.setLore(enchantedGoldenAppleLore);
		enchantedGoldenApple.setItemMeta(enchantedGoldenAppleMeta);
		gui.setItem(15, enchantedGoldenApple);

		ItemStack mcmmo10 = new ItemStack(Material.PAPER, 8);
		ItemMeta mcmmo10Meta = mcmmo10.getItemMeta();
		mcmmo10Meta.setDisplayName(ChatUtils.translateToColor("&6&lmcMMO All Skills +10"));
		List<String> mcmmo10Lore = new ArrayList<>();
		mcmmo10Lore.add(ChatUtils.translateToColor("&e8% Chance"));
		mcmmo10Meta.setLore(mcmmo10Lore);
		mcmmo10.setItemMeta(mcmmo10Meta);
		gui.setItem(16, mcmmo10);

		ItemStack epicKey = new KeyEpic().getItem();
		epicKey.setAmount(2);
		ItemMeta epicKeyMeta = epicKey.getItemMeta();
		epicKeyMeta.setDisplayName(ChatUtils.translateToColor("&3&lEpic Crate Key"));
		List<String> epicKeyLore = new ArrayList<>();
		epicKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		epicKeyMeta.setLore(epicKeyLore);
		epicKey.setItemMeta(epicKeyMeta);
		gui.setItem(20, epicKey);

		ItemStack discount10 = new ItemStack(Material.PAPER, 8);
		ItemMeta discount10Meta = discount10.getItemMeta();
		discount10Meta.setDisplayName(ChatUtils.translateToColor("&6&l10% Server Store Coupon"));
		List<String> discount10Lore = new ArrayList<>();
		discount10Lore.add(ChatUtils.translateToColor("&c5% Chance"));
		discount10Meta.setLore(discount10Lore);
		discount10.setItemMeta(discount10Meta);
		gui.setItem(21, discount10);

		ItemStack godlyKey = new KeyGodly().getItem();
		ItemMeta godlyKeyMeta = godlyKey.getItemMeta();
		godlyKeyMeta.setDisplayName(ChatUtils.translateToColor("&5&lGodly Crate Key"));
		List<String> godlyKeyLore = new ArrayList<>();
		godlyKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		godlyKeyMeta.setLore(godlyKeyLore);
		godlyKey.setItemMeta(godlyKeyMeta);
		gui.setItem(24, godlyKey);

		// Fill empty slots with the blank item
		ItemStack blank = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
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

	/**
	 * Initializes the GUI of the Godly Crate.
	 * @param player The player viewing the contents of a Godly Crate.
	 * @return The initialized inventory of the Godly Crate.
	 */
	private Inventory initializeGodlyCrate(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Crate - Godly");

		ItemStack money7500 = new ItemStack(Material.GOLD_INGOT);
		ItemMeta money7500Meta = money7500.getItemMeta();
		money7500Meta.setDisplayName(ChatUtils.translateToColor("&6&l$7500 of In-Game Currency"));
		List<String> money7500Lore = new ArrayList<>();
		money7500Lore.add(ChatUtils.translateToColor("&a12% Chance"));
		money7500Meta.setLore(money7500Lore);
		money7500.setItemMeta(money7500Meta);
		gui.setItem(2, money7500);

		ItemStack diamondBlock = new ItemStack(Material.DIAMOND_BLOCK, 16);
		ItemMeta diamondBlockMeta = diamondBlock.getItemMeta();
		diamondBlockMeta.setDisplayName(ChatUtils.translateToColor("#a0f0ed&lDiamond Block"));
		List<String> diamondBlockLore = new ArrayList<>();
		diamondBlockLore.add(ChatUtils.translateToColor("&a12% Chance"));
		diamondBlockMeta.setLore(diamondBlockLore);
		diamondBlock.setItemMeta(diamondBlockMeta);
		gui.setItem(3, diamondBlock);

		ItemStack iron = new ItemStack(Material.NETHERITE_BLOCK, 1);
		ItemMeta ironMeta = iron.getItemMeta();
		ironMeta.setDisplayName(ChatUtils.translateToColor("#3a383a&lNetherite Block"));
		List<String> ironLore = new ArrayList<>();
		ironLore.add(ChatUtils.translateToColor("&a12% Chance"));
		ironMeta.setLore(ironLore);
		iron.setItemMeta(ironMeta);
		gui.setItem(5, iron);

		ItemStack enchantedGoldenApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8);
		ItemMeta enchantedGoldenAppleMeta = enchantedGoldenApple.getItemMeta();
		enchantedGoldenAppleMeta.setDisplayName(ChatUtils.translateToColor("#fcd34d&lEnchanted Golden Apple"));
		List<String> enchantedGoldenAppleLore = new ArrayList<>();
		enchantedGoldenAppleLore.add(ChatUtils.translateToColor("&a12% Chance"));
		enchantedGoldenAppleMeta.setLore(enchantedGoldenAppleLore);
		enchantedGoldenApple.setItemMeta(enchantedGoldenAppleMeta);
		gui.setItem(6, enchantedGoldenApple);

		ItemStack mcmmo10 = new ItemStack(Material.PAPER, 1);
		ItemMeta mcmmo10Meta = mcmmo10.getItemMeta();
		mcmmo10Meta.setDisplayName(ChatUtils.translateToColor("&6&lmcMMO All Skills +30"));
		List<String> mcmmo10Lore = new ArrayList<>();
		mcmmo10Lore.add(ChatUtils.translateToColor("&e8% Chance"));
		mcmmo10Meta.setLore(mcmmo10Lore);
		mcmmo10.setItemMeta(mcmmo10Meta);
		gui.setItem(10, mcmmo10);

		ItemStack netherStar = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta netherStarMeta = netherStar.getItemMeta();
		netherStarMeta.setDisplayName(ChatUtils.translateToColor("#d8d6fb&lNether Star"));
		List<String> netherStarLore = new ArrayList<>();
		netherStarLore.add(ChatUtils.translateToColor("&e8% Chance"));
		netherStarMeta.setLore(netherStarLore);
		netherStar.setItemMeta(netherStarMeta);
		gui.setItem(11, netherStar);

		ItemStack heavyCore = new ItemStack(Material.HEAVY_CORE, 1);
		ItemMeta heavyCoreMeta = heavyCore.getItemMeta();
		heavyCoreMeta.setDisplayName(ChatUtils.translateToColor("#4d5158&lHeavy Core"));
		List<String> heavyCoreLore = new ArrayList<>();
		heavyCoreLore.add(ChatUtils.translateToColor("&e8% Chance"));
		heavyCoreMeta.setLore(heavyCoreLore);
		heavyCore.setItemMeta(heavyCoreMeta);
		gui.setItem(16, heavyCore);

		ItemStack epicKey = new KeyEpic().getItem();
		epicKey.setAmount(3);
		ItemMeta epicKeyMeta = epicKey.getItemMeta();
		epicKeyMeta.setDisplayName(ChatUtils.translateToColor("&3&lEpic Crate Key"));
		List<String> epicKeyLore = new ArrayList<>();
		epicKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		epicKeyMeta.setLore(epicKeyLore);
		epicKey.setItemMeta(epicKeyMeta);
		gui.setItem(20, epicKey);

		ItemStack discount30 = new ItemStack(Material.PAPER, 8);
		ItemMeta discount30Meta = discount30.getItemMeta();
		discount30Meta.setDisplayName(ChatUtils.translateToColor("&6&l30% Server Store Coupon"));
		List<String> discount30Lore = new ArrayList<>();
		discount30Lore.add(ChatUtils.translateToColor("&c5% Chance"));
		discount30Meta.setLore(discount30Lore);
		discount30.setItemMeta(discount30Meta);
		gui.setItem(21, discount30);

		ItemStack aranarthium = new AranarthiumIngot().getItem();
		ItemMeta aranarthiumMeta = aranarthium.getItemMeta();
		List<String> aranarthiumLore = new ArrayList<>();
		aranarthiumLore.add(ChatUtils.translateToColor("&c5% Chance"));
		aranarthiumMeta.setLore(aranarthiumLore);
		aranarthium.setItemMeta(aranarthiumMeta);
		gui.setItem(23, aranarthium);

		ItemStack godlyKey = new KeyGodly().getItem();
		godlyKey.setAmount(2);
		ItemMeta godlyKeyMeta = godlyKey.getItemMeta();
		godlyKeyMeta.setDisplayName(ChatUtils.translateToColor("&5&lGodly Crate Key"));
		List<String> godlyKeyLore = new ArrayList<>();
		godlyKeyLore.add(ChatUtils.translateToColor("&c5% Chance"));
		godlyKeyMeta.setLore(godlyKeyLore);
		godlyKey.setItemMeta(godlyKeyMeta);
		gui.setItem(24, godlyKey);

		// Fill empty slots with the blank item
		ItemStack blank = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
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

	/**
	 * Provides the Cluster that is associated to the input index for a Rare Crate.
	 * @param index The index of the cluster.
	 */
	public void updateRareCrateItems(int index) {
		ItemStack cluster = null;
		switch (index) {
			case 1 -> cluster = new IronCluster().getItem();
			case 2 -> cluster = new GoldCluster().getItem();
			case 3 -> cluster = new QuartzCluster().getItem();
			case 4 -> cluster = new LapisCluster().getItem();
			case 5 -> cluster = new RedstoneCluster().getItem();
			case 6 -> cluster = new DiamondCluster().getItem();
			case 7 -> cluster = new EmeraldCluster().getItem();
			default -> cluster = new CopperCluster().getItem();
		}
		ItemMeta cycledClusterMeta = cluster.getItemMeta();
		List<String> cycledClusterLore = new ArrayList<>();
		cycledClusterLore.add(ChatUtils.translateToColor("&c5% Chance"));
		cycledClusterMeta.setLore(cycledClusterLore);
		cluster.setItemMeta(cycledClusterMeta);
		initializedGui.setItem(23, cluster);
	}

	/**
	 * Provides the Cluster that is associated to the input index for an Epic Crate.
	 * @param armorTrimIndex The index of the armor trim.
	 * @param clusterIndex The index of the cluster.
	 */
	public void updateEpicCrateItems(int armorTrimIndex, int clusterIndex) {
		// Cycle through the Armor Trims
		ItemStack trim = null;
		switch (armorTrimIndex) {
			case 1 -> trim = new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 2 -> trim = new ItemStack(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 3 -> trim = new ItemStack(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 4 -> trim = new ItemStack(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 5 -> trim = new ItemStack(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 6 -> trim = new ItemStack(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 7 -> trim = new ItemStack(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 8 -> trim = new ItemStack(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 9 -> trim = new ItemStack(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 10 -> trim = new ItemStack(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 11 -> trim = new ItemStack(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 12 -> trim = new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 13 -> trim = new ItemStack(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 14 -> trim = new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 15 -> trim = new ItemStack(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 16 -> trim = new ItemStack(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
			case 17 -> trim = new ItemStack(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
			default -> trim = new ItemStack(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
		}
		ItemMeta cycledArmorTrimMeta = trim.getItemMeta();
		String trimName = trim.getType().name().split("_")[0].toLowerCase();
		trimName = trimName.substring(0, 1).toUpperCase() + trimName.substring(1) + " Armor Trim";
		if (trimName.startsWith("Ward") || trimName.startsWith("Spire") || trimName.startsWith("Eye") || trimName.startsWith("Vex")) {
			trimName = "&b&l" + trimName;
		} else if (trimName.startsWith("Silence")) {
			trimName = "&d&l" + trimName;
		} else {
			trimName = "&e&l" + trimName;
		}
		cycledArmorTrimMeta.setDisplayName(ChatUtils.translateToColor(trimName));
		List<String> cycledArmorTrimLore = new ArrayList<>();
		cycledArmorTrimLore.add(ChatUtils.translateToColor("&c5% Chance"));
		cycledArmorTrimMeta.setLore(cycledArmorTrimLore);
		trim.setItemMeta(cycledArmorTrimMeta);
		initializedGui.setItem(5, trim);

		// Cycle through the Clusters
		ItemStack cluster = null;
		switch (clusterIndex) {
			case 1 -> cluster = new IronCluster().getItem();
			case 2 -> cluster = new GoldCluster().getItem();
			case 3 -> cluster = new QuartzCluster().getItem();
			case 4 -> cluster = new LapisCluster().getItem();
			case 5 -> cluster = new RedstoneCluster().getItem();
			case 6 -> cluster = new DiamondCluster().getItem();
			case 7 -> cluster = new EmeraldCluster().getItem();
			default -> cluster = new CopperCluster().getItem();
		}
		cluster.setAmount(4);
		ItemMeta cycledClusterMeta = cluster.getItemMeta();
		cycledClusterMeta.setDisplayName(cycledClusterMeta.getDisplayName() + " (Mix)");
		List<String> cycledClusterLore = new ArrayList<>();
		cycledClusterLore.add(ChatUtils.translateToColor("&c5% Chance"));
		cycledClusterMeta.setLore(cycledClusterLore);
		cluster.setItemMeta(cycledClusterMeta);
		initializedGui.setItem(23, cluster);
	}

	/**
	 * Provides the Aranarthium Ingot that is associated to the input index for a Godly Crate.
	 * @param index The index of the cluster.
	 */
	public void updateGodlyCrateItems(int index) {
		ItemStack ingot = null;
		switch (index) {
			case 1 -> ingot = new AranarthiumAquatic().getItem();
			case 2 -> ingot = new AranarthiumArdent().getItem();
			case 3 -> ingot = new AranarthiumDwarven().getItem();
			case 4 -> ingot = new AranarthiumElven().getItem();
			case 5 -> ingot = new AranarthiumScorched().getItem();
			default -> ingot = new AranarthiumSoulbound().getItem();
		}
		ItemMeta cycledIngotMeta = ingot.getItemMeta();
		List<String> cycledIngotLore = new ArrayList<>();
		cycledIngotLore.add(ChatUtils.translateToColor("&c5% Chance"));
		cycledIngotMeta.setLore(cycledIngotLore);
		ingot.setItemMeta(cycledIngotMeta);
		initializedGui.setItem(15, ingot);
	}

}
