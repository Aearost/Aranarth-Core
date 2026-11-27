package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.RandomItem;
import com.aearost.aranarthcore.objects.RandomizerPreset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared logic for the Randomizer perk: parsing patterns, the on/off toggle, and saved presets.
 */
public class RandomizerUtils {

	public static final int MAX_PRESETS = 9;
	public static final int MAX_PATTERN_ITEMS = 36;
	private static final int MAX_PATTERN_LENGTH = 512;
	// The visible length of a preset name, and the length including its color codes
	private static final int MAX_NAME_LENGTH = 30;
	private static final int MAX_RAW_NAME_LENGTH = 150;

	private static final Set<UUID> awaitingPresetName = new HashSet<>();

	/**
	 * The result of parsing a pattern: either the items, or the lang key of the error.
	 */
	private record ParseResult(List<RandomItem> items, String errorKey) {}

	/**
	 * Parses a pattern such as 50%stone,50%andesite, or stone,andesite to split it evenly.
	 *
	 * @param pattern The pattern that was entered.
	 * @return The parsed items, or the lang key of the reason it is invalid.
	 */
	private static ParseResult parse(String pattern) {
		if (pattern == null || pattern.length() > MAX_PATTERN_LENGTH) {
			return new ParseResult(null, "randomizer.invalid_format");
		}
		if (!pattern.contains(",")) {
			return new ParseResult(null, "randomizer.need_multiple_items");
		}

		String[] components = pattern.split(",");
		if (components.length > MAX_PATTERN_ITEMS) {
			return new ParseResult(null, "randomizer.too_many_items");
		}
		List<Material> unspecifiedMaterials = new ArrayList<>();
		List<RandomItem> randomItems = new ArrayList<>();
		int specifiedPercentageSum = 0;
		for (String component : components) {
			String materialName;
			int percentage = 0;
			boolean hasPercentage = component.contains("%");
			if (hasPercentage) {
				String[] percentageItem = component.split("%");
				if (percentageItem.length != 2) {
					return new ParseResult(null, "randomizer.invalid_format");
				}

				// Validates the percentage was entered correctly
				try {
					percentage = Integer.parseInt(percentageItem[0]);
				} catch (NumberFormatException e) {
					return new ParseResult(null, "randomizer.invalid_percent");
				}
				if (percentage <= 0) {
					return new ParseResult(null, "randomizer.zero_percent");
				}
				materialName = percentageItem[1];
			} else {
				materialName = component;
			}

			// Validates a proper item was entered
			Material material;
			try {
				material = Material.valueOf(materialName.toUpperCase());
			} catch (IllegalArgumentException e) {
				return new ParseResult(null, "randomizer.invalid_material");
			}
			if (material.isLegacy() || material.isAir() || !material.isItem()) {
				return new ParseResult(null, "randomizer.invalid_material");
			}

			if (hasPercentage) {
				specifiedPercentageSum += percentage;
				randomItems.add(new RandomItem(percentage, new ItemStack(material, 1)));
			} else {
				unspecifiedMaterials.add(material);
			}
		}

		// Evenly splits the remaining percentage between the materials entered without one
		if (!unspecifiedMaterials.isEmpty()) {
			int remaining = 100 - specifiedPercentageSum;
			int count = unspecifiedMaterials.size();
			if (remaining < count) {
				return new ParseResult(null, "randomizer.not_100_percent");
			}
			int base = remaining / count;
			int extra = remaining % count;
			for (int i = 0; i < count; i++) {
				int share = base + (i < extra ? 1 : 0);
				randomItems.add(new RandomItem(share, new ItemStack(unspecifiedMaterials.get(i), 1)));
			}
		}

		// Ensures the total sums to 100%
		int totalPercentageSum = 0;
		for (RandomItem randomItem : randomItems) {
			totalPercentageSum += randomItem.getPercentage();
		}
		if (totalPercentageSum != 100) {
			return new ParseResult(null, "randomizer.not_100_percent");
		}
		return new ParseResult(randomItems, null);
	}

