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

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.algolia.search.saas.listeners.SearchListener;
import com.algolia.search.sdk.LocalIndex;
import com.algolia.search.sdk.SearchResults;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * An online index that can also be mirrored locally.
 *
 * @note Requires Algolia's SDK.
 */
public class MirroredIndex extends Index
{
    private LocalIndex localIndex;

    private boolean mirrored;
    private MirrorSettings mirrorSettings = new MirrorSettings();
    private long delayBetweenSyncs = 1000 * 60 * 60; // 1 hour

    private boolean syncing;
    private File tmpDir;
    private File settingsFile;
    private List<File> objectFiles;
    private Throwable error;
    private SyncStats stats;

    private Set<SyncListener> syncListeners = new HashSet<>();

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    /** Key used to indicate the origin of results in the returned JSON. */
    public static final String JSON_KEY_ORIGIN = "origin";

    /** Value for `JSON_KEY_ORIGIN` indicating that the results come from the local mirror. */
    public static final String JSON_VALUE_ORIGIN_LOCAL = "local";

    /** Value for `JSON_KEY_ORIGIN` indicating that the results come from the online API. */
    public static final String JSON_VALUE_ORIGIN_REMOTE = "remote";

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    protected MirroredIndex(OfflineAPIClient client, String indexName)
    {
        super(client, indexName);
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    public OfflineAPIClient getClient()
    {
        return (OfflineAPIClient)super.getClient();
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
     * @note All queries are implicitly browse queries (and not search queries). The maximum number of items is
     * specified using the `hitsPerPage` property.
     * FIXME: We need a better mechanism.
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
    public void setDataSelectionQueries(@NonNull DataSelectionQuery[] queries)
    {
        mirrorSettings.setQueries(queries);
        mirrorSettings.setQueriesModificationDate(new Date());
        saveMirrorSettings();
    }

    public @NonNull DataSelectionQuery[] getDataSelectionQueries()
    {
        return mirrorSettings.getQueries();
    }

    public long getDelayBetweenSyncs()
    {
        return delayBetweenSyncs;
    }

    public void setDelayBetweenSyncs(long delayBetweenSyncs)
    {
        this.delayBetweenSyncs = delayBetweenSyncs;
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

        public DataSelectionQuery(Query query, int maxObjects)
        {
            this.query = query;
            this.maxObjects = maxObjects;
        }
    }

    public void sync()
    {
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
        // Notify listeners (make sure it is on main thread).
        mainHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                fireSyncDidStart();
            }
        });
    }

    public void syncIfNeeded()
    {
        long currentDate = System.currentTimeMillis();
        if (currentDate - mirrorSettings.getLastSyncDate().getTime() > delayBetweenSyncs || mirrorSettings.getQueriesModificationDate().compareTo(mirrorSettings.getLastSyncDate()) > 0) {
            sync();
        }
    }

    /**
     * Refresh the local mirror.
     * @warning Should be called from a background thread.
     */
    private void _sync()
    {
        if (!mirrored)
            throw new IllegalArgumentException("Mirroring not activated on this index");

        // Reset statistics.
        stats = new SyncStats();
        long startTime = System.currentTimeMillis();

        try {
            // Create temporary directory.
            tmpDir = new File(getTempDir(), UUID.randomUUID().toString());
            tmpDir.mkdirs();

            // TODO: We are doing everything sequentially so far.
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
            for (int i = 0; i < queries.length; ++i) {
                DataSelectionQuery query = queries[i];
                String queryString = query.query.build();
                String cursor = null;
                int retrievedObjects = 0;
                do {
                    // Make next request.
                    // TODO: JSON DOM is not strictly necessary. We could use SAX parsing and just extract the cursor.
                    String url = "/1/indexes/" + getEncodedIndexName() + "/browse?" + queryString;
                    if (cursor != null) {
                        url += "&cursor=" + URLEncoder.encode(cursor, "UTF-8");
                    }
                    JSONObject objectsJSON = getClient().getRequest(url, true);

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
                        Log.e(this.getClass().getName(), "No hits in result for query: " + queryString);
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
            int status = localIndex.build(settingsFile.getAbsolutePath(), objectFilePaths);
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
            mainHandler.post(new Runnable()
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
     * @param listener Listener to be notified of search results.
     */
    public void searchASync(Query query, SearchListener listener)
    {
        new SearchTask().execute(new TaskParams.Search(listener, query));
    }

    private class SearchTask extends AsyncTask<TaskParams.Search, Void, TaskParams.Search>
    {
        private SearchListener listener;
        private Query query;

        @Override
        protected TaskParams.Search doInBackground(TaskParams.Search... params)
        {
            TaskParams.Search p = params[0];
            listener = p.listener;
            query = p.query;
            // First search the online API.
            try {
                p.content = search(p.query);
                // Indicate that the results come from the online API.
                p.content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            }
            catch (AlgoliaException e) {
                // Fallback to the offline mirror if available.
                if (mirrored) {
                    try {
                        p.content = _searchMirror(query.build());
                    }
                    catch (AlgoliaException e2) {
                        p.error = e2;
                    }
                }
                else {
                    p.error = e;
                }
            }
            catch (JSONException e) {
                // Should never happen.
                p.error = new AlgoliaException("Failed to patch online result JSON", e);
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Search p)
        {
            p.sendResult(MirroredIndex.this);
        }
    }

    /**
     * Search the local mirror.
     *
     * @param query Search query.
     * @param listener Listener to be notified of search results.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public void searchMirrorAsync(Query query, SearchListener listener)
    {
        if (!mirrored)
            throw new IllegalStateException("Mirroring not activated on this index");
        new SearchMirrorTask().execute(new TaskParams.Search(listener, query));
    }

    private class SearchMirrorTask extends AsyncTask<TaskParams.Search, Void, TaskParams.Search>
    {
        @Override
        protected TaskParams.Search doInBackground(TaskParams.Search... params)
        {
            TaskParams.Search p = params[0];
            Query query = p.query;
            try {
                p.content = _searchMirror(query.build());
            }
            catch (AlgoliaException e) {
                p.error = e;
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Search p)
        {
            p.sendResult(MirroredIndex.this);
        }
    }

    private JSONObject _searchMirror(String query) throws AlgoliaException
    {
        try {
            ensureLocalIndex();
            SearchResults searchResults = localIndex.search(query);
            if (searchResults.statusCode == 200) {
                String jsonString = new String(searchResults.data, "UTF-8");
                JSONObject json = new JSONObject(jsonString);
                // Indicate that the results come from the local mirror.
                json.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_LOCAL);
                return json;
            }
            else {
                throw new AlgoliaException(searchResults.errorMessage, searchResults.statusCode);
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
     * Browse the local mirror.
     *
     * @param query Browse query. Same restrictions as the online API.
     * @param listener Listener to be notified of results.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public void browseMirrorAsync(Query query, SearchListener listener)
    {
        if (!mirrored)
            throw new IllegalStateException("Mirroring not activated on this index");
        new BrowseMirrorTask().execute(new TaskParams.Search(listener, query));
    }

    private class BrowseMirrorTask extends AsyncTask<TaskParams.Search, Void, TaskParams.Search>
    {
        @Override
        protected TaskParams.Search doInBackground(TaskParams.Search... params)
        {
            TaskParams.Search p = params[0];
            Query query = p.query;
            try {
                p.content = _browseMirror(query.build());
            }
            catch (AlgoliaException e) {
                p.error = e;
            }
            return p;
        }

        @Override
        protected void onPostExecute(TaskParams.Search p)
        {
            p.sendResult(MirroredIndex.this);
        }
    }

    private JSONObject _browseMirror(String query) throws AlgoliaException
    {
        try {
            ensureLocalIndex();
            SearchResults searchResults = localIndex.browse(query);
            if (searchResults.statusCode == 200) {
                String jsonString = new String(searchResults.data, "UTF-8");
                JSONObject json = new JSONObject(jsonString);
                // Indicate that the results come from the local mirror.
                json.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_LOCAL);
                return json;
            }
            else {
                throw new AlgoliaException(searchResults.errorMessage, searchResults.statusCode);
            }
        }
        catch (Exception e) {
            throw new AlgoliaException("Search failed", e);
        }
    }

    // ----------------------------------------------------------------------
    // Listeners
    // ----------------------------------------------------------------------

    public void addSyncListener(SyncListener listener)
    {
        syncListeners.add(listener);
    }

    public void removeSyncListener(SyncListener listener)
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
