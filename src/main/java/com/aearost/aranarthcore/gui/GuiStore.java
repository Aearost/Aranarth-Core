package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.objects.StorePage;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiStore {

	private final Player player;
	private final ItemStack blank;
	private final ItemStack previous;
	private final ItemStack exit;
	private final Inventory initializedGui;

	public GuiStore(Player player, StorePage page) {
		this.player = player;
		blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta meta = blank.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&f"));
		blank.setItemMeta(meta);

		previous = new ItemStack(Material.RED_WOOL);
		ItemMeta previousMeta = previous.getItemMeta();
		previousMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
		previous.setItemMeta(previousMeta);

		exit = new ItemStack(Material.BARRIER);
		ItemMeta exitMeta = exit.getItemMeta();
		exitMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
		exit.setItemMeta(exitMeta);

		if (page == StorePage.SAINT) {
			this.initializedGui = initializeGuiSaint(player);
		} else if (page == StorePage.PERKS) {
			this.initializedGui = initializeGuiPerks(player);
		} else if (page == StorePage.BOOSTS) {
			this.initializedGui = initializeGuiBoosts(player);
		} else if (page == StorePage.CRATES) {
			this.initializedGui = initializeGuiCrates(player);
		} else {
			this.initializedGui = initializeGuiMain(player);
		}
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);
	}

	private Inventory initializeGuiMain(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Aranarth Store - Main");

		// Set empty spaces
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		gui.setItem(9, blank);
		gui.setItem(11, blank);
		gui.setItem(13, blank);
		gui.setItem(15, blank);
		gui.setItem(17, blank);

		gui.setItem(18, blank);
		gui.setItem(19, blank);
		gui.setItem(20, blank);
		gui.setItem(21, blank);
		gui.setItem(23, blank);
		gui.setItem(24, blank);
		gui.setItem(25, blank);
		gui.setItem(26, blank);

		// Specified Slots
		ItemStack saint = new ItemStack(Material.PINK_CONCRETE_POWDER);
		ItemMeta saintMeta = saint.getItemMeta();
		saintMeta.setDisplayName(ChatUtils.translateToColor("&5&lSaint Ranks"));
		List<String> saintLore = new ArrayList<>();
		saintLore.add(ChatUtils.translateToColor("&dMonthly or Lifetime donation ranks"));
		saintMeta.setLore(saintLore);
		saint.setItemMeta(saintMeta);
		gui.setItem(10, saint);

		ItemStack perks = new ItemStack(Material.WHITE_CONCRETE_POWDER);
		ItemMeta perksMeta = perks.getItemMeta();
		perksMeta.setDisplayName(ChatUtils.translateToColor("&7&lPerks"));
		List<String> perksLore = new ArrayList<>();
		perksLore.add(ChatUtils.translateToColor("&fIndividually purchasable donation perks"));
		perksMeta.setLore(perksLore);
		perks.setItemMeta(perksMeta);
		gui.setItem(12, perks);

		ItemStack boosts = new ItemStack(Material.LIGHT_BLUE_CONCRETE_POWDER);
		ItemMeta boostsMeta = boosts.getItemMeta();
		boostsMeta.setDisplayName(ChatUtils.translateToColor("&3&lServer Boosts"));
		List<String> boostsLore = new ArrayList<>();
		boostsLore.add(ChatUtils.translateToColor("&bGlobally applied server boosts"));
		boostsMeta.setLore(boostsLore);
		boosts.setItemMeta(boostsMeta);
		gui.setItem(14, boosts);

		ItemStack crates = new ItemStack(Material.YELLOW_CONCRETE_POWDER);
		ItemMeta cratesMeta = crates.getItemMeta();
		cratesMeta.setDisplayName(ChatUtils.translateToColor("&6&lCrate Keys"));
		List<String> cratesLore = new ArrayList<>();
		cratesLore.add(ChatUtils.translateToColor("&ePacks of Crate Keys"));
		cratesMeta.setLore(cratesLore);
		crates.setItemMeta(cratesMeta);
		gui.setItem(16, crates);

		gui.setItem(gui.getSize() - 5, exit);

		return gui;
	}

	private Inventory initializeGuiSaint(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 36, "Aranarth Store - Saint Ranks");

		// Set empty spaces
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		gui.setItem(9, blank);
		gui.setItem(10, blank);
		gui.setItem(12, blank);
		gui.setItem(14, blank);
		gui.setItem(16, blank);
		gui.setItem(17, blank);

		gui.setItem(18, blank);
		gui.setItem(19, blank);
		gui.setItem(21, blank);
		gui.setItem(23, blank);
		gui.setItem(25, blank);
		gui.setItem(26, blank);

		gui.setItem(27, blank);
		gui.setItem(28, blank);
		gui.setItem(29, blank);
		gui.setItem(30, blank);
		gui.setItem(32, blank);
		gui.setItem(33, blank);
		gui.setItem(34, blank);
		gui.setItem(35, blank);

		// Specified Slots
		ItemStack saint1monthly = new ItemStack(Material.PINK_CONCRETE_POWDER);
		ItemMeta saint1monthlyMeta = saint1monthly.getItemMeta();
		saint1monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lSaint I (1 Month) - $3.99"));
		List<String> saint1monthlyLore = new ArrayList<>();
		saint1monthlyLore.add(ChatUtils.translateToColor("&dAccess to the Saint I perks for 1 month"));
		saint1monthlyMeta.setLore(saint1monthlyLore);
		saint1monthly.setItemMeta(saint1monthlyMeta);
		gui.setItem(11, saint1monthly);

		ItemStack saint2monthly = new ItemStack(Material.MAGENTA_CONCRETE_POWDER);
		ItemMeta saint2monthlyMeta = saint2monthly.getItemMeta();
		saint2monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lSaint II (1 Month) - $5.99"));
		List<String> saint2monthlyLore = new ArrayList<>();
		saint2monthlyLore.add(ChatUtils.translateToColor("&dAccess to the Saint II perks for 1 month"));
		saint2monthlyMeta.setLore(saint2monthlyLore);
		saint2monthly.setItemMeta(saint2monthlyMeta);
		gui.setItem(13, saint2monthly);

		ItemStack saint3monthly = new ItemStack(Material.PURPLE_CONCRETE_POWDER);
		ItemMeta saint3monthlyMeta = saint3monthly.getItemMeta();
		saint3monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lSaint III (1 Month) - $7.99"));
		List<String> saint3monthlyLore = new ArrayList<>();
		saint3monthlyLore.add(ChatUtils.translateToColor("&dAccess to the Saint III perks for 1 month"));
		saint3monthlyMeta.setLore(saint3monthlyLore);
		saint3monthly.setItemMeta(saint3monthlyMeta);
		gui.setItem(15, saint3monthly);

		ItemStack saint1 = new ItemStack(Material.PINK_CONCRETE);
		ItemMeta saint1Meta = saint1.getItemMeta();
		saint1Meta.setDisplayName(ChatUtils.translateToColor("&5&lSaint I (Lifetime) - $9.99"));
		List<String> saint1Lore = new ArrayList<>();
		saint1Lore.add(ChatUtils.translateToColor("&dPermanent access to the Saint I perks"));
		saint1Meta.setLore(saint1Lore);
		saint1.setItemMeta(saint1Meta);
		gui.setItem(20, saint1);

		ItemStack saint2 = new ItemStack(Material.MAGENTA_CONCRETE);
		ItemMeta saint2Meta = saint2.getItemMeta();
		saint2Meta.setDisplayName(ChatUtils.translateToColor("&5&lSaint II (Lifetime) - $19.99"));
		List<String> saint2Lore = new ArrayList<>();
		saint2Lore.add(ChatUtils.translateToColor("&dPermanent access to the Saint II perks"));
		saint2Meta.setLore(saint2Lore);
		saint2.setItemMeta(saint2Meta);
		gui.setItem(22, saint2);

		ItemStack saint3 = new ItemStack(Material.PURPLE_CONCRETE);
		ItemMeta saint3Meta = saint3.getItemMeta();
		saint3Meta.setDisplayName(ChatUtils.translateToColor("&5&lSaint III (Lifetime) - $34.99"));
		List<String> saint3Lore = new ArrayList<>();
		saint3Lore.add(ChatUtils.translateToColor("&dPermanent access to the Saint III perks"));
		saint3Meta.setLore(saint3Lore);
		saint3.setItemMeta(saint3Meta);
		gui.setItem(24, saint3);

		gui.setItem(gui.getSize() - 5, previous);

		return gui;
	}

	private Inventory initializeGuiPerks(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 45, "Aranarth Store - Perks");

		// Set empty spaces
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		gui.setItem(9, blank);
		gui.setItem(10, blank);
		gui.setItem(11, blank);
		gui.setItem(15, blank);
		gui.setItem(16, blank);
		gui.setItem(17, blank);

		gui.setItem(18, blank);
		gui.setItem(22, blank);
		gui.setItem(26, blank);

		gui.setItem(27, blank);
		gui.setItem(28, blank);
		gui.setItem(29, blank);
		gui.setItem(33, blank);
		gui.setItem(34, blank);
		gui.setItem(35, blank);

		gui.setItem(36, blank);
		gui.setItem(37, blank);
		gui.setItem(38, blank);
		gui.setItem(39, blank);
		gui.setItem(41, blank);
		gui.setItem(42, blank);
		gui.setItem(43, blank);
		gui.setItem(44, blank);

		// Specified Slots
		ItemStack blacklist = new ItemStack(Material.BLACK_WOOL);
		ItemMeta blacklistMeta = blacklist.getItemMeta();
		blacklistMeta.setDisplayName(ChatUtils.translateToColor("&8&lBlacklist - $1.99"));
		List<String> blacklistLore = new ArrayList<>();
		blacklistLore.add(ChatUtils.translateToColor("&7Prevents specified items from being picked up"));
		blacklistMeta.setLore(blacklistLore);
		blacklist.setItemMeta(blacklistMeta);
		gui.setItem(12, blacklist);

		ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
		ItemMeta shulkerMeta = shulker.getItemMeta();
		shulkerMeta.setDisplayName(ChatUtils.translateToColor("&5&lShulker Assist - $3.99"));
		List<String> shulkerLore = new ArrayList<>();
		shulkerLore.add(ChatUtils.translateToColor("&dAmplifies the use of shulker boxes"));
		shulkerMeta.setLore(shulkerLore);
		shulker.setItemMeta(shulkerMeta);
		gui.setItem(13, shulker);

		ItemStack inventory = new ItemStack(Material.CHEST);
		ItemMeta inventoryMeta = inventory.getItemMeta();
		inventoryMeta.setDisplayName(ChatUtils.translateToColor("&3&lInventory Assist - $5.99"));
		List<String> inventoryLore = new ArrayList<>();
		inventoryLore.add(ChatUtils.translateToColor("&bSimplifies and improves inventory management"));
		inventoryMeta.setLore(inventoryLore);
		inventory.setItemMeta(inventoryMeta);
		gui.setItem(14, inventory);

		ItemStack compressor = new ItemStack(Material.PISTON);
		ItemMeta compressorMeta = compressor.getItemMeta();
		compressorMeta.setDisplayName(ChatUtils.translateToColor("&6&lCompressor - $5.99"));
		List<String> compressorLore = new ArrayList<>();
		compressorLore.add(ChatUtils.translateToColor("&eCompresses picked up items into their block form"));
		compressorMeta.setLore(compressorLore);
		compressor.setItemMeta(compressorMeta);
		gui.setItem(19, compressor);

		ItemStack randomizer = new ItemStack(Material.ENDER_EYE);
		ItemMeta randomizerMeta = randomizer.getItemMeta();
		randomizerMeta.setDisplayName(ChatUtils.translateToColor("&a&lRandomizer - $3.99"));
		List<String> randomizerLore = new ArrayList<>();
		randomizerLore.add(ChatUtils.translateToColor("&fRandom placement of blocks per specified pattern"));
		randomizerMeta.setLore(randomizerLore);
		randomizer.setItemMeta(randomizerMeta);
		gui.setItem(20, randomizer);

		ItemStack tables = new ItemStack(Material.CRAFTING_TABLE);
		ItemMeta tablesMeta = tables.getItemMeta();
		tablesMeta.setDisplayName(ChatUtils.translateToColor("&6&lTables - $0.99"));
		List<String> tablesLore = new ArrayList<>();
		tablesLore.add(ChatUtils.translateToColor("&eAccess to using all tables from anywhere"));
		tablesMeta.setLore(tablesLore);
		tables.setItemMeta(tablesMeta);
		gui.setItem(21, tables);

		ItemStack chat = new ItemStack(Material.WRITABLE_BOOK);
		ItemMeta chatMeta = chat.getItemMeta();
		chatMeta.setDisplayName(ChatUtils.translateToColor("&e&lColored Chat - $0.99"));
		List<String> chatLore = new ArrayList<>();
		chatLore.add(ChatUtils.translateToColor("&fAccess to &cc&6o&el&ao&3r&be&dd &fchat"));
		chatMeta.setLore(chatLore);
		chat.setItemMeta(chatMeta);
		gui.setItem(23, chat);

		ItemStack itemname = new ItemStack(Material.NAME_TAG);
		ItemMeta itemnameMeta = itemname.getItemMeta();
		itemnameMeta.setDisplayName(ChatUtils.translateToColor("&c&lItem Name - $2.99"));
		List<String> itemnameLore = new ArrayList<>();
		itemnameLore.add(ChatUtils.translateToColor("&fCustom naming of items including &cc&6o&el&ao&br&ds"));
		itemnameMeta.setLore(itemnameLore);
		itemname.setItemMeta(itemnameMeta);
		gui.setItem(24, itemname);

		ItemStack bluefire = new ItemStack(Material.SOUL_CAMPFIRE);
		ItemMeta bluefireMeta = bluefire.getItemMeta();
		bluefireMeta.setDisplayName(ChatUtils.translateToColor("&b&lBlue Fire - $3.99"));
		List<String> bluefireLore = new ArrayList<>();
		bluefireLore.add(ChatUtils.translateToColor("&fConverts firebending to be blue fire (cosmetic only)"));
		bluefireMeta.setLore(bluefireLore);
		bluefire.setItemMeta(bluefireMeta);
		gui.setItem(25, bluefire);

		ItemStack invisibleItemFrame = new InvisibleItemFrame().getItem();
		ItemMeta invisibleItemFrameMeta = invisibleItemFrame.getItemMeta();
		invisibleItemFrameMeta.setDisplayName(ChatUtils.translateToColor("&f&lInvisible Item Frames - $1.99"));
		List<String> invisibleItemFrameLore = new ArrayList<>();
		invisibleItemFrameLore.add(ChatUtils.translateToColor("&eUnlocks the crafting of invisible item frames"));
		invisibleItemFrameMeta.setLore(invisibleItemFrameLore);
		invisibleItemFrame.setItemMeta(invisibleItemFrameMeta);
		gui.setItem(30, invisibleItemFrame);

		ItemStack homes = new ItemStack(Material.RED_BED);
		ItemMeta homesMeta = homes.getItemMeta();
		homesMeta.setDisplayName(ChatUtils.translateToColor("&4&lAdditional 3 Homes - $2.99"));
		List<String> homesLore = new ArrayList<>();
		homesLore.add(ChatUtils.translateToColor("&cProvides 3 additional homes (capped at 15)"));
		homesMeta.setLore(homesLore);
		homes.setItemMeta(homesMeta);
		gui.setItem(31, homes);

		ItemStack discord = new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA);
		ItemMeta discordMeta = discord.getItemMeta();
		discordMeta.setDisplayName(ChatUtils.translateToColor("&5&lDiscord Chat - $1.99"));
		List<String> discordLore = new ArrayList<>();
		discordLore.add(ChatUtils.translateToColor("&dProvides access to the Discord chat channel"));
		discordMeta.setLore(discordLore);
		discord.setItemMeta(discordMeta);
		gui.setItem(32, discord);

		gui.setItem(gui.getSize() - 5, previous);

		return gui;
	}

	private Inventory initializeGuiBoosts(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Aranarth Store - Boosts");

		// Set empty spaces
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		gui.setItem(9, blank);
		gui.setItem(11, blank);
		gui.setItem(13, blank);
		gui.setItem(15, blank);
		gui.setItem(17, blank);

		gui.setItem(18, blank);
		gui.setItem(19, blank);
		gui.setItem(20, blank);
		gui.setItem(21, blank);
		gui.setItem(23, blank);
		gui.setItem(24, blank);
		gui.setItem(25, blank);
		gui.setItem(26, blank);

		// Specified Slots
		ItemStack miner = new ItemStack(Material.NETHERITE_PICKAXE);
		ItemMeta minerMeta = miner.getItemMeta();
		minerMeta.setDisplayName(ChatUtils.translateToColor("&8&lBoost of the Miner - $3.49"));
		List<String> minerLore = new ArrayList<>();
		minerLore.add(ChatUtils.translateToColor("&7Increased ore and cluster drops"));
		minerMeta.setLore(minerLore);
		miner.setItemMeta(minerMeta);
		gui.setItem(10, miner);

		ItemStack harvest = new ItemStack(Material.NETHERITE_HOE);
		ItemMeta harvestMeta = harvest.getItemMeta();
		harvestMeta.setDisplayName(ChatUtils.translateToColor("&6&lBoost of the Harvest - $2.99"));
		List<String> harvestLore = new ArrayList<>();
		harvestLore.add(ChatUtils.translateToColor("&eIncreased crop, soil, and log drops"));
		harvestMeta.setLore(harvestLore);
		harvest.setItemMeta(harvestMeta);
		gui.setItem(12, harvest);

		ItemStack hunter = new ItemStack(Material.CROSSBOW);
		ItemMeta hunterMeta = hunter.getItemMeta();
		hunterMeta.setDisplayName(ChatUtils.translateToColor("&4&lBoost of the Hunter - $2.99"));
		List<String> hunterLore = new ArrayList<>();
		hunterLore.add(ChatUtils.translateToColor("&cIncreased passive and hostile mob drops"));
		hunterMeta.setLore(hunterLore);
		hunter.setItemMeta(hunterMeta);
		gui.setItem(14, hunter);

		ItemStack chi = new ItemStack(Material.SUGAR);
		ItemMeta chiMeta = chi.getItemMeta();
		chiMeta.setDisplayName(ChatUtils.translateToColor("&7&lBoost of Chi - $2.99"));
		List<String> chiLore = new ArrayList<>();
		chiLore.add(ChatUtils.translateToColor("&fIncreased bending damage and decreased cooldowns"));
		chiMeta.setLore(chiLore);
		chi.setItemMeta(chiMeta);
		gui.setItem(16, chi);

		gui.setItem(gui.getSize() - 5, previous);

		return gui;
	}

	private Inventory initializeGuiCrates(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 27, "Aranarth Store - Crate Keys");

		// Set empty spaces
		gui.setItem(0, blank);
		gui.setItem(1, blank);
		gui.setItem(2, blank);
		gui.setItem(3, blank);
		gui.setItem(4, blank);
		gui.setItem(5, blank);
		gui.setItem(6, blank);
		gui.setItem(7, blank);
		gui.setItem(8, blank);

		gui.setItem(9, blank);
		gui.setItem(11, blank);
		gui.setItem(13, blank);
		gui.setItem(15, blank);
		gui.setItem(17, blank);

		gui.setItem(18, blank);
		gui.setItem(19, blank);
		gui.setItem(20, blank);
		gui.setItem(21, blank);
		gui.setItem(23, blank);
		gui.setItem(24, blank);
		gui.setItem(25, blank);
		gui.setItem(26, blank);

		// Specified Slots
		ItemStack rareKey = new ItemStack(Material.TRIPWIRE_HOOK);
		ItemMeta rareKeyMeta = rareKey.getItemMeta();
		rareKeyMeta.setDisplayName(ChatUtils.translateToColor("&6&lRare Crate Key (x3) - $0.99"));
		rareKey.setItemMeta(rareKeyMeta);
		gui.setItem(10, rareKey);

		ItemStack epicKey = new ItemStack(Material.TRIPWIRE_HOOK);
		ItemMeta epicKeyMeta = epicKey.getItemMeta();
		epicKeyMeta.setDisplayName(ChatUtils.translateToColor("&3&lEpic Crate Key (x3) - $2.99"));
		epicKey.setItemMeta(epicKeyMeta);
		gui.setItem(13, epicKey);

		ItemStack godlyKey = new ItemStack(Material.TRIPWIRE_HOOK);
		ItemMeta godlyKeyMeta = godlyKey.getItemMeta();
		godlyKeyMeta.setDisplayName(ChatUtils.translateToColor("&5&lGodly Crate Key (x3) - $4.99"));
		godlyKey.setItemMeta(godlyKeyMeta);
		gui.setItem(16, godlyKey);

		gui.setItem(gui.getSize() - 5, previous);

		return gui;
	}

}
