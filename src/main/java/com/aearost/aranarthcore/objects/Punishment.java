package com.aearost.aranarthcore.objects;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles all necessary functionality relating to a dominion on Aranarth.
 */
public class Punishment {
	private UUID uuid;
	private LocalDateTime date;
	private String type;
	private String reason;
	private UUID appliedBy;

	public Punishment(UUID uuid, LocalDateTime date, String type, String reason, UUID appliedBy) {
		this.uuid = uuid;
		this.date = date;
		this.type = type;
		this.reason = reason;
		this.appliedBy = appliedBy;
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
	 * Updates the date of the punishment.
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * Updates the date of the punishment.
	 * @param date The date of the punishment.
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	/**
	 * Provides the type of the punishment.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Updates the type of the punishment.
	 * @param type The type of the punishment.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Provides the reason of the punishment.
	 * @return The reason of the punishment.
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Updates the reason of the punishment.
	 * @param reason The reason of the punishment.
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Provides the UUID of the council member that applied the punishment.
	 */
	public UUID getAppliedBy() {
		return appliedBy;
	}

	/**
	 * Updates the UUID of the council member that applied the punishment.
	 * @param appliedBy The UUID of the council member that applied the punishment.
	 */
	public void setAppliedBy(UUID appliedBy) {
		this.appliedBy = appliedBy;
	}

}
