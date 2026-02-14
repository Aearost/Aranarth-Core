package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Sentinel;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
                        dropNearbyApples(player);
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
     * Shakes nearby trees and drops apples and god apple fragments from them.
     * @param player The player.
     */
    private void dropNearbyApples(Player player) {
        if (player.getWorld().getName().equals("world") || player.getWorld().getName().equals("smp")
                || player.getWorld().getName().equals("resource")) {
            Location loc = player.getLocation();
            Block below = loc.subtract(0, 1, 0).getBlock();
            if (below.getType() == Material.GRASS_BLOCK || below.getType() == Material.DIRT
                    || below.getType() == Material.SAND || below.getType() == Material.GRAVEL
                    || below.getType() == Material.OAK_LOG || below.getType() == Material.BIRCH_LOG) {
                // Iterate 20 blocks around the player, and 5 blocks above/below for each leaf
                for (int x = loc.getBlockX() - 10; x < loc.getBlockX() + 10; x++) {
                    for (int z = loc.getBlockZ() - 10; z < loc.getBlockZ() + 10; z++) {
                        for (int y = loc.getBlockY() + 5; y > loc.getBlockY() - 5; y--) {
                            Block block = loc.getWorld().getBlockAt(x, y, z);
                            if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.DARK_OAK_LEAVES) {
                                Block belowBlock = block.getWorld().getBlockAt(x, y - 1, z);
                                if (belowBlock.getType() == Material.AIR || belowBlock.getType() == Material.LEAF_LITTER) {
                                    if (AranarthUtils.getMonth() != Month.SOLARVOR) {
                                        // 2.5% chance of dropping an apple
                                        if (new Random().nextInt(40) == 0) {
                                            belowBlock.getLocation().getWorld().dropItemNaturally(belowBlock.getLocation(), new ItemStack(Material.APPLE));
                                        }
                                        // 0.25% chance of dropping a god apple fragment during normal months
                                        else if (new Random().nextInt(400) == 0) {
                                            belowBlock.getLocation().getWorld().dropItemNaturally(belowBlock.getLocation(), new GodAppleFragment().getItem());
                                            for (Player nearby : Bukkit.getOnlinePlayers()) {
                                                if (!belowBlock.getWorld().getName().equals(nearby.getWorld().getName())) {
                                                    continue;
                                                }

                                                // If the player is within 48 blocks of the spawn location
                                                if (belowBlock.getLocation().distance(nearby.getLocation()) <= 48) {
                                                    nearby.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has dropped nearby"));
                                                }
                                            }
                                        }
                                    }
                                    // Increased God Apple Fragment drop rates
                                    else {
                                        // 5% chance of dropping an apple
                                        if (new Random().nextInt(20) == 0) {
                                            belowBlock.getLocation().getWorld().dropItemNaturally(belowBlock.getLocation(), new ItemStack(Material.APPLE));
                                        }
                                        // 0.5% chance of dropping a god apple fragment during Solarvor
                                        else if (new Random().nextInt(20) == 0) {
                                            belowBlock.getLocation().getWorld().dropItemNaturally(belowBlock.getLocation(), new GodAppleFragment().getItem());
                                            for (Player nearby : Bukkit.getOnlinePlayers()) {
                                                if (!belowBlock.getWorld().getName().equals(nearby.getWorld().getName())) {
                                                    continue;
                                                }

                                                // If the player is within 48 blocks of the spawn location
                                                if (belowBlock.getLocation().distance(nearby.getLocation()) <= 48) {
                                                    nearby.sendMessage(ChatUtils.chatMessage("&7A god apple fragment has dropped nearby"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
                            (!nearbyDominion.getLeader().equals(playerDominion.getLeader())
                            && !DominionUtils.areAllied(playerDominion, nearbyDominion)
                            && !DominionUtils.areTruced(playerDominion, nearbyDominion))) {
                        closestPlayer = nearbyPlayer;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closestPlayer;
    }

}
