package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.items.AranarthItem;
import com.aearost.aranarthcore.items.InvisibleItemFrame;
import com.aearost.aranarthcore.items.aranarthium.clusters.*;
import com.aearost.aranarthcore.items.arrow.*;
import com.aearost.aranarthcore.items.brew.BrewRecipe;
import com.aearost.aranarthcore.items.key.KeyEpic;
import com.aearost.aranarthcore.items.key.KeyGodly;
import com.aearost.aranarthcore.items.key.KeyRare;
import com.aearost.aranarthcore.objects.Quest;
import com.aearost.aranarthcore.objects.QuestDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Item rewards for quests.
 */
public class QuestRewardUtils {

    private static final String[] STANDARD_ARROW = {"iron", "iron", "gold", "gold", "amethyst", "obsidian", "obsidian", "diamond", "diamond"};
    private static final int[] STANDARD_ARROW_AMOUNT = {32, 40, 48, 56, 64, 80, 96, 112, 128};
    private static final int[] SPECIAL_ARROW_AMOUNT = {4, 4, 5, 5, 6, 6, 7, 7, 8};
    private static final int[] FRAME_AMOUNT = {4, 4, 6, 6, 8, 10, 12, 14, 16};
    private static final int[] CLUSTER_AMOUNT = {1, 1, 1, 2, 2, 2, 3, 3, 3};

    private static final Map<String, String> CLUSTER_FOR_QUEST = Map.of(
            "mi_gold", "gold", "mi_diamond", "diamond", "mi_emerald", "emerald",
            "mi_iron", "iron", "mi_copper", "copper", "mi_lapis", "lapis",
            "mi_redstone", "redstone", "mi_quartz", "quartz");

    private static int at(int[] values, int rank) {
        return values[Math.max(0, Math.min(rank, values.length - 1))];
    }

    private static String at(String[] values, int rank) {
        return values[Math.max(0, Math.min(rank, values.length - 1))];
    }

    /**
     * @return The special arrow types the rank has unlocked.
     */
    private static List<String> getSpecialArrows(int rank) {
        List<String> types = new ArrayList<>(List.of("rooting", "gust"));
        if (rank >= 4) {
            types.add("lightning");
        }
        if (rank >= 6) {
            types.add("explosive");
        }
        if (rank >= 8) {
            types.add("dragon");
        }
        return types;
    }

    /**
     * Picks the job item reward for a quest, or null if the quest cannot give one.
     *
     * @param player The player, used for job levels and locked recipes.
     * @param definition The quest.
     * @param type The board type.
     * @param rank The player's rank.
     * @param random The random source.
     * @return The item descriptor, or null.
     */
    public static String rollJobItem(UUID player, int minerLevel, QuestDefinition definition, QuestType type, int rank, Random random) {
        List<JobType> jobs = definition.getJobs();
        if (jobs.contains(JobType.MINER) && definition.getTier().isHard() && minerLevel >= 7) {
            String ore = CLUSTER_FOR_QUEST.get(definition.getId());
            if (ore != null) {
                return "cluster:" + ore + ":" + at(CLUSTER_AMOUNT, rank);
            }
        }
        if (jobs.contains(JobType.BUILDER)) {
            return "frames:" + at(FRAME_AMOUNT, rank);
        }
        if (jobs.contains(JobType.ALCHEMIST) && type == QuestType.WEEKLY) {
            BrewRecipe recipe = BrewRecipeUtils.getRandomLockedRare(player);
            if (recipe == null) {
                List<BrewRecipe> common = BrewRecipeUtils.getLockedCommonRecipes(player);
                recipe = common.isEmpty() ? null : common.get(random.nextInt(common.size()));
            }
            return recipe == null ? null : "recipe:" + recipe.getId();
        }
        if (jobs.contains(JobType.HUNTER)) {
            List<String> specials = getSpecialArrows(rank);
            return "arrows:" + at(STANDARD_ARROW, rank) + ":" + at(STANDARD_ARROW_AMOUNT, rank)
                    + ":" + specials.get(random.nextInt(specials.size())) + ":" + at(SPECIAL_ARROW_AMOUNT, rank);
        }
        return null;
    }

    /**
     * @return Whether the quest is one that could give a job item, so the board can pick which quest gets one.
     */
    public static boolean canGiveJobItem(QuestDefinition definition, QuestType type, int minerLevel) {
        List<JobType> jobs = definition.getJobs();
        return (jobs.contains(JobType.MINER) && definition.getTier().isHard() && minerLevel >= 7
                && CLUSTER_FOR_QUEST.containsKey(definition.getId()))
                || jobs.contains(JobType.BUILDER)
                || (jobs.contains(JobType.ALCHEMIST) && type == QuestType.WEEKLY)
                || jobs.contains(JobType.HUNTER);
    }

