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

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.algolia.search.offline.core.LocalIndex;
import com.algolia.search.offline.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * An online index that can also be mirrored locally.
 *
 * **Note:** You cannot construct this class directly. Please use {@link OfflineClient#getIndex(String)} to obtain an
 * instance.
 *
 * **Note:** Requires Algolia Offline Core. {@link OfflineClient#enableOfflineMode(String)} must be called with a
 * valid license key prior to calling any offline-related method.
 *
 * When created, an instance of this class has its `mirrored` flag set to false, and behaves like a normal,
 * online {@link Index}. When the `mirrored` flag is set to true, the index becomes capable of acting upon local data.
 *
 * **Warning:** It is a programming error to call methods acting on the local data when `mirrored` is false. Doing so
 * will result in an assertion exception being thrown.
 *
 *
 * ## Request strategy
 *
 * When the index is mirrored and the device is online, it becomes possible to transparently switch between online and
 * offline requests. There is no single best strategy for that, because it depends on the use case and the current
 * network conditions. You can choose the strategy via {@link #setRequestStrategy(Strategy) setRequestStrategy}. The
 * default is {@link Strategy#FALLBACK_ON_FAILURE FALLBACK_ON_FAILURE}, which will always target the online API first,
 * then fallback to the offline mirror in case of failure (including network unavailability).
 *
 * **Note:** If you want to explicitly target either the online API or the offline mirror, doing so is always possible
 * using the {@link #searchOnlineAsync searchOnlineAsync} or {@link #searchOfflineAsync searchOfflineAsync} methods.
 *
 * **Note:** The strategy applies to:
 *
 * - `searchAsync`
 * - `searchDisjunctiveFacetingAsync`
 * - `multipleQueriesAsync`
 * - `getObjectAsync`
 * - `getObjectsAsync`
 *
 *
 * ## Bootstrapping
 *
 * Before the first sync has successfully completed, a mirrored index is not available offline, because it has simply
 * no data to search in yet. In most cases, this is not a problem: the app will sync as soon as instructed, so unless
 * the device is offline when the app is started for the first time, or unless search is required right after the
 * first launch, the user should not notice anything.
 *
 * However, in some cases, you might need to have offline data available as soon as possible. To achieve that,
 * `MirroredIndex` provides a **manual build** feature.
 *
 * ### Manual build
 *
 * Manual building consists in specifying the source data for your index from local files, instead of downloading it
 * from the API. Namely, you need:
 *
 * - the **index settings** (one JSON file); and
 * - the **objects** (as many JSON files as needed, each containing an array of objects).
 *
 * Those files are typically embedded in the application as resources, although any other origin works too.
 *
 * ### Conditional bootstrapping
 *
 * To avoid replacing the local mirror every time the app is started (and potentially overwriting more recent data
 * synced from the API), you should test whether the index already has offline data using {@link #hasOfflineData()}.
 *
 * #### Discussion
 *
 * **Warning:** We strongly advise against prepackaging index files. While it may work in some cases, Algolia Offline
 * makes no guarantee whatsoever that the index file format will remain backward-compatible forever, nor that it
 * is independent of the hardware architecture (e.g. 32 bits vs 64 bits, or Little Endian vs Big Endian). Instead,
 * always use the manual build feature.
 *
 * While a manual build involves computing the offline index on the device, and therefore incurs a small delay before
 * the mirror is actually usable, using plain JSON offers several advantages compared to prepackaging the index file
 * itself:
 *
 * - You only need to ship the raw object data, which is smaller than shipping an entire index file, which contains
 *   both the raw data *and* indexing metadata.
 *
 * - Plain JSON compresses well with standard compression techniques like GZip, whereas an index file uses a binary
 *   format which doesn't compress very efficiently.
 *
 * - Build automation is facilitated: you can easily extract the required data from your back-end, whereas building
 *   an index would involve running the app on each mobile platform as part of your build process and capturing the
 *   filesystem.
 *
 * Also, the build process is purposedly single-threaded across all indices, which means that on most modern devices
 * with multi-core CPUs, the impact of manual building on the app's performance will be very moderate, especially
 * regarding UI responsiveness.
 *
 *
 * ## Listeners
 *
 * You may register a {@link SyncListener} to listen for sync-related events. The listener methods will be called using
 * the client's completion executor (which is, by default, the main thread).
 *
 *
 * ## Limitations
 *
 * Algolia's core features are fully supported offline, including (but not limited to): **ranking**,
 * **typo tolerance**, **filtering**, **faceting**, **highlighting/snippeting**...
 *
 * However, and partly due to tight memory, CPU and disk space constraints, some features are disabled:
 *
 * - **Synonyms** are only partially supported:
 *
 *     - Multi-way ("regular") synonyms are fully supported.
 *     - One-way synonyms are not supported.
 *     - Alternative corrections are limited to one alternative (compared to multiple alternatives with online indices).
 *     - Placeholders are fully supported.
 *
 * - Dictionary-based **plurals** are not supported. ("Simple" plurals with a final S are supported.)
 *
 * - **IP geolocation** (see {@link Query#setAroundLatLngViaIP(Boolean)}) is not supported.
 *
 * - **CJK segmentation** is not supported.
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
    private Set<BuildListener> buildListeners = new HashSet<>();

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

        // Notify listeners.
        getClient().completionExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                fireSyncDidStart();
            }
        });

        try {
            // Create temporary directory.
            tmpDir = new File(getClient().getTempDir(), UUID.randomUUID().toString());
            tmpDir.mkdirs();

            // NOTE: We are doing everything sequentially, because this is a background job: we care more about
            // resource consumption than about how long it will take.

            // Fetch settings.
            {
                JSONObject settingsJSON = this.getSettings(1);
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

                    cursor = objectsJSON.optString("cursor", null);
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
            _buildOffline(settingsFile, objectFiles.toArray(new File[objectFiles.size()]));

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

            // Notify listeners.
            getClient().completionExecutor.execute(new Runnable()
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
    // Manual build
    // ----------------------------------------------------------------------

    /**
     * Test if this index has offline data on disk.
     *
     * **Warning:** This method is synchronous! It will block until completion.
     *
     * @return `true` if data exists on disk for this index, `false` otherwise.
     */
    public boolean hasOfflineData() {
        return getLocalIndex().exists();
    }

    /**
     * Replace the local mirror with local data stored on the filesystem.
     *
     * @param settingsFile Absolute path to the file containing the index settings, in JSON format.
     * @param objectFiles Absolute path(s) to the file(s) containing the objects. Each file must contain an array of
     *                    objects, in JSON format.
     * @param completionHandler Optional completion handler to be notified of the build's outcome.
     * @return A cancellable request.
     *
     * **Note:** Cancelling the request does *not* cancel the build; it merely prevents the completion handler from
     * being called.
     */
    public Request buildOfflineFromFiles(@NonNull final File settingsFile, @NonNull final File[] objectFiles, @Nullable CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _buildOffline(settingsFile, objectFiles);
            }
        }.start();
    }

    public Request buildOfflineFromFiles(@NonNull final File settingsFile, @NonNull final File... objectFiles) {
        return buildOfflineFromFiles(settingsFile, objectFiles, null);
    }

    /**
     * Replace the local mirror with local data stored in raw resources.
     *
     * @param resources A {@link Resources} instance to read resources from.
     * @param settingsResId Resource identifier of the index settings, in JSON format.
     * @param objectsResIds Resource identifiers of the various objects files. Each file must contain an array of
     *                    objects, in JSON format.
     * @param completionHandler Optional completion handler to be notified of the build's outcome.
     * @return A cancellable request.
     *
     * **Note:** Cancelling the request does *not* cancel the build; it merely prevents the completion handler from
     * being called.
     */
    public Request buildOfflineFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int[] objectsResIds, @Nullable CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _buildOfflineFromRawResources(resources, settingsResId, objectsResIds);
            }
        }.start();
    }

    public Request buildOfflineFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int... objectsResIds) {
        return buildOfflineFromRawResources(resources, settingsResId, objectsResIds, null);
    }

    private JSONObject _buildOfflineFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int... objectsResIds) throws AlgoliaException {
        // Save resources to independent files on disk.
        // TODO: See if we can have the Offline Core read directly from resources or assets.
        File tmpDir = new File(getClient().getTempDir(), UUID.randomUUID().toString());
        try {
            tmpDir.mkdirs();
            // Settings.
            File settingsFile = new File(tmpDir, "settings.json");
            FileUtils.writeFile(settingsFile, resources.openRawResource(settingsResId));
            // Objects.
            File[] objectFiles = new File[objectsResIds.length];
            for (int i = 0; i < objectsResIds.length; ++i) {
                objectFiles[i] = new File(tmpDir, "objects#" + Integer.toString(objectsResIds[i]) + ".json");
                FileUtils.writeFile(objectFiles[i], resources.openRawResource(objectsResIds[i]));
            }
            // Build the index.
            return _buildOffline(settingsFile, objectFiles);
        } catch (IOException e) {
            throw new AlgoliaException("Failed to write build resources to disk", e);
        } finally {
            // Delete temporary files.
            FileUtils.deleteRecursive(tmpDir);
        }
    }

    private JSONObject _buildOffline(@NonNull File settingsFile, @NonNull File... objectFiles) throws AlgoliaException {
        AlgoliaException error = null;
        try {
            // Notify listeners.
            getClient().completionExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    fireBuildDidStart();
                }
            });

            // Build the index.
            String[] objectFilePaths = new String[objectFiles.length];
            for (int i = 0; i < objectFiles.length; ++i) {
                objectFilePaths[i] = objectFiles[i].getAbsolutePath();
            }
            final int status = getLocalIndex().build(settingsFile.getAbsolutePath(), objectFilePaths, true /* clearIndex */, null /* deletedObjectIDs */);
            if (status != 200) {
                error = new AlgoliaException(String.format("Failed to build local mirror \"%s\"", MirroredIndex.this.getIndexName()), status);
                throw error;
            }
            return new JSONObject();
        }
        finally {
            // Notify listeners.
            final Throwable finalError = error;
            getClient().completionExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    fireBuildDidFinish(finalError);
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
     *
     * WARNING: The fallback logic requires that all phases are run sequentially. Since we have no guaranteee that
     * the completion handlers run on a serial executor or even on the same executor as the time-triggered fallback,
     * we explicitly synchronize the blocks using a serial dispatch queue specific to this operation.
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
        public synchronized void cancel() {
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
        public synchronized boolean isFinished() {
            return onlineRequest.isFinished() && (offlineRequest == null || offlineRequest.isFinished());
        }

        @Override
        public synchronized boolean isCancelled() {
            return cancelled;
        }

        public synchronized OnlineOfflineRequest start() {
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
                        synchronized (OnlineOfflineRequest.this) {
                            // If the online request has not returned yet, or has returned an error, use the offline mirror.
                            // Let's also make sure that we don't start the offline request twice.
                            if (mayRunOfflineRequest && offlineRequest == null) {
                                startOffline();
                            }
                        }
                    }
                };
                getClient().mixedRequestHandler.postDelayed(startOfflineRunnable, offlineFallbackTimeout);
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
                    synchronized (OnlineOfflineRequest.this) {
                        if (error != null && error.isTransient() && mayRunOfflineRequest) {
                            startOffline();
                        } else {
                            cancelOffline();
                            callCompletion(content, error);
                        }
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
                    synchronized (OnlineOfflineRequest.this) {
                        if (onlineRequest != null) {
                            onlineRequest.cancel();
                        }
                        callCompletion(content, error);
                    }
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
                getClient().mixedRequestHandler .removeCallbacks(startOfflineRunnable);
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
            protected JSONObject run() throws AlgoliaException {
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
            protected JSONObject run() throws AlgoliaException {
                return _searchOffline(queryCopy);
            }
        }.start();
    }

    private JSONObject _searchOffline(@NonNull Query query) throws AlgoliaException
    {
        try {
            Response searchResults = getLocalIndex().search(query.build());
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
            protected JSONObject run() throws AlgoliaException {
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
            protected JSONObject run() throws AlgoliaException {
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
        return new MultipleQueryEmulator(this.getIndexName()) {
            @Override
            protected JSONObject singleQuery(@NonNull Query query) throws AlgoliaException {
                return _searchOffline(query);
            }
        }.multipleQueries(queries, strategy);
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
            protected JSONObject run() throws AlgoliaException {
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
            protected JSONObject run() throws AlgoliaException {
                return _browseMirror(query);
            }
        }.start();
    }

    private JSONObject _browseMirror(@NonNull Query query) throws AlgoliaException
    {
        try {
            Response searchResults = getLocalIndex().browse(query.build());
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
    // Getting individual objects
    // ----------------------------------------------------------------------

    /**
     * Get an individual object from the online API, falling back to the local mirror in case of error (when enabled).
     *
     * @param objectID Identifier of the object to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    @Override
    public Request getObjectAsync(final @NonNull String objectID, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            return super.getObjectAsync(objectID, attributesToRetrieve, completionHandler);
        } else {
            return new OnlineOfflineGetObjectRequest(objectID, attributesToRetrieve, completionHandler).start();
        }
    }

    private class OnlineOfflineGetObjectRequest extends OnlineOfflineRequest {
        private final String objectID;
        private final List<String> attributesToRetrieve;

        public OnlineOfflineGetObjectRequest(@NonNull String objectID, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
            super(completionHandler);
            this.objectID = objectID;
            this.attributesToRetrieve = attributesToRetrieve;
        }

        @Override
        protected Request startOnlineRequest(CompletionHandler completionHandler) {
            return getObjectOnlineAsync(objectID, attributesToRetrieve, completionHandler);
        }

        @Override
        protected Request startOfflineRequest(CompletionHandler completionHandler) {
            return getObjectOfflineAsync(objectID, attributesToRetrieve, completionHandler);
        }
    }

    /**
     * Get an individual object, explicitly targeting the online API, not the offline mirror.
     *
     * @param objectID Identifier of the object to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectOnlineAsync(@NonNull final String objectID, final @Nullable List<String> attributesToRetrieve, @NonNull final CompletionHandler completionHandler) {
        // TODO: Cannot perform origin tagging because it could conflict with the object's attributes
        return super.getObjectAsync(objectID, attributesToRetrieve, completionHandler);
    }

    /**
     * Get an individual object, explicitly targeting the online API, not the offline mirror.
     *
     * @param objectID Identifier of the object to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectOnlineAsync(@NonNull final String objectID, @NonNull final CompletionHandler completionHandler) {
        return getObjectOnlineAsync(objectID, null, completionHandler);
    }

    /**
     * Get an individual object, explicitly targeting the offline mirror, not the online API.
     *
     * @param objectID Identifier of the object to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request getObjectOfflineAsync(@NonNull final String objectID, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _getObjectOffline(objectID, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Get an individual object, explicitly targeting the offline mirror, not the online API.
     *
     * @param objectID Identifier of the object to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request getObjectOfflineAsync(@NonNull final String objectID, @NonNull final CompletionHandler completionHandler) {
        return getObjectOfflineAsync(objectID, null, completionHandler);
    }

    private JSONObject _getObjectOffline(@NonNull final String objectID, final @Nullable List<String> attributesToRetrieve) throws AlgoliaException
    {
        try {
            JSONObject content = _getObjectsOffline(Collections.singletonList(objectID), attributesToRetrieve);
            JSONArray results = content.getJSONArray("results");
            return results.getJSONObject(0);
        }
        catch (JSONException e) {
            throw new AlgoliaException("Invalid response returned", e); // should never happen
        }
    }

    /**
     * Get individual objects from the online API, falling back to the local mirror in case of error (when enabled).
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    @Override
    public Request getObjectsAsync(final @NonNull List<String> objectIDs, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            return super.getObjectsAsync(objectIDs, attributesToRetrieve, completionHandler);
        } else {
            return new OnlineOfflineGetObjectsRequest(objectIDs, attributesToRetrieve, completionHandler).start();
        }
    }

    private class OnlineOfflineGetObjectsRequest extends OnlineOfflineRequest {
        private final List<String> objectIDs;
        private final List<String> attributesToRetrieve;

        public OnlineOfflineGetObjectsRequest(@NonNull List<String> objectIDs, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
            super(completionHandler);
            this.objectIDs = objectIDs;
            this.attributesToRetrieve = attributesToRetrieve;
        }

        @Override
        protected Request startOnlineRequest(CompletionHandler completionHandler) {
            return getObjectsOnlineAsync(objectIDs, attributesToRetrieve, completionHandler);
        }

        @Override
        protected Request startOfflineRequest(CompletionHandler completionHandler) {
            return getObjectsOfflineAsync(objectIDs, attributesToRetrieve, completionHandler);
        }
    }

    /**
     * Get individual objects, explicitly targeting the online API, not the offline mirror.
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsOnlineAsync(@NonNull final List<String> objectIDs, final @Nullable List<String> attributesToRetrieve, @NonNull final CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return getObjectsOnline(objectIDs, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Get individual objects, explicitly targeting the online API, not the offline mirror.
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsOnlineAsync(@NonNull final List<String> objectIDs, @NonNull final CompletionHandler completionHandler) {
        return getObjectsOnlineAsync(objectIDs, null, completionHandler);
    }

    private JSONObject getObjectsOnline(@NonNull final List<String> objectIDs, final @Nullable List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            JSONObject content = super.getObjects(objectIDs, attributesToRetrieve);
            // TODO: Factorize origin tagging
            content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
            return content;
        }
        catch (JSONException e) {
            throw new AlgoliaException("Failed to patch JSON result");
        }
    }

    /**
     * Get individual objects, explicitly targeting the offline mirror, not the online API.
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param attributesToRetrieve Attributes to retrieve. If `null` or if at least one item is `*`, all retrievable
     *                             attributes will be retrieved.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request getObjectsOfflineAsync(@NonNull final List<String> objectIDs, final @Nullable List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Mirroring not activated on this index");
        }
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _getObjectsOffline(objectIDs, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Get individual objects, explicitly targeting the offline mirror, not the online API.
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @throws IllegalStateException if mirroring is not activated on this index.
     */
    public Request getObjectsOfflineAsync(@NonNull final List<String> objectIDs, @NonNull final CompletionHandler completionHandler) {
        return getObjectsOfflineAsync(objectIDs, null, completionHandler);
    }

    private JSONObject _getObjectsOffline(@NonNull final List<String> objectIDs, final @Nullable List<String> attributesToRetrieve) throws AlgoliaException
    {
        try {
            Query query = new Query();
            if (attributesToRetrieve != null) {
                query.setAttributesToRetrieve(attributesToRetrieve.toArray(new String[attributesToRetrieve.size()]));
            }
            Response searchResults = getLocalIndex().getObjects(objectIDs.toArray(new String[objectIDs.size()]), query.build());
            if (searchResults.getStatusCode() == 200) {
                String jsonString = new String(searchResults.getData(), "UTF-8");
                JSONObject json = new JSONObject(jsonString);
                json.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_LOCAL);
                return json;
            }
            else {
                throw new AlgoliaException(searchResults.getErrorMessage(), searchResults.getStatusCode());
            }
        }
        catch (JSONException | UnsupportedEncodingException e) {
            throw new AlgoliaException("Get objects failed", e);
        }
    }

    // ----------------------------------------------------------------------
    // Search for facet values
    // ----------------------------------------------------------------------

    @Override
    public Request searchForFacetValues(@NonNull String facetName, @NonNull String text, @Nullable Query query, @NonNull final CompletionHandler completionHandler) {
        // A non-mirrored index behaves exactly as an online index.
        if (!mirrored) {
            return super.searchForFacetValues(facetName, text, query, completionHandler);
        }
        // A mirrored index launches a mixed offline/online request.
        else {
            final Query queryCopy = query != null ? new Query(query) : null;
            return new MixedFacetSearchRequest(facetName, text, queryCopy, completionHandler).start();
        }
    }

    /**
     * Search for facet values, explicitly targeting the online API, not the offline mirror.
     * Same parameters as {@link Index#searchForFacetValues(String, String, Query, CompletionHandler)}.
     */
    public Request searchForFacetValuesOnline(@NonNull String facetName, @NonNull String text, @Nullable Query query, @NonNull final CompletionHandler completionHandler) {
        return super.searchForFacetValues(facetName, text, query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                try {
                    if (content != null)
                        content.put(JSON_KEY_ORIGIN, JSON_VALUE_ORIGIN_REMOTE);
                }
                catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
                completionHandler.requestCompleted(content, error);
            }
        });
    }

    /**
     * Search for facet values, explicitly targeting the offline mirror, not the online API.
     */
    public Request searchForFacetValuesOffline(final @NonNull String facetName, final @NonNull String text, @Nullable Query query, @NonNull final CompletionHandler completionHandler) {
        if (!mirrored) {
            throw new IllegalStateException("Offline requests are only available when the index is mirrored");
        }
        final Query queryCopy = query != null ? new Query(query) : null;
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localSearchExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _searchForFacetValuesOffline(facetName, text, queryCopy);
            }
        }.start();
    }

    private class MixedFacetSearchRequest extends OnlineOfflineRequest {
        private final @NonNull String facetName;
        private final @NonNull String facetQuery;
        private final Query query;

        public MixedFacetSearchRequest(@NonNull String facetName, @NonNull String facetQuery, @Nullable Query query, @NonNull CompletionHandler completionHandler) {
            super(completionHandler);
            this.facetName = facetName;
            this.facetQuery = facetQuery;
            this.query = query;
        }

        @Override
        protected Request startOnlineRequest(CompletionHandler completionHandler) {
            return searchForFacetValuesOnline(facetName, facetQuery, query, completionHandler);
        }

        @Override
        protected Request startOfflineRequest(CompletionHandler completionHandler) {
            return searchForFacetValuesOffline(facetName, facetQuery, query, completionHandler);
        }
    }

    private JSONObject _searchForFacetValuesOffline(@NonNull String facetName, @NonNull String text, @Nullable Query query) throws AlgoliaException {
        try {
            Response searchResults =  getLocalIndex().searchForFacetValues(facetName, text, query != null ? query.build() : null);
            if (searchResults.getStatusCode() == 200) {
                String jsonString = new String(searchResults.getData(), "UTF-8");
                return new JSONObject(jsonString); // NOTE: Origin tagging performed by the SDK
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

    // SyncListener

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

    // BuildListener

    /**
     * Add a listener for build events.
     * @param listener The listener to add.
     */
    public void addBuildListener(@NonNull BuildListener listener) {
        buildListeners.add(listener);
    }

    /**
     * Remove a listener for build events.
     * @param listener The listener to remove.
     */
    public void removeBuildListener(@NonNull BuildListener listener) {
        buildListeners.remove(listener);
    }

    private void fireBuildDidStart() {
        for (BuildListener listener : buildListeners) {
            listener.buildDidStart(this);
        }
    }

    private void fireBuildDidFinish(@Nullable Throwable error) {
        for (BuildListener listener : buildListeners) {
            listener.buildDidFinish(this, error);
        }
    }
}
