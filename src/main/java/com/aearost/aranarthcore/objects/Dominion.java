package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to a dominion on Aranarth.
 */
public class Dominion {

	private String name;
	private UUID owner;
	private List<UUID> members;
	private List<UUID> allied;
	private List<UUID> truced;
	private List<UUID> enemied;
	private List<Chunk> chunks;
	private int dominionPower;
	private Location dominionHome;
	private double balance;

	public Dominion(String name, UUID owner, List<UUID> members, List<UUID> allied, List<UUID> truced, List<UUID> enemied,
					List<Chunk> chunks, int dominionPower, Location dominionHome, double balance) {
		name = ChatUtils.removeSpecialCharacters(name);
		this.name = name;
		this.owner = owner;
		this.allied = allied;
		this.truced = truced;
		this.enemied = enemied;
		this.members = members;
		this.chunks = chunks;
		this.dominionPower = dominionPower;
		this.dominionHome = dominionHome;
		this.balance = balance;
	}

	public Dominion(String name, UUID owner, List<UUID> members, List<UUID> allied, List<UUID> truced, List<UUID> enemied,
					String worldName, List<Chunk> chunks, int dominionPower, double x, double y, double z, float yaw, float pitch,
					double balance) {
		name = ChatUtils.removeSpecialCharacters(name);
		this.name = name;
		this.owner = owner;
		this.allied = allied;
		this.truced = truced;
		this.enemied = enemied;
		this.members = members;
		this.chunks = chunks;
		this.dominionPower = dominionPower;
		this.dominionHome = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
		this.balance = balance;
	}

	/**
	 * Provides the dominion's name.
	 * @return The dominion's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Updates the dominion's name.
	 * @param name The new name of the dominion.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Provides the dominion's owner's UUID.
	 * @return the dominion's owner's UUID.
	 */
	public UUID getOwner() {
		return owner;
	}

	/**
	 * Updates the dominion's owner's UUID.
	 * @param owner The dominion's owner's UUID.
     */
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	/**
	 * Provides the list of players that are trusted to open the dominion.
	 * @return the list of players that are trusted to open the dominion.
	 */
	public List<UUID> getMembers() {
		return this.members;
	}

	/**
	 * Updates the list of players that are trusted in the dominion.
	 * @param members The list of players that will be in the dominion.
	 */
	public void setMembers(List<UUID> members) {
		this.members = members;
	}

	/**
	 * Provides the list of Dominion owners that are allies.
	 * @return The list of Dominion owners that are allies.
	 */
	public List<UUID> getAllied() {
		return allied;
	}

	/**
	 * Updates the list of Dominion owners that are allies.
	 * @param allied The list of Dominion owners that are allies.
	 */
	public void setAllied(List<UUID> allied) {
		this.allied = allied;
	}

	/**
	 * Provides the list of Dominion owners that are truced.
	 * @return The list of Dominion owners that are truced.
	 */
	public List<UUID> getTruced() {
		return truced;
	}

	/**
	 * Updates the list of Dominion owners that are truced.
	 * @param truced The list of Dominion owners that are truced.
	 */
	public void setTruced(List<UUID> truced) {
		this.truced = truced;
	}

	/**
	 * Provides the list of Dominion owners that are enemies.
	 * @return The list of Dominion owners that are enemies.
	 */
	public List<UUID> getEnemied() {
		return enemied;
	}

	/**
	 * Updates the list of Dominion owners that are enemies.
	 * @param enemied The list of Dominion owners that are enemies.
	 */
	public void setEnemied(List<UUID> enemied) {
		this.enemied = enemied;
	}

	/**
	 * Provides the current chunks of the dominion.
	 * @return The current chunks of the dominion.
	 */
	public List<Chunk> getChunks() {
		return chunks;
	}

	/**
	 * Updates the chunks of the dominion.
	 * @param chunks The new chunks of the dominion.
	 */
	public void setChunks(List<Chunk> chunks) {
		this.chunks = chunks;
	}

	/**
	 * Provides the current power of the dominion.
	 * @return The current power of the dominion.
	 */
	public int getDominionPower() {
		return dominionPower;
	}

	/**
	 * Updates the current power of the dominion.
	 * @param dominionPower The new value of the current power of the dominion.
	 */
	public void setDominionPower(int dominionPower) {
		this.dominionPower = dominionPower;
	}

	/**
	 * Provides the current Location of the dominion's home.
	 * @return The current Location of the dominion's home.
	 */
	public Location getDominionHome() {
		return dominionHome;
	}

	/**
	 * Updates the Location of the dominion's home.
	 * @param dominionHome The new Location of the dominion's home.
	 */
	public void setDominionHome(Location dominionHome) {
		this.dominionHome = dominionHome;
	}

	/**
	 * Provides the balance of the dominion.
	 * @return The balance of the dominion.
	 */
	public double getBalance() {
		return balance;
	}

	/**
	 * Updates the balance of the dominion.
	 * @param balance The new balance of the dominion.
	 */
	public void setBalance(double balance) {
		this.balance = balance;
	}

//	@Override
//	public String toString() {
//		String divider = "---------------\n";
//		String owner = "OWNER: " + Bukkit.getOfflinePlayer(getOwner()).getName() + " (" + getOwner() + ")";
//		StringBuilder trustedSB = new StringBuilder();
//		trustedSB.append("TRUSTED: ");
//		for (int i = 0; i < getTrusted().size(); i++) {
//			if (i == getTrusted().size() - 1) {
//				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(")");
//			} else {
//				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(") | ");
//			}
//		}
//		String trusted = trustedSB.toString();
//		String x1 = "x1: " + locations[0].getBlockX() + " | ";
//		String y1 = "y1: " + locations[0].getBlockY() + " | ";
//		String z1 = "z1: " + locations[0].getBlockZ();
//		String x2 = "";
//		String y2 = "";
//		String z2 = "";
//		if (locations[1] != null) {
//			x2 = "x2: " + locations[1].getBlockX() + " | ";
//			y2 = "y2: " + locations[1].getBlockY() + " | ";
//			z2 = "z2: " + locations[1].getBlockZ();
//		}
//		String loc1 = "LOC 1: " + x1 + y1 + z1;
//		String loc2 = "LOC 2: " + x2 + y2 + z2;
//		if (loc2.isEmpty()) {
//			return divider + owner + "\n" + trusted + "\n" + loc1;
//		} else {
//			return divider + owner + "\n" + trusted + "\n" + loc1 + "\n" + loc2;
//		}
//	}

}
