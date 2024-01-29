package com.aearost.aranarthcore.objects;

import org.bukkit.inventory.Inventory;

public class AranarthPlayer {

	private String username;
	private boolean isStandingOnHomePad;
	private int currentGuiPageNum;
	private String nickname;
	private String prefix;
	private boolean isMountSwimEnabled;
	private Inventory survivalInventory;
	private Inventory creativeInventory;

	public AranarthPlayer(String username) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = "";
		this.prefix = "";
		this.isMountSwimEnabled = false;
		this.survivalInventory = null;
		this.creativeInventory = null;
	}
	
	public AranarthPlayer(String username, String nickname, String prefix) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = nickname;
		this.prefix = prefix;
		this.isMountSwimEnabled = false;
		this.survivalInventory = null;
		this.creativeInventory = null;
	}
	
	public AranarthPlayer(String username, String nickname, String prefix, Inventory survivalInventory, Inventory creativeInventory) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 0;
		this.nickname = nickname;
		this.prefix = prefix;
		this.isMountSwimEnabled = false;
		this.survivalInventory = survivalInventory;
		this.creativeInventory = creativeInventory;
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
	
	public Inventory getSurvivalInventory() {
		return survivalInventory;
	}
	
	public void setSurvivalInventory(Inventory survivalInventory) {
		this.survivalInventory = survivalInventory;
	}
	
	public Inventory getCreativeInventory() {
		return creativeInventory;
	}
	
	public void setCreativeInventory(Inventory creativeInventory) {
		this.creativeInventory = creativeInventory;
	}
	
}
