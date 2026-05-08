package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.Pronouns;
import org.bukkit.Location;
import org.bukkit.MusicInstrument;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


/**
 * Handles all necessary functionality relating to a player on Aranarth.
 */
public class AranarthPlayer {

	private String username;
	private boolean isStandingOnHomePad;
	private int currentGuiPageNum;
	private String nickname;
	private String survivalInventory;
	private String arenaInventory;
	private String creativeInventory;
	private int blacklistingMethod;
	private HashMap<ItemStack, Integer> potions;
	private List<ItemStack> arrows;
	private List<ItemStack> blacklist;
	private boolean isHitByTippedArrow;
	private float expBeforeDeath;
	private int levelBeforeDeath;
	private boolean isAddingToShulker;
	private List<RandomItem> randomItems;
	private boolean isRandomizing;
	private boolean isMissingItemMessageSent;
	private double balance;
	private int potionQuantityToRemove;
	private UUID trustedPlayerUUID;
	private UUID untrustedPlayerUUID;
	private boolean isLockingContainer;
	private boolean isUnlockingContainer;
	private Pronouns pronouns;
	private int rank;
	private int saintRank;
	private int councilRank;
	private int architectRank;
	private Dominion pendingDominion;
	private List<Home> homes;
	private String muteEndDate;
	private UUID teleportToUuid;
	private UUID teleportFromUuid;
	private Location lastKnownTeleportLocation;
	private int particleNum = 100;
	private boolean isAddingPotions = false;
	private HashMap<Perk, Integer> perks = new HashMap<>();
	private boolean isInAdminMode = false;
	private long saintExpireDate;
	private boolean isCompressingItems = true;
	private int bulkTransactionNum = 0;
	private boolean isOpeningCrateWithCyclingItem = false;
	private CrateType crateTypeBeingOpened = null;
	private boolean isAutoClaimEnabled = false;
	private boolean isTogglingMessages = false;
	private boolean isTogglingChat = false;
	private boolean isTogglingTp = false;
	private boolean isTogglingChangeClaim = false;
	private boolean hasBlueFireDisabled = false;
	private boolean isUsingGoatHorn = false;
	private HashMap<MusicInstrument, Long> horns = new HashMap<>();
	private long lastWorldCommandUse = 0;
	private HashMap<EntityType, List<Sentinel>> sentinel = new HashMap<>();
	private int plentifulBlocksToDestroy = 0;
	private AfkLocation afkLocation = null;
	private int votePointsSpent = 0;
	private UUID lastReceivedMessage = null;
	private boolean isUsingSpawnBoost = true;
	private HashMap<UUID, Long> combatLogTime = new HashMap<>();
	private boolean isTogglingInventoryAssist = false;
	private boolean isAutoLockingChests = true;
	private String firstJoinDate = "";
	private boolean isVanished = false;
	private boolean isInCouncilChat = false;
	private boolean isInDominionChat = false;
	private String dominionChatType = "dominion";
	private boolean isHurtingOwnPets = false;
	private boolean isGradientChatEnabled = false;
	private String gradientChatColors = "";
	private boolean isGradientChatBold = false;
	private UUID petTransferUuid = null;
	private boolean isDayMessageDisabled = false;
	private boolean isWeatherMessageDisabled = false;
	private boolean isBulkSellShulkerEnabled = false;

