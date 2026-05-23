package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiToggle;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.GateUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

public class GuiToggleClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        Player player = (Player) e.getWhoClicked();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        int slot = e.getSlot();

        switch (slot) {
            // Exit
            case 31 -> {
                player.closeInventory();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
            }
            // Blacklist
            case 9 -> {
				if (!player.hasPermission("aranarth.blacklist")) {
					return;
				}
                if (aranarthPlayer.getBlacklistingMethod() != -1) {
                    aranarthPlayer.setBlacklistingMethod(-1);
                    player.sendMessage(ChatUtils.chatMessage("&7Your blacklist is now &cdisabled"));
                } else {
                    aranarthPlayer.setBlacklistingMethod(0);
                    player.sendMessage(ChatUtils.chatMessage("&7You will now ignore blacklisted items"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Blue Fire
            case 10 -> {
				if (!aranarthPlayer.getPerks().containsKey(Perk.BLUEFIRE) || aranarthPlayer.getPerks().get(Perk.BLUEFIRE) != 1) {
					return;
				}
                if (aranarthPlayer.hasBlueFireDisabled()) {
                    aranarthPlayer.setBlueFireDisabled(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7your blue fire"));
                } else {
                    aranarthPlayer.setBlueFireDisabled(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7your blue fire"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                PermissionUtils.evaluatePlayerPermissions(player);
                refreshGui(player);
            }
            // Bulk Sell Shulker
            case 11 -> {
				if (!player.hasPermission("aranarth.shulker")) {
					return;
				}
                if (aranarthPlayer.isBulkSellShulkerEnabled()) {
                    aranarthPlayer.setBulkSellShulkerEnabled(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7bulk sell shulker"));
                } else {
                    aranarthPlayer.setBulkSellShulkerEnabled(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7bulk sell shulker"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Dominion Claim Messages
            case 12 -> {
                if (aranarthPlayer.isTogglingChangeClaim()) {
                    aranarthPlayer.setTogglingChangeClaim(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7Dominion claim change messages"));
                } else {
                    aranarthPlayer.setTogglingChangeClaim(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7Dominion claim change messages"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Chat
            case 13 -> {
				if (!player.hasPermission("aranarth.toggle.chat")) {
					return;
				}
                if (aranarthPlayer.isTogglingChat()) {
                    aranarthPlayer.setTogglingChat(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7chat messages"));
                } else {
                    aranarthPlayer.setTogglingChat(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7chat messages"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Chest Lock
            case 14 -> {
                if (aranarthPlayer.isAutoLockingChests()) {
                    aranarthPlayer.setAutoLockingChests(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7automatic chest locking"));
                } else {
                    aranarthPlayer.setAutoLockingChests(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7automatic chest locking"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Compressor
            case 15 -> {
				if (!player.hasPermission("aranarth.compressor")) {
					return;
				}
                if (aranarthPlayer.isCompressingItems()) {
                    aranarthPlayer.setCompressingItems(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the compressor"));
                } else {
                    aranarthPlayer.setCompressingItems(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the compressor"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Day Message
            case 16 -> {
                if (aranarthPlayer.isDayMessageDisabled()) {
                    aranarthPlayer.setDayMessageDisabled(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the new day message"));
                } else {
                    aranarthPlayer.setDayMessageDisabled(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the new day message"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Gate Creation
            case 17 -> {
				if (!player.hasPermission("aranarth.gate")) {
					return;
				}
                boolean enabled = GateUtils.toggleGatePlacementMode(player.getUniqueId());
                if (enabled) {
                    player.sendMessage(ChatUtils.chatMessage("&7Gate creation mode &aenabled&7. Place a fence or bar block to start a new gate."));
                } else {
                    player.sendMessage(ChatUtils.chatMessage("&7Gate creation mode &cdisabled&7."));
                }
                refreshGui(player);
            }
            // Gradient Chat
            case 18 -> {
                boolean hasAccess = aranarthPlayer.getPerks().containsKey(Perk.CHAT) || aranarthPlayer.getSaintRank() >= 2;
				if (!hasAccess) {
					return;
				}
                if (aranarthPlayer.isGradientChatEnabled()) {
                    aranarthPlayer.setGradientChatEnabled(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7gradient chat"));
                } else {
                    if (aranarthPlayer.getGradientChatColors().isEmpty()) {
                        player.sendMessage(ChatUtils.chatMessage("&cYou have not saved any gradient colors yet"));
                        return;
                    }
                    aranarthPlayer.setGradientChatEnabled(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7gradient chat"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Inventory Assist
            case 19 -> {
				if (!player.hasPermission("aranarth.inventory")) {
					return;
				}
                if (aranarthPlayer.isTogglingInventoryAssist()) {
                    aranarthPlayer.setTogglingInventoryAssist(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the inventory assist perk"));
                } else {
                    aranarthPlayer.setTogglingInventoryAssist(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the inventory assist perk"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Private Messages
            case 20 -> {
				if (!player.hasPermission("aranarth.toggle.msg")) {
					return;
				}
                if (aranarthPlayer.isTogglingMessages()) {
                    aranarthPlayer.setTogglingMessages(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7private messages"));
                } else {
                    aranarthPlayer.setTogglingMessages(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7private messages"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Pet Hurt
            case 21 -> {
                if (aranarthPlayer.isHurtingOwnPets()) {
                    aranarthPlayer.setHurtingOwnPets(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the ability to hurt your own pets"));
                } else {
                    aranarthPlayer.setHurtingOwnPets(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the ability to hurt your own pets"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Shulker Assist
            case 22 -> {
				if (!player.hasPermission("aranarth.shulker")) {
					return;
				}
                if (aranarthPlayer.isAddingToShulker()) {
                    aranarthPlayer.setAddingToShulker(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the shulker assist perk"));
                } else {
                    aranarthPlayer.setAddingToShulker(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the shulker assist perk"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Spawn Boost
            case 23 -> {
                if (aranarthPlayer.isUsingSpawnBoost()) {
                    aranarthPlayer.setUsingSpawnBoost(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7the spawn boost effects"));
                    if (AranarthUtils.isSpawnLocation(player.getLocation())) {
                        player.clearActivePotionEffects();
                    }
                } else {
                    aranarthPlayer.setUsingSpawnBoost(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7the spawn boost effects"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Teleport Requests
            case 24 -> {
				if (!player.hasPermission("aranarth.toggle.tp")) {
					return;
				}
                if (aranarthPlayer.isTogglingTp()) {
                    aranarthPlayer.setTogglingTp(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7teleport requests"));
                } else {
                    aranarthPlayer.setTogglingTp(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7teleport requests"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
            // Weather Messages
            case 25 -> {
                if (aranarthPlayer.isWeatherMessageDisabled()) {
                    aranarthPlayer.setWeatherMessageDisabled(false);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &aenabled &7weather change messages"));
                } else {
                    aranarthPlayer.setWeatherMessageDisabled(true);
                    player.sendMessage(ChatUtils.chatMessage("&7You have &cdisabled &7weather change messages"));
                }
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                refreshGui(player);
            }
        }
    }

    private void refreshGui(Player player) {
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 0.8F);
        new GuiToggle(player).openGui();
    }

}
