package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.event.mob.MountListener;
import com.aearost.aranarthcore.objects.Mount;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.Element;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Sniffer;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central manager and utility class for all player mount data and operations.
 */
public class MountUtils {

    private MountUtils() {
    }

    public static final double SPEED_XP_PER_BLOCK = 0.1;
    public static final long HEALTH_XP_PER_HALF_HEART = 1;
    public static final long RAM_XP_PER_HIT = 3;
    private static final long SKILL_BAR_DURATION_TICKS = 200L; // 10 seconds
    public static final int BAR_HEALTH = 0;
    public static final int BAR_SPEED = 1;
    public static final int BAR_THIRD = 2;
    private static final Map<UUID, Map<String, Mount>> mounts = new HashMap<>();
    private static final Map<UUID, String[]> activeMounts = new HashMap<>();
    private static final Map<UUID, UUID> playerToMount = new HashMap<>();
    private static final Map<UUID, Double> speedAccumulator = new HashMap<>();
    private static final Map<UUID, BossBar[]> playerSkillBars = new HashMap<>();
    private static final Map<UUID, BukkitTask[]> playerSkillBarTasks = new HashMap<>();

    /**
     * Toggles the player's current elemental mount: dismisses it if active, otherwise summons it.
     */
    public static void toggleMount(Player player) {
        String element = getElementForPlayer(player);
        if (element == null) {
            player.sendMessage(ChatUtils.chatMessage(
                    "&cYou must have an element in order to summon a mount!"));
            return;
        }

        UUID existingMountId = getActiveMountEntityUUID(player.getUniqueId());
        if (existingMountId != null && Bukkit.getEntity(existingMountId) != null) {
            dismissMount(player, existingMountId);
        } else {
            summonMount(player, element);
        }
    }

    /**
     * Summons the player's mount for the given element.
     */
    public static void summonMount(Player player, String element) {
        String worldName = player.getWorld().getName();
        if (worldName.equals("spawn") || worldName.equals("arena") || worldName.equals("creative")) {
            player.sendMessage(ChatUtils.chatMessage("&cYou cannot summon a mount in this world!"));
            return;
        }

        if (getEntityClassForElement(element) == null) {
            player.sendMessage(ChatUtils.chatMessage(
                    getElementColor(element) + getMountNameForElement(element)
                            + " &7has not been implemented yet"));
            return;
        }

        Mount mount = getOrCreate(player.getUniqueId(), element);

        if (mount.isRecharging()) {
            String displayName = getDisplayName(player.getUniqueId(), element);
            player.sendMessage(ChatUtils.chatMessage(
                    getElementColor(element) + displayName
                            + " &7is still recovering! Ready in &e"
                            + formatRechargeTime(mount.getRechargeRemainingSeconds())));
            return;
        }

        if (player.getVehicle() != null) {
            player.sendMessage(ChatUtils.chatMessage("&cYou must exit this vehicle before calling your mount"));
            return;
        }

        MountListener ml = MountListener.getInstance();
        if (ml == null) {
            player.sendMessage(ChatUtils.chatMessage(
                    "&cSomething isn't working as expected with summoning mounts..."));
            return;
        }

        ml.summonMount(player, element, mount);
    }

    /**
     * Dismisses the player's active mount, saving its current HP and removing the entity.
     *
     * @param player  The owning player.
     * @param mountId UUID of the active mount entity.
     */
    public static void dismissMount(Player player, UUID mountId) {
        Entity entity = Bukkit.getEntity(mountId);
        if (!(entity instanceof LivingEntity mount)) {
            unregisterActive(mountId);
            player.sendMessage(ChatUtils.chatMessage("&cYour mount could not be found"));
            return;
        }

        String[] info = getActiveMountInfo(mountId);
        if (info != null) {
            Mount pet = get(player.getUniqueId(), info[1]);
            if (pet != null) {
                pet.setCurrentHealth(mount.getHealth());
            }
        }

        MountListener ml = MountListener.getInstance();
        if (ml != null) {
            ml.cleanupMountPublic(mountId);
        }

        mount.eject();
        mount.remove();

        String displayName = info != null
                ? getDisplayName(player.getUniqueId(), info[1])
                : "mount";
        String color = info != null ? getElementColor(info[1]) : "&f";
        player.sendMessage(ChatUtils.chatMessage(color + displayName + " &7has returned"));
    }

