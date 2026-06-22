package com.aearost.aranarthcore.abilities.airbending.spiritual;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PastLives extends AvatarAbility implements AddonAbility, MultiAbility {

    public enum AvatarForm {
        WAN("Wan", 0),
        SZETO("Szeto", 1),
        YANGCHEN("Yangchen", 2),
        KURUK("Kuruk", 3),
        KYOSHI("Kyoshi", 4),
        ROKU("Roku", 5),
        AANG("Aang", 6),
        KORRA("Korra", 7);

        private final String displayName;
        private final int hotbarIndex; // 0-indexed

        AvatarForm(String displayName, int hotbarIndex) {
            this.displayName = displayName;
            this.hotbarIndex = hotbarIndex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getHotbarIndex() {
            return hotbarIndex;
        }

        public static AvatarForm fromHotbarIndex(int index) {
            for (AvatarForm form : values()) {
                if (form.hotbarIndex == index) {
                    return form;
                }
            }
            return null;
        }
    }

    private enum State {
        SELECTING,
        ACTIVE,
        CANCELING
    }

    private static final Map<UUID, PastLives> activeInstances = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    @Attribute(Attribute.DURATION)
    private long duration;

    private State state;
    private AvatarForm activeForm;
    private final int pastLivesSlot;

    public PastLives(final Player player) {
        super(player);

        this.pastLivesSlot = player.getInventory().getHeldItemSlot();

        if (!bPlayer.canBend(this)) {
            return;
        }

        if (bPlayer.isAvatarState()) {
            player.sendMessage(ChatUtils.chatMessage("&cYou must exit the " + Element.AVATAR.getColor() + "AvatarState &cto do this"));
            return;
        }

        this.cooldown = 300_000L;    // 5 minutes
        this.duration = 1_800_000L;  // 30 minutes
        this.state = State.SELECTING;

        start();
        activeInstances.put(player.getUniqueId(), this);
        MultiAbilityManager.bindMultiAbility(player, "PastLives");
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            endAbility(false);
            return;
        }

        switch (state) {
            case SELECTING, CANCELING -> { /* Dealt with by listener events */ }
            case ACTIVE    -> handleActive();
        }
    }

    private void handleActive() {
        if (bPlayer.isAvatarState()) {
            player.sendMessage(ChatUtils.chatMessage("&cThe Avatar State disrupts your connection to your past lives!"));
            endAbility(false);
            return;
        }
        if (System.currentTimeMillis() - getStartTime() >= duration) {
            player.sendMessage(ChatUtils.chatMessage("&5Your connection to your past lives has faded"));
            endAbility(true);
            return;
        }
        tickForm();
    }

    public void onSneakPress() {
        if (state == State.ACTIVE) {
            openCancelMenu();
        }
    }

    public void onSneakRelease() {
        if (state == State.SELECTING) {
            remove(); // No cooldown, player changed their mind
        } else if (state == State.CANCELING) {
            closeCancelMenu();
        }
    }

    public boolean onSlotChange(final int newSlot) {
        if (state == State.SELECTING) {
            final AvatarForm chosen = AvatarForm.fromHotbarIndex(newSlot);
            if (chosen != null) {
                activateForm(chosen);
            }
            // Slot 9 - stay in SELECTING
            return true;
        } else if (state == State.CANCELING) {
            if (newSlot == 0) {
                closeCancelMenu(); // "Continue" - slot 1
            } else if (newSlot == 1) {
                endAbility(true);  // "Exit" - slot 2
            }
            // Any other slot is ignored while the cancel menu is open
            return true;
        }
        return false; // ACTIVE - let normal slot-change logic proceed
    }


    private void openCancelMenu() {
        state = State.CANCELING;
        MultiAbilityManager.MultiAbilityInfo info = MultiAbilityManager.getMultiAbility("PastLives");
        if (info != null) {
            ArrayList<MultiAbilityInfoSub> cancelOptions = new ArrayList<>();
            cancelOptions.add(new MultiAbilityInfoSub("Continue", Element.AVATAR));
            cancelOptions.add(new MultiAbilityInfoSub("Exit",     Element.AVATAR));
            info.setAbilities(cancelOptions);
        }
        MultiAbilityManager.bindMultiAbility(player, "PastLives");
        if (info != null) {
            info.setAbilities(buildAvatarList());
        }
    }

    private void closeCancelMenu() {
        state = State.ACTIVE;
        MultiAbilityManager.unbindMultiAbility(player);
    }

    private static ArrayList<MultiAbilityInfoSub> buildAvatarList() {
        ArrayList<MultiAbilityInfoSub> abils = new ArrayList<>();
        abils.add(new MultiAbilityInfoSub("Wan",      Element.FIRE));
        abils.add(new MultiAbilityInfoSub("Szeto",    Element.FIRE));
        abils.add(new MultiAbilityInfoSub("Yangchen", Element.AIR));
        abils.add(new MultiAbilityInfoSub("Kuruk",    Element.WATER));
        abils.add(new MultiAbilityInfoSub("Kyoshi",   Element.EARTH));
        abils.add(new MultiAbilityInfoSub("Roku",     Element.FIRE));
        abils.add(new MultiAbilityInfoSub("Aang",     Element.AIR));
        abils.add(new MultiAbilityInfoSub("Korra",    Element.WATER));
        return abils;
    }

    private void activateForm(final AvatarForm form) {
        this.activeForm = form;
        this.state = State.ACTIVE;
        MultiAbilityManager.unbindMultiAbility(player);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.7f);
        player.sendMessage(ChatUtils.chatMessage("&5You have channeled the spirit of &dAvatar " + form.getDisplayName() + "&5!"));
        // TODO Apply form-specific effects
    }

    private void tickForm() {
        // TODO Per-tick logic for each active form
    }

    public void endAbility(final boolean applyCooldown) {
        if (activeForm != null) {
            player.sendMessage(ChatUtils.chatMessage("&7Your connection to &e" + activeForm.getDisplayName() + " &7has faded"));
            // TODO Remove form-specific effects
        }
        if (applyCooldown) {
            bPlayer.addCooldown(this);
        }
        remove();
    }

    public void endAbilityWithCooldown(final long overrideCooldownMs) {
        if (activeForm != null) {
            player.sendMessage(ChatUtils.chatMessage("&7Your connection to &e" + activeForm.getDisplayName() + " &7has faded"));
            // TODO: Remove form-specific effects
        }
        bPlayer.addCooldown(getName(), overrideCooldownMs);
        remove();
    }

    @Override
    public void remove() {
        MultiAbilityManager.unbindMultiAbility(player);
        activeInstances.remove(player.getUniqueId());
        super.remove();
    }

    public static void endAllInstances() {
        new ArrayList<>(activeInstances.values()).forEach(pl -> pl.endAbility(false));
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return activeInstances.containsKey(uuid);
    }

    public static PastLives getActiveInstance(final UUID uuid) {
        return activeInstances.get(uuid);
    }

    @Override
    public ArrayList<MultiAbilityInfoSub> getMultiAbilities() {
        return buildAvatarList();
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public String getName() {
        return "PastLives";
    }

    @Override
    public void load() {
    }

    @Override
    public void stop() {
        if (player != null) {
            MultiAbilityManager.unbindMultiAbility(player);
            activeInstances.remove(player.getUniqueId());
        }
    }

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Channel the spirit of one of your past lives, and channel that Avatar's power for an extensive period of time.\n" +
                ChatUtils.translateToColor("&fTo select: Hold Sneak > Select Slot (1-8)\n") +
                ChatUtils.translateToColor("&fTo exit: Hold Sneak > Select Slot (1-2)\n") +
                ChatUtils.translateToColor("&fAvatars:\n") +
                ChatUtils.translateToColor("  &f1. Wan - XXX") +
                ChatUtils.translateToColor("  &f2. Szeto - XXX") +
                ChatUtils.translateToColor("  &f3. Yangchen - XXX") +
                ChatUtils.translateToColor("  &f4. Kuruk - XXX") +
                ChatUtils.translateToColor("  &f5. Kyoshi - XXX") +
                ChatUtils.translateToColor("  &f6. Roku - XXX") +
                ChatUtils.translateToColor("  &f7. Aang - XXX") +
                ChatUtils.translateToColor("  &f8. Korra - XXX");
    }
}
