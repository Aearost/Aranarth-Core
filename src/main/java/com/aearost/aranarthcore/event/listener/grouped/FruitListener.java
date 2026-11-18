package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.FruitUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Lets players pick the fruit of custom trees, by right-clicking it or breaking it or the block it hangs from, and grows
 * picked fruit back slowly.
 */
public class FruitListener implements Listener {

    /** How often picked fruit is checked to grow back: every minute. */
    private static final long REGROW_TICKS = 1200L;

    public FruitListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                FruitUtils.trackPicked(chunk);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, FruitUtils::regrow, REGROW_TICKS, REGROW_TICKS);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        FruitUtils.trackPicked(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        FruitUtils.untrackPicked(e.getChunk());
    }

    /**
     * Right-clicking ripe fruit picks it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND || e.getClickedBlock() == null) {
            return;
        }
        Block block = e.getClickedBlock();
        if (FruitUtils.getRipe(block) == null) {
            return;
        }
        Player player = e.getPlayer();
        e.setCancelled(true);
        Dominion dominion = DominionUtils.getDominionOfChunk(block.getChunk());
        if (dominion != null && !DominionUtils.hasPermission(player, dominion, DominionPermission.MISC_INTERACT)) {
            return;
        }
        if (FruitUtils.harvest(block, player, 0, false)) {
            FruitUtils.playPickSound(block);
            player.swingMainHand();
        }
    }

    /**
     * Breaking ripe fruit, or the leaf or log it hangs from, picks it.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        if (FruitUtils.getRipe(block) != null) {
            e.setDropItems(false);
            FruitUtils.harvest(block, player, fortune, true);
            return;
        }
        // Breaking a bunch that is still growing back starts it over, and drops nothing
        FruitUtils.Spot growing = FruitUtils.getGrowingBunch(block);
        if (growing != null) {
            e.setDropItems(false);
            FruitUtils.restartBunch(block, growing);
        }
        for (Block fruit : FruitUtils.getAttached(block)) {
            FruitUtils.harvest(fruit, player, 0, true);
        }
    }

    /**
     * Fruit falls when the leaf it hangs from decays, and a bunch of fruit that decays drops its fruit.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        Block block = e.getBlock();
        for (Block fruit : FruitUtils.getAttached(block)) {
            FruitUtils.harvest(fruit, null, 0, true);
        }
        if (FruitUtils.getRipe(block) != null) {
            e.setCancelled(true);
            FruitUtils.dropDecayedBunch(block);
            block.setType(Material.AIR);
        } else if (FruitUtils.getGrowingBunch(block) != null) {
            FruitUtils.forget(block);
        }
    }

    /**
     * Fruit falls when the block it hangs from burns away. A burning bunch of fruit is lost.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Block block = e.getBlock();
        for (Block fruit : FruitUtils.getAttached(block)) {
            FruitUtils.harvest(fruit, null, 0, true);
        }
        if (FruitUtils.getStored(block) != null) {
            FruitUtils.forget(block);
        }
    }

    /**
     * A fruit head knocked off any other way, such as by an explosion or a piston, drops its fruit instead of the head.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeadDrop(ItemSpawnEvent e) {
        if (e.getEntity().getItemStack().getType() != Material.PLAYER_HEAD) {
            return;
        }
        Location location = e.getLocation();
        Block block = location.getBlock();
        if (FruitUtils.dropFallenHead(block)) {
            e.setCancelled(true);
        }
    }
}
