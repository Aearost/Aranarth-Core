package com.aearost.aranarthcore.commands.general;

import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

/**
 * Allows players to claim any pending crate keys that could not be
 * delivered at the time they were earned (e.g. player was in a non-survival world).
 */
public class CommandKeyClaim implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.chatMessage("&cOnly players can execute this command!"));
            return true;
        }

        String worldName = player.getWorld().getName();
        if (!AranarthUtils.isSurvivalWorld(worldName)) {
            player.sendMessage(ChatUtils.chatMessage("&cYou can only claim crate keys in survival worlds!"));
            return true;
        }

        UUID uuid = player.getUniqueId();
        Integer pendingVote = AranarthUtils.getPendingVoteKeys().get(uuid);
        Integer pendingRare = AranarthUtils.getPendingRareKeys().get(uuid);
        Integer pendingEpic = AranarthUtils.getPendingEpicKeys().get(uuid);
        Integer pendingGodly = AranarthUtils.getPendingGodlyKeys().get(uuid);

        boolean hasPending = (pendingVote != null && pendingVote > 0)
                || (pendingRare != null && pendingRare > 0)
                || (pendingEpic != null && pendingEpic > 0)
                || (pendingGodly != null && pendingGodly > 0);

        if (!hasPending) {
            player.sendMessage(ChatUtils.chatMessage("&7You have no pending crate keys to claim!"));
            return true;
        }

        int totalClaimed = 0;
        int totalRemaining = 0;

        if (pendingVote != null && pendingVote > 0) {
            int[] r = claimKeys(player, pendingVote, new KeyVote().getItem());
            totalClaimed += r[0];
            totalRemaining += r[1];
            if (r[1] == 0) {
                AranarthUtils.removePendingVoteKeys(uuid);
            } else {
                AranarthUtils.setPendingVoteKeys(uuid, r[1]);
            }
        }

        if (pendingRare != null && pendingRare > 0) {
            int[] r = claimKeys(player, pendingRare, new KeyRare().getItem());
            totalClaimed += r[0];
            totalRemaining += r[1];
            if (r[1] == 0) {
                AranarthUtils.removePendingRareKeys(uuid);
            } else {
                AranarthUtils.setPendingRareKeys(uuid, r[1]);
            }
        }

        if (pendingEpic != null && pendingEpic > 0) {
            int[] r = claimKeys(player, pendingEpic, new KeyEpic().getItem());
            totalClaimed += r[0];
            totalRemaining += r[1];
            if (r[1] == 0) {
                AranarthUtils.removePendingEpicKeys(uuid);
            } else {
                AranarthUtils.setPendingEpicKeys(uuid, r[1]);
            }
        }

        if (pendingGodly != null && pendingGodly > 0) {
            int[] r = claimKeys(player, pendingGodly, new KeyGodly().getItem());
            totalClaimed += r[0];
            totalRemaining += r[1];
            if (r[1] == 0) {
                AranarthUtils.removePendingGodlyKeys(uuid);
            } else {
                AranarthUtils.setPendingGodlyKeys(uuid, r[1]);
            }
        }

        if (totalClaimed == 0) {
            player.sendMessage(ChatUtils.chatMessage("&cYour inventory is full - make some room and try again."));
        } else if (totalRemaining > 0) {
            String keyWordClaimed = totalClaimed > 1 ? "keys" : "key";
            String keyWordRemaining = totalRemaining > 1 ? "keys" : "key";
            player.sendMessage(ChatUtils.chatMessage("&7You have claimed &e" + totalClaimed + " " + keyWordClaimed));
            player.sendMessage(ChatUtils.chatMessage("&cYour inventory is full - you still have &e" + totalRemaining + " " + keyWordRemaining + " &7to claim"));
        } else {
            player.sendMessage(ChatUtils.chatMessage("&7You have claimed all of your pending keys!"));
        }

        return true;
    }

    /**
     * Tries to give {@code pending} copies of {@code template} to the player one at a time.
     * Returns [claimed, remaining].
     */
    private int[] claimKeys(Player player, int pending, ItemStack template) {
        int claimed = 0;
        int remaining = pending;
        while (remaining > 0) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(template.clone());
            if (!leftover.isEmpty()) {
                break;
            }
            claimed++;
            remaining--;
        }
        return new int[]{claimed, remaining};
    }
}
