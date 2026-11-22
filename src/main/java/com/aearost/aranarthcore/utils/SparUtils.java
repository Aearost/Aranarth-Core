package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles spars started via /countdown.
 */
public class SparUtils {

    private static final int STUN_TICKS = 60;
    private static final long RECENT_HIT_MILLIS = 10_000L;
    private static final long SPAR_EXPIRY_MILLIS = 30 * 60 * 1000L;

    private static final Map<UUID, UUID> partners = new HashMap<>();
    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Map<UUID, Long> lastHitByPartner = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> stunnedPlayers = new HashMap<>();

    /**
     * Starts a spar between the two players, ending any spar either of them was already in.
     *
     * @param playerA The first player.
     * @param playerB The second player.
     */
    public static void startSpar(Player playerA, Player playerB) {
        endSpar(playerA.getUniqueId());
        endSpar(playerB.getUniqueId());
        long now = System.currentTimeMillis();
        partners.put(playerA.getUniqueId(), playerB.getUniqueId());
        partners.put(playerB.getUniqueId(), playerA.getUniqueId());
        lastActivity.put(playerA.getUniqueId(), now);
        lastActivity.put(playerB.getUniqueId(), now);
    }

    /**
     * Ends the spar the player is in, if any.
     *
     * @param uuid The player's UUID.
     */
    public static void endSpar(UUID uuid) {
        UUID partner = partners.remove(uuid);
        lastActivity.remove(uuid);
        lastHitByPartner.remove(uuid);
        if (partner != null) {
            partners.remove(partner);
            lastActivity.remove(partner);
            lastHitByPartner.remove(partner);
        }
    }

    /**
     * @param uuid The player's UUID.
     * @return The UUID of the player's spar partner, or null if they are not sparring.
     */
    public static UUID getPartner(UUID uuid) {
        UUID partner = partners.get(uuid);
        if (partner == null) {
            return null;
        }
        Long last = lastActivity.get(uuid);
        if (last == null || System.currentTimeMillis() - last > SPAR_EXPIRY_MILLIS) {
            endSpar(uuid);
            return null;
        }
        return partner;
    }

    /**
     * Records that the victim was damaged by their spar partner.
     *
     * @param victim The player that was damaged.
     */
    public static void recordPartnerHit(UUID victim) {
        long now = System.currentTimeMillis();
        lastHitByPartner.put(victim, now);
        lastActivity.put(victim, now);
        UUID partner = partners.get(victim);
        if (partner != null) {
            lastActivity.put(partner, now);
        }
    }

    /**
     * @param victim The player's UUID.
     * @return True if the player's spar partner damaged them recently.
     */
    public static boolean wasRecentlyHitByPartner(UUID victim) {
        Long last = lastHitByPartner.get(victim);
        return last != null && System.currentTimeMillis() - last <= RECENT_HIT_MILLIS;
    }

    /**
     * @param player The player.
     * @return True if the player is currently stunned from losing a spar.
     */
    public static boolean isStunned(Player player) {
        return stunnedPlayers.containsKey(player.getUniqueId());
    }

    /**
     * Ends the spar with the loser stunned, sat down and invulnerable for a few seconds.
     *
     * @param loser The player that lost the spar.
     * @param winner The player that won the spar, if they are online.
     */
    public static void loseSpar(Player loser, Player winner) {
        endSpar(loser.getUniqueId());

        String loserNickname = AranarthUtils.getNickname(loser);
        if (winner != null) {
            String winnerNickname = AranarthUtils.getNickname(winner);
            loser.sendMessage(ChatUtils.chatMessage(Lang.getFor(loser, "countdown.spar_lost", "player", winnerNickname)));
            winner.sendMessage(ChatUtils.chatMessage(Lang.getFor(winner, "countdown.spar_won", "player", loserNickname)));
        } else {
            loser.sendMessage(ChatUtils.chatMessage(Lang.getFor(loser, "countdown.spar_lost", "player", "?")));
        }

        stun(loser);
    }

    /**
     * Stuns the player, making them blind, invulnerable and sat on the ground until the stun ends.
     *
     * @param player The player.
     */
    private static void stun(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable existing = stunnedPlayers.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }

        // Drop off any mount so the player falls straight down to the ground
        Entity vehicle = player.getVehicle();
        if (vehicle != null && !SitUtils.isSitCushion(vehicle)) {
            player.leaveVehicle();
        }
        player.setGliding(false);
        Vector velocity = player.getVelocity();
        player.setVelocity(new Vector(0, Math.min(0, velocity.getY()), 0));
        player.setFireTicks(0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, STUN_TICKS, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, STUN_TICKS, 255, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, STUN_TICKS, 128, false, false, false));

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= STUN_TICKS) {
                    clearStun(player);
                    return;
                }

                // Sits the player down once they reach the ground, and re-sits them if they try to stand up
                if (!SitUtils.isSitting(player) && !player.isInsideVehicle() && player.isOnGround()) {
                    SitUtils.sitOnGround(player);
                }

                if (ticks % 5 == 0) {
                    Location head = player.getEyeLocation().add(0, 0.4, 0);
                    player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, head, 2, 0.35, 0.2, 0.35, 0);
                }
                ticks++;
            }
        };
        stunnedPlayers.put(uuid, task);
        task.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);
    }

    /**
     * Removes the player's stun and stands them back up.
     *
     * @param player The player.
     */
    public static void clearStun(Player player) {
        BukkitRunnable task = stunnedPlayers.remove(player.getUniqueId());
        if (task == null) {
            return;
        }
        task.cancel();
        // The stun's potion effects last exactly as long as the stun, so they are left to expire on their own
        if (player.isOnline()) {
            SitUtils.standUp(player);
        }
    }

    /**
     * @param entity The entity that caused the damage.
     * @return The UUID of the player responsible, including the owner of a tamed pet, otherwise the entity's own UUID.
     */
    public static UUID getAttackerUuid(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            AnimalTamer owner = tameable.getOwner();
            if (owner != null) {
                return owner.getUniqueId();
            }
        }
        return entity.getUniqueId();
    }

    /**
     * @param cause The cause of the damage.
     * @return True if the damage must never be prevented, as it is meant to kill regardless (void, /kill, etc.).
     */
    public static boolean isUnpreventable(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.WORLD_BORDER
                || cause == EntityDamageEvent.DamageCause.SUICIDE;
    }

    /**
     * Spar protection only applies in Survival worlds, so kills in the arena and creative worlds behave as normal.
     *
     * @param player The player.
     * @return True if the player is in a world where spar kills are prevented.
     */
    public static boolean isInSparWorld(Player player) {
        return AranarthUtils.isSurvivalWorld(player.getWorld().getName());
    }

    /**
     * Clears all stuns, used when the plugin is disabled.
     */
    public static void clearAllStuns() {
        for (UUID uuid : new HashMap<>(stunnedPlayers).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                clearStun(player);
            } else {
                BukkitRunnable task = stunnedPlayers.remove(uuid);
                if (task != null) {
                    task.cancel();
                }
            }
        }
    }
}
