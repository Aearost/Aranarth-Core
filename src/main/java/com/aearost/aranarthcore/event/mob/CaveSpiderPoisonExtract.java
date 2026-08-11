package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Right-clicking a cave spider with an empty glass bottle produces a Potion of Poison II.
 * Hard limit: 32 extractions per spider per 10-minute window.
 */
public class CaveSpiderPoisonExtract {

    private static final int MAX_EXTRACTIONS = 32;
    private static final Map<UUID, Integer> extractionCounts = new HashMap<>();

    public static void resetExtractionCounts() {
        extractionCounts.clear();
    }

    public void execute(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(e.getRightClicked() instanceof CaveSpider)) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GLASS_BOTTLE) {
            return;
        }

        e.setCancelled(true);

        UUID spiderId = e.getRightClicked().getUniqueId();
        int count = extractionCounts.getOrDefault(spiderId, 0);
        if (count >= MAX_EXTRACTIONS) {
            player.sendMessage(ChatUtils.chatMessage("&7This cave spider has no venom left"));
            return;
        }
        extractionCounts.put(spiderId, count + 1);

        ItemStack poison = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) poison.getItemMeta();
        meta.setBasePotionType(PotionType.POISON);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 864, 1, false, true, true), true);
        poison.setItemMeta(meta);

        PlayerInventory inv = player.getInventory();
        if (item.getAmount() == 1) {
            inv.setItemInMainHand(poison);
        } else {
            item.setAmount(item.getAmount() - 1);
            HashMap<Integer, ItemStack> leftover = inv.addItem(poison);
            leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_COW_MILK, 1.0f, 1.7f);
        player.sendMessage(ChatUtils.chatMessage("&aYou extract the venom from the cave spider"));
    }
}
