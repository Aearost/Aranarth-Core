package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.player.HornSeekExtraDamage;
import com.aearost.aranarthcore.event.player.WeaponsExtraDamage;
import com.aearost.aranarthcore.event.mob.HappyGhastPreventDamage;
import com.aearost.aranarthcore.event.mob.PetHurtPrevent;
import com.aearost.aranarthcore.event.player.SpecialArrowDamageEffects;
import com.aearost.aranarthcore.event.player.TippedArrowDamagePrevent;
import com.aearost.aranarthcore.event.world.FireDamageIncrease;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityDamageEventListener implements Listener {

    public EntityDamageEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity being damaged
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getDamageSource().getDirectEntity() instanceof Arrow arrow) {
            new TippedArrowDamagePrevent().execute(e);
            new SpecialArrowDamageEffects().execute(e);
        }

        if (e.getEntity() instanceof Tameable tameable && tameable.isTamed()) {
            new PetHurtPrevent().execute(e);
        }
        // Do not affect tamed mobs
        else {
            new WeaponsExtraDamage().execute(e);
            new HornSeekExtraDamage().execute(e);
        }

        if (e.getEntity().getType() == EntityType.HAPPY_GHAST) {
            new HappyGhastPreventDamage().execute(e);
        }

        if (AranarthUtils.getMonth() == Month.ARDORVOR) {
            new FireDamageIncrease().execute(e);
        }
    }
}
