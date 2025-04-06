package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.objects.PlayerShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Provides utility methods to facilitate the reading and writing of json and
 * txt files stored in the AranarthCore plugin folder.
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
			String fieldName;
			String fieldValue;

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
					Bukkit.getLogger().info("An error occurred in the creation of homes.json");
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
			String fieldName;
			String fieldValue;

			UUID uuid = null;
			String nickname = null;
			String prefix = null;
			String survivalInventory = null;
			String arenaInventory = null;
			String creativeInventory = null;
			List<ItemStack> potions = null;
			List<ItemStack> arrows = null;
			List<ItemStack> blacklist = null;
			boolean isDeletingBlacklistedItems = false;
			double balance = 0.00;

			Bukkit.getLogger().info("Attempting to read the aranarth_players file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "balance" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("balance"))) {
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
					case "arenaInventory" -> {
						arenaInventory = fieldValue;
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
                            potions = new LinkedList<>(Arrays.asList(potionsAsItemStackArray));
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
							arrows = new LinkedList<>(Arrays.asList(arrowsAsItemStackArray));
						}
						fieldCount++;
					} case "blacklist" -> {
						ItemStack[] blacklistAsItemStackArray;
						if (!fieldValue.isEmpty()) {
							try {
								blacklistAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fieldValue);
							} catch (IOException e) {
								Bukkit.getLogger().info("There was an issue loading arrows!");
								reader.close();
								return;
							}
							blacklist = new LinkedList<>(Arrays.asList(blacklistAsItemStackArray));
						}
						fieldCount++;
					}
					case "isDeletingBlacklistedItems" -> {
						isDeletingBlacklistedItems = Boolean.parseBoolean(fieldValue);
						fieldCount++;
					}
					case "balance" -> {
						balance = Double.parseDouble(fieldValue);
						fieldCount++;
					}
                }
				
				if (fieldCount == 11) {
					AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname, prefix, survivalInventory, arenaInventory, creativeInventory, potions, arrows, blacklist, isDeletingBlacklistedItems, balance));
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
						writer.write("        \"arenaInventory\": \"" + aranarthPlayer.getArenaInventory() + "\",\n");
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
						if (Objects.nonNull(aranarthPlayer.getBlacklist())) {
							ItemStack[] blacklist = aranarthPlayer.getBlacklist().toArray(new ItemStack[0]);
							writer.write("        \"blacklist\": \"" + ItemUtils.itemStackArrayToBase64(blacklist) + "\",\n");
						} else {
							writer.write("        \"blacklist\": \"\",\n");
						}
						writer.write("        \"isDeletingBlacklistedItems\": \"" + aranarthPlayer.getIsDeletingBlacklistedItems() + "\",\n");
						writer.write("        \"balance\": \"" + aranarthPlayer.getBalance() + "\",\n");

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

	/**
	 * Initializes the playerShops HashMap based on the contents of playershops.json.
	 */
	public static void loadPlayerShops() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "playershops.json";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			int fieldCount = 0;
			String fieldName;
			String fieldValue;

			UUID uuid = null;
			World world = null;
			double x = 0;
			double y = 0;
			double z = 0;
			ItemStack item = null;
			int quantity = 0;
			double buyPrice = 0;
			double sellPrice = 0;

			Bukkit.getLogger().info("Attempting to read the playershops file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "sellPrice" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("sellPrice"))) {
					fieldName = parts[1];
					fieldValue = parts[3];
				} else {
					continue;
				}

				switch (fieldName) {
					case "uuid" -> {
						if (fieldValue.equals("null")) {
							uuid = null;
						} else {
							uuid = UUID.fromString(fieldValue);
						}
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
					case "item" -> {
						try {
							item = ItemUtils.itemStackArrayFromBase64(fieldValue)[0];
						} catch (IOException e) {
							Bukkit.getLogger().info("There was an issue initializing a shop item!");
							item = new ItemStack(Material.AIR, 1);
						}
						fieldCount++;
					}
					case "quantity" -> {
						quantity = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "buyPrice" -> {
						buyPrice = Double.parseDouble(fieldValue);
						fieldCount++;
					}
					case "sellPrice" -> {
						sellPrice = Double.parseDouble(fieldValue);
						fieldCount++;
					}
				}

				if (fieldCount == 9) {
					Location location = new Location(world, x, y, z);
					PlayerShop playerShop = new PlayerShop(uuid, location, item, quantity, buyPrice, sellPrice);

					AranarthUtils.addShop(uuid, playerShop);
					fieldCount = 0;
				}
			}
			Bukkit.getLogger().info("All playershops have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the playershops!");
		}
	}

	/**
	 * Saves the contents of the playerShops HashMap to the playershops.json file.
	 */
	public static void savePlayerShops() {
		HashMap<UUID, List<PlayerShop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "playershops.json";
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
						Bukkit.getLogger().info("A new playershops.json file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of playershops.json");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("{\n");
					writer.write("    \"playershops\": {\n");

					int totalUuidAmount = playerShops.size();
					int currentUuidAmount = 0;
					int shopCounter = 0;

					for (UUID uuid : playerShops.keySet()) {
						currentUuidAmount++;
						int shopAmountFromUuid = playerShops.get(uuid).size();
						for (PlayerShop shop : AranarthUtils.getShops().get(uuid)) {
							shopCounter++;
							// If it's a server shop
							if (shop.getUuid() == null) {
								writer.write("        \"uuid\": \"" + null + "\",\n");
							} else {
								writer.write("        \"uuid\": \"" + shop.getUuid().toString() + "\",\n");
							}

							writer.write("        \"worldName\": \"" + shop.getLocation().getWorld().getName() + "\",\n");
							writer.write("        \"x\": \"" + shop.getLocation().getX() + "\",\n");
							writer.write("        \"y\": \"" + shop.getLocation().getY() + "\",\n");
							writer.write("        \"z\": \"" + shop.getLocation().getZ() + "\",\n");
							ItemStack[] itemAsArray = new ItemStack[1];
							itemAsArray[0] = shop.getItem();
							writer.write("        \"item\": \"" + ItemUtils.itemStackArrayToBase64(itemAsArray) + "\",\n");
							writer.write("        \"quantity\": \"" + shop.getQuantity() + "\",\n");
							writer.write("        \"buyPrice\": \"" + shop.getBuyPrice() + "\",\n");
							writer.write("        \"sellPrice\": \"" + shop.getSellPrice() + "\"\n");


							if (currentUuidAmount == totalUuidAmount && shopCounter == shopAmountFromUuid) {
								writer.write("    }\n");
							} else {
								writer.write("    },\n");
								writer.write("    {\n");
							}
						}
					}

					writer.write("}\n");
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the playershops");
				}
			}
		}
	}

	/**
	 * Initializes the server date based on the contents of serverdate.json.
	 */
	public static void loadServerDate() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "serverdate.json";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);
			int fieldCount = 0;
			int day = 0;
			int weekday = 0;
			int month = 0;
			int year = 0;

			Bukkit.getLogger().info("Attempting to read the serverdate file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split(":");

				switch (parts[0]) {
					case "day" -> {
						day = Integer.parseInt(parts[1]);
						fieldCount++;
					}
					case "weekday" -> {
						weekday = Integer.parseInt(parts[1]);
						fieldCount++;
					}
					case "month" -> {
						month = Integer.parseInt(parts[1]);
						fieldCount++;
					}
					case "year" -> {
						year = Integer.parseInt(parts[1]);
						fieldCount++;
					}
				}

				if (fieldCount == 4) {
					AranarthUtils.setDay(day);
					AranarthUtils.setWeekday(weekday);
					AranarthUtils.setMonth(month);
					AranarthUtils.setYear(year);
				}
			}
			Bukkit.getLogger().info("The server date has been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the server date!");
		}
	}

	/**
	 * Saves the server date to the serverdate.json file.
	 */
	public static void saveServerDate() {
		HashMap<UUID, List<PlayerShop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "serverdate.json";
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
						Bukkit.getLogger().info("A new serverdate.json file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of serverdate.json");
				}

				try {
					FileWriter writer = new FileWriter(filePath);

					writer.write("day:" + AranarthUtils.getDay() + "\n");
					writer.write("weekday:" + AranarthUtils.getWeekday() + "\n");
					writer.write("month:" + AranarthUtils.getMonth() + "\n");
					writer.write("year:" + AranarthUtils.getYear());

					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the serverdate");
				}
			}
		}
	}

}
