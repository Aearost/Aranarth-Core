package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
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

import java.text.NumberFormat;
import java.util.*;

public class GuiTopGuesses {

    // Caches the most recent total player count per viewer
    private static final Map<UUID, Integer> totalCountCache = new HashMap<>();

    public static int getCachedTotalCount(UUID viewerUuid) {
        return totalCountCache.getOrDefault(viewerUuid, AranarthUtils.getTopGuesses().size());
    }

    private final Player player;
    private final Inventory initializedGui;

    private GuiTopGuesses(Player player, int pageNum, List<UUID> sortedUuids,
                          Map<UUID, DatabaseManager.ChatGameEntry> dbData,
                          Map<UUID, PlayerProfile> profiles) {
        this.player = player;
        this.initializedGui = initializeGui(pageNum, sortedUuids, dbData, profiles);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    /**
     * Opens the GUI showing player heads sorted by most chat game guesses.
     */
    public static void open(Player player, int pageNum) {
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            // Fetch cross-server data from DB when available
            final Map<UUID, DatabaseManager.ChatGameEntry> dbData;
            final List<UUID> sortedUuids;
            if (DatabaseManager.isActive()) {
                dbData = DatabaseManager.getInstance().loadAllChatGameGuesses();
                sortedUuids = dbData.entrySet().stream()
                        .filter(e -> e.getValue().guessCount() > 0)
                        .sorted((a, b) -> Integer.compare(b.getValue().guessCount(), a.getValue().guessCount()))
                        .map(Map.Entry::getKey)
                        .toList();
            } else {
                dbData = Collections.emptyMap();
                sortedUuids = AranarthUtils.getTopGuesses();
            }

            int startIndex = pageNum * 45;
            List<UUID> pageUuids = new ArrayList<>();
            for (int i = startIndex; i < Math.min(startIndex + 45, sortedUuids.size()); i++) {
                pageUuids.add(sortedUuids.get(i));
            }

            Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
            for (UUID uuid : pageUuids) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                PlayerProfile profile = Bukkit.createProfile(uuid, op.getName());
                profile.complete(true);
                profiles.put(uuid, profile);
            }

            final int totalCount = sortedUuids.size();
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                totalCountCache.put(player.getUniqueId(), totalCount);
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setCurrentGuiPageNum(pageNum);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                new GuiTopGuesses(player, pageNum, sortedUuids, dbData, profiles).openGui();
            });
        });
    }

    private Inventory initializeGui(int pageNum, List<UUID> sortedUuids,
                                    Map<UUID, DatabaseManager.ChatGameEntry> dbData,
                                    Map<UUID, PlayerProfile> profiles) {
        int startIndex = pageNum * 45;
        Inventory gui = Bukkit.getServer().createInventory(player, 54, "Top Guesses");

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

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(0);

        for (int i = 0; i < 45; i++) {
            if (startIndex + i >= sortedUuids.size()) {
                gui.setItem(i, blank);
                continue;
            }

            UUID uuid = sortedUuids.get(startIndex + i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

            PlayerProfile profile = profiles.get(uuid);
            if (profile != null) {
                skullMeta.setPlayerProfile(profile);
            }

            // Resolve display name
            String displayName;
            AranarthPlayer localPlayer = AranarthUtils.getPlayer(uuid);
            if (localPlayer != null) {
                displayName = localPlayer.getNickname();
            } else {
                NetworkPlayer remote = NetworkManager.isActive()
                        ? NetworkManager.getInstance().getRemotePlayer(uuid) : null;
                if (remote != null && !remote.getNickname().isEmpty()) {
                    displayName = remote.getNickname();
                } else {
                    DatabaseManager.ChatGameEntry entry = dbData.get(uuid);
                    if (entry != null && !entry.nickname().isEmpty()) {
                        displayName = ChatUtils.stripColorFormatting(entry.nickname());
                    } else if (entry != null && !entry.username().isEmpty()) {
                        displayName = entry.username();
                    } else {
                        displayName = uuid.toString();
                    }
                }
            }

            // Resolve stats
            final int guessCount;
            final double earnings;
            final double bestTime;
            if (!dbData.isEmpty()) {
                DatabaseManager.ChatGameEntry entry = dbData.get(uuid);
                guessCount = entry != null ? entry.guessCount() : 0;
                earnings = entry != null ? entry.totalEarnings() : 0.0;
                bestTime = entry != null ? entry.bestTime() : 0.0;
            } else {
                guessCount = AranarthUtils.getChatGameGuesses().getOrDefault(uuid, 0);
                earnings = AranarthUtils.getChatGameEarnings().getOrDefault(uuid, 0.0);
                bestTime = AranarthUtils.getChatGameBestTimes().getOrDefault(uuid, 0.0);
            }

            skullMeta.setDisplayName(ChatUtils.translateToColor("&e" + displayName));
            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.translateToColor("&6&o$" + nf.format(Math.round(earnings)) + " &7&ototal earned"));
            lore.add(ChatUtils.translateToColor("&e&o" + guessCount + " &7&ocorrect guess" + (guessCount == 1 ? "" : "es")));
            lore.add(ChatUtils.translateToColor("&7&oBest speed: &e&o" + (bestTime > 0 ? String.format("%.2f", bestTime) + "s" : "N/A")));
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
            gui.setItem(i, head);
        }
        return gui;
    }
}
