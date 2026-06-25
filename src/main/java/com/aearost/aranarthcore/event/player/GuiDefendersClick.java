package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDefenders;
import com.aearost.aranarthcore.gui.GuiDominionPermissions;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.DominionRank;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

/**
 * Handles click events for the Defenders GUI.
 */
public class GuiDefendersClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (dominion == null) return;

        DominionRank memberRank = dominion.getMemberRank(player.getUniqueId());
        boolean canManage = dominion.getLeader().equals(player.getUniqueId())
                || (memberRank != null && dominion.getDominionPermissions().hasPermission(memberRank, DominionPermission.MANAGE_DEFENDERS));
        if (!canManage) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to manage defenders!"));
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        // Back button
        if (clicked.getType() == Material.BARRIER) {
            new GuiDominionPermissions(player).openGui();
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.5F, 1F);
            return;
        }

        // Match clicked material to a DefenderType spawn egg
        DefenderType clickedType = null;
        for (DefenderType type : DefenderType.values()) {
            if (clicked.getType() == type.getSpawnEgg()) {
                clickedType = type;
                break;
            }
        }
        if (clickedType == null) return;

        String result;
        if (e.getClick() == ClickType.RIGHT) {
            result = DefenderUtils.purchaseDefender(dominion, clickedType);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1F);
        } else if (e.getClick() == ClickType.LEFT) {
            result = DefenderUtils.sellDefender(dominion, clickedType);
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 0.5F, 1F);
        } else {
            return;
        }

        player.sendMessage(ChatUtils.chatMessage(result));
        GuiDefenders.open(player);
    }
}
