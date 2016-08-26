/*
 * Copyright (c) 2012-2016 Algolia
 * http://www.algolia.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.algolia.search.saas;

import android.support.annotation.NonNull;
import android.util.Log;

import com.algolia.search.offline.core.LocalIndex;
import com.algolia.search.offline.core.SearchResults;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * An online index that can also be mirrored locally.
 *
 * <p>When created, an instance of this class has its <code>mirrored</code> flag set to false, and behaves like a normal,
 * online {@link Index}. When the <code>mirrored</code> flag is set to true, the index becomes capable of acting upon
 * local data.</p>
 *
 * <p>It is a programming error to call methods acting on the local data when <code>mirrored</code> is false. Doing so
 * will result in an {@link IllegalStateException} being thrown.</p>
 *
 * <p>Native resources are lazily instantiated at the first method call requiring them. They are released when the
 * object is garbage-collected. Although the client guards against concurrent accesses, it is strongly discouraged
 * to create more than one <code>MirroredIndex</code> instance pointing to the same index, as that would duplicate
 * native resources.</p>
 *
 * <p>NOTE: Requires Algolia's SDK. The {@link OfflineClient#enableOfflineMode(String)} method must be called with
 * a valid license key prior to calling any offline-related method.</p>
 *
 * <h3>Request strategy</h3>
 *
 * When the index is mirrored and the device is online, it becomes possible to transparently switch between online and
 * offline requests. There is no single best strategy for that, because it depends on the use case and the current
 * network conditions. You can choose the strategy through {@link #setRequestStrategy(Strategy)}. The default is
 * {@link Strategy#FALLBACK_ON_FAILURE}, which will always target the online API first, then fallback to the offline
 * mirror in case of failure (including network unavailability).
 *
 * NOTE: If you want to explicitly target either the online API or the offline mirror, doing so is always possible
 * using the {@link #searchOnlineAsync} or {@link #searchOfflineAsync}` methods.
 *
 * NOTE: The strategy applies both to {@link #searchAsync} and {@link #searchDisjunctiveFacetingAsync}..
 */
public class MirroredIndex extends Index
{
    private LocalIndex localIndex;

    private boolean mirrored;
    private MirrorSettings mirrorSettings = new MirrorSettings();
    private long delayBetweenSyncs = DEFAULT_DELAY_BETWEEN_SYNCS;

    private boolean syncing;
    private File tmpDir;
    private File settingsFile;
    private List<File> objectFiles;
    private Throwable error;
    private SyncStats stats;

    private Set<SyncListener> syncListeners = new HashSet<>();

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /** Key used to indicate the origin of results in the returned JSON. */
    public static final String JSON_KEY_ORIGIN = "origin";

    /** Value for `JSON_KEY_ORIGIN` indicating that the results come from the local mirror. */
    public static final String JSON_VALUE_ORIGIN_LOCAL = "local";

    /** Value for `JSON_KEY_ORIGIN` indicating that the results come from the online API. */
    public static final String JSON_VALUE_ORIGIN_REMOTE = "remote";

    /** Default minimum delay between two syncs (in milliseconds). */
    public static final long DEFAULT_DELAY_BETWEEN_SYNCS = 1000 * 60 * 60 * 24; // 1 day

