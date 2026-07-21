package com.aearost.aranarthcore.utils;

import com.aearost.aranarthcore.AranarthCore;
import com.aearost.aranarthcore.database.DatabaseManager;
import com.aearost.aranarthcore.network.NetworkManager;
import com.aearost.aranarthcore.objects.AranarthPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages the chat word-scramble mini-game.
 */
public class ChatGameUtils {

    // Each value is proportional to the cost to reach the next rank from that tier
    private static final double[] RANK_REWARDS = { 50, 100, 200, 500, 1000, 2000, 4000, 7000, 10000 };

    private static final List<String> wordPool = new ArrayList<>();
    private static Plugin plugin;
    private static final List<String> remainingWords = new ArrayList<>();
    private static final Random random = new Random();
    private static final Object gameLock = new Object();

    private static volatile String currentAnswer = null;
    private static volatile String currentScrambled = null;
    private static volatile String currentGameOrigin = null;

    // Streak state tracks consecutive wins by the same player
    private static volatile UUID streakWinnerUUID = null;
    private static volatile int streakCount = 0;

    // Timeout task for the active game (cancelled on win)
    private static volatile BukkitTask timeoutTask = null;

    // Timestamp (ms) when the current game started, for speed calculation
    private static volatile long gameStartTime = 0;

    // Global all-time speed record
    private static final Object globalRecordLock = new Object();
    private static volatile UUID globalBestHolderUUID = null;
    private static volatile String globalBestHolderNickname = "";
    private static volatile double globalBestTime = 0;

    private static final int TIMEOUT_TICKS = 600; // 30 seconds

    private ChatGameUtils() {}

    /**
     * Loads words from the words.txt resource file and schedules the first game.
     */
    public static void initialize(Plugin p) {
        plugin = p;
        File wordsFile = new File(plugin.getDataFolder(), "words.txt");
        if (!wordsFile.exists()) {
            plugin.saveResource("words.txt", false);
        }
        loadWords(wordsFile);
        if (!wordPool.isEmpty()) {
            scheduleNextGame(plugin);
        }
    }

    /**
     * Loads the global all-time best unscramble time from the database into the in-memory cache.
     * Called once on startup after chat game data has been loaded.
     */
    public static void loadGlobalBestFromDatabase() {
        if (!DatabaseManager.isActive()) {
            return;
        }
        DatabaseManager.GlobalBestEntry entry = DatabaseManager.getInstance().loadGlobalBestTime();
        if (entry != null) {
            synchronized (globalRecordLock) {
                globalBestHolderUUID = entry.holderUUID();
                globalBestHolderNickname = entry.nickname();
                globalBestTime = entry.time();
            }
        }
    }

    /**
     * Updates the in-memory global best cache. Called when another server beats the record.
     */
    public static void updateGlobalBestCache(UUID holderUUID, String holderNickname, double time) {
        synchronized (globalRecordLock) {
            if (globalBestTime == 0 || time < globalBestTime) {
                globalBestHolderUUID = holderUUID;
                globalBestHolderNickname = holderNickname;
                globalBestTime = time;
            }
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
     * Returns a sorted snapshot of the current word pool, for tab completion.
     */
    public static List<String> getWords() {
        List<String> sorted = new ArrayList<>(wordPool);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Adds a word to the pool and appends it to words.txt.
     * @return false if the word already exists
     */
    public static boolean addWord(String word) {
        word = word.toLowerCase();
        if (wordPool.contains(word)) {
            return false;
        }
        wordPool.add(word);
        remainingWords.add(word);
        File wordsFile = new File(plugin.getDataFolder(), "words.txt");
        try {
            byte[] bytes = Files.readAllBytes(wordsFile.toPath());
            boolean needsNewline = bytes.length > 0 && bytes[bytes.length - 1] != '\n';
            try (PrintWriter pw = new PrintWriter(new FileWriter(wordsFile, true))) {
                if (needsNewline) {
                    pw.println();
                }
                pw.println(word);
            }
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to save words.txt: " + e.getMessage());
        }
        return true;
    }

    /**
     * Removes a word from the pool and from words.txt.
     * @return false if the word was not found
     */
    public static boolean removeWord(String word) {
        word = word.toLowerCase();
        if (!wordPool.remove(word)) {
            return false;
        }
        remainingWords.remove(word);
        File wordsFile = new File(plugin.getDataFolder(), "words.txt");
        try {
            List<String> lines = Files.readAllLines(wordsFile.toPath());
            final String finalWord = word;
            List<String> filtered = lines.stream()
                    .filter(line -> !line.trim().equalsIgnoreCase(finalWord))
                    .collect(Collectors.toList());
            Files.write(wordsFile.toPath(), filtered);
        } catch (IOException e) {
            Bukkit.getLogger().warning(AranarthCore.LOG_PREFIX + "Failed to save words.txt: " + e.getMessage());
        }
        return true;
    }

    /**
     * Schedules the next game to start after a random delay of 5-15 minutes.
     */
    public static void scheduleNextGame(Plugin plugin) {
        // Random delay between 8-10 minutes
        int delay = 9600 + random.nextInt(4800);
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
            gameStartTime = System.currentTimeMillis();
            answer = word;
            scrambled = sc;

            // Schedule timeout inside the lock so tryAnswer can never miss it
            timeoutTask = new BukkitRunnable() {
                @Override
                public void run() {
                    handleTimeout(plugin, word);
                }
            }.runTaskLater(plugin, TIMEOUT_TICKS);
        }

        String startMsg = ChatUtils.chatMessage("&7Unscramble the following word: &e" + scrambled);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(startMsg);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
        }

        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishChatGameStart(scrambled, answer);
        }
    }

