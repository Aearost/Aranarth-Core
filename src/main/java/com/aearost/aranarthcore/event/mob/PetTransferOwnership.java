package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Allows a player to transfer the ownership of their pet or mount to the stored transfer UUID.
 * Supports both vanilla {@link Tameable} entities and non-tamed mounts tracked via
 * the {@code mount_owner} PDC key (e.g. Sniffers).
 */
public class PetTransferOwnership {
	public void execute(PlayerInteractEntityEvent e) {
		Player player = e.getPlayer();
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getPetTransferUuid() == null) {
			return;
		}

		Entity entity = e.getRightClicked();

		if (entity instanceof Tameable tameable) {
			transferTameable(e, player, aranarthPlayer, tameable);
		} else if (entity instanceof LivingEntity living) {
			transferMountOwner(e, player, aranarthPlayer, living);
		}
	}

	// -------------------------------------------------------------------------
	// Tameable branch (Horses, Wolves, Cats, etc.)
	// -------------------------------------------------------------------------

	private void transferTameable(PlayerInteractEntityEvent e, Player player,
								  AranarthPlayer aranarthPlayer, Tameable tameable) {
		if (tameable.isTamed()) {
			if (tameable.getOwnerUniqueId().equals(player.getUniqueId())) {
				UUID transferUUID = aranarthPlayer.getPetTransferUuid();
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(transferUUID);
				tameable.setOwner(offlinePlayer);
				e.setCancelled(true);
				AranarthPlayer offlineAranarthPlayer = AranarthUtils.getPlayer(transferUUID);
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

	// -------------------------------------------------------------------------
	// AranarthMount branch (Sniffers and future non-tamed mounts)
	// -------------------------------------------------------------------------

	private void transferMountOwner(PlayerInteractEntityEvent e, Player player,
									AranarthPlayer aranarthPlayer, LivingEntity entity) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		if (!pdc.has(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING)) {
			return;
		}

		UUID ownerUUID;
		try {
			ownerUUID = UUID.fromString(pdc.get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING));
		} catch (IllegalArgumentException ignored) {
			return;
		}

		if (!ownerUUID.equals(player.getUniqueId())) {
			player.sendMessage(ChatUtils.chatMessage("&cThis is not your mount!"));
			aranarthPlayer.setPetTransferUuid(null);
			AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
			return;
		}

		UUID transferUUID = aranarthPlayer.getPetTransferUuid();
		pdc.set(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING, transferUUID.toString());
		e.setCancelled(true);

		AranarthPlayer offlineAranarthPlayer = AranarthUtils.getPlayer(transferUUID);
		player.sendMessage(ChatUtils.chatMessage("&e" + entity.getName() + " &7is now owned by &e" + offlineAranarthPlayer.getNickname()));
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(transferUUID);
		if (offlinePlayer.isOnline()) {
			offlinePlayer.getPlayer().sendMessage(ChatUtils.chatMessage("&e" + aranarthPlayer.getNickname() + " &7has transferred the ownership of &e" + entity.getName() + " &7to you!"));
		}

		aranarthPlayer.setPetTransferUuid(null);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
	}
}
