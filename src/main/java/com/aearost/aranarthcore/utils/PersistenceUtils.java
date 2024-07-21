package com.aearost.aranarthcore.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;

/**
 * Provides utility methods to facilitate the reading and writing of json and
 * txt files stored in the AranarthCore plugin folder.
 * 
 * @author Aearost
 *
 */
public class PersistenceUtils {

	/**
	 * Initializes the homes HashMap based on the contents of homes.json.
	 */
	public static void loadHomes() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "homes.json";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			int fieldCount = 0;
			String fieldName = null;
			String fieldValue = null;

			String homeName = null;
			World world = null;
			double x = 0;
			double y = 0;
			double z = 0;
			float yaw = 0;
			float pitch = 0;
			Material icon = null;

			Bukkit.getLogger().info("Attempting to read the homes file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "icon" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("icon"))) {
					fieldName = parts[1];
					fieldValue = parts[3];
				} else {
					continue;
				}

                switch (fieldName) {
                    case "homeName" -> {
                        homeName = fieldValue;
                        fieldCount++;
                    }
                    case "worldName" -> {
                        world = Bukkit.getWorld(fieldValue);
                        fieldCount++;
                    }
                    case "x" -> {
                        x = Double.parseDouble(fieldValue);
                        fieldCount++;
                    }
                    case "y" -> {
                        y = Double.parseDouble(fieldValue);
                        fieldCount++;
                    }
                    case "z" -> {
                        z = Double.parseDouble(fieldValue);
                        fieldCount++;
                    }
                    case "yaw" -> {
                        yaw = Float.parseFloat(fieldValue);
                        fieldCount++;
                    }
                    case "pitch" -> {
                        pitch = Float.parseFloat(fieldValue);
                        fieldCount++;
                    }
                    case "icon" -> {
                        icon = Material.valueOf(fieldValue);
                        fieldCount++;
                    }
                }

