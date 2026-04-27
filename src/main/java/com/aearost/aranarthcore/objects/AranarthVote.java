package com.aearost.aranarthcore.objects;

import java.util.UUID;

/**
 * Handles all necessary functionality relating to an Aranarth Vote.
 */
public class AranarthVote {

    private UUID uuid;
    private int pointsRewarded;
    private long timestamp;

    public AranarthVote(UUID uuid, int pointsRewarded, long timestamp) {
        this.uuid = uuid;
        this.pointsRewarded = pointsRewarded;
        this.timestamp = timestamp;
    }

    /**
     * Provides the player's UUID.
     * @return The player's UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Sets the player's UUID.
     * @param uuid The player's UUID.
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Provides the number of points rewarded in the vote.
     * @return The number of points rewarded in the vote.
     */
    public int getPointsRewarded() {
        return pointsRewarded;
    }

    /**
     * Updates the number of points rewarded in the vote.
     * @param pointsRewarded The number of points rewarded in the vote.
     */
    public void setPointsRewarded(int pointsRewarded) {
        this.pointsRewarded = pointsRewarded;
    }

    /**
     * Provides the timestamp of the vote.
     * @return The timestamp of the vote.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Updates the timestamp of the vote.
     * @param timestamp Provides the timestamp of the vote.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
