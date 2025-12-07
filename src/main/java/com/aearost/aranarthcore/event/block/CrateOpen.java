package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiCrate;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.crates.KeyEpic;
import com.aearost.aranarthcore.items.crates.KeyGodly;
import com.aearost.aranarthcore.items.crates.KeyRare;
import com.aearost.aranarthcore.items.crates.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CrateType;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles logic of opening a crate.
 */
public class CrateOpen {

    private int scheduledSkipTask = -1;

    public void execute(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        Player player = e.getPlayer();

        if (e.getHand() == EquipmentSlot.HAND) {
            if (block != null) {
                if (AranarthUtils.isSpawnLocation(block.getLocation())) {
                    if (block.getType() == Material.CHEST) {
                        e.setCancelled(true);
                        ItemStack heldItem = player.getInventory().getItemInMainHand();
                        int x = block.getX();
                        int y = block.getY();
                        int z = block.getZ();
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

                        // Vote Crate
                        if (x == 104 && y == 65 && z == -204) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                GuiCrate gui = new GuiCrate(player, CrateType.VOTE, null);
                                gui.openGui();
                            }
                            // Attempts to open the crate
                            else {
                                ItemStack voteKey = new KeyVote().getItem();
                                if (heldItem == null || !heldItem.isSimilar(voteKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding a &aVote Crate Key &cto do this!"));
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    determineVoteCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Rare Crate
                        else if (x == 104 && y == 65 && z == -202) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                aranarthPlayer.setIsOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> index = new ArrayList<>();
                                index.add(0);
                                GuiCrate gui = new GuiCrate(player, CrateType.RARE, index);
                                gui.openGui();
                                index.set(0, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.getIsOpeningCrateWithCyclingItem()) {
                                            gui.updateRareCrateClusterItem(index.get(0));

                                            // Cycle through the next iteration
                                            if (index.get(0) < 7) {
                                                index.set(0, index.get(0) + 1);
                                            } else {
                                                index.set(0, 0);
                                            }
                                        } else {
                                            Bukkit.getScheduler().cancelTask(scheduledSkipTask);
                                        }
                                    }
                                }, 20, 20);
                            }
                            // Attempts to open the crate
                            else {
                                ItemStack rareKey = new KeyRare().getItem();
                                if (heldItem == null || !heldItem.isSimilar(rareKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding a &6Rare Crate Key &cto do this!"));
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    determineRareCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Epic Crate
                        else if (x == 104 && y == 65 && z == -200) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                GuiCrate gui = new GuiCrate(player, CrateType.EPIC, null);
                                gui.openGui();
                            }
                            // Attempts to open the crate
                            else {
                                ItemStack epicKey = new KeyEpic().getItem();
                                if (heldItem == null || !heldItem.isSimilar(epicKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding an &3Epic Crate Key &cto do this!"));
                                    return;
                                }
                            }
                        }
                        // Godly Crate
                        else if (x == 104 && y == 65 && z == -198) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                GuiCrate gui = new GuiCrate(player, CrateType.GODLY, null);
                                gui.openGui();
                            }
                            // Attempts to open the crate
                            else {
                                ItemStack godlyKey = new KeyGodly().getItem();
                                if (heldItem == null || !heldItem.isSimilar(godlyKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding a &5Godly Crate Key &cto do this!"));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Plays a sound effect when a player opens the crate.
     * @param player The player that is opening the crate.
     * @param type The type of crate that is being opened.
     * @param onFinish The callback allowing the item to be provided with a delay.
     */
    private void playCrateOpenSound(Player player, CrateType type, Runnable onFinish) {
        AranarthUtils.addCrateInUse(type);
        Sound sound = Sound.BLOCK_NOTE_BLOCK_BELL;
        new BukkitRunnable() {
            int runs = 0;
            @Override
            public void run() {
                float pitch = 1F;

                switch (runs) {
                    case 0 -> pitch = 1F;
                    case 1 -> pitch = 0.75F;
                    case 2 -> pitch = 1F;
                    case 3 -> pitch = 0.75F;
                    case 4 -> pitch = 1.1F;
                    case 5 -> pitch = 0.95F;
                    case 6 -> pitch = 1.1F;
                    case 7 -> pitch = 0.95F;
                    case 8 -> pitch = 1.5F;
                    case 9 -> pitch = 1.25F;
                    case 10 -> pitch = 1F;
                    case 11 -> pitch = 1.1F;
                    case 12 -> pitch = 1.25F;
                    case 13 -> pitch = 1.5F;
                    default -> {
                        pitch = 0;
                    }
                }

                // No sound
                if (pitch != 0) {
                    player.playSound(player.getLocation(), sound, 1F, pitch);
                }

                // End after a short delay
                if (runs == 16) {
                    cancel();

                    // finish callback
                    Bukkit.getScheduler().runTask(AranarthCore.getInstance(), onFinish);
                    return;
                }
                runs++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0, 4); // Runs every 5 ticks
    }

    /**
     * Determines which reward the player will get from the Vote Crate.
     * @param player The player opening the Vote Crate.
     */
    private void determineVoteCrateReward(Player player) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.VOTE)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &aVote Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.VOTE);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            ItemStack heldItem = player.getInventory().getItemInMainHand();
            heldItem.setAmount(heldItem.getAmount() - 1);

            playCrateOpenSound(player, CrateType.VOTE, () -> {
                ItemStack reward = null;
                String name = "";
                int chance = new Random().nextInt(100) + 1;

                if (chance <= 12) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 50);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$50 In-Game Money &7from the &aVote Crate"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.BREAD, 16);
                    name = "#ba8727&lBread x16";
                } else if (chance <= 36) {
                    reward = new ItemStack(Material.IRON_INGOT, 16);
                    name = "#eeeeee&lIron Ingot x16";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.GOLD_INGOT, 16);
                    name = "#fcd34d&lGold Ingot x16";
                } else if (chance <= 56) {
                    reward = new GodAppleFragment().getItem();
                    reward.setAmount(4);
                    name = "&6&lGod Apple Fragment x4";
                } else if (chance <= 64) {
                    reward = new ItemStack(Material.EMERALD, 8);
                    name = "#50c878&lEmerald x8";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.DIAMOND, 4);
                    name = "#a0f0ed&lDiamond x4";
                } else if (chance <= 80) {
                    reward = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
                    name = "#c1e377&lBottle o' Enchanting x16";
                } else if (chance <= 85) {
                    reward = new ItemStack(Material.TRIAL_KEY, 1);
                    name = "#515950&lTrial Key x1";
                } else if (chance <= 90) {
                    reward = new ItemStack(Material.BLAZE_ROD, 8);
                    name = "#fcbf00&lBlaze Rod x8";
                } else if (chance <= 95) {
                    reward = new ItemStack(Material.BREEZE_ROD, 8);
                    name = "#bdadc7&lBreeze Rod x8";
                } else {
                    reward = new KeyRare().getItem();
                    name = "&6&lRare Crate Key x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name + " &7from the &aVote Crate"));
            });
        }
    }

    /**
     * Determines which reward the player will get from the Rare Crate.
     * @param player The player opening the Rare Crate.
     */
    private void determineRareCrateReward(Player player) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.RARE)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &6Rare Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.RARE);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            ItemStack heldItem = player.getInventory().getItemInMainHand();
            heldItem.setAmount(heldItem.getAmount() - 1);

            playCrateOpenSound(player, CrateType.RARE, () -> {
                ItemStack reward = null;
                String name = "";
                int chance = new Random().nextInt(100) + 1;

                if (chance <= 12) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 250);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$250 In-Game Money &7from the &6Rare Crate"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.ENCHANTED_BOOK, 1);
                    EnchantmentStorageMeta mendingMeta = (EnchantmentStorageMeta) reward.getItemMeta();
                    mendingMeta.addStoredEnchant(Enchantment.MENDING, 1, true);
                    reward.setItemMeta(mendingMeta);
                    name = "#9f1c43&lMending Book x1";
                } else if (chance <= 36) {
                    reward = new ItemStack(Material.GOLDEN_CARROT, 32);
                    name = "#fcd34d&lGolden Carrot x32";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.DIAMOND, 16);
                    name = "#a0f0ed&lDiamond x16";
                } else if (chance <= 56) {
                    reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2);
                    name = "#fcd34d&lEnchanted Golden Apple x2";
                } else if (chance <= 64) {
                    reward = new ItemStack(Material.OMINOUS_TRIAL_KEY, 1);
                    name = "#515950&lOminous Trial Key x1";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.NETHERITE_INGOT, 1);
                    name = "#3a383a&lNetherite Ingot x1";
                } else if (chance <= 80) {
                    reward = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
                    name = "#f5eba3&lTotem of Undying x1";
                } else if (chance <= 85) {
                    reward = new ItemStack(Material.DRIED_GHAST, 1);
                    name = "#9b8d8d&lDried Ghast x1";
                } else if (chance <= 90) {
                    reward = new ItemStack(Material.SNIFFER_EGG, 1);
                    name = "#4e9c70&lSniffer Egg x1";
                } else if (chance <= 95) {
                    int random = new Random().nextInt(8);
                    switch (random) {
                        case 1 -> reward = new IronCluster().getItem();
                        case 2 -> reward = new GoldCluster().getItem();
                        case 3 -> reward = new QuartzCluster().getItem();
                        case 4 -> reward = new LapisCluster().getItem();
                        case 5 -> reward = new RedstoneCluster().getItem();
                        case 6 -> reward = new DiamondCluster().getItem();
                        case 7 -> reward = new EmeraldCluster().getItem();
                        default -> reward = new CopperCluster().getItem();
                    }
                    name = reward.getItemMeta().getDisplayName() + " x1";
                } else {
                    reward = new KeyEpic().getItem();
                    name = "&3&lEpic Crate Key x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.RARE);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name + " &7from the &6Rare Crate"));
            });
        }
    }

    /**
     * Provides the name of the crate type that the player is currently opening.
     * @param aranarthPlayer The player that is already opening a crate.
     * @return The name of the crate type that the player is currently opening.
     */
    private String getCrateTypeBeingOpenedName(AranarthPlayer aranarthPlayer) {
        CrateType type = aranarthPlayer.getCrateTypeBeingOpened();
        if (type == CrateType.RARE) {
            return "&6Rare Crate";
        } else if (type == CrateType.EPIC) {
            return "&3Epic Crate";
        } else if (type == CrateType.GODLY) {
            return "&5Godly Crate";
        } else {
            return "&aVote Crate";
        }
    }

    /**
     * Provides the Cluster that is associated to the input index.
     * @param index The index of the cluster.
     * @return The cluster.
     */
    private ItemStack getCycledCluster(int index) {
        ItemStack cluster = null;
        switch (index) {
            case 1: cluster = new IronCluster().getItem();
            case 2: cluster = new GoldCluster().getItem();
            case 3: cluster = new QuartzCluster().getItem();
            case 4: cluster = new LapisCluster().getItem();
            case 5: cluster = new RedstoneCluster().getItem();
            case 6: cluster = new DiamondCluster().getItem();
            case 7: cluster = new EmeraldCluster().getItem();
            default: cluster = new CopperCluster().getItem();
        }
        return cluster;
    }
}
