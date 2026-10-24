package com.aearost.aranarthcore.abilities.chiblocking;

import com.aearost.aranarthcore.utils.AbilityConfig;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ChiAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HighJump extends ChiAbility implements AddonAbility {

    public enum JumpType { JUMP, DOUBLEJUMP, EVADE, LUNGE }

    private static final Map<UUID, HighJump> ACTIVE_INSTANCES = new HashMap<>();

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute("JumpCooldown")
    private long jumpCooldown;
    @Attribute("DoubleJumpCooldown")
    private long doubleJumpCooldown;
    @Attribute("EvadeCooldown")
    private long evadeCooldown;
    @Attribute("EvadeDistance")
    private double evadeDistance;
    @Attribute("LungeCooldown")
    private long lungeCooldown;
    @Attribute("LungeDistance")
    private double lungeDistance;

    // 0.6 ~ 2 blocks, 0.81 ~ 4 blocks, 1.0 ~5 blocks
    private double jumpHeight;
    private double doubleJumpHeight;
    private double evadeHeight;
    private double lungeHeight;

    private JumpType jumpType;

    public HighJump(final Player player, final JumpType jumpType) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        this.jumpType           = jumpType;
        this.cooldown           = AbilityConfig.getLong("HighJump.Cooldown");
        this.jumpCooldown       = AbilityConfig.getLong("HighJump.JumpCooldown");
        this.doubleJumpCooldown = AbilityConfig.getLong("HighJump.DoubleJumpCooldown");
        this.evadeCooldown      = AbilityConfig.getLong("HighJump.EvadeCooldown");
        this.evadeDistance      = AbilityConfig.getDouble("HighJump.EvadeDistance");
        this.lungeCooldown      = AbilityConfig.getLong("HighJump.LungeCooldown");
        this.lungeDistance      = AbilityConfig.getDouble("HighJump.LungeDistance");
        this.jumpHeight         = AbilityConfig.getDouble("HighJump.JumpHeight");
        this.doubleJumpHeight   = AbilityConfig.getDouble("HighJump.DoubleJumpHeight");
        this.evadeHeight        = AbilityConfig.getDouble("HighJump.EvadeHeight");
        this.lungeHeight        = AbilityConfig.getDouble("HighJump.LungeHeight");

        // Register before start() so same-tick duplicate events are blocked immediately.
        ACTIVE_INSTANCES.put(player.getUniqueId(), this);
        start();
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        switch (jumpType) {
            case JUMP       -> performJump();
            case DOUBLEJUMP -> performDoubleJump();
            case EVADE      -> performEvade();
            case LUNGE      -> performLunge();
        }
    }

    private void performJump() {
        Vector velocity = player.getVelocity();
        velocity.setY(jumpHeight);
        player.setVelocity(velocity);
        poof();
        bPlayer.addCooldown(this, jumpCooldown);
        remove();
    }

    private void performDoubleJump() {
        Vector velocity = player.getVelocity();
        velocity.setY(doubleJumpHeight);
        player.setVelocity(velocity);
        poof();
        bPlayer.addCooldown(this, doubleJumpCooldown);
        remove();
    }

    private void performEvade() {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(-evadeDistance);
        velocity.setY(evadeHeight);
        player.setVelocity(velocity);
        poof();
        bPlayer.addCooldown(this, evadeCooldown);
        remove();
    }

    private void performLunge() {
        Vector velocity = player.getLocation().getDirection().normalize().multiply(lungeDistance);
        velocity.setY(lungeHeight);
        player.setVelocity(velocity);
        poof();
        bPlayer.addCooldown(this, lungeCooldown);
        remove();
    }

    private void poof() {
        final Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT,  loc, 20, 0.5, 1.0, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.5, 1.0, 0.5, 0.002);
        player.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 0.5f, 1.0f);
    }

    @Override
    public void remove() {
        ACTIVE_INSTANCES.remove(player.getUniqueId());
        super.remove();
    }

    public static boolean hasActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.containsKey(uuid);
    }

    public static HighJump getActiveInstance(final UUID uuid) {
        return ACTIVE_INSTANCES.get(uuid);
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return true;
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
        return "HighJump";
    }

    @Override
    public String getDescription() {
        return "A multi-use ability that lets a Chiblocker move in four different ways: a high jump, a double jump, an evasive backflip, and a forward lunge.\n"
                + ChatUtils.translateToColor("&fUsage: Left-click (jump) | Sprint + Left-click (lunge) | Sneak mid-air (double jump) | Sneak on ground (evade)");
    }

    @Override
    public void load() {}

    @Override
    public void stop() {}

    @Override
    public String getAuthor() {
        return "Aearost";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