	public AranarthPlayer(String username) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = "";
		this.survivalInventory = "";
		this.arenaInventory = "";
		this.creativeInventory = "";
		this.potions = null;
		this.arrows = null;
		this.blacklist = null;
		this.blacklistingMethod = 0;
		this.isHitByTippedArrow = false;
		this.isAddingToShulker = true;
		this.randomItems = new ArrayList<>();
		this.isRandomizing = false;
		this.isMissingItemMessageSent = false;
		this.balance = 0.00;
		this.potionQuantityToRemove = 0;
		this.trustedPlayerUUID = null;
		this.untrustedPlayerUUID = null;
		this.isLockingContainer = false;
		this.isUnlockingContainer = false;
		this.rank = 0;
		this.saintRank = 0;
		this.councilRank = 0;
		this.architectRank = 0;
		this.pendingDominion = null;
		this.homes = new ArrayList<>();
		this.muteEndDate = "";
		this.pronouns = Pronouns.MALE;
		this.teleportFromUuid = null;
		this.teleportToUuid = null;
		this.lastKnownTeleportLocation = null;
		this.particleNum = 100;
		this.isAddingPotions = false;
		this.perks = new HashMap<>();
		this.isInAdminMode = false;
		this.saintExpireDate = 0;
		this.isCompressingItems = true;
		this.bulkTransactionNum = 0;
		this.votePointsSpent = 0;
		this.isAutoClaimEnabled = false;
		this.isTogglingMessages = false;
		this.isTogglingChat = false;
		this.isTogglingTp = false;
		this.isTogglingChangeClaim = false;
		this.hasBlueFireDisabled = false;
		this.isUsingGoatHorn = false;
		this.horns = new HashMap<>();
		this.lastWorldCommandUse = 0;
		this.sentinel = new HashMap<>();
		this.afkLocation = null;
		this.lastReceivedMessage = null;
		this.isUsingSpawnBoost = true;
		this.combatLogTime = new HashMap<>();
		this.isTogglingInventoryAssist = false;
		this.isAutoLockingChests = true;
		this.firstJoinDate = "";
		this.isVanished = false;
		this.isInCouncilChat = false;
		this.isHurtingOwnPets = false;
		this.isGradientChatEnabled = false;
		this.gradientChatColors = "";
		this.isGradientChatBold = false;
		this.petTransferUuid = null;
	}

	public AranarthPlayer(String username, String nickname, String survivalInventory, String arenaInventory,
						  String creativeInventory, HashMap<ItemStack, Integer> potions, List<ItemStack> arrows,
						  List<ItemStack> blacklist, int blacklistingMethod, double balance, int rank,
						  int saintRank, int councilRank, int architectRank, List<Home> homes, String muteEndDate,
						  int particleNum, HashMap<Perk, Integer> perks, long saintExpireDate, boolean isCompressingItems,
						  int votePointsSpent, boolean isUsingSpawnBoost, String firstJoinDate,
						  Pronouns pronouns) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = nickname;
		this.survivalInventory = survivalInventory;
		this.arenaInventory = arenaInventory;
		this.creativeInventory = creativeInventory;
		this.potions = potions;
		this.arrows = arrows;
		this.blacklist = blacklist;
		this.blacklistingMethod = blacklistingMethod;
		this.isHitByTippedArrow = false;
		this.isAddingToShulker = true;
		this.randomItems = null;
		this.isRandomizing = false;
		this.isMissingItemMessageSent = false;
		this.balance = balance;
		this.potionQuantityToRemove = 0;
		this.trustedPlayerUUID = null;
		this.untrustedPlayerUUID = null;
		this.isLockingContainer = false;
		this.isUnlockingContainer = false;
		this.rank = rank;
		this.saintRank = saintRank;
		this.councilRank = councilRank;
		this.architectRank = architectRank;
		this.pendingDominion = null;
		this.homes = homes;
		this.muteEndDate = "";
		this.teleportFromUuid = null;
		this.teleportToUuid = null;
		this.lastKnownTeleportLocation = null;
		this.particleNum = particleNum;
		this.isAddingPotions = false;
		this.perks = perks;
		this.isInAdminMode = false;
		this.saintExpireDate = saintExpireDate;
		this.isCompressingItems = isCompressingItems;
		this.bulkTransactionNum = 0;
		this.votePointsSpent = votePointsSpent;
		this.isAutoClaimEnabled = false;
		this.isTogglingMessages = false;
		this.isTogglingChat = false;
		this.isTogglingTp = false;
		this.isTogglingChangeClaim = false;
		this.hasBlueFireDisabled = false;
		this.isUsingGoatHorn = false;
		this.horns = new HashMap<>();
		this.lastWorldCommandUse = 0;
		this.sentinel = new HashMap<>();
		this.afkLocation = null;
		this.lastReceivedMessage = null;
		this.isUsingSpawnBoost = isUsingSpawnBoost;
		this.combatLogTime = new HashMap<>();
		this.isTogglingInventoryAssist = false;
		this.isAutoLockingChests = true;
		this.firstJoinDate = firstJoinDate;
		this.isVanished = false;
		this.isInCouncilChat = false;
		this.isHurtingOwnPets = false;
		this.isGradientChatEnabled = false;
		this.gradientChatColors = "";
		this.isGradientChatBold = false;
		this.petTransferUuid = null;

		// Keep pronouns at the end
		this.pronouns = pronouns;
	}

	/**
	 * Provides the username of the player.
	 * @return The current username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Updates the username of the player.
	 * @param username The new username.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Provides confirmation whether the player is on a homepad.
	 * @return Confirmation whether they are on a homepad.
	 */
	public boolean isStandingOnHomePad() {
		return isStandingOnHomePad;
	}

	/**
	 * Updates whether the player is on a homepad.
	 * @param isStandingOnHomePad The new value.
	 */
	public void setStandingOnHomePad(boolean isStandingOnHomePad) {
		this.isStandingOnHomePad = isStandingOnHomePad;
	}

	/**
	 * Provides the current page of GUI the player is on.
	 * @return The current page number.
	 */
	public int getCurrentGuiPageNum() {
		return currentGuiPageNum;
	}

	/**
	 * Updates the current page number.
	 * @param currentGuiPageNum The new page number.
	 */
	public void setCurrentGuiPageNum(int currentGuiPageNum) {
		this.currentGuiPageNum = currentGuiPageNum;
	}

	/**
	 * Provides the player's current nickname.
	 * @return The current nickname.
	 */
	public String getNickname() {
		return (nickname == null || nickname.isEmpty()) ? getUsername() : nickname;
	}

	/**
	 * Updates the current nickname.
	 * @param nickname The new nickname.
	 */
	public void setNickname(String nickname) {
		nickname = nickname.replaceAll("\\|", "");
		this.nickname = nickname;
	}

	/**
	 * Provides the player's current Survival inventory.
	 * @return The player's current Survival inventory.
	 */
	public String getSurvivalInventory() {
		return survivalInventory;
	}

	/**
	 * Updates the player's Survival inventory.
	 * @param survivalInventory The new Survival inventory.
	 */
	public void setSurvivalInventory(String survivalInventory) {
		this.survivalInventory = survivalInventory;
	}

	/**
	 * Provides the player's current Arena inventory.
	 * @return The player's current Arena inventory.
	 */
	public String getArenaInventory() {
		return arenaInventory;
	}

	/**
	 * Updates the player's Arena inventory.
	 * @param arenaInventory The new Arena inventory.
	 */
	public void setArenaInventory(String arenaInventory) {
		this.arenaInventory = arenaInventory;
	}

	/**
	 * Provides the player's current Creative inventory.
	 * @return The player's current Creative inventory.
	 */
	public String getCreativeInventory() {
		return creativeInventory;
	}

	/**
	 * Updates the player's Creative inventory.
	 * @param creativeInventory The new Creative inventory.
	 */
	public void setCreativeInventory(String creativeInventory) {
		this.creativeInventory = creativeInventory;
	}

	/**
	 * Provides the blacklisting method used by the player.
	 * @return The blacklisting method used by the player.
	 */
	public int getBlacklistingMethod() {
		return blacklistingMethod;
	}

	/**
	 * Updates the blacklisting method used by the player.
	 * @param blacklistingMethod The blacklisting method used by the player.
	 */
	public void setBlacklistingMethod(int blacklistingMethod) {
		this.blacklistingMethod = blacklistingMethod;
	}

	/**
	 * Provides the List of potions that the player currently has in their potion inventory.
	 * @return The List of the player's potions.
	 */
	public HashMap<ItemStack, Integer> getPotions() {
		return potions;
	}

	/**
	 * Updates the player's current List of potions.
	 * @param potions The new List of potions.
	 */
	public void setPotions(HashMap<ItemStack, Integer> potions) {
		this.potions = potions;
	}

	/**
	 * Provides the List of arrows that the player currently has in their quiver.
	 * @return The List of the player's arrows.
	 */
	public List<ItemStack> getArrows() {
		return arrows;
	}

	/**
	 * Updates the player's current List of arrows.
	 * @param arrows The new List of arrows.
	 */
	public void setArrows(List<ItemStack> arrows) {
		this.arrows = arrows;
	}

	/**
	 * Provides the List of blacklisted items that the player has.
	 * @return The List of the player's blacklisted items.
	 */
	public List<ItemStack> getBlacklist() {
		return blacklist;
	}

	/**
	 * Updates the player's current List of blacklisted items.
	 * @param blacklist The new List of blacklisted Items.
	 */
	public void setBlacklist(List<ItemStack> blacklist) {
		this.blacklist = blacklist;
	}

	/**
	 * Provides the current value confirming if they were hit by a tipped arrow.
	 * @return The current value confirming if they were hit by a tipped arrow.
	 */
	public boolean isHitByTippedArrow() {
		return isHitByTippedArrow;
	}

	/**
	 * Updates the player's current value confirming if they were hit by a tipped arrow.
	 * @param isHitByTippedArrow The new confirmation of if they were hit by a tipped arrow.
	 */
	public void setHitByTippedArrow(boolean isHitByTippedArrow) {
		this.isHitByTippedArrow = isHitByTippedArrow;
	}

	/**
	 * Provides the player's EXP before they died in the arena or creative world.
	 * @return The player's EXP before they died in the arena or creative world.
	 */
	public float getExpBeforeDeath() {
		return expBeforeDeath;
	}

	/**
	 * Updates the player's current EXP before they died in the arena or creative world.
	 * @param expBeforeDeath The player's current EXP before they died in the arena or creative world.
	 */
	public void setExpBeforeDeath(float expBeforeDeath) {
		this.expBeforeDeath = expBeforeDeath;
	}

	/**
	 * Provides the player's level before they died in the arena or creative world.
	 * @return The player's level before they died in the arena or creative world.
	 */
	public int getLevelBeforeDeath() {
		return levelBeforeDeath;
	}

	/**
	 * Updates the player's current level before they died in the arena or creative world.
	 * @param levelBeforeDeath The player's current level before they died in the arena or creative world.
	 */
	public void setLevelBeforeDeath(int levelBeforeDeath) {
		this.levelBeforeDeath = levelBeforeDeath;
	}

	/**
	 * Temporary variable tracking if the player is actively adding to their shulker box.
	 * @return Whether the player is adding to their shulker box.
	 */
	public boolean isAddingToShulker() {
		return isAddingToShulker;
	}

	/**
	 * Setting the temporary variable tracking whether the player is adding to their shulker box.
	 * @param isAddingToShulker Whether the player is adding to their shulker box.
	 */
	public void setAddingToShulker(boolean isAddingToShulker) {
		this.isAddingToShulker = isAddingToShulker;
	}

	/**
	 * Temporary variable tracking the items and percentages the player is randomizing.
	 * @return The list of items and percentages the player is randomizing.
	 */
	public List<RandomItem> getRandomItems() {
		return randomItems;
	}

	/**
	 * Setting the list of items and percentages the player is randomizing.
	 * @param randomItems The list of items and percentages the player will randomize.
	 */
	public void setRandomItems(List<RandomItem> randomItems) {
		this.randomItems = randomItems;
	}

	/**
	 * Temporary variable tracking if the player is actively randomizing items.
	 * @return Whether the player is randomizing items.
	 */
	public boolean isRandomizing() {
		return isRandomizing;
	}

	/**
	 * Setting the temporary variable tracking whether the player is randomizing items.
	 * @param isRandomizing Whether the player is randomizing items.
	 */
	public void isRandomizing(boolean isRandomizing) {
		this.isRandomizing = isRandomizing;
	}

	/**
	 * Temporary variable tracking if the player is missing items from the randomizer pattern.
	 * @return Whether the player is missing items from the randomizer pattern.
	 */
	public boolean isMissingItemMessageSent() {
		return isMissingItemMessageSent;
	}

	/**
	 * Setting the temporary variable tracking whether the player is missing items from the randomizer pattern.
	 * @param isMissingItemMessageSent Whether the player is missing items from the randomizer pattern.
	 */
	public void isMissingItemMessageSent(boolean isMissingItemMessageSent) {
		this.isMissingItemMessageSent = isMissingItemMessageSent;
	}

	/**
	 * Provides the player's current balance.
	 * @return The player's current balance.
	 */
	public double getBalance() {
		return balance;
	}

	/**
	 * Sets the player's current balance.
	 * @param balance The new value for the player's balance.
	 */
	public void setBalance(double balance) {
		this.balance = balance;
	}

	/**
	 * Provides the temporary amount of the potion to be removed from the /potions remove command.
	 * @return The quantity to be removed.
	 */
	public int getPotionQuantityToRemove() {
		return potionQuantityToRemove;
	}

	/**
	 * Updates the temporary amount of the potion to be removed from the /potions remove command.
	 * @param potionQuantityToRemove The quantity of the potion to be removed.
	 */
	public void setPotionQuantityToRemove(int potionQuantityToRemove) {
		this.potionQuantityToRemove = potionQuantityToRemove;
	}

	/**
	 * Provides the temporary variable tracking which player is being trusted to a container.
	 * @return The player's UUID that will be trusted to the container.
	 */
	public UUID getTrustedPlayerUUID() {
		return trustedPlayerUUID;
	}

	/**
	 * Updates the temporary variable tracking which player is being trusted to a container.
	 * @param trustedPlayerUUID The UUID of the player to be trusted.
	 */
	public void setTrustedPlayerUUID(UUID trustedPlayerUUID) {
		this.trustedPlayerUUID = trustedPlayerUUID;
	}

	/**
	 * Provides the temporary variable tracking which player is being untrusted from a container.
	 * @return The player's UUID that will be untrusted from the container.
	 */
	public UUID getUntrustedPlayerUUID() {
		return untrustedPlayerUUID;
	}

	/**
	 * Updates the temporary variable tracking which player is being untrusted from a container.
	 * @param untrustedPlayerUUID The UUID of the player to be untrusted.
	 */
	public void setUntrustedPlayerUUID(UUID untrustedPlayerUUID) {
		this.untrustedPlayerUUID = untrustedPlayerUUID;
	}

	/**
	 * Provides the temporary variable tracking whether the player is attempting to unlock a container.
	 * @return Whether the player is attempting to unlock a container.
	 */
	public boolean isUnlockingContainer() {
		return isUnlockingContainer;
	}

	/**
	 * Updates the temporary variable tracking whether the player is attempting to unlock a container.
	 * @param isUnlockingContainer The temporary variable of whether the player is attempting to unlock a container.
	 */
	public void setUnlockingContainer(boolean isUnlockingContainer) {
		this.isUnlockingContainer = isUnlockingContainer;
	}

	/**
	 * Provides the temporary variable tracking whether the player is attempting to lock a container.
	 * @return Whether the player is attempting to lock a container.
	 */
	public boolean isLockingContainer() {
		return isLockingContainer;
	}

	/**
	 * Updates the temporary variable tracking whether the player is attempting to lock a container.
	 * @param isLockingContainer The temporary variable of whether the player is attempting to lock a container.
	 */
	public void setLockingContainer(boolean isLockingContainer) {
		this.isLockingContainer = isLockingContainer;
	}

	/**
	 * Provides the player's pronouns.
	 * @return The player's pronouns.
	 */
	public Pronouns getPronouns() {
		return pronouns;
	}

	/**
	 * Updates the player's pronouns to the input value.
	 * @param pronouns The new value.
	 */
	public void setPronouns(Pronouns pronouns) {
		this.pronouns = pronouns;
	}

	/**
	 * Provides the rank of the player.
	 * @return The rank of the player.
	 */
	public int getRank() {
		return rank;
	}

	/**
	 * Updates the rank of the player.
	 * @param rank The new rank of the player.
	 */
	public void setRank(int rank) {
		this.rank = rank;
	}

	/**
	 * Provides the Saint rank of the player.
	 * @return The Saint rank of the player.
	 */
	public int getSaintRank() {
		return saintRank;
	}

	/**
	 * Updates the Saint rank of the player.
	 * @param saintRank The new Saint rank of the player.
	 */
	public void setSaintRank(int saintRank) {
		this.saintRank = saintRank;
	}

	/**
	 * Provides the Council rank of the player.
	 * @return The Council rank of the player.
	 */
	public int getCouncilRank() {
		return councilRank;
	}

	/**
	 * Updates the Council rank of the player.
	 * @param councilRank The new Council rank of the player.
	 */
	public void setCouncilRank(int councilRank) {
		this.councilRank = councilRank;
	}

	/**
	 * Provides the Architect rank of the player.
	 * @return The Architect rank of the player.
	 */
	public int getArchitectRank() {
		return architectRank;
	}

	/**
	 * Updates the Architect rank of the player.
	 * @param architectRank The new Architect rank of the player.
	 */
	public void setArchitectRank(int architectRank) {
		this.architectRank = architectRank;
	}

	/**
	 * Provides the pending Dominion of the player.
	 * @return The pending Dominion of the player.
	 */
	public Dominion getPendingDominion() {
		return pendingDominion;
	}

	/**
	 * Updates the pending Dominion of the player.
	 * @param pendingDominion The pending Dominion.
	 */
	public void setPendingDominion(Dominion pendingDominion) {
		this.pendingDominion = pendingDominion;
	}

	/**
	 * Provides the player's homes.
	 * @return The player's homes.
	 */
	public List<Home> getHomes() {
		return homes;
	}

	/**
	 * Updates the homes of the player.
	 * @param homes The new homes of the player.
	 */
	public void setHomes(List<Home> homes) {
		this.homes = homes;
	}

	/**
	 * Provides the player's mute end date.
	 * YYMMDDhhmm - Year | Month | Day | Hour | Minute
	 * If value is "none", the player is muted indefinitely.
	 * @return The player's mute end date.
	 */
	public String getMuteEndDate() {
		return muteEndDate;
	}

	/**
	 * Updates the player's mute end date.
	 * YYMMDDhhmm - Year | Month | Day | Hour | Minute
	 * If value is "none", the player is muted indefinitely.
	 * @param muteEndDate The player's new mute end date.
	 */
	public void setMuteEndDate(String muteEndDate) {
		this.muteEndDate = muteEndDate;
	}

	/**
	 * Provides the UUID of the player that sent the request to teleport to them.
	 * @return The UUID of the player that sent the request to teleport to them.
	 */
	public UUID getTeleportToUuid() {
		return teleportToUuid;
	}

	/**
	 * Updates the UUID of the player that sent the request to teleport to them.
	 * @param teleportToUuid The new UUID of the player that sent the request to teleport to them.
	 */
	public void setTeleportToUuid(UUID teleportToUuid) {
		this.teleportToUuid = teleportToUuid;
	}

	/**
	 * Provides the UUID of the player that sent the request to teleport to this player.
	 * @return The UUID of the player that sent the request to teleport to this player.
	 */
	public UUID getTeleportFromUuid() {
		return teleportFromUuid;
	}

	/**
	 * Updates the UUID of the player that sent the request to teleport to this player
	 * @param teleportFromUuid The new UUID of the player that sent the request to teleport to this player
	 */
	public void setTeleportFromUuid(UUID teleportFromUuid) {
		this.teleportFromUuid = teleportFromUuid;
	}

	/**
	 * Provides the last known location of the player before a previous teleport.
	 * @return The last known location of the player before a previous teleport.
	 */
	public Location getLastKnownTeleportLocation() {
		return lastKnownTeleportLocation;
	}

	/**
	 * Updates the last known location of the player before a previous teleport.
	 * @param lastKnownTeleportLocation The new last known location of the player before a previous teleport.
	 */
	public void setLastKnownTeleportLocation(Location lastKnownTeleportLocation) {
		this.lastKnownTeleportLocation = lastKnownTeleportLocation;
	}

	/**
	 * Provides the number of Aranarth particles seen by the player.
	 * @return The number of Aranarth particles seen by the player.
	 */
	public int getParticleNum() {
		return particleNum;
	}

	/**
	 * Updates the number of Aranarth particles seen by the player.
	 * @param particleNum The number of Aranarth particles seen by the player.
	 */
	public void setParticleNum(int particleNum) {
		this.particleNum = particleNum;
	}

	/**
	 * Provides whether the player is adding potions to their potions pouch.
	 * @return Whether the player is adding potions to their potions pouch
	 */
	public boolean isAddingPotions() {
		return isAddingPotions;
	}

	/**
	 * Updates whether the player is adding potions to their potions pouch.
	 * @param isAddingPotions Whether the player is adding potions to their potions pouch.
	 */
	public void setAddingPotions(boolean isAddingPotions) {
		this.isAddingPotions = isAddingPotions;
	}

	/**
	 * Provides the perks that the player has access to.
	 * @return The perks that the player has access to.
	 */
	public HashMap<Perk, Integer> getPerks() {
		return perks;
	}

	/**
	 * Updates the perks that the player has access to.
	 * @param perks The player's new perks.
	 */
	public void setPerks(HashMap<Perk, Integer> perks) {
		this.perks = perks;
	}

	/**
	 * Provides the variable tracking whether the player is in admin mode.
	 * @return Whether the player is in admin mode.
	 */
	public boolean isInAdminMode() {
		return isInAdminMode;
	}

	/**
	 * Updates the variable tracking whether the player is in admin mode.
	 * @param isInAdminMode Whether the player is in admin mode.
	 */
	public void setInAdminMode(boolean isInAdminMode) {
		this.isInAdminMode = isInAdminMode;
	}

	/**
	 * Provides the expiration date of the player's temporary saint rank.
	 * @return The expiration date of the player's temporary saint rank.
	 */
	public long getSaintExpireDate() {
		return saintExpireDate;
	}

	/**
	 * Updates the expiration date of the player's temporary saint rank.
	 * @param saintExpireDate The expiration date of the player's temporary saint rank.
	 */
	public void setSaintExpireDate(long saintExpireDate) {
		this.saintExpireDate = saintExpireDate;
	}

	/**
	 * Provides the variable tracking whether the player is compressing items or not.
	 * @return Whether the player is compressing items or not.
	 */
	public boolean isCompressingItems() {
		return isCompressingItems;
	}

	/**
	 * Updates the variable tracking whether the player is compressing items or not.
	 * @param isCompressingItems Whether the player is compressing items or not.
	 */
	public void setCompressingItems(boolean isCompressingItems) {
		this.isCompressingItems = isCompressingItems;
	}

	/**
	 * Provides the variable tracking which point of the bulk transaction the player is currently making.
	 * 0 is none, 1 is actively making a bulk purchase, -1 is just finished making a bulk purchase.
	 */
	public int getBulkTransactionNum() {
		return bulkTransactionNum;
	}

	/**
	 * Updates the variable tracking which point of the bulk transaction the player is currently making.
	 * @param bulkTransactionNum Which point of the bulk transaction the player is currently making.
	 */
	public void setBulkTransactionNum(int bulkTransactionNum) {
		this.bulkTransactionNum = bulkTransactionNum;
	}

	/**
	 * Provides the variable tracking whether the player is currently opening a crate with a cycling item.
	 * @return The variable tracking whether the player is currently opening a crate with a cycling item.
	 */
	public boolean isOpeningCrateWithCyclingItem() {
		return isOpeningCrateWithCyclingItem;
	}

	/**
	 * Updates the variable tracking whether the player is currently opening a crate with a cycling item.
	 * @param isOpeningCrateWithCyclingItem The variable tracking whether the player is currently opening a crate with a cycling item.
	 */
	public void setOpeningCrateWithCyclingItem(boolean isOpeningCrateWithCyclingItem) {
		this.isOpeningCrateWithCyclingItem = isOpeningCrateWithCyclingItem;
	}

	/**
	 * Provides the variable tracking what kind of crate is currently being opened by the player.
	 * @return The crate type that is currently being opened by the player.
	 */
	public CrateType getCrateTypeBeingOpened() {
		return crateTypeBeingOpened;
	}

	/**
	 * Updates the variable tracking what kind of crate is currently being opened by the player.
	 * @param crateTypeBeingOpened The crate type that is currently being opened by the player.
	 */
	public void setCrateTypeBeingOpened(CrateType crateTypeBeingOpened) {
		this.crateTypeBeingOpened = crateTypeBeingOpened;
	}

	/**
	 * Provides the value tracking whether the player is claiming chunks.
	 * @return The value tracking whether the player is claiming chunks.
	 */
	public boolean isAutoClaimEnabled() {
		return isAutoClaimEnabled;
	}

	/**
	 * Updates the value tracking whether the player is claiming chunks.
	 * @param isAutoClaimEnabled The value tracking whether the player is claiming chunks.
	 */
	public void setAutoClaimEnabled(boolean isAutoClaimEnabled) {
		this.isAutoClaimEnabled = isAutoClaimEnabled;
	}

	/**
	 * Provides the value tracking whether the player is toggling incoming messages.
	 * @return The value tracking whether the player is toggling incoming messages.
	 */
	public boolean isTogglingMessages() {
		return isTogglingMessages;
	}

	/**
	 * Updates the value tracking whether the player is toggling incoming messages.
	 * @param togglingMessages The value tracking whether the player is toggling incoming messages.
	 */
	public void setTogglingMessages(boolean togglingMessages) {
		isTogglingMessages = togglingMessages;
	}

	/**
	 * Updates the value tracking whether the player is toggling incoming chat messages.
	 * @return The value tracking whether the player is toggling incoming chat messages.
	 */
	public boolean isTogglingChat() {
		return isTogglingChat;
	}

	/**
	 * Updates the value tracking whether the player is toggling incoming chat messages.
	 * @param togglingChat The value tracking whether the player is toggling incoming chat messages.
	 */
	public void setTogglingChat(boolean togglingChat) {
		isTogglingChat = togglingChat;
	}

	/**
	 * Provides the value tracking whether the player is toggling incoming teleport requests.
	 * @return The value tracking whether the player is toggling incoming teleport requests.
	 */
	public boolean isTogglingTp() {
		return isTogglingTp;
	}

	/**
	 * Updates the value tracking whether the player is toggling incoming teleport requests.
	 * @param togglingTp The value tracking whether the player is toggling incoming teleport requests.
	 */
	public void setTogglingTp(boolean togglingTp) {
		isTogglingTp = togglingTp;
	}

	/**
	 * Updates the value tracking whether the player is toggling claim change messages.
	 * @return The value tracking whether the player is toggling claim change messages.
	 */
	public boolean isTogglingChangeClaim() {
		return isTogglingChangeClaim;
	}

	/**
	 * Updates the value tracking whether the player is toggling claim change messages.
	 * @param togglingChangeClaim The value tracking whether the player is toggling claim change messages.
	 */
	public void setTogglingChangeClaim(boolean togglingChangeClaim) {
		isTogglingChangeClaim = togglingChangeClaim;
	}

	/**
	 * Provides the value tracking whether the player currently has blue fire disabled.
	 * @return The value tracking whether the player currently has blue fire disabled.
	 */
	public boolean hasBlueFireDisabled() {
		return hasBlueFireDisabled;
	}

	/**
	 * Updates the value tracking whether the player currently has blue fire disabled.
	 * @param hasBlueFireDisabled The value tracking whether the player currently has blue fire disabled.
	 */
	public void setBlueFireDisabled(boolean hasBlueFireDisabled) {
		this.hasBlueFireDisabled = hasBlueFireDisabled;
	}

	/**
	 * Provides the HashMap tracking the last executions of each horn.
	 * @return The HashMap tracking the last executions of each horn.
	 */
	public HashMap<MusicInstrument, Long> getHorns() {
		return horns;
	}

	/**
	 * Updates the HashMap tracking the last executions of each horn.
	 * @param horns The HashMap tracking the last executions of each horn.
	 */
	public void setHorns(HashMap<MusicInstrument, Long> horns) {
		this.horns = horns;
	}

	/**
	 * Provides the value tracking whether the player has just used a Goat Horn.
	 * @return The value tracking whether the player has just used a Goat Horn.
	 */
	public boolean isUsingGoatHorn() {
		return isUsingGoatHorn;
	}

	/**
	 * Updates the value tracking whether the player has just used a Goat Horn.
	 * @param usingGoatHorn The value tracking whether the player has just used a Goat Horn.
	 */
	public void setUsingGoatHorn(boolean usingGoatHorn) {
		isUsingGoatHorn = usingGoatHorn;
	}

	/**
	 * Provides the last time that the player changed world via commands.
	 * @return The last time that the player changed world via commands.
	 */
	public long getLastWorldCommandUse() {
		return lastWorldCommandUse;
	}

	/**
	 * Updates the last time that the player changed world via commands.
	 * @param lastWorldCommandUse The last time that the player changed world via commands.
	 */
	public void setLastWorldCommandUse(long lastWorldCommandUse) {
		this.lastWorldCommandUse = lastWorldCommandUse;
	}

	/**
	 * Provides the HashMap of the player's designated sentinels to be summoned by a horn.
	 * @return The HashMap of the player's designated sentinels to be summoned by a horn.
	 */
	public HashMap<EntityType, List<Sentinel>> getSentinels() {
		return sentinel;
	}

	/**
	 * Updates the HashMap of the player's designated sentinels to be summoned by a horn.
	 * @param sentinels The HashMap of the player's designated sentinels to be summoned by a horn.
	 */
	public void setSentinels(HashMap<EntityType, List<Sentinel>> sentinels) {
		this.sentinel = sentinels;
	}

	/**
	 * Provides the temporary variable tracking the number of blocks to destroy from the Incantation of Plentiful.
	 * @return The temporary variable tracking the number of blocks to destroy from the Incantation of Plentiful.
	 */
	public int getPlentifulBlocksToDestroy() {
		return plentifulBlocksToDestroy;
	}

	/**
	 * Updates the temporary variable tracking the number of blocks to destroy from the Incantation of Plentiful.
	 * @param plentifulBlocksToDestroy The temporary variable tracking the number of blocks to destroy from the Incantation of Plentiful.
	 */
	public void setPlentifulBlocksToDestroy(int plentifulBlocksToDestroy) {
		this.plentifulBlocksToDestroy = plentifulBlocksToDestroy;
	}

	/**
	 * Provides the AFK Location tracking where the player is currently located.
	 * @return The AFK Location tracking where the player is currently located.
	 */
	public AfkLocation getAfkLocation() {
		return afkLocation;
	}

	/**
	 * Updates the AFK Location tracking where the player is currently located.
	 * @param afkLocation The AFK Location tracking where the player is currently located.
	 */
	public void setAfkLocation(AfkLocation afkLocation) {
		this.afkLocation = afkLocation;
	}

	/**
	 * Provides the number of vote points that have been spent by the player.
	 * @return The number of vote points that have been spent by the player.
	 */
	public int getVotePointsSpent() {
		return votePointsSpent;
	}

	/**
	 * Updates the number of vote points that have been spent by the player.
	 * @param votePointsSpent The number of vote points that have been spent by the player.
	 */
	public void setVotePointsSpent(int votePointsSpent) {
		this.votePointsSpent = votePointsSpent;
	}

	/**
	 * Provides the UUID of the player who last messaged this player.
	 * @return The UUID of the player who last messaged this player.
	 */
	public UUID getLastReceivedMessage() {
		return lastReceivedMessage;
	}

	/**
	 * Updates the UUID of the player who last messaged this player.
	 * @param lastReceivedMessage The UUID of the player who last messaged this player.
	 */
	public void setLastReceivedMessage(UUID lastReceivedMessage) {
		this.lastReceivedMessage = lastReceivedMessage;
	}

	/**
	 * Provides the value tracking if the player is using the spawn boost effects.
	 * @return The value tracking if the player is using the spawn boost effects.
	 */
	public boolean isUsingSpawnBoost() {
		return isUsingSpawnBoost;
	}

	/**
	 * Updates the value tracking if the player is using the spawn boost effects.
	 * @param usingSpawnBoost The value tracking if the player is using the spawn boost effects.
	 */
	public void setUsingSpawnBoost(boolean usingSpawnBoost) {
		isUsingSpawnBoost = usingSpawnBoost;
	}

	/**
	 * Provides the player and the time that combat logged the Aranarth Player.
	 * @return The player and the time that combat logged the Aranarth Player.
	 */
	public HashMap<UUID, Long> getCombatLogTime() {
		return combatLogTime;
	}

	/**
	 * The player and the time that combat logged the Aranarth Player.
	 * @param combatLogTime The player and the time that combat logged the Aranarth Player.
	 */
	public void setCombatLogTime(HashMap<UUID, Long> combatLogTime) {
		this.combatLogTime = combatLogTime;
	}

	/**
	 * Provides the value tracking if the player's inventory assist is disabled.
	 * @return The value tracking if the player's inventory assist is disabled.
	 */
	public boolean isTogglingInventoryAssist() {
		return isTogglingInventoryAssist;
	}

	/**
	 * Updates the value tracking if the player's inventory assist is disabled.
	 * @param togglingInventoryAssist The value tracking if the player's inventory assist is disabled.
	 */
	public void setTogglingInventoryAssist(boolean togglingInventoryAssist) {
		isTogglingInventoryAssist = togglingInventoryAssist;
	}

	/**
	 * Provides the value tracking if the player is automatically locking places chests.
	 * @return The value tracking if the player is automatically locking places chests.
	 */
	public boolean isAutoLockingChests() {
		return isAutoLockingChests;
	}

	/**
	 * Updates the value tracking if the player is automatically locking places chests.
	 * @param autoLockingChests The value tracking if the player is automatically locking places chests.
	 */
	public void setAutoLockingChests(boolean autoLockingChests) {
		isAutoLockingChests = autoLockingChests;
	}

	/**
	 * Provides the date that the player first joined the server.
	 * @return The date that the player first joined the server.
	 */
	public String getFirstJoinDate() {
		return firstJoinDate;
	}

	/**
	 * Updates the date that the player first joined the server.
	 * @param firstJoinDate The date that the player first joined the server.
	 */
	public void setFirstJoinDate(String firstJoinDate) {
		this.firstJoinDate = firstJoinDate;
	}

	/**
	 * Provides the variable tracking whether the player is vanished or not.
	 * @return The variable tracking whether the player is vanished or not.
	 */
	public boolean isVanished() {
		return isVanished;
	}

	/**
	 * Updates the variable tracking whether the player is vanished or not.
	 * @param isVanished The variable tracking whether the player is vanished or not.
	 */
	public void setVanished(boolean isVanished) {
		this.isVanished = isVanished;
	}

	/**
	 * Provides the temporary variable tracking whether the player is sending messages in council chat or not.
	 * @return The temporary variable tracking whether the player is sending messages in council chat or not.
	 */
	public boolean isInCouncilChat() {
		return isInCouncilChat;
	}

	/**
	 * Updates the temporary variable tracking whether the player is sending messages in council chat or not.
	 * @param inCouncilChat The temporary variable tracking whether the player is sending messages in council chat or not.
	 */
	public void setInCouncilChat(boolean inCouncilChat) {
		isInCouncilChat = inCouncilChat;
	}

	/**
	 * Provides the temporary variable tracking whether the player is sending messages in dominion chat or not.
	 * @return The temporary variable tracking whether the player is sending messages in dominion chat or not.
	 */
	public boolean isInDominionChat() {
		return isInDominionChat;
	}

	/**
	 * Updates the temporary variable tracking whether the player is sending messages in dominion chat or not.
	 * @param inDominionChat The temporary variable tracking whether the player is sending messages in dominion chat or not.
	 */
	public void setInDominionChat(boolean inDominionChat) {
		isInDominionChat = inDominionChat;
	}

	/**
	 * Provides the current dominion chat type for the player.
	 * Valid values are: "dominion", "ally", "truce", "allytruce".
	 * @return The dominion chat type.
	 */
	public String getDominionChatType() {
		return dominionChatType;
	}

	/**
	 * Updates the current dominion chat type for the player.
	 * Valid values are: "dominion", "ally", "truce", "allytruce".
	 * @param dominionChatType The new dominion chat type.
	 */
	public void setDominionChatType(String dominionChatType) {
		this.dominionChatType = dominionChatType;
	}

	/**
	 * Provides the temporary variable tracking whether the player is allowed to hurt their own pets or not.
	 * @return Whether the player is allowed to hurt their own pets or not.
	 */
	public boolean isHurtingOwnPets() {
		return isHurtingOwnPets;
	}

	/**
	 * Updates the temporary variable tracking whether the player is allowed to hurt their own pets or not.
	 * @param isHurtingOwnPets Whether the player is allowed to hurt their own pets or not.
	 */
	public void setHurtingOwnPets(boolean isHurtingOwnPets) {
		this.isHurtingOwnPets = isHurtingOwnPets;
	}

	/**
	 * Provides whether the player has gradient chat enabled.
	 * @return Whether gradient chat is enabled.
	 */
	public boolean isGradientChatEnabled() {
		return isGradientChatEnabled;
	}

	/**
	 * Updates whether the player has gradient chat enabled.
	 * @param isGradientChatEnabled Whether gradient chat is enabled.
	 */
	public void setGradientChatEnabled(boolean isGradientChatEnabled) {
		this.isGradientChatEnabled = isGradientChatEnabled;
	}

	/**
	 * Provides the player's saved gradient chat colors as a comma-separated hex string.
	 * @return The gradient chat colors (e.g. "#FF0000,#00FF00,#0000FF"), or empty string if none set.
	 */
	public String getGradientChatColors() {
		return gradientChatColors;
	}

	/**
	 * Updates the player's saved gradient chat colors.
	 * @param gradientChatColors Comma-separated hex color string (e.g. "#FF0000,#00FF00").
	 */
	public void setGradientChatColors(String gradientChatColors) {
		this.gradientChatColors = gradientChatColors;
	}

	/**
	 * Provides whether the player has gradient chat bold mode enabled.
	 * @return Whether gradient chat bold is enabled.
	 */
	public boolean isGradientChatBold() {
		return isGradientChatBold;
	}

	/**
	 * Updates whether the player has gradient chat bold mode enabled.
	 * @param isGradientChatBold Whether gradient chat bold is enabled.
	 */
	public void setGradientChatBold(boolean isGradientChatBold) {
		this.isGradientChatBold = isGradientChatBold;
	}

	/**
	 * Provides the UUID of the player that will become the owner of the next clicked pet.
	 * @return The UUID of the player that will become the owner of the next clicked pet.
	 */
	public UUID getPetTransferUuid() {
		return petTransferUuid;
	}

	/**
	 * Updates the UUID of the player that will become the owner of the next clicked pet.
	 * @param petTransferUuid The UUID of the player that will become the owner of the next clicked pet.
	 */
	public void setPetTransferUuid(UUID petTransferUuid) {
		this.petTransferUuid = petTransferUuid;
	}

	public boolean isDayMessageDisabled() {
		return isDayMessageDisabled;
	}

	public void setDayMessageDisabled(boolean isDayMessageDisabled) {
		this.isDayMessageDisabled = isDayMessageDisabled;
	}

	public boolean isWeatherMessageDisabled() {
		return isWeatherMessageDisabled;
	}

	public void setWeatherMessageDisabled(boolean isWeatherMessageDisabled) {
		this.isWeatherMessageDisabled = isWeatherMessageDisabled;
	}

	public boolean isBulkSellShulkerEnabled() {
		return isBulkSellShulkerEnabled;
	}

	public void setBulkSellShulkerEnabled(boolean isBulkSellShulkerEnabled) {
		this.isBulkSellShulkerEnabled = isBulkSellShulkerEnabled;
	}
}