    public static void showAllSkillBars(Player player, String element) {
        refreshSkillBar(player, element, BAR_HEALTH);
        refreshSkillBar(player, element, BAR_SPEED);
        refreshSkillBar(player, element, BAR_THIRD);
    }

    /**
     * Updates a single skill bar's content and makes it visible to the player.
     */
    public static void refreshSkillBar(Player player, String element, int barIndex) {
        UUID playerUUID = player.getUniqueId();
        Mount pet = get(playerUUID, element);
        if (pet == null) {
            return;
        }

        BossBar[] bars = playerSkillBars.computeIfAbsent(playerUUID, k -> new BossBar[]{
                Bukkit.createBossBar("", elementBarColor(element), BarStyle.SOLID),
                Bukkit.createBossBar("", elementBarColor(element), BarStyle.SOLID),
                Bukkit.createBossBar("", elementBarColor(element), BarStyle.SOLID)
        });

        BossBar bar = bars[barIndex];
        // Keep color correct if player changed element since bars were first created
        bar.setColor(elementBarColor(element));

        // Resolve skill stats for this slot
        int level;
        long xp, needed;
        if (barIndex == BAR_HEALTH) {
            level = pet.getHealthLevel();
            xp = pet.getHealthXp();
            needed = pet.xpNeededForNextHealthLevel();
        } else if (barIndex == BAR_SPEED) {
            level = pet.getSpeedLevel();
            xp = pet.getSpeedXp();
            needed = pet.xpNeededForNextSpeedLevel();
        } else {
            level = pet.getThirdLevel();
            xp = pet.getThirdXp();
            needed = pet.xpNeededForNextThirdLevel();
        }

        double progress = level >= Mount.MAX_LEVEL ? 1.0
                : (needed > 0 ? Math.min(1.0, Math.max(0.01, (double) xp / needed)) : 1.0);

        bar.setTitle(buildBarTitle(barIndex, getDisplayName(playerUUID, element),
                getElementColor(element), element, level, xp, needed));
        bar.setProgress(progress);

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }

