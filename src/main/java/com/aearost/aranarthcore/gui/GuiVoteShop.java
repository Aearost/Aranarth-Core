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
import com.aearost.aranarthcore.utils.Lang;
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
                    && ChatUtils.stripColorFormatting(player.getOpenInventory().getTitle()).equals(com.aearost.aranarthcore.utils.ChatUtils.stripColorFormatting(Lang.getFor(player, "gui.voteshop.title")))) {
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
        meta.setDisplayName(Lang.getFor(player, "gui.voteshop.random_key"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.voteshop.random_key_points"));
        lore.add(Lang.getFor(player, "gui.voteshop.pct_vote"));
        lore.add(Lang.getFor(player, "gui.voteshop.pct_rare"));
        lore.add(Lang.getFor(player, "gui.voteshop.pct_epic"));
        lore.add(Lang.getFor(player, "gui.voteshop.pct_godly"));
        meta.getPersistentDataContainer().set(randomKeyNSKey, PersistentDataType.STRING, "true");
        meta.setLore(lore);
        randomKey.setItemMeta(meta);
        initializedGui.setItem(17, randomKey);
    }

    private Inventory initializeGui(Player player) {
        Inventory gui = Bukkit.getServer().createInventory(player, 54, Lang.getFor(player, "gui.voteshop.title"));
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
        meta.setDisplayName(Lang.getFor(player, "gui.voteshop.stats"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.getFor(player, "gui.voteshop.total_votes", "count", String.valueOf(AranarthUtils.getVoteNum(player.getUniqueId()))));
        lore.add(Lang.getFor(player, "gui.voteshop.total_points", "count", String.valueOf(AranarthUtils.getVotePoints(player.getUniqueId()))));
        lore.add(Lang.getFor(player, "gui.voteshop.available_points", "count", String.valueOf(AranarthUtils.getAvailableVotePoints(player.getUniqueId()))));
        meta.setLore(lore);
        skull.setItemMeta(meta);
        gui.setItem(4, skull);

        ItemStack keyVote = new KeyVote().getItem();
        ItemMeta keyVoteMeta = keyVote.getItemMeta();
        keyVoteMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.vote_key"));
        List<String> keyVoteLore = new ArrayList<>();
        keyVoteLore.add(Lang.getFor(player, "gui.voteshop.vote_key_points"));
        keyVoteMeta.setLore(keyVoteLore);
        keyVote.setItemMeta(keyVoteMeta);
        gui.setItem(11, keyVote);

        ItemStack keyRare = new KeyRare().getItem();
        ItemMeta keyRareMeta = keyRare.getItemMeta();
        keyRareMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.rare_key"));
        List<String> keyRareLore = new ArrayList<>();
        keyRareLore.add(Lang.getFor(player, "gui.voteshop.rare_key_points"));
        keyRareMeta.setLore(keyRareLore);
        keyRare.setItemMeta(keyRareMeta);
        gui.setItem(12, keyRare);

        ItemStack bendingChange = new ItemStack(Material.WHITE_CONCRETE_POWDER);
        ItemMeta bendingChangeMeta = bendingChange.getItemMeta();
        bendingChangeMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.bending_change"));
        List<String> bendingChangeLore = new ArrayList<>();
        bendingChangeLore.add(Lang.getFor(player, "gui.voteshop.bending_change_points"));
        bendingChangeMeta.setLore(bendingChangeLore);
        bendingChange.setItemMeta(bendingChangeMeta);
        gui.setItem(9, bendingChange);

        NamespacedKey randomKeyNSKey = new NamespacedKey(AranarthCore.getInstance(), "random_key");
        ItemStack randomKey = new KeyVote().getItem();
        ItemMeta randomKeyMeta = randomKey.getItemMeta();
        randomKeyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.random_key"));
        List<String> randomKeyLore = new ArrayList<>();
        randomKeyLore.add(Lang.getFor(player, "gui.voteshop.random_key_points"));
        randomKeyLore.add(Lang.getFor(player, "gui.voteshop.pct_vote"));
        randomKeyLore.add(Lang.getFor(player, "gui.voteshop.pct_rare"));
        randomKeyLore.add(Lang.getFor(player, "gui.voteshop.pct_epic"));
        randomKeyLore.add(Lang.getFor(player, "gui.voteshop.pct_godly"));
        randomKeyMeta.getPersistentDataContainer().set(randomKeyNSKey, PersistentDataType.STRING, "true");
        randomKeyMeta.setLore(randomKeyLore);
        randomKey.setItemMeta(randomKeyMeta);
        gui.setItem(17, randomKey);

        ItemStack keyEpic = new KeyEpic().getItem();
        ItemMeta keyEpicMeta = keyEpic.getItemMeta();
        keyEpicMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.epic_key"));
        List<String> keyEpicLore = new ArrayList<>();
        keyEpicLore.add(Lang.getFor(player, "gui.voteshop.epic_key_points"));
        keyEpicMeta.setLore(keyEpicLore);
        keyEpic.setItemMeta(keyEpicMeta);
        gui.setItem(14, keyEpic);

        ItemStack keyGodly = new KeyGodly().getItem();
        ItemMeta keyGodlyMeta = keyGodly.getItemMeta();
        keyGodlyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.godly_key"));
        List<String> keyGodlyLore = new ArrayList<>();
        keyGodlyLore.add(Lang.getFor(player, "gui.voteshop.godly_key_points"));
        keyGodlyMeta.setLore(keyGodlyLore);
        keyGodly.setItemMeta(keyGodlyMeta);
        gui.setItem(15, keyGodly);

        ItemStack discord = new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA);
        ItemMeta discordMeta = discord.getItemMeta();
        discordMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.discord"));
        List<String> discordLore = new ArrayList<>();
        discordLore.add(Lang.getFor(player, "gui.voteshop.discord_points"));
        discordMeta.setLore(discordLore);
        discord.setItemMeta(discordMeta);
        gui.setItem(18, discord);

        ItemStack tables = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta tablesMeta = tables.getItemMeta();
        tablesMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.tables"));
        List<String> tablesLore = new ArrayList<>();
        tablesLore.add(Lang.getFor(player, "gui.voteshop.tables_points"));
        tablesMeta.setLore(tablesLore);
        tables.setItemMeta(tablesMeta);
        gui.setItem(19, tables);

        ItemStack invisFrames = new InvisibleItemFrame().getItem();
        ItemMeta invisFramesMeta = invisFrames.getItemMeta();
        invisFramesMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.invis_frames"));
        List<String> invisFramesLore = new ArrayList<>();
        invisFramesLore.add(Lang.getFor(player, "gui.voteshop.invis_frames_points"));
        invisFramesMeta.setLore(invisFramesLore);
        invisFrames.setItemMeta(invisFramesMeta);
        gui.setItem(20, invisFrames);

        ItemStack homes = new ItemStack(Material.RED_BED);
        ItemMeta homesMeta = homes.getItemMeta();
        homesMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.homes"));
        List<String> homesLore = new ArrayList<>();
        homesLore.add(Lang.getFor(player, "gui.voteshop.homes_points"));
        homesMeta.setLore(homesLore);
        homes.setItemMeta(homesMeta);
        gui.setItem(21, homes);

        NamespacedKey nicknamePerkNSKey = new NamespacedKey(AranarthCore.getInstance(), "nickname_perk");
        ItemStack nickname = new ItemStack(Material.NAME_TAG);
        ItemMeta nicknameMeta = nickname.getItemMeta();
        nicknameMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.nickname"));
        List<String> nicknameLore = new ArrayList<>();
        nicknameLore.add(Lang.getFor(player, "gui.voteshop.nickname_points"));
        nicknameMeta.getPersistentDataContainer().set(nicknamePerkNSKey, PersistentDataType.STRING, "true");
        nicknameMeta.setLore(nicknameLore);
        nickname.setItemMeta(nicknameMeta);
        gui.setItem(23, nickname);

        ItemStack chat = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta chatMeta = chat.getItemMeta();
        chatMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.colored_chat"));
        List<String> chatLore = new ArrayList<>();
        chatLore.add(Lang.getFor(player, "gui.voteshop.colored_chat_points"));
        chatMeta.setLore(chatLore);
        chat.setItemMeta(chatMeta);
        gui.setItem(24, chat);

        ItemStack itemname = new ItemStack(Material.NAME_TAG);
        ItemMeta itemnameMeta = itemname.getItemMeta();
        itemnameMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.item_name"));
        List<String> itemnameLore = new ArrayList<>();
        itemnameLore.add(Lang.getFor(player, "gui.voteshop.item_name_points"));
        itemnameMeta.setLore(itemnameLore);
        itemname.setItemMeta(itemnameMeta);
        gui.setItem(25, itemname);

        ItemStack blacklist = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta blacklistMeta = blacklist.getItemMeta();
        blacklistMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.blacklist"));
        List<String> blacklistLore = new ArrayList<>();
        blacklistLore.add(Lang.getFor(player, "gui.voteshop.blacklist_points"));
        blacklistMeta.setLore(blacklistLore);
        blacklist.setItemMeta(blacklistMeta);
        gui.setItem(26, blacklist);

        ItemStack mcmmo10 = new ItemStack(Material.PAPER);
        ItemMeta mcmmo10Meta = mcmmo10.getItemMeta();
        mcmmo10Meta.setDisplayName(Lang.getFor(player, "gui.voteshop.mcmmo_10"));
        List<String> mcmmo10Lore = new ArrayList<>();
        mcmmo10Lore.add(Lang.getFor(player, "gui.voteshop.mcmmo_10_points"));
        mcmmo10Meta.setLore(mcmmo10Lore);
        mcmmo10.setItemMeta(mcmmo10Meta);
        gui.setItem(30, mcmmo10);

        ItemStack money = new ItemStack(Material.GOLD_INGOT);
        ItemMeta moneyMeta = money.getItemMeta();
        moneyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.money"));
        List<String> moneyLore = new ArrayList<>();
        moneyLore.add(Lang.getFor(player, "gui.voteshop.money_points"));
        moneyMeta.setLore(moneyLore);
        money.setItemMeta(moneyMeta);
        gui.setItem(31, money);

        ItemStack mcmmo50 = new ItemStack(Material.PAPER);
        ItemMeta mcmmo50Meta = mcmmo50.getItemMeta();
        mcmmo50Meta.setDisplayName(Lang.getFor(player, "gui.voteshop.mcmmo_50"));
        List<String> mcmmo50Lore = new ArrayList<>();
        mcmmo50Lore.add(Lang.getFor(player, "gui.voteshop.mcmmo_50_points"));
        mcmmo50Meta.setLore(mcmmo50Lore);
        mcmmo50.setItemMeta(mcmmo50Meta);
        gui.setItem(32, mcmmo50);

        ItemStack saint1monthly = new ItemStack(Material.PINK_CONCRETE_POWDER);
        ItemMeta saint1monthlyMeta = saint1monthly.getItemMeta();
        saint1monthlyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.saint1_monthly"));
        List<String> saint1monthlyLore = new ArrayList<>();
        saint1monthlyLore.add(Lang.getFor(player, "gui.voteshop.saint1_points"));
        saint1monthlyMeta.setLore(saint1monthlyLore);
        saint1monthly.setItemMeta(saint1monthlyMeta);
        gui.setItem(38, saint1monthly);

        ItemStack saint2monthly = new ItemStack(Material.MAGENTA_CONCRETE_POWDER);
        ItemMeta saint2monthlyMeta = saint2monthly.getItemMeta();
        saint2monthlyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.saint2_monthly"));
        List<String> saint2monthlyLore = new ArrayList<>();
        saint2monthlyLore.add(Lang.getFor(player, "gui.voteshop.saint2_points"));
        saint2monthlyMeta.setLore(saint2monthlyLore);
        saint2monthly.setItemMeta(saint2monthlyMeta);
        gui.setItem(40, saint2monthly);

        ItemStack saint3monthly = new ItemStack(Material.PURPLE_CONCRETE_POWDER);
        ItemMeta saint3monthlyMeta = saint3monthly.getItemMeta();
        saint3monthlyMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.saint3_monthly"));
        List<String> saint3monthlyLore = new ArrayList<>();
        saint3monthlyLore.add(Lang.getFor(player, "gui.voteshop.saint3_points"));
        saint3monthlyMeta.setLore(saint3monthlyLore);
        saint3monthly.setItemMeta(saint3monthlyMeta);
        gui.setItem(42, saint3monthly);

        ItemStack boostMiner = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta boostMinerMeta = boostMiner.getItemMeta();
        boostMinerMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.boost_miner"));
        List<String> boostMinerLore = new ArrayList<>();
        boostMinerLore.add(Lang.getFor(player, "gui.voteshop.boost_miner_points"));
        boostMinerMeta.setLore(boostMinerLore);
        boostMiner.setItemMeta(boostMinerMeta);
        gui.setItem(27, boostMiner);

        ItemStack boostHarvest = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta boostHarvestMeta = boostHarvest.getItemMeta();
        boostHarvestMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.boost_harvest"));
        List<String> boostHarvestLore = new ArrayList<>();
        boostHarvestLore.add(Lang.getFor(player, "gui.voteshop.boost_harvest_points"));
        boostHarvestMeta.setLore(boostHarvestLore);
        boostHarvest.setItemMeta(boostHarvestMeta);
        gui.setItem(36, boostHarvest);

        ItemStack boostHunter = new ItemStack(Material.CROSSBOW);
        ItemMeta boostHunterMeta = boostHunter.getItemMeta();
        boostHunterMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.boost_hunter"));
        List<String> boostHunterLore = new ArrayList<>();
        boostHunterLore.add(Lang.getFor(player, "gui.voteshop.boost_hunter_points"));
        boostHunterMeta.setLore(boostHunterLore);
        boostHunter.setItemMeta(boostHunterMeta);
        gui.setItem(35, boostHunter);

        ItemStack boostChi = new ItemStack(Material.SUGAR);
        ItemMeta boostChiMeta = boostChi.getItemMeta();
        boostChiMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.boost_chi"));
        List<String> boostChiLore = new ArrayList<>();
        boostChiLore.add(Lang.getFor(player, "gui.voteshop.boost_chi_points"));
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
        exitMeta.setDisplayName(Lang.getFor(player, "gui.voteshop.exit"));
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
