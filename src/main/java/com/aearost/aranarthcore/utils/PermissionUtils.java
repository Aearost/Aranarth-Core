package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.objects.Perk;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.OfflineBendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

/**
 * Provides a large variety of utility methods for everything related to items and inventory.
 */
public class PermissionUtils {

	/**
	 * Centralizes all permissions logic being set.
	 * @param player The player.
	 */
	public static void evaluatePlayerPermissions(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		PermissionAttachment perms = player.addAttachment(AranarthCore.getInstance());

		setDefaultPermissions(player, perms);
		setRankPermissions(perms, aranarthPlayer.getRank());
		reEvaluateMonthlySaints(player);
		setSaintPermissions(perms, aranarthPlayer.getSaintRank());
		setCouncilPermissions(perms, aranarthPlayer.getCouncilRank());
		addPlayerPerks(perms, player);

		Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
		// If the player is the avatar
		if (currentAvatar != null && currentAvatar.getUuid().equals(player.getUniqueId())) {
			updateAvatarPermissions(player.getUniqueId(), false);
		}

		updateSubElements(player);

		Bukkit.getLogger().info(player.getName() + "'s permissions have been evaluated");
	}

	/**
	 * Updates the player's sub-elements based on their current permissions.
	 * @param player The player.
	 */
	public static void updateSubElements(Player player) {
		Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), new Runnable() {
			@Override
			public void run() {
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
					bendingPlayer.saveSubElements();
					bendingPlayer.saveElements();
				}
			}
		}, 1);
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
		perms.setPermission("bending.fire.bluefirebending", false);
		perms.setPermission("bending.earth.metalbending", false);
		perms.setPermission("bending.earth.lavabending", false);
		perms.setPermission("bending.earth.sandbending", false);
		perms.setPermission("bending.air.flight", false);
		perms.setPermission("bending.air.spiritual", false);
		perms.setPermission("bending.earth.sandbending", false);
		perms.setPermission("bending.water.bloodbending", false);
		perms.setPermission("bending.water.bloodbending.anytime", false);

		// Disable abilities
		perms.setPermission("bending.ability.waterarms", false);
		perms.setPermission("bending.ability.firecomet", false);
		perms.setPermission("bending.ability.metalclips", false);
		perms.setPermission("bending.ability.sonicblast", false);
		perms.setPermission("bending.ability.suffocate", false);
		perms.setPermission("bending.earth.lavaflux", false);
		perms.setPermission("bending.earth.fissure", false);

		// Enable all Aranarth abilities by default as sub-element permission takes precedence
		perms.setPermission("bending.ability.astralprojection", true);
		perms.setPermission("bending.ability.vinewhip", true);

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
		perms.setPermission("aranarth.warp.modify", false);
		perms.setPermission("aranarth.warn", false);
		perms.setPermission("aranarth.punishments", false);
		perms.setPermission("aranarth.avatar.set", false);
		perms.setPermission("aranarth.broadcast", false);
		perms.setPermission("aranarth.shulker", false);
		perms.setPermission("aranarth.inventory", false);
		perms.setPermission("aranarth.invisible_item_frame", false);
		perms.setPermission("aranarth.boost.modify", false);
		perms.setPermission("aranarth.vote.test", false);
		perms.setPermission("aranarth.shop.modify", false);
	}

	/**
	 * Sets the permissions for either removing an avatar or for a new avatar.
	 * @param uuid The player's UUID.
	 */
	public static void updateAvatarPermissions(UUID uuid, boolean isRemoval) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
		OfflineBendingPlayer bendingPlayer = BendingPlayer.getOfflineBendingPlayer(player.getName());

		if (bendingPlayer == null) {
			return;
		}

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

				// Allow manual element changes for the avatar
				perms.setPermission("bending.command.choose", true);
				perms.setPermission("bending.command.rechoose", true);

				// Removing avatar-exclusive permissions
				perms.setPermission("bending.avatar", false);
				perms.setPermission("bending.ability.avatarstate", false);
				perms.setPermission("bending.ability.elementsphere", false);
				perms.setPermission("bending.ability.elementsphere.air", false);
				perms.setPermission("bending.ability.elementsphere.earth", false);
				perms.setPermission("bending.ability.elementsphere.fire", false);
				perms.setPermission("bending.ability.elementsphere.water", false);
				perms.setPermission("bending.ability.elementsphere.stream", false);
				perms.setPermission("bending.ability.spiritbeam", false);
				perms.setPermission("bending.earth.lavaflux", true);

				evaluatePlayerPermissions((Player) player);
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
				if (subElement != Element.SubElement.BLOOD && subElement != Element.SubElement.FLIGHT) {
					if (subElement == Element.SubElement.BLUE_FIRE) {
						if (player.isOnline()) {
							Player onlinePlayer = player.getPlayer();
							if (!onlinePlayer.hasPermission("bending.fire.bluefirebending")) {
								continue;
							}
						}
					}

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
				perms.setPermission("bending.air.spiritual", true);

				// Enable all abilities
				perms.setPermission("bending.ability.waterarms", true);
				perms.setPermission("bending.ability.firecomet", true);
				perms.setPermission("bending.ability.metalclips", true);
				perms.setPermission("bending.ability.sonicblast", true);
				perms.setPermission("bending.ability.suffocate", true);

				// Adding avatar-exclusive permissions
				perms.setPermission("bending.avatar", true);
				perms.setPermission("bending.ability.avatarstate", true);
				perms.setPermission("bending.ability.elementsphere", true);
				perms.setPermission("bending.ability.spiritbeam", true);
				perms.setPermission("bending.ability.elementsphere.air", true);
				perms.setPermission("bending.ability.elementsphere.earth", true);
				perms.setPermission("bending.ability.elementsphere.fire", true);
				perms.setPermission("bending.ability.elementsphere.water", true);
				perms.setPermission("bending.ability.elementsphere.stream", true);
			}
		}
	}

	/**
	 * Adds the permissions for a player's additional perks.
	 * @param perms The permissions the player will have access to.
	 * @param player The player.
	 */
	private static void addPlayerPerks(PermissionAttachment perms, Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		HashMap<Perk, Integer> perks = aranarthPlayer.getPerks();

		// Compressor
		if (perks.get(Perk.COMPRESSOR) == 1) {
			perms.setPermission("aranarth.compress", true);
		}
		// Randomizer
		if (perks.get(Perk.RANDOMIZER) == 1) {
			perms.setPermission("aranarth.randomizer", true);
		}
		// Blacklist
		if (perks.get(Perk.BLACKLIST) == 1) {
			perms.setPermission("aranarth.blacklist", true);
		}
		// Tables
		if (perks.get(Perk.TABLES) == 1) {
			perms.setPermission("aranarth.tables", true);
		}
		// Itemname
		if (perks.get(Perk.ITEMNAME) == 1) {
			perms.setPermission("aranarth.itemname", true);
			perms.setPermission("aranarth.itemname.gradient", true);
		}
		// Chat
		if (perks.get(Perk.CHAT) == 1) {
			perms.setPermission("aranarth.chat.color", true);
			perms.setPermission("aranarth.chat.hex", true);
		}
		// Shulker
		if (perks.get(Perk.SHULKER) == 1) {
			perms.setPermission("aranarth.shulker", true);
		}
		// Inventory
		if (perks.get(Perk.INVENTORY) == 1) {
			perms.setPermission("aranarth.inventory", true);
		}
		// Homes
		if (perks.get(Perk.HOMES) != 0) {
            switch (perks.get(Perk.HOMES)) {
                case 6 -> perms.setPermission("aranarth.extrahomes.6", true);
                case 9 -> perms.setPermission("aranarth.extrahomes.9", true);
                case 12 -> perms.setPermission("aranarth.extrahomes.12", true);
                case 15 -> perms.setPermission("aranarth.extrahomes.15", true);
                default -> perms.setPermission("aranarth.extrahomes.3", true);
            }
		}
		// Item Frame
		if (perks.get(Perk.ITEMFRAME) == 1) {
			perms.setPermission("aranarth.invisible_item_frame", true);
		}
		// Blue Fire
		if (perks.get(Perk.BLUEFIRE) == 1) {
			if (aranarthPlayer.hasBlueFireDisabled()) {
				perms.setPermission("bending.fire.bluefirebending", false);
			}
			// Will default to be enabled, must be manually toggled off via /ac toggle bluefire
			else {
				perms.setPermission("bending.fire.bluefirebending", true);
			}
			updateSubElements(player);
		} else if (perks.get(Perk.BLUEFIRE) == 0) {
			perms.setPermission("bending.fire.bluefirebending", false);
			updateSubElements(player);
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
			perms.setPermission("aranarth.tphere", true);
			perms.setPermission("bending.command.preset.create.8", true);
		} else {
			return;
		}

		// Count
		if (rank >= 4) {
			perms.setPermission("bending.ability.sonicblast", true);
			perms.setPermission("aranarth.back", true);
			perms.setPermission("bending.command.preset.create.10", true);
		} else {
			return;
		}

		// Duke
		if (rank >= 5) {
			perms.setPermission("bending.ability.metalclips", true);
			perms.setPermission("bending.ability.suffocate", true);
			perms.setPermission("aranarth.nick", true);
			perms.setPermission("bending.command.preset.create.12", true);
		} else {
			return;
		}

		// Prince
		if (rank >= 6) {
			perms.setPermission("bending.fire.lightningbending", true);
			perms.setPermission("bending.air.spiritual", true);
			perms.setPermission("aranarth.toggle.msg", true);
			perms.setPermission("aranarth.toggle.tp", true);
			perms.setPermission("bending.command.preset.create.15", true);

		} else {
			return;
		}

		// King
		if (rank >= 7) {
			perms.setPermission("bending.earth.lavabending", true);
			perms.setPermission("bending.ability.firecomet", true);
			perms.setPermission("aranarth.toggle.chat", true);
			perms.setPermission("aranarth.nick.color", true);
			perms.setPermission("bending.earth.lavaflux", true);
			perms.setPermission("bending.earth.fissure", true);
			perms.setPermission("bending.command.preset.create.20", true);
		} else {
			return;
		}

		// Emperor
		if (rank >= 8) {
			perms.setPermission("bending.fire.combustionbending", true);
			perms.setPermission("bending.command.preset.create.25", true);
		} else {
			return;
		}
	}

	/**
	 * Re-evaluates the player's temporary saint status.
	 * @param player The player.
	 */
	public static void reEvaluateMonthlySaints(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.getSaintExpireDate() != 0) {
			Instant now = Instant.now();
			if (now.isAfter(Instant.ofEpochMilli(aranarthPlayer.getSaintExpireDate()))) {
				aranarthPlayer.setSaintExpireDate(0);
				aranarthPlayer.setSaintRank(0);
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				player.sendMessage(ChatUtils.chatMessage("&7Your monthly &dSaint &7rank has expired!"));
			}
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
			perms.setPermission("aranarth.nick", true);
			perms.setPermission("aranarth.nick.color", true);
			perms.setPermission("aranarth.blacklist", true);
			perms.setPermission("aranarth.tables", true);
		} else {
			return;
		}

		if (saintRank >= 2) {
			perms.setPermission("aranarth.itemname", true);
			perms.setPermission("aranarth.chat.hex", true);
			perms.setPermission("aranarth.nick.hex", true);
			perms.setPermission("aranarth.shulker", true);
		} else {
			return;
		}

		if (saintRank >= 3) {
			perms.setPermission("aranarth.nick.gradient", true);
			perms.setPermission("aranarth.itemname.gradient", true);
			perms.setPermission("aranarth.compress", true);
			perms.setPermission("aranarth.randomizer", true);
			perms.setPermission("aranarth.inventory", true);
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
			perms.setPermission("aranarth.nick", true);
			perms.setPermission("aranarth.nick.color", true);
			perms.setPermission("aranarth.nick.hex", true);
			perms.setPermission("aranarth.nick.gradient", true);
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
			perms.setPermission("aranarth.shop.modify", true);
		} else {
			return;
		}

		if (councilRank >= 3) {
			perms.setPermission("aranarth.unban", true);
			perms.setPermission("aranarth.give", true);
			perms.setPermission("aranarth.rankset", true);
			perms.setPermission("aranarth.warp.modify", true);
			perms.setPermission("aranarth.avatar.set", true);
			perms.setPermission("aranarth.boosts.modify", true);
			perms.setPermission("aranarth.vote.test", true);
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
		if (architectRank == 0) {
			return;
		}

		if (architectRank == 1) {
			perms.setPermission("worldedit.*", true);
		}
	}

}
