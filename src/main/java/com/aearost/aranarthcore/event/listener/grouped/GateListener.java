package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.Gate;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Centralizes all event functionality involving the custom gate functionality on Aranarth.
 */
public class GateListener implements Listener {

    public GateListener(AranarthCore plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Material type = e.getBlockPlaced().getType();
        if (!GateUtils.isGateMaterial(type)) {
            return;
        }

        Location placedLoc = GateUtils.normalized(e.getBlockPlaced().getLocation());
        Player player = e.getPlayer();

        // Interior positions are air in the world but still tracked in the block set
        Gate occupant = GateUtils.getGateAt(placedLoc);
        if (occupant != null && occupant.isOpen()) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot place a block inside an open gate!"));
            return;
        }

        // Route through GateUtils — it decides whether to register the block as a gate
        // block based on placement mode / sneaking state, and validates axis alignment.
        String error = GateUtils.handleBlockPlaced(e.getBlockPlaced(), player);
        if (error != null) {
            e.setCancelled(true);
            player.sendMessage(ChatUtils.chatMessage("&c" + error));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!GateUtils.isGateMaterial(e.getBlock().getType())) {
            return;
        }
        GateUtils.handleBlockRemoved(e.getBlock().getLocation());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        // Gate blocks are indestructible — remove them from the explosion block list.
        e.blockList().removeIf(block ->
                GateUtils.isGateMaterial(block.getType()) && GateUtils.getGateAt(block.getLocation()) != null
        );
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        // Gate blocks are indestructible — remove them from the explosion block list.
        e.blockList().removeIf(block ->
                GateUtils.isGateMaterial(block.getType()) && GateUtils.getGateAt(block.getLocation()) != null
        );
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        if (!GateUtils.isGateMaterial(e.getBlock().getType())) {
            return;
        }
        if (GateUtils.getGateAt(e.getBlock().getLocation()) != null) {
            e.setCancelled(true);
        }
    }

    /**
     * Opening and closing gates.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (e.getClickedBlock() == null) {
            return;
        }
        if (!GateUtils.isGateMaterial(e.getClickedBlock().getType())) {
            return;
        }

        Gate gate = GateUtils.getGateAt(e.getClickedBlock().getLocation());
        if (gate == null) {
            return;
        }

        // Extend the gate, do not cancel
        Player player = e.getPlayer();
        if (player.isSneaking()) {
            Material held = player.getInventory().getItemInMainHand().getType();
            if (GateUtils.isGateMaterial(held)) {
                return;
            }
        }

        e.setCancelled(true);

        // Enforce the Dominion "Gates" (fence gate) permission.
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (!aranarthPlayer.isInAdminMode()) {
            Dominion dominion = DominionUtils.getDominionOfChunk(e.getClickedBlock().getChunk());
            if (dominion != null && !DominionUtils.hasPermission(player, dominion, DominionPermission.FENCE_GATE)) {
                player.sendMessage(ChatUtils.chatMessage("&cYou don't have permission to open this gate!"));
                return;
            }
        }

        String error = GateUtils.toggleGate(gate);
        if (error != null) {
            player.sendMessage(ChatUtils.chatMessage("&c" + error));
            return;
        }
    }
}
