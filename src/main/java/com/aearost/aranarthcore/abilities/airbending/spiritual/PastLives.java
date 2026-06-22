package com.aearost.aranarthcore.abilities.airbending.spiritual;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    private static final double WAN_DAMAGE_BONUS = 0.25;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    @Attribute(Attribute.DURATION)
    private long duration;

    private State state;
    private AvatarForm activeForm;
    private final int pastLivesSlot;
    private PotionEffect savedHealthBoostEffect = null; // Kyoshi: original Health Boost to restore on form end

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
        // Move off slot 0 so the player can press key 1 to select Wan (slot 0).
        // bindMultiAbility forces slot 0, which means pressing key 1 again fires no event.
        player.getInventory().setHeldItemSlot(8);
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
        // Move off slot 0 so both "Continue" (slot 0) and "Exit" (slot 1) are reachable
        player.getInventory().setHeldItemSlot(2);
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
        switch (form) {
            case WAN    -> activateWan();
            case KYOSHI -> activateKyoshi();
            case KORRA  -> activateKorra();
            default     -> {}
        }
    }

    private void tickForm() {
        if (activeForm == AvatarForm.WAN) {
            tickWan();
        }
        // TODO Per-tick logic for each other active form
    }

    public void endAbility(final boolean applyCooldown) {
        if (activeForm != null) {
            player.sendMessage(ChatUtils.chatMessage("&5Your connection to &dAvatar " + activeForm.getDisplayName() + " &5has faded"));
            removeFormEffects();
        }
        if (applyCooldown) {
            bPlayer.addCooldown(this);
        }
        remove();
    }

    public void endAbilityWithCooldown(final long overrideCooldownMs) {
        if (activeForm != null) {
            player.sendMessage(ChatUtils.chatMessage("&5Your connection to &dAvatar " + activeForm.getDisplayName() + " &5has faded"));
            removeFormEffects();
        }
        bPlayer.addCooldown(getName(), overrideCooldownMs);
        remove();
    }

    private void removeFormEffects() {
        if (activeForm == null) return;
        switch (activeForm) {
            case WAN    -> player.removePotionEffect(PotionEffectType.STRENGTH);
            case KYOSHI -> {
                player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
                if (savedHealthBoostEffect != null) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.HEALTH_BOOST,
                            savedHealthBoostEffect.getDuration(),
                            savedHealthBoostEffect.getAmplifier(),
                            savedHealthBoostEffect.isAmbient(),
                            savedHealthBoostEffect.hasParticles(),
                            savedHealthBoostEffect.hasIcon()
                    ));
                    savedHealthBoostEffect = null;
                }
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
            case KORRA  -> {
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.removePotionEffect(PotionEffectType.SPEED);
            }
            default     -> {}
        }
    }

    private void activateWan() {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 2, false, true, true));
    }

    private void activateKyoshi() {
        savedHealthBoostEffect = player.getPotionEffect(PotionEffectType.HEALTH_BOOST);
        int baseAmplifier = savedHealthBoostEffect != null ? savedHealthBoostEffect.getAmplifier() : -1;

        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, baseAmplifier + 5, false, true, true));

        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 3, false, true, true));
    }

    private void activateKorra() {
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 4, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 4, false, true, true));
    }

    private void tickWan() {
        World world = player.getWorld();
        // Use getDirection() for an accurate forward vector (yaw-only, ignore pitch for positioning)
        Location eyeLoc = player.getEyeLocation();
        Vector forward = new Vector(-Math.sin(Math.toRadians(eyeLoc.getYaw())), 0,
                Math.cos(Math.toRadians(eyeLoc.getYaw()))).normalize();
        // Right vector: perpendicular to forward in the horizontal plane
        Vector right = new Vector(forward.getZ(), 0, -forward.getX());

        // Chest center: just touching the front of the player model
        Location chest = player.getLocation().clone()
                .add(forward.getX() * 0.3, 1.0, forward.getZ() * 0.1);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE, 1.2f);

        // Oval body: Raava's teardrop shape centered on the chest
        for (int i = 0; i < 10; i++) {
            double angle = 2 * Math.PI * i / 10;
            double rightOffset = Math.cos(angle) * 0.12;
            double upOffset = Math.sin(angle) * 0.22;
            Location particleLoc = chest.clone()
                    .add(right.getX() * rightOffset, upOffset, right.getZ() * rightOffset);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }

        // Trailing tendrils below the oval, animated
        for (int i = 0; i < 3; i++) {
            double phase = (System.currentTimeMillis() / 300.0) + i * (2 * Math.PI / 3);
            double wave = Math.sin(phase) * 0.06;
            double sideOffset = (i - 1) * 0.08;
            double downOffset = 0.18 + i * 0.12;
            Location tendrilLoc = chest.clone()
                    .add(right.getX() * (sideOffset + wave), -downOffset, right.getZ() * (sideOffset + wave));
            world.spawnParticle(Particle.DUST, tendrilLoc, 1, 0, 0, 0, 0, dustOptions);
        }
    }

    public void onPlayerMeleeHit(EntityDamageByEntityEvent e) {
        if (activeForm != AvatarForm.WAN) return;
        e.setDamage(e.getDamage() * 1.5);
        if (e.getEntity() instanceof LivingEntity target) {
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.0f);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 8, 0.2, 0.4, 0.2, 0, dustOptions);
        }
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
                ChatUtils.translateToColor("  &f1. Wan - Channels Raava's spirit, granting strength and dealing bonus damage to those you strike\n") +
                ChatUtils.translateToColor("  &f2. Szeto - Heightens all of your active beneficial effects, pushing them beyond their normal limits\n") +
                ChatUtils.translateToColor("  &f3. Yangchen - Purifies all harmful effects on you, turning them into their positive counterparts\n") +
                ChatUtils.translateToColor("  &f4. Kuruk - Builds power with every consecutive hit, but becomes a blur of speed when close to death\n") +
                ChatUtils.translateToColor("  &f5. Kyoshi - Fortifies the body with an extra row of hearts that vanish when the form ends\n") +
                ChatUtils.translateToColor("  &f6. Roku - Calls upon Fang, soaring through the skies at ever-growing speed\n") +
                ChatUtils.translateToColor("  &f7. Aang - Runs faster the longer you sprint, and reduces the wait on all your other abilities\n") +
                ChatUtils.translateToColor("  &f8. Korra - Overwhelms enemies with raw strength and blazing speed");
    }
}
