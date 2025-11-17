package com.aearost.aranarthcore.commands;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Avatar;
import com.aearost.aranarthcore.utils.AranarthUtils;
import org.bukkit.command.CommandSender;

/**
 * Provides the list of avatars.
 */
public class CommandAvatar {

	/**
	 * @param sender The user that entered the command.
	 * @param args The arguments of the command.
	 */
	public static boolean onCommand(CommandSender sender, String[] args) {
		// Lists the current avatar
		if (args.length == 0) {
			// Avatar XXX, Air Nomads, Since Obscurvor 27, 105


//			List<Avatar> avatars = AranarthUtils.getAvatars();
//			if (avatars.isEmpty()) {
//				sender.sendMessage(ChatUtils.chatMessage("&7There are no avatars yet"));
//				return true;
//			} else {
//				Avatar currentAvatar = avatars.get(avatars.size() - 1);
//				sender.sendMessage(ChatUtils.translateToColor("&8      - - - &6&lAvatars of Aranarth &8- - -"));
//				sender.sendMessage(ChatUtils.translateToColor("&6Current Avatar: " + getAvatarMessage(currentAvatar)));
//
//				for (int i = 0; i < avatars.size(); i++) {
//					Avatar avatar = avatars.get(i);
//					sender.sendMessage(ChatUtils.translateToColor("&8[" + i + "&8] " + getAvatarMessage(avatar)));
//				}
//			}
		}

		return false;
	}

	private static String getAvatarMessage(Avatar avatar) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(avatar.getUuid());
		String nickname = aranarthPlayer.getNickname();
//		int startMonth = avatar.getStart().getMonthValue();

		String element = avatar.getElement();
		String nation = switch (element) {
			case "water" -> "&bWater Tribe";
			case "earth" -> "&aEarth Kingdom";
			case "fire" -> "&cFire Nation";
			case "air" -> "&7Air Nomad";
			default -> "Nationless";
		};

		return "slay";

		// Have to decide if I want to track irl date or in-game date
		// Also decide what to do with displaying the avatars but probably the avatar room

//		String message = nickname + "&7, &r" + nation + "&7,  &7| &e" + startDate + "&7, deceased &e" + endDat + " &7| ";return null
	}

	public String getDurationOfReign(Avatar avatar) {

		// Use numeric format of string to represent the in-game days
		// i.e 0100100105 for Ignivor the 1st in year 105 (01 for the month, 001 for the day, 00105 for year 105)

		return null;
	}

}
