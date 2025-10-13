package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.Home;
import com.aearost.aranarthcore.objects.LockedContainer;
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
			Pronouns pronouns = null;
			int rank = 0;
			int saintRank = 0;
			int councilRank = 0;

			Bukkit.getLogger().info("Attempting to read the aranarth_players file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "balance" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("councilRank"))) {
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
					case "pronouns" -> {
						pronouns = Pronouns.valueOf(fieldValue);
						fieldCount++;
					}
					case "rank" -> {
						rank = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "rankSaint" -> {
						saintRank = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "rankCouncil" -> {
						councilRank = Integer.parseInt(fieldValue);
						fieldCount++;
					}
                }
				
				if (fieldCount == 15) {
					AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname, prefix, survivalInventory, arenaInventory, creativeInventory, potions, arrows, blacklist, isDeletingBlacklistedItems, balance, pronouns, rank, saintRank, councilRank));
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
						writer.write("        \"pronouns\": \"" + aranarthPlayer.getPronouns().name() + "\",\n");
						writer.write("        \"rank\": \"" + aranarthPlayer.getRank() + "\",\n");
						writer.write("        \"rankSaint\": \"" + aranarthPlayer.getSaintRank() + "\",\n");
						writer.write("        \"rankCouncil\": \"" + aranarthPlayer.getCouncilRank() + "\",\n");

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
	 * Initializes the server date based on the contents of serverdate.txt.
	 */
	public static void loadServerDate() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "serverdate.txt";
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
			Month month = null;
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
						month = Month.valueOf(parts[1]);
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
	 * Saves the server date to the serverdate.txt file.
	 */
	public static void saveServerDate() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
				+ File.separator + "serverdate.txt";
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
					Bukkit.getLogger().info("A new serverdate.txt file has been generated");
				}
			} catch (IOException e) {
				Bukkit.getLogger().info("An error occurred in the creation of serverdate.txt");
			}

			try {
				FileWriter writer = new FileWriter(filePath);

				writer.write("day:" + AranarthUtils.getDay() + "\n");
				writer.write("weekday:" + AranarthUtils.getWeekday() + "\n");
				writer.write("month:" + AranarthUtils.getMonth().name() + "\n");
				writer.write("year:" + AranarthUtils.getYear());

				writer.close();
			} catch (IOException e) {
				Bukkit.getLogger().info("There was an error in saving the serverdate");
			}
		}
	}

	/**
	 * Initializes the server date based on the contents of lockedcontainers.txt.
	 */
	public static void loadLockedContainers() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "lockedcontainers.txt";
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

			UUID owner = null;
			List<UUID> trusted = new ArrayList<>();
			String world = "";
			int x1 = 0;
			int y1 = 0;
			int z1 = 0;
			int x2 = 0;
			int y2 = 0;
			int z2 = 0;
			boolean isLoc2Null = false;

			Bukkit.getLogger().info("Attempting to read the lockedcontainers file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] parts = line.split("\"");

				// If it's a field and not a parenthesis
				// Make sure to replace "z" with the last field in the list
				if (parts[parts.length - 1].equals(",")
						|| (parts.length > 1 && parts[1].equals("z2"))) {
					fieldName = parts[1];
					fieldValue = parts[3];
				} else {
					continue;
				}

				switch (fieldName) {
					case "owner" -> {
						owner = UUID.fromString(fieldValue);
						fieldCount++;
					}
					case "trusted" -> {
						String[] trustedUuids = fieldValue.split("___");
						for (String trustedUuid : trustedUuids) {
							trusted.add(UUID.fromString(trustedUuid));
						}
						fieldCount++;
					}
					case "world" -> {
						world = fieldValue;
						fieldCount++;
					}
					case "x1" -> {
						x1 = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "y1" -> {
						y1 = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "z1" -> {
						z1 = Integer.parseInt(fieldValue);
						fieldCount++;
					}
					case "x2" -> {
						try {
							x2 = Integer.parseInt(fieldValue);
						} catch (NumberFormatException e) {
							isLoc2Null = true;
						}
						fieldCount++;
					}
					case "y2" -> {
						try {
							y2 = Integer.parseInt(fieldValue);
						} catch (NumberFormatException e) {
							isLoc2Null = true;
						}
						fieldCount++;
					}
					case "z2" -> {
						try {
							z2 = Integer.parseInt(fieldValue);
						} catch (NumberFormatException e) {
							isLoc2Null = true;
						}
						fieldCount++;
					}
				}

				if (fieldCount == 9) {
					Location loc1 = new Location(Bukkit.getWorld(world), x1, y1, z1);
					Location loc2 = null;
					if (!isLoc2Null) {
						loc2 = new Location(Bukkit.getWorld(world), x2, y2, z2);
					}
					LockedContainer lockedContainer = new LockedContainer(owner, trusted, new Location[] { loc1, loc2 });
					AranarthUtils.addLockedContainer(lockedContainer);
					fieldCount = 0;
					owner = null;
					trusted = new ArrayList<>();
				}
			}
			Bukkit.getLogger().info("All lockedcontainers have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the lockedcontainers!");
		}
	}

	/**
	 * Saves the server date to the lockedcontainers.txt file.
	 */
	public static void saveLockedContainers() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
				+ File.separator + "lockedcontainers.txt";
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
					Bukkit.getLogger().info("A new lockedcontainers.json file has been generated");
				}
			} catch (IOException e) {
				Bukkit.getLogger().info("An error occurred in the creation of lockedcontainers.json");
			}

			List<LockedContainer> lockedContainers = AranarthUtils.getLockedContainers();

			try {
				FileWriter writer = new FileWriter(filePath);
				writer.write("{\n");
				writer.write("    \"lockedcontainers\": {\n");

				if (lockedContainers != null && !lockedContainers.isEmpty()) {
					int totalContainerAmount = lockedContainers.size();
					int currentShopNum = 0;
					for (LockedContainer container : lockedContainers) {
						currentShopNum++;
						writer.write("        \"owner\": \"" + container.getOwner().toString() + "\",\n");
						StringBuilder trusted = new StringBuilder();
						for (UUID trustedUuid : container.getTrusted()) {
							if (trusted.isEmpty()) {
								trusted = new StringBuilder(trustedUuid.toString());
							} else {
								trusted.append("___").append(trustedUuid.toString());
							}
						}
						writer.write("        \"trusted\": \"" + trusted + "\",\n");
						Location[] locations = container.getLocations();

						writer.write("        \"world\": \"" + locations[0].getWorld().getName() + "\",\n");
						writer.write("        \"x1\": \"" + locations[0].getBlockX() + "\",\n");
						writer.write("        \"y1\": \"" + locations[0].getBlockY() + "\",\n");
						writer.write("        \"z1\": \"" + locations[0].getBlockZ() + "\",\n");
						if (locations[1] == null) {
							writer.write("        \"x2\": \" \",\n");
							writer.write("        \"y2\": \" \",\n");
							writer.write("        \"z2\": \" \"\n");
						} else {
							writer.write("        \"x2\": \"" + locations[1].getBlockX() + "\",\n");
							writer.write("        \"y2\": \"" + locations[1].getBlockY() + "\",\n");
							writer.write("        \"z2\": \"" + locations[1].getBlockZ() + "\"\n");
						}


						if (currentShopNum == totalContainerAmount) {
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
				Bukkit.getLogger().info("There was an error in saving the lockedcontainers");
			}
		}
	}

}
