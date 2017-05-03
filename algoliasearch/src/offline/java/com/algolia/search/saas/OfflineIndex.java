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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.algolia.search.offline.core.LocalIndex;
import com.algolia.search.offline.core.Response;
import com.algolia.search.saas.helpers.DisjunctiveFaceting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;



/**
 * A purely offline index.
 * Such an index has no online counterpart. It is updated and queried locally.
 *
 * **Note:** You cannot construct this class directly. Please use {@link OfflineClient#getOfflineIndex(String)} to
 * obtain an instance.
 *
 * **Note:** Requires Algolia Offline Core. {@link OfflineClient#enableOfflineMode(String)} must be called with a
 * valid license key prior to calling any offline-related method.
 *
 *
 * ## Reading
 *
 * Read operations behave identically as with online indices.
 *
 *
 * ## Writing
 *
 * Updating an index involves rebuilding it, which is an expensive and potentially lengthy operation. Therefore, all
 * updates must be wrapped inside a **transaction**.
 *
 * The procedure to update an index is as follows:
 *
 * - Create a transaction by calling {@link #newTransaction()}.
 *
 * - Populate the transaction: call the various write methods on the {@link WriteTransaction WriteTransaction} class.
 *
 * - Either commit or rollback the transaction.
 *
 * ### Synchronous vs asynchronous updates
 *
 * Any write operation, especially (but not limited to) the final commit, is potentially lengthy. This is why all
 * operations provide an asynchronous version, which accepts an optional completion handler that will be notified of
 * the operation's completion (either successful or erroneous).
 *
 * If you already have a background thread/queue performing data-handling tasks, you may find it more convenient to
 * use the synchronous versions of the write methods. They are named after the asynchronous versions, suffixed by
 * `Sync`. The flow is identical to the asynchronous version (see above).
 *
 * **Warning:** You must not call synchronous methods from the main thread. The methods will throw an
 * `IllegalStateException` if you do so.
 *
 * **Note:** The synchronous methods can throw; you have to catch and handle the exception.
 *
 * ### Parallel transactions
 *
 * While it is possible to create parallel transactions, there is little interest in doing so, since each committed
 * transaction results in an index rebuild. Multiplying transactions therefore only degrades performance.
 *
 * Also, transactions are serially executed in the order they were committed, the latest transaction potentially
 * overwriting the previous transactions' result.
 *
 * ### Manual build
 *
 * As an alternative to using write transactions, `OfflineIndex` also offers a **manual build** feature. Provided that
 * you have:
 *
 * - the **index settings** (one JSON file); and
 * - the **objects** (as many JSON files as needed, each containing an array of objects)
 *
 * ... available as local files on disk, you can replace the index's content with that data by calling
 * {@link #buildFromFiles buildFromFiles} or {@link #buildFromRawResources buildFromRawResources}.
 *
 *
 * ## Caveats
 *
 * ### Limitations
 *
 * Though offline indices support most features of an online index, there are some limitations:
 *
 * - Objects **must contain an `objectID`** field. The SDK will refuse to index objects without an ID.
 *
 * - **Partial updates** are not supported.
 *
 * - **Replica indices** are not supported.
 *
 * ### Differences
 *
 * - **Settings** are not incremental: the new settings completely replace the previous ones. If a setting
 *   is omitted in the new version, it reverts back to its default value. (This is in contrast with the online API,
 *   where you can only specify the settings you want to change and omit the others.)
 *
 * - You cannot batch arbitrary write operations in a single method call (as you would do with
 *   {@link Client#batchAsync Client.batchAsync}). However, all write operations are *de facto* batches, since they
 *   must be wrapped inside a transaction (see below).
 */
public class OfflineIndex {
    /** The client to which this index belongs. */
    private final OfflineClient client;

    /** This index's name. */
    private final String name;

    /** Local index backing this offline index. */
    private final LocalIndex localIndex;

