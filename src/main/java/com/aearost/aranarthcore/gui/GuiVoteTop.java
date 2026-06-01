package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

public class GuiVoteTop {

    private final Player player;
    private final Inventory initializedGui;

    private GuiVoteTop(Player player, String title, List<Map.Entry<UUID, Integer>> pageEntries,
                       Map<UUID, PlayerProfile> profiles) {
        this.player = player;
        this.initializedGui = initializeGui(title, pageEntries, profiles);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    /**
     * Computes the sorted voter list for the given filter and opens the GUI at the requested page.
     *
     * @param player  The player opening the GUI.
     * @param year    Year filter, or null for all-time.
     * @param month   Month filter (1-12), or null.
     * @param pageNum Zero-based page number.
     */
    public static void open(Player player, Integer year, Integer month, int pageNum) {
        List<AranarthVote> allVotes = AranarthUtils.getVotes();

        long startTime;
        long endTime;
        String title;

        if (year != null && month != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            startTime = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            endTime = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            title = String.format("Top Voters Month %02d-%d", month, year);
        } else if (year != null) {
            startTime = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            endTime = LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_000_000)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            title = "Top Voters Year " + year;
        } else {
            startTime = Long.MIN_VALUE;
            endTime = Long.MAX_VALUE;
            title = "Top Voters";
        }

        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (AranarthVote vote : allVotes) {
            if (vote.getTimestamp() >= startTime && vote.getTimestamp() <= endTime) {
                voteCounts.merge(vote.getUuid(), 1, Integer::sum);
            }
        }

        List<Map.Entry<UUID, Integer>> sorted = voteCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        if (sorted.isEmpty()) {
            player.sendMessage(ChatUtils.chatMessage("&7There are no votes in this period"));
            return;
        }

        int startIndex = pageNum * 45;
        List<Map.Entry<UUID, Integer>> pageEntries = new ArrayList<>();
        for (int i = startIndex; i < Math.min(startIndex + 45, sorted.size()); i++) {
            pageEntries.add(sorted.get(i));
        }

        // If the requested page is empty and it's not page 0, wrap to page 0
        if (pageEntries.isEmpty() && pageNum > 0) {
            open(player, year, month, 0);
            return;
        }

        final String finalTitle = title;
        final List<Map.Entry<UUID, Integer>> finalPageEntries = pageEntries;

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
            for (Map.Entry<UUID, Integer> entry : finalPageEntries) {
                UUID uuid = entry.getKey();
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                PlayerProfile profile = Bukkit.createProfile(uuid, op.getName());
                profile.complete(true);
                profiles.put(uuid, profile);
            }

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setCurrentGuiPageNum(pageNum);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                new GuiVoteTop(player, finalTitle, finalPageEntries, profiles).openGui();
            });
        });
    }

    private Inventory initializeGui(String title, List<Map.Entry<UUID, Integer>> pageEntries,
                                    Map<UUID, PlayerProfile> profiles) {
        Inventory gui = Bukkit.getServer().createInventory(player, 54, title);

        ItemStack previous = new ItemStack(Material.RED_WOOL);
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemStack next = new ItemStack(Material.LIME_WOOL);
        ItemStack blank = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);

        ItemMeta previousMeta = previous.getItemMeta();
        if (Objects.nonNull(previousMeta)) {
            previousMeta.setDisplayName(ChatUtils.translateToColor("&c&lPrevious"));
            previous.setItemMeta(previousMeta);
        }

        ItemMeta barrierMeta = barrier.getItemMeta();
        if (Objects.nonNull(barrierMeta)) {
            barrierMeta.setDisplayName(ChatUtils.translateToColor("&4&lExit"));
            barrier.setItemMeta(barrierMeta);
        }

        ItemMeta nextMeta = next.getItemMeta();
        if (Objects.nonNull(nextMeta)) {
            nextMeta.setDisplayName(ChatUtils.translateToColor("&a&lNext"));
            next.setItemMeta(nextMeta);
        }

        ItemMeta blankMeta = blank.getItemMeta();
        if (Objects.nonNull(blankMeta)) {
            blankMeta.setDisplayName(ChatUtils.translateToColor("&f"));
            blank.setItemMeta(blankMeta);
        }

        gui.setItem(45, previous);
        gui.setItem(46, blank);
        gui.setItem(47, blank);
        gui.setItem(48, blank);
        gui.setItem(49, barrier);
        gui.setItem(50, blank);
        gui.setItem(51, blank);
        gui.setItem(52, blank);
        gui.setItem(53, next);

        for (int i = 0; i < 45; i++) {
            if (i >= pageEntries.size()) {
                gui.setItem(i, blank);
                continue;
            }

            Map.Entry<UUID, Integer> entry = pageEntries.get(i);
            UUID uuid = entry.getKey();
            int voteCount = entry.getValue();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

            PlayerProfile profile = profiles.get(uuid);
            if (profile != null) {
                skullMeta.setPlayerProfile(profile);
            }

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            String displayName = aranarthPlayer != null
                    ? aranarthPlayer.getNickname()
                    : (profile != null && profile.getName() != null ? profile.getName() : uuid.toString());
            skullMeta.setDisplayName(ChatUtils.translateToColor("&e" + displayName));

            List<String> lore = new ArrayList<>();
            String voteWord = voteCount == 1 ? "vote" : "votes";
            lore.add(ChatUtils.translateToColor("&6" + voteCount + " &e" + voteWord));
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
            gui.setItem(i, head);
        }
        return gui;
    }
}
