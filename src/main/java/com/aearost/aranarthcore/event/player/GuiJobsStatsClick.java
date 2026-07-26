package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsJoin;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.JobUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class GuiJobsStatsClick {

    public void execute(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        int slot = e.getRawSlot();

        if (slot == 40) {
            player.closeInventory();
            new GuiJobs(player).openGui();
            return;
        }

        AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
        if (ap == null) return;
        JobData jobData = ap.getJobData();

        int[] slots = GuiJobsJoin.JOB_SLOTS;
        JobType[] jobs = GuiJobsJoin.JOB_ORDER;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                player.closeInventory();
                sendStatsToChat(player, jobs[i], jobData);
                return;
            }
        }
    }

    public static void sendStatsToChat(Player player, JobType job, JobData jobData) {
        int level = jobData.getLevel(job);
        double currentXp = jobData.getCurrentXp(job);
        long required = JobUtils.getXpRequired(level);
        String xpStr = level >= 10 ? "Max Level" : (int) currentXp + " / " + required;

        player.sendMessage(ChatUtils.chatMessage("&e" + job.getDisplayName() + " &7Statistics &8- &7Level &e" + level));
        player.sendMessage(ChatUtils.chatMessage("&7XP: &e" + xpStr));
        player.sendMessage(ChatUtils.chatMessage(""));
        player.sendMessage(ChatUtils.chatMessage("&e--- Actions ---"));

        double multiplier = JobUtils.getLevelMultiplier(level);
        for (String line : getActionLines(job, multiplier)) {
            player.sendMessage(ChatUtils.chatMessage(line));
        }
    }

    private static List<String> getActionLines(JobType job, double multiplier) {
        return switch (job) {
            case MINER -> List.of(
                "&7Break:",
                "&7  Stone / Andesite / Granite / Diorite &8- &e" + fmt(0.03, multiplier),
                "&7  Deepslate / Cobblestone &8- &e" + fmt(0.03, multiplier),
                "&7  Tuff / Calcite / Basalt / Netherrack &8- &e" + fmt(0.02, multiplier),
                "&7  Copper Ore &8- &e" + fmt(0.04, multiplier),
                "&7  Coal Ore &8- &e" + fmt(0.04, multiplier),
                "&7  Iron Ore &8- &e" + fmt(0.08, multiplier),
                "&7  Gold Ore &8- &e" + fmt(0.12, multiplier),
                "&7  Lapis Ore &8- &e" + fmt(0.06, multiplier),
                "&7  Redstone Ore &8- &e" + fmt(0.05, multiplier),
                "&7  Nether Quartz Ore &8- &e" + fmt(0.05, multiplier),
                "&7  Emerald Ore &8- &e" + fmt(0.80, multiplier),
                "&7  Diamond Ore &8- &e" + fmt(0.50, multiplier),
                "&7  Ancient Debris &8- &e" + fmt(3.00, multiplier),
                "&7  Amethyst Cluster &8- &e" + fmt(0.10, multiplier)
            );
            case FARMER -> List.of(
                "&7Harvest:",
                "&7  Wheat &8- &e" + fmt(0.10, multiplier),
                "&7  Carrots / Potatoes &8- &e" + fmt(0.05, multiplier),
                "&7  Beetroot &8- &e" + fmt(0.12, multiplier),
                "&7  Nether Wart &8- &e" + fmt(0.15, multiplier),
                "&7  Cocoa Beans &8- &e" + fmt(0.10, multiplier),
                "&7  Melon / Pumpkin &8- &e" + fmt(0.20, multiplier),
                "&7  Sweet Berries &8- &e" + fmt(0.08, multiplier),
                "&7  Glow Berries &8- &e" + fmt(0.06, multiplier),
                "&7  Sugarcane / Cactus &8- &e" + fmt(0.04, multiplier),
                "&7  Collect Honey (bottle) &8- &e" + fmt(0.50, multiplier),
                "&7  Collect Honeycomb (shears) &8- &e" + fmt(0.40, multiplier),
                "&7  Kill Passive Mob &8- &e" + fmt(0.10, multiplier)
            );
            case EXCAVATOR -> List.of(
                "&7Dig:",
                "&7  Dirt / Coarse Dirt / Rooted Dirt &8- &e" + fmt(0.02, multiplier),
                "&7  Podzol / Mycelium &8- &e" + fmt(0.02, multiplier),
                "&7  Sand / Red Sand &8- &e" + fmt(0.02, multiplier),
                "&7  Gravel &8- &e" + fmt(0.02, multiplier),
                "&7  Clay &8- &e" + fmt(0.04, multiplier),
                "&7  Mud &8- &e" + fmt(0.02, multiplier),
                "&7  Soul Sand / Soul Soil &8- &e" + fmt(0.03, multiplier),
                "&7  Snow Block &8- &e" + fmt(0.02, multiplier),
                "&7  Brush Suspicious Sand &8- &e" + fmt(2.50, multiplier),
                "&7  Brush Suspicious Gravel &8- &e" + fmt(2.50, multiplier)
            );
            case LUMBERJACK -> List.of(
                "&7Chop:",
                "&7  Log (any type) &8- &e" + fmt(0.08, multiplier),
                "&7  Stripped Log &8- &e" + fmt(0.05, multiplier),
                "&7  Bamboo Block &8- &e" + fmt(0.04, multiplier),
                "&7  Mushroom Block &8- &e" + fmt(0.06, multiplier),
                "&7  Leaves &8- &e" + fmt(0.01, multiplier),
                "&7Craft:",
                "&7  Planks (per plank) &8- &e" + fmt(0.02, multiplier),
                "&7  Stairs (per stair) &8- &e" + fmt(0.04, multiplier),
                "&7  Slabs (per slab) &8- &e" + fmt(0.02, multiplier),
                "&7  Door &8- &e" + fmt(0.08, multiplier),
                "&7  Trapdoor &8- &e" + fmt(0.06, multiplier),
                "&7  Fence / Gate &8- &e" + fmt(0.05, multiplier),
                "&7  Chiseled Bookshelf &8- &e" + fmt(0.10, multiplier)
            );
            case BUILDER -> List.of(
                "&7Place:",
                "&7  Any buildable block &8- &e" + fmt(0.10, multiplier)
            );
            case SMITH -> List.of(
                "&7Craft:",
                "&7  Iron Tool &8- &e" + fmt(0.50, multiplier),
                "&7  Iron Armor &8- &e" + fmt(0.60, multiplier),
                "&7  Gold Tool &8- &e" + fmt(0.35, multiplier),
                "&7  Gold Armor &8- &e" + fmt(0.40, multiplier),
                "&7  Diamond Tool &8- &e" + fmt(2.00, multiplier),
                "&7  Diamond Armor &8- &e" + fmt(2.50, multiplier),
                "&7  Chainmail Armor &8- &e" + fmt(0.80, multiplier),
                "&7  Copper Block &8- &e" + fmt(0.15, multiplier),
                "&7  Iron Block &8- &e" + fmt(0.25, multiplier),
                "&7  Gold Block &8- &e" + fmt(0.30, multiplier),
                "&7  Chain / Lantern / Iron Bars &8- &e" + fmt(0.20, multiplier),
                "&7Smith:",
                "&7  Netherite Upgrade &8- &e" + fmt(5.00, multiplier),
                "&7  Armor Trim &8- &e" + fmt(2.00, multiplier)
            );
            case EXPLORER -> List.of(
                "&7Travel:",
                "&7  Walk (per block) &8- &e" + fmt(0.003, multiplier),
                "&7  Ride Horse (per block) &8- &e" + fmt(0.004, multiplier),
                "&7Discover:",
                "&7  Open Overworld Chest &8- &e" + fmt(3.00, multiplier),
                "&7  Open Nether Chest &8- &e" + fmt(5.00, multiplier),
                "&7  Open End Chest &8- &e" + fmt(8.00, multiplier)
            );
            case ALCHEMIST -> List.of(
                "&7Brew:",
                "&7  Vanilla Potion (per bottle) &8- &e" + fmt(0.80, multiplier),
                "&7  BreweryX Alcohol (per bottle) &8- &e" + fmt(1.00, multiplier),
                "&7Enchant:",
                "&7  Enchanting Table &8- &e" + fmt(0.50, multiplier),
                "&7  Apply Enchanted Book (Anvil) &8- &e" + fmt(0.40, multiplier),
                "&7  Disenchant (Grindstone) &8- &e" + fmt(0.30, multiplier),
                "&7  Rename (Anvil) &8- &e" + fmt(0.10, multiplier)
            );
            case HUNTER -> List.of(
                "&7Kill:",
                "&7  Zombie &8- &e" + fmt(0.20, multiplier),
                "&7  Skeleton / Stray &8- &e" + fmt(0.25, multiplier),
                "&7  Spider / Cave Spider &8- &e" + fmt(0.20, multiplier),
                "&7  Creeper &8- &e" + fmt(0.35, multiplier),
                "&7  Witch &8- &e" + fmt(0.50, multiplier),
                "&7  Blaze &8- &e" + fmt(0.80, multiplier),
                "&7  Ghast &8- &e" + fmt(1.00, multiplier),
                "&7  Enderman &8- &e" + fmt(0.15, multiplier),
                "&7  Piglin / Zombified Piglin &8- &e" + fmt(0.20, multiplier),
                "&7  Wither Skeleton &8- &e" + fmt(1.50, multiplier),
                "&7  Guardian / Elder Guardian &8- &e" + fmt(0.75, multiplier),
                "&7  Shulker &8- &e" + fmt(0.60, multiplier),
                "&7  Breeze &8- &e" + fmt(1.20, multiplier),
                "&7  Warden &8- &e" + fmt(15.00, multiplier),
                "&7  Passive Mob &8- &e" + fmt(0.10, multiplier),
                "&7  Player &8- &e" + fmt(5.00, multiplier),
                "&7Fish:",
                "&7  Cod / Salmon &8- &e" + fmt(0.20, multiplier),
                "&7  Pufferfish &8- &e" + fmt(0.35, multiplier),
                "&7  Tropical Fish &8- &e" + fmt(0.25, multiplier),
                "&7  Treasure Item &8- &e" + fmt(1.50, multiplier)
            );
        };
    }

    private static String fmt(double base, double multiplier) {
        double actual = base * multiplier;
        double xp = actual * 100;
        String xpStr = xp == Math.floor(xp) ? String.valueOf((int) xp) : String.format("%.1f", xp);
        return String.format("$%.2f &8(&7%s XP&8)", actual, xpStr);
    }
}
