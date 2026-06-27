package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiDefenderManage;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

/**
 * Opens the defender management GUI when a player right-clicks one of their dominion's defenders.
 */
public class DefenderInteract {

    public void execute(PlayerInteractEntityEvent e) {
        UUID entityUUID = e.getRightClicked().getUniqueId();
        if (!DefenderUtils.isDefender(entityUUID)) {
            return;
        }

        e.setCancelled(true);

        Player player = e.getPlayer();
        UUID dominionId = DefenderUtils.getDefenderDominionId(entityUUID);
        Dominion dominion = DominionUtils.getDominionById(dominionId);
        if (dominion == null) return;

        boolean canManage = dominion.getLeader().equals(player.getUniqueId())
                || (dominion.getMemberRank(player.getUniqueId()) != null
                    && dominion.getDominionPermissions().hasPermission(
                            dominion.getMemberRank(player.getUniqueId()), DominionPermission.MANAGE_DEFENDERS));
        if (!canManage) {
            player.sendMessage(ChatUtils.chatMessage("&cYou do not have permission to manage defenders"));
            return;
        }

        GuiDefenderManage.open(player, entityUUID);
    }
}
