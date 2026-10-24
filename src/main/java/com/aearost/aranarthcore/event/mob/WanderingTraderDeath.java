package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.enums.WanderingTraderType;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Deals with displaying a chat message when a wandering trader is killed.
 */
public class WanderingTraderDeath {
    public void execute(final EntityDeathEvent e) {
        WanderingTrader wt = (WanderingTrader) e.getEntity();
        String typeName = resolveTypeName(wt);

        if (e.getDamageSource().getCausingEntity() instanceof Player player) {
            Lang.broadcast("trader.slain_by_player", "type", typeName, "player", AranarthUtils.getNickname(player));
        } else if (e.getDamageSource().getCausingEntity() != null) {
            Lang.broadcast("trader.slain_by", "type", typeName, "killer", e.getDamageSource().getCausingEntity().getName());
        } else {
            Lang.broadcast("trader.died", "type", typeName);
        }
    }

    private String resolveTypeName(WanderingTrader wt) {
        PersistentDataContainer pdc = wt.getPersistentDataContainer();
        if (!pdc.has(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)) {
            return "Wandering Trader";
        }
        try {
            WanderingTraderType type = WanderingTraderType.valueOf(
                pdc.get(CustomKeys.WANDERING_TRADER_TYPE, PersistentDataType.STRING)
            );
            return type.getPlainName();
        } catch (IllegalArgumentException ignored) {
            return "Wandering Trader";
        }
    }
}