        // Cancel the pending hide task (if any) and reschedule
        BukkitTask[] tasks = playerSkillBarTasks.computeIfAbsent(playerUUID, k -> new BukkitTask[3]);
        if (tasks[barIndex] != null) {
            try {
                tasks[barIndex].cancel();
            } catch (Exception ignored) {
            }
        }
        tasks[barIndex] = Bukkit.getScheduler().runTaskLater(AranarthCore.getInstance(),
                () -> bar.removePlayer(player), SKILL_BAR_DURATION_TICKS);
    }

    /**
     * Hides all skill bars for the player and cancels their hide timers.
     * Should be called when the player logs out.
     */
    public static void cleanupPlayerBars(UUID playerUUID) {
        BukkitTask[] tasks = playerSkillBarTasks.remove(playerUUID);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                if (task != null) {
                    try {
                        task.cancel();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        BossBar[] bars = playerSkillBars.remove(playerUUID);
        Player player = Bukkit.getPlayer(playerUUID);
        if (bars != null && player != null) {
            for (BossBar bar : bars) {
                if (bar != null) {
                    bar.removePlayer(player);
                }
            }
        }
    }

    /**
     * Builds the boss bar title for a skill slot.
     */
    private static String buildBarTitle(int barIndex, String displayName, String elementColor,
                                        String element, int level, long xp, long needed) {
        String skillLabel = switch (barIndex) {
            case BAR_HEALTH -> "Health";
            case BAR_SPEED -> "Speed";
            default -> getThirdAttrLabel(element);
        };
        String nameAndSkill = elementColor + displayName + "'s " + skillLabel;
        boolean maxed = level >= Mount.MAX_LEVEL;

        return ChatUtils.translateToColor(maxed
                ? nameAndSkill + " &8» &fLvl " + level + " &a(MAX)"
                : nameAndSkill + " &8» &fLvl " + level + " &7(&e" + xp + "&7/&e" + needed + " &eXP&7)");
    }

    private static BarColor elementBarColor(String element) {
        return switch (element) {
            case "EARTH" -> BarColor.GREEN;
            case "FIRE" -> BarColor.RED;
            case "WATER" -> BarColor.BLUE;
            default -> BarColor.WHITE;
        };
    }

    /**
     * Full label for the third skill attribute of each element's mount.
     */
    public static String getThirdAttrLabel(String element) {
        return switch (element) {
            case "EARTH" -> "Dig Speed";
            case "FIRE" -> "Ramming";
            case "WATER" -> "Bite Strength";
            case "AIR" -> "Bellow Power";
            default -> "Special";
        };
    }

    /**
     * Returns the primary bending element name for the player.
     */
    @Nullable
    public static String getElementForPlayer(Player player) {
        try {
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
            if (bPlayer == null) {
                return null;
            }
            for (Element element : bPlayer.getElements()) {
                if (element instanceof Element.SubElement) {
                    continue;
                }
                if (element == Element.CHI) {
                    continue;
                }
                String name = element.getName().toUpperCase();
                if (name.equals("AIR") || name.equals("WATER")
                        || name.equals("EARTH") || name.equals("FIRE")) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static Class<? extends LivingEntity> getEntityClassForElement(String element) {
        return switch (element) {
            case "EARTH" -> Sniffer.class;
            case "FIRE" -> Ravager.class;
            case "AIR" -> HappyGhast.class;
            case "WATER" -> PolarBear.class;
            default -> null;
        };
    }

    public static String getMountNameForElement(String element) {
        return switch (element) {
            case "EARTH" -> "Badger Mole";
            case "FIRE" -> "Komodo Rhino";
            case "AIR" -> "Flying Bison";
            case "WATER" -> "Polar Bear Dog";
            default -> "Unknown";
        };
    }

    /**
     * Returns the player's nickname for this element's mount, or the default if none was set.
     */
    public static String getDisplayName(UUID playerUUID, String element) {
        Mount mount = get(playerUUID, element);
        if (mount != null && mount.hasNickname()) {
            return mount.getNickname();
        }
        return getMountNameForElement(element);
    }

    public static String getElementColor(String element) {
        return switch (element) {
            case "AIR" -> "&7";
            case "WATER" -> "&b";
            case "EARTH" -> "&a";
            case "FIRE" -> "&c";
            default -> "&f";
        };
    }

    public static String formatElement(String element) {
        if (element == null || element.isEmpty()) {
            return "";
        }
        return element.charAt(0) + element.substring(1).toLowerCase();
    }

    public static Mount getOrCreate(UUID playerUUID, String element) {
        mounts.computeIfAbsent(playerUUID, k -> new HashMap<>());
        return mounts.get(playerUUID).computeIfAbsent(element, k -> new Mount());
    }

    @Nullable
    public static Mount get(UUID playerUUID, String element) {
        Map<String, Mount> map = mounts.get(playerUUID);
        return map == null ? null : map.get(element);
    }

    public static void put(UUID playerUUID, String element, Mount mount) {
        mounts.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(element, mount);
    }

    public static Map<UUID, Map<String, Mount>> getAllMounts() {
        return mounts;
    }

    /**
     * Registers a freshly-spawned mount entity as active.
     * Must be called immediately after the entity is added to the world.
     */
    public static void registerActive(UUID mountEntityUUID, UUID playerUUID, String element) {
        activeMounts.put(mountEntityUUID, new String[]{playerUUID.toString(), element});
        speedAccumulator.put(mountEntityUUID, 0.0);
        playerToMount.put(playerUUID, mountEntityUUID);
    }

    /**
     * Removes a mount from all active-tracking maps.
     */
    public static void unregisterActive(UUID mountEntityUUID) {
        String[] info = activeMounts.remove(mountEntityUUID);
        speedAccumulator.remove(mountEntityUUID);
        if (info != null) {
            try {
                playerToMount.remove(UUID.fromString(info[0]));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static boolean isActiveMount(UUID mountEntityUUID) {
        return activeMounts.containsKey(mountEntityUUID);
    }

    /**
     * Returns true if the mount entity with the given UUID is currently in water.
     */
    public static boolean isMountInWater(UUID mountEntityUUID) {
        Entity e = Bukkit.getEntity(mountEntityUUID);
        return e instanceof LivingEntity le && le.isInWater();
    }

    @Nullable
    public static String[] getActiveMountInfo(UUID mountEntityUUID) {
        return activeMounts.get(mountEntityUUID);
    }

    @Nullable
    public static UUID getActiveMountEntityUUID(UUID playerUUID) {
        return playerToMount.get(playerUUID);
    }

    /**
     * Accumulates speed XP for a moving mount and refreshes the speed skill bar.
     * Should be called every tick the mount is travelling (from the movement loop).
     *
     * @param mountEntityUUID UUID of the mount entity.
     * @param blocksThisTick  Number of blocks the mount moved this tick (its speed attribute).
     */
    public static void accumulateSpeedXp(UUID mountEntityUUID, double blocksThisTick) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        double acc = speedAccumulator.getOrDefault(mountEntityUUID, 0.0)
                + blocksThisTick * SPEED_XP_PER_BLOCK;
        long whole = (long) acc;
        speedAccumulator.put(mountEntityUUID, acc - whole);
        if (whole <= 0) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        boolean leveled = pet.addSpeedXp(whole);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_SPEED);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eSpeed" + getElementColor(info[1])
                                + " has reached level &e" + pet.getSpeedLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Awards health XP when the mount absorbs damage and refreshes the health skill bar.
     *
     * @param mountEntityUUID UUID of the mount entity.
     * @param damage          Raw damage in half-hearts.
     */
    public static void addHealthXp(UUID mountEntityUUID, double damage) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        if (!Double.isFinite(damage) || damage <= 0) {
            return;
        }
        long xp = Math.max(1, Math.min((long) damage, 200L)) * HEALTH_XP_PER_HALF_HEART;
        boolean leveled = pet.addHealthXp(xp);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_HEALTH);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eHealth" + getElementColor(info[1])
                                + " has reached level &e" + pet.getHealthLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Awards ram XP when the Komodo Rhino mount successfully hits an entity.
     */
    public static void addRamXp(UUID mountEntityUUID) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        boolean leveled = pet.addThirdXp(RAM_XP_PER_HIT);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_THIRD);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eRamming" + getElementColor(info[1])
                                + " has reached level &e" + pet.getThirdLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Awards dig XP when the Badger Mole mount breaks blocks while tunneling.
     *
     * @param mountEntityUUID UUID of the mount entity.
     * @param xp              Amount of XP to award (typically 1 per 25-block threshold reached).
     */
    public static void addDigXp(UUID mountEntityUUID, long xp) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        boolean leveled = pet.addThirdXp(xp);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_THIRD);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eDig Speed" + getElementColor(info[1])
                                + " has reached level &e" + pet.getThirdLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Awards Bellow XP when the Flying Bison's roar hits entities.
     *
     * @param mountEntityUUID UUID of the mount entity.
     * @param xp              Total XP to award (base + per-hit bonus, pre-computed by caller).
     */
    public static void addBellowXp(UUID mountEntityUUID, int xp) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        boolean leveled = pet.addThirdXp(xp);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_THIRD);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eBellow Power" + getElementColor(info[1])
                                + " has reached level &e" + pet.getThirdLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Awards Bite XP when the Polar Bear Dog's lunge connects with an entity.
     *
     * @param mountEntityUUID UUID of the mount entity.
     * @param xp              Total XP to award (base + per-hit bonus, pre-computed by caller).
     */
    public static void addBiteXp(UUID mountEntityUUID, int xp) {
        String[] info = activeMounts.get(mountEntityUUID);
        if (info == null) {
            return;
        }

        UUID ownerUUID = parseUUID(info[0]);
        if (ownerUUID == null) {
            return;
        }
        Mount pet = get(ownerUUID, info[1]);
        if (pet == null) {
            return;
        }

        boolean leveled = pet.addThirdXp(xp);
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            refreshSkillBar(owner, info[1], BAR_THIRD);
            if (leveled) {
                owner.sendMessage(ChatUtils.chatMessage(
                        getElementColor(info[1]) + getDisplayName(ownerUUID, info[1])
                                + "'s &eBite Strength" + getElementColor(info[1])
                                + " has reached level &e" + pet.getThirdLevel()));
                MountListener ml = MountListener.getInstance();
                if (ml != null) {
                    ml.updateMountStats(mountEntityUUID);
                }
            }
        }
    }

    /**
     * Formats a duration in seconds as "4m 30s" or "45s".
     */
    public static String formatRechargeTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }

    /**
     * Writes the current health of every active (summoned) mount.
     */
    public static void syncAllActiveHealthToData() {
        for (Map.Entry<UUID, String[]> entry : new HashMap<>(activeMounts).entrySet()) {
            UUID mountId = entry.getKey();
            String[] info = entry.getValue();
            Entity entity = Bukkit.getEntity(mountId);
            if (entity instanceof LivingEntity le && !le.isDead()) {
                UUID ownerUUID = parseUUID(info[0]);
                if (ownerUUID == null) {
                    continue;
                }
                Mount mount = get(ownerUUID, info[1]);
                if (mount != null) {
                    mount.setCurrentHealth(le.getHealth());
                }
            }
        }
    }

    @Nullable
    private static UUID parseUUID(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
