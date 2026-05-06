package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.LoginStreakUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A GUI displaying the player's 28-day (4-week) login streak progress and rewards.
 */
public class GuiLoginStreak {

    // 28 slots, one per streak day, ordered days 1-28
    public static final int[] DAY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,   // Week 1: days  1-7
            19, 20, 21, 22, 23, 24, 25,   // Week 2: days  8-14
            28, 29, 30, 31, 32, 33, 34,   // Week 3: days 15-21
            37, 38, 39, 40, 41, 42, 43    // Week 4: days 22-28
    };

    private static final int INFO_SLOT = 49;

    private final Player player;
    private final Inventory gui;
    private final int rank;

    public GuiLoginStreak(Player player) {
        this.player = player;
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        this.rank = aranarthPlayer.getRank();
        this.gui = initializeGui();
    }

    public void openGui() {
        player.openInventory(gui);
    }

    private Inventory initializeGui() {
        Inventory inv = Bukkit.createInventory(player, 54, ChatUtils.translateToColor("&6&lLogin Streak"));

        UUID uuid = player.getUniqueId();

        // Validate streak (resets if a day was missed) before rendering
        LoginStreakUtils.ensureStreakValid(uuid);

        ItemStack blackPane  = makePane(Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(0,  blackPane);
        inv.setItem(1,  blackPane);
        inv.setItem(2,  blackPane);
        inv.setItem(3,  blackPane);
        inv.setItem(4,  blackPane);
        inv.setItem(5,  blackPane);
        inv.setItem(6,  blackPane);
        inv.setItem(7,  blackPane);
        inv.setItem(8,  blackPane);

        inv.setItem(9,  blackPane);
        inv.setItem(17, blackPane);

        inv.setItem(18, blackPane);
        inv.setItem(26, blackPane);

        inv.setItem(27, blackPane);
        inv.setItem(35, blackPane);

        inv.setItem(36, blackPane);
        inv.setItem(44, blackPane);

        inv.setItem(45, blackPane);
        inv.setItem(46, blackPane);
        inv.setItem(47, blackPane);
        inv.setItem(48, blackPane);
        inv.setItem(50, blackPane);
        inv.setItem(51, blackPane);
        inv.setItem(52, blackPane);
        inv.setItem(53, blackPane);

        int currentDay = LoginStreakUtils.getStreakDay(uuid);
        boolean canClaim = LoginStreakUtils.canClaim(uuid);

        // Day items
        for (int i = 0; i < DAY_SLOTS.length; i++) {
            inv.setItem(DAY_SLOTS[i], makeDayItem(i + 1, currentDay, canClaim, rank));
        }

        // Info item
        inv.setItem(INFO_SLOT, makeInfoItem(currentDay, canClaim));

        return inv;
    }

    /**
     * Builds the item for a given streak day, reflecting its claim state.
     */
    public static ItemStack makeDayItem(int day, int currentDay, boolean canClaim, int rank) {
        Material mat;
        String statusColor;
        String statusText;

        if (day < currentDay) {
            mat = Material.GRAY_CONCRETE;
            statusColor = "&7";
            statusText = "Claimed";
        } else if (day == currentDay && canClaim) {
            mat = Material.LIME_CONCRETE;
            statusColor = "&a";
            statusText = "Click to Claim!";
        } else if (day == currentDay) {
            mat = Material.YELLOW_CONCRETE;
            statusColor = "&e";
            statusText = "Already Claimed Today";
        } else {
            mat = Material.RED_CONCRETE;
            statusColor = "&c";
            statusText = "Locked";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&f&lDay " + day));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&8Login Streak Reward"));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Reward: " + LoginStreakUtils.getRewardDisplayName(day, rank)));
        lore.add("");
        lore.add(ChatUtils.translateToColor(statusColor + statusText));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeInfoItem(int currentDay, boolean canClaim) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&e&lStreak Info"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Current Streak Day: &f" + currentDay + "&7/&f28"));
        lore.add("");
        if (canClaim) {
            lore.add(ChatUtils.translateToColor("&aYou can claim today's reward!"));
            lore.add(ChatUtils.translateToColor("&7Click &aDay " + currentDay + " &7to claim."));
        } else {
            lore.add(ChatUtils.translateToColor("&7You have already claimed today."));
            lore.add(ChatUtils.translateToColor("&7Come back tomorrow!"));
        }
        lore.add("");
        lore.add(ChatUtils.translateToColor("&7Rewards available at &f12:00 AM EST &7daily."));
        lore.add(ChatUtils.translateToColor("&cMissing a day resets your streak!"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(" "));
        pane.setItemMeta(meta);
        return pane;
    }
}