    private static void handleTimeout(Plugin plugin, String expectedAnswer) {
        synchronized (gameLock) {
            if (!expectedAnswer.equals(currentAnswer)) {
                return; // Game already won or ended
            }
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;
            timeoutTask = null;
            streakWinnerUUID = null;
            streakCount = 0;
        }
        String expireMsg = ChatUtils.chatMessage("&7Nobody guessed! The word was &e" + expectedAnswer);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(expireMsg);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1.0f);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.1f);
        }
        if (NetworkManager.isActive()) {
            NetworkManager.getInstance().publishChatGameExpire(expectedAnswer);
        }
        scheduleNextGame(plugin);
    }

    /**
     * Checks whether the given message is the correct answer to the active game.
     * @return true if the player guessed correctly (caller should cancel the chat event)
     */
    public static boolean tryAnswer(Player player, String message) {
        final String answer;
        final String origin;
        final int localStreakCount;
        final double elapsedSeconds;
        synchronized (gameLock) {
            if (currentAnswer == null) {
                return false;
            }
            if (!message.equalsIgnoreCase(currentAnswer)) {
                return false;
            }

            elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000.0;
            answer = currentAnswer;
            origin = currentGameOrigin;
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;
            gameStartTime = 0;

            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }

            UUID uuid = player.getUniqueId();
            if (uuid.equals(streakWinnerUUID)) {
                streakCount++;
            } else {
                streakWinnerUUID = uuid;
                streakCount = 1;
            }
            localStreakCount = streakCount;
        }

        Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
            AranarthPlayer ap = AranarthUtils.getPlayer(player.getUniqueId());
            int rank = Math.min(ap.getRank(), RANK_REWARDS.length - 1);
            double baseReward = RANK_REWARDS[rank];
            double multiplier = Math.min(1.0 + 0.1 * (localStreakCount - 1), 2.0);
            double reward = baseReward * multiplier;

            ap.setBalance(ap.getBalance() + reward);
            AranarthUtils.addChatGameGuess(player.getUniqueId());
            AranarthUtils.addChatGameEarnings(player.getUniqueId(), reward);

            // Increment in DB so /topguesses is always accurate across servers
            if (DatabaseManager.isActive()) {
                final double finalReward = reward;
                Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                        DatabaseManager.getInstance().incrementChatGameGuessCount(player.getUniqueId(), finalReward));
            }

            String winnerNickname = ap.getNickname();
            String timeStr = String.format("%.2f", elapsedSeconds);
            String winMsg = ChatUtils.chatMessage("&e" + winnerNickname + " &7guessed &e" + answer + " &7correctly in &e" + timeStr + "s!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(winMsg);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }

            if (localStreakCount >= 3) {
                String streakMsg = ChatUtils.chatMessage("&e" + winnerNickname + " &7is on a &e" + localStreakCount + "x &7unscramble streak!");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(streakMsg);
                }
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishBroadcast(streakMsg);
                }
            }

            // Personal best speed check
            double personalBest = AranarthUtils.getChatGameBestTime(player.getUniqueId());
            if (personalBest == 0 || elapsedSeconds < personalBest) {
                AranarthUtils.setChatGameBestTime(player.getUniqueId(), elapsedSeconds);
                if (DatabaseManager.isActive()) {
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                            DatabaseManager.getInstance().updatePersonalBestTime(player.getUniqueId(), elapsedSeconds));
                }
            }

            // Personal best streak check
            int currentHighestStreak = AranarthUtils.getChatGameHighestStreak(player.getUniqueId());
            if (localStreakCount > currentHighestStreak) {
                AranarthUtils.setChatGameHighestStreak(player.getUniqueId(), localStreakCount);
                if (DatabaseManager.isActive()) {
                    Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () ->
                            DatabaseManager.getInstance().updateHighestStreak(player.getUniqueId(), localStreakCount));
                }
            }

            // Global record check
            final String oldHolderNickname;
            final double oldGlobalBestTime;
            final boolean newGlobalRecord;
            synchronized (globalRecordLock) {
                if (globalBestTime == 0 || elapsedSeconds < globalBestTime) {
                    newGlobalRecord = true;
                    oldHolderNickname = globalBestHolderNickname;
                    oldGlobalBestTime = globalBestTime;
                    globalBestTime = elapsedSeconds;
                    globalBestHolderUUID = player.getUniqueId();
                    globalBestHolderNickname = winnerNickname;
                } else {
                    newGlobalRecord = false;
                    oldHolderNickname = null;
                    oldGlobalBestTime = 0;
                }
            }

            if (newGlobalRecord && oldHolderNickname != null && !oldHolderNickname.isEmpty()) {
                String oldTimeStr = String.format("%.2f", oldGlobalBestTime);
                String recordMsg = ChatUtils.chatMessage("&e" + winnerNickname + " &7has beat &e" + oldHolderNickname + "&e's &7record of &e" + oldTimeStr + "s");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(recordMsg);
                }
                if (NetworkManager.isActive()) {
                    NetworkManager.getInstance().publishBroadcast(recordMsg);
                }
            }

            String rewardText = formatMoney(reward);
            if (localStreakCount > 1) {
                player.sendMessage(ChatUtils.chatMessage(
                        "&7You earned &6" + rewardText
                                + " &7- &e" + localStreakCount + "x streak"));
            } else {
                player.sendMessage(ChatUtils.chatMessage("&7You earned &6" + rewardText));
            }

            if (NetworkManager.isActive()) {
                NetworkManager.getInstance().publishChatGameWin(winnerNickname, answer, player.getUniqueId(),
                        elapsedSeconds, newGlobalRecord, winnerNickname, player.getUniqueId(), elapsedSeconds);
            }

            // Only the server that originated the game schedules the next one
            boolean isOrigin = !NetworkManager.isActive()
                    || NetworkManager.getInstance().getThisServer().equals(origin);
            if (isOrigin) {
                scheduleNextGame(AranarthCore.getInstance());
            }
        });
        return true;
    }

    private static String formatMoney(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        return "$" + nf.format(Math.round(amount));
    }

    /**
     * Called by NetworkManager when another server has started a chat game.
     */
    public static void applyNetworkGameStart(String scrambled, String answer, String originServer) {
        synchronized (gameLock) {
            currentAnswer = answer;
            currentScrambled = scrambled;
            currentGameOrigin = originServer;
            gameStartTime = System.currentTimeMillis();
        }
        String startMsg = ChatUtils.chatMessage("&7Unscramble the following word: &e" + scrambled);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(startMsg);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5f, 1f);
        }
    }

    /**
     * Called by NetworkManager when a player on another server has won the chat game.
     */
    public static void applyNetworkGameWin(Plugin plugin, String winnerNickname, String answer, UUID winnerUUID,
            double elapsedSeconds, boolean newGlobalRecord, String newHolderNickname, UUID newHolderUUID, double newGlobalBestTime) {
        final boolean wasOrigin;
        final int networkStreakCount;
        synchronized (gameLock) {
            wasOrigin = NetworkManager.isActive()
                    && NetworkManager.getInstance().getThisServer().equals(currentGameOrigin);
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
            if (winnerUUID.equals(streakWinnerUUID)) {
                streakCount++;
            } else {
                streakWinnerUUID = winnerUUID;
                streakCount = 1;
            }
            networkStreakCount = streakCount;
        }
        if (newGlobalRecord && newHolderUUID != null) {
            updateGlobalBestCache(newHolderUUID, newHolderNickname, newGlobalBestTime);
        }
        String timeStr = String.format("%.2f", elapsedSeconds);
        String winMsg = ChatUtils.chatMessage("&e" + winnerNickname + " &7guessed &e" + answer + " &7correctly in &e" + timeStr + "s!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(winMsg);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
        if (networkStreakCount >= 3) {
            String streakMsg = ChatUtils.chatMessage("&e" + winnerNickname + " &7is on a &e" + networkStreakCount + "x &7unscramble streak!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(streakMsg);
            }
        }
        if (wasOrigin) {
            scheduleNextGame(plugin);
        }
    }

    /**
     * Called by NetworkManager when the game on another server expired with no winner.
     */
    public static void applyNetworkGameExpire(Plugin plugin, String answer) {
        final boolean wasOrigin;
        synchronized (gameLock) {
            if (!answer.equals(currentAnswer)) {
                return; // Already handled locally
            }
            wasOrigin = NetworkManager.isActive()
                    && NetworkManager.getInstance().getThisServer().equals(currentGameOrigin);
            currentAnswer = null;
            currentScrambled = null;
            currentGameOrigin = null;
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
            streakWinnerUUID = null;
            streakCount = 0;
        }
        String expireMsg = ChatUtils.chatMessage("&7Nobody guessed! The word was &e" + answer);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(expireMsg);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1.0f);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.1f);
        }
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
