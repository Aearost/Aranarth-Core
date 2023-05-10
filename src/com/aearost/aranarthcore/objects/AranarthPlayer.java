package com.aearost.aranarthcore.objects;

public class AranarthPlayer {

	private String username;
	private boolean isStandingOnHomePad;
	private int currentGuiPageNum;
	private String nickname;
	private String prefix;
	private boolean isHorseSwimEnabled;
	

	public AranarthPlayer(String username) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 1;
		this.nickname = "";
		this.prefix = "";
		this.isHorseSwimEnabled = false;
	}
	
	public AranarthPlayer(String username, String nickname, String prefix) {
		this.username = username;
		this.isStandingOnHomePad = false;
		this.currentGuiPageNum = 1;
		this.nickname = nickname;
		this.prefix = prefix;
		this.isHorseSwimEnabled = false;
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
	
	public boolean getIsHorseSwimEnabled() {
		return isHorseSwimEnabled;
	}
	
	public void setIsHorseSwimEnabled(boolean isHorseSwimEnabled) {
		this.isHorseSwimEnabled = isHorseSwimEnabled;
	}
	
}
