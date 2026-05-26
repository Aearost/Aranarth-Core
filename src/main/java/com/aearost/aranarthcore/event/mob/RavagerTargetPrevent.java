package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Prevents owned Ravager mounts from targeting any entity.
 */
public class RavagerTargetPrevent {

    public void execute(EntityTargetEvent e) {
        if (!(e.getEntity() instanceof Ravager ravager)) {
            return;
        }
        if (ravager.getPersistentDataContainer().has(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING)) {
            e.setCancelled(true);
        }
    }
}
