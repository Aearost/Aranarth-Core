package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages in-memory storage for player mail.
 */
public class MailUtils {

    private static final HashMap<UUID, List<Mail>> mailbox = new HashMap<>();
    // UUIDs whose mail was modified by a local event (send/remove/clear) on this server.
    // Only these are written back to the shared MySQL to avoid overwriting updates made by
    // another server in the network with stale data loaded at startup.
    private static final Set<UUID> locallyModifiedMail = new HashSet<>();

    public static List<Mail> getMail(UUID recipientUUID) {
        return mailbox.getOrDefault(recipientUUID, new ArrayList<>());
    }

    public static void addMail(UUID recipientUUID, Mail mail) {
        mailbox.computeIfAbsent(recipientUUID, k -> new ArrayList<>()).add(mail);
        locallyModifiedMail.add(recipientUUID);
    }

    public static HashMap<UUID, List<Mail>> getAllMail() {
        return mailbox;
    }

    public static void setAllMail(HashMap<UUID, List<Mail>> data) {
        mailbox.clear();
        mailbox.putAll(data);
    }

    public static Set<UUID> getLocallyModifiedMailUuids() {
        return locallyModifiedMail;
    }

    /**
     * Sets a player's mail list without marking it as locally modified.
     * Use this only for reload-from-database operations so the reloaded data is not
     * written back to MySQL again on the next periodic save.
     */
    public static void setMailForPlayer(UUID uuid, List<Mail> mail) {
        mailbox.put(uuid, mail);
    }

    /**
     * Removes a single mail by its index in the internal (chronological) list.
     */
    public static void removeMail(UUID recipientUUID, int index) {
        List<Mail> mail = mailbox.get(recipientUUID);
        if (mail != null && index >= 0 && index < mail.size()) {
            mail.remove(index);
            locallyModifiedMail.add(recipientUUID);
        }
    }

    public static void clearMail(UUID recipientUUID) {
        mailbox.put(recipientUUID, new ArrayList<>());
        locallyModifiedMail.add(recipientUUID);
    }
}
