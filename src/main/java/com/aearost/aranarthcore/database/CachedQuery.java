package com.aearost.aranarthcore.database;

import com.aearost.aranarthcore.AranarthCore;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs an expensive database query off the main thread and reuses its result for a while, so commands
 * that read the whole player table cannot be spammed to lag the server.
 *
 * @param <T> The type of the query result.
 */
public class CachedQuery<T> {

    private final Supplier<T> query;
    private final long maxAgeMs;
    private final List<Consumer<T>> waiting = new ArrayList<>();
    private T cached;
    private long cachedAt;
    private boolean isLoading = false;

    /**
     * @param query    The query, which is run asynchronously.
     * @param maxAgeMs How long a result is reused before the query runs again.
     */
    public CachedQuery(Supplier<T> query, long maxAgeMs) {
        this.query = query;
        this.maxAgeMs = maxAgeMs;
    }

    /**
     * Provides the result on the main thread, from the cache when it is recent enough.
     * Must be called from the main thread.
     *
     * @param callback Receives the result on the main thread.
     */
    public void get(Consumer<T> callback) {
        if (cached != null && System.currentTimeMillis() - cachedAt < maxAgeMs) {
            callback.accept(cached);
            return;
        }
        waiting.add(callback);
        if (isLoading) {
            return;
        }
        isLoading = true;
        Bukkit.getScheduler().runTaskAsynchronously(AranarthCore.getInstance(), () -> {
            T result = query.get();
            Bukkit.getScheduler().runTask(AranarthCore.getInstance(), () -> {
                cached = result;
                cachedAt = System.currentTimeMillis();
                isLoading = false;
                List<Consumer<T>> callbacks = new ArrayList<>(waiting);
                waiting.clear();
                for (Consumer<T> waitingCallback : callbacks) {
                    waitingCallback.accept(result);
                }
            });
        });
    }
}
