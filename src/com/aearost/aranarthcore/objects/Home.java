package com.aearost.aranarthcore.objects;

import org.bukkit.Location;
import org.bukkit.Material;

public class Home {

	private String homeName;
	private Location location;
	private Material icon;
	
	public Home(String homeName, Location location, Material icon) {
		this.homeName = homeName;
		this.location = location;
		this.icon = icon;
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
	
	public Material getIcon() {
		return icon;
	}
	
	public void setIcon(Material icon) {
		this.icon = icon;
	}
	
}
