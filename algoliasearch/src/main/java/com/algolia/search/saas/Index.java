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

import com.algolia.search.saas.helpers.DisjunctiveFaceting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A proxy to an Algolia index.
 * <p>
 * You cannot construct this class directly. Please use {@link Client#getIndex(String)} to obtain an instance.
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

    // Search
    // ------

    /**
     * Search inside this index (asynchronously).
     *
     * @param query             Search parameters. May be null to use an empty query.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchAsync(@Nullable Query query, @Nullable final RequestOptions requestOptions, @Nullable CompletionHandler completionHandler) {
        final Query queryCopy = query != null ? new Query(query) : new Query();
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return search(queryCopy, requestOptions);
            }
        }.start();
    }

    /**
     * Search inside this index (asynchronously).
     *
     * @param query             Search parameters. May be null to use an empty query.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchAsync(@Nullable Query query, @Nullable CompletionHandler completionHandler) {
        return searchAsync(query, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Search inside this index (synchronously).
     *
     * @param query Search parameters.
     * @param requestOptions Request-specific options.
     * @return Search results.
     */
    public JSONObject searchSync(@Nullable Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        return search(query, requestOptions);
    }

    /**
     * Search inside this index (synchronously).
     *
     * @return Search results.
     */
    public JSONObject searchSync(@Nullable Query query) throws AlgoliaException {
        return searchSync(query, /* requestOptions: */ null);
    }

    /**
     * Run multiple queries on this index with one API call.
     * A variant of {@link Client#multipleQueriesAsync(List, Client.MultipleQueriesStrategy, CompletionHandler)}
     * where the targeted index is always the receiver.
     *
     * @param queries           The queries to run.
     * @param strategy          The strategy to use.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request multipleQueriesAsync(final @NonNull List<Query> queries, final Client.MultipleQueriesStrategy strategy, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        final List<Query> queriesCopy = new ArrayList<>(queries.size());
        for (Query query : queries) {
            queriesCopy.add(new Query(query));
        }
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return multipleQueries(queriesCopy, strategy == null ? null : strategy.toString(), requestOptions);
            }
        }.start();
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
        return multipleQueriesAsync(queries, strategy, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Search inside this index synchronously.
     *
     * @param query Search parameters.
     * @param requestOptions Request-specific options.
     * @return Search results.
     */
    protected byte[] searchSyncRaw(@NonNull Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        return searchRaw(query, requestOptions);
    }

    /**
     * Perform a search with disjunctive facets, generating as many queries as number of disjunctive facets (helper).
     *
     * @param query             The query.
     * @param disjunctiveFacets List of disjunctive facets.
     * @param refinements       The current refinements, mapping facet names to a list of values.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchDisjunctiveFacetingAsync(@NonNull Query query, @NonNull final List<String> disjunctiveFacets, @NonNull final Map<String, List<String>> refinements, @Nullable final RequestOptions requestOptions, @NonNull final CompletionHandler completionHandler) {
        return new DisjunctiveFaceting() {
            @Override
            protected Request multipleQueriesAsync(@NonNull List<Query> queries, @NonNull CompletionHandler completionHandler) {
                return Index.this.multipleQueriesAsync(queries, null, requestOptions, completionHandler);
            }
        }.searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, completionHandler);
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
        return searchDisjunctiveFacetingAsync(query, disjunctiveFacets, refinements, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Search (asynchronously) for some text in a facet values.
     *
     * @param facetName The name of the facet to search. It must have been declared in the index's `attributesForFaceting` setting with the `searchable()` modifier.
     * @param text      The text to search for in the facet's values.
     * @param handler   A Completion handler that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchForFacetValuesAsync(@NonNull String facetName, @NonNull String text, @NonNull final CompletionHandler handler) throws AlgoliaException {
        return searchForFacetValuesAsync(facetName, text, /* requestOptions: */ null, handler);
    }

    /**
     * Search (asynchronously) for some text in a facet values.
     *
     * @deprecated Please use {@link #searchForFacetValuesAsync(String, String, CompletionHandler)} instead.
     *
     * @param facetName The name of the facet to search. It must have been declared in the index's `attributesForFaceting` setting with the `searchable()` modifier.
     * @param text      The text to search for in the facet's values.
     * @param handler   A Completion handler that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchForFacetValues(@NonNull String facetName, @NonNull String text, @NonNull final CompletionHandler handler) {
        return searchForFacetValuesAsync(facetName, text, /* requestOptions: */ null, handler);
    }

    /**
     * Search for some text in a facet values, optionally restricting the returned values to those contained in objects matching other (regular) search criteria.
     *
     * @param facetName The name of the facet to search. It must have been declared in the index's `attributesForFaceting` setting with the `searchable()` modifier.
     * @param facetText The text to search for in the facet's values.
     * @param query     An optional query to take extra search parameters into account. There parameters apply to index objects like in a regular search query. Only facet values contained in the matched objects will be returned
     * @param handler   A Completion handler that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchForFacetValuesAsync(@NonNull String facetName, @NonNull String facetText, @Nullable Query query, @NonNull final CompletionHandler handler) {
        return searchForFacetValuesAsync(facetName, facetText, query, /* requestOptions: */ null, handler);
    }

    /**
     * Search for some text in a facet values, optionally restricting the returned values to those contained in objects matching other (regular) search criteria.
     *
     * @param facetName The name of the facet to search. It must have been declared in the index's `attributesForFaceting` setting with the `searchable()` modifier.
     * @param facetText The text to search for in the facet's values.
     * @param query     An optional query to take extra search parameters into account. There parameters apply to index objects like in a regular search query. Only facet values contained in the matched objects will be returned
     * @param requestOptions Request-specific options.
     * @param handler   A Completion handler that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchForFacetValuesAsync(@NonNull String facetName, @NonNull String facetText, @Nullable Query query, @Nullable final RequestOptions requestOptions, @NonNull final CompletionHandler handler) {
        try {
            final String path = "/1/indexes/" + getEncodedIndexName() + "/facets/" + URLEncoder.encode(facetName, "UTF-8") + "/query";
            final Query params = (query != null ? new Query(query) : new Query());
            params.set("facetQuery", facetText);
            final JSONObject requestBody = new JSONObject().put("params", params.build());

            final Client client = getClient();
            return client.new AsyncTaskRequest(handler) {
                @NonNull
                @Override
                protected JSONObject run() throws AlgoliaException {
                    return client.postRequest(path, /* urlParameters: */ null, requestBody.toString(), true, requestOptions);
                }
            }.start();
        } catch (UnsupportedEncodingException | JSONException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    /**
     * Search (asynchronously) for some text in a facet values, optionally restricting the returned values to those contained in objects matching other (regular) search criteria.
     *
     * @deprecated Please use {@link #searchForFacetValuesAsync(String, String, Query, CompletionHandler)} instead.
     *
     * @param facetName The name of the facet to search. It must have been declared in the index's `attributesForFaceting` setting with the `searchable()` modifier.
     * @param facetText The text to search for in the facet's values.
     * @param query     An optional query to take extra search parameters into account. There parameters apply to index objects like in a regular search query. Only facet values contained in the matched objects will be returned
     * @param handler   A Completion handler that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request searchForFacetValues(@NonNull String facetName, @NonNull String facetText, @Nullable Query query, @NonNull final CompletionHandler handler) {
        return searchForFacetValuesAsync(facetName, facetText, query, handler);
    }

    // Manage objects
    // --------------

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
            @Override protected JSONObject run() throws AlgoliaException {
                return addObject(object, /* requestOptions: */ null);
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
        return addObjectAsync(object, objectID, /* requestOptions: */ null, completionHandler);
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
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectAsync(final @NonNull JSONObject object, final @NonNull String objectID, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return addObject(object, objectID, requestOptions);
            }
        }.start();
    }

    /**
     * Add several objects to this index (asynchronously).
     *
     * @param objects           Objects to add.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request addObjectsAsync(final @NonNull JSONArray objects, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return addObjects(objects, requestOptions);
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
        return addObjectsAsync(objects, /* requestOptions: */ null, completionHandler);
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
        return saveObjectAsync(object, objectID, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Update an object (asynchronously).
     *
     * @param object            New version of the object to update.
     * @param objectID          Identifier of the object to update.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectAsync(final @NonNull JSONObject object, final @NonNull String objectID, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return saveObject(object, objectID, requestOptions);
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
        return saveObjectsAsync(objects, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Update several objects (asynchronously).
     *
     * @param objects           Objects to update. Each object must contain an <code>objectID</code> attribute.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request saveObjectsAsync(final @NonNull JSONArray objects, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return saveObjects(objects, requestOptions);
            }
        }.start();
    }

    /**
     * Partially update an object (asynchronously).
     * <p>
     * **Note:** This method will create the object if it does not exist already. If you don't wish to, you can use
     * {@link #partialUpdateObjectAsync(JSONObject, String, boolean, CompletionHandler)} and specify `false` for the
     * `createIfNotExists` argument.
     *
     * @param partialObject     New value/operations for the object.
     * @param objectID          Identifier of object to be updated.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectAsync(final @NonNull JSONObject partialObject, final @NonNull String objectID, CompletionHandler completionHandler) {
        return partialUpdateObjectAsync(partialObject, objectID, /* createIfNotExists: */ true, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Partially update an object (asynchronously).
     *
     * @param partialObject     New value/operations for the object.
     * @param objectID          Identifier of object to be updated.
     * @param createIfNotExists Whether the object should be created if it does not exist already.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectAsync(final @NonNull JSONObject partialObject, final @NonNull String objectID, final boolean createIfNotExists, CompletionHandler completionHandler) {
        return partialUpdateObjectAsync(partialObject, objectID, createIfNotExists, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Partially update an object (asynchronously).
     *
     * @param partialObject     New value/operations for the object.
     * @param objectID          Identifier of object to be updated.
     * @param createIfNotExists Whether the object should be created if it does not exist already.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectAsync(final @NonNull JSONObject partialObject, final @NonNull String objectID, final boolean createIfNotExists, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return partialUpdateObject(partialObject, objectID, createIfNotExists, requestOptions);
            }
        }.start();
    }

    /**
     * Partially update several objects (asynchronously).
     * <p>
     * **Note:** This method will create the objects if they do not exist already. If you don't wish to, you can use
     * {@link #partialUpdateObjectsAsync(JSONArray, boolean, CompletionHandler)} and specify `false` for the
     * `createIfNotExists` argument.
     *
     * @param partialObjects    New values/operations for the objects. Each object must contain an <code>objectID</code>
     *                          attribute.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectsAsync(final @NonNull JSONArray partialObjects, CompletionHandler completionHandler) {
        return partialUpdateObjectsAsync(partialObjects, /* createIfNotExists: */ true, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Partially update several objects (asynchronously).
     *
     * @param partialObjects    New values/operations for the objects. Each object must contain an <code>objectID</code>
     *                          attribute.
     * @param createIfNotExists Whether objects should be created if they do not exist already.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectsAsync(final @NonNull JSONArray partialObjects, final boolean createIfNotExists, CompletionHandler completionHandler) {
        return partialUpdateObjectsAsync(partialObjects, createIfNotExists, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Partially update several objects (asynchronously).
     *
     * @param partialObjects    New values/operations for the objects. Each object must contain an <code>objectID</code>
     *                          attribute.
     * @param createIfNotExists Whether objects should be created if they do not exist already.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request partialUpdateObjectsAsync(final @NonNull JSONArray partialObjects, final boolean createIfNotExists, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return partialUpdateObjects(partialObjects, createIfNotExists, requestOptions);
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
        return getObjectAsync(objectID, /* attributesToRetrieve: */ null, /* requestOptions: */ null, completionHandler);
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
        return getObjectAsync(objectID, attributesToRetrieve, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Get an object from this index, optionally restricting the retrieved content (asynchronously).
     *
     * @param objectID             Identifier of the object to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param requestOptions Request-specific options.
     * @param completionHandler    The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectAsync(final @NonNull String objectID, final List<String> attributesToRetrieve, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return getObject(objectID, attributesToRetrieve, requestOptions);
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
        return getObjectsAsync(objectIDs, /* attributesToRetrieve: */ null, /* requestOptions: */ null, completionHandler);
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
        return getObjectsAsync(objectIDs, attributesToRetrieve, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Get several objects from this index (asynchronously), optionally restricting the retrieved content (asynchronously).
     *
     * @param objectIDs            Identifiers of objects to retrieve.
     * @param attributesToRetrieve List of attributes to retrieve.
     * @param requestOptions Request-specific options.
     * @param completionHandler    The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getObjectsAsync(final @NonNull List<String> objectIDs, final List<String> attributesToRetrieve, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return getObjects(objectIDs, attributesToRetrieve, requestOptions);
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
     * @deprecated Task IDs are always integers. Please use {@link #waitTaskAsync(int, CompletionHandler)} instead.
     */
    public Request waitTaskAsync(final @NonNull String taskID, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return waitTask(taskID);
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
    public Request waitTaskAsync(final int taskID, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return waitTask(Integer.toString(taskID));
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
        return deleteObjectAsync(objectID, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Delete an object from this index (asynchronously).
     *
     * @param objectID          Identifier of the object to delete.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectAsync(final @NonNull String objectID, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return deleteObject(objectID, requestOptions);
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
        return deleteObjectsAsync(objectIDs, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Delete several objects from this index (asynchronously).
     *
     * @param objectIDs         Identifiers of objects to delete.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request deleteObjectsAsync(final @NonNull List<String> objectIDs, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return deleteObjects(objectIDs, requestOptions);
            }
        }.start();
    }

    /**
     * Delete all objects matching a query (helper) using browse and deleteObjects.
     *
     * @param query             The query that objects to delete must match.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     * @deprecated use {@link Index#deleteByAsync(Query, CompletionHandler)} instead.
     */
    public Request deleteByQueryAsync(@NonNull Query query, CompletionHandler completionHandler) {
        return deleteByQueryAsync(query, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Delete all objects matching a query (helper).
     *
     * @param query             The query that objects to delete must match.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @deprecated use {@link Index#deleteByAsync(Query, CompletionHandler)} instead.
     * @return A cancellable request.
     */
    public Request deleteByQueryAsync(@NonNull Query query, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                deleteByQuery(queryCopy, requestOptions);
                return new JSONObject();
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
    public Request deleteByAsync(@NonNull Query query, CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return deleteBy(queryCopy);
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
        return getSettingsAsync(/* requestOptions */ null, completionHandler);
    }

    /**
     * Get this index's settings (asynchronously).
     *
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request getSettingsAsync(@Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return getSettings(2, requestOptions);
            }
        }.start();
    }

    /**
     * Set this index's settings (asynchronously).
     * <p>
     * Please refer to our <a href="https://www.algolia.com/doc/android#index-settings">API documentation</a> for the
     * list of supported settings.
     *
     * @param settings          New settings.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request setSettingsAsync(final @NonNull JSONObject settings, CompletionHandler completionHandler) {
        return setSettingsAsync(settings, /* forwardToReplicas: */ false, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Set this index's settings (asynchronously).
     * <p>
     * Please refer to our <a href="https://www.algolia.com/doc/android#index-settings">API documentation</a> for the
     * list of supported settings.
     *
     * @param settings          New settings.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request setSettingsAsync(final @NonNull JSONObject settings, final boolean forwardToReplicas, @Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return setSettings(settings, forwardToReplicas, requestOptions);
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
        return browseAsync(query, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Browse all index content (initial call).
     * This method should be called once to initiate a browse. It will return the first page of results and a cursor,
     * unless the end of the index has been reached. To retrieve subsequent pages, call `browseFromAsync` with that
     * cursor.
     *
     * @param query             The query parameters for the browse.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseAsync(@NonNull Query query, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        final Query queryCopy = new Query(query);
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return browse(queryCopy, requestOptions);
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
        return browseFromAsync(cursor, /* requestOptions: */ null, completionHandler);
    }

    /**
     * Browse the index from a cursor.
     * This method should be called after an initial call to `browseAsync()`. It returns a cursor, unless the end of
     * the index has been reached.
     *
     * @param cursor            The cursor of the next page to retrieve.
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request browseFromAsync(final @NonNull String cursor, @Nullable final RequestOptions requestOptions, @NonNull CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return browseFrom(cursor, requestOptions);
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
        return clearIndexAsync(/* requestOptions: */ null, completionHandler);
    }

    /**
     * Clear this index.
     *
     * @param requestOptions Request-specific options.
     * @param completionHandler The listener that will be notified of the request's outcome.
     * @return A cancellable request.
     */
    public Request clearIndexAsync(@Nullable final RequestOptions requestOptions, CompletionHandler completionHandler) {
        return getClient().new AsyncTaskRequest(completionHandler) {
            @NonNull
            @Override protected JSONObject run() throws AlgoliaException {
                return clearIndex(requestOptions);
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
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName, /* urlParameters: */ null, obj.toString(), false, requestOptions);
    }

    /**
     * Add an object in this index
     *
     * @param obj      the object to add.
     * @param objectID an objectID you want to attribute to this object
     *                 (if the attribute already exist the old object will be overwrite)
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject addObject(JSONObject obj, String objectID, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), /* urlParameters: */ null, obj.toString(), requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Custom batch
     *
     * @param actions the array of actions
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject batch(JSONArray actions, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONObject content = new JSONObject();
            content.put("requests", actions);
            return client.postRequest("/1/indexes/" + encodedIndexName + "/batch", /* urlParameters: */ null, content.toString(), false, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Add several objects
     *
     * @param inputArray contains an array of objects to add.
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject addObjects(JSONArray inputArray, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONArray array = new JSONArray();
            for (int n = 0; n < inputArray.length(); n++) {
                JSONObject action = new JSONObject();
                action.put("action", "addObject");
                action.put("body", inputArray.getJSONObject(n));
                array.put(action);
            }
            return batch(array, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID the unique identifier of the object to retrieve
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            return client.getRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), /* urlParameters: */ null, false, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get an object from this index
     *
     * @param objectID             the unique identifier of the object to retrieve
     * @param attributesToRetrieve contains the list of attributes to retrieve.
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject getObject(String objectID, List<String> attributesToRetrieve, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            String path = "/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8");
            Map<String, String> urlParameters = new HashMap<>();
            if (attributesToRetrieve != null) {
                urlParameters.put("attributesToRetrieve", AbstractQuery.buildCommaArray(attributesToRetrieve.toArray(new String[attributesToRetrieve.size()])));
            }
            return client.getRequest(path, urlParameters, false, requestOptions);
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
        return getObjects(objectIDs, /* attributesToRetrieve: */ null, /* requestOptions: */ null);
    }

    /**
     * Get several objects from this index
     *
     * @param objectIDs            the array of unique identifier of objects to retrieve
     * @param attributesToRetrieve contains the list of attributes to retrieve.
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject getObjects(@NonNull List<String> objectIDs, @Nullable List<String> attributesToRetrieve, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            JSONArray requests = new JSONArray();
            for (String id : objectIDs) {
                JSONObject request = new JSONObject();
                request.put("indexName", this.indexName);
                request.put("objectID", id);
                if (attributesToRetrieve != null) {
                    request.put("attributesToRetrieve", new JSONArray(attributesToRetrieve));
                }
                requests.put(request);
            }
            JSONObject body = new JSONObject();
            body.put("requests", requests);
            return client.postRequest("/1/indexes/*/objects", /* urlParameters: */ null, body.toString(), true, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Update partially an object (only update attributes passed in argument)
     *
     * @param partialObject the object attributes to override
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObject(JSONObject partialObject, String objectID, Boolean createIfNotExists, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            String path = "/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8") + "/partial";
            Map<String, String> urlParameters = new HashMap<>();
            if (createIfNotExists != null) {
                urlParameters.put("createIfNotExists", createIfNotExists.toString());
            }
            return client.postRequest(path, urlParameters, partialObject.toString(), false, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Partially Override the content of several objects
     *
     * @param inputArray the array of objects to update (each object must contains an objectID attribute)
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject partialUpdateObjects(JSONArray inputArray, boolean createIfNotExists, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            final String action = createIfNotExists ? "partialUpdateObject" : "partialUpdateObjectNoCreate";
            JSONArray array = new JSONArray();
            for (int n = 0; n < inputArray.length(); n++) {
                JSONObject obj = inputArray.getJSONObject(n);
                JSONObject operation = new JSONObject();
                operation.put("action", action);
                operation.put("objectID", obj.getString("objectID"));
                operation.put("body", obj);
                array.put(operation);
            }
            return batch(array, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Override the content of object
     *
     * @param object the object to save
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject saveObject(JSONObject object, String objectID, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            return client.putRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), /* urlParameters: */ null, object.toString(), requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override the content of several objects
     *
     * @param inputArray contains an array of objects to update (each object must contains an objectID attribute)
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject saveObjects(JSONArray inputArray, @Nullable RequestOptions requestOptions) throws AlgoliaException {
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
            return batch(array, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete an object from the index
     *
     * @param objectID the unique identifier of object to delete
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject deleteObject(String objectID, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        if (objectID.length() == 0) {
            throw new AlgoliaException("Invalid objectID");
        }
        try {
            return client.deleteRequest("/1/indexes/" + encodedIndexName + "/" + URLEncoder.encode(objectID, "UTF-8"), /* urlParameters: */ null, requestOptions);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete several objects
     *
     * @param objects the array of objectIDs to delete
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject deleteObjects(List<String> objects, @Nullable RequestOptions requestOptions) throws AlgoliaException {
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
            return batch(array, requestOptions);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete all objects matching a query using browse and deleteObjects.
     *
     * @param query the query string
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     * @deprecated use {@link Index#deleteBy(Query)} instead.
     */
    @Deprecated
    protected void deleteByQuery(@NonNull Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        try {
            boolean hasMore;
            do {
                // Browse index for the next batch of objects.
                // WARNING: Since deletion invalidates cursors, we always browse from the start.
                List<String> objectIDs = new ArrayList<>(1000);
                JSONObject content = browse(query, requestOptions);
                JSONArray hits = content.getJSONArray("hits");
                for (int i = 0; i < hits.length(); ++i) {
                    JSONObject hit = hits.getJSONObject(i);
                    objectIDs.add(hit.getString("objectID"));
                }
                hasMore = content.optString("cursor", null) != null;

                // Delete objects.
                JSONObject task = this.deleteObjects(objectIDs, /* requestOptions: */ null);
                this.waitTask(task.getString("taskID"));
            }
            while (hasMore);
        } catch (JSONException e) {
            throw new AlgoliaException(e.getMessage());
        }
    }

    /**
     * Delete all records matching the query.
     *
     * @param query the query string
     * @throws AlgoliaException
     */
    protected JSONObject deleteBy(@NonNull Query query) throws AlgoliaException {
        try {
            return client.postRequest("/1/indexes/" + indexName + "/deleteByQuery", query.getParameters(), new JSONObject().put("params", query.build()).toString(), false, /* requestOptions: */ null);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Search inside the index
     *
     * @return a JSONObject containing search results
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject search(@Nullable Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        if (query == null) {
            query = new Query();
        }

        String cacheKey = null;
        byte[] rawResponse = null;
        if (isCacheEnabled) {
            cacheKey = query.build();
            rawResponse = searchCache.get(cacheKey);
        }
        try {
            if (rawResponse == null) {
                rawResponse = searchRaw(query, requestOptions);
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
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected byte[] searchRaw(@Nullable Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        if (query == null) {
            query = new Query();
        }

        try {
            String paramsString = query.build();
            if (paramsString.length() > 0) {
                JSONObject body = new JSONObject();
                body.put("params", paramsString);
                return client.postRequestRaw("/1/indexes/" + encodedIndexName + "/query", /* urlParameters: */ null, body.toString(), true, requestOptions);
            } else {
                return client.getRequestRaw("/1/indexes/" + encodedIndexName, /* urlParameters: */ null, true, requestOptions);
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
                JSONObject obj = client.getRequest("/1/indexes/" + encodedIndexName + "/task/" + URLEncoder.encode(taskID, "UTF-8"), /* urlParameters: */ null, false, /* requestOptions: */ null);
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
     * Waits for the publication of a task on the server.
     * All server task are asynchronous and you can check with this method that the task is published.
     *
     * @param taskID the id of the task returned by server
     * @throws AlgoliaException
     */
    protected JSONObject waitTask(String taskID) throws AlgoliaException {
        return waitTask(taskID, MAX_TIME_MS_TO_WAIT);
    }

    public static final int DEFAULT_SETTINGS_VERSION = 2;

    /**
     * Gets the settings of this index.
     *
     * @param requestOptions Request-specific options.
     */
    protected JSONObject getSettings(@Nullable RequestOptions requestOptions) throws AlgoliaException {
        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("getVersion", Integer.toString(DEFAULT_SETTINGS_VERSION));
        return client.getRequest("/1/indexes/" + encodedIndexName + "/settings", urlParameters, false, requestOptions);
    }


    /**
     * Gets the settings of this index for a specific settings format.
     *
     * @param formatVersion the version of a settings format.
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject getSettings(int formatVersion, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("getVersion", Integer.toString(formatVersion));
        return client.getRequest("/1/indexes/" + encodedIndexName + "/settings", urlParameters, false, requestOptions);
    }

    /**
     * Set settings for this index.
     *
     * @param settings          the settings object.
     * @param forwardToReplicas if true, the new settings will be forwarded to replica indices.
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject setSettings(JSONObject settings, boolean forwardToReplicas, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("forwardToReplicas", Boolean.toString(forwardToReplicas));
        return client.putRequest("/1/indexes/" + encodedIndexName + "/settings", urlParameters, settings.toString(), requestOptions);
    }

    /**
     * Delete the index content without removing settings and index specific API keys.
     *
     * @param requestOptions Request-specific options.
     * @throws AlgoliaException
     */
    protected JSONObject clearIndex(@Nullable RequestOptions requestOptions) throws AlgoliaException {
        return client.postRequest("/1/indexes/" + encodedIndexName + "/clear", /* urlParameters: */ null, "", false, requestOptions);
    }

    protected JSONObject browse(@NonNull Query query, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        return client.getRequest("/1/indexes/" + encodedIndexName + "/browse", query.getParameters(), true, requestOptions);
    }

    protected JSONObject browseFrom(@NonNull String cursor, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        Map<String, String> urlParameters = new HashMap<>();
        urlParameters.put("cursor", cursor);
        return client.getRequest("/1/indexes/" + encodedIndexName + "/browse", urlParameters, true, requestOptions);
    }

    /**
     * Run multiple queries on this index with one API call.
     * A variant of {@link Client#multipleQueries(List, String, RequestOptions)} where all queries target this index.
     *
     * @param queries  Queries to run.
     * @param strategy Strategy to use.
     * @param requestOptions Request-specific options.
     * @return The JSON results returned by the server.
     * @throws AlgoliaException
     */
    protected JSONObject multipleQueries(@NonNull List<Query> queries, String strategy, @Nullable RequestOptions requestOptions) throws AlgoliaException {
        List<IndexQuery> requests = new ArrayList<>(queries.size());
        for (Query query : queries) {
            requests.add(new IndexQuery(this, query));
        }
        return client.multipleQueries(requests, strategy, requestOptions);
    }
}
