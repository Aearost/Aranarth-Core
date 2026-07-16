package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

public class PhantomTargetPrevent {
    public void execute(EntityTargetEvent e) {
        if (e.getEntity() instanceof Phantom) {
            if (e.getTarget() instanceof Player player) {
                if (AranarthUtils.isWearingArmorType(player, "fae")) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