    /** Default delay before launching an offline request (in milliseconds). */
    public static final long DEFAULT_OFFLINE_FALLBACK_TIMEOUT = 1000; // 1s

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    protected MirroredIndex(@NonNull OfflineClient client, @NonNull String indexName)
    {
        super(client, indexName);
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public OfflineClient getClient()
    {
        return (OfflineClient)super.getClient();
    }

    public boolean isMirrored()
    {
        return mirrored;
    }

    public void setMirrored(boolean mirrored)
    {
        if (!this.mirrored && mirrored) {
            loadMirroSettings();
        }
        this.mirrored = mirrored;
    }

    /**
     * Add a data selection query to this index.
     * NOTE: All queries are implicitly browse queries (and not search queries).
     *
     * @param query The data selection query to add.
     */
    public void addDataSelectionQuery(@NonNull DataSelectionQuery query)
    {
        mirrorSettings.addQuery(query);
        mirrorSettings.setQueriesModificationDate(new Date());
        saveMirrorSettings();
    }

    /**
     * Replace all data selection queries associated to this index.
     * @param queries The new data selection queries. (May be empty, although this will actually empty your mirror!)
     */
    public void setDataSelectionQueries(@NonNull DataSelectionQuery... queries)
    {
        DataSelectionQuery[] oldQueries = mirrorSettings.getQueries();
        if (!Arrays.equals(oldQueries, queries)) {
            mirrorSettings.setQueries(queries);
            mirrorSettings.setQueriesModificationDate(new Date());
            saveMirrorSettings();
        }
    }

    public @NonNull DataSelectionQuery[] getDataSelectionQueries()
    {
        return mirrorSettings.getQueries();
    }

    public long getDelayBetweenSyncs()
    {
        return delayBetweenSyncs;
    }

    /**
     * Set the delay after which data is considered to be obsolete.
     * @param delayBetweenSyncs The delay between syncs, in milliseconds.
     */
    public void setDelayBetweenSyncs(long delayBetweenSyncs)
    {
        if (delayBetweenSyncs <= 0) {
            throw new IllegalArgumentException();
        }
        this.delayBetweenSyncs = delayBetweenSyncs;
    }

    /**
     * Get the delay between two syncs.
     *
     * @param unit The unit in which the result will be expressed.
     * @return The delay, expressed in <code>unit</code>.
     */
    public long getDelayBetweenSyncs(@NonNull TimeUnit unit) {
        return unit.convert(this.getDelayBetweenSyncs(), TimeUnit.MILLISECONDS);
    }

    /**
     * Set the delay after which data is considered to be obsolete.
     * @param duration The delay between syncs, expressed in <code>unit</code>.
     * @param unit The unit in which <code>duration</code> is expressed.
     */
    public void setDelayBetweenSyncs(long duration, @NonNull TimeUnit unit) {
        this.setDelayBetweenSyncs(TimeUnit.MILLISECONDS.convert(duration, unit));
    }

    /**
     * Lazy instantiate the local index.
     */
    private synchronized void ensureLocalIndex()
    {
        if (localIndex == null) {
            localIndex = new LocalIndex(getClient().getRootDataDir().getAbsolutePath(), getClient().getApplicationID(), getIndexName());
        }
    }

    /**
     * Get the local index, lazy instantiating it if needed.
     *
     * @return The local index.
     */
    protected LocalIndex getLocalIndex() {
        ensureLocalIndex();
        return localIndex;
    }

    private File getTempDir()
    {
        // TODO: Use better value
        return getClient().getRootDataDir();
    }

    private File getDataDir()
    {
        return new File(new File(getClient().getRootDataDir(), getClient().getApplicationID()), getIndexName());
    }

    private File getSettingsFile()
    {
        return new File(getDataDir(), "mirror.json");
    }

    // ----------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------

    private void saveMirrorSettings()
    {
        File dataDir = getDataDir();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        mirrorSettings.save(getSettingsFile());
    }

    private void loadMirroSettings()
    {
        File settingsFile = getSettingsFile();
        if (settingsFile.exists()) {
            mirrorSettings.load(settingsFile);
        }
    }

    // ----------------------------------------------------------------------
    // NOTE: THREAD-SAFETY
    // ----------------------------------------------------------------------
    // It is the client's responsibility to guard against concurrent access on a local index. The native SDK doesn't
    // do it. Therefore:
    //
    // - Sync uses a synchronized boolean as mutex (`syncing`).
    // - All syncs for all indices are executed on a sequential queue. (Building an index is CPU and memory intensive
    //   and we don't want to kill the device!)
    //
    // NOTE: Although the SDK supports concurrent read accesses, search and browse use `AsyncTask`s, which are always
    // executed sequentially since Android 3.0 (see <http://developer.android.com/reference/android/os/AsyncTask.html>).
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Sync
    // ----------------------------------------------------------------------

    /**
     * Statistics about a sync.
     */
    public static class SyncStats
    {
        protected int objectCount;
        protected int fileCount;
        protected long fetchTime;
        protected long buildTime;
        protected long totalTime;

        public int getObjectCount()
        {
            return objectCount;
        }

        public int getFileCount()
        {
            return fileCount;
        }

        public long getFetchTime()
        {
            return fetchTime;
        }

        public long getBuildTime()
        {
            return buildTime;
        }

        public long getTotalTime()
        {
            return totalTime;
        }

        @Override public String toString()
        {
            return String.format("%s{objects=%d, files=%d, fetch=%dms, build=%dms, total=%dms}", this.getClass().getSimpleName(), objectCount, fileCount, fetchTime, buildTime, totalTime);
        }
    }

    /**
     * A data selection query.
     */
    public static class DataSelectionQuery
    {
        /**
         * Query parameters. Remember that data selection queries are browse queries, so certain options will not work.
         */
        public Query query;

        /** Maximum number of objects to retrieve. */
        public int maxObjects;

        public DataSelectionQuery(@NonNull Query query, int maxObjects)
        {
            if (maxObjects < 0) {
                throw new IllegalArgumentException();
            }
            this.query = query;
            this.maxObjects = maxObjects;
        }

        // ----------------------------------------------------------------------
        // Equality
        // ----------------------------------------------------------------------

        @Override
        public boolean equals(Object other) {
            return other != null && other instanceof DataSelectionQuery && this.query.equals(((DataSelectionQuery)other).query) && this.maxObjects == ((DataSelectionQuery)other).maxObjects;
        }

        @Override
        public int hashCode() {
            return query.hashCode() ^ maxObjects;
        }
    }

    /**
     * Launch a sync.
     * If a sync is already running, this call is ignored. Otherwise, the sync is enqueued and runs in the background.
     *
     * NOTE: All index syncs are sequential: no two syncs can run at the same time.
     *
     * @throws IllegalStateException If no data selection queries were set.
     */
    public void sync()
    {
        if (getDataSelectionQueries().length == 0) {
            throw new IllegalStateException("Cannot sync with empty data selection queries");
        }
        synchronized (this) {
            if (syncing)
                return;
            syncing = true;
        }
        getClient().localBuildExecutorService.submit(new Runnable()
        {
            @Override
            public void run()
            {
                _sync();
            }
        });
    }

    /**
     * Launch a sync only if the data is obsolete.
     * The data is obsolete if the last successful sync is older than the delay between syncs, or if the data selection
     * queries have been changed in the meantime.
     *
     * @throws IllegalStateException If no data selection queries were set.
     */
    public void syncIfNeeded()
    {
        long currentDate = System.currentTimeMillis();
        if (currentDate - mirrorSettings.getLastSyncDate().getTime() > delayBetweenSyncs || mirrorSettings.getQueriesModificationDate().compareTo(mirrorSettings.getLastSyncDate()) > 0) {
            sync();
        }
    }

    /**
     * Refresh the local mirror.
     * WARNING: Should be called from a background thread.
     */
    private void _sync()
    {
        if (!mirrored)
            throw new IllegalArgumentException("Mirroring not activated on this index");

        // Reset statistics.
        stats = new SyncStats();
        long startTime = System.currentTimeMillis();

        // Notify listeners (on main thread).
        getClient().mainHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                fireSyncDidStart();
            }
        });

        try {
            // Create temporary directory.
            tmpDir = new File(getTempDir(), UUID.randomUUID().toString());
            tmpDir.mkdirs();

            // NOTE: We are doing everything sequentially, because this is a background job: we care more about
            // resource consumption than about how long it will take.

            // Fetch settings.
            {
                JSONObject settingsJSON = this.getSettings();
                settingsFile = new File(tmpDir, "settings.json");
                String data = settingsJSON.toString();
                Writer writer = new OutputStreamWriter(new FileOutputStream(settingsFile), "UTF-8");
                writer.write(data);
                writer.close();
            }

            // Perform data selection queries.
            objectFiles = new ArrayList<>();
            final DataSelectionQuery[] queries = mirrorSettings.getQueries();
            for (DataSelectionQuery query : queries) {
                String cursor = null;
                int retrievedObjects = 0;
                do {
                    // Make next request.
                    JSONObject objectsJSON = cursor == null ? this.browse(query.query) : this.browseFrom(cursor);

                    // Write result to file.
                    int objectFileNo = objectFiles.size();
                    File file = new File(tmpDir, String.format("%d.json", objectFileNo));
                    objectFiles.add(file);
                    String data = objectsJSON.toString();
                    Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                    writer.write(data);
                    writer.close();

                    cursor = objectsJSON.optString("cursor");
                    JSONArray hits = objectsJSON.optJSONArray("hits");
                    if (hits == null) {
                        // Something went wrong:
                        // Report the error, and just abort this batch and proceed with the next query.
                        Log.e(this.getClass().getName(), "No hits in result for query: " + query.query);
                        break;
                    }
                    retrievedObjects += hits.length();
                }
                while (retrievedObjects < query.maxObjects && cursor != null);

                stats.objectCount += retrievedObjects;
            }

            // Update statistics.
            long afterFetchTime = System.currentTimeMillis();
            stats.fetchTime = afterFetchTime - startTime;
            stats.fileCount = objectFiles.size();

            // Build the index.
            String[] objectFilePaths = new String[objectFiles.size()];
            for (int i = 0; i < objectFiles.size(); ++i)
                objectFilePaths[i] = objectFiles.get(i).getAbsolutePath();
            int status = getLocalIndex().build(settingsFile.getAbsolutePath(), objectFilePaths, true /* clearIndex */, null /* deletedObjectIDs */);
            if (status != 200) {
                throw new AlgoliaException("Build index failed", status);
            }

            // Update statistics.
            long afterBuildTime = System.currentTimeMillis();
            stats.buildTime = afterBuildTime - afterFetchTime;
            stats.totalTime = afterBuildTime - startTime;

            // Remember the last sync date.
            mirrorSettings.setLastSyncDate(new Date());
            saveMirrorSettings();

            // Log statistics.
            Log.d(this.getClass().getName(), "Sync stats: " + stats);
        }
        catch (Exception e) {
            Log.e(this.getClass().getName(), "Sync failed", e);
            error = e;
        }
        finally {
            // Clean up.
            if (tmpDir != null) {
                FileUtils.deleteRecursive(tmpDir);
                tmpDir = null;
            }
            settingsFile = null;
            objectFiles = null;

            // Mark sync as finished.
            synchronized (this) {
                syncing = false;
            }

            // Notify listeners (on main thread).
            getClient().mainHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    fireSyncDidFinish();
                }
            });
        }
    }

    // ----------------------------------------------------------------------
    // Search
    // ----------------------------------------------------------------------

    /**
     * Strategy to choose between online and offline search.
     */
    public enum Strategy {
        /**
         * Search online only.
         * The search will fail when the API can't be reached.
         *
         * NOTE: You might consider that this defeats the purpose of having a mirror in the first place... But this
         * is intended for applications wanting to manually manage their policy.
         */
        ONLINE_ONLY,

        /**
         * Search offline only.
         * The search will fail when the offline mirror has not yet been synced.
         */
        OFFLINE_ONLY,

        /**
         * Search online, then fallback to offline on failure.
         * Please note that when online, this is likely to hit the request timeout on <em>every host</em> before
         * failing.
         */
        FALLBACK_ON_FAILURE,

        /**
         * Fallback after a certain timeout.
         * Will first try an online request, but fallback to offline in case of failure or when a timeout has been
         * reached, whichever comes first.
         *
         * The timeout can be set through {@link #setOfflineFallbackTimeout(long)}.
         */
        FALLBACK_ON_TIMEOUT
    }

    /** Strategy to use for offline fallback. Default = {@link Strategy#FALLBACK_ON_FAILURE}. */
    private Strategy requestStrategy = Strategy.FALLBACK_ON_FAILURE;

    public Strategy getRequestStrategy() {
        return requestStrategy;
    }

    public void setRequestStrategy(Strategy requestStrategy) {
        this.requestStrategy = requestStrategy;
    }

    /**
     * Timeout used to control offline fallback (ms).
     *
     * NOTE: Only used by the {@link Strategy#FALLBACK_ON_TIMEOUT} strategy.
     */
    private long offlineFallbackTimeout = DEFAULT_OFFLINE_FALLBACK_TIMEOUT;

    public long getOfflineFallbackTimeout() {
        return offlineFallbackTimeout;
    }

    public void setOfflineFallbackTimeout(long offlineFallbackTimeout) {
        this.offlineFallbackTimeout = offlineFallbackTimeout;
    }

    public void setOfflineFallbackTimeout(long offlineFallbackTimeout, TimeUnit unit) {
        this.offlineFallbackTimeout = unit.convert(offlineFallbackTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Search the online API, falling back to the local mirror if enabled in case of error.
     *
     * @param query Search query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    @Override
    public Request searchAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        // A non-mirrored index behaves exactly as an online index.
        if (!mirrored) {
            return super.searchAsync(query, completionHandler);
        }
        // A mirrored index launches a mixed offline/online request.
        else {
            final Query queryCopy = new Query(query);
            return new OnlineOfflineSearchRequest(queryCopy, completionHandler).start();
        }
    }

    /**
     * A mixed online/offline request.
     * This request encapsulates two concurrent online and offline requests, to optimize response time.
     */
    private abstract class OnlineOfflineRequest implements Request {
        private CompletionHandler completionHandler;
        private boolean cancelled = false;
        private Request onlineRequest;
        private Request offlineRequest;
        private transient boolean mayRunOfflineRequest = true;
        private Runnable startOfflineRunnable;

        /**
         * Construct a new mixed online/offline request.
         */
        public OnlineOfflineRequest(@NonNull CompletionHandler completionHandler) {
            if (!mirrored) {
                throw new IllegalStateException("This index is not mirrored");
            }
            this.completionHandler = completionHandler;
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                if (onlineRequest != null) {
                    onlineRequest.cancel();
                }
                if (offlineRequest != null) {
                    offlineRequest.cancel();
                }
                cancelled = true;
            }
        }

        @Override
        public boolean isFinished() {
            return onlineRequest.isFinished() && (offlineRequest == null || offlineRequest.isFinished());
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public OnlineOfflineRequest start() {
            // WARNING: All callbacks must run sequentially; we cannot afford race conditions between them.
            // Since most methods use the main thread for callbacks, we have to use it as well.

            // If the strategy is "offline only", well, go offline straight away.
            if (requestStrategy == Strategy.OFFLINE_ONLY) {
                startOffline();
            }
            // Otherwise, always launch an online request.
            else {
                if (requestStrategy == Strategy.ONLINE_ONLY || !getLocalIndex().exists()) {
                    mayRunOfflineRequest = false;
                }
                startOnline();
            }
            if (requestStrategy == Strategy.FALLBACK_ON_TIMEOUT && mayRunOfflineRequest) {
                // Schedule an offline request to start after a certain delay.
                startOfflineRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // If the online request has not returned yet, or has returned an error, use the offline mirror.
                        // Let's also make sure that we don't start the offline request twice.
                        if (mayRunOfflineRequest && offlineRequest == null) {
                            startOffline();
                        }
                    }
                };
                getClient().mainHandler.postDelayed(startOfflineRunnable, offlineFallbackTimeout);
            }
            return this;
        }

        private void startOnline() {
            // Avoid launching the request twice.
            if (onlineRequest != null) {
                return;
            }
            onlineRequest = startOnlineRequest(new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    if (error != null && error.isTransient() && mayRunOfflineRequest) {
                        startOffline();
                    } else {
                        cancelOffline();
                        callCompletion(content, error);
                    }
                }
            });
        }

        private void startOffline() {
            // NOTE: If we reach this handler, it means the offline request has not been cancelled.
            if (!mayRunOfflineRequest) {
                throw new AssertionError("Should never happen");
            }
            // Avoid launching the request twice.
            if (offlineRequest != null) {
                return;
            }
            offlineRequest = startOfflineRequest(new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    if (onlineRequest != null) {
                        onlineRequest.cancel();
                    }
                    callCompletion(content, error);
                }
            });
        }

        protected abstract Request startOnlineRequest(CompletionHandler completionHandler);

        protected abstract Request startOfflineRequest(CompletionHandler completionHandler);

        /**
         * Cancel any pending offline request and prevent a future one from being launched.
         */
        private void cancelOffline() {
            // Flag the offline request as obsolete.
            mayRunOfflineRequest = false;
            // Prevent the start offline request runnable from even running if it is still time.
            if (startOfflineRunnable != null) {
                getClient().mainHandler.removeCallbacks(startOfflineRunnable);
            }
            // Cancel the offline request if already running.
            if (offlineRequest != null) {
                offlineRequest.cancel();
            }
        }

        private void callCompletion(JSONObject content, AlgoliaException error) {
            if (!isCancelled()) {
                completionHandler.requestCompleted(content, error);
            }
        }
    }

    private class OnlineOfflineSearchRequest extends OnlineOfflineRequest {
        private final Query query;

        public OnlineOfflineSearchRequest(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
            super(completionHandler);
            this.query = query;
        }

        @Override
        protected Request startOnlineRequest(CompletionHandler completionHandler) {
            return searchOnlineAsync(query, completionHandler);
        }

        @Override
        protected Request startOfflineRequest(CompletionHandler completionHandler) {
            return searchOfflineAsync(query, completionHandler);
        }
    }

    /**
     * Search the online API.
     *
     * @param query Search query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchOnlineAsync(@NonNull Query query, @NonNull final CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return searchOnline(queryCopy);
            }
        }.start();
    }

    private JSONObject searchOnline(@NonNull Query query) throws AlgoliaException {
        try {
            JSONObject content = super.search(query);
            content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            return content;
        }
        catch (JSONException e) {
            throw new AlgoliaException("Failed to patch JSON result");
        }
    }

    /**
     * Search the local mirror.
     *
     * @param query Search query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request searchOfflineAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return _searchOffline(queryCopy);
            }
        }.start();
    }

    private JSONObject _searchOffline(@NonNull Query query) throws AlgoliaException
    {
        try {
            SearchResults searchResults = getLocalIndex().search(query.build());
            if (searchResults.getStatusCode() == 200) {
                String jsonString = new String(searchResults.getData(), "UTF-8");
                JSONObject json = new JSONObject(jsonString);
                // NOTE: Origin tagging performed by the SDK.
                return json;
            }
            else {
                throw new AlgoliaException(searchResults.getErrorMessage(), searchResults.getStatusCode());
            }
        }
        catch (JSONException | UnsupportedEncodingException e) {
            throw new AlgoliaException("Search failed", e);
        }
    }

    // ----------------------------------------------------------------------
    // Multiple queries
    // ----------------------------------------------------------------------

    @Override
    public Request multipleQueriesAsync(@NonNull List<Query> queries, final Client.MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        // A non-mirrored index behaves exactly as an online index.
        if (!mirrored) {
            return super.multipleQueriesAsync(queries, strategy, completionHandler);
        }
        // A mirrored index launches a mixed offline/online request.
        else {
            final List<Query> queriesCopy = new ArrayList<>(queries.size());
            for (Query query: queries) {
                queriesCopy.add(new Query(query));
            }
            return new OnlineOfflineMultipleQueriesRequest(queriesCopy, strategy, completionHandler).start();
        }
    }

    /**
     * Run multiple queries on this index, explicitly targeting the online API.
     *
     * @param queries Queries to run.
     * @param strategy Strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesOnlineAsync(@NonNull List<Query> queries, final Client.MultipleQueriesStrategy strategy, final @NonNull CompletionHandler completionHandler) {
        final List<Query> queriesCopy = new ArrayList<>(queries.size());
        for (Query query: queries) {
            queriesCopy.add(new Query(query));
        }
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return multipleQueriesOnline(queriesCopy, strategy == null ? null : strategy.toString());
            }
        }.start();
    }

    /**
     * Run multiple queries on this index, explicitly targeting the offline mirror.
     *
     * @param queries Queries to run.
     * @param strategy Strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request multipleQueriesOfflineAsync(final @NonNull List<Query> queries, final Client.MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Offline requests are only available when the index is mirrored");
        }
        final List<Query> queriesCopy = new ArrayList<>(queries.size());
        for (Query query: queries) {
            queriesCopy.add(new Query(query));
        }
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return _multipleQueriesOffline(queriesCopy, strategy == null ? null : strategy.toString());
            }
        }.start();
    }

    private class OnlineOfflineMultipleQueriesRequest extends OnlineOfflineRequest {
        private final List<Query> queries;
        private final Client.MultipleQueriesStrategy strategy;

        public OnlineOfflineMultipleQueriesRequest(@NonNull List<Query> queries, Client.MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
            super(completionHandler);
            this.queries = queries;
            this.strategy = strategy;
        }

        @Override
        protected Request startOnlineRequest(CompletionHandler completionHandler) {
            return multipleQueriesOnlineAsync(queries, strategy, completionHandler);
        }

        @Override
        protected Request startOfflineRequest(CompletionHandler completionHandler) {
            return multipleQueriesOfflineAsync(queries, strategy, completionHandler);
        }
    }

    /**
     * Run multiple queries on this index, explicitly targeting the online API.
     */
    private JSONObject multipleQueriesOnline(@NonNull List<Query> queries, String strategy) throws AlgoliaException {
        try {
            JSONObject content = super.multipleQueries(queries, strategy);
            content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            return content;
        }
        catch (JSONException e) {
            throw new AlgoliaException("Failed to patch JSON result");
        }
    }

    /**
     * Run multiple queries on this index, explicitly targeting the offline mirror.
     */
    private JSONObject _multipleQueriesOffline(@NonNull List<Query> queries, String strategy) throws AlgoliaException
    {
        if (!mirrored) {
            throw new IllegalStateException("Cannot run offline search on a non-mirrored index");
        }
        // TODO: Move to `LocalIndex` to factorize implementation between platforms?
        try {
            JSONArray results = new JSONArray();
            boolean shouldProcess = true;
            for (Query query: queries) {
                // Implement the "stop if enough matches" strategy.
                if (!shouldProcess) {
                    JSONObject returnedContent = new JSONObject()
                        .put("hits", new JSONArray())
                        .put("page", 0)
                        .put("nbHits", 0)
                        .put("nbPages", 0)
                        .put("hitsPerPage", 0)
                        .put("processingTimeMS", 1)
                        .put("params", query.build())
                        .put("index", this.getIndexName())
                        .put("processed", false);
                    results.put(returnedContent);
                    continue;
                }

                JSONObject returnedContent = this._searchOffline(query);
                returnedContent.put("index", this.getIndexName());
                results.put(returnedContent);

                // Implement the "stop if enough matches strategy".
                if (strategy != null && strategy.equals(Client.MultipleQueriesStrategy.STOP_IF_ENOUGH_MATCHES.toString())) {
                    int nbHits = returnedContent.getInt("nbHits");
                    int hitsPerPage = returnedContent.getInt("hitsPerPage");
                    if (nbHits >= hitsPerPage) {
                        shouldProcess = false;
                    }
                }
            }
            return new JSONObject()
                .put("results", results)
                .put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_LOCAL);
        }
        catch (JSONException e) {
            // The `put()` calls should never throw, but the `getInt()` calls may if individual queries return
            // unexpected results.
            throw new AlgoliaException("When running multiple queries", e);
        }
    }

    // ----------------------------------------------------------------------
    // Browse
    // ----------------------------------------------------------------------
    // NOTE: Contrary to search, there is no point in transparently switching from online to offline when browsing,
    // as the results would likely be inconsistent. Anyway, the cursor is not portable across instances, so the
    // fall back could only work for the first query.

    /**
     * Browse the local mirror (initial call).
     * Same semantics as {@link Index#browseAsync}.
     *
     * @param query Browse query. Same restrictions as the online API.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request browseMirrorAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return _browseMirror(queryCopy);
            }
        }.start();
    }

    /**
     * Browse the local mirror (subsequent calls).
     * Same semantics as {@link Index#browseFromAsync}.
     *
     * @param cursor Cursor to browse from.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request browseMirrorFromAsync(@NonNull String cursor, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        final Query query = new Query().set("cursor", cursor);
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return _browseMirror(query);
            }
        }.start();
    }

    private JSONObject _browseMirror(@NonNull Query query) throws AlgoliaException
    {
        try {
            SearchResults searchResults = getLocalIndex().browse(query.build());
            if (searchResults.getStatusCode() == 200) {
                String jsonString = new String(searchResults.getData(), "UTF-8");
                JSONObject json = new JSONObject(jsonString);
                // NOTE: Origin tagging performed by the SDK.
                return json;
            }
            else {
                throw new AlgoliaException(searchResults.getErrorMessage(), searchResults.getStatusCode());
            }
        }
        catch (JSONException | UnsupportedEncodingException e) {
            throw new AlgoliaException("Search failed", e);
        }
    }

    // ----------------------------------------------------------------------
    // Listeners
    // ----------------------------------------------------------------------

    /**
     * Add a listener for sync events.
     * @param listener The listener to add.
     */
    public void addSyncListener(@NonNull SyncListener listener)
    {
        syncListeners.add(listener);
    }

    /**
     * Remove a listener for sync events.
     * @param listener The listener to remove.
     */
    public void removeSyncListener(@NonNull SyncListener listener)
    {
        syncListeners.remove(listener);
    }

    private void fireSyncDidStart()
    {
        for (SyncListener listener : syncListeners) {
            listener.syncDidStart(this);
        }
    }

    private void fireSyncDidFinish()
    {
        for (SyncListener listener : syncListeners) {
            listener.syncDidFinish(this, error, stats);
        }
    }
}
