package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.Outpost;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DominionUtils;
import com.aearost.aranarthcore.utils.OutpostUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

		// Action bar for plot entry/exit (must run before same-dominion early returns)
		String fromChunkKey = from.getWorld().getName() + ":" + from.getX() + ":" + from.getZ();
		String toChunkKey = to.getWorld().getName() + ":" + to.getX() + ":" + to.getZ();
		String fromPlotName = (dominionFrom != null) ? dominionFrom.getPlotChunkNames().get(fromChunkKey) : null;
		String toPlotName = (dominionTo != null) ? dominionTo.getPlotChunkNames().get(toChunkKey) : null;

		if (fromPlotName != null && !fromPlotName.equals(toPlotName)) {
			player.sendActionBar(Component.text("You have exited the plot of ").color(NamedTextColor.GRAY)
					.append(Component.text(fromPlotName).color(NamedTextColor.YELLOW)));
		}
		if (toPlotName != null && !toPlotName.equals(fromPlotName)) {
			player.sendActionBar(Component.text("You have entered the plot of ").color(NamedTextColor.GRAY)
					.append(Component.text(toPlotName).color(NamedTextColor.YELLOW)));
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
					player.sendMessage(ChatUtils.chatMessage("&7You have exited the Dominion of &e" + dominionFrom.getName()));
				} else if (outpostFrom != null && outpostDominionFrom != null) {
					player.sendMessage(ChatUtils.chatMessage("&7You have exited &e" + outpostDominionFrom.getName()
							+ "&7's outpost, &e" + outpostFrom.getName()));
				}
			}
		}

		// Enter message
		if (effectiveTo != null && !aranarthPlayer.isTogglingChangeClaim()) {
			if (dominionTo != null) {
				player.sendMessage(ChatUtils.chatMessage("&7You have entered the Dominion of &e" + dominionTo.getName()));
			} else if (outpostTo != null && outpostDominionTo != null) {
				player.sendMessage(ChatUtils.chatMessage("&7You have entered &e" + outpostDominionTo.getName()
						+ "&7's outpost, &e" + outpostTo.getName()));
			}
		}

	}
}
