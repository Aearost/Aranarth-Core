package com.aearost.aranarthcore.objects;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to an avatar on Aranarth.
 */
public class Avatar {
	private UUID uuid;
	private LocalDateTime start;
	private LocalDateTime end;
	private String element;

	public Avatar(UUID uuid, LocalDateTime start, LocalDateTime end, String element) {
		this.uuid = uuid;
		this.start = start;
		this.end = end;
		this.element = element;
	}

	/**
	 * Provides the punished player's UUID.
	 * @return the punished player's UUID.
	 */
	public UUID getUuid() {
		return uuid;
	}

	/**
	 * Updates the punished player's UUID.
	 * @param uuid The punished player's UUID.
     */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * Provides the start date of the avatar's reign.
	 */
	public LocalDateTime getStart() {
		return start;
	}

	/**
	 * Updates the start of the avatar's reign.
	 * @param start The start date of the avatar's reign.
	 */
	public void setStart(LocalDateTime start) {
		this.start = start;
	}

	/**
	 * Provides the end date of the avatar's reign.
	 */
	public LocalDateTime getEnd() {
		return end;
	}

	/**
	 * Updates the end of the avatar's reign.
	 * @param end The end date of the avatar's reign.
	 */
	public void setEnd(LocalDateTime end) {
		this.end = end;
	}

	/**
	 * Provides the reason of the punishment.
	 * @return The reason of the punishment.
	 */
	public String getElement() {
		return element;
	}

	/**
	 * Updates the original element of the avatar.
	 * @param element The original element of the avatar.
	 */
	public void setElement(String element) {
		this.element = element;
	}

}
