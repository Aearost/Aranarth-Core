package com.aearost.aranarthcore.event.block;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.gui.GuiCrate;
import com.aearost.aranarthcore.items.GodAppleFragment;
import com.aearost.aranarthcore.items.HoneyGlazedHam;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.aranarthium.ingots.*;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.items.incantation.IncantationBeheading;
import com.aearost.aranarthcore.items.incantation.IncantationLifesteal;
import com.aearost.aranarthcore.items.incantation.IncantationMagnetism;
import com.aearost.aranarthcore.items.incantation.IncantationPlentiful;
import com.aearost.aranarthcore.items.incantation.IncantationPreservation;
import com.aearost.aranarthcore.items.incantation.IncantationResilience;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.items.key.KeyVote;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.AranarthVote;
import com.aearost.aranarthcore.objects.CrateType;
import com.aearost.aranarthcore.utils.*;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.EventUtils;
import com.gmail.nossr50.util.skills.SkillTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.time.LocalDateTime;
import java.util.*;

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
                if (block.getWorld().getName().equals("spawn")) {
                    if (block.getType() == Material.CHEST) {
                        e.setCancelled(true);
                        ItemStack heldItem = player.getInventory().getItemInMainHand();
                        int x = block.getX();
                        int y = block.getY();
                        int z = block.getZ();
                        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
                        int emptySlotNum = getEmptySlotNum(player);

                        // Vote Crate
                        if (x == -71 && y == 110 && z == -5) {
                            // Previews the contents of the crate
                            if (AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> indexes = new ArrayList<>();
                                indexes.add(0); // blaze rod and breeze rod
                                indexes.add(0); // brew recipes
                                GuiCrate gui = new GuiCrate(player, CrateType.VOTE, indexes);
                                gui.openGui();
                                indexes.set(0, 1);
                                indexes.set(1, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.isOpeningCrateWithCyclingItem()) {
                                            gui.updateVoteCrateItems(indexes.get(0), indexes.get(1));

                                            // Alternate between blaze rod (0) and breeze rod (1)
                                            indexes.set(0, indexes.get(0) == 0 ? 1 : 0);

                                            // Cycle through rare recipe maps
                                            int nextRecipe = indexes.get(1) + 1;
                                            if (nextRecipe >= BrewRecipeUtils.getRareRecipeCount()) {
                                                nextRecipe = 0;
                                            }
                                            indexes.set(1, nextRecipe);
                                        } else {
                                            Bukkit.getScheduler().cancelTask(scheduledSkipTask);
                                        }
                                    }
                                }, 20, 20);
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
                                    determineVoteCrateReward(player, block);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Rare Crate
                        else if (x == -81 && y == 112 && z == -11) {
                            // Previews the contents of the crate
                            if (AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> indexes = new ArrayList<>();
                                // Sets default value to display at first
                                indexes.add(0);
                                indexes.add(0);
                                GuiCrate gui = new GuiCrate(player, CrateType.RARE, indexes);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                indexes.set(0, 1);
                                indexes.set(1, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.isOpeningCrateWithCyclingItem()) {
                                            gui.updateRareCrateItems(indexes.get(0), indexes.get(1));

                                            // Cycle through the next trim iteration
                                            if (indexes.get(0) < 17) {
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
                                    determineRareCrateReward(player, block);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Epic Crate
                        else if (x == -69 && y == 112 && z == -18) {
                            // Previews the contents of the crate
                            if (AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> indexes = new ArrayList<>();
                                // Sets default value to display at first
                                indexes.add(0); // egg index
                                indexes.add(0); // cluster index
                                indexes.add(0); // incantation index
                                indexes.add(0); // weapon index
                                indexes.add(0); // ham/sniffer/shulker index
                                GuiCrate gui = new GuiCrate(player, CrateType.EPIC, indexes);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                indexes.set(0, 1);
                                indexes.set(1, 1);
                                indexes.set(2, 1);
                                indexes.set(3, 1);
                                indexes.set(4, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.isOpeningCrateWithCyclingItem()) {
                                            gui.updateEpicCrateItems(indexes.get(0), indexes.get(1), indexes.get(2), indexes.get(3), indexes.get(4));

                                            // Cycle through the next spawn egg iteration
                                            if (indexes.get(0) < 3) {
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

                                            // Cycle through the next incantation iteration
                                            if (indexes.get(2) < 1) {
                                                indexes.set(2, indexes.get(2) + 1);
                                            } else {
                                                indexes.set(2, 0);
                                            }

                                            // Cycle through the next weapon iteration (trident, elytra, conduit, heavy core)
                                            if (indexes.get(3) < 3) {
                                                indexes.set(3, indexes.get(3) + 1);
                                            } else {
                                                indexes.set(3, 0);
                                            }

                                            // Cycle through the next ham/sniffer/shulker iteration
                                            if (indexes.get(4) < 2) {
                                                indexes.set(4, indexes.get(4) + 1);
                                            } else {
                                                indexes.set(4, 0);
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
                                    determineEpicCrateReward(player, block);
                                } else {
                                    player.sendMessage(ChatUtils.chatMessage("&cYou are already opening the " + getCrateTypeBeingOpenedName(aranarthPlayer)));
                                    return;
                                }
                            }
                        }
                        // Godly Crate
                        else if (x == -81 && y == 115 && z == -26) {
                            // Previews the contents of the crate
                            if (AranarthUtils.isPhysicallySneaking(player.getUniqueId())) {
                                player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0.6F);
                                aranarthPlayer.setOpeningCrateWithCyclingItem(true);
                                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                                List<Integer> indexes = new ArrayList<>();
                                // Sets default value to display at first
                                indexes.add(0);
                                indexes.add(0);
                                indexes.add(0);
                                indexes.add(0); // incantation index (Resilience, Preservation, Plentiful)
                                GuiCrate gui = new GuiCrate(player, CrateType.GODLY, indexes);
                                gui.openGui();
                                // Updates to next slot so task can update it accordingly
                                indexes.set(0, 1);
                                indexes.set(1, 1);
                                indexes.set(2, 1);
                                indexes.set(3, 1);

                                scheduledSkipTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AranarthCore.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aranarthPlayer.isOpeningCrateWithCyclingItem()) {
                                            gui.updateGodlyCrateItems(indexes.get(0), indexes.get(1), indexes.get(2), indexes.get(3));

                                            // Cycle through the next enhanced aranarthium iteration
                                            if (indexes.get(0) < 5) {
                                                indexes.set(0, indexes.get(0) + 1);
                                            } else {
                                                indexes.set(0, 0);
                                            }

                                            // Cycle through the next spawn egg iteration
                                            if (indexes.get(1) < 2) {
                                                indexes.set(1, indexes.get(1) + 1);
                                            } else {
                                                indexes.set(1, 0);
                                            }

                                            // Cycle through the next diamond block / shulker shells iteration
                                            if (indexes.get(2) < 1) {
                                                indexes.set(2, indexes.get(2) + 1);
                                            } else {
                                                indexes.set(2, 0);
                                            }

                                            // Cycle through incantations (Resilience, Preservation, Plentiful)
                                            if (indexes.get(3) < 2) {
                                                indexes.set(3, indexes.get(3) + 1);
                                            } else {
                                                indexes.set(3, 0);
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
                                    determineGodlyCrateReward(player, block);
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
     *
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
     *
     * @param player   The player that is opening the crate.
     * @param type     The type of crate that is being opened.
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
                int crateVol = AranarthUtils.getPlayer(player.getUniqueId()).getCrateSoundVolume();
                if (pitch != 0 && crateVol > 0) {
                    player.playSound(player, sound, crateVol / 100f, pitch);
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
     * Pre-rolls all random values before the animation starts so the winner is
     * known at sequence-build time and the animation displays the correct item.
     *
     * @param player     The player opening the Vote Crate.
     * @param crateBlock The chest block of the crate.
     */
    private void determineVoteCrateReward(Player player, Block crateBlock) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.VOTE)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &aVote Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.VOTE);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            // Pre-roll all random values before animation starts
            final int chance = new Random().nextInt(100) + 1;
            final boolean isBlazeRod = new Random().nextBoolean();
            final BrewRecipe lockedRecipe = BrewRecipeUtils.getRandomLockedRare(player.getUniqueId());

            // Determine the display item shown in the animation as the winner
            final ItemStack displayItem;
            if (chance <= 12) {
                displayItem = new ItemStack(Material.GOLD_INGOT);
            } else if (chance <= 24) {
                displayItem = new ItemStack(Material.BREAD, 16);
            } else if (chance <= 36) {
                displayItem = new ItemStack(Material.IRON_INGOT, 16);
            } else if (chance <= 48) {
                displayItem = new ItemStack(Material.GOLD_INGOT, 16);
            } else if (chance <= 56) {
                ItemStack fragment = new GodAppleFragment().getItem();
                fragment.setAmount(4);
                displayItem = fragment;
            } else if (chance <= 64) {
                displayItem = new ItemStack(Material.EMERALD, 8);
            } else if (chance <= 72) {
                displayItem = new ItemStack(Material.DIAMOND, 4);
            } else if (chance <= 80) {
                displayItem = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
            } else if (chance <= 85) {
                displayItem = new ItemStack(Material.PAPER);
            } else if (chance <= 90) {
                displayItem = new ItemStack(isBlazeRod ? Material.BLAZE_ROD : Material.BREEZE_ROD, 8);
            } else if (chance <= 95) {
                displayItem = lockedRecipe != null ? BrewRecipeUtils.createRecipeMapItem(lockedRecipe) : new ItemStack(Material.BREEZE_ROD, 8);
            } else {
                displayItem = new KeyRare().getItem();
            }

            List<ItemStack> sequence = buildAnimationSequence(CrateType.VOTE, displayItem);
            startCrateAnimation(crateBlock, sequence, () -> {
                if (!player.isOnline()) {
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    AranarthUtils.addPendingVoteKeys(player.getUniqueId(), 1);
                    return;
                }

                ItemStack reward = null;
                String name = "";

                if (chance <= 12) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 500);
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), 500);
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&6$500 of In-Game Currency", "&aVote Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$500 of In-Game Currency"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled $500 of In-Game Currency in a Vote Crate"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.BREAD, 16);
                    name = "&#ba8727&lBread x16";
                } else if (chance <= 36) {
                    reward = new ItemStack(Material.IRON_INGOT, 16);
                    name = "&#eeeeee&lIron Ingot x16";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.GOLD_INGOT, 16);
                    name = "&#fcd34d&lGold Ingot x16";
                } else if (chance <= 56) {
                    reward = new GodAppleFragment().getItem();
                    reward.setAmount(4);
                    name = "&6&lGod Apple Fragment x4";
                } else if (chance <= 64) {
                    reward = new ItemStack(Material.EMERALD, 8);
                    name = "&#50c878&lEmerald x8";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.DIAMOND, 4);
                    name = "&#a0f0ed&lDiamond x4";
                } else if (chance <= 80) {
                    reward = new ItemStack(Material.EXPERIENCE_BOTTLE, 16);
                    name = "&#c1e377&lBottle o' Enchanting x16";
                } else if (chance <= 85) {
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    AranarthUtils.addVote(new AranarthVote(player.getUniqueId(), 10, System.currentTimeMillis()));
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&aVote Points +10", "&aVote Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &aVote Points +10"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled Vote Points +10 in a Vote Crate"));
                    return;
                } else if (chance <= 90) {
                    // 50/50 blaze rod or breeze rod - use pre-rolled value
                    if (isBlazeRod) {
                        reward = new ItemStack(Material.BLAZE_ROD, 8);
                        name = "&#fcbf00&lBlaze Rod x8";
                    } else {
                        reward = new ItemStack(Material.BREEZE_ROD, 8);
                        name = "&#bdadc7&lBreeze Rod x8";
                    }
                } else if (chance <= 95) {
                    // Random locked rare-tier brew recipe map; fallback to breeze rod if all unlocked - use pre-rolled value
                    if (lockedRecipe != null) {
                        reward = BrewRecipeUtils.createRecipeMapItem(lockedRecipe);
                        name = "&6&l[Recipe] " + lockedRecipe.getDisplayName();
                    } else {
                        reward = new ItemStack(Material.BREEZE_ROD, 8);
                        name = "&#bdadc7&lBreeze Rod x8";
                    }
                } else {
                    reward = new KeyRare().getItem();
                    name = "&l" + reward.getItemMeta().getDisplayName() + " x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.VOTE);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(reward);
                if (!remainder.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage("&7The reward was dropped as you didn't have enough space!"));
                    for (ItemStack remain : remainder.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remain);
                    }
                }
                broadcastRewardToNearbyPlayers(player, aranarthPlayer, name, "&aVote Crate");
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled " + name + " in a Vote Crate"));
            });
            playCrateOpenSound(player, CrateType.VOTE, () -> {});
        }
    }

    /**
     * Determines which reward the player will get from the Rare Crate.
     * Pre-rolls all random values before the animation starts so the winner is
     * known at sequence-build time and the animation displays the correct item.
     *
     * @param player     The player opening the Rare Crate.
     * @param crateBlock The chest block of the crate.
     */
    private void determineRareCrateReward(Player player, Block crateBlock) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.RARE)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &6Rare Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.RARE);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);

            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            // Pre-roll all random values before animation starts
            final int chance = new Random().nextInt(100) + 1;
            final int trimRoll = new Random().nextInt(18);
            final int clusterRoll = new Random().nextInt(8);

            // Determine the display item shown in the animation as the winner
            final ItemStack displayItem;
            if (chance <= 12) {
                displayItem = new ItemStack(Material.GOLD_INGOT);
            } else if (chance <= 24) {
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
                EnchantmentStorageMeta mendingMeta = (EnchantmentStorageMeta) book.getItemMeta();
                mendingMeta.addStoredEnchant(Enchantment.MENDING, 1, true);
                book.setItemMeta(mendingMeta);
                displayItem = book;
            } else if (chance <= 36) {
                displayItem = new HoneyGlazedHam().getItem();
            } else if (chance <= 48) {
                displayItem = new ItemStack(Material.DIAMOND, 16);
            } else if (chance <= 56) {
                displayItem = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4);
            } else if (chance <= 64) {
                displayItem = new ItemStack(Material.OMINOUS_TRIAL_KEY);
            } else if (chance <= 72) {
                displayItem = getCycledArmorTrim(trimRoll);
            } else if (chance <= 80) {
                displayItem = new ItemStack(Material.TOTEM_OF_UNDYING);
            } else if (chance <= 85) {
                displayItem = new IncantationBeheading().getItem();
            } else if (chance <= 90) {
                displayItem = new ItemStack(Material.NETHERITE_INGOT, 2);
            } else if (chance <= 95) {
                displayItem = getCycledCluster(clusterRoll);
            } else {
                displayItem = new KeyEpic().getItem();
            }

            List<ItemStack> sequence = buildAnimationSequence(CrateType.RARE, displayItem);
            startCrateAnimation(crateBlock, sequence, () -> {
                if (!player.isOnline()) {
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.RARE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    AranarthUtils.addPendingRareKeys(player.getUniqueId(), 1);
                    return;
                }

                ItemStack reward = null;
                String name = "";

                if (chance <= 12) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 10000);
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), 10000);
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.RARE);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&6$10,000 of In-Game Currency", "&6Rare Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$10,000 of In-Game Currency"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled $10,000 of In-Game Currency in a Rare Crate"));
                    return;
                } else if (chance <= 24) {
                    reward = new ItemStack(Material.ENCHANTED_BOOK, 1);
                    EnchantmentStorageMeta mendingMeta = (EnchantmentStorageMeta) reward.getItemMeta();
                    mendingMeta.addStoredEnchant(Enchantment.MENDING, 1, true);
                    reward.setItemMeta(mendingMeta);
                    name = "&#9f1c43&lMending Book x1";
                } else if (chance <= 36) {
                    reward = new HoneyGlazedHam().getItem();
                    reward.setAmount(32);
                    name = "&6&lHoney Glazed Ham x32";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.DIAMOND, 16);
                    name = "&#a0f0ed&lDiamond x16";
                } else if (chance <= 56) {
                    reward = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4);
                    name = "&#fcd34d&lEnchanted Golden Apple x4";
                } else if (chance <= 64) {
                    reward = new ItemStack(Material.OMINOUS_TRIAL_KEY, 1);
                    name = "&#515950&lOminous Trial Key x1";
                } else if (chance <= 72) {
                    // Use pre-rolled trimRoll
                    reward = getCycledArmorTrim(trimRoll);
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
                } else if (chance <= 80) {
                    reward = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
                    name = "&#f5eba3&lTotem of Undying x1";
                } else if (chance <= 85) {
                    reward = new IncantationBeheading().getItem();
                    name = reward.getItemMeta().getDisplayName() + " x1";
                } else if (chance <= 90) {
                    reward = new ItemStack(Material.NETHERITE_INGOT, 2);
                    name = "&#3a383a&lNetherite Ingot x2";
                } else if (chance <= 95) {
                    // Use pre-rolled clusterRoll
                    reward = getCycledCluster(clusterRoll);
                    name = reward.getItemMeta().getDisplayName() + " x1";
                } else {
                    reward = new KeyEpic().getItem();
                    name = "&3&lEpic Crate Key x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.RARE);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(reward);
                if (!remainder.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage("&7The reward was dropped as you didn't have enough space!"));
                    for (ItemStack remain : remainder.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remain);
                    }
                }
                broadcastRewardToNearbyPlayers(player, aranarthPlayer, name, "&6Rare Crate");
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled " + name + " in a Rare Crate"));
            });
            playCrateOpenSound(player, CrateType.RARE, () -> {});
        }
    }

    /**
     * Determines which reward the player will get from the Epic Crate.
     * Pre-rolls all random values before the animation starts so the winner is
     * known at sequence-build time and the animation displays the correct item.
     *
     * @param player     The player opening the Epic Crate.
     * @param crateBlock The chest block of the crate.
     */
    private void determineEpicCrateReward(Player player, Block crateBlock) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.EPIC)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &3Epic Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.EPIC);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7is opening an &3&lEpic Crate"));

            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            // Pre-roll all random values before animation starts
            final int chance = new Random().nextInt(100) + 1;
            final int hamRoll = new Random().nextInt(3);
            final int weaponRoll = new Random().nextInt(4);
            final int eggRoll = new Random().nextInt(4);
            final int[] clusterRolls = {
                new Random().nextInt(8),
                new Random().nextInt(8),
                new Random().nextInt(8),
                new Random().nextInt(8)
            };
            final boolean isMagnetism = new Random().nextInt(2) == 0;

            // Determine the display item shown in the animation as the winner
            final ItemStack displayItem;
            if (chance <= 12) {
                displayItem = new ItemStack(Material.GOLD_INGOT);
            } else if (chance <= 24) {
                if (hamRoll == 0) {
                    displayItem = new HoneyGlazedHam().getItem();
                } else if (hamRoll == 1) {
                    displayItem = new ItemStack(Material.SNIFFER_EGG);
                } else {
                    displayItem = new ItemStack(Material.SHULKER_SHELL, 8);
                }
            } else if (chance <= 36) {
                displayItem = new ItemStack(Material.NETHERITE_INGOT, 4);
            } else if (chance <= 48) {
                displayItem = new ItemStack(Material.DIAMOND, 64);
            } else if (chance <= 56) {
                switch (weaponRoll) {
                    case 0 -> displayItem = new ItemStack(Material.TRIDENT);
                    case 1 -> displayItem = new ItemStack(Material.ELYTRA);
                    case 2 -> displayItem = new ItemStack(Material.CONDUIT);
                    default -> displayItem = new ItemStack(Material.HEAVY_CORE);
                }
            } else if (chance <= 64) {
                displayItem = getCycledEpicSpawnEgg(eggRoll);
            } else if (chance <= 72) {
                displayItem = getCycledCluster(clusterRolls[0]);
            } else if (chance <= 80) {
                displayItem = new ItemStack(Material.ENCHANTED_BOOK);
            } else if (chance <= 85) {
                ItemStack key = new KeyRare().getItem();
                key.setAmount(3);
                displayItem = key;
            } else if (chance <= 90) {
                ItemStack key = new KeyEpic().getItem();
                key.setAmount(2);
                displayItem = key;
            } else if (chance <= 95) {
                displayItem = isMagnetism ? new IncantationMagnetism().getItem() : new IncantationLifesteal().getItem();
            } else {
                displayItem = new KeyGodly().getItem();
            }

            List<ItemStack> sequence = buildAnimationSequence(CrateType.EPIC, displayItem);
            startCrateAnimation(crateBlock, sequence, () -> {
                if (!player.isOnline()) {
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    AranarthUtils.addPendingEpicKeys(player.getUniqueId(), 1);
                    return;
                }

                ItemStack reward = null;
                String name = "";

                if (chance <= 12) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 25000);
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), 25000);
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&6$25,000 of In-Game Currency", "&3Epic Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$25,000 of In-Game Currency"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled $25,000 of In-Game Currency in an Epic Crate"));
                    return;
                } else if (chance <= 24) {
                    // Use pre-rolled hamRoll
                    if (hamRoll == 0) {
                        reward = new HoneyGlazedHam().getItem();
                        reward.setAmount(64);
                        name = "&6&lHoney Glazed Ham x64";
                    } else if (hamRoll == 1) {
                        reward = new ItemStack(Material.SNIFFER_EGG, 1);
                        name = "&#6ab567&lSniffer Egg x1";
                    } else {
                        reward = new ItemStack(Material.SHULKER_SHELL, 8);
                        name = "&#946794&lShulker Shell x8";
                    }
                } else if (chance <= 36) {
                    reward = new ItemStack(Material.NETHERITE_INGOT, 4);
                    name = "&#3a383a&lNetherite Ingot x4";
                } else if (chance <= 48) {
                    reward = new ItemStack(Material.DIAMOND, 64);
                    name = "&#a0f0ed&lDiamond x64";
                } else if (chance <= 56) {
                    // Use pre-rolled weaponRoll
                    switch (weaponRoll) {
                        case 0 -> {
                            reward = new ItemStack(Material.TRIDENT, 1);
                            name = "&#579b8c&lTrident x1";
                        }
                        case 1 -> {
                            reward = new ItemStack(Material.ELYTRA, 1);
                            name = "&#7d7d96&lElytra x1";
                        }
                        case 2 -> {
                            reward = new ItemStack(Material.CONDUIT, 1);
                            name = "&#4dcfcf&lConduit x1";
                        }
                        default -> {
                            reward = new ItemStack(Material.HEAVY_CORE, 1);
                            name = "&#4d5158&lHeavy Core x1";
                        }
                    }
                } else if (chance <= 64) {
                    // Use pre-rolled eggRoll
                    reward = getCycledEpicSpawnEgg(eggRoll);
                    if (reward.getType() == Material.SPIDER_SPAWN_EGG) {
                        name = ChatUtils.translateToColor("&#5F5347&lSpider Spawn Egg");
                    } else if (reward.getType() == Material.SKELETON_SPAWN_EGG) {
                        name = ChatUtils.translateToColor("&#BABABA&lSkeleton Spawn Egg");
                    } else if (reward.getType() == Material.CAVE_SPIDER_SPAWN_EGG) {
                        name = ChatUtils.translateToColor("&#002D31&lCave Spider Spawn Egg");
                    } else {
                        name = ChatUtils.translateToColor("&#71915D&lZombie Spawn Egg");
                    }
                } else if (chance <= 72) {
                    // Use pre-rolled clusterRolls
                    ItemStack cluster1 = getCycledCluster(clusterRolls[0]);
                    ItemStack cluster2 = getCycledCluster(clusterRolls[1]);
                    ItemStack cluster3 = getCycledCluster(clusterRolls[2]);
                    ItemStack cluster4 = getCycledCluster(clusterRolls[3]);
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
                            HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(cluster);
                            if (!remainder.isEmpty()) {
                                player.sendMessage(ChatUtils.chatMessage("&7The reward was dropped as you didn't have enough space!"));
                                for (ItemStack remain : remainder.values()) {
                                    player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remain);
                                }
                            }
                        }
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, name, "&3Epic Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled " + name + " in an Epic Crate"));
                    return;
                } else if (chance <= 80) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
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
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&eAll Skills +10 Levels", "&3Epic Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7Your mcMMO Skills have each increased by &e10 Levels"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled All Skills +10 Levels in an Epic Crate"));
                    return;
                } else if (chance <= 85) {
                    reward = new KeyRare().getItem();
                    reward.setAmount(3);
                    name = "&6&lRare Crate Key x3";
                } else if (chance <= 90) {
                    reward = new KeyEpic().getItem();
                    reward.setAmount(2);
                    name = "&3&lEpic Crate Key x2";
                } else if (chance <= 95) {
                    // Use pre-rolled isMagnetism
                    if (isMagnetism) {
                        reward = new IncantationMagnetism().getItem();
                    } else {
                        reward = new IncantationLifesteal().getItem();
                    }
                    name = reward.getItemMeta().getDisplayName();
                } else {
                    reward = new KeyGodly().getItem();
                    name = "&5&lGodly Crate Key x1";
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.EPIC);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(reward);
                if (!remainder.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage("&7The reward was dropped as you didn't have enough space!"));
                    for (ItemStack remain : remainder.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remain);
                    }
                }
                broadcastRewardToNearbyPlayers(player, aranarthPlayer, name, "&3Epic Crate");
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled " + name + " in an Epic Crate"));
            });
            playCrateOpenSound(player, CrateType.EPIC, () -> {});
        }
    }

    /**
     * Determines which reward the player will get from the Godly Crate.
     * Pre-rolls all random values before the animation starts so the winner is
     * known at sequence-build time and the animation displays the correct item.
     *
     * @param player     The player opening the Godly Crate.
     * @param crateBlock The chest block of the crate.
     */
    private void determineGodlyCrateReward(Player player, Block crateBlock) {
        if (AranarthUtils.getCratesInUse().contains(CrateType.GODLY)) {
            player.sendMessage(ChatUtils.chatMessage("&cThe &5Godly Crate &cis currently in use"));
        } else {
            AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
            aranarthPlayer.setCrateTypeBeingOpened(CrateType.GODLY);
            AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
            Bukkit.broadcastMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7is opening a &5&lGodly Crate"));

            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);

            // Pre-roll all random values before animation starts
            final int chance = new Random().nextInt(100) + 1;
            final boolean isDiamond = new Random().nextBoolean();
            final int aranarthiumRoll = new Random().nextInt(6);
            final int godlyEggRoll = new Random().nextInt(3);
            final int incantRoll = new Random().nextInt(3);

            // Determine the display item shown in the animation as the winner
            final ItemStack displayItem;
            if (chance <= 12) {
                displayItem = new ItemStack(Material.GOLD_INGOT);
            } else if (chance <= 24) {
                displayItem = isDiamond ? new ItemStack(Material.DIAMOND_BLOCK, 64) : new ItemStack(Material.SHULKER_SHELL, 32);
            } else if (chance <= 32) {
                displayItem = getCycledAranarthium(aranarthiumRoll);
            } else if (chance <= 44) {
                displayItem = new ItemStack(Material.NETHERITE_BLOCK);
            } else if (chance <= 52) {
                displayItem = new ItemStack(Material.ENCHANTED_BOOK);
            } else if (chance <= 60) {
                displayItem = new AranarthiumIngot().getItem();
            } else if (chance <= 72) {
                displayItem = new ItemStack(Material.NETHER_STAR);
            } else if (chance <= 80) {
                if (incantRoll == 0) {
                    displayItem = new IncantationResilience().getItem();
                } else if (incantRoll == 1) {
                    displayItem = new IncantationPreservation().getItem();
                } else {
                    displayItem = new IncantationPlentiful().getItem();
                }
            } else if (chance <= 85) {
                ItemStack key = new KeyGodly().getItem();
                key.setAmount(2);
                displayItem = key;
            } else if (chance <= 90) {
                ItemStack coupon = new ItemStack(Material.PAPER);
                ItemMeta couponMeta = coupon.getItemMeta();
                couponMeta.setMaxStackSize(1);
                couponMeta.setDisplayName(ChatUtils.translateToColor("&6&l30% Store Coupon"));
                List<String> couponLore = new ArrayList<>();
                couponLore.add(ChatUtils.translateToColor("&eContact a Council member to obtain this reward!"));
                couponLore.add(ChatUtils.translateToColor("&7Acquired on " + getCurrentTime()));
                couponMeta.setLore(couponLore);
                coupon.setItemMeta(couponMeta);
                displayItem = coupon;
            } else if (chance <= 95) {
                ItemStack key = new KeyEpic().getItem();
                key.setAmount(3);
                displayItem = key;
            } else {
                displayItem = getCycledGodlySpawnEgg(godlyEggRoll);
            }

            List<ItemStack> sequence = buildAnimationSequence(CrateType.GODLY, displayItem);
            startCrateAnimation(crateBlock, sequence, () -> {
                if (!player.isOnline()) {
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    AranarthUtils.addPendingGodlyKeys(player.getUniqueId(), 1);
                    return;
                }

                ItemStack reward = null;
                String name = "";

                if (chance <= 12) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                    aranarthPlayer.setBalance(aranarthPlayer.getBalance() + 75000);
                    if (NetworkManager.isActive()) {
                        NetworkManager.getInstance().publishBalanceAdjust(player.getUniqueId(), 75000);
                    }
                    aranarthPlayer.setCrateTypeBeingOpened(null);
                    AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                    AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                    PersistenceUtils.saveAranarthPlayerImmediately(player.getUniqueId());
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&6$75,000 of In-Game Currency", "&5Godly Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7You have earned &6$75,000 of In-Game Currency"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled $75,000 of In-Game Currency in a Godly Crate"));
                    return;
                } else if (chance <= 24) {
                    // Use pre-rolled isDiamond
                    if (isDiamond) {
                        reward = new ItemStack(Material.DIAMOND_BLOCK, 64);
                        name = "&#a0f0ed&lDiamond Block x64";
                    } else {
                        reward = new ItemStack(Material.SHULKER_SHELL, 32);
                        name = "&#946794&lShulker Shells x32";
                    }
                } else if (chance <= 32) {
                    // Use pre-rolled aranarthiumRoll
                    reward = getCycledAranarthium(aranarthiumRoll);
                    name = reward.getItemMeta().getDisplayName() + " x1";
                } else if (chance <= 44) {
                    reward = new ItemStack(Material.NETHERITE_BLOCK, 1);
                    name = "&#3a383a&lNetherite Block x1";
                } else if (chance <= 52) {
                    player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
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
                    broadcastRewardToNearbyPlayers(player, aranarthPlayer, "&eAll Skills +30 Levels", "&5Godly Crate");
                    player.sendMessage(ChatUtils.chatMessage("&7Your mcMMO Skills have each increased by &e30 Levels"));
                    Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled All Skills +30 Levels in a Godly Crate"));
                    return;
                } else if (chance <= 60) {
                    reward = new AranarthiumIngot().getItem();
                    name = reward.getItemMeta().getDisplayName() + " &f&lx1";
                } else if (chance <= 72) {
                    reward = new ItemStack(Material.NETHER_STAR, 1);
                    name = "&#d8d6fb&lNether Star x1";
                } else if (chance <= 80) {
                    // Use pre-rolled incantRoll
                    if (incantRoll == 0) {
                        reward = new IncantationResilience().getItem();
                        name = reward.getItemMeta().getDisplayName() + " x1";
                    } else if (incantRoll == 1) {
                        reward = new IncantationPreservation().getItem();
                        name = reward.getItemMeta().getDisplayName() + " x1";
                    } else {
                        reward = new IncantationPlentiful().getItem();
                        name = reward.getItemMeta().getDisplayName() + " x1";
                    }
                } else if (chance <= 85) {
                    reward = new KeyGodly().getItem();
                    reward.setAmount(2);
                    name = "&5&lGodly Crate Key x2";
                } else if (chance <= 90) {
                    DiscordUtils.createNotification(player.getName() + " has earned a 30% Store Coupon", player.getUniqueId());
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
                    // Use pre-rolled godlyEggRoll
                    reward = getCycledGodlySpawnEgg(godlyEggRoll);
                    if (reward.getType() == Material.MAGMA_CUBE_SPAWN_EGG) {
                        name = ChatUtils.translateToColor("&#4F0E0E&lMagma Cube Spawn Egg");
                    } else if (reward.getType() == Material.BLAZE_SPAWN_EGG) {
                        name = ChatUtils.translateToColor("&#FCD228&lBlaze Spawn Egg");
                    } else {
                        name = ChatUtils.translateToColor("&#51A03E&lSlime Spawn Egg");
                    }
                }

                aranarthPlayer.setCrateTypeBeingOpened(null);
                AranarthUtils.removeCrateFromUse(CrateType.GODLY);
                AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
                player.playSound(player, Sound.ENTITY_CHICKEN_EGG, 1, 0.6F);
                HashMap<Integer, ItemStack> remainder = player.getInventory().addItem(reward);
                if (!remainder.isEmpty()) {
                    player.sendMessage(ChatUtils.chatMessage("&7The reward was dropped as you didn't have enough space!"));
                    for (ItemStack remain : remainder.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), remain);
                    }
                }
                broadcastRewardToNearbyPlayers(player, aranarthPlayer, name, "&5Godly Crate");
                player.sendMessage(ChatUtils.chatMessage("&7You have earned " + name));
                Bukkit.getLogger().info("[AC] " + ChatUtils.stripColorFormatting(aranarthPlayer.getNickname() + " has rolled " + name + " in a Godly Crate"));
            });
            playCrateOpenSound(player, CrateType.GODLY, () -> {});
        }
    }

    /**
     * Sends a nearby broadcast message to all players within 10 blocks of the crate opener.
     *
     * @param player         The player that opened the crate.
     * @param aranarthPlayer The AranarthPlayer object of the opener.
     * @param reward         The reward string to display.
     * @param crateName      The name of the crate type (with color codes).
     */
    private void broadcastRewardToNearbyPlayers(Player player, AranarthPlayer aranarthPlayer, String reward, String crateName) {
        for (Player nearby : Bukkit.getOnlinePlayers()) {
            if (!nearby.equals(player) && nearby.getWorld().equals(player.getWorld()) && nearby.getLocation().distance(player.getLocation()) <= 10) {
                nearby.sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has rolled " + reward + " &7in a " + crateName));
            }
        }
    }

    /**
     * Provides the current time as a formatted String.
     *
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
     *
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
     *
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
     *
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
     *
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
     * Provides the spawn egg that is associated to the input index for Epic crate rewards.
     *
     * @param index The index of the spawn egg.
     * @return The spawn egg.
     */
    private ItemStack getCycledEpicSpawnEgg(int index) {
        ItemStack egg = null;
        switch (index) {
            case 1 -> egg = new ItemStack(Material.SPIDER_SPAWN_EGG);
            case 2 -> egg = new ItemStack(Material.SKELETON_SPAWN_EGG);
            case 3 -> egg = new ItemStack(Material.CAVE_SPIDER_SPAWN_EGG);
            default -> egg = new ItemStack(Material.ZOMBIE_SPAWN_EGG);
        }
        return egg;
    }

    /**
     * Provides the spawn egg that is associated to the input index for Godly crate rewards.
     *
     * @param index The index of the spawn egg.
     * @return The spawn egg.
     */
    private ItemStack getCycledGodlySpawnEgg(int index) {
        ItemStack egg = null;
        switch (index) {
            case 1 -> egg = new ItemStack(Material.BLAZE_SPAWN_EGG);
            case 2 -> egg = new ItemStack(Material.SLIME_SPAWN_EGG);
            default -> egg = new ItemStack(Material.MAGMA_CUBE_SPAWN_EGG);
        }
        return egg;
    }

    /**
     * Builds a fixed 24-item animation sequence for the given crate type and pre-determined winner.
     *
     * @param type   The type of crate being opened.
     * @param winner The pre-determined winning item to place at index 22.
     * @return A fixed 24-item sequence.
     */
    private List<ItemStack> buildAnimationSequence(CrateType type, ItemStack winner) {
        List<ItemStack> pool = getCrateItemPool(type);
        int poolSize = pool.size();
        List<ItemStack> sequence = new ArrayList<>(24);

        // Pool items cycling in order
        for (int i = 0; i < 22; i++) {
            sequence.add(pool.get(i % poolSize));
        }

        // The winner (will be at center when animation ends)
        sequence.add(winner);

        // One more pool item visible to the right, giving the impression it would have been next
        sequence.add(pool.get(22 % poolSize));

        return sequence;
    }

    /**
     * Spawns a scrolling ItemDisplay animation above a crate chest when it is opened.
     *
     * @param chestBlock The chest block being opened.
     * @param sequence   The pre-built item sequence from buildAnimationSequence.
     * @param onComplete Runnable fired (via scheduler) when the winner reaches center.
     */
    private void startCrateAnimation(Block chestBlock, List<ItemStack> sequence, Runnable onComplete) {
        int seqSize = sequence.size();
        World world = chestBlock.getWorld();

        double cx = chestBlock.getX() + 0.5;
        double cy = chestBlock.getY() + 1.15;
        double cz = chestBlock.getZ() + 0.5;

        // Remove any residual ItemDisplay entities from a previous roll near this chest
        Location animCenter = new Location(world, cx, cy, cz);
        world.getNearbyEntities(animCenter, 1, 1, 1, e -> e instanceof ItemDisplay)
                .forEach(e -> e.remove());

        float sideOffset = 0.225f;
        float maxYOff = -(0.35f - 0.1875f) / 2.0f; // base-aligns side items to center item

        Quaternionf rotate180 = new Quaternionf().rotateY((float) Math.PI);

        // All 4 entities are anchored at the chest center
        List<ItemDisplay> displays = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            ItemDisplay d = (ItemDisplay) world.spawnEntity(new Location(world, cx, cy, cz), EntityType.ITEM_DISPLAY);
            d.setBillboard(Display.Billboard.CENTER);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            d.setGravity(false);
            d.setInvulnerable(true);
            d.setPersistent(false);
            d.setInterpolationDuration(2);
            displays.add(d);
        }

        // Initial entity assignment:
        // display[2] = sequence[0] (center)
        // display[1] = sequence[1] (right side)
        // display[0] = sequence[2] (entering from right, partially offscreen)
        // display[3] = sequence[seqSize-1] as stand-in for left-side exiting item
        float[] virtPos = {2 * sideOffset, sideOffset, 0, -sideOffset};
        int[] nextSeqIdx = {3};
        displays.get(0).setItemStack(sequence.get(2));              // entering
        displays.get(1).setItemStack(sequence.get(1));              // right side
        displays.get(2).setItemStack(sequence.get(0));              // center
        displays.get(3).setItemStack(sequence.get(seqSize - 1));    // left side (exiting quickly)
        for (int i = 0; i < 4; i++) {
            applyDisplayTransform(displays.get(i), virtPos[i], sideOffset, maxYOff, rotate180);
        }

        new BukkitRunnable() {
            int tick = 0;
            boolean finalPhase = false;
            int finalPhaseTick = 0;
            int winnerEntityIdx = -1;

            @Override
            public void run() {
                for (ItemDisplay d : displays) {
                    if (!d.isValid()) { cancel(); return; }
                }

                if (finalPhase) {
                    if (finalPhaseTick == 0) {
                        // Hide all non-winner entities so no side items duplicate the winner
                        for (int j = 0; j < 4; j++) {
                            if (j != winnerEntityIdx) {
                                displays.get(j).setInterpolationDuration(0);
                                displays.get(j).setTransformation(new Transformation(
                                        new Vector3f(0, 0, 0), rotate180,
                                        new Vector3f(0, 0, 0), new Quaternionf()));
                            }
                        }
                    }
                    if (++finalPhaseTick >= 20) {
                        cancel();
                        displays.forEach(ItemDisplay::remove);
                    }
                    return;
                }

                // Determine movement delta per tick based on speed phase
                float moveDelta;
                if (tick < 28) {
                    moveDelta = sideOffset / 2.0f;
                } else if (tick < 52) {
                    moveDelta = sideOffset / 4.0f;
                } else {
                    moveDelta = sideOffset / 8.0f;
                }

                for (int i = 0; i < 4; i++) {
                    virtPos[i] -= moveDelta;

                    // When an entity wraps around from the left exit to the right enter,
                    // assign the next item from the sequence (if available).
                    if (virtPos[i] <= -2 * sideOffset) {
                        virtPos[i] += 4 * sideOffset;

                        if (nextSeqIdx[0] < seqSize) {
                            // Track which entity carries the winner (index seqSize-2)
                            if (nextSeqIdx[0] == seqSize - 2) {
                                winnerEntityIdx = i;
                            }
                            displays.get(i).setItemStack(sequence.get(nextSeqIdx[0]));
                        }
                        nextSeqIdx[0]++;
                    }

                    applyDisplayTransform(displays.get(i), virtPos[i], sideOffset, maxYOff, rotate180);
                }

                // Check if the winner entity has reached center
                if (winnerEntityIdx >= 0 && virtPos[winnerEntityIdx] <= 0) {
                    // Lock winner precisely at center
                    virtPos[winnerEntityIdx] = 0;
                    displays.get(winnerEntityIdx).setInterpolationDelay(0);
                    displays.get(winnerEntityIdx).setInterpolationDuration(4);
                    displays.get(winnerEntityIdx).setTransformation(new Transformation(
                            new Vector3f(0, 0, 0), rotate180,
                            new Vector3f(0.35f, 0.35f, 0.35f), new Quaternionf()));
                    Bukkit.getScheduler().runTask(AranarthCore.getInstance(), onComplete);
                    finalPhase = true;
                }

                tick++;

                // Safety cleanup if animation runs too long
                if (tick >= 150) {
                    cancel();
                    displays.forEach(ItemDisplay::remove);
                }
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0, 1);
    }

    /**
     * Applies a Transformation to an ItemDisplay.
     */
    private void applyDisplayTransform(ItemDisplay display, float virtPos,
                                       float sideOffset, float maxYOff, Quaternionf rotate180) {
        float absVp = Math.abs(virtPos);

        float scale;
        if (absVp >= 2 * sideOffset) {
            scale = 0.0f;
        } else if (absVp >= sideOffset) {
            float t = (absVp - sideOffset) / sideOffset;
            scale = 0.1875f * (1.0f - t);
        } else {
            float t = absVp / sideOffset;
            scale = 0.35f - (0.35f - 0.1875f) * t;
        }

        float yOff = absVp >= sideOffset ? maxYOff : maxYOff * (absVp / sideOffset);

        // virtPos is always along local X (viewer-right), since Billboard.CENTER
        // makes local X always point horizontally towards the viewer's right
        display.setInterpolationDelay(0);
        display.setTransformation(new Transformation(
                new Vector3f(virtPos, yOff, 0),
                rotate180,
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));
    }

    /**
     * Returns a representative pool of items for each crate type used in the roll animation.
     *
     * @param type The crate type.
     * @return List of items to cycle through.
     */
    private List<ItemStack> getCrateItemPool(CrateType type) {
        List<ItemStack> pool = new ArrayList<>();
        switch (type) {
            case VOTE -> {
                pool.add(new ItemStack(Material.BREAD));
                pool.add(new ItemStack(Material.IRON_INGOT));
                pool.add(new ItemStack(Material.GOLD_INGOT));
                pool.add(new GodAppleFragment().getItem());
                pool.add(new ItemStack(Material.EMERALD));
                pool.add(new ItemStack(Material.DIAMOND));
                pool.add(new ItemStack(Material.EXPERIENCE_BOTTLE));
                pool.add(new ItemStack(Material.PAPER));
                pool.add(new ItemStack(Material.BLAZE_ROD));
                pool.add(new ItemStack(Material.BREEZE_ROD));
                pool.add(new ItemStack(Material.FILLED_MAP));
                pool.add(new KeyRare().getItem());
            }
            case RARE -> {
                pool.add(new ItemStack(Material.ENCHANTED_BOOK));
                pool.add(new HoneyGlazedHam().getItem());
                pool.add(new ItemStack(Material.DIAMOND));
                pool.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
                pool.add(new ItemStack(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE));
                pool.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                pool.add(new ItemStack(Material.NETHERITE_INGOT));
                pool.add(new IncantationBeheading().getItem());
                pool.add(new CopperCluster().getItem());
                pool.add(new ItemStack(Material.OMINOUS_TRIAL_KEY));
                pool.add(new KeyEpic().getItem());
            }
            case EPIC -> {
                pool.add(new HoneyGlazedHam().getItem());
                pool.add(new ItemStack(Material.NETHERITE_INGOT));
                pool.add(new ItemStack(Material.DIAMOND));
                pool.add(new ItemStack(Material.TRIDENT));
                pool.add(new ItemStack(Material.ELYTRA));
                pool.add(new ItemStack(Material.CONDUIT));
                pool.add(new CopperCluster().getItem());
                pool.add(new ItemStack(Material.ZOMBIE_SPAWN_EGG));
                pool.add(new IncantationMagnetism().getItem());
                pool.add(new ItemStack(Material.SNIFFER_EGG));
                pool.add(new ItemStack(Material.SHULKER_SHELL));
                pool.add(new ItemStack(Material.ENCHANTED_BOOK));
                pool.add(new KeyGodly().getItem());
            }
            case GODLY -> {
                pool.add(new ItemStack(Material.DIAMOND_BLOCK));
                pool.add(new AranarthiumIngot().getItem());
                pool.add(new ItemStack(Material.NETHERITE_BLOCK));
                pool.add(new ItemStack(Material.NETHER_STAR));
                pool.add(new AranarthiumArdent().getItem());
                pool.add(new IncantationResilience().getItem());
                pool.add(new ItemStack(Material.MAGMA_CUBE_SPAWN_EGG));
                pool.add(new ItemStack(Material.SHULKER_SHELL));
                pool.add(new ItemStack(Material.ENCHANTED_BOOK));
                pool.add(new ItemStack(Material.PAPER));
                pool.add(new KeyEpic().getItem());
                pool.add(new ItemStack(Material.BLAZE_SPAWN_EGG));
                pool.add(new KeyGodly().getItem());
            }
        }
        return pool;
    }

    /**
     * Combines the 4 clusters if they are for the same type.
     *
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

        return combined;
    }
}
