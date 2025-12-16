package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to a dominion on Aranarth.
 */
public class Dominion {

	private String name;
	private UUID leader;
	private List<UUID> members;
	private List<UUID> allied;
	private List<UUID> allianceRequests;
	private List<UUID> truced;
	private List<UUID> truceRequests;
	private List<UUID> enemied;
	private List<UUID> neutralRequests;
	private List<Chunk> chunks;
	private int dominionPower;
	private Location dominionHome;
	private double balance;

	public Dominion(String name, UUID leader, List<UUID> members, List<UUID> allied, List<UUID> truced, List<UUID> enemied,
					List<Chunk> chunks, int dominionPower, Location dominionHome, double balance) {
		name = ChatUtils.removeSpecialCharacters(name);
		this.name = name;
		this.leader = leader;
		this.allied = allied;
		this.allianceRequests = new ArrayList<>();
		this.truced = truced;
		this.truceRequests = new ArrayList<>();
		this.enemied = enemied;
		this.neutralRequests = new ArrayList<>();
		this.members = members;
		this.chunks = chunks;
		this.dominionPower = dominionPower;
		this.dominionHome = dominionHome;
		this.balance = balance;
	}

	public Dominion(String name, UUID leader, List<UUID> members, List<UUID> allied, List<UUID> truced, List<UUID> enemied,
					String worldName, List<Chunk> chunks, int dominionPower, double x, double y, double z, float yaw, float pitch,
					double balance) {
		name = ChatUtils.removeSpecialCharacters(name);
		this.name = name;
		this.leader = leader;
		this.allied = allied;
		this.allianceRequests = new ArrayList<>();
		this.truced = truced;
		this.truceRequests = new ArrayList<>();
		this.enemied = enemied;
		this.neutralRequests = new ArrayList<>();
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
	 * Provides the dominion's leader's UUID.
	 * @return the dominion's leader's UUID.
	 */
	public UUID getLeader() {
		return leader;
	}

	/**
	 * Updates the dominion's leader's UUID.
	 * @param leader The dominion's leader's UUID.
     */
	public void setLeader(UUID leader) {
		this.leader = leader;
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
	 * Provides the list of Dominion leaders that are allies.
	 * @return The list of Dominion leaders that are allies.
	 */
	public List<UUID> getAllied() {
		return allied;
	}

	/**
	 * Updates the list of Dominion leaders that are allies.
	 * @param allied The list of Dominion leaders that are allies.
	 */
	public void setAllied(List<UUID> allied) {
		this.allied = allied;
	}

	/**
	 * Provides the list of Dominion leaders that have sent Alliance requests.
	 * @return The list of Dominion leaders that have sent Alliance requests.
	 */
	public List<UUID> getAllianceRequests() {
		return allianceRequests;
	}

	/**
	 * Updates the list of Dominion leaders that have sent Alliance requests.
	 * @param allianceRequests The list of Dominion leaders that have sent Alliance requests.
	 */
	public void setAllianceRequests(List<UUID> allianceRequests) {
		this.allianceRequests = allianceRequests;
	}

	/**
	 * Provides the list of Dominion leaders that are truced.
	 * @return The list of Dominion leaders that are truced.
	 */
	public List<UUID> getTruced() {
		return truced;
	}

	/**
	 * Updates the list of Dominion leaders that are truced.
	 * @param truced The list of Dominion leaders that are truced.
	 */
	public void setTruced(List<UUID> truced) {
		this.truced = truced;
	}

	/**
	 * Provides the list of Dominion leaders that have sent Truce requests.
	 * @return The list of Dominion leaders that have sent Truce requests.
	 */
	public List<UUID> getTruceRequests() {
		return truceRequests;
	}

	/**
	 * Updates the list of Dominion leaders that have sent Truce requests.
	 * @param truceRequests The list of Dominion leaders that have sent Truce requests.
	 */
	public void setTruceRequests(List<UUID> truceRequests) {
		this.truceRequests = truceRequests;
	}

	/**
	 * Provides the list of Dominion leaders that are enemies.
	 * @return The list of Dominion leaders that are enemies.
	 */
	public List<UUID> getEnemied() {
		return enemied;
	}

	/**
	 * Updates the list of Dominion leaders that are enemies.
	 * @param enemied The list of Dominion leaders that are enemies.
	 */
	public void setEnemied(List<UUID> enemied) {
		this.enemied = enemied;
	}

	/**
	 * Provides the list of Dominion leaders that have sent Neutrality requests.
	 * @return The list of Dominion leaders that have sent Neutrality requests.
	 */
	public List<UUID> getNeutralRequests() {
		return neutralRequests;
	}

	/**
	 * Updates the list of Dominion leaders that have sent Neutrality requests.
	 * @param neutralRequests The list of Dominion leaders that have sent Neutrality requests.
	 */
	public void setNeutralRequests(List<UUID> neutralRequests) {
		this.neutralRequests = neutralRequests;
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

}
