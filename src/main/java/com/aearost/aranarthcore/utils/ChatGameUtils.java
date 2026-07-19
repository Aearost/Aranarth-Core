package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the chat word-scramble mini-game.
 */
public class ChatGameUtils {

    // Each value is proportional to the cost to reach the next rank from that tier
    private static final double[] RANK_REWARDS = { 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 25000, };

    private static final List<String> wordPool = new ArrayList<>();
    private static final List<String> remainingWords = new ArrayList<>();
    private static final Random random = new Random();
    private static final Object gameLock = new Object();

    private static volatile String currentAnswer = null;
    private static volatile String currentScrambled = null;

    private ChatGameUtils() {}

    /**
     * Loads words from the words.txt resource file and schedules the first game.
     */
    public static void initialize(Plugin plugin) {
        File wordsFile = new File(plugin.getDataFolder(), "words.txt");
        if (!wordsFile.exists()) {
            plugin.saveResource("words.txt", false);
        }
        loadWords(wordsFile);
        if (!wordPool.isEmpty()) {
            scheduleNextGame(plugin);
        }
    }

    private static void loadWords(File wordsFile) {
        wordPool.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(wordsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    wordPool.add(line);
                }
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to load words.txt: " + e.getMessage());
        }
    }

    /**
     * Schedules the next game to start after a random delay of 5-15 minutes.
     */
    public static void scheduleNextGame(Plugin plugin) {
        // Random delay between 5-10 minutes
        int delay = 6000 + random.nextInt(6001);
        new BukkitRunnable() {
            @Override
            public void run() {
                startGame(plugin);
            }
        }.runTaskLater(plugin, delay);
    }

    private static void startGame(Plugin plugin) {
        if (wordPool.isEmpty()) {
            return;
        }

        if (remainingWords.isEmpty()) {
            remainingWords.addAll(wordPool);
            Collections.shuffle(remainingWords, random);
        }

        String word = remainingWords.remove(remainingWords.size() - 1);
        String scrambled = scramble(word);

        synchronized (gameLock) {
            currentAnswer = word;
            currentScrambled = scrambled;
        }

        Bukkit.broadcastMessage(ChatUtils.chatMessage(
                "&8&l[&6&lAC&8&l] &7Unscramble the following word: &e" + scrambled));
    }

    /**
     * Checks whether the given message is the correct answer to the active game.
     */
    public static void tryAnswer(Player player, String message) {
        synchronized (gameLock) {
            if (currentAnswer == null) {
                return;
            }
            if (!message.equalsIgnoreCase(currentAnswer)) {
                return;
            }

            final String answer = currentAnswer;
            currentAnswer = null;
            currentScrambled = null;

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                int rank = Math.min(ap.getRank(), RANK_REWARDS.length - 1);
                double reward = RANK_REWARDS[rank];
                ap.setBalance(ap.getBalance() + reward);
                AranarthUtils.addChatGameGuess(player.getUniqueId());

                NumberFormat formatter = NumberFormat.getCurrencyInstance();
                Bukkit.broadcastMessage(ChatUtils.chatMessage(
                        "&8&l[&6&lAC&8&l] &e" + ap.getNickname()
                                + " &7guessed &e" + answer + " &7correctly!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                scheduleNextGame(AranarthCore.getInstance());
            });

        }
    }

    /**
     * @return true if a word-scramble game is currently active.
     */
    public static boolean isGameActive() {
        return currentAnswer != null;
    }

    /**
     * @return the currently scrambled word, or null if no game is active.
     */
    public static String getCurrentScrambled() {
        return currentScrambled;
    }

    private static String scramble(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) {
            chars.add(c);
        }
        String scrambled;
        int attempts = 0;
        do {
            Collections.shuffle(chars, random);
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            scrambled = sb.toString();
            attempts++;
        } while (scrambled.equals(word) && attempts < 20);
        return scrambled;
    }
}
