package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDefenderManage;
import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles click events for the per-defender management GUI.
 */
public class GuiDefenderManageClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();

        UUID defenderUUID = GuiDefenderManage.getDefenderForPlayer(player.getUniqueId());
        if (defenderUUID == null) {
            return;
        }

        UUID dominionId = DefenderUtils.getDefenderDominionId(defenderUUID);
        if (dominionId == null) {
            return;
        }

        Dominion dominion = DominionUtils.getDominionById(dominionId);
        if (dominion == null) {
            return;
        }

        // Permission check
        boolean canManage = dominion.getLeader().equals(player.getUniqueId())
                || (dominion.getMemberRank(player.getUniqueId()) != null
                && dominion.getDominionPermissions().hasPermission(
                dominion.getMemberRank(player.getUniqueId()), DominionPermission.MANAGE_DEFENDERS));
        if (!canManage) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to manage defenders"));
            player.closeInventory();
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = e.getSlot();

        if (slot == GuiDefenderManage.SLOT_MODE) {
            if (DefenderUtils.isFollowLockedFor(defenderUUID, player.getUniqueId(), dominion.getLeader())) {
                UUID followId = DefenderUtils.getFollowPlayerId(defenderUUID);
                String followerName = followId != null
                        ? (Bukkit.getOfflinePlayer(followId).getName() != null
                        ? Bukkit.getOfflinePlayer(followId).getName() : "another player")
                        : "another player";
                player.sendMessage(ChatUtils.chatMessage(
                        "&cThis defender is currently following &e" + followerName));
                return;
            }

            DefenderMode currentMode = DefenderUtils.getDefenderMode(defenderUUID);
            DefenderMode nextMode = currentMode.next();

            // Resolve follow player and guard position for the incoming mode
            UUID followPlayerId = null;
            Location guardPosition = null;

            if (nextMode == DefenderMode.FOLLOW) {
                followPlayerId = player.getUniqueId();
            } else if (nextMode == DefenderMode.GUARD) {
                Entity entity = Bukkit.getEntity(defenderUUID);
                if (entity != null) {
                    guardPosition = entity.getLocation();
                } else {
                    // Entity is not loaded, fall back to dominion home
                    guardPosition = dominion.getDominionHome();
                }
            }

            DefenderUtils.setDefenderMode(defenderUUID, nextMode, followPlayerId, guardPosition);

            String modeMsg = switch (nextMode) {
                case PATROL -> "&7This defender will now &epatrol &7the dominion territory";
                case FOLLOW -> "&7This defender will now &efollow &7you";
                case IDLE -> "&7This defender is now &eidle &7and completely still";
                case GUARD -> "&7This defender will now &eguard &7its current position";
            };
            player.sendMessage(ChatUtils.chatMessage(modeMsg));
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            GuiDefenderManage.open(player, defenderUUID);

        } else if (slot == GuiDefenderManage.SLOT_TELEPORT_HOME) {
            Entity defender = Bukkit.getEntity(defenderUUID);
            if (defender == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThis defender is no longer available"));
                GuiDefenderManage.clearSession(player.getUniqueId());
                player.closeInventory();
                return;
            }
            Location homeLocation = DefenderUtils.getDefenderHomeLocation(defenderUUID);
            if (homeLocation == null) {
                player.sendMessage(ChatUtils.chatMessage("&cThis territory does not have a home set"));
                return;
            }
            UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(defenderUUID);
            String homeName;
            if (assignedOutpostId != null) {
                Outpost outpost = OutpostUtils.getOutpostById(assignedOutpostId);
                homeName = outpost != null ? outpost.getName() : dominion.getName();
            } else {
                homeName = dominion.getName();
            }
            defender.teleport(homeLocation);
            DefenderUtils.setDefenderMode(defenderUUID, DefenderMode.PATROL, null, null);
            player.sendMessage(ChatUtils.chatMessage(
                    "&7The defender has been teleported to &e" + homeName + "&7's home"));
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1F);
            GuiDefenderManage.clearSession(player.getUniqueId());
            player.closeInventory();

        } else if (slot == GuiDefenderManage.SLOT_LOCATION) {
            List<Outpost> outposts = OutpostUtils.getDominionOutposts(dominionId);
            if (outposts.isEmpty()) {
                player.sendMessage(ChatUtils.chatMessage("&cThis dominion has no outposts to assign this defender to"));
                return;
            }
            UUID currentOutpostId = DefenderUtils.getAssignedOutpostId(defenderUUID);
            UUID nextOutpostId = null;
            if (currentOutpostId == null) {
                nextOutpostId = outposts.get(0).getId();
            } else {
                int currentIndex = -1;
                for (int i = 0; i < outposts.size(); i++) {
                    if (outposts.get(i).getId().equals(currentOutpostId)) {
                        currentIndex = i;
                        break;
                    }
                }
                if (currentIndex == -1 || currentIndex == outposts.size() - 1) {
                    nextOutpostId = null; // cycle back to dominion
                } else {
                    nextOutpostId = outposts.get(currentIndex + 1).getId();
                }
            }
            DefenderUtils.setAssignedOutpost(defenderUUID, nextOutpostId);
            String assignMsg = nextOutpostId == null
                    ? "&7This defender is now assigned to &e" + dominion.getName() + "&7's main territory"
                    : "&7This defender is now assigned to outpost &e"
                      + OutpostUtils.getOutpostById(nextOutpostId).getName();
            player.sendMessage(ChatUtils.chatMessage(assignMsg));
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            GuiDefenderManage.open(player, defenderUUID);

        } else if (slot == GuiDefenderManage.SLOT_SELL) {
            DefenderType type = DefenderUtils.getDefenderType(defenderUUID);
            String result = DefenderUtils.sellDefender(dominion, type, defenderUUID);
            player.sendMessage(ChatUtils.chatMessage(result));
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1F);
            GuiDefenderManage.clearSession(player.getUniqueId());
            player.closeInventory();
        }
    }
}
