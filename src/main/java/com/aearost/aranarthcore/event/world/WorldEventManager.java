package com.aearost.aranarthcore.event.world;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.enums.WorldEvent;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.DiscordUtils;
import com.aearost.aranarthcore.utils.PermissionUtils;
import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WorldEventManager implements Listener {

    private static WorldEventManager instance;
    private final Map<UUID, Integer> nightBlindnessCounts = new HashMap<>();
    private final Random random = new Random();

    public WorldEventManager(AranarthCore plugin) {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static WorldEventManager getInstance() {
        return instance;
    }

    /**
     * Starts the given world event at the specified intensity.
     * Broadcasts announcement and applies effects to all online players.
     *
     * @param event     The world event to start.
     * @param intensity 0=Weak, 1=Normal, 2=Strong
     */
    public void startEvent(WorldEvent event, int intensity) {
        AranarthUtils.setActiveWorldEvent(event);
        AranarthUtils.setActiveWorldEventIntensity(intensity);

        String title = ChatUtils.translateToColor(event.getTitleText(intensity));
        String subtitle = ChatUtils.translateToColor(event.getSubtitleText(intensity));
        String chatMsg = ChatUtils.chatMessage("&6[World Event] &f" + event.getName(intensity) + " &7has begun!");
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            if (!AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
                continue;
            }
            player.sendTitle(title, subtitle, 20, 120, 20);
            player.sendMessage(chatMsg);
        }

        if (event.isElementalEvent()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyElementalEventToPlayer(player, event);
            }
        }

        if (!AranarthCore.isSmpServer()) {
            DiscordUtils.worldEventMessage(event, intensity, true);
        }
        Bukkit.getLogger().info("[AC] World Event started: " + event.getName(intensity));
    }

    /**
     * Ends the currently active world event.
     */
    public void endEvent() {
        WorldEvent event = AranarthUtils.getActiveWorldEvent();
        if (event == null) {
            return;
        }
        int intensity = AranarthUtils.getActiveWorldEventIntensity();

        AranarthUtils.setActiveWorldEvent(null);
        AranarthUtils.setActiveWorldEventIntensity(1);

        String chatMsg = ChatUtils.chatMessage("&6[World Event] &7The " + event.getName(intensity) + " has ended.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            if (!AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
                continue;
            }
            player.sendMessage(chatMsg);
        }

        // Re-evaluate all player permissions to revoke event-granted sub-elements
        if (event.isElementalEvent()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PermissionUtils.evaluatePlayerPermissions(player);
            }
        }

        if (!AranarthCore.isSmpServer()) {
            DiscordUtils.worldEventMessage(event, intensity, false);
        }
        nightBlindnessCounts.clear();
        Bukkit.getLogger().info("[AC] World Event ended: " + event.getName(intensity));
    }

    /**
     * Applies elemental event sub-element permissions to a player.
     * Only has an effect if the player has the matching element.
     *
     * @param player The player.
     * @param event  The active elemental world event.
     */
    public void applyElementalEventToPlayer(Player player, WorldEvent event) {
        if (!event.isElementalEvent()) {
            return;
        }
        BendingPlayer bendingPlayer = BendingPlayer.getBendingPlayer(player);
        if (bendingPlayer == null || !bendingPlayer.getElements().contains(event.getElement())) {
            return;
        }

        PermissionAttachment perms = PermissionUtils.addTrackedAttachment(player);
        switch (event) {
            case SEIKOS_COMET -> grantFireEventPerms(perms);
            case BLUE_MOON_OF_LEIKS -> grantWaterEventPerms(perms);
            case HARMONIC_CONVERGENCE_OF_SACHSI -> grantAirEventPerms(perms);
            case AEAROSTS_METEORITE -> grantEarthEventPerms(perms);
            default -> {}
        }
        PermissionUtils.updateSubElements(player);
    }

    private void grantFireEventPerms(PermissionAttachment perms) {
        perms.setPermission("bending.fire.combustionbending", true);
        perms.setPermission("bending.fire.lightningbending", true);
        perms.setPermission("bending.ability.barrage", true);
        perms.setPermission("bending.ability.combustionstrike", true);
        perms.setPermission("bending.ability.Combustion", true);
        perms.setPermission("bending.ability.Discharge", true);
        perms.setPermission("bending.ability.Lightning", true);
        perms.setPermission("bending.ability.jolt", true);
        perms.setPermission("bending.ability.static", true);
        perms.setPermission("bending.ability.electricstrike", true);
        perms.setPermission("bending.ability.jetbolt", true);
        perms.setPermission("bending.ability.LightningBurst", true);
        perms.setPermission("bending.ability.Bolt", true);
    }

    private void grantWaterEventPerms(PermissionAttachment perms) {
        perms.setPermission("bending.water.healing", true);
        perms.setPermission("bending.water.plantbending", true);
        perms.setPermission("bending.water.bloodbending", true);
        perms.setPermission("bending.water.bloodbending.anytime", true);
        perms.setPermission("bending.ability.mendingwaters", true);
        perms.setPermission("bending.ability.healinghelix", true);
        perms.setPermission("bending.ability.corruptinghelix", true);
        perms.setPermission("bending.ability.toxicspores", true);
        perms.setPermission("bending.ability.vinewhip", true);
        perms.setPermission("bending.ability.leafscythe", true);
        perms.setPermission("bending.ability.rootsnare", true);
        perms.setPermission("bending.ability.bloodfreeze", true);
        perms.setPermission("bending.ability.disalignment", true);
        perms.setPermission("bending.ability.liferip", true);
        perms.setPermission("bending.ability.BloodPuppet", true);
    }

    private void grantAirEventPerms(PermissionAttachment perms) {
        // No flight - too rank-defining
        perms.setPermission("bending.air.spiritual", true);
        perms.setPermission("bending.air.sound", true);
        perms.setPermission("bending.ability.Meditate", true);
        perms.setPermission("bending.ability.astralprojection", true);
        perms.setPermission("bending.ability.astralshot", true);
        perms.setPermission("bending.ability.angeredspirits", true);
        perms.setPermission("bending.ability.energyburst", true);
        perms.setPermission("bending.ability.sonicboom", true);
        perms.setPermission("bending.ability.sonicpulse", true);
        perms.setPermission("bending.ability.deafeningscream", true);
    }

    private void grantEarthEventPerms(PermissionAttachment perms) {
        perms.setPermission("bending.earth.metalbending", true);
        perms.setPermission("bending.earth.lavabending", true);
        perms.setPermission("bending.ability.MetalShred", true);
        perms.setPermission("bending.ability.MagnetShield", true);
        perms.setPermission("bending.ability.MetalFragments", true);
        perms.setPermission("bending.ability.cablethrash", true);
        perms.setPermission("bending.ability.cableslash", true);
        perms.setPermission("bending.ability.metalblade", true);
        perms.setPermission("bending.ability.metalshots", true);
        perms.setPermission("bending.ability.metalstrips", true);
        perms.setPermission("bending.ability.metalshred", true);
        perms.setPermission("bending.ability.LavaThrow", true);
        perms.setPermission("bending.ability.LavaDisc", true);
        perms.setPermission("bending.ability.MagmaBlast", true);
        perms.setPermission("bending.ability.LavaFlux", true);
        perms.setPermission("bending.ability.Fissure", true);
        perms.setPermission("bending.ability.magmawave", true);
        perms.setPermission("bending.ability.magmaglaives", true);
        perms.setPermission("bending.ability.moltenblast", true);
        perms.setPermission("bending.ability.eruption", true);
    }

    /**
     * Attempts to apply a sparse Blindness effect to players during Lunaris Obscura night.
     * Each player can be blinded at most 5 times per in-game day, for 5-10 seconds each.
     * Called every 100 ticks (5 seconds) from DateUtils.
     */
    public void tryApplyLunarisObscuraBlindness() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            if (!AranarthUtils.isSurvivalWorld(worldName) || worldName.equals("spawn") || worldName.equals("shops")) {
                continue;
            }
            long time = player.getWorld().getTime();
            if (time < 12300 || time > 23960) {
                continue;
            }
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)) {
                continue;
            }
            int count = nightBlindnessCounts.getOrDefault(player.getUniqueId(), 0);
            if (count >= 5) {
                continue;
            }
            if (random.nextInt(20) == 0) { // ~5% chance per 5-second tick
                int duration = 100 + random.nextInt(101); // 5-10 seconds (100-200 ticks)
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS, duration, 0));
                nightBlindnessCounts.put(player.getUniqueId(), count + 1);
            }
        }
    }

    /**
     * Resets the Lunaris Obscura blindness counter for all players at the start of each new in-game day.
     */
    public void resetNightBlindness() {
        nightBlindnessCounts.clear();
    }

    /**
     * Blocks bed entry during Lunaris (all tiers) at nighttime.
     */
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        WorldEvent active = AranarthUtils.getActiveWorldEvent();
        if (active != WorldEvent.LUNARIS) {
            return;
        }
        long time = e.getPlayer().getWorld().getTime();
        if (time < 12300 || time > 23960) {
            return;
        }
        int intensity = AranarthUtils.getActiveWorldEventIntensity();
        String eventName = active.getName(intensity);
        e.setUseBed(PlayerBedEnterEvent.Result.DENY);
        e.getPlayer().sendMessage(ChatUtils.chatMessage("&9" + eventName + " &7- this night cannot be shortened."));
    }
}
