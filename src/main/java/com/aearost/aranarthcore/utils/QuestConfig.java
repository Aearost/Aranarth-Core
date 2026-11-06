package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.JobType;
import com.aearost.aranarthcore.enums.QuestTrigger;
import com.aearost.aranarthcore.objects.QuestContract;
import com.aearost.aranarthcore.objects.QuestDefinition;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Loads quests.yml: the quest settings, every quest definition and the Job Contracts.
 */
public class QuestConfig {

    private static final String FILE_NAME = "quests.yml";

    private static final LinkedHashMap<String, QuestDefinition> definitions = new LinkedHashMap<>();
    private static final LinkedHashMap<String, QuestContract> contracts = new LinkedHashMap<>();

    private static double[] dailyRoll = {0.75, 1.25};
    private static double[] weeklyRoll = {1.0, 1.5};
    private static double dailyXpPercent = 2.5;
    private static double weeklyXpPercent = 10.0;
    private static int[] masterDailyBonus = new int[9];
    private static int[] masterWeeklyBonus = new int[9];
    private static double boardBonusPercent = 25;
    private static double dailyItemChance = 0.20;
    private static double weeklyItemChance = 0.30;
    private static double contractChance = 0.20;
    private static double rerollCostFraction = 0.25;
    private static int dailyCooldownDays = 3;
    private static int weeklyCooldownWeeks = 2;
    private static double[] dailyKeyChance = new double[9];
    private static double[] weeklyKeyChance = new double[9];
    private static double[] weeklyGodlyChance = new double[9];
    private static double dailyEpicChance = 0.15;
    private static double[] dailyBonusEpicChance = {0.05, 0.08, 0.11, 0.14, 0.17, 0.20, 0.23, 0.26, 0.30};
    private static double legendaryRecipeChance = 0.33;
    private static int pvpVictimCooldownMinutes = 60;
    private static int milkCooldownSeconds = 10;

