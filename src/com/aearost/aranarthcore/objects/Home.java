package com.aearost.aranarthcore.objects;

import org.bukkit.Location;

public class Home {

	private String homeName;
	private Location location;
	
	public Home(String homeName, Location location) {
		this.homeName = homeName;
		this.location = location;
	}
	
	public Home(Location location) {
		this.location = location;
		this.homeName = "NEW";
	}
	
	public String getHomeName() {
		return homeName;
	}

	public void setHomeName(String homeName) {
		this.homeName = homeName;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	
}
