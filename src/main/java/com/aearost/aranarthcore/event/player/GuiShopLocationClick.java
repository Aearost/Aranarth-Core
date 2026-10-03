package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiShopLocation;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.PendingTeleport;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Shop;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.ShopIslandUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Handles the teleport logic and search functionality for the shop location GUI.
 */
public class GuiShopLocationClick {

    public void execute(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        e.setCancelled(true);

        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }

        int slot = e.getSlot();
        Inventory topInv = e.getView().getTopInventory();
        int currentPage = GuiShopLocation.playerPage.getOrDefault(player.getUniqueId(), 0);
        String filter = GuiShopLocation.getActiveFilter(player.getUniqueId());

        int totalEntries = (filter != null)
                ? GuiShopLocation.getFilteredShops(filter).size()
                : AranarthUtils.getShopLocations().size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / GuiShopLocation.ITEMS_PER_PAGE));

        if (slot == GuiShopLocation.SLOT_SEARCH) {
            if (e.getCurrentItem() == null) return;
            if (e.getCurrentItem().getType() == Material.SPYGLASS) {
                GuiShopLocation.initiateSearch(player);
            } else if (e.getCurrentItem().getType() == Material.ARROW) {
                GuiShopLocation.clearFilter(player.getUniqueId());
                GuiShopLocation.open(player, 0);
            }
        } else if (slot == GuiShopLocation.SLOT_PREV) {
            if (currentPage > 0) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                if (filter != null) {
                    GuiShopLocation.refreshInPlace(player, currentPage - 1, filter, topInv);
                } else {
                    GuiShopLocation.open(player, currentPage - 1);
                }
            }
        } else if (slot == GuiShopLocation.SLOT_NEXT) {
            if (currentPage < totalPages - 1) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
                if (filter != null) {
                    GuiShopLocation.refreshInPlace(player, currentPage + 1, filter, topInv);
                } else {
                    GuiShopLocation.open(player, currentPage + 1);
                }
            }
        } else if (slot == GuiShopLocation.SLOT_CLOSE) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.25F, 1);
            player.closeInventory();
        } else if (slot >= GuiShopLocation.ITEMS_START && slot <= GuiShopLocation.ITEMS_END) {
            int itemIndex = currentPage * GuiShopLocation.ITEMS_PER_PAGE + (slot - GuiShopLocation.ITEMS_START);
            UUID targetUuid = null;

            if (filter != null) {
                List<Shop> results = GuiShopLocation.getFilteredShops(filter);
                if (itemIndex >= results.size()) return;
                targetUuid = results.get(itemIndex).getUuid();
            } else {
                HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
                List<UUID> uuidList = new ArrayList<>(shopLocations.keySet());
                if (itemIndex >= uuidList.size()) return;
                targetUuid = uuidList.get(itemIndex);
            }

            if (targetUuid == null) return;

            HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
            Location shopLoc = shopLocations.get(targetUuid);
            if (shopLoc == null) return;

            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            AranarthPlayer shopOwnerPlayer = AranarthUtils.getPlayer(targetUuid);
            String defaultName = shopOwnerPlayer.getNickname() + "'s Shop";
            String shopName = AranarthUtils.getShopName(targetUuid, defaultName);

            player.closeInventory();

            if (AranarthCore.isSmpServer() && NetworkManager.isActive() && shopLoc.getWorld() == null) {
                AranarthUtils.teleportPlayer(player, player.getLocation(), player.getLocation(),
                        aranarthPlayer.isInAdminMode(), shopName, "&7Transferring to shop...", success -> {
                    if (success) {
                        PendingTeleport pt = new PendingTeleport(
                                ShopIslandUtils.SHOPS_WORLD,
                                shopLoc.getX(), shopLoc.getY(), shopLoc.getZ(),
                                shopLoc.getYaw(), shopLoc.getPitch(),
                                "&e&l" + shopName, "&7You have teleported to " + shopName);
                        String survivalServerName = AranarthCore.getInstance().getConfig()
                                .getString("network.servers.survival", "survival");
                        NetworkManager.getInstance().saveInventoryAndTransfer(player, survivalServerName, pt);
                    }
                });
            } else {
                AranarthUtils.teleportPlayer(player, player.getLocation(), shopLoc, aranarthPlayer.isInAdminMode(),
                        shopName, Lang.getFor(player, "shop.teleported", "name", shopName), success -> {
                    if (success) {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.teleported", "name", shopName)));
                    } else {
                        player.sendMessage(ChatUtils.chatMessage(Lang.get("shop.teleport_failed", "name", shopName)));
                    }
                });
            }
        }
    }
}