				if (fieldCount == 8) {
					Location location = new Location(world, x, y, z, yaw, pitch);
					AranarthUtils.addNewHome(location);

					if (Objects.nonNull(homeName)) {
						if (!homeName.equals("NEW")) {
							AranarthUtils.updateHome(homeName, location, icon);
						}
					}
					fieldCount = 0;
				}
			}
			Bukkit.getLogger().info("All homes have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the homes!");
		}
	}

	/**
	 * Saves the contents of the homes HashMap to the homes.json file.
	 */
	public static void saveHomes() {
		List<Home> homes = AranarthUtils.getHomes();
		if (!homes.isEmpty()) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "homes.json";
			File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
			File file = new File(filePath);

			// If the directory exists
			boolean isDirectoryCreated = true;
			if (!pluginDirectory.isDirectory()) {
				isDirectoryCreated = pluginDirectory.mkdir();
			}
			if (isDirectoryCreated) {
				try {
					// If the file isn't already there
					if (file.createNewFile()) {
						Bukkit.getLogger().info("A new homes.json file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occured in the creation of homes.json");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("{\n");
					writer.write("    \"homes\": {\n");

					int homeCounter = 0;
					for (Home home : homes) {
						if (Objects.nonNull(home.getLocation().getWorld())) {
							writer.write("        \"homeName\": \"" + home.getHomeName() + "\",\n");
							writer.write("        \"worldName\": \"" + home.getLocation().getWorld().getName() + "\",\n");
							writer.write("        \"x\": \"" + home.getLocation().getX() + "\",\n");
							writer.write("        \"y\": \"" + home.getLocation().getY() + "\",\n");
							writer.write("        \"z\": \"" + home.getLocation().getZ() + "\",\n");
							writer.write("        \"yaw\": \"" + home.getLocation().getYaw() + "\",\n");
							writer.write("        \"pitch\": \"" + home.getLocation().getPitch() + "\",\n");
							writer.write("        \"icon\": \"" + home.getIcon().name() + "\"\n");
						} else {
							Bukkit.getLogger().info("The world name is null and the home has been skipped!");
							return;
						}

						if (homeCounter + 1 == homes.size()) {
							writer.write("    }\n");
						} else {
							writer.write("    },\n");
							writer.write("    {\n");
							homeCounter++;
						}
					}

					writer.write("}\n");
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the homes");
				}
			}
		}
	}
	
	
	/**
	 * Initializes the homes HashMap based on the contents of aranarth_players.json.
	 */
	public static void loadAranarthPlayers() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "aranarth_players.json";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			// UUID must not be reset each time
			int fieldCount = 0;
			String fieldName = null;
			String fieldValue = null;

			UUID uuid = null;
			String nickname = null;
			String prefix = null;
			String survivalInventory = null;
			String creativeInventory = null;
			List<ItemStack> potions = null;
			List<ItemStack> arrows = null;

			Bukkit.getLogger().info("Attempting to read the aranarth_players file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "icon" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("icon"))) {
					fieldName = parts[1];
					fieldValue = parts[3];
				} else {
					continue;
				}

                switch (fieldName) {
                    case "uuid" -> {
                        uuid = UUID.fromString(fieldValue);
                        fieldCount++;
                    }
                    case "nickname" -> {
                        nickname = fieldValue;
                        fieldCount++;
                    }
                    case "prefix" -> {
                        prefix = fieldValue;
                        fieldCount++;
                    }
                    case "survivalInventory" -> {
                        survivalInventory = fieldValue;
                        fieldCount++;
                    }
                    case "creativeInventory" -> {
                        creativeInventory = fieldValue;
                        fieldCount++;
                    }
                    case "potions" -> {
                        ItemStack[] potionsAsItemStackArray;
                        if (!fieldValue.isEmpty()) {
                            try {
                                potionsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fieldValue);
                            } catch (IOException e) {
                                Bukkit.getLogger().info("There was an issue loading potions!");
                                reader.close();
                                return;
                            }
                            potions = new LinkedList<ItemStack>(Arrays.asList(potionsAsItemStackArray));
                        }
                        fieldCount++;
                    }
                    case "arrows" -> {
                        ItemStack[] arrowsAsItemStackArray;
                        if (!fieldValue.isEmpty()) {
                            try {
                                arrowsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fieldValue);
                            } catch (IOException e) {
                                Bukkit.getLogger().info("There was an issue loading arrows!");
                                reader.close();
                                return;
                            }
                            arrows = new LinkedList<ItemStack>(Arrays.asList(arrowsAsItemStackArray));
                        }
                        fieldCount++;
                    }
                }
				
				if (fieldCount == 7) {
					if (Objects.nonNull(uuid)) {
						AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname, prefix, survivalInventory, creativeInventory, potions, arrows));
					} else {
						Bukkit.getLogger().info("The UUID is null!");
					}
					fieldCount = 0;
				}
			}
			Bukkit.getLogger().info("All aranarth players have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the aranarth players!");
		}
	}

	/**
	 * Saves the contents of the homes HashMap to the aranarth_players.json file.
	 */
	public static void saveAranarthPlayers() {
		HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
		if (!aranarthPlayers.isEmpty()) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "aranarth_players.json";
			File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
			File file = new File(filePath);

			// If the directory exists
			boolean isDirectoryCreated = true;
			if (!pluginDirectory.isDirectory()) {
				isDirectoryCreated = pluginDirectory.mkdir();
			}
			if (isDirectoryCreated) {
				try {
					// If the file isn't already there
					if (file.createNewFile()) {
						Bukkit.getLogger().info("A new aranarth_players.json file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of aranarth_players.json");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("{\n");
					writer.write("    \"aranarth_players\": {\n");

					int aranarthPlayerCounter = 0;
					
					for (Map.Entry<UUID, AranarthPlayer> entry : aranarthPlayers.entrySet()) {
						UUID uuid = entry.getKey();
						AranarthPlayer aranarthPlayer = entry.getValue();
						
						writer.write("        \"uuid\": \"" + uuid.toString() + "\",\n");
						writer.write("        \"nickname\": \"" + aranarthPlayer.getNickname() + "\",\n");
						writer.write("        \"prefix\": \"" + aranarthPlayer.getPrefix() + "\",\n");
						writer.write("        \"survivalInventory\": \"" + aranarthPlayer.getSurvivalInventory() + "\",\n");
						writer.write("        \"creativeInventory\": \"" + aranarthPlayer.getCreativeInventory() + "\",\n");
						if (Objects.nonNull(aranarthPlayer.getPotions())) {
							ItemStack[] potions = aranarthPlayer.getPotions().toArray(new ItemStack[0]);
							writer.write("        \"potions\": \"" + ItemUtils.itemStackArrayToBase64(potions) + "\",\n");
						} else {
							writer.write("        \"potions\": \"\",\n");
						}
						if (Objects.nonNull(aranarthPlayer.getArrows())) {
							ItemStack[] arrows = aranarthPlayer.getArrows().toArray(new ItemStack[0]);
							writer.write("        \"arrows\": \"" + ItemUtils.itemStackArrayToBase64(arrows) + "\",\n");
						} else {
							writer.write("        \"arrows\": \"\",\n");
						}
						
						if (aranarthPlayerCounter + 1 == aranarthPlayers.size()) {
							writer.write("    }\n");
						} else {
							writer.write("    },\n");
							writer.write("    {\n");
							aranarthPlayerCounter++;
						}
						
					}

					writer.write("}\n");
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the aranarth players!");
				}
			}
		}
	}
	
}
