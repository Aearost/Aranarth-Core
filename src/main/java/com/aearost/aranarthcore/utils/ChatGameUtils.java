package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.network.NetworkManager;
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
    private static volatile String currentGameOrigin = null;

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

        final String answer;
        final String scrambled;
        synchronized (gameLock) {
            if (currentAnswer != null) {
                // A cross-server game is already active
                scheduleNextGame(plugin);
                return;
            }

            if (remainingWords.isEmpty()) {
                remainingWords.addAll(wordPool);
                Collections.shuffle(remainingWords, random);
            }

            String word = remainingWords.remove(remainingWords.size() - 1);
            String sc = scramble(word);

            currentAnswer = word;
            currentScrambled = sc;
            currentGameOrigin = NetworkManager.isActive()
                    ? NetworkManager.getInstance().getThisServer()
                    : "local";
            answer = word;
            scrambled = sc;
        }

        Bukkit.broadcastMessage(ChatUtils.chatMessage(
                "&8&l[&6&lAC&8&l] &7Unscramble the following word: &e" + scrambled));

        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishChatGameStart(scrambled, answer);
        }
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
            final String origin = currentGameOrigin;
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;

            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
                int rank = Math.min(ap.getRank(), RANK_REWARDS.length - 1);
                double reward = RANK_REWARDS[rank];
                ap.setBalance(ap.getBalance() + reward);
                AranarthUtils.addChatGameGuess(player.getUniqueId());

                String winnerNickname = ap.getNickname();
                Bukkit.broadcastMessage(ChatUtils.chatMessage(
                        "&8&l[&6&lAC&8&l] &e" + winnerNickname
                                + " &7guessed &e" + answer + " &7correctly!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishChatGameWin(winnerNickname, answer);
                }

                // Only the server that originated the game schedules the next one
                boolean isOrigin = !NetworkManager.isActive()
                        || NetworkManager.getInstance().getThisServer().equals(origin);
                if (isOrigin) {
                    scheduleNextGame(AranarthCore.getInstance());
                }
            });
        }
    }

    /**
     * Called by NetworkManager when another server has started a chat game.
     * Sets local game state and broadcasts the scramble message to players on this server.
     * Must be called on the main thread.
     */
    public static void applyNetworkGameStart(String scrambled, String answer, String originServer) {
        synchronized (gameLock) {
            currentAnswer = answer;
            currentScrambled = scrambled;
            currentGameOrigin = originServer;
        }
        Bukkit.broadcastMessage(ChatUtils.chatMessage(
                "&8&l[&6&lAC&8&l] &7Unscramble the following word: &e" + scrambled));
    }

    /**
     * Called by NetworkManager when a player on another server has won the chat game.
     * Clears local game state, broadcasts the win message, and — if this server was the
     * game's origin — schedules the next game.
     * Must be called on the main thread.
     */
    public static void applyNetworkGameWin(Plugin plugin, String winnerNickname, String answer) {
        final boolean wasOrigin;
        synchronized (gameLock) {
            wasOrigin = NetworkManager.isActive()
                    && NetworkManager.getInstance().getThisServer().equals(currentGameOrigin);
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;
        }
        Bukkit.broadcastMessage(ChatUtils.chatMessage(
                "&8&l[&6&lAC&8&l] &e" + winnerNickname + " &7guessed &e" + answer + " &7correctly!"));
        if (wasOrigin) {
            scheduleNextGame(plugin);
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
