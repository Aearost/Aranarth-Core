package com.aearost.aranarthcore.event.listener;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.event.mob.PetHurtPrevent;
import com.aearost.aranarthcore.event.player.*;
import com.aearost.aranarthcore.event.world.FireDamageIncrease;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class EntityDamageEventListener implements Listener {

    public EntityDamageEventListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Centralizes all logic to be called by an entity being damaged.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        new IncantationResilienceProtect().executeFireLava(e);
        if (e.isCancelled()) {
            return;
        }

        // Cancel suffocation for players riding Fang (Roku PastLives form)
        if (e.getEntity() instanceof Player player
                && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION
                && player.getVehicle() instanceof EnderDragon dragon
                && dragon.getPersistentDataContainer().has(CustomKeys.FANG_OWNER, PersistentDataType.STRING)) {
            e.setCancelled(true);
            return;
        }

        if (DefenderUtils.isDefender(e.getEntity().getUniqueId())
                && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            return;
        }

        if (e.getDamageSource().getDirectEntity() instanceof Firework firework
                && firework.hasMetadata("newYearFirework")) {
            e.setCancelled(true);
            return;
        }

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
        }

        new HornSeekExtraDamage().execute(e);
        new ResourceWorldDamagePrevent().execute(e);

        if (e.getEntity() instanceof Player player && AranarthUtils.isWearingArmorType(player, "ardent")) {
            int arVol = AranarthUtils.getPlayer(player.getUniqueId()).getAranarthiumSoundVolume();
            if (arVol > 0) {
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.4f * (arVol / 100f), 0.7f);
            }
        }

        if (AranarthUtils.getMonth() == Month.ARDORVOR) {
            new FireDamageIncrease().execute(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageMonitor(EntityDamageEvent e) {
        new PetAutoEat().execute(e);
    }
}
