package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
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

import java.util.*;

public class GuiTopDeaths {

    private final Player player;
    private final Inventory initializedGui;

    private GuiTopDeaths(Player player, int pageNum, Map<UUID, PlayerProfile> profiles) {
        this.player = player;
        this.initializedGui = initializeGui(player, pageNum, profiles);
    }

    public void openGui() {
        player.closeInventory();
        if (initializedGui != null) {
            player.openInventory(initializedGui);
        }
    }

    /**
     * Opens the GUI and shows the player heads with the top deaths.
     */
    public static void open(Player player, int pageNum) {
        List<UUID> uuidList = AranarthUtils.getTopDeaths(player.getWorld());
        int startIndex = pageNum * 45;
        List<UUID> pageUuids = new ArrayList<>();
        for (int i = startIndex; i < Math.min(startIndex + 45, uuidList.size()); i++) {
            pageUuids.add(uuidList.get(i));
        }

        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            Map<UUID, PlayerProfile> profiles = new LinkedHashMap<>();
            for (UUID uuid : pageUuids) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                PlayerProfile profile = Bukkit.createProfile(uuid, op.getName());
                profile.complete(true);
                profiles.put(uuid, profile);
            }

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                aranarthPlayer.setCurrentGuiPageNum(pageNum);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                new GuiTopDeaths(player, pageNum, profiles).openGui();
            });
        });
    }

    private Inventory initializeGui(Player player, int pageNum, Map<UUID, PlayerProfile> profiles) {
        List<UUID> uuidList = AranarthUtils.getTopDeaths(player.getWorld());
        int startIndex = pageNum * 45;
        Inventory gui = Bukkit.getServer().createInventory(player, 54, "Top Deaths");

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
            if (i >= uuidList.size()) {
                gui.setItem(i, blank);
                continue;
            }

            UUID uuid = uuidList.get(startIndex + i);
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();

            PlayerProfile profile = profiles.get(uuid);
            if (profile != null) {
                skullMeta.setPlayerProfile(profile);
            }

            skullMeta.setDisplayName(ChatUtils.translateToColor("&e" + aranarthPlayer.getNickname()));
            List<String> lore = new ArrayList<>();
            int deathCount = AranarthUtils.getKillsOrDeathsInWorld(uuid, player.getWorld(), false);
            lore.add(ChatUtils.translateToColor("&e" + deathCount + " deaths"));
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
            gui.setItem(i, head);
        }
        return gui;
    }

}
