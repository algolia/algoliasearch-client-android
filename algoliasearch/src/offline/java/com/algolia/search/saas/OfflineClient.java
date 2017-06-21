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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.algolia.search.offline.core.LocalIndex;
import com.algolia.search.offline.core.Response;
import com.algolia.search.offline.core.Sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An API client that adds offline features on top of the regular online API client.
 *
 * <p>NOTE: Requires Algolia's SDK. The {@link #enableOfflineMode(String)} method must be called with a valid license
 * key prior to calling any offline-related method.</p>
 */
public class OfflineClient extends Client
{
    private Context context;
    private File rootDataDir;

    // Threading facilities
    // --------------------
    // Used by the indices to coordinate their execution.
    //
    // NOTE: The build and search queues must be serial to prevent concurrent searches or builds on a given index, but
    // may be distinct because building can be done in parallel with search.
    //
    // NOTE: Although serialization is only strictly needed at the index level, we use global queues as a way to limit
    // resource consumption by the SDK.

    /** Background queue used to build local indices. */
    protected ExecutorService localBuildExecutorService = Executors.newSingleThreadExecutor();

    /** Background queue used to search local indices. */
    protected ExecutorService localSearchExecutorService = Executors.newSingleThreadExecutor();

    /** Background queue used to run transaction bodies (but not the build). */
    protected ExecutorService transactionExecutorService = Executors.newSingleThreadExecutor();

