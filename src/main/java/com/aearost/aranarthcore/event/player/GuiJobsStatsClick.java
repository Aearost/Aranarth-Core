package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuiJobsStatsClick {

    // Slots matching the active jobs shown in GuiJobsStats
    private static final int[] JOB_SLOTS = {10, 12, 14, 19, 21, 23, 28, 30, 32};

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            new GuiJobs(player).openGui();
            return;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();
        List<JobType> activeJobs = jobData.getActiveJobs();

        for (int i = 0; i < JOB_SLOTS.length && i < activeJobs.size(); i++) {
            if (JOB_SLOTS[i] == slot) {
                JobType job = activeJobs.get(i);
                sendStatsToChat(player, job, jobData);
                return;
            }
        }
    }

    public static void sendStatsToChat(Player player, JobType job, JobData jobData) {
        int level = jobData.getLevel(job);
        double currentXp = jobData.getCurrentXp(job);
        long required = JobUtils.getXpRequired(level);
        String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " &8/ &f" + required;

        player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &eStatistics - &6" + job.getDisplayName() + " &7(Level " + level + ")"));
        player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &7XP: &f" + xpStr));
        player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8]"));
        player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] &e--- Actions ---"));

        double multiplier = JobUtils.getLevelMultiplier(level);
        for (String line : getActionLines(job, multiplier)) {
            player.sendMessage(ChatUtils.translateToColor("&8[&6Jobs&8] " + line));
        }
    }

    private static List<String> getActionLines(JobType job, double multiplier) {
        return switch (job) {
            case MINER -> List.of(
                "&7 Break:",
                "&f  Stone / Andesite / Granite / Diorite - &a" + fmt(0.03, multiplier),
                "&f  Deepslate / Cobblestone - &a" + fmt(0.03, multiplier),
                "&f  Tuff / Calcite / Basalt / Netherrack - &a" + fmt(0.02, multiplier),
                "&f  Copper Ore - &a" + fmt(0.04, multiplier),
                "&f  Coal Ore - &a" + fmt(0.04, multiplier),
                "&f  Iron Ore - &a" + fmt(0.08, multiplier),
                "&f  Gold Ore - &a" + fmt(0.12, multiplier),
                "&f  Lapis Ore - &a" + fmt(0.06, multiplier),
                "&f  Redstone Ore - &a" + fmt(0.05, multiplier),
                "&f  Nether Quartz Ore - &a" + fmt(0.05, multiplier),
                "&f  Emerald Ore - &a" + fmt(0.80, multiplier),
                "&f  Diamond Ore - &a" + fmt(0.50, multiplier),
                "&f  Ancient Debris - &a" + fmt(3.00, multiplier),
                "&f  Amethyst Cluster - &a" + fmt(0.10, multiplier)
            );
            case FARMER -> List.of(
                "&7 Harvest:",
                "&f  Wheat - &a" + fmt(0.10, multiplier),
                "&f  Carrots / Potatoes - &a" + fmt(0.05, multiplier),
                "&f  Beetroot - &a" + fmt(0.12, multiplier),
                "&f  Nether Wart - &a" + fmt(0.15, multiplier),
                "&f  Cocoa Beans - &a" + fmt(0.10, multiplier),
                "&f  Melon / Pumpkin - &a" + fmt(0.20, multiplier),
                "&f  Sweet Berries - &a" + fmt(0.08, multiplier),
                "&f  Glow Berries - &a" + fmt(0.06, multiplier),
                "&f  Sugarcane / Cactus - &a" + fmt(0.04, multiplier),
                "&f  Collect Honey (bottle) - &a" + fmt(0.50, multiplier),
                "&f  Collect Honeycomb (shears) - &a" + fmt(0.40, multiplier),
                "&f  Kill Passive Mob - &a" + fmt(0.10, multiplier)
            );
            case EXCAVATOR -> List.of(
                "&7 Dig:",
                "&f  Dirt / Coarse Dirt / Rooted Dirt - &a" + fmt(0.02, multiplier),
                "&f  Podzol / Mycelium - &a" + fmt(0.02, multiplier),
                "&f  Sand / Red Sand - &a" + fmt(0.02, multiplier),
                "&f  Gravel - &a" + fmt(0.02, multiplier),
                "&f  Clay - &a" + fmt(0.04, multiplier),
                "&f  Mud - &a" + fmt(0.02, multiplier),
                "&f  Soul Sand / Soul Soil - &a" + fmt(0.03, multiplier),
                "&f  Snow Block - &a" + fmt(0.02, multiplier),
                "&f  Brush Suspicious Sand - &a" + fmt(2.50, multiplier),
                "&f  Brush Suspicious Gravel - &a" + fmt(2.50, multiplier)
            );
            case LUMBERJACK -> List.of(
                "&7 Chop:",
                "&f  Log (any type) - &a" + fmt(0.08, multiplier),
                "&f  Stripped Log - &a" + fmt(0.05, multiplier),
                "&f  Bamboo Block - &a" + fmt(0.04, multiplier),
                "&f  Mushroom Block - &a" + fmt(0.06, multiplier),
                "&f  Leaves - &a" + fmt(0.01, multiplier),
                "&7 Craft:",
                "&f  Planks (per plank) - &a" + fmt(0.02, multiplier),
                "&f  Stairs (per stair) - &a" + fmt(0.04, multiplier),
                "&f  Slabs (per slab) - &a" + fmt(0.02, multiplier),
                "&f  Door - &a" + fmt(0.08, multiplier),
                "&f  Trapdoor - &a" + fmt(0.06, multiplier),
                "&f  Fence / Gate - &a" + fmt(0.05, multiplier),
                "&f  Chiseled Bookshelf - &a" + fmt(0.10, multiplier)
            );
            case BUILDER -> List.of(
                "&7 Place:",
                "&f  Any buildable block - &a" + fmt(0.10, multiplier)
            );
            case SMITH -> List.of(
                "&7 Craft:",
                "&f  Iron Tool - &a" + fmt(0.50, multiplier),
                "&f  Iron Armor - &a" + fmt(0.60, multiplier),
                "&f  Gold Tool - &a" + fmt(0.35, multiplier),
                "&f  Gold Armor - &a" + fmt(0.40, multiplier),
                "&f  Diamond Tool - &a" + fmt(2.00, multiplier),
                "&f  Diamond Armor - &a" + fmt(2.50, multiplier),
                "&f  Chainmail Armor - &a" + fmt(0.80, multiplier),
                "&f  Copper Block - &a" + fmt(0.15, multiplier),
                "&f  Iron Block - &a" + fmt(0.25, multiplier),
                "&f  Gold Block - &a" + fmt(0.30, multiplier),
                "&f  Chain / Lantern / Iron Bars - &a" + fmt(0.20, multiplier),
                "&7 Smith:",
                "&f  Netherite Upgrade - &a" + fmt(5.00, multiplier),
                "&f  Armor Trim - &a" + fmt(2.00, multiplier)
            );
            case EXPLORER -> List.of(
                "&7 Travel:",
                "&f  Walk (per block) - &a" + fmt(0.003, multiplier),
                "&f  Ride Horse (per block) - &a" + fmt(0.004, multiplier),
                "&7 Discover:",
                "&f  Open Overworld Chest - &a" + fmt(3.00, multiplier),
                "&f  Open Nether Chest - &a" + fmt(5.00, multiplier),
                "&f  Open End Chest - &a" + fmt(8.00, multiplier)
            );
            case ALCHEMIST -> List.of(
                "&7 Brew:",
                "&f  Vanilla Potion (per bottle) - &a" + fmt(0.80, multiplier),
                "&f  BreweryX Alcohol (per bottle) - &a" + fmt(1.00, multiplier),
                "&7 Enchant:",
                "&f  Enchanting Table - &a" + fmt(0.50, multiplier),
                "&f  Apply Enchanted Book (Anvil) - &a" + fmt(0.40, multiplier),
                "&f  Disenchant (Grindstone) - &a" + fmt(0.30, multiplier),
                "&f  Rename (Anvil) - &a" + fmt(0.10, multiplier)
            );
            case HUNTER -> List.of(
                "&7 Kill:",
                "&f  Zombie - &a" + fmt(0.20, multiplier),
                "&f  Skeleton / Stray - &a" + fmt(0.25, multiplier),
                "&f  Spider / Cave Spider - &a" + fmt(0.20, multiplier),
                "&f  Creeper - &a" + fmt(0.35, multiplier),
                "&f  Witch - &a" + fmt(0.50, multiplier),
                "&f  Blaze - &a" + fmt(0.80, multiplier),
                "&f  Ghast - &a" + fmt(1.00, multiplier),
                "&f  Enderman - &a" + fmt(0.15, multiplier),
                "&f  Piglin / Zombified Piglin - &a" + fmt(0.20, multiplier),
                "&f  Wither Skeleton - &a" + fmt(1.50, multiplier),
                "&f  Guardian / Elder Guardian - &a" + fmt(0.75, multiplier),
                "&f  Shulker - &a" + fmt(0.60, multiplier),
                "&f  Breeze - &a" + fmt(1.20, multiplier),
                "&f  Warden - &a" + fmt(15.00, multiplier),
                "&f  Passive Mob - &a" + fmt(0.10, multiplier),
                "&f  Player - &a" + fmt(5.00, multiplier),
                "&7 Fish:",
                "&f  Cod / Salmon - &a" + fmt(0.20, multiplier),
                "&f  Pufferfish - &a" + fmt(0.35, multiplier),
                "&f  Tropical Fish - &a" + fmt(0.25, multiplier),
                "&f  Treasure Item - &a" + fmt(1.50, multiplier)
            );
        };
    }

    private static String fmt(double base, double multiplier) {
        double actual = base * multiplier;
        return String.format("$%.3f", actual);
    }
}
