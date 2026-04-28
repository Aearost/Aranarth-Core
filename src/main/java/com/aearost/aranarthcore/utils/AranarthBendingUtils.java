package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.Dominion;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class AranarthBendingUtils {

    private AranarthBendingUtils() {}

    /**
     * Returns true if the player's ability should be prevented because they are near
     * a Dominion they are not a member of or allied with. Checks the 3x3 chunk grid
     * centered on the player's current chunk.
     *
     * @param player The player using the ability.
     * @return Whether the ability should be cancelled.
     */
    public static boolean preventAbilityNearDominion(Player player) {
        int chunkX = player.getLocation().getChunk().getX();
        int chunkZ = player.getLocation().getChunk().getZ();
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                Dominion dominion = DominionUtils.getDominionOfChunk(chunk);
                if (dominion == null) {
                    continue;
                }
                Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
                boolean areAllied = playerDominion != null && dominion.isAllied(playerDominion);
                if (!dominion.getMembers().contains(player.getUniqueId()) && !areAllied) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes an ability instance that fires as part of a combo's input sequence and
     * wipes its cooldown, so the player is not penalised for the trigger that activated
     * the combo. The removal is deferred by one tick so PK has time to recognise the combo.
     *
     * @param bPlayer     The BendingPlayer whose cooldown should be cleared.
     * @param player      The Player whose active ability instances are searched.
     * @param abilityName The exact ability name to remove (case-sensitive).
     */
    public static void suppressComboTrigger(BendingPlayer bPlayer, Player player, String abilityName) {
        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            for (final CoreAbility ability : new ArrayList<>(CoreAbility.getAbilities(player))) {
                if (ability.getName().equals(abilityName)) {
                    ability.remove();
                    bPlayer.removeCooldown(abilityName);
                }
            }
        });
    }
}
