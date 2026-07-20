package com.aearost.aranarthcore.event.listener.misc;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiDominionPlayerPermissions;
import com.aearost.aranarthcore.network.NetworkManager;
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
import org.bukkit.block.Biome;
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

	private final AranarthCore plugin;

	public PlayerChatListener(AranarthCore plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
    public void chatEvent(final AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();

        // If the player is awaiting a user-search input for the player permission GUI, handle it first
        if (GuiDominionPlayerPermissions.isAwaitingSearch(player.getUniqueId())) {
            e.setCancelled(true);
            GuiDominionPlayerPermissions.handleSearchInput(player, message);
            return;
        }

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

                        final int count = enteredNumber;
                        final Biome capturedBiome = dominion.getBiomeResourcesBeingClaimed();
                        // Deduct and clear state immediately on this thread to prevent a second claim
                        // being started while the main-thread task is still queued.
                        dominion.setClaimableResources(dominion.getClaimableResources() - count);
                        dominion.setBiomeResourcesBeingClaimed(null);
                        DominionUtils.updateDominion(dominion);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (int i = 0; i < count; i++) {
                                claimDominionResources(dominion, player, capturedBiome);
                            }
                            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1F);
                        });
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatUtils.chatMessage("&cThat number is invalid! Please re-enter /dominion resources to try again."));
                        dominion.setBiomeResourcesBeingClaimed(null);
                    }
                    return;
                }
            }
        }

        // If another listener (e.g. chat game) already cancelled this event, don't send it to chat
        if (e.isCancelled()) {
            return;
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
        // preserve unescaped form for council routing

        e.setCancelled(true);

        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + aranarthPlayer.getNickname() + "&e's &7info");
        // Deserialize with legacySection() since formatChatPrefix has already translated & → §
        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);
        prefixComponent = ChatUtils.clickableCommand(prefixComponent, hoverMsg, "/info " + player.getName(), true);

        // Build the message component. For gradient chat, build directly from the raw message so
        // that URL characters receive their own per-character gradient colors rather than a flat color.
        Component messageComponent = null;
        if (aranarthPlayer.isGradientChatEnabled() && !aranarthPlayer.getGradientChatColors().isEmpty()) {
            messageComponent = ChatUtils.buildGradientMessageWithUrls(
                    aranarthPlayer.getGradientChatColors(), message, aranarthPlayer.isGradientChatBold());
        }
        if (messageComponent == null) {
            messageComponent = ChatUtils.buildMessageWithUrls(chatMessage);
        }

        // Use Component.empty() as root so chatMessage is a sibling of prefixComponent, not a child.
        // Children inherit hover/click from their parent, siblings do not.
        Component fullMessage = Component.empty()
                .append(prefixComponent)
                .append(messageComponent);

        if (aranarthPlayer.isInCouncilChat()) {
            // Council chat toggle is on — route to council chat once (evaluateCouncilMessage sends to all council members)
            // Pass raw message (no gradient) since council chat is not public chat
            ChatUtils.evaluateCouncilMessage(player, message.split(" "), false);
        } else if (aranarthPlayer.isInDominionChat()) {
            // Dominion chat toggle is on — route to dominion chat
            ChatUtils.evaluateDominionMessage(player, message.split(" "), false);
        } else {
            for (Player recipient : e.getRecipients()) {
                recipient.sendMessage(fullMessage);
            }
        }

        if (!aranarthPlayer.isInCouncilChat() && !aranarthPlayer.isInDominionChat()) {
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                    ChatUtils.translateToColor(prefix + chatMessage)));
        }

        if (!aranarthPlayer.isInCouncilChat() && !aranarthPlayer.isInDominionChat()) {
            DiscordUtils.sendChatMessage(prefix + chatMessage);
            // Relay to SMP server so its players see public chat
            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishChat(prefix, chatMessage);
            }
        }
    }

    /**
     * Claims the resources of the Dominion for the given biome.
     * @param dominion The Dominion.
     * @param player The player claiming the resources.
     * @param biome The biome whose resources should be distributed.
     */
    private void claimDominionResources(Dominion dominion, Player player, Biome biome) {
        List<ItemStack> resourcesToClaim = DominionUtils.getResourcesByDominionAndBiome(dominion, biome);
        Location loc = player.getLocation();
        for (ItemStack resource : resourcesToClaim) {
            HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(resource);
            if (!remainder.isEmpty()) {
                loc.getWorld().dropItemNaturally(loc, remainder.get(0));
            }
        }
        player.sendMessage(ChatUtils.chatMessage("&7The resources have been added to your inventory"));
    }
}
