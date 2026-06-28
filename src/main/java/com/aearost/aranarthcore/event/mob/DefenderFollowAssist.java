package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.utils.DefenderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.UUID;

/**
 * When a player punches any mob, defenders following that player immediately target it.
 */
public class DefenderFollowAssist {

    public void execute(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) {
            return;
        }
        if (!(e.getEntity() instanceof LivingEntity target)) {
            return;
        }

        List<UUID> followers = DefenderUtils.getFollowDefenders(player.getUniqueId());
        if (followers.isEmpty()) {
            return;
        }

        UUID targetUUID = target.getUniqueId();

        for (UUID followerUUID : followers) {
            if (DefenderUtils.getDefenderMode(followerUUID) != DefenderMode.FOLLOW) {
                continue;
            }

            // Don't redirect onto a same-dominion defender
            UUID followerDominion = DefenderUtils.getDefenderDominionId(followerUUID);
            if (DefenderUtils.isDefender(targetUUID)
                    && followerDominion != null
                    && followerDominion.equals(DefenderUtils.getDefenderDominionId(targetUUID))) {
                continue;
            }

            Entity followerEntity = Bukkit.getEntity(followerUUID);
            if (followerEntity instanceof Mob followerMob && !followerMob.isDead()) {
                followerMob.setTarget(target);
            }
        }
    }
}
