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
import com.aearost.aranarthcore.utils.EmojiUtils;
import com.aearost.aranarthcore.utils.InteractiveChatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles formatting chat messages.
 * Based on <a href="https://www.spigotmc.org/threads/editing-message-to-player-from-asyncplayerchatevent.362198/">Spigot URL</a>
 */
public class PlayerChatListener implements Listener {

    private final AranarthCore plugin;

    /** Matches [item], [inv], [ec], [coords], [pos] case-insensitively. */
    private static final Pattern INTERACTIVE_PATTERN =
            Pattern.compile("\\[item]|\\[inv]|\\[ec]|\\[coords?]|\\[pos]", Pattern.CASE_INSENSITIVE);

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
                int pmVol = AranarthUtils.getPlayer(recipient.getUniqueId()).getPrivateMsgSoundVolume();
                if (pmVol > 0) {
                    recipient.playSound(recipient, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f * (pmVol / 100f), 1f);
                }
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

        if (aranarthPlayer.isEmojiEnabled()) {
            message = EmojiUtils.translateEmojis(message);
        }

        String prefix = ChatUtils.formatChatPrefix(player);
        String chatMessage = ChatUtils.formatChatMessage(player, message);
        // preserve unescaped form for council routing

        e.setCancelled(true);

        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + aranarthPlayer.getNickname() + "&e's &7info");
        // Deserialize with legacySection() since formatChatPrefix has already translated & → §
        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);
        prefixComponent = ChatUtils.clickableCommand(prefixComponent, hoverMsg, "/info " + player.getName(), true);

        // Build the message component, injecting interactive chat keywords where applicable.
        Component messageComponent = null;
        boolean canUseInteractiveChat = InteractiveChatManager.hasInteractiveChatPerm(aranarthPlayer)
                && aranarthPlayer.isInteractiveChatEnabled()
                && INTERACTIVE_PATTERN.matcher(message).find();
        if (canUseInteractiveChat) {
            messageComponent = buildInteractiveChatComponent(player, aranarthPlayer, message, chatMessage);
        }
        // Fall back to standard gradient or plain message building.
        if (messageComponent == null) {
            if (aranarthPlayer.isGradientChatEnabled() && !aranarthPlayer.getGradientChatColors().isEmpty()) {
                messageComponent = ChatUtils.buildGradientMessageWithUrls(
                        aranarthPlayer.getGradientChatColors(), ChatUtils.stripColorFormatting(message), aranarthPlayer.isGradientChatBold());
            }
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
            // Council chat toggle is on - route to council chat once (evaluateCouncilMessage sends to all council members)
            // Pass raw message (no gradient) since council chat is not public chat
            ChatUtils.evaluateCouncilMessage(player, message.split(" "), false);
        } else if (aranarthPlayer.isInDominionChat()) {
            // Dominion chat toggle is on - route to dominion chat
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
     * Builds a Component for the player's message.
     */
    private Component buildInteractiveChatComponent(Player player, AranarthPlayer ap, String rawMessage, String chatMessage) {
        String nickname = ap.getNickname().isEmpty() ? player.getName() : ChatUtils.stripColorFormatting(ap.getNickname());
        Location loc = player.getLocation();
        Component result = Component.empty();

        Matcher matcher = INTERACTIVE_PATTERN.matcher(rawMessage);
        int lastEnd = 0;

        while (matcher.find()) {
            // Append the text segment before this keyword
            if (matcher.start() > lastEnd) {
                String rawSegment = rawMessage.substring(lastEnd, matcher.start());
                result = result.append(buildSegment(player, ap, rawSegment, chatMessage, lastEnd));
            }

            String keyword = matcher.group().toLowerCase();
            Component interactiveComp = buildKeywordComponent(player, ap, nickname, loc, keyword);
            result = result.append(interactiveComp);
            lastEnd = matcher.end();
        }

        // Append any remaining text after the last keyword
        if (lastEnd < rawMessage.length()) {
            String rawSegment = rawMessage.substring(lastEnd);
            result = result.append(buildSegment(player, ap, rawSegment, chatMessage, lastEnd));
        }

        return result;
    }

    /**
     * Formats a plain text segment using the player's chat permissions (gradient or color).
     */
    private Component buildSegment(Player player, AranarthPlayer ap, String rawSegment, String chatMessage, int rawOffset) {
        if (rawSegment.isEmpty()) return Component.empty();

        if (ap.isGradientChatEnabled() && !ap.getGradientChatColors().isEmpty()) {
            String stripped = ChatUtils.stripColorFormatting(rawSegment);
            Component gradComp = ChatUtils.buildGradientMessageWithUrls(
                    ap.getGradientChatColors(), stripped, ap.isGradientChatBold());
            if (gradComp != null) return gradComp;
        }

        // Non-gradient
        String formattedSegment = applySegmentFormatting(player, rawSegment);
        return ChatUtils.buildMessageWithUrls(formattedSegment);
    }

    private String applySegmentFormatting(Player player, String segment) {
        if (player.hasPermission("aranarth.chat.hex")) {
            return ChatUtils.translateToColor(segment);
        } else if (player.hasPermission("aranarth.chat.color")) {
            String result = ChatUtils.playerColorChat(segment);
            return result != null ? result : segment;
        }
        return segment;
    }

    /**
     * Builds the interactive Component for a single keyword ([item], [inv], [ec], [coords]/[pos]).
     */
    private Component buildKeywordComponent(Player player, AranarthPlayer ap, String nickname, Location loc, String keyword) {
        return switch (keyword) {
            case "[item]" -> buildItemComponent(player, nickname);
            case "[inv]"  -> buildInvComponent(player, nickname);
            case "[ec]"   -> buildEcComponent(player, nickname);
            default       -> buildCoordsComponent(nickname, loc); // [coords], [coord], [pos]
        };
    }

    private Component buildItemComponent(Player player, String nickname) {
        ItemStack held = player.getInventory().getItemInMainHand().clone();
        ItemStack[] items = new ItemStack[]{held.getType().isAir() ? null : held};
        UUID snapshotId = InteractiveChatManager.storeSnapshot(
                player.getUniqueId(), nickname, InteractiveChatManager.SnapshotType.ITEM, items);

        String displayName = ChatUtils.translateToColor("&6&l[" + nickname + "'s Item]");
        Component display = LegacyComponentSerializer.legacySection().deserialize(displayName);
        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + nickname + "&7's held item");
        return ChatUtils.clickableCommand(display, hoverMsg, "/ichat view " + snapshotId, false);
    }

    private Component buildInvComponent(Player player, String nickname) {
        ItemStack[] invItems = new ItemStack[41];
        invItems[0] = cloneOrNull(player.getInventory().getHelmet());
        invItems[1] = cloneOrNull(player.getInventory().getChestplate());
        invItems[2] = cloneOrNull(player.getInventory().getLeggings());
        invItems[3] = cloneOrNull(player.getInventory().getBoots());
        invItems[4] = cloneOrNull(player.getInventory().getItemInOffHand());
        for (int i = 0; i < 27; i++) {
            invItems[5 + i] = cloneOrNull(player.getInventory().getItem(9 + i));
        }
        for (int i = 0; i < 9; i++) {
            invItems[32 + i] = cloneOrNull(player.getInventory().getItem(i));
        }
        UUID snapshotId = InteractiveChatManager.storeSnapshot(
                player.getUniqueId(), nickname, InteractiveChatManager.SnapshotType.INV, invItems);

        String displayName = ChatUtils.translateToColor("&6&l[" + nickname + "'s Inventory]");
        Component display = LegacyComponentSerializer.legacySection().deserialize(displayName);
        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + nickname + "&7's inventory");
        return ChatUtils.clickableCommand(display, hoverMsg, "/ichat view " + snapshotId, false);
    }

    private Component buildEcComponent(Player player, String nickname) {
        ItemStack[] ecItems = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            ecItems[i] = cloneOrNull(player.getEnderChest().getItem(i));
        }
        UUID snapshotId = InteractiveChatManager.storeSnapshot(
                player.getUniqueId(), nickname, InteractiveChatManager.SnapshotType.EC, ecItems);

        String displayName = ChatUtils.translateToColor("&6&l[" + nickname + "'s Ender Chest]");
        Component display = LegacyComponentSerializer.legacySection().deserialize(displayName);
        String hoverMsg = ChatUtils.translateToColor("&7Click to view &e" + nickname + "&7's ender chest");
        return ChatUtils.clickableCommand(display, hoverMsg, "/ichat view " + snapshotId, false);
    }

    private Component buildCoordsComponent(String nickname, Location loc) {
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        String displayName = ChatUtils.translateToColor("&b&l[" + nickname + "'s Coords]");
        Component display = LegacyComponentSerializer.legacySection().deserialize(displayName);

        String hoverMsg = ChatUtils.translateToColor(
                "&7X: &e" + x + " &7Y: &e" + y + " &7Z: &e" + z + "\n&7World: &e" + worldName);
        Component hover = LegacyComponentSerializer.legacySection().deserialize(hoverMsg);

        return display
                .hoverEvent(HoverEvent.showText(hover))
                .clickEvent(ClickEvent.suggestCommand("/tp " + x + " " + y + " " + z));
    }

    private static ItemStack cloneOrNull(ItemStack item) {
        return (item != null && !item.getType().isAir()) ? item.clone() : null;
    }

    /**
     * Claims the resources of the Dominion for the given biome.
     *
     * @param dominion The Dominion.
     * @param player   The player claiming the resources.
     * @param biome    The biome whose resources should be distributed.
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
