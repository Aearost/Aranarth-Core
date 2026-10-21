package com.aearost.aranarthcore.event.listener.grouped;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.items.aranarthium.ingots.AranarthiumIngot;
import com.aearost.aranarthcore.items.essence.*;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.CustomKeys;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.api.ItemSpawnReason;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;
import com.gmail.nossr50.events.items.McMMOItemSpawnEvent;
import com.gmail.nossr50.events.skills.repair.McMMOPlayerRepairCheckEvent;
import com.gmail.nossr50.events.skills.salvage.McMMOPlayerSalvageCheckEvent;
import com.gmail.nossr50.util.ItemUtils;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.aearost.aranarthcore.objects.CustomKeys.*;

/**
 * Handles mcMMO-specific restrictions and interactions.
 */
public class McMMOListener implements Listener {

    private static final double[] EXTRACT_ANY_CHANCES = {2, 10, 25, 50, 75, 100, 100, 100, 100, 100, 100};
    private static final double[] EXTRACT_APPLIED_CHANCES = {0, 0, 1, 5, 10, 20, 30, 45, 60, 80, 100};
    private final Map<UUID, List<ItemStack>> pendingSalvageExtractions = new HashMap<>();

    public McMMOListener(AranarthCore plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Prevents mcMMO Repair from being used on Aranarthium armor.
     */
    @EventHandler
    public void onRepairCheck(McMMOPlayerRepairCheckEvent e) {
        ItemStack repairedObject = e.getRepairedObject();
        if (repairedObject != null && repairedObject.getItemMeta() != null
                && repairedObject.getItemMeta().getPersistentDataContainer().has(CustomKeys.ARMOR_TYPE, PersistentDataType.STRING)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "aranarthium.cannot_repair")));
        }
    }

    /**
     * Prevents mcMMO Salvage from being used on Aranarthium armor.
     */
    @EventHandler
    public void onSalvageCheck(McMMOPlayerSalvageCheckEvent e) {
        ItemStack salvageItem = e.getSalvageItem();
        if (salvageItem != null && salvageItem.getItemMeta() != null
                && salvageItem.getItemMeta().getPersistentDataContainer().has(CustomKeys.ARMOR_TYPE, PersistentDataType.STRING)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatUtils.chatMessage(Lang.getFor(e.getPlayer(), "aranarthium.cannot_salvage")));
        }
    }

    /**
     * Rolls whether the essence and Aranarthium Ingots applied to a salvaged item are extracted, based on Salvage level.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSalvageExtractEssence(McMMOPlayerSalvageCheckEvent e) {
        Player player = e.getPlayer();
        pendingSalvageExtractions.remove(player.getUniqueId());
        if (e.isCancelled()) {
            return;
        }

        ItemStack salvageItem = e.getSalvageItem();
        if (salvageItem == null || !salvageItem.hasItemMeta()) {
            return;
        }
        PersistentDataContainer pdc = salvageItem.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ESSENCE_TYPE, PersistentDataType.STRING)) {
            return;
        }

        String essenceType = pdc.get(ESSENCE_TYPE, PersistentDataType.STRING);
        Essence essence = getEssence(essenceType);
        if (essence == null) {
            return;
        }

        McMMOPlayer mcMMOPlayer = UserManager.getPlayer(player);
        if (mcMMOPlayer == null) {
            return;
        }

        int salvageLevel = mcMMOPlayer.getSkillLevel(PrimarySkillType.SALVAGE);
        double extractChance = getExtractionChance(salvageLevel, EXTRACT_ANY_CHANCES);
        double appliedChance = getExtractionChance(salvageLevel, EXTRACT_APPLIED_CHANCES);

        Integer levelOnItem = pdc.get(ESSENCE_LEVEL, PersistentDataType.INTEGER);
        int level = levelOnItem == null ? 1 : levelOnItem;

        // A partially used Preservation essence can only be extracted as a blank Essence, otherwise its uses could be refreshed
        boolean canExtractApplied = true;
        if (essenceType.equals("essence_preservation")) {
            Integer uses = pdc.get(PRESERVATION_USES, PersistentDataType.INTEGER);
            canExtractApplied = uses != null && uses >= 3;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<ItemStack> extracted = new ArrayList<>();
        for (int i = 0; i < level; i++) {
            double roll = random.nextDouble(100);
            if (roll < appliedChance && canExtractApplied) {
                extracted.add(essence.getItem());
            } else if (roll < extractChance) {
                extracted.add(new EssenceBlank().getItem());
            }
            // Each application of these essences consumed an Aranarthium Ingot, which is as rare to extract as the applied essence
            if (requiresAranarthiumIngot(essenceType) && random.nextDouble(100) < appliedChance) {
                extracted.add(new AranarthiumIngot().getItem());
            }
        }

        if (!extracted.isEmpty()) {
            pendingSalvageExtractions.put(player.getUniqueId(), extracted);
        }
    }

    /**
     * Spawns the extracted essences and Aranarthium Ingots at the same location as the salvaged enchanted book.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSalvageMaterialsSpawn(McMMOItemSpawnEvent e) {
        if (e.getItemSpawnReason() != ItemSpawnReason.SALVAGE_MATERIALS || e.getPlayer() == null) {
            return;
        }

        Player player = e.getPlayer();
        List<ItemStack> extracted = pendingSalvageExtractions.remove(player.getUniqueId());
        if (extracted == null) {
            return;
        }

        // Mirrors the speed mcMMO uses when spawning salvaged items towards the player
        Location spawnLocation = e.getLocation();
        Location playerLocation = player.getLocation();
        double distance = spawnLocation.getWorld() == playerLocation.getWorld() ? spawnLocation.distance(playerLocation) : 0;
        double speed = Math.min(0.6, Math.max(0.3, distance * 0.2));
        for (ItemStack item : extracted) {
            ItemUtils.spawnItemTowardsLocation(player, spawnLocation.clone(), playerLocation.clone(), item, speed, ItemSpawnReason.SALVAGE_ENCHANTMENT_BOOK);
        }
    }

    /**
     * Clears any unclaimed salvage extractions when the player leaves.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        pendingSalvageExtractions.remove(e.getPlayer().getUniqueId());
    }

    /**
     * Provides the chance at the input Salvage level, interpolated linearly between every 100 levels.
     *
     * @param salvageLevel The Salvage level.
     * @param chances      The chances at levels 0, 100, 200, ..., 1000.
     * @return The chance as a percentage.
     */
    private double getExtractionChance(int salvageLevel, double[] chances) {
        int maxLevel = (chances.length - 1) * 100;
        if (salvageLevel <= 0) {
            return chances[0];
        }
        if (salvageLevel >= maxLevel) {
            return chances[chances.length - 1];
        }
        int index = salvageLevel / 100;
        double progress = (salvageLevel % 100) / 100.0;
        return chances[index] + (chances[index + 1] - chances[index]) * progress;
    }

    /**
     * Provides the essence of the input essence type.
     *
     * @param essenceType The essence type, i.e "essence_beheading".
     * @return The essence, or null if the essence type is unknown.
     */
    private Essence getEssence(String essenceType) {
        return switch (essenceType) {
            case "essence_beheading" -> new EssenceBeheading();
            case "essence_lifesteal" -> new EssenceLifesteal();
            case "essence_magnetism" -> new EssenceMagnetism();
            case "essence_plentiful" -> new EssencePlentiful();
            case "essence_preservation" -> new EssencePreservation();
            case "essence_resilience" -> new EssenceResilience();
            default -> null;
        };
    }

    /**
     * Determines if applying the essence type consumes an Aranarthium Ingot.
     *
     * @param essenceType The essence type, i.e "essence_beheading".
     * @return Whether applying the essence type consumes an Aranarthium Ingot.
     */
    private boolean requiresAranarthiumIngot(String essenceType) {
        return switch (essenceType) {
            case "essence_lifesteal", "essence_magnetism", "essence_plentiful", "essence_preservation" -> true;
            default -> false;
        };
    }

    /**
     * Sends a custom cross-server broadcast when a player reaches a power level or
     * individual skill level milestone (every 100 levels).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMcmmoLevelUp(McMMOPlayerLevelUpEvent e) {
        Player player = e.getPlayer();
        AranarthPlayer aranarthPlayer = AranarthUtils.getPlayer(player.getUniqueId());
        String displayName = aranarthPlayer != null ? aranarthPlayer.getNickname() : player.getName();

        int skillLevel = e.getSkillLevel();
        if (skillLevel % 100 == 0) {
            String skillName = e.getSkill().getName();
            Lang.broadcast("mcmmo.skill_level", "player", displayName, "level", skillLevel, "skill", skillName);
        }

        int powerLevel = ExperienceAPI.getPowerLevel(player);
        if (powerLevel % 100 == 0) {
            Lang.broadcast("mcmmo.power_level", "player", displayName, "level", powerLevel);
        }
    }

}
