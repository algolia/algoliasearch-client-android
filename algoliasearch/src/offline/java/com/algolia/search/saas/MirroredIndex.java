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
import android.util.Log;

import com.algolia.search.saas.listeners.SearchListener;
import com.algolia.search.sdk.LocalIndex;
import com.algolia.search.sdk.SearchResults;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

    private Set<SyncListener> syncListeners = new HashSet<>();

    private Handler mainHandler = new Handler(Looper.getMainLooper());

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

    public void addDataSelectionQuery(Query query)
    {
        Query tweakedQuery = new Query(query);
        final List<String> emptyList = new ArrayList<String>();
        tweakedQuery.setAttributesToHighlight(emptyList);
        tweakedQuery.setAttributesToSnippet(emptyList);
        tweakedQuery.getRankingInfo(false);
        mirrorSettings.addQuery(tweakedQuery.getQueryString());
        mirrorSettings.setQueriesModificationDate(new Date());
        saveMirrorSettings();
    }

    public String[] getDataSelectionQueries()
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
            final String[] queries = mirrorSettings.getQueries();
            for (int i = 0; i < queries.length; ++i) {
                String query = queries[i];
                JSONObject objectsJSON = getClient().getRequest("/1/indexes/" + getEncodedIndexName() + "?" + query, true);
                File file = new File(tmpDir, String.format("%d.json", i));
                objectFiles.add(file);
                String data = objectsJSON.toString();
                Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                writer.write(data);
                writer.close();
            }

            // Build the index.
            ensureLocalIndex();
            String[] objectFilePaths = new String[objectFiles.size()];
            for (int i = 0; i < objectFiles.size(); ++i)
                objectFilePaths[i] = objectFiles.get(i).getAbsolutePath();
            int status = localIndex.build(settingsFile.getAbsolutePath(), objectFilePaths);
            if (status != 200) {
                throw new AlgoliaException("Build index failed", status);
            }

            // Remember the last sync date.
            mirrorSettings.setLastSyncDate(new Date());
            saveMirrorSettings();
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

    public void searchASync(Query query, SearchListener listener)
    {
        new SearchMirrorTask().execute(new TaskParams.Search(listener, query));
    }

    private class SearchMirrorTask extends AsyncTask<TaskParams.Search, Void, TaskParams.Search>
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
        if (!mirrored)
            throw new IllegalArgumentException("Mirroring not activated on this index");

        try {
            ensureLocalIndex();
            SearchResults searchResults = localIndex.search(query);
            if (searchResults.statusCode == 200) {
                String jsonString = new String(searchResults.data, "UTF-8");
                JSONObject json = new JSONObject(jsonString);
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
            listener.syncDidFinish(this, error);
        }
    }
}
