package com.aearost.aranarthcore.objects;

import org.bukkit.entity.Ravager;
import org.bukkit.entity.Sniffer;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Tracks ownership and stat data for any Aranarth-managed mount.
 */
public class AranarthMount {

    private UUID ownerUUID;
    private final double speed;
    private final Double thirdAttribute;
    private final String thirdAttributeLabel;

    public AranarthMount(@Nullable UUID ownerUUID, double speed,
                         @Nullable Double thirdAttribute, @Nullable String thirdAttributeLabel) {
        this.ownerUUID = ownerUUID;
        this.speed = speed;
        this.thirdAttribute = thirdAttribute;
        this.thirdAttributeLabel = thirdAttributeLabel;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public double getSpeed() {
        return speed;
    }

    /**
     * Mob-specific third attribute.
     */
    @Nullable
    public Double getThirdAttribute() {
        return thirdAttribute;
    }

    /**
     * Human-readable label for the third attribute.
     */
    @Nullable
    public String getThirdAttributeLabel() {
        return thirdAttributeLabel;
    }

    /**
     * Converts Sniffer movement speed (blocks/tick) to blocks per second.
     */
    public double getSnifferSpeedMetersPerSecond() {
        return speed * 20.0;
    }

    public double getDigSpeedBlocksPerSecond() {
        if (thirdAttribute == null) {
            return 0;
        }
        return thirdAttribute * 20.0 * 0.5 / 252.0;
    }

    public double getJumpHeightBlocks() {
        if (thirdAttribute == null) {
            return 0;
        }
        double s = thirdAttribute;
        return -0.1817584952 * Math.pow(s, 3)
                + 3.689713992 * Math.pow(s, 2)
                + 2.128599134 * s
                - 0.343930367;
    }

    /**
     * Constructs an {@code AranarthMount} from a Ravager's persistent data.
     * Returns {@code null} if the entity has no tracked mount speed.
     */
    @Nullable
    public static AranarthMount fromRavager(Ravager ravager) {
        PersistentDataContainer pdc = ravager.getPersistentDataContainer();

        if (!pdc.has(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE)) {
            return null;
        }

        UUID ownerUUID = null;
        if (pdc.has(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING)) {
            try {
                ownerUUID = UUID.fromString(pdc.get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }

        double speed = pdc.get(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE);
        Double thirdAttr = pdc.has(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE)
                ? pdc.get(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE) : null;

        return new AranarthMount(ownerUUID, speed, thirdAttr, thirdAttr != null ? "Ram Damage" : null);
    }

    /**
     * Constructs an {@code AranarthMount} from a Sniffer's persistent data.
     * Returns {@code null} if the entity has no tracked mount speed.
     */
    @Nullable
    public static AranarthMount fromSniffer(Sniffer sniffer) {
        PersistentDataContainer pdc = sniffer.getPersistentDataContainer();

        if (!pdc.has(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE)) {
            return null;
        }

        UUID ownerUUID = null;
        if (pdc.has(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING)) {
            try {
                ownerUUID = UUID.fromString(pdc.get(CustomKeys.MOUNT_OWNER, PersistentDataType.STRING));
            } catch (IllegalArgumentException ignored) {
            }
        }

        double speed = pdc.get(CustomKeys.MOUNT_SPEED, PersistentDataType.DOUBLE);
        Double thirdAttr = pdc.has(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE)
                ? pdc.get(CustomKeys.MOUNT_THIRD_ATTR, PersistentDataType.DOUBLE) : null;

        return new AranarthMount(ownerUUID, speed, thirdAttr, thirdAttr != null ? "Dig Speed" : null);
    }
}
