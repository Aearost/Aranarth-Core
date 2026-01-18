package com.aearost.aranarthcore.abilities;

import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.SpiritualAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.*;
import org.bukkit.entity.*;

public class AstralProjection extends SpiritualAbility implements AddonAbility {

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DURATION)
    private int duration;
    @Attribute(Attribute.CHARGE_DURATION)
    private int chargeDuration;

    private Location projectionLocation;
    private boolean isCharged;
    private Mannequin mannequin;
    private long abilityStart;
    private int sneakToggleNum;
    private long toggleStart;

    public AstralProjection(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 30000;
        range = 25;
        duration = 30000;
        chargeDuration = 4000;

        start();
    }

    /**
     * Causes the ability to be updated.
     */
    @Override
    public void progress() {
        if (player.getGameMode() == GameMode.SURVIVAL) {
            // Charging the projection
            if (!isCharged && player.isSneaking()) {
                chargeProjection();
            } else {
                if (isCharged) {
                    // Continuously display particles while it is fully charged and they are still sneaking
                    if (player.isSneaking()) {
                        player.spawnParticle(Particle.END_ROD, player.getLocation(), 1);
                    }
                    // The actual ability is now activated
                    else {
                        Mannequin mannequin = (Mannequin) player.getWorld().spawnEntity(player.getLocation(), EntityType.MANNEQUIN);
                        mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
                        this.mannequin = mannequin;
                        abilityStart = System.currentTimeMillis();
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                }
                // If they are no longer sneaking, end the ability charge
                else {
                    remove();
                    return;
                }
            }
        } else {
            boolean isExceedingRange = player.getLocation().distance(mannequin.getLocation()) > range;
            if (isExceedingRange || (sneakToggleNum == 4 && !isSneakToggleExceeded()) || (abilityStart + duration < System.currentTimeMillis())) {
                endAbility();
            } else {
                // Reset if the toggle duration is exceeded
                if (toggleStart != 0 && isSneakToggleExceeded()) {
                    toggleStart = 0;
                    sneakToggleNum = 0;
                }

                // Increase only when releasing the sneak key
                if (!player.isSneaking()) {
                    if (sneakToggleNum % 2 != 0) {
                        sneakToggleNum++;
                    }
                }
                // Increase only when pressing down on the sneak key
                else {
                    if (sneakToggleNum % 2 == 0) {
                        // Starts the timer to ensure the double sneak is done quickly.
                        if (toggleStart == 0) {
                            toggleStart = System.currentTimeMillis();
                        }
                        sneakToggleNum++;
                    }
                }
            }
        }
    }

    /**
     * Determines if this ability uses the PlayerToggleSneakEvent as a controlling mechanism.
     * @return If this ability uses the PlayerToggleSneakEvent as a controlling mechanism.
     */
    @Override
    public boolean isSneakAbility() {
        return true;
    }

    /**
     * Determines if this ability is considered harmless against other players.
     * @return Whether this ability is considered harmless against other players.
     */
    @Override
    public boolean isHarmlessAbility() {
        return true;
    }

    /**
     * Provides the cooldown of the ability.
     * @return The cooldown of the ability.
     */
    @Override
    public long getCooldown() {
        return cooldown;
    }

    /**
     * Provides the name of the ability.
     * @return The name of the ability.
     */
    @Override
    public String getName() {
        return "AstralProjection";
    }

    /**
     * Specifies the Location of the ability.
     * @return the location of the Ability.
     */
    @Override
    public Location getLocation() {
        return player.getLocation();
    }

    /**
     * Handles the charging of the ability.
     */
    public void chargeProjection() {
        if (!isCharged) {
            if (chargeDuration != 0) {
                // Maybe particles or a sound effect? Like a hum sound?
                if (System.currentTimeMillis() > getStartTime() + chargeDuration) {
                    isCharged = true;
                }
            }
            return;
        }
    }

    /**
     * Determines whether the duration for the disabling of the ability via sneak is exceeded.
     * @return Whether the duration for the disabling of the ability via sneak is exceeded.
     */
    public boolean isSneakToggleExceeded() {
        return System.currentTimeMillis() - toggleStart > 500;
    }

    /**
     * Provides the mannequin where the player was standing.
     * @return The mannequin where the player was standing.
     */
    public Mannequin getMannequin() {
        return mannequin;
    }

    /**
     * Ends the ability.
     */
    public void endAbility() {
        player.teleport(mannequin.getLocation());
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1F, 1.3F);
        mannequin.remove();
        player.setGameMode(GameMode.SURVIVAL);
        bPlayer.addCooldown(this);
        remove();
        return;
    }

    /**
     * Called when the ability is loaded by PK. This is where the developer registers Listeners and Permissions.
     */
    @Override
    public void load() {
    }

    /**
     * Called whenever ProjectKorra stops and the ability is unloaded.
     */
    @Override
    public void stop() {
        player.setGameMode(GameMode.SURVIVAL);
    }

    /**
     * @return the name of the author of this AddonAbility
     */
    @Override
    public String getAuthor() {
        return "Aearost";
    }

    /**
     * @return The version of the ability as a String.
     */
    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "This Spiritual ability allows the bender to focus their chi, and project their astral body, flying around the body." +
                "To use this ability, hold sneak to charge until you see aura particles." +
                "You can then release sneak, projecting your astral body." +
                "By traveling too far from your body, or double tapping sneak, the ability will cancel, and you will return to your body.\n" +
                ChatUtils.translateToColor("&fTo activate: Sneak (Hold to charge) > Sneak (Release)") + "\n" +
                ChatUtils.translateToColor("&fTo de-activate: Tap Sneak > Tap Sneak");
    }
}
