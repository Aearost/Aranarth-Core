package com.aearost.aranarthcore.abilities.waterbending;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

public class VineWhip extends PlantAbility implements AddonAbility {

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.SELECT_RANGE)
    private int selectRange;
    @Attribute(Attribute.RANGE)
    private int range;
    @Attribute(Attribute.DURATION)
    private int duration;
    @Attribute(Attribute.DAMAGE)
    private int damage;

    private Block source;

    public VineWhip(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        cooldown = 4000;
        selectRange = 6;
        range = 25;
        duration = 5000;
        damage = 6;

        // Activates the ability
        start();
    }

    /**
     * Causes the ability to be updated.
     */
    @Override
    public void progress() {
        if (source == null) {
            selectSource();
        }

        // Base logic on WaterManipulation
        // http://github.com/ProjectKorra/ProjectKorra/blob/master/core/src/com/projectkorra/projectkorra/waterbending/WaterManipulation.java
    }

    public void selectSource() {
        Block block = BlockSource.getWaterSourceBlock(player, selectRange, ClickType.SHIFT_DOWN, false, false, bPlayer.canPlantbend());
        if (block.getBlockData() instanceof Leaves) {
            source = block;
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
        return "VineWhip";
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
