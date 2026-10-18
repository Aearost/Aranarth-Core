package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.DefenderUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all logic regarding preventing specified explosions.
 */
public class ExplosionListener implements Listener {

    public ExplosionListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Deals with cancelling explosion block damage.
     *
     * @param e The event.
     */
    @EventHandler
    public void onExplodeBlock(final EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof WindCharge) && !(e.getEntity() instanceof BreezeWindCharge)) {
            if (e.getEntity() instanceof EnderDragon dragon
                    && dragon.getPersistentDataContainer().has(CustomKeys.FANG_OWNER, PersistentDataType.STRING)) {
                e.setCancelled(true);
                return;
            }
            if (e.getEntity() instanceof Creeper creeper) {
                // Defender creepers: allow explosion entity damage but clear block list
                if (DefenderUtils.isDefender(creeper.getUniqueId())) {
                    e.blockList().clear();
                    // Remove all potion effects before the explosion finalizes so no lingering cloud spawns
                    for (org.bukkit.potion.PotionEffect effect : new java.util.ArrayList<>(creeper.getActivePotionEffects())) {
                        creeper.removePotionEffect(effect.getType());
                    }
                    // Decrement the defender count - EntityDeathEvent may not fire reliably for explosion deaths
                    DefenderUtils.onDefenderDeath(creeper.getUniqueId());
                    return;
                }
                // Non-defender creepers: cancel entirely (existing behavior)
                e.setCancelled(true);
                return;
            }

            if (isExplosionAllowed(e.getEntity().getLocation().getChunk())) {
                filterBlocks(e.blockList());
            } else {
                e.setCancelled(true);
            }
        }
    }

    /**
     * Deals with block explosions such as beds and respawn anchors.
     * @param e The event.
     */
    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent e) {
        if (isExplosionAllowed(e.getBlock().getChunk())) {
            filterBlocks(e.blockList());
        } else {
            e.setCancelled(true);
        }
    }

    /**
     * Removes locked containers and blocks in land where explosions are disabled, so an explosion just
     * outside a border cannot destroy protected blocks.
     */
    private void filterBlocks(List<Block> blocks) {
        Map<Long, Boolean> allowedByChunk = new HashMap<>();
        blocks.removeIf(block -> {
            long chunkKey = ((long) (block.getX() >> 4) << 32) | ((block.getZ() >> 4) & 0xFFFFFFFFL);
            boolean allowed = allowedByChunk.computeIfAbsent(chunkKey, k -> isExplosionAllowed(block.getChunk()));
            return !allowed || AranarthUtils.getLockedContainerAtBlock(block) != null;
        });
    }

    /**
     * Provides whether explosions may damage blocks in the chunk, using the outpost flag first and then
     * the dominion flag. Unclaimed land always allows explosions.
     */
    private boolean isExplosionAllowed(Chunk chunk) {
        Outpost outpost = OutpostUtils.getOutpostOfChunk(chunk);
        if (outpost != null) {
            return outpost.isExplosionEnabled();
        }
        Dominion chunkDominion = DominionUtils.getDominionOfChunk(chunk);
        return chunkDominion == null || chunkDominion.isExplosionEnabled();
    }

    /**
     * Deals with cancelling explosion item damage.
     *
     * @param e The event.
     */
    @EventHandler
    public void onExplodeItem(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Item) {
            if (e.getCause() == DamageCause.BLOCK_EXPLOSION || e.getCause() == DamageCause.ENTITY_EXPLOSION) {
                if (!isExplosionAllowed(e.getEntity().getLocation().getChunk())) {
                    e.setCancelled(true);
                }
            }
        }
    }

}
