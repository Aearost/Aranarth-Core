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
 * A GUI displaying the player's 7-day login streak progress and rewards.
 */
public class GuiLoginStreak {

    public static final int[] DAY_SLOTS = {19, 20, 21, 22, 23, 24, 25};

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
        Inventory inv = Bukkit.createInventory(player, 45, ChatUtils.translateToColor("&6&lLogin Streak"));

        UUID uuid = player.getUniqueId();

        // Validate streak (resets if a day was missed) before rendering
        LoginStreakUtils.ensureStreakValid(uuid);

        ItemStack yellowPane = makePane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack blackPane  = makePane(Material.BLACK_STAINED_GLASS_PANE);

        // Row 0
        inv.setItem(0, yellowPane);
        inv.setItem(1, yellowPane);
        inv.setItem(2, blackPane);
        inv.setItem(3, blackPane);
        inv.setItem(4, blackPane);
        inv.setItem(5, blackPane);
        inv.setItem(6, blackPane);
        inv.setItem(7, yellowPane);
        inv.setItem(8, yellowPane);

        // Row 1 — borders; day items filled below
        inv.setItem(9, blackPane);
        inv.setItem(10, yellowPane);
        inv.setItem(11, blackPane);
        inv.setItem(12, blackPane);
        inv.setItem(13, blackPane);
        inv.setItem(14, blackPane);
        inv.setItem(15, blackPane);
        inv.setItem(16, yellowPane);
        inv.setItem(17, blackPane);

        // Row 2
        inv.setItem(18, blackPane);
        inv.setItem(19, yellowPane);
        inv.setItem(20, blackPane);
        inv.setItem(21, blackPane);
        inv.setItem(22, blackPane);
        inv.setItem(23, blackPane);
        inv.setItem(24, blackPane);
        inv.setItem(25, yellowPane);
        inv.setItem(26, blackPane);

        // Row 3
        inv.setItem(27, blackPane);
        inv.setItem(28, yellowPane);
        inv.setItem(29, blackPane);
        inv.setItem(30, blackPane);
        inv.setItem(31, blackPane);
        inv.setItem(32, blackPane);
        inv.setItem(33, blackPane);
        inv.setItem(34, yellowPane);
        inv.setItem(35, blackPane);

        // Row 4
        inv.setItem(36, yellowPane);
        inv.setItem(37, yellowPane);
        inv.setItem(38, blackPane);
        inv.setItem(39, blackPane);
        inv.setItem(40, blackPane);
        inv.setItem(41, blackPane);
        inv.setItem(42, blackPane);
        inv.setItem(43, yellowPane);
        inv.setItem(44, yellowPane);

        int currentDay = LoginStreakUtils.getStreakDay(uuid);
        boolean canClaim = LoginStreakUtils.canClaim(uuid);

        // Day items
        for (int i = 0; i < 7; i++) {
            inv.setItem(DAY_SLOTS[i], makeDayItem(i + 1, currentDay, canClaim, rank));
        }

        // Info item
        inv.setItem(31, makeInfoItem(currentDay, canClaim));

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
        lore.add(ChatUtils.translateToColor("&7Current Streak Day: &f" + currentDay + "&7/&f7"));
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
