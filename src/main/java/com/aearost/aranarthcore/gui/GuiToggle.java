package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiToggle {

    private final Player player;
    private final Inventory initializedGui;

    public GuiToggle(Player player) {
        this.player = player;
        this.initializedGui = initializeGui(player);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    private Inventory initializeGui(Player player) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        Inventory gui = Bukkit.getServer().createInventory(player, 36, "Player Toggles");

        ItemStack blank = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta blankMeta = blank.getItemMeta();
        blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
        blank.setItemMeta(blankMeta);

        for (int i = 0; i <= 8; i++) {
            gui.setItem(i, blank);
        }
        for (int i = 27; i <= 35; i++) {
            gui.setItem(i, blank);
        }

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exit.getItemMeta();
        exitMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
        exit.setItemMeta(exitMeta);
        gui.setItem(31, exit);

        // Blacklist
        if (player.hasPermission("aranarth.blacklist")) {
            boolean active = aranarthPlayer.getBlacklistingMethod() != -1;
            gui.setItem(9, buildToggleItem(Material.LAVA_BUCKET, "&f&lBlacklist", active));
        } else {
            gui.setItem(9, buildLockedItem(Material.LAVA_BUCKET, "&f&lBlacklist"));
        }

        // Blue Fire
        if (aranarthPlayer.getPerks().containsKey(Perk.BLUEFIRE) && aranarthPlayer.getPerks().get(Perk.BLUEFIRE) == 1) {
            gui.setItem(10, buildToggleItem(Material.SOUL_CAMPFIRE, "&f&lBlue Fire", !aranarthPlayer.hasBlueFireDisabled()));
        } else {
            gui.setItem(10, buildLockedItem(Material.SOUL_CAMPFIRE, "&f&lBlue Fire"));
        }

        // Bulk Sell Shulker
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(11, buildToggleItem(Material.PURPLE_SHULKER_BOX, "&f&lBulk Sell Shulker", aranarthPlayer.isBulkSellShulkerEnabled()));
        } else {
            gui.setItem(11, buildLockedItem(Material.PURPLE_SHULKER_BOX, "&f&lBulk Sell Shulker"));
        }

        // Dominion Claim Messages
        gui.setItem(12, buildToggleItem(Material.WHITE_BANNER, "&f&lDominion Claim Messages", !aranarthPlayer.isTogglingChangeClaim()));

        // Chat
        if (player.hasPermission("aranarth.toggle.chat")) {
            gui.setItem(13, buildToggleItem(Material.WRITTEN_BOOK, "&f&lChat", !aranarthPlayer.isTogglingChat()));
        } else {
            gui.setItem(13, buildLockedItem(Material.WRITTEN_BOOK, "&f&lChat"));
        }

        // Chest Lock
        gui.setItem(14, buildToggleItem(Material.TRIAL_KEY, "&f&lChest Lock", aranarthPlayer.isAutoLockingChests()));

        // Compressor
        if (player.hasPermission("aranarth.compressor")) {
            gui.setItem(15, buildToggleItem(Material.PISTON, "&f&lCompressor", aranarthPlayer.isCompressingItems()));
        } else {
            gui.setItem(15, buildLockedItem(Material.PISTON, "&f&lCompressor"));
        }

        // Day Message
        gui.setItem(16, buildToggleItem(Material.CLOCK, "&f&lNew Day Message", !aranarthPlayer.isDayMessageDisabled()));

        // Gate Creation
        if (player.hasPermission("aranarth.gate")) {
            gui.setItem(17, buildToggleItem(Material.IRON_BARS, "&f&lGate Creation", GateUtils.isInGatePlacementMode(player.getUniqueId())));
        } else {
            gui.setItem(17, buildLockedItem(Material.IRON_BARS, "&f&lGate Creation"));
        }

        // Gradient Chat
        boolean hasGradientAccess = aranarthPlayer.getPerks().containsKey(Perk.CHAT) || aranarthPlayer.getSaintRank() >= 2;
        if (hasGradientAccess) {
            gui.setItem(18, buildToggleItem(Material.ORANGE_GLAZED_TERRACOTTA, "&f&lGradient Chat", aranarthPlayer.isGradientChatEnabled()));
        } else {
            gui.setItem(18, buildLockedItem(Material.ORANGE_GLAZED_TERRACOTTA, "&f&lGradient Chat"));
        }

        // Inventory Assist
        if (player.hasPermission("aranarth.inventory")) {
            gui.setItem(19, buildToggleItem(Material.CHEST, "&f&lInventory Assist", !aranarthPlayer.isTogglingInventoryAssist()));
        } else {
            gui.setItem(19, buildLockedItem(Material.CHEST, "&f&lInventory Assist"));
        }

        // Private Messages
        if (player.hasPermission("aranarth.toggle.msg")) {
            gui.setItem(20, buildToggleItem(Material.PAPER, "&f&lPrivate Messages", !aranarthPlayer.isTogglingMessages()));
        } else {
            gui.setItem(20, buildLockedItem(Material.PAPER, "&f&lPrivate Messages"));
        }

        // Pet Hurt
        gui.setItem(21, buildToggleItem(Material.NAME_TAG, "&f&lPet Hurt", aranarthPlayer.isHurtingOwnPets()));

        // Shulker Assist
        if (player.hasPermission("aranarth.shulker")) {
            gui.setItem(22, buildToggleItem(Material.SHULKER_BOX, "&f&lShulker Assist", aranarthPlayer.isAddingToShulker()));
        } else {
            gui.setItem(22, buildLockedItem(Material.SHULKER_BOX, "&f&lShulker Assist"));
        }

        // Spawn Boost
        gui.setItem(23, buildToggleItem(Material.FEATHER, "&f&lSpawn Boost", aranarthPlayer.isUsingSpawnBoost()));

        // Teleport Requests
        if (player.hasPermission("aranarth.toggle.tp")) {
            gui.setItem(24, buildToggleItem(Material.ENDER_PEARL, "&f&lTeleport Requests", !aranarthPlayer.isTogglingTp()));
        } else {
            gui.setItem(24, buildLockedItem(Material.ENDER_PEARL, "&f&lTeleport Requests"));
        }

        // Weather Messages
        gui.setItem(25, buildToggleItem(Material.WIND_CHARGE, "&f&lWeather Messages", !aranarthPlayer.isWeatherMessageDisabled()));

        // Dominion Msg Compact
        gui.setItem(26, buildToggleItem(Material.COMPASS, "&f&lDominion Msg Compact", aranarthPlayer.isDominionMsgCompact()));

        return gui;
    }

    private ItemStack buildToggleItem(Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String status = active ? "&a&lActive" : "&c&lInactive";
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- " + status));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLockedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name + " &7&l- &8&lLocked"));
        item.setItemMeta(meta);
        return item;
    }

}
