package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.EnchantingUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Random;

/**
 * Handles storing EXP in the player's held empty bottle.
 */
public class ExpStore {

    // Vanilla bottles provide 3 to 11 EXP
    private static final double AVERAGE_BOTTLE_EXP = 7.0;
    private static final Random RANDOM = new Random();

    public void execute(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (e.getHand() == EquipmentSlot.HAND) {
            if (player.getInventory().getItemInMainHand().getType() == Material.GLASS_BOTTLE) {
                if (player.isSneaking()) {
                    int amountToReduce = getBottleCost(player);
                    int totalExp = getTotalExp(player);

                    if (totalExp >= amountToReduce) {
                        // The bottle would otherwise go to the Creative or Arena inventory, losing the player's EXP
                        if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.bottled_survival_only")));
                            return;
                        }
                        int unlockLevel = EnchantingUtils.getUnlockLevel("bottled-knowledge");
                        if (EnchantingUtils.getLevel(player) < unlockLevel) {
                            player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "enchanting.bottled_knowledge_locked", "level", unlockLevel)));
                            return;
                        }
                        // Reduces by more than what the bottle provides, based on the Bottled Knowledge sub-skill
                        player.giveExp(-amountToReduce);

                        int expVol = AranarthUtils.getPlayer(player.getUniqueId()).getExpStoreSoundVolume();
                        if (expVol > 0) {
                            float expVf = expVol / 100f;
                            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 1F);
                            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 1.5F);
                            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, expVf, 2F);
                        }
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
                        // If the player's inventory was full, drop it to the ground
                        if (!leftover.isEmpty()) {
                            player.getLocation().getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                        }
                        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                    }
                }
            }
        }
    }

    /**
     * Provides the EXP cost of storing a bottle.
     *
     * @param player The player storing the EXP.
     * @return The EXP cost.
     */
    private int getBottleCost(Player player) {
        double exactCost = AVERAGE_BOTTLE_EXP / EnchantingUtils.getBottledKnowledgeRatio(EnchantingUtils.getLevel(player));
        int cost = (int) Math.floor(exactCost);
        // Randomly rounds up based on the fraction, so the average cost matches the exact cost
        if (RANDOM.nextDouble() < exactCost - cost) {
            cost++;
        }
        return Math.max(cost, (int) AVERAGE_BOTTLE_EXP + 1);
    }

    /**
     * Helper method to provide the true total EXP a player has.
     *
     * @param player The player to be analyzed.
     * @return The true total EXP.
     */
    private int getTotalExp(Player player) {
        int level = player.getLevel();
        float progress = player.getExp(); // 0.0 - 1.0

        int exp = 0;

        if (level <= 16) {
            exp = (int) (Math.pow(level, 2) + 6 * level);
        } else if (level <= 31) {
            exp = (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            exp = (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }

        // Add progress within the current level
        exp += Math.round(progress * player.getExpToLevel());
        return exp;
    }
}
