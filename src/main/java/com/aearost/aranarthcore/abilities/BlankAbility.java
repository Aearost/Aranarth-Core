package com.aearost.aranarthcore.abilities;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BlankAbility extends PlantAbility implements AddonAbility {

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.SELECT_RANGE)
    private int selectRange;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DURATION)
    private int duration;

    public BlankAbility(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        // Add ability values here
        // cooldown = 1000;

        // Activates the ability
        start();
    }

    /**
     * Causes the ability to be updated.
     */
    @Override
    public void progress() {

    }

    /**
     * Determines if this ability uses the PlayerToggleSneakEvent as a controlling mechanism.
     * @return If this ability uses the PlayerToggleSneakEvent as a controlling mechanism.
     */
    @Override
    public boolean isSneakAbility() {
        return false;
    }

    /**
     * Determines if this ability is considered harmless against other players.
     * @return Whether this ability is considered harmless against other players.
     */
    @Override
    public boolean isHarmlessAbility() {
        return false;
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

    /**
     * The Description of the ability.
     */
    @Override
    public String getDescription() {
        return "";
    }
}
