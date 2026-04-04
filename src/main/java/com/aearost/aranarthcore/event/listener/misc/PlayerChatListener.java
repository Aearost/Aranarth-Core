package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Handles formatting chat messages.
 * Based on <a href="https://www.spigotmc.org/threads/editing-message-to-player-from-asyncplayerchatevent.362198/">Spigot URL</a>
 */
public class PlayerChatListener implements Listener {

	public PlayerChatListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
    public void chatEvent(final AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();

        Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());
        // If resources are actively being claimed by the Dominion, prioritize this above all other chat functionality
        if (dominion != null) {
            if (dominion.getBiomeResourcesBeingClaimed() != null) {
                if (player.getUniqueId().equals(dominion.getLeader())) {
                    e.setCancelled(true);
                    try {
                        int enteredNumber = Integer.parseInt(message);
                        if (enteredNumber <= 0) {
                            throw new NumberFormatException();
                        }

                        if (enteredNumber > dominion.getClaimableResources()) {
                            enteredNumber = dominion.getClaimableResources();
                        }

                        while (enteredNumber > 0) {
                            claimDominionResources(dominion, player);
                            enteredNumber--;
                        }
                        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1F);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatUtils.chatMessage("&cThat number is invalid!"));
                    }
                    dominion.setBiomeResourcesBeingClaimed(null);
                    DominionUtils.updateDominion(dominion);
                    return;
                }
            }
        }

        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

        // Prevents chat messages from going through if the receiving user has toggled their chat
        List<Player> toRemove = new ArrayList<>();
        Iterator<Player> recipientIterator = e.getRecipients().iterator();
        for (int i = 0; i < e.getRecipients().size(); i++) {
            Player recipient = recipientIterator.next();
            AranarthPlayer recipientAranarthPlayer = AranarthUtils.getPlayer(recipient.getUniqueId());
            if (recipientAranarthPlayer.isTogglingChat()) {
                // Only block non-council messages
                if (aranarthPlayer.getCouncilRank() == 0) {
                    toRemove.add(recipient);
                    continue;
                }
            }

            boolean isSenderTheRecipient = player.getDisplayName().equals(recipient.getDisplayName());
            String strippedNickname = ChatUtils.stripColorFormatting(recipientAranarthPlayer.getNickname());
            if (!isSenderTheRecipient && (message.toLowerCase().contains(recipient.getDisplayName().toLowerCase())
                    || message.toLowerCase().contains(strippedNickname.toLowerCase()))) {
                recipient.playSound(recipient, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
            }
        }
        e.getRecipients().removeAll(toRemove);


        if (aranarthPlayer.getAfkLocation() != null) {
            // Automatically un-afk the player if they type a message
            if (aranarthPlayer.getAfkLocation().getSeconds() >= AranarthUtils.getAfkSecondsAmount()) {
                AranarthUtils.toggleAfkStatus(player.getUniqueId(), false);
            }
            // Reset their AFK timer
            else {
                aranarthPlayer.setAfkLocation(null);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            }
        }

        if (ChatUtils.isPlayerMuted(player)) {
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot send any messages as you are muted!"));
            e.setCancelled(true);
            return;
        }

        String prefix = ChatUtils.formatChatPrefix(player);
        String chatMessage = ChatUtils.formatChatMessage(player, message);
        chatMessage = chatMessage.replaceAll("%", "%%"); // Throws exception with only one

        e.setCancelled(true);

        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + aranarthPlayer.getNickname() + "&e's &7info");
        // Deserialize with legacySection() since formatChatPrefix has already translated & → §
        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);
        prefixComponent = ChatUtils.clickableCommand(prefixComponent, hoverMsg, "/info " + player.getName(), true);

        // Use Component.empty() as root so chatMessage is a sibling of prefixComponent, not a child.
        // Children inherit hover/click from their parent, siblings do not.
        Component fullMessage = Component.empty()
                .append(prefixComponent)
                .append(LegacyComponentSerializer.legacySection().deserialize(chatMessage));

        for (Player recipient : e.getRecipients()) {
            recipient.sendMessage(fullMessage);
        }

        Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                ChatUtils.translateToColor(prefix + chatMessage)));

        DiscordUtils.sendChatMessage(prefix + chatMessage);
    }

    /**
     * Claims the resources of the Dominion based on the stored Biome variable.
     * @param dominion The Dominion.
     * @param player The player claiming the resources.
     */
    private void claimDominionResources(Dominion dominion, Player player) {
        List<ItemStack> resourcesToClaim = DominionUtils.getResourcesByDominionAndBiome(dominion, dominion.getBiomeResourcesBeingClaimed());
        Location loc = player.getLocation();
        for (ItemStack resource : resourcesToClaim) {
            HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(resource);
            if (!remainder.isEmpty()) {
                loc.getWorld().dropItemNaturally(loc, remainder.get(0));
            }
        }
        dominion.setClaimableResources(dominion.getClaimableResources() - 1);
        player.sendMessage(ChatUtils.chatMessage("&7The resources have been added to your inventory"));
    }
}
