package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.objects.*;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Provides utility methods to facilitate the reading and writing of json and
 * txt files stored in the AranarthCore plugin folder.
 */
public class PersistenceUtils {

	private static final Logger log = LoggerFactory.getLogger(PersistenceUtils.class);

	/**
	 * Initializes the homes HashMap based on the contents of homes.txt.
	 */
	public static void loadHomepads() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "homepads.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			String fieldName;
			String fieldValue;

			Bukkit.getLogger().info("Attempting to read the homepads file...");

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				// homeName|worldName|x|y|z|yaw|pitch|icon
				String[] fields = row.split("\\|");

				String homeName = fields[0];
				String worldName = fields[1];
				double x = Double.parseDouble(fields[2]);
				double y = Double.parseDouble(fields[3]);
				double z = Double.parseDouble(fields[4]);
				float yaw = Float.parseFloat(fields[5]);
				float pitch = Float.parseFloat(fields[6]);
				Material icon = Material.valueOf(fields[7]);

				Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
				AranarthUtils.addNewHomepad(location);

				if (Objects.nonNull(homeName)) {
					if (!homeName.equals("NEW")) {
						AranarthUtils.updateHomepad(homeName, location, icon);
					}
				}
			}

			Bukkit.getLogger().info("All homepads have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the homepads!");
		}
	}

	/**
	 * Saves the contents of the homes HashMap to the homes.txt file.
	 */
	public static void saveHomepads() {
		List<Home> homes = AranarthUtils.getHomepads();
		if (!homes.isEmpty()) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "homepads.txt";
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
						Bukkit.getLogger().info("A new homepads.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of homepads.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#homeName|worldName|x|y|z|yaw|pitch|icon\n");

					for (Home homepad : homes) {
						String homeName = homepad.getName();
						String worldName = homepad.getLocation().getWorld().getName();
						String x = homepad.getLocation().getX() + "";
						String y = homepad.getLocation().getY() + "";
						String z = homepad.getLocation().getZ() + "";
						String yaw = homepad.getLocation().getYaw() + "";
						String pitch = homepad.getLocation().getPitch() + "";
						String icon = homepad.getIcon().name();

						String row = homeName + "|" + worldName + "|" + x + "|" + y + "|" + z
								+ "|" + yaw + "|" + pitch + "|" + icon + "\n";
						writer.write(row);
					}
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the homepads");
				}
			}
		}
	}

	/**
	 * Initializes the players HashMap based on the contents of aranarth_players.txt.
	 */
	public static void loadAranarthPlayers() {

		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "aranarth_players.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);
			Bukkit.getLogger().info("Attempting to read the aranarth_players file...");

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				// uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|isCompressingItems|pronouns
				String[] fields = row.split("\\|");
				int lastIndex = fields.length - 1;

				UUID uuid = UUID.fromString(fields[0]);
				String nickname = fields[1];
				String survivalInventory = fields[2];
				String arenaInventory = fields[3];
				String creativeInventory = fields[4];

				HashMap<ItemStack, Integer> potions = new HashMap<>();
				if (!fields[5].isEmpty()) {
					String[] potionAsArray = fields[5].split("___");
					for (String potionInArray : potionAsArray) {
						String[] parts = potionInArray.split("_");
						ItemStack[] potionType = new ItemStack[1];
						try {
							potionType = ItemUtils.itemStackArrayFromBase64(parts[0]);
						} catch (IOException e) {
							Bukkit.getLogger().info("There was an issue loading the player's potions!");
							reader.close();
							return;
						}
						int amount = Integer.parseInt(parts[1]);
						potions.put(potionType[0], amount);
					}
				}

				List<ItemStack> arrows = null;
				ItemStack[] arrowsAsItemStackArray;
				if (!fields[6].isEmpty()) {
					try {
						arrowsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[6]);
					} catch (IOException e) {
						Bukkit.getLogger().info("There was an issue loading the player's arrows!");
						reader.close();
						return;
					}
					arrows = new LinkedList<>(Arrays.asList(arrowsAsItemStackArray));
				}

				List<ItemStack> blacklist = null;
				ItemStack[] blacklistAsItemStackArray;
				if (!fields[7].isEmpty()) {
					try {
						blacklistAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[7]);
					} catch (IOException e) {
						Bukkit.getLogger().info("There was an issue loading the player's blacklist!");
						reader.close();
						return;
					}
					blacklist = new LinkedList<>(Arrays.asList(blacklistAsItemStackArray));
				}

				boolean isDeletingBlacklistedItems = false;
				if (fields[8].equals("1")) {
					isDeletingBlacklistedItems = true;
				}

				double balance = Double.parseDouble(fields[9]);



				int rank = Integer.parseInt(fields[10]);
				int saintRank = Integer.parseInt(fields[11]);
				int councilRank = Integer.parseInt(fields[12]);
				int architectRank = Integer.parseInt(fields[13]);

				List<Home> homes = new ArrayList<>();
				String[] homesStrings = null;
				if (!fields[14].isEmpty()) {
					homesStrings = fields[14].split("___");
				}

				// Only 1 empty index if no homes are set
				if (homesStrings != null) {
					for (String home : homesStrings) {
						String[] homeParts = home.split("_");
						String homeName = homeParts[0];
						String worldName = homeParts[1];
						double x = Double.parseDouble(homeParts[2]);
						double y = Double.parseDouble(homeParts[3]);
						double z = Double.parseDouble(homeParts[4]);
						float yaw = Float.parseFloat(homeParts[5]);
						float pitch = Float.parseFloat(homeParts[6]);
						Material icon = Material.valueOf(homeParts[7]);
						Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
						homes.add(new Home(homeName, loc, icon));
					}
				}

				String muteEndDate = fields[15];
				int particles = Integer.parseInt(fields[16]);
				String perks = fields[17];
				long saintExpireDate = Long.parseLong(fields[18]);
				boolean isCompressingItems = false;
				if (fields[19].equals("1")) {
					isCompressingItems = true;
				}

				// Keep pronouns at the end and add before this
				// No need to update the index as it will be dynamic
				Pronouns pronouns = Pronouns.MALE;
				if (fields[lastIndex].equals("F")) {
					pronouns = Pronouns.FEMALE;
				} else if (fields[lastIndex].equals("N")) {
					pronouns = Pronouns.NEUTRAL;
				}

				AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname,
						survivalInventory, arenaInventory, creativeInventory, potions, arrows, blacklist,
						isDeletingBlacklistedItems, balance, rank, saintRank, councilRank, architectRank, homes,
						muteEndDate, particles, perks, saintExpireDate, isCompressingItems, pronouns));
			}
			Bukkit.getLogger().info("All aranarth players have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the aranarth players!");
		}
	}

	/**
	 * Saves the contents of the players HashMap to the aranarth_players.txt file.
	 */
	public static void saveAranarthPlayers() {
		HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
		if (!aranarthPlayers.isEmpty()) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "aranarth_players.txt";
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
						Bukkit.getLogger().info("A new aranarth_players.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of aranarth_players.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					// Template line
					writer.write("#uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|saintExpireDate|isCompressingItems|pronouns\n");

					for (Map.Entry<UUID, AranarthPlayer> entry : aranarthPlayers.entrySet()) {
						AranarthPlayer aranarthPlayer = entry.getValue();

						String uuid = entry.getKey().toString();
						String nickname = aranarthPlayer.getNickname();
						if (nickname == null) {
							nickname = "";
						} else if (nickname.equals(aranarthPlayer.getUsername())) {
							nickname = "";
						}

						String survivalInventory = aranarthPlayer.getSurvivalInventory();
						String arenaInventory = aranarthPlayer.getArenaInventory();
						String creativeInventory = aranarthPlayer.getCreativeInventory();

						String potions = "";
						if (aranarthPlayer.getPotions() != null && !aranarthPlayer.getPotions().isEmpty()) {
							for (ItemStack potion : aranarthPlayer.getPotions().keySet()) {
								int amount = aranarthPlayer.getPotions().get(potion);
								String part = ItemUtils.itemStackArrayToBase64(new ItemStack[] { potion });
								part += "_" + amount + "___";
								potions += part;
							}

							if (potions.endsWith("___")) {
								potions = potions.substring(0, potions.length() - 2); // Remove the last three characters
							}
						}

						String arrows = "";
						if (Objects.nonNull(aranarthPlayer.getArrows())) {
							arrows = ItemUtils.itemStackArrayToBase64(aranarthPlayer.getArrows().toArray(new ItemStack[0]));
						}
						String blacklist = "";
						if (Objects.nonNull(aranarthPlayer.getBlacklist())) {
							blacklist = ItemUtils.itemStackArrayToBase64(aranarthPlayer.getBlacklist().toArray(new ItemStack[0]));
						}
						String isDeletingBlacklistedItems = "0";
						if (aranarthPlayer.getIsDeletingBlacklistedItems()) {
							isDeletingBlacklistedItems = "1";
						}
						String balance = aranarthPlayer.getBalance() + "";
						String rank = aranarthPlayer.getRank() + "";
						String saint = aranarthPlayer.getSaintRank() + "";
						String council = aranarthPlayer.getCouncilRank() + "";
						String architect = aranarthPlayer.getArchitectRank() + "";
						List<String> homes = new ArrayList<>();
						if (aranarthPlayer.getHomes() != null) {
							for (int i = 0; i < aranarthPlayer.getHomes().size(); i++) {
								Home home = aranarthPlayer.getHomes().get(i);
								String name = home.getName();
								String worldName = home.getLocation().getWorld().getName();
								double x = home.getLocation().getX();
								double y = home.getLocation().getY();
								double z = home.getLocation().getZ();
								float yaw = home.getLocation().getYaw();
								float pitch = home.getLocation().getPitch();
								Material type = home.getIcon();
								if (i == aranarthPlayer.getHomes().size() - 1) {
									homes.add(name + "_" + worldName + "_" + x + "_" + y + "_" + z + "_" + yaw + "_" + pitch + "_" + type.name());
								} else {
									homes.add(name + "_" + worldName + "_" + x + "_" + y + "_" + z + "_" + yaw + "_" + pitch + "_" + type.name() + "___");
								}
							}
						}
						StringBuilder allHomesBuilder = new StringBuilder();
						for (String home : homes) {
							allHomesBuilder.append(home);
						}
						String allHomes = allHomesBuilder.toString();
						if (allHomes.isEmpty()) {
							allHomes = "";
						}

						String muteEndDate = aranarthPlayer.getMuteEndDate();
						String particles = aranarthPlayer.getParticleNum() + "";
						String perks = aranarthPlayer.getPerks();
						long saintExpireDate = aranarthPlayer.getSaintExpireDate();
						String isCompressingItems = "0";
						if (aranarthPlayer.getIsCompressingItems()) {
							isCompressingItems = "1";
						}

						// Keep pronouns at the end and add before this
						String pronouns = "M";
						if (aranarthPlayer.getPronouns() == Pronouns.FEMALE) {
							pronouns = "F";
						} else if (aranarthPlayer.getPronouns() == Pronouns.NEUTRAL) {
							pronouns = "N";
						}

						String row = uuid + "|" + nickname + "|" + survivalInventory + "|" + arenaInventory + "|"
								+ creativeInventory + "|" + potions + "|" + arrows + "|" + blacklist + "|" + isDeletingBlacklistedItems
								+ "|" + balance + "|" + rank + "|" + saint + "|" + council + "|" + architect + "|"
								+ allHomes + "|" + muteEndDate + "|" + particles + "|" + perks + "|" + saintExpireDate
								+ "|" + isCompressingItems + "|"
								// Keep pronouns at the end and add before this
								+ pronouns + "\n";
						writer.write(row);
					}
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the aranarth players!");
				}
			}
		}
	}

	/**
	 * Initializes the shops HashMap based on the contents of shops.txt.
	 */
	public static void loadShops() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "shops.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			Bukkit.getLogger().info("Attempting to read the shops file...");

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				String[] fields = row.split("\\|");

				UUID uuid = null;
				if (!fields[0].isEmpty()) {
					uuid = UUID.fromString(fields[0]);
				}
				String worldName = fields[1];
				int x = Integer.parseInt(fields[2]);
				int y = Integer.parseInt(fields[3]);
				int z = Integer.parseInt(fields[4]);
				ItemStack item = null;
				try {
					item = ItemUtils.itemStackArrayFromBase64(fields[5])[0];
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an issue initializing a shop item!");
					item = new ItemStack(Material.AIR, 1);
				}
				int quantity = Integer.parseInt(fields[6]);
				double buyPrice = Double.parseDouble(fields[7]);
				double sellPrice = Double.parseDouble(fields[8]);

				Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
				Shop playerShop = new Shop(uuid, location, item, quantity, buyPrice, sellPrice);
				AranarthUtils.addShop(uuid, playerShop);
			}
			Bukkit.getLogger().info("All shops have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the shops!");
		}
	}

	/**
	 * Saves the contents of the shops HashMap to the shops.txt file.
	 */
	public static void saveShops() {
		HashMap<UUID, List<Shop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "shops.txt";
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
						Bukkit.getLogger().info("A new shops.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of shops.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#uuid|worldName|x|y|z|item|quantity|buyPrice|sellPrice\n");

					for (UUID uuid : playerShops.keySet()) {
						for (Shop shop : AranarthUtils.getShops().get(uuid)) {

							String uuidString = "";
							if (uuid != null) {
								uuidString = uuid.toString();
							}
							String worldName = shop.getLocation().getWorld().getName();
							String x = shop.getLocation().getBlockX() + "";
							String y = shop.getLocation().getBlockY() + "";
							String z = shop.getLocation().getBlockZ() + "";
							String item = ItemUtils.itemStackArrayToBase64(new ItemStack[] { shop.getItem() });
							String quantity = shop.getQuantity() + "";
							String buyPrice = shop.getBuyPrice() + "";
							String sellPrice = shop.getSellPrice() + "";

							String row = uuidString + "|" + worldName + "|" + x + "|" + y + "|" + z + "|"
									+ item + "|" + quantity + "|" + buyPrice + "|" + sellPrice + "\n";
							writer.write(row);
						}
					}

					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the shops");
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

			Bukkit.getLogger().info("Attempting to read the lockedcontainers file...");

			while (reader.hasNextLine()) {
				String line = reader.nextLine();
				String[] fields = line.split("\\|");

				if (line.startsWith("#")) {
					continue;
				}

				UUID owner = UUID.fromString(fields[0]);
				String[] trustedUuids = fields[1].split("___");
				List<UUID> trusted = new ArrayList<>();
				for (String trustedUuid : trustedUuids) {
					trusted.add(UUID.fromString(trustedUuid));
				}
				String worldName = fields[2];
				int x1 = Integer.parseInt(fields[3]);
				int y1 = Integer.parseInt(fields[4]);
				int z1 = Integer.parseInt(fields[5]);
				Location loc1 = new Location(Bukkit.getWorld(worldName), x1, y1, z1);

				int x2 = 0;
				int y2 = 0;
				int z2 = 0;
				boolean isLoc2Null = false;
				try {
					x2 = Integer.parseInt(fields[6]);
					y2 = Integer.parseInt(fields[7]);
					z2 = Integer.parseInt(fields[8]);
				} catch (NumberFormatException e) {
					isLoc2Null = true;
				}
				Location loc2 = null;
				if (!isLoc2Null) {
					loc2 = new Location(Bukkit.getWorld(worldName), x2, y2, z2);
				}

				LockedContainer lockedContainer = new LockedContainer(owner, trusted, new Location[] { loc1, loc2 });
				AranarthUtils.addLockedContainer(lockedContainer);
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
					Bukkit.getLogger().info("A new lockedcontainers.txt file has been generated");
				}
			} catch (IOException e) {
				Bukkit.getLogger().info("An error occurred in the creation of lockedcontainers.txt");
			}

			List<LockedContainer> lockedContainers = AranarthUtils.getLockedContainers();

			try {
				FileWriter writer = new FileWriter(filePath);
				writer.write("#owner|trusted|worldName|x1|y1|z1|x2|y2|z2\n");

				if (lockedContainers != null && !lockedContainers.isEmpty()) {
					for (LockedContainer container : lockedContainers) {
						String owner = container.getOwner().toString();
						StringBuilder trusted = new StringBuilder();
						for (UUID trustedUuid : container.getTrusted()) {
							if (trusted.isEmpty()) {
								trusted = new StringBuilder(trustedUuid.toString());
							} else {
								trusted.append("___").append(trustedUuid.toString());
							}
						}
						String trustedString = trusted.toString();
						Location[] locations = container.getLocations();
						String worldName = locations[0].getWorld().getName();
						String x1 = locations[0].getBlockX() + "";
						String y1 = locations[0].getBlockY() + "";
						String z1 = locations[0].getBlockZ() + "";
						String x2 = "";
						String y2 = "";
						String z2 = "";
						if (locations[1] != null) {
							x2 = locations[1].getBlockX() + "";
							y2 = locations[1].getBlockY() + "";
							z2 = locations[1].getBlockZ() + "";
						}

						String row = owner + "|" + trustedString + "|" + worldName + "|" + x1 + "|" + y1 + "|" + z1 + "|" + x2 + "|" + y2 + "|" + z2 + "\n";
					}
				}
				writer.close();
			} catch (IOException e) {
				Bukkit.getLogger().info("There was an error in saving the lockedcontainers");
			}
		}
	}

	/**
	 * Initializes the dominions list based on the contents of dominions.txt.
	 */
	public static void loadDominions() {

		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "dominions.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);
			Bukkit.getLogger().info("Attempting to read the dominions file...");

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				// #name|owner|members|world|chunks|power|x|y|z|yaw|pitch
				String[] fields = row.split("\\|");

				String name = fields[0];
				UUID owner = UUID.fromString(fields[1]);
				List<UUID> members = new ArrayList<>();
				String[] memberParts = fields[2].split("___");
				for (String member : memberParts) {
					members.add(UUID.fromString(member));
				}

				String worldName = fields[3];
				World world = Bukkit.getWorld(worldName);

				List<Chunk> chunks = new ArrayList<>();
				String[] claimedChunks = fields[4].split("___");
				for (String chunk : claimedChunks) {
					String[] coordinates = chunk.split(",");
					int x = Integer.parseInt(coordinates[0]);
					int z = Integer.parseInt(coordinates[1]);
					chunks.add(world.getChunkAt(x, z));
				}

				int power = Integer.parseInt(fields[5]);
				double x = Double.parseDouble(fields[6]);
				double y = Double.parseDouble(fields[7]);
				double z = Double.parseDouble(fields[8]);
				float yaw = Float.parseFloat(fields[9]);
				float pitch = Float.parseFloat(fields[10]);
				double balance = Double.parseDouble(fields[11]);

				DominionUtils.createDominion(new Dominion(name, owner, members, worldName, chunks, power, x, y, z, yaw, pitch, balance));
			}
			Bukkit.getLogger().info("All dominions have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the dominions!");
		}
	}

	/**
	 * Saves the contents of the dominions list to the dominions.txt file.
	 */
	public static void saveDominions() {
		HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
		if (!aranarthPlayers.isEmpty()) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "dominions.txt";
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
						Bukkit.getLogger().info("A new dominions.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of dominions.txt");
				}

				List<Dominion> dominions = DominionUtils.getDominions();
				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#name|owner|members|worldName|chunks|power|x|y|z|yaw|pitch|balance\n");

					if (dominions != null && !dominions.isEmpty()) {
						for (Dominion dominion : dominions) {
							String name = dominion.getName();
							String owner = dominion.getOwner().toString();
							StringBuilder members = new StringBuilder();
							for (UUID memberUuid : dominion.getMembers()) {
								if (members.isEmpty()) {
									members = new StringBuilder(memberUuid.toString());
								} else {
									members.append("___").append(memberUuid.toString());
								}
							}
							String membersString = members.toString();
							String worldName = dominion.getDominionHome().getWorld().getName();

							StringBuilder chunks = new StringBuilder();
							for (Chunk chunk : dominion.getChunks()) {
								String chunkXZ = chunk.getX() + "," + chunk.getZ();
								if (chunks.isEmpty()) {
									chunks = new StringBuilder(chunkXZ);
								} else {
									chunks.append("___").append(chunkXZ);
								}
							}
							String chunksString = chunks.toString();

							String dominionPower = dominion.getDominionPower() + "";
							Location dominionHome = dominion.getDominionHome();
							String x = dominionHome.getX() + "";
							String y = dominionHome.getY() + "";
							String z = dominionHome.getZ() + "";
							String yaw = dominionHome.getYaw() + "";
							String pitch = dominionHome.getPitch() + "";
							String balance = dominion.getBalance() + "";

							String row = name + "|" + owner + "|" + membersString + "|" + worldName + "|" + chunksString + "|" + dominionPower + "|" + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch + "|" + balance + "\n";
							writer.write(row);
						}
					}
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the dominions!");
				}
			}
		}
	}

	/**
	 * Initializes the warps list based on the contents of warps.txt.
	 */
	public static void loadWarps() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "warps.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			Bukkit.getLogger().info("Attempting to read the warps file...");
			List<Home> warps = new ArrayList<>();

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				// warpName|worldName|x|y|z|yaw|pitch|icon
				String[] fields = row.split("\\|");

				String warpName = fields[0];
				String worldName = fields[1];
				double x = Double.parseDouble(fields[2]);
				double y = Double.parseDouble(fields[3]);
				double z = Double.parseDouble(fields[4]);
				float yaw = Float.parseFloat(fields[5]);
				float pitch = Float.parseFloat(fields[6]);
				Material icon = Material.valueOf(fields[7]);

				Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
				Home warp = new Home(warpName, location, icon);
				warps.add(warp);
			}
			AranarthUtils.setWarps(warps);
			Bukkit.getLogger().info("All warps have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the warps!");
		}
	}

	/**
	 * Saves the contents of the warps list to the warps.txt file.
	 */
	public static void saveWarps() {
		HashMap<UUID, List<Shop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "warps.txt";
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
						Bukkit.getLogger().info("A new warps.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of warps.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#warpName|worldName|x|y|z|yaw|pitch|icon\n");

					for (Home warp : AranarthUtils.getWarps()) {
						String warpName = warp.getName();
						String worldName = warp.getLocation().getWorld().getName();
						String x = warp.getLocation().getX() + "";
						String y = warp.getLocation().getY() + "";
						String z = warp.getLocation().getZ() + "";
						String yaw = warp.getLocation().getYaw() + "";
						String pitch = warp.getLocation().getPitch() + "";
						String icon = warp.getIcon().name();

						String row = warpName + "|" + worldName + "|" + x + "|" + y + "|" + z
								+ "|" + yaw + "|" + pitch + "|" + icon + "\n";
						writer.write(row);
					}

					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the warps");
				}
			}
		}
	}

	/**
	 * Initializes the punishments list based on the contents of punishments.txt.
	 */
	public static void loadPunishments() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "punishments.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			Bukkit.getLogger().info("Attempting to read the punishments file...");
			HashMap<UUID, List<Punishment>> punishments = new HashMap<>();

			while (reader.hasNextLine()) {
				String row = reader.nextLine();

				// Skip any commented out lines
				if (row.startsWith("#")) {
					continue;
				}

				// uuid|date|type|reason|appliedBy
				String[] fields = row.split("\\|");

				UUID uuid = UUID.fromString(fields[0]);
				LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(fields[1])), ZoneId.systemDefault());
				String type = fields[2];
				String reason = fields[3];
				UUID appliedBy = null;
				if (!fields[4].equals("CONSOLE")) {
					appliedBy = UUID.fromString(fields[4]);
				}
				Punishment punishment = new Punishment(uuid, date, type, reason, appliedBy);
				AranarthUtils.addPunishment(uuid, punishment, true);
			}
			Bukkit.getLogger().info("All punishments have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the punishments!");
		}
	}

	/**
	 * Saves the contents of the punishments list to the punishments.txt file.
	 */
	public static void savePunishments() {
		HashMap<UUID, List<Shop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "punishments.txt";
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
						Bukkit.getLogger().info("A new punishments.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of punishments.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#uuid|date|type|reason|appliedBy\n");

					for (UUID uuid : AranarthUtils.getAllPunishments().keySet()) {
						for (Punishment punishment : AranarthUtils.getPunishments(uuid)) {
							String uuidString = uuid.toString();
							String date = punishment.getDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + "";
							String type = punishment.getType();
							String reason = punishment.getReason();
							UUID appliedByUuid = punishment.getAppliedBy();
							String appliedBy = "";
							if (appliedByUuid == null) {
								appliedBy = "CONSOLE";
							} else {
								appliedBy = appliedByUuid.toString();
							}

							String row = uuidString + "|" + date + "|" + type + "|" + reason + "|" + appliedBy + "\n";
							writer.write(row);
						}
					}

					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the punishments");
				}
			}
		}
	}

	/**
	 * Initializes the avatars list based on the contents of avatars.txt.
	 */
	public static void loadAvatars() {
		String currentPath = System.getProperty("user.dir");
		String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
				+ "avatars.txt";
		File file = new File(filePath);

		// First run of plugin
		if (!file.exists()) {
			return;
		}

		Scanner reader;
		try {
			reader = new Scanner(file);

			Bukkit.getLogger().info("Attempting to read the avatars file...");
			HashMap<UUID, List<Punishment>> punishments = new HashMap<>();

			while (reader.hasNextLine()) {
				String row = reader.nextLine();
				if (row.equals("none")) {
					AvatarUtils.addAvatar(null);
					continue;
				}

				// Skip any commented out lines
				if (row.startsWith("#")) {
					// Applies the avatar's binds to ensure they are not reset upon relogging
					if (row.contains("_")) {
						String noHashtag = row.substring(1);
						String[] parts = noHashtag.split("_");

						Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
						if (currentAvatar != null) {
							BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
							bendingPlayer.bindAbility(parts[1], Integer.parseInt(parts[0]));
						}
					}

					continue;
				}

				// uuid|startInGame|endInGame|startInRealLife|endInRealLife|element
				String[] fields = row.split("\\|");

				UUID uuid = UUID.fromString(fields[0]);
				String startInGame = fields[1];
				String endInGame = fields[2];
				String startInRealLife = fields[3];
				String endInRealLife = fields[4];
				char element = fields[5].charAt(0);

				Avatar avatar = new Avatar(uuid, startInGame, endInGame, startInRealLife, endInRealLife, element);
				AvatarUtils.addAvatar(avatar);
			}
			Bukkit.getLogger().info("All avatars have been initialized");
			reader.close();
		} catch (FileNotFoundException e) {
			Bukkit.getLogger().info("Something went wrong with loading the avatars!");
		}
	}

	/**
	 * Saves the contents of the avatars list to the avatars.txt file.
	 */
	public static void saveAvatars() {
		HashMap<UUID, List<Shop>> playerShops = AranarthUtils.getShops();
		if (playerShops != null) {
			String currentPath = System.getProperty("user.dir");
			String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
					+ File.separator + "avatars.txt";
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
						Bukkit.getLogger().info("A new avatars.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of avatars.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);

					// Saving the avatar's binds to ensure they are not reset upon relogging
					Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
					if (currentAvatar != null) {
						BendingPlayer currentAvatarBendingPlayer = BendingPlayer.getBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
						for (int index : currentAvatarBendingPlayer.getAbilities().keySet()) {
							writer.write("#" + index + "_" + currentAvatarBendingPlayer.getAbilities().get(index) + "\n");
						}
					}

					writer.write("#uuid|startInGame|endInGame|startInRealLife|endInRealLife|element\n");

					for (Avatar avatar : AvatarUtils.getAvatars()) {
						if (avatar == null) {
							writer.write("none\n");
						} else {
							String uuid = avatar.getUuid().toString();
							String startInGame = avatar.getStartInGame();
							String endInGame = avatar.getEndInGame();
							String startInRealLife = avatar.getStartInRealLife();
							String endInRealLife = avatar.getEndInRealLife();
							char element = avatar.getElement();

							writer.write(uuid + "|" + startInGame + "|" + endInGame + "|"
									+ startInRealLife + "|" + endInRealLife + "|" + element + "\n");
						}
					}
					writer.close();
				} catch (IOException e) {
					Bukkit.getLogger().info("There was an error in saving the avatars");
				}
			}
		}
	}

}
