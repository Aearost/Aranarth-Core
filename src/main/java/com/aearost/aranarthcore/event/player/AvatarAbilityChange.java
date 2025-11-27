package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.PersistenceUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles saving the Avatar's binds as they make changes to their abilities
 */
public class AvatarAbilityChange {

    public void execute(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);

        String[] parts = e.getMessage().split(" ");
        if (parts.length > 1) {
            if (parts[0].startsWith("/b")) {
                if (parts[1].equals("b") || parts[1].equals("bind") || parts[1].equals("c") || parts[1].equals("clear")
                        || parts[1].equals("p") || parts[1].equals("preset") || parts[1].equals("copy")) {

                    for (String name : bendingPlayer.getAbilities().values()) {
                        if (isSubAbility(ChatUtils.stripColorFormatting(name))) {
                            return;
                        }
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            PersistenceUtils.saveAvatarBinds();
                        }
                    }.runTaskLater(AranarthCore.getInstance(), 1);
                }
            }
        }
    }

    /**
     * Confirms if the player is using a multi-mode ability such as ElementSphere or WaterArms.
     * @param name The ability name.
     * @return Confirmation if the player is using a multi-mode ability.
     */
    private boolean isSubAbility(String name) {
        return name.equals("Air") || name.equals("Earth") || name.equals("Fire") || name.equals("Water") || name.equals("Stream")
                || name.equals("Pull") || name.equals("Punch") || name.equals("Grapple") || name.equals("Grab") || name.equals("Freeze")
                || name.equals("Spear");
    }

}
