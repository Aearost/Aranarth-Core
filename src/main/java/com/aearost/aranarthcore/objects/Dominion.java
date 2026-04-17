package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Handles all necessary functionality relating to a dominion on Aranarth.
 */
public class Dominion {

	private final UUID id;
	private String name;
	private UUID leader;
	private List<UUID> members;
	private Map<UUID, DominionRank> memberRanks;
	private DominionPermissions dominionPermissions;
	private List<UUID> allied;
	private List<UUID> allianceRequests;
	private List<UUID> truced;
	private List<UUID> truceRequests;
	private List<UUID> enemied;
	private List<UUID> neutralRequests;
	private List<Chunk> chunks;
	private Location dominionHome;
	private ItemStack[] food;
	private int foodPowerBeingConsumed;
	private int claimableResources;
	private Biome biomeResourcesBeingClaimed;
	private List<UUID> conquered;
	private UUID conqueredRequest;
	private UUID rebelRequest;

	private boolean memberPvpEnabled;
	private boolean mobSpawningEnabled;

	// Keep balance at the end
	private double balance;

	public Dominion(UUID id, String name, UUID leader, List<UUID> members, Map<UUID, DominionRank> memberRanks,
					List<UUID> allied, List<UUID> truced, List<UUID> enemied,
					String worldName, List<Chunk> chunks, double x, double y, double z, float yaw, float pitch, ItemStack[] food,
					int claimableResources, List<UUID> conquered,
					DominionPermissions dominionPermissions,
					// Keep balance at the end
					double balance) {
		this.id = id != null ? id : UUID.randomUUID();
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
		this.memberRanks = memberRanks != null ? memberRanks : new HashMap<>();
		this.dominionPermissions = dominionPermissions != null ? dominionPermissions : DominionPermissions.createDefaults();
		this.chunks = chunks;
		this.dominionHome = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
		this.food = food;
		this.claimableResources = claimableResources;
		this.biomeResourcesBeingClaimed = null;
		this.conquered = conquered;
		this.conqueredRequest = null;

		// Keep balance at the end
		this.balance = balance;
	}

