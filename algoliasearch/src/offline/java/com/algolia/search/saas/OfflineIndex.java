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
import android.support.annotation.Nullable;

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
import java.util.List;
import java.util.Map;


/**
 * A purely offline index.
 * Such an index has no online counterpart. It is updated and queried locally.
 *
 * **Note:** You cannot construct this class directly. Please use {@link OfflineClient#initOfflineIndex(String)} to
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
 *   As a consequence, {@link #addObjectAsync} and {@link #saveObjectAsync} are synonyms.
 *
 * - **Partial updates** are not supported.
 *
 * - **Batch** operations are not supported.
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
 * ### Cancellation
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
    // Operations
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
     * Add an object to this index (asynchronously).
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object The object to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectAsync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
        return saveObjectAsync(object, completionHandler);
    }

    private JSONObject addObjectSync(final @NonNull JSONObject object) throws AlgoliaException {
        return saveObjectSync(object);
    }

    /**
     * Add an object to this index, assigning it the specified object ID (asynchronously).
     * If an object already exists with the same object ID, the existing object will be overwritten.
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object The object to add.
     * @param objectID Identifier that you want to assign this object.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectAsync(final @NonNull JSONObject object, final @NonNull String objectID, CompletionHandler completionHandler)  {
        return saveObjectAsync(objectWithID(object, objectID), completionHandler);
    }

    /**
     * Add several objects to this index (asynchronously).
     *
     * @param objects Objects to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectsAsync(final @NonNull Collection<JSONObject> objects, CompletionHandler completionHandler) {
        return saveObjectsAsync(objects, completionHandler);
    }

    /**
     * Update an object (asynchronously).
     *
     * @param object New version of the object to update.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectAsync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return saveObjectSync(object);
            }
        }.start();
    }

    private JSONObject saveObjectSync(final @NonNull JSONObject object) throws AlgoliaException {
        try {
            JSONObject content = saveObjectsSync(Collections.singletonList(object));
            final Object objectID = content.getJSONArray("objectIDs").get(0);
            return new JSONObject()
                .put("updatedAt", DateUtils.iso8601String(new Date()))
                .put("objectID", objectID);
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Update several objects (asynchronously).
     *
     * @param objects Objects to update. Each object must contain an <code>objectID</code> attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectsAsync(final @NonNull Collection<JSONObject> objects, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return saveObjectsSync(objects);
            }
        }.start();
    }

    private JSONObject saveObjectsSync(final @NonNull Collection<JSONObject> objects) throws AlgoliaException {
        File objectFile = null;
        try {
            JSONArray objectIDs = new JSONArray();
            for (JSONObject object : objects) {
                Object objectID = object.opt("objectID");
                if (objectID == null) {
                    throw new AlgoliaException("Object missing mandatory `objectID` attribute");
                }
                objectIDs.put(objectID);
            }
            objectFile = writeTempJSONFile(objects);
            int status = localIndex.build(null /* settings */, new String[] { objectFile.getAbsolutePath() }, false /* clearIndex */, null /* deletedObjectIDs */);
            if (status != 200) {
                throw new AlgoliaException("Failed to build index", status);
            }
            return new JSONObject()
                .put("objectIDs", objectIDs);
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        } finally {
            if (objectFile != null)
                objectFile.delete();
        }
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
     * Delete an object from this index (asynchronously).
     *
     * @param objectID Identifier of the object to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectAsync(final @NonNull String objectID, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteObjectSync(objectID);
            }
        }.start();
    }

    private JSONObject deleteObjectSync(@NonNull String objectID) throws AlgoliaException {
        try {
            deleteObjectsSync(Collections.singletonList(objectID));
            return new JSONObject()
                .put("deletedAt", DateUtils.iso8601String(new Date()));
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Delete several objects from this index (asynchronously).
     *
     * @param objectIDs Identifiers of objects to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectsAsync(final @NonNull List<String> objectIDs, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteObjectsSync(objectIDs);
            }
        }.start();
    }

    private JSONObject deleteObjectsSync(@NonNull List<String> objectIDs) throws AlgoliaException {
        try {
            final String[] deletedObjectIDs = objectIDs.toArray(new String[objectIDs.size()]);
            int status = localIndex.build(null /* settings */, new String[] {}, false /* clearIndex */, deletedObjectIDs /* deletedObjectIDs */);
            if (status != 200) {
                throw new AlgoliaException("Failed to build index", status);
            }
            return new JSONObject()
                .put("objectIDs", new JSONArray(objectIDs));
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
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
     * Set this index's settings (asynchronously).
     *
     * Please refer to our <a href="https://www.algolia.com/doc/android#index-settings">API documentation</a> for the
     * list of supported settings.
     *
     * @param settings New settings.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request setSettingsAsync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return setSettingsSync(settings);
            }
        }.start();
    }

    private JSONObject setSettingsSync(@NonNull JSONObject settings) throws AlgoliaException {
        File settingsFile = null;
        try {
            settingsFile = writeTempJSONFile(settings);
            int status = localIndex.build(settingsFile.getAbsolutePath(), new String[] { }, false /* clearIndex */, null /* deletedObjectIDs */);
            if (status != 200) {
                throw new AlgoliaException("Failed to build index", status);
            }
            return new JSONObject();
        } finally {
            if (settingsFile != null)
                settingsFile.delete();
        }
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

    /**
     * Clear this index.
     *
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request clearIndexAsync(CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return clearIndexSync();
            }
        }.start();
    }

    private JSONObject clearIndexSync() throws AlgoliaException {
        try {
            int status = localIndex.build(null, new String[] { }, true /* clearIndex */, null /* deletedObjectIDs */);
            if (status != 200) {
                throw new AlgoliaException("Failed to build index", status);
            }
            return new JSONObject()
                .put("updatedAt", DateUtils.iso8601String(new Date()));
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
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
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override
            JSONObject run() throws AlgoliaException {
                return deleteByQuerySync(queryCopy);
            }
        }.start();
    }

    private JSONObject deleteByQuerySync(@NonNull Query query) throws AlgoliaException {
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
            return deleteObjectsSync(objectIDsToDelete);
        } catch (JSONException e) {
            throw new AlgoliaException("Invalid JSON results", e);
        }
    }

    // ----------------------------------------------------------------------
    // Helpers
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
    private @NonNull File writeTempJSONFile(@NonNull JSONObject object) throws AlgoliaException {
        return writeTempFile(object.toString());
    }

    /**
     * Write a temporary file containing JSON objects. The files are written as an array.
     *
     * @param objects JSON objects to write.
     * @return Path to the created file.
     * @throws AlgoliaException
     */
    private File writeTempJSONFile(@NonNull Collection<JSONObject> objects) throws AlgoliaException {
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
}
