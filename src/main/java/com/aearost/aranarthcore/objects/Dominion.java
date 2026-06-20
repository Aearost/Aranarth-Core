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
	private long conqueredRequestTimestamp;
	private long conqueredRequestDefenderLastSeen;
	private UUID rebelRequest;
	private long rebelRequestTimestamp;
	private long rebelRequestConquerorLastSeen;
	private long lastConquerAttemptTimestamp;
	private long lastRebelAttemptTimestamp;
	private long conqueredTimestamp;

	private boolean memberPvpEnabled;
	private boolean mobSpawningEnabled;
	private int boughtChunks;

	// Dominion level system (1–5), updated automatically every 30 minutes.
	private int dominionLevel;
	private int cachedFarmlandCount;
	private int cachedLivestockCount;
	private long foundedTimestamp;     // ms epoch; 0 = "ancient" (pre-feature legacy dominion)
	private long levelDropTimestamp;   // ms epoch when this dominion first dropped a level; 0 = compliant

	// Outpost system
	private int boughtOutpostChunks;

	// A null entry for a permission means it is inherited from the player's rank or relation.
	private Map<UUID, Map<DominionPermission, Boolean>> playerPermissionOverrides;

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
		this.playerPermissionOverrides = new HashMap<>();
		this.chunks = chunks;
		this.dominionHome = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
		this.food = food;
		this.claimableResources = claimableResources;
		this.biomeResourcesBeingClaimed = null;
		this.conquered = conquered;
		this.conqueredRequest = null;

		// Dominion level system defaults
		this.dominionLevel = 1;
		this.cachedFarmlandCount = 0;
		this.cachedLivestockCount = 0;
		this.foundedTimestamp = 0L;

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
	 * Provides the full map of per-player permission overrides.
	 * @return A map of player UUID to their permission overrides (permission -> Boolean override, null means inherited).
	 */
	public Map<UUID, Map<DominionPermission, Boolean>> getPlayerPermissionOverrides() {
		if (playerPermissionOverrides == null) {
			playerPermissionOverrides = new HashMap<>();
		}
		return playerPermissionOverrides;
	}

	/**
	 * Replaces the full per-player permission overrides map (used during persistence load).
	 * @param overrides The new overrides map.
	 */
	public void setPlayerPermissionOverrides(Map<UUID, Map<DominionPermission, Boolean>> overrides) {
		this.playerPermissionOverrides = overrides != null ? overrides : new HashMap<>();
	}

	/**
	 * Returns the explicit override value for a specific player and permission.
	 * @param playerUuid The player's UUID.
	 * @param permission The permission to check.
	 * @return True/false if explicitly overridden, or null if inherited.
	 */
	public Boolean getPlayerPermissionOverride(UUID playerUuid, DominionPermission permission) {
		Map<DominionPermission, Boolean> overrides = getPlayerPermissionOverrides().get(playerUuid);
		if (overrides == null) return null;
		return overrides.get(permission);
	}

	/**
	 * Toggles an explicit override for the given player and permission.
	 * @param playerUuid The player's UUID.
	 * @param permission The permission to toggle.
	 * @param inheritedValue The value the player would inherit without any override.
	 */
	public void togglePlayerPermissionOverride(UUID playerUuid, DominionPermission permission, boolean inheritedValue) {
		Map<DominionPermission, Boolean> overrides = getPlayerPermissionOverrides().computeIfAbsent(playerUuid, k -> new HashMap<>());
		if (overrides.containsKey(permission)) {
			overrides.put(permission, !overrides.get(permission));
		} else {
			overrides.put(permission, !inheritedValue);
		}
	}

	/**
	 * Removes all per-player permission overrides for the given player.
	 * @param playerUuid The player's UUID.
	 */
	public void clearPlayerPermissionOverrides(UUID playerUuid) {
		getPlayerPermissionOverrides().remove(playerUuid);
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
	 * Provides the number of extra chunks purchased beyond the member-based limit.
	 * @return The number of bought extra chunks.
	 */
	public int getBoughtChunks() {
		return boughtChunks;
	}

	/**
	 * Updates the number of extra chunks purchased beyond the member-based limit.
	 * @param boughtChunks The new count of bought extra chunks.
	 */
	public void setBoughtChunks(int boughtChunks) {
		this.boughtChunks = boughtChunks;
	}

	/**
	 * Provides the maximum number of chunks this Dominion is allowed to claim,
	 * based on member count and bought chunks.
	 * @return The maximum claimable chunk count.
	 */
	public int getMaxChunks() {
		return this.members.size() * 25 + this.boughtChunks;
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
	 * Provides the timestamp of when the active conquer request was issued on this Dominion.
	 * @return The timestamp.
	 */
	public long getConqueredRequestTimestamp() {
		return conqueredRequestTimestamp;
	}

	/**
	 * Updates the timestamp of when the active conquer request was issued on this Dominion.
	 * @param conqueredRequestTimestamp The timestamp.
	 */
	public void setConqueredRequestTimestamp(long conqueredRequestTimestamp) {
		this.conqueredRequestTimestamp = conqueredRequestTimestamp;
	}

	/**
	 * Provides the timestamp of the last time any defending member logged on during an active conquest window.
	 * Used for rolling 3-day inactivity detection (auto-conquest if no defender seen for 3 consecutive days).
	 * @return The timestamp in milliseconds, or 0 if not set.
	 */
	public long getConqueredRequestDefenderLastSeen() {
		return conqueredRequestDefenderLastSeen;
	}

	/**
	 * Updates the last-seen timestamp for defending members during an active conquest window.
	 * @param timestamp The timestamp in milliseconds.
	 */
	public void setConqueredRequestDefenderLastSeen(long timestamp) {
		this.conqueredRequestDefenderLastSeen = timestamp;
	}

	/**
	 * Provides the timestamp of the last time this Dominion issued a conquer attempt.
	 * @return The timestamp.
	 */
	public long getLastConquerAttemptTimestamp() {
		return lastConquerAttemptTimestamp;
	}

	/**
	 * Updates the timestamp of the last time this Dominion issued a conquer attempt.
	 * @param lastConquerAttemptTimestamp The timestamp.
	 */
	public void setLastConquerAttemptTimestamp(long lastConquerAttemptTimestamp) {
		this.lastConquerAttemptTimestamp = lastConquerAttemptTimestamp;
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

	/**
	 * Provides the timestamp of when the active rebel request was issued on this Dominion.
	 * @return The timestamp, or 0 if none.
	 */
	public long getRebelRequestTimestamp() {
		return rebelRequestTimestamp;
	}

	/**
	 * Updates the timestamp of when the rebel request was issued on this Dominion.
	 * @param rebelRequestTimestamp The timestamp, or 0 to clear.
	 */
	public void setRebelRequestTimestamp(long rebelRequestTimestamp) {
		this.rebelRequestTimestamp = rebelRequestTimestamp;
	}

	/**
	 * Provides the last time any member of this (conquering) Dominion logged on since a rebellion started.
	 * Used for the rolling 3-day inactivity check. 0 means never updated.
	 * @return The timestamp, or 0 if no rebellion is active.
	 */
	public long getRebelRequestConquerorLastSeen() {
		return rebelRequestConquerorLastSeen;
	}

	/**
	 * Updates the last-seen timestamp for the conquering Dominion during an active rebellion.
	 * @param rebelRequestConquerorLastSeen The timestamp.
	 */
	public void setRebelRequestConquerorLastSeen(long rebelRequestConquerorLastSeen) {
		this.rebelRequestConquerorLastSeen = rebelRequestConquerorLastSeen;
	}

	/**
	 * Provides the timestamp of when the last rebellion attempt by this Dominion ended.
	 * Used to enforce the 3-day rebellion cooldown.
	 * @return The timestamp, or 0 if they have never rebelled.
	 */
	public long getLastRebelAttemptTimestamp() {
		return lastRebelAttemptTimestamp;
	}

	/**
	 * Updates the timestamp of when the last rebellion attempt ended.
	 * @param lastRebelAttemptTimestamp The timestamp, or 0 to clear.
	 */
	public void setLastRebelAttemptTimestamp(long lastRebelAttemptTimestamp) {
		this.lastRebelAttemptTimestamp = lastRebelAttemptTimestamp;
	}

	/**
	 * Provides the timestamp of when this Dominion was conquered.
	 * @return The epoch millisecond when conquest completed, or 0 if never conquered.
	 */
	public long getConqueredTimestamp() {
		return conqueredTimestamp;
	}

	/**
	 * Updates the timestamp of when this Dominion was conquered.
	 * @param conqueredTimestamp The epoch millisecond when conquest completed, or 0 to clear.
	 */
	public void setConqueredTimestamp(long conqueredTimestamp) {
		this.conqueredTimestamp = conqueredTimestamp;
	}

	/**
	 * Provides the current dominion level (1–5).
	 * @return The dominion level.
	 */
	public int getDominionLevel() {
		return dominionLevel;
	}

	/**
	 * Updates the dominion level (1–5).
	 * @param dominionLevel The new dominion level.
	 */
	public void setDominionLevel(int dominionLevel) {
		this.dominionLevel = dominionLevel;
	}

	/**
	 * Provides the cached farmland-with-crop count from the last periodic scan.
	 * @return The cached farmland block count.
	 */
	public int getCachedFarmlandCount() {
		return cachedFarmlandCount;
	}

	/**
	 * Updates the cached farmland-with-crop count.
	 * @param cachedFarmlandCount The new count.
	 */
	public void setCachedFarmlandCount(int cachedFarmlandCount) {
		this.cachedFarmlandCount = cachedFarmlandCount;
	}

	/**
	 * Provides the cached domesticated livestock count from the last periodic scan.
	 * @return The cached livestock count.
	 */
	public int getCachedLivestockCount() {
		return cachedLivestockCount;
	}

	/**
	 * Updates the cached domesticated livestock count.
	 * @param cachedLivestockCount The new count.
	 */
	public void setCachedLivestockCount(int cachedLivestockCount) {
		this.cachedLivestockCount = cachedLivestockCount;
	}

	/**
	 * Provides the epoch-millisecond timestamp when this dominion was founded.
	 * A value of 0 indicates a legacy dominion (treated as "ancient" for age criteria).
	 * @return The founded timestamp, or 0 for legacy dominions.
	 */
	public long getFoundedTimestamp() {
		return foundedTimestamp;
	}

	/**
	 * Updates the epoch-millisecond timestamp when this dominion was founded.
	 * @param foundedTimestamp The founded timestamp.
	 */
	public void setFoundedTimestamp(long foundedTimestamp) {
		this.foundedTimestamp = foundedTimestamp;
	}

	/**
	 * Provides the epoch-millisecond timestamp when this dominion first dropped a level.
	 * A value of 0 indicates the dominion is currently compliant (no active penalty window).
	 * @return The level-drop timestamp, or 0 if compliant.
	 */
	public long getLevelDropTimestamp() {
		return levelDropTimestamp;
	}

	/**
	 * Updates the epoch-millisecond timestamp when this dominion first dropped a level.
	 * Set to 0 to clear the penalty window (dominion has recovered).
	 * @param levelDropTimestamp The timestamp, or 0 to clear.
	 */
	public void setLevelDropTimestamp(long levelDropTimestamp) {
		this.levelDropTimestamp = levelDropTimestamp;
	}

	/**
	 * Provides the number of extra outpost chunks purchased beyond each outpost's base limit.
	 * This value applies equally to every outpost owned by this dominion.
	 * @return The number of bought extra outpost chunks.
	 */
	public int getBoughtOutpostChunks() {
		return boughtOutpostChunks;
	}

	/**
	 * Updates the number of extra outpost chunks purchased.
	 * @param boughtOutpostChunks The new count.
	 */
	public void setBoughtOutpostChunks(int boughtOutpostChunks) {
		this.boughtOutpostChunks = boughtOutpostChunks;
	}

	/**
	 * Determines if this Dominion and the other Dominion are mutual allies.
	 * Both dominions must have each other in their allied lists.
	 * @param other The other Dominion.
	 * @return True if both dominions are allied with each other.
	 */
	public boolean isAllied(Dominion other) {
		return this.allied.contains(other.leader) && other.allied.contains(this.leader);
	}

	/**
	 * Determines if this Dominion and the other Dominion have a mutual truce.
	 * Both dominions must have each other in their truced lists.
	 * @param other The other Dominion.
	 * @return True if both dominions are truced with each other.
	 */
	public boolean isTruced(Dominion other) {
		return this.truced.contains(other.leader) && other.truced.contains(this.leader);
	}

	/**
	 * Determines if this Dominion and the other Dominion are enemies.
	 * If either dominion has the other in their enemy list, both are considered enemies.
	 * @param other The other Dominion.
	 * @return True if either dominion has marked the other as an enemy.
	 */
	public boolean isEnemied(Dominion other) {
		return this.enemied.contains(other.leader) || other.enemied.contains(this.leader);
	}

	/**
	 * Determines if this Dominion and the other Dominion are neutral with each other.
	 * Two dominions are neutral if they are not allied, truced, or enemied.
	 * @param other The other Dominion.
	 * @return True if the two dominions have no active relationship.
	 */
	public boolean isNeutral(Dominion other) {
		return !isAllied(other) && !isTruced(other) && !isEnemied(other);
	}

	/**
	 * Determines if this Dominion is the same as the other Dominion.
	 * @param other The other Dominion.
	 * @return True if both dominions share the same stable ID.
	 */
	public boolean isSameDominion(Dominion other) {
		return this.id.equals(other.id);
	}
}
