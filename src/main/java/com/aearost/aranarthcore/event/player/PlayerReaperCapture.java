package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.ItemUtils;
import com.aearost.aranarthcore.utils.ReaperManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * On player death, captures all drops into a Reaper Inventory persisted in the database.
 */
public class PlayerReaperCapture {

    public void execute(PlayerDeathEvent e) {
        Player player = e.getEntity();

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        if (aranarthPlayer != null && aranarthPlayer.isReaperDisabled()) {
            return;
        }

        List<ItemStack> drops = e.getDrops();

        if (drops.isEmpty()) {
            return;
        }

        ItemStack[] dropsArray = drops.toArray(new ItemStack[0]);
        drops.clear();

        String dropsB64;
        try {
            dropsB64 = ItemUtils.itemStackArrayToBase64(dropsArray);
        } catch (IllegalStateException ex) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to serialize reaper drops for " + player.getName() + ": " + ex.getMessage());
            return;
        }

        long deathTime = System.currentTimeMillis();
        Location deathLocation = player.getLocation();
        String deathWorld = deathLocation.getWorld() != null ? deathLocation.getWorld().getName() : "world";
        double deathX = deathLocation.getX();
        double deathY = deathLocation.getY();
        double deathZ = deathLocation.getZ();

        ReaperManager.put(player.getUniqueId(), dropsB64, deathTime, deathWorld, deathX, deathY, deathZ);
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                DatabaseManager.getInstance().upsertReaperInventory(player.getUniqueId(), dropsB64, deathTime, deathWorld, deathX, deathY, deathZ));

        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () ->
                player.sendMessage(ChatUtils.chatMessage("&7Purchase back your inventory using &e/reaper")), 1L);
    }
}
