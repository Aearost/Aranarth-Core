package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.enums.Month;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.objects.DefenderType;
import com.aearost.aranarthcore.objects.Quest;
import com.projectkorra.projectkorra.BendingPlayer;

import java.util.stream.Collectors;

import com.projectkorra.projectkorra.OfflineBendingPlayer;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Provides utility methods to facilitate the reading and writing of json and
 * txt files stored in the AranarthCore plugin folder.
 */
public class PersistenceUtils {

    private static final Gson GSON = new Gson();

    /**
     * Converts a stored homepad worldName (e.g. "smp:world") to the actual Bukkit world name
     * used on the current server (e.g. "world" on the SMP server).
     */
    private static String toBukkitWorldName(String storedWorldName) {
        if (AranarthCore.isSmpServer() && storedWorldName.startsWith("smp:")) {
            return storedWorldName.substring(4);
        }
        return storedWorldName;
    }

    /**
     * Runs a database sync task. If the plugin is still enabled, runs it asynchronously.
     * During shutdown (plugin disabled), runs it on the current thread to avoid the
     * "Plugin attempted to register task while disabled" error.
     */
    private static void runDbSync(Runnable task) {
        if (AranarthCore.getInstance().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), task);
        } else {
            task.run();
        }
    }

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

            Bukkit.getLogger().info("[AC] Attempting to read the homepads file...");

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

                Location location = new Location(Bukkit.getWorld(toBukkitWorldName(worldName)), x, y, z, yaw, pitch);
                AranarthUtils.addNewHomepad(location);

                if (Objects.nonNull(homeName)) {
                    if (!homeName.equals("NEW")) {
                        AranarthUtils.updateHomepad(homeName, location, icon);
                    }
                }
            }

            Bukkit.getLogger().info("[AC] All homepads have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the homepads!");
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
                        Bukkit.getLogger().info("[AC] A new homepads.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of homepads.txt");
                }

                try {
                    FileWriter writer = new FileWriter(filePath);
                    writer.write("#homeName|worldName|x|y|z|yaw|pitch|icon\n");

                    for (Home homepad : homes) {
                        String homeName = homepad.getName();
                        String worldName = homepad.getWorldName();
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
                    Bukkit.getLogger().info("[AC] There was an error in saving the homepads");
                }
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncHomepadsToDatabase);
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
            Bukkit.getLogger().info("[AC] Attempting to read the aranarth_players file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();

                // Skip any commented out lines
                if (row.startsWith("#")) {
                    continue;
                }

                // uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|saintExpirationDate|isCompressingItems|votePointsSpent|firstJoinDate|pronouns
                String[] fields = row.split("\\|");
                try {
                    int lastIndex = fields.length - 1;

                    UUID uuid = UUID.fromString(fields[0]);
                    String nickname = fields[1];
                    String survivalInventory = fields[2];
                    String arenaInventory = fields[3];
                    String creativeInventory = fields[4];

                    HashMap<ItemStack, Integer> potions = new HashMap<>();
                    if (!fields[5].isEmpty()) {
                        String[] potionAsArray = fields[5].split("\\*\\*\\*");
                        for (String potionInArray : potionAsArray) {
                            String[] parts = potionInArray.split("\\*");
                            ItemStack[] potionType = new ItemStack[1];
                            try {
                                potionType = ItemUtils.itemStackArrayFromBase64(parts[0]);
                            } catch (IOException e) {
                                throw new RuntimeException("Could not decode player potions", e);
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
                            throw new RuntimeException("Could not decode player arrows", e);
                        }
                        arrows = new LinkedList<>(Arrays.asList(arrowsAsItemStackArray));
                    }

                    List<ItemStack> blacklist = null;
                    ItemStack[] blacklistAsItemStackArray;
                    if (!fields[7].isEmpty()) {
                        try {
                            blacklistAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[7]);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not decode player blacklist", e);
                        }
                        blacklist = new LinkedList<>(Arrays.asList(blacklistAsItemStackArray));
                    }

                    int blacklistingMethod = Integer.parseInt(fields[8]);
                    double balance = Double.parseDouble(fields[9]);
                    int rank = Integer.parseInt(fields[10]);
                    int saintRank = Integer.parseInt(fields[11]);
                    int councilRank = Integer.parseInt(fields[12]);
                    int architectRank = Integer.parseInt(fields[13]);

                    List<Home> homes = new ArrayList<>();
                    String[] homesStrings = null;
                    if (!fields[14].isEmpty()) {
                        homesStrings = fields[14].split("\\*\\*\\*");
                    }

                    // Only 1 empty index if no homes are set
                    if (homesStrings != null) {
                        for (String home : homesStrings) {
                            String[] homeParts = home.split("\\*");
                            if (homeParts.length < 8) {
                                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Skipping malformed home entry: " + home);
                                continue;
                            }
                            String homeName = homeParts[0];
                            String fileWorldName = homeParts[1];
                            double x = Double.parseDouble(homeParts[2]);
                            double y = Double.parseDouble(homeParts[3]);
                            double z = Double.parseDouble(homeParts[4]);
                            float yaw = Float.parseFloat(homeParts[5]);
                            float pitch = Float.parseFloat(homeParts[6]);
                            Material icon = Material.valueOf(homeParts[7]);

                            // Translate old Survival-side "smp" world names to the "smp:" prefix format.
                            String savedWorldName;
                            if (fileWorldName.equals("smp")) {
                                savedWorldName = "smp:world";
                            } else if (fileWorldName.equals("smp_nether")) {
                                savedWorldName = "smp:world_nether";
                            } else if (fileWorldName.equals("smp_the_end")) {
                                savedWorldName = "smp:world_the_end";
                            } else {
                                savedWorldName = fileWorldName;
                            }

                            // Resolve the Bukkit World for this server.
                            // SMP homes resolve to the local SMP world; survival homes resolve to
                            // null on the SMP server (they live on the other server).
                            World bukttiWorld;
                            if (savedWorldName.startsWith("smp:")) {
                                String smpPart = savedWorldName.substring(4);
                                String localName;
                                if (smpPart.equals("world")) {
                                    localName = AranarthCore.getSmpMainWorldName();
                                } else if (smpPart.equals("world_nether")) {
                                    localName = AranarthCore.getSmpNetherWorldName();
                                } else if (smpPart.equals("world_the_end")) {
                                    localName = AranarthCore.getSmpEndWorldName();
                                } else {
                                    localName = smpPart;
                                }
                                bukttiWorld = Bukkit.getWorld(localName);
                            } else if (AranarthCore.isSmpServer()) {
                                // Non-SMP world names don't exist on the SMP server.
                                // Keep null so CommandHome knows to cross-server transfer.
                                bukttiWorld = null;
                            } else {
                                bukttiWorld = Bukkit.getWorld(savedWorldName);
                            }

                            Location loc = new Location(bukttiWorld, x, y, z, yaw, pitch);
                            homes.add(new Home(homeName, loc, icon, savedWorldName));
                        }
                    }

                    String muteEndDate = fields[15];
                    int particles = Integer.parseInt(fields[16]);

                    String[] perksValues = fields[17].split("\\*");
                    Perk[] perkArray = Perk.values();
                    HashMap<Perk, Integer> perks = new HashMap<>();
                    for (int i = 0; i < perkArray.length; i++) {
                        Perk perk = perkArray[i];
                        perks.put(perk, Integer.parseInt(perksValues[i]));
                    }

                    long saintExpireDate = Long.parseLong(fields[18]);
                    boolean isCompressingItems = false;
                    if (fields[19].equals("1")) {
                        isCompressingItems = true;
                    }

                    int votePointsSpent = Integer.parseInt(fields[20]);
                    int spawnBoostValue = Integer.parseInt(fields[21]);
                    boolean isUsingSpawnBoost = spawnBoostValue == 1;

                    String firstJoinDate = fields[22];

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
                            blacklistingMethod, balance, rank, saintRank, councilRank, architectRank, homes,
                            muteEndDate, particles, perks, saintExpireDate, isCompressingItems, votePointsSpent, isUsingSpawnBoost,
                            firstJoinDate,
                            pronouns)); // Keep pronouns at the end
                    long conquestDisbandCooldownEnd = fields.length > 24 ? Long.parseLong(fields[23]) : 0L;
                    String survivalEnderChest = fields.length > 25 ? fields[24] : "";
                    double survivalHealth = fields.length > 26 ? Double.parseDouble(fields[25]) : 20.0;
                    int survivalFoodLevel = fields.length > 27 ? Integer.parseInt(fields[26]) : 20;
                    float survivalSaturation = fields.length > 28 ? Float.parseFloat(fields[27]) : 5.0f;
                    int survivalExpLevel = fields.length > 29 ? Integer.parseInt(fields[28]) : 0;
                    float survivalExpProgress = fields.length > 30 ? Float.parseFloat(fields[29]) : 0.0f;
                    AranarthUtils.getPlayer(uuid).setConquestDisbandCooldownEnd(conquestDisbandCooldownEnd);
                    AranarthUtils.getPlayer(uuid).setSurvivalEnderChest(survivalEnderChest);
                    AranarthUtils.getPlayer(uuid).setSurvivalHealth(survivalHealth);
                    AranarthUtils.getPlayer(uuid).setSurvivalFoodLevel(survivalFoodLevel);
                    AranarthUtils.getPlayer(uuid).setSurvivalSaturation(survivalSaturation);
                    AranarthUtils.getPlayer(uuid).setSurvivalExpLevel(survivalExpLevel);
                    AranarthUtils.getPlayer(uuid).setSurvivalExpProgress(survivalExpProgress);
                } catch (Exception e) {
                    // Skip malformed rows without aborting the rest of the load
                }
            }
            Bukkit.getLogger().info("[AC] All aranarth players have been initialized");
            reader.close();
        } catch (Exception e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the aranarth players!");
        }
    }

    /**
     * Builds the pipe-delimited row string for a single AranarthPlayer (without trailing newline).
     * Extracted from saveAranarthPlayers() so it can be reused by syncAranarthPlayersToDatabase().
     */
    public static String buildAranarthPlayerRow(UUID uuid, AranarthPlayer aranarthPlayer) {
        String uuidStr = uuid.toString();
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
                String part = ItemUtils.itemStackArrayToBase64(new ItemStack[]{potion});
                part += "*" + amount + "***";
                potions += part;
            }
            if (potions.endsWith("***")) {
                potions = potions.substring(0, potions.length() - 2);
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
        String blacklistingMethod = "0";
        if (aranarthPlayer.getBlacklistingMethod() == -1) {
            blacklistingMethod = "-1";
        } else if (aranarthPlayer.getBlacklistingMethod() == 1) {
            blacklistingMethod = "1";
        }
        String balance = aranarthPlayer.getBalance() + "";
        String rank = aranarthPlayer.getRank() + "";
        String saint = aranarthPlayer.getSaintRank() + "";
        String council = aranarthPlayer.getCouncilRank() + "";
        String architect = aranarthPlayer.getArchitectRank() + "";
        List<String> homes = new ArrayList<>();
        if (aranarthPlayer.getHomes() != null) {
            for (Home home : aranarthPlayer.getHomes()) {
                String name = home.getName();
                String worldName = home.getWorldName();
                double x = home.getLocation().getX();
                double y = home.getLocation().getY();
                double z = home.getLocation().getZ();
                float yaw = home.getLocation().getYaw();
                float pitch = home.getLocation().getPitch();
                Material type = home.getIcon();
                homes.add(name + "*" + worldName + "*" + x + "*" + y + "*" + z + "*" + yaw + "*" + pitch + "*" + type.name());
            }
        }
        String allHomes = String.join("***", homes);

        String muteEndDate = aranarthPlayer.getMuteEndDate();
        String particles = aranarthPlayer.getParticleNum() + "";

        String perks = "";
        for (int i = 0; i < Perk.values().length; i++) {
            Perk perk = Perk.values()[i];
            if (aranarthPlayer.getPerks().get(perk) == null) {
                perks += 0;
            } else {
                perks += aranarthPlayer.getPerks().get(perk);
            }
            if (i < aranarthPlayer.getPerks().size() - 1) {
                perks += "*";
            }
        }

        long saintExpireDate = aranarthPlayer.getSaintExpireDate();
        String isCompressingItems = "0";
        if (aranarthPlayer.isCompressingItems()) {
            isCompressingItems = "1";
        }
        int votePointsSpent = aranarthPlayer.getVotePointsSpent();
        boolean isUsingSpawnBoost = aranarthPlayer.isUsingSpawnBoost();
        int spawnBoostValue = isUsingSpawnBoost ? 1 : 0;

        String firstJoinDate = aranarthPlayer.getFirstJoinDate();

        // Keep pronouns at the end and add before this
        String pronouns = "M";
        if (aranarthPlayer.getPronouns() == Pronouns.FEMALE) {
            pronouns = "F";
        } else if (aranarthPlayer.getPronouns() == Pronouns.NEUTRAL) {
            pronouns = "N";
        }

        long conquestDisbandCooldownEnd = aranarthPlayer.getConquestDisbandCooldownEnd();
        String survivalEnderChest = aranarthPlayer.getSurvivalEnderChest();
        double survivalHealth = aranarthPlayer.getSurvivalHealth();
        int survivalFoodLevel = aranarthPlayer.getSurvivalFoodLevel();
        float survivalSaturation = aranarthPlayer.getSurvivalSaturation();
        int survivalExpLevel = aranarthPlayer.getSurvivalExpLevel();
        float survivalExpProgress = aranarthPlayer.getSurvivalExpProgress();
        return uuidStr + "|" + nickname + "|" + survivalInventory + "|" + arenaInventory + "|"
                + creativeInventory + "|" + potions + "|" + arrows + "|" + blacklist + "|" + blacklistingMethod
                + "|" + balance + "|" + rank + "|" + saint + "|" + council + "|" + architect + "|"
                + allHomes + "|" + muteEndDate + "|" + particles + "|" + perks + "|" + saintExpireDate
                + "|" + isCompressingItems + "|" + votePointsSpent + "|" + spawnBoostValue + "|"
                + firstJoinDate + "|" + conquestDisbandCooldownEnd + "|" + survivalEnderChest + "|"
                + survivalHealth + "|" + survivalFoodLevel + "|" + survivalSaturation + "|"
                + survivalExpLevel + "|" + survivalExpProgress + "|"
                // Keep pronouns at the end and add before this
                + pronouns;
    }

    /**
     * Builds and returns the raw pipe-delimited DB row for the given player using their current
     * in-memory AranarthPlayer data. Safe to call on the main thread; the returned String can
     * then be written to the database on an async thread.
     *
     * @return The serialized row, or {@code null} if the player is not loaded in memory.
     */
    public static String buildPlayerRowForTransfer(UUID uuid) {
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap == null) {
            return null;
        }
        return buildAranarthPlayerRow(uuid, ap);
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
                        Bukkit.getLogger().info("[AC] A new aranarth_players.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of aranarth_players.txt");
                }

                // Write to a temp file first; only replace the real file if the entire
                // write succeeds. This prevents a mid-write exception from truncating the
                // live file (e.g. a NullPointerException from a home in an unloaded world).
                // NOTE: Files.move() must be called AFTER the FileWriter is closed —
                // on Windows, renaming an open file throws an exception.
                File tempFile = new File(filePath + ".tmp");
                boolean writeSucceeded = false;
                try (FileWriter writer = new FileWriter(tempFile)) {
                    // Template line
                    writer.write("#uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|saintExpirationDate|isCompressingItems|votePointsSpent|spawnBoostValue|firstJoinDate|pronouns\n");

                    for (Map.Entry<UUID, AranarthPlayer> entry : aranarthPlayers.entrySet()) {
                        String row = buildAranarthPlayerRow(entry.getKey(), entry.getValue()) + "\n";
                        writer.write(row);
                    }
                    writeSucceeded = true;
                } catch (Exception e) {
                    Bukkit.getLogger().severe(AranarthCore.LOG_PREFIX
                            + "Error saving aranarth players (live file unchanged): " + e.getMessage());
                    if (!tempFile.delete()) {
                        Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Could not delete temp file: " + tempFile.getPath());
                    }
                }
                // Writer is now closed — safe to move on Windows.
                if (writeSucceeded) {
                    try {
                        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        Bukkit.getLogger().severe(AranarthCore.LOG_PREFIX
                                + "Error replacing aranarth players file: " + e.getMessage());
                        if (!tempFile.delete()) {
                            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Could not delete temp file: " + tempFile.getPath());
                        }
                    }
                }
            }
        }
        // MySQL sync (additive — runs after file save)
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncAranarthPlayersToDatabase);
        }
    }

    /**
     * Initializes the toggled features based on the contents of toggled.txt.
     */
    public static void loadToggledFeatures() {

        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "toggled.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the toggled file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();

                // Skip any commented out lines
                if (row.startsWith("#")) {
                    continue;
                }

                // uuid|chat|messages|teleport|spawnboost|changeclaim|inventory|shulker|blacklist|compressing|chestlock|ping|bluefire
                String[] fields = row.split("\\|");
                int lastIndex = fields.length - 1;

                UUID uuid = UUID.fromString(fields[0]);
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);

                if (aranarthPlayer == null) {
                    continue;
                }

                // Chat
                if (fields[1].equals("0")) {
                    aranarthPlayer.setTogglingChat(false);
                } else {
                    aranarthPlayer.setTogglingChat(true);
                }

                // Messages
                if (fields[2].equals("0")) {
                    aranarthPlayer.setTogglingMessages(false);
                } else {
                    aranarthPlayer.setTogglingMessages(true);
                }

                // Teleport
                if (fields[3].equals("0")) {
                    aranarthPlayer.setTogglingTp(false);
                } else {
                    aranarthPlayer.setTogglingTp(true);
                }

                // Spawn Boost
                if (fields[4].equals("0")) {
                    aranarthPlayer.setUsingSpawnBoost(true);
                } else {
                    aranarthPlayer.setUsingSpawnBoost(false);
                }

                // Change Claim
                if (fields[5].equals("0")) {
                    aranarthPlayer.setTogglingChangeClaim(false);
                } else {
                    aranarthPlayer.setTogglingChangeClaim(true);
                }

                // Inventory
                if (fields[6].equals("0")) {
                    aranarthPlayer.setTogglingInventoryAssist(false);
                } else {
                    aranarthPlayer.setTogglingInventoryAssist(true);
                }

                // Shulker
                if (fields[7].equals("0")) {
                    aranarthPlayer.setAddingToShulker(true);
                } else {
                    aranarthPlayer.setAddingToShulker(false);
                }

                // Blacklist
                if (fields[8].equals("-1")) {
                    aranarthPlayer.setBlacklistingMethod(-1);
                } else if (fields[8].equals("1")) {
                    aranarthPlayer.setBlacklistingMethod(1);
                } else {
                    aranarthPlayer.setBlacklistingMethod(0);
                }

                // Compressing
                if (fields[9].equals("0")) {
                    aranarthPlayer.setCompressingItems(true);
                } else {
                    aranarthPlayer.setCompressingItems(false);
                }

                // Chest Lock
                if (fields[10].equals("0")) {
                    aranarthPlayer.setAutoLockingChests(true);
                } else {
                    aranarthPlayer.setAutoLockingChests(false);
                }

                // Fields from index 11 onward are optional — checked with fields.length > N so that
                // existing files without the field load fine and use the default from the constructor.

                // Blue Fire (index 11)
                if (fields.length > 11) {
                    aranarthPlayer.setBlueFireDisabled(!fields[11].equals("0"));
                }

                // Gradient Chat Enabled (index 12)
                if (fields.length > 12) {
                    aranarthPlayer.setGradientChatEnabled(!fields[12].equals("0"));
                }

                // Gradient Chat Colors (index 13)
                if (fields.length > 13 && !fields[13].equals("none")) {
                    aranarthPlayer.setGradientChatColors(fields[13]);
                }

                // Day Message (index 14)
                if (fields.length > 14) {
                    aranarthPlayer.setDayMessageDisabled(!fields[14].equals("0"));
                }

                // Weather Message (index 15)
                if (fields.length > 15) {
                    aranarthPlayer.setWeatherMessageDisabled(!fields[15].equals("0"));
                }

                // Dominion Msg Compact (index 16)
                if (fields.length > 16) {
                    aranarthPlayer.setDominionMsgCompact(!fields[16].equals("0"));
                }

                AranarthUtils.setPlayer(uuid, aranarthPlayer);
            }
            Bukkit.getLogger().info("[AC] All toggled features have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the toggled features!");
        }
    }

    /**
     * Saves the toggled features to the toggled.txt file.
     */
    public static void saveToggledFeatures() {
        HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
        if (!aranarthPlayers.isEmpty()) {
            String currentPath = System.getProperty("user.dir");
            String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                    + File.separator + "toggled.txt";
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
                        Bukkit.getLogger().info("[AC] A new toggled.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of toggled.txt");
                }

                try {
                    FileWriter writer = new FileWriter(filePath);
                    // Template line
                    writer.write("#uuid|chat|messages|teleport|spawnboost|changeclaim|inventory|shulker|blacklist|compressing|chestlock|bluefire|gradientchatenabled|gradientchatcolors|daymessage|weathermessage\n");

                    for (Map.Entry<UUID, AranarthPlayer> entry : aranarthPlayers.entrySet()) {
                        AranarthPlayer aranarthPlayer = entry.getValue();

                        String uuid = entry.getKey().toString();
                        String chat = aranarthPlayer.isTogglingChat() ? "1" : "0";
                        String messages = aranarthPlayer.isTogglingMessages() ? "1" : "0";
                        String teleport = aranarthPlayer.isTogglingTp() ? "1" : "0";
                        String spawnboost = aranarthPlayer.isUsingSpawnBoost() ? "0" : "1";
                        String changeClaim = aranarthPlayer.isTogglingChangeClaim() ? "1" : "0";
                        String inventory = aranarthPlayer.isTogglingInventoryAssist() ? "1" : "0";
                        String shulker = aranarthPlayer.isAddingToShulker() ? "0" : "1";
                        String blacklist = aranarthPlayer.getBlacklistingMethod() + "";
                        String compressing = aranarthPlayer.isCompressingItems() ? "0" : "1";
                        String chestLock = aranarthPlayer.isAutoLockingChests() ? "0" : "1";
                        String bluefire = aranarthPlayer.hasBlueFireDisabled() ? "1" : "0";
                        String gradientEnabled = aranarthPlayer.isGradientChatEnabled() ? "1" : "0";
                        String gradientColors = aranarthPlayer.getGradientChatColors().isEmpty() ? "none" : aranarthPlayer.getGradientChatColors();
                        String dayMessage = aranarthPlayer.isDayMessageDisabled() ? "1" : "0";
                        String weatherMessage = aranarthPlayer.isWeatherMessageDisabled() ? "1" : "0";
                        String dominionMsgCompact = aranarthPlayer.isDominionMsgCompact() ? "1" : "0";

                        String row = uuid + "|" + chat + "|" + messages + "|" + teleport + "|" + spawnboost + "|" + changeClaim
                                + "|" + inventory + "|" + shulker + "|" + blacklist + "|" + compressing + "|" + chestLock + "|"
                                + bluefire + "|" + gradientEnabled + "|" + gradientColors + "|" + dayMessage + "|" + weatherMessage
                                + "|" + dominionMsgCompact + "\n";
                        writer.write(row);
                    }
                    writer.close();
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] There was an error in saving the toggled features!");
                }
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncToggledFeaturesToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the shops file...");

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
                    Bukkit.getLogger().info("[AC] There was an issue initializing a shop item!");
                    item = new ItemStack(Material.AIR, 1);
                }
                int quantity = Integer.parseInt(fields[6]);
                double buyPrice = Double.parseDouble(fields[7]);
                double sellPrice = Double.parseDouble(fields[8]);

                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                Shop playerShop = new Shop(uuid, location, item, quantity, buyPrice, sellPrice);
                playerShop.setWorldName(worldName);
                ShopUtils.addShop(uuid, playerShop);
            }
            Bukkit.getLogger().info("[AC] All shops have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the shops!");
        }
    }

    /**
     * Saves the contents of the shops HashMap to the shops.txt file.
     */
    public static void saveShops() {
        HashMap<UUID, List<Shop>> playerShops = ShopUtils.getShops();
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
                        Bukkit.getLogger().info("[AC] A new shops.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of shops.txt");
                }

                try {
                    FileWriter writer = new FileWriter(filePath);
                    writer.write("#uuid|worldName|x|y|z|item|quantity|buyPrice|sellPrice\n");

                    for (UUID uuid : playerShops.keySet()) {
                        for (Shop shop : ShopUtils.getShops().get(uuid)) {

                            String uuidString = "";
                            if (uuid != null) {
                                uuidString = uuid.toString();
                            }
                            String worldName = shop.getWorldName();
                            String x = shop.getLocation().getBlockX() + "";
                            String y = shop.getLocation().getBlockY() + "";
                            String z = shop.getLocation().getBlockZ() + "";
                            String item = ItemUtils.itemStackArrayToBase64(new ItemStack[]{shop.getItem()});
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
                    Bukkit.getLogger().info("[AC] There was an error in saving the shops");
                }
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncShopsToDatabase);
            runDbSync(PersistenceUtils::syncServerShopsToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the serverdate file...");

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
                    case "lastResourceWorldResetTime" -> {
                        AranarthUtils.setLastResourceWorldResetTime(Long.parseLong(parts[1]));
                    }
                }

                if (fieldCount == 4) {
                    AranarthUtils.setDay(day);
                    AranarthUtils.setWeekday(weekday);
                    AranarthUtils.setMonth(month);
                    AranarthUtils.setYear(year);
                }
            }
            Bukkit.getLogger().info("[AC] The server date has been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the server date!");
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
                    Bukkit.getLogger().info("[AC] A new serverdate.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of serverdate.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);

                writer.write("day:" + AranarthUtils.getDay() + "\n");
                writer.write("weekday:" + AranarthUtils.getWeekday() + "\n");
                writer.write("month:" + AranarthUtils.getMonth().name() + "\n");
                writer.write("year:" + AranarthUtils.getYear() + "\n");
                writer.write("lastResourceWorldResetTime:" + AranarthUtils.getLastResourceWorldResetTime());

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the serverdate");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncServerDateToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the lockedcontainers file...");

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] fields = line.split("\\|");

                if (line.startsWith("#")) {
                    continue;
                }

                UUID owner = UUID.fromString(fields[0]);
                String[] trustedUuids = fields[1].split("\\*\\*\\*");
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
                    if (fields.length == 6) {
                        throw new NumberFormatException();
                    }
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

                LockedContainer lockedContainer = new LockedContainer(owner, trusted, new Location[]{loc1, loc2});
                lockedContainer.setWorldName(worldName);
                AranarthUtils.addLockedContainer(lockedContainer);
            }
            Bukkit.getLogger().info("[AC] All lockedcontainers have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the lockedcontainers!");
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
                    Bukkit.getLogger().info("[AC] A new lockedcontainers.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of lockedcontainers.txt");
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
                                trusted.append("***").append(trustedUuid.toString());
                            }
                        }
                        String trustedString = trusted.toString();
                        Location[] locations = container.getLocations();
                        String worldName = container.getWorldName();
                        if (worldName == null) {
                            Bukkit.getLogger().warning("[AC] Skipping locked container save: unknown world");
                            continue;
                        }
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
                        writer.write(row);
                    }
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the lockedcontainers");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncLockedContainersToDatabase);
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
            Bukkit.getLogger().info("[AC] Attempting to read the dominions file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();

                // Skip any commented out lines
                if (row.startsWith("#")) {
                    continue;
                }

                // #id|name|leader|members|allied|truced|enemied|world|chunks|x|y|z|yaw|pitch|food|claimableResources|conquered|balance|memberRanks
                String[] fields = row.split("\\|");

                UUID id = null;
                if (!fields[0].isEmpty()) {
                    id = UUID.fromString(fields[0]);
                }

                String name = fields[1];
                UUID leader = UUID.fromString(fields[2]);
                List<UUID> members = new ArrayList<>();
                String[] memberParts = fields[3].split("\\*\\*\\*");
                for (String member : memberParts) {
                    members.add(UUID.fromString(member));
                }

                List<UUID> allies = new ArrayList<>();
                String[] alliesParts = fields[4].split("\\*\\*\\*");
                if (!alliesParts[0].isEmpty()) {
                    for (String ally : alliesParts) {
                        allies.add(UUID.fromString(ally));
                    }
                }

                List<UUID> truced = new ArrayList<>();
                String[] trucedParts = fields[5].split("\\*\\*\\*");
                if (!trucedParts[0].isEmpty()) {
                    for (String truce : trucedParts) {
                        truced.add(UUID.fromString(truce));
                    }
                }

                List<UUID> enemies = new ArrayList<>();
                String[] enemyParts = fields[6].split("\\*\\*\\*");
                if (!enemyParts[0].isEmpty()) {
                    for (String enemy : enemyParts) {
                        enemies.add(UUID.fromString(enemy));
                    }
                }

                String worldName = fields[7];
                World world = Bukkit.getWorld(worldName);

                List<Chunk> chunks = new ArrayList<>();
                String[] claimedChunks = fields[8].split("\\*\\*\\*");
                for (String chunk : claimedChunks) {
                    String[] coordinates = chunk.split(",");
                    int x = Integer.parseInt(coordinates[0]);
                    int z = Integer.parseInt(coordinates[1]);
                    chunks.add(world.getChunkAt(x, z));
                }
                double x = Double.parseDouble(fields[9]);
                double y = Double.parseDouble(fields[10]);
                double z = Double.parseDouble(fields[11]);
                float yaw = Float.parseFloat(fields[12]);
                float pitch = Float.parseFloat(fields[13]);
                ItemStack[] food = new ItemStack[54];
                if (!fields[14].isEmpty()) {
                    food = ItemUtils.itemStackArrayFromBase64(fields[14]);
                }
                int claimableResources = Integer.parseInt(fields[15]);
                List<UUID> conquered = new ArrayList<>();
                String[] conqueredUuids = fields[16].split("_");
                for (String uuid : conqueredUuids) {
                    if (!uuid.isEmpty()) {
                        conquered.add(UUID.fromString(uuid));
                    }
                }

                // Keep balance at the end
                double balance = Double.parseDouble(fields[17]);

                Map<UUID, DominionRank> memberRanks = new HashMap<>();
                if (fields.length > 18 && !fields[18].isEmpty()) {
                    for (String entry : fields[18].split("\\*\\*\\*")) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            memberRanks.put(UUID.fromString(parts[0]), DominionRank.valueOf(parts[1]));
                        }
                    }
                }

                // Back-fill ranks for any members not found in the persisted map
                for (UUID memberUuid : members) {
                    if (!memberRanks.containsKey(memberUuid)) {
                        memberRanks.put(memberUuid, memberUuid.equals(leader) ? DominionRank.LEADER : DominionRank.CITIZEN);
                    }
                }

                boolean memberPvpEnabled = fields.length > 19 && fields[19].equals("1");
                boolean mobSpawningEnabled = fields.length > 20 && fields[20].equals("1");
                long conqueredRequestTimestamp = fields.length > 21 ? Long.parseLong(fields[21]) : 0L;
                long lastConquerAttemptTimestamp = fields.length > 22 ? Long.parseLong(fields[22]) : 0L;
                long rebelRequestTimestamp = fields.length > 23 ? Long.parseLong(fields[23]) : 0L;
                // Backward-compatible: old saves stored a boolean "0"/"1" for field 24; treat both as 0L (no timestamp)
                long conqueredRequestDefenderLastSeen = 0L;
                if (fields.length > 24) {
                    String f24 = fields[24];
                    if (!f24.equals("0") && !f24.equals("1")) {
                        conqueredRequestDefenderLastSeen = Long.parseLong(f24);
                    }
                }
                long rebelRequestConquerorLastSeen = fields.length > 25 ? Long.parseLong(fields[25]) : 0L;
                long lastRebelAttemptTimestamp = fields.length > 26 ? Long.parseLong(fields[26]) : 0L;
                long conqueredTimestamp = fields.length > 27 ? Long.parseLong(fields[27]) : 0L;
                int boughtChunks = fields.length > 28 ? Integer.parseInt(fields[28]) : 0;
                int dominionLevel = fields.length > 29 ? Integer.parseInt(fields[29]) : 1;
                int cachedFarmlandCount = fields.length > 30 ? Integer.parseInt(fields[30]) : 0;
                int cachedLivestockCount = fields.length > 31 ? Integer.parseInt(fields[31]) : 0;
                long foundedTimestamp = fields.length > 32 ? Long.parseLong(fields[32]) : 0L;
                // Migrate old real-time ms values (pre-calendar system) to 0 (Ancient)
                if (foundedTimestamp > 1_000_000_000L) {
                    foundedTimestamp = 0L;
                }
                long levelDropTimestamp = fields.length > 33 ? Long.parseLong(fields[33]) : 0L;
                int boughtOutpostChunks = fields.length > 34 ? Integer.parseInt(fields[34]) : 0;
                String cachedLivestockByWorldString = fields.length > 35 ? fields[35] : "";
                boolean bendingEnabled = fields.length <= 36 || !fields[36].equals("0");

                Dominion dominion = new Dominion(id, name, leader, members, memberRanks, allies, truced, enemies, worldName, chunks,
                        x, y, z, yaw, pitch, food, claimableResources, conquered, null,
                        // Keep balance at the end
                        balance);
                dominion.setMemberPvpEnabled(memberPvpEnabled);
                dominion.setMobSpawningEnabled(mobSpawningEnabled);
                dominion.setConqueredRequestTimestamp(conqueredRequestTimestamp);
                dominion.setLastConquerAttemptTimestamp(lastConquerAttemptTimestamp);
                dominion.setRebelRequestTimestamp(rebelRequestTimestamp);
                dominion.setConqueredRequestDefenderLastSeen(conqueredRequestDefenderLastSeen);
                dominion.setRebelRequestConquerorLastSeen(rebelRequestConquerorLastSeen);
                dominion.setLastRebelAttemptTimestamp(lastRebelAttemptTimestamp);
                dominion.setConqueredTimestamp(conqueredTimestamp);
                dominion.setBoughtChunks(boughtChunks);
                dominion.setDominionLevel(dominionLevel);
                dominion.setCachedFarmlandCount(cachedFarmlandCount);
                dominion.setCachedLivestockCount(cachedLivestockCount);
                dominion.setFoundedTimestamp(foundedTimestamp);
                dominion.setLevelDropTimestamp(levelDropTimestamp);
                dominion.setBoughtOutpostChunks(boughtOutpostChunks);
                dominion.setBendingEnabled(bendingEnabled);
                if (!cachedLivestockByWorldString.isEmpty()) {
                    for (String entry : cachedLivestockByWorldString.split(";")) {
                        String[] kv = entry.split("=", 2);
                        if (kv.length == 2) {
                            try {
                                dominion.getCachedLivestockByWorld().put(kv[0], Integer.parseInt(kv[1]));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
                DominionUtils.resizeFoodArray(dominion);
                DominionUtils.createDominion(dominion);
            }
            Bukkit.getLogger().info("[AC] All dominions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the dominions!");
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with instantiating a dominion's food");
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
                        Bukkit.getLogger().info("[AC] A new dominions.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of dominions.txt");
                }

                List<Dominion> dominions = DominionUtils.getDominions();
                try {
                    FileWriter writer = new FileWriter(filePath);
                    writer.write("#id|name|leader|members|allied|truced|enemied|world|chunks|x|y|z|yaw|pitch|food|claimableResources|conquered|balance|memberRanks|memberPvpEnabled|mobSpawningEnabled|conqueredRequestTimestamp|lastConquerAttemptTimestamp|rebelRequestTimestamp|conqueredRequestDefenderLastSeen|rebelRequestConquerorLastSeen|lastRebelAttemptTimestamp|conqueredTimestamp|boughtChunks|dominionLevel|cachedFarmlandCount|cachedLivestockCount|foundedTimestamp|levelDropTimestamp|boughtOutpostChunks|cachedLivestockByWorld|bendingEnabled\n");

                    if (dominions != null && !dominions.isEmpty()) {
                        for (Dominion dominion : dominions) {
                            String row = buildDominionRow(dominion) + "\n";
                            writer.write(row);
                        }
                    }
                    writer.close();
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] There was an error in saving the dominions!");
                }
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncDominionsToDatabase);
        }
    }

    /**
     * Builds the pipe-delimited row string for a single Dominion (without trailing newline).
     * Extracted from saveDominions() so it can be reused by syncDominionsToDatabase().
     */
    private static String buildDominionRow(Dominion dominion) {
        String name = dominion.getName();
        String leader = dominion.getLeader().toString();
        StringBuilder members = new StringBuilder();
        for (UUID memberUuid : dominion.getMembers()) {
            if (members.isEmpty()) {
                members = new StringBuilder(memberUuid.toString());
            } else {
                members.append("***").append(memberUuid.toString());
            }
        }
        String membersString = members.toString();

        StringBuilder allies = new StringBuilder();
        for (UUID alliedUuid : dominion.getAllied()) {
            if (allies.isEmpty()) {
                allies = new StringBuilder(alliedUuid.toString());
            } else {
                allies.append("***").append(alliedUuid.toString());
            }
        }
        String alliesString = allies.toString();

        StringBuilder truced = new StringBuilder();
        for (UUID trucedUuid : dominion.getTruced()) {
            if (truced.isEmpty()) {
                truced = new StringBuilder(trucedUuid.toString());
            } else {
                truced.append("***").append(trucedUuid.toString());
            }
        }
        String trucedString = truced.toString();

        StringBuilder enemies = new StringBuilder();
        for (UUID enemiedUuid : dominion.getEnemied()) {
            if (enemies.isEmpty()) {
                enemies = new StringBuilder(enemiedUuid.toString());
            } else {
                enemies.append("***").append(enemiedUuid.toString());
            }
        }
        String enemiesString = enemies.toString();

        String worldName = dominion.getDominionHomeWorldName();

        StringBuilder chunks = new StringBuilder();
        for (Chunk chunk : dominion.getChunks()) {
            String chunkXZ = chunk.getX() + "," + chunk.getZ();
            if (chunks.isEmpty()) {
                chunks = new StringBuilder(chunkXZ);
            } else {
                chunks.append("***").append(chunkXZ);
            }
        }
        String chunksString = chunks.toString();

        Location dominionHome = dominion.getDominionHome();
        String x = dominionHome.getX() + "";
        String y = dominionHome.getY() + "";
        String z = dominionHome.getZ() + "";
        String yaw = dominionHome.getYaw() + "";
        String pitch = dominionHome.getPitch() + "";
        String foodString = ItemUtils.itemStackArrayToBase64(dominion.getFood());
        int claimableResources = dominion.getClaimableResources();

        String conquered = "";
        for (int i = 0; i < dominion.getConquered().size(); i++) {
            conquered += dominion.getConquered().get(i);
            if (i < dominion.getConquered().size() - 1) {
                conquered += "_";
            }
        }

        // Keep balance at the end
        String balance = dominion.getBalance() + "";

        String dominionId = dominion.getId().toString();

        StringBuilder memberRanksBuilder = new StringBuilder();
        for (Map.Entry<UUID, DominionRank> entry : dominion.getMemberRanks().entrySet()) {
            if (!memberRanksBuilder.isEmpty()) {
                memberRanksBuilder.append("***");
            }
            memberRanksBuilder.append(entry.getKey()).append(":").append(entry.getValue().name());
        }
        String memberRanksString = memberRanksBuilder.toString();

        return dominionId + "|" + name + "|" + leader + "|" + membersString + "|" + alliesString + "|" + trucedString + "|"
                + enemiesString + "|" + worldName + "|" + chunksString + "|"
                + x + "|" + y + "|" + z + "|" + yaw + "|" + pitch + "|" + foodString + "|" + claimableResources + "|"
                + conquered + "|"
                // Keep balance before memberRanks
                + balance + "|" + memberRanksString + "|" + (dominion.isMemberPvpEnabled() ? "1" : "0") + "|" + (dominion.isMobSpawningEnabled() ? "1" : "0")
                + "|" + dominion.getConqueredRequestTimestamp() + "|" + dominion.getLastConquerAttemptTimestamp()
                + "|" + dominion.getRebelRequestTimestamp()
                + "|" + dominion.getConqueredRequestDefenderLastSeen()
                + "|" + dominion.getRebelRequestConquerorLastSeen()
                + "|" + dominion.getLastRebelAttemptTimestamp()
                + "|" + dominion.getConqueredTimestamp()
                + "|" + dominion.getBoughtChunks()
                + "|" + dominion.getDominionLevel()
                + "|" + dominion.getCachedFarmlandCount()
                + "|" + dominion.getCachedLivestockCount()
                + "|" + dominion.getFoundedTimestamp()
                + "|" + dominion.getLevelDropTimestamp()
                + "|" + dominion.getBoughtOutpostChunks()
                + "|" + dominion.getCachedLivestockByWorld().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(";"))
                + "|" + (dominion.isBendingEnabled() ? "1" : "0");
    }

    /**
     * Initializes dominion permissions from dominions_permissions.txt.
     * Must be called after loadDominions().
     */
    public static void loadDominionPermissions() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "dominions_permissions.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the dominions_permissions file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }

                // Format: dominionId|permissions
                // permissions: NEWCOMER:PERM1,PERM2;CITIZEN:PERM3;ALLIED:PERM4,...
                String[] fields = row.split("\\|", -1);
                if (fields.length < 2) {
                    continue;
                }

                UUID dominionId = UUID.fromString(fields[0]);
                Dominion dominion = DominionUtils.getDominionById(dominionId);
                if (dominion == null) {
                    continue;
                }

                Map<DominionRank, Set<DominionPermission>> allPerms = new EnumMap<>(DominionRank.class);
                if (!fields[1].isEmpty()) {
                    for (String rankEntry : fields[1].split(";")) {
                        String[] parts = rankEntry.split(":", 2);
                        if (parts.length == 2) {
                            try {
                                DominionRank rank = DominionRank.valueOf(parts[0]);
                                Set<DominionPermission> perms = new HashSet<>();
                                if (!parts[1].isEmpty()) {
                                    for (String permName : parts[1].split(",")) {
                                        try {
                                            perms.add(DominionPermission.valueOf(permName));
                                        } catch (IllegalArgumentException ignored) {
                                        }
                                    }
                                }
                                allPerms.put(rank, perms);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }

                // LEADER always has all permissions regardless of what was persisted,
                // since new permissions added to the enum would otherwise be missing.
                allPerms.put(DominionRank.LEADER, new HashSet<>(Arrays.asList(DominionPermission.values())));
                dominion.setDominionPermissions(new DominionPermissions(allPerms));
            }
            Bukkit.getLogger().info("[AC] All dominion permissions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading dominion permissions!");
        }
    }

    /**
     * Saves dominion permissions to dominions_permissions.txt.
     */
    public static void saveDominionPermissions() {
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null || dominions.isEmpty()) {
            return;
        }

        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "dominions_permissions.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (isDirectoryCreated) {
            try {
                // If the file isn't already there
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new dominions_permissions.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred creating dominions_permissions.txt");
                return;
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#dominionId|permissions\n");
                writer.write("#permissions format: RANK:PERM1,PERM2;RANK2:PERM3,...\n");

                for (Dominion dominion : dominions) {
                    DominionPermissions perms = dominion.getDominionPermissions();
                    if (perms == null) {
                        continue;
                    }

                    // Serialize all permissions (ranks and relations) into one field
                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<DominionRank, Set<DominionPermission>> entry : perms.getPermissionsMap().entrySet()) {
                        if (!builder.isEmpty()) {
                            builder.append(";");
                        }
                        String permList = entry.getValue().stream()
                                .map(DominionPermission::name)
                                .collect(Collectors.joining(","));
                        builder.append(entry.getKey().name()).append(":").append(permList);
                    }

                    writer.write(dominion.getId().toString() + "|" + builder + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error saving dominion permissions!");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncDominionPermissionsToDatabase);
        }
    }

    /**
     * Loads per-player dominion permission overrides from dominions_player_permissions.txt.
     */
    public static void loadDominionPlayerPermissions() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "dominions_player_permissions.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the dominions_player_permissions file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#") || row.isBlank()) {
                    continue;
                }

                String[] fields = row.split("\\|", -1);
                if (fields.length < 2) {
                    continue;
                }

                UUID dominionId;
                try {
                    dominionId = UUID.fromString(fields[0]);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                Dominion dominion = DominionUtils.getDominionById(dominionId);
                if (dominion == null) {
                    continue;
                }

                Map<UUID, Map<DominionPermission, Boolean>> allOverrides = new HashMap<>();

                if (!fields[1].isEmpty()) {
                    for (String playerEntry : fields[1].split(";")) {
                        String[] parts = playerEntry.split(":", 2);
                        if (parts.length != 2) {
                            continue;
                        }

                        UUID playerUuid;
                        try {
                            playerUuid = UUID.fromString(parts[0]);
                        } catch (IllegalArgumentException ignored) {
                            continue;
                        }

                        Map<DominionPermission, Boolean> overrides = new HashMap<>();
                        if (!parts[1].isEmpty()) {
                            for (String permEntry : parts[1].split(",")) {
                                String[] kv = permEntry.split("=", 2);
                                if (kv.length != 2) {
                                    continue;
                                }
                                try {
                                    DominionPermission perm = DominionPermission.valueOf(kv[0]);
                                    boolean value = Boolean.parseBoolean(kv[1]);
                                    overrides.put(perm, value);
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                        if (!overrides.isEmpty()) {
                            allOverrides.put(playerUuid, overrides);
                        }
                    }
                }

                dominion.setPlayerPermissionOverrides(allOverrides);
            }

            Bukkit.getLogger().info("[AC] All dominion player permissions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading dominion player permissions!");
        }
    }

    /**
     * Saves per-player dominion permission overrides to dominions_player_permissions.txt.
     */
    public static void saveDominionPlayerPermissions() {
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null || dominions.isEmpty()) {
            return;
        }

        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "dominions_player_permissions.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new dominions_player_permissions.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred creating dominions_player_permissions.txt");
                return;
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#dominionId|playerOverrides\n");
                writer.write("#format: dominionId|playerUUID:PERM1=true,PERM2=false;playerUUID2:PERM3=true\n");

                for (Dominion dominion : dominions) {
                    Map<UUID, Map<DominionPermission, Boolean>> allOverrides =
                            dominion.getPlayerPermissionOverrides();
                    if (allOverrides.isEmpty()) {
                        continue;
                    }

                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<UUID, Map<DominionPermission, Boolean>> playerEntry
                            : allOverrides.entrySet()) {
                        if (playerEntry.getValue().isEmpty()) {
                            continue;
                        }

                        if (!builder.isEmpty()) {
                            builder.append(";");
                        }
                        builder.append(playerEntry.getKey().toString()).append(":");

                        String permStr = playerEntry.getValue().entrySet().stream()
                                .map(e -> e.getKey().name() + "=" + e.getValue())
                                .collect(Collectors.joining(","));
                        builder.append(permStr);
                    }

                    if (!builder.isEmpty()) {
                        writer.write(dominion.getId().toString() + "|" + builder + "\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error saving dominion player permissions!");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncDominionPlayerPermissionsToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the warps file...");
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
                Home warp = new Home(warpName, location, icon, worldName);
                warps.add(warp);
            }
            AranarthUtils.setWarps(warps);
            Bukkit.getLogger().info("[AC] All warps have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the warps!");
        }
    }

    /**
     * Saves the contents of the warps list to the warps.txt file.
     */
    public static void saveWarps() {
        List<Home> warps = AranarthUtils.getWarps();
        if (warps != null) {
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
                        Bukkit.getLogger().info("[AC] A new warps.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("[AC] An error occurred in the creation of warps.txt");
                }

                try {
                    FileWriter writer = new FileWriter(filePath);
                    writer.write("#warpName|worldName|x|y|z|yaw|pitch|icon\n");

                    for (Home warp : AranarthUtils.getWarps()) {
                        String warpName = warp.getName();
                        String worldName = warp.getWorldName();
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
                    Bukkit.getLogger().info("[AC] There was an error in saving the warps");
                }
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncWarpsToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the punishments file...");
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
            Bukkit.getLogger().info("[AC] All punishments have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the punishments!");
        }
    }

    /**
     * Saves the contents of the punishments list to the punishments.txt file.
     */
    public static void savePunishments() {
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
                    Bukkit.getLogger().info("[AC] A new punishments.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of punishments.txt");
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
                Bukkit.getLogger().info("[AC] There was an error in saving the punishments");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncPunishmentsToDatabase);
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

            Bukkit.getLogger().info("[AC] Attempting to read the avatars file...");
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
                    if (row.contains("*")) {
                        String noHashtag = row.substring(1);
                        String[] parts = noHashtag.split("\\*");

                        Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
                        if (currentAvatar != null) {
                            OfflineBendingPlayer bendingPlayer = BendingPlayer.getOfflineBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()).getName());
                            if (bendingPlayer != null) {
                                bendingPlayer.bindAbility(parts[1], Integer.parseInt(parts[0]));
                            }
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
            Bukkit.getLogger().info("[AC] All avatars have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the avatars!");
        }
    }

    /**
     * Saves the contents of the avatars list to the avatars.txt file.
     */
    public static void saveAvatars() {
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
                    Bukkit.getLogger().info("[AC] A new avatars.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of avatars.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);

                // Saving the avatar's binds to ensure they are not reset upon relogging
                Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
                if (currentAvatar != null) {
                    OfflineBendingPlayer currentAvatarBendingPlayer = BendingPlayer.getBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
                    if (currentAvatarBendingPlayer != null) {
                        for (int index : currentAvatarBendingPlayer.getAbilities().keySet()) {
                            writer.write("#" + index + "*" + currentAvatarBendingPlayer.getAbilities().get(index) + "\n");
                        }
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
                Bukkit.getLogger().info("[AC] There was an error in saving the avatars");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncAvatarsToDatabase);
        }
    }

    /**
     * Initializes the avatar's binds' based on the contents of avatar_binds.txt.
     */
    public static void loadAvatarBinds() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "avatar_binds.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the avatar_binds file...");
            HashMap<UUID, List<Punishment>> punishments = new HashMap<>();

            while (reader.hasNextLine()) {
                String row = reader.nextLine();

                // Applies the avatar's binds to ensure they are not reset upon relogging
                String[] parts = row.split("\\*");
                Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
                if (currentAvatar != null) {
                    OfflineBendingPlayer bendingPlayer = BendingPlayer.getOfflineBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()).getName());
                    if (bendingPlayer != null) {
                        bendingPlayer.bindAbility(parts[1], Integer.parseInt(parts[0]));
                    }
                }
            }
            Bukkit.getLogger().info("[AC] The avatar's binds have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the avatar's binds!");
        }
    }

    /**
     * Saves the avatar's binds to the avatar_binds.txt file.
     */
    public static void saveAvatarBinds() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "avatar_binds.txt";
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
                    Bukkit.getLogger().info("[AC] A new avatar_binds.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of avatar_binds.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);

                // Saving the avatar's binds to ensure they are not reset upon relogging
                Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
                if (currentAvatar != null) {
                    BendingPlayer currentAvatarBendingPlayer = BendingPlayer.getBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
                    if (currentAvatarBendingPlayer != null) {
                        for (int index : currentAvatarBendingPlayer.getAbilities().keySet()) {
                            writer.write(index + "*" + currentAvatarBendingPlayer.getAbilities().get(index) + "\n");
                        }
                    }
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the avatar binds");
            }
        }
        // MySQL sync (updates the same server_avatars table)
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncAvatarsToDatabase);
        }
    }

    /**
     * Initializes the active server boosts based on the contents of boosts.txt.
     */
    public static void loadBoosts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "boosts.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the server boosts file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                String[] parts = row.split("\\|");

                Boost boost = Boost.valueOf(parts[0]);
                LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[1])), ZoneId.systemDefault());
                AranarthUtils.addServerBoost(boost, end, null, false);
            }
            Bukkit.getLogger().info("[AC] The server boosts have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the server boosts");
        }
    }

    /**
     * Saves the active server boosts to the boosts.txt file.
     */
    public static void saveBoosts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "boosts.txt";
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
                    Bukkit.getLogger().info("[AC] A new boosts.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of boosts.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);

                HashMap<Boost, LocalDateTime> boosts = AranarthUtils.getServerBoosts();
                if (!boosts.isEmpty()) {
                    for (Boost boost : boosts.keySet()) {
                        LocalDateTime ldt = boosts.get(boost);
                        writer.write(boost.name() + "|" + ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + "\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the server boosts");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncBoostsToDatabase);
        }
    }

    /**
     * Initializes the compressible items lists based on the contents of compressible.txt.
     */
    public static void loadCompressible() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "compressible.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the compressible items lists file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                String[] parts = row.split("\\*");
                UUID uuid = UUID.fromString(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    Material material = Material.valueOf(parts[i]);
                    AranarthUtils.addCompressibleItem(uuid, material);
                }
            }
            Bukkit.getLogger().info("[AC] The compressible items lists have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the compressible items lists");
        }
    }

    /**
     * Saves the compressible items lists to the compressible.txt file.
     */
    public static void saveCompressible() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "compressible.txt";
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
                    Bukkit.getLogger().info("[AC] A new compressible.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of compressible.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                HashMap<UUID, List<Material>> compressibleTypes = AranarthUtils.getCompressibleTypes();
                for (UUID uuid : compressibleTypes.keySet()) {
                    String line = uuid.toString() + "*";
                    List<Material> materials = compressibleTypes.get(uuid);
                    if (materials.isEmpty()) {
                        continue;
                    } else {
                        for (int i = 0; i < materials.size(); i++) {
                            Material material = materials.get(i);
                            line += material.name();
                            if (i < materials.size() - 1) {
                                line += "*";
                            }
                        }
                    }
                    writer.write(line + "\n");
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the compressible items lists");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncCompressibleToDatabase);
        }
    }

    /**
     * Loads the shop locations based on the contents of shop_locations.txt.
     */
    public static void loadShopLocations() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "shop_locations.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the shop locations file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }
                // Split off optional custom name (appended after '|')
                String customName = null;
                int pipeIdx = row.indexOf('|');
                if (pipeIdx >= 0) {
                    customName = row.substring(pipeIdx + 1);
                    row = row.substring(0, pipeIdx);
                }
                String[] parts = row.split("_");
                UUID uuid = UUID.fromString(parts[0]);
                String world = parts[1];
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                double z = Double.parseDouble(parts[4]);
                float yaw = Float.parseFloat(parts[5]);
                float pitch = Float.parseFloat(parts[6]);
                Location location = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                AranarthUtils.createShopLocation(uuid, location);

                // Extended format: includes island center coordinates (parts[7] and parts[8])
                if (parts.length >= 9) {
                    int centerX = Integer.parseInt(parts[7]);
                    int centerZ = Integer.parseInt(parts[8]);
                    AranarthUtils.addShopIslandCenter(uuid, centerX, centerZ);
                }

                if (customName != null && !customName.isEmpty()) {
                    AranarthUtils.setShopName(uuid, customName);
                }
            }
            Bukkit.getLogger().info("[AC] The shop locations have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the shop locations");
        }
    }

    /**
     * Saves the shop locations to the shop_locations.txt file.
     */
    public static void saveShopLocations() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "shop_locations.txt";
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
                    Bukkit.getLogger().info("[AC] A new shop_locations.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of shop_locations.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
                HashMap<UUID, int[]> shopIslandCenters = AranarthUtils.getShopIslandCenters();
                for (UUID uuid : shopLocations.keySet()) {
                    Location location = shopLocations.get(uuid);
                    if (location.getWorld() == null) {
                        Bukkit.getLogger().warning("[AC] Skipping shop island location save for " + uuid + ": null world (shutdown race)");
                        continue;
                    }
                    String shopLocation = uuid + "_";
                    shopLocation += location.getWorld().getName() + "_";
                    shopLocation += location.getX() + "_";
                    shopLocation += location.getY() + "_";
                    shopLocation += location.getZ() + "_";
                    shopLocation += location.getYaw() + "_";
                    shopLocation += location.getPitch();
                    // Append island center if available
                    int[] center = shopIslandCenters.get(uuid);
                    if (center != null) {
                        shopLocation += "_" + center[0] + "_" + center[1];
                    }
                    // Append custom shop name if set
                    String customName = AranarthUtils.getShopNames().get(uuid);
                    if (customName != null && !customName.isEmpty()) {
                        shopLocation += "|" + customName;
                    }
                    writer.write(shopLocation + "\n");
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the shop locations");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncShopLocationsToDatabase);
        }
    }

    /**
     * Loads the shop island counter from config.yml (key: shop.island-counter).
     * Guards against a stale/missing counter (e.g. after a server migration) by
     * computing the minimum safe value from the already-loaded island centers and
     * using whichever is larger.
     */
    public static void loadShopIslandCounter() {
        int counter = AranarthCore.getInstance().getConfig().getInt("shop.island-counter", 0);

        // Derive the minimum safe counter from loaded island centers so we never
        // collide with an existing plot if config.yml was reset or not migrated.
        int minSafe = 0;
        for (int[] center : AranarthUtils.getShopIslandCenters().values()) {
            int col = (center[0] - ShopIslandUtils.GRID_SIZE / 2) / ShopIslandUtils.GRID_SIZE;
            int row = (center[1] - ShopIslandUtils.GRID_SIZE / 2) / ShopIslandUtils.GRID_SIZE;
            int index = row * ShopIslandUtils.GRID_ROW_WIDTH + col;
            if (index + 1 > minSafe) {
                minSafe = index + 1;
            }
        }

        if (minSafe > counter) {
            Bukkit.getLogger().warning("[AC] Shop island counter in config.yml (" + counter + ") is behind the highest known island index. Correcting to " + minSafe + ".");
            counter = minSafe;
        }

        AranarthUtils.setShopIslandCounter(counter);
        Bukkit.getLogger().info("[AC] Shop island counter loaded: " + counter);
    }

    /**
     * Saves the shop island counter to config.yml (key: shop.island-counter).
     */
    public static void saveShopIslandCounter() {
        AranarthCore.getInstance().getConfig().set("shop.island-counter", AranarthUtils.getShopIslandCounter());
        AranarthCore.getInstance().saveConfig();
    }

    /**
     * Loads shop collaborators from shop_collaborators.txt.
     */
    public static void loadShopCollaborators() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "shop_collaborators.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the shop collaborators file...");
            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.isEmpty() || row.startsWith("#")) {
                    continue;
                }
                String[] parts = row.split(":");
                if (parts.length < 2) {
                    continue;
                }
                UUID ownerUuid = UUID.fromString(parts[0]);
                for (String collab : parts[1].split(",")) {
                    collab = collab.trim();
                    if (!collab.isEmpty()) {
                        AranarthUtils.addShopCollaborator(ownerUuid, UUID.fromString(collab));
                    }
                }
            }
            reader.close();
            Bukkit.getLogger().info("[AC] Shop collaborators have been initialized");
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading shop collaborators");
        }
    }

    /**
     * Saves shop collaborators to shop_collaborators.txt.
     */
    public static void saveShopCollaborators() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "shop_collaborators.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new shop_collaborators.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of shop_collaborators.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                for (Map.Entry<UUID, Set<UUID>> entry : AranarthUtils.getShopCollaborators().entrySet()) {
                    if (entry.getValue().isEmpty()) {
                        continue;
                    }
                    StringBuilder sb = new StringBuilder(entry.getKey().toString()).append(":");
                    sb.append(String.join(",", entry.getValue().stream().map(UUID::toString).toArray(String[]::new)));
                    writer.write(sb + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving shop collaborators");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncShopCollaboratorsToDatabase);
        }
    }

    /**
     * Loads the votes on the contents of votes.txt.
     */
    public static void loadVotes() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "votes.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the votes file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }

                String[] parts = row.split("\\|");
                // #uuid|keyNum|timestamp
                UUID uuid = UUID.fromString(parts[0]);
                int keyNum = Integer.parseInt(parts[1]);
                long timestamp = Long.parseLong(parts[2]);
                AranarthUtils.addVote(new AranarthVote(uuid, keyNum, timestamp));
            }
            Bukkit.getLogger().info("[AC] The votes have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the votes");
        }
    }

    /**
     * Saves the votes to the votes.txt file.
     */
    public static void saveVotes() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "votes.txt";
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
                    Bukkit.getLogger().info("[AC] A new votes.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of votes.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|keyNum|timestamp\n");
                for (AranarthVote vote : AranarthUtils.getVotes()) {
                    UUID uuid = vote.getUuid();
                    int keyNum = vote.getPointsRewarded();
                    long timestamp = vote.getTimestamp();
                    writer.write(uuid.toString() + "|" + keyNum + "|" + timestamp + "\n");
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the votes");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncVotesToDatabase);
        }
    }

    /**
     * Loads the sentinels based on the contents of sentinels.txt.
     */
    public static void loadSentinels() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "sentinels.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the sentinels file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                String[] playerParts = row.split("\\|");
                UUID playerUuid = UUID.fromString(playerParts[0]);
                AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(playerUuid);
                if (aranarthPlayer == null) {
                    continue;
                }

                HashMap<EntityType, List<Sentinel>> sentinels = new HashMap<>();

                List<Sentinel> horse = new ArrayList<>();
                String[] horseParts = playerParts[1].split("___");
                for (int i = 1; i < horseParts.length; i++) {
                    String[] parts = horseParts[i].split("_");
                    UUID uuid = UUID.fromString(parts[0]);
                    World world = Bukkit.getWorld(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int z = Integer.parseInt(parts[4]);
                    Location loc = new Location(world, x, y, z);
                    Sentinel sentinel = new Sentinel(uuid, EntityType.HORSE, loc);
                    sentinel.setWorldName(parts[1]);
                    horse.add(sentinel);
                }

                List<Sentinel> ironGolems = new ArrayList<>();
                String[] golemParts = playerParts[2].split("___");
                for (int i = 1; i < golemParts.length; i++) {
                    String[] parts = golemParts[i].split("_");
                    UUID uuid = UUID.fromString(parts[0]);
                    World world = Bukkit.getWorld(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int z = Integer.parseInt(parts[4]);
                    Location loc = new Location(world, x, y, z);
                    Sentinel sentinel = new Sentinel(uuid, EntityType.IRON_GOLEM, loc);
                    sentinel.setWorldName(parts[1]);
                    ironGolems.add(sentinel);
                }

                List<Sentinel> wolves = new ArrayList<>();
                String[] wolfParts = playerParts[3].split("___");
                for (int i = 1; i < wolfParts.length; i++) {
                    String[] parts = wolfParts[i].split("_");
                    UUID uuid = UUID.fromString(parts[0]);
                    World world = Bukkit.getWorld(parts[1]);
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    int z = Integer.parseInt(parts[4]);
                    Location loc = new Location(world, x, y, z);
                    Sentinel sentinel = new Sentinel(uuid, EntityType.WOLF, loc);
                    sentinel.setWorldName(parts[1]);
                    wolves.add(sentinel);
                }

                sentinels.put(EntityType.HORSE, horse);
                sentinels.put(EntityType.IRON_GOLEM, ironGolems);
                sentinels.put(EntityType.WOLF, wolves);
                aranarthPlayer.setSentinels(sentinels);
                AranarthUtils.setPlayer(playerUuid, aranarthPlayer);
            }
            Bukkit.getLogger().info("[AC] The sentinels have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the sentinels");
        }
    }

    /**
     * Saves the sentinels to the sentinels.txt file.
     */
    public static void saveSentinels() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "sentinels.txt";
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
                    Bukkit.getLogger().info("[AC] A new sentinels.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of sentinels.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                HashMap<UUID, AranarthPlayer> aranarthPlayers = AranarthUtils.getAranarthPlayers();
                for (UUID uuid : aranarthPlayers.keySet()) {
                    AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(uuid);
                    HashMap<EntityType, List<Sentinel>> sentinels = aranarthPlayer.getSentinels();
                    if (sentinels == null || sentinels.isEmpty()) {
                        continue;
                    }

                    String playerSentinels = uuid + "|";

                    // Ensures that all types of sentinels have been initialized
                    if (sentinels.get(EntityType.HORSE) == null) {
                        sentinels.put(EntityType.HORSE, new ArrayList<>());
                    }
                    playerSentinels += EntityType.HORSE.name() + "___";
                    for (Sentinel sentinel : sentinels.get(EntityType.HORSE)) {
                        playerSentinels += sentinel.getUuid() + "_";
                        Location loc = sentinel.getLocation();
                        playerSentinels += sentinel.getWorldName() + "_";
                        playerSentinels += loc.getBlockX() + "_";
                        playerSentinels += loc.getBlockY() + "_";
                        playerSentinels += loc.getBlockZ() + "___";
                    }
                    playerSentinels += "|";

                    if (sentinels.get(EntityType.IRON_GOLEM) == null) {
                        sentinels.put(EntityType.IRON_GOLEM, new ArrayList<>());
                    }
                    playerSentinels += EntityType.IRON_GOLEM.name() + "___";
                    for (Sentinel sentinel : sentinels.get(EntityType.IRON_GOLEM)) {
                        playerSentinels += sentinel.getUuid() + "_";
                        Location loc = sentinel.getLocation();
                        playerSentinels += sentinel.getWorldName() + "_";
                        playerSentinels += loc.getBlockX() + "_";
                        playerSentinels += loc.getBlockY() + "_";
                        playerSentinels += loc.getBlockZ() + "___";
                    }
                    playerSentinels += "|";

                    if (sentinels.get(EntityType.WOLF) == null) {
                        sentinels.put(EntityType.WOLF, new ArrayList<>());
                    }
                    playerSentinels += EntityType.WOLF.name() + "___";
                    for (Sentinel sentinel : sentinels.get(EntityType.WOLF)) {
                        playerSentinels += sentinel.getUuid() + "_";
                        Location loc = sentinel.getLocation();
                        playerSentinels += sentinel.getWorldName() + "_";
                        playerSentinels += loc.getBlockX() + "_";
                        playerSentinels += loc.getBlockY() + "_";
                        playerSentinels += loc.getBlockZ() + "___";
                    }

                    playerSentinels += "\n";
                    writer.write(playerSentinels);
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the sentinels");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncSentinelsToDatabase);
        }
    }

    /**
     * Initializes the kill and death counts based on the contents of kills_and_deaths.txt.
     */
    public static void loadKillDeathCount() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "kills_and_deaths.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);

            Bukkit.getLogger().info("[AC] Attempting to read the kills and deaths file...");
            HashMap<UUID, List<Punishment>> punishments = new HashMap<>();

            while (reader.hasNextLine()) {
                String row = reader.nextLine();

                // Skip any commented out lines
                if (row.startsWith("#")) {
                    continue;
                }

                // uuid|worldPrefix|kills|deaths
                String[] fields = row.split("\\|");

                UUID uuid = UUID.fromString(fields[0]);
                String worldPrefix = fields[1];
                int kills = Integer.parseInt(fields[2]);
                int deaths = Integer.parseInt(fields[3]);

                PlayerKillDeathScore pkds = new PlayerKillDeathScore(uuid, worldPrefix, kills, deaths);
                AranarthUtils.addPlayerKillDeathScore(pkds);
            }
            Bukkit.getLogger().info("[AC] All kills and deaths have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the kills and deaths!");
        }
    }

    /**
     * Saves the kill and death counts to the kills_and_deaths.txt file.
     */
    public static void saveKillDeathCount() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "kills_and_deaths.txt";
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
                    Bukkit.getLogger().info("[AC] A new kills_and_deaths.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of kills_and_deaths.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);

                writer.write("#uuid|worldPrefix|kills|deaths\n");

                for (UUID uuid : AranarthUtils.getKillDeathScores().keySet()) {
                    for (PlayerKillDeathScore pkds : AranarthUtils.getKillDeathScores().get(uuid)) {
                        String world = pkds.getWorldPrefix();
                        int kills = pkds.getKills();
                        int deaths = pkds.getDeaths();

                        writer.write(uuid + "|" + world + "|" + kills + "|" + deaths + "\n");
                    }
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the kills and deaths");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncKillDeathToDatabase);
        }
    }

    /**
     * Loads the pending vote keys from vote_keys.txt.
     */
    public static void loadVoteKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore" + File.separator
                + "vote_keys.txt";
        File file = new File(filePath);

        // First run of plugin
        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the vote keys file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }

                String[] parts = row.split("\\|");
                UUID uuid = UUID.fromString(parts[0]);
                int amount = Integer.parseInt(parts[1]);
                AranarthUtils.setPendingVoteKeys(uuid, amount);
            }
            Bukkit.getLogger().info("[AC] All pending vote keys have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the vote keys!");
        }
    }

    /**
     * Saves the pending vote keys to the vote_keys.txt file.
     */
    public static void saveVoteKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "vote_keys.txt";
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
                    Bukkit.getLogger().info("[AC] A new vote_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of vote_keys.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingVoteKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the vote keys");
            }
        }
    }

    public static void loadRareKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "rare_keys.txt";
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try {
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }
                String[] parts = row.split("\\|");
                AranarthUtils.setPendingRareKeys(UUID.fromString(parts[0]), Integer.parseInt(parts[1]));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the rare keys!");
        }
    }

    public static void saveRareKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "rare_keys.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);
        boolean isDirectoryCreated = pluginDirectory.isDirectory() || pluginDirectory.mkdir();
        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new rare_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of rare_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingRareKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the rare keys");
            }
        }
    }

    public static void loadEpicKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "epic_keys.txt";
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try {
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }
                String[] parts = row.split("\\|");
                AranarthUtils.setPendingEpicKeys(UUID.fromString(parts[0]), Integer.parseInt(parts[1]));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the epic keys!");
        }
    }

    public static void saveEpicKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "epic_keys.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);
        boolean isDirectoryCreated = pluginDirectory.isDirectory() || pluginDirectory.mkdir();
        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new epic_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of epic_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingEpicKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the epic keys");
            }
        }
    }

    public static void loadGodlyKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "godly_keys.txt";
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        try {
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }
                String[] parts = row.split("\\|");
                AranarthUtils.setPendingGodlyKeys(UUID.fromString(parts[0]), Integer.parseInt(parts[1]));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the godly keys!");
        }
    }

    public static void saveGodlyKeys() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "godly_keys.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);
        boolean isDirectoryCreated = pluginDirectory.isDirectory() || pluginDirectory.mkdir();
        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new godly_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of godly_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingGodlyKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the godly keys");
            }
        }
        // MySQL sync (covers votes + all key types)
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncVotesToDatabase);
        }
    }

    // -------------------------------------------------------------------------
    // Quest State Persistence
    // -------------------------------------------------------------------------

    /**
     * Loads reset timestamps from quest_state.txt.
     */
    public static void loadQuestState() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "quest_state.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the quest_state file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }

                if (row.startsWith("lastDailyReset|")) {
                    QuestUtils.setLastDailyReset(Long.parseLong(row.split("\\|")[1]));
                } else if (row.startsWith("lastWeeklyReset|")) {
                    QuestUtils.setLastWeeklyReset(Long.parseLong(row.split("\\|")[1]));
                }
            }

            Bukkit.getLogger().info("[AC] Quest state has been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the quest state!");
        }
    }

    /**
     * Saves reset timestamps to quest_state.txt.
     */
    public static void saveQuestState() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "quest_state.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            // If the file isn't already there
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new quest_state.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of quest_state.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#Quest state — do not edit manually\n");
            writer.write("lastDailyReset|" + QuestUtils.getLastDailyReset() + "\n");
            writer.write("lastWeeklyReset|" + QuestUtils.getLastWeeklyReset() + "\n");
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error in saving the quest state");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncQuestDataToDatabase);
        }
    }

    // -------------------------------------------------------------------------
    // Quest Progress Persistence
    // -------------------------------------------------------------------------

    /**
     * Loads per-player quest progress (including active quest assignments) from quest_progress.txt.
     * Format: uuid|rank|d0task|d0prog|d0done|d0claimed|d1task|d1prog|d1done|d1claimed|d2task|d2prog|d2done|d2claimed|
     * w0task|w0prog|w0done|w0claimed|w1task|w1prog|w1done|w1claimed|w2task|w2prog|w2done|w2claimed
     */
    public static void loadQuestProgress() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "quest_progress.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the quest_progress file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }

                String[] fields = row.split("\\|");
                if (fields.length < 26) {
                    continue;
                }

                UUID uuid = UUID.fromString(fields[0]);
                int rank = Integer.parseInt(fields[1]);

                // New format (32 fields): task|reward|prog|done|claimed per quest slot
                // Old format (26 fields): task|prog|done|claimed per quest slot (reward falls back to random)
                boolean hasRewards = fields.length >= 32;

                String[] dTasks;
                double[] dRewards;
                int[] dailyProgress;
                boolean[] dailyCompleted, dailyClaimed;
                String[] wTasks;
                double[] wRewards;
                int[] weeklyProgress;
                boolean[] weeklyCompleted, weeklyClaimed;

                if (hasRewards) {
                    dTasks = new String[]{fields[2], fields[7], fields[12]};
                    dRewards = new double[]{Double.parseDouble(fields[3]), Double.parseDouble(fields[8]), Double.parseDouble(fields[13])};
                    dailyProgress = new int[]{Integer.parseInt(fields[4]), Integer.parseInt(fields[9]), Integer.parseInt(fields[14])};
                    dailyCompleted = new boolean[]{fields[5].equals("1"), fields[10].equals("1"), fields[15].equals("1")};
                    dailyClaimed = new boolean[]{fields[6].equals("1"), fields[11].equals("1"), fields[16].equals("1")};
                    wTasks = new String[]{fields[17], fields[22], fields[27]};
                    wRewards = new double[]{Double.parseDouble(fields[18]), Double.parseDouble(fields[23]), Double.parseDouble(fields[28])};
                    weeklyProgress = new int[]{Integer.parseInt(fields[19]), Integer.parseInt(fields[24]), Integer.parseInt(fields[29])};
                    weeklyCompleted = new boolean[]{fields[20].equals("1"), fields[25].equals("1"), fields[30].equals("1")};
                    weeklyClaimed = new boolean[]{fields[21].equals("1"), fields[26].equals("1"), fields[31].equals("1")};
                } else {
                    // Old format — rewards will be regenerated randomly
                    dTasks = new String[]{fields[2], fields[6], fields[10]};
                    dRewards = new double[]{0.0, 0.0, 0.0};
                    dailyProgress = new int[]{Integer.parseInt(fields[3]), Integer.parseInt(fields[7]), Integer.parseInt(fields[11])};
                    dailyCompleted = new boolean[]{fields[4].equals("1"), fields[8].equals("1"), fields[12].equals("1")};
                    dailyClaimed = new boolean[]{fields[5].equals("1"), fields[9].equals("1"), fields[13].equals("1")};
                    wTasks = new String[]{fields[14], fields[18], fields[22]};
                    wRewards = new double[]{0.0, 0.0, 0.0};
                    weeklyProgress = new int[]{Integer.parseInt(fields[15]), Integer.parseInt(fields[19]), Integer.parseInt(fields[23])};
                    weeklyCompleted = new boolean[]{fields[16].equals("1"), fields[20].equals("1"), fields[24].equals("1")};
                    weeklyClaimed = new boolean[]{fields[17].equals("1"), fields[21].equals("1"), fields[25].equals("1")};
                }

                // Restore active daily quests from the pool using stored task types and rewards
                List<Quest> activeDailyQuests = resolveQuestsFromPool(uuid, rank, dTasks, dRewards, QuestType.DAILY);
                List<Quest> activeWeeklyQuests = resolveQuestsFromPool(uuid, rank, wTasks, wRewards, QuestType.WEEKLY);

                if (activeDailyQuests != null) {
                    QuestUtils.setPlayerActiveDailyQuests(uuid, activeDailyQuests);
                }
                if (activeWeeklyQuests != null) {
                    QuestUtils.setPlayerActiveWeeklyQuests(uuid, activeWeeklyQuests);
                }

                QuestUtils.getPlayerDailyProgress().put(uuid, dailyProgress);
                QuestUtils.getPlayerDailyCompleted().put(uuid, dailyCompleted);
                QuestUtils.getPlayerDailyClaimed().put(uuid, dailyClaimed);
                QuestUtils.getPlayerWeeklyProgress().put(uuid, weeklyProgress);
                QuestUtils.getPlayerWeeklyCompleted().put(uuid, weeklyCompleted);
                QuestUtils.getPlayerWeeklyClaimed().put(uuid, weeklyClaimed);
                QuestUtils.getPlayerQuestRank().put(uuid, rank);
            }

            Bukkit.getLogger().info("[AC] Quest progress has been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading quest progress!");
        }
    }

    /**
     * Resolves Quest objects from the quest pool using stored task type names and rewards.
     * If a stored reward is 0 (old format), a new random reward is generated.
     * Returns null if no valid quests could be resolved.
     */
    private static List<Quest> resolveQuestsFromPool(UUID uuid, int rank, String[] taskNames, double[] rewards, QuestType type) {
        List<Quest> pool = type == QuestType.DAILY ? QuestUtils.getDailyQuestPool(rank) : QuestUtils.getWeeklyQuestPool(rank);
        List<Quest> resolved = new ArrayList<>();
        for (int i = 0; i < taskNames.length; i++) {
            String taskName = taskNames[i];
            if (taskName.equals("NONE")) {
                continue;
            }
            try {
                // Parse optional stored required count
                int storedRequired = 0;
                if (taskName.contains(":")) {
                    String[] parts = taskName.split(":", 2);
                    taskName = parts[0];
                    storedRequired = Integer.parseInt(parts[1]);
                }
                QuestTaskType taskType = QuestTaskType.valueOf(taskName);
                Quest found = null;
                for (Quest q : pool) {
                    if (q.getTaskType() == taskType) {
                        found = q;
                        break;
                    }
                }
                if (found != null) {
                    // Use stored required count if available
                    if (storedRequired > 0) {
                        found = found.withRequired(storedRequired, QuestUtils.generateDisplayName(taskType, storedRequired));
                    }
                    if (rewards[i] < 0) {
                        resolved.add(found.withItemReward(QuestUtils.resolveKeyFromSentinel((int) rewards[i])));
                    } else {
                        double reward = rewards[i] > 0 ? rewards[i] : QuestUtils.generateRandomReward(rank, type);
                        resolved.add(found.withReward(reward));
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return resolved.isEmpty() ? null : resolved;
    }

    /**
     * Saves per-player quest progress (including active quest assignments) to quest_progress.txt.
     * Format: uuid|rank|d0task|d0prog|d0done|d0claimed|d1task|...|w2task|w2prog|w2done|w2claimed
     */
    public static void saveQuestProgress() {
        HashMap<UUID, List<Quest>> activeDailyMap = QuestUtils.getPlayerActiveDailyQuestsMap();
        HashMap<UUID, List<Quest>> activeWeeklyMap = QuestUtils.getPlayerActiveWeeklyQuestsMap();
        HashMap<UUID, int[]> dailyProgress = QuestUtils.getPlayerDailyProgress();
        HashMap<UUID, boolean[]> dailyCompleted = QuestUtils.getPlayerDailyCompleted();
        HashMap<UUID, boolean[]> dailyClaimed = QuestUtils.getPlayerDailyClaimed();
        HashMap<UUID, int[]> weeklyProgress = QuestUtils.getPlayerWeeklyProgress();
        HashMap<UUID, boolean[]> weeklyCompleted = QuestUtils.getPlayerWeeklyCompleted();
        HashMap<UUID, boolean[]> weeklyClaimed = QuestUtils.getPlayerWeeklyClaimed();
        HashMap<UUID, Integer> questRanks = QuestUtils.getPlayerQuestRank();

        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "quest_progress.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            // If the file isn't already there
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new quest_progress.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of quest_progress.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#uuid|rank|d0task|d0reward|d0prog|d0done|d0claimed|d1task|d1reward|d1prog|d1done|d1claimed|d2task|d2reward|d2prog|d2done|d2claimed|w0task|w0reward|w0prog|w0done|w0claimed|w1task|w1reward|w1prog|w1done|w1claimed|w2task|w2reward|w2prog|w2done|w2claimed\n");

            Set<UUID> allUuids = new HashSet<>();
            allUuids.addAll(activeDailyMap.keySet());
            allUuids.addAll(activeWeeklyMap.keySet());
            allUuids.addAll(dailyProgress.keySet());
            allUuids.addAll(weeklyProgress.keySet());

            for (UUID uuid : allUuids) {
                int rank = questRanks.getOrDefault(uuid, 0);

                List<Quest> dq = activeDailyMap.getOrDefault(uuid, new ArrayList<>());
                int[] dp = dailyProgress.getOrDefault(uuid, new int[3]);
                boolean[] dc = dailyCompleted.getOrDefault(uuid, new boolean[3]);
                boolean[] dClaim = dailyClaimed.getOrDefault(uuid, new boolean[3]);

                List<Quest> wq = activeWeeklyMap.getOrDefault(uuid, new ArrayList<>());
                int[] wp = weeklyProgress.getOrDefault(uuid, new int[3]);
                boolean[] wc = weeklyCompleted.getOrDefault(uuid, new boolean[3]);
                boolean[] wClaim = weeklyClaimed.getOrDefault(uuid, new boolean[3]);

                StringBuilder row = new StringBuilder(uuid + "|" + rank);
                for (int i = 0; i < 3; i++) {
                    String task = i < dq.size() ? (dq.get(i).getTaskType().name() + ":" + dq.get(i).getRequired()) : "NONE";
                    int reward = i < dq.size() ? (dq.get(i).hasItemReward() ? QuestUtils.getItemRewardSentinel(dq.get(i).getItemReward()) : (int) dq.get(i).getReward()) : 0;
                    int prog = i < dp.length ? dp[i] : 0;
                    int done = (i < dc.length && dc[i]) ? 1 : 0;
                    int claimed = (i < dClaim.length && dClaim[i]) ? 1 : 0;
                    row.append("|").append(task).append("|").append(reward).append("|").append(prog).append("|").append(done).append("|").append(claimed);
                }
                for (int i = 0; i < 3; i++) {
                    String task = i < wq.size() ? (wq.get(i).getTaskType().name() + ":" + wq.get(i).getRequired()) : "NONE";
                    int reward = i < wq.size() ? (wq.get(i).hasItemReward() ? QuestUtils.getItemRewardSentinel(wq.get(i).getItemReward()) : (int) wq.get(i).getReward()) : 0;
                    int prog = i < wp.length ? wp[i] : 0;
                    int done = (i < wc.length && wc[i]) ? 1 : 0;
                    int claimed = (i < wClaim.length && wClaim[i]) ? 1 : 0;
                    row.append("|").append(task).append("|").append(reward).append("|").append(prog).append("|").append(done).append("|").append(claimed);
                }
                writer.write(row + "\n");
            }

            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error in saving quest progress");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncQuestDataToDatabase);
        }
    }

    // -------------------------------------------------------------------------
    // Login Streak Persistence
    // -------------------------------------------------------------------------

    /**
     * Loads per-player login streak data from login_streaks.txt.
     * Format per line: uuid|currentDay|lastClaimEpochDay
     */
    public static void loadLoginStreaks() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "login_streaks.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the login_streaks file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }

                String[] fields = row.split("\\|");
                if (fields.length < 3) {
                    continue;
                }

                UUID uuid = UUID.fromString(fields[0]);
                int day = Integer.parseInt(fields[1]);
                long lastClaim = Long.parseLong(fields[2]);

                LoginStreakUtils.setStreakDay(uuid, day);
                LoginStreakUtils.setLastClaimEpochDay(uuid, lastClaim);
            }

            Bukkit.getLogger().info("[AC] Login streaks have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading login streaks!");
        }
    }

    /**
     * Saves per-player login streak data to login_streaks.txt.
     * Format per line: uuid|currentDay|lastClaimEpochDay
     */
    public static void saveLoginStreaks() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "login_streaks.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new login_streaks.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of login_streaks.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#uuid|currentDay|lastClaimEpochDay\n");

            HashMap<UUID, Integer> days = LoginStreakUtils.getCurrentStreakDayMap();
            HashMap<UUID, Long> claims = LoginStreakUtils.getLastClaimEpochDayMap();

            for (UUID uuid : days.keySet()) {
                int day = days.get(uuid);
                long lastClaim = claims.getOrDefault(uuid, 0L);
                writer.write(uuid + "|" + day + "|" + lastClaim + "\n");
            }

            // Also persist players who have a lastClaim but no explicit day entry
            for (UUID uuid : claims.keySet()) {
                if (!days.containsKey(uuid)) {
                    writer.write(uuid + "|1|" + claims.get(uuid) + "\n");
                }
            }

            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error in saving login streaks");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncLoginStreaksToDatabase);
        }
    }

    /**
     * Initializes gates from gates.txt.
     * Format: id|ownerUuid|type|isOpen|axis|world|x1:y1:z1,x2:y2:z2,...
     * axis is X, Z, or NONE (single-block gate with no axis determined yet).
     */
    public static void loadGates() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "gates.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        GateUtils.clearGates();

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the gates file...");

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }

                String[] fields = line.split("\\|");
                if (fields.length < 7) {
                    continue;
                }

                UUID id = UUID.fromString(fields[0]);
                UUID owner = UUID.fromString(fields[1]);
                // Backward compat: "true" → METAL, "false" → WOODEN; new saves use GateType names.
                Gate.GateType gateType = switch (fields[2]) {
                    case "true", "METAL" -> Gate.GateType.METAL;
                    case "NETHER_BRICK" -> Gate.GateType.NETHER_BRICK;
                    case "WALL" -> Gate.GateType.WALL;
                    default -> Gate.GateType.WOODEN;
                };
                boolean isOpen = Boolean.parseBoolean(fields[3]);
                String axisField = fields[4];
                String worldName = fields[5];
                String blocksField = fields[6];

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }

                Gate.Axis axis = null;
                if ("X".equals(axisField)) {
                    axis = Gate.Axis.X;
                } else if ("Z".equals(axisField)) {
                    axis = Gate.Axis.Z;
                }

                Map<Location, Material> blockMaterials = new HashMap<>();
                if (!blocksField.isEmpty()) {
                    for (String blockEntry : blocksField.split(",")) {
                        String[] parts = blockEntry.split(":");
                        if (parts.length < 3) {
                            continue;
                        }
                        int bx = Integer.parseInt(parts[0]);
                        int by = Integer.parseInt(parts[1]);
                        int bz = Integer.parseInt(parts[2]);
                        Location loc = new Location(world, bx, by, bz);
                        Material mat;
                        if (parts.length >= 4) {
                            mat = Material.matchMaterial(parts[3]);
                            if (mat == null) {
                                mat = fallbackMaterial(gateType);
                            }
                        } else {
                            // Legacy entry without material — use a sensible default.
                            mat = fallbackMaterial(gateType);
                        }
                        blockMaterials.put(loc, mat);
                    }
                }

                if (blockMaterials.isEmpty()) {
                    continue;
                }
                Location firstBlock = blockMaterials.keySet().iterator().next();
                Gate gate = new Gate(id, owner, gateType, firstBlock, blockMaterials.get(firstBlock));
                gate.setBlockMaterials(blockMaterials);
                gate.setAxis(axis);
                gate.setOpen(isOpen);
                GateUtils.addGate(gate);
            }

            Bukkit.getLogger().info("[AC] All gates have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading the gates!");
        }
    }

    /**
     * Saves all gates to gates.txt.
     */
    public static void saveGates() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "gates.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }

        if (isDirectoryCreated) {
            try {
                if (file.createNewFile()) {
                    Bukkit.getLogger().info("[AC] A new gates.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] An error occurred in the creation of gates.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#id|ownerUuid|gateType|isOpen|axis|world|x1:y1:z1:MAT1,x2:y2:z2:MAT2,...\n");

                for (Gate gate : GateUtils.getGates()) {
                    if (gate.getBlocks().isEmpty()) {
                        continue;
                    }
                    Location anyBlock = gate.getBlocks().iterator().next();
                    if (anyBlock.getWorld() == null) {
                        continue;
                    }

                    String id = gate.getId().toString();
                    String owner = gate.getOwner().toString();
                    String gateType = gate.getGateType().name();
                    String isOpen = String.valueOf(gate.isOpen());
                    String axis = gate.getAxis() == null ? "NONE" : gate.getAxis().name();
                    String world = anyBlock.getWorld().getName();

                    StringBuilder blocksSB = new StringBuilder();
                    for (Location loc : gate.getBlocks()) {
                        if (!blocksSB.isEmpty()) {
                            blocksSB.append(",");
                        }
                        Material mat = gate.getMaterialAt(loc);
                        blocksSB.append(loc.getBlockX()).append(":").append(loc.getBlockY())
                                .append(":").append(loc.getBlockZ()).append(":").append(mat.name());
                    }

                    writer.write(id + "|" + owner + "|" + gateType + "|" + isOpen + "|" + axis
                            + "|" + world + "|" + blocksSB + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("[AC] There was an error in saving the gates");
            }
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncGatesToDatabase);
        }
    }

    private static Material fallbackMaterial(Gate.GateType gateType) {
        return switch (gateType) {
            case METAL -> Material.IRON_BARS;
            case NETHER_BRICK -> Material.NETHER_BRICK_FENCE;
            case WALL -> Material.COBBLESTONE_WALL;
            default -> Material.OAK_FENCE;
        };
    }

    /**
     * Loads per-player per-element mount progress from petprogress.txt.
     * {@code playerUUID|element|healthLevel|healthXp|speedLevel|speedXp|thirdLevel|thirdXp|rechargeEndMs|currentHealth|nickname|harnessColor}
     */
    public static void loadMounts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "petprogress.txt";
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        try {
            Scanner reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the petprogress file...");
            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }
                String[] f = row.split("\\|");
                if (f.length < 10) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(f[0]);
                    String element = f[1];
                    int healthLevel = Integer.parseInt(f[2]);
                    long healthXp = Long.parseLong(f[3]);
                    int speedLevel = Integer.parseInt(f[4]);
                    long speedXp = Long.parseLong(f[5]);
                    int thirdLevel = Integer.parseInt(f[6]);
                    long thirdXp = Long.parseLong(f[7]);
                    long rechargeEnd = Long.parseLong(f[8]);
                    double curHealth = Double.parseDouble(f[9]);

                    Mount pm =
                            new Mount(
                                    healthLevel, healthXp,
                                    speedLevel, speedXp,
                                    thirdLevel, thirdXp,
                                    rechargeEnd, curHealth);
                    if (f.length >= 11 && !f[10].isEmpty()) {
                        pm.setNickname(f[10]);
                    }
                    if (f.length >= 12 && !f[11].isEmpty()) {
                        pm.setHarnessColor(f[11]);
                    }
                    MountUtils.put(uuid, element, pm);
                } catch (Exception ignored) {
                }
            }
            reader.close();
            Bukkit.getLogger().info("[AC] Mount progress has been initialised");
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading petprogress.txt!");
        }
    }

    /**
     * Saves all mount progress to petprogress.txt.
     * {@code playerUUID|element|healthLevel|healthXp|speedLevel|speedXp|thirdLevel|thirdXp|rechargeEndMs|currentHealth|nickname}
     */
    public static void saveMounts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "petprogress.txt";
        File pluginDirectory = new File(
                currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = pluginDirectory.isDirectory() || pluginDirectory.mkdir();
        if (!isDirectoryCreated) {
            return;
        }

        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new petprogress.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of petprogress.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#playerUUID|element|healthLevel|healthXp|speedLevel|speedXp|thirdLevel|thirdXp|rechargeEndMs|currentHealth|nickname|harnessColor\n");
            for (Map.Entry<UUID, Map<String, Mount>> playerEntry
                    : MountUtils.getAllMounts().entrySet()) {
                for (Map.Entry<String, Mount> elementEntry : playerEntry.getValue().entrySet()) {
                    Mount pm = elementEntry.getValue();
                    writer.write(
                            playerEntry.getKey() + "|"
                                    + elementEntry.getKey() + "|"
                                    + pm.getHealthLevel() + "|"
                                    + pm.getHealthXp() + "|"
                                    + pm.getSpeedLevel() + "|"
                                    + pm.getSpeedXp() + "|"
                                    + pm.getThirdLevel() + "|"
                                    + pm.getThirdXp() + "|"
                                    + pm.getRechargeEndMs() + "|"
                                    + pm.getCurrentHealth() + "|"
                                    + (pm.hasNickname() ? pm.getNickname() : "") + "|"
                                    + pm.getHarnessColor() + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error in saving mount progress");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncMountsToDatabase);
        }
    }

    /**
     * Loads player mail from mail.txt.
     * Format: {@code recipientUUID|senderUUID|timestamp|message}
     */
    public static void loadMail() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "mail.txt";
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        try {
            Scanner reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the mail file...");
            HashMap<UUID, List<Mail>> mailData = new HashMap<>();
            while (reader.hasNextLine()) {
                String row = reader.nextLine().trim();
                if (row.startsWith("#") || row.isEmpty()) {
                    continue;
                }
                String[] f = row.split("\\|", 4);
                if (f.length < 4) {
                    continue;
                }
                try {
                    UUID recipientUUID = UUID.fromString(f[0]);
                    UUID senderUUID = UUID.fromString(f[1]);
                    long timestamp = Long.parseLong(f[2]);
                    String message = f[3];
                    mailData.computeIfAbsent(recipientUUID, k -> new ArrayList<>())
                            .add(new Mail(senderUUID, recipientUUID, timestamp, message));
                } catch (Exception ignored) {
                }
            }
            reader.close();
            MailUtils.setAllMail(mailData);
            Bukkit.getLogger().info("[AC] Mail has been initialised");
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("[AC] Something went wrong with loading mail.txt");
        }
    }

    /**
     * Saves all player mail to mail.txt.
     * Format: {@code recipientUUID|senderUUID|timestamp|message}
     */
    public static void saveMail() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "mail.txt";
        File pluginDirectory = new File(
                currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new mail.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred in the creation of mail.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#Mail data — do not edit manually\n");
            for (Map.Entry<UUID, List<Mail>> entry : MailUtils.getAllMail().entrySet()) {
                UUID recipientUUID = entry.getKey();
                for (Mail mail : entry.getValue()) {
                    writer.write(recipientUUID + "|" + mail.getSenderUUID() + "|"
                            + mail.getTimestamp() + "|" + mail.getMessage() + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error in saving mail data");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(
                    PersistenceUtils::syncMailToDatabase);
        }
    }

    /**
     * Loads all outposts from outposts.txt and registers them with OutpostUtils.
     * Must be called after loadDominions().
     */
    public static void loadOutposts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "outposts.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        Scanner reader;
        try {
            reader = new Scanner(file);
            Bukkit.getLogger().info("[AC] Attempting to read the outposts file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }

                // #id|dominionId|name|outpostIndex|worldName|homeX|homeY|homeZ|homeYaw|homePitch|chunks|createdTimestamp
                String[] fields = row.split("\\|", -1);
                if (fields.length < 12) {
                    continue;
                }

                UUID id = UUID.fromString(fields[0]);
                UUID dominionId = UUID.fromString(fields[1]);
                String name = fields[2];
                int outpostIndex = Integer.parseInt(fields[3]);
                String worldName = fields[4];
                double homeX = Double.parseDouble(fields[5]);
                double homeY = Double.parseDouble(fields[6]);
                double homeZ = Double.parseDouble(fields[7]);
                float homeYaw = Float.parseFloat(fields[8]);
                float homePitch = Float.parseFloat(fields[9]);

                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    Bukkit.getLogger().warning("Outpost " + name + " references unknown world: " + worldName + " — skipping.");
                    continue;
                }

                List<Chunk> chunks = new ArrayList<>();
                if (!fields[10].isEmpty()) {
                    for (String chunkEntry : fields[10].split("\\*\\*\\*")) {
                        String[] parts = chunkEntry.split(",");
                        int cx = Integer.parseInt(parts[0]);
                        int cz = Integer.parseInt(parts[1]);
                        chunks.add(world.getChunkAt(cx, cz));
                    }
                }

                long createdTimestamp = Long.parseLong(fields[11]);

                Outpost outpost = new Outpost(
                        id, name, dominionId, outpostIndex,
                        worldName, homeX, homeY, homeZ, homeYaw, homePitch,
                        chunks, createdTimestamp
                );
                OutpostUtils.registerOutpost(outpost);
            }

            Bukkit.getLogger().info("[AC] All outposts have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().warning("outposts.txt not found — skipping outpost load.");
        }
    }

    /**
     * Saves all outposts to outposts.txt.
     */
    public static void saveOutposts() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "outposts.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new outposts.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred creating outposts.txt");
            return;
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#id|dominionId|name|outpostIndex|worldName|homeX|homeY|homeZ|homeYaw|homePitch|chunks|createdTimestamp\n");

            for (Dominion dominion : DominionUtils.getDominions()) {
                for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                    StringBuilder chunks = new StringBuilder();
                    for (Chunk chunk : outpost.getChunks()) {
                        if (!chunks.isEmpty()) {
                            chunks.append("***");
                        }
                        chunks.append(chunk.getX()).append(",").append(chunk.getZ());
                    }

                    Location home = outpost.getHome();
                    if (home.getWorld() == null) {
                        Bukkit.getLogger().warning("[AC] Skipping outpost save for " + outpost.getId() + ": null world (shutdown race)");
                        continue;
                    }
                    String row = outpost.getId() + "|"
                            + outpost.getDominionId() + "|"
                            + outpost.getName() + "|"
                            + outpost.getOutpostIndex() + "|"
                            + home.getWorld().getName() + "|"
                            + home.getX() + "|"
                            + home.getY() + "|"
                            + home.getZ() + "|"
                            + home.getYaw() + "|"
                            + home.getPitch() + "|"
                            + chunks + "|"
                            + outpost.getCreatedTimestamp() + "\n";
                    writer.write(row);
                }
            }
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error saving outposts!");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncOutpostsToDatabase);
        }
    }

    /**
     * Loads defenders from defenders.txt and spawns each one at its saved location.
     */
    public static void loadDefenders() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "defenders.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return;
        }

        record DefenderEntry(
                UUID dominionId, DefenderType type,
                String worldName, double x, double y, double z,
                DefenderMode mode,
                UUID followPlayerId,
                String guardWorld, double guardX, double guardY, double guardZ,
                UUID assignedOutpostId) {
        }
        List<DefenderEntry> entries = new ArrayList<>();

        Scanner reader;
        try {
            reader = new Scanner(file);
            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
                }

                String[] fields = row.split("\\|", -1);
                if (fields.length < 6) {
                    continue;
                }

                try {
                    UUID dominionId = UUID.fromString(fields[0]);
                    DefenderType type = DefenderType.valueOf(fields[1]);
                    String worldName = fields[2];
                    double x = Double.parseDouble(fields[3]);
                    double y = Double.parseDouble(fields[4]);
                    double z = Double.parseDouble(fields[5]);

                    // Optional extended fields (backward compatible)
                    DefenderMode mode = DefenderMode.PATROL;
                    UUID followPlayerId = null;
                    String guardWorld = null;
                    double guardX = 0, guardY = 0, guardZ = 0;

                    if (fields.length > 6 && !fields[6].isEmpty()) {
                        try {
                            mode = DefenderMode.valueOf(fields[6]);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (fields.length > 7 && !fields[7].isEmpty()) {
                        try {
                            followPlayerId = UUID.fromString(fields[7]);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (fields.length > 11 && !fields[8].isEmpty()) {
                        guardWorld = fields[8];
                        guardX = Double.parseDouble(fields[9]);
                        guardY = Double.parseDouble(fields[10]);
                        guardZ = Double.parseDouble(fields[11]);
                    }

                    UUID assignedOutpostId = null;
                    if (fields.length > 12 && !fields[12].isEmpty()) {
                        try {
                            assignedOutpostId = UUID.fromString(fields[12]);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    entries.add(new DefenderEntry(dominionId, type, worldName, x, y, z,
                            mode, followPlayerId, guardWorld, guardX, guardY, guardZ, assignedOutpostId));
                } catch (IllegalArgumentException ignored) {
                }
            }
            reader.close();

        } catch (FileNotFoundException e) {
            Bukkit.getLogger().warning("Something went wrong with loading defenders.txt");
            return;
        }

        // Spawn defenders at their saved locations on the next tick
        new BukkitRunnable() {
            @Override
            public void run() {
                for (DefenderEntry entry : entries) {
                    Dominion dominion = DominionUtils.getDominionById(entry.dominionId());
                    if (dominion == null) {
                        continue;
                    }

                    // Always spawn at dominion home when in follow mode
                    Location spawnLoc;
                    if (entry.mode() == DefenderMode.FOLLOW
                            && dominion.getDominionHome() != null) {
                        spawnLoc = dominion.getDominionHome();
                    } else {
                        World world = Bukkit.getWorld(entry.worldName());
                        if (world == null) {
                            continue;
                        }
                        spawnLoc = new Location(world, entry.x(), entry.y(), entry.z());
                    }

                    // Resolve guard position
                    Location guardPos = null;
                    if (entry.guardWorld() != null) {
                        World gWorld = Bukkit.getWorld(entry.guardWorld());
                        if (gWorld != null) {
                            guardPos = new Location(gWorld, entry.guardX(), entry.guardY(), entry.guardZ());
                        }
                    }

                    DefenderUtils.loadAndSpawnAt(entry.dominionId(), entry.type(), spawnLoc,
                            entry.mode(), entry.followPlayerId(), guardPos, entry.assignedOutpostId());
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
    }

    /**
     * Saves all live defender entities to defenders.txt, one line per entity with location.
     */
    public static void saveDefenders() {
        String currentPath = System.getProperty("user.dir");
        String filePath = currentPath + File.separator + "plugins" + File.separator + "AranarthCore"
                + File.separator + "defenders.txt";
        File pluginDirectory = new File(currentPath + File.separator + "plugins" + File.separator + "AranarthCore");
        File file = new File(filePath);

        boolean isDirectoryCreated = true;
        if (!pluginDirectory.isDirectory()) {
            isDirectoryCreated = pluginDirectory.mkdir();
        }
        if (!isDirectoryCreated) {
            return;
        }

        try {
            if (file.createNewFile()) {
                Bukkit.getLogger().info("[AC] A new defenders.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] An error occurred creating defenders.txt");
            return;
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#dominionId|type|world|x|y|z|mode|followPlayerId|guardWorld|guardX|guardY|guardZ|assignedOutpostId\n");

            for (Map.Entry<UUID, UUID> entry : DefenderUtils.getEntityToDominion().entrySet()) {
                UUID entityUUID = entry.getKey();
                UUID dominionId = entry.getValue();
                DefenderType type = DefenderUtils.getDefenderType(entityUUID);
                if (type == null) {
                    continue;
                }
                Entity entity = Bukkit.getEntity(entityUUID);
                if (entity instanceof LivingEntity le && le.isDead()) {
                    continue;
                }
                // Use live location if the chunk is loaded, otherwise fall back to last known cached location
                Location loc = (entity != null)
                        ? entity.getLocation()
                        : DefenderUtils.getEntityToLastLocation().get(entityUUID);
                if (loc == null || loc.getWorld() == null) {
                    continue;
                }

                DefenderMode mode = DefenderUtils.getDefenderMode(entityUUID);
                UUID followPlayerId = DefenderUtils.getFollowPlayerId(entityUUID);
                Location guardPos = DefenderUtils.getGuardPosition(entityUUID);
                UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(entityUUID);

                String followStr = followPlayerId != null ? followPlayerId.toString() : "";
                String guardWorldStr = (guardPos != null && guardPos.getWorld() != null)
                        ? guardPos.getWorld().getName() : "";
                String guardX = guardPos != null ? String.valueOf(guardPos.getX()) : "0";
                String guardY = guardPos != null ? String.valueOf(guardPos.getY()) : "0";
                String guardZ = guardPos != null ? String.valueOf(guardPos.getZ()) : "0";
                String assignedOutpostStr = assignedOutpostId != null ? assignedOutpostId.toString() : "";

                writer.write(dominionId + "|" + type.name() + "|"
                        + loc.getWorld().getName() + "|"
                        + loc.getX() + "|" + loc.getY() + "|" + loc.getZ() + "|"
                        + mode.name() + "|" + followStr + "|"
                        + guardWorldStr + "|" + guardX + "|" + guardY + "|" + guardZ + "|"
                        + assignedOutpostStr + "\n");
            }

            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("[AC] There was an error saving the defenders");
        }
        // MySQL sync
        if (DatabaseManager.isActive()) {
            runDbSync(PersistenceUtils::syncDefendersToDatabase);
        }
    }

    // =========================================================================
    // MySQL sync helpers — called from existing save/load methods when DB is active.
    // All MySQL operations run synchronously here; callers may wrap in async tasks.
    // The file-based code is always executed first; MySQL is purely supplemental.
    // =========================================================================

    /**
     * Syncs all AranarthPlayer data to MySQL. Call after the file has been saved.
     * Each player's pipe-delimited row is stored as a plain text blob in data_json.
     * This preserves the existing serialization format without a full rewrite.
     */
    public static void syncAranarthPlayersToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        HashMap<UUID, AranarthPlayer> players = AranarthUtils.getAranarthPlayers();
        for (Map.Entry<UUID, AranarthPlayer> entry : players.entrySet()) {
            UUID uuid = entry.getKey();
            AranarthPlayer ap = entry.getValue();
            // Build a minimal JSON wrapper around the player's key fields.
            // We store a JSON snapshot that is sufficient for cross-server awareness
            // (rank, nickname, balance, etc.). The full pipe-delimited row is kept in files.
            String username = ap.getUsername();
            if (username == null) {
                username = Bukkit.getOfflinePlayer(uuid).getName();
            }
            if (username == null) {
                continue; // no name resolvable, skip
            }
            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("nickname", ap.getNickname() != null ? ap.getNickname() : "");
            json.addProperty("balance", ap.getBalance());
            json.addProperty("rank", ap.getRank());
            json.addProperty("saintRank", ap.getSaintRank());
            json.addProperty("councilRank", ap.getCouncilRank());
            json.addProperty("architectRank", ap.getArchitectRank());
            json.addProperty("muteEndDate", ap.getMuteEndDate() != null ? ap.getMuteEndDate() : "");
            json.addProperty("saintExpireDate", ap.getSaintExpireDate());
            json.addProperty("firstJoinDate", ap.getFirstJoinDate() != null ? ap.getFirstJoinDate() : "");
            json.addProperty("votePointsSpent", ap.getVotePointsSpent());
            json.addProperty("isCompressingItems", ap.isCompressingItems());
            json.addProperty("isUsingSpawnBoost", ap.isUsingSpawnBoost());
            json.addProperty("pronouns", ap.getPronouns() != null ? ap.getPronouns().name() : "MALE");
            json.addProperty("conquestDisbandCooldownEnd", ap.getConquestDisbandCooldownEnd());
            try {
                db.saveAranarthPlayer(uuid, username, GSON.toJson(json));
                String rawRow = buildAranarthPlayerRow(uuid, ap);
                db.saveAranarthPlayerRaw(uuid, rawRow);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync player " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Saves a single player's AranarthPlayer data to MySQL immediately (async).
     * Called before a cross-server transfer so the destination server reads fresh data.
     */
    public static void saveAranarthPlayerImmediately(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap == null) {
            return;
        }
        String rawRow = buildAranarthPlayerRow(uuid, ap);
        DatabaseManager db = DatabaseManager.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                db.saveAranarthPlayerRaw(uuid, rawRow)
        );
    }

    /**
     * Reloads a single player's AranarthPlayer data from MySQL, replacing the in-memory entry.
     * Blocks briefly for a single-row DB read. Call on the main thread only.
     * Used when a player arrives via cross-server transfer to pick up data saved by the source server.
     */
    public static void reloadPlayerFromDatabase(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        try {
            String rawRow = DatabaseManager.getInstance().loadAranarthPlayerRaw(uuid);
            if (rawRow != null) {
                parseAndAddAranarthPlayer(rawRow);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to reload player " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Reloads a single player's quest assignments and progress from MySQL.
     * Call on the main thread when a player arrives via cross-server transfer.
     */
    public static void reloadQuestProgressForPlayer(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        try {
            String[] row = DatabaseManager.getInstance().loadQuestData(uuid);
            if (row == null || row[1] == null) {
                return;
            }
            JsonObject prog = GSON.fromJson(row[1], JsonObject.class);
            int rank = prog.has("rank") ? prog.get("rank").getAsInt() : 0;
            if (prog.has("dp")) {
                QuestUtils.getPlayerDailyProgress().put(uuid, GSON.fromJson(prog.get("dp"), int[].class));
            }
            if (prog.has("dc")) {
                QuestUtils.getPlayerDailyCompleted().put(uuid, GSON.fromJson(prog.get("dc"), boolean[].class));
            }
            if (prog.has("dClaim")) {
                QuestUtils.getPlayerDailyClaimed().put(uuid, GSON.fromJson(prog.get("dClaim"), boolean[].class));
            }
            if (prog.has("wp")) {
                QuestUtils.getPlayerWeeklyProgress().put(uuid, GSON.fromJson(prog.get("wp"), int[].class));
            }
            if (prog.has("wc")) {
                QuestUtils.getPlayerWeeklyCompleted().put(uuid, GSON.fromJson(prog.get("wc"), boolean[].class));
            }
            if (prog.has("wClaim")) {
                QuestUtils.getPlayerWeeklyClaimed().put(uuid, GSON.fromJson(prog.get("wClaim"), boolean[].class));
            }
            QuestUtils.getPlayerQuestRank().put(uuid, rank);
            if (prog.has("dTasks") && prog.has("wTasks")) {
                String[] dTasks = GSON.fromJson(prog.get("dTasks"), String[].class);
                double[] dRewards = prog.has("dRewards") ? GSON.fromJson(prog.get("dRewards"), double[].class) : new double[3];
                String[] wTasks = GSON.fromJson(prog.get("wTasks"), String[].class);
                double[] wRewards = prog.has("wRewards") ? GSON.fromJson(prog.get("wRewards"), double[].class) : new double[3];
                List<Quest> activeDailyQuests = resolveQuestsFromPool(uuid, rank, dTasks, dRewards, QuestType.DAILY);
                List<Quest> activeWeeklyQuests = resolveQuestsFromPool(uuid, rank, wTasks, wRewards, QuestType.WEEKLY);
                if (activeDailyQuests != null) {
                    QuestUtils.setPlayerActiveDailyQuests(uuid, activeDailyQuests);
                }
                if (activeWeeklyQuests != null) {
                    QuestUtils.setPlayerActiveWeeklyQuests(uuid, activeWeeklyQuests);
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to reload quest progress for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Syncs kill/death data to MySQL. Call after the file has been saved.
     */
    public static void syncKillDeathToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, List<PlayerKillDeathScore>> entry : AranarthUtils.getKillDeathScores().entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, int[]> data = new HashMap<>();
            for (PlayerKillDeathScore pkds : entry.getValue()) {
                data.put(pkds.getWorldPrefix(), new int[]{pkds.getKills(), pkds.getDeaths()});
            }
            try {
                db.saveKillDeathData(uuid, data);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync kill/death for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs vote and key data to MySQL. Call after the files have been saved.
     * Votes are stored per-player as an aggregate vote count + pending keys.
     */
    public static void syncVotesToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();

        // Aggregate vote counts per player
        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (AranarthVote vote : AranarthUtils.getVotes()) {
            voteCounts.merge(vote.getUuid(), 1, Integer::sum);
        }

        // Merge all key maps to get full set of UUIDs
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(voteCounts.keySet());
        allUuids.addAll(AranarthUtils.getPendingVoteKeys().keySet());
        allUuids.addAll(AranarthUtils.getPendingRareKeys().keySet());
        allUuids.addAll(AranarthUtils.getPendingEpicKeys().keySet());
        allUuids.addAll(AranarthUtils.getPendingGodlyKeys().keySet());

        for (UUID uuid : allUuids) {
            int vc = voteCounts.getOrDefault(uuid, 0);
            int vk = AranarthUtils.getPendingVoteKeys().getOrDefault(uuid, 0);
            int rk = AranarthUtils.getPendingRareKeys().getOrDefault(uuid, 0);
            int ek = AranarthUtils.getPendingEpicKeys().getOrDefault(uuid, 0);
            int gk = AranarthUtils.getPendingGodlyKeys().getOrDefault(uuid, 0);
            try {
                db.saveVoteData(uuid, vc, vk, rk, ek, gk);
                // Also save individual vote history
                JsonArray history = new JsonArray();
                for (AranarthVote vote : AranarthUtils.getVotes()) {
                    if (!vote.getUuid().equals(uuid)) {
                        continue;
                    }
                    JsonObject v = new JsonObject();
                    v.addProperty("keyNum", vote.getPointsRewarded());
                    v.addProperty("timestamp", vote.getTimestamp());
                    history.add(v);
                }
                db.saveVoteHistory(uuid, GSON.toJson(history));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync vote data for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs quest state + progress to MySQL. Call after the files have been saved.
     * Stores the raw pipe-delimited lines as JSON strings.
     */
    public static void syncQuestDataToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();

        // Quest state (global reset timestamps)
        String stateJson = "{\"lastDailyReset\":" + QuestUtils.getLastDailyReset()
                + ",\"lastWeeklyReset\":" + QuestUtils.getLastWeeklyReset() + "}";

        // Per-player quest progress — store as JSON mapping uuid -> progress string
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(QuestUtils.getPlayerActiveDailyQuestsMap().keySet());
        allUuids.addAll(QuestUtils.getPlayerActiveWeeklyQuestsMap().keySet());
        allUuids.addAll(QuestUtils.getPlayerDailyProgress().keySet());
        allUuids.addAll(QuestUtils.getPlayerWeeklyProgress().keySet());

        // Use a global uuid for the state row
        UUID stateUuid = new UUID(0L, 0L);
        try {
            db.saveQuestData(stateUuid, stateJson, null);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync quest state: " + e.getMessage());
        }

        for (UUID uuid : allUuids) {
            // Build a compact JSON snapshot of the player's quest progress + assignments
            JsonObject prog = new JsonObject();
            int[] dp = QuestUtils.getPlayerDailyProgress().getOrDefault(uuid, new int[3]);
            boolean[] dc = QuestUtils.getPlayerDailyCompleted().getOrDefault(uuid, new boolean[3]);
            boolean[] dClaim = QuestUtils.getPlayerDailyClaimed().getOrDefault(uuid, new boolean[3]);
            int[] wp = QuestUtils.getPlayerWeeklyProgress().getOrDefault(uuid, new int[3]);
            boolean[] wc = QuestUtils.getPlayerWeeklyCompleted().getOrDefault(uuid, new boolean[3]);
            boolean[] wClaim = QuestUtils.getPlayerWeeklyClaimed().getOrDefault(uuid, new boolean[3]);
            int rank = QuestUtils.getPlayerQuestRank().getOrDefault(uuid, 0);
            prog.addProperty("rank", rank);
            prog.add("dp", GSON.toJsonTree(dp));
            prog.add("dc", GSON.toJsonTree(dc));
            prog.add("dClaim", GSON.toJsonTree(dClaim));
            prog.add("wp", GSON.toJsonTree(wp));
            prog.add("wc", GSON.toJsonTree(wc));
            prog.add("wClaim", GSON.toJsonTree(wClaim));
            // Persist quest assignments so the other server can restore the same tasks
            List<Quest> dq = QuestUtils.getPlayerActiveDailyQuestsMap().getOrDefault(uuid, new ArrayList<>());
            List<Quest> wq = QuestUtils.getPlayerActiveWeeklyQuestsMap().getOrDefault(uuid, new ArrayList<>());
            String[] dTasks = new String[3];
            double[] dRewards = new double[3];
            String[] wTasks = new String[3];
            double[] wRewards = new double[3];
            for (int i = 0; i < 3; i++) {
                if (i < dq.size()) {
                    Quest q = dq.get(i);
                    dTasks[i] = q.getTaskType().name() + ":" + q.getRequired();
                    dRewards[i] = q.hasItemReward() ? QuestUtils.getItemRewardSentinel(q.getItemReward()) : q.getReward();
                } else {
                    dTasks[i] = "NONE";
                    dRewards[i] = 0;
                }
                if (i < wq.size()) {
                    Quest q = wq.get(i);
                    wTasks[i] = q.getTaskType().name() + ":" + q.getRequired();
                    wRewards[i] = q.hasItemReward() ? QuestUtils.getItemRewardSentinel(q.getItemReward()) : q.getReward();
                } else {
                    wTasks[i] = "NONE";
                    wRewards[i] = 0;
                }
            }
            prog.add("dTasks", GSON.toJsonTree(dTasks));
            prog.add("dRewards", GSON.toJsonTree(dRewards));
            prog.add("wTasks", GSON.toJsonTree(wTasks));
            prog.add("wRewards", GSON.toJsonTree(wRewards));
            try {
                db.saveQuestData(uuid, null, GSON.toJson(prog));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync quest progress for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs login streak data to MySQL. Call after the file has been saved.
     */
    public static void syncLoginStreaksToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        HashMap<UUID, Integer> days = LoginStreakUtils.getCurrentStreakDayMap();
        HashMap<UUID, Long> claims = LoginStreakUtils.getLastClaimEpochDayMap();
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(days.keySet());
        allUuids.addAll(claims.keySet());
        for (UUID uuid : allUuids) {
            int day = days.getOrDefault(uuid, 1);
            long lastClaim = claims.getOrDefault(uuid, 0L);
            String json = "{\"day\":" + day + ",\"lastClaim\":" + lastClaim + "}";
            try {
                db.saveLoginStreak(uuid, json);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync login streak for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs mail data to MySQL. Call after the file has been saved.
     * Each recipient's mail list is serialized as a JSON array.
     */
    public static void syncMailToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, List<Mail>> entry : MailUtils.getAllMail().entrySet()) {
            UUID recipientUuid = entry.getKey();
            JsonArray arr = new JsonArray();
            for (Mail mail : entry.getValue()) {
                JsonObject m = new JsonObject();
                m.addProperty("sender", mail.getSenderUUID().toString());
                m.addProperty("recipient", recipientUuid.toString());
                m.addProperty("timestamp", mail.getTimestamp());
                m.addProperty("message", mail.getMessage());
                arr.add(m);
            }
            try {
                db.saveAllMail(recipientUuid, GSON.toJson(arr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync mail for " + recipientUuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs mount data to MySQL. Call after the file has been saved.
     * Each player's mounts map is serialized as a JSON object.
     */
    public static void syncMountsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, Map<String, Mount>> playerEntry : MountUtils.getAllMounts().entrySet()) {
            UUID uuid = playerEntry.getKey();
            JsonObject playerMounts = new JsonObject();
            for (Map.Entry<String, Mount> elementEntry : playerEntry.getValue().entrySet()) {
                Mount pm = elementEntry.getValue();
                JsonObject m = new JsonObject();
                m.addProperty("healthLevel", pm.getHealthLevel());
                m.addProperty("healthXp", pm.getHealthXp());
                m.addProperty("speedLevel", pm.getSpeedLevel());
                m.addProperty("speedXp", pm.getSpeedXp());
                m.addProperty("thirdLevel", pm.getThirdLevel());
                m.addProperty("thirdXp", pm.getThirdXp());
                m.addProperty("rechargeEnd", pm.getRechargeEndMs());
                m.addProperty("curHealth", pm.getCurrentHealth());
                m.addProperty("nickname", pm.hasNickname() ? pm.getNickname() : "");
                m.addProperty("harnessColor", pm.getHarnessColor() != null ? pm.getHarnessColor() : "");
                playerMounts.add(elementEntry.getKey(), m);
            }
            try {
                db.saveMountData(uuid, GSON.toJson(playerMounts));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync mount data for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs punishment data to MySQL. Call after the file has been saved.
     * Each player's punishment list is stored as a JSON array.
     */
    public static void syncPunishmentsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (UUID uuid : AranarthUtils.getAllPunishments().keySet()) {
            JsonArray arr = new JsonArray();
            for (Punishment p : AranarthUtils.getPunishments(uuid)) {
                JsonObject po = new JsonObject();
                po.addProperty("date", p.getDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                po.addProperty("type", p.getType());
                po.addProperty("reason", p.getReason());
                po.addProperty("appliedBy", p.getAppliedBy() != null ? p.getAppliedBy().toString() : "CONSOLE");
                arr.add(po);
            }
            try {
                db.savePunishments(uuid, GSON.toJson(arr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync punishments for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs server boost data to MySQL. Call after the file has been saved.
     */
    public static void syncBoostsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonArray arr = new JsonArray();
        for (Map.Entry<Boost, java.time.LocalDateTime> entry : AranarthUtils.getServerBoosts().entrySet()) {
            JsonObject bo = new JsonObject();
            bo.addProperty("boost", entry.getKey().name());
            bo.addProperty("end", entry.getValue().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            arr.add(bo);
        }
        try {
            db.saveBoosts(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync boosts: " + e.getMessage());
        }
    }

    /**
     * Syncs server homepads to MySQL.
     */
    public static void syncHomepadsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonArray arr = new JsonArray();
        for (Home homepad : AranarthUtils.getHomepads()) {
            JsonObject h = new JsonObject();
            h.addProperty("name", homepad.getName());
            h.addProperty("worldName", homepad.getWorldName());
            h.addProperty("x", homepad.getLocation().getX());
            h.addProperty("y", homepad.getLocation().getY());
            h.addProperty("z", homepad.getLocation().getZ());
            h.addProperty("yaw", homepad.getLocation().getYaw());
            h.addProperty("pitch", homepad.getLocation().getPitch());
            h.addProperty("icon", homepad.getIcon().name());
            arr.add(h);
        }
        try {
            db.saveHomepads(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync homepads: " + e.getMessage());
        }
    }

    /**
     * Syncs per-player toggled features to MySQL.
     */
    public static void syncToggledFeaturesToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, AranarthPlayer> entry : AranarthUtils.getAranarthPlayers().entrySet()) {
            UUID uuid = entry.getKey();
            AranarthPlayer ap = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("chat", ap.isTogglingChat());
            obj.addProperty("messages", ap.isTogglingMessages());
            obj.addProperty("teleport", ap.isTogglingTp());
            obj.addProperty("spawnBoost", ap.isUsingSpawnBoost());
            obj.addProperty("changeClaim", ap.isTogglingChangeClaim());
            obj.addProperty("inventory", ap.isTogglingInventoryAssist());
            obj.addProperty("shulker", ap.isAddingToShulker());
            obj.addProperty("blacklistMethod", ap.getBlacklistingMethod());
            obj.addProperty("compressing", ap.isCompressingItems());
            obj.addProperty("chestLock", ap.isAutoLockingChests());
            obj.addProperty("blueFireDisabled", ap.hasBlueFireDisabled());
            obj.addProperty("gradientChatEnabled", ap.isGradientChatEnabled());
            obj.addProperty("gradientChatColors", ap.getGradientChatColors());
            obj.addProperty("dayMessageDisabled", ap.isDayMessageDisabled());
            obj.addProperty("weatherMessageDisabled", ap.isWeatherMessageDisabled());
            obj.addProperty("dominionMsgCompact", ap.isDominionMsgCompact());
            obj.addProperty("bulkSellShulker", ap.isBulkSellShulkerEnabled());
            try {
                db.savePlayerToggles(uuid, GSON.toJson(obj));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync toggles for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs per-player shop data to MySQL.
     */
    public static void syncShopsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, List<Shop>> entry : ShopUtils.getShops().entrySet()) {
            UUID uuid = entry.getKey();
            if (uuid == null) {
                continue; // server shops are handled by syncServerShopsToDatabase
            }
            JsonArray arr = new JsonArray();
            for (Shop shop : entry.getValue()) {
                JsonObject s = new JsonObject();
                s.addProperty("worldName", shop.getWorldName());
                s.addProperty("x", shop.getLocation().getBlockX());
                s.addProperty("y", shop.getLocation().getBlockY());
                s.addProperty("z", shop.getLocation().getBlockZ());
                s.addProperty("item", ItemUtils.itemStackArrayToBase64(new ItemStack[]{shop.getItem()}));
                s.addProperty("quantity", shop.getQuantity());
                s.addProperty("buyPrice", shop.getBuyPrice());
                s.addProperty("sellPrice", shop.getSellPrice());
                arr.add(s);
            }
            try {
                db.savePlayerShops(uuid, GSON.toJson(arr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync shops for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs server shop data (null-UUID shops) to MySQL as a singleton blob.
     */
    public static void syncServerShopsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        List<Shop> serverShops = ShopUtils.getShops().get(null);
        JsonArray arr = new JsonArray();
        if (serverShops != null) {
            for (Shop shop : serverShops) {
                JsonObject s = new JsonObject();
                s.addProperty("worldName", shop.getWorldName());
                s.addProperty("x", shop.getLocation().getBlockX());
                s.addProperty("y", shop.getLocation().getBlockY());
                s.addProperty("z", shop.getLocation().getBlockZ());
                s.addProperty("item", ItemUtils.itemStackArrayToBase64(new ItemStack[]{shop.getItem()}));
                s.addProperty("quantity", shop.getQuantity());
                s.addProperty("buyPrice", shop.getBuyPrice());
                s.addProperty("sellPrice", shop.getSellPrice());
                arr.add(s);
            }
        }
        try {
            db.saveServerShops(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync server shops: " + e.getMessage());
        }
    }

    /**
     * Syncs server date to MySQL.
     */
    public static void syncServerDateToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonObject obj = new JsonObject();
        obj.addProperty("day", AranarthUtils.getDay());
        obj.addProperty("weekday", AranarthUtils.getWeekday());
        obj.addProperty("month", AranarthUtils.getMonth().name());
        obj.addProperty("year", AranarthUtils.getYear());
        obj.addProperty("lastResourceWorldResetTime", AranarthUtils.getLastResourceWorldResetTime());
        try {
            db.saveServerDate(GSON.toJson(obj));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync server date: " + e.getMessage());
        }
    }

    /**
     * Syncs locked containers to MySQL.
     */
    public static void syncLockedContainersToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonArray arr = new JsonArray();
        List<LockedContainer> containers = AranarthUtils.getLockedContainers();
        if (containers != null) {
            for (LockedContainer container : containers) {
                Location[] locations = container.getLocations();
                String worldName = container.getWorldName();
                if (worldName == null) {
                    continue;
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("owner", container.getOwner().toString());
                JsonArray trusted = new JsonArray();
                for (UUID t : container.getTrusted()) {
                    trusted.add(t.toString());
                }
                obj.add("trusted", trusted);
                obj.addProperty("worldName", worldName);
                obj.addProperty("x1", locations[0].getBlockX());
                obj.addProperty("y1", locations[0].getBlockY());
                obj.addProperty("z1", locations[0].getBlockZ());
                if (locations[1] != null) {
                    obj.addProperty("x2", locations[1].getBlockX());
                    obj.addProperty("y2", locations[1].getBlockY());
                    obj.addProperty("z2", locations[1].getBlockZ());
                }
                arr.add(obj);
            }
        }
        try {
            db.saveLockedContainers(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync locked containers: " + e.getMessage());
        }
    }

    /**
     * Syncs dominion data to MySQL using the same pipe-delimited row format.
     */
    public static void syncDominionsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null) {
            return;
        }
        for (Dominion dominion : dominions) {
            try {
                String row = buildDominionRow(dominion);
                db.saveDominion(dominion.getId(), row);
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync dominion " + dominion.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs dominion permissions to MySQL.
     */
    public static void syncDominionPermissionsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null) {
            return;
        }
        for (Dominion dominion : dominions) {
            DominionPermissions perms = dominion.getDominionPermissions();
            if (perms == null) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<DominionRank, Set<DominionPermission>> entry : perms.getPermissionsMap().entrySet()) {
                if (!builder.isEmpty()) {
                    builder.append(";");
                }
                String permList = entry.getValue().stream()
                        .map(DominionPermission::name)
                        .collect(Collectors.joining(","));
                builder.append(entry.getKey().name()).append(":").append(permList);
            }
            try {
                db.saveDominionPermissions(dominion.getId(), builder.toString());
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync dominion permissions for " + dominion.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs per-player dominion permission overrides to MySQL.
     */
    public static void syncDominionPlayerPermissionsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        List<Dominion> dominions = DominionUtils.getDominions();
        if (dominions == null) {
            return;
        }
        for (Dominion dominion : dominions) {
            Map<UUID, Map<DominionPermission, Boolean>> allOverrides = dominion.getPlayerPermissionOverrides();
            if (allOverrides.isEmpty()) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<UUID, Map<DominionPermission, Boolean>> playerEntry : allOverrides.entrySet()) {
                if (playerEntry.getValue().isEmpty()) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append(";");
                }
                builder.append(playerEntry.getKey().toString()).append(":");
                String permStr = playerEntry.getValue().entrySet().stream()
                        .map(e -> e.getKey().name() + "=" + e.getValue())
                        .collect(Collectors.joining(","));
                builder.append(permStr);
            }
            if (!builder.isEmpty()) {
                try {
                    db.saveDominionPlayerPerms(dominion.getId(), builder.toString());
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync dominion player perms for " + dominion.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Syncs warps to MySQL.
     */
    public static void syncWarpsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        List<Home> warps = AranarthUtils.getWarps();
        if (warps == null) {
            return;
        }
        JsonArray arr = new JsonArray();
        for (Home warp : warps) {
            JsonObject w = new JsonObject();
            w.addProperty("name", warp.getName());
            w.addProperty("worldName", warp.getWorldName());
            w.addProperty("x", warp.getLocation().getX());
            w.addProperty("y", warp.getLocation().getY());
            w.addProperty("z", warp.getLocation().getZ());
            w.addProperty("yaw", warp.getLocation().getYaw());
            w.addProperty("pitch", warp.getLocation().getPitch());
            w.addProperty("icon", warp.getIcon().name());
            arr.add(w);
        }
        try {
            db.saveWarps(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync warps: " + e.getMessage());
        }
    }

    /**
     * Syncs avatar list and current avatar binds to MySQL.
     */
    public static void syncAvatarsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonArray avatarsArr = new JsonArray();
        for (Avatar avatar : AvatarUtils.getAvatars()) {
            if (avatar == null) {
                avatarsArr.add((com.google.gson.JsonElement) null);
            } else {
                JsonObject a = new JsonObject();
                a.addProperty("uuid", avatar.getUuid().toString());
                a.addProperty("startInGame", avatar.getStartInGame());
                a.addProperty("endInGame", avatar.getEndInGame());
                a.addProperty("startInRealLife", avatar.getStartInRealLife());
                a.addProperty("endInRealLife", avatar.getEndInRealLife());
                a.addProperty("element", String.valueOf(avatar.getElement()));
                avatarsArr.add(a);
            }
        }
        JsonArray bindsArr = new JsonArray();
        Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
        if (currentAvatar != null) {
            BendingPlayer bp = BendingPlayer.getBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()));
            if (bp != null) {
                for (Map.Entry<Integer, String> bindEntry : bp.getAbilities().entrySet()) {
                    JsonObject bind = new JsonObject();
                    bind.addProperty("slot", bindEntry.getKey());
                    bind.addProperty("ability", bindEntry.getValue());
                    bindsArr.add(bind);
                }
            }
        }
        JsonObject root = new JsonObject();
        root.add("avatars", avatarsArr);
        root.add("binds", bindsArr);
        try {
            db.saveAvatars(GSON.toJson(root));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync avatars: " + e.getMessage());
        }
    }

    /**
     * Syncs per-player compressible item types to MySQL.
     */
    public static void syncCompressibleToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, List<Material>> entry : AranarthUtils.getCompressibleTypes().entrySet()) {
            UUID uuid = entry.getKey();
            JsonArray arr = new JsonArray();
            for (Material mat : entry.getValue()) {
                arr.add(mat.name());
            }
            try {
                db.savePlayerCompressible(uuid, GSON.toJson(arr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync compressible for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs per-player shop locations to MySQL.
     */
    public static void syncShopLocationsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
        HashMap<UUID, int[]> shopIslandCenters = AranarthUtils.getShopIslandCenters();
        for (Map.Entry<UUID, Location> entry : shopLocations.entrySet()) {
            UUID uuid = entry.getKey();
            if (uuid == null) {
                continue; // server shops have no island location
            }
            Location loc = entry.getValue();
            if (loc.getWorld() == null) {
                continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("worldName", loc.getWorld().getName());
            obj.addProperty("x", loc.getX());
            obj.addProperty("y", loc.getY());
            obj.addProperty("z", loc.getZ());
            obj.addProperty("yaw", loc.getYaw());
            obj.addProperty("pitch", loc.getPitch());
            int[] center = shopIslandCenters.get(uuid);
            if (center != null) {
                obj.addProperty("centerX", center[0]);
                obj.addProperty("centerZ", center[1]);
            }
            String shopName = AranarthUtils.getShopNames().get(uuid);
            if (shopName != null) {
                obj.addProperty("name", shopName);
            }
            try {
                db.savePlayerShopLocation(uuid, GSON.toJson(obj));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync shop location for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Immediately removes the shop location row for the given UUID from MySQL.
     */
    public static void deleteShopLocationFromDatabase(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        try {
            db.deletePlayerShopLocation(uuid);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to delete shop location for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Syncs per-player shop collaborators to MySQL.
     */
    public static void syncShopCollaboratorsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, Set<UUID>> entry : AranarthUtils.getShopCollaborators().entrySet()) {
            UUID uuid = entry.getKey();
            if (entry.getValue().isEmpty()) {
                continue;
            }
            JsonArray arr = new JsonArray();
            for (UUID collab : entry.getValue()) {
                arr.add(collab.toString());
            }
            try {
                db.savePlayerShopCollaborators(uuid, GSON.toJson(arr));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync shop collaborators for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs per-player sentinel data to MySQL.
     */
    public static void syncSentinelsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Map.Entry<UUID, AranarthPlayer> entry : AranarthUtils.getAranarthPlayers().entrySet()) {
            UUID uuid = entry.getKey();
            AranarthPlayer ap = entry.getValue();
            HashMap<EntityType, List<Sentinel>> sentinels = ap.getSentinels();
            if (sentinels == null || sentinels.isEmpty()) {
                continue;
            }
            JsonObject obj = new JsonObject();
            for (Map.Entry<EntityType, List<Sentinel>> typeEntry : sentinels.entrySet()) {
                JsonArray arr = new JsonArray();
                for (Sentinel sentinel : typeEntry.getValue()) {
                    JsonObject s = new JsonObject();
                    s.addProperty("uuid", sentinel.getUuid().toString());
                    s.addProperty("worldName", sentinel.getWorldName());
                    Location loc = sentinel.getLocation();
                    s.addProperty("x", loc.getBlockX());
                    s.addProperty("y", loc.getBlockY());
                    s.addProperty("z", loc.getBlockZ());
                    arr.add(s);
                }
                obj.add(typeEntry.getKey().name(), arr);
            }
            try {
                db.savePlayerSentinels(uuid, GSON.toJson(obj));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync sentinels for " + uuid + ": " + e.getMessage());
            }
        }
    }

    /**
     * Syncs gates to MySQL.
     */
    public static void syncGatesToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        JsonArray arr = new JsonArray();
        for (Gate gate : GateUtils.getGates()) {
            if (gate.getBlocks().isEmpty()) {
                continue;
            }
            Location anyBlock = gate.getBlocks().iterator().next();
            if (anyBlock.getWorld() == null) {
                continue;
            }
            JsonObject g = new JsonObject();
            g.addProperty("id", gate.getId().toString());
            g.addProperty("owner", gate.getOwner().toString());
            g.addProperty("gateType", gate.getGateType().name());
            g.addProperty("isOpen", gate.isOpen());
            g.addProperty("axis", gate.getAxis() == null ? "NONE" : gate.getAxis().name());
            g.addProperty("world", anyBlock.getWorld().getName());
            JsonArray blocks = new JsonArray();
            for (Location loc : gate.getBlocks()) {
                JsonObject b = new JsonObject();
                b.addProperty("x", loc.getBlockX());
                b.addProperty("y", loc.getBlockY());
                b.addProperty("z", loc.getBlockZ());
                b.addProperty("mat", gate.getMaterialAt(loc).name());
                blocks.add(b);
            }
            g.add("blocks", blocks);
            arr.add(g);
        }
        try {
            db.saveGates(GSON.toJson(arr));
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync gates: " + e.getMessage());
        }
    }

    /**
     * Syncs outpost data to MySQL.
     */
    public static void syncOutpostsToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        for (Dominion dominion : DominionUtils.getDominions()) {
            for (Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                Location home = outpost.getHome();
                if (home.getWorld() == null) {
                    continue;
                }
                JsonObject obj = new JsonObject();
                obj.addProperty("id", outpost.getId().toString());
                obj.addProperty("dominionId", outpost.getDominionId().toString());
                obj.addProperty("name", outpost.getName());
                obj.addProperty("outpostIndex", outpost.getOutpostIndex());
                obj.addProperty("worldName", home.getWorld().getName());
                obj.addProperty("homeX", home.getX());
                obj.addProperty("homeY", home.getY());
                obj.addProperty("homeZ", home.getZ());
                obj.addProperty("homeYaw", home.getYaw());
                obj.addProperty("homePitch", home.getPitch());
                obj.addProperty("createdTimestamp", outpost.getCreatedTimestamp());
                JsonArray chunks = new JsonArray();
                for (Chunk chunk : outpost.getChunks()) {
                    JsonObject c = new JsonObject();
                    c.addProperty("x", chunk.getX());
                    c.addProperty("z", chunk.getZ());
                    chunks.add(c);
                }
                obj.add("chunks", chunks);
                try {
                    db.saveOutpost(outpost.getId(), GSON.toJson(obj));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync outpost " + outpost.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Syncs defender data to MySQL, grouped per-dominion.
     */
    public static void syncDefendersToDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager db = DatabaseManager.getInstance();
        // Group entries by dominion
        Map<UUID, JsonArray> byDominion = new HashMap<>();
        for (Map.Entry<UUID, UUID> entry : DefenderUtils.getEntityToDominion().entrySet()) {
            UUID entityUUID = entry.getKey();
            UUID dominionId = entry.getValue();
            DefenderType type = DefenderUtils.getDefenderType(entityUUID);
            if (type == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity instanceof LivingEntity le && le.isDead()) {
                continue;
            }
            Location loc = (entity != null)
                    ? entity.getLocation()
                    : DefenderUtils.getEntityToLastLocation().get(entityUUID);
            if (loc == null || loc.getWorld() == null) {
                continue;
            }
            DefenderMode mode = DefenderUtils.getDefenderMode(entityUUID);
            UUID followPlayerId = DefenderUtils.getFollowPlayerId(entityUUID);
            Location guardPos = DefenderUtils.getGuardPosition(entityUUID);
            UUID assignedOutpostId = DefenderUtils.getAssignedOutpostId(entityUUID);
            JsonObject d = new JsonObject();
            d.addProperty("type", type.name());
            d.addProperty("world", loc.getWorld().getName());
            d.addProperty("x", loc.getX());
            d.addProperty("y", loc.getY());
            d.addProperty("z", loc.getZ());
            d.addProperty("mode", mode != null ? mode.name() : DefenderMode.PATROL.name());
            d.addProperty("followPlayerId", followPlayerId != null ? followPlayerId.toString() : "");
            if (guardPos != null && guardPos.getWorld() != null) {
                d.addProperty("guardWorld", guardPos.getWorld().getName());
                d.addProperty("guardX", guardPos.getX());
                d.addProperty("guardY", guardPos.getY());
                d.addProperty("guardZ", guardPos.getZ());
            } else {
                d.addProperty("guardWorld", "");
            }
            d.addProperty("assignedOutpostId", assignedOutpostId != null ? assignedOutpostId.toString() : "");
            byDominion.computeIfAbsent(dominionId, k -> new JsonArray()).add(d);
        }
        for (Map.Entry<UUID, JsonArray> entry : byDominion.entrySet()) {
            try {
                db.saveDefendersForDominion(entry.getKey(), GSON.toJson(entry.getValue()));
            } catch (Exception e) {
                Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to sync defenders for dominion " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // DB-primary load methods (called from AranarthCore.initializeUtils when DB active)
    // =========================================================================

    /**
     * Shared parser — same logic as the while-loop body in loadAranarthPlayers().
     * Parses a single pipe-delimited row and registers the player via AranarthUtils.
     */
    static void parseAndAddAranarthPlayer(String row) throws Exception {
        String[] fields = row.split("\\|");
        int lastIndex = fields.length - 1;

        UUID uuid = UUID.fromString(fields[0]);
        String nickname = fields[1];
        String survivalInventory = fields[2];
        String arenaInventory = fields[3];
        String creativeInventory = fields[4];

        HashMap<ItemStack, Integer> potions = new HashMap<>();
        if (!fields[5].isEmpty()) {
            String[] potionAsArray = fields[5].split("\\*\\*\\*");
            for (String potionInArray : potionAsArray) {
                String[] parts = potionInArray.split("\\*");
                ItemStack[] potionType;
                potionType = ItemUtils.itemStackArrayFromBase64(parts[0]);
                int amount = Integer.parseInt(parts[1]);
                potions.put(potionType[0], amount);
            }
        }

        List<ItemStack> arrows = null;
        if (!fields[6].isEmpty()) {
            ItemStack[] arrowsAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[6]);
            arrows = new LinkedList<>(Arrays.asList(arrowsAsItemStackArray));
        }

        List<ItemStack> blacklist = null;
        if (!fields[7].isEmpty()) {
            ItemStack[] blacklistAsItemStackArray = ItemUtils.itemStackArrayFromBase64(fields[7]);
            blacklist = new LinkedList<>(Arrays.asList(blacklistAsItemStackArray));
        }

        int blacklistingMethod = Integer.parseInt(fields[8]);
        double balance = Double.parseDouble(fields[9]);
        int rank = Integer.parseInt(fields[10]);
        int saintRank = Integer.parseInt(fields[11]);
        int councilRank = Integer.parseInt(fields[12]);
        int architectRank = Integer.parseInt(fields[13]);

        List<Home> homes = new ArrayList<>();
        String[] homesStrings = null;
        if (!fields[14].isEmpty()) {
            homesStrings = fields[14].split("\\*\\*\\*");
        }

        if (homesStrings != null) {
            for (String home : homesStrings) {
                String[] homeParts = home.split("\\*");
                if (homeParts.length < 8) {
                    Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Skipping malformed home entry: " + home);
                    continue;
                }
                String homeName = homeParts[0];
                String fileWorldName = homeParts[1];
                double x = Double.parseDouble(homeParts[2]);
                double y = Double.parseDouble(homeParts[3]);
                double z = Double.parseDouble(homeParts[4]);
                float yaw = Float.parseFloat(homeParts[5]);
                float pitch = Float.parseFloat(homeParts[6]);
                Material icon = Material.valueOf(homeParts[7]);

                // Translate old Survival-side "smp" world names to the "smp:" prefix format.
                String savedWorldName;
                if (fileWorldName.equals("smp")) {
                    savedWorldName = "smp:world";
                } else if (fileWorldName.equals("smp_nether")) {
                    savedWorldName = "smp:world_nether";
                } else if (fileWorldName.equals("smp_the_end")) {
                    savedWorldName = "smp:world_the_end";
                } else {
                    savedWorldName = fileWorldName;
                }

                World bukttiWorld;
                if (savedWorldName.startsWith("smp:")) {
                    String smpPart = savedWorldName.substring(4);
                    String localName;
                    if (smpPart.equals("world")) {
                        localName = AranarthCore.getSmpMainWorldName();
                    } else if (smpPart.equals("world_nether")) {
                        localName = AranarthCore.getSmpNetherWorldName();
                    } else if (smpPart.equals("world_the_end")) {
                        localName = AranarthCore.getSmpEndWorldName();
                    } else {
                        localName = smpPart;
                    }
                    bukttiWorld = Bukkit.getWorld(localName);
                } else if (AranarthCore.isSmpServer()) {
                    bukttiWorld = null;
                } else {
                    bukttiWorld = Bukkit.getWorld(savedWorldName);
                }

                Location loc = new Location(bukttiWorld, x, y, z, yaw, pitch);
                homes.add(new Home(homeName, loc, icon, savedWorldName));
            }
        }

        String muteEndDate = fields[15];
        int particles = Integer.parseInt(fields[16]);

        String[] perksValues = fields[17].split("\\*");
        Perk[] perkArray = Perk.values();
        HashMap<Perk, Integer> perks = new HashMap<>();
        for (int i = 0; i < perkArray.length; i++) {
            Perk perk = perkArray[i];
            perks.put(perk, Integer.parseInt(perksValues[i]));
        }

        long saintExpireDate = Long.parseLong(fields[18]);
        boolean isCompressingItems = fields[19].equals("1");

        int votePointsSpent = Integer.parseInt(fields[20]);
        int spawnBoostValue = Integer.parseInt(fields[21]);
        boolean isUsingSpawnBoost = spawnBoostValue == 1;

        String firstJoinDate = fields[22];

        Pronouns pronouns = Pronouns.MALE;
        if (fields[lastIndex].equals("F")) {
            pronouns = Pronouns.FEMALE;
        } else if (fields[lastIndex].equals("N")) {
            pronouns = Pronouns.NEUTRAL;
        }

        AranarthUtils.addPlayer(uuid, new AranarthPlayer(Bukkit.getOfflinePlayer(uuid).getName(), nickname,
                survivalInventory, arenaInventory, creativeInventory, potions, arrows, blacklist,
                blacklistingMethod, balance, rank, saintRank, councilRank, architectRank, homes,
                muteEndDate, particles, perks, saintExpireDate, isCompressingItems, votePointsSpent, isUsingSpawnBoost,
                firstJoinDate,
                pronouns));
        long conquestDisbandCooldownEnd = fields.length > 24 ? Long.parseLong(fields[23]) : 0L;
        String survivalEnderChest = fields.length > 25 ? fields[24] : "";
        double survivalHealth = fields.length > 26 ? Double.parseDouble(fields[25]) : 20.0;
        int survivalFoodLevel = fields.length > 27 ? Integer.parseInt(fields[26]) : 20;
        float survivalSaturation = fields.length > 28 ? Float.parseFloat(fields[27]) : 5.0f;
        int survivalExpLevel = fields.length > 29 ? Integer.parseInt(fields[28]) : 0;
        float survivalExpProgress = fields.length > 30 ? Float.parseFloat(fields[29]) : 0.0f;
        AranarthUtils.getPlayer(uuid).setConquestDisbandCooldownEnd(conquestDisbandCooldownEnd);
        AranarthUtils.getPlayer(uuid).setSurvivalEnderChest(survivalEnderChest);
        AranarthUtils.getPlayer(uuid).setSurvivalHealth(survivalHealth);
        AranarthUtils.getPlayer(uuid).setSurvivalFoodLevel(survivalFoodLevel);
        AranarthUtils.getPlayer(uuid).setSurvivalSaturation(survivalSaturation);
        AranarthUtils.getPlayer(uuid).setSurvivalExpLevel(survivalExpLevel);
        AranarthUtils.getPlayer(uuid).setSurvivalExpProgress(survivalExpProgress);
    }

    /**
     * Loads aranarth players from MySQL raw_data. Falls back gracefully if empty.
     */
    public static void loadAranarthPlayersFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> rawRows = db.loadAllAranarthPlayersRaw();
        if (rawRows.isEmpty()) {
            Bukkit.getLogger().info("[AC] No raw player data in DB, will load from file");
            loadAranarthPlayers();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading " + rawRows.size() + " players from MySQL...");
        for (Map.Entry<UUID, String> entry : rawRows.entrySet()) {
            try {
                parseAndAddAranarthPlayer(entry.getValue());
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse DB player row for " + entry.getKey() + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] All aranarth players have been initialized from MySQL");
    }

    /**
     * Loads kill/death data from MySQL. Falls back to file if empty.
     */
    public static void loadKillDeathCountFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, Map<String, int[]>> all = db.loadAllKillDeathData();
        if (all.isEmpty()) {
            loadKillDeathCount();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading kill/death data from MySQL...");
        for (Map.Entry<UUID, Map<String, int[]>> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            for (Map.Entry<String, int[]> kd : entry.getValue().entrySet()) {
                AranarthUtils.addPlayerKillDeathScore(new PlayerKillDeathScore(uuid, kd.getKey(), kd.getValue()[0], kd.getValue()[1]));
            }
        }
        Bukkit.getLogger().info("[AC] Kill/death data initialized from MySQL");
    }

    /**
     * Loads vote data from MySQL. Falls back to files if empty.
     */
    public static void loadVotesFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> histories = db.loadAllVoteHistories();
        Map<UUID, int[]> counts = db.loadAllVoteCounts();
        if (counts.isEmpty()) {
            loadVotes();
            loadVoteKeys();
            loadRareKeys();
            loadEpicKeys();
            loadGodlyKeys();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading vote data from MySQL...");
        // Restore individual vote history
        for (Map.Entry<UUID, String> entry : histories.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    JsonObject v = el.getAsJsonObject();
                    int keyNum = v.get("keyNum").getAsInt();
                    long timestamp = v.get("timestamp").getAsLong();
                    AranarthUtils.addVote(new AranarthVote(uuid, keyNum, timestamp));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse vote history for " + uuid + ": " + e.getMessage());
            }
        }
        // Restore pending key counts
        for (Map.Entry<UUID, int[]> entry : counts.entrySet()) {
            UUID uuid = entry.getKey();
            int[] vals = entry.getValue();
            if (vals[1] > 0) {
                AranarthUtils.setPendingVoteKeys(uuid, vals[1]);
            }
            if (vals[2] > 0) {
                AranarthUtils.setPendingRareKeys(uuid, vals[2]);
            }
            if (vals[3] > 0) {
                AranarthUtils.setPendingEpicKeys(uuid, vals[3]);
            }
            if (vals[4] > 0) {
                AranarthUtils.setPendingGodlyKeys(uuid, vals[4]);
            }
        }
        Bukkit.getLogger().info("[AC] Vote data initialized from MySQL");
    }

    /**
     * Loads quest state (global reset timestamps) from MySQL. Falls back to file if empty.
     */
    public static void loadQuestStateFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String[]> all = db.loadAllQuestData();
        if (all.isEmpty()) {
            loadQuestState();
            return;
        }
        UUID stateKey = new UUID(0L, 0L);
        String[] stateRow = all.get(stateKey);
        if (stateRow == null || stateRow[0] == null) {
            loadQuestState();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading quest state from MySQL...");
        try {
            JsonObject state = GSON.fromJson(stateRow[0], JsonObject.class);
            if (state.has("lastDailyReset")) {
                QuestUtils.setLastDailyReset(state.get("lastDailyReset").getAsLong());
            }
            if (state.has("lastWeeklyReset")) {
                QuestUtils.setLastWeeklyReset(state.get("lastWeeklyReset").getAsLong());
            }
            Bukkit.getLogger().info("[AC] Quest state initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse quest state from DB: " + e.getMessage());
            loadQuestState();
        }
    }

    /**
     * Loads per-player quest progress from MySQL. Falls back to file if empty.
     */
    public static void loadQuestProgressFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String[]> all = db.loadAllQuestData();
        if (all.isEmpty()) {
            loadQuestProgress();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading quest progress from MySQL...");
        UUID stateKey = new UUID(0L, 0L);
        for (Map.Entry<UUID, String[]> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            if (uuid.equals(stateKey)) {
                continue;
            }
            String progressJson = entry.getValue()[1];
            if (progressJson == null) {
                continue;
            }
            try {
                JsonObject prog = GSON.fromJson(progressJson, JsonObject.class);
                int rank = prog.has("rank") ? prog.get("rank").getAsInt() : 0;
                int[] dp = GSON.fromJson(prog.get("dp"), int[].class);
                boolean[] dc = GSON.fromJson(prog.get("dc"), boolean[].class);
                boolean[] dClaim = GSON.fromJson(prog.get("dClaim"), boolean[].class);
                int[] wp = GSON.fromJson(prog.get("wp"), int[].class);
                boolean[] wc = GSON.fromJson(prog.get("wc"), boolean[].class);
                boolean[] wClaim = GSON.fromJson(prog.get("wClaim"), boolean[].class);
                QuestUtils.getPlayerDailyProgress().put(uuid, dp);
                QuestUtils.getPlayerDailyCompleted().put(uuid, dc);
                QuestUtils.getPlayerDailyClaimed().put(uuid, dClaim);
                QuestUtils.getPlayerWeeklyProgress().put(uuid, wp);
                QuestUtils.getPlayerWeeklyCompleted().put(uuid, wc);
                QuestUtils.getPlayerWeeklyClaimed().put(uuid, wClaim);
                QuestUtils.getPlayerQuestRank().put(uuid, rank);
                // Restore quest assignments if present
                if (prog.has("dTasks") && prog.has("wTasks")) {
                    String[] dTasks = GSON.fromJson(prog.get("dTasks"), String[].class);
                    double[] dRewards = prog.has("dRewards") ? GSON.fromJson(prog.get("dRewards"), double[].class) : new double[3];
                    String[] wTasks = GSON.fromJson(prog.get("wTasks"), String[].class);
                    double[] wRewards = prog.has("wRewards") ? GSON.fromJson(prog.get("wRewards"), double[].class) : new double[3];
                    List<Quest> activeDailyQuests = resolveQuestsFromPool(uuid, rank, dTasks, dRewards, QuestType.DAILY);
                    List<Quest> activeWeeklyQuests = resolveQuestsFromPool(uuid, rank, wTasks, wRewards, QuestType.WEEKLY);
                    if (activeDailyQuests != null) {
                        QuestUtils.setPlayerActiveDailyQuests(uuid, activeDailyQuests);
                    }
                    if (activeWeeklyQuests != null) {
                        QuestUtils.setPlayerActiveWeeklyQuests(uuid, activeWeeklyQuests);
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse quest progress for " + uuid + " from DB: " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Quest progress initialized from MySQL");
    }

    /**
     * Loads login streak data from MySQL. Falls back to file if empty.
     */
    public static void loadLoginStreaksFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllLoginStreaks();
        if (all.isEmpty()) {
            loadLoginStreaks();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading login streaks from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonObject j = GSON.fromJson(entry.getValue(), JsonObject.class);
                int day = j.has("day") ? j.get("day").getAsInt() : 1;
                long lastClaim = j.has("lastClaim") ? j.get("lastClaim").getAsLong() : 0L;
                LoginStreakUtils.setStreakDay(uuid, day);
                LoginStreakUtils.setLastClaimEpochDay(uuid, lastClaim);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse login streak for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Login streaks initialized from MySQL");
    }

    /**
     * Loads mail data from MySQL. Falls back to file if empty.
     */
    public static void loadMailFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllMailData();
        if (all.isEmpty()) {
            loadMail();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading mail from MySQL...");
        HashMap<UUID, List<Mail>> mailData = new HashMap<>();
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID recipientUuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                List<Mail> list = new ArrayList<>();
                for (com.google.gson.JsonElement el : arr) {
                    JsonObject m = el.getAsJsonObject();
                    UUID sender = UUID.fromString(m.get("sender").getAsString());
                    long timestamp = m.get("timestamp").getAsLong();
                    String message = m.get("message").getAsString();
                    list.add(new Mail(sender, recipientUuid, timestamp, message));
                }
                mailData.put(recipientUuid, list);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse mail for " + recipientUuid + ": " + e.getMessage());
            }
        }
        MailUtils.setAllMail(mailData);
        Bukkit.getLogger().info("[AC] Mail initialized from MySQL");
    }

    /**
     * Loads mount data from MySQL. Falls back to file if empty.
     */
    public static void loadMountsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllMountsData();
        if (all.isEmpty()) {
            loadMounts();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading mounts from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonObject playerMounts = GSON.fromJson(entry.getValue(), JsonObject.class);
                for (Map.Entry<String, com.google.gson.JsonElement> me : playerMounts.entrySet()) {
                    String element = me.getKey();
                    JsonObject m = me.getValue().getAsJsonObject();
                    Mount pm = new Mount(
                            m.get("healthLevel").getAsInt(), m.get("healthXp").getAsLong(),
                            m.get("speedLevel").getAsInt(), m.get("speedXp").getAsLong(),
                            m.get("thirdLevel").getAsInt(), m.get("thirdXp").getAsLong(),
                            m.get("rechargeEnd").getAsLong(), m.get("curHealth").getAsDouble());
                    String nick = m.has("nickname") ? m.get("nickname").getAsString() : "";
                    if (!nick.isEmpty()) {
                        pm.setNickname(nick);
                    }
                    String harness = m.has("harnessColor") ? m.get("harnessColor").getAsString() : "";
                    if (!harness.isEmpty()) {
                        pm.setHarnessColor(harness);
                    }
                    MountUtils.put(uuid, element, pm);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse mounts for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Mounts initialized from MySQL");
    }

    /**
     * Loads punishment data from MySQL. Falls back to file if empty.
     */
    public static void loadPunishmentsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPunishmentsData();
        if (all.isEmpty()) {
            loadPunishments();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading punishments from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    JsonObject po = el.getAsJsonObject();
                    java.time.LocalDateTime date = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(po.get("date").getAsLong()),
                            java.time.ZoneId.systemDefault());
                    String type = po.get("type").getAsString();
                    String reason = po.get("reason").getAsString();
                    UUID appliedBy = po.get("appliedBy").getAsString().equals("CONSOLE") ? null
                            : UUID.fromString(po.get("appliedBy").getAsString());
                    AranarthUtils.addPunishment(uuid, new Punishment(uuid, date, type, reason, appliedBy), true);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse punishments for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Punishments initialized from MySQL");
    }

    /**
     * Loads server boosts from MySQL. Falls back to file if empty.
     */
    public static void loadBoostsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadBoosts();
        if (json == null || json.isEmpty()) {
            loadBoosts();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading boosts from MySQL...");
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (com.google.gson.JsonElement el : arr) {
                JsonObject bo = el.getAsJsonObject();
                Boost boost = Boost.valueOf(bo.get("boost").getAsString());
                java.time.LocalDateTime end = java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(bo.get("end").getAsLong()),
                        java.time.ZoneId.systemDefault());
                AranarthUtils.addServerBoost(boost, end, null, false);
            }
            Bukkit.getLogger().info("[AC] Boosts initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse boosts from DB: " + e.getMessage());
            loadBoosts();
        }
    }

    /**
     * Loads homepads from MySQL. Falls back to file if empty.
     */
    public static void loadHomepadsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadHomepads();
        if (json == null || json.isEmpty()) {
            loadHomepads();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading homepads from MySQL...");
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (com.google.gson.JsonElement el : arr) {
                JsonObject h = el.getAsJsonObject();
                String homeName = h.get("name").getAsString();
                String worldName = h.get("worldName").getAsString();
                double x = h.get("x").getAsDouble();
                double y = h.get("y").getAsDouble();
                double z = h.get("z").getAsDouble();
                float yaw = h.get("yaw").getAsFloat();
                float pitch = h.get("pitch").getAsFloat();
                Material icon = Material.valueOf(h.get("icon").getAsString());
                Location location = new Location(Bukkit.getWorld(toBukkitWorldName(worldName)), x, y, z, yaw, pitch);
                AranarthUtils.addNewHomepad(location);
                if (homeName != null && !homeName.equals("NEW")) {
                    AranarthUtils.updateHomepad(homeName, location, icon);
                }
            }
            Bukkit.getLogger().info("[AC] Homepads initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse homepads from DB: " + e.getMessage());
            loadHomepads();
        }
    }

    /**
     * Loads toggled features from MySQL. Must be called AFTER loadAranarthPlayersFromDatabase().
     * Falls back to file if empty.
     */
    public static void loadToggledFeaturesFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerToggles();
        if (all.isEmpty()) {
            loadToggledFeatures();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading toggled features from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
            if (ap == null) {
                continue;
            }
            try {
                JsonObject obj = GSON.fromJson(entry.getValue(), JsonObject.class);
                if (obj.has("chat")) {
                    ap.setTogglingChat(obj.get("chat").getAsBoolean());
                }
                if (obj.has("messages")) {
                    ap.setTogglingMessages(obj.get("messages").getAsBoolean());
                }
                if (obj.has("teleport")) {
                    ap.setTogglingTp(obj.get("teleport").getAsBoolean());
                }
                if (obj.has("spawnBoost")) {
                    ap.setUsingSpawnBoost(obj.get("spawnBoost").getAsBoolean());
                }
                if (obj.has("changeClaim")) {
                    ap.setTogglingChangeClaim(obj.get("changeClaim").getAsBoolean());
                }
                if (obj.has("inventory")) {
                    ap.setTogglingInventoryAssist(obj.get("inventory").getAsBoolean());
                }
                if (obj.has("shulker")) {
                    ap.setAddingToShulker(obj.get("shulker").getAsBoolean());
                }
                if (obj.has("blacklistMethod")) {
                    ap.setBlacklistingMethod(obj.get("blacklistMethod").getAsInt());
                }
                if (obj.has("compressing")) {
                    ap.setCompressingItems(obj.get("compressing").getAsBoolean());
                }
                if (obj.has("chestLock")) {
                    ap.setAutoLockingChests(obj.get("chestLock").getAsBoolean());
                }
                if (obj.has("blueFireDisabled")) {
                    ap.setBlueFireDisabled(obj.get("blueFireDisabled").getAsBoolean());
                }
                if (obj.has("gradientChatEnabled")) {
                    ap.setGradientChatEnabled(obj.get("gradientChatEnabled").getAsBoolean());
                }
                if (obj.has("gradientChatColors")) {
                    ap.setGradientChatColors(obj.get("gradientChatColors").getAsString());
                }
                if (obj.has("dayMessageDisabled")) {
                    ap.setDayMessageDisabled(obj.get("dayMessageDisabled").getAsBoolean());
                }
                if (obj.has("weatherMessageDisabled")) {
                    ap.setWeatherMessageDisabled(obj.get("weatherMessageDisabled").getAsBoolean());
                }
                if (obj.has("dominionMsgCompact")) {
                    ap.setDominionMsgCompact(obj.get("dominionMsgCompact").getAsBoolean());
                }
                if (obj.has("bulkSellShulker")) {
                    ap.setBulkSellShulkerEnabled(obj.get("bulkSellShulker").getAsBoolean());
                }
                AranarthUtils.setPlayer(uuid, ap);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse toggles for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Toggled features initialized from MySQL");
    }

    /**
     * Builds the toggle JSON string for the given player from their current in-memory state.
     * Safe to call on the main thread; the returned string can be written to MySQL on an async thread.
     */
    public static String buildPlayerToggleJson(UUID uuid) {
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap == null) {
            return null;
        }
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("chat", ap.isTogglingChat());
        obj.addProperty("messages", ap.isTogglingMessages());
        obj.addProperty("teleport", ap.isTogglingTp());
        obj.addProperty("spawnBoost", ap.isUsingSpawnBoost());
        obj.addProperty("changeClaim", ap.isTogglingChangeClaim());
        obj.addProperty("inventory", ap.isTogglingInventoryAssist());
        obj.addProperty("shulker", ap.isAddingToShulker());
        obj.addProperty("blacklistMethod", ap.getBlacklistingMethod());
        obj.addProperty("compressing", ap.isCompressingItems());
        obj.addProperty("chestLock", ap.isAutoLockingChests());
        obj.addProperty("blueFireDisabled", ap.hasBlueFireDisabled());
        obj.addProperty("gradientChatEnabled", ap.isGradientChatEnabled());
        obj.addProperty("gradientChatColors", ap.getGradientChatColors());
        obj.addProperty("dayMessageDisabled", ap.isDayMessageDisabled());
        obj.addProperty("weatherMessageDisabled", ap.isWeatherMessageDisabled());
        obj.addProperty("dominionMsgCompact", ap.isDominionMsgCompact());
        obj.addProperty("bulkSellShulker", ap.isBulkSellShulkerEnabled());
        return GSON.toJson(obj);
    }

    /**
     * Loads a single player's toggle settings from MySQL and applies them to their in-memory AranarthPlayer.
     * Must be called on the main thread (brief synchronous DB read).
     */
    public static void loadPlayerTogglesFromDatabase(UUID uuid) {
        if (!DatabaseManager.isActive()) {
            return;
        }
        AranarthPlayer ap = AranarthUtils.getPlayer(uuid);
        if (ap == null) {
            return;
        }
        try {
            String json = DatabaseManager.getInstance().loadPlayerToggles(uuid);
            if (json == null || json.isEmpty()) {
                return;
            }
            com.google.gson.JsonObject obj = GSON.fromJson(json, com.google.gson.JsonObject.class);
            if (obj.has("chat")) {
                ap.setTogglingChat(obj.get("chat").getAsBoolean());
            }
            if (obj.has("messages")) {
                ap.setTogglingMessages(obj.get("messages").getAsBoolean());
            }
            if (obj.has("teleport")) {
                ap.setTogglingTp(obj.get("teleport").getAsBoolean());
            }
            if (obj.has("spawnBoost")) {
                ap.setUsingSpawnBoost(obj.get("spawnBoost").getAsBoolean());
            }
            if (obj.has("changeClaim")) {
                ap.setTogglingChangeClaim(obj.get("changeClaim").getAsBoolean());
            }
            if (obj.has("inventory")) {
                ap.setTogglingInventoryAssist(obj.get("inventory").getAsBoolean());
            }
            if (obj.has("shulker")) {
                ap.setAddingToShulker(obj.get("shulker").getAsBoolean());
            }
            if (obj.has("blacklistMethod")) {
                ap.setBlacklistingMethod(obj.get("blacklistMethod").getAsInt());
            }
            if (obj.has("compressing")) {
                ap.setCompressingItems(obj.get("compressing").getAsBoolean());
            }
            if (obj.has("chestLock")) {
                ap.setAutoLockingChests(obj.get("chestLock").getAsBoolean());
            }
            if (obj.has("blueFireDisabled")) {
                ap.setBlueFireDisabled(obj.get("blueFireDisabled").getAsBoolean());
            }
            if (obj.has("gradientChatEnabled")) {
                ap.setGradientChatEnabled(obj.get("gradientChatEnabled").getAsBoolean());
            }
            if (obj.has("gradientChatColors")) {
                ap.setGradientChatColors(obj.get("gradientChatColors").getAsString());
            }
            if (obj.has("dayMessageDisabled")) {
                ap.setDayMessageDisabled(obj.get("dayMessageDisabled").getAsBoolean());
            }
            if (obj.has("weatherMessageDisabled")) {
                ap.setWeatherMessageDisabled(obj.get("weatherMessageDisabled").getAsBoolean());
            }
            if (obj.has("dominionMsgCompact")) {
                ap.setDominionMsgCompact(obj.get("dominionMsgCompact").getAsBoolean());
            }
            if (obj.has("bulkSellShulker")) {
                ap.setBulkSellShulkerEnabled(obj.get("bulkSellShulker").getAsBoolean());
            }
            AranarthUtils.setPlayer(uuid, ap);
        } catch (Exception e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "[DB] Failed to load toggles for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Loads shop data from MySQL. Falls back to file if empty.
     */
    public static void loadShopsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerShops();
        if (all.isEmpty()) {
            loadShops();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading shops from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    JsonObject s = el.getAsJsonObject();
                    String worldName = s.get("worldName").getAsString();
                    int x = s.get("x").getAsInt();
                    int y = s.get("y").getAsInt();
                    int z = s.get("z").getAsInt();
                    ItemStack item;
                    try {
                        item = ItemUtils.itemStackArrayFromBase64(s.get("item").getAsString())[0];
                    } catch (Exception ex) {
                        item = new ItemStack(Material.AIR, 1);
                    }
                    int quantity = s.get("quantity").getAsInt();
                    double buyPrice = s.get("buyPrice").getAsDouble();
                    double sellPrice = s.get("sellPrice").getAsDouble();
                    Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                    Shop shop = new Shop(uuid, location, item, quantity, buyPrice, sellPrice);
                    shop.setWorldName(worldName);
                    ShopUtils.addShop(uuid, shop);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse shops for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Shops initialized from MySQL");
    }

    /**
     * Loads server shop data (null-UUID shops) from MySQL.
     */
    public static void loadServerShopsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadServerShops();
        if (json == null || json.isEmpty()) {
            return;
        }
        Bukkit.getLogger().info("[AC] Loading server shops from MySQL...");
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (com.google.gson.JsonElement el : arr) {
                JsonObject s = el.getAsJsonObject();
                String worldName = s.get("worldName").getAsString();
                int x = s.get("x").getAsInt();
                int y = s.get("y").getAsInt();
                int z = s.get("z").getAsInt();
                ItemStack item;
                try {
                    item = ItemUtils.itemStackArrayFromBase64(s.get("item").getAsString())[0];
                } catch (Exception ex) {
                    item = new ItemStack(Material.AIR, 1);
                }
                int quantity = s.get("quantity").getAsInt();
                double buyPrice = s.get("buyPrice").getAsDouble();
                double sellPrice = s.get("sellPrice").getAsDouble();
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                Shop shop = new Shop(null, location, item, quantity, buyPrice, sellPrice);
                shop.setWorldName(worldName);
                ShopUtils.addShop(null, shop);
            }
            Bukkit.getLogger().info("[AC] Server shops initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse server shops from DB: " + e.getMessage());
        }
    }

    /**
     * Loads server date from MySQL. Falls back to file if empty.
     */
    public static void loadServerDateFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadServerDate();
        if (json == null || json.isEmpty()) {
            loadServerDate();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading server date from MySQL...");
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.has("day")) {
                AranarthUtils.setDay(obj.get("day").getAsInt());
            }
            if (obj.has("weekday")) {
                AranarthUtils.setWeekday(obj.get("weekday").getAsInt());
            }
            if (obj.has("month")) {
                AranarthUtils.setMonth(Month.valueOf(obj.get("month").getAsString()));
            }
            if (obj.has("year")) {
                AranarthUtils.setYear(obj.get("year").getAsInt());
            }
            if (obj.has("lastResourceWorldResetTime")) {
                AranarthUtils.setLastResourceWorldResetTime(obj.get("lastResourceWorldResetTime").getAsLong());
            }
            Bukkit.getLogger().info("[AC] Server date initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse server date from DB: " + e.getMessage());
            loadServerDate();
        }
    }

    /**
     * Loads locked containers from MySQL. Falls back to file if empty.
     */
    public static void loadLockedContainersFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadLockedContainers();
        if (json == null || json.isEmpty()) {
            loadLockedContainers();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading locked containers from MySQL...");
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (com.google.gson.JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                UUID owner = UUID.fromString(obj.get("owner").getAsString());
                List<UUID> trusted = new ArrayList<>();
                for (com.google.gson.JsonElement t : obj.get("trusted").getAsJsonArray()) {
                    trusted.add(UUID.fromString(t.getAsString()));
                }
                String worldName = obj.get("worldName").getAsString();
                int x1 = obj.get("x1").getAsInt();
                int y1 = obj.get("y1").getAsInt();
                int z1 = obj.get("z1").getAsInt();
                Location loc1 = new Location(Bukkit.getWorld(worldName), x1, y1, z1);
                Location loc2 = null;
                if (obj.has("x2")) {
                    loc2 = new Location(Bukkit.getWorld(worldName), obj.get("x2").getAsInt(), obj.get("y2").getAsInt(), obj.get("z2").getAsInt());
                }
                LockedContainer lc = new LockedContainer(owner, trusted, new Location[]{loc1, loc2});
                lc.setWorldName(worldName);
                AranarthUtils.addLockedContainer(lc);
            }
            Bukkit.getLogger().info("[AC] Locked containers initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse locked containers from DB: " + e.getMessage());
            loadLockedContainers();
        }
    }

    /**
     * Parses a single pipe-delimited dominion row and registers it via DominionUtils.
     * Mirrors the while-loop body of loadDominions().
     */
    private static void parseAndAddDominionRow(String row) throws Exception {
        String[] fields = row.split("\\|");

        UUID id = null;
        if (!fields[0].isEmpty()) {
            id = UUID.fromString(fields[0]);
        }

        String name = fields[1];
        UUID leader = UUID.fromString(fields[2]);
        List<UUID> members = new ArrayList<>();
        String[] memberParts = fields[3].split("\\*\\*\\*");
        for (String member : memberParts) {
            members.add(UUID.fromString(member));
        }

        List<UUID> allies = new ArrayList<>();
        String[] alliesParts = fields[4].split("\\*\\*\\*");
        if (!alliesParts[0].isEmpty()) {
            for (String ally : alliesParts) allies.add(UUID.fromString(ally));
        }

        List<UUID> truced = new ArrayList<>();
        String[] trucedParts = fields[5].split("\\*\\*\\*");
        if (!trucedParts[0].isEmpty()) {
            for (String truce : trucedParts) truced.add(UUID.fromString(truce));
        }

        List<UUID> enemies = new ArrayList<>();
        String[] enemyParts = fields[6].split("\\*\\*\\*");
        if (!enemyParts[0].isEmpty()) {
            for (String enemy : enemyParts) enemies.add(UUID.fromString(enemy));
        }

        String worldName = fields[7];
        World world = Bukkit.getWorld(worldName);

        List<Chunk> chunks = new ArrayList<>();
        String[] claimedChunks = fields[8].split("\\*\\*\\*");
        for (String chunk : claimedChunks) {
            String[] coordinates = chunk.split(",");
            int cx = Integer.parseInt(coordinates[0]);
            int cz = Integer.parseInt(coordinates[1]);
            if (world != null) {
                chunks.add(world.getChunkAt(cx, cz));
            }
        }

        double x = Double.parseDouble(fields[9]);
        double y = Double.parseDouble(fields[10]);
        double z = Double.parseDouble(fields[11]);
        float yaw = Float.parseFloat(fields[12]);
        float pitch = Float.parseFloat(fields[13]);
        ItemStack[] food = new ItemStack[54];
        if (!fields[14].isEmpty()) {
            food = ItemUtils.itemStackArrayFromBase64(fields[14]);
        }
        int claimableResources = Integer.parseInt(fields[15]);
        List<UUID> conquered = new ArrayList<>();
        String[] conqueredUuids = fields[16].split("_");
        for (String cuuid : conqueredUuids) {
            if (!cuuid.isEmpty()) {
                conquered.add(UUID.fromString(cuuid));
            }
        }

        double balance = Double.parseDouble(fields[17]);

        Map<UUID, DominionRank> memberRanks = new HashMap<>();
        if (fields.length > 18 && !fields[18].isEmpty()) {
            for (String entry : fields[18].split("\\*\\*\\*")) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    memberRanks.put(UUID.fromString(parts[0]), DominionRank.valueOf(parts[1]));
                }
            }
        }
        for (UUID memberUuid : members) {
            if (!memberRanks.containsKey(memberUuid)) {
                memberRanks.put(memberUuid, memberUuid.equals(leader) ? DominionRank.LEADER : DominionRank.CITIZEN);
            }
        }

        boolean memberPvpEnabled = fields.length > 19 && fields[19].equals("1");
        boolean mobSpawningEnabled = fields.length > 20 && fields[20].equals("1");
        long conqueredRequestTimestamp = fields.length > 21 ? Long.parseLong(fields[21]) : 0L;
        long lastConquerAttemptTimestamp = fields.length > 22 ? Long.parseLong(fields[22]) : 0L;
        long rebelRequestTimestamp = fields.length > 23 ? Long.parseLong(fields[23]) : 0L;
        long conqueredRequestDefenderLastSeen = 0L;
        if (fields.length > 24) {
            String f24 = fields[24];
            if (!f24.equals("0") && !f24.equals("1")) {
                conqueredRequestDefenderLastSeen = Long.parseLong(f24);
            }
        }
        long rebelRequestConquerorLastSeen = fields.length > 25 ? Long.parseLong(fields[25]) : 0L;
        long lastRebelAttemptTimestamp = fields.length > 26 ? Long.parseLong(fields[26]) : 0L;
        long conqueredTimestamp = fields.length > 27 ? Long.parseLong(fields[27]) : 0L;
        int boughtChunks = fields.length > 28 ? Integer.parseInt(fields[28]) : 0;
        int dominionLevel = fields.length > 29 ? Integer.parseInt(fields[29]) : 1;
        int cachedFarmlandCount = fields.length > 30 ? Integer.parseInt(fields[30]) : 0;
        int cachedLivestockCount = fields.length > 31 ? Integer.parseInt(fields[31]) : 0;
        long foundedTimestamp = fields.length > 32 ? Long.parseLong(fields[32]) : 0L;
        if (foundedTimestamp > 1_000_000_000L) {
            foundedTimestamp = 0L;
        }
        long levelDropTimestamp = fields.length > 33 ? Long.parseLong(fields[33]) : 0L;
        int boughtOutpostChunks = fields.length > 34 ? Integer.parseInt(fields[34]) : 0;
        String cachedLivestockByWorldString = fields.length > 35 ? fields[35] : "";
        boolean bendingEnabled = fields.length <= 36 || !fields[36].equals("0");

        Dominion dominion = new Dominion(id, name, leader, members, memberRanks, allies, truced, enemies, worldName, chunks,
                x, y, z, yaw, pitch, food, claimableResources, conquered, null, balance);
        dominion.setMemberPvpEnabled(memberPvpEnabled);
        dominion.setMobSpawningEnabled(mobSpawningEnabled);
        dominion.setConqueredRequestTimestamp(conqueredRequestTimestamp);
        dominion.setLastConquerAttemptTimestamp(lastConquerAttemptTimestamp);
        dominion.setRebelRequestTimestamp(rebelRequestTimestamp);
        dominion.setConqueredRequestDefenderLastSeen(conqueredRequestDefenderLastSeen);
        dominion.setRebelRequestConquerorLastSeen(rebelRequestConquerorLastSeen);
        dominion.setLastRebelAttemptTimestamp(lastRebelAttemptTimestamp);
        dominion.setConqueredTimestamp(conqueredTimestamp);
        dominion.setBoughtChunks(boughtChunks);
        dominion.setDominionLevel(dominionLevel);
        dominion.setCachedFarmlandCount(cachedFarmlandCount);
        dominion.setCachedLivestockCount(cachedLivestockCount);
        dominion.setFoundedTimestamp(foundedTimestamp);
        dominion.setLevelDropTimestamp(levelDropTimestamp);
        dominion.setBoughtOutpostChunks(boughtOutpostChunks);
        dominion.setBendingEnabled(bendingEnabled);
        if (!cachedLivestockByWorldString.isEmpty()) {
            for (String kv : cachedLivestockByWorldString.split(";")) {
                String[] kvParts = kv.split("=", 2);
                if (kvParts.length == 2) {
                    try {
                        dominion.getCachedLivestockByWorld().put(kvParts[0], Integer.parseInt(kvParts[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        DominionUtils.resizeFoodArray(dominion);
        DominionUtils.createDominion(dominion);
    }

    /**
     * Loads dominions from MySQL. Falls back to file if empty.
     */
    public static void loadDominionsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllDominions();
        if (all.isEmpty()) {
            loadDominions();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading " + all.size() + " dominions from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            try {
                parseAndAddDominionRow(entry.getValue());
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse DB dominion row for " + entry.getKey() + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] All dominions initialized from MySQL");
    }

    /**
     * Loads dominion permissions from MySQL. Must be called after loadDominionsFromDatabase().
     * Falls back to file if empty.
     */
    public static void loadDominionPermissionsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllDominionPermissions();
        if (all.isEmpty()) {
            loadDominionPermissions();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading dominion permissions from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID dominionId = entry.getKey();
            Dominion dominion = DominionUtils.getDominionById(dominionId);
            if (dominion == null) {
                continue;
            }
            try {
                Map<DominionRank, Set<DominionPermission>> allPerms = new EnumMap<>(DominionRank.class);
                if (!entry.getValue().isEmpty()) {
                    for (String rankEntry : entry.getValue().split(";")) {
                        String[] parts = rankEntry.split(":", 2);
                        if (parts.length == 2) {
                            try {
                                DominionRank rank = DominionRank.valueOf(parts[0]);
                                Set<DominionPermission> perms = new HashSet<>();
                                if (!parts[1].isEmpty()) {
                                    for (String permName : parts[1].split(",")) {
                                        try {
                                            perms.add(DominionPermission.valueOf(permName));
                                        } catch (IllegalArgumentException ignored) {
                                        }
                                    }
                                }
                                allPerms.put(rank, perms);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
                allPerms.put(DominionRank.LEADER, new HashSet<>(Arrays.asList(DominionPermission.values())));
                dominion.setDominionPermissions(new DominionPermissions(allPerms));
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse dominion permissions for " + dominionId + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Dominion permissions initialized from MySQL");
    }

    /**
     * Loads dominion player permission overrides from MySQL. Falls back to file if empty.
     */
    public static void loadDominionPlayerPermissionsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllDominionPlayerPerms();
        if (all.isEmpty()) {
            loadDominionPlayerPermissions();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading dominion player permissions from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID dominionId = entry.getKey();
            Dominion dominion = DominionUtils.getDominionById(dominionId);
            if (dominion == null) {
                continue;
            }
            try {
                Map<UUID, Map<DominionPermission, Boolean>> allOverrides = new HashMap<>();
                if (!entry.getValue().isEmpty()) {
                    for (String playerEntry : entry.getValue().split(";")) {
                        String[] parts = playerEntry.split(":", 2);
                        if (parts.length != 2) {
                            continue;
                        }
                        UUID playerUuid;
                        try {
                            playerUuid = UUID.fromString(parts[0]);
                        } catch (IllegalArgumentException ignored) {
                            continue;
                        }
                        Map<DominionPermission, Boolean> overrides = new HashMap<>();
                        if (!parts[1].isEmpty()) {
                            for (String permEntry : parts[1].split(",")) {
                                String[] kv = permEntry.split("=", 2);
                                if (kv.length != 2) {
                                    continue;
                                }
                                try {
                                    overrides.put(DominionPermission.valueOf(kv[0]), Boolean.parseBoolean(kv[1]));
                                } catch (IllegalArgumentException ignored) {
                                }
                            }
                        }
                        if (!overrides.isEmpty()) {
                            allOverrides.put(playerUuid, overrides);
                        }
                    }
                }
                dominion.setPlayerPermissionOverrides(allOverrides);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse dominion player perms for " + dominionId + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Dominion player permissions initialized from MySQL");
    }

    /**
     * Loads warps from MySQL. Falls back to file if empty.
     */
    public static void loadWarpsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadWarps();
        if (json == null || json.isEmpty()) {
            loadWarps();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading warps from MySQL...");
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            List<Home> warps = new ArrayList<>();
            for (com.google.gson.JsonElement el : arr) {
                JsonObject w = el.getAsJsonObject();
                String warpName = w.get("name").getAsString();
                String worldName = w.get("worldName").getAsString();
                double x = w.get("x").getAsDouble();
                double y = w.get("y").getAsDouble();
                double z = w.get("z").getAsDouble();
                float yaw = w.get("yaw").getAsFloat();
                float pitch = w.get("pitch").getAsFloat();
                Material icon = Material.valueOf(w.get("icon").getAsString());
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                warps.add(new Home(warpName, location, icon, worldName));
            }
            AranarthUtils.setWarps(warps);
            Bukkit.getLogger().info("[AC] Warps initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse warps from DB: " + e.getMessage());
            loadWarps();
        }
    }

    /**
     * Loads avatars and binds from MySQL. Falls back to both file methods if empty.
     */
    public static void loadAvatarsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadAvatars();
        if (json == null || json.isEmpty()) {
            loadAvatars();
            loadAvatarBinds();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading avatars from MySQL...");
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray avatarsArr = root.getAsJsonArray("avatars");
            for (com.google.gson.JsonElement el : avatarsArr) {
                if (el.isJsonNull()) {
                    AvatarUtils.addAvatar(null);
                } else {
                    JsonObject a = el.getAsJsonObject();
                    UUID uuid = UUID.fromString(a.get("uuid").getAsString());
                    String startInGame = a.get("startInGame").getAsString();
                    String endInGame = a.get("endInGame").getAsString();
                    String startInRealLife = a.get("startInRealLife").getAsString();
                    String endInRealLife = a.get("endInRealLife").getAsString();
                    char element = a.get("element").getAsString().charAt(0);
                    AvatarUtils.addAvatar(new Avatar(uuid, startInGame, endInGame, startInRealLife, endInRealLife, element));
                }
            }
            // Restore binds
            Avatar currentAvatar = AvatarUtils.getCurrentAvatar();
            if (currentAvatar != null && root.has("binds")) {
                JsonArray bindsArr = root.getAsJsonArray("binds");
                OfflineBendingPlayer bendingPlayer =
                        BendingPlayer.getOfflineBendingPlayer(Bukkit.getOfflinePlayer(currentAvatar.getUuid()).getName());
                if (bendingPlayer != null) {
                    for (com.google.gson.JsonElement bindEl : bindsArr) {
                        JsonObject bind = bindEl.getAsJsonObject();
                        int slot = bind.get("slot").getAsInt();
                        String ability = bind.get("ability").getAsString();
                        bendingPlayer.bindAbility(ability, slot);
                    }
                }
            }
            Bukkit.getLogger().info("[AC] Avatars initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse avatars from DB: " + e.getMessage());
            loadAvatars();
            loadAvatarBinds();
        }
    }

    /**
     * Loads compressible item types from MySQL. Falls back to file if empty.
     */
    public static void loadCompressibleFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerCompressible();
        if (all.isEmpty()) {
            loadCompressible();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading compressible items from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    try {
                        Material mat = Material.valueOf(el.getAsString());
                        AranarthUtils.addCompressibleItem(uuid, mat);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse compressible for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Compressible items initialized from MySQL");
    }

    /**
     * Loads shop locations from MySQL. Falls back to file if empty.
     */
    public static void loadShopLocationsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerShopLocations();
        if (all.isEmpty()) {
            loadShopLocations();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading shop locations from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonObject obj = GSON.fromJson(entry.getValue(), JsonObject.class);
                String worldName = obj.get("worldName").getAsString();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                double z = obj.get("z").getAsDouble();
                float yaw = obj.get("yaw").getAsFloat();
                float pitch = obj.get("pitch").getAsFloat();
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                AranarthUtils.createShopLocation(uuid, location);
                if (obj.has("centerX") && obj.has("centerZ")) {
                    AranarthUtils.addShopIslandCenter(uuid, obj.get("centerX").getAsInt(), obj.get("centerZ").getAsInt());
                }
                if (obj.has("name")) {
                    AranarthUtils.setShopName(uuid, obj.get("name").getAsString());
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse shop location for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Shop locations initialized from MySQL");
    }

    /**
     * Loads shop collaborators from MySQL. Falls back to file if empty.
     */
    public static void loadShopCollaboratorsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerShopCollaborators();
        if (all.isEmpty()) {
            loadShopCollaborators();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading shop collaborators from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    try {
                        AranarthUtils.addShopCollaborator(uuid, UUID.fromString(el.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse shop collaborators for " + uuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Shop collaborators initialized from MySQL");
    }

    /**
     * Loads sentinel data from MySQL. Must be called after players are loaded.
     * Falls back to file if empty.
     */
    public static void loadSentinelsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllPlayerSentinels();
        if (all.isEmpty()) {
            loadSentinels();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading sentinels from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID playerUuid = entry.getKey();
            AranarthPlayer ap = AranarthUtils.getPlayer(playerUuid);
            if (ap == null) {
                continue;
            }
            try {
                JsonObject obj = GSON.fromJson(entry.getValue(), JsonObject.class);
                HashMap<EntityType, List<Sentinel>> sentinels = new HashMap<>();
                for (EntityType type : new EntityType[]{EntityType.HORSE, EntityType.IRON_GOLEM, EntityType.WOLF}) {
                    List<Sentinel> list = new ArrayList<>();
                    if (obj.has(type.name())) {
                        for (com.google.gson.JsonElement el : obj.getAsJsonArray(type.name())) {
                            JsonObject s = el.getAsJsonObject();
                            UUID sentinelUuid = UUID.fromString(s.get("uuid").getAsString());
                            String worldName = s.get("worldName").getAsString();
                            int x = s.get("x").getAsInt();
                            int y = s.get("y").getAsInt();
                            int z = s.get("z").getAsInt();
                            World world = Bukkit.getWorld(worldName);
                            Location loc = new Location(world, x, y, z);
                            Sentinel sentinel = new Sentinel(sentinelUuid, type, loc);
                            sentinel.setWorldName(worldName);
                            list.add(sentinel);
                        }
                    }
                    sentinels.put(type, list);
                }
                ap.setSentinels(sentinels);
                AranarthUtils.setPlayer(playerUuid, ap);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse sentinels for " + playerUuid + ": " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Sentinels initialized from MySQL");
    }

    /**
     * Loads gates from MySQL. Falls back to file if empty.
     */
    public static void loadGatesFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        String json = db.loadGates();
        if (json == null || json.isEmpty()) {
            loadGates();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading gates from MySQL...");
        GateUtils.clearGates();
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (com.google.gson.JsonElement el : arr) {
                JsonObject g = el.getAsJsonObject();
                UUID id = UUID.fromString(g.get("id").getAsString());
                UUID owner = UUID.fromString(g.get("owner").getAsString());
                Gate.GateType gateType = switch (g.get("gateType").getAsString()) {
                    case "METAL" -> Gate.GateType.METAL;
                    case "NETHER_BRICK" -> Gate.GateType.NETHER_BRICK;
                    case "WALL" -> Gate.GateType.WALL;
                    default -> Gate.GateType.WOODEN;
                };
                boolean isOpen = g.get("isOpen").getAsBoolean();
                String axisStr = g.get("axis").getAsString();
                String worldName = g.get("world").getAsString();
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    continue;
                }
                Gate.Axis axis = null;
                if ("X".equals(axisStr)) {
                    axis = Gate.Axis.X;
                } else if ("Z".equals(axisStr)) {
                    axis = Gate.Axis.Z;
                }
                Map<Location, Material> blockMaterials = new HashMap<>();
                for (com.google.gson.JsonElement bEl : g.getAsJsonArray("blocks")) {
                    JsonObject b = bEl.getAsJsonObject();
                    Location loc = new Location(world, b.get("x").getAsInt(), b.get("y").getAsInt(), b.get("z").getAsInt());
                    Material mat = Material.matchMaterial(b.get("mat").getAsString());
                    if (mat == null) {
                        mat = fallbackMaterial(gateType);
                    }
                    blockMaterials.put(loc, mat);
                }
                if (blockMaterials.isEmpty()) {
                    continue;
                }
                Location firstBlock = blockMaterials.keySet().iterator().next();
                Gate gate = new Gate(id, owner, gateType, firstBlock, blockMaterials.get(firstBlock));
                gate.setBlockMaterials(blockMaterials);
                gate.setAxis(axis);
                gate.setOpen(isOpen);
                GateUtils.addGate(gate);
            }
            Bukkit.getLogger().info("[AC] Gates initialized from MySQL");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[AC] Failed to parse gates from DB: " + e.getMessage());
            loadGates();
        }
    }

    /**
     * Loads outposts from MySQL. Must be called after loadDominionsFromDatabase().
     * Falls back to file if empty.
     */
    public static void loadOutpostsFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllOutposts();
        if (all.isEmpty()) {
            loadOutposts();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading outposts from MySQL...");
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            try {
                JsonObject obj = GSON.fromJson(entry.getValue(), JsonObject.class);
                UUID id = UUID.fromString(obj.get("id").getAsString());
                UUID dominionId = UUID.fromString(obj.get("dominionId").getAsString());
                String name = obj.get("name").getAsString();
                int outpostIndex = obj.get("outpostIndex").getAsInt();
                String worldName = obj.get("worldName").getAsString();
                double homeX = obj.get("homeX").getAsDouble();
                double homeY = obj.get("homeY").getAsDouble();
                double homeZ = obj.get("homeZ").getAsDouble();
                float homeYaw = obj.get("homeYaw").getAsFloat();
                float homePitch = obj.get("homePitch").getAsFloat();
                long createdTimestamp = obj.get("createdTimestamp").getAsLong();
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    Bukkit.getLogger().warning("[AC] Outpost " + name + " references unknown world: " + worldName + " — skipping.");
                    continue;
                }
                List<Chunk> chunks = new ArrayList<>();
                for (com.google.gson.JsonElement cEl : obj.getAsJsonArray("chunks")) {
                    JsonObject c = cEl.getAsJsonObject();
                    chunks.add(world.getChunkAt(c.get("x").getAsInt(), c.get("z").getAsInt()));
                }
                Outpost outpost = new Outpost(
                        id, name, dominionId, outpostIndex,
                        worldName, homeX, homeY, homeZ, homeYaw, homePitch,
                        chunks, createdTimestamp
                );
                OutpostUtils.registerOutpost(outpost);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse outpost " + entry.getKey() + " from DB: " + e.getMessage());
            }
        }
        Bukkit.getLogger().info("[AC] Outposts initialized from MySQL");
    }

    /**
     * Loads defenders from MySQL. Must be called after loadDominionsFromDatabase().
     * Falls back to file if empty.
     */
    public static void loadDefendersFromDatabase() {
        DatabaseManager db = DatabaseManager.getInstance();
        Map<UUID, String> all = db.loadAllDefenders();
        if (all.isEmpty()) {
            loadDefenders();
            return;
        }
        Bukkit.getLogger().info("[AC] Loading defenders from MySQL...");

        record DefenderEntry(
                UUID dominionId, DefenderType type,
                String worldName, double x, double y, double z,
                DefenderMode mode,
                UUID followPlayerId,
                String guardWorld, double guardX, double guardY, double guardZ,
                UUID assignedOutpostId) {
        }

        List<DefenderEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : all.entrySet()) {
            UUID dominionId = entry.getKey();
            try {
                JsonArray arr = GSON.fromJson(entry.getValue(), JsonArray.class);
                for (com.google.gson.JsonElement el : arr) {
                    JsonObject d = el.getAsJsonObject();
                    DefenderType type = DefenderType.valueOf(d.get("type").getAsString());
                    String worldName = d.get("world").getAsString();
                    double x = d.get("x").getAsDouble();
                    double y = d.get("y").getAsDouble();
                    double z = d.get("z").getAsDouble();
                    DefenderMode mode = DefenderMode.PATROL;
                    try {
                        mode = DefenderMode.valueOf(d.get("mode").getAsString());
                    } catch (Exception ignored) {
                    }
                    UUID followPlayerId = null;
                    String followStr = d.has("followPlayerId") ? d.get("followPlayerId").getAsString() : "";
                    if (!followStr.isEmpty()) {
                        try {
                            followPlayerId = UUID.fromString(followStr);
                        } catch (Exception ignored) {
                        }
                    }
                    String guardWorld = d.has("guardWorld") ? d.get("guardWorld").getAsString() : "";
                    double guardX = d.has("guardX") ? d.get("guardX").getAsDouble() : 0;
                    double guardY = d.has("guardY") ? d.get("guardY").getAsDouble() : 0;
                    double guardZ = d.has("guardZ") ? d.get("guardZ").getAsDouble() : 0;
                    UUID assignedOutpostId = null;
                    String outpostStr = d.has("assignedOutpostId") ? d.get("assignedOutpostId").getAsString() : "";
                    if (!outpostStr.isEmpty()) {
                        try {
                            assignedOutpostId = UUID.fromString(outpostStr);
                        } catch (Exception ignored) {
                        }
                    }
                    entries.add(new DefenderEntry(dominionId, type, worldName, x, y, z, mode, followPlayerId,
                            guardWorld.isEmpty() ? null : guardWorld, guardX, guardY, guardZ, assignedOutpostId));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[AC] Failed to parse defenders for dominion " + dominionId + ": " + e.getMessage());
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (DefenderEntry entry : entries) {
                    Dominion dominion = DominionUtils.getDominionById(entry.dominionId());
                    if (dominion == null) {
                        continue;
                    }
                    Location spawnLoc;
                    if (entry.mode() == DefenderMode.FOLLOW && dominion.getDominionHome() != null) {
                        spawnLoc = dominion.getDominionHome();
                    } else {
                        World world = Bukkit.getWorld(entry.worldName());
                        if (world == null) {
                            continue;
                        }
                        spawnLoc = new Location(world, entry.x(), entry.y(), entry.z());
                    }
                    Location guardPos = null;
                    if (entry.guardWorld() != null) {
                        World gWorld = Bukkit.getWorld(entry.guardWorld());
                        if (gWorld != null) {
                            guardPos = new Location(gWorld, entry.guardX(), entry.guardY(), entry.guardZ());
                        }
                    }
                    DefenderUtils.loadAndSpawnAt(entry.dominionId(), entry.type(), spawnLoc,
                            entry.mode(), entry.followPlayerId(), guardPos, entry.assignedOutpostId());
                }
            }
        }.runTaskLater(AranarthCore.getInstance(), 1L);
        Bukkit.getLogger().info("[AC] Defenders scheduled to spawn from MySQL");
    }

}
