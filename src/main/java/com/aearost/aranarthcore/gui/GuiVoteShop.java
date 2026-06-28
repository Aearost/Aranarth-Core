package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GuiVoteShop {

	private final Player player;
	private final Inventory initializedGui;

	public GuiVoteShop(Player player) {
		this.player = player;
		this.initializedGui = initializeGui(player);
	}

	public void openGui() {
		player.closeInventory();
		player.openInventory(initializedGui);

		int[] randomKeyIndex = {1};
		int[] taskId = {-1};
		taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), () -> {
			if (player.getOpenInventory() != null
					&& ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals("Aranarth Vote Shop")) {
				updateRandomKeyItem(randomKeyIndex[0]);
				randomKeyIndex[0] = (randomKeyIndex[0] + 1) % 4;
			} else {
				Bukkit.getScheduler().cancelTask(taskId[0]);
			}
		}, 20, 20);
	}

	public void updateRandomKeyItem(int index) {
		NamespacedKey randomKeyNSKey = new NamespacedKey(AranarthCore.getInstance(), "random_key");
		ItemStack randomKey;
		switch (index) {
			case 1 -> randomKey = new KeyRare().getItem();
			case 2 -> randomKey = new KeyEpic().getItem();
			case 3 -> randomKey = new KeyGodly().getItem();
			default -> randomKey = new KeyVote().getItem();
		}
		ItemMeta meta = randomKey.getItemMeta();
		meta.setDisplayName(ChatUtils.translateToColor("&4&lRandom Crate Key"));
		List<String> lore = new ArrayList<>();
		lore.add(ChatUtils.translateToColor("&e10 vote points"));
		lore.add(ChatUtils.translateToColor("&a&oVote &7&o- 65%"));
		lore.add(ChatUtils.translateToColor("&6&oRare &7&o- 25%"));
		lore.add(ChatUtils.translateToColor("&3&oEpic &7&o- 7%"));
		lore.add(ChatUtils.translateToColor("&5&oGodly &7&o- 3%"));
		meta.getPersistentDataContainer().set(randomKeyNSKey, PersistentDataType.STRING, "true");
		meta.setLore(lore);
		randomKey.setItemMeta(meta);
		initializedGui.setItem(17, randomKey);
	}
	
	private Inventory initializeGui(Player player) {
		Inventory gui = Bukkit.getServer().createInventory(player, 54, ChatUtils.translateToColor("&a&lAranarth Vote Shop"));
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		ItemStack headerFooter = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		ItemMeta headFooterMeta = headerFooter.getItemMeta();
		headFooterMeta.setDisplayName(" ");
		headerFooter.setItemMeta(headFooterMeta);
		gui.setItem(0, headerFooter);
		gui.setItem(1, headerFooter);
		gui.setItem(2, headerFooter);
		gui.setItem(3, headerFooter);
		gui.setItem(5, headerFooter);
		gui.setItem(6, headerFooter);
		gui.setItem(7, headerFooter);
		gui.setItem(8, headerFooter);

		ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) skull.getItemMeta();
		meta.setOwningPlayer(player);
		meta.setDisplayName(ChatUtils.translateToColor("&a&lYour Vote Stats"));
		List<String> lore = new ArrayList<>();
		lore.add(ChatUtils.translateToColor("&7Total votes: &e" + AranarthUtils.getVoteNum(player.getUniqueId())));
		lore.add(ChatUtils.translateToColor("&7Total vote points: &e" + AranarthUtils.getVotePoints(player.getUniqueId())));
		lore.add(ChatUtils.translateToColor("&7Available vote points: &e" + AranarthUtils.getAvailableVotePoints(player.getUniqueId())));
		meta.setLore(lore);
		skull.setItemMeta(meta);
		gui.setItem(4, skull);

		ItemStack keyVote = new KeyVote().getItem();
		ItemMeta keyVoteMeta = keyVote.getItemMeta();
		keyVoteMeta.setDisplayName(ChatUtils.translateToColor("&a&lVote Crate Key"));
		List<String> keyVoteLore = new ArrayList<>();
		keyVoteLore.add(ChatUtils.translateToColor("&e3 vote points"));
		keyVoteMeta.setLore(keyVoteLore);
		keyVote.setItemMeta(keyVoteMeta);
		gui.setItem(11, keyVote);

		ItemStack keyRare = new KeyRare().getItem();
		ItemMeta keyRareMeta = keyRare.getItemMeta();
		keyRareMeta.setDisplayName(ChatUtils.translateToColor("&6&lRare Crate Key"));
		List<String> keyRareLore = new ArrayList<>();
		keyRareLore.add(ChatUtils.translateToColor("&e20 vote points"));
		keyRareMeta.setLore(keyRareLore);
		keyRare.setItemMeta(keyRareMeta);
		gui.setItem(12, keyRare);

		ItemStack bendingChange = new ItemStack(Material.WHITE_CONCRETE_POWDER);
		ItemMeta bendingChangeMeta = bendingChange.getItemMeta();
		bendingChangeMeta.setDisplayName(ChatUtils.translateToColor("&5&lBending Change Cooldown"));
		List<String> bendingChangeLore = new ArrayList<>();
		bendingChangeLore.add(ChatUtils.translateToColor("&e10 vote points"));
		bendingChangeMeta.setLore(bendingChangeLore);
		bendingChange.setItemMeta(bendingChangeMeta);
		gui.setItem(9, bendingChange);

		NamespacedKey randomKeyNSKey = new NamespacedKey(AranarthCore.getInstance(), "random_key");
		ItemStack randomKey = new KeyVote().getItem();
		ItemMeta randomKeyMeta = randomKey.getItemMeta();
		randomKeyMeta.setDisplayName(ChatUtils.translateToColor("&4&lRandom Crate Key"));
		List<String> randomKeyLore = new ArrayList<>();
		randomKeyLore.add(ChatUtils.translateToColor("&e10 vote points"));
		randomKeyLore.add(ChatUtils.translateToColor("&a&oVote &7&o- 65%"));
		randomKeyLore.add(ChatUtils.translateToColor("&6&oRare &7&o- 25%"));
		randomKeyLore.add(ChatUtils.translateToColor("&3&oEpic &7&o- 7%"));
		randomKeyLore.add(ChatUtils.translateToColor("&5&oGodly &7&o- 3%"));
		randomKeyMeta.getPersistentDataContainer().set(randomKeyNSKey, PersistentDataType.STRING, "true");
		randomKeyMeta.setLore(randomKeyLore);
		randomKey.setItemMeta(randomKeyMeta);
		gui.setItem(17, randomKey);

		ItemStack keyEpic = new KeyEpic().getItem();
		ItemMeta keyEpicMeta = keyEpic.getItemMeta();
		keyEpicMeta.setDisplayName(ChatUtils.translateToColor("&3&lEpic Crate Key"));
		List<String> keyEpicLore = new ArrayList<>();
		keyEpicLore.add(ChatUtils.translateToColor("&e50 vote points"));
		keyEpicMeta.setLore(keyEpicLore);
		keyEpic.setItemMeta(keyEpicMeta);
		gui.setItem(14, keyEpic);

		ItemStack keyGodly = new KeyGodly().getItem();
		ItemMeta keyGodlyMeta = keyGodly.getItemMeta();
		keyGodlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lGodly Crate Key"));
		List<String> keyGodlyLore = new ArrayList<>();
		keyGodlyLore.add(ChatUtils.translateToColor("&e75 vote points"));
		keyGodlyMeta.setLore(keyGodlyLore);
		keyGodly.setItemMeta(keyGodlyMeta);
		gui.setItem(15, keyGodly);

		ItemStack discord = new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA);
		ItemMeta discordMeta = discord.getItemMeta();
		discordMeta.setDisplayName(ChatUtils.translateToColor("&5&lDiscord Chat"));
		List<String> discordLore = new ArrayList<>();
		discordLore.add(ChatUtils.translateToColor("&e150 vote points"));
		discordMeta.setLore(discordLore);
		discord.setItemMeta(discordMeta);
		gui.setItem(19, discord);

		ItemStack tables = new ItemStack(Material.CRAFTING_TABLE);
		ItemMeta tablesMeta = tables.getItemMeta();
		tablesMeta.setDisplayName(ChatUtils.translateToColor("&6&lTables Perk"));
		List<String> tablesLore = new ArrayList<>();
		tablesLore.add(ChatUtils.translateToColor("&e150 vote points"));
		tablesMeta.setLore(tablesLore);
		tables.setItemMeta(tablesMeta);
		gui.setItem(20, tables);

		ItemStack invisFrames = new InvisibleItemFrame().getItem();
		ItemMeta invisFramesMeta = invisFrames.getItemMeta();
		invisFramesMeta.setDisplayName(ChatUtils.translateToColor("&f&lInvisible Item Frames Perk"));
		List<String> invisFramesLore = new ArrayList<>();
		invisFramesLore.add(ChatUtils.translateToColor("&e200 vote points"));
		invisFramesMeta.setLore(invisFramesLore);
		invisFrames.setItemMeta(invisFramesMeta);
		gui.setItem(21, invisFrames);

		ItemStack homes = new ItemStack(Material.RED_BED);
		ItemMeta homesMeta = homes.getItemMeta();
		homesMeta.setDisplayName(ChatUtils.translateToColor("&4&lAdditional 3 Homes"));
		List<String> homesLore = new ArrayList<>();
		homesLore.add(ChatUtils.translateToColor("&e150 vote points"));
		homesMeta.setLore(homesLore);
		homes.setItemMeta(homesMeta);
		gui.setItem(22, homes);

		ItemStack chat = new ItemStack(Material.WRITABLE_BOOK);
		ItemMeta chatMeta = chat.getItemMeta();
		chatMeta.setDisplayName(ChatUtils.translateToColor("&e&lFull Colored Chat Perk"));
		List<String> chatLore = new ArrayList<>();
		chatLore.add(ChatUtils.translateToColor("&e200 vote points"));
		chatMeta.setLore(chatLore);
		chat.setItemMeta(chatMeta);
		gui.setItem(23, chat);

		ItemStack itemname = new ItemStack(Material.NAME_TAG);
		ItemMeta itemnameMeta = itemname.getItemMeta();
		itemnameMeta.setDisplayName(ChatUtils.translateToColor("&c&lFull Item Name Perk"));
		List<String> itemnameLore = new ArrayList<>();
		itemnameLore.add(ChatUtils.translateToColor("&e200 vote points"));
		itemnameMeta.setLore(itemnameLore);
		itemname.setItemMeta(itemnameMeta);
		gui.setItem(24, itemname);

		ItemStack blacklist = new ItemStack(Material.LAVA_BUCKET);
		ItemMeta blacklistMeta = blacklist.getItemMeta();
		blacklistMeta.setDisplayName(ChatUtils.translateToColor("&8&lBlacklist Perk"));
		List<String> blacklistLore = new ArrayList<>();
		blacklistLore.add(ChatUtils.translateToColor("&e200 vote points"));
		blacklistMeta.setLore(blacklistLore);
		blacklist.setItemMeta(blacklistMeta);
		gui.setItem(25, blacklist);

		ItemStack mcmmo10 = new ItemStack(Material.PAPER);
		ItemMeta mcmmo10Meta = mcmmo10.getItemMeta();
		mcmmo10Meta.setDisplayName(ChatUtils.translateToColor("&6&lmcMMO All Skills +10"));
		List<String> mcmmo10Lore = new ArrayList<>();
		mcmmo10Lore.add(ChatUtils.translateToColor("&e150 vote points"));
		mcmmo10Meta.setLore(mcmmo10Lore);
		mcmmo10.setItemMeta(mcmmo10Meta);
		gui.setItem(30, mcmmo10);

		ItemStack money = new ItemStack(Material.GOLD_INGOT);
		ItemMeta moneyMeta = money.getItemMeta();
		moneyMeta.setDisplayName(ChatUtils.translateToColor("&6&l$15000 of In-Game Currency"));
		List<String> moneyLore = new ArrayList<>();
		moneyLore.add(ChatUtils.translateToColor("&e15 vote points"));
		moneyMeta.setLore(moneyLore);
		money.setItemMeta(moneyMeta);
		gui.setItem(31, money);

		ItemStack mcmmo50 = new ItemStack(Material.PAPER);
		ItemMeta mcmmo50Meta = mcmmo50.getItemMeta();
		mcmmo50Meta.setDisplayName(ChatUtils.translateToColor("&6&lmcMMO All Skills +50"));
		List<String> mcmmo50Lore = new ArrayList<>();
		mcmmo50Lore.add(ChatUtils.translateToColor("&e500 vote points"));
		mcmmo50Meta.setLore(mcmmo50Lore);
		mcmmo50.setItemMeta(mcmmo50Meta);
		gui.setItem(32, mcmmo50);

		ItemStack saint1monthly = new ItemStack(Material.PINK_CONCRETE_POWDER);
		ItemMeta saint1monthlyMeta = saint1monthly.getItemMeta();
		saint1monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lAcolyte (1 Month)"));
		List<String> saint1monthlyLore = new ArrayList<>();
		saint1monthlyLore.add(ChatUtils.translateToColor("&e250 vote points"));
		saint1monthlyMeta.setLore(saint1monthlyLore);
		saint1monthly.setItemMeta(saint1monthlyMeta);
		gui.setItem(38, saint1monthly);

		ItemStack saint2monthly = new ItemStack(Material.MAGENTA_CONCRETE_POWDER);
		ItemMeta saint2monthlyMeta = saint2monthly.getItemMeta();
		saint2monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lDisciple (1 Month)"));
		List<String> saint2monthlyLore = new ArrayList<>();
		saint2monthlyLore.add(ChatUtils.translateToColor("&e350 vote points"));
		saint2monthlyMeta.setLore(saint2monthlyLore);
		saint2monthly.setItemMeta(saint2monthlyMeta);
		gui.setItem(40, saint2monthly);

		ItemStack saint3monthly = new ItemStack(Material.PURPLE_CONCRETE_POWDER);
		ItemMeta saint3monthlyMeta = saint3monthly.getItemMeta();
		saint3monthlyMeta.setDisplayName(ChatUtils.translateToColor("&5&lSeraph (1 Month)"));
		List<String> saint3monthlyLore = new ArrayList<>();
		saint3monthlyLore.add(ChatUtils.translateToColor("&e500 vote points"));
		saint3monthlyMeta.setLore(saint3monthlyLore);
		saint3monthly.setItemMeta(saint3monthlyMeta);
		gui.setItem(42, saint3monthly);

		ItemStack boostMiner = new ItemStack(Material.NETHERITE_PICKAXE);
		ItemMeta boostMinerMeta = boostMiner.getItemMeta();
		boostMinerMeta.setDisplayName(ChatUtils.translateToColor("&8&lBoost of the Miner"));
		List<String> boostMinerLore = new ArrayList<>();
		boostMinerLore.add(ChatUtils.translateToColor("&e150 vote points"));
		boostMinerMeta.setLore(boostMinerLore);
		boostMiner.setItemMeta(boostMinerMeta);
		gui.setItem(27, boostMiner);

		ItemStack boostHarvest = new ItemStack(Material.NETHERITE_HOE);
		ItemMeta boostHarvestMeta = boostHarvest.getItemMeta();
		boostHarvestMeta.setDisplayName(ChatUtils.translateToColor("&6&lBoost of the Harvest"));
		List<String> boostHarvestLore = new ArrayList<>();
		boostHarvestLore.add(ChatUtils.translateToColor("&e125 vote points"));
		boostHarvestMeta.setLore(boostHarvestLore);
		boostHarvest.setItemMeta(boostHarvestMeta);
		gui.setItem(36, boostHarvest);

		ItemStack boostHunter = new ItemStack(Material.CROSSBOW);
		ItemMeta boostHunterMeta = boostHunter.getItemMeta();
		boostHunterMeta.setDisplayName(ChatUtils.translateToColor("&c&lBoost of the Hunter"));
		List<String> boostHunterLore = new ArrayList<>();
		boostHunterLore.add(ChatUtils.translateToColor("&e100 vote points"));
		boostHunterMeta.setLore(boostHunterLore);
		boostHunter.setItemMeta(boostHunterMeta);
		gui.setItem(35, boostHunter);

		ItemStack boostChi = new ItemStack(Material.SUGAR);
		ItemMeta boostChiMeta = boostChi.getItemMeta();
		boostChiMeta.setDisplayName(ChatUtils.translateToColor("&f&lBoost of Chi"));
		List<String> boostChiLore = new ArrayList<>();
		boostChiLore.add(ChatUtils.translateToColor("&e75 vote points"));
		boostChiMeta.setLore(boostChiLore);
		boostChi.setItemMeta(boostChiMeta);
		gui.setItem(44, boostChi);

		gui.setItem(45, headerFooter);
		gui.setItem(46, headerFooter);
		gui.setItem(47, headerFooter);
		gui.setItem(48, headerFooter);
		gui.setItem(50, headerFooter);
		gui.setItem(51, headerFooter);
		gui.setItem(52, headerFooter);
		gui.setItem(53, headerFooter);

		ItemStack exit = new ItemStack(Material.BARRIER);
		ItemMeta exitMeta = exit.getItemMeta();
		exitMeta.setDisplayName(ChatUtils.translateToColor("&c&lExit"));
		exit.setItemMeta(exitMeta);
		gui.setItem(49, exit);

		ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta fillMeta = headerFooter.getItemMeta();
		fillMeta.setDisplayName(" ");
		fill.setItemMeta(fillMeta);
		// Fills in empty spaces
		for (int i = 0; i < gui.getSize(); i++) {
			if (gui.getItem(i) == null) {
				gui.setItem(i, fill);
			}
		}

		return gui;
	}

}
