package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class GuiShopLocation {

    private final Player player;
    private final Inventory initializedGui;

    private GuiShopLocation(Player player, int pageNum, Map<UUID, PlayerProfile> profiles) {
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
     * Opens the GUI showing all shop owners' heads.
     */
    public static void open(Player player, int pageNum) {
        HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
        List<UUID> uuidList = new ArrayList<>(shopLocations.keySet());
        int startIndex = pageNum * 27;
        List<UUID> pageUuids = new ArrayList<>();
        for (int i = startIndex; i < Math.min(startIndex + 27, uuidList.size()); i++) {
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
                new GuiShopLocation(player, pageNum, profiles).openGui();
            });
        });
    }

    private Inventory initializeGui(Player player, int pageNum, Map<UUID, PlayerProfile> profiles) {
        HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
        int shopLocationStartIndex = pageNum * 27;
        Inventory gui = Bukkit.getServer().createInventory(player, 36, "Player Shops");

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

        gui.setItem(27, previous);
        gui.setItem(28, blank);
        gui.setItem(29, blank);
        gui.setItem(30, blank);
        gui.setItem(31, barrier);
        gui.setItem(32, blank);
        gui.setItem(33, blank);
        gui.setItem(34, blank);
        gui.setItem(35, next);

        List<UUID> uuidList = new ArrayList<>(shopLocations.keySet());

        for (int i = 0; i < 27; i++) {
            if (i >= shopLocations.size()) {
                gui.setItem(i, blank);
                continue;
            }

            UUID uuid = uuidList.get(shopLocationStartIndex + i);
            AranarthPlayer shopLocationPlayer = AranarthUtils.getPlayer(uuid);
            ItemStack shopItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta shopItemMeta = (SkullMeta) shopItem.getItemMeta();

            PlayerProfile profile = profiles.get(uuid);
            if (profile != null) {
                shopItemMeta.setPlayerProfile(profile);
            }

            String defaultName = shopLocationPlayer.getNickname() + "'s Shop";
            String shopDisplayName = AranarthUtils.getShopName(uuid, defaultName);
            shopItemMeta.setDisplayName(ChatUtils.translateToColor("&e" + shopDisplayName));
            shopItem.setItemMeta(shopItemMeta);
            gui.setItem(i, shopItem);
        }
        return gui;
    }

}