    /**
     * Syncs the data-folder copy of quests.yml with the bundled one, then loads it.
     */
    public static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        syncDataFile(plugin, file);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadSettings(config.getConfigurationSection("settings"));
        loadDefinitions(config.getConfigurationSection("quests"));
        loadContracts(config.getConfigurationSection("contracts"));
        Bukkit.getLogger().info("[AC] Loaded " + definitions.size() + " quests and " + contracts.size() + " Job Contracts");
    }

    /**
     * Brings the server's quests.yml up to date with the bundled defaults.
     */
    private static void syncDataFile(Plugin plugin, File file) {
        String bundledText;
        try (InputStream in = plugin.getResource(FILE_NAME)) {
            if (in == null) {
                return;
            }
            bundledText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Could not read the bundled " + FILE_NAME + ": " + e.getMessage());
            return;
        }

        File snapshotDir = new File(plugin.getDataFolder(), "defaults");
        File snapshotFile = new File(snapshotDir, FILE_NAME);
        try {
            if (!file.exists()) {
                Files.writeString(file.toPath(), bundledText, StandardCharsets.UTF_8);
            } else {
                YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(new java.io.ByteArrayInputStream(bundledText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                YamlConfiguration live = YamlConfiguration.loadConfiguration(file);
                YamlConfiguration snapshot = snapshotFile.exists() ? YamlConfiguration.loadConfiguration(snapshotFile) : null;
                // A live file that matches the last shipped defaults (or one in the old format) is simply replaced
                boolean isUntouched = snapshot != null && snapshot.saveToString().equals(live.saveToString());
                boolean isOldFormat = !live.isConfigurationSection("quests");
                if (isUntouched || isOldFormat) {
                    Files.writeString(file.toPath(), bundledText, StandardCharsets.UTF_8);
                } else {
                    boolean isChanged = false;
                    for (String path : bundled.getKeys(true)) {
                        if (bundled.isConfigurationSection(path)) {
                            continue;
                        }
                        Object bundledValue = bundled.get(path);
                        if (!live.contains(path)) {
                            live.set(path, bundledValue);
                            isChanged = true;
                        } else if (snapshot != null && snapshot.contains(path)
                                && Objects.equals(String.valueOf(live.get(path)), String.valueOf(snapshot.get(path)))
                                && !Objects.equals(String.valueOf(live.get(path)), String.valueOf(bundledValue))) {
                            live.set(path, bundledValue);
                            isChanged = true;
                        }
                    }
                    for (String path : new ArrayList<>(live.getKeys(true))) {
                        if (live.isConfigurationSection(path) || bundled.contains(path) || !live.contains(path)) {
                            continue;
                        }
                        boolean isShippedValue = snapshot == null || (snapshot.contains(path)
                                && Objects.equals(String.valueOf(live.get(path)), String.valueOf(snapshot.get(path))));
                        if (isShippedValue) {
                            live.set(path, null);
                            isChanged = true;
                        }
                    }
                    if (isChanged) {
                        live.save(file);
                    }
                }
            }
            if (!snapshotDir.isDirectory() && !snapshotDir.mkdirs()) {
                return;
            }
            Files.writeString(snapshotFile.toPath(), bundledText, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[AC] Could not sync " + FILE_NAME + ": " + e.getMessage());
        }
    }

    private static void loadSettings(ConfigurationSection s) {
        if (s == null) {
            return;
        }
        dailyRoll = doubles(s, "daily-roll", dailyRoll);
        weeklyRoll = doubles(s, "weekly-roll", weeklyRoll);
        dailyXpPercent = s.getDouble("daily-xp-percent", dailyXpPercent);
        weeklyXpPercent = s.getDouble("weekly-xp-percent", weeklyXpPercent);
        masterDailyBonus = ints(s.getList("master-daily-bonus"));
        masterWeeklyBonus = ints(s.getList("master-weekly-bonus"));
        boardBonusPercent = s.getDouble("board-bonus-percent", boardBonusPercent);
        dailyItemChance = s.getDouble("daily-item-chance", dailyItemChance);
        weeklyItemChance = s.getDouble("weekly-item-chance", weeklyItemChance);
        contractChance = s.getDouble("contract-chance", contractChance);
        rerollCostFraction = s.getDouble("reroll-cost-fraction", rerollCostFraction);
        dailyCooldownDays = s.getInt("daily-cooldown-days", dailyCooldownDays);
        weeklyCooldownWeeks = s.getInt("weekly-cooldown-weeks", weeklyCooldownWeeks);
        dailyKeyChance = doubles(s, "daily-key-chance", dailyKeyChance);
        weeklyKeyChance = doubles(s, "weekly-key-chance", weeklyKeyChance);
        weeklyGodlyChance = doubles(s, "weekly-godly-chance", weeklyGodlyChance);
        dailyEpicChance = s.getDouble("daily-epic-chance", dailyEpicChance);
        dailyBonusEpicChance = doubles(s, "daily-bonus-epic-chance", dailyBonusEpicChance);
        legendaryRecipeChance = s.getDouble("legendary-recipe-chance", legendaryRecipeChance);
        pvpVictimCooldownMinutes = s.getInt("pvp-victim-cooldown-minutes", pvpVictimCooldownMinutes);
        milkCooldownSeconds = s.getInt("milk-cooldown-seconds", milkCooldownSeconds);
    }

    private static void loadDefinitions(ConfigurationSection quests) {
        definitions.clear();
        if (quests == null) {
            Bukkit.getLogger().warning("[AC] No quests found in " + FILE_NAME);
            return;
        }
        for (String id : quests.getKeys(false)) {
            ConfigurationSection q = quests.getConfigurationSection(id);
            if (q == null) {
                continue;
            }
            try {
                QuestTrigger trigger = QuestTrigger.valueOf(q.getString("trigger", ""));
                QuestDefinition.Tier tier = QuestDefinition.Tier.valueOf(q.getString("tier", "STANDARD"));
                LinkedHashMap<String, QuestDefinition.Variant> variants = new LinkedHashMap<>();
                ConfigurationSection vs = q.getConfigurationSection("variants");
                if (vs != null) {
                    for (String key : vs.getKeys(false)) {
                        ConfigurationSection v = vs.getConfigurationSection(key);
                        if (v == null) {
                            continue;
                        }
                        variants.put(key, new QuestDefinition.Variant(key,
                                new QuestDefinition.TargetMatcher(v.getStringList("targets")),
                                v.getInt("level", 1), v.getInt("alchemy_tier", 0), v.getString("month"),
                                v.getInt("daily-weight", 1), intsOrNull(v.getList("daily-amount")),
                                intsOrNull(v.getList("weekly-amount"))));
                    }
                }
                Map<Integer, int[]> range = new HashMap<>();
                ConfigurationSection rs = q.getConfigurationSection("weekly.range");
                if (rs != null) {
                    for (String rank : rs.getKeys(false)) {
                        int[] pair = ints(rs.getList(rank));
                        if (pair.length == 2) {
                            range.put(Integer.parseInt(rank), pair);
                        }
                    }
                }
                ConfigurationSection boost = q.getConfigurationSection("boost");
                QuestDefinition definition = new QuestDefinition(id, jobs(q.getStringList("jobs")), tier,
                        q.getString("family", id), new HashSet<>(q.getStringList("acts")), trigger,
                        q.contains("targets") ? new QuestDefinition.TargetMatcher(q.getStringList("targets")) : null,
                        new HashSet<>(q.getStringList("tags")), q.getBoolean("distinct", false), q.getInt("level", 1),
                        q.getString("eligibility"), boost != null ? boost.getString("month") : null,
                        boost != null ? boost.getInt("weight", 1) : 1,
                        intsOrNull(q.getList("daily.amount")), intsOrNull(q.getList("daily.reward")),
                        intsOrNull(q.getList("weekly.amount")), intsOrNull(q.getList("weekly.reward")),
                        q.getBoolean("weekly.fixed", false), range, variants);
                definition.setWeight(q.getInt("weight", 1));
                definitions.put(id, definition);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[AC] Skipping quest '" + id + "' in " + FILE_NAME + ": " + e.getMessage());
            }
        }
    }

    private static void loadContracts(ConfigurationSection section) {
        contracts.clear();
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection c = section.getConfigurationSection(id);
            if (c == null) {
                continue;
            }
            List<QuestContract.Part> parts = new ArrayList<>();
            for (Map<?, ?> map : c.getMapList("parts")) {
                Object questId = map.get("quest");
                Object amount = map.get("amount");
                if (!(questId instanceof String qid) || !(amount instanceof List<?> list) || !definitions.containsKey(qid)) {
                    continue;
                }
                Object variant = map.get("variant");
                parts.add(new QuestContract.Part(qid, ints(list), variant instanceof String s ? s : null));
            }
            if (parts.size() != 2) {
                Bukkit.getLogger().warning("[AC] Skipping Job Contract '" + id + "': it needs two valid parts");
                continue;
            }
            contracts.put(id, new QuestContract(id, jobs(c.getStringList("jobs")), parts, ints(c.getList("reward"))));
        }
    }

    private static List<JobType> jobs(List<String> names) {
        List<JobType> jobs = new ArrayList<>();
        for (String name : names) {
            jobs.add(JobType.valueOf(name));
        }
        return jobs;
    }

    private static int[] ints(List<?> list) {
        if (list == null) {
            return new int[9];
        }
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i) instanceof Number n ? n.intValue() : 0;
        }
        return result;
    }

    private static int[] intsOrNull(List<?> list) {
        return list == null || list.isEmpty() ? null : ints(list);
    }

    private static double[] doubles(ConfigurationSection s, String path, double[] fallback) {
        List<?> list = s.getList(path);
        if (list == null) {
            return fallback;
        }
        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i) instanceof Number n ? n.doubleValue() : 0;
        }
        return result;
    }

    private static double at(double[] values, int rank) {
        return values.length == 0 ? 0 : values[Math.max(0, Math.min(rank, values.length - 1))];
    }

    private static int at(int[] values, int rank) {
        return values.length == 0 ? 0 : values[Math.max(0, Math.min(rank, values.length - 1))];
    }

    public static Collection<QuestDefinition> getDefinitions() {
        return definitions.values();
    }

    public static QuestDefinition getDefinition(String id) {
        return definitions.get(id);
    }

    public static Collection<QuestContract> getContracts() {
        return contracts.values();
    }

    public static QuestContract getContract(String id) {
        return contracts.get(id);
    }

    public static double[] getDailyRoll() {
        return dailyRoll;
    }

    public static double[] getWeeklyRoll() {
        return weeklyRoll;
    }

    public static double getDailyXpPercent() {
        return dailyXpPercent;
    }

    public static double getWeeklyXpPercent() {
        return weeklyXpPercent;
    }

    public static int getMasterDailyBonus(int rank) {
        return at(masterDailyBonus, rank);
    }

    public static int getMasterWeeklyBonus(int rank) {
        return at(masterWeeklyBonus, rank);
    }

    public static double getBoardBonusPercent() {
        return boardBonusPercent;
    }

    public static double getDailyItemChance() {
        return dailyItemChance;
    }

    public static double getWeeklyItemChance() {
        return weeklyItemChance;
    }

    public static double getContractChance() {
        return contractChance;
    }

    public static double getRerollCostFraction() {
        return rerollCostFraction;
    }

    public static int getDailyCooldownDays() {
        return dailyCooldownDays;
    }

    public static int getWeeklyCooldownWeeks() {
        return weeklyCooldownWeeks;
    }

    public static double getDailyKeyChance(int rank) {
        return at(dailyKeyChance, rank);
    }

    public static double getWeeklyKeyChance(int rank) {
        return at(weeklyKeyChance, rank);
    }

    public static double getWeeklyGodlyChance(int rank) {
        return at(weeklyGodlyChance, rank);
    }

    public static double getDailyEpicChance() {
        return dailyEpicChance;
    }

    public static double getDailyBonusEpicChance(int rank) {
        return at(dailyBonusEpicChance, rank);
    }

    public static double getLegendaryRecipeChance() {
        return legendaryRecipeChance;
    }

    public static int getPvpVictimCooldownMinutes() {
        return pvpVictimCooldownMinutes;
    }

    public static int getMilkCooldownSeconds() {
        return milkCooldownSeconds;
    }
}
