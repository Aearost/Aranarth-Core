package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility methods to facilitate the formatting of all chat related content.
 */
public class ChatUtils {

	private static final List<String> tips = new ArrayList<>();
	private static int tipIndex = 0;

	/**
	 * Allows messages to contain color codes and begin with the AranarthCore prefix.
	 * 
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String chatMessage(String msg) {
		msg = checkForHex(msg);
		return ChatColor.translateAlternateColorCodes('&', "&8&l[&6&lAC&8&l] &r" + msg);
	}

	/**
	 * Allows messages to contain color and hex codes.
	 *
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String translateToColor(String msg) {
		msg = checkForHex(msg);
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	/**
	 * Allows messages to contain color codes.
	 * To be used only for players that have color chat but not gradient chat permissions.
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String playerColorChat(String msg) {
		if (msg.contains("#")) {
			return null;
		}
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	/**
	 * Allows messages to contain color codes.
	 *
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String translateToGradient(String gradientColors, String msg, boolean isBold) {
		// The text must be without colour formatting
		if (msg.contains("#") || msg.contains("&")) {
			return null;
		}

		String[] colors = gradientColors.split(",");
		int numColors = colors.length;
		int msgLength = msg.length();

		// Ensure colors are valid hex codes
		for (String color : colors) {
			if (!color.startsWith("#") || color.length() != 7 || !color.substring(1).matches("[0-9A-Fa-f]+")) {
				return null;
			}
		}

		// If the message has fewer characters than colors, use the first color for all characters
		if (msgLength < numColors) {
			StringBuilder result = new StringBuilder();
			String firstColor = colors[0];
			if (isBold) {
				firstColor += "&l";
			}
			for (char c : msg.toCharArray()) {
				result.append(firstColor).append(c); // Apply the first color to all characters
			}
			msg = result.toString();
		} else {
			// Calculate the transition points based on the number of colors and message length
			int sectionSize = msgLength / (numColors - 1); // Number of characters per gradient section
			StringBuilder result = new StringBuilder();

			for (int i = 0; i < msgLength; i++) {
				// Determine which two colors we're interpolating between
				int colorIndex = Math.min(i / sectionSize, numColors - 2); // Index for the left color
				String startColor = colors[colorIndex];
				String endColor = colors[colorIndex + 1];

				// Calculate the interpolation factor between the two colors (0 to 1)
				int startOfSection = colorIndex * sectionSize;
				int endOfSection = (colorIndex + 1) * sectionSize;
				double x = (i - startOfSection) / (double) (endOfSection - startOfSection);

				// Interpolate the color at position i
				String interpolatedColor = interpolateColor(startColor, endColor, x);
				if (isBold) {
					interpolatedColor += "&l";
				}
				result.append(interpolatedColor).append(msg.charAt(i)); // Apply color to the character
			}
			msg = result.toString();
		}
		msg = checkForHex(msg);
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	private static String interpolateColor(String startHex, String endHex, double x) {
		int r1 = Integer.parseInt(startHex.substring(1, 3), 16);
		int g1 = Integer.parseInt(startHex.substring(3, 5), 16);
		int b1 = Integer.parseInt(startHex.substring(5, 7), 16);

		int r2 = Integer.parseInt(endHex.substring(1, 3), 16);
		int g2 = Integer.parseInt(endHex.substring(3, 5), 16);
		int b2 = Integer.parseInt(endHex.substring(5, 7), 16);

		// Linear interpolation for each color component
		int r = (int) (r1 + x * (r2 - r1));
		int g = (int) (g1 + x * (g2 - g1));
		int b = (int) (b1 + x * (b2 - b1));

		// Return the interpolated color in hex
		return String.format("#%02X%02X%02X", r, g, b);
	}

	private static String checkForHex(String msg) {
		Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
		Matcher matcher = pattern.matcher(msg);
		while (matcher.find()) {
			// Gets the color code and replaces it with the actual color
			String color = msg.substring(matcher.start(), matcher.end());
			msg = msg.replace(color, ChatColor.of(color) + "");
			matcher = pattern.matcher(msg);
		}
		return msg;
	}

	/**
	 * Removes the formatting from messages.
	 * 
	 * @param msg The message to remove formatting.
	 * @return The message without the formatting.
	 */
	public static String stripColorFormatting(String msg) {
		String colorStripped = ChatColor.stripColor(msg);
		while (colorStripped.startsWith("&")) {
			colorStripped = colorStripped.substring(2);
		}
		return colorStripped;
	}

	/**
	 * Removes all special characters from the input string.
	 * @param value The input string.
	 * @return The input string with all special characters removed.
	 */
	public static String removeSpecialCharacters(String value) {
		return value.replaceAll("[^a-zA-Z0-9\\s&§#]", "");
	}

