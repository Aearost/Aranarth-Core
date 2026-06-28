package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.DefenderMode;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Sentinel;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles logic when a player blows a Goat Horn.
 */
public class GoatHornUse {
    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player.getWorld().getName().startsWith("world") || player.getWorld().getName().startsWith("smp")) {
            if (e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) {
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                MusicInstrumentMeta meta = (MusicInstrumentMeta) e.getItem().getItemMeta();
                player.setCooldown(e.getItem(), 1); // Mimics no cooldown

                if (meta.getInstrument() == MusicInstrument.PONDER_GOAT_HORN) {
                    if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.PONDER_GOAT_HORN)) {
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.SPEED, 600, 4));
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.JUMP_BOOST, 600, 4));
                    }
                } else if (meta.getInstrument() == MusicInstrument.SING_GOAT_HORN) {
                    if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.SING_GOAT_HORN)) {
                        callNearbyDefenders(player);
                    }
                } else if (meta.getInstrument() == MusicInstrument.SEEK_GOAT_HORN) {
                    if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.SEEK_GOAT_HORN)) {
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.STRENGTH, 600, 2));
                        // Will additionally cause the player to take more damage as per HornSeekExtraDamage
                    }
                } else if (meta.getInstrument() == MusicInstrument.FEEL_GOAT_HORN) {
                    if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.FEEL_GOAT_HORN)) {
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                        applyHornPotionEffect(player, new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
                    }
                } else if (meta.getInstrument() == MusicInstrument.ADMIRE_GOAT_HORN) {
                    List<Sentinel> sentinels = aranarthPlayer.getSentinels().get(EntityType.IRON_GOLEM);
                    if (sentinels != null && !sentinels.isEmpty()) {
                        Entity target = e.getPlayer().getTargetEntity(5);
                        // Prevents the calling of the event when marking a sentinel
                        if (target != null && target.getType() == EntityType.IRON_GOLEM) {
                            // Goat horns are heard up to 256 blocks away, this is to prevent it from being heard again
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                if (player.getLocation().distance(onlinePlayer.getLocation()) <= 256) {
                                    onlinePlayer.stopSound(SoundCategory.RECORDS);
                                }
                            }
                            return;
                        }

                        if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.ADMIRE_GOAT_HORN)) {
                            // Summon up to 2 designated iron golems
                            summonSentinels(player, EntityType.IRON_GOLEM);
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&cYou have not yet designated any &eIron Golem Sentinels"));
                    }
                } else if (meta.getInstrument() == MusicInstrument.CALL_GOAT_HORN) {
                    List<Sentinel> sentinels = aranarthPlayer.getSentinels().get(EntityType.WOLF);
                    if (sentinels != null && !sentinels.isEmpty()) {
                        Entity target = e.getPlayer().getTargetEntity(5);
                        // Prevents the calling of the event when marking a sentinel
                        if (target != null && target.getType() == EntityType.WOLF) {
                            // Goat horns are heard up to 256 blocks away, this is to prevent it from being heard again
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                if (player.getLocation().distance(onlinePlayer.getLocation()) <= 256) {
                                    onlinePlayer.stopSound(SoundCategory.RECORDS);
                                }
                            }
                            return;
                        }

                        if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.CALL_GOAT_HORN)) {
                            // Summon up to 8 designated tamed wolves
                            summonSentinels(player, EntityType.WOLF);
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&cYou have not yet designated any &eWolf Sentinels"));
                    }
                } else if (meta.getInstrument() == MusicInstrument.YEARN_GOAT_HORN) {
                    List<Sentinel> sentinel = aranarthPlayer.getSentinels().get(EntityType.HORSE);
                    if (sentinel != null && !sentinel.isEmpty()) {
                        if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.YEARN_GOAT_HORN)) {
                            // Summon 1 designated horse
                            summonSentinels(player, EntityType.HORSE);
                        }
                    } else {
                        player.sendMessage(ChatUtils.chatMessage("&cYou have not yet designated a &eHorse Sentinel"));
                    }
                } else if (meta.getInstrument() == MusicInstrument.DREAM_GOAT_HORN) {
                    if (AranarthUtils.canUseHornSuccessfully(player, MusicInstrument.DREAM_GOAT_HORN)) {
                        cleanseNegativeEffects(player);
                    }
                }
            }
        } else {
            player.sendMessage(ChatUtils.chatMessage("&7Goat horns do not have extra functionality in this world!"));
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

    /**
     * Rallies all nearby defenders of the player's dominion to the targeted location.
     * @param player The player blowing the horn.
     */
    private void callNearbyDefenders(Player player) {
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        if (playerDominion == null) {
            player.sendMessage(ChatUtils.chatMessage("&cYou must be in a dominion to use this horn"));
            return;
        }

        // The block the player is aiming at (up to 48 blocks)
        Block targetBlock = player.getTargetBlockExact(48);
        Location targetLocation = targetBlock != null
                ? targetBlock.getLocation().add(0.5, 1, 0.5)
                : player.getLocation();

        List<UUID> defenderIds = DefenderUtils.getDefendersInDominion(playerDominion.getId());
        int count = 0;

        for (UUID entityUUID : defenderIds) {
            Entity entity = Bukkit.getEntity(entityUUID);
            if (!(entity instanceof Mob mob) || mob.isDead()) {
                continue;
            }

            DefenderMode mode = DefenderUtils.getDefenderMode(entityUUID);
            if (mode == DefenderMode.IDLE) {
                continue;
            }

            // Only rally defenders within 64 blocks of the player
            if (entity.getLocation().distanceSquared(player.getLocation()) > 4096) {
                continue;
            }

            // Don't interrupt a defender already engaged with a hostile
            if (mob.getTarget() instanceof Monster) {
                continue;
            }

            mob.getPathfinder().moveTo(targetLocation, 1.4);
            count++;
        }

        // Visual and audio effects at the target location to mark the rally point
        World world = player.getWorld();
        world.spawnParticle(Particle.FLAME, targetLocation, 40, 1.5, 1.5, 1.5, 0.03);
        world.spawnParticle(Particle.SMOKE, targetLocation.clone().add(0, 1, 0), 20, 1, 1, 1, 0.05);
        world.playSound(targetLocation, Sound.BLOCK_BELL_USE, 1.5F, 0.8F);

        if (count == 0) {
            player.sendMessage(ChatUtils.chatMessage("&7No nearby defenders responded to the call"));
        } else {
            String amountString = (count == 1) ? " defender has" : " defenders have";
            player.sendMessage(ChatUtils.chatMessage("&e&o" + count + " been sent to the target area"));
        }
    }

    /**
     * Removes all negative potion effects from the player.
     * @param player The player.
     */
    private void cleanseNegativeEffects(Player player) {
        List<PotionEffectType> toRemove = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) {
                toRemove.add(effect.getType());
            }
        }

        for (PotionEffectType type : toRemove) {
            player.removePotionEffect(type);
        }
    }

    /**
     * Summons the pre-designated sentinels associated to the player.
     * @param player The player who blew the horn.
     * @param sentinelType The sentinel type that the player will be summoning.
     */
    private void summonSentinels(Player player, EntityType sentinelType) {
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (sentinelType == EntityType.IRON_GOLEM || sentinelType == EntityType.WOLF) {
            List<Sentinel> sentinels = aranarthPlayer.getSentinels().get(sentinelType);

            // Must manually load the chunk to allow the entity to teleport
            for (Sentinel sentinel : sentinels) {
                Chunk chunk = sentinel.getLocation().getChunk();
                chunk.load(true);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    Player closestPlayerTarget = getTarget(player);

                    for (Sentinel sentinel : sentinels) {
                        Entity entity = Bukkit.getEntity(sentinel.getUuid());
                        entity.teleport(player.getLocation());
                        if (entity instanceof Mob mob) {
                            if (closestPlayerTarget != null) {
                                mob.setTarget(closestPlayerTarget);
                            }
                            if (mob instanceof Wolf wolf) {
                                wolf.setSitting(false);
                            }
                        }
                    }
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getEyeLocation(), 250, 3, 2, 3);
                }
            }.runTaskLater(AranarthCore.getInstance(), 60L);
        } else if (sentinelType == EntityType.HORSE) {
            Sentinel sentinel = aranarthPlayer.getSentinels().get(sentinelType).getFirst();

            // Must manually load the chunk to allow the entity to teleport
            Chunk chunk = sentinel.getLocation().getChunk();
            chunk.load(true);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Entity entity = Bukkit.getEntity(sentinel.getUuid());
                    entity.teleport(player.getLocation());
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.9F);
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getEyeLocation(), 250, 3, 2, 3);
                }
            }.runTaskLater(AranarthCore.getInstance(), 60L);
        }
    }

    /**
     * Provides the target of the player using the horn.
     * @param player The player using the horn.
     * @return The target of the player using the horn.
     */
    private Player getTarget(Player player) {
        Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        double closestDistance = 64;
        Player closestPlayer = null;

        for (Entity entity : player.getNearbyEntities(64, 16, 64)) {
            if (entity instanceof Player nearbyPlayer) {
                double distance = player.getLocation().distance(nearbyPlayer.getLocation());
                if (distance < closestDistance) {
                    Dominion nearbyDominion = DominionUtils.getPlayerDominion(nearbyPlayer.getUniqueId());

                    if ((playerDominion == null || nearbyDominion == null) ||
                            (!nearbyDominion.isSameDominion(playerDominion)
                            && !playerDominion.isAllied(nearbyDominion)
                            && !playerDominion.isTruced(nearbyDominion))) {
                        closestPlayer = nearbyPlayer;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closestPlayer;
    }

}
