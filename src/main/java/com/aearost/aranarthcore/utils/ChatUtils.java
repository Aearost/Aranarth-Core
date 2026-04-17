package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.SpecialDay;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Dominion;
import com.aearost.aranarthcore.objects.DominionRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.*;
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
				double x = Math.min(1.0, (i - startOfSection) / (double) (endOfSection - startOfSection));

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
	 * Builds a display string where each saved gradient hex code is shown as plain text
	 * colored in its own color (e.g. "#FF0000" rendered in red).
	 *
	 * @param gradientColors Comma-separated hex codes (e.g. "#FF0000,#00FF00").
	 * @return A pre-escaped string ready to pass into chatMessage().
	 */
	public static String formatGradientColorsDisplay(String gradientColors) {
		String[] colorArray = gradientColors.split(",");
		StringBuilder formatted = new StringBuilder();
		for (int i = 0; i < colorArray.length; i++) {
			String color = colorArray[i]; // e.g. "#FF0000"
			String escape = ChatColor.of(color) + "";
			// escape is the §-prefixed sequence; inserting it between "#" and the hex digits
			// means checkForHex won't re-match the display text (pattern needs # followed
			// immediately by 6 hex chars, but here it's followed by a § escape char).
			formatted.append(escape).append("#").append(escape).append(color.substring(1));
			if (i < colorArray.length - 1) {
				formatted.append(ChatColor.GRAY).append(", ");
			}
		}
		return formatted.toString();
	}

	/**
	 * Removes the formatting from messages.
	 * 
	 * @param msg The message to remove formatting.
	 * @return The message without the formatting.
	 */
	public static String stripColorFormatting(String msg) {
		// Removes basic color codes
		String colorStripped = ChatColor.stripColor(msg);
		if (colorStripped.contains("&")) {
			String pattern = "&[0-9a-fk-or]";
			colorStripped = colorStripped.replaceAll(pattern, "");
		}

		// Removes any manually added hex codes
		if (colorStripped.contains("#")) {
			String pattern = "#.{6}";
			colorStripped = colorStripped.replaceAll(pattern, "");
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
			messages[1] = "The caroling is about to begin, thanks, " + displayName + "&7!";
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
			messages[1] = displayName + " &7has left, along with their love...";
			messages[2] = "The love story of " + displayName + " &7is over";
			messages[3] = "Hearts are broken, as " + displayName + " &7has departed";
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
	public static String formatChatPrefix(OfflinePlayer player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		String nickname = aranarthPlayer.getNickname();
		String prefix = "&l⊰&r";

		prefix += AranarthUtils.getCouncilRank(aranarthPlayer);
		prefix += AranarthUtils.getArchitectRank(aranarthPlayer);
		prefix += AranarthUtils.getSaintRank(aranarthPlayer);
		prefix += AranarthUtils.getAvatarRank(aranarthPlayer);

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
	 * Provides the formatted prefix and the name of the player.
	 * @param uuid The UUID of the player.
	 * @return The formatted prefix and the name of the player.
	 */
	public static String providePrefixAndName(UUID uuid) {
		String prefixAndName = ChatUtils.formatChatPrefix(Bukkit.getOfflinePlayer(uuid));
		prefixAndName = prefixAndName.substring(5, prefixAndName.length() - 1);
		prefixAndName = prefixAndName.substring(0, prefixAndName.length() - 7);
		return prefixAndName;
	}

	/**
	 * Formats the player's message based on their permissions.
	 * @param player The player sending the message.
	 * @param msg The message to be formatted.
	 * @return The formatted message.
	 */
	public static String formatChatMessage(Player player, String msg) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.isGradientChatEnabled() && !aranarthPlayer.getGradientChatColors().isEmpty()) {
			String gradientMsg = translateToGradientPreservingUrls(aranarthPlayer.getGradientChatColors(), msg, aranarthPlayer.isGradientChatBold());
			if (gradientMsg != null) {
				return gradientMsg;
			}
		}
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
			tips.add("&7&oNeed some materials? Gather them in &e&o/resource");
			tips.add("&7&oGet your roles in &5&oDiscord &7&owith &e&o/discord link");
			tips.add("&7&oIf you're confused with &6&oAranarth&7&o, go to &e&o/warp Tutorial");
			tips.add("&7&oInterested in special &d&operks? &7&oCheck out &e&o/store");
			tips.add("&7&oDon't forget to use &e&o/vote &7&oto get daily &a&oVote Crate Keys");
			tips.add("&7&oFound a &c&obug &7&oor have a &6&osuggestion&7&o? Log it in our &5&oDiscord");
			tips.add("&7&oSell your items at &e&o/warp Market &7&oto earn &a&omoney");
			tips.add("&7&oView the available &d&oin-game ranks &7&oat &e&o/ranks");
			tips.add("&7&oBe sure to follow the rules seen in &e&o/rules");
			tips.add("&7&oSpend your &a&ovote points &7&oin the &e&o/voteshop");
			tips.add("&7&oWant to create your &e&o/shop&7? Reach out to &6&oThe Council");

			Collections.shuffle(tips);
		}

		Bukkit.broadcastMessage(ChatUtils.chatMessage(tips.get(tipIndex)));
		tipIndex++;

		if (tipIndex == tips.size()) {
			tipIndex = 0;

			Collections.shuffle(tips);
		}
	}

	/**
	 * Sends a private message from the player to the target.
	 * @param player The player sending the message.
	 * @param target The player receiving the message.
	 * @param args The arguments of the command.
	 * @param isReply Whether the message is a reply to a previous message.
	 */
	public static void sendPrivateMessage(Player player, Player target, String[] args, boolean isReply) {
		int startIndex = isReply ? 0 : 1;
		StringBuilder msg = new StringBuilder();
		for (int i = startIndex; i < args.length; i++) {
			msg.append(args[i]);
			if (i < args.length - 1) {
				msg.append(" ");
			}
		}
		String assembledMsg = msg.toString();

		// If sending the message to yourself
		if (player.getUniqueId().equals(target.getUniqueId())) {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			String prefixStart = "&7⊰&r";
			String prefixEnd = "&7⊱&r";
			String senderPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oTo Yourself" + prefixEnd + " &7&o>> &e");
			// Formats to color if the player sending has the permissions (no gradient for private messages)
			String formattedMsg;
			if (player.hasPermission("aranarth.chat.hex")) {
				formattedMsg = ChatUtils.translateToColor(assembledMsg);
			} else if (player.hasPermission("aranarth.chat.color")) {
				formattedMsg = ChatUtils.playerColorChat(assembledMsg);
			} else {
				formattedMsg = assembledMsg;
			}
			player.sendMessage(ChatUtils.translateToColor(senderPrefix + formattedMsg));
		} else {
			AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
			AranarthPlayer targetAranarthPlayer = AranarthUtils.getPlayer(target.getUniqueId());
			String prefixStart = "&7⊰&r";
			String prefixEnd = "&7⊱&r";
			String senderPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oTo: &r&e" + targetAranarthPlayer.getNickname() + prefixEnd + " &7&o>> ");
			String targetPrefix = ChatUtils.translateToColor(prefixStart + "&7&l&oFrom: &r&e" + aranarthPlayer.getNickname() + prefixEnd + " &7&o>> &e&o");

			// Formats to color if the player sending has the permissions (no gradient for private messages)
			String formattedMsg;
			if (player.hasPermission("aranarth.chat.hex")) {
				formattedMsg = ChatUtils.translateToColor(assembledMsg);
			} else if (player.hasPermission("aranarth.chat.color")) {
				formattedMsg = ChatUtils.playerColorChat(assembledMsg);
			} else {
				formattedMsg = assembledMsg;
			}

			player.sendMessage(ChatUtils.translateToColor(senderPrefix + formattedMsg));
			target.sendMessage(ChatUtils.translateToColor(targetPrefix + formattedMsg));

			targetAranarthPlayer.setLastReceivedMessage(player.getUniqueId());
			AranarthUtils.setPlayer(target.getUniqueId(), targetAranarthPlayer);

			String adminPrefix = prefixStart + "&r&e" + aranarthPlayer.getNickname() + " &7&o>> &r&e&o" + targetAranarthPlayer.getNickname() + prefixEnd + " &c&o";
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				AranarthPlayer onlineAranarthPlayer = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
				if (onlineAranarthPlayer.isInAdminMode()) {
					if (!player.getUniqueId().equals(onlinePlayer.getUniqueId()) && !target.getUniqueId().equals(onlinePlayer.getUniqueId())) {
						onlinePlayer.sendMessage(ChatUtils.translateToColor("&8&l[&4&lSPY&8&l] " + adminPrefix + formattedMsg));
					}
				}
			}
		}
	}

	/**
	 * Builds a clickable chat component that suggests a command on click.
	 *
	 * @param displayComponent The visible text in chat (supports & color codes)
	 * @param hoverText The tooltip shown on hover (supports & color codes)
	 * @param command The command to suggest or run (include the leading /)
	 * @param suggest true = fills chat bar, false = executes immediately
	 */
	public static Component clickableCommand(Component displayComponent, String hoverText, String command, boolean suggest) {
		Component hover = LegacyComponentSerializer.legacySection().deserialize(hoverText);
		return displayComponent.hoverEvent(HoverEvent.showText(hover))
								.clickEvent(suggest ? ClickEvent.suggestCommand(command) : ClickEvent.runCommand(command)
		);
	}

	/**
	 * Builds a clickable chat component that opens a URL in the player's browser on click.
	 *
	 * @param displayComponent The visible text in chat
	 * @param hoverText The tooltip shown on hover (supports § color codes)
	 * @param url The URL to open
	 */
	public static Component clickableUrl(Component displayComponent, String hoverText, String url) {
		Component hover = LegacyComponentSerializer.legacySection().deserialize(hoverText);
		return displayComponent.hoverEvent(HoverEvent.showText(hover))
								.clickEvent(ClickEvent.openUrl(url));
	}

	private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\u00A7]+");

	/**
	 * Applies gradient formatting to a message while keeping any URLs intact as plain text
	 * so that {@link #buildMessageWithUrls} can still detect and make them clickable.
	 *
	 * @param gradientColors Comma-separated hex color string.
	 * @param msg The raw (unformatted) message.
	 * @param isBold Whether to apply bold formatting.
	 * @return The formatted string, or null if the message cannot be gradient-formatted.
	 */
	private static String translateToGradientPreservingUrls(String gradientColors, String msg, boolean isBold) {
		Matcher matcher = URL_PATTERN.matcher(msg);
		if (!matcher.find()) {
			return translateToGradient(gradientColors, msg, isBold);
		}
		StringBuilder result = new StringBuilder();
		int lastEnd = 0;
		matcher.reset();
		while (matcher.find()) {
			if (matcher.start() > lastEnd) {
				String textPart = msg.substring(lastEnd, matcher.start());
				String gradientPart = translateToGradient(gradientColors, textPart, isBold);
				result.append(gradientPart != null ? gradientPart : textPart);
			}
			result.append(matcher.group());
			lastEnd = matcher.end();
		}
		if (lastEnd < msg.length()) {
			String textPart = msg.substring(lastEnd);
			String gradientPart = translateToGradient(gradientColors, textPart, isBold);
			result.append(gradientPart != null ? gradientPart : textPart);
		}
		return result.toString();
	}

	/**
	 * Deserializes a legacy-formatted chat message into a Component, replacing any URLs with
	 * clickable components that open the link in the player's browser.
	 *
	 * @param legacyMessage The message string with § color codes
	 * @return A Component with embedded clickable URL components
	 */
	public static Component buildMessageWithUrls(String legacyMessage) {
		Matcher matcher = URL_PATTERN.matcher(legacyMessage);
		Component result = Component.empty();
		int lastEnd = 0;
		String hoverText = translateToColor("&7Open the link in your browser");

		while (matcher.find()) {
			// Append any text before this URL
			if (matcher.start() > lastEnd) {
				result = result.append(LegacyComponentSerializer.legacySection()
						.deserialize(legacyMessage.substring(lastEnd, matcher.start())));
			}

			String url = matcher.group();
			Component urlComponent = LegacyComponentSerializer.legacySection().deserialize(url);
			result = result.append(clickableUrl(urlComponent, hoverText, url));
			lastEnd = matcher.end();
		}

		// Append any remaining text after the last URL
		if (lastEnd < legacyMessage.length()) {
			result = result.append(LegacyComponentSerializer.legacySection()
					.deserialize(legacyMessage.substring(lastEnd)));
		}

		return result;
	}

	/**
	 * Helper method to evaluate a dominion chat message.
	 * Handles toggling dominion chat, setting the chat type, and sending messages.
	 * @param player The player sending the message.
	 * @param args The arguments of the command.
	 * @param isSingleMessage If the message is one single message or if messages are toggled.
	 */
	public static void evaluateDominionMessage(Player player, String[] args, boolean isSingleMessage) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		Dominion dominion = DominionUtils.getPlayerDominion(player.getUniqueId());

		if (dominion == null) {
			player.sendMessage(ChatUtils.chatMessage("&cYou are not in a Dominion!"));
			return;
		}

		if (isSingleMessage) {
			if (args.length == 1) {
				String onOrOffMessage = aranarthPlayer.isInDominionChat() ? "&coff" : "&aon";
				player.sendMessage(ChatUtils.chatMessage("&7You have toggled " + onOrOffMessage + " &7Dominion Chat"));
				aranarthPlayer.setInDominionChat(!aranarthPlayer.isInDominionChat());
				AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				return;
			}

			if (args.length == 2) {
				String typeArg = args[1].toLowerCase();
				if (typeArg.equals("dominion") || typeArg.equals("ally") || typeArg.equals("truce") || typeArg.equals("allytruce")) {
					aranarthPlayer.setDominionChatType(typeArg);
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
					player.sendMessage(ChatUtils.chatMessage("&7Dominion chat type set to &e" + typeArg));
					return;
				}
			}
		}

		int startIndex = isSingleMessage ? 1 : 0;
		StringBuilder msg = new StringBuilder();
		for (int i = startIndex; i < args.length; i++) {
			msg.append(args[i]);
			if (i < args.length - 1) {
				msg.append(" ");
			}
		}
		String assembledMsg = msg.toString();

		DominionRank rank = dominion.getMemberRank(player.getUniqueId());
		if (rank == null) {
			rank = DominionRank.NEWCOMER;
		}
		String rankColor = DominionUtils.getRankColor(rank);
		String nickname = aranarthPlayer.getNickname();
		String dominionName = ChatUtils.stripColorFormatting(dominion.getName());
		String chatType = aranarthPlayer.getDominionChatType();

		String typeLabel = switch (chatType) {
			case "ally" -> "&5[Ally] ";
			case "truce" -> "&d[Truce] ";
			case "allytruce" -> "&5[AllyTruce] ";
			default -> "";
		};

		String prefixStart = "&7⊰&r";
		String prefixEnd = "&7⊱&r";
		String prefixReceive = ChatUtils.translateToColor(prefixStart + typeLabel + "&e" + dominionName + " &7| " + DominionUtils.getFormattedRankName(rank) + " &f" + nickname + prefixEnd + " &7&o>> &7&o");

		List<UUID> recipientLeaders = new ArrayList<>();
		recipientLeaders.add(dominion.getLeader());

		if (chatType.equals("ally") || chatType.equals("allytruce")) {
			for (UUID alliedLeader : dominion.getAllied()) {
				Dominion alliedDominion = DominionUtils.getPlayerDominion(alliedLeader);
				if (alliedDominion != null && DominionUtils.areAllied(dominion, alliedDominion)) {
					recipientLeaders.add(alliedLeader);
				}
			}
		}

		if (chatType.equals("truce") || chatType.equals("allytruce")) {
			for (UUID trucedLeader : dominion.getTruced()) {
				Dominion trucedDominion = DominionUtils.getPlayerDominion(trucedLeader);
				if (trucedDominion != null && DominionUtils.areTruced(dominion, trucedDominion)) {
					if (!recipientLeaders.contains(trucedLeader)) {
						recipientLeaders.add(trucedLeader);
					}
				}
			}
		}

		List<UUID> recipientUuids = new ArrayList<>();
		for (UUID leaderUuid : recipientLeaders) {
			Dominion recipientDominion = DominionUtils.getPlayerDominion(leaderUuid);
			if (recipientDominion != null) {
				recipientUuids.addAll(recipientDominion.getMembers());
			}
		}

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (recipientUuids.contains(onlinePlayer.getUniqueId())) {
				onlinePlayer.sendMessage(ChatUtils.translateToColor(prefixReceive + assembledMsg));
			}
		}
	}

	/**
	 * Helper method to evaluate a message for council members.
	 * @param sender The sender of the message.
	 * @param args The arguments of the command.
	 * @param isSingleMessage If the message is one single message or if messages are toggled.
	 */
	public static void evaluateCouncilMessage(CommandSender sender, String[] args, boolean isSingleMessage) {
		if (isSingleMessage) {
			if (args.length == 1) {
				if (sender instanceof Player player) {
					AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
					String onOrOffMessage = aranarthPlayer.isInCouncilChat() ? "&coff" : "&aon";
					sender.sendMessage(ChatUtils.chatMessage("&7You have toggled " + onOrOffMessage + " &7Council Chat"));
					aranarthPlayer.setInCouncilChat(!aranarthPlayer.isInCouncilChat());
					AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
				} else {
					sender.sendMessage(ChatUtils.chatMessage("&cOnly players can toggle Council Chat"));
				}
				return;
			}
		}

		int startIndex = isSingleMessage ? 1 : 0;
		StringBuilder msg = new StringBuilder();
		for (int i = startIndex; i < args.length; i++) {
			msg.append(args[i]);
			if (i < args.length - 1) {
				msg.append(" ");
			}
		}
		String assembledMsg = msg.toString();

		String nickname = "";
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			nickname = AranarthUtils.getPlayer(player.getUniqueId()).getNickname();
		} else {
			nickname = "&4&lCONSOLE";
		}

		String prefixStart = "&7⊰&r";
		String prefixEnd = "&7⊱&r";
		String prefixReceive = ChatUtils.translateToColor(prefixStart + "&8&lCouncil &e" + nickname + prefixEnd + " &7&o>> &6&o");

		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			AranarthPlayer onlineAranarthPlayer = AranarthUtils.getPlayer(onlinePlayer.getUniqueId());
			if (onlineAranarthPlayer.getCouncilRank() > 0) {
				onlinePlayer.sendMessage(ChatUtils.translateToColor(prefixReceive + assembledMsg));
			}
		}
	}
}
