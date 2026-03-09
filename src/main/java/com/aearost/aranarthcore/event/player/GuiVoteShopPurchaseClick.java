package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.gui.GuiVoteShop;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Perk;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.AvatarUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.EventUtils;
import com.gmail.nossr50.util.skills.SkillTools;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Handles the teleport logic for homes.
 */
public class GuiVoteShopPurchaseClick {
    public void execute(InventoryClickEvent e) {
        // If the user did not click a slot
        if (e.getClickedInventory() == null) {
            return;
        }

        if (e.getWhoClicked() instanceof Player player) {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

            // Clicked the shop and not their inventory
            if (e.getClickedInventory().getType() == InventoryType.CHEST) {
                e.setCancelled(true);

                ItemStack clicked = e.getClickedInventory().getItem(e.getSlot());
                // Ensures the player is actually clicking a home
                if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE
                        || clicked.getType() == Material.PLAYER_HEAD) {
                    return;
                } else if (clicked.getType() == Material.BARRIER) {
                    player.closeInventory();
                    return;
                }

                // Back button
                if (e.getSlot() == 12) {
                    GuiVoteShop gui = new GuiVoteShop(player);
                    gui.openGui();
                }
                // Confirming the purchase
                else if (e.getSlot() == 14) {
                    String pointsAsString = clicked.getItemMeta().getLore().get(0).split(" ")[0];
                    int requiredPoints = Integer.parseInt(ChatUtils.stripColorFormatting(pointsAsString));
                    aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() + requiredPoints);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

                    player.closeInventory();
                    String[] parts = clicked.getItemMeta().getDisplayName().split(" ");
                    String itemName = "";
                    for (int i = 1; i < parts.length; i++) {
                        itemName += parts[i];
                        if (i < parts.length - 1) {
                            itemName += " ";
                        }
                    }

                    // One of the crate keys was selected
                    if (clicked.getType() == Material.TRIAL_KEY) {
                        String type = ChatUtils.stripColorFormatting(clicked.getItemMeta().getDisplayName()).split(" ")[1].toLowerCase();
                        Bukkit.getLogger().info(type);
                        if (type.equals("vote")) {
                            addOrDropItem(player, new KeyVote().getItem());
                        } else if (type.equals("rare")) {
                            addOrDropItem(player, new KeyRare().getItem());
                        } else if (type.equals("epic")) {
                            addOrDropItem(player, new KeyEpic().getItem());
                        } else if (type.equals("godly")) {
                            addOrDropItem(player, new KeyGodly().getItem());
                        }
                    }
                    // Bending change
                    else if (clicked.getType() == Material.WHITE_CONCRETE_POWDER) {
                        BendingPlayer bendingPlayer = new BendingPlayer(player);
                        if (bendingPlayer == null) {
                            player.sendMessage(ChatUtils.chatMessage("&cYou do not have any elements!"));
                        } else {
                            if (AvatarUtils.getCurrentAvatar() != null && AvatarUtils.getCurrentAvatar().getUuid().equals(player.getUniqueId())) {
                                player.sendMessage(ChatUtils.chatMessage("&cThe Avatar cannot use this perk!"));
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "b cooldown reset " + player.getName());
                                player.sendMessage(ChatUtils.chatMessage("&7You have purchased " + itemName));
                                return;
                            }
                        }
                        aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        return;
                    }
                    // Discord perk
                    else if (clicked.getType() == Material.PURPLE_GLAZED_TERRACOTTA) {
                        if (aranarthPlayer.getPerks().get(Perk.DISCORD) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " discord 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() + requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // Tables perk
                    else if (clicked.getType() == Material.CRAFTING_TABLE) {
                        if (aranarthPlayer.getPerks().get(Perk.TABLES) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " tables 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // Invisible Item Frames perk
                    else if (clicked.getType() == Material.GLOW_ITEM_FRAME) {
                        if (aranarthPlayer.getPerks().get(Perk.ITEMFRAME) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " itemframe 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // Colored chat perk
                    else if (clicked.getType() == Material.WRITABLE_BOOK) {
                        if (aranarthPlayer.getPerks().get(Perk.CHAT) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " chat 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // Item name perk
                    else if (clicked.getType() == Material.NAME_TAG) {
                        if (aranarthPlayer.getPerks().get(Perk.ITEMNAME) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " itemname 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // Blacklist perk
                    else if (clicked.getType() == Material.LAVA_BUCKET) {
                        if (aranarthPlayer.getPerks().get(Perk.BLACKLIST) == 0) {
                            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac perks " + player.getName() + " blacklist 1");
                        } else {
                            player.sendMessage(ChatUtils.chatMessage("&cYou already have this perk!"));
                            aranarthPlayer.setVotePointsSpent(aranarthPlayer.getVotePointsSpent() - requiredPoints);
                            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                        }
                        return;
                    }
                    // mcMMO Boost
                    else if (clicked.getType() == Material.PAPER) {
                        String amount = clicked.getItemMeta().getDisplayName().split(" ")[4];
                        int levelsToIncrease = 0;
                        if (amount.equals("+10")) {
                            levelsToIncrease = 10;
                        } else if (amount.equals("+50")) {
                            levelsToIncrease = 50;
                        }
                        McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
                        PlayerProfile profile = mcMMOPlayer.getProfile();

                        for (PrimarySkillType type : PrimarySkillType.values()) {
                            // Skip child skills as they do not have XP
                            if (SkillTools.isChildSkill(type)) {
                                continue;
                            }

                            int currentLevel = profile.getSkillLevel(type);
                            float currentXP = profile.getSkillXpLevel(type);
                            profile.modifySkill(type, currentLevel + levelsToIncrease);
                            profile.setSkillXpLevel(type, currentXP); // Must re-apply or XP is lost
                        }
                    }
                    // $1000 money
                    else if (clicked.getType() == Material.GOLD_INGOT) {
                        aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 1000);
                        AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    }
                    // Saint I
                    else if (clicked.getType() == Material.PINK_CONCRETE_POWDER) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac rankset saintmonth " + player.getName() + " 1");
                    }
                    // Saint II
                    else if (clicked.getType() == Material.MAGENTA_CONCRETE_POWDER) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac rankset saintmonth " + player.getName() + " 2");
                    }
                    // Saint III
                    else if (clicked.getType() == Material.PURPLE_CONCRETE_POWDER) {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "ac rankset saintmonth " + player.getName() + " 3");
                    }

                    player.sendMessage(ChatUtils.chatMessage("&7You have purchased " + itemName));
                }
            }
        }
    }

    /**
     * Adds the item to the player's inventory or drops it to the floor if there is not enough space.
     *
     * @param player The player.
     * @param item   The item being added.
     */
    private void addOrDropItem(Player player, ItemStack item) {
        Bukkit.getLogger().info("B");
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        // If the player's inventory was full, drop it to the ground
        if (!leftover.isEmpty()) {
            Bukkit.getLogger().info("C");
            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
            player.sendMessage(ChatUtils.chatMessage("&7The item was dropped as you don't have enough space!"));
        }
    }

}
