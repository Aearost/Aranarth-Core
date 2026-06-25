package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Immediately removes any entity tagged with DEFENDER_DOMINION_ID that is not already registered in DefenderUtils.
 */
public class DefenderChunkLoad {

    public void execute(ChunkLoadEvent e) {
        for (Entity entity : e.getChunk().getEntities()) {
            String tag = entity.getPersistentDataContainer()
                    .get(CustomKeys.DEFENDER_DOMINION_ID, PersistentDataType.STRING);
            if (tag == null) {
                continue;
            }

            if (!DefenderUtils.isDefender(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }
}
