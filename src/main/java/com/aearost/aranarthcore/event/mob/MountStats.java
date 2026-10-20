package com.aearost.aranarthcore.event.mob;

import com.aearost.aranarthcore.objects.AranarthMount;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Displays a mount's stats when shift + right-clicked.
 */
public class MountStats {
    public void execute(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();

        if (!player.isSneaking()) {
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() == Material.GOAT_HORN) {
            return;
        }

        Entity clicked = e.getRightClicked();

        if (clicked instanceof AbstractHorse mount) {
            e.setCancelled(true);
            showHorseCamelStats(player, mount);
        } else if (clicked instanceof Camel camel) {
            e.setCancelled(true);
            showCamelStats(player, camel);
        } else if (clicked instanceof Sniffer sniffer) {
            e.setCancelled(true);
            showSnifferStats(player, sniffer);
        } else if (clicked instanceof Ravager ravager) {
            e.setCancelled(true);
            showRavagerStats(player, ravager);
        } else if (clicked instanceof HappyGhast ghast) {
            e.setCancelled(true);
            showHappyGhastStats(player, ghast);
        } else if (clicked instanceof PolarBear bear) {
            e.setCancelled(true);
            showPolarBearStats(player, bear);
        }
    }

    private void showHorseCamelStats(Player player, AbstractHorse mount) {
        double jumpBlocks = getJumpHeight(mount.getJumpStrength());
        double speed = mount.getAttribute(Attribute.MOVEMENT_SPEED).getValue() * 43.17;
        double health = mount.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);

        String ownerName = mount.getOwner() != null
                ? AranarthUtils.getPlayer(mount.getOwner().getUniqueId()).getNickname()
                : Lang.getFor(player, "general.none");

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&e" + mount.getName(), "color", "&e")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_jump", "value", String.format("%.2f", jumpBlocks))));
    }

    private void showCamelStats(Player player, Camel camel) {
        double jumpStrength = camel.getAttribute(Attribute.JUMP_STRENGTH).getValue();
        double jumpBlocks = getJumpHeight(jumpStrength);
        double speed = camel.getAttribute(Attribute.MOVEMENT_SPEED).getValue() * 43.17;
        double health = camel.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);

        String ownerName = camel.getOwner() != null
                ? AranarthUtils.getPlayer(camel.getOwner().getUniqueId()).getNickname()
                : Lang.getFor(player, "general.none");

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&e" + camel.getName(), "color", "&e")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_jump", "value", String.format("%.2f", jumpBlocks))));
    }

    private void showSnifferStats(Player player, Sniffer sniffer) {
        AranarthMount mount = AranarthMount.fromSniffer(sniffer);
        if (mount == null) {
            return;
        }

        double health = sniffer.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);
        double speed = mount.getSnifferSpeedMetersPerSecond();

        String ownerName = Lang.getFor(player, "general.none");
        if (mount.getOwnerUUID() != null) {
            ownerName = AranarthUtils.getPlayer(mount.getOwnerUUID()).getNickname();
        }

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&e" + sniffer.getName(), "color", "&e")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));

        if (mount.getThirdAttribute() != null) {
            double digSpeed = mount.getDigSpeedBlocksPerSecond();
            player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_dig_speed", "value", String.format("%.1f", digSpeed))));
        }
    }

    private void showRavagerStats(Player player, Ravager ravager) {
        AranarthMount mount = AranarthMount.fromRavager(ravager);
        if (mount == null) {
            return;
        }

        double health = ravager.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);
        double speed = mount.getSnifferSpeedMetersPerSecond(); // blocks/tick * 20

        String ownerName = Lang.getFor(player, "general.none");
        if (mount.getOwnerUUID() != null) {
            ownerName = AranarthUtils.getPlayer(mount.getOwnerUUID()).getNickname();
        }

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&e" + ravager.getName(), "color", "&e")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));

        if (mount.getThirdAttribute() != null) {
            double maxHearts = mount.getThirdAttribute() / 2.0;
            player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_ram_damage", "value", String.format("%.1f", maxHearts))));
        }
    }

    private void showHappyGhastStats(Player player, HappyGhast ghast) {
        AranarthMount mount = AranarthMount.fromHappyGhast(ghast);
        if (mount == null) {
            return;
        }

        double health = ghast.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);
        double speed = mount.getSnifferSpeedMetersPerSecond(); // blocks/tick * 20

        String ownerName = Lang.getFor(player, "general.none");
        if (mount.getOwnerUUID() != null) {
            ownerName = AranarthUtils.getPlayer(mount.getOwnerUUID()).getNickname();
        }

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&7" + ghast.getName(), "color", "&7")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));

        if (mount.getThirdAttribute() != null) {
            double maxHearts = mount.getThirdAttribute() / 2.0;
            player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_bellow_damage", "value", String.format("%.1f", maxHearts))));
        }
    }

    private void showPolarBearStats(Player player, PolarBear bear) {
        AranarthMount mount = AranarthMount.fromPolarBear(bear);
        if (mount == null) {
            return;
        }

        double health = bear.getAttribute(Attribute.MAX_HEALTH).getValue();
        int halfHearts = (int) Math.round(health);
        double speed = mount.getSnifferSpeedMetersPerSecond(); // blocks/tick * 20

        String ownerName = Lang.getFor(player, "general.none");
        if (mount.getOwnerUUID() != null) {
            ownerName = AranarthUtils.getPlayer(mount.getOwnerUUID()).getNickname();
        }

        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stats_header", "name", "&b" + bear.getName(), "color", "&b")));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_owner", "owner", ownerName)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_health", "value", halfHearts)));
        player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_speed", "value", String.format("%.2f", speed))));

        if (mount.getThirdAttribute() != null) {
            double maxHearts = mount.getThirdAttribute() / 2.0;
            player.sendMessage(ChatUtils.translateToColor(Lang.getFor(player, "mount.stat_bite_damage", "value", String.format("%.1f", maxHearts))));
        }
    }

    private double getJumpHeight(double strength) {
        return -0.1817584952 * Math.pow(strength, 3)
                + 3.689713992 * Math.pow(strength, 2)
                + 2.128599134 * strength
                - 0.343930367;
    }
}
