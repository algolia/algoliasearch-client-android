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
 * <h3>Preventive offline searches</h3>
 *
 * <p>When the index is mirrored, it may launch a "preventive" request to the offline mirror for every online search
 * request. <strong>This may result in the completion handler being called twice:</strong> a first time with the
 * offline results, and a second time with the online results. This behavior may be turned off by calling
 * {@link #setPreventiveOfflineSearch(boolean)}.</p>
 *
 * <p> To avoid wasting CPU when the network connection is good, the offline request is only launched after a certain
 * delay. This delay can be adjusted by calling {@link #setPreventiveOfflineSearchDelay(long)}. The default is
 * {@link #DEFAULT_PREVENTIVE_OFFLINE_SEARCH_DELAY}. If the online request finishes with a definitive result (i.e. success
 * or application error) before the offline request has finished (or even been launched), the offline request will be
 * cancelled (or not be launched at all).</p>
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

    /**
     * Whether to launch a preventive offline search for every online search.
     * Only valid when the index is mirrored.
     */
    private boolean preventiveOfflineSearch = true;

    /** The delay before a preventive offline search is launched. */
    private long preventiveOfflineSearchDelay = DEFAULT_PREVENTIVE_OFFLINE_SEARCH_DELAY;

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

    /** Default delay before preventive offline search (in milliseconds). */
    public static final long DEFAULT_PREVENTIVE_OFFLINE_SEARCH_DELAY = 200; // 200 ms

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
    protected void ensureLocalIndex()
    {
        if (localIndex == null) {
            localIndex = new LocalIndex(getClient().getRootDataDir().getAbsolutePath(), getClient().getApplicationID(), getIndexName());
        }
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

    /**
     * Get the delay before a preventive offline search in launched.
     * Only used when the index is mirrored.
     *
     * @return The delay (in milliseconds).
     */
    public long getPreventiveOfflineSearchDelay() {
        return preventiveOfflineSearchDelay;
    }

    /**
     * Set the delay before a preventive offline search in launched.
     * Only used when the index is mirrored.
     *
     * @param preventiveOfflineSearchDelay The delay (in milliseconds).
     */
    public void setPreventiveOfflineSearchDelay(long preventiveOfflineSearchDelay) {
        this.preventiveOfflineSearchDelay = preventiveOfflineSearchDelay;
    }

    /**
     * Whether the index may launch a preventive offline request for every online search request.
     * Only used when the index is mirrored.
     *
     * @return true if preventive offline requests are allowed, false if they are forbidden.
     */
    public boolean isPreventiveOfflineSearch() {
        return preventiveOfflineSearch;
    }

    /**
     * Set whether
     * @param preventiveOfflineSearch
     */
    public void setPreventiveOfflineSearch(boolean preventiveOfflineSearch) {
        this.preventiveOfflineSearch = preventiveOfflineSearch;
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
        getClient().buildExecutorService.submit(new Runnable()
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
            ensureLocalIndex();
            String[] objectFilePaths = new String[objectFiles.size()];
            for (int i = 0; i < objectFiles.size(); ++i)
                objectFilePaths[i] = objectFiles.get(i).getAbsolutePath();
            int status = localIndex.build(settingsFile.getAbsolutePath(), objectFilePaths, true /* clearIndex */);
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
     * <p>
     * WARNING: Can only be used when the index is mirrored.
     * </p>
     */
    private class OnlineOfflineSearchRequest implements Request {
        private final Query query;
        private CompletionHandler completionHandler;
        private boolean cancelled = false;
        private Request onlineRequest;
        private Request offlineRequest;
        private transient boolean mayRunOfflineRequest = true;
        private Runnable startOfflineRunnable;

        /**
         * Construct a new mixed online/offline request.
         *
         * @throws IllegalStateException if the index is not mirrored.
         */
        public OnlineOfflineSearchRequest(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
            if (!mirrored) {
                throw new IllegalStateException();
            }
            this.query = query;
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

        public OnlineOfflineSearchRequest start() {
            // WARNING: All callbacks must run sequentially; we cannot afford race conditions between them.
            // Since most methods use the main thread for callbacks, we have to use it as well.

            // Launch an online request immediately.
            onlineRequest = MirroredIndex.super.searchAsync(query, new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    if (error != null && error.isTransient()) {
                        startOffline();
                    } else {
                        cancelOffline();
                        if (content != null) {
                            addOriginRemote(content);
                        }
                        completionHandler.requestCompleted(content, error);
                    }
                }
            });

            // Preventive offline mode: schedule an offline request to start after a certain delay.
            if (preventiveOfflineSearch) {
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
                getClient().mainHandler.postDelayed(startOfflineRunnable, preventiveOfflineSearchDelay);
            }

            return this;
        }

        private void startOffline() {
            offlineRequest = searchMirrorAsync(query, new CompletionHandler() {
                @Override
                public void requestCompleted(JSONObject content, AlgoliaException error) {
                    // NOTE: If we reach this handler, it means the offline request has not been cancelled.
                    // WARNING: A 404 error likely indicates that the local mirror has not been synced yet,
                    // so we absorb it (gulp).
                    if (error != null && error.getStatusCode() == 404) {
                        return;
                    }
                    completionHandler.requestCompleted(content, error);
                }
            });
        }

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

        /**
         * Add to the content an indication that the results came from the online API.
         *
         * @param content The JSON content to be patched.
         */
        private void addOriginRemote(@NonNull JSONObject content) {
            try {
                content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            }
            catch (JSONException e) {
                throw new RuntimeException("Failed to patch online result JSON", e); // should never happen
            }
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
    public Request searchMirrorAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return _searchMirror(queryCopy);
            }
        }.start();
    }

    private JSONObject _searchMirror(@NonNull Query query) throws AlgoliaException
    {
        try {
            ensureLocalIndex();
            SearchResults searchResults = localIndex.search(query.build());
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
        return getClient().new AsyncTaskRequest(completionHandler) {
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
        return getClient().new AsyncTaskRequest(completionHandler) {
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
            ensureLocalIndex();
            SearchResults searchResults = localIndex.browse(query.build());
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
