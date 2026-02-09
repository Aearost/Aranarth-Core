package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.MusicInstrument;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles logic when a player blows a Goat Horn.
 */
public class GoatHornUse {

//    Ponder --> Hunger
//    Sing --> Song
//    Seek --> Attacking
//    Feel --> Defensive
//
//    Admire --> Golems
//    Call --> Wolves
//    Yearn --> Horse
//    Dream --> Cleanse

    // For future: the basis will be to take the current time in epoch milliseconds when the horn is used successfully
    // If the horn is attempted to be used again before the specified elapsed difference is passed, then display a message

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
            MusicInstrumentMeta meta = (MusicInstrumentMeta) e.getItem().getItemMeta();
            if (meta.getInstrument() == MusicInstrument.SEEK_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.SEEK_GOAT_HORN)) {
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.STRENGTH, 600, 1));
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.SPEED, 600, 1));
                    player.setCooldown(e.getItem(), 1200); // 60 second cooldown on the horn
                }
            }
        }
    }

    /**
     * Temporarily toggles the goat horn attribute and applies the potion effect.
     * @param player The player.
     * @param effect The potion effect.
     */
    private void applyHornPotionEffect(Player player, PotionEffect effect) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        aranarthPlayer.setUsingGoatHorn(true);
        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
        player.addPotionEffect(effect);
    }
}
