package com.aearost.aranarthcore.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

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

				if (fieldName.equals("homeName")) {
					homeName = fieldValue;
					fieldCount++;
				}
				else if (fieldName.equals("worldName")) {
					world = Bukkit.getWorld(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("x")) {
					x = Double.parseDouble(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("y")) {
					y = Double.parseDouble(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("z")) {
					z = Double.parseDouble(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("yaw")) {
					yaw = Float.parseFloat(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("pitch")) {
					pitch = Float.parseFloat(fieldValue);
					fieldCount++;
				} else if (fieldName.equals("icon")) {
					icon = Material.valueOf(fieldValue);
					fieldCount++;
				}

				if (fieldCount == 8) {
					Location location = new Location(world, x, y, z, yaw, pitch);
					AranarthUtils.addNewHome(location);
					if (!homeName.equals("NEW")) {
						AranarthUtils.setHomeNameAndDirection(homeName, location, icon);
					}
					fieldCount = 0;
				}
			}
			Bukkit.getLogger().info("All homes have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the homes!");
			e.printStackTrace();
		}
	}

	/**
	 * Saves the contents of the homes HashMap to the homes.json file.
	 */
	public static void saveHomes() {
		List<Home> homes = AranarthUtils.getHomes();
		if (homes.size() > 0) {
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
					e.printStackTrace();
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("{\n");
					writer.write("    \"homes\": {\n");

					int homeCounter = 0;
					for (Home home : homes) {
						writer.write("        \"homeName\": \"" + home.getHomeName() + "\",\n");
						writer.write("        \"worldName\": \"" + home.getLocation().getWorld().getName() + "\",\n");
						writer.write("        \"x\": \"" + home.getLocation().getX() + "\",\n");
						writer.write("        \"y\": \"" + home.getLocation().getY() + "\",\n");
						writer.write("        \"z\": \"" + home.getLocation().getZ() + "\",\n");
						writer.write("        \"yaw\": \"" + home.getLocation().getYaw() + "\",\n");
						writer.write("        \"pitch\": \"" + home.getLocation().getPitch() + "\",\n");
						writer.write("        \"icon\": \"" + home.getIcon().name() + "\"\n");

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
					e.printStackTrace();
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

			Bukkit.getLogger().info("Attempting to read the aranarth_players file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				if (parts[parts.length - 1].equals(",") || isRegularNumber(parts[parts.length - 1])) {
					fieldName = parts[1];
					fieldValue = parts[3];
				} else {
					continue;
				}

				if (fieldName.equals("uuid")) {
					uuid = UUID.fromString(fieldValue);
					fieldCount++;
				}
				else if (fieldName.equals("nickname")) {
					nickname = fieldValue;
					fieldCount++;
				} else if (fieldName.equals("prefix")) {
					prefix = fieldValue;
					fieldCount++;
				}

				if (fieldCount == 3) {
					AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname, prefix));
					fieldCount = 0;
				}
			}
			Bukkit.getLogger().info("All aranarth players have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the aranarth players!");
			e.printStackTrace();
		}
	}

	/**
	 * Saves the contents of the homes HashMap to the aranarth_players.json file.
	 */
	public static void saveAranarthPlayers() {
		HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
		if (aranarthPlayers.size() > 0) {
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
					Bukkit.getLogger().info("An error occured in the creation of aranarth_players.json");
					e.printStackTrace();
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
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Determines if the input String can be parsed to a float.
	 * 
	 * @param part
	 * @return
	 */
	@SuppressWarnings("unused")
	public static boolean isRegularNumber(String part) {
		try {
			float partAsFloat = Float.parseFloat(part);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}


}
