package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.MountUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RavagerRam extends BukkitRunnable {

    private static final double MIN_DAMAGE = 2.0; // 1 heart (fixed floor)
    private static final long RAM_COOLDOWN_TICKS = 20L;
    private static final double CONTACT_RANGE = 1.5;
    private static final double KNOCKBACK_HORIZONTAL = 0.75;
    private static final double KNOCKBACK_VERTICAL = 0.5;

    private final UUID ravagerUUID;
    private final UUID riderUUID;
    private final Map<UUID, RavagerRam> activeRams;
    private final double maxDamage;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    private long tick = 0;

    public RavagerRam(Ravager ravager, Player rider, Map<UUID, RavagerRam> activeRams, double maxDamage) {
        this.ravagerUUID = ravager.getUniqueId();
        this.riderUUID = rider.getUniqueId();
        this.activeRams = activeRams;
        this.maxDamage = maxDamage;
    }

    @Override
    public void run() {
        tick++;
        Entity entity = Bukkit.getEntity(ravagerUUID);
        if (!(entity instanceof Ravager ravager) || ravager.isDead()) {
            finish();
            return;
        }
        if (ravager.getPassengers().isEmpty()) {
            finish();
            return;
        }

        cooldowns.entrySet().removeIf(e -> tick >= e.getValue());

        for (Entity nearby : ravager.getNearbyEntities(CONTACT_RANGE, CONTACT_RANGE, CONTACT_RANGE)) {
            if (!(nearby instanceof LivingEntity target)) {
                continue;
            }
            if (target.getUniqueId().equals(ravagerUUID)) {
                continue;
            }
            if (target.getUniqueId().equals(riderUUID)) {
                continue;
            }
            if (cooldowns.containsKey(target.getUniqueId())) {
                continue;
            }

            ram(ravager, target);
            cooldowns.put(target.getUniqueId(), tick + RAM_COOLDOWN_TICKS);
        }
    }

    private void ram(Ravager ravager, LivingEntity target) {
        double damage = MIN_DAMAGE + random.nextDouble() * (maxDamage - MIN_DAMAGE);
        target.damage(damage, ravager);

        // Knock target away from the ravager and upward
        Vector knockback = target.getLocation().toVector()
                .subtract(ravager.getLocation().toVector())
                .setY(0);
        if (knockback.lengthSquared() < 0.001) {
            knockback = new Vector(0, 0, 1);
        } else {
            knockback.normalize();
        }
        knockback.multiply(KNOCKBACK_HORIZONTAL).setY(KNOCKBACK_VERTICAL);
        target.setVelocity(knockback);

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 1.5f, 0.8f);

        // Award ram-strength XP if this Ravager is a mount
        MountUtils.addRamXp(ravagerUUID);
    }

    private void finish() {
        activeRams.remove(ravagerUUID);
        this.cancel();
    }
}
