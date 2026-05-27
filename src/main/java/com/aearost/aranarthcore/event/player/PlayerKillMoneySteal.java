package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

/**
 * Handles logic to steal a portion of the victim's money when killed by another player.
 */
public class PlayerKillMoneySteal {

    public void execute(EntityDeathEvent e) {
        Player victim = (Player) e.getEntity();
        if (!victim.getWorld().getName().startsWith("world")) {
            return;
        }
        if (!(e.getDamageSource().getCausingEntity() instanceof Player killer)) {
            return;
        }

        AranarthPlayer victimAranarthPlayer = AranarthUtils.getPlayer(victim.getUniqueId());
        AranarthPlayer killerAranarthPlayer = AranarthUtils.getPlayer(killer.getUniqueId());

        double victimBalance = victimAranarthPlayer.getBalance();
        if (victimBalance <= 0) {
            return;
        }

        double percentage = 2.5 + new Random().nextDouble() * 2.5;
        DecimalFormat df = new DecimalFormat("0.00");
        int warMultiplier = DominionUtils.getDeathPenaltyMultiplier(victim.getUniqueId(), killer.getUniqueId());
        double stolenAmount = Double.parseDouble(df.format(victimBalance * percentage / 100.0 * warMultiplier));

        victimAranarthPlayer.setBalance(victimBalance - stolenAmount);
        killerAranarthPlayer.setBalance(killerAranarthPlayer.getBalance() + stolenAmount);

        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        String formattedAmount = formatter.format(stolenAmount);
        String warSuffix = warMultiplier > 1 ? " &4(" + warMultiplier + "x war penalty)" : "";

        killer.sendMessage(ChatUtils.chatMessage("&7You have stolen &6" + formattedAmount + " &7from &e" + victimAranarthPlayer.getNickname() + warSuffix));
        killer.playSound(killer, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
        victim.sendMessage(ChatUtils.chatMessage("&e" + killerAranarthPlayer.getNickname() + " &chas stolen &6" + formattedAmount + " &cfrom you!" + warSuffix));
    }
}
