package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.Month;
import com.aearost.aranarthcore.enums.Pronouns;
import com.aearost.aranarthcore.enums.QuestTaskType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.objects.*;
import com.aearost.aranarthcore.objects.Quest;
import com.projectkorra.projectkorra.BendingPlayer;

import java.util.stream.Collectors;

import com.projectkorra.projectkorra.OfflineBendingPlayer;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

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

                // uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|saintExpirationDate|isCompressingItems|votePointsSpent|firstJoinDate|pronouns
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
                AranarthUtils.getPlayer(uuid).setConquestDisbandCooldownEnd(conquestDisbandCooldownEnd);
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
                    writer.write("#uuid|nickname|survivalInventory|arenaInventory|creativeInventory|potions|arrows|blacklist|isDeletingBlacklistedItems|balance|rank|saint|council|architect|homes|muteEndDate|particles|perks|saintExpirationDate|isCompressingItems|votePointsSpent|spawnBoostValue|firstJoinDate|pronouns\n");

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
                                String part = ItemUtils.itemStackArrayToBase64(new ItemStack[]{potion});
                                part += "*" + amount + "***";
                                potions += part;
                            }

                            if (potions.endsWith("***")) {
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
                        String blacklistingMethod = "0";
                        if (aranarthPlayer.getBlacklistingMethod() == -1) {
                            blacklistingMethod = "-1";
                        } else if (aranarthPlayer.getBlacklistingMethod() == 1) {
                            blacklistingMethod = "1";
                        } else {
                            blacklistingMethod = "0";
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
                                    homes.add(name + "*" + worldName + "*" + x + "*" + y + "*" + z + "*" + yaw + "*" + pitch + "*" + type.name());
                                } else {
                                    homes.add(name + "*" + worldName + "*" + x + "*" + y + "*" + z + "*" + yaw + "*" + pitch + "*" + type.name() + "***");
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

                        String perks = "";
                        for (int i = 0; i < Perk.values().length; i++) {
                            Perk perk = Perk.values()[i];
                            if (aranarthPlayer.getPerks().get(perk) == null) {
                                perks += 0;
                            } else {
                                // May be 0, 1, or a multiple of 3 if it's the homes perk
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
                        String row = uuid + "|" + nickname + "|" + survivalInventory + "|" + arenaInventory + "|"
                                + creativeInventory + "|" + potions + "|" + arrows + "|" + blacklist + "|" + blacklistingMethod
                                + "|" + balance + "|" + rank + "|" + saint + "|" + council + "|" + architect + "|"
                                + allHomes + "|" + muteEndDate + "|" + particles + "|" + perks + "|" + saintExpireDate
                                + "|" + isCompressingItems + "|" + votePointsSpent + "|" + spawnBoostValue + "|"
                                + firstJoinDate + "|" + conquestDisbandCooldownEnd + "|"
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
            Bukkit.getLogger().info("Attempting to read the toggled file...");

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
            Bukkit.getLogger().info("All toggled features have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the toggled features!");
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
                        Bukkit.getLogger().info("A new toggled.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("An error occurred in the creation of toggled.txt");
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
                    Bukkit.getLogger().info("There was an error in saving the toggled features!");
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
                ShopUtils.addShop(uuid, playerShop);
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
                        Bukkit.getLogger().info("A new shops.txt file has been generated");
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().info("An error occurred in the creation of shops.txt");
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
                            String worldName = shop.getLocation().getWorld().getName();
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
                                trusted.append("***").append(trustedUuid.toString());
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
                        writer.write(row);
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
                DominionUtils.resizeFoodArray(dominion);
                DominionUtils.createDominion(dominion);
            }
            Bukkit.getLogger().info("All dominions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the dominions!");
        } catch (IOException e) {
            Bukkit.getLogger().info("Something went wrong with instantiating a dominion's food");
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
                    writer.write("#id|name|leader|members|allied|truced|enemied|world|chunks|x|y|z|yaw|pitch|food|claimableResources|conquered|balance|memberRanks|memberPvpEnabled|mobSpawningEnabled|conqueredRequestTimestamp|lastConquerAttemptTimestamp|rebelRequestTimestamp|conqueredRequestDefenderLastSeen|rebelRequestConquerorLastSeen|lastRebelAttemptTimestamp|conqueredTimestamp|boughtChunks|dominionLevel|cachedFarmlandCount|cachedLivestockCount|foundedTimestamp|levelDropTimestamp|boughtOutpostChunks\n");

                    if (dominions != null && !dominions.isEmpty()) {
                        for (Dominion dominion : dominions) {
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

                            String worldName = dominion.getDominionHome().getWorld().getName();

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

                            String row = dominionId + "|" + name + "|" + leader + "|" + membersString + "|" + alliesString + "|" + trucedString + "|"
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
                                    + "|" + dominion.getBoughtOutpostChunks() + "\n";
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
            Bukkit.getLogger().info("Attempting to read the dominions_permissions file...");

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
            Bukkit.getLogger().info("All dominion permissions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading dominion permissions!");
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
                    Bukkit.getLogger().info("A new dominions_permissions.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred creating dominions_permissions.txt");
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
                Bukkit.getLogger().info("There was an error saving dominion permissions!");
            }
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
            Bukkit.getLogger().info("Attempting to read the dominions_player_permissions file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#") || row.isBlank()) {
                    continue;
                }

                String[] fields = row.split("\\|", -1);
                if (fields.length < 2) continue;

                UUID dominionId;
                try {
                    dominionId = UUID.fromString(fields[0]);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                Dominion dominion = DominionUtils.getDominionById(dominionId);
                if (dominion == null) continue;

                Map<UUID, Map<DominionPermission, Boolean>> allOverrides = new HashMap<>();

                if (!fields[1].isEmpty()) {
                    for (String playerEntry : fields[1].split(";")) {
                        String[] parts = playerEntry.split(":", 2);
                        if (parts.length != 2) continue;

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
                                if (kv.length != 2) continue;
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

            Bukkit.getLogger().info("All dominion player permissions have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading dominion player permissions!");
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
                    Bukkit.getLogger().info("A new dominions_player_permissions.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred creating dominions_player_permissions.txt");
                return;
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#dominionId|playerOverrides\n");
                writer.write("#format: dominionId|playerUUID:PERM1=true,PERM2=false;playerUUID2:PERM3=true\n");

                for (Dominion dominion : dominions) {
                    Map<UUID, Map<DominionPermission, Boolean>> allOverrides =
                            dominion.getPlayerPermissionOverrides();
                    if (allOverrides.isEmpty()) continue;

                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<UUID, Map<DominionPermission, Boolean>> playerEntry
                            : allOverrides.entrySet()) {
                        if (playerEntry.getValue().isEmpty()) continue;

                        if (!builder.isEmpty()) builder.append(";");
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
                Bukkit.getLogger().info("There was an error saving dominion player permissions!");
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
                Bukkit.getLogger().info("There was an error in saving the avatars");
            }
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

            Bukkit.getLogger().info("Attempting to read the avatar_binds file...");
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
            Bukkit.getLogger().info("The avatar's binds have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the avatar's binds!");
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
                    Bukkit.getLogger().info("A new avatar_binds.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of avatar_binds.txt");
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
                Bukkit.getLogger().info("There was an error in saving the avatar binds");
            }
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

            Bukkit.getLogger().info("Attempting to read the server boosts file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                String[] parts = row.split("\\|");

                Boost boost = Boost.valueOf(parts[0]);
                LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[1])), ZoneId.systemDefault());
                AranarthUtils.addServerBoost(boost, end, null, false);
            }
            Bukkit.getLogger().info("The server boosts have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the server boosts");
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
                    Bukkit.getLogger().info("A new boosts.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of boosts.txt");
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
                Bukkit.getLogger().info("There was an error in saving the server boosts");
            }
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

            Bukkit.getLogger().info("Attempting to read the compressible items lists file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                String[] parts = row.split("\\*");
                UUID uuid = UUID.fromString(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    Material material = Material.valueOf(parts[i]);
                    AranarthUtils.addCompressibleItem(uuid, material);
                }
            }
            Bukkit.getLogger().info("The compressible items lists have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the compressible items lists");
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
                    Bukkit.getLogger().info("A new compressible.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of compressible.txt");
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
                Bukkit.getLogger().info("There was an error in saving the compressible items lists");
            }
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

            Bukkit.getLogger().info("Attempting to read the shop locations file...");

            while (reader.hasNextLine()) {
                String row = reader.nextLine();
                if (row.startsWith("#")) {
                    continue;
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
            }
            Bukkit.getLogger().info("The shop locations have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the shop locations");
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
                    Bukkit.getLogger().info("A new shop_locations.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of shop_locations.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                HashMap<UUID, Location> shopLocations = AranarthUtils.getShopLocations();
                java.util.HashMap<UUID, int[]> shopIslandCenters = AranarthUtils.getShopIslandCenters();
                for (UUID uuid : shopLocations.keySet()) {
                    Location location = shopLocations.get(uuid);
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
                    writer.write(shopLocation + "\n");
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the shop locations");
            }
        }
    }

    /**
     * Loads the shop island counter from config.yml (key: shop.island-counter).
     */
    public static void loadShopIslandCounter() {
        int counter = AranarthCore.getInstance().getConfig().getInt("shop.island-counter", 0);
        AranarthUtils.setShopIslandCounter(counter);
        Bukkit.getLogger().info("Shop island counter loaded: " + counter);
    }

    /**
     * Saves the shop island counter to config.yml (key: shop.island-counter).
     */
    public static void saveShopIslandCounter() {
        AranarthCore.getInstance().getConfig().set("shop.island-counter", AranarthUtils.getShopIslandCounter());
        AranarthCore.getInstance().saveConfig();
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

            Bukkit.getLogger().info("Attempting to read the votes file...");

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
            Bukkit.getLogger().info("The votes have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the votes");
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
                    Bukkit.getLogger().info("A new votes.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of votes.txt");
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
                Bukkit.getLogger().info("There was an error in saving the votes");
            }
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

            Bukkit.getLogger().info("Attempting to read the sentinels file...");

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
                    wolves.add(sentinel);
                }

                sentinels.put(EntityType.HORSE, horse);
                sentinels.put(EntityType.IRON_GOLEM, ironGolems);
                sentinels.put(EntityType.WOLF, wolves);
                aranarthPlayer.setSentinels(sentinels);
                AranarthUtils.setPlayer(playerUuid, aranarthPlayer);
            }
            Bukkit.getLogger().info("The sentinels have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the sentinels");
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
                    Bukkit.getLogger().info("A new sentinels.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of sentinels.txt");
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
                        playerSentinels += loc.getWorld().getName() + "_";
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
                        playerSentinels += loc.getWorld().getName() + "_";
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
                        playerSentinels += loc.getWorld().getName() + "_";
                        playerSentinels += loc.getBlockX() + "_";
                        playerSentinels += loc.getBlockY() + "_";
                        playerSentinels += loc.getBlockZ() + "___";
                    }

                    playerSentinels += "\n";
                    writer.write(playerSentinels);
                }

                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the sentinels");
            }
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

            Bukkit.getLogger().info("Attempting to read the kills and deaths file...");
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
            Bukkit.getLogger().info("All kills and deaths have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the kills and deaths!");
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
                    Bukkit.getLogger().info("A new kills_and_deaths.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of kills_and_deaths.txt");
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
                Bukkit.getLogger().info("There was an error in saving the kills and deaths");
            }
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
            Bukkit.getLogger().info("Attempting to read the vote keys file...");

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
            Bukkit.getLogger().info("All pending vote keys have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the vote keys!");
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
                    Bukkit.getLogger().info("A new vote_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of vote_keys.txt");
            }

            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingVoteKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the vote keys");
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
            Bukkit.getLogger().info("Something went wrong with loading the rare keys!");
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
                    Bukkit.getLogger().info("A new rare_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of rare_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingRareKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the rare keys");
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
            Bukkit.getLogger().info("Something went wrong with loading the epic keys!");
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
                    Bukkit.getLogger().info("A new epic_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of epic_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingEpicKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the epic keys");
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
            Bukkit.getLogger().info("Something went wrong with loading the godly keys!");
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
                    Bukkit.getLogger().info("A new godly_keys.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of godly_keys.txt");
            }
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write("#uuid|amount\n");
                for (Map.Entry<UUID, Integer> entry : AranarthUtils.getPendingGodlyKeys().entrySet()) {
                    writer.write(entry.getKey().toString() + "|" + entry.getValue() + "\n");
                }
                writer.close();
            } catch (IOException e) {
                Bukkit.getLogger().info("There was an error in saving the godly keys");
            }
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
            Bukkit.getLogger().info("Attempting to read the quest_state file...");

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

            Bukkit.getLogger().info("Quest state has been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the quest state!");
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
                Bukkit.getLogger().info("A new quest_state.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred in the creation of quest_state.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#Quest state — do not edit manually\n");
            writer.write("lastDailyReset|" + QuestUtils.getLastDailyReset() + "\n");
            writer.write("lastWeeklyReset|" + QuestUtils.getLastWeeklyReset() + "\n");
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("There was an error in saving the quest state");
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
            Bukkit.getLogger().info("Attempting to read the quest_progress file...");

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

            Bukkit.getLogger().info("Quest progress has been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading quest progress!");
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
                QuestTaskType taskType = QuestTaskType.valueOf(taskName);
                Quest found = null;
                for (Quest q : pool) {
                    if (q.getTaskType() == taskType) {
                        found = q;
                        break;
                    }
                }
                if (found != null) {
                    double reward = rewards[i] > 0 ? rewards[i] : QuestUtils.generateRandomReward(rank, type);
                    resolved.add(found.withReward(reward));
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
                Bukkit.getLogger().info("A new quest_progress.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred in the creation of quest_progress.txt");
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
                    String task = i < dq.size() ? dq.get(i).getTaskType().name() : "NONE";
                    int reward = i < dq.size() ? (int) dq.get(i).getReward() : 0;
                    int prog = i < dp.length ? dp[i] : 0;
                    int done = (i < dc.length && dc[i]) ? 1 : 0;
                    int claimed = (i < dClaim.length && dClaim[i]) ? 1 : 0;
                    row.append("|").append(task).append("|").append(reward).append("|").append(prog).append("|").append(done).append("|").append(claimed);
                }
                for (int i = 0; i < 3; i++) {
                    String task = i < wq.size() ? wq.get(i).getTaskType().name() : "NONE";
                    int reward = i < wq.size() ? (int) wq.get(i).getReward() : 0;
                    int prog = i < wp.length ? wp[i] : 0;
                    int done = (i < wc.length && wc[i]) ? 1 : 0;
                    int claimed = (i < wClaim.length && wClaim[i]) ? 1 : 0;
                    row.append("|").append(task).append("|").append(reward).append("|").append(prog).append("|").append(done).append("|").append(claimed);
                }
                writer.write(row + "\n");
            }

            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("There was an error in saving quest progress");
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
            Bukkit.getLogger().info("Attempting to read the login_streaks file...");

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

            Bukkit.getLogger().info("Login streaks have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading login streaks!");
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
                Bukkit.getLogger().info("A new login_streaks.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred in the creation of login_streaks.txt");
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
            Bukkit.getLogger().info("There was an error in saving login streaks");
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
            Bukkit.getLogger().info("Attempting to read the gates file...");

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

            Bukkit.getLogger().info("All gates have been initialized");
            reader.close();
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading the gates!");
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
                    Bukkit.getLogger().info("A new gates.txt file has been generated");
                }
            } catch (IOException e) {
                Bukkit.getLogger().info("An error occurred in the creation of gates.txt");
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
                Bukkit.getLogger().info("There was an error in saving the gates");
            }
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
            Bukkit.getLogger().info("Attempting to read the petprogress file...");
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

                    com.aearost.aranarthcore.objects.Mount pm =
                            new com.aearost.aranarthcore.objects.Mount(
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
                    com.aearost.aranarthcore.utils.MountUtils.put(uuid, element, pm);
                } catch (Exception ignored) {
                }
            }
            reader.close();
            Bukkit.getLogger().info("Mount progress has been initialised");
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading petprogress.txt!");
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
                Bukkit.getLogger().info("A new petprogress.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred in the creation of petprogress.txt");
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
            Bukkit.getLogger().info("There was an error in saving mount progress");
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
            Bukkit.getLogger().info("Attempting to read the mail file...");
            HashMap<UUID, List<com.aearost.aranarthcore.objects.Mail>> mailData = new HashMap<>();
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
                            .add(new com.aearost.aranarthcore.objects.Mail(senderUUID, recipientUUID, timestamp, message));
                } catch (Exception ignored) {
                }
            }
            reader.close();
            MailUtils.setAllMail(mailData);
            Bukkit.getLogger().info("Mail has been initialised");
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().info("Something went wrong with loading mail.txt!");
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
                Bukkit.getLogger().info("A new mail.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred in the creation of mail.txt");
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#Mail data — do not edit manually\n");
            for (Map.Entry<UUID, List<com.aearost.aranarthcore.objects.Mail>> entry : MailUtils.getAllMail().entrySet()) {
                UUID recipientUUID = entry.getKey();
                for (com.aearost.aranarthcore.objects.Mail mail : entry.getValue()) {
                    writer.write(recipientUUID + "|" + mail.getSenderUUID() + "|"
                            + mail.getTimestamp() + "|" + mail.getMessage() + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            Bukkit.getLogger().info("There was an error in saving mail data");
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
            Bukkit.getLogger().info("Attempting to read the outposts file...");

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

                org.bukkit.World world = Bukkit.getWorld(worldName);
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

                com.aearost.aranarthcore.objects.Outpost outpost = new com.aearost.aranarthcore.objects.Outpost(
                        id, name, dominionId, outpostIndex,
                        worldName, homeX, homeY, homeZ, homeYaw, homePitch,
                        chunks, createdTimestamp
                );
                OutpostUtils.registerOutpost(outpost);
            }

            Bukkit.getLogger().info("All outposts have been initialized");
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
                Bukkit.getLogger().info("A new outposts.txt file has been generated");
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("An error occurred creating outposts.txt");
            return;
        }

        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write("#id|dominionId|name|outpostIndex|worldName|homeX|homeY|homeZ|homeYaw|homePitch|chunks|createdTimestamp\n");

            for (com.aearost.aranarthcore.objects.Dominion dominion : DominionUtils.getDominions()) {
                for (com.aearost.aranarthcore.objects.Outpost outpost : OutpostUtils.getDominionOutposts(dominion.getId())) {
                    StringBuilder chunks = new StringBuilder();
                    for (Chunk chunk : outpost.getChunks()) {
                        if (!chunks.isEmpty()) chunks.append("***");
                        chunks.append(chunk.getX()).append(",").append(chunk.getZ());
                    }

                    org.bukkit.Location home = outpost.getHome();
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
            Bukkit.getLogger().info("There was an error saving outposts!");
        }
    }

}
