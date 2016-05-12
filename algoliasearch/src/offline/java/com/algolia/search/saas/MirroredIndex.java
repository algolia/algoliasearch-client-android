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
        final Query queryCopy = new Query(query);
        return new Request(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return search(queryCopy);
            }
        }.start();
    }

    @Override
    protected JSONObject search(@NonNull Query query) throws AlgoliaException {
        try {
            JSONObject content = super.search(query);
            // Indicate that the results come from the online API.
            content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            return content;
        }
        catch (AlgoliaException e) {
            // Fallback to the offline mirror if available.
            // TODO: We might only want to fallback if the error is a network error.
            if (mirrored) {
                return _searchMirror(query);
            } else {
                throw e;
            }
        }
        catch (JSONException e) {
            throw new RuntimeException("Failed to patch online result JSON", e); // should never happen
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
        return new Request(completionHandler) {
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
        catch (Exception e) {
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
        return new Request(completionHandler) {
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
        return new Request(completionHandler) {
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
        catch (Exception e) {
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