    /**
     * Handler used to run mixed online/offline requests.
     * NOTE: We need a `Handler` instead of an `ExecutorService` because we need to schedule delayed calls.
     */
    protected Handler mixedRequestHandler = new Handler(Looper.getMainLooper());

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey)
    {
        this(context, applicationID, apiKey, null, null);
    }

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     * @param dataDir Path to the directory where the local data will be stored. If null, the default directory will
     *                be used. See {@link #getDefaultDataDir()}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey, File dataDir)
    {
        this(context, applicationID, apiKey, dataDir, null);
    }

    /**
     * Construct a new offline-enabled API client.
     *
     * @param context An Android context.
     * @param applicationID See {@link Client}.
     * @param apiKey See {@link Client}.
     * @param dataDir Path to the directory where the local data will be stored. If null, the default directory will
     *                be used. See {@link #getDefaultDataDir()}.
     * @param hosts See {@link Client}.
     */
    public OfflineClient(@NonNull Context context, @NonNull String applicationID, @NonNull String apiKey, File dataDir, String[] hosts)
    {
        super(applicationID, apiKey, hosts);
        this.context = context;
        if (dataDir != null) {
            this.rootDataDir = dataDir;
        } else {
            this.rootDataDir = getDefaultDataDir();
        }
        this.addUserAgent(new LibraryVersion("algoliasearch-offline-core-android", Sdk.getInstance().getVersionString()));
    }

    /**
     * Create a new index. Although this will always be an instance of {@link MirroredIndex}, mirroring is deactivated
     * by default.
     *
     * @param indexName the name of index
     * @return The newly created index.
     *
     * @deprecated You should now use {@link #getIndex(String)}, which re-uses instances with the same name.
     */
    @Override
    public MirroredIndex initIndex(@NonNull String indexName)
    {
        return new MirroredIndex(this, indexName);
    }

    /**
     * Obtain a mirrored index. Although this will always be an instance of {@link MirroredIndex}, mirroring is
     * deactivated by default.
     *
     * @param indexName The name of the index.
     * @return A proxy to the specified index.
     *
     * **Warning:** The name should not overlap with any `OfflineIndex`. See {@link #getOfflineIndex(String)}.
     */
    @Override
    public @NonNull MirroredIndex getIndex(@NonNull String indexName) {
        MirroredIndex index = null;
        WeakReference<Object> existingIndex = indices.get(indexName);
        if (existingIndex != null) {
            index = (MirroredIndex)existingIndex.get();
        }
        if (index == null) {
            index = new MirroredIndex(this, indexName);
            indices.put(indexName, new WeakReference<Object>(index));
        }
        return index;
    }

    /**
     * Obtain a purely offline index.
     *
     * @param indexName The name of the index.
     * @return A proxy to the specified index.
     *
     * **Warning:** The name should not overlap with any `MirroredIndex`. See {@link #getIndex(String)}.
     */
    public OfflineIndex getOfflineIndex(@NonNull String indexName) {
        OfflineIndex index = null;
        WeakReference<Object> existingIndex = indices.get(indexName);
        if (existingIndex != null) {
            index = (OfflineIndex)existingIndex.get();
        }
        if (index == null) {
            index = new OfflineIndex(this, indexName);
            indices.put(indexName, new WeakReference<Object>(index));
        }
        return index;
    }

    /**
     * Get the path to directory where the local data is stored.
     */
    public @NonNull File getRootDataDir()
    {
        return rootDataDir;
    }

    /**
     * Get the path to the temporary directory used by this client.
     *
     * @return The path to the temporary directory.
     */
    protected @NonNull File getTempDir() {
        return context.getCacheDir();
    }

    /**
     * Enable the offline mode.
     * @param licenseData License for Algolia's SDK.
     */
    public void enableOfflineMode(@NonNull String licenseData) {
        // Init the SDK.
        Sdk.getInstance().init(context, licenseData);
        // TODO: Report any error.
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    /**
     * Get the default data directory.
     * This is an "algolia" subdirectory inside the application's files directory.
     *
     * @return The default data directory.
     */
    public File getDefaultDataDir() {
        return new File(context.getFilesDir(), "algolia");
    }

    public @NonNull Context getContext() {
        return context;
    }

    /**
     * Get the data directory for the current application ID.
     *
     * @return The data directory for the current application ID.
     */
    private @NonNull File getAppDir() {
        return new File(getRootDataDir(), getApplicationID());
    }

    /**
     * Get the data directory for the index with a specified name.
     *
     * @param name The index's name.
     * @return The data directory for the index.
     */
    protected @NonNull File getIndexDir(@NonNull String name) {
        return new File(getAppDir(), name);
    }

    // ----------------------------------------------------------------------
    // Operations
    // ----------------------------------------------------------------------

    /**
     * Test if an index has offline data on disk.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * **Warning:** This method is synchronous!
     *
     * @param name The index's name.
     * @return `true` if data exists on disk for this index, `false` otherwise.
     */
    public boolean hasOfflineData(@NonNull String name) {
        // TODO: Suboptimal; we should be able to test existence without instantiating a `LocalIndex`.
        return new LocalIndex(getRootDataDir().getAbsolutePath(), getApplicationID(), name).exists();
    }

    /**
     * List existing offline indices.
     * Only indices that *actually exist* on disk are listed. If an instance was created but never synced or written
     * to, it will not appear in the list.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request listIndexesOfflineAsync(@NonNull CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return listIndexesOfflineSync();
            }
        }.start();
    }

    /**
     * List existing offline indices.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances. Only indices that
     * *actually exist* on disk are listed. If an instance was created but never synced or written to, it will not
     * appear in the list.
     *
     * @return A JSON object with an `items` attribute containing the indices details as JSON objects.
     */
    private JSONObject listIndexesOfflineSync() throws AlgoliaException {
        try {
            final String rootDataPath = getRootDataDir().getAbsolutePath();
            final File appDir = getAppDir();
            final File[] directories = appDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            JSONObject response = new JSONObject();
            JSONArray items = new JSONArray();
            if (directories != null) {
                for (File directory : directories) {
                    final String name = directory.getName();
                    if (hasOfflineData(name)) {
                        items.put(new JSONObject()
                            .put("name", name)
                        );
                        // TODO: Do we need other data as in the online API?
                    }
                }
            }
            response.put("items", items);
            return response;
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Delete an offline index.
     * This deletes the data on disk. If the index does not exist, this method does nothing.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * @param indexName Name of index to delete.
     * @return A JSON object.
     */
    public Request deleteIndexOfflineAsync(final @NonNull String indexName, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler, localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return deleteIndexOfflineSync(indexName);
            }
        }.start();
    }

    /**
     * Delete an offline index.
     * This deletes the data on disk. If the index does not exist, this method does nothing.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * @param indexName Name of index to delete.
     * @return A JSON response.
     */
    private JSONObject deleteIndexOfflineSync(final @NonNull String indexName) throws AlgoliaException {
        try {
            FileUtils.deleteRecursive(getIndexDir(indexName));
            return new JSONObject()
                .put("deletedAt", DateUtils.iso8601String(new Date()));
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Move an existing offline index.
     *
     * **Warning:** This will overwrite the destination index if it exists.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * @param srcIndexName Name of index to move.
     * @param dstIndexName The new index name.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request moveIndexOfflineAsync(final @NonNull String srcIndexName, final @NonNull String dstIndexName, CompletionHandler completionHandler) {
        return new AsyncTaskRequest(completionHandler, localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return moveIndexOfflineSync(srcIndexName, dstIndexName);
            }
        }.start();
    }

    /**
     * Move an existing offline index.
     *
     * **Warning:** This will overwrite the destination index if it exists.
     *
     * **Note:** This applies both to {@link MirroredIndex} and {@link OfflineIndex} instances.
     *
     * @param srcIndexName Name of index to move.
     * @param dstIndexName The new index name.
     * @return A JSON response.
     */
    private JSONObject moveIndexOfflineSync(final @NonNull String srcIndexName, final @NonNull String dstIndexName) throws AlgoliaException {
        try {
            final File srcDir = getIndexDir(srcIndexName);
            final File dstDir = getIndexDir(dstIndexName);
            if (dstDir.exists()) {
                FileUtils.deleteRecursive(dstDir);
            }
            if (srcDir.renameTo(dstDir)) {
                return new JSONObject()
                    .put("updatedAt", DateUtils.iso8601String(new Date()));
            } else {
                throw new AlgoliaException("Could not move index");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    // NOTE: Copy not supported because it would be too resource-intensive.

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    static protected JSONObject parseSearchResults(Response searchResults) throws AlgoliaException {
        try {
            if (searchResults.getStatusCode() == 200) {
                if (searchResults.getData() != null) {
                    String jsonString = new String(searchResults.getData(), "UTF-8");
                    return new JSONObject(jsonString);
                } else { // may happen when building: no output
                    return new JSONObject();
                }
            }
            else {
                throw new AlgoliaException(searchResults.getErrorMessage(), searchResults.getStatusCode());
            }
        }
        catch (JSONException | UnsupportedEncodingException e) {
            throw new AlgoliaException("Offline Core returned invalid JSON", e);
        }
    }
}
