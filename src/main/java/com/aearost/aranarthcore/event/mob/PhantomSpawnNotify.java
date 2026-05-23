package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.*;

/**
 * Notifies players when phantoms spawn and begin targeting them.
 * Phantoms spawning within a short time window are grouped into a wave,
 * and each player receives at most one notification per wave.
 */
public class PhantomSpawnNotify {

    private static final Map<UUID, UUID> phantomWave = new HashMap<>();
    private static final Map<UUID, Integer> waveSize = new HashMap<>();
    private static final Map<UUID, Set<UUID>> notifiedWaves = new HashMap<>();
    // Tracks a pending delayed notification per player (player UUID -> wave UUID)
    private static final Map<UUID, UUID> pendingNotification = new HashMap<>();

    private static UUID currentWaveId = null;
    private static long currentWaveStartTime = 0L;

    public void execute(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Phantom)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (currentWaveId == null || (now - currentWaveStartTime) > 5000L) {
            currentWaveId = UUID.randomUUID();
            currentWaveStartTime = now;
            waveSize.put(currentWaveId, 0);
        }

        phantomWave.put(e.getEntity().getUniqueId(), currentWaveId);
        waveSize.merge(currentWaveId, 1, Integer::sum);
    }

    public void execute(EntityTargetEvent e) {
        if (!(e.getEntity() instanceof Phantom) || !(e.getTarget() instanceof Player player)) {
            return;
        }

        UUID waveId = phantomWave.get(e.getEntity().getUniqueId());
        if (waveId == null) {
            return;
        }

        // Already notified for this wave
        Set<UUID> playerNotified = notifiedWaves.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (playerNotified.contains(waveId)) {
            return;
        }

        // Already have a pending notification scheduled for this wave
        if (waveId.equals(pendingNotification.get(player.getUniqueId()))) {
            return;
        }

        // Schedule a 2-tick delayed notification so all phantoms in the batch
        // have time to spawn and join the wave before the message is sent
        pendingNotification.put(player.getUniqueId(), waveId);
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            pendingNotification.remove(player.getUniqueId(), waveId);
            Set<UUID> notified = notifiedWaves.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
            if (!notified.add(waveId) || !waveSize.containsKey(waveId)) {
                return;
            }
            int count = waveSize.getOrDefault(waveId, 1);
            String message = count > 1 ? "&7Phantoms have started attacking you." : "&7A phantom has started attacking you.";
            player.sendMessage(ChatUtils.chatMessage(message));
        }, 5L);
    }

    public void execute(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Phantom)) {
            return;
        }

        UUID waveId = phantomWave.remove(e.getEntity().getUniqueId());
        if (waveId == null) {
            return;
        }

        waveSize.computeIfPresent(waveId, (k, v) -> v <= 1 ? null : v - 1);
        if (!waveSize.containsKey(waveId)) {
            notifiedWaves.values().forEach(set -> set.remove(waveId));
        }
    }
}
