package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shares villager cure discounts for villagers located in a Dominion's land.
 */
public class VillagerDominionDiscount {

	// Vanilla gossip weights used when calculating a player's reputation with a villager
	private static final int MAJOR_POSITIVE_WEIGHT = 5;
	private static final int MINOR_POSITIVE_WEIGHT = 1;
	private static final int MINOR_NEGATIVE_WEIGHT = -1;
	private static final int MAJOR_NEGATIVE_WEIGHT = -5;
	private static final int TRADING_WEIGHT = 1;

	public void execute(InventoryOpenEvent e) {
		if (e.isCancelled()) {
			return;
		}
		if (!(e.getInventory() instanceof MerchantInventory merchantInv)) {
			return;
		}
		if (!(merchantInv.getMerchant() instanceof Villager villager)) {
			return;
		}
		if (!(e.getPlayer() instanceof Player player)) {
			return;
		}

		// Only villagers in a Dominion's land share discounts, and only with players allowed to interact with them
		Dominion dominion = DominionUtils.getDominionOfChunkAnywhere(villager.getLocation().getChunk());
		if (dominion == null || !DominionUtils.hasPermission(player, dominion, DominionPermission.VILLAGER)) {
			return;
		}

		Reputation ownReputation = villager.getReputation(player.getUniqueId());
		int ownCure = getCureReputation(ownReputation);

		List<UUID> dominionPlayers = new ArrayList<>(dominion.getMembers());
		dominionPlayers.add(dominion.getLeader());
		int bestCure = ownCure;
		for (UUID uuid : dominionPlayers) {
			if (uuid == null || uuid.equals(player.getUniqueId())) {
				continue;
			}
			bestCure = Math.max(bestCure, getCureReputation(villager.getReputation(uuid)));
		}
		if (bestCure <= ownCure) {
			return;
		}

		// Vanilla has already applied the player's own discount, so only apply the difference
		int ownTotal = getTotalReputation(ownReputation);
		int sharedTotal = ownTotal - ownCure + bestCure;
		for (MerchantRecipe recipe : villager.getRecipes()) {
			int ownDiscount = (int) Math.floor(ownTotal * recipe.getPriceMultiplier());
			int sharedDiscount = (int) Math.floor(sharedTotal * recipe.getPriceMultiplier());
			recipe.setSpecialPrice(recipe.getSpecialPrice() - (sharedDiscount - ownDiscount));
		}
	}

	/**
	 * Provides the portion of the reputation that is gained from curing a zombie villager.
	 *
	 * @param reputation The player's reputation with the villager.
	 * @return The weighted cure reputation.
	 */
	private int getCureReputation(Reputation reputation) {
		if (reputation == null) {
			return 0;
		}
		return reputation.getReputation(ReputationType.MAJOR_POSITIVE) * MAJOR_POSITIVE_WEIGHT
				+ reputation.getReputation(ReputationType.MINOR_POSITIVE) * MINOR_POSITIVE_WEIGHT;
	}

	/**
	 * Provides the full weighted reputation, matching how vanilla calculates trade prices.
	 * @param reputation The player's reputation with the villager.
	 * @return The weighted total reputation.
	 */
	private int getTotalReputation(Reputation reputation) {
		if (reputation == null) {
			return 0;
		}
		return getCureReputation(reputation)
				+ reputation.getReputation(ReputationType.MINOR_NEGATIVE) * MINOR_NEGATIVE_WEIGHT
				+ reputation.getReputation(ReputationType.MAJOR_NEGATIVE) * MAJOR_NEGATIVE_WEIGHT
				+ reputation.getReputation(ReputationType.TRADING) * TRADING_WEIGHT;
	}
}
