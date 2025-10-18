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
				+ "homes.txt";
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

			Bukkit.getLogger().info("Attempting to read the homes file...");

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
				AranarthUtils.addNewHome(location);

				if (Objects.nonNull(homeName)) {
					if (!homeName.equals("NEW")) {
						AranarthUtils.updateHome(homeName, location, icon);
					}
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
					+ File.separator + "homes.txt";
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
						Bukkit.getLogger().info("A new homes.txt file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of homes.txt");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					writer.write("#homeName|worldName|x|y|z|yaw|pitch|icon\n");

					for (Home home : homes) {
						String homeName = home.getHomeName();
						String worldName = home.getLocation().getWorld().getName();
						String x = home.getLocation().getX() + "";
						String y = home.getLocation().getY() + "";
						String z = home.getLocation().getZ() + "";
						String yaw = home.getLocation().getYaw() + "";
						String pitch = home.getLocation().getPitch() + "";
						String icon = home.getIcon().name();

						String row = homeName + "|" + worldName + "|" + x + "|" + y + "|" + z
								+ "|" + yaw + "|" + pitch + "|" + icon + "\n";
						writer.write(row);
					}
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

				// uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|pronouns|rank|saint|council
				String[] fields = row.split("\\|");

				UUID uuid = UUID.fromString(fields[0]);
				String nickname = fields[1];
				String survivalInventory = fields[2];
				String arenaInventory = fields[3];
				String creativeInventory = fields[4];

				List<ItemStack> potions = null;
				ItemStack[] potionsAsItemStackArray;
				if (!fields[5].isEmpty()) {
					try {
						potionsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[5]);
					} catch (IOException e) {
						Bukkit.getLogger().info("There was an issue loading potions!");
						reader.close();
						return;
					}
					potions = new LinkedList<>(Arrays.asList(potionsAsItemStackArray));
				}

				List<ItemStack> arrows = null;
				ItemStack[] arrowsAsItemStackArray;
				if (!fields[6].isEmpty()) {
					try {
						arrowsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[6]);
					} catch (IOException e) {
						Bukkit.getLogger().info("There was an issue loading arrows!");
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
						Bukkit.getLogger().info("There was an issue loading the blacklist!");
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

				Pronouns pronouns = Pronouns.MALE;
				if (fields[10].equals("F")) {
					pronouns = Pronouns.FEMALE;
				} else if (fields[10].equals("N")) {
					pronouns = Pronouns.NEUTRAL;
				}

				int rank = Integer.parseInt(fields[11]);
				int saintRank = Integer.parseInt(fields[12]);
				int councilRank = Integer.parseInt(fields[13]);

				AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname, survivalInventory, arenaInventory, creativeInventory, potions, arrows, blacklist, isDeletingBlacklistedItems, balance, pronouns, rank, saintRank, councilRank));
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
						Bukkit.getLogger().info("A new aranarth_players.json file has been generated");
					}
				} catch (IOException e) {
					Bukkit.getLogger().info("An error occurred in the creation of aranarth_players.json");
				}

				try {
					FileWriter writer = new FileWriter(filePath);
					// Template line
					writer.write("#uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|pronouns|rank|saint|council\n");

					for (Map.Entry<UUID, AranarthPlayer> entry : aranarthPlayers.entrySet()) {
						AranarthPlayer aranarthPlayer = entry.getValue();

						String uuid = entry.getKey().toString();
						String nickname = aranarthPlayer.getNickname();
						String survivalInventory = aranarthPlayer.getSurvivalInventory();
						String arenaInventory = aranarthPlayer.getArenaInventory();
						String creativeInventory = aranarthPlayer.getCreativeInventory();
						String potions = "";
						if (Objects.nonNull(aranarthPlayer.getPotions())) {
							potions = ItemUtils.itemStackArrayToBase64(aranarthPlayer.getPotions().toArray(new ItemStack[0]));
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
						String pronouns = "M";
						if (aranarthPlayer.getPronouns() == Pronouns.FEMALE) {
							pronouns = "F";
						} else if (aranarthPlayer.getPronouns() == Pronouns.NEUTRAL) {
							pronouns = "N";
						}
						String rank = aranarthPlayer.getRank() + "";
						String saint = aranarthPlayer.getSaintRank() + "";
						String council = aranarthPlayer.getCouncilRank() + "";

						String row = uuid + "|" + nickname + "|" + survivalInventory + "|" + arenaInventory + "|"
								+ creativeInventory + "|" + potions + "|" + arrows + "|" + blacklist + "|" + isDeletingBlacklistedItems
								+ "|" + balance + "|" + pronouns + "|" + rank + "|" + saint + "|" + council + "\n";
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
	 * Initializes the playerShops HashMap based on the contents of playershops.json.
	 */
	public static void loadPlayerShops() {
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
				PlayerShop playerShop = new PlayerShop(uuid, location, item, quantity, buyPrice, sellPrice);
				AranarthUtils.addShop(uuid, playerShop);
			}
			Bukkit.getLogger().info("All shops have been initialized");
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
						for (PlayerShop shop : AranarthUtils.getShops().get(uuid)) {

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
					Bukkit.getLogger().info("A new lockedcontainers.json file has been generated");
				}
			} catch (IOException e) {
				Bukkit.getLogger().info("An error occurred in the creation of lockedcontainers.json");
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

}