	/**
	 * Provides the dominion's stable unique ID.
	 * @return The dominion's stable unique ID.
	 */
	public UUID getId() {
		return id;
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
	 * Provides the map of member UUIDs to their respective ranks.
	 * @return The map of member UUIDs to their ranks.
	 */
	public Map<UUID, DominionRank> getMemberRanks() {
		return memberRanks;
	}

	/**
	 * Updates the map of member UUIDs to their respective ranks.
	 * @param memberRanks The new member ranks map.
	 */
	public void setMemberRanks(Map<UUID, DominionRank> memberRanks) {
		this.memberRanks = memberRanks;
	}

	/**
	 * Provides the rank of a specific member.
	 * @param uuid The UUID of the member.
	 * @return The rank of the member, or null if not found.
	 */
	public DominionRank getMemberRank(UUID uuid) {
		return memberRanks.get(uuid);
	}

	/**
	 * Sets the rank of a specific member.
	 * @param uuid The UUID of the member.
	 * @param rank The rank to assign.
	 */
	public void setMemberRank(UUID uuid, DominionRank rank) {
		memberRanks.put(uuid, rank);
	}

	/**
	 * Provides the permissions configuration for this dominion.
	 * @return The DominionPermissions instance.
	 */
	public DominionPermissions getDominionPermissions() {
		return dominionPermissions;
	}

	/**
	 * Updates the permissions configuration for this dominion.
	 * @param dominionPermissions The new DominionPermissions instance.
	 */
	public void setDominionPermissions(DominionPermissions dominionPermissions) {
		this.dominionPermissions = dominionPermissions;
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
	 * Returns whether PvP between members of this dominion is enabled.
	 * @return True if member PvP is enabled.
	 */
	public boolean isMemberPvpEnabled() {
		return memberPvpEnabled;
	}

	/**
	 * Sets whether PvP between members of this dominion is enabled.
	 * @param memberPvpEnabled True to allow members to harm each other.
	 */
	public void setMemberPvpEnabled(boolean memberPvpEnabled) {
		this.memberPvpEnabled = memberPvpEnabled;
	}

	/**
	 * Returns whether mob spawning is allowed in this dominion's chunks.
	 * @return True if mob spawning is allowed.
	 */
	public boolean isMobSpawningEnabled() {
		return mobSpawningEnabled;
	}

	/**
	 * Sets whether mob spawning is allowed in this dominion's chunks.
	 * @param mobSpawningEnabled True to allow mobs to spawn.
	 */
	public void setMobSpawningEnabled(boolean mobSpawningEnabled) {
		this.mobSpawningEnabled = mobSpawningEnabled;
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

	/**
	 * Provides the food that a dominion has stored.
	 * @return The food that a dominion has stored.
	 */
	public ItemStack[] getFood() {
		return food;
	}

	/**
	 * Updates the food that a dominion has stored.
	 * @param food The food that a dominion has stored.
	 */
	public void setFood(ItemStack[] food) {
		this.food = food;
	}

	/**
	 * Provides the temporary variable tracking the power from the food item that is currently being consumed.
	 * @return The temporary variable tracking the power from the food item that is currently being consumed.
	 */
	public int getFoodPowerBeingConsumed() {
		return foodPowerBeingConsumed;
	}

	/**
	 * Updates the temporary variable tracking the power from the food item that is currently being consumed.
	 * @param foodPowerBeingConsumed The temporary variable tracking the power from the food item that is currently being consumed.
	 */
	public void setFoodPowerBeingConsumed(int foodPowerBeingConsumed) {
		this.foodPowerBeingConsumed = foodPowerBeingConsumed;
	}

	/**
	 * Provides the number of available resource claims a Dominion has available.
	 * @return The number of available resource claims a Dominion has available.
	 */
	public int getClaimableResources() {
		return claimableResources;
	}

	/**
	 * Updates the number of available resource claims a Dominion has available.
	 * @param claimableResources The number of available resource claims a Dominion has available.
	 */
	public void setClaimableResources(int claimableResources) {
		this.claimableResources = claimableResources;
	}

	/**
	 * Provides the Biome that the Dominion is currently attempting to claim resources from.
	 * @return The Biome that the Dominion is currently attempting to claim resources from.
	 */
	public Biome getBiomeResourcesBeingClaimed() {
		return biomeResourcesBeingClaimed;
	}

	/**
	 * Updates the Biome that the Dominion is currently attempting to claim resources from.
	 * @param biomeResourcesBeingClaimed The Biome that the Dominion is currently attempting to claim resources from.
	 */
	public void setBiomeResourcesBeingClaimed(Biome biomeResourcesBeingClaimed) {
		this.biomeResourcesBeingClaimed = biomeResourcesBeingClaimed;
	}

	/**
	 * Provides the list of Dominion leaders that have been conquered by this Dominion.
	 * @return The list of Dominion leaders that have been conquered by this Dominion.
	 */
	public List<UUID> getConquered() {
		return conquered;
	}

	/**
	 * Updates the list of Dominion leaders that have been conquered by this Dominion.
	 * @param conquered The list of Dominion leaders that have been conquered by this Dominion.
	 */
	public void setConquered(List<UUID> conquered) {
		this.conquered = conquered;
	}

	/**
	 * Provides the UUID of the Dominion leader that sent a conquer request.
	 * @return The UUID of the Dominion leader that sent a conquer request.
	 */
	public UUID getConqueredRequest() {
		return conqueredRequest;
	}

	/**
	 * Updates the UUID of the Dominion leader that sent a conquer request.
	 * @param conqueredRequest The UUID of the Dominion leader that sent a conquer request.
	 */
	public void setConqueredRequest(UUID conqueredRequest) {
		this.conqueredRequest = conqueredRequest;
	}

	/**
	 * Provides the UUID of the Dominion leader of the conquered Dominion.
	 * @return The UUID of the Dominion leader of the conquered Dominion.
	 */
	public UUID getRebelRequest() {
		return rebelRequest;
	}

	/**
	 * Updates the UUID of the Dominion leader of the conquered Dominion.
	 * @param rebelRequest The UUID of the Dominion leader of the conquered Dominion.
	 */
	public void setRebelRequest(UUID rebelRequest) {
		this.rebelRequest = rebelRequest;
	}
}
