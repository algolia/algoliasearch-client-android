/*
 * Copyright (c) 2015 Algolia
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A proxy to an Algolia index.
 * <p>
 * You cannot construct this class directly. Please use {@link Client#initIndex(String)} to obtain an instance.
 * </p>
 * <p>
 * WARNING: For performance reasons, arguments to asynchronous methods are not cloned. Therefore, you should not
 * modify mutable arguments after they have been passed (unless explicitly noted).
 * </p>
 */
public class Index {
    /** The client to which this index belongs. */
    private Client client;

    /** This index's name. */
    private String indexName;

    /** This index's name, URL-encoded. Cached for optimization. */
    private String encodedIndexName;

    private ExpiringCache<String, byte[]> searchCache;
    private boolean isCacheEnabled = false;

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final long MAX_TIME_MS_TO_WAIT = 10000L;

    // ----------------------------------------------------------------------
    // Initialization
    // ----------------------------------------------------------------------

    protected Index(@NonNull Client client, @NonNull String indexName) {
        try {
            this.client = client;
            this.encodedIndexName = URLEncoder.encode(indexName, "UTF-8");
            this.indexName = indexName;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // should never happen, as UTF-8 is always supported
        }
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    @Override public String toString() {
        return String.format("%s{%s}", this.getClass().getSimpleName(), getIndexName());
    }

    public String getIndexName() {
        return indexName;
    }

    public Client getClient() {
        return client;
    }

    protected String getEncodedIndexName() {
        return encodedIndexName;
    }

    // ----------------------------------------------------------------------
    // Public operations
    // ----------------------------------------------------------------------

    /**
     * Search inside this index (asynchronously).
     *
     * @param query             Search parameters. May be null to use an empty query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return search(queryCopy);
            }
        }.start();
    }

    /**
     * Search inside this index (synchronously).
     *
     * @return Search results.
     */
    public JSONObject searchSync(@NonNull Query query) throws AlgoliaException {
        return search(query);
    }

    /**
     * Run multiple queries on this index with one API call.
     * A variant of {@link Client#multipleQueriesAsync(List, Client.MultipleQueriesStrategy, CompletionHandler)}
     * where the targeted index is always the receiver.
     *
     * @param queries           The queries to run.
     * @param strategy          The strategy to use.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<Query> queries, final Client.MultipleQueriesStrategy strategy, @NonNull CompletionHandler completionHandler) {
        final List<Query> queriesCopy = new ArrayList<>(queries.size());
        for (Query query : queries) {
            queriesCopy.add(new Query(query));
        }
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return multipleQueries(queriesCopy, strategy == null ? null : strategy.toString());
            }
        }.start();
    }

    /**
     * Search inside this index synchronously.
     *
     * @return Search results.
     */
    protected byte[] searchSyncRaw(@NonNull Query query) throws AlgoliaException {
        return searchRaw(query);
    }

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
        final List<Query> queries = computeDisjunctiveFacetingQueries(query, disjunctiveFacets, refinements);
        return multipleQueriesAsync(queries, null, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject content, AlgoliaException error) {
                JSONObject aggregatedResults = null;
                try {
                    if (content != null) {
                        aggregatedResults = aggregateDisjunctiveFacetingResults(content, disjunctiveFacets, refinements);
                    }
                } catch (AlgoliaException e) {
                    error = e;
                }
                completionHandler.requestCompleted(aggregatedResults, error);
            }
        });
    }

    /**
     * Add an object to this index (asynchronously).
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object            The object to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectAsync(final @NonNull JSONObject object, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return addObject(object);
            }
        }.start();
    }

    /**
     * Add an object to this index, assigning it the specified object ID (asynchronously).
     * If an object already exists with the same object ID, the existing object will be overwritten.
     * <p>
     * WARNING: For performance reasons, the arguments are not cloned. Since the method is executed in the background,
     * you should not modify the object after it has been passed.
     * </p>
     *
     * @param object            The object to add.
     * @param objectID          Identifier that you want to assign this object.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectAsync(final @NonNull JSONObject object, final @NonNull String objectID, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return addObject(object, objectID);
            }
        }.start();
    }

    /**
     * Add several objects to this index (asynchronously).
     *
     * @param objects           Objects to add.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectsAsync(final @NonNull JSONArray objects, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return addObjects(objects);
            }
        }.start();
    }

    /**
     * Update an object (asynchronously).
     *
     * @param object            New version of the object to update.
     * @param objectID          Identifier of the object to update.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectAsync(final @NonNull JSONObject object, final @NonNull String objectID, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return saveObject(object, objectID);
            }
        }.start();
    }

    /**
     * Update several objects (asynchronously).
     *
     * @param objects           Objects to update. Each object must contain an <code>objectID</code> attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectsAsync(final @NonNull JSONArray objects, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return saveObjects(objects);
            }
        }.start();
    }

    /**
     * Partially update an object (asynchronously).
     *
     * @param partialObject     New value/operations for the object.
     * @param objectID          Identifier of object to be updated.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectAsync(final @NonNull JSONObject partialObject, final @NonNull String objectID, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return partialUpdateObject(partialObject, objectID);
            }
        }.start();
    }

    /**
     * Partially update several objects (asynchronously).
     *
     * @param partialObjects    New values/operations for the objects. Each object must contain an <code>objectID</code>
     *                          attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectsAsync(final @NonNull JSONArray partialObjects, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return partialUpdateObjects(partialObjects);
            }
        }.start();
    }

    /**
     * Get an object from this index (asynchronously).
     *
     * @param objectID          Identifier of the object to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectAsync(final @NonNull String objectID, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return getObject(objectID);
            }
        }.start();
    }

    /**
     * Get an object from this index, optionally restricting the retrieved content (asynchronously).
     *
     * @param objectID             Identifier of the object to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param completionHandler    The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectAsync(final @NonNull String objectID, final List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return getObject(objectID, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Get several objects from this index (asynchronously).
     *
     * @param objectIDs         Identifiers of objects to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsAsync(final @NonNull List<String> objectIDs, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return getObjects(objectIDs);
            }
        }.start();
    }

    /**
     * Get several objects from this index (asynchronously), optionally restricting the retrieved content (asynchronously).
     *
     * @param objectIDs            Identifiers of objects to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param completionHandler    The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsAsync(final @NonNull List<String> objectIDs, final List<String> attributesToRetrieve, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return getObjects(objectIDs, attributesToRetrieve);
            }
        }.start();
    }

    /**
     * Wait until the publication of a task on the server (helper).
     * All server tasks are asynchronous. This method helps you check that a task is published.
     *
     * @param taskID            Identifier of the task (as returned by the server).
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request waitTaskAsync(final @NonNull String taskID, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return waitTask(taskID);
            }
        }.start();
    }

    /**
     * Delete an object from this index (asynchronously).
     *
     * @param objectID          Identifier of the object to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectAsync(final @NonNull String objectID, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return deleteObject(objectID);
            }
        }.start();
    }

    /**
     * Delete several objects from this index (asynchronously).
     *
     * @param objectIDs         Identifiers of objects to delete.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectsAsync(final @NonNull List<String> objectIDs, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return deleteObjects(objectIDs);
            }
        }.start();
    }

    /**
     * Delete all objects matching a query (helper).
     *
     * @param query             The query that objects to delete must match.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteByQueryAsync(@NonNull Query query, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                deleteByQuery(queryCopy);
                return new JSONObject();
            }
        }.start();
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
            @Override JSONObject run() throws AlgoliaException {
                return getSettings();
            }
        }.start();
    }

    /**
     * Set this index's settings (asynchronously).
     *
     * Please refer to our <a href="https://www.algolia.com/doc/android#index-settings">API documentation</a> for the
     * list of supported settings.
     *
     * @param settings          New settings.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request setSettingsAsync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return setSettings(settings);
            }
        }.start();
    }

    /**
     * Browse all index content (initial call).
     * This method should be called once to initiate a browse. It will return the first page of results and a cursor,
     * unless the end of the index has been reached. To retrieve subsequent pages, call `browseFromAsync` with that
     * cursor.
     *
     * @param query             The query parameters for the browse.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseAsync(@NonNull Query query, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return browse(queryCopy);
            }
        }.start();
    }

    /**
     * Browse the index from a cursor.
     * This method should be called after an initial call to `browseAsync()`. It returns a cursor, unless the end of
     * the index has been reached.
     *
     * @param cursor            The cursor of the next page to retrieve.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseFromAsync(final @NonNull String cursor, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override JSONObject run() throws AlgoliaException {
                return browseFrom(cursor);
            }
        }.start();
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
            @Override JSONObject run() throws AlgoliaException {
                return clearIndex();
            }
        }.start();
    }

    // ----------------------------------------------------------------------
    // Search cache
    // ----------------------------------------------------------------------

    /**
     * Enable search cache with default parameters
     */
    public void enableSearchCache() {
        enableSearchCache(ExpiringCache.defaultExpirationTimeout, ExpiringCache.defaultMaxSize);
    }

    /**
     * Enable search cache with custom parameters
     *
     * @param timeoutInSeconds duration during which an request is kept in cache
     * @param maxRequests      maximum amount of requests to keep before removing the least recently used
     */
    public void enableSearchCache(int timeoutInSeconds, int maxRequests) {
        isCacheEnabled = true;
        searchCache = new ExpiringCache<>(timeoutInSeconds, maxRequests);
    }

    /**
     * Disable and reset cache
     */
    public void disableSearchCache() {
        isCacheEnabled = false;
        if (searchCache != null) {
            searchCache.reset();
        }
    }

    /**
     * Remove all entries from cache
     */
    public void clearSearchCache() {
        if (searchCache != null) {
            searchCache.reset();
        }
    }

    // ----------------------------------------------------------------------
    // Internal operations
    // ----------------------------------------------------------------------

    /**
     * Add an object in this index
     *
     * @param obj the object to add.
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj) throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName, obj.toString(), false);
    }

    /**
     * Add an object in this index
     *
     * @param obj      the object to add.
     * @param objectID an objectID you want to attribute to this object
     *                 (if the attribute already exist the old object will be overwrite)
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), obj.toString());
        } catch (UnsupportedEncodingException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @throws AlgoliaException
     */
    protected JSONObject batch(JSONArray actions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("requests", actions);
            return client.postRequest("/1/indexes/" + encodedIndexName + "/batch", content.toString(), false);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Add several objects
     *
     * @param inputArray contains an array of objects to add.
     * @throws AlgoliaException
     */
    protected JSONObject addObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (int n = 0; n < inputArray.length(); n++) {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body", inputArray.getJSONObject(n));
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID the unique identifier of the object to retrieve
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID) throws AlgoliaException {
        try {
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID             the unique identifier of the object to retrieve
     * @param attributesToRetrieve contains the list of attributes to retrieve.
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID, List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            String params = encodeAttributes(attributesToRetrieve, true);
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + params, false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get several objects from this index
     *
     * @param objectIDs the array of unique identifier of objects to retrieve
     * @throws AlgoliaException
     */
    protected JSONObject getObjects(List<String> objectIDs) throws AlgoliaException {
        return getObjects(objectIDs, null);
    }

    /**
     * Get several objects from this index
     *
     * @param objectIDs            the array of unique identifier of objects to retrieve
     * @param attributesToRetrieve contains the list of attributes to retrieve.
     * @throws AlgoliaException
     */
    protected JSONObject getObjects(List<String> objectIDs, List<String> attributesToRetrieve) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (String id : objectIDs) {
                JSONObject request = new JSONObject();
                request.put("indexName", this.indexName);
                request.put("objectID", id);
                request.put("attributesToRetrieve", encodeAttributes(attributesToRetrieve, false));
                requests.put(request);
            }
            JSONObject body = new JSONObject();
            body.put("requests", requests);
            return client.postRequest("/1/indexes/*/objects", body.toString(), true);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private String encodeAttributes(List<String> attributesToRetrieve, boolean forURL) throws UnsupportedEncodingException {
        if (attributesToRetrieve == null) {
            return null;
        }

        StringBuilder params = new StringBuilder();
        if (forURL) {
            params.append("?attributes=");
        }
        for (int i = 0; i < attributesToRetrieve.size(); ++i) {
            if (i > 0) {
                params.append(",");
            }
            params.append(URLEncoder.encode(attributesToRetrieve.get(i), "UTF-8"));
        }
        return params.toString();
    }

    /**
     * Update partially an object (only update attributes passed in argument)
     *
     * @param partialObject the object attributes to override
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObject(JSONObject partialObject, String objectID) throws AlgoliaException {
        try {
            return client.postRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial", partialObject.toString(), false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Partially Override the content of several objects
     *
     * @param inputArray the array of objects to update (each object must contains an objectID attribute)
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (int n = 0; n < inputArray.length(); n++) {
                JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "partialUpdateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Override the content of object
     *
     * @param object the object to save
     * @throws AlgoliaException
     */
    protected JSONObject saveObject(JSONObject object, String objectID) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), object.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of several objects
     *
     * @param inputArray contains an array of objects to update (each object must contains an objectID attribute)
     * @throws AlgoliaException
     */
    protected JSONObject saveObjects(JSONArray inputArray) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (int n = 0; n < inputArray.length(); n++) {
                JSONObject obj = inputArray.getJSONObject(n);
                JSONObject action = new JSONObject();
                action.put("action", "updateObject");
                action.put("objectID", obj.getString("objectID"));
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete an object from the index
     *
     * @param objectID the unique identifier of object to delete
     * @throws AlgoliaException
     */
    protected JSONObject deleteObject(String objectID) throws AlgoliaException {
        if (objectID.length() == 0) {
            throw new AlgoliaException("Invalid objectID");
        }
        try {
            return client.deleteRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete several objects
     *
     * @param objects the array of objectIDs to delete
     * @throws AlgoliaException
     */
    protected JSONObject deleteObjects(List<String> objects) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (String id : objects) {
                JSONObject obj = new JSONObject();
                obj.put("objectID", id);
                JSONObject action = new JSONObject();
                action.put("action", "deleteObject");
                action.put("body", obj);
                array.put(action);
            }
            return batch(array);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete all objects matching a query
     *
     * @param query the query string
     * @throws AlgoliaException
     */
    protected void deleteByQuery(@NonNull Query query) throws AlgoliaException {
        try {
            boolean hasMore;
            do {
                // Browse index for the next batch of objects.
                // WARNING: Since deletion invalidates cursors, we always browse from the start.
                List<String> objectIDs = new ArrayList<>(1000);
                JSONObject content = browse(query);
                JSONArray hits = content.getJSONArray("hits");
                for (int i = 0; i < hits.length(); ++i) {
                    JSONObject hit = hits.getJSONObject(i);
                    objectIDs.add(hit.getString("objectID"));
                }
                hasMore = content.optString("cursor", null) != null;

                // Delete objects.
                JSONObject task = this.deleteObjects(objectIDs);
                this.waitTask(task.getString("taskID"));
            }
            while (hasMore);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Search inside the index
     *
     * @return a JSONObject containing search results
     * @throws AlgoliaException
     */
    protected JSONObject search(@NonNull Query query) throws AlgoliaException {
        String cacheKey = null;
        byte[] rawResponse = null;
        if (isCacheEnabled) {
            cacheKey = query.build();
            rawResponse = searchCache.get(cacheKey);
        }
        try {
            if (rawResponse == null) {
                rawResponse = searchRaw(query);
                if (isCacheEnabled) {
                    searchCache.put(cacheKey, rawResponse);
                }
            }
            return Client._getJSONObject(rawResponse);
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Search inside the index
     *
     * @return a byte array containing search results
     * @throws AlgoliaException
     */
    protected byte[] searchRaw(@NonNull Query query) throws AlgoliaException {
        try {
            String paramsString = query.build();
            if (paramsString.length() > 0) {
                JSONObject body = new JSONObject();
                body.put("params", paramsString);
                return client.postRequestRaw("/1/indexes/" + encodedIndexName + "/query", body.toString(), true);
            } else {
                return client.getRequestRaw("/1/indexes/" + encodedIndexName, true);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Wait the publication of a task on the server.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID     the id of the task returned by server
     * @param timeToWait time to sleep seed
     * @throws AlgoliaException
     */
    protected JSONObject waitTask(String taskID, long timeToWait) throws AlgoliaException {
        try {
            while (true) {
                JSONObject obj = client.getRequest("/1/indexes/" + encodedIndexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"), false);
                if (obj.getString("status").equals("published")) {
                    return obj;
                }
                try {
                    Thread.sleep(timeToWait >= MAX_TIME_MS_TO_WAIT ? MAX_TIME_MS_TO_WAIT : timeToWait);
                } catch (InterruptedException e) {
                    continue;
                }
                timeToWait *= 2;
            }
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wait the publication of a task on the server.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @throws AlgoliaException
     */
    protected JSONObject waitTask(String taskID) throws AlgoliaException {
        return waitTask(taskID, MAX_TIME_MS_TO_WAIT);
    }

    /**
     * Get settings of this index
     *
     * @throws AlgoliaException
     */
    protected JSONObject getSettings() throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/settings", false);
    }

    /**
     * Set settings for this index, not forwarding to slave indices.
     *
     * @param settings the settings object.
     * @throws AlgoliaException
     */
    protected JSONObject setSettings(JSONObject settings) throws AlgoliaException {
        return setSettings(settings, false);
    }

    /**
     * Set settings for this index.
     *
     * @param settings        the settings object.
     * @param forwardToSlaves if true, the new settings will be forwarded to slave indices.
     * @throws AlgoliaException
     */
    protected JSONObject setSettings(JSONObject settings, boolean forwardToSlaves) throws AlgoliaException {
        final String url = "/1/indexes/" + encodedIndexName + "/settings" + "?forwardToSlaves=" + forwardToSlaves;
        return client.putRequest(url, settings.toString());
    }

    /**
     * Delete the index content without removing settings and index specific API keys.
     *
     * @throws AlgoliaException
     */
    protected JSONObject clearIndex() throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName + "/clear", "", false);
    }

    /**
     * Filter disjunctive refinements from generic refinements and a list of disjunctive facets.
     *
     * @param disjunctiveFacets the array of disjunctive facets
     * @param refinements       Map representing the current refinements
     * @return The disjunctive refinements
     */
    private
    @NonNull
    Map<String, List<String>> computeDisjunctiveRefinements(@NonNull List<String> disjunctiveFacets, @NonNull Map<String, List<String>> refinements) {
        Map<String, List<String>> disjunctiveRefinements = new HashMap<>();
        for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
            if (disjunctiveFacets.contains(elt.getKey())) {
                disjunctiveRefinements.put(elt.getKey(), elt.getValue());
            }
        }
        return disjunctiveRefinements;
    }

    /**
     * Compute the queries to run to implement disjunctive faceting.
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @return A list of queries suitable for {@link Index#multipleQueries}.
     */
    private @NonNull
    List<Query> computeDisjunctiveFacetingQueries(@NonNull Query query, @NonNull List<String> disjunctiveFacets, @NonNull Map<String, List<String>> refinements) {
        // Retain only refinements corresponding to the disjunctive facets.
        Map<String, List<String>> disjunctiveRefinements = computeDisjunctiveRefinements(disjunctiveFacets, refinements);

        // build queries
        List<Query> queries = new ArrayList<>();
        // hits + regular facets query
        JSONArray filters = new JSONArray();
        for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
            JSONArray or = new JSONArray();
            final boolean isDisjunctiveFacet = disjunctiveRefinements.containsKey(elt.getKey());
            for (String val : elt.getValue()) {
                if (isDisjunctiveFacet) {
                    // disjunctive refinements are ORed
                    or.put(String.format("%s:%s", elt.getKey(), val));
                } else {
                    filters.put(String.format("%s:%s", elt.getKey(), val));
                }
            }
            // Add or
            if (isDisjunctiveFacet) {
                filters.put(or);
            }
        }

        queries.add(new Query(query).setFacetFilters(filters));
        // one query per disjunctive facet (use all refinements but the current one + hitsPerPage=1 + single facet
        for (String disjunctiveFacet : disjunctiveFacets) {
            filters = new JSONArray();

            for (Map.Entry<String, List<String>> elt : refinements.entrySet()) {
                if (disjunctiveFacet.equals(elt.getKey())) {
                    continue;
                }
                JSONArray or = new JSONArray();
                final boolean isDisjunctiveFacet = disjunctiveRefinements.containsKey(elt.getKey());
                for (String val : elt.getValue()) {
                    if (isDisjunctiveFacet) {
                        // disjunctive refinements are ORed
                        or.put(String.format("%s:%s", elt.getKey(), val));
                    } else {
                        filters.put(String.format("%s:%s", elt.getKey(), val));
                    }
                }
                // Add or
                if (isDisjunctiveFacet) {
                    filters.put(or);
                }
            }
            String[] facets = new String[]{disjunctiveFacet};
            queries.add(new Query(query).setHitsPerPage(0).setAnalytics(false)
                    .setAttributesToRetrieve().setAttributesToHighlight().setAttributesToSnippet()
                    .setFacets(facets).setFacetFilters(filters));
        }
        return queries;
    }

    /**
     * Aggregate results from multiple queries into disjunctive faceting results.
     *
     * @param answers           The response from the multiple queries.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       Facet refinements.
     * @return The aggregated results.
     * @throws AlgoliaException
     */
    JSONObject aggregateDisjunctiveFacetingResults(@NonNull JSONObject answers, @NonNull List<String> disjunctiveFacets, @NonNull Map<String, List<String>> refinements) throws AlgoliaException {
        Map<String, List<String>> disjunctiveRefinements = computeDisjunctiveRefinements(disjunctiveFacets, refinements);

        // aggregate answers
        // first answer stores the hits + regular facets
        try {
            boolean nonExhaustiveFacetsCount = false;
            JSONArray results = answers.getJSONArray("results");
            JSONObject aggregatedAnswer = results.getJSONObject(0);
            JSONObject disjunctiveFacetsJSON = new JSONObject();
            for (int i = 1; i < results.length(); ++i) {
                if (!results.getJSONObject(i).optBoolean("exhaustiveFacetsCount")) {
                    nonExhaustiveFacetsCount = true;
                }
                JSONObject facets = results.getJSONObject(i).getJSONObject("facets");
                @SuppressWarnings("unchecked")
                Iterator<String> keys = facets.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    // Add the facet to the disjunctive facet hash
                    disjunctiveFacetsJSON.put(key, facets.getJSONObject(key));
                    // concatenate missing refinements
                    if (!disjunctiveRefinements.containsKey(key)) {
                        continue;
                    }
                    for (String refine : disjunctiveRefinements.get(key)) {
                        if (!disjunctiveFacetsJSON.getJSONObject(key).has(refine)) {
                            disjunctiveFacetsJSON.getJSONObject(key).put(refine, 0);
                        }
                    }
                }
            }
            aggregatedAnswer.put("disjunctiveFacets", disjunctiveFacetsJSON);
            if (nonExhaustiveFacetsCount) {
                aggregatedAnswer.put("exhaustiveFacetsCount", false);
            }
            return aggregatedAnswer;
        } catch (JSONException e) {
            throw new AlgoliaException("Failed to aggregate results", e);
        }
    }

    protected JSONObject browse(@NonNull Query query) throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/browse?" + query.build(), true);
    }

    protected JSONObject browseFrom(@NonNull String cursor) throws AlgoliaException {
        try {
            return client.getRequest("/1/indexes/" + encodedIndexName + "/browse?cursor=" + URLEncoder.encode(cursor, "UTF-8"), true);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); // Should never happen: UTF-8 is always supported.
        }
    }

    /**
     * Run multiple queries on this index with one API call.
     * A variant of {@link Client#multipleQueries(List, String)} where all queries target this index.
     *
     * @param queries  Queries to run.
     * @param strategy Strategy to use.
     * @return The JSON results returned by the server.
     * @throws AlgoliaException
     */
    protected JSONObject multipleQueries(@NonNull List<Query> queries, String strategy) throws AlgoliaException {
        List<IndexQuery> requests = new ArrayList<>(queries.size());
        for (Query query : queries) {
            requests.add(new IndexQuery(this, query));
        }
        return client.multipleQueries(requests, strategy);
    }
}