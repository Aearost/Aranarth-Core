package com.aearost.aranarthcore.abilities.airbending.spiritual;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.listener.misc.PotionEffectListener;
import com.aearost.aranarthcore.event.mob.MountListener;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AvatarAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private static final Map<UUID, PastLives> fangInstances = new HashMap<>();

    private static final double WAN_DAMAGE_BONUS = 0.25;

    // Szeto - beneficial effects are eligible for the +1 amplifier boost
    private static final Set<PotionEffectType> SZETO_POSITIVE_EFFECTS = Set.of(
            PotionEffectType.SPEED, PotionEffectType.HASTE, PotionEffectType.STRENGTH,
            PotionEffectType.INSTANT_HEALTH, PotionEffectType.JUMP_BOOST,
            PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING,
            PotionEffectType.INVISIBILITY, PotionEffectType.NIGHT_VISION,
            PotionEffectType.ABSORPTION, PotionEffectType.SATURATION,
            PotionEffectType.LUCK, PotionEffectType.HERO_OF_THE_VILLAGE,
            PotionEffectType.SLOW_FALLING, PotionEffectType.CONDUIT_POWER,
            PotionEffectType.DOLPHINS_GRACE, PotionEffectType.HEALTH_BOOST
    );

    // Yangchen - negative effects switch to their positive counterparts, unmapped ones are simply cleansed
    private static final Map<PotionEffectType, PotionEffectType> YANGCHEN_CONVERSIONS = Map.of(
            PotionEffectType.POISON,   PotionEffectType.REGENERATION,
            PotionEffectType.SLOWNESS, PotionEffectType.SPEED,
            PotionEffectType.WEAKNESS, PotionEffectType.STRENGTH,
            PotionEffectType.BLINDNESS, PotionEffectType.NIGHT_VISION
    );

    private static final Set<PotionEffectType> YANGCHEN_NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.POISON, PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS, PotionEffectType.MINING_FATIGUE,
            PotionEffectType.INSTANT_DAMAGE, PotionEffectType.NAUSEA,
            PotionEffectType.HUNGER, PotionEffectType.WITHER,
            PotionEffectType.LEVITATION, PotionEffectType.UNLUCK,
            PotionEffectType.BAD_OMEN, PotionEffectType.DARKNESS
    );

    public static final double ROKU_MIN_SPEED = 1.0;   // 20 m/s
    private static final double ROKU_MAX_SPEED = 3.75;  // 75 m/s
    // Over 10 seconds
    private static final double ROKU_RAMP_RATE = (ROKU_MAX_SPEED - ROKU_MIN_SPEED) / 200.0;
    static final long ROKU_DEATH_COOLDOWN_MS = 900_000L; // 15 minutes

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    @Attribute(Attribute.DURATION)
    private long duration;

    private State state;
    private AvatarForm activeForm;
    private final int pastLivesSlot;
    private PotionEffect savedHealthBoostEffect = null; // Kyoshi: original Health Boost to restore on form end
    private List<PotionEffect> szetoSavedEffects = null; // Szeto: original effects before the +1 boost

    // Kuruk state
    private UUID kurukCurrentTarget = null;
    private int kurukHitCount = 0;
    private long kurukLastHitTime = 0L;
    private static final long KURUK_STREAK_TIMEOUT_MS = 5_000L;
    // 50% HP = 10 hearts = 20 half-hearts
    private static final double KURUK_LOW_HP_FRACTION = 0.5;

    // Aang state — speed level 3–10 (amplifier = level - 1)
    private int aangSpeedLevel = 3; // Speed III minimum
    private int aangSecondTimer = 0; // counts ticks toward next 1-second step
    private static final long AANG_END_COOLDOWN_MS = 10_000L;

    // Roku state
    private UUID fangUUID = null;
    private double rokuCurrentSpeed = ROKU_MIN_SPEED;

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
            endAbility(true);
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
            case WAN      -> activateWan();
            case SZETO    -> activateSzeto();
            case YANGCHEN -> activateYangchen();
            case KURUK    -> activateKuruk();
            case KYOSHI   -> activateKyoshi();
            case ROKU     -> activateRoku();
            case AANG     -> activateAang();
            case KORRA    -> activateKorra();
        }
    }

    private void tickForm() {
        if (activeForm == AvatarForm.WAN) {
            tickWan();
        } else if (activeForm == AvatarForm.YANGCHEN) {
            tickYangchen();
        } else if (activeForm == AvatarForm.KURUK) {
            tickKuruk();
        } else if (activeForm == AvatarForm.ROKU) {
            tickRoku();
        } else if (activeForm == AvatarForm.AANG) {
            tickAang();
        }
    }

    public void endAbility(final boolean applyCooldown) {
        if (activeForm != null) {
            player.sendMessage(ChatUtils.chatMessage("&5Your connection to &dAvatar " + activeForm.getDisplayName() + " &5has faded"));
            removeFormEffects();
        }
        if (applyCooldown) {
            // Aang form uses a short cooldown so the player can reactivate quickly
            if (activeForm == AvatarForm.AANG) {
                bPlayer.addCooldown(getName(), AANG_END_COOLDOWN_MS);
            } else {
                bPlayer.addCooldown(this);
            }
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
            case SZETO  -> removeSzetoEffects();
            case KURUK  -> removeKurukEffects();
            case ROKU   -> removeRokuEffects();
            case AANG   -> {
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.JUMP_BOOST);
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

    private void activateSzeto() {
        szetoSavedEffects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!SZETO_POSITIVE_EFFECTS.contains(effect.getType())) continue;
            szetoSavedEffects.add(effect);
            player.removePotionEffect(effect.getType());
            player.addPotionEffect(new PotionEffect(
                    effect.getType(),
                    effect.getDuration(),
                    effect.getAmplifier() + 1,
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            ));
        }
    }

    private void removeSzetoEffects() {
        if (szetoSavedEffects == null) return;
        for (PotionEffect saved : szetoSavedEffects) {
            player.removePotionEffect(saved.getType());
            player.addPotionEffect(saved);
        }
        szetoSavedEffects = null;
    }

    private void activateYangchen() {
        // Yangchen's conversion runs every tick via tickYangchen
    }

    private void tickYangchen() {
        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            PotionEffectType type = effect.getType();
            if (!YANGCHEN_NEGATIVE_EFFECTS.contains(type)) continue;
            player.removePotionEffect(type);
            PotionEffectType converted = YANGCHEN_CONVERSIONS.get(type);
            if (converted != null) {
                // Stack on top of any existing converted effect already on the player
                PotionEffect existing = player.getPotionEffect(converted);
                int newAmplifier;
                if (existing != null) {
                    int existingAmp = existing.getAmplifier();
                    int incomingAmp = effect.getAmplifier();
                    if (existingAmp == 0 && incomingAmp == 0) newAmplifier = 1;
                    else if (existingAmp == 0) newAmplifier = incomingAmp + 1;
                    else if (incomingAmp == 0) newAmplifier = existingAmp + 1;
                    else newAmplifier = existingAmp + incomingAmp + 1;
                } else {
                    newAmplifier = effect.getAmplifier();
                }
                newAmplifier = PotionEffectListener.determineEffectAmplifierRestriction(newAmplifier, converted, player);
                player.addPotionEffect(new PotionEffect(
                        converted,
                        effect.getDuration(),
                        newAmplifier,
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon()
                ));
            }
        }
    }

    private void activateKuruk() {
        kurukCurrentTarget = null;
        kurukHitCount = 0;
        kurukLastHitTime = 0L;
    }

    private void tickKuruk() {
        double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        boolean lowHp = player.getHealth() < maxHp * KURUK_LOW_HP_FRACTION;

        if (lowHp) {
            // Below 50% HP
            if (player.getPotionEffect(PotionEffectType.SPEED) == null) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, true, true));
            }
            if (player.getPotionEffect(PotionEffectType.REGENERATION) == null) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 1, false, true, true));
            }
            // Reset streak while in low-HP phase
            kurukHitCount = 0;
            kurukCurrentTarget = null;
        } else {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            // Expire streak if 5s without hitting the current target
            if (kurukHitCount > 0 && System.currentTimeMillis() - kurukLastHitTime > KURUK_STREAK_TIMEOUT_MS) {
                kurukHitCount = 0;
                kurukCurrentTarget = null;
            }
        }
    }

    private void removeKurukEffects() {
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        kurukHitCount = 0;
        kurukCurrentTarget = null;
    }

    private void activateAang() {
        aangSpeedLevel = 3;
        aangSecondTimer = 0;
        // Jump Boost II for the entire form duration
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 1, false, true, true));
        // Start at Speed III immediately
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 2, false, true, true));
    }

    private void tickAang() {
        aangSecondTimer++;
        if (aangSecondTimer < 20) return; // Only adjust once per second
        aangSecondTimer = 0;

        if (player.isSprinting()) {
            aangSpeedLevel = Math.min(10, aangSpeedLevel + 1);
        } else {
            aangSpeedLevel = Math.max(3, aangSpeedLevel - 1);
        }

        int targetAmplifier = aangSpeedLevel - 1;
        PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
        if (current == null || current.getAmplifier() != targetAmplifier) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, targetAmplifier, false, true, true));
        }
    }

    private void activateKyoshi() {
        savedHealthBoostEffect = player.getPotionEffect(PotionEffectType.HEALTH_BOOST);
        int baseAmplifier = savedHealthBoostEffect != null ? savedHealthBoostEffect.getAmplifier() : -1;

        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, baseAmplifier + 5, false, true, true));

        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 3, false, true, true));
    }

    private void activateRoku() {
        if (player.getWorld().getName().equalsIgnoreCase("arena")) {
            player.sendMessage(ChatUtils.chatMessage("&cFang cannot fly in the arena!"));
            remove();
            return;
        }

        rokuCurrentSpeed = ROKU_MIN_SPEED;

        Location spawnLoc = player.getLocation().clone().add(0, 4, 0);
        EnderDragon dragon = player.getWorld().spawn(spawnLoc, EnderDragon.class, entity -> {
            entity.setPersistent(false);
            entity.setCustomName("Fang");
            entity.setCustomNameVisible(true);

            var maxHpAttr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHpAttr != null) {
                maxHpAttr.setBaseValue(200.0);
            }
            entity.setHealth(200.0);
            entity.setPhase(EnderDragon.Phase.HOVER);

            entity.getPersistentDataContainer().set(
                    CustomKeys.FANG_OWNER, PersistentDataType.STRING, player.getUniqueId().toString());
        });

        // Silence all vanilla sounds as we play our own occasional sounds in tickRoku
        dragon.setSilent(true);

        fangUUID = dragon.getUniqueId();
        fangInstances.put(fangUUID, this);

        // Delay mounting by 3 ticks so the player's sneak key is released first
        Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(), () -> {
            if (!dragon.isDead() && fangUUID != null) {
                dragon.addPassenger(player);
                MountListener.getInstance().registerFang(dragon, player, ROKU_MIN_SPEED);
            }
        }, 3L);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.2f);
    }

    private static final float[] ROKU_SOUND_PITCHES = {1.2f, 1.25f, 1.5f};

    private void tickRoku() {
        if (fangUUID == null) return;
        Entity e = Bukkit.getEntity(fangUUID);
        if (!(e instanceof EnderDragon dragon) || dragon.isDead()) return;

        boolean playerIsRiding = dragon.getPassengers().contains(player);

        if (!playerIsRiding) {
            dragon.setGravity(false); // Handled manually so AI doesn't fight the descent
            dragon.setPhase(EnderDragon.Phase.HOVER);
            rokuCurrentSpeed = ROKU_MIN_SPEED;

            // Descend toward the ground so the player can easily reach Fang again
            Location loc = dragon.getLocation();
            boolean nearGround = false;
            for (int i = 1; i <= 5; i++) {
                if (!dragon.getWorld()
                        .getBlockAt((int) loc.getX(), (int) (loc.getY() - i), (int) loc.getZ())
                        .isPassable()) {
                    nearGround = true;
                    break;
                }
            }
            if (!nearGround) {
                dragon.teleport(loc.subtract(0, 0.4, 0));
            }
            return;
        }

        // Disable gravity while riding so Fang floats in place when the player makes no input
        dragon.setGravity(false);
        // Lock phase to prevent the dragon entering attack or circling phases
        dragon.setPhase(EnderDragon.Phase.HOVER);

        // Ramp speed toward maximum each tick
        rokuCurrentSpeed = Math.min(ROKU_MAX_SPEED, rokuCurrentSpeed + ROKU_RAMP_RATE);

        // Push the updated speed into MountListener
        MountListener.getInstance().updateFangSpeed(fangUUID, rokuCurrentSpeed);

        // Occasionally play a pitched ambient roar (every 10~ seconds)
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < 0.005) {
            float pitch = ROKU_SOUND_PITCHES[rng.nextInt(ROKU_SOUND_PITCHES.length)];
            dragon.getWorld().playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, pitch);
        }
    }

    private void removeRokuEffects() {
        if (fangUUID == null) return;
        fangInstances.remove(fangUUID);
        Entity e = Bukkit.getEntity(fangUUID);
        if (e instanceof EnderDragon dragon && !dragon.isDead()) {
            for (Entity passenger : new ArrayList<>(dragon.getPassengers())) {
                passenger.leaveVehicle();
            }
            dragon.remove();
        }
        fangUUID = null;
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
        if (activeForm == AvatarForm.KURUK) {
            onKurukHit(e);
        }
    }

    private void onKurukHit(EntityDamageByEntityEvent e) {
        // Only applies above 50% HP
        double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (player.getHealth() < maxHp * KURUK_LOW_HP_FRACTION) return;

        UUID targetId = e.getEntity().getUniqueId();

        // Reset streak if switched target, or 5s elapsed since last hit on the same target
        if (!targetId.equals(kurukCurrentTarget)
                || (kurukLastHitTime > 0 && System.currentTimeMillis() - kurukLastHitTime > KURUK_STREAK_TIMEOUT_MS)) {
            kurukHitCount = 0;
        }

        kurukCurrentTarget = targetId;
        kurukHitCount = Math.min(10, kurukHitCount + 1);
        kurukLastHitTime = System.currentTimeMillis();

        // Extra 0.5 hearts per hit, capped at 10 hits
        e.setDamage(e.getDamage() + kurukHitCount * 1.0);
    }

    @Override
    public void remove() {
        MultiAbilityManager.unbindMultiAbility(player);
        activeInstances.remove(player.getUniqueId());
        if (fangUUID != null) {
            fangInstances.remove(fangUUID);
        }
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

    public AvatarForm getActiveForm() {
        return activeForm;
    }

    public static PastLives getFangInstance(final UUID fangEntityUUID) {
        return fangInstances.get(fangEntityUUID);
    }

    /**
     * Called when Fang is killed by an external source.
     */
    public void onFangDeath() {
        fangInstances.remove(fangUUID);
        fangUUID = null;
        player.sendMessage(ChatUtils.chatMessage("&cFang has fallen! You must wait before calling upon Roku again."));
        bPlayer.addCooldown(getName(), ROKU_DEATH_COOLDOWN_MS);
        remove();
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
                ChatUtils.translateToColor("  &f1. Wan - Channels Raava's spirit, granting strength and 1.5x damage to those you strike\n") +
                ChatUtils.translateToColor("  &f2. Szeto - Increases the level of positive status effects when applied\n") +
                ChatUtils.translateToColor("  &f3. Yangchen - Convert harmful status effects applied to you into their positive counterparts\n") +
                ChatUtils.translateToColor("  &f4. Kuruk - Attacking form deals increasing damage with consecutive hits, defensive form provides speed and regeneration\n") +
                ChatUtils.translateToColor("  &f5. Kyoshi - Adds an extra row of hearts and high resistance\n") +
                ChatUtils.translateToColor("  &f6. Roku - Summon and ride his dragon, Fang\n") +
                ChatUtils.translateToColor("  &f7. Aang - Increases speed as you sprint, and 2/3 ability cooldown\n") +
                ChatUtils.translateToColor("  &f8. Korra - Significant base Strength and Speed");
    }
}
