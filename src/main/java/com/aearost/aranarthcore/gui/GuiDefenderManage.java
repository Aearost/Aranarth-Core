package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI for managing a specific defender entity.
 */
public class GuiDefenderManage {

    public static final String TITLE = "Manage Defender";
    public static final int SLOT_MODE = 10;
    public static final int SLOT_TELEPORT_HOME = 11;
    public static final int SLOT_LOCATION = 12;
    public static final int SLOT_SELL = 15;
    private static final Map<UUID, UUID> playerToDefender = new HashMap<>();

    public static void open(Player player, UUID defenderEntityUUID) {
        DefenderType type = DefenderUtils.getDefenderType(defenderEntityUUID);
        UUID dominionId = DefenderUtils.getDefenderDominionId(defenderEntityUUID);
        if (type == null || dominionId == null) return;

        Dominion dominion = DominionUtils.getDominionById(dominionId);
        if (dominion == null) return;

        playerToDefender.put(player.getUniqueId(), defenderEntityUUID);

        Inventory gui = Bukkit.createInventory(player, 27, ChatUtils.translateToColor(TITLE));

        ItemStack border = buildBorder();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(18 + i, border);
        }

        ItemStack filler = buildFiller();
        for (int slot = 9; slot < 18; slot++) {
            gui.setItem(slot, filler);
        }

        DefenderMode mode = DefenderUtils.getDefenderMode(defenderEntityUUID);
        boolean followLocked = DefenderUtils.isFollowLockedFor(
                defenderEntityUUID, player.getUniqueId(), dominion.getLeader());
        UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(defenderEntityUUID);

        gui.setItem(SLOT_MODE, followLocked
                ? buildLockedModeButton(defenderEntityUUID)
                : buildModeButton(mode, defenderEntityUUID));
        gui.setItem(SLOT_TELEPORT_HOME, buildTeleportHomeButton(dominion, assignedOutpostId));
        gui.setItem(SLOT_LOCATION, buildLocationButton(defenderEntityUUID, dominion));
        gui.setItem(13, buildInfoItem(type, defenderEntityUUID, dominion, mode));
        gui.setItem(SLOT_SELL, buildSellButton(type));

        player.closeInventory();
        player.openInventory(gui);
    }

    public static UUID getDefenderForPlayer(UUID playerUUID) {
        return playerToDefender.get(playerUUID);
    }

    public static void clearSession(UUID playerUUID) {
        playerToDefender.remove(playerUUID);
    }

    private static ItemStack buildModeButton(DefenderMode mode, UUID defenderEntityUUID) {
        ItemStack item = new ItemStack(modeToMaterial(mode));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&b&lMode: &f" + mode.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7" + mode.getDescription()));
        lore.add("");

        // Show follow player name if relevant
        if (mode == DefenderMode.FOLLOW) {
            UUID followId = DefenderUtils.getFollowPlayerId(defenderEntityUUID);
            String name = followId != null
                    ? (Bukkit.getOfflinePlayer(followId).getName() != null
                       ? Bukkit.getOfflinePlayer(followId).getName() : followId.toString())
                    : "none";
            lore.add(ChatUtils.translateToColor("&7Following: &e" + name));
            lore.add("");
        }

        // Show guard position if relevant
        if (mode == DefenderMode.GUARD) {
            Location guardPos = DefenderUtils.getGuardPosition(defenderEntityUUID);
            if (guardPos != null) {
                lore.add(ChatUtils.translateToColor(
                        "&7Guard pos: &e" + (int) guardPos.getX() + ", "
                        + (int) guardPos.getY() + ", " + (int) guardPos.getZ()));
                lore.add("");
            }
        }

        lore.add(ChatUtils.translateToColor("&eClick &7to cycle to next mode"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Material modeToMaterial(DefenderMode mode) {
        return switch (mode) {
            case PATROL -> Material.COMPASS;
            case FOLLOW -> Material.LEAD;
            case IDLE   -> Material.GRAY_DYE;
            case GUARD  -> Material.IRON_SWORD;
        };
    }

    private static ItemStack buildLockedModeButton(UUID defenderEntityUUID) {
        UUID followId = DefenderUtils.getFollowPlayerId(defenderEntityUUID);
        String name = followId != null
                ? (Bukkit.getOfflinePlayer(followId).getName() != null
                   ? Bukkit.getOfflinePlayer(followId).getName() : followId.toString())
                : "someone";
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&c&lMode Locked"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7This defender is currently following &e" + name));
        lore.add(ChatUtils.translateToColor("&7This currently cannot be done"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildTeleportHomeButton(Dominion dominion, UUID assignedOutpostId) {
        String homeName;
        if (assignedOutpostId != null) {
            Outpost outpost = OutpostUtils.getOutpostById(assignedOutpostId);
            homeName = outpost != null ? outpost.getName() : dominion.getName();
        } else {
            homeName = dominion.getName();
        }
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&a&lTeleport to Home"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Teleports this defender to &e" + homeName + "&7's home"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildLocationButton(UUID defenderEntityUUID, Dominion dominion) {
        UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(defenderEntityUUID);
        String locationName;
        String description;
        if (assignedOutpostId == null) {
            locationName = dominion.getName() + " &7(Dominion)";
            description = "&7Patrols the main dominion territory";
        } else {
            Outpost outpost = OutpostUtils.getOutpostById(assignedOutpostId);
            if (outpost != null) {
                locationName = outpost.getName() + " &7(Outpost " + outpost.getOutpostIndex() + ")";
                description = "&7Patrols &e" + outpost.getName() + "&7's territory";
            } else {
                locationName = dominion.getName() + " &7(Dominion)";
                description = "&7Patrols the main dominion territory";
            }
        }
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&b&lTerritory: &e" + locationName));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor(description));
        lore.add("");
        lore.add(ChatUtils.translateToColor("&eClick &7to cycle territory"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoItem(DefenderType type, UUID entityUUID, Dominion dominion, DefenderMode mode) {
        NumberFormat fmt = NumberFormat.getInstance();

        double currentHp = 0;
        double maxHp = type.getMaxHealth();
        Entity entity = Bukkit.getEntity(entityUUID);
        if (entity instanceof LivingEntity living) {
            currentHp = living.getHealth();
            if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHp = living.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
        }

        ItemStack item = new ItemStack(type.getSpawnEgg());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&c&l" + type.getDisplayName() + " Defender"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Dominion: &e" + dominion.getName()));
        lore.add(ChatUtils.translateToColor("&7Role: &e" + type.getRole()));
        lore.add(ChatUtils.translateToColor("&7Health: &e" + (int) (currentHp / 2) + "/" + (int) (maxHp / 2) + " hearts"));
        lore.add(ChatUtils.translateToColor("&7Mode: &b" + mode.getDisplayName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSellButton(DefenderType type) {
        NumberFormat fmt = NumberFormat.getInstance();
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.translateToColor("&6&lSell This Defender"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatUtils.translateToColor("&7Sells this specific &e" + type.getDisplayName() + " defender."));
        lore.add(ChatUtils.translateToColor("&7Refund: &6$" + fmt.format((long) type.getSellPrice())));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
