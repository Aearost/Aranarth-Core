package com.aearost.aranarthcore.objects;

import java.util.UUID;

/**
 * Represents a piece of mail sent from one player to another.
 */
public class Mail {

    private final UUID senderUUID;
    private final UUID receiverUUID;
    private final long timestamp;
    private final String message;

    public Mail(UUID senderUUID, UUID receiverUUID, long timestamp, String message) {
        this.senderUUID = senderUUID;
        this.receiverUUID = receiverUUID;
        this.timestamp = timestamp;
        this.message = message;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public UUID getReceiverUUID() {
        return receiverUUID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
