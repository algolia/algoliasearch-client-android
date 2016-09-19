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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.algolia.search.offline.core.LocalIndex;
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
 *
 * # Caveats
 *
 * ## Limitations
 *
 * Though offline indices support most features of an online index, there are some limitations:
 *
 * - Objects **must contain an `objectID`** field. The SDK will refuse to index objects without an ID.
 *   As a consequence, `addObject(s)Async()` and `saveObject(s)Async()` are synonyms.
 *
 * - **Partial updates** are not supported.
 *
 * - **Batch** operations are not supported.
 *
 * - **Slave indices** are not supported.
 *
 * ## Differences
 *
 * - **Settings** are not incremental: the new settings completely replace the previous ones. If a setting
 *   is omitted in the new version, it reverts back to its default value. (This is in contrast with the online API,
 *   where you can only specify the settings you want to change and omit the others.)
 *
 *
 * # Operations
 *
 * ## Asynchronicity
 *
 * **Reminder:** Write operations on an online `Index` are twice asynchronous: the response from the server received
 * by the completion handler is merely an acknowledgement of the task. If you want to detect the end of the write
 * operation, you have to use `waitTask()`.
 *
 * In contrast, write operations on an `OfflineIndex` are only once asynchronous: when the completion handler is
 * called, the operation has completed (either successfully or unsuccessfully).
 *
 * Read operations behave identically as on online indices.
 *
 * ## Cancellation
 *
 * Just like online indices, an offline index bears **no rollback semantic**: cancelling an operation does **not**
 * prevent the data from being modified. It just prevents the completion handler from being called.
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

    /** The current transaction, or `null` if no transaction is currently open. */
    private WriteTransaction transaction;

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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
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
            JSONObject run() throws AlgoliaException {
                return browseFromSync(cursor);
            }
        }.start();
    }

    private JSONObject browseFromSync(@NonNull String cursor) throws AlgoliaException {
        final Query query = new Query().set("cursor", cursor);
        return OfflineClient.parseSearchResults(localIndex.browse(query.build()));
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
     * A transaction can be created by calling `OfflineIndex.beginTransaction()`.
     */
    private class WriteTransaction {

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
                rollback();
            }
        }

        // Populating
        // ----------

        /**
         * Update several objects.
         *
         * @param objects New versions of the objects to update. Each one must contain an `objectID` attribute.
         */
        public void saveObjects(@NonNull Collection<JSONObject> objects) throws AlgoliaException {
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                for (JSONObject object : objects) {
                    tmpObjects.add(object);
                }
                flushObjectsToDisk(false);
            }
        }

        /**
         * Delete several objects.
         *
         * @param objectIDs Identifiers of the objects to delete.
         */
        public void deleteObjects(@NonNull Collection<String> objectIDs) throws AlgoliaException {
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                deletedObjectIDs.addAll(objectIDs);
            }
        }

        /**
         * Set the index's settings.
         *
         * Please refer to our [API documentation](https://www.algolia.com/doc/swift#index-settings) for the list of
         * supported settings.
         *
         * @param settings New settings.
         */
        public void setSettings(@NonNull  JSONObject settings) throws AlgoliaException {
            synchronized(this) {
                if (finished) throw new IllegalStateException();
                settingsFile = writeTmpJSONFile(settings);
            }
        }

        /**
         * Delete the index content without removing settings.
         */
        public void clearIndex() throws AlgoliaException {
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
         * **Warning:** It is the caller's responsibility to serialize commits with respect to the local build queue.
         */
        public void commit() throws AlgoliaException {
            // Serialize calls with respect to this transaction.
            synchronized (this) {
                if (finished) throw new IllegalStateException();
                try {
                    flushObjectsToDisk(true);
                    int statusCode = OfflineIndex.this.localIndex.build(
                        settingsFile != null ? settingsFile.getAbsolutePath() : null,
                        objectFilePaths.toArray(new String[objectFilePaths.size()]),
                        shouldClearIndex,
                        deletedObjectIDs.toArray(new String[deletedObjectIDs.size()])
                    );
                    if (statusCode != 200) {
                        throw new AlgoliaException("Error building index", statusCode);
                    }
                } finally {
                    finished = true;
                }
            }
        }

        /**
         * Rollback the transaction.
         * The index will be left untouched.
         */
        public void rollback() {
            synchronized(this) {
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
     *
     * **Warning:** You cannot open parallel transactions. This method will assert if a transaction is already open.
     */
    public void beginTransaction() {
        if (transaction != null) throw new IllegalStateException("A transaction is already open");
        transaction = new WriteTransaction();
    }

    /**
     * Commit the current write transaction.
     *
     * **Warning:** This method will assert/crash if no transaction is currently open.
     *
     * **Warning:** Cancelling the returned operation does **not** roll back the transaction. The operation is returned
     *   for lifetime management purposes only.
     *
     * @param completionHandler Completion handler to be notified of the transaction's outcome.
     * @return A cancellable operation (see warning for important caveat).
     */
    public Request commitTransactionAsync(@NonNull CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                commitTransactionSync();
                return new JSONObject();
            }
        }.start();
    }

    /**
     * Commit the current write transaction (synchronously).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     */
    public void commitTransactionSync() throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        try {
            transaction.commit();
        } finally {
            transaction = null;
        }
    }

    /**
     * Roll back the current write transaction.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** Cancelling the returned operation does **not** cancel the rollback. The operation is returned
     *   for lifetime management purposes only.
     *
     *  @param completionHandler Completion handler to be notified of the transaction's outcome.
     *  @return A cancellable operation (see warning for important caveat).
     */
    public Request rollbackTransactionAsync(@NonNull CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                rollbackTransactionSync();
                return new JSONObject();
            }
        }.start();
    }

    /**
     * Roll back the current write transaction (synchronously).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     */
    public void rollbackTransactionSync() throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        try {
            transaction.rollback();
        } finally {
            transaction = null;
        }
    }

    // ----------------------------------------------------------------------
    // Write operations
    // ----------------------------------------------------------------------

    /**
     * Delete an object from this index.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param objectID Identifier of object to delete.
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request deleteObjectAsync(final @NonNull String objectID, CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    deleteObjectSync(objectID);
                    return new JSONObject()
                        .put("deletedAt", DateUtils.iso8601String(new Date()));
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Delete an object from this index (synchronous version).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     *
     *  @param objectID Identifier of object to delete.
     */
    public void deleteObjectSync(String objectID) throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        deleteObjectsSync(Collections.singletonList(objectID));
    }

    /**
     * Delete several objects from this index.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param objectIDs Identifiers of objects to delete.
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request deleteObjectsAsync(final Collection<String> objectIDs, CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    deleteObjectsSync(objectIDs);
                    return new JSONObject()
                        .put("objectIDs", new JSONArray(objectIDs))
                        .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Delete several objects from this index (synchronous version).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     *
     *  @param objectIDs Identifiers of objects to delete.
     */
    public void deleteObjectsSync(Collection<String> objectIDs) throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        transaction.deleteObjects(objectIDs);
    }

    /**
     * Update an object.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param object New version of the object to update. Must contain an `objectID` attribute.
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request saveObjectAsync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    String objectID = saveObjectSync(object);
                    return new JSONObject()
                        .put("objectID", objectID)
                        .put("updatedAt", DateUtils.iso8601String(new Date()))
                        .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Update an object (synchronous version).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     *
     *  @param object New version of the object to update. Must contain an `objectID` attribute.
     *  @return Identifier of saved object.
     */
    public String saveObjectSync(JSONObject object) throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        Collection<String> objectIDs = saveObjectsSync(Collections.singletonList(object));
        return objectIDs.iterator().next();
    }

    /**
     * Update several objects.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param objects New versions of the objects to update. Each one must contain an `objectID` attribute.
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request saveObjectsAsync(final @NonNull Collection<JSONObject> objects, CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    Collection<String> objectIDs = saveObjectsSync(objects);
                    return new JSONObject()
                        .put("objectIDs", new JSONArray(objectIDs))
                        .put("updatedAt", DateUtils.iso8601String(new Date()))
                        .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Update several objects (synchronous version).
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     *
     *  @param objects New versions of the objects to update. Each one must contain an `objectID` attribute.
     *  @return Identifiers of passed objects.
     */
    public Collection<String> saveObjectsSync(Collection<JSONObject> objects) throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        List<String> objectIDs = new ArrayList<>(objects.size());
        for (JSONObject object : objects) {
            String objectID = object.optString("objectID");
            if (objectID == null) {
                throw new AlgoliaException("Object missing mandatory `objectID` attribute");
            }
            objectIDs.add(objectID);
        }
        transaction.saveObjects(objects);
        return objectIDs;
    }

    /**
     * Set this index's settings.
     *
     * Please refer to our [API documentation](https://www.algolia.com/doc/swift#index-settings) for the list of
     * supported settings.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param settings New settings.
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request setSettingsAsync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    setSettingsSync(settings);
                    return new JSONObject()
                            .put("updatedAt", DateUtils.iso8601String(new Date()))
                            .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Set this index's settings (synchronous version).
     *
     * Please refer to our [API documentation](https://www.algolia.com/doc/swift#index-settings) for the list of
     * supported settings.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     *
     *  @param settings New settings.
     */
    public void setSettingsSync(JSONObject settings) throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        transaction.setSettings(settings);
    }

    /**
     * Delete the index content without removing settings.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  @param completionHandler Completion handler to be notified of the request's outcome.
     *  @return A cancellable operation.
     */
    public Request clearIndexAsync(CompletionHandler completionHandler) {
        assertTransaction();
        return getClient().new AsyncTaskRequest(completionHandler, getClient().transactionExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    clearIndexSync();
                    return new JSONObject()
                            .put("updatedAt", DateUtils.iso8601String(new Date()))
                            .put("taskID", transaction.id);
                } catch (JSONException e) {
                    throw new RuntimeException(e); // should never happen
                }
            }
        }.start();
    }

    /**
     * Delete the index content without removing settings.
     *
     *  **Warning:** This method will assert/crash if no transaction is currently open.
     *
     *  **Warning:** This method must not be called from the main thread.
     */
    public void clearIndexSync() throws AlgoliaException {
        assertNotMainThread();
        assertTransaction();
        transaction.clearIndex();
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
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler, getClient().localBuildExecutorService) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                try {
                    Collection<String> deletedObjectIDs = deleteByQuerySync(queryCopy);
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

    private Collection<String> deleteByQuerySync(@NonNull Query query) throws AlgoliaException {
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
            deleteObjectsSync(objectIDsToDelete);
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
            File tmpDir = client.getTemporaryDirectory();
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

    private void assertTransaction() {
        if (transaction == null) {
            throw new IllegalStateException("Write operations must be wrapped inside a transaction");
        }
    }

    private void assertNotMainThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Log.println(Log.ASSERT, "AlgoliaSearch", "Synchronous methods should not be called from the main thread");
        }
    }
}
