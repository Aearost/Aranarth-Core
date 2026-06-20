package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.objects.Mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Manages in-memory storage for player mail.
 */
public class MailUtils {

    private static final HashMap<UUID, List<Mail>> mailbox = new HashMap<>();

    public static List<Mail> getMail(UUID recipientUUID) {
        return mailbox.getOrDefault(recipientUUID, new ArrayList<>());
    }

    public static void addMail(UUID recipientUUID, Mail mail) {
        mailbox.computeIfAbsent(recipientUUID, k -> new ArrayList<>()).add(mail);
    }

    public static HashMap<UUID, List<Mail>> getAllMail() {
        return mailbox;
    }

    public static void setAllMail(HashMap<UUID, List<Mail>> data) {
        mailbox.clear();
        mailbox.putAll(data);
    }

    /**
     * Removes a single mail by its index in the internal (chronological) list.
     */
    public static void removeMail(UUID recipientUUID, int index) {
        List<Mail> mail = mailbox.get(recipientUUID);
        if (mail != null && index >= 0 && index < mail.size()) {
            mail.remove(index);
        }
    }

    public static void clearMail(UUID recipientUUID) {
        mailbox.remove(recipientUUID);
    }
}
