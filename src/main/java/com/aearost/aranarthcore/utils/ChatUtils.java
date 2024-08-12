package com.aearost.aranarthcore.utils;

import java.util.Random;

import org.bukkit.ChatColor;

import com.aearost.aranarthcore.enums.SpecialDay;

/**
 * Provides utility methods to facilitate the formatting of all chat related content.
 */
public class ChatUtils {

	/**
	 * Allows messages to contain color codes and begin with the AranarthCore prefix.
	 * 
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String chatMessage(String msg) {
		return ChatColor.translateAlternateColorCodes('&', "&8&l[&6&lAranarthCore&8&l] &r" + msg);
	}

	/**
	 * Allows messages to contain color codes.
	 * 
	 * @param msg The message to be formatted.
	 * @return The formatted chat message.
	 */
	public static String translateToColor(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
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
}
