package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.PetInventoryUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Auto-consumes food from a pet's inventory to heal the pet when it is in danger.
 */
public class PetAutoEat {

    public void execute(EntityDamageEvent e) {
        if (!PetInventoryUtils.isPetType(e.getEntity())) {
            return;
        }
        if (!(e.getEntity() instanceof Tameable tameable)) {
            return;
        }
        if (!tameable.isTamed() || tameable.getOwnerUniqueId() == null) {
            return;
        }

        double currentHealth = tameable.getHealth();
        double finalDamage = e.getFinalDamage();
        double remainingHealth = currentHealth - finalDamage;

        // Pet must survive this hit, and be vulnerable enough that another equal hit would kill it
        if (remainingHealth <= 0 || remainingHealth > finalDamage) {
            return;
        }

        double maxHealth = tameable.getAttribute(Attribute.MAX_HEALTH).getValue();
        double missingHealthAfterHit = maxHealth - remainingHealth;

        PetInventoryUtils.tryAutoEat(tameable, missingHealthAfterHit);
    }
}
