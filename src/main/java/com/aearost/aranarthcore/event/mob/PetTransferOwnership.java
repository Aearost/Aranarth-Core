package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Allows a player to transfer the ownership of their pet to the stored player UUID.
 */
public class PetTransferOwnership {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getPetTransferUuid() != null) {
			Tameable tameable = (Tameable) e.getRightClicked();
			if (tameable.isTamed()) {
				if (tameable.getOwnerUniqueId().equals(player.getUniqueId())) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(aranarthPlayer.getPetTransferUuid());
					tameable.setOwner(offlinePlayer);
					e.setCancelled(true);
					AranarthPlayer offlineAranarthPlayer = AranarthUtils.getPlayer(aranarthPlayer.getPetTransferUuid());
					player.sendMessage(ChatUtils.chatMessage("&e" + tameable.getName() + " &7is now owned by &e" + offlineAranarthPlayer.getNickname()));
					if (offlinePlayer.isOnline()) {
						offlinePlayer.getPlayer().sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has transferred the ownership of &e" + tameable.getName() + " &7to you!"));
					}
				} else {
					player.sendMessage(ChatUtils.chatMessage("&cThis is not your pet!"));
				}
			} else {
				player.sendMessage(ChatUtils.chatMessage("&cThis pet is not tamed yet!"));
			}
			aranarthPlayer.setPetTransferUuid(null);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		}
	}
}
