package com.aearost.aranarthcore.abilities.airbending;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MultiAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager;
import com.projectkorra.projectkorra.ability.util.MultiAbilityManager.MultiAbilityInfoSub;
import com.projectkorra.projectkorra.attribute.Attribute;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class AstralProjection extends SpiritualAbility implements AddonAbility, MultiAbility {

    public static final int MAX_USES = 5;
    public static final long AURA_COOLDOWN_MS = 8000;
    public static final long SCREAM_COOLDOWN_MS = 3000;
    public static final long POSSESS_COOLDOWN_MS = 12000;

    // PDC keys for crash-safe armor persistence (index matches getArmorContents: 0=boots,1=legs,2=chest,3=helmet)
    private static final String PDC_ARMOR_PREFIX = "astral_armor_";

    private static final HashMap<UUID, AstralProjection> activeProjections = new HashMap<>();
    // Tracks UUIDs currently dealing sub-ability damage so the listener doesn't cancel it
    private static final java.util.HashSet<UUID> subAbilityDamaging = new java.util.HashSet<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DURATION)
    private int duration;
    @Attribute(Attribute.CHARGE_DURATION)
    private int chargeDuration;

    private boolean isCharged;
    private Mannequin mannequin;
    private long abilityStart;
    private int sneakToggleNum;
    private long toggleStart;
    private int usesRemaining;
    // Armor stripped from the player when the projection launches; restored on end
    private ItemStack[] storedArmor;
    // Snapshot of the player's potion effects taken before projection (fallback for restore)
    private java.util.Collection<PotionEffect> storedEffects;
    // Pre-projection glowing state so it can be restored accurately on end
    private boolean wasGlowing;
    // Dedicated task that re-applies projection effects each tick AFTER PK's own tick completes
    private BukkitTask projectionEffectTask;


    public AstralProjection(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 60000;
        range = 30;
        duration = 30000;
        chargeDuration = 4000;
        usesRemaining = MAX_USES;

        start();
    }

    @Override
    public void progress() {
        if (mannequin == null) {
            if (!isCharged) {
                if (player.isSneaking()) {
                    chargeProjection();
                } else {
                    remove();
                }
            } else {
                if (player.isSneaking()) {
                    Location eyeLoc = player.getEyeLocation();
                    player.spawnParticle(Particle.ENCHANT, eyeLoc, 4, 0.25, 0.25, 0.25, 0.05);
                } else {
                    launchAstralForm();
                }
            }
        } else {
            boolean isExceedingRange = player.getLocation().distance(mannequin.getLocation()) > range;
            if (isExceedingRange
                    || (sneakToggleNum == 4 && !isSneakToggleExceeded())
                    || (abilityStart + duration < System.currentTimeMillis())) {
                endAbility();
                return;
            }

            if (toggleStart != 0 && isSneakToggleExceeded()) {
                toggleStart = 0;
                sneakToggleNum = 0;
            }

            if (!player.isSneaking()) {
                if (sneakToggleNum % 2 != 0) {
                    sneakToggleNum++;
                }
            } else {
                if (sneakToggleNum % 2 == 0) {
                    if (toggleStart == 0) {
                        toggleStart = System.currentTimeMillis();
                    }
                    sneakToggleNum++;
                }
            }

            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 1, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private void launchAstralForm() {
        Mannequin m = (Mannequin) player.getWorld().spawnEntity(player.getLocation(), EntityType.MANNEQUIN);
        m.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
        m.setPose(Pose.SNEAKING);
        this.mannequin = m;
        abilityStart = System.currentTimeMillis();

        // Transfer armor: save to field + PDC, equip on mannequin, strip from player
        storedArmor = player.getInventory().getArmorContents().clone();
        persistArmorToPlayer(player, storedArmor);
        equipArmorOnMannequin(m, storedArmor);
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Snapshot effects BEFORE adding invisibility, transfer to mannequin, clear from player
        storedEffects = player.getActivePotionEffects();
        for (PotionEffect effect : storedEffects) {
            m.addPotionEffect(effect);
        }
        for (PotionEffect effect : storedEffects) {
            player.removePotionEffect(effect.getType());
        }

        // Store pre-projection glowing state for accurate restoration
        wasGlowing = player.isGlowing();

        player.setAllowFlight(true);
        player.setFlying(true);
        // Immediately launch upward so the player is airborne without manual input
        player.setVelocity(new Vector(0, 0.5, 0));
        player.setInvulnerable(true);
        player.setCollidable(false);

        activeProjections.put(player.getUniqueId(), this);
        MultiAbilityManager.bindMultiAbility(player, "AstralProjection");
        // Start a dedicated task registered AFTER PK's own tick loop so it always runs last
        // each tick, ensuring our invisibility and glowing are never overridden by PK internals.
        // Starts 2 ticks out so bindMultiAbility's own deferred work has fully settled first.
        projectionEffectTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isProjecting(player.getUniqueId())) {
                    cancel();
                    return;
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
                player.setGlowing(true);
            }
        }.runTaskTimer(AranarthCore.getInstance(), 2L, 1L);

        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 0.7F);
    }

    public void endAbility() {
        if (mannequin == null) {
            bPlayer.addCooldown(this);
            remove();
            return;
        }
        Location bodyLocation = mannequin.getLocation();
        // restorePlayer must run before mannequin.remove() so it can read ticked-down effects
        restorePlayer();
        mannequin.remove();
        mannequin = null;
        if (!player.isDead()) {
            player.teleport(bodyLocation);
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 1.3F);
        }
        bPlayer.addCooldown(this);
        remove();
    }

    public void endAbilityWithDamage(double damage) {
        Location bodyLocation = (mannequin != null) ? mannequin.getLocation() : player.getLocation();
        // restorePlayer must run before mannequin.remove() so it can read ticked-down effects
        restorePlayer();
        if (mannequin != null) {
            mannequin.remove();
            mannequin = null;
        }
        if (!player.isDead()) {
            player.teleport(bodyLocation);
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 1.3F);
            player.sendMessage(ChatUtils.chatMessage("&cYour body was struck while projecting!"));
        }
        bPlayer.addCooldown(this);
        remove();
        if (!player.isDead()) {
            player.damage(damage);
        }
    }

    @Override
    public void remove() {
        // Guard: restore state if still projecting (e.g. forced PK removal)
        if (activeProjections.containsKey(player.getUniqueId())) {
            restorePlayer();
            if (mannequin != null) {
                mannequin.remove();
                mannequin = null;
            }
            activeProjections.remove(player.getUniqueId());
        }
        MultiAbilityManager.unbindMultiAbility(player);
        super.remove();
    }

    /**
     * Restores the player's old potion effects and armor upon the end of the ability.
     */
    private void restorePlayer() {
        if (projectionEffectTask != null) {
            projectionEffectTask.cancel();
            projectionEffectTask = null;
        }
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setGlowing(wasGlowing);
        player.setCollidable(true);
        // Remove projection effects before restoring — don't let them linger or be double-applied
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.GLOWING);

        // Transfer effects back from the mannequin (reflects ticked-down durations during projection).
        // Fall back to the original snapshot only if the mannequin is already gone.
        if (mannequin != null) {
            for (PotionEffect effect : mannequin.getActivePotionEffects()) {
                player.addPotionEffect(effect);
            }
        } else if (storedEffects != null) {
            for (PotionEffect effect : storedEffects) {
                player.addPotionEffect(effect);
            }
        }
        storedEffects = null;

        // Restore armor from the in-memory store (covers all normal end paths)
        if (storedArmor != null) {
            player.getInventory().setArmorContents(storedArmor);
            storedArmor = null;
        }
        clearArmorFromPdc(player);
    }

    // -------------------------------------------------------------------------
    // Armor helpers
    // -------------------------------------------------------------------------

    /**
     * Equips the player's armor on the mannequin.
     */
    private static void equipArmorOnMannequin(Mannequin mannequin, ItemStack[] armor) {
        EntityEquipment eq = mannequin.getEquipment();
        if (eq == null) return;
        // getArmorContents order: [boots, leggings, chestplate, helmet]
        eq.setBoots(armor[0]);
        eq.setLeggings(armor[1]);
        eq.setChestplate(armor[2]);
        eq.setHelmet(armor[3]);
    }

    /**
     * Persists armor pieces to the player's PersistentDataContainer so they can
     * be recovered if the server crashes before the projection ends cleanly.
     */
    private static void persistArmorToPlayer(Player player, ItemStack[] armor) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), PDC_ARMOR_PREFIX + i);
                pdc.set(key, PersistentDataType.BYTE_ARRAY, armor[i].serializeAsBytes());
            }
        }
    }

    /**
     * Removes all persisted armor entries from the player (called after a successful restore).
     */
    private static void clearArmorFromPdc(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        for (int i = 0; i < 4; i++) {
            pdc.remove(new NamespacedKey(AranarthCore.getInstance(), PDC_ARMOR_PREFIX + i));
        }
    }

    /**
     * Restores the player's persisted armor upon server join for crash recovery.
     */
    public static void restoreArmorFromPdc(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        ItemStack[] armor = new ItemStack[4];
        boolean found = false;
        for (int i = 0; i < 4; i++) {
            NamespacedKey key = new NamespacedKey(AranarthCore.getInstance(), PDC_ARMOR_PREFIX + i);
            if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
                byte[] bytes = pdc.get(key, PersistentDataType.BYTE_ARRAY);
                armor[i] = ItemStack.deserializeBytes(bytes);
                pdc.remove(key);
                found = true;
            }
        }
        if (found) {
            player.getInventory().setArmorContents(armor);
            player.sendMessage(ChatUtils.chatMessage("&7Your armor has been restored from your last Astral Projection."));
        }
    }

    // -------------------------------------------------------------------------
    // Sub-ability activation — triggered by the listener on left-click per slot
    // -------------------------------------------------------------------------

    /** Slot 0: Emit a disorienting aura (Slowness II + Nausea) in a 5-block radius. */
    public void activateAura() {
        if (!isAuraReady()) {
            long cooldown = (int) bPlayer.getCooldown("Aura") / 1000;
            player.sendMessage(ChatUtils.chatMessage("&7Aura is on cooldown for another " + cooldown + " seconds"));
            return;
        }
        if (!consumeUse()) return;

        double radius = 5.0;
        int durationTicks = 100; // 5 seconds
        Location loc = player.getLocation();

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            player.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(x, 0.5, z), 3, 0.1, 0.1, 0.1, 0);
            player.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(x, 1.5, z), 2, 0.1, 0.1, 0.1, 0);
        }

        for (Entity entity : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity.equals(player) || entity.equals(mannequin)) continue;
            if (entity instanceof LivingEntity living) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 1, false, true, true));
                living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, durationTicks, 0, false, true, true));
            }
        }

        bPlayer.addCooldown("Aura", AURA_COOLDOWN_MS);
    }

    /** Slot 1: Release a ghastly scream that erupts soul particles and damages nearby entities. */
    public void activateScream() {
        if (!isScreamReady()) {
            int cooldown = (int) (bPlayer.getCooldown("Scream") - System.currentTimeMillis()) / 1000 + 1;
            player.sendMessage(ChatUtils.chatMessage("&7Scream is on cooldown for another " + cooldown + " seconds"));
            return;
        }
        if (!consumeUse()) return;

        double radius = 4.0;
        double damage = 2.0;
        Location loc = player.getLocation();

        player.getWorld().playSound(loc, Sound.ENTITY_GHAST_SCREAM, 2.0F, 0.8F);

        for (double pitch = 0; pitch <= Math.PI; pitch += Math.PI / 8) {
            for (double yaw = 0; yaw < Math.PI * 2; yaw += Math.PI / 8) {
                double x = Math.sin(pitch) * Math.cos(yaw) * radius;
                double y = Math.cos(pitch) * radius;
                double z = Math.sin(pitch) * Math.sin(yaw) * radius;
                player.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0);
            }
        }
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 30, 0.5, 0.5, 0.5, 0.05);

        subAbilityDamaging.add(player.getUniqueId());
        for (Entity entity : player.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity.equals(player) || entity.equals(mannequin)) continue;
            if (entity instanceof LivingEntity living) {
                living.damage(damage, player);
            }
        }
        subAbilityDamaging.remove(player.getUniqueId());

        bPlayer.addCooldown("Scream", SCREAM_COOLDOWN_MS);
    }

    /** Slot 2: Possess a player in line of sight within 8 blocks, scrambling their movement. */
    public void activatePossess() {
        if (!isPossessReady()) {
            long cooldown = (int) bPlayer.getCooldown("Possess") / 1000;
            player.sendMessage(ChatUtils.chatMessage("&7Possess is on cooldown for another " + cooldown + " seconds"));
            return;
        }

        Player victim = findPossessTarget();
        if (victim == null) {
            return;
        }

        if (!consumeUse()) return;

        victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 1, false, true, true));
        victim.sendMessage(ChatUtils.chatMessage("&5An unseen force seizes your body!"));

        new BukkitRunnable() {
            private int ticks = 0;
            private final Random rand = new Random();

            @Override
            public void run() {
                if (ticks >= 60 || !victim.isOnline() || victim.isDead()) {
                    victim.sendMessage(ChatUtils.chatMessage("&7You regain control of your body"));
                    cancel();
                    return;
                }
                if (ticks % 8 == 0) {
                    double x = (rand.nextDouble() - 0.5) * 1.2;
                    double z = (rand.nextDouble() - 0.5) * 1.2;
                    victim.setVelocity(new Vector(x, 0.15, z));
                }
                ticks++;
            }
        }.runTaskTimer(AranarthCore.getInstance(), 0L, 1L);

        bPlayer.addCooldown("Possess", POSSESS_COOLDOWN_MS);
    }

    /**
     * Finds a valid possession target: a Survival player the caster is roughly looking at within 8 blocks.
     * Requires line of sight and the target to be within ~25 degrees of the look direction.
     */
    private Player findPossessTarget() {
        Vector lookDir = player.getLocation().getDirection().normalize();
        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.getGameMode() != GameMode.SURVIVAL) continue;
            if (isProjecting(candidate.getUniqueId())) continue;
            if (!player.hasLineOfSight(entity)) continue;
            Vector toTarget = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            if (toTarget.dot(lookDir) > 0.9) {
                return candidate;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Use and cooldown helpers
    // -------------------------------------------------------------------------

    private boolean consumeUse() {
        if (usesRemaining <= 0) {
            player.sendMessage(ChatUtils.chatMessage("&cNo astral uses remaining!"));
            return false;
        }
        usesRemaining--;
        player.sendMessage(ChatUtils.chatMessage("&7Astral uses remaining: &f" + usesRemaining + "/" + MAX_USES));
        return true;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }


    private boolean isAuraReady() {
        return !bPlayer.isOnCooldown("Aura");
    }

    private boolean isScreamReady() {
        return !bPlayer.isOnCooldown("Scream");
    }

    private boolean isPossessReady() {
        return !bPlayer.isOnCooldown("Possess");
    }

    // -------------------------------------------------------------------------
    // Static access
    // -------------------------------------------------------------------------

    public static boolean isProjecting(UUID uuid) {
        return activeProjections.containsKey(uuid);
    }

    public static AstralProjection getActiveProjection(UUID uuid) {
        return activeProjections.get(uuid);
    }

    public static HashMap<UUID, AstralProjection> getActiveProjections() {
        return activeProjections;
    }

    public static boolean isSubAbilityDamaging(UUID uuid) {
        return subAbilityDamaging.contains(uuid);
    }

    /**
     * Ends all active AstralProjections, teleporting each player back to their
     * mannequin location and restoring their state. Safe to call on PK reload or
     * plugin disable.
     */
    public static void endAllProjections() {
        new ArrayList<>(activeProjections.values()).forEach(AstralProjection::endAbility);
    }

    // -------------------------------------------------------------------------
    // PK ability interface
    // -------------------------------------------------------------------------

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
    public String getName() {
        return "AstralProjection";
    }

    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    @Override
    public ArrayList<MultiAbilityInfoSub> getMultiAbilities() {
        ArrayList<MultiAbilityInfoSub> abils = new ArrayList<>();
        abils.add(new MultiAbilityInfoSub("Aura", Element.SPIRITUAL));
        abils.add(new MultiAbilityInfoSub("Scream", Element.SPIRITUAL));
        abils.add(new MultiAbilityInfoSub("Possess", Element.SPIRITUAL));
        return abils;
    }

    public void chargeProjection() {
        if (!isCharged) {
            if (chargeDuration != 0) {
                if (System.currentTimeMillis() > getStartTime() + chargeDuration) {
                    isCharged = true;
                }
            }
        }
    }

    public boolean isSneakToggleExceeded() {
        return System.currentTimeMillis() - toggleStart > 500;
    }

    public Mannequin getMannequin() {
        return mannequin;
    }

    @Override
    public void load() {}

    @Override
    public void stop() {
        if (player != null && activeProjections.containsKey(player.getUniqueId())) {
            restorePlayer();
            if (mannequin != null) {
                mannequin.remove();
                mannequin = null;
            }
            activeProjections.remove(player.getUniqueId());
            MultiAbilityManager.unbindMultiAbility(player);
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
        return "This Spiritual ability allows the bender to leave their body, and project themselves as a flying, invisible, and invulnerable form. " +
                "While projecting, up to " + MAX_USES + " sub-abilities can be used: " +
                "Aura (Slowness + Nausea in radius), Scream (soul burst + damage), and Possess (disorient a nearby player's movement). " +
                "Enemies who strike your body will snap you back and deal that damage to you. " +
                "Travel too far away from your body, or double-tap sneak to return early.\n" +
                ChatUtils.translateToColor("&fTo activate: Sneak (hold for " + (chargeDuration / 4) + " seconds) > Sneak (release)") + "\n" +
                ChatUtils.translateToColor("&fTo cancel: Double-tap Sneak") + "\n" +
                ChatUtils.translateToColor("&fSub-abilities: Left-click while projecting with the correct hotbar slot selected");
    }
}
