package com.aearost.aranarthcore.objects;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to a dominion on Aranarth.
 */
public class Dominion {

	private UUID owner;
	private List<UUID> trusted;
	private List<Chunk> chunks;
	private int dominionPower;
	private Location dominionHome;

	public Dominion(UUID owner, List<UUID> trusted, List<Chunk> chunks, int dominionPower, Location dominionHome) {
		this.owner = owner;
		this.trusted = trusted;
		this.chunks = chunks;
		this.dominionPower = dominionPower;
		this.dominionHome = dominionHome;
	}

	/**
	 * Provides the dominion's owner's UUID.
	 * @return the dominion's owner's UUID.
	 */
	public UUID getOwner() {
		return owner;
	}

	/**
	 * Updates the dominion's owner's UUID.
	 * @param owner The dominion's owner's UUID.
     */
	public void setOwner(UUID owner) {
		this.owner = owner;
	}

	/**
	 * Provides the list of players that are trusted to open the dominion.
	 * @return the list of players that are trusted to open the dominion.
	 */
	public List<UUID> getTrusted() {
		return this.trusted;
	}

	/**
	 * Updates the list of players that are trusted to open the dominion.
	 * @param trusted The list of players that will be trusted to open the dominion.
	 */
	public void setTrusted(List<UUID> trusted) {
		this.trusted = trusted;
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
	 * Provides the current power of the dominion.
	 * @return The current power of the dominion.
	 */
	public int getDominionPower() {
		return dominionPower;
	}

	/**
	 * Updates the current power of the dominion.
	 * @param dominionPower The new value of the current power of the dominion.
	 */
	public void setDominionPower(int dominionPower) {
		this.dominionPower = dominionPower;
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

//	@Override
//	public String toString() {
//		String divider = "---------------\n";
//		String owner = "OWNER: " + Bukkit.getOfflinePlayer(getOwner()).getName() + " (" + getOwner() + ")";
//		StringBuilder trustedSB = new StringBuilder();
//		trustedSB.append("TRUSTED: ");
//		for (int i = 0; i < getTrusted().size(); i++) {
//			if (i == getTrusted().size() - 1) {
//				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(")");
//			} else {
//				trustedSB.append(Bukkit.getOfflinePlayer(getTrusted().get(i)).getName()).append(" (").append(getTrusted().get(i)).append(") | ");
//			}
//		}
//		String trusted = trustedSB.toString();
//		String x1 = "x1: " + locations[0].getBlockX() + " | ";
//		String y1 = "y1: " + locations[0].getBlockY() + " | ";
//		String z1 = "z1: " + locations[0].getBlockZ();
//		String x2 = "";
//		String y2 = "";
//		String z2 = "";
//		if (locations[1] != null) {
//			x2 = "x2: " + locations[1].getBlockX() + " | ";
//			y2 = "y2: " + locations[1].getBlockY() + " | ";
//			z2 = "z2: " + locations[1].getBlockZ();
//		}
//		String loc1 = "LOC 1: " + x1 + y1 + z1;
//		String loc2 = "LOC 2: " + x2 + y2 + z2;
//		if (loc2.isEmpty()) {
//			return divider + owner + "\n" + trusted + "\n" + loc1;
//		} else {
//			return divider + owner + "\n" + trusted + "\n" + loc1 + "\n" + loc2;
//		}
//	}

}
