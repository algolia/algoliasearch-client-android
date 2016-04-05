package com.algolia.search.saas;

import android.support.v4.util.LruCache;
import android.util.Pair;

import java.util.concurrent.TimeUnit;

/**
 * A cache that holds strong references to a limited number of values for a limited time.
 */
class ExpiringCache<K, V> {
    public static final TimeUnit expirationTimeUnit = TimeUnit.SECONDS;
    public static final int defaultExpirationTimeout = 2;
    public static final int defaultMaxSize = 64;
    public final int expirationTimeout; // Time after which a cache entry is invalidated

    private final LruCache<K, Pair<V, Long>> lruCache;

    public ExpiringCache(final int timeout, final int maxSize) {
        lruCache = new LruCache<>(maxSize);
        expirationTimeout = timeout;
    }

    public ExpiringCache() {
        this(defaultExpirationTimeout, defaultMaxSize);
    }


    /**
     * Puts a value in the cache, computing an expiration time
     *
     * @return the previous value for this key, if any
     */
    public V put(K key, V value) {
        V previous = null;

        synchronized (this) {
            long timeout = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(expirationTimeout, expirationTimeUnit);
            final Pair<V, Long> previousPair = lruCache.put(key, new Pair<>(value, timeout));
            if (previousPair != null) {
                previous = previousPair.first;
            }
        }
        return previous;
    }

    /**
     * Get a value from the cache
     *
     * @return the cached value if it is still valid
     */
    synchronized public V get(K key) {
        final Pair<V, Long> cachePair = lruCache.get(key);
        if (cachePair != null && cachePair.first != null) {
            if (cachePair.second > System.currentTimeMillis()) {
                return cachePair.first;
            } else {
                lruCache.remove(key);
            }
        }
        return null;
    }

    /**
     * @return the number of entries in the cache.
     */
    public int size() {
        return lruCache.size();
    }

    /**
     * Reset the cache, keeping the current settings.
     */
    public void reset() {
        lruCache.evictAll();
    }
}
