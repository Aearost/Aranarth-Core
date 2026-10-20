package com.aearost.aranarthcore.event.player;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.gui.GuiJobs;
import com.aearost.aranarthcore.gui.GuiJobsJoin;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import com.aearost.aranarthcore.objects.JobData;
import com.aearost.aranarthcore.utils.AranarthUtils;
import com.aearost.aranarthcore.utils.ChatUtils;
import com.aearost.aranarthcore.utils.Lang;
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
        String xpStr = level >= 10 ? Lang.getFor(player, "jobs.max_level") : (int) currentXp + " / " + required;

        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "jobs.stats_header", "job", Lang.key(job.getNameKey()), "level", level)));
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "jobs.stats_xp", "xp", xpStr)));
        player.sendMessage(ChatUtils.chatMessage(""));
        player.sendMessage(ChatUtils.chatMessage(Lang.getFor(player, "jobs.actions_header")));

        double multiplier = JobUtils.getLevelMultiplier(level);
        for (String line : getActionLines(player, job, multiplier)) {
            player.sendMessage(ChatUtils.chatMessage(line));
        }
    }

    private static List<String> getActionLines(Player player, JobType job, double multiplier) {
        return switch (job) {
            case MINER -> List.of(
                Lang.getFor(player, "jobs.act_header_break"),
                Lang.getFor(player, "jobs.act_stone", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_stone_variants", "xp", fmt(0.07, multiplier)),
                Lang.getFor(player, "jobs.act_dripstone", "xp", fmt(0.07, multiplier)),
                Lang.getFor(player, "jobs.act_sulphur", "xp", fmt(0.07, multiplier)),
                Lang.getFor(player, "jobs.act_netherrack", "xp", fmt(0.03, multiplier)),
                Lang.getFor(player, "jobs.act_copper_ore", "xp", fmt(1.90, multiplier)),
                Lang.getFor(player, "jobs.act_coal_ore", "xp", fmt(1.90, multiplier)),
                Lang.getFor(player, "jobs.act_iron_ore", "xp", fmt(3.85, multiplier)),
                Lang.getFor(player, "jobs.act_gold_ore", "xp", fmt(5.75, multiplier)),
                Lang.getFor(player, "jobs.act_lapis_ore", "xp", fmt(2.90, multiplier)),
                Lang.getFor(player, "jobs.act_redstone_ore", "xp", fmt(2.40, multiplier)),
                Lang.getFor(player, "jobs.act_quartz_ore", "xp", fmt(2.40, multiplier)),
                Lang.getFor(player, "jobs.act_emerald_ore", "xp", fmt(38.00, multiplier)),
                Lang.getFor(player, "jobs.act_diamond_ore", "xp", fmt(24.00, multiplier)),
                Lang.getFor(player, "jobs.act_ancient_debris", "xp", fmt(75.00, multiplier)),
                Lang.getFor(player, "jobs.act_amethyst_cluster", "xp", fmt(4.80, multiplier))
            );
            case FARMER -> List.of(
                Lang.getFor(player, "jobs.act_header_harvest"),
                Lang.getFor(player, "jobs.act_wheat", "xp", fmt(1.20, multiplier)),
                Lang.getFor(player, "jobs.act_carrots", "xp", fmt(0.60, multiplier)),
                Lang.getFor(player, "jobs.act_beetroot", "xp", fmt(1.45, multiplier)),
                Lang.getFor(player, "jobs.act_nether_wart", "xp", fmt(1.80, multiplier)),
                Lang.getFor(player, "jobs.act_melon", "xp", fmt(2.40, multiplier)),
                Lang.getFor(player, "jobs.act_sweet_berries", "xp", fmt(0.95, multiplier)),
                Lang.getFor(player, "jobs.act_glow_berries", "xp", fmt(0.73, multiplier)),
                Lang.getFor(player, "jobs.act_sugarcane", "xp", fmt(1.90, multiplier)),
                Lang.getFor(player, "jobs.act_honey", "xp", fmt(12.00, multiplier)),
                Lang.getFor(player, "jobs.act_honeycomb", "xp", fmt(9.60, multiplier)),
                Lang.getFor(player, "jobs.act_milk", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_mushroom_stew", "xp", fmt(9.60, multiplier)),
                Lang.getFor(player, "jobs.act_shear_sheep", "xp", fmt(6.00, multiplier)),
                Lang.getFor(player, "jobs.act_shear_mooshroom", "xp", fmt(12.00, multiplier)),
                Lang.getFor(player, "jobs.act_kill_passive", "xp", fmt(1.20, multiplier))
            );
            case EXCAVATOR -> List.of(
                Lang.getFor(player, "jobs.act_header_dig"),
                Lang.getFor(player, "jobs.act_dirt", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_podzol", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_sand", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_gravel", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_clay", "xp", fmt(0.10, multiplier)),
                Lang.getFor(player, "jobs.act_mud", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_soul_sand", "xp", fmt(0.07, multiplier)),
                Lang.getFor(player, "jobs.act_snow_block", "xp", fmt(0.05, multiplier)),
                Lang.getFor(player, "jobs.act_brush_sand", "xp", fmt(25.00, multiplier)),
                Lang.getFor(player, "jobs.act_brush_gravel", "xp", fmt(25.00, multiplier))
            );
            case LUMBERJACK -> List.of(
                Lang.getFor(player, "jobs.act_header_chop"),
                Lang.getFor(player, "jobs.act_log", "xp", fmt(2.90, multiplier)),
                Lang.getFor(player, "jobs.act_stripped_log", "xp", fmt(1.90, multiplier)),
                Lang.getFor(player, "jobs.act_bamboo_block", "xp", fmt(1.45, multiplier)),
                Lang.getFor(player, "jobs.act_mushroom_block", "xp", fmt(1.90, multiplier)),
                Lang.getFor(player, "jobs.act_leaves", "xp", fmt(0.08, multiplier)),
                Lang.getFor(player, "jobs.act_header_craft_saw"),
                Lang.getFor(player, "jobs.act_planks", "xp", fmt(1.20, multiplier)),
                Lang.getFor(player, "jobs.act_stairs", "xp", fmt(2.40, multiplier)),
                Lang.getFor(player, "jobs.act_slabs", "xp", fmt(1.20, multiplier)),
                Lang.getFor(player, "jobs.act_door", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_trapdoor", "xp", fmt(3.60, multiplier)),
                Lang.getFor(player, "jobs.act_fence", "xp", fmt(2.90, multiplier)),
                Lang.getFor(player, "jobs.act_chiseled_bookshelf", "xp", fmt(6.00, multiplier))
            );
            case BUILDER -> List.of(
                Lang.getFor(player, "jobs.act_header_place"),
                Lang.getFor(player, "jobs.act_any_block", "xp", fmt(1.00, multiplier))
            );
            case EXPLORER -> List.of(
                Lang.getFor(player, "jobs.act_header_travel"),
                Lang.getFor(player, "jobs.act_walk", "xp", fmt(0.25, multiplier)),
                Lang.getFor(player, "jobs.act_ride_horse", "xp", fmt(0.50, multiplier)),
                Lang.getFor(player, "jobs.act_header_discover"),
                Lang.getFor(player, "jobs.act_chest_overworld", "xp", fmt(100.00, multiplier)),
                Lang.getFor(player, "jobs.act_chest_nether", "xp", fmt(150.00, multiplier)),
                Lang.getFor(player, "jobs.act_chest_end", "xp", fmt(250.00, multiplier))
            );
            case ALCHEMIST -> List.of(
                Lang.getFor(player, "jobs.act_header_brew"),
                Lang.getFor(player, "jobs.act_vanilla_potion", "xp", fmt(19.20, multiplier)),
                Lang.getFor(player, "jobs.act_brewery_alcohol", "xp", fmt(48.00, multiplier)),
                Lang.getFor(player, "jobs.act_header_enchant"),
                Lang.getFor(player, "jobs.act_enchanting_table", "xp", fmt(24.00, multiplier)),
                Lang.getFor(player, "jobs.act_enchanted_book", "xp", fmt(19.20, multiplier)),
                Lang.getFor(player, "jobs.act_disenchant", "xp", fmt(14.40, multiplier)),
                Lang.getFor(player, "jobs.act_rename", "xp", fmt(4.80, multiplier))
            );
            case HUNTER -> List.of(
                Lang.getFor(player, "jobs.act_header_kill"),
                Lang.getFor(player, "jobs.act_zombie", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_skeleton", "xp", fmt(6.00, multiplier)),
                Lang.getFor(player, "jobs.act_spider", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_creeper", "xp", fmt(8.40, multiplier)),
                Lang.getFor(player, "jobs.act_witch", "xp", fmt(12.00, multiplier)),
                Lang.getFor(player, "jobs.act_blaze", "xp", fmt(19.20, multiplier)),
                Lang.getFor(player, "jobs.act_ghast", "xp", fmt(24.00, multiplier)),
                Lang.getFor(player, "jobs.act_enderman", "xp", fmt(3.60, multiplier)),
                Lang.getFor(player, "jobs.act_piglin", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_wither_skeleton", "xp", fmt(36.00, multiplier)),
                Lang.getFor(player, "jobs.act_guardian", "xp", fmt(18.00, multiplier)),
                Lang.getFor(player, "jobs.act_elder_guardian", "xp", fmt(250.00, multiplier)),
                Lang.getFor(player, "jobs.act_shulker", "xp", fmt(14.40, multiplier)),
                Lang.getFor(player, "jobs.act_breeze", "xp", fmt(28.80, multiplier)),
                Lang.getFor(player, "jobs.act_warden", "xp", fmt(100.00, multiplier)),
                Lang.getFor(player, "jobs.act_passive_mob", "xp", fmt(2.40, multiplier)),
                Lang.getFor(player, "jobs.act_player", "xp", fmt(120.00, multiplier)),
                Lang.getFor(player, "jobs.act_spawner_note"),
                Lang.getFor(player, "jobs.act_header_fish"),
                Lang.getFor(player, "jobs.act_cod", "xp", fmt(4.80, multiplier)),
                Lang.getFor(player, "jobs.act_pufferfish", "xp", fmt(8.40, multiplier)),
                Lang.getFor(player, "jobs.act_tropical_fish", "xp", fmt(6.00, multiplier)),
                Lang.getFor(player, "jobs.act_treasure", "xp", fmt(36.00, multiplier))
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
