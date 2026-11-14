package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.enums.PunishmentCategory;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.network.NetworkPlayer;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Punishment;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * The /ac user hub, where the Council can reach every tool for managing a player from one place.
 */
public class GuiUser {

    public static final int SLOT_HEAD = 4;
    public static final int SLOT_LOCATION = 10;
    public static final int SLOT_INVENTORY = 12;
    public static final int SLOT_ENDER_CHEST = 14;
    public static final int SLOT_HISTORY = 16;
    public static final int SLOT_UNMUTE = 20;
    public static final int SLOT_ISSUE = 22;
    public static final int SLOT_UNBAN = 24;

    /**
     * Identifies the hub and the player it manages.
     */
    public record HubData(UUID targetUuid, String username) {
    }

    private final Player viewer;
    private final UUID targetUuid;
    private final String username;

    public GuiUser(Player viewer, UUID targetUuid, String username) {
        this.viewer = viewer;
        this.targetUuid = targetUuid;
        this.username = username;
    }

    public void openGui() {
        Inventory gui = GuiHolder.create(viewer, 27, Lang.getFor(viewer, "user.hub_title", "name", username));
        GuiHolder.of(gui).setData(new HubData(targetUuid, username));

        ItemStack pane = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, pane);
        }

        gui.setItem(SLOT_HEAD, buildHead());
        if (viewer.hasPermission("aranarth.whereis")) {
            gui.setItem(SLOT_LOCATION, buildItem(Material.COMPASS, Lang.getFor(viewer, "user.hub_location_name"),
                    List.of(Lang.getFor(viewer, "user.hub_location_lore"))));
        }
        if (viewer.hasPermission("aranarth.invsee")) {
            gui.setItem(SLOT_INVENTORY, buildItem(Material.CHEST, Lang.getFor(viewer, "user.hub_inventory_name"),
                    List.of(Lang.getFor(viewer, "user.hub_inventory_lore"))));
            gui.setItem(SLOT_ENDER_CHEST, buildItem(Material.ENDER_CHEST, Lang.getFor(viewer, "user.hub_ender_name"),
                    List.of(Lang.getFor(viewer, "user.hub_ender_lore"))));
        }
        if (viewer.hasPermission("aranarth.punishments")) {
            List<Punishment> punishments = AranarthUtils.getPunishments(targetUuid);
            int count = punishments == null ? 0 : punishments.size();
            gui.setItem(SLOT_HISTORY, buildItem(Material.WRITABLE_BOOK, Lang.getFor(viewer, "user.hub_history_name"),
                    List.of(Lang.getFor(viewer, "user.hub_history_lore", "count", count))));
        }
        if (viewer.hasPermission("aranarth.warn") || viewer.hasPermission("aranarth.mute") || viewer.hasPermission("aranarth.ban")) {
            gui.setItem(SLOT_ISSUE, buildItem(Material.ANVIL, Lang.getFor(viewer, "user.hub_issue_name"),
                    List.of(Lang.getFor(viewer, "user.hub_issue_lore"))));
        }
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
        if (viewer.hasPermission("aranarth.unmute") && aranarthPlayer != null && !aranarthPlayer.getMuteEndDate().isEmpty()) {
            gui.setItem(SLOT_UNMUTE, buildItem(Material.MILK_BUCKET, Lang.getFor(viewer, "user.hub_unmute_name"),
                    List.of(Lang.getFor(viewer, "user.hub_unmute_lore"))));
        }
        if (viewer.hasPermission("aranarth.unban") && getBanEntry(targetUuid) != null) {
            gui.setItem(SLOT_UNBAN, buildItem(Material.OAK_DOOR, Lang.getFor(viewer, "user.hub_unban_name"),
                    List.of(Lang.getFor(viewer, "user.hub_unban_lore"))));
        }
        viewer.openInventory(gui);
    }

    /**
     * Handles a click in the hub by running the matching /ac user action, which checks its own permission.
     */
    public static void handleClick(InventoryClickEvent e, HubData data) {
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        String action = switch (e.getSlot()) {
            case SLOT_LOCATION -> "location";
            case SLOT_INVENTORY -> "inventory";
            case SLOT_ENDER_CHEST -> "enderchest";
            case SLOT_HISTORY -> "history";
            case SLOT_ISSUE -> "issue";
            case SLOT_UNMUTE -> "unmute";
            case SLOT_UNBAN -> "unban";
            default -> null;
        };
        // Empty slots are filled with panes, so only real buttons run an action
        ItemStack clicked = e.getCurrentItem();
        if (action == null || clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }
        player.closeInventory();
        player.performCommand("ac user " + data.username() + " " + action);
    }

    /**
     * Provides the player's ban on this server, or null if they are not banned.
     */
    public static BanEntry<?> getBanEntry(UUID uuid) {
        ProfileBanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
        return banList.getBanEntry(Bukkit.getOfflinePlayer(uuid).getPlayerProfile());
    }

    private ItemStack buildHead() {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(ChatUtils.translateToColor("&e" + AranarthUtils.getNickname(target)));

        List<String> lore = new ArrayList<>();
        lore.add(getStatusLine());

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(targetUuid);
        String muteEndDate = aranarthPlayer == null ? "" : aranarthPlayer.getMuteEndDate();
        if (muteEndDate.isEmpty()) {
            lore.add(Lang.getFor(viewer, "user.hub_not_muted"));
        } else if (muteEndDate.equals("none")) {
            lore.add(Lang.getFor(viewer, "user.hub_muted_permanent"));
        } else {
            lore.add(Lang.getFor(viewer, "user.hub_muted_until", "until", formatMuteEndDate(muteEndDate)));
        }

        BanEntry<?> ban = getBanEntry(targetUuid);
        if (ban == null) {
            lore.add(Lang.getFor(viewer, "user.hub_not_banned"));
        } else if (ban.getExpiration() == null) {
            lore.add(Lang.getFor(viewer, "user.hub_banned_permanent"));
        } else {
            lore.add(Lang.getFor(viewer, "user.hub_banned_until", "until", formatDate(ban.getExpiration())));
        }

        lore.add("");
        lore.add(Lang.getFor(viewer, "user.hub_offences"));
        List<Punishment> punishments = AranarthUtils.getPunishments(targetUuid);
        for (PunishmentCategory category : PunishmentCategory.values()) {
            lore.add(Lang.getFor(viewer, "user.hub_offence_line",
                    "category", Lang.getFor(viewer, "user.category_" + category.getKey()),
                    "count", category.countOffences(punishments)));
        }
        meta.setLore(lore.stream().map(ChatUtils::translateToColor).toList());
        head.setItemMeta(meta);
        return head;
    }

    private String getStatusLine() {
        if (Bukkit.getPlayer(targetUuid) != null) {
            return Lang.getFor(viewer, "user.hub_status_here");
        }
        if (NetworkManager.isActive()) {
            NetworkPlayer remote = NetworkManager.getInstance().getRemotePlayer(targetUuid);
            if (remote != null) {
                return Lang.getFor(viewer, "user.hub_status_remote", "server", remote.getServer().toUpperCase());
            }
        }
        return Lang.getFor(viewer, "user.hub_status_offline");
    }

    /**
     * Formats a mute end date stored as yymmddhhmm.
     */
    private static String formatMuteEndDate(String muteEndDate) {
        if (muteEndDate.length() != 10) {
            return muteEndDate;
        }
        return "20" + muteEndDate.substring(0, 2) + "-" + muteEndDate.substring(2, 4) + "-" + muteEndDate.substring(4, 6)
                + " " + muteEndDate.substring(6, 8) + ":" + muteEndDate.substring(8, 10);
    }

    private static String formatDate(Date date) {
        LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return time.getYear() + "-" + pad(time.getMonthValue()) + "-" + pad(time.getDayOfMonth())
                + " " + pad(time.getHour()) + ":" + pad(time.getMinute());
    }

    private static String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor(name));
        meta.setLore(lore.stream().map(ChatUtils::translateToColor).toList());
        item.setItemMeta(meta);
        return item;
    }
}
