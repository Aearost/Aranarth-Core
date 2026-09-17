package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * When ProjectKorra replaces a water bottle with a glass bottle in the player's inventory,
 * this listener auto-refills the slot from the player's /potions storage and gives them
 * the resulting empty bottle.
 */
public class WaterbendingBottleRefillListener implements Listener {

	public WaterbendingBottleRefillListener(AranarthCore plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onInventorySlotChange(PlayerInventorySlotChangeEvent e) {
		Player player = e.getPlayer();

		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		if (!AranarthUtils.isSurvivalWorld(player.getWorld().getName())) {
			return;
		}

		ItemStack oldItem = e.getOldItemStack();
		ItemStack newItem = e.getNewItemStack();

		// Only care about water bottle - glass bottle transitions
		if (!isPlainWaterBottle(oldItem) || newItem.getType() != Material.GLASS_BOTTLE) {
			return;
		}

		// Only main inventory
		if (e.getRawSlot() > 35) {
			return;
		}

		// If the player is drinking the bottle, PotionConsumeListener handles the refill
		if (PotionConsumeListener.consumeWaterBottleDrinkFlag(player.getUniqueId())) {
			return;
		}

		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<ItemStack, Integer> potions = aranarthPlayer.getPotions();
		if (Objects.isNull(potions)) {
			return;
		}

		// Find a stored water bottle entry
		ItemStack waterBottleKey = null;
		for (ItemStack stored : potions.keySet()) {
			if (isPlainWaterBottle(stored)) {
				waterBottleKey = stored;
				break;
			}
		}

		if (waterBottleKey == null) {
			return;
		}

		final int slot = e.getRawSlot();
		final ItemStack keyRef = waterBottleKey;

		Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
			if (!player.isOnline()) {
				return;
			}

			// Re-fetch state in case something changed in the same tick
			AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
			HashMap<ItemStack, Integer> storedPotions = ap.getPotions();
			if (Objects.isNull(storedPotions) || !storedPotions.containsKey(keyRef)) {
				return;
			}

			int remaining = storedPotions.get(keyRef) - 1;
			if (remaining <= 0) {
				storedPotions.remove(keyRef);
			} else {
				storedPotions.put(keyRef, remaining);
			}
			ap.setPotions(storedPotions);
			AranarthUtils.setPlayer(player.getUniqueId(), ap);

			// Restore the slot to a fresh water bottle
			ItemStack waterBottle = keyRef.clone();
			waterBottle.setAmount(1);
			player.getInventory().setItem(slot, waterBottle);

			// Give back the empty bottle
			Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE, 1));
			leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

			player.updateInventory();
		});
	}

	private boolean isPlainWaterBottle(ItemStack item) {
		if (item == null || item.getType() != Material.POTION) {
			return false;
		}
		if (!(item.getItemMeta() instanceof PotionMeta meta)) {
			return false;
		}
		return meta.getBasePotionType() == PotionType.WATER && meta.getCustomEffects().isEmpty();
	}
}
