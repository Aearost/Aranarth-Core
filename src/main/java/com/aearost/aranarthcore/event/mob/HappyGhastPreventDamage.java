package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.Objects;

/**
 * Prevents players from hurting Happy Ghasts.
 */
public class HappyGhastPreventDamage {
	public void execute(EntityDamageEvent e) {
		if (isPlayerCausedDamage(e.getCause())) {
			if (Objects.nonNull(e.getDamageSource().getCausingEntity())
					&& e.getDamageSource().getCausingEntity() instanceof Player attacker) {
				attacker.sendMessage(ChatUtils.chatMessage("&7You cannot harm a Happy Ghast!"));
				e.setCancelled(true);
			}
		}
	}
	
	private boolean isPlayerCausedDamage(DamageCause cause) {
		return (cause == DamageCause.ENTITY_ATTACK) || (cause == DamageCause.ENTITY_SWEEP_ATTACK)
				|| (cause == DamageCause.PROJECTILE);
	}
}
