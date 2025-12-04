package com.aearost.aranarthcore.objects;

import com.aearost.aranarthcore.enums.Pronouns;
import org.bukkit.Location;
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
	private boolean isDeletingBlacklistedItems;
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
	private String perks;
	private boolean isInAdminMode = false;
	private long saintExpireDate;
	private boolean isCompressingItems = false;
	private int bulkTransactionNum = 0;

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
		this.isDeletingBlacklistedItems = false;
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
		this.perks = "0_0_0_0_0_0_0_0_0_0_0_0";
		this.isInAdminMode = false;
		this.saintExpireDate = 0;
		this.isCompressingItems = false;
		this.bulkTransactionNum = 0;
	}

	public AranarthPlayer(String username, String nickname, String survivalInventory, String arenaInventory,
						  String creativeInventory, HashMap<ItemStack, Integer> potions, List<ItemStack> arrows,
						  List<ItemStack> blacklist, boolean isDeletingBlacklistedItems, double balance, int rank,
						  int saintRank, int councilRank, int architectRank, List<Home> homes, String muteEndDate,
						  int particleNum, String perks, long saintExpireDate, boolean isCompressingItems,
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
		this.isDeletingBlacklistedItems = isDeletingBlacklistedItems;
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
	public boolean getIsStandingOnHomePad() {
		return isStandingOnHomePad;
	}

	/**
	 * Updates whether the player is on a homepad.
	 * @param isStandingOnHomePad The new value.
	 */
	public void setIsStandingOnHomePad(boolean isStandingOnHomePad) {
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
		return (nickname == null || nickname.isEmpty()) ? username : nickname;
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
	 * Provides confirmation whether the player is deleting blacklisted items or not.
	 * @return Confirmation of whether they are deleting the items or not.
	 */
	public boolean getIsDeletingBlacklistedItems() {
		return isDeletingBlacklistedItems;
	}

	/**
	 * Updates whether the player will be deleting blacklisted items or not.
	 * @param isDeletingBlacklistedItems The new value.
	 */
	public void setIsDeletingBlacklistedItems(boolean isDeletingBlacklistedItems) {
		this.isDeletingBlacklistedItems = isDeletingBlacklistedItems;
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
	public boolean getIsHitByTippedArrow() {
		return isHitByTippedArrow;
	}

	/**
	 * Updates the player's current value confirming if they were hit by a tipped arrow.
	 * @param isHitByTippedArrow The new confirmation of if they were hit by a tipped arrow.
	 */
	public void setIsHitByTippedArrow(boolean isHitByTippedArrow) {
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
	public boolean getIsAddingToShulker() {
		return isAddingToShulker;
	}

	/**
	 * Setting the temporary variable tracking whether the player is adding to their shulker box.
	 * @param isAddingToShulker Whether the player is adding to their shulker box.
	 */
	public void setIsAddingToShulker(boolean isAddingToShulker) {
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
	public boolean getIsRandomizing() {
		return isRandomizing;
	}

	/**
	 * Setting the temporary variable tracking whether the player is randomizing items.
	 * @param isRandomizing Whether the player is randomizing items.
	 */
	public void setIsRandomizing(boolean isRandomizing) {
		this.isRandomizing = isRandomizing;
	}

	/**
	 * Temporary variable tracking if the player is missing items from the randomizer pattern.
	 * @return Whether the player is missing items from the randomizer pattern.
	 */
	public boolean getIsMissingItemMessageSent() {
		return isMissingItemMessageSent;
	}

	/**
	 * Setting the temporary variable tracking whether the player is missing items from the randomizer pattern.
	 * @param isMissingItemMessageSent Whether the player is missing items from the randomizer pattern.
	 */
	public void setIsMissingItemMessageSent(boolean isMissingItemMessageSent) {
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
	 * Provides the temporary amount of the potion to be removed from the /ac potions remove command.
	 * @return The quantity to be removed.
	 */
	public int getPotionQuantityToRemove() {
		return potionQuantityToRemove;
	}

	/**
	 * Updates the temporary amount of the potion to be removed from the /ac potions remove command.
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
	public boolean getIsUnlockingContainer() {
		return isUnlockingContainer;
	}

	/**
	 * Updates the temporary variable tracking whether the player is attempting to unlock a container.
	 * @param isUnlockingContainer The temporary variable of whether the player is attempting to unlock a container.
	 */
	public void setIsUnlockingContainer(boolean isUnlockingContainer) {
		this.isUnlockingContainer = isUnlockingContainer;
	}

	/**
	 * Provides the temporary variable tracking whether the player is attempting to lock a container.
	 * @return Whether the player is attempting to lock a container.
	 */
	public boolean getIsLockingContainer() {
		return isLockingContainer;
	}

	/**
	 * Updates the temporary variable tracking whether the player is attempting to lock a container.
	 * @param isLockingContainer The temporary variable of whether the player is attempting to lock a container.
	 */
	public void setIsLockingContainer(boolean isLockingContainer) {
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
	public void setIsAddingPotions(boolean isAddingPotions) {
		this.isAddingPotions = isAddingPotions;
	}

	/**
	 * Provides the perks that the player has access to.
	 * Perks will be split as: compressor_randomizer_blacklist_tables_itemname_chat_shulker_inventory_homes_itemframe_bluefire_discord
	 * @return The perks that the player has access to.
	 */
	public String getPerks() {
		return perks;
	}

	/**
	 * Updates the perks that the player has access to.
	 * compressor_randomizer_blacklist_tables_itemname_chat_shulker_inventory_homes_itemframe_bluefire_discord
	 * @param perks The player's new perks.
	 */
	public void setPerks(String perks) {
		this.perks = perks;
	}

	/**
	 * Provides the variable tracking whether the player is in admin mode.
	 * @return Whether the player is in admin mode.
	 */
	public boolean getIsInAdminMode() {
		return isInAdminMode;
	}

	/**
	 * Updates the variable tracking whether the player is in admin mode.
	 * @param isInAdminMode Whether the player is in admin mode.
	 */
	public void setIsInAdminMode(boolean isInAdminMode) {
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
	public boolean getIsCompressingItems() {
		return isCompressingItems;
	}

	/**
	 * Updates the variable tracking whether the player is compressing items or not.
	 * @param isCompressingItems Whether the player is compressing items or not.
	 */
	public void setIsCompressingItems(boolean isCompressingItems) {
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
	 * Updates the variable tracking which point of the bulk transaction the player is currently making
	 * @param bulkTransactionNum Which point of the bulk transaction the player is currently making
	 */
	public void setBulkTransactionNum(int bulkTransactionNum) {
		this.bulkTransactionNum = bulkTransactionNum;
	}

}
