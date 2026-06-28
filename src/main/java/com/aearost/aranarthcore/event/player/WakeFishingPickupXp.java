package com.aearost.aranarthcore.event.player;

import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.experience.XPGainSource;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.skills.fishing.FishingManager;
import com.gmail.nossr50.util.EventUtils;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * Awards mcMMO Fishing XP when a player picks up a fish spawned by WakeFishing.
 */
public class WakeFishingPickupXp {

    private static final int WAKE_FISHING_XP = 30;

    public void execute(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        Material type = e.getItem().getItemStack().getType();
        if (type != Material.COD && type != Material.SALMON
                && type != Material.PUFFERFISH && type != Material.TROPICAL_FISH) {
            return;
        }

        CoreAbility prototype = CoreAbility.getAbility("WakeFishing");
        if (prototype == null || !CoreAbility.hasAbility(player, prototype.getClass())) {
            return;
        }

        McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
        if (mcMMOPlayer == null) {
            return;
        }

        FishingManager fishingManager = new FishingManager(mcMMOPlayer);
        fishingManager.applyXpGain(WAKE_FISHING_XP, XPGainReason.PVE, XPGainSource.CUSTOM);
    }
}
