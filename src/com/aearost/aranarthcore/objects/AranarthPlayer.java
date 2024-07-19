package com.aearost.aranarthcore.objects;

import java.util.List;

import org.bukkit.inventory.ItemStack;

public class AranarthPlayer {

	private String username;
	private boolean isStandingOnHomePad;
	private int currentGuiPageNum;
	private String nickname;
	private String prefix;
	private boolean isMountSwimEnabled;
	private String survivalInventory;
	private String creativeInventory;
	private boolean isDeletingBlacklistedItems;
	private List<ItemStack> potions;
	private List<ItemStack> arrows;

	public AranarthPlayer(String username) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = "";
		this.prefix = "";
		this.isMountSwimEnabled = false;
		this.survivalInventory = "";
		this.creativeInventory = "";
		this.isDeletingBlacklistedItems = false;
		this.potions = null;
		this.arrows = null;
	}

	public AranarthPlayer(String username, String nickname, String prefix, String survivalInventory, String creativeInventory, List<ItemStack> potions, List<ItemStack> arrows) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = nickname;
		this.prefix = prefix;
		this.isMountSwimEnabled = false;
		this.survivalInventory = survivalInventory;
		this.creativeInventory = creativeInventory;
		this.isDeletingBlacklistedItems = false;
		this.potions = potions;
		this.arrows = arrows;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public boolean getIsStandingOnHomePad() {
		return isStandingOnHomePad;
	}
	
	public void setIsStandingOnHomePad(boolean isStandingOnHomePad) {
		this.isStandingOnHomePad = isStandingOnHomePad;
	}
	
	public int getCurrentGuiPageNum() {
		return currentGuiPageNum;
	}
	
	public void setCurrentGuiPageNum(int currentGuiPageNum) {
		this.currentGuiPageNum = currentGuiPageNum;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public boolean getIsMountSwimEnabled() {
		return isMountSwimEnabled;
	}
	
	public void setIsMountSwimEnabled(boolean isMountSwimEnabled) {
		this.isMountSwimEnabled = isMountSwimEnabled;
	}
	
	public String getSurvivalInventory() {
		return survivalInventory;
	}
	
	public void setSurvivalInventory(String survivalInventory) {
		this.survivalInventory = survivalInventory;
	}
	
	public String getCreativeInventory() {
		return creativeInventory;
	}
	
	public void setCreativeInventory(String creativeInventory) {
		this.creativeInventory = creativeInventory;
	}
	
	public boolean getIsDeletingBlacklistedItems() {
		return isDeletingBlacklistedItems;
	}
	
	public void setIsDeletingBlacklistedItems(boolean isDeletingBlacklistedItems) {
		this.isDeletingBlacklistedItems = isDeletingBlacklistedItems;
	}
	
	public List<ItemStack> getPotions() {
		return potions;
	}
	
	public void setPotions(List<ItemStack> potions) {
		this.potions = potions;
	}
	
	public List<ItemStack> getArrows() {
		return arrows;
	}
	
	public void setArrows(List<ItemStack> arrows) {
		this.arrows = arrows;
	}
	
}
