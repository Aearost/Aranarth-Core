package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.objects.StorePage;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
        previousMeta.setDisplayName(Lang.getFor(player, "gui.page_prev"));
        previous.setItemMeta(previousMeta);

        exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(Lang.getFor(player, "gui.exit"));
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
        Inventory gui = Bukkit.getServer().createInventory(player, 27, Lang.getFor(player, "gui.store.title_main"));

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
        saintMeta.setDisplayName(Lang.getFor(player, "gui.store.saint_ranks"));
        List<String> saintLore = new ArrayList<>();
        saintLore.add(Lang.getFor(player, "gui.store.saint_ranks_lore"));
        saintMeta.setLore(saintLore);
        saint.setItemMeta(saintMeta);
        gui.setItem(10, saint);

        ItemStack perks = new ItemStack(Material.WHITE_CONCRETE_POWDER);
        ItemMeta perksMeta = perks.getItemMeta();
        perksMeta.setDisplayName(Lang.getFor(player, "gui.store.perks"));
        List<String> perksLore = new ArrayList<>();
        perksLore.add(Lang.getFor(player, "gui.store.perks_lore"));
        perksMeta.setLore(perksLore);
        perks.setItemMeta(perksMeta);
        gui.setItem(12, perks);

        ItemStack boosts = new ItemStack(Material.LIGHT_BLUE_CONCRETE_POWDER);
        ItemMeta boostsMeta = boosts.getItemMeta();
        boostsMeta.setDisplayName(Lang.getFor(player, "gui.store.boosts"));
        List<String> boostsLore = new ArrayList<>();
        boostsLore.add(Lang.getFor(player, "gui.store.boosts_lore"));
        boostsMeta.setLore(boostsLore);
        boosts.setItemMeta(boostsMeta);
        gui.setItem(14, boosts);

        ItemStack crates = new ItemStack(Material.YELLOW_CONCRETE_POWDER);
        ItemMeta cratesMeta = crates.getItemMeta();
        cratesMeta.setDisplayName(Lang.getFor(player, "gui.store.crate_keys"));
        List<String> cratesLore = new ArrayList<>();
        cratesLore.add(Lang.getFor(player, "gui.store.crate_keys_lore"));
        cratesMeta.setLore(cratesLore);
        crates.setItemMeta(cratesMeta);
        gui.setItem(16, crates);

        gui.setItem(gui.getSize() - 5, exit);

        return gui;
    }

    private Inventory initializeGuiSaint(Player player) {
        Inventory gui = Bukkit.getServer().createInventory(player, 36, Lang.getFor(player, "gui.store.title_saint"));

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
        saint1monthlyMeta.setDisplayName(Lang.getFor(player, "gui.store.saint1_monthly"));
        List<String> saint1monthlyLore = new ArrayList<>();
        saint1monthlyLore.add(Lang.getFor(player, "gui.store.saint1_monthly_lore"));
        saint1monthlyMeta.setLore(saint1monthlyLore);
        saint1monthly.setItemMeta(saint1monthlyMeta);
        gui.setItem(11, saint1monthly);

        ItemStack saint2monthly = new ItemStack(Material.MAGENTA_CONCRETE_POWDER);
        ItemMeta saint2monthlyMeta = saint2monthly.getItemMeta();
        saint2monthlyMeta.setDisplayName(Lang.getFor(player, "gui.store.saint2_monthly"));
        List<String> saint2monthlyLore = new ArrayList<>();
        saint2monthlyLore.add(Lang.getFor(player, "gui.store.saint2_monthly_lore"));
        saint2monthlyMeta.setLore(saint2monthlyLore);
        saint2monthly.setItemMeta(saint2monthlyMeta);
        gui.setItem(13, saint2monthly);

        ItemStack saint3monthly = new ItemStack(Material.PURPLE_CONCRETE_POWDER);
        ItemMeta saint3monthlyMeta = saint3monthly.getItemMeta();
        saint3monthlyMeta.setDisplayName(Lang.getFor(player, "gui.store.saint3_monthly"));
        List<String> saint3monthlyLore = new ArrayList<>();
        saint3monthlyLore.add(Lang.getFor(player, "gui.store.saint3_monthly_lore"));
        saint3monthlyMeta.setLore(saint3monthlyLore);
        saint3monthly.setItemMeta(saint3monthlyMeta);
        gui.setItem(15, saint3monthly);

        ItemStack saint1 = new ItemStack(Material.PINK_CONCRETE);
        ItemMeta saint1Meta = saint1.getItemMeta();
        saint1Meta.setDisplayName(Lang.getFor(player, "gui.store.saint1"));
        List<String> saint1Lore = new ArrayList<>();
        saint1Lore.add(Lang.getFor(player, "gui.store.saint1_lore"));
        saint1Meta.setLore(saint1Lore);
        saint1.setItemMeta(saint1Meta);
        gui.setItem(20, saint1);

        ItemStack saint2 = new ItemStack(Material.MAGENTA_CONCRETE);
        ItemMeta saint2Meta = saint2.getItemMeta();
        saint2Meta.setDisplayName(Lang.getFor(player, "gui.store.saint2"));
        List<String> saint2Lore = new ArrayList<>();
        saint2Lore.add(Lang.getFor(player, "gui.store.saint2_lore"));
        saint2Meta.setLore(saint2Lore);
        saint2.setItemMeta(saint2Meta);
        gui.setItem(22, saint2);

        ItemStack saint3 = new ItemStack(Material.PURPLE_CONCRETE);
        ItemMeta saint3Meta = saint3.getItemMeta();
        saint3Meta.setDisplayName(Lang.getFor(player, "gui.store.saint3"));
        List<String> saint3Lore = new ArrayList<>();
        saint3Lore.add(Lang.getFor(player, "gui.store.saint3_lore"));
        saint3Meta.setLore(saint3Lore);
        saint3.setItemMeta(saint3Meta);
        gui.setItem(24, saint3);

        gui.setItem(gui.getSize() - 5, previous);

        return gui;
    }

    private Inventory initializeGuiPerks(Player player) {
        Inventory gui = Bukkit.getServer().createInventory(player, 45, Lang.getFor(player, "gui.store.title_perks"));

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
        gui.setItem(26, blank);

        gui.setItem(27, blank);
        gui.setItem(28, blank);
        gui.setItem(29, blank);
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
        ItemStack blacklist = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta blacklistMeta = blacklist.getItemMeta();
        blacklistMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_blacklist"));
        List<String> blacklistLore = new ArrayList<>();
        blacklistLore.add(Lang.getFor(player, "gui.store.perk_blacklist_lore"));
        blacklistMeta.setLore(blacklistLore);
        blacklist.setItemMeta(blacklistMeta);
        gui.setItem(12, blacklist);

        ItemStack shulker = new ItemStack(Material.SHULKER_BOX);
        ItemMeta shulkerMeta = shulker.getItemMeta();
        shulkerMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_shulker"));
        List<String> shulkerLore = new ArrayList<>();
        shulkerLore.add(Lang.getFor(player, "gui.store.perk_shulker_lore"));
        shulkerMeta.setLore(shulkerLore);
        shulker.setItemMeta(shulkerMeta);
        gui.setItem(13, shulker);

        ItemStack inventory = new ItemStack(Material.CHEST);
        ItemMeta inventoryMeta = inventory.getItemMeta();
        inventoryMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_inventory"));
        List<String> inventoryLore = new ArrayList<>();
        inventoryLore.add(Lang.getFor(player, "gui.store.perk_inventory_lore"));
        inventoryMeta.setLore(inventoryLore);
        inventory.setItemMeta(inventoryMeta);
        gui.setItem(14, inventory);

        ItemStack compressor = new ItemStack(Material.PISTON);
        ItemMeta compressorMeta = compressor.getItemMeta();
        compressorMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_compressor"));
        List<String> compressorLore = new ArrayList<>();
        compressorLore.add(Lang.getFor(player, "gui.store.perk_compressor_lore"));
        compressorMeta.setLore(compressorLore);
        compressor.setItemMeta(compressorMeta);
        gui.setItem(19, compressor);

        ItemStack randomizer = new ItemStack(Material.ENDER_EYE);
        ItemMeta randomizerMeta = randomizer.getItemMeta();
        randomizerMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_randomizer"));
        List<String> randomizerLore = new ArrayList<>();
        randomizerLore.add(Lang.getFor(player, "gui.store.perk_randomizer_lore"));
        randomizerMeta.setLore(randomizerLore);
        randomizer.setItemMeta(randomizerMeta);
        gui.setItem(20, randomizer);

        ItemStack tables = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta tablesMeta = tables.getItemMeta();
        tablesMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_tables"));
        List<String> tablesLore = new ArrayList<>();
        tablesLore.add(Lang.getFor(player, "gui.store.perk_tables_lore"));
        tablesMeta.setLore(tablesLore);
        tables.setItemMeta(tablesMeta);
        gui.setItem(21, tables);

        ItemStack nickname = new ItemStack(Material.NAME_TAG);
        ItemMeta nicknameMeta = nickname.getItemMeta();
        nicknameMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_nickname"));
        List<String> nicknameLore = new ArrayList<>();
        nicknameLore.add(Lang.getFor(player, "gui.store.perk_nickname_lore"));
        nicknameMeta.setLore(nicknameLore);
        nickname.setItemMeta(nicknameMeta);
        gui.setItem(22, nickname);

        ItemStack chat = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta chatMeta = chat.getItemMeta();
        chatMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_chat"));
        List<String> chatLore = new ArrayList<>();
        chatLore.add(Lang.getFor(player, "gui.store.perk_chat_lore"));
        chatMeta.setLore(chatLore);
        chat.setItemMeta(chatMeta);
        gui.setItem(23, chat);

        ItemStack itemname = new ItemStack(Material.NAME_TAG);
        ItemMeta itemnameMeta = itemname.getItemMeta();
        itemnameMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_itemname"));
        List<String> itemnameLore = new ArrayList<>();
        itemnameLore.add(Lang.getFor(player, "gui.store.perk_itemname_lore"));
        itemnameMeta.setLore(itemnameLore);
        itemname.setItemMeta(itemnameMeta);
        gui.setItem(24, itemname);

        ItemStack bluefire = new ItemStack(Material.SOUL_CAMPFIRE);
        ItemMeta bluefireMeta = bluefire.getItemMeta();
        bluefireMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_bluefire"));
        List<String> bluefireLore = new ArrayList<>();
        bluefireLore.add(Lang.getFor(player, "gui.store.perk_bluefire_lore"));
        bluefireMeta.setLore(bluefireLore);
        bluefire.setItemMeta(bluefireMeta);
        gui.setItem(25, bluefire);

        ItemStack invisibleItemFrame = new InvisibleItemFrame().getItem();
        ItemMeta invisibleItemFrameMeta = invisibleItemFrame.getItemMeta();
        invisibleItemFrameMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_invisframes"));
        List<String> invisibleItemFrameLore = new ArrayList<>();
        invisibleItemFrameLore.add(Lang.getFor(player, "gui.store.perk_invisframes_lore"));
        invisibleItemFrameMeta.setLore(invisibleItemFrameLore);
        invisibleItemFrame.setItemMeta(invisibleItemFrameMeta);
        gui.setItem(29, invisibleItemFrame);

        ItemStack homes = new ItemStack(Material.RED_BED);
        ItemMeta homesMeta = homes.getItemMeta();
        homesMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_homes"));
        List<String> homesLore = new ArrayList<>();
        homesLore.add(Lang.getFor(player, "gui.store.perk_homes_lore"));
        homesMeta.setLore(homesLore);
        homes.setItemMeta(homesMeta);
        gui.setItem(30, homes);

        ItemStack discord = new ItemStack(Material.PURPLE_GLAZED_TERRACOTTA);
        ItemMeta discordMeta = discord.getItemMeta();
        discordMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_discord"));
        List<String> discordLore = new ArrayList<>();
        discordLore.add(Lang.getFor(player, "gui.store.perk_discord_lore"));
        discordMeta.setLore(discordLore);
        discord.setItemMeta(discordMeta);
        gui.setItem(32, discord);

        ItemStack invisibleArmor = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta invisibleArmorMeta = invisibleArmor.getItemMeta();
        invisibleArmorMeta.setDisplayName(Lang.getFor(player, "gui.store.perk_invisarmor"));
        List<String> invisibleArmorLore = new ArrayList<>();
        invisibleArmorLore.add(Lang.getFor(player, "gui.store.perk_invisarmor_lore"));
        invisibleArmorMeta.setLore(invisibleArmorLore);
        invisibleArmor.setItemMeta(invisibleArmorMeta);
        gui.setItem(33, invisibleArmor);

        gui.setItem(gui.getSize() - 5, previous);

        return gui;
    }

    private Inventory initializeGuiBoosts(Player player) {
        Inventory gui = Bukkit.getServer().createInventory(player, 27, Lang.getFor(player, "gui.store.title_boosts"));

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
        minerMeta.setDisplayName(Lang.getFor(player, "gui.store.boost_miner"));
        List<String> minerLore = new ArrayList<>();
        minerLore.add(Lang.getFor(player, "gui.store.boost_miner_lore"));
        minerMeta.setLore(minerLore);
        miner.setItemMeta(minerMeta);
        gui.setItem(10, miner);

        ItemStack harvest = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta harvestMeta = harvest.getItemMeta();
        harvestMeta.setDisplayName(Lang.getFor(player, "gui.store.boost_harvest"));
        List<String> harvestLore = new ArrayList<>();
        harvestLore.add(Lang.getFor(player, "gui.store.boost_harvest_lore"));
        harvestMeta.setLore(harvestLore);
        harvest.setItemMeta(harvestMeta);
        gui.setItem(12, harvest);

        ItemStack hunter = new ItemStack(Material.CROSSBOW);
        ItemMeta hunterMeta = hunter.getItemMeta();
        hunterMeta.setDisplayName(Lang.getFor(player, "gui.store.boost_hunter"));
        List<String> hunterLore = new ArrayList<>();
        hunterLore.add(Lang.getFor(player, "gui.store.boost_hunter_lore"));
        hunterMeta.setLore(hunterLore);
        hunter.setItemMeta(hunterMeta);
        gui.setItem(14, hunter);

        ItemStack chi = new ItemStack(Material.SUGAR);
        ItemMeta chiMeta = chi.getItemMeta();
        chiMeta.setDisplayName(Lang.getFor(player, "gui.store.boost_chi"));
        List<String> chiLore = new ArrayList<>();
        chiLore.add(Lang.getFor(player, "gui.store.boost_chi_lore"));
        chiMeta.setLore(chiLore);
        chi.setItemMeta(chiMeta);
        gui.setItem(16, chi);

        gui.setItem(gui.getSize() - 5, previous);

        return gui;
    }

    private Inventory initializeGuiCrates(Player player) {
        Inventory gui = Bukkit.getServer().createInventory(player, 27, Lang.getFor(player, "gui.store.title_crates"));

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
        rareKeyMeta.setDisplayName(Lang.getFor(player, "gui.store.rare_key"));
        rareKey.setItemMeta(rareKeyMeta);
        gui.setItem(10, rareKey);

        ItemStack epicKey = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta epicKeyMeta = epicKey.getItemMeta();
        epicKeyMeta.setDisplayName(Lang.getFor(player, "gui.store.epic_key"));
        epicKey.setItemMeta(epicKeyMeta);
        gui.setItem(13, epicKey);

        ItemStack godlyKey = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta godlyKeyMeta = godlyKey.getItemMeta();
        godlyKeyMeta.setDisplayName(Lang.getFor(player, "gui.store.godly_key"));
        godlyKey.setItemMeta(godlyKeyMeta);
        gui.setItem(16, godlyKey);

        gui.setItem(gui.getSize() - 5, previous);

        return gui;
    }

}
