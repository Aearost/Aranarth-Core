package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.enums.QuestType;
import com.aearost.aranarthcore.objects.PlayerQuestBoard;
import com.aearost.aranarthcore.objects.Quest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts a player's quest board to and from JSON, for quest_progress.txt and the shared database.
 */
public class QuestBoardSerializer {

    private static final int VERSION = 2;

    public static String toJson(PlayerQuestBoard board) {
        JsonObject json = new JsonObject();
        json.addProperty("v", VERSION);
        json.addProperty("dailyPeriod", board.getPeriod(QuestType.DAILY));
        json.addProperty("weeklyPeriod", board.getPeriod(QuestType.WEEKLY));
        json.addProperty("dailyReroll", board.isRerollUsed(QuestType.DAILY));
        json.addProperty("weeklyReroll", board.isRerollUsed(QuestType.WEEKLY));
        json.addProperty("dailyBonus", board.isBonusClaimed(QuestType.DAILY));
        json.addProperty("weeklyBonus", board.isBonusClaimed(QuestType.WEEKLY));
        json.addProperty("dailySalt", board.getSalt(QuestType.DAILY));
        json.addProperty("weeklySalt", board.getSalt(QuestType.WEEKLY));
        JsonObject history = new JsonObject();
        for (Map.Entry<String, Long> entry : board.getHistory().entrySet()) {
            history.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("history", history);
        json.add("daily", questsToJson(board.getQuests(QuestType.DAILY)));
        json.add("weekly", questsToJson(board.getQuests(QuestType.WEEKLY)));
        return json.toString();
    }

    private static JsonArray questsToJson(List<Quest> quests) {
        JsonArray array = new JsonArray();
        for (Quest quest : quests) {
            JsonObject q = new JsonObject();
            q.addProperty("id", quest.getId());
            q.addProperty("contract", quest.isContract());
            q.addProperty("rank", quest.getRank());
            JsonArray variants = new JsonArray();
            for (String variant : quest.getVariants()) {
                variants.add(variant == null ? "" : variant);
            }
            q.add("variants", variants);
            JsonArray required = new JsonArray();
            JsonArray progress = new JsonArray();
            for (int i = 0; i < quest.getPartCount(); i++) {
                required.add(quest.getRequired(i));
                progress.add(quest.getProgress(i));
            }
            q.add("required", required);
            q.add("progress", progress);
            JsonArray seen = new JsonArray();
            for (String id : quest.getSeen()) {
                seen.add(id);
            }
            q.add("seen", seen);
            q.addProperty("baseReward", quest.getBaseReward());
            if (quest.hasItemReward()) {
                q.addProperty("item", quest.getItemReward());
            }
            q.addProperty("completed", quest.isCompleted());
            q.addProperty("claimed", quest.isClaimed());
            array.add(q);
        }
        return array;
    }

    /**
     * @return The board, or null if the JSON is missing or from the quest system before #450.
     */
    public static PlayerQuestBoard fromJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            if (!json.has("v") || json.get("v").getAsInt() < VERSION) {
                return null;
            }
            PlayerQuestBoard board = new PlayerQuestBoard();
            board.setPeriod(QuestType.DAILY, json.get("dailyPeriod").getAsLong());
            board.setPeriod(QuestType.WEEKLY, json.get("weeklyPeriod").getAsLong());
            board.setRerollUsed(QuestType.DAILY, json.get("dailyReroll").getAsBoolean());
            board.setRerollUsed(QuestType.WEEKLY, json.get("weeklyReroll").getAsBoolean());
            board.setBonusClaimed(QuestType.DAILY, json.get("dailyBonus").getAsBoolean());
            board.setBonusClaimed(QuestType.WEEKLY, json.get("weeklyBonus").getAsBoolean());
            if (json.has("dailySalt")) {
                board.setSalt(QuestType.DAILY, json.get("dailySalt").getAsLong());
                board.setSalt(QuestType.WEEKLY, json.get("weeklySalt").getAsLong());
            }
            if (json.has("history")) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("history").entrySet()) {
                    board.getHistory().put(entry.getKey(), entry.getValue().getAsLong());
                }
            }
            board.setQuests(QuestType.DAILY, questsFromJson(json.getAsJsonArray("daily"), QuestType.DAILY));
            board.setQuests(QuestType.WEEKLY, questsFromJson(json.getAsJsonArray("weekly"), QuestType.WEEKLY));
            return board;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Quest> questsFromJson(JsonArray array, QuestType type) {
        List<Quest> quests = new ArrayList<>();
        if (array == null) {
            return quests;
        }
        for (JsonElement element : array) {
            JsonObject q = element.getAsJsonObject();
            String id = q.get("id").getAsString();
            boolean isContract = q.get("contract").getAsBoolean();
            // A quest removed from quests.yml is dropped, so its slot shows no quest until the next reset
            if (isContract ? QuestConfig.getContract(id) == null : QuestConfig.getDefinition(id) == null) {
                continue;
            }
            JsonArray variantArray = q.getAsJsonArray("variants");
            JsonArray requiredArray = q.getAsJsonArray("required");
            JsonArray progressArray = q.getAsJsonArray("progress");
            String[] variants = new String[requiredArray.size()];
            int[] required = new int[requiredArray.size()];
            for (int i = 0; i < required.length; i++) {
                String variant = i < variantArray.size() ? variantArray.get(i).getAsString() : "";
                variants[i] = variant.isEmpty() ? null : variant;
                required[i] = requiredArray.get(i).getAsInt();
            }
            Quest quest = new Quest(id, isContract, type, q.get("rank").getAsInt(), variants, required,
                    q.get("baseReward").getAsDouble());
            for (int i = 0; i < required.length && i < progressArray.size(); i++) {
                quest.setProgress(i, progressArray.get(i).getAsInt());
            }
            if (q.has("seen")) {
                for (JsonElement seen : q.getAsJsonArray("seen")) {
                    quest.getSeen().add(seen.getAsString());
                }
            }
            if (q.has("item")) {
                quest.setItemReward(q.get("item").getAsString());
            }
            quest.setCompleted(q.get("completed").getAsBoolean());
            quest.setClaimed(q.get("claimed").getAsBoolean());
            quests.add(quest);
        }
        return quests;
    }
}
