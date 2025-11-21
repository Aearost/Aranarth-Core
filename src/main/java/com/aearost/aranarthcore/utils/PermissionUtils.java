package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.UUID;

/**
 * Provides a large variety of utility methods for everything related to items and inventory.
 */
public class PermissionUtils {

	/**
	 * Centralizes all permissions logic being set.
	 * @param player The player.
	 * @param isSecondCall If it was a recursive call from the same method for the sub-element fix.
	 */
	public static void evaluatePlayerPermissions(Player player, boolean isSecondCall) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());

		setDefaultPermissions(player, perms);
		setRankPermissions(perms, aranarthPlayer.getRank());
		setSaintPermissions(perms, aranarthPlayer.getSaintRank());
		setCouncilPermissions(perms, aranarthPlayer.getCouncilRank());

		Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
		// If the player is the avatar
		if (currentAvatar != null && currentAvatar.getUuid().equals(player.getUniqueId())) {
			updateAvatarPermissions(player.getUniqueId(), false);
		}


		// Updates the sub-elements and abilities according to their current rank
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);

		if (bendingPlayer != null) {
			for (Element element : bendingPlayer.getElements()) {
				for (Element.SubElement subElement : Element.getSubElements(element)) {
					if (bendingPlayer.hasSubElementPermission(subElement)) {
						bendingPlayer.addSubElement(subElement);
					} else {
						bendingPlayer.getSubElements().remove(subElement);
					}
				}
			}
			bendingPlayer.removeUnusableAbilities();
		}

		if (isSecondCall) {
			Bukkit.getLogger().info(player.getName() + "'s permissions have been evaluated");
		} else {
			// Applies again as sub-elements do not update correctly unless command is re-executed
			Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
				@Override
				public void run() {
					evaluatePlayerPermissions(player, true);
				}
			}, 10);

		}
	}

	/**
	 * Sets the default permissions for all players.
	 * @param player The player whose permissions are being evaluated.
	 * @param perms The permissions the player will have access to.
	 */
	private static void setDefaultPermissions(Player player, PermissionAttachment perms) {

		perms.setPermission("bending.command.rechoose", true);

		// Disable sub-elements
		perms.setPermission("bending.water.healing", false);
		perms.setPermission("bending.water.plantbending", false);
		perms.setPermission("bending.fire.combustionbending", false);
		perms.setPermission("bending.fire.lightningbending", false);
		perms.setPermission("bending.fire.bluefire", false);
		perms.setPermission("bending.earth.metalbending", false);
		perms.setPermission("bending.earth.lavabending", false);
		perms.setPermission("bending.earth.sandbending", false);
		perms.setPermission("bending.air.flight", false);
		perms.setPermission("bending.earth.sandbending", false);
		perms.setPermission("bending.water.bloodbending", false);
		perms.setPermission("bending.water.bloodbending.anytime", false);

		// Disable abilities
		perms.setPermission("bending.ability.waterarms", false);
		perms.setPermission("bending.ability.firecomet", false);
		perms.setPermission("bending.ability.metalclips", false);
		perms.setPermission("bending.ability.sonicblast", false);
		perms.setPermission("bending.ability.suffocate", false);

		// Disable aranarth functionality
		perms.setPermission("aranarth.exp", false);
		perms.setPermission("aranarth.seen", false);
		perms.setPermission("aranarth.msg", false);
		perms.setPermission("aranarth.back", false);
		perms.setPermission("aranarth.tphere", false);
		perms.setPermission("aranarth.nick", false);
		perms.setPermission("aranarth.nick.color", false);
		perms.setPermission("aranarth.nick.hex", false);
		perms.setPermission("aranarth.nick.gradient", false);
		perms.setPermission("aranarth.chat.color", false);
		perms.setPermission("aranarth.chat.hex", false);
		perms.setPermission("aranarth.toggle.msg", false);
		perms.setPermission("aranarth.toggle.chat", false);
		perms.setPermission("aranarth.toggle.tp", false);
		perms.setPermission("aranarth.hat", false);
		perms.setPermission("aranarth.trash", false);
		perms.setPermission("aranarth.blacklist", false);
		perms.setPermission("aranarth.randomizer", false);
		perms.setPermission("aranarth.tables", false);
		perms.setPermission("aranarth.itemname", false);
		perms.setPermission("aranarth.itemname.gradient", false);
		perms.setPermission("aranarth.compress", false);
		perms.setPermission("aranarth.mute", false);
		perms.setPermission("aranarth.give", false);
		perms.setPermission("aranarth.whereis", false);
		perms.setPermission("aranarth.ban", false);
		perms.setPermission("aranarth.invsee", false);
		perms.setPermission("aranarth.spy", false);
		perms.setPermission("aranarth.unmute", false);
		perms.setPermission("aranarth.unban", false);
		perms.setPermission("aranarth.dominion.create", false);
		perms.setPermission("aranarth.dominion.home", false);
		perms.setPermission("aranarth.give", false);
		perms.setPermission("aranarth.rankset", false);
		perms.setPermission("aranarth.protect.bypass", false);
		perms.setPermission("aranarth.warp.modify", false);
		perms.setPermission("aranarth.warn", false);
		perms.setPermission("aranarth.punishments", false);
		perms.setPermission("aranarth.avatar.set", false);
		perms.setPermission("aranarth.broadcast", false);
	}

	/**
	 * Sets the permissions for either removing an avatar or for a new avatar.
	 * @param uuid The player's UUID.
	 */
	public static void updateAvatarPermissions(UUID uuid, boolean isRemoval) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);

		// Remove the old avatar's permissions
		if (isRemoval) {
			Avatar previousAvatar = AvatarUtils.getAvatars().get(AvatarUtils.getAvatars().size() - 2);
			// Removes all elements and sub-elements
			bendingPlayer.getSubElements().clear();
			bendingPlayer.saveSubElements();
			bendingPlayer.getElements().clear();
			bendingPlayer.saveElements();

			// Adds back their original element
			char element = previousAvatar.getElement();
			Element elementToAdd = null;
			if (element == 'A') {
				elementToAdd = Element.AIR;
			} else if (element == 'W') {
				elementToAdd = Element.WATER;
			} else if (element == 'E') {
				elementToAdd = Element.EARTH;
			} else if (element == 'F') {
				elementToAdd = Element.FIRE;
			}
			bendingPlayer.addElement(elementToAdd);
			bendingPlayer.saveElements();

			// Permissions will be reloaded once they join back if they are not online
			if (player.isOnline()) {
				Player onlinePlayer = (Player) player;
				PermissionAttachment perms = onlinePlayer.addAttachment(AranarthCore.getInstance());

				// Do not allow manual element changes for the avatar
				perms.setPermission("bending.command.choose", true);
				perms.setPermission("bending.command.rechoose", true);

				evaluatePlayerPermissions((Player) player, false);
			}
		}
		// A new avatar
		else {
			if (!bendingPlayer.getElements().contains(Element.AIR)) {
				bendingPlayer.addElement(Element.AIR);
			}
			if (!bendingPlayer.getElements().contains(Element.WATER)) {
				bendingPlayer.addElement(Element.WATER);
			}
			if (!bendingPlayer.getElements().contains(Element.EARTH)) {
				bendingPlayer.addElement(Element.EARTH);
			}
			if (!bendingPlayer.getElements().contains(Element.FIRE)) {
				bendingPlayer.addElement(Element.FIRE);
			}

			for (Element.SubElement subElement : Element.SubElement.getSubElements()) {
				// Skips bloodbending, flight, and blue fire
				if (subElement != Element.SubElement.BLOOD && subElement != Element.SubElement.BLUE_FIRE
						&& subElement != Element.SubElement.FLIGHT) {
					if (!bendingPlayer.hasSubElement(subElement)) {
						bendingPlayer.addSubElement(subElement);
					}
				}
			}
			bendingPlayer.saveSubElements();
			bendingPlayer.saveElements();

			if (player.isOnline()) {
				Player onlinePlayer = (Player) player;
				PermissionAttachment perms = onlinePlayer.addAttachment(AranarthCore.getInstance());

				// Do not allow manual element changes for the avatar
				perms.setPermission("bending.command.choose", false);
				perms.setPermission("bending.command.rechoose", false);

				// Enable all sub-elements
				perms.setPermission("bending.water.healing", true);
				perms.setPermission("bending.water.plantbending", true);
				perms.setPermission("bending.fire.combustionbending", true);
				perms.setPermission("bending.fire.lightningbending", true);
				perms.setPermission("bending.earth.metalbending", true);
				perms.setPermission("bending.earth.lavabending", true);
				perms.setPermission("bending.earth.sandbending", true);

				// Enable all abilities
				perms.setPermission("bending.ability.waterarms", true);
				perms.setPermission("bending.ability.firecomet", true);
				perms.setPermission("bending.ability.metalclips", true);
				perms.setPermission("bending.ability.sonicblast", true);
				perms.setPermission("bending.ability.suffocate", true);
			}
		}
	}

	/**
	 * Sets the permissions for all in-game ranks.
	 * @param perms The permissions the player will have access to.
	 * @param rank The player's in-game rank.
	 */
	private static void setRankPermissions(PermissionAttachment perms, int rank) {
		// Esquire
		if (rank >= 1) {
			perms.setPermission("bending.water.healing", true);
			perms.setPermission("bending.water.plantbending", true);
			perms.setPermission("bending.earth.sandbending", true);
			perms.setPermission("aranarth.seen", true);
			perms.setPermission("aranarth.dominion.home", true);
		} else {
			return;
		}

		// Knight
		if (rank >= 2) {
			perms.setPermission("bending.earth.metalbending", true);
			perms.setPermission("aranarth.exp", true);
			perms.setPermission("aranarth.msg", true);
		} else {
			return;
		}

		// Baron
		if (rank >= 3) {
			perms.setPermission("bending.ability.waterarms", true);
			perms.setPermission("aranarth.dominion.create", true);
		} else {
			return;
		}

		// Count
		if (rank >= 4) {
			perms.setPermission("bending.ability.sonicblast", true);
			perms.setPermission("aranarth.back", true);
		} else {
			return;
		}

		// Duke
		if (rank >= 5) {
			perms.setPermission("bending.ability.metalclips", true);
			perms.setPermission("bending.ability.suffocate", true);
			perms.setPermission("aranarth.nick", true);
		} else {
			return;
		}

		// Prince
		if (rank >= 6) {
			perms.setPermission("bending.fire.lightningbending", true);
			perms.setPermission("aranarth.toggle.msg", true);
			perms.setPermission("aranarth.toggle.chat", true);
			perms.setPermission("aranarth.tphere", true);
		} else {
			return;
		}

		// King
		if (rank >= 7) {
			perms.setPermission("bending.earth.lavabending", true);
			perms.setPermission("bending.ability.firecomet", true);
			perms.setPermission("aranarth.toggle.tp", true);
		} else {
			return;
		}

		// Emperor
		if (rank >= 8) {
			perms.setPermission("bending.fire.combustionbending", true);
			perms.setPermission("aranarth.nick.color", true);
		} else {
			return;
		}
	}

	/**
	 * Sets the permissions for the Saint donor ranks.
	 * @param perms The permissions the player will have access to.
	 * @param saintRank The player's Saint rank.
	 */
	private static void setSaintPermissions(PermissionAttachment perms, int saintRank) {
		if (saintRank == 0) {
			return;
		}

		if (saintRank >= 1) {
			perms.setPermission("aranarth.chat.color", true);
			perms.setPermission("aranarth.hat", true);
			perms.setPermission("aranarth.trash", true);
			perms.setPermission("aranarth.back", true);
			perms.setPermission("aranarth.nick.color", true);
			perms.setPermission("aranarth.blacklist", true);
		} else {
			return;
		}

		if (saintRank >= 2) {
			perms.setPermission("aranarth.tables", true);
			perms.setPermission("aranarth.itemname", true);
			perms.setPermission("aranarth.chat.hex", true);
			perms.setPermission("aranarth.nick.hex", true);
		} else {
			return;
		}

		if (saintRank >= 3) {
			perms.setPermission("aranarth.nick.gradient", true);
			perms.setPermission("aranarth.itemname.gradient", true);
			perms.setPermission("aranarth.compress", true);
			perms.setPermission("aranarth.randomizer", true);
		} else {
			return;
		}
	}

	/**
	 * Sets the permissions for the Council staff ranks.
	 * @param perms The permissions the player will have access to.
	 * @param councilRank The player's Council rank.
	 */
	private static void setCouncilPermissions(PermissionAttachment perms, int councilRank) {
		if (councilRank == 0) {
			return;
		}

		if (councilRank >= 1) {
			perms.setPermission("aranarth.mute", true);
			perms.setPermission("aranarth.warn", true);
			perms.setPermission("aranarth.punishments", true);
		} else {
			return;
		}

		if (councilRank >= 2) {
			perms.setPermission("aranarth.ban", true);
			perms.setPermission("aranarth.give", true);
			perms.setPermission("aranarth.invsee", true);
			perms.setPermission("aranarth.spy", true);
			perms.setPermission("aranarth.unmute", true);
			perms.setPermission("aranarth.whereis", true);
			perms.setPermission("aranarth.broadcast", true);
		} else {
			return;
		}

		if (councilRank >= 3) {
			perms.setPermission("aranarth.unban", true);
			perms.setPermission("aranarth.give", true);
			perms.setPermission("aranarth.rankset", true);
			perms.setPermission("aranarth.protect.bypass", true);
			perms.setPermission("aranarth.warp.modify", true);
			perms.setPermission("aranarth.avatar.set", true);
			setSaintPermissions(perms, 3);
			setArchitectPermissions(perms, 1);
		} else {
			return;
		}
	}

	/**
	 * Sets the permissions for the Architect staff ranks.
	 * @param perms The permissions the player will have access to.
	 * @param architectRank The player's Architect rank.
	 */
	private static void setArchitectPermissions(PermissionAttachment perms, int architectRank) {

	}

}
