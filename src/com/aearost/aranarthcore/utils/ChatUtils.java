package com.aearost.aranarthcore.utils;

import org.bukkit.ChatColor;

import com.aearost.aranarthcore.enums.SpecialDay;

/**
 * Provides utility methods to facilitate the formatting of all chat related
 * content.
 * 
 * @author Aearost
 *
 */
public class ChatUtils {

	/**
	 * Allows the formatting of messages to contain Minecraft colors, and begin with
	 * the AranarthCore prefix.
	 * 
	 * @param msg
	 * @return
	 */
	public static String chatMessage(String msg) {
		return ChatColor.translateAlternateColorCodes('&', "&8&l[&6&lAranarthCore&8&l] &r" + msg);
	}
	
	/**
	 * Allows the formatting of messages to contain Minecraft colors, and begin with
	 * the AranarthCore prefix, specifically for errors
	 * 
	 * @param msg
	 * @return
	 */
	public static String chatMessageError(String msg) {
		return ChatColor.translateAlternateColorCodes('&', "&8&l[&6&lAranarthCore&8&l] &r&c" + msg);
	}

	/**
	 * Allows the formatting of messages to contain Minecraft colors
	 * 
	 * @param msg
	 * @return
	 */
	public static String translateToColor(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
	}

	/**
	 * Removes the styling from Strings.
	 * 
	 * @param msg
	 * @return
	 */
	public static String stripColor(String msg) {
		String colorStripped = ChatColor.stripColor(msg);
		while (colorStripped.startsWith("&")) {
			colorStripped = colorStripped.substring(2);
		}
		return colorStripped;
	}
	
	public static String getFormattedItemName(String nameToFormat) {
		String[] words = nameToFormat.toLowerCase().split("_");
		String fullItemName = "";
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String formattedWord = "";
			// If it shouldn't be capitalized
			if (word.equals("the") || word.equals("of") || word.equals("and") || word.equals("a") || word.equals("on")) {
				formattedWord = word;
			} else {
				formattedWord = Character.toUpperCase(word.charAt(0)) + word.substring(1);
			}
			
			if (i == words.length - 1) {
				fullItemName += formattedWord;
				break;
			} else {
				fullItemName += formattedWord + " ";
			}
		}
		return fullItemName;
	}
	
	public static String getSpecialJoinMessage(String displayName, SpecialDay specialDay) {
		if (specialDay == SpecialDay.VALENTINES) {
			return displayName + " has joined!";
		} else {
			return displayName;
		}
	}
	
	public static String getSpecialQuitMessage(String displayName, SpecialDay specialDay) {
		return displayName + " has quit!";
	}
	
}