	/**
	 * Parses a pattern, telling the player why it is invalid if it cannot be used.
	 * @param player The player that entered the pattern.
	 * @param pattern The pattern that was entered.
	 * @return The parsed items, or null if the pattern is invalid.
	 */
	public static List<RandomItem> parsePattern(Player player, String pattern) {
		ParseResult result = parse(pattern);
		if (result.errorKey() != null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, result.errorKey(), "max", MAX_PATTERN_ITEMS)));
			return null;
		}
		return result.items();
	}

	/**
	 * Parses a pattern without telling anyone, such as when loading saved data.
	 * @param pattern The saved pattern.
	 * @return The parsed items, or null if the pattern is no longer valid.
	 */
	public static List<RandomItem> parsePatternSilently(String pattern) {
		return parse(pattern).items();
	}

	/**
	 * Converts items back into their normalized pattern, such as 50%stone,50%andesite.
	 * @param randomItems The items of the pattern.
	 * @return The normalized pattern, or an empty String if there are no items.
	 */
	public static String formatPattern(List<RandomItem> randomItems) {
		if (randomItems == null || randomItems.isEmpty()) {
			return "";
		}
		List<String> components = new ArrayList<>();
		for (RandomItem randomItem : randomItems) {
			components.add(randomItem.getPercentage() + "%" + randomItem.getItem().getType().name().toLowerCase());
		}
		return String.join(",", components);
	}

	/**
	 * Builds one lore line per item of a pattern, such as "- 50% Rooted Dirt".
	 * @param player The player viewing the lore.
	 * @param pattern The normalized pattern.
	 * @return The lore lines, or no lines if the pattern is invalid.
	 */
	public static List<String> getPatternLore(Player player, String pattern) {
		List<String> lore = new ArrayList<>();
		List<RandomItem> randomItems = parsePatternSilently(pattern);
		if (randomItems == null) {
			return lore;
		}
		for (RandomItem randomItem : randomItems) {
			lore.add(Lang.getFor(player, "randomizer.preset_item_line",
					"percent", randomItem.getPercentage(),
					"item", ChatUtils.getFormattedItemName(randomItem.getItem().getType().name())));
		}
		return lore;
	}

	/**
	 * Joins command arguments into one pattern, so that a pattern typed with spaces after its commas still works.
	 * @param args The arguments of the command.
	 * @param start The index of the first argument of the pattern.
	 * @return The pattern without any spaces.
	 */
	public static String joinPattern(String[] args, int start) {
		StringBuilder builder = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			builder.append(args[i]);
		}
		return builder.toString();
	}

	/**
	 * Sets the player's active pattern and turns the randomizer on if it was off.
	 * @param player The player.
	 * @param randomItems The items of the pattern.
	 */
	public static void applyPattern(Player player, List<RandomItem> randomItems) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		aranarthPlayer.setRandomItems(randomItems);
		aranarthPlayer.isMissingItemMessageSent(false);
		// Setting a pattern also turns the randomizer on, but never toggles it off if it was already on
		boolean turnedOn = !aranarthPlayer.isRandomizing();
		aranarthPlayer.isRandomizing(true);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		if (turnedOn) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.blocks_now")));
		}
		saveAsync(player.getUniqueId());
	}

	/**
	 * Turns the player's randomizer on or off.
	 * @param player The player.
	 */
	public static void toggle(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (aranarthPlayer.isRandomizing()) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.block_no_longer")));
		} else {
			if (!hasPattern(aranarthPlayer)) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.no_pattern")));
				return;
			}
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.blocks_now")));
		}
		aranarthPlayer.isRandomizing(!aranarthPlayer.isRandomizing());
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		saveAsync(player.getUniqueId());
	}

	/**
	 * Whether the player has an active pattern set.
	 */
	public static boolean hasPattern(AranarthPlayer aranarthPlayer) {
		return aranarthPlayer.getRandomItems() != null && !aranarthPlayer.getRandomItems().isEmpty();
	}

	/**
	 * Finds the player's preset with the given name, ignoring case and color codes.
	 * @return The preset, or null if the player has none with that name.
	 */
	public static RandomizerPreset getPreset(AranarthPlayer aranarthPlayer, String name) {
		String strippedName = ChatUtils.stripColorFormatting(name).trim();
		for (RandomizerPreset preset : aranarthPlayer.getRandomizerPresets()) {
			if (ChatUtils.stripColorFormatting(preset.getName()).trim().equalsIgnoreCase(strippedName)) {
				return preset;
			}
		}
		return null;
	}

	/**
	 * The preset's name with its color codes applied.
	 */
	public static String getDisplayName(RandomizerPreset preset) {
		return ChatUtils.translateToColor(preset.getName());
	}

	/**
	 * Verifies a preset name can be used, telling the player why if it cannot.
	 * Names may contain spaces, and color codes if the player has permission to use them.
	 * @param player The player naming the preset.
	 * @param name The name that was entered.
	 * @return Whether the name can be used.
	 */
	private static boolean isValidName(Player player, String name) {
		String strippedName = ChatUtils.stripColorFormatting(name).trim();
		if (strippedName.isEmpty()) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gradient.name_empty")));
			return false;
		}
		if (strippedName.length() > MAX_NAME_LENGTH || name.length() > MAX_RAW_NAME_LENGTH) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "gradient.name_too_long")));
			return false;
		}
		// Commas start the pattern in /randomizer preset create, so a name can never contain one
		if (name.contains(",")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_invalid_name")));
			return false;
		}
		if (name.contains("&") && !player.hasPermission("aranarth.nick.color")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission_color")));
			return false;
		}
		if (name.contains("#") && !player.hasPermission("aranarth.nick.hex")) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.no_permission_hex")));
			return false;
		}
		return true;
	}

	/**
	 * Saves a pattern as a preset, replacing the pattern of an existing preset with the same name.
	 * @param player The player.
	 * @param name The name of the preset.
	 * @param randomItems The items of the pattern.
	 * @return Whether the preset was saved.
	 */
	public static boolean savePreset(Player player, String name, List<RandomItem> randomItems) {
		name = name.trim();
		if (!isValidName(player, name)) {
			return false;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		String pattern = formatPattern(randomItems);
		RandomizerPreset existing = getPreset(aranarthPlayer, name);
		if (existing != null) {
			// Re-saving under the same name also takes on the new name's colors
			existing.setName(name);
			existing.setPattern(pattern);
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_saved", "name", getDisplayName(existing))));
		} else {
			if (aranarthPlayer.getRandomizerPresets().size() >= MAX_PRESETS) {
				player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_limit", "max", MAX_PRESETS)));
				return false;
			}
			RandomizerPreset preset = new RandomizerPreset(name, pattern);
			aranarthPlayer.getRandomizerPresets().add(preset);
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_saved", "name", getDisplayName(preset))));
		}
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		saveAsync(player.getUniqueId());
		return true;
	}

	/**
	 * Makes the player's preset with the given name their active pattern.
	 * @param player The player.
	 * @param name The name of the preset.
	 */
	public static void bindPreset(Player player, String name) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		RandomizerPreset preset = getPreset(aranarthPlayer, name);
		if (preset == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_not_found", "name", name)));
			return;
		}
		List<RandomItem> randomItems = parsePattern(player, preset.getPattern());
		if (randomItems == null) {
			return;
		}
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_bound", "name", getDisplayName(preset))));
		player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "randomizer.preset_pattern", "pattern", preset.getPattern())));
		applyPattern(player, randomItems);
	}

	/**
	 * Deletes the player's preset with the given name.
	 * @param player The player.
	 * @param name The name of the preset.
	 */
	public static void deletePreset(Player player, String name) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		RandomizerPreset preset = getPreset(aranarthPlayer, name);
		if (preset == null) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_not_found", "name", name)));
			return;
		}
		aranarthPlayer.getRandomizerPresets().remove(preset);
		AranarthUtils.setPlayer(player.getUniqueId(), aranarthPlayer);
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_deleted", "name", getDisplayName(preset))));
		saveAsync(player.getUniqueId());
	}

	/**
	 * Lists the player's presets, with each pattern shown below its name.
	 * @param player The player.
	 */
	public static void listPresets(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		List<RandomizerPreset> presets = aranarthPlayer.getRandomizerPresets();
		if (presets.isEmpty()) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_list_empty")));
			return;
		}
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_list_header",
				"count", presets.size(), "max", MAX_PRESETS)));
		for (RandomizerPreset preset : presets) {
			player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "randomizer.preset_list_entry", "name", getDisplayName(preset))));
			player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "randomizer.preset_pattern", "pattern", preset.getPattern())));
		}
	}

	/**
	 * Starts waiting for the player to type a name in chat to save their current pattern as a preset.
	 */
	public static void promptPresetName(Player player) {
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
		if (!hasPattern(aranarthPlayer)) {
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.no_pattern")));
			return;
		}
		player.closeInventory();
		awaitingPresetName.add(player.getUniqueId());
		player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.preset_name_prompt")));
	}

	public static boolean isAwaitingPresetName(UUID uuid) {
		return awaitingPresetName.contains(uuid);
	}

	public static void clearAwaitingPresetName(UUID uuid) {
		awaitingPresetName.remove(uuid);
	}

	/**
	 * Saves the player's current pattern under the name they typed in chat. Must be called on the main thread.
	 * @param player The player.
	 * @param message The name the player typed.
	 */
	public static void handlePresetNameInput(Player player, String message) {
		UUID uuid = player.getUniqueId();
		if (message.equalsIgnoreCase("cancel")) {
			awaitingPresetName.remove(uuid);
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "general.cancelled")));
			return;
		}
		AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
		if (!hasPattern(aranarthPlayer)) {
			awaitingPresetName.remove(uuid);
			player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "randomizer.no_pattern")));
			return;
		}
		// Keeps waiting on an invalid name so the player can try again
		if (savePreset(player, message.trim(), aranarthPlayer.getRandomItems())) {
			awaitingPresetName.remove(uuid);
		} else if (aranarthPlayer.getRandomizerPresets().size() >= MAX_PRESETS) {
			awaitingPresetName.remove(uuid);
		}
	}

	/**
	 * Writes the player's randomizer state into their toggle JSON.
	 */
	public static void writeToJson(JsonObject obj, AranarthPlayer aranarthPlayer) {
		obj.addProperty("randomizing", aranarthPlayer.isRandomizing());
		obj.addProperty("randomizerPattern", formatPattern(aranarthPlayer.getRandomItems()));
		JsonArray presets = new JsonArray();
		for (RandomizerPreset preset : aranarthPlayer.getRandomizerPresets()) {
			JsonObject presetObj = new JsonObject();
			presetObj.addProperty("name", preset.getName());
			presetObj.addProperty("pattern", preset.getPattern());
			presets.add(presetObj);
		}
		obj.add("randomizerPresets", presets);
	}

	/**
	 * Reads the player's randomizer state from their toggle JSON, skipping any part that is missing or no longer valid.
	 */
	public static void readFromJson(JsonObject obj, AranarthPlayer aranarthPlayer) {
		if (obj.has("randomizerPattern")) {
			List<RandomItem> randomItems = parsePatternSilently(obj.get("randomizerPattern").getAsString());
			aranarthPlayer.setRandomItems(randomItems != null ? randomItems : new ArrayList<>());
		}
		if (obj.has("randomizing")) {
			aranarthPlayer.isRandomizing(obj.get("randomizing").getAsBoolean() && hasPattern(aranarthPlayer));
		}
		if (obj.has("randomizerPresets") && obj.get("randomizerPresets").isJsonArray()) {
			List<RandomizerPreset> presets = new ArrayList<>();
			for (JsonElement element : obj.getAsJsonArray("randomizerPresets")) {
				if (!element.isJsonObject() || presets.size() >= MAX_PRESETS) {
					continue;
				}
				JsonObject presetObj = element.getAsJsonObject();
				if (presetObj.has("name") && presetObj.has("pattern")) {
					presets.add(new RandomizerPreset(presetObj.get("name").getAsString(), presetObj.get("pattern").getAsString()));
				}
			}
			aranarthPlayer.setRandomizerPresets(presets);
		}
	}

	/**
	 * Saves the player's toggle JSON to MySQL right away, so the change is there when they switch servers.
	 */
	public static void saveAsync(UUID uuid) {
		if (!DatabaseManager.isActive()) {
			return;
		}
		String toggleJson = PersistenceUtils.buildPlayerToggleJson(uuid);
		if (toggleJson != null) {
			Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
					DatabaseManager.getInstance().savePlayerToggles(uuid, toggleJson));
		}
	}

}
