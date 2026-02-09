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

//    0 Ponder --> Hunger
//    1 Sing --> Song
//    2 Seek --> Attacking
//    3 Feel --> Defensive
//
//    4 Admire --> Golems
//    5 Call --> Wolves
//    6 Yearn --> Horse
//    7 Dream --> Cleanse

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
            MusicInstrumentMeta meta = (MusicInstrumentMeta) e.getItem().getItemMeta();
            player.setCooldown(e.getItem(), 1); // Mimics no cooldown
            if (meta.getInstrument() == MusicInstrument.PONDER_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.PONDER_GOAT_HORN)) {

                }
            } else if (meta.getInstrument() == MusicInstrument.SING_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.SING_GOAT_HORN)) {

                }
            } else if (meta.getInstrument() == MusicInstrument.SEEK_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.SEEK_GOAT_HORN)) {
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.STRENGTH, 600, 1));
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.SPEED, 600, 1));
                }
            } else if (meta.getInstrument() == MusicInstrument.FEEL_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.FEEL_GOAT_HORN)) {
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                    applyHornPotionEffect(player, new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
                }
            } else if (meta.getInstrument() == MusicInstrument.ADMIRE_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.ADMIRE_GOAT_HORN)) {

                }
            } else if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.CALL_GOAT_HORN)) {

                }
            } else if (meta.getInstrument() == MusicInstrument.YEARN_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.YEARN_GOAT_HORN)) {

                }
            } else if (meta.getInstrument() == MusicInstrument.DREAM_GOAT_HORN) {
                if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.DREAM_GOAT_HORN)) {

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
