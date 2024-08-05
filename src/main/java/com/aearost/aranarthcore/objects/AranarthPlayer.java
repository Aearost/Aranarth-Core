package com.aearost.aranarthcore.objects;

import java.util.List;

import org.bukkit.inventory.ItemStack;


/**
 * Handles all necessary functionality relating to a player on Aranarth.
 */
public class AranarthPlayer {

	private String username;
	private boolean isStandingOnHomePad;
	private int currentGuiPageNum;
	private String nickname;
	private String prefix;
	private boolean isMountSwimEnabled;
	private String survivalInventory;
	private String arenaInventory;
	private String creativeInventory;
	private boolean isDeletingBlacklistedItems;
	private List<ItemStack> potions;
	private List<ItemStack> arrows;
	private List<ItemStack> blacklist;

	public AranarthPlayer(String username) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = "";
		this.prefix = "";
		this.isMountSwimEnabled = false;
		this.survivalInventory = "";
		this.arenaInventory = "";
		this.creativeInventory = "";
		this.potions = null;
		this.arrows = null;
		this.blacklist = null;
		this.isDeletingBlacklistedItems = false;
	}

	public AranarthPlayer(String username, String nickname, String prefix, String survivalInventory, String arenaInventory, String creativeInventory, List<ItemStack> potions, List<ItemStack> arrows, List<ItemStack> blacklist, boolean isDeletingBlacklistedItems) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = nickname;
		this.prefix = prefix;
		this.isMountSwimEnabled = false;
		this.survivalInventory = survivalInventory;
		this.arenaInventory = arenaInventory;
		this.creativeInventory = creativeInventory;
		this.potions = potions;
		this.arrows = arrows;
		this.blacklist = blacklist;
		this.isDeletingBlacklistedItems = isDeletingBlacklistedItems;
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
		return nickname;
	}

	/**
	 * Updates the current nickname.
	 * @param nickname The new nickname.
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * Provides the player's current prefix.
	 * @return The current prefix.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Updates the current prefix.
	 * @param prefix The new prefix.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Provides confirmation whether the player's mount will swim.
	 * @return Confirmation whether the player's mount is swimming.
	 */
	public boolean getIsMountSwimEnabled() {
		return isMountSwimEnabled;
	}

	/**
	 * Updates whether the player's mount will swim.
	 * @param isMountSwimEnabled The new value.
	 */
	public void setIsMountSwimEnabled(boolean isMountSwimEnabled) {
		this.isMountSwimEnabled = isMountSwimEnabled;
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
	public List<ItemStack> getPotions() {
		return potions;
	}

	/**
	 * Updates the player's current List of potions.
	 * @param potions The new List of potions.
	 */
	public void setPotions(List<ItemStack> potions) {
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
	
}
