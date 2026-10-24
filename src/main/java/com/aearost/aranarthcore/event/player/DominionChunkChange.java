package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionPermission;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.aearost.aranarthcore.utils.OutpostUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles Dominion enter and exit messages, as well as auto-claim functionality.
 */
public class DominionChunkChange {

	public void execute(PlayerMoveEvent e) {
		if (e.getTo() == null) {
			return;
		}

		String toWorldName = e.getTo().getWorld().getName();
		if (!toWorldName.startsWith("world") && !toWorldName.startsWith("smp")) {
			return;
		}

		Chunk from = e.getFrom().getChunk();
		Chunk to = e.getTo().getChunk();

		if (from.getX() == to.getX() && from.getZ() == to.getZ()
				&& from.getWorld().getName().equals(to.getWorld().getName())) {
			return;
		}

		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		// Resolve ownership of both chunks
		Dominion dominionFrom = DominionUtils.getDominionOfChunk(from);
		Outpost outpostFrom = (dominionFrom == null) ? OutpostUtils.getOutpostOfChunk(from) : null;
		Dominion outpostDominionFrom = (outpostFrom != null) ? DominionUtils.getDominionById(outpostFrom.getDominionId()) : null;

		Dominion dominionTo = DominionUtils.getDominionOfChunk(to);
		Outpost outpostTo = (dominionTo == null) ? OutpostUtils.getOutpostOfChunk(to) : null;
		Dominion outpostDominionTo = (outpostTo != null) ? DominionUtils.getDominionById(outpostTo.getDominionId()) : null;

		Dominion effectiveFrom = (dominionFrom != null) ? dominionFrom : outpostDominionFrom;
		Dominion effectiveTo = (dominionTo != null) ? dominionTo : outpostDominionTo;

		// Action bar for region entry/exit (must run before same-dominion early returns)
		String fromChunkKey = from.getWorld().getName() + ":" + from.getX() + ":" + from.getZ();
		String toChunkKey = to.getWorld().getName() + ":" + to.getX() + ":" + to.getZ();

		// Region auto-claim - adds chunks of the player's own Dominion that are not yet in a region
		String autoClaimRegion = aranarthPlayer.getAutoClaimRegion();
		if (autoClaimRegion != null && dominionTo != null && !dominionTo.getRegionChunkNames().containsKey(toChunkKey)) {
			Dominion playerDominion = DominionUtils.getPlayerDominion(player.getUniqueId());
			if (playerDominion != null && playerDominion.isSameDominion(dominionTo)) {
				if (!dominionTo.getRegionMembers().containsKey(autoClaimRegion)) {
					aranarthPlayer.setAutoClaimRegion(null);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.autoclaim_region_stopped", "region", autoClaimRegion)));
				} else if (!DominionUtils.hasPermission(player, dominionTo, DominionPermission.MANAGE_REGIONS)) {
					aranarthPlayer.setAutoClaimRegion(null);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.no_permission_regions")));
				} else {
					dominionTo.getRegionChunkNames().put(toChunkKey, autoClaimRegion);
					DominionUtils.updateDominion(dominionTo);
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.region_chunk_added", "region", autoClaimRegion)));
				}
			}
		}

		String fromRegionName = (dominionFrom != null) ? dominionFrom.getRegionChunkNames().get(fromChunkKey) : null;
		String toRegionName = (dominionTo != null) ? dominionTo.getRegionChunkNames().get(toChunkKey) : null;

		// Entering a region shows its name, and leaving one for the rest of the same Dominion shows the Dominion's name
		if (toRegionName != null && !toRegionName.equals(fromRegionName)) {
			player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(Lang.getFor(player, "dominion.region_name", "name", toRegionName)));
		} else if (fromRegionName != null && toRegionName == null && dominionTo != null && dominionTo.isSameDominion(dominionFrom)) {
			player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(Lang.getFor(player, "dominion.region_name", "name", dominionTo.getName())));
		}

		// Determine if moving within the same territory
		if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isSameDominion(effectiveTo)) {
			if (dominionFrom == null && dominionTo == null && outpostFrom != null && outpostTo != null
					&& outpostFrom.getId().equals(outpostTo.getId())) {
				return; // Same outpost
			}
			if (dominionFrom != null && dominionTo != null) {
				return; // Same main dominion
			}
			// Show messages
		}

		boolean sameDominionTransition = effectiveFrom != null && effectiveTo != null
				&& effectiveFrom.isSameDominion(effectiveTo);

		// Exit message
		if (effectiveFrom != null && !sameDominionTransition) {
			if (effectiveTo == null && aranarthPlayer.isAutoClaimEnabled()) {
				String result = DominionUtils.claimChunk(player, to);
				player.sendMessage(ChatUtils.chatMessage(result));
				return;
			}
			if (!aranarthPlayer.isTogglingChangeClaim()) {
				if (dominionFrom != null) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.exited", "name", dominionFrom.getName())));
				} else if (outpostFrom != null && outpostDominionFrom != null) {
					player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.exited_outpost", "outpost", outpostFrom.getName(), "name", outpostDominionFrom.getName())));
				}
			}
		}

		// Enter message
		if (effectiveTo != null && !aranarthPlayer.isTogglingChangeClaim()) {
			if (dominionTo != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.entered", "name", dominionTo.getName())));
			} else if (outpostTo != null && outpostDominionTo != null) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "dominion.entered_outpost", "outpost", outpostTo.getName(), "name", outpostDominionTo.getName())));
			}
		}

	}
}
