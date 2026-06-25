package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * Sets a custom death message when a player is killed by a Dominion Defender.
 */
public class DefenderKillDeathMessage {

    public void execute(PlayerDeathEvent e) {
        Entity damager = e.getDamageSource().getCausingEntity();
        if (damager == null) {
            return;
        }

        UUID damagerUUID = damager.getUniqueId();
        if (!DefenderUtils.isDefender(damagerUUID)) {
            return;
        }

        UUID dominionId = DefenderUtils.getDefenderDominionId(damagerUUID);
        DefenderType type = DefenderUtils.getDefenderType(damagerUUID);
        if (dominionId == null || type == null) {
            return;
        }

        Dominion dominion = DominionUtils.getDominionById(dominionId);
        if (dominion == null) {
            return;
        }

        e.setDeathMessage(ChatUtils.translateToColor(e.getEntity().getName() + " was slain by &e"
                + dominion.getName() + "&e's " + type.getDisplayName() + " defender"));
    }
}