	/**
	 * Formats material names to properly capitalized strings.
	 *
	 * @param nameToFormat The item name to be formatted.
	 * @return The formatted item name.
	 */
	public static String getFormattedItemName(String nameToFormat) {
		String[] words = nameToFormat.toLowerCase().split("_");
		StringBuilder fullItemName = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String formattedWord;
			// If it shouldn't be capitalized
			if (word.equals("the") || word.equals("of") || word.equals("and") || word.equals("a") || word.equals("on")) {
				formattedWord = word;
			} else {
				formattedWord = Character.toUpperCase(word.charAt(0)) + word.substring(1);
			}
			
			if (i == words.length - 1) {
				fullItemName.append(formattedWord);
				break;
			} else {
				fullItemName.append(formattedWord).append(" ");
			}
		}
		return fullItemName.toString();
	}

	/**
	 * Fetches a custom holiday server join message based on the current date.
	 *
	 * @param displayName The player's displayed name.
	 * @param specialDay The date that the message should be reflective of.
	 * @return The randomly selected and formatted custom join message.
	 */
	public static String getSpecialJoinMessage(String displayName, SpecialDay specialDay) {
		Random random = new Random();
		int randomInt = random.nextInt(4);
		String[] messages = new String[4];
		
		if (specialDay == SpecialDay.VALENTINES) {
			messages[0] = "Cupid must have striked " + displayName + "&7, say hello!";
			messages[1] = "Will " + displayName + " &7be your Valentine?";
			messages[2] = "Your heart just skipped a beat, " + displayName + " &7has joined!";
			messages[3] = "Roses are red, I can't rhyme, " + displayName + " &7is here!";
		} else if (specialDay == SpecialDay.EASTER) {
			messages[0] = displayName + " &7has joined the egg hunt!";
			messages[1] = displayName + " &7thinks they're the Easter bunny...";
			messages[2] = "Spring has sprung now that " + displayName + " &7is here!";
			messages[3] = "Hippity-hoppity, it's " + displayName + "&7!";
		} else if (specialDay == SpecialDay.HALLOWEEN) {
			messages[0] = "Double double toil and trouble! " + displayName + " &7is here!";
			messages[1] = displayName + " &7is here to haunt you! BOO!";
			messages[2] = "Trick or treat! It's " + displayName + "&7...";
			messages[3] = displayName + " &7is ready to scare the monsters...";
		} else if (specialDay == SpecialDay.CHRISTMAS) {
			messages[0] = displayName + " &7is looking for someone under the mistletoe!";
			messages[1] = "The caroling is about to begin, thanks" + displayName + "&7!";
			messages[2] = displayName + " &7is on Santa's nice list...";
			messages[3] = "Is that Santa? Nevermind, it's just " + displayName;
		} else {
			return displayName;
		}
		return messages[randomInt];
	}

	/**
	 * Fetches a custom holiday server quit message based on the current date.
	 *
	 * @param displayName The player's displayed name.
	 * @param specialDay The date that the message should be reflective of.
	 * @return The randomly selected and formatted custom quit message.
	 */
	public static String getSpecialQuitMessage(String displayName, SpecialDay specialDay) {
		Random random = new Random();
		int randomInt = random.nextInt(4);
		String[] messages = new String[4];
		
		if (specialDay == SpecialDay.VALENTINES) {
			messages[0] = displayName + " &7has left to spend time with their Valentine";
			messages[1] = displayName + " &7 has left, along with their love...";
			messages[2] = "The love story of " + displayName + " &7 is over";
			messages[3] = "Hearts are broken; " + displayName + " &7has departed";
		} else if (specialDay == SpecialDay.EASTER) {
			messages[0] = displayName + " &7has hopped off the server!";
			messages[1] = displayName + " &7took all of the Easter eggs and ran away!";
			messages[2] = displayName + " &7ate too much chocolate and had to leave...";
			messages[3] = displayName + " &7bounced away into the distance...";
		} else if (specialDay == SpecialDay.HALLOWEEN) {
			messages[0] = displayName + " &7has closed their coffin";
			messages[1] = displayName + " &7ate too much candy and felt sick...";
			messages[2] = displayName + " &7is not a zombie, they just disconnected";
			messages[3] = displayName + " &7fell for one of their own tricks!";
		} else if (specialDay == SpecialDay.CHRISTMAS) {
			messages[0] = displayName + " &7has to unwrap all their gifts!";
			messages[1] = "Santa's helper, " + displayName + "&7, is offline";
			messages[2] = "It's the First Noel without " + displayName + "&7...";
			messages[3] = "It's a Silent Night now that " + displayName + " &7has left...";
		} else {
			return displayName;
		}
		return messages[randomInt];
	}

	/**
	 * Formats the player's prefix based on their current ranks.
	 * @param player The player sending the message.
	 * @return The formatted prefix.
	 */
	public static String formatChatPrefix(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		String nickname = aranarthPlayer.getNickname();
		String prefix = "&l⊰&r";

		prefix += AranarthUtils.getCouncilRank(aranarthPlayer);
		prefix += AranarthUtils.getArchitectRank(aranarthPlayer);
		prefix += AranarthUtils.getSaintRank(aranarthPlayer);
		if (!prefix.equals("&l⊰&r")) {
			prefix += " ";
		}
		prefix += AranarthUtils.getRank(aranarthPlayer);

		if (nickname != null && !nickname.isEmpty()) {
			prefix += nickname + "&r";
		} else {
			prefix += player.getName();
		}

		prefix += "&l⊱ &r";
		prefix = ChatUtils.translateToColor(prefix);
		return prefix;
	}

	/**
	 * Formats the player's message based on their permissions.
	 * @param player The player sending the message.
	 * @param msg The message to be formatted.
	 * @return The formatted message.
	 */
	public static String formatChatMessage(Player player, String msg) {
		if (player.hasPermission("aranarth.chat.hex")) {
			msg = ChatUtils.translateToColor(msg);
		} else if (player.hasPermission("aranarth.chat.color")) {
			msg = ChatUtils.playerColorChat(msg);
		}
		return msg;
	}

	/**
	 * Confirms if the player is currently muted.
	 * @param player The player to be verified.
	 * @return Confirmation if the player is currently muted.
	 */
	public static boolean isPlayerMuted(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());

		// YYMMDDhhmm
		String muteEndDate = aranarthPlayer.getMuteEndDate();

		if (muteEndDate.isEmpty()) {
			return false;
		}
		if (muteEndDate.equals("none")) {
			return true;
		}

		LocalDateTime currentDate = LocalDateTime.now();
		LocalDateTime definedMuteDate = getMuteEndAsLocalDateTime(aranarthPlayer);

		return definedMuteDate.isBefore(currentDate);
	}

	/**
	 * Provides the AranarthPlayer's mute end date as a LocalDateTime object.
	 * @param aranarthPlayer The player.
	 * @return The player's mute end date as a LocalDateTime object.
	 */
	public static LocalDateTime getMuteEndAsLocalDateTime(AranarthPlayer aranarthPlayer) {
		String muteEndDate = aranarthPlayer.getMuteEndDate();

		LocalDateTime definedMuteDate = null;
		try {
			int year = Integer.parseInt(muteEndDate.substring(0, 2));
			int month = Integer.parseInt(trimZero(muteEndDate.substring(2, 4)));
			int day = Integer.parseInt(trimZero(muteEndDate.substring(4, 6)));
			int hour = Integer.parseInt(trimZero(muteEndDate.substring(6, 8)));
			int minute = Integer.parseInt(trimZero(muteEndDate.substring(8, 10)));
			definedMuteDate = LocalDateTime.of(year, month, day, hour, minute);
		} catch (NumberFormatException e) {
			Bukkit.getLogger().info("Something went wrong with parsing the player's mute date...");
			return null;
		}
		return definedMuteDate;
	}

	/**
	 * Trims the leading zero if the value is only one digit.
	 * @param value The value.
	 * @return The trimmed value.
	 */
	private static String trimZero(String value) {
		if (value.startsWith("0")) {
			return value.substring(1);
		} else {
			return value;
		}
	}

	/**
	 * Handles sending the messages of automatic server tips.
	 */
	public static void sendServerTips() {
		if (tips.isEmpty()) {
			tips.add("&7&oNeed some materials? Gather them in the resource world at &e&o/ac resource");
			tips.add("&7&oBe sure to link your Discord with &e&o/discord link &7&oto get your roles in Discord");
			tips.add("&7&oConfused about Aranarth? Check out &e&o/ac warp tutorial &7&ofor some help");
			tips.add("&7&oInterested in some special perks? Check out our server store at &e&o/ac store");
			tips.add("&7&oDon't forget to use &e&o/ac vote &7&oto get your daily vote crate keys");
			tips.add("&7&oFound a bug or have an idea? Report it in our Discord server");
			tips.add("&7&oEarn money by selling your items at &e&o/ac warp market");
			tips.add("&7&oView the available in-game ranks at &e&o/ac ranks");
			tips.add("&7&oBe sure to follow the rules at &e&o/ac rules");

			Collections.shuffle(tips);
		}

		Bukkit.broadcastMessage(ChatUtils.chatMessage(tips.get(tipIndex)));
		tipIndex++;

		if (tipIndex == tips.size()) {
			tipIndex = 0;

			Collections.shuffle(tips);
		}
	}
}
