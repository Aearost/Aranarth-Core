package com.aearost.aranarthcore.gui;

import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.*;
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
import java.util.*;

/**
 * GUI for managing a specific defender entity.
 */
public class GuiDefenderManage {

    public static final String TITLE_KEY = "gui.defendermanage.title";
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

        Inventory gui = Bukkit.createInventory(player, 27, Lang.getFor(player, TITLE_KEY));

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

    public static ItemStack buildModeButton(DefenderMode mode, UUID defenderEntityUUID) {
        ItemStack item = new ItemStack(modeToMaterial(mode));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defendermanage.mode", "mode", mode.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defendermanage.mode_desc", "desc", mode.getDescription()));
        lore.add("");

        // Show follow player name if relevant
        if (mode == DefenderMode.FOLLOW) {
            UUID followId = DefenderUtils.getFollowPlayerId(defenderEntityUUID);
            String name = followId != null
                    ? (Bukkit.getOfflinePlayer(followId).getName() != null
                       ? Bukkit.getOfflinePlayer(followId).getName() : followId.toString())
                    : "none";
            lore.add(Lang.get("gui.defendermanage.following", "name", name));
            lore.add("");
        }

        // Show guard position if relevant
        if (mode == DefenderMode.GUARD) {
            Location guardPos = DefenderUtils.getGuardPosition(defenderEntityUUID);
            if (guardPos != null) {
                lore.add(Lang.get("gui.defendermanage.guard_pos", "x", String.valueOf((int) guardPos.getX()), "y", String.valueOf((int) guardPos.getY()), "z", String.valueOf((int) guardPos.getZ())));
                lore.add("");
            }
        }

        lore.add(Lang.get("gui.defendermanage.cycle_mode"));
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

    public static ItemStack buildLockedModeButton(UUID defenderEntityUUID) {
        UUID followId = DefenderUtils.getFollowPlayerId(defenderEntityUUID);
        String name = followId != null
                ? (Bukkit.getOfflinePlayer(followId).getName() != null
                   ? Bukkit.getOfflinePlayer(followId).getName() : followId.toString())
                : "someone";
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defendermanage.mode_locked"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defendermanage.mode_locked_lore", "name", name));
        lore.add(Lang.get("gui.defendermanage.mode_locked_lore2"));
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
        meta.setDisplayName(Lang.get("gui.defendermanage.teleport_home"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defendermanage.teleport_home_lore", "name", homeName));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildLocationButton(UUID defenderEntityUUID, Dominion dominion) {
        UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(defenderEntityUUID);
        String locationName;
        String description;
        if (assignedOutpostId == null) {
            locationName = dominion.getName() + " &7(Dominion)";
            description = Lang.get("gui.defendermanage.patrols_main");
        } else {
            Outpost outpost = OutpostUtils.getOutpostById(assignedOutpostId);
            if (outpost != null) {
                locationName = outpost.getName() + " &7(Outpost " + outpost.getOutpostIndex() + ")";
                description = Lang.get("gui.defendermanage.patrols_outpost", "name", outpost.getName());
            } else {
                locationName = dominion.getName() + " &7(Dominion)";
                description = Lang.get("gui.defendermanage.patrols_main");
            }
        }
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defendermanage.territory", "name", ChatUtils.translateToColor(locationName)));
        List<String> lore = new ArrayList<>();
        lore.add(description);
        lore.add("");
        lore.add(Lang.get("gui.defendermanage.cycle_territory"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildInfoItem(DefenderType type, UUID entityUUID, Dominion dominion, DefenderMode mode) {
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
        meta.setDisplayName(Lang.get("gui.defendermanage.info_name", "type", type.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defendermanage.info_dominion", "name", dominion.getName()));
        lore.add(Lang.get("gui.defenders.role", "role", type.getRole()));
        lore.add(Lang.get("gui.defendermanage.info_hp", "hp", String.valueOf((int) (currentHp / 2)), "max", String.valueOf((int) (maxHp / 2))));
        lore.add(Lang.get("gui.defendermanage.info_mode", "mode", mode.getDisplayName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildSellButton(DefenderType type) {
        NumberFormat fmt = NumberFormat.getInstance();
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.get("gui.defendermanage.sell"));
        List<String> lore = new ArrayList<>();
        lore.add(Lang.get("gui.defendermanage.sell_lore", "type", type.getDisplayName()));
        lore.add(Lang.get("gui.defendermanage.sell_refund", "amount", fmt.format((long) type.getSellPrice())));
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
