package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiCrate;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.items.crates.KeyEpic;
import com.aearost.aranarthcore.items.crates.KeyGodly;
import com.aearost.aranarthcore.items.crates.KeyRare;
import com.aearost.aranarthcore.items.crates.KeyVote;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CrateType;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.EventUtils;
import com.gmail.nossr50.util.skills.SkillTools;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
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
                        int emptySlotNum = getEmptySlotNum(player);

                        // Vote Crate
                        if (x == -67 && y == 91 && z == 41) {
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
                                    player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    // Compressible items require up to 2 empty slots
                                    if (emptySlotNum < 2) {
                                        player.sendMessage(ChatUtils.chatMessage("&cYou need at least 2 empty inventory slots to open this crate!"));
                                        player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                        return;
                                    }
                                    determineVoteCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Rare Crate
                        else if (x == -57 && y == 94 && z == 45) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setIsOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> index = new ArrayList<>();
                                // Sets default value to display at first
                                index.add(0);
                                GuiCrate gui = new GuiCrate(player, CrateType.RARE, index);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                index.set(0, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.getIsOpeningCrateWithCyclingItem()) {
                                            gui.updateRareCrateItems(index.get(0));

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
                                    player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    // Compressible items require up to 2 empty slots
                                    if (emptySlotNum < 2) {
                                        player.sendMessage(ChatUtils.chatMessage("&cYou need at least 2 empty inventory slots to open this crate!"));
                                        player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                        return;
                                    }
                                    determineRareCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Epic Crate
                        else if (x == -64 && y == 99 && z == 49) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setIsOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> indexes = new ArrayList<>();
                                // Sets default value to display at first
                                indexes.add(0);
                                indexes.add(0);
                                GuiCrate gui = new GuiCrate(player, CrateType.EPIC, indexes);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                indexes.set(0, 1);
                                indexes.set(1, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.getIsOpeningCrateWithCyclingItem()) {
                                            gui.updateEpicCrateItems(indexes.get(0), indexes.get(1));

                                            // Cycle through the next trim iteration
                                            if (indexes.get(0) < 18) {
                                                indexes.set(0, indexes.get(0) + 1);
                                            } else {
                                                indexes.set(0, 0);
                                            }

                                            // Cycle through the next cluster iteration
                                            if (indexes.get(1) < 7) {
                                                indexes.set(1, indexes.get(1) + 1);
                                            } else {
                                                indexes.set(1, 0);
                                            }
                                        } else {
                                            Bukkit.getScheduler().cancelTask(scheduledSkipTask);
                                        }
                                    }
                                }, 20, 20);
                            }
                            // Attempts to open the crate
                            else {
                                ItemStack epicKey = new KeyEpic().getItem();
                                if (heldItem == null || !heldItem.isSimilar(epicKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding a &3Epic Crate Key &cto do this!"));
                                    player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    // Clusters require up to 4 empty slots
                                    if (emptySlotNum < 4) {
                                        player.sendMessage(ChatUtils.chatMessage("&cYou need at least 4 empty inventory slots to open this crate!"));
                                        player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                        return;
                                    }
                                    determineEpicCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Godly Crate
                        else if (x == -74 && y == 105 && z == 53) {
                            // Previews the contents of the crate
                            if (player.isSneaking()) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setIsOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> index = new ArrayList<>();
                                // Sets default value to display at first
                                index.add(0);
                                GuiCrate gui = new GuiCrate(player, CrateType.GODLY, index);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                index.set(0, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.getIsOpeningCrateWithCyclingItem()) {
                                            gui.updateGodlyCrateItems(index.get(0));

                                            // Cycle through the next iteration
                                            if (index.get(0) < 5) {
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
                                ItemStack godlyKey = new KeyGodly().getItem();
                                if (heldItem == null || !heldItem.isSimilar(godlyKey)) {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou must be holding a &5Godly Crate Key &cto do this!"));
                                    player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                    return;
                                }

                                if (aranarthPlayer.getCrateTypeBeingOpened() == null) {
                                    // Compressible items require up to 2 empty slots
                                    if (emptySlotNum < 2) {
                                        player.sendMessage(ChatUtils.chatMessage("&cYou need at least 2 empty inventory slots to open this crate!"));
                                        player.playSound(block.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1, 0.7F);
                                        return;
                                    }
                                    determineGodlyCrateReward(player);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
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
     * Gets the number of empty inventory slots the player has.
     * @param player The player.
     * @return The number of empty inventory slots the player has.
     */
    private int getEmptySlotNum(Player player) {
        int emptySlotNum = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlotNum++;
            }
        }
        return emptySlotNum;
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
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$50 of In-Game Currency"));
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
                    name = "&l" + reward.getItemMeta().getDisplayName() + " x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
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
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.RARE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$250 of In-Game Currency"));
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
                    reward = getCycledCluster(new Random().nextInt(8));
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
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
            });
        }
    }

    /**
     * Determines which reward the player will get from the Epic Crate.
     * @param player The player opening the Epic Crate.
     */
    private void determineEpicCrateReward(Player player) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.EPIC)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &3Epic Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.EPIC);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7is opening an &3&lEpic Crate"));

            ItemStack heldItem = player.getInventory().getItemInMainHand();
            heldItem.setAmount(heldItem.getAmount() - 1);

            playCrateOpenSound(player, CrateType.EPIC, () -> {
                ItemStack reward = null;
                String name = "";
                int chance = new Random().nextInt(100) + 1;

                if (chance <= 12) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 1500);
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$1500 of In-Game Currency"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.SHULKER_BOX, 1);
                    name = "#956895&lShulker Box x1";
                } else if (chance <= 36) {
                    reward = getCycledArmorTrim(new Random().nextInt(18));
                    String trimName = reward.getType().name().split("_")[0].toLowerCase();
                    trimName = trimName.substring(0, 1).toUpperCase() + trimName.substring(1) + " Armor Trim";
                    if (trimName.startsWith("Ward") || trimName.startsWith("Spire") || trimName.startsWith("Eye") || trimName.startsWith("Vex")) {
                        trimName = "&b&l" + trimName;
                    } else if (trimName.startsWith("Silence")) {
                        trimName = "&d&l" + trimName;
                    } else {
                        trimName = "&e&l" + trimName;
                    }
                    name = trimName + " x1";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.DIAMOND, 32);
                    name = "#a0f0ed&lDiamond x32";
                } else if (chance <= 56) {
                    reward = new ItemStack(Material.TRIDENT, 1);
                    name = "#579b8c&lTrident x1";
                } else if (chance <= 64) {
                    reward = new ItemStack(Material.ELYTRA, 1);
                    name = "#7d7d96&lElytra x1";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 6);
                    name = "#fcd34d&lEnchanted Golden Apple x6";
                } else if (chance <= 80) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
                    PlayerProfile profile = mcMMOPlayer.getProfile();

                    for (PrimarySkillType type : PrimarySkillType.values()) {
                        // Skip child skills as they do not have XP
                        if (SkillTools.isChildSkill(type)) {
                            continue;
                        }

                        int currentLevel = profile.getSkillLevel(type);
                        float currentXP = profile.getSkillXpLevel(type);
                        profile.modifySkill(type, currentLevel + 10);
                        profile.setSkillXpLevel(type, currentXP); // Must re-apply or XP is lost
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7Your mcMMO Skills have each increased by &e10 Levels"));
                    return;
                } else if (chance <= 85) {
                    reward = new KeyEpic().getItem();
                    reward.setAmount(2);
                    name = "&3&lEpic Crate Key x2";
                } else if (chance <= 90) {
                    DiscordUtils.crateItemNotification(player, player.getName() + " has earned a 10% Store Coupon");
                    reward = new ItemStack(Material.PAPER);
                    ItemMeta rewardMeta = reward.getItemMeta();
                    rewardMeta.setMaxStackSize(1);
                    rewardMeta.setDisplayName(ChatUtils.translateToColor("&6&l10% Store Coupon"));
                    List<String> rewardLore = new ArrayList<>();
                    rewardLore.add(ChatUtils.translateToColor("&eContact a Council member to obtain this reward!"));
                    String dayCouponWasAcquired = getCurrentTime();
                    rewardLore.add(ChatUtils.translateToColor("&7Acquired on " + dayCouponWasAcquired));
                    rewardMeta.setLore(rewardLore);
                    reward.setItemMeta(rewardMeta);
                    name = rewardMeta.getDisplayName() + " x1";
                } else if (chance <= 95) {
                    ItemStack cluster1 = getCycledCluster(new Random().nextInt(8));
                    ItemStack cluster2 = getCycledCluster(new Random().nextInt(8));
                    ItemStack cluster3 = getCycledCluster(new Random().nextInt(8));
                    ItemStack cluster4 = getCycledCluster(new Random().nextInt(8));
                    ItemStack[] combined = combineClusters(cluster1, cluster2, cluster3, cluster4);

                    for (int i = 0; i < combined.length; i++) {
                        if (combined[i] != null) {
                            if (i == combined.length - 1) {
                                name += "&7and ";
                            }

                            name += combined[i].getItemMeta().getDisplayName() + " x" + combined[i].getAmount();
                        } else {
                            continue;
                        }

                        if (i < combined.length - 1) {
                            name += ", ";
                        }
                    }
                    for (ItemStack cluster : combined) {
                        if (cluster != null) {
                            player.getInventory().addItem(cluster);
                        }
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                    return;
                } else {
                    reward = new KeyGodly().getItem();
                    name = "&5&lGodly Crate Key x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
            });
        }
    }

    /**
     * Determines which reward the player will get from the Godly Crate.
     * @param player The player opening the Godly Crate.
     */
    private void determineGodlyCrateReward(Player player) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.GODLY)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &5Godly Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.GODLY);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7is opening a &5&lGodly Crate"));

            ItemStack heldItem = player.getInventory().getItemInMainHand();
            heldItem.setAmount(heldItem.getAmount() - 1);

            playCrateOpenSound(player, CrateType.GODLY, () -> {
                ItemStack reward = null;
                String name = "";
                int chance = new Random().nextInt(100) + 1;

                if (chance <= 12) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 7500);
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$7500 of In-Game Currency"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.DIAMOND_BLOCK, 32);
                    name = "#a0f0ed&lDiamond Block x16";
                } else if (chance <= 36) {
                    reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8);
                    name = "#fcd34d&lEnchanted Golden Apple x8";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.NETHERITE_BLOCK, 1);
                    name = "#3a383a&lNetherite Block x1";
                } else if (chance <= 56) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    McMMOPlayer mcMMOPlayer = EventUtils.getMcMMOPlayer(player);
                    PlayerProfile profile = mcMMOPlayer.getProfile();

                    for (PrimarySkillType type : PrimarySkillType.values()) {
                        // Skip child skills as they do not have XP
                        if (SkillTools.isChildSkill(type)) {
                            continue;
                        }

                        int currentLevel = profile.getSkillLevel(type);
                        float currentXP = profile.getSkillXpLevel(type);
                        profile.modifySkill(type, currentLevel + 30);
                        profile.setSkillXpLevel(type, currentXP); // Must re-apply or XP is lost
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.sendMessage(ChatUtils.chatMessage("&7Your mcMMO Skills have each increased by &e30 Levels"));
                    return;
                } else if (chance <= 64) {
                    reward = getCycledAranarthium(new Random().nextInt(6));
                    name = reward.getItemMeta().getDisplayName() + " x1";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.NETHER_STAR, 1);
                    name = "#d8d6fb&lNether Star x1";
                } else if (chance <= 80) {
                    reward = new ItemStack(Material.HEAVY_CORE, 1);
                    name = "#4d5158&lHeavy Core x1";
                } else if (chance <= 85) {
                    reward = new KeyGodly().getItem();
                    reward.setAmount(2);
                    name = "&5&lGodly Crate Key x2";
                } else if (chance <= 90) {
                    DiscordUtils.crateItemNotification(player, player.getName() + " has earned a 30% Store Coupon");
                    reward = new ItemStack(Material.PAPER);
                    ItemMeta rewardMeta = reward.getItemMeta();
                    rewardMeta.setMaxStackSize(1);
                    rewardMeta.setDisplayName(ChatUtils.translateToColor("&6&l30% Store Coupon"));
                    List<String> rewardLore = new ArrayList<>();
                    rewardLore.add(ChatUtils.translateToColor("&eContact a Council member to obtain this reward!"));
                    String dayCouponWasAcquired = getCurrentTime();
                    rewardLore.add(ChatUtils.translateToColor("&7Acquired on " + dayCouponWasAcquired));
                    rewardMeta.setLore(rewardLore);
                    reward.setItemMeta(rewardMeta);
                    name = rewardMeta.getDisplayName() + " x1";
                } else if (chance <= 95) {
                    reward = new KeyEpic().getItem();
                    reward.setAmount(3);
                    name = "&3&lEpic Crate Key x3";
                } else {
                    reward = new AranarthiumIngot().getItem();
                    name = reward.getItemMeta().getDisplayName() + " &f&lx1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                player.getInventory().addItem(reward);
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
            });
        }
    }

    /**
     * Provides the current time as a formatted String.
     * @return The current time.
     */
    private String getCurrentTime() {
        LocalDateTime ldt = LocalDateTime.now();
        String month = ldt.getMonthValue() < 10 ? "0" + ldt.getMonthValue() : ldt.getMonthValue() + "";
        String day = ldt.getDayOfMonth() < 10 ? "0" + ldt.getDayOfMonth() : ldt.getDayOfMonth() + "";
        String year = ldt.getYear() < 10 ? "0" + ldt.getYear() : ldt.getYear() + "";
        String hour = ldt.getHour() < 10 ? "0" + ldt.getHour() : ldt.getHour() + "";
        String minute = ldt.getMinute() < 10 ? "0" + ldt.getMinute() : ldt.getMinute() + "";
        String second = ldt.getSecond() < 10 ? "0" + ldt.getSecond() : ldt.getSecond() + "";
        return month + "/" + day + "/" + year + " " + hour + ":" + minute + ":" + second;
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
     * Provides the cluster that is associated to the input index.
     * @param index The index of the cluster.
     * @return The cluster.
     */
    private ItemStack getCycledCluster(int index) {
        ItemStack cluster = null;
        switch (index) {
            case 1 -> cluster = new IronCluster().getItem();
            case 2 -> cluster = new GoldCluster().getItem();
            case 3 -> cluster = new QuartzCluster().getItem();
            case 4 -> cluster = new LapisCluster().getItem();
            case 5 -> cluster = new RedstoneCluster().getItem();
            case 6 -> cluster = new DiamondCluster().getItem();
            case 7 -> cluster = new EmeraldCluster().getItem();
            default -> cluster = new CopperCluster().getItem();
        }
        return cluster;
    }

    /**
     * Provides the armor trim that is associated to the input index.
     * @param index The index of the armor trim.
     * @return The armor trim.
     */
    private ItemStack getCycledArmorTrim(int index) {
        ItemStack trim = null;
        switch (index) {
            case 1 -> trim = new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 2 -> trim = new ItemStack(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 3 -> trim = new ItemStack(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 4 -> trim = new ItemStack(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 5 -> trim = new ItemStack(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 6 -> trim = new ItemStack(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 7 -> trim = new ItemStack(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 8 -> trim = new ItemStack(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 9 -> trim = new ItemStack(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 10 -> trim = new ItemStack(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 11 -> trim = new ItemStack(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 12 -> trim = new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 13 -> trim = new ItemStack(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 14 -> trim = new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 15 -> trim = new ItemStack(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 16 -> trim = new ItemStack(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
            case 17 -> trim = new ItemStack(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
            default -> trim = new ItemStack(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
        }
        return trim;
    }

    /**
     * Provides the enhanced aranarthium ingot that is associated to the input index.
     * @param index The index of the enhanced aranarthium ingot.
     * @return The enhanced aranarthium ingot.
     */
    private ItemStack getCycledAranarthium(int index) {
        ItemStack ingot = null;
        switch (index) {
            case 1 -> ingot = new AranarthiumAquatic().getItem();
            case 2 -> ingot = new AranarthiumArdent().getItem();
            case 3 -> ingot = new AranarthiumDwarven().getItem();
            case 4 -> ingot = new AranarthiumElven().getItem();
            case 5 -> ingot = new AranarthiumScorched().getItem();
            default -> ingot = new AranarthiumSoulbound().getItem();
        }
        return ingot;
    }

    /**
     * Combines the 4 clusters if they are for the same type.
     * @param cluster1 The first cluster.
     * @param cluster2 The second cluster.
     * @param cluster3 The third cluster.
     * @param cluster4 The fourth cluster.
     * @return The combined clusters.
     */
    private ItemStack[] combineClusters(ItemStack cluster1, ItemStack cluster2, ItemStack cluster3, ItemStack cluster4) {
        ItemStack[] combined = new ItemStack[4];

        // Remove cluster 1
        if (cluster1.isSimilar(cluster2)) {
            cluster2.setAmount(cluster2.getAmount() + 1);
            cluster1 = null;
        } else if (cluster1.isSimilar(cluster3)) {
            cluster3.setAmount(cluster3.getAmount() + 1);
            cluster1 = null;
        } else if (cluster1.isSimilar(cluster4)) {
            cluster4.setAmount(cluster4.getAmount() + 1);
            cluster1 = null;
        }
        combined[0] = cluster1;

        // Remove cluster 2
        if (cluster2.isSimilar(cluster3)) {
            cluster3.setAmount(cluster3.getAmount() + 1);
            cluster2 = null;
        } else if (cluster2.isSimilar(cluster4)) {
            cluster4.setAmount(cluster4.getAmount() + 1);
            cluster2 = null;
        }
        combined[1] = cluster2;

        // Remove cluster 3
        if (cluster3.isSimilar(cluster4)) {
            cluster4.setAmount(cluster4.getAmount() + 1);
            cluster3 = null;
        }
        combined[2] = cluster3;

        // Nothing to remove with cluster 4
        combined[3] = cluster4;

        for (ItemStack is : combined) {
            if (is != null) {
                Bukkit.getLogger().info(is.getItemMeta().getDisplayName() + " x" + is.getAmount());
            } else {
                Bukkit.getLogger().info("NULL");
            }
        }

        return combined;
    }
}
