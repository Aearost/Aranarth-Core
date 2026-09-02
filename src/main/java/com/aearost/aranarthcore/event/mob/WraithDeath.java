package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.event.block.StructureWraith.WraithType;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.objects.CustomKeys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class WraithDeath {

    public void execute(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        String typeRaw = pdc.get(CustomKeys.WRAITH_TYPE, PersistentDataType.STRING);
        if (typeRaw == null) {
            return;
        }

        WraithType wraithType;
        try {
            wraithType = WraithType.valueOf(typeRaw);
        } catch (IllegalArgumentException ex) {
            return;
        }

        e.getDrops().add(switch (wraithType) {
            case ANCIENT_CITY   -> new DiamondCluster().getItem();
            case MINESHAFT      -> new GoldCluster().getItem();
            case STRONGHOLD     -> new IronCluster().getItem();
            case DESERT_PYRAMID -> new RedstoneCluster().getItem();
            case FORTRESS       -> new QuartzCluster().getItem();
            case JUNGLE_PYRAMID -> new EmeraldCluster().getItem();
            case OCEAN_RUINS    -> new LapisCluster().getItem();
            case SHIPWRECK      -> new CopperCluster().getItem();
        });
    }
}