    /** Serial number for transactions. */
    private int transactionSeqNo = 0;

    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    /**
     * Create a new offline index.
     *
     * @param client Offline client to be used by the index.
     * @param name Index name.
     */
    protected OfflineIndex(@NonNull OfflineClient client, @NonNull String name) {
        this.client = client;
        this.name = name;
        this.localIndex = new LocalIndex(getClient().getRootDataDir().getAbsolutePath(), getClient().getApplicationID(), name);
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    @Override
    public String toString()
    {
        return String.format("%s{\"%s\"}", this.getClass().getSimpleName(), getName());
    }

    /**
     * Get this index's name.
     *
     * @return This index's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the client to which this index belongs.
     *
     * @return The client to which this index belongs.
     */
    public OfflineClient getClient()
    {
        return client;
    }

    // ----------------------------------------------------------------------
    // Read operations
    // ----------------------------------------------------------------------

    /**
     * Search inside this index (asynchronously).
     *
     * @param query Search parameters. May be null to use an empty query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return searchSync(queryCopy);
            }
        }.start();
    }

    /**
     * Search inside this index (synchronously).
     *
     * @return Search results.
     */
    private JSONObject searchSync(@NonNull Query query) throws AlgoliaException {
        return OfflineClient.parseSearchResults(localIndex.search(query.build()));
    }

    /**
     * Run multiple queries on this index with one API call.
     * A variant of {@link Client#multipleQueriesAsync(List, Client.MultipleQueriesStrategy, CompletionHandler)}
     * where the targeted index is always the receiver.
     *
     * @param queries The queries to run.
     * @param strategy The strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<Query> queries, @Nullable final Client.MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        final List<Query> queriesCopy = new ArrayList<>(queries);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return multipleQueriesSync(queriesCopy, strategy);
            }
        }.start();
    }

    private JSONObject multipleQueriesSync(final @NonNull List<Query> queries, @Nullable final Client.MultipleQueriesStrategy strategy) throws AlgoliaException {
        return new MultipleQueryEmulator(name) {
            @Override
            protected JSONObject singleQuery(@NonNull Query query) throws AlgoliaException {
                return searchSync(query);
            }
        }.multipleQueries(queries, strategy == null ? null : strategy.toString());
    }

    /**
     * Get an object from this index (asynchronously).
     *
     * @param objectID Identifier of the object to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectAsync(final @NonNull String objectID, @NonNull CompletionHandler completionHandler) {
        return getObjectAsync(objectID, null, completionHandler);
    }

    /**
     * Get an object from this index, optionally restricting the retrieved content (asynchronously).
     *
     * @param objectID Identifier of the object to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectAsync(final @NonNull String objectID, final List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return getObjectSync(objectID, attributesToRetrieve);
            }
        }.start();
    }

    private JSONObject getObjectSync(@NonNull String objectID, List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            JSONObject content = getObjectsSync(Collections.singletonList(objectID), attributesToRetrieve);
            JSONArray results = content.getJSONArray("results");
            return results.getJSONObject(0);
        }
        catch (JSONException e) {
            throw new AlgoliaException("Invalid JSON returned", e);
        }
    }

    /**
     * Get several objects from this index (asynchronously).
     *
     * @param objectIDs Identifiers of objects to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsAsync(final @NonNull List<String> objectIDs, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return getObjectsSync(objectIDs, null);
            }
        }.start();
    }

    private JSONObject getObjectsSync(@NonNull List<String> objectIDs, List<String> attributesToRetrieve) throws AlgoliaException {
        final String[] objectIDsAsArray = objectIDs.toArray(new String[objectIDs.size()]);
        final String queryParameters = attributesToRetrieve == null ? null : new Query().setAttributesToRetrieve(attributesToRetrieve.toArray(new String[attributesToRetrieve.size()])).build();
        return OfflineClient.parseSearchResults(localIndex.getObjects(objectIDsAsArray, queryParameters));
    }

    /**
     * Get this index's settings (asynchronously).
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getSettingsAsync(@NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return getSettingsSync();
            }
        }.start();
    }

    private JSONObject getSettingsSync() throws AlgoliaException {
        return OfflineClient.parseSearchResults(localIndex.getSettings());
    }

    /**
     * Browse all index content (initial call).
     * This method should be called once to initiate a browse. It will return the first page of results and a cursor,
     * unless the end of the index has been reached. To retrieve subsequent pages, call `browseFromAsync` with that
     * cursor.
     *
     * @param query The query parameters for the browse.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return browseSync(queryCopy);
            }
        }.start();
    }

    private JSONObject browseSync(@NonNull Query query) throws AlgoliaException {
        return OfflineClient.parseSearchResults(localIndex.browse(query.build()));
    }

    /**
     * Browse the index from a cursor.
     * This method should be called after an initial call to `browseAsync()`. It returns a cursor, unless the end of
     * the index has been reached.
     *
     * @param cursor The cursor of the next page to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseFromAsync(final @NonNull String cursor, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return browseFromSync(cursor);
            }
        }.start();
    }

    private JSONObject browseFromSync(@NonNull String cursor) throws AlgoliaException {
        final Query query = new Query().set("cursor", cursor);
        return OfflineClient.parseSearchResults(localIndex.browse(query.build()));
    }

    /**
     * Search for facet values (asynchronously).
     * Same parameters as {@link Index#searchForFacetValues(String, String, Query, CompletionHandler)}.
     */
    public Request searchForFacetValuesAsync(final @NonNull String facetName, final @NonNull String facetQuery, @Nullable Query query, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = query != null ? new Query(query) : null;
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return searchForFacetValuesSync(facetName, facetQuery, queryCopy);
            }
        }.start();
    }

    /**
     * Search for facet values (synchronously).
     */
    private JSONObject searchForFacetValuesSync(@NonNull String facetName, @NonNull String facetQuery, @Nullable Query query) throws AlgoliaException {
        return OfflineClient.parseSearchResults(localIndex.searchForFacetValues(facetName, facetQuery, query != null ? query.build() : null));
    }

    // ----------------------------------------------------------------------
    // Transaction management
    // ----------------------------------------------------------------------

    /**
     * A transaction to update the index.
     *
     * A transaction gathers all the operations that will be performed when the transaction is committed.
     * Its purpose is twofold:
     *
     * 1. Avoid rebuilding the index for every individual operation, which would be astonishingly costly.
     * 2. Avoid keeping all the necessary data in memory, e.g. by flushing added objects to temporary files on disk.
     *
     * A transaction can be created by calling {@link #newTransaction()}.
     */
    public class WriteTransaction {

        /**
         * This transaction's ID.
         * Unique within the context of `index`.
         * *Not* guaranteed to be unique across all indices.
         */
        private int id;

        /** Whether this transaction has completed (committed or rolled back). */
        private boolean finished = false;

        /**
         * Path to the temporary file containing the new settings.
         * If `null`, settings will be unchanged by this transaction.
         * */
        private @Nullable File settingsFile;

        /** Paths to the temporary files containing the objects that will be added/updated by this transaction. */
        private List<String> objectFilePaths = new ArrayList<>();

        /**
         * Identifiers of objects that will be deleted by this transaction.
         *
         * **Warning:** Deleted objects have higher precedence than added/updated objects.
         */
        private Set<String> deletedObjectIDs = new HashSet<>();

        /** Whether the index should be cleared before executing this transaction. */
        private boolean shouldClearIndex = false;

        /** Temporary directory for this transaction. */
        private final File tmpDir;

        /** Temporary in-memory cache for objects. */
        private List<JSONObject> tmpObjects = new ArrayList<>();

        // Constants
        // ---------

        /** Maximum number of objects to keep in memory before flushing to disk. */
        private int maxObjectsInMemory = 100;

        // Initialization
        // --------------

        public WriteTransaction() {
            this.id = nextTransactionSeqNo();
            this.tmpDir = new File(getClient().getTempDir(), UUID.randomUUID().toString());

            // Create temporary directory.
            tmpDir.mkdirs();
        }

        @Override
        public void finalize() {
            if (!finished) {
                Log.w("AlgoliaSearch", String.format("Transaction %s was never committed nor rolled back", this));
                doRollback();
            }
        }

        // Accessors
        // ---------

        @Override
        public @NonNull String toString() {
            return String.format("%s{index: %s, id: %d}", this.getClass().getSimpleName(), OfflineIndex.this, id);
        }

        /**
         * Get this transaction's ID.
         * @return This transaction's ID.
         */
        public int getId() {
            return id;
        }

        /**
         * Test whether this transaction is finished.
         * @return Whether this transaction is finished.
         */
        public boolean isFinished() {
            return finished;
        }

        // Populating
        // ----------

        /**
         * Save an object.
         *
         *  @param object Object to save. Must contain an `objectID` attribute.
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request saveObjectAsync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    try {
                        String objectID = object.getString("objectID");
                        saveObjectSync(object);
                        return new JSONObject()
                                .put("objectID", objectID)
                                .put("taskID", id);
                    } catch (JSONException e) {
                        throw new AlgoliaException("Object missing `objectID` attribute", e);
                    }
                }
            }.start();
        }

        /**
         * Save an object (synchronously).
         *
         * @param object Object to save. Must contain an `objectID` attribute.
         */
        public void saveObjectSync(@NonNull JSONObject object) throws AlgoliaException {
            assertNotMainThread();
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                tmpObjects.add(object);
                flushObjectsToDisk(false);
            }
        }

        /**
         * Save multiple objects.
         *
         *  @param objects Objects to save. Each one must contain an `objectID` attribute.
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request saveObjectsAsync(final @NonNull JSONArray objects, CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    try {
                        List<String> objectIDs = new ArrayList<>();
                        for (int i = 0; i < objects.length(); ++i) {
                            objectIDs.add(objects.getJSONObject(i).getString("objectID"));
                        }
                        saveObjectsSync(objects);
                        return new JSONObject()
                                .put("objectIDs", new JSONArray(objectIDs))
                                .put("taskID", id);
                    } catch (JSONException e) {
                        throw new AlgoliaException("Object missing `objectID` attribute", e);
                    }
                }
            }.start();
        }

        /**
         * Save multiple objects (synchronously).
         *
         * @param objects Objects to save. Each one must contain an `objectID` attribute.
         */
        public void saveObjectsSync(@NonNull JSONArray objects) throws AlgoliaException {
            assertNotMainThread();
            try {
                synchronized(this) {
                    if (finished) throw new IllegalStateException();
                    for (int i = 0; i < objects.length(); ++i) {
                        JSONObject object = objects.getJSONObject(i);
                        tmpObjects.add(object);
                    }
                    flushObjectsToDisk(false);
                }
            } catch (JSONException e) {
                throw new AlgoliaException("Array must contain only objects", e);
            }
        }

        /**
         * Delete an object.
         *
         *  @param objectID Identifier of object to delete.
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request deleteObjectAsync(final @NonNull String objectID, CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    deleteObjectSync(objectID);
                    return new JSONObject();
                }
            }.start();
        }

        /**
         * Delete an object (synchronously).
         *
         * @param objectID Identifier of the object to delete.
         */
        public void deleteObjectSync(@NonNull String objectID) throws AlgoliaException {
            assertNotMainThread();
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                deletedObjectIDs.add(objectID);
            }
        }

        /**
         * Delete multiple objects.
         *
         *  @param objectIDs Identifiers of objects to delete.
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request deleteObjectsAsync(final Collection<String> objectIDs, CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    try {
                        deleteObjectsSync(objectIDs);
                        return new JSONObject()
                                .put("objectIDs", new JSONArray(objectIDs))
                                .put("taskID", id);
                    } catch (JSONException e) {
                        throw new RuntimeException(e); // should never happen
                    }
                }
            }.start();
        }

        /**
         * Delete multiple objects (synchronously).
         *
         * @param objectIDs Identifiers of the objects to delete.
         */
        public void deleteObjectsSync(@NonNull Collection<String> objectIDs) throws AlgoliaException {
            assertNotMainThread();
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                deletedObjectIDs.addAll(objectIDs);
            }
        }

        /**
         * Set the index settings.
         *
         * Please refer to our [API documentation](https://www.algolia.com/doc/swift#index-settings) for the list of
         * supported settings.
         *
         *  @param settings New settings.
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request setSettingsAsync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    try {
                        setSettingsSync(settings);
                        return new JSONObject()
                                .put("taskID", id);
                    } catch (JSONException e) {
                        throw new RuntimeException(e); // should never happen
                    }
                }
            }.start();
        }

        /**
         * Set the index settings (synchronously).
         *
         * Please refer to our [API documentation](https://www.algolia.com/doc/swift#index-settings) for the list of
         * supported settings.
         *
         * @param settings New settings.
         */
        public void setSettingsSync(@NonNull  JSONObject settings) throws AlgoliaException {
            assertNotMainThread();
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                settingsFile = writeTmpJSONFile(settings);
            }
        }

        /**
         * Delete the index content without removing settings.
         *
         *  @param completionHandler Completion handler to be notified of the request's outcome.
         *  @return A cancellable operation.
         */
        public Request clearIndexAsync(CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    try {
                        clearIndexSync();
                        return new JSONObject()
                                .put("taskID", id);
                    } catch (JSONException e) {
                        throw new RuntimeException(e); // should never happen
                    }
                }
            }.start();
        }

        /**
         * Delete the index content without removing settings.
         */
        public void clearIndexSync() throws AlgoliaException {
            assertNotMainThread();
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                shouldClearIndex = true;
                deletedObjectIDs.clear();
                objectFilePaths.clear();
            }
        }

        // Completion
        // ----------

        /**
         * Commit the transaction.
         *
         * **Warning:** Cancelling the returned operation does **not** roll back the transaction. The operation is returned
         *   for lifetime management purposes only.
         *
         * @param completionHandler Completion handler to be notified of the transaction's outcome.
         * @return A cancellable operation (see warning for important caveat).
         */
        public Request commitAsync(@NonNull CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    doCommit();
                    return new JSONObject();
                }
            }.start();
        }

        /**
         * Commit the transaction (synchronously).
         */
        public void commitSync() throws AlgoliaException {
            assertNotMainThread();
            doCommit();
        }

        private void doCommit() throws AlgoliaException {
            // Serialize calls with respect to this transaction.
            synchronized (this) {
                if (finished) throw new IllegalStateException();
                try {
                    flushObjectsToDisk(true);
                    Response result = OfflineIndex.this.localIndex.build(
                        settingsFile != null ? settingsFile.getAbsolutePath() : null,
                        objectFilePaths.toArray(new String[objectFilePaths.size()]),
                        shouldClearIndex,
                        deletedObjectIDs.toArray(new String[deletedObjectIDs.size()])
                    );
                    OfflineClient.parseSearchResults(result);
                } finally {
                    finished = true;
                }
            }
        }

        /**
         * Roll back the current write transaction.
         *
         *  **Warning:** Cancelling the returned operation does **not** cancel the rollback. The operation is returned
         *   for lifetime management purposes only.
         *
         *  @param completionHandler Completion handler to be notified of the transaction's outcome.
         *  @return A cancellable operation (see warning for important caveat).
         */
        public Request rollbackAsync(CompletionHandler completionHandler) {
            return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    rollbackSync();
                    return new JSONObject();
                }
            }.start();
        }

        /**
         * Rollback the transaction.
         * The index will be left untouched.
         */
        public void rollbackSync() {
            assertNotMainThread();
            doRollback();
        }

        private void doRollback() {
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                FileUtils.deleteRecursive(tmpDir);
                finished = true;
            }
        }

        // Utils
        // -----

        private void flushObjectsToDisk(boolean force) throws AlgoliaException {
            if (force || tmpObjects.size() >= maxObjectsInMemory) {
                File tmpFile = writeTmpJSONFile(tmpObjects);
                objectFilePaths.add(tmpFile.getAbsolutePath());
                tmpObjects.clear();
            }
        }
    }

    /**
     * Create a new write transaction.
     */
    public @NonNull WriteTransaction newTransaction() {
        return new WriteTransaction();
    }

    /**
     * Test if this index has offline data on disk.
     *
     * **Warning:** This method is synchronous! It will block until completion.
     *
     * @return `true` if data exists on disk for this index, `false` otherwise.
     */
    public boolean hasOfflineData() {
        return localIndex.exists();
    }

    // ----------------------------------------------------------------------
    // Manual build
    // ----------------------------------------------------------------------

    /**
     * Build the index from local data stored on the filesystem.
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
    public Request buildFromFiles(@NonNull final File settingsFile, @NonNull final File[] objectFiles, @Nullable CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _build(settingsFile, objectFiles);
            }
        }.start();
    }

    public Request buildFromFiles(@NonNull final File settingsFile, @NonNull final File... objectFiles) {
        return buildFromFiles(settingsFile, objectFiles, null);
    }

    /**
     * Build the index from local data stored in raw resources.
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
    public Request buildFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int[] objectsResIds, @Nullable CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                return _buildFromRawResources(resources, settingsResId, objectsResIds);
            }
        }.start();
    }

    public Request buildFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int... objectsResIds) {
        return buildFromRawResources(resources, settingsResId, objectsResIds, null);
    }

    private JSONObject _buildFromRawResources(@NonNull final Resources resources, @NonNull final int settingsResId, @NonNull final int... objectsResIds) throws AlgoliaException {
        // Save resources to independent files on disk.
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
            return _build(settingsFile, objectFiles);
        } catch (IOException e) {
            throw new AlgoliaException("Failed to write build resources to disk", e);
        } finally {
            // Delete temporary files.
            FileUtils.deleteRecursive(tmpDir);
        }
    }

    private JSONObject _build(@NonNull File settingsFile, @NonNull File... objectFiles) throws AlgoliaException {
        AlgoliaException error = null;
        String[] objectFilePaths = new String[objectFiles.length];
        for (int i = 0; i < objectFiles.length; ++i) {
            objectFilePaths[i] = objectFiles[i].getAbsolutePath();
        }
        final Response result = localIndex.build(settingsFile.getAbsolutePath(), objectFilePaths, true /* clearIndex */, null /* deletedObjectIDs */);
        return OfflineClient.parseSearchResults(result);
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    /**
     * Perform a search with disjunctive facets, generating as many queries as number of disjunctive facets (helper).
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchDisjunctiveFacetingAsync(@NonNull Query query, @NonNull final List<String> disjunctiveFacets, @NonNull final Map<String, List<String>> refinements, @NonNull final CompletionHandler completionHandler) {
        return new DisjunctiveFaceting() {
            @Override
            protected Request multipleQueriesAsync(@NonNull List<Query> queries, @NonNull CompletionHandler completionHandler) {
                return OfflineIndex.this.multipleQueriesAsync(queries, null, completionHandler);
            }
        }.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, completionHandler);
    }

    /**
     * Delete all objects matching a query (helper).
     *
     * @param query The query that objects to delete must match.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteByQueryAsync(@NonNull Query query, CompletionHandler completionHandler) {
        final WriteTransaction transaction = newTransaction();
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            protected JSONObject run() throws AlgoliaException {
                try {
                    Collection<String> deletedObjectIDs = deleteByQuerySync(queryCopy, transaction);
                    transaction.commitSync();
                    return new JSONObject()
                        .put("objectIDs", new JSONArray(deletedObjectIDs))
                        .put("updatedAt", DateUtils.iso8601String(new Date()))
                        .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    private Collection<String> deleteByQuerySync(@NonNull Query query, @NonNull WriteTransaction transaction) throws AlgoliaException {
        try {
            final Query browseQuery = new Query(query);
            browseQuery.setAttributesToRetrieve("objectID");
            final String queryParameters = browseQuery.build();

            // Gather object IDs to delete.
            List<String> objectIDsToDelete = new ArrayList<>();
            boolean hasMore = true;
            while (hasMore) {
                JSONObject content = OfflineClient.parseSearchResults(localIndex.browse(queryParameters));
                JSONArray hits = content.getJSONArray("hits");

                // Retrieve object IDs.
                for (int i = 0; i < hits.length(); ++i) {
                    JSONObject hit = hits.getJSONObject(i);
                    String objectID = hit.getString("objectID");
                    objectIDsToDelete.add(objectID);
                }
                hasMore = content.optString("cursor", null) != null;
            }

            // Delete objects.
            transaction.deleteObjectsSync(objectIDsToDelete);
            return objectIDsToDelete;
        } catch (JSONException e) {
            throw new AlgoliaException("Invalid JSON results", e);
        }
    }

    // ----------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------

    /**
     * Create a copy of a JSON object with a specific <code>objectID</code> attribute.
     *
     * @param object The object to copy.
     * @param objectID The ID for the new object.
     * @return A new JSON object with the specified object ID.
     */
    private static JSONObject objectWithID(@NonNull JSONObject object, @NonNull String objectID) {
        try {
            // WARNING: Could not find a better way to clone the object.
            JSONObject patchedObject = new JSONObject(object.toString());
            patchedObject.put("objectID", objectID);
            return patchedObject;
        }
        catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Write a temporary file containing a JSON object.
     *
     * @param object JSON object to write.
     * @return Path to the created file.
     * @throws AlgoliaException
     */
    private @NonNull File writeTmpJSONFile(@NonNull JSONObject object) throws AlgoliaException {
        return writeTempFile(object.toString());
    }

    /**
     * Write a temporary file containing JSON objects. The files are written as an array.
     *
     * @param objects JSON objects to write.
     * @return Path to the created file.
     * @throws AlgoliaException
     */
    private File writeTmpJSONFile(@NonNull Collection<JSONObject> objects) throws AlgoliaException {
        // TODO: Maybe split in several files if too big?
        // TODO: Stream writing.
        JSONArray array = new JSONArray();
        for (JSONObject object : objects) {
            array.put(object);
        }
        return writeTempFile(array.toString());
    }

    /**
     * Write a temporary file containing textual data in UTF-8 encoding.
     *
     * @param data Data to write.
     * @return Path to the created file.
     * @throws AlgoliaException
     */
    private File writeTempFile(@NonNull String data) throws AlgoliaException {
        try {
            // Create temporary file.
            File tmpDir = client.getTempDir();
            File tmpFile = File.createTempFile("algolia.", ".json", tmpDir);

            // Write to file.
            Writer writer = new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8");
            writer.write(data);
            writer.close();

            return tmpFile;
        } catch (IOException e) {
            throw new AlgoliaException("Could not create temporary file", e);
        }
    }

    private synchronized int nextTransactionSeqNo() {
        transactionSeqNo += 1;
        return transactionSeqNo;
    }

    private void assertNotMainThread() {
        // NOTE: Throwing an exception would be rather extreme, and also causes problems with unit tests, where all
        // threads are unwound onto the main thread. => A log message should be deterrent enough.
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.println(Log.ASSERT, "AlgoliaSearch", "Synchronous methods should not be called from the main thread");
        }
    }
}