    public static String rollDailyKey(Random random) {
        return random.nextDouble() < QuestConfig.getDailyEpicChance() ? "key:epic" : "key:rare";
    }

    public static String rollWeeklyKey(int rank, Random random) {
        return random.nextDouble() < QuestConfig.getWeeklyGodlyChance(rank) ? "key:godly" : "key:epic";
    }

    /**
     * Builds the items for a reward descriptor.
     */
    public static List<ItemStack> createItems(String descriptor) {
        List<ItemStack> items = new ArrayList<>();
        if (descriptor == null) {
            return items;
        }
        String[] parts = descriptor.split(":");
        switch (parts[0]) {
            case "key" -> items.add(switch (parts[1]) {
                case "epic" -> new KeyEpic().getItem();
                case "godly" -> new KeyGodly().getItem();
                default -> new KeyRare().getItem();
            });
            case "recipe" -> {
                BrewRecipe recipe = BrewRecipe.fromId(parts[1]);
                if (recipe != null) {
                    items.add(BrewRecipeUtils.createRecipeMapItem(recipe));
                }
            }
            case "cluster" -> items.add(withAmount(cluster(parts[1]), Integer.parseInt(parts[2])));
            case "frames" -> items.add(withAmount(new InvisibleItemFrame(), Integer.parseInt(parts[1])));
            case "arrows" -> {
                items.add(withAmount(arrow(parts[1]), Integer.parseInt(parts[2])));
                items.add(withAmount(arrow(parts[3]), Integer.parseInt(parts[4])));
            }
            default -> {
            }
        }
        return items;
    }

    /**
     * @return The reward's display text, such as "48x Gold Arrow + 5x Gust Arrow".
     */
    public static String getDisplayName(String descriptor) {
        List<String> names = new ArrayList<>();
        for (ItemStack item : createItems(descriptor)) {
            String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName()
                    : item.getType().name();
            names.add(item.getAmount() > 1 ? item.getAmount() + "x " + name : name);
        }
        return String.join(ChatUtils.translateToColor(" &7+ "), names);
    }

    public static boolean isKey(String descriptor) {
        return descriptor != null && descriptor.startsWith("key:");
    }

    public static boolean isRecipe(String descriptor) {
        return descriptor != null && descriptor.startsWith("recipe:");
    }

    /**
     * Gives the reward items, sending crate keys to /keyclaim outside the survival worlds.
     *
     * @return Whether the items could be given. Items other than keys can only be claimed in a survival world.
     */
    public static boolean giveItems(Player player, Quest quest) {
        String descriptor = quest.getItemReward();
        boolean isKeyReward = isKey(descriptor);
        boolean isSurvivalWorld = AranarthUtils.isSurvivalWorld(player.getWorld().getName());
        if (!isKeyReward && !isSurvivalWorld) {
            return false;
        }
        for (ItemStack item : createItems(descriptor)) {
            if (isKeyReward && !isSurvivalWorld) {
                AranarthUtils.addPendingKey(player.getUniqueId(), item, item.getAmount());
                continue;
            }
            for (ItemStack overflow : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
        return true;
    }

    private static ItemStack withAmount(AranarthItem item, int amount) {
        ItemStack stack = item.getItem();
        stack.setAmount(Math.max(1, amount));
        return stack;
    }

    private static AranarthItem cluster(String ore) {
        return switch (ore) {
            case "gold" -> new GoldCluster();
            case "diamond" -> new DiamondCluster();
            case "emerald" -> new EmeraldCluster();
            case "copper" -> new CopperCluster();
            case "lapis" -> new LapisCluster();
            case "redstone" -> new RedstoneCluster();
            case "quartz" -> new QuartzCluster();
            default -> new IronCluster();
        };
    }

    private static AranarthItem arrow(String type) {
        return switch (type) {
            case "gold" -> new ArrowGold();
            case "amethyst" -> new ArrowAmethyst();
            case "obsidian" -> new ArrowObsidian();
            case "diamond" -> new ArrowDiamond();
            case "gust" -> new ArrowGust();
            case "rooting" -> new ArrowRooting();
            case "lightning" -> new ArrowLightning();
            case "explosive" -> new ArrowExplosive();
            case "dragon" -> new ArrowDragon();
            default -> new ArrowIron();
        };
    }
}
